package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.logic.MobAttributeLogic;
import com.xulai.elementalcraft.network.FireCounterLockPacket;
import net.minecraftforge.network.PacketDistributor;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.event.iss.ISSCore;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.event.ReactionHandler;
import com.xulai.elementalcraft.event.SteamReactionHandler;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import com.xulai.elementalcraft.util.ElementDamageHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class ScorchedHandler {
    public static final String NBT_SCORCHED_TICKS = "ec_scorched_ticks";
    public static final String NBT_SCORCHED_STRENGTH = "ec_scorched_str";
    public static final String NBT_SCORCHED_SOURCE_FIRE_POWER = "EC_ScorchedSourceFirePower";
    public static final String NBT_ATTACKER_SCORCHED_COOLDOWN = "ec_scorched_attacker_cd";
    public static final String NBT_SCORCHED_DAMAGE_MULT = "ec_scorched_dmg_mult";
    public static final String NBT_SCORCHED_DAMAGE_MULT_SRC = "ec_scorched_dmg_mult_src";
    public static final String NBT_SCORCHED_ATTACKER = "ec_scorched_attacker";
    public static final String NBT_SCORCHED_TICK_LOGGED = "ec_scorched_tick_logged";
    public static final String NBT_WETNESS_STEAM_COOLDOWN = "ec_wetness_steam_cooldown";
    public static final String NBT_SCORCHED_AURA_LOGGED = "ec_scorched_aura_logged";
    public static final String NBT_TEMP_SCORCH = "ec_temp_scorch";
    public static final String NBT_TEMP_SCORCH_STRENGTH = "ec_temp_scorch_str";
    private static final String NBT_CREEPER_POISON_ENHANCED = "ec_creeper_poison_enhanced";
    private static final String NBT_CREEPER_ORIGINAL_MAX_SWELL = "ec_creeper_orig_max_swell";
    private static final int CREEPER_ACCELERATED_FUSE_TICKS = 10;
    private static final Field MAX_SWELL_FIELD;
    private static final Field SWELL_DIR_FIELD;

    static {
        MAX_SWELL_FIELD = findCreeperField("maxSwell", "f_32271_");
        SWELL_DIR_FIELD = findCreeperField("DATA_SWELL_DIR", "f_32268_");
    }

    private static Field findCreeperField(String mojangName, String srgName) {
        for (String name : new String[]{mojangName, srgName}) {
            try {
                Field f = Creeper.class.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {}
        }
        return null;
    }
    public static final String NBT_TEMP_SCORCH_TTL = "ec_temp_scorch_ttl";
    public static final String NBT_FLEE_ACTIVE = "EC_FleeActive";

    public static class ScorchedApplyResult {
        public static final ScorchedApplyResult FAILED = new ScorchedApplyResult(0, ElementType.NONE, 1.0f);
        public final int adjustedDuration;
        public final ElementType targetElement;
        public final float multiplier;

        public ScorchedApplyResult(int adjustedDuration, ElementType targetElement, float multiplier) {
            this.adjustedDuration = adjustedDuration;
            this.targetElement = targetElement;
            this.multiplier = multiplier;
        }
    }

    public static ScorchedApplyResult applyScorched(LivingEntity target, LivingEntity attacker, int fireStrength, int duration, int sourceFirePower, float damageMultiplier, boolean bypassCooldown) {
        if (ElementalFireNatureReactionsConfig.scorchedTriggerThreshold <= 0) return ScorchedApplyResult.FAILED;
        if (target.level().isClientSide) return ScorchedApplyResult.FAILED;
        if (target instanceof Player player && player.isCreative()) return ScorchedApplyResult.FAILED;
        if (SteamReactionHandler.isInCondensingCloud(target)) {
            if (attacker != null) {
                DebugCommand.sendReactionFailed(target, "scorched", "steam_cloud", attacker.getDisplayName(), target.getDisplayName());
            }
            return ScorchedApplyResult.FAILED;
        }
        var key = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (key == null) return ScorchedApplyResult.FAILED;
        String entityId = key.toString();
        var blacklist = ElementalFireNatureReactionsConfig.cachedScorchedBlacklist;
        if (ElementalConfig.matchesBlacklist(blacklist, entityId)) {
            if (attacker != null) {
                DebugCommand.sendReactionFailed(target, "scorched", "blacklist", attacker.getDisplayName(), target.getDisplayName());
            }
            return ScorchedApplyResult.FAILED;
        }
        CompoundTag targetData = target.getPersistentData();
        if (targetData.contains(NBT_SCORCHED_TICKS) && targetData.getInt(NBT_SCORCHED_TICKS) > 0) {
            if (attacker != null) {
                DebugCommand.sendReactionFailed(target, "scorched", "already", attacker.getDisplayName(), target.getDisplayName());
            }
            return ScorchedApplyResult.FAILED;
        }
        if (FrostbiteHandler.isFrozen(target)) {
            if (attacker != null) {
                DebugCommand.sendReactionFailed(target, "scorched", "frozen", attacker.getDisplayName(), target.getDisplayName());
            }
            return ScorchedApplyResult.FAILED;
        }

        ElementType targetElement = ElementUtils.getConsistentAttackElement(target);
        float multiplier = getElementDurationMultiplier(targetElement);
        int adjustedDuration = (int) Math.round(duration * multiplier);

        if (adjustedDuration < 1) adjustedDuration = 1;
        long gameTime = target.level().getGameTime();
        targetData.putInt(NBT_SCORCHED_TICKS, adjustedDuration);
        targetData.putInt(NBT_SCORCHED_STRENGTH, fireStrength);
        targetData.putInt(NBT_SCORCHED_SOURCE_FIRE_POWER, sourceFirePower);
        targetData.putFloat(NBT_SCORCHED_DAMAGE_MULT, damageMultiplier);
        targetData.putInt(NBT_SCORCHED_TICK_LOGGED, 0);
        targetData.putInt(NBT_SCORCHED_AURA_LOGGED, 0);
        targetData.putString(NBT_SCORCHED_DAMAGE_MULT_SRC, "");
        if (attacker != null) {
            targetData.putUUID(NBT_SCORCHED_ATTACKER, attacker.getUUID());
        }

        if (attacker != null && !bypassCooldown) {
            CompoundTag attackerData = attacker.getPersistentData();
            attackerData.putLong(NBT_ATTACKER_SCORCHED_COOLDOWN, gameTime + adjustedDuration + ElementalFireNatureReactionsConfig.scorchedCooldown);
        }

        target.setRemainingFireTicks(adjustedDuration);

        if (target.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ISSCore.getFireParticle(), target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 20, 0.2, 0.2, 0.2, 0.0);
        }

        return new ScorchedApplyResult(adjustedDuration, targetElement, multiplier);
    }

    public static int applyScorched(LivingEntity target, LivingEntity attacker, int fireStrength, int duration, int sourceFirePower, float damageMultiplier) {
        return applyScorched(target, attacker, fireStrength, duration, sourceFirePower, damageMultiplier, false).adjustedDuration;
    }

    public static int applyScorched(LivingEntity target, LivingEntity attacker, int fireStrength, int duration, int sourceFirePower) {
        return applyScorched(target, attacker, fireStrength, duration, sourceFirePower, 1.0f, false).adjustedDuration;
    }

    public static boolean isScorched(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        return (data.contains(NBT_SCORCHED_TICKS) && data.getInt(NBT_SCORCHED_TICKS) > 0)
                || data.getBoolean(NBT_TEMP_SCORCH);
    }

    public static UUID getScorchedAttackerUUID(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (data.hasUUID(NBT_SCORCHED_ATTACKER)) {
            return data.getUUID(NBT_SCORCHED_ATTACKER);
        }
        return null;
    }

    public static void igniteCreeperIfScorched(LivingEntity entity) {
        if (!(entity instanceof Creeper creeper) || !creeper.isAlive() || creeper.isDeadOrDying()) return;
        if (creeper.hasEffect(MobEffects.POISON)) {
            creeper.getPersistentData().putBoolean(NBT_CREEPER_POISON_ENHANCED, true);
            creeper.removeEffect(MobEffects.POISON);
        }
        creeper.ignite();
        if (MAX_SWELL_FIELD != null && SWELL_DIR_FIELD != null) {
            try {
                int swellDir = creeper.getEntityData().get((EntityDataAccessor<Integer>) SWELL_DIR_FIELD.get(null));
                if (swellDir == 1) {
                    int originalMax = MAX_SWELL_FIELD.getInt(creeper);
                    creeper.getPersistentData().putInt(NBT_CREEPER_ORIGINAL_MAX_SWELL, originalMax);
                    MAX_SWELL_FIELD.setInt(creeper, CREEPER_ACCELERATED_FUSE_TICKS);
                }
            } catch (Exception ignored) {}
        }
    }

    private static void restoreMaxSwell(Creeper creeper) {
        if (MAX_SWELL_FIELD == null) return;
        CompoundTag data = creeper.getPersistentData();
        if (data.contains(NBT_CREEPER_ORIGINAL_MAX_SWELL)) {
            try {
                MAX_SWELL_FIELD.setInt(creeper, data.getInt(NBT_CREEPER_ORIGINAL_MAX_SWELL));
            } catch (Exception ignored) {}
            data.remove(NBT_CREEPER_ORIGINAL_MAX_SWELL);
        }
    }

    private static boolean creeperPoisonEnhancedActive = false;

    @SubscribeEvent
    public static void onExplosionStart(ExplosionEvent.Start event) {
        if (creeperPoisonEnhancedActive) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Explosion explosion = event.getExplosion();
        if (!(explosion.getExploder() instanceof Creeper creeper)) return;
        restoreMaxSwell(creeper);
        if (!creeper.getPersistentData().getBoolean(NBT_CREEPER_POISON_ENHANCED)) return;
        creeper.getPersistentData().remove(NBT_CREEPER_POISON_ENHANCED);
        event.setCanceled(true);
        float baseRadius = creeper.isPowered() ? 12.0f : 6.0f;
        creeperPoisonEnhancedActive = true;
        try {
            level.explode(creeper, creeper.getX(), creeper.getY(), creeper.getZ(),
                    baseRadius, Level.ExplosionInteraction.MOB);
        } finally {
            creeperPoisonEnhancedActive = false;
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!creeperPoisonEnhancedActive) return;
        if (!(event.getLevel() instanceof ServerLevel)) return;
        Explosion explosion = event.getExplosion();
        if (!(explosion.getExploder() instanceof Creeper creeper)) return;
        Vec3 pos = explosion.getPosition();
        float radius = creeper.isPowered() ? 12.0f : 6.0f;
        DamageSource source = ModDamageTypes.source(event.getLevel(), ModDamageTypes.LAVA_MAGIC, creeper);
        for (Entity entity : event.getAffectedEntities()) {
            if (!(entity instanceof LivingEntity living)) continue;
            double dist = entity.position().distanceTo(pos);
            if (dist > radius) continue;
            float damage = (float) ((1.0 - dist / radius) * radius * 7.0);
            living.hurt(source, damage);
        }
    }

    public static void clearScorched(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        data.remove(NBT_SCORCHED_TICKS);
        data.remove(NBT_SCORCHED_STRENGTH);
        data.remove(NBT_SCORCHED_SOURCE_FIRE_POWER);
        data.remove(NBT_SCORCHED_DAMAGE_MULT);
        data.remove(NBT_SCORCHED_DAMAGE_MULT_SRC);
        data.remove(NBT_SCORCHED_ATTACKER);
        data.remove(NBT_SCORCHED_TICK_LOGGED);
        data.remove(NBT_SCORCHED_AURA_LOGGED);
        data.remove(NBT_TEMP_SCORCH);
        data.remove(NBT_TEMP_SCORCH_STRENGTH);
        data.remove(NBT_TEMP_SCORCH_TTL);
        data.remove(NBT_WETNESS_STEAM_COOLDOWN);
        entity.clearFire();
        if (entity instanceof Creeper creeper) {
            restoreMaxSwell(creeper);
            data.remove(NBT_CREEPER_POISON_ENHANCED);
        }
    }

    public static boolean isTempScorched(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        return data.getBoolean(NBT_TEMP_SCORCH);
    }

    public static int getTempScorchFireStrength(LivingEntity entity) {
        return entity.getPersistentData().getInt(NBT_TEMP_SCORCH_STRENGTH);
    }

    public static int getScorchFireStrength(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (data.contains(NBT_SCORCHED_TICKS) && data.getInt(NBT_SCORCHED_TICKS) > 0) {
            return data.getInt(NBT_SCORCHED_SOURCE_FIRE_POWER);
        }
        if (data.getBoolean(NBT_TEMP_SCORCH)) {
            return data.getInt(NBT_TEMP_SCORCH_STRENGTH);
        }
        return 0;
    }

    public static void applyTempScorch(LivingEntity target, int fireStrength, int ttl) {
        if (SteamReactionHandler.isInCondensingCloud(target)) return;
        CompoundTag data = target.getPersistentData();
        data.putBoolean(NBT_TEMP_SCORCH, true);
        data.putInt(NBT_TEMP_SCORCH_STRENGTH, fireStrength);
        data.putInt(NBT_TEMP_SCORCH_TTL, ttl);
        data.putFloat(NBT_SCORCHED_DAMAGE_MULT, 1.0f);
        data.putString(NBT_SCORCHED_DAMAGE_MULT_SRC, "");
        target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), ttl));
    }

    public static void clearTempScorch(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        data.remove(NBT_TEMP_SCORCH);
        data.remove(NBT_TEMP_SCORCH_STRENGTH);
        data.remove(NBT_TEMP_SCORCH_TTL);
        data.remove(NBT_WETNESS_STEAM_COOLDOWN);
        if (!isScorched(entity)) {
            entity.clearFire();
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        if (entity instanceof Player player && player.isCreative()) return;

        CompoundTag data = entity.getPersistentData();
        if (data.contains(NBT_ATTACKER_SCORCHED_COOLDOWN)) {
            long cd = data.getLong(NBT_ATTACKER_SCORCHED_COOLDOWN);
            if (entity.level().getGameTime() >= cd) {
                data.remove(NBT_ATTACKER_SCORCHED_COOLDOWN);
            }
        }

        if (!data.contains(NBT_SCORCHED_TICKS)) {
            handleTempScorch(entity, data);
            return;
        }
        handleRegularScorch(entity, data);
        if (data.contains(NBT_FIRE_COUNTER_SAVED_SPEED) && data.contains(NBT_FIRE_COUNTER_SPEED_TIME)) {
            long elapsed = entity.level().getGameTime() - data.getLong(NBT_FIRE_COUNTER_SPEED_TIME);
            if (elapsed > 100 && entity instanceof net.minecraft.world.entity.Mob mob) {
                double savedSpeed = data.getDouble(NBT_FIRE_COUNTER_SAVED_SPEED);
                if (savedSpeed > 0) {
                    var attr = mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
                    if (attr != null) attr.setBaseValue(savedSpeed);
                }
                data.remove(NBT_FIRE_COUNTER_SAVED_SPEED);
                data.remove(NBT_FIRE_COUNTER_SPEED_TIME);
            }
        }
    }

    private static void handleTempScorch(LivingEntity entity, CompoundTag data) {
        if (!data.getBoolean(NBT_TEMP_SCORCH)) return;
        int ttl = data.getInt(NBT_TEMP_SCORCH_TTL) - 1;
        if (ttl <= 0) {
            clearTempScorch(entity);
            return;
        }
        data.putInt(NBT_TEMP_SCORCH_TTL, ttl);

        int tempResist = ElementUtils.getDisplayResistance(entity, ElementType.FIRE);
        if (tempResist >= ElementalFireNatureReactionsConfig.scorchedResistThreshold) {
            DebugCommand.sendReactionFailed(entity, "scorched", "resistance",
                    entity.getDisplayName(), tempResist, ElementalFireNatureReactionsConfig.scorchedResistThreshold);
            clearTempScorch(entity);
            return;
        }

        int tempStrength = data.getInt(NBT_TEMP_SCORCH_STRENGTH);

        int steamCooldown = data.getInt(NBT_WETNESS_STEAM_COOLDOWN);
        if (steamCooldown > 0) {
            data.putInt(NBT_WETNESS_STEAM_COOLDOWN, steamCooldown - 1);
            return;
        }
        if (ElementalFireNatureReactionsConfig.scorchedAuraSteamEnabled && WetnessHandler.getWetnessLevel(entity) > 0) {
            int wetness = WetnessHandler.getWetnessLevel(entity);
            int steamLevel = Math.max(1, Math.min(wetness, ElementalFireNatureReactionsConfig.steamHighHeatMaxLevel));
            WetnessHandler.clearWetnessData(entity);
            clearTempScorch(entity);
            SteamReactionHandler.spawnSteamCloud(entity, true, steamLevel);
            DebugCommand.sendReactionSuccess(entity, "scorched_aura_wet_steam",
                    entity.getDisplayName(),
                    Component.literal(String.valueOf(steamLevel)));
            return;
        }

        if (entity.hasEffect(MobEffects.POISON)) {
            float poisonMult = (float) ElementalFireNatureReactionsConfig.poisonScorchDamageMultiplier;
            data.putFloat(NBT_SCORCHED_DAMAGE_MULT, poisonMult);
            data.putString(NBT_SCORCHED_DAMAGE_MULT_SRC, "poison");
            entity.removeEffect(MobEffects.POISON);
            if (entity.getRemainingFireTicks() < ttl) {
                entity.setRemainingFireTicks(ttl);
            }
        }

        if (handleFrostbiteToWetness(entity, "temp_scorch_frostbite_to_wetness")) {
            return;
        }

        if (entity.isInWater()) {
            ServerLevel sLevel = (ServerLevel) entity.level();
            triggerThermalShock(entity, sLevel, ttl, data.getInt(NBT_TEMP_SCORCH_STRENGTH));
            clearTempScorch(entity);
            return;
        }

        if (entity.getRemainingFireTicks() < ttl) {
            entity.setRemainingFireTicks(ttl);
        }

        if (entity.tickCount % 20 == 0) {
            int fs = data.getInt(NBT_TEMP_SCORCH_STRENGTH);
            float baseDamage = calculateScorchedDamage(fs, entity);
            ElementType entityElement = ElementUtils.getConsistentAttackElement(entity);
            float elementMult = getElementDurationMultiplier(entityElement);
            applyScorchedTickDamage(entity, baseDamage, entityElement, elementMult, (ServerLevel) entity.level(), false);
            igniteCreeperIfScorched(entity);
        }
    }

    private static void handleRegularScorch(LivingEntity entity, CompoundTag data) {
        int ticks = data.getInt(NBT_SCORCHED_TICKS);

        if (ticks <= 0) {
            clearScorched(entity);
            return;
        }

        int resistPoints = ElementUtils.getDisplayResistance(entity, ElementType.FIRE);
        if (resistPoints >= ElementalFireNatureReactionsConfig.scorchedResistThreshold) {
            DebugCommand.sendReactionFailed(entity, "scorched", "resistance",
                    entity.getDisplayName(), resistPoints, ElementalFireNatureReactionsConfig.scorchedResistThreshold);
            clearScorched(entity);
            return;
        }

        if (handleFrostbiteToWetness(entity, "scorched_frostbite_to_wetness")) {
            return;
        }

        if (entity.hasEffect(MobEffects.POISON)) {
            int enhancedDuration = (int) (ElementalFireNatureReactionsConfig.scorchedDuration
                    * ElementalFireNatureReactionsConfig.poisonScorchDurationMultiplier);
            ticks = enhancedDuration;
            data.putInt(NBT_SCORCHED_TICKS, ticks);
            data.putFloat(NBT_SCORCHED_DAMAGE_MULT, (float) ElementalFireNatureReactionsConfig.poisonScorchDamageMultiplier);
            data.putString(NBT_SCORCHED_DAMAGE_MULT_SRC, "poison");
            entity.removeEffect(MobEffects.POISON);
            if (entity.getRemainingFireTicks() < ticks) {
                entity.setRemainingFireTicks(ticks);
            }
        }

        data.putInt(NBT_SCORCHED_TICKS, ticks - 1);
        int fireStrength = data.getInt(NBT_SCORCHED_STRENGTH);
        ServerLevel level = (ServerLevel) entity.level();

        if (entity.isInWater()) {
            triggerThermalShock(entity, level, ticks, fireStrength);
            return;
        }

        if (entity.getRemainingFireTicks() < ticks) {
            entity.setRemainingFireTicks(ticks);
        }

        if (entity.tickCount % 20 == 0) {
            ScorchedDamageResult detail = calculateScorchedDamageDetailed(fireStrength, entity);
            ElementType entityElement = ElementUtils.getConsistentAttackElement(entity);
            float elementMult = getElementDurationMultiplier(entityElement);
            applyScorchedTickDamage(entity, detail.finalDamage, entityElement, elementMult, level, true, detail);
            igniteCreeperIfScorched(entity);

            int auraThreshold = ElementalFireNatureReactionsConfig.scorchedAuraFirePowerThreshold;
            int sourceFirePower = data.getInt(NBT_SCORCHED_SOURCE_FIRE_POWER);
            if (auraThreshold > 0 && sourceFirePower >= auraThreshold) {
                applyAuraDamage(entity, sourceFirePower, fireStrength, level);
            }
        }
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().getEffect() == ModMobEffects.WETNESS.get()) {
            if (isScorched(event.getEntity())) {
                event.setResult(Event.Result.DENY);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        if (!isScorched(entity)) return;
        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.IS_FIRE) && !source.is(ModDamageTypes.LAVA_MAGIC)) {
            if (ModMobEffects.SPORES.isPresent() && entity.hasEffect(ModMobEffects.SPORES.get())) {
                event.setAmount(0.001f);
            } else {
                event.setCanceled(true);
            }
        }
    }

    private static double findWaterSurfaceY(ServerLevel level, double x, double startY, double z) {
        int ix = (int) Math.floor(x);
        int iz = (int) Math.floor(z);
        int maxY = level.getMaxBuildHeight();
        for (int iy = (int) Math.floor(startY); iy < maxY; iy++) {
            if (!level.getFluidState(new BlockPos(ix, iy, iz)).is(FluidTags.WATER)) {
                return iy;
            }
        }
        return startY;
    }

    private static void triggerThermalShock(LivingEntity entity, ServerLevel level, int remainingTicks, int fireStrength) {
        double remainingSeconds = remainingTicks / 20.0;
        float dps = calculateScorchedDamage(fireStrength, entity);
        float totalRemainingDamage = (float) (remainingSeconds * dps);
        float ratio = (float) ElementalFireNatureReactionsConfig.scorchedShockDamageRatio;
        float shockDamage = totalRemainingDamage * ratio;

        if (shockDamage > 0.5f) {
            ElementDamageHelper.applyDamage(entity, shockDamage, ModDamageTypes.source(entity.level(), ModDamageTypes.LAVA_MAGIC));
        }

        int steamLevel = 0;
        if (ElementalFireNatureReactionsConfig.steamLowHeatMaxLevel > 0) {
            steamLevel = Math.max(1, Math.min(fireStrength / 20, ElementalFireNatureReactionsConfig.steamLowHeatMaxLevel));
            double surfaceY = findWaterSurfaceY(level, entity.getX(), entity.getY(), entity.getZ());
            SteamReactionHandler.spawnSteamCloud(level, entity.getX(), surfaceY, entity.getZ(), false, steamLevel);
            DebugCommand.sendSteamCloudCombinedLog(
                    entity, null, false, steamLevel,
                    0f, 1f,
                    (float) ElementalFireNatureReactionsConfig.steamCondensationRadius,
                    SteamReactionHandler.computeCloudDuration(false, steamLevel),
                    ElementalFireNatureReactionsConfig.steamCloudHeightCeiling, false,
                    ElementType.NONE, 1f, false, 0f);
        }

        DebugCommand.ThermalShockLogContext tsctx = new DebugCommand.ThermalShockLogContext();
        tsctx.target = entity;
        tsctx.remainingTicks = remainingTicks;
        tsctx.totalRemainingDamage = totalRemainingDamage;
        tsctx.ratio = ratio;
        tsctx.shockDamage = shockDamage;
        tsctx.steamLevel = steamLevel;
        DebugCommand.sendThermalShockLog(tsctx);

        BlockPos entityPos = entity.blockPosition();
        if (level.getFluidState(entityPos).is(FluidTags.WATER)) {
            level.setBlock(entityPos, Blocks.AIR.defaultBlockState(), 3);
        }

        clearScorched(entity);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f, 2.0f);
        level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 20, 0.5, 0.5, 0.5, 0.05);
    }

    private static void applyAuraDamage(LivingEntity source, int sourceFirePower, int fireStrength, ServerLevel level) {
        double radius = ElementalFireNatureReactionsConfig.scorchedAuraRadius;
        double sourceY = source.getY();
        AABB box = new AABB(
                source.getX() - radius, sourceY - 0.5, source.getZ() - radius,
                source.getX() + radius, sourceY + 1.5, source.getZ() + radius
        );
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != source && Math.abs(e.getY() - sourceY) < 1.0);
        if (nearby.isEmpty()) return;

        int resistThreshold = ElementalFireNatureReactionsConfig.scorchedResistThreshold;
        int tempTtl = 30;

        for (LivingEntity target : nearby) {
            if (target.isDeadOrDying()) continue;

            int resist = ElementUtils.getDisplayResistance(target, ElementType.FIRE);
            if (resist >= resistThreshold) continue;

            double dx = target.getX() - source.getX();
            double dz = target.getZ() - source.getZ();
            if (dx * dx + dz * dz > radius * radius) continue;

            CompoundTag targetData = target.getPersistentData();
            if (targetData.contains(NBT_SCORCHED_TICKS) && targetData.getInt(NBT_SCORCHED_TICKS) > 0) continue;
            if (targetData.getBoolean(NBT_FLEE_ACTIVE) && target instanceof Mob) continue;

            if (FrostbiteHandler.hasFrostbite(target) || FrostbiteHandler.isTempFrostbite(target)) {
                FrostbiteHandler.clearFrostbite(target);
                FrostbiteHandler.clearTempFrostbite(target);
                if (target instanceof Mob) MobAttributeLogic.processFlee(target, source, radius);
                continue;
            }
            applyTempScorch(target, fireStrength, tempTtl);
            igniteCreeperIfScorched(target);

            ScorchedDamageResult detail = calculateScorchedDamageDetailed(fireStrength, target);
            float auraDamage = detail.finalDamage;
            ElementType targetElement = ElementUtils.getConsistentAttackElement(target);
            float elementMult = getElementDurationMultiplier(targetElement);
            auraDamage *= elementMult;
            if (auraDamage > 0) {
                ElementDamageHelper.applyDamage(target, auraDamage, ModDamageTypes.source(level, ModDamageTypes.LAVA_MAGIC));
                if (source.getPersistentData().getInt(NBT_SCORCHED_AURA_LOGGED) == 0) {
                    float dmgMult = target.getPersistentData().getFloat(NBT_SCORCHED_DAMAGE_MULT);
                    if (dmgMult <= 0.0f) dmgMult = 1.0f;
                    String multSrc = target.getPersistentData().getString(NBT_SCORCHED_DAMAGE_MULT_SRC);
                    DebugCommand.sendScorchedAuraLog(source, target, detail.rawDamage, targetElement, elementMult, auraDamage, dmgMult, multSrc, detail.fireProtLevel, detail.genProtLevel, detail.enchReduction);
                    source.getPersistentData().putInt(NBT_SCORCHED_AURA_LOGGED, 1);
                }
                level.sendParticles(ISSCore.getFireParticle(), target.getX(), target.getY() + 0.1, target.getZ(), 5, 0.3, 0.1, 0.3, 0.01);
            }
            if (target instanceof Mob) MobAttributeLogic.processFlee(target, source, radius);
        }
    }

    static class ScorchedDamageResult {
        final float rawDamage;
        final float finalDamage;
        final int fireProtLevel;
        final int genProtLevel;
        final float enchReduction;

        ScorchedDamageResult(float rawDamage, float finalDamage, int fireProtLevel, int genProtLevel, float enchReduction) {
            this.rawDamage = rawDamage;
            this.finalDamage = finalDamage;
            this.fireProtLevel = fireProtLevel;
            this.genProtLevel = genProtLevel;
            this.enchReduction = enchReduction;
        }
    }

    static ScorchedDamageResult calculateScorchedDamageDetailed(int fireStrength, LivingEntity target) {
        int resistPoints = ElementUtils.getDisplayResistance(target, ElementType.FIRE);
        if (resistPoints >= ElementalFireNatureReactionsConfig.scorchedResistThreshold) {
            return new ScorchedDamageResult(0, 0, 0, 0, 0);
        }

        double base = ElementalFireNatureReactionsConfig.scorchedDamageBase;
        int step = Math.max(1, ElementalFireNatureReactionsConfig.scorchedDamageScalingStep);
        int threshold = ElementalFireNatureReactionsConfig.scorchedTriggerThreshold;
        double bonus = Math.max(0, (double)(fireStrength - threshold) / step * 0.5);
        double rawDamage = base + bonus;

        if (target.fireImmune()) {
            rawDamage *= ElementalFireNatureReactionsConfig.scorchedImmuneModifier;
        }

        int[] levels = getTotalEnchantmentLevels(target);
        int fireProtLevel = levels[0];
        int genProtLevel = levels[1];

        double denom = Math.max(1.0, ElementalFireNatureReactionsConfig.enchantmentCalculationDenominator);
        double fireProtReduction = (Math.min(fireProtLevel, denom) / denom) * ElementalFireNatureReactionsConfig.scorchedFireProtReduction;
        double genProtReduction = (Math.min(genProtLevel, denom) / denom) * ElementalFireNatureReactionsConfig.scorchedGenProtReduction;

        double finalDamage = rawDamage * (1.0 - fireProtReduction) * (1.0 - genProtReduction);

        float damageMult = target.getPersistentData().getFloat(NBT_SCORCHED_DAMAGE_MULT);
        if (damageMult <= 0.0f) damageMult = 1.0f;
        finalDamage *= damageMult;

        float enchRed = (float)(1.0 - (1.0 - fireProtReduction) * (1.0 - genProtReduction));
        return new ScorchedDamageResult((float) rawDamage, (float) finalDamage, fireProtLevel, genProtLevel, enchRed);
    }

    public static float calculateScorchedDamage(int fireStrength, LivingEntity target) {
        return calculateScorchedDamageDetailed(fireStrength, target).finalDamage;
    }

    private static float getElementDurationMultiplier(ElementType element) {
        if (element == ElementType.FIRE) return (float) ElementalFireNatureReactionsConfig.scorchedFireDurationMultiplier;
        if (element == ElementType.NATURE) return (float) ElementalFireNatureReactionsConfig.scorchedNatureDurationMultiplier;
        if (element == ElementType.THUNDER) return (float) ElementalFireNatureReactionsConfig.scorchedThunderDurationMultiplier;
        if (element == ElementType.FROST) return (float) ElementalFireNatureReactionsConfig.scorchedFrostDurationMultiplier;
        return 1.0f;
    }

    private static int[] getTotalEnchantmentLevels(LivingEntity entity) {
        int fireProt = 0, genProt = 0;
        for (ItemStack stack : entity.getArmorSlots()) {
            fireProt += stack.getEnchantmentLevel(Enchantments.FIRE_PROTECTION);
            genProt += stack.getEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION);
        }
        return new int[]{fireProt, genProt};
    }

    private static boolean handleFrostbiteToWetness(LivingEntity entity, String debugKey) {
        boolean hasReal = FrostbiteHandler.hasFrostbite(entity);
        boolean hasTemp = FrostbiteHandler.isTempFrostbite(entity);
        if ((!hasReal && !hasTemp) || FrostbiteHandler.isFrozen(entity)) return false;
        if (ElementalThunderFrostReactionsConfig.scorchedFrostbiteToWetnessRatio <= 0) return false;

        int frostbiteStacks = hasReal
                ? FrostbiteHandler.getFrostbiteStacks(entity)
                : entity.getPersistentData().getInt(FrostbiteHandler.NBT_TEMP_FROSTBITE_STACKS);
        int wetnessToAdd = frostbiteStacks * ElementalThunderFrostReactionsConfig.scorchedFrostbiteToWetnessRatio;
        if (wetnessToAdd > 0) {
            int maxWetness = ElementalFireNatureReactionsConfig.wetnessMaxLevel;
            if (maxWetness > 0) wetnessToAdd = Math.min(wetnessToAdd, maxWetness);
            WetnessHandler.updateWetnessLevel(entity, wetnessToAdd);
        }
        if (hasReal) {
            FrostbiteHandler.clearFrostbite(entity);
        } else {
            FrostbiteHandler.clearTempFrostbite(entity);
        }
        clearScorched(entity);
        entity.getPersistentData().putInt(NBT_WETNESS_STEAM_COOLDOWN, 20);
        DebugCommand.sendReactionSuccess(entity, debugKey,
                entity.getDisplayName(),
                Component.literal(String.valueOf(frostbiteStacks)),
                Component.literal(String.valueOf(wetnessToAdd)));
        return true;
    }

    private static void applyScorchedTickDamage(LivingEntity entity, float baseDamage, ElementType entityElement, float elementMult, ServerLevel level, boolean logTick) {
        applyScorchedTickDamage(entity, baseDamage, entityElement, elementMult, level, logTick, null);
    }

    private static void applyScorchedTickDamage(LivingEntity entity, float baseDamage, ElementType entityElement, float elementMult, ServerLevel level, boolean logTick, ScorchedDamageResult detail) {
        float damage = baseDamage * elementMult;
        if (damage <= 0) return;
        ElementDamageHelper.applyDamage(entity, damage, ModDamageTypes.source(level, ModDamageTypes.LAVA_MAGIC));
        level.sendParticles(ISSCore.getFireParticle(), entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 10, 0.2, 0.2, 0.2, 0.0);
        level.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 5, 0.2, 0.2, 0.2, 0.0);
        if (logTick) {
            CompoundTag data = entity.getPersistentData();
            if (data.getInt(NBT_SCORCHED_TICK_LOGGED) == 0) {
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.2f, 1.0f);
                float dmgMult = data.getFloat(NBT_SCORCHED_DAMAGE_MULT);
                if (dmgMult <= 0.0f) dmgMult = 1.0f;
                float rawBase = baseDamage / dmgMult;
                String multSrc = data.getString(NBT_SCORCHED_DAMAGE_MULT_SRC);
                if (detail != null) {
                    DebugCommand.sendScorchedTickLog(entity, detail.rawDamage, entityElement, elementMult, damage, dmgMult, multSrc, detail.fireProtLevel, detail.genProtLevel, detail.enchReduction);
                } else {
                    DebugCommand.sendScorchedTickLog(entity, rawBase, entityElement, elementMult, damage, dmgMult, multSrc, 0, 0, 0);
                }
                data.putInt(NBT_SCORCHED_TICK_LOGGED, 1);
            }
        }
    }

    public static final String NBT_FIRE_COUNTER_CD = "ec_fire_counter_cd";
    public static final String NBT_FIRE_COUNTER_INVULN = "ec_fire_counter_invuln";
    private static final String NBT_FIRE_COUNTER_LOCK = "ec_fire_counter_lock";
    private static final String NBT_FIRE_COUNTER_SAVED_SPEED = "ec_fire_counter_saved_speed";
    private static final String NBT_FIRE_COUNTER_SPEED_TIME = "ec_fire_counter_speed_time";

    private static final Map<ResourceKey<Level>, ActiveFireCounter> activeFireCounters = new HashMap<>();

    private enum FireCounterPhase { CONTRACT, EXPLODE }

    private static class ActiveFireCounter {
        final double x, y, z;
        final UUID ownerUUID;
        final double maxRadius;
        final double expansionSpeed;
        final double damage;
        final double knockback;
        final int fireStrength;
        final Set<UUID> affectedEntities;
        boolean collected;
        double currentRadius;
        int phaseTicks;
        FireCounterPhase phase;

        ActiveFireCounter(LivingEntity owner) {
            this.x = owner.getX();
            this.y = owner.getY();
            this.z = owner.getZ();
            this.ownerUUID = owner.getUUID();
            this.maxRadius = ElementalFireNatureReactionsConfig.fireCounterRadius;
            this.expansionSpeed = ElementalFireNatureReactionsConfig.fireCounterExpansionSpeed;
            this.damage = ElementalFireNatureReactionsConfig.fireCounterDamage;
            this.knockback = ElementalFireNatureReactionsConfig.fireCounterKnockback;
            this.fireStrength = ElementUtils.getDisplayEnhancement(owner, ElementType.FIRE);
            this.affectedEntities = new HashSet<>();
            this.collected = false;
            this.currentRadius = this.maxRadius;
            this.phaseTicks = 0;
            this.phase = FireCounterPhase.CONTRACT;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTryFireCounter(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getSource().is(ModDamageTypes.LAVA_MAGIC)) return;
        LivingEntity target = event.getEntity();
        int fireResist = ElementUtils.getDisplayResistance(target, ElementType.FIRE);
        if (fireResist >= ElementalFireNatureReactionsConfig.scorchedResistThreshold) return;
        double bloodThreshold = ElementalFireNatureReactionsConfig.fireCounterBloodThreshold;
        if (bloodThreshold <= 0) return;
        float currentHP = target.getHealth() + target.getAbsorptionAmount();
        if (currentHP - event.getAmount() >= target.getMaxHealth() * bloodThreshold) return;
        if (ElementUtils.getConsistentAttackElement(target) != ElementType.FIRE) {
            if (!(target instanceof Mob) || ElementUtils.getDisplayEnhancement(target, ElementType.FIRE) <= 0) return;
        }
        double firePower = ElementUtils.getDisplayEnhancement(target, ElementType.FIRE);
        if (target instanceof Mob && firePower <= 0) {
            int strikeLv = net.minecraft.world.item.enchantment.EnchantmentHelper.getTagEnchantmentLevel(
                    com.xulai.elementalcraft.enchantment.ModEnchantments.FIRE_STRIKE.get(), target.getMainHandItem());
            if (strikeLv > 0) {
                firePower = strikeLv * ElementalConfig.getStrengthPerLevel();
            }
        }
        double threshold = ElementalFireNatureReactionsConfig.fireCounterStrengthThreshold;
        if (threshold <= 0 || firePower < threshold) {
            if (threshold > 0) {
                DebugCommand.sendReactionFailed(target, "fire_counter", "power_low",
                        target.getDisplayName(),
                        String.format("%.0f", firePower),
                        String.valueOf((int) threshold));
            }
            return;
        }
        if (!ReactionHandler.checkHealthRecovery(target, NBT_FIRE_COUNTER_CD)) return;
        triggerFireCounter(target);
        ReactionHandler.setHealthRecoveryThreshold(target, NBT_FIRE_COUNTER_CD,
                target.getMaxHealth(), ElementalFireNatureReactionsConfig.fireCounterHealthRecoveryThreshold);
    }

    public static void triggerFireCounter(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel)) return;
        ResourceKey<Level> dim = target.level().dimension();
        if (activeFireCounters.containsKey(dim)) return;
        target.getPersistentData().putBoolean(NBT_FIRE_COUNTER_INVULN, true);
        if (target instanceof Mob mob) {
            mob.getNavigation().stop();
            var attr = mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            if (attr != null) {
                target.getPersistentData().putDouble(NBT_FIRE_COUNTER_SAVED_SPEED, attr.getBaseValue());
                attr.setBaseValue(0);
                target.getPersistentData().putLong(NBT_FIRE_COUNTER_SPEED_TIME, target.level().getGameTime());
            }
        }
        if (target instanceof net.minecraft.server.level.ServerPlayer sp) {
            ElementalCraft.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                    new FireCounterLockPacket(true));
        }
        activeFireCounters.put(dim, new ActiveFireCounter(target));
    }

    @SubscribeEvent
    public static void onLevelTickFireCounter(TickEvent.LevelTickEvent event) {
        if (event.level.isClientSide()) return;
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel sl)) return;

        ResourceKey<Level> dim = event.level.dimension();
        ActiveFireCounter fc = activeFireCounters.get(dim);
        if (fc == null) return;

        Entity ownerEntity = sl.getEntity(fc.ownerUUID);
        if (ownerEntity == null || !(ownerEntity instanceof LivingEntity owner) || owner.isDeadOrDying()) {
            if (ownerEntity instanceof Mob mob) {
                double savedSpeed = ownerEntity.getPersistentData().getDouble(NBT_FIRE_COUNTER_SAVED_SPEED);
                if (savedSpeed > 0) {
                    var attr = mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
                    if (attr != null) attr.setBaseValue(savedSpeed);
                }
                ownerEntity.getPersistentData().remove(NBT_FIRE_COUNTER_SAVED_SPEED);
                ownerEntity.getPersistentData().remove(NBT_FIRE_COUNTER_SPEED_TIME);
            }
            if (ownerEntity instanceof net.minecraft.server.level.ServerPlayer sp) {
                ElementalCraft.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                        new FireCounterLockPacket(false));
            }
            if (ownerEntity != null) {
                ownerEntity.getPersistentData().remove(NBT_FIRE_COUNTER_INVULN);
                ownerEntity.getPersistentData().remove(NBT_FIRE_COUNTER_LOCK);
            }
            activeFireCounters.remove(dim);
            return;
        }

        double rate = fc.expansionSpeed / 20.0;
        fc.phaseTicks++;

        switch (fc.phase) {
            case CONTRACT:
                if (!fc.collected) {
                    collectAndLock(sl, fc, owner);
                    fc.collected = true;
                }
                fc.currentRadius = Math.max(0, fc.maxRadius - fc.phaseTicks * rate);
                spawnFireRingParticles(sl, fc.x, fc.y, fc.z, fc.currentRadius);
                pullAndLock(sl, fc, owner);
                owner.setDeltaMovement(0, 0, 0);
                if (fc.currentRadius <= 0) {
                    fc.phase = FireCounterPhase.EXPLODE;
                    fc.phaseTicks = 0;
                }
                break;

            case EXPLODE:
                doExplosion(sl, fc, owner);
                owner.getPersistentData().remove(NBT_FIRE_COUNTER_INVULN);
                if (owner instanceof net.minecraft.server.level.ServerPlayer sp) {
                    ElementalCraft.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                            new FireCounterLockPacket(false));
                }
                if (owner instanceof Mob mob) {
                    double savedSpeed = owner.getPersistentData().getDouble(NBT_FIRE_COUNTER_SAVED_SPEED);
                    if (savedSpeed > 0) {
                        var attr = mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
                        if (attr != null) attr.setBaseValue(savedSpeed);
                    }
                    owner.getPersistentData().remove(NBT_FIRE_COUNTER_SAVED_SPEED);
                    owner.getPersistentData().remove(NBT_FIRE_COUNTER_SPEED_TIME);
                }
                activeFireCounters.remove(dim);
                break;
        }
    }

    private static void collectAndLock(ServerLevel level, ActiveFireCounter fc, LivingEntity owner) {
        AABB area = new AABB(
                fc.x - fc.maxRadius, fc.y - fc.maxRadius, fc.z - fc.maxRadius,
                fc.x + fc.maxRadius, fc.y + fc.maxRadius, fc.z + fc.maxRadius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (entity == owner) continue;
            if (entity instanceof Player p && p.isCreative()) continue;
            double dist = entity.distanceToSqr(fc.x, entity.getY(), fc.z);
            if (dist > fc.maxRadius * fc.maxRadius) continue;
            fc.affectedEntities.add(entity.getUUID());
            entity.setDeltaMovement(0, 0, 0);
        }
    }

    private static void pullAndLock(ServerLevel level, ActiveFireCounter fc, LivingEntity owner) {
        for (UUID uid : fc.affectedEntities) {
            Entity e = level.getEntity(uid);
            if (!(e instanceof LivingEntity entity) || entity.isDeadOrDying()) continue;
            entity.setDeltaMovement(0, 0, 0);
            Vec3 toCenter = new Vec3(fc.x - entity.getX(), 0, fc.z - entity.getZ());
            double dist = toCenter.length();
            if (dist > fc.currentRadius) continue;
            double pull = 0.5 * (1 - dist / Math.max(fc.currentRadius, 1));
            Vec3 pullVec = toCenter.normalize().scale(pull);
            entity.push(pullVec.x, pullVec.y, pullVec.z);
            entity.hurtMarked = true;
        }
    }

    private static void doExplosion(ServerLevel level, ActiveFireCounter fc, LivingEntity owner) {
        level.explode(owner, fc.x, fc.y, fc.z, 0f, Level.ExplosionInteraction.NONE);

        for (UUID uid : fc.affectedEntities) {
            Entity e = level.getEntity(uid);
            if (!(e instanceof LivingEntity entity) || entity.isDeadOrDying()) continue;
            Vec3 delta = entity.position().subtract(owner.position());
            if (delta.lengthSqr() < 1e-7) {
                delta = new Vec3(level.random.nextDouble() - 0.5, 0, level.random.nextDouble() - 0.5);
            }
            delta = delta.normalize();
            entity.push(delta.x * fc.knockback, 0.6, delta.z * fc.knockback);
            entity.hurtMarked = true;

            ElementDamageHelper.applyDamage(entity, (float) fc.damage,
                    ModDamageTypes.source(level, ModDamageTypes.LAVA_MAGIC));

            if (fc.fireStrength > 0) {
                ScorchedHandler.applyScorched(entity, owner, fc.fireStrength,
                        ElementalFireNatureReactionsConfig.scorchedDuration, fc.fireStrength, 1.0f, true);
            }

            level.sendParticles(ISSCore.getFireParticle(),
                    entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                    15, 0.5, 0.5, 0.5, 0.1);
        }

        spawnFireTornado(level, new Vec3(fc.x, fc.y, fc.z), fc.maxRadius, 0);

        DebugCommand.FireCounterLogContext ctx = new DebugCommand.FireCounterLogContext();
        ctx.owner = owner;
        ctx.radius = fc.maxRadius;
        ctx.affectedCount = fc.affectedEntities.size();
        ctx.damage = fc.damage;
        ctx.knockback = fc.knockback;
        DebugCommand.sendFireCounterLog(ctx);
    }

    private static void spawnFireRingParticles(ServerLevel level, double cx, double cy, double cz, double radius) {
        if (radius <= 0) return;
        int count = Math.max(8, (int) (radius * 12));
        double step = (Math.PI * 2) / count;
        for (int i = 0; i < count; i += 2) {
            double angle = step * i;
            double px = cx + Math.cos(angle) * radius;
            double pz = cz + Math.sin(angle) * radius;
            level.sendParticles(ISSCore.getFireParticle(), px, cy + 0.1, pz, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.SMOKE, px, cy + 0.1, pz, 1, 0, 0, 0, 0);
        }
    }

    private static void spawnFireTornado(ServerLevel level, Vec3 pos, double radius, int tick) {
        int expandTicks = 20;
        int maxTick = expandTicks + 10;
        if (tick > maxTick) return;
        double progress = Math.min((double) tick / expandTicks, 1.0);
        double currentR = 0.5 + (radius - 0.5) * progress;
        int totalHelices = 6;
        int layers = 6;
        double baseAngle = tick * 0.3;
        double coneH = Math.min(currentR * 1.5, 10.0);
        for (int h = 0; h < totalHelices; h++) {
            double helixAngle = baseAngle + (2 * Math.PI * h) / totalHelices;
            for (int p = 0; p < layers; p++) {
                double t = p / (double) (layers - 1);
                double y = 0.1 + t * coneH;
                double r = currentR * (0.3 + 1.78 * Math.pow(t - 0.556, 2));
                double x = pos.x + Math.cos(helixAngle) * r;
                double z = pos.z + Math.sin(helixAngle) * r;
                double vx = -Math.sin(helixAngle) * (0.15 + 0.1 * level.random.nextDouble())
                        + Math.cos(helixAngle) * (0.03 + 0.02 * level.random.nextDouble());
                double vz = Math.cos(helixAngle) * (0.15 + 0.1 * level.random.nextDouble())
                        + Math.sin(helixAngle) * (0.03 + 0.02 * level.random.nextDouble());
                double vy = tick <= expandTicks ? 0.03 + 0.02 * level.random.nextDouble() : 0;
                level.sendParticles(ISSCore.getFireParticle(), x, pos.y + y, z, 0, vx, vy, vz, 1.0);
                if (level.random.nextFloat() < 0.3f) {
                    level.sendParticles(ParticleTypes.SMOKE, x, pos.y + y, z, 0, vx * 0.5, vy, vz * 0.5, 1.0);
                }
            }
        }
        level.getServer().execute(() -> spawnFireTornado(level, pos, radius, tick + 1));
    }
}
