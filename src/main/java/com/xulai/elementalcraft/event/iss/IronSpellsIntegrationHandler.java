package com.xulai.elementalcraft.event.iss;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.config.ElementalISSIntegrationConfig;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.enchantment.ModEnchantments;
import com.xulai.elementalcraft.event.ReactionHandler;
import com.xulai.elementalcraft.event.ScorchedHandler;
import com.xulai.elementalcraft.event.StaticShockHandler;
import com.xulai.elementalcraft.event.SteamReactionHandler;
import com.xulai.elementalcraft.event.WetnessHandler;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.sound.ModSounds;
import com.xulai.elementalcraft.util.ElementDamageHelper;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class IronSpellsIntegrationHandler {

    private static final Random RANDOM = new Random();
    private static final boolean ISS_LOADED;

    private static final List<String> THUNDER_SPELL_IDS = List.of(
            "irons_spellbooks:lightning_lance",
            "irons_spellbooks:chain_lightning",
            "irons_spellbooks:ball_lightning",
            "irons_spellbooks:electrocute",
            "irons_spellbooks:lightning_bolt",
            "irons_spellbooks:shockwave",
            "irons_spellbooks:thunderstorm",
            "irons_spellbooks:ascension",
            "irons_spellbooks:volt_strike");

    private static final List<String> NATURE_SPELL_IDS = List.of(
            "irons_spellbooks:acid_orb",
            "irons_spellbooks:poison_arrow",
            "irons_spellbooks:earthquake",
            "irons_spellbooks:firefly_swarm",
            "irons_spellbooks:poison_spray",
            "irons_spellbooks:oakskin",
            "irons_spellbooks:poison_splash",
            "irons_spellbooks:root",
            "irons_spellbooks:stomp");

    private static final Set<String> NATURE_DAMAGE_SPELLS = Set.of(
            "irons_spellbooks:acid_orb",
            "irons_spellbooks:poison_arrow",
            "irons_spellbooks:earthquake",
            "irons_spellbooks:firefly_swarm",
            "irons_spellbooks:poison_spray",
            "irons_spellbooks:stomp");

    private static final String NBT_MOB_CASTER = "EC_ISS_MobCaster";
    private static final String NBT_MOB_ELEMENT = "EC_ISS_MobElement";
    private static final String NBT_MOB_NEXT_WET = "EC_ISS_NextWet";
    private static final String NBT_MOB_CAST_CD = "EC_ISS_CastCD";

    private static Object spellRegistryGetSpell;
    private static Object spellRegistryNone;
    private static Object abstractSpellOnCast;
    private static Object castSourceMob;
    private static Object magicDataCtor;

    private static final net.minecraft.world.effect.MobEffect REND_EFFECT;

    private static final ResourceKey<DamageType> ISS_LIGHTNING_MAGIC = ResourceKey.create(
            Registries.DAMAGE_TYPE, new ResourceLocation("irons_spellbooks", "lightning_magic"));

    private static final String NBT_ISS_ACTIVE = "EC_ISS_Active";
    private static final String NBT_ISS_DAMAGE = "EC_ISS_Damage";
    private static final String NBT_ISS_ATTACKER = "EC_ISS_Attacker";
    private static final String NBT_ISS_MAINHAND_ENCH = "EC_ISS_MainhandEnch";
    private static final String NBT_ISS_OFFHAND_ENCH = "EC_ISS_OffhandEnch";
    private static final String NBT_ISS_PARALYSIS_CD = "EC_ISS_ParalysisCooldown";


    private static final Map<ResourceKey<Level>, ISSElectrifiedWater> ACTIVE_WATERS = new HashMap<>();

    private record ISSElectrifiedWater(
            BlockPos center, double range, long startTick, int duration,
            float recordedDamage, Set<UUID> damagedEntities, long lastParticleTick
    ) {}

    static {
        System.out.println("[EC ROOT DEBUG] === Static block START ===");
        boolean loaded = false;
        try {
            loaded = ModList.get() != null && ModList.get().isLoaded("irons_spellbooks");
        } catch (Exception e) {
            loaded = false;
        }
        ISS_LOADED = loaded;
        System.out.println("[EC ROOT DEBUG] ISS_LOADED=" + ISS_LOADED);

        if (ISS_LOADED) {
            try {
                Class<?> reg = Class.forName("io.redspace.ironsspellbooks.api.registry.SpellRegistry");
                spellRegistryGetSpell = reg.getMethod("getSpell", String.class);
                spellRegistryNone = reg.getMethod("none");

                Class<?> spell = Class.forName("io.redspace.ironsspellbooks.api.spells.AbstractSpell");
                Class<?> cs = Class.forName("io.redspace.ironsspellbooks.api.spells.CastSource");
                Class<?> md = Class.forName("io.redspace.ironsspellbooks.api.magic.MagicData");
                abstractSpellOnCast = spell.getMethod("onCast", Level.class, int.class, LivingEntity.class, cs, md);
                castSourceMob = cs.getField("MOB").get(null);
                magicDataCtor = md.getConstructor(boolean.class);
            } catch (Exception e) {
                ElementalCraft.LOGGER.error("Failed to cache ISS reflection", e);
            }
        }

        REND_EFFECT = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("irons_spellbooks", "rend"));
        System.out.println("[EC ROOT DEBUG] === Static block END ===");
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        var source = event.getSource();
        if (!(source.getDirectEntity() instanceof LightningBolt)) return;

        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) return;
        if (!target.hasEffect(ModMobEffects.WETNESS.get())) return;

        if (isImmuneToParalysis(target)) return;
        if (target.getPersistentData().getInt("ec_paralysis_cooldown_timer") > 0) return;

        int maxStacks = ElementalThunderFrostReactionsConfig.staticMaxTotalStacks;
        int duration = maxStacks * ElementalThunderFrostReactionsConfig.staticDurationPerStackTicks;

        CompoundTag data = target.getPersistentData();
        int current = data.getInt("ec_static_stacks");
        if (current >= maxStacks) return;

        data.putInt("ec_static_stacks", maxStacks);
        data.putInt("ec_static_timer", duration);

        target.addEffect(new MobEffectInstance(
                ModMobEffects.STATIC_SHOCK.get(), duration, maxStacks - 1, false, false, true
        ));

        if (target.isInWater()
                && ElementalThunderFrostReactionsConfig.waterElectrificationRangeBase > 0) {
            int pDuration = ElementalThunderFrostReactionsConfig.waterElectrificationParalysisDuration;
            if (!StaticShockHandler.tryTriggerWaterElectrification(target, pDuration)) {
                data.remove("ec_static_stacks");
                data.remove("ec_static_timer");
                target.removeEffect(ModMobEffects.STATIC_SHOCK.get());
                WetnessHandler.clearWetnessData(target);
                target.addEffect(new MobEffectInstance(
                        ModMobEffects.PARALYSIS.get(), pDuration, 0, false, false, true
                ));
            }
            return;
        }

        LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
        WetnessHandler.resolveElementReactionConflict(target, attacker);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurtSpellDamage(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!ISS_LOADED) return;
        if (!isLightningSpellDamage(event.getSource())) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;

        int perStack = ElementalISSIntegrationConfig.thunderEnhancementPerStaticStack;
        if (perStack <= 0) return;

        int thunderEnhancement = ElementUtils.getDisplayEnhancement(attacker, ElementType.THUNDER);
        int stacks = thunderEnhancement / perStack;
        if (stacks <= 0) return;

        event.getEntity().getPersistentData().putInt("EC_ISS_DiffStacks", stacks);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onNatureSpellHit(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!ISS_LOADED) return;
        if (!isNatureSpellDamage(event.getSource())) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;

        if (attacker instanceof Mob mob && mob.getPersistentData().getBoolean(NBT_MOB_CASTER)) {
            CompoundTag mobData = mob.getPersistentData();
            mobData.remove("EC_ISS_MissCount");
            mobData.remove("EC_ISS_PendingCast");
            int cd = mob.getHealth() / mob.getMaxHealth() < ElementalISSIntegrationConfig.mobLowHealthThreshold
                    ? ElementalISSIntegrationConfig.mobAggressiveCastCooldown
                    : ElementalISSIntegrationConfig.mobNormalCastCooldown;
            mobData.putLong(NBT_MOB_CAST_CD, mob.level().getGameTime() + cd);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingDamageHighest(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!isLightningSpellDamage(event.getSource())) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;

        if (attacker instanceof Mob mob && mob.getPersistentData().getBoolean(NBT_MOB_CASTER)) {
            CompoundTag mobData = mob.getPersistentData();
            mobData.remove("EC_ISS_MissCount");
            mobData.remove("EC_ISS_PendingCast");
        }

        CompoundTag data = event.getEntity().getPersistentData();

        saveAndClearEnchantments(attacker.getMainHandItem(), data, NBT_ISS_MAINHAND_ENCH);
        saveAndClearEnchantments(attacker.getOffhandItem(), data, NBT_ISS_OFFHAND_ENCH);

        data.putBoolean(NBT_ISS_ACTIVE, true);
        data.putUUID(NBT_ISS_ATTACKER, attacker.getUUID());

        if (ElementUtils.getDisplayEnhancement(attacker, ElementType.THUNDER) > 0) {
            data.putFloat(NBT_ISS_DAMAGE, event.getAmount());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamageLowest(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        CompoundTag data = event.getEntity().getPersistentData();
        if (!data.getBoolean(NBT_ISS_ACTIVE)) return;

        data.remove(NBT_ISS_ACTIVE);

        restoreAttackerEnchantments(event.getEntity(), data);

        if (!data.contains(NBT_ISS_DAMAGE)) {
            return;
        }

        float spellDamage = data.getFloat(NBT_ISS_DAMAGE);
        data.remove(NBT_ISS_DAMAGE);

        LivingEntity target = event.getEntity();
        if (target.isDeadOrDying()) return;

        if (target.hasEffect(ModMobEffects.WETNESS.get())) {
            triggerParalysis(target);
        }

        if (isInOrOnWater(target)) {
            spawnElectrifiedWater(target, spellDamage);
        }

        if (target.hasEffect(ModMobEffects.SPORES.get())) {
            tryTriggerSporeBlast(target);
        }

        if (SteamReactionHandler.isInCondensingCloud(target)) {
            if (chargeSteamCloud(target)) {
                applyCondensingCloudParalysis(target);
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.level.isClientSide()) return;
        if (event.phase != TickEvent.Phase.END) return;
        if (!ISS_LOADED) return;

        ResourceKey<Level> dim = event.level.dimension();
        ISSElectrifiedWater water = ACTIVE_WATERS.get(dim);
        if (water == null) return;

        long gameTime = event.level.getGameTime();

        if (gameTime - water.startTick >= water.duration) {
            ACTIVE_WATERS.remove(dim);
            return;
        }

        if (!(event.level instanceof ServerLevel serverLevel)) return;

        AABB area = new AABB(
                water.center.getX() - water.range, water.center.getY() - water.range, water.center.getZ() - water.range,
                water.center.getX() + water.range, water.center.getY() + water.range, water.center.getZ() + water.range
        );

        for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, area)) {
            if (water.damagedEntities.contains(entity.getUUID())) continue;
            if (entity instanceof Player p && p.isCreative()) continue;
            if (entity.isDeadOrDying()) continue;
            if (!isInOrOnWater(entity)) continue;
            if (isImmuneToParalysis(entity)) continue;

            water.damagedEntities.add(entity.getUUID());

            if (water.duration > 0 && ElementalThunderFrostReactionsConfig.paralysisMaxStacks > 0) {
                entity.addEffect(new MobEffectInstance(
                        ModMobEffects.PARALYSIS.get(), water.duration, 0, false, false, true));
            }

            if (water.recordedDamage > 0) {
                ElementDamageHelper.applyDamage(entity, water.recordedDamage,
                        ModDamageTypes.source(serverLevel, ModDamageTypes.STATIC_SHOCK));
            }
        }
    }

    @SubscribeEvent
    public static void onMobTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!ISS_LOADED) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        CompoundTag data = mob.getPersistentData();
        if (!data.getBoolean(NBT_MOB_CASTER)) return;

        if (!data.getBoolean("EC_ISS_Equipped")) {
            equipCasterMob(mob, data);
        }

        if ("nature".equals(data.getString(NBT_MOB_ELEMENT))) {
            onNatureMobTick(mob, data);
            return;
        }

        long gameTime = mob.level().getGameTime();

        if (mob.getHealth() / mob.getMaxHealth() < ElementalISSIntegrationConfig.mobLowHealthThreshold) {
            LivingEntity target = mob.getTarget();
            if (target != null && target.isAlive()) {
                if (gameTime >= data.getLong(NBT_MOB_CAST_CD)) {
                    castThunderSpell(mob, target, true);
                    data.putLong(NBT_MOB_CAST_CD, gameTime + ElementalISSIntegrationConfig.mobAggressiveCastCooldown);
                }
            }
            return;
        }

        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return;

        if (isImmuneToParalysis(target)) {
            if (gameTime >= data.getLong(NBT_MOB_CAST_CD)) {
                castThunderSpell(mob, target, false);
                data.putLong(NBT_MOB_CAST_CD, gameTime + ElementalISSIntegrationConfig.mobNormalCastCooldown);
            }
            return;
        }

        CompoundTag td = target.getPersistentData();
        boolean onCooldown = gameTime < td.getLong(NBT_ISS_PARALYSIS_CD)
                || gameTime < td.getLong("ec_paralysis_cooldown_timer");

        if (onCooldown) {
            int wetnessLevel = WetnessHandler.getWetnessLevel(target);
            int maxWetness = ElementalFireNatureReactionsConfig.wetnessMaxLevel;

            if (wetnessLevel >= maxWetness) return;

            if (gameTime >= data.getLong(NBT_MOB_NEXT_WET)) {
                throwSplashWaterBottle(mob, target);
                data.putLong(NBT_MOB_NEXT_WET, gameTime + ElementalISSIntegrationConfig.mobWaterBottleInterval);
            }
            return;
        }

        long pendingCast = data.getLong("EC_ISS_PendingCast");
        if (pendingCast > 0 && gameTime >= pendingCast) {
            data.remove("EC_ISS_PendingCast");
            if (target.hasEffect(ModMobEffects.WETNESS.get())) {
                int missCount = data.getInt("EC_ISS_MissCount");
                if (missCount >= ElementalISSIntegrationConfig.mobMaxMissCount) {
                    data.remove("EC_ISS_MissCount");
                    data.putLong(NBT_MOB_CAST_CD, gameTime + ElementalISSIntegrationConfig.mobNormalCastCooldown);
                } else {
                    data.putLong(NBT_MOB_CAST_CD, 0);
                }
            } else {
                data.remove("EC_ISS_MissCount");
                data.putLong(NBT_MOB_CAST_CD, 0);
            }
            return;
        }
        if (pendingCast > 0) return;

        if (target.hasEffect(ModMobEffects.WETNESS.get())) {
            if (data.getLong(NBT_MOB_CAST_CD) == 0 || gameTime >= data.getLong(NBT_MOB_CAST_CD)) {
                castThunderSpell(mob, target, false);
                data.putLong("EC_ISS_PendingCast", gameTime + ElementalISSIntegrationConfig.mobWaterBottleInterval);
                data.putLong(NBT_MOB_CAST_CD, gameTime + ElementalISSIntegrationConfig.mobNormalCastCooldown);
            }
            return;
        }

        if (gameTime >= data.getLong(NBT_MOB_NEXT_WET)) {
            throwSplashWaterBottle(mob, target);
            data.putLong(NBT_MOB_NEXT_WET, gameTime + ElementalISSIntegrationConfig.mobWaterBottleInterval);
        }
        castThunderSpell(mob, target, false);
        data.putLong("EC_ISS_PendingCast", gameTime + ElementalISSIntegrationConfig.mobWaterBottleInterval);
        data.putLong(NBT_MOB_CAST_CD, gameTime + ElementalISSIntegrationConfig.mobNormalCastCooldown);
        data.putInt("EC_ISS_MissCount", 0);
    }

    @SubscribeEvent
    public static void onMobDrops(LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!ISS_LOADED) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        CompoundTag data = mob.getPersistentData();
        if (!data.getBoolean(NBT_MOB_CASTER)) return;

        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (stack.isEmpty()) continue;
            if (stack.getEnchantmentLevel(ModEnchantments.NATURE_STRIKE.get()) > 0
                    || stack.getEnchantmentLevel(ModEnchantments.THUNDER_STRIKE.get()) > 0) {
                stack.removeTagKey("Enchantments");
            }
        }
    }

    @SubscribeEvent
    public static void onLivingTickCheckRoot(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!ISS_LOADED) return;

        LivingEntity target = event.getEntity();
        Entity vehicle = target.getVehicle();
        if (vehicle == null || !isRootEntity(vehicle)) return;

        CompoundTag data = target.getPersistentData();
        String NBT_ROOT_SPORE_APPLIED = "ec_root_spore_applied";

        UUID currentRootUUID = vehicle.getUUID();
        if (!data.hasUUID(NBT_ROOT_SPORE_APPLIED) || !data.getUUID(NBT_ROOT_SPORE_APPLIED).equals(currentRootUUID)) {
            int perStack = ElementalISSIntegrationConfig.natureEnhancementPerSporeStack;
            if (perStack > 0) {
                LivingEntity owner = getRootOwner(vehicle);
                if (owner != null) {
                    int natureEnhancement = ElementUtils.getDisplayEnhancement(owner, ElementType.NATURE);
                    int amount = natureEnhancement / perStack;
                    if (amount > 0) {
                        ElementalCraft.LOGGER.info("[EC ROOT DEBUG] Tick applying {} spores to {}", amount, target.getName().getString());
                        ReactionHandler.stackSporeEffect(target, amount, null);
                    }
                }
            }
            data.putUUID(NBT_ROOT_SPORE_APPLIED, currentRootUUID);
        }

        if (!ScorchedHandler.isScorched(target)) return;

        ElementalCraft.LOGGER.info("[EC ROOT DEBUG] Tick detected scorched ROOT target, removing ROOT!");

        vehicle.ejectPassengers();
        vehicle.discard();

        if (data.contains(ScorchedHandler.NBT_SCORCHED_TICKS)) {
            int currentTicks = data.getInt(ScorchedHandler.NBT_SCORCHED_TICKS);
            int enhancedTicks = (int) (currentTicks * ElementalFireNatureReactionsConfig.poisonScorchDurationMultiplier);
            data.putInt(ScorchedHandler.NBT_SCORCHED_TICKS, enhancedTicks);

            float enhancedDmgMult = (float) ElementalFireNatureReactionsConfig.poisonScorchDamageMultiplier;
            data.putFloat(ScorchedHandler.NBT_SCORCHED_DAMAGE_MULT, enhancedDmgMult);
        }
    }

    private static void onNatureMobTick(Mob mob, CompoundTag data) {
        long gameTime = mob.level().getGameTime();
        String spellId = data.getString("EC_ISS_SpellId");

        long pendingCast = data.getLong("EC_ISS_PendingCast");
        if (pendingCast > 0 && gameTime >= pendingCast) {
            data.remove("EC_ISS_PendingCast");
            int missCount = data.getInt("EC_ISS_MissCount");
            boolean aggressive = mob.getHealth() / mob.getMaxHealth() < ElementalISSIntegrationConfig.mobLowHealthThreshold;
            int cooldown = aggressive ? ElementalISSIntegrationConfig.mobAggressiveCastCooldown : ElementalISSIntegrationConfig.mobNormalCastCooldown;
            if (missCount >= ElementalISSIntegrationConfig.mobMaxMissCount) {
                data.remove("EC_ISS_MissCount");
                cooldown = ElementalISSIntegrationConfig.mobNormalCastCooldown;
            }
            data.putLong(NBT_MOB_CAST_CD, gameTime + cooldown);
            return;
        }
        if (pendingCast > 0) return;

        if ("irons_spellbooks:acid_orb".equals(spellId)
                && REND_EFFECT != null && mob.getTarget() != null && mob.getTarget().hasEffect(REND_EFFECT)) {
            return;
        }

        if (mob.getHealth() / mob.getMaxHealth() < ElementalISSIntegrationConfig.mobLowHealthThreshold) {
            if (data.getLong(NBT_MOB_CAST_CD) > gameTime + ElementalISSIntegrationConfig.mobAggressiveCastCooldown) {
                data.putLong(NBT_MOB_CAST_CD, gameTime);
            }
            LivingEntity target = mob.getTarget();
            if (target != null && target.isAlive()
                    && NATURE_DAMAGE_SPELLS.contains(spellId)
                    && gameTime >= data.getLong(NBT_MOB_CAST_CD)) {
                castThunderSpell(mob, target, true);
                data.putLong("EC_ISS_PendingCast", gameTime + ElementalISSIntegrationConfig.mobWaterBottleInterval);
                data.putLong(NBT_MOB_CAST_CD, gameTime + ElementalISSIntegrationConfig.mobAggressiveCastCooldown);
                data.putInt("EC_ISS_MissCount", 0);
            }
            return;
        }

        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return;

        if (isImmuneToParalysis(target)) {
            if (gameTime >= data.getLong(NBT_MOB_CAST_CD)) {
                castThunderSpell(mob, target, false);
                data.putLong(NBT_MOB_CAST_CD, gameTime + ElementalISSIntegrationConfig.mobNormalCastCooldown);
            }
            return;
        }

        if (gameTime >= data.getLong(NBT_MOB_CAST_CD)) {
            castThunderSpell(mob, target, false);
            data.putLong("EC_ISS_PendingCast", gameTime + ElementalISSIntegrationConfig.mobWaterBottleInterval);
            data.putLong(NBT_MOB_CAST_CD, gameTime + ElementalISSIntegrationConfig.mobNormalCastCooldown);
            data.putInt("EC_ISS_MissCount", 0);
        }
    }

    private static void triggerParalysis(LivingEntity target) {
        if (isImmuneToParalysis(target)) {
            target.removeEffect(ModMobEffects.WETNESS.get());
            WetnessHandler.clearWetnessData(target);
            return;
        }

        CompoundTag data = target.getPersistentData();
        long gameTime = target.level().getGameTime();
        if (gameTime < data.getLong(NBT_ISS_PARALYSIS_CD)) {
            target.removeEffect(ModMobEffects.WETNESS.get());
            WetnessHandler.clearWetnessData(target);
            return;
        }

        int wetnessLevel = target.hasEffect(ModMobEffects.WETNESS.get())
                ? target.getEffect(ModMobEffects.WETNESS.get()).getAmplifier() + 1 : 1;
        int diffStacks = data.getInt("EC_ISS_DiffStacks");
        data.remove("EC_ISS_DiffStacks");
        int maxParalysis = ElementalThunderFrostReactionsConfig.paralysisMaxStacks;
        if (maxParalysis <= 0) {
            target.removeEffect(ModMobEffects.WETNESS.get());
            WetnessHandler.clearWetnessData(target);
            return;
        }

        int paralysisStacks = Math.min(Math.max(wetnessLevel, diffStacks), maxParalysis);
        int duration = ElementalThunderFrostReactionsConfig.paralysisDurationPerStackTicks * paralysisStacks;

        target.addEffect(new MobEffectInstance(
                ModMobEffects.PARALYSIS.get(), duration, paralysisStacks - 1, false, false, true));

        target.removeEffect(ModMobEffects.WETNESS.get());
        WetnessHandler.clearWetnessData(target);

        int cooldownTicks = ElementalThunderFrostReactionsConfig.paralysisCooldownTicks;
        data.putLong(NBT_ISS_PARALYSIS_CD, gameTime + duration + cooldownTicks);

        if (!target.level().isClientSide) {
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    ModSounds.ELECTRIC_ZAP.get(), SoundSource.PLAYERS, 1.0f, 0.5f);
        }
    }

    private static void spawnElectrifiedWater(LivingEntity target, float recordedDamage) {
        if (ElementalThunderFrostReactionsConfig.waterElectrificationRangeBase <= 0) return;
        if (recordedDamage <= 0) return;

        ResourceKey<Level> dim = target.level().dimension();
        ISSElectrifiedWater existing = ACTIVE_WATERS.get(dim);
        long gameTime = target.level().getGameTime();
        if (existing != null && gameTime - existing.startTick < existing.duration) return;

        int stacks = ElementalThunderFrostReactionsConfig.staticMaxTotalStacks;
        double range = ElementalThunderFrostReactionsConfig.waterElectrificationRangeBase
                + stacks * ElementalThunderFrostReactionsConfig.waterElectrificationRangePerStack;
        range = Math.min(range, ElementalThunderFrostReactionsConfig.waterElectrificationMaxRange);

        int duration = ElementalThunderFrostReactionsConfig.waterElectrificationParalysisDuration;

        ISSElectrifiedWater water = new ISSElectrifiedWater(
                target.blockPosition(), range, gameTime, duration,
                recordedDamage, new HashSet<>(), 0
        );
        water.damagedEntities().add(target.getUUID());

        if (target.level() instanceof ServerLevel sl) {
            AABB area = new AABB(
                    target.getX() - range, target.getY() - range, target.getZ() - range,
                    target.getX() + range, target.getY() + range, target.getZ() + range
            );

            for (LivingEntity entity : sl.getEntitiesOfClass(LivingEntity.class, area)) {
                if (entity == target) continue;
                if (entity instanceof Player p && p.isCreative()) continue;
                if (entity.isDeadOrDying()) continue;
                if (!isInOrOnWater(entity)) continue;
                if (isImmuneToParalysis(entity)) continue;

                water.damagedEntities().add(entity.getUUID());

                if (duration > 0 && ElementalThunderFrostReactionsConfig.paralysisMaxStacks > 0) {
                    entity.addEffect(new MobEffectInstance(
                            ModMobEffects.PARALYSIS.get(), duration, 0, false, false, true));
                }

                if (recordedDamage > 0) {
                    ElementDamageHelper.applyDamage(entity, recordedDamage,
                            ModDamageTypes.source(sl, ModDamageTypes.STATIC_SHOCK));
                }
            }
        }

        ACTIVE_WATERS.put(dim, water);
    }

    private static void tryTriggerSporeBlast(LivingEntity target) {
        MobEffectInstance sporeEffect = target.getEffect(ModMobEffects.SPORES.get());
        if (sporeEffect == null) return;
        int sporeStacks = sporeEffect.getAmplifier() + 1;

        if (ElementalThunderFrostReactionsConfig.staticSporeBlastBaseChance <= 0) return;

        int staticStacks = ElementalThunderFrostReactionsConfig.staticMaxTotalStacks;
        double chance = Math.min(1.0,
                ElementalThunderFrostReactionsConfig.staticSporeBlastBaseChance
                        + staticStacks * ElementalThunderFrostReactionsConfig.staticSporeBlastPerStaticStack
                        + sporeStacks * ElementalThunderFrostReactionsConfig.staticSporeBlastPerSporeStack);
        if (RANDOM.nextDouble() >= chance) return;

        ReactionHandler.triggerStaticSporeBlast(target,
                ElementalFireNatureReactionsConfig.scorchedTriggerThreshold);
    }

    private static void equipCasterMob(Mob mob, CompoundTag data) {
        try {
            boolean isNature = "nature".equals(data.getString(NBT_MOB_ELEMENT));
            List<String> spellList = isNature ? NATURE_SPELL_IDS : THUNDER_SPELL_IDS;

            Class<?> itemRegistry = Class.forName("io.redspace.ironsspellbooks.registries.ItemRegistry");
            Object scrollRO = itemRegistry.getField("SCROLL").get(null);
            Item scrollItem = (Item) scrollRO.getClass().getMethod("get").invoke(scrollRO);
            ItemStack scrollStack = new ItemStack(scrollItem);

            String spellId = spellList.get(RANDOM.nextInt(spellList.size()));
            Object spell = ((java.lang.reflect.Method) spellRegistryGetSpell).invoke(null, spellId);
            Object noneSpell = ((java.lang.reflect.Method) spellRegistryNone).invoke(null);
            if (spell == noneSpell) return;

            Class<?> spellContainer = Class.forName("io.redspace.ironsspellbooks.api.spells.ISpellContainer");
            java.lang.reflect.Method createScroll = spellContainer.getMethod("createScrollContainer",
                    Class.forName("io.redspace.ironsspellbooks.api.spells.AbstractSpell"), int.class, ItemStack.class);
            int maxLevel = (int) spell.getClass().getMethod("getMaxLevel").invoke(spell);
            int randomLevel = RANDOM.nextInt(maxLevel) + 1;
            createScroll.invoke(null, spell, randomLevel, scrollStack);

            data.putString("EC_ISS_SpellId", spellId);
            data.putInt("EC_ISS_SpellLevel", randomLevel);

            if (isNature) {
                mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                mob.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                mob.setItemSlot(EquipmentSlot.MAINHAND, scrollStack);
                mob.setDropChance(EquipmentSlot.MAINHAND, ElementalISSIntegrationConfig.scrollDropChance);
                scrollStack.enchant(ModEnchantments.NATURE_STRIKE.get(), 1);
            } else {
                mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                mob.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                mob.setItemSlot(EquipmentSlot.MAINHAND, scrollStack);
                mob.setDropChance(EquipmentSlot.MAINHAND, ElementalISSIntegrationConfig.scrollDropChance);
                scrollStack.enchant(ModEnchantments.THUNDER_STRIKE.get(), 1);

                ItemStack waterBottle = new ItemStack(Items.SPLASH_POTION);
                PotionUtils.setPotion(waterBottle, Potions.WATER);
                mob.setItemSlot(EquipmentSlot.OFFHAND, waterBottle);
                mob.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
            }

            data.putBoolean("EC_ISS_Equipped", true);
        } catch (Exception e) {
            ElementalCraft.LOGGER.error("Failed to equip ISS caster mob", e);
        }
    }

    private static void throwSplashWaterBottle(Mob mob, LivingEntity target) {
        ItemStack waterBottle = new ItemStack(Items.SPLASH_POTION);
        PotionUtils.setPotion(waterBottle, Potions.WATER);

        ThrownPotion potion = new ThrownPotion(mob.level(), mob);
        potion.setItem(waterBottle);
        potion.setXRot(potion.getXRot() - -20.0F);

        Vec3 vel = target.getDeltaMovement();
        double d0 = target.getX() + vel.x - mob.getX();
        double d1 = target.getEyeY() - 1.1 - mob.getEyeY();
        double d2 = target.getZ() + vel.z - mob.getZ();
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        potion.shoot(d0, d1 + d3 * 0.2, d2, 0.75F, 8.0F);

        mob.level().addFreshEntity(potion);
        mob.level().playSound(null, mob, SoundEvents.WITCH_THROW, mob.getSoundSource(), 1.0F, 0.8F);
    }

    private static void castThunderSpell(Mob mob, LivingEntity target, boolean forceHit) {
        if (target == null || !target.isAlive()) return;

        double dx = target.getX() - mob.getX();
        double dy = target.getEyeY() - mob.getEyeY();
        double dz = target.getZ() - mob.getZ();
        float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) -(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * (180.0 / Math.PI));
        mob.setYRot(yaw);
        mob.setYHeadRot(yaw);
        mob.setXRot(pitch);

        if (!forceHit) {
            Vec3 look = mob.getViewVector(1.0F);
            Vec3 toTarget = new Vec3(dx, dy, dz).normalize();
            if (look.dot(toTarget) < 0.87) return;
        }

        try {
            CompoundTag data = mob.getPersistentData();
            String spellId = data.getString("EC_ISS_SpellId");
            if (spellId.isEmpty()) {
                boolean isNature = "nature".equals(data.getString(NBT_MOB_ELEMENT));
                List<String> fallback = isNature ? NATURE_SPELL_IDS : THUNDER_SPELL_IDS;
                spellId = fallback.get(RANDOM.nextInt(fallback.size()));
            }
            int spellLevel = data.getInt("EC_ISS_SpellLevel");
            if (spellLevel <= 0) spellLevel = 1;

            Object spell = ((java.lang.reflect.Method) spellRegistryGetSpell).invoke(null, spellId);
            Object noneSpell = ((java.lang.reflect.Method) spellRegistryNone).invoke(null);
            if (spell == noneSpell) return;

            Object md = ((java.lang.reflect.Constructor<?>) magicDataCtor).newInstance(true);
            ((java.lang.reflect.Method) abstractSpellOnCast).invoke(spell, mob.level(), spellLevel, mob, castSourceMob, md);
        } catch (Exception e) {
            ElementalCraft.LOGGER.error("ISS mob cast failed", e);
        }
    }

    private static UUID getUUID(CompoundTag tag, String key) {
        if (!tag.contains(key + "Most")) return null;
        return tag.getUUID(key);
    }

    private static void putUUID(CompoundTag tag, String key, UUID uuid) {
        tag.putUUID(key, uuid);
    }

    private static void saveAndClearEnchantments(ItemStack stack, CompoundTag data, String key) {
        if (stack.isEmpty()) return;
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("Enchantments", 9)) {
            data.put(key, tag.getList("Enchantments", 10));
            tag.remove("Enchantments");
        }
    }

    private static void restoreAttackerEnchantments(LivingEntity target, CompoundTag data) {
        if (!data.contains(NBT_ISS_ATTACKER + "Most")) return;
        UUID attackerId = data.getUUID(NBT_ISS_ATTACKER);
        data.remove(NBT_ISS_ATTACKER + "Most");
        data.remove(NBT_ISS_ATTACKER + "Least");

        Entity attackerEntity = target.level().getPlayerByUUID(attackerId);
        if (attackerEntity == null && target.level() instanceof ServerLevel serverLevel) {
            attackerEntity = serverLevel.getEntity(attackerId);
        }
        if (!(attackerEntity instanceof LivingEntity attacker)) return;

        restoreEnchantments(attacker.getMainHandItem(), data, NBT_ISS_MAINHAND_ENCH);
        restoreEnchantments(attacker.getOffhandItem(), data, NBT_ISS_OFFHAND_ENCH);
    }

    private static void restoreEnchantments(ItemStack stack, CompoundTag data, String key) {
        if (stack.isEmpty()) return;
        if (!data.contains(key)) return;
        ListTag list = data.getList(key, 10);
        stack.getOrCreateTag().put("Enchantments", list);
        data.remove(key);
    }

    private static boolean chargeSteamCloud(LivingEntity target) {
        if (target.level().isClientSide) return false;
        if (ElementalThunderFrostReactionsConfig.staticSteamCloudTriggerStacks <= 0) return false;

        double searchRadius = ElementalFireNatureReactionsConfig.steamCloudRadius * 3.0;
        AABB box = target.getBoundingBox().inflate(searchRadius);
        boolean charged = false;

        for (AreaEffectCloud cloud : target.level().getEntitiesOfClass(AreaEffectCloud.class, box,
                c -> c.getTags().contains(SteamReactionHandler.TAG_STEAM_CLOUD)
                        && !c.getTags().contains(SteamReactionHandler.TAG_HIGH_HEAT)
                        && !c.getTags().contains(SteamReactionHandler.TAG_STATIC_CHARGED))) {
            if (!isEntityInSteamCloud(target, cloud)) continue;
            cloud.addTag(SteamReactionHandler.TAG_STATIC_CHARGED);
            charged = true;
        }

        if (charged) {
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.5f, 1.2f);
        }
        return charged;
    }

    private static void applyCondensingCloudParalysis(LivingEntity target) {
        if (target.level().isClientSide) return;
        if (ElementalThunderFrostReactionsConfig.paralysisMaxStacks <= 0) return;

        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).toString();
        if (ElementalThunderFrostReactionsConfig.cachedParalysisImmunityBlacklist.contains(entityId)) return;

        double searchRadius = ElementalFireNatureReactionsConfig.steamCloudRadius * 3.0;
        AABB box = target.getBoundingBox().inflate(searchRadius);
        int paralysisDuration = 0;

        for (AreaEffectCloud cloud : target.level().getEntitiesOfClass(AreaEffectCloud.class, box,
                c -> c.getTags().contains(SteamReactionHandler.TAG_STEAM_CLOUD)
                        && c.getTags().contains(SteamReactionHandler.TAG_STATIC_CHARGED))) {
            if (!isEntityInSteamCloud(target, cloud)) continue;
            paralysisDuration = cloud.getDuration();
            break;
        }

        if (paralysisDuration <= 0) return;

        target.addEffect(new MobEffectInstance(
                ModMobEffects.PARALYSIS.get(), paralysisDuration, 2, false, false, true
        ));
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                ModSounds.ELECTRIC_ZAP.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
    }

    private static boolean isEntityInSteamCloud(LivingEntity entity, AreaEffectCloud cloud) {
        if (cloud.getBoundingBox().inflate(0.1).intersects(entity.getBoundingBox())) {
            return true;
        }
        double heightCeiling = ElementalFireNatureReactionsConfig.steamCloudHeightCeiling;
        double dx = entity.getX() - cloud.getX();
        double dz = entity.getZ() - cloud.getZ();
        double hDist = Math.sqrt(dx * dx + dz * dz);
        double dy = entity.getY() - cloud.getY();
        return hDist <= cloud.getRadius() && dy >= 0 && dy <= heightCeiling;
    }

    private static boolean isLightningSpellDamage(DamageSource source) {
        if (!ISS_LOADED) return false;
        return source.is(ISS_LIGHTNING_MAGIC);
    }

    private static boolean isNatureSpellDamage(DamageSource source) {
        if (!ISS_LOADED) return false;
        if (source.is(ISS_LIGHTNING_MAGIC)) return false;
        java.util.Optional<ResourceKey<DamageType>> key = source.typeHolder().unwrapKey();
        return key.isPresent() && "irons_spellbooks".equals(key.get().location().getNamespace());
    }

    private static boolean isInOrOnWater(LivingEntity entity) {
        if (entity.isInWater()) return true;
        return entity.level().getFluidState(entity.blockPosition()).is(FluidTags.WATER);
    }

    private static boolean isRootEntity(Entity entity) {
        if (entity == null) return false;
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return key != null && "irons_spellbooks".equals(key.getNamespace()) && "root".equals(key.getPath());
    }

    private static LivingEntity getRootOwner(Entity rootEntity) {
        try {
            return (LivingEntity) rootEntity.getClass().getMethod("getOwner").invoke(rootEntity);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isImmuneToParalysis(LivingEntity entity) {
        var entityId = ForgeRegistries.ENTITY_TYPES
                .getKey(entity.getType()).toString();
        return ElementalThunderFrostReactionsConfig.cachedParalysisImmunityBlacklist
                .contains(entityId);
    }
}
