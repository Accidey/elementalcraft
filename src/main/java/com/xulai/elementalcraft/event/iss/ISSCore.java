package com.xulai.elementalcraft.event.iss;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.config.ElementalISSIntegrationConfig;
import com.xulai.elementalcraft.enchantment.ModEnchantments;
import com.xulai.elementalcraft.event.WetnessHandler;
import com.xulai.elementalcraft.util.AttributeEquipUtils;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class ISSCore {

    public static final Random RANDOM = new Random();
    public static final boolean ISS_LOADED;

    static final List<String> THUNDER_SPELL_IDS = List.of(
            "irons_spellbooks:lightning_lance",
            "irons_spellbooks:chain_lightning",
            "irons_spellbooks:ball_lightning",
            "irons_spellbooks:electrocute",
            "irons_spellbooks:lightning_bolt",
            "irons_spellbooks:shockwave",
            "irons_spellbooks:thunderstorm",
            "irons_spellbooks:ascension",
            "irons_spellbooks:volt_strike");

    static final List<String> NATURE_SPELL_IDS = List.of(
            "irons_spellbooks:acid_orb",
            "irons_spellbooks:poison_arrow",
            "irons_spellbooks:earthquake",
            "irons_spellbooks:firefly_swarm",
            "irons_spellbooks:poison_spray",
            "irons_spellbooks:oakskin",
            "irons_spellbooks:poison_splash",
            "irons_spellbooks:root",
            "irons_spellbooks:stomp");

    static final Set<String> NATURE_DAMAGE_SPELLS = Set.of(
            "irons_spellbooks:acid_orb",
            "irons_spellbooks:poison_arrow",
            "irons_spellbooks:earthquake",
            "irons_spellbooks:firefly_swarm",
            "irons_spellbooks:poison_spray",
            "irons_spellbooks:stomp");

    static final Set<String> NATURE_NO_SPORE_SPELLS = Set.of(
            "irons_spellbooks:poison_arrow",
            "irons_spellbooks:poison_spray",
            "irons_spellbooks:poison_splash");

    static final List<String> FROST_SPELL_IDS = List.of(
            "irons_spellbooks:cone_of_cold",
            "irons_spellbooks:icicle",
            "irons_spellbooks:ray_of_frost",
            "irons_spellbooks:frostwave",
            "irons_spellbooks:ice_spikes",
            "irons_spellbooks:snowball",
            "irons_spellbooks:frostbite",
            "irons_spellbooks:blizzard",
            "irons_spellbooks:ice_tomb",
            "irons_spellbooks:summon_polar_bear"
        );

    static final List<String> FIRE_SPELL_IDS = List.of(
            "irons_spellbooks:firebolt",
            "irons_spellbooks:fireball",
            "irons_spellbooks:burning_das",
            "irons_spellbooks:magma_bomb",
            "irons_spellbooks:flaming_barrage",
            "irons_spellbooks:flaming_strike",
            "irons_spellbooks:scorch",
            "irons_spellbooks:heat_surge",
            "irons_spellbooks:blaze_storm",
            "irons_spellbooks:fire_breath",
            "irons_spellbooks:fire_arrow"
        );

    static final Set<String> NON_AGGRESSIVE_SPELLS = Set.of(
            "irons_spellbooks:heat_surge",
            "irons_spellbooks:acid_orb",
            "irons_spellbooks:oakskin",
            "irons_spellbooks:fire_breath",
            "irons_spellbooks:cone_of_cold",
            "irons_spellbooks:electrocute"
        );

    static final String NBT_MOB_CASTER = "EC_ISS_MobCaster";
    static final String NBT_MOB_ELEMENT = "EC_ISS_MobElement";
    static final String NBT_MOB_NEXT_WET = "EC_ISS_NextWet";
    static final String NBT_MOB_CAST_CD = "EC_ISS_CastCD";
    static final String NBT_POLAR_BEAR_CAST = "EC_ISS_PolarBearCast";
    static final String NBT_POLAR_BEAR_COUNT = "EC_ISS_PolarBearCount";

    static final Set<String> NO_AGGRESSIVE_SPELLS = Set.of(
            "irons_spellbooks:heat_surge",
            "irons_spellbooks:acid_orb",
            "irons_spellbooks:oakskin",
            "irons_spellbooks:fire_breath",
            "irons_spellbooks:cone_of_cold",
            "irons_spellbooks:electrocute"
    );

    private static Object spellRegistryGetSpell;
    private static Object spellRegistryNone;
    private static Object abstractSpellOnCast;
    private static Object castSourceMob;
    private static Object magicDataCtor;

    public static ParticleOptions FIRE_PARTICLE = ParticleTypes.FLAME;
    private static boolean fireParticleInit;

    public static ParticleOptions getFireParticle() {
        if (!fireParticleInit && ISS_LOADED) {
            fireParticleInit = true;
            try {
                Class<?> reg = Class.forName("io.redspace.ironsspellbooks.registries.ParticleRegistry");
                Object supplier = reg.getField("DRAGON_FIRE_PARTICLE").get(null);
                ParticleOptions p = (ParticleOptions) supplier.getClass().getMethod("get").invoke(supplier);
                if (p != null) FIRE_PARTICLE = p;
            } catch (Exception ignored) {}
        }
        return FIRE_PARTICLE;
    }

    public static final ResourceKey<DamageType> ISS_FIRE_MAGIC = ResourceKey.create(
            Registries.DAMAGE_TYPE, new ResourceLocation("irons_spellbooks", "fire_magic"));
    public static final ResourceKey<DamageType> ISS_ICE_MAGIC = ResourceKey.create(
            Registries.DAMAGE_TYPE, new ResourceLocation("irons_spellbooks", "ice_magic"));
    public static final ResourceKey<DamageType> ISS_LIGHTNING_MAGIC = ResourceKey.create(
            Registries.DAMAGE_TYPE, new ResourceLocation("irons_spellbooks", "lightning_magic"));
    public static final ResourceKey<DamageType> ISS_NATURE_MAGIC = ResourceKey.create(
            Registries.DAMAGE_TYPE, new ResourceLocation("irons_spellbooks", "nature_magic"));
    public static final ResourceKey<DamageType> ISS_HOLY_MAGIC = ResourceKey.create(
            Registries.DAMAGE_TYPE, new ResourceLocation("irons_spellbooks", "holy_magic"));
    public static final ResourceKey<DamageType> ISS_ENDER_MAGIC = ResourceKey.create(
            Registries.DAMAGE_TYPE, new ResourceLocation("irons_spellbooks", "ender_magic"));
    public static final ResourceKey<DamageType> ISS_BLOOD_MAGIC = ResourceKey.create(
            Registries.DAMAGE_TYPE, new ResourceLocation("irons_spellbooks", "blood_magic"));
    public static final ResourceKey<DamageType> ISS_EVOCATION_MAGIC = ResourceKey.create(
            Registries.DAMAGE_TYPE, new ResourceLocation("irons_spellbooks", "evocation_magic"));
    public static final ResourceKey<DamageType> ISS_ELDRITCH_MAGIC = ResourceKey.create(
            Registries.DAMAGE_TYPE, new ResourceLocation("irons_spellbooks", "eldritch_magic"));

    static final String NBT_ISS_REFRESH_CD = "EC_ISS_RefreshCD";

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
                Class<?> spellReg = Class.forName("io.redspace.ironsspellbooks.api.registry.SpellRegistry");
                spellRegistryGetSpell = spellReg.getMethod("getSpell", String.class);
                spellRegistryNone = spellReg.getMethod("none");

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

        System.out.println("[EC ROOT DEBUG] === Static block END ===");
        System.out.println("[EC ROOT DEBUG] === Static block END ===");
    }

    @SubscribeEvent
    public static void onAnyEvent(net.minecraftforge.eventbus.api.Event event) {
        if (!ISS_LOADED) return;
        if (!event.getClass().getName().equals("io.redspace.ironsspellbooks.api.events.SpellOnCastEvent")) return;
        try {
            Entity entity = (Entity) event.getClass().getMethod("getEntity").invoke(event);
            if (!(entity instanceof Player player)) return;
            String spellId = (String) event.getClass().getMethod("getSpellId").invoke(event);
            int spellLevel = (int) event.getClass().getMethod("getSpellLevel").invoke(event);
            Object castSource = event.getClass().getMethod("getCastSource").invoke(event);
            player.getPersistentData().putString("EC_LastSpellId", spellId);
            player.getPersistentData().putInt("EC_LastSpellLevel", spellLevel);
            player.getPersistentData().putString("EC_LastCastSource", castSource.getClass().getMethod("name").invoke(castSource).toString());
        } catch (Exception ignored) {}
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onCasterMobSpellHit(net.minecraftforge.event.entity.living.LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!ISS_LOADED) return;
        if (event.isCanceled()) return;

        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;

        Entity directEntity = event.getSource().getDirectEntity();
        if (directEntity != null && directEntity.getClass().getName()
                .equals("io.redspace.ironsspellbooks.entity.mobs.SummonedPolarBear")) return;

        DamageSource source = event.getSource();
        String element = null;
        int spellStacks = 0;

        if (attacker instanceof Mob mob && mob.getPersistentData().getBoolean(NBT_MOB_CASTER)) {
            element = mob.getPersistentData().getString(NBT_MOB_ELEMENT);
            spellStacks = mob.getPersistentData().getInt("EC_ISS_SpellStacks");
        } else {
            if (ThunderSpellHandler.isLightningSpellDamage(source)) {
                element = "thunder";
            } else if (NatureSpellHandler.isNatureSpellDamage(source)) {
                element = "nature";
            } else if (FrostSpellHandler.isIceSpellDamage(source)) {
                element = "frost";
            } else if (FireSpellHandler.isFireSpellDamage(source)) {
                element = "fire";
            }

            if (element != null) {
                spellStacks = tryGetStacksFromItem(attacker);
            }
        }

        if (element == null) return;

        if ("nature".equals(element)) {
            NatureSpellHandler.handleReaction(target, attacker, source, spellStacks);
        } else if ("thunder".equals(element)) {
            ThunderSpellHandler.handleReaction(target, attacker, source, spellStacks);
        } else if ("frost".equals(element)) {
            FrostSpellHandler.handleReaction(target, attacker, source, spellStacks);
        } else if ("fire".equals(element)) {
            FireSpellHandler.handleReaction(target, attacker, source, spellStacks);
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
            NatureSpellHandler.onMobTick(mob, data);
            return;
        }

        if ("frost".equals(data.getString(NBT_MOB_ELEMENT))) {
            FrostSpellHandler.onMobTick(mob, data);
            return;
        }

        if ("fire".equals(data.getString(NBT_MOB_ELEMENT))) {
            FireSpellHandler.onMobTick(mob, data);
            return;
        }

        ThunderSpellHandler.onMobTick(mob, data);
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
                    || stack.getEnchantmentLevel(ModEnchantments.THUNDER_STRIKE.get()) > 0
                    || stack.getEnchantmentLevel(ModEnchantments.FROST_STRIKE.get()) > 0
                    || stack.getEnchantmentLevel(ModEnchantments.FIRE_STRIKE.get()) > 0) {
                stack.removeTagKey("Enchantments");
            }
        }
    }

    static void equipCasterMob(Mob mob, CompoundTag data) {
        try {
            String element = data.getString(NBT_MOB_ELEMENT);
            boolean isNature = "nature".equals(element);
            boolean isFrost = "frost".equals(element);
            boolean isFire = "fire".equals(element);
            List<String> spellList = isNature ? NATURE_SPELL_IDS : isFrost ? FROST_SPELL_IDS : isFire ? FIRE_SPELL_IDS : THUNDER_SPELL_IDS;

            Class<?> itemRegistry = Class.forName("io.redspace.ironsspellbooks.registries.ItemRegistry");
            Object scrollRO = itemRegistry.getField("SCROLL").get(null);
            Item scrollItem = (Item) scrollRO.getClass().getMethod("get").invoke(scrollRO);
            ItemStack scrollStack = new ItemStack(scrollItem);

            String spellId = spellList.get(RANDOM.nextInt(spellList.size()));
            Object spell = ((java.lang.reflect.Method) spellRegistryGetSpell).invoke(null, spellId);
            Object noneSpell = ((java.lang.reflect.Method) spellRegistryNone).invoke(null);
            if (spell == noneSpell) return;

            int maxLevel = (int) spell.getClass().getMethod("getMaxLevel").invoke(spell);

            ElementType elementType = isNature ? ElementType.NATURE : isFrost ? ElementType.FROST : isFire ? ElementType.FIRE : ElementType.THUNDER;
            int enhancement = ElementUtils.getDisplayEnhancement(mob, elementType);
            int spellStacks = computeStacksFromEnhancement(enhancement, element);

            int bestLevel = 1;
            int bestDiff = Integer.MAX_VALUE;
            for (int lvl = 1; lvl <= maxLevel; lvl++) {
                Object rarity = spell.getClass().getMethod("getRarity", int.class).invoke(spell, lvl);
                int rarityValue = (int) rarity.getClass().getMethod("getValue").invoke(rarity);
                int rarityStacks = rarityValue + 1;
                int diff = Math.abs(rarityStacks - spellStacks);
                if (diff < bestDiff) {
                    bestDiff = diff;
                    bestLevel = lvl;
                }
            }

            Class<?> spellContainer = Class.forName("io.redspace.ironsspellbooks.api.spells.ISpellContainer");
            java.lang.reflect.Method createScroll = spellContainer.getMethod("createScrollContainer",
                    Class.forName("io.redspace.ironsspellbooks.api.spells.AbstractSpell"), int.class, ItemStack.class);
            createScroll.invoke(null, spell, bestLevel, scrollStack);

            data.putString("EC_ISS_SpellId", spellId);
            data.putInt("EC_ISS_SpellLevel", bestLevel);
            data.putInt("EC_ISS_SpellStacks", spellStacks);

            if (isNature) {
                NatureSpellHandler.equipSlots(mob, scrollStack, spellId);
            } else if (isFrost) {
                FrostSpellHandler.equipSlots(mob, scrollStack, spellId);
            } else if (isFire) {
                FireSpellHandler.equipSlots(mob, scrollStack);
            } else {
                ThunderSpellHandler.equipSlots(mob, scrollStack, spellId);
            }

            data.putBoolean("EC_ISS_Equipped", true);
        } catch (Exception e) {
            ElementalCraft.LOGGER.error("Failed to equip ISS caster mob", e);
        }
    }

    private static int computeStacksFromEnhancement(int enhancement, String element) {
        double threshold;
        double step;
        if ("nature".equals(element)) {
            threshold = ElementalFireNatureReactionsConfig.natureParasiteBaseThreshold;
            step = ElementalFireNatureReactionsConfig.natureParasiteScalingStep;
        } else if ("frost".equals(element)) {
            threshold = ElementalThunderFrostReactionsConfig.frostStrengthThreshold;
            step = ElementalThunderFrostReactionsConfig.frostbiteScalingStep;
        } else if ("fire".equals(element)) {
            threshold = ElementalFireNatureReactionsConfig.wildfireTriggerThreshold;
            step = ElementalFireNatureReactionsConfig.natureParasiteScalingStep;
        } else {
            threshold = ElementalThunderFrostReactionsConfig.thunderStrengthThreshold;
            step = ElementalThunderFrostReactionsConfig.staticScalingStep;
        }
        if (threshold <= 0 || enhancement < threshold) return 1;
        int stepInt = (int) Math.max(1, step);
        return 1 + (enhancement - (int) threshold) / stepInt;
    }

    static void tryAggressiveCast(Mob mob, CompoundTag data, long gameTime) {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return;

        String spellId = data.getString("EC_ISS_SpellId");
        if (!NON_AGGRESSIVE_SPELLS.contains(spellId) && gameTime >= data.getLong(NBT_MOB_CAST_CD)) {
            castSpell(mob, target, true);
            data.putLong(NBT_MOB_CAST_CD, gameTime + ElementalISSIntegrationConfig.mobAggressiveCastCooldown);
        }
    }

    static void castSpell(Mob mob, LivingEntity target, boolean forceHit) {
        if (target == null || !target.isAlive()) return;

        if (target != mob) {
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
        }

        try {
            CompoundTag data = mob.getPersistentData();
            String spellId = data.getString("EC_ISS_SpellId");
            if (spellId.isEmpty()) {
                String el = data.getString(NBT_MOB_ELEMENT);
                boolean isNature = "nature".equals(el);
                boolean isFire = "fire".equals(el);
                List<String> fallback = isNature ? NATURE_SPELL_IDS : isFire ? FIRE_SPELL_IDS : THUNDER_SPELL_IDS;
                spellId = fallback.get(RANDOM.nextInt(fallback.size()));
            }
            int spellLevel = data.getInt("EC_ISS_SpellLevel");
            if (spellLevel <= 0) spellLevel = 1;

            if ("irons_spellbooks:ice_tomb".equals(spellId)) {
                try {
                    Class<?> iceTombClass = Class.forName("io.redspace.ironsspellbooks.entity.spells.ice_tomb.IceTombEntity");
                    java.lang.reflect.Constructor<?> ctor = iceTombClass.getConstructor(Level.class, Entity.class);
                    Object iceTomb = ctor.newInstance(mob.level(), target);
                    iceTomb.getClass().getMethod("setEvil").invoke(iceTomb);
                    iceTomb.getClass().getMethod("setLifetime", int.class).invoke(iceTomb, 20 * 5);
                    iceTomb.getClass().getMethod("setHealing", float.class).invoke(iceTomb, 0.0f);
                    ((Entity) iceTomb).setPos(target.getX(), target.getY(), target.getZ());
                    mob.level().addFreshEntity((Entity) iceTomb);
                    target.startRiding((Entity) iceTomb, true);
                } catch (Exception e) {
                    ElementalCraft.LOGGER.error("ISS mob ice tomb cast failed", e);
                }
                return;
            }

            Object spell = ((java.lang.reflect.Method) spellRegistryGetSpell).invoke(null, spellId);
            Object noneSpell = ((java.lang.reflect.Method) spellRegistryNone).invoke(null);
            if (spell == noneSpell) return;

            Object md = ((java.lang.reflect.Constructor<?>) magicDataCtor).newInstance(true);
            ((java.lang.reflect.Method) abstractSpellOnCast).invoke(spell, mob.level(), spellLevel, mob, castSourceMob, md);
        } catch (Exception e) {
            ElementalCraft.LOGGER.error("ISS mob cast failed", e);
        }
    }

    public static void throwSplashWaterBottle(Mob mob, LivingEntity target) {
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

    static boolean tryThrowWaterBottle(Mob mob, CompoundTag data, LivingEntity target, long gameTime) {
        var wetness = com.xulai.elementalcraft.potion.ModMobEffects.WETNESS.get();
        if (wetness != null && target.hasEffect(wetness)) return true;

        if (gameTime < data.getLong("EC_ISS_BottleCd")) return true;

        long pendingCast = data.getLong("EC_ISS_PendingCast");
        if (pendingCast > 0 && gameTime >= pendingCast) {
            data.remove("EC_ISS_PendingCast");
            if (data.contains("EC_ISS_SpellHit")) {
                data.remove("EC_ISS_SpellHit");
                data.remove("EC_ISS_PendingIsSpell");
            } else if (data.contains("EC_ISS_PendingIsSpell")) {
                data.remove("EC_ISS_PendingIsSpell");
                int miss = data.getInt("EC_ISS_MissCount") + 1;
                if (miss >= 2) {
                    data.putLong("EC_ISS_BottleCd", gameTime + 200);
                    data.remove("EC_ISS_MissCount");
                    return true;
                }
                data.putInt("EC_ISS_MissCount", miss);
                data.putLong(NBT_MOB_CAST_CD, 0);
            } else if (wetness == null || !target.hasEffect(wetness)) {
                int miss = data.getInt("EC_ISS_MissCount") + 1;
                if (miss >= 2) {
                    data.putLong("EC_ISS_BottleCd", gameTime + 200);
                    data.remove("EC_ISS_MissCount");
                    return true;
                }
                data.putInt("EC_ISS_MissCount", miss);
                data.putLong(NBT_MOB_CAST_CD, 0);
            } else {
                data.remove("EC_ISS_MissCount");
            }
        }
        if (pendingCast > 0) return false;

        if (gameTime >= data.getLong(NBT_MOB_NEXT_WET)) {
            throwSplashWaterBottle(mob, target);
            data.remove("EC_ISS_PendingIsSpell");
            data.putLong("EC_ISS_PendingCast", gameTime + 40);
            data.putLong(NBT_MOB_NEXT_WET, gameTime + 40);
        }
        return false;
    }

    static boolean tryThrowPoisonBottle(Mob mob, CompoundTag data, LivingEntity target, long gameTime) {
        if (target.hasEffect(net.minecraft.world.effect.MobEffects.POISON)) return true;

        if (gameTime < data.getLong("EC_ISS_BottleCd")) return true;

        long pendingCast = data.getLong("EC_ISS_PendingCast");
        if (pendingCast > 0 && gameTime >= pendingCast) {
            data.remove("EC_ISS_PendingCast");
            if (data.contains("EC_ISS_SpellHit")) {
                data.remove("EC_ISS_SpellHit");
                data.remove("EC_ISS_PendingIsSpell");
            } else if (data.contains("EC_ISS_PendingIsSpell")) {
                data.remove("EC_ISS_PendingIsSpell");
                int miss = data.getInt("EC_ISS_MissCount") + 1;
                if (miss >= 2) {
                    data.putLong("EC_ISS_BottleCd", gameTime + 200);
                    data.remove("EC_ISS_MissCount");
                    return true;
                }
                data.putInt("EC_ISS_MissCount", miss);
                data.putLong(NBT_MOB_CAST_CD, 0);
            } else if (!target.hasEffect(net.minecraft.world.effect.MobEffects.POISON)) {
                int miss = data.getInt("EC_ISS_MissCount") + 1;
                if (miss >= 2) {
                    data.putLong("EC_ISS_BottleCd", gameTime + 200);
                    data.remove("EC_ISS_MissCount");
                    return true;
                }
                data.putInt("EC_ISS_MissCount", miss);
                data.putLong(NBT_MOB_CAST_CD, 0);
            } else {
                data.remove("EC_ISS_MissCount");
            }
        }
        if (pendingCast > 0) return false;

        if (gameTime >= data.getLong(NBT_MOB_NEXT_WET)) {
            ItemStack potion = new ItemStack(Items.SPLASH_POTION);
            net.minecraft.world.item.alchemy.PotionUtils.setPotion(potion, net.minecraft.world.item.alchemy.Potions.POISON);
            ThrownPotion thrown = new ThrownPotion(mob.level(), mob);
            thrown.setItem(potion);
            thrown.setXRot(thrown.getXRot() - -20.0F);
            Vec3 vel = target.getDeltaMovement();
            double d0 = target.getX() + vel.x - mob.getX();
            double d1 = target.getEyeY() - 1.1 - mob.getEyeY();
            double d2 = target.getZ() + vel.z - mob.getZ();
            double d3 = Math.sqrt(d0 * d0 + d2 * d2);
            thrown.shoot(d0, d1 + d3 * 0.2, d2, 0.75F, 8.0F);
            mob.level().addFreshEntity(thrown);
            mob.level().playSound(null, mob, SoundEvents.WITCH_THROW, mob.getSoundSource(), 1.0F, 0.8F);
            data.putLong("EC_ISS_PendingCast", gameTime + 40);
            data.putLong(NBT_MOB_NEXT_WET, gameTime + 40);
            data.remove("EC_ISS_PendingIsSpell");
        }
        return false;
    }

    static boolean isInOrOnWater(LivingEntity entity) {
        if (entity.isInWater()) return true;
        return entity.level().getFluidState(entity.blockPosition()).is(FluidTags.WATER);
    }

    static boolean isImmuneToParalysis(LivingEntity entity) {
        var entityId = ForgeRegistries.ENTITY_TYPES
                .getKey(entity.getType()).toString();
        return com.xulai.elementalcraft.config.ElementalConfig.matchesBlacklist(
                com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig.cachedParalysisImmunityBlacklist, entityId);
    }

    static int tryGetStacksFromItem(LivingEntity attacker) {
        if (!ISS_LOADED) return 0;
        try {
            Class<?> containerClass = Class.forName("io.redspace.ironsspellbooks.api.spells.ISpellContainer");
            java.lang.reflect.Method getMethod = containerClass.getMethod("get", ItemStack.class);
            java.lang.reflect.Method getCount = containerClass.getMethod("getActiveSpellCount");
            java.lang.reflect.Method getAtIndex = containerClass.getMethod("getSpellAtIndex", int.class);

            for (ItemStack stack : new ItemStack[]{attacker.getMainHandItem(), attacker.getOffhandItem()}) {
                int stacks = checkStackForStacks(stack, getMethod, getCount, getAtIndex);
                if (stacks > 0) return stacks;
            }

            try {
                Class<?> curiosApi = Class.forName("top.theillusivec4.curios.api.CuriosApi");
                Object helper = curiosApi.getMethod("getCuriosHelper").invoke(null);
                Object resultOpt = helper.getClass().getMethod("findCurio", LivingEntity.class, String.class, int.class).invoke(helper, attacker, "spellbook", 0);
                if (resultOpt instanceof java.util.Optional<?> opt && opt.isPresent()) {
                    ItemStack stack = (ItemStack) opt.get().getClass().getMethod("stack").invoke(opt.get());
                    int stacks = checkStackForStacks(stack, getMethod, getCount, getAtIndex);
                    if (stacks > 0) return stacks;
                }
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
        return getStacksFromLastCast(attacker);
    }

    static int getStacksFromLastCast(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        String lastId = data.getString("EC_LastSpellId");
        int lastLevel = data.getInt("EC_LastSpellLevel");
        if (lastLevel > 0 && !lastId.isEmpty()) {
            try {
                Object spell = ((java.lang.reflect.Method) spellRegistryGetSpell).invoke(null, lastId);
                if (spell != ((java.lang.reflect.Method) spellRegistryNone).invoke(null)) {
                    Object rarity = spell.getClass().getMethod("getRarity", int.class).invoke(spell, lastLevel);
                    return (int) rarity.getClass().getMethod("getValue").invoke(rarity) + 1;
                }
            } catch (Exception ignored) {}
        }
        return 0;
    }

    private static int checkStackForStacks(ItemStack stack,
            java.lang.reflect.Method getMethod, java.lang.reflect.Method getCount,
            java.lang.reflect.Method getAtIndex) {
        try {
            if (stack == null || stack.isEmpty()) return 0;
            Object container = getMethod.invoke(null, stack);
            if (container == null) return 0;
            if ((int) getCount.invoke(container) <= 0) return 0;
            Object data = getAtIndex.invoke(container, 0);
            Object spell = data.getClass().getMethod("getSpell").invoke(data);
            int level = (int) data.getClass().getMethod("getLevel").invoke(data);
            Object rarity = spell.getClass().getMethod("getRarity", int.class).invoke(spell, level);
            return (int) rarity.getClass().getMethod("getValue").invoke(rarity) + 1;
        } catch (Exception e) { return 0; }
    }

    public static boolean isISSMagicDamage(DamageSource source) {
        if (!ISS_LOADED) return false;
        return source.is(ISS_FIRE_MAGIC) || source.is(ISS_ICE_MAGIC)
                || source.is(ISS_LIGHTNING_MAGIC) || source.is(ISS_NATURE_MAGIC)
                || source.is(ISS_HOLY_MAGIC) || source.is(ISS_ENDER_MAGIC)
                || source.is(ISS_BLOOD_MAGIC) || source.is(ISS_EVOCATION_MAGIC)
                || source.is(ISS_ELDRITCH_MAGIC);
    }
}
