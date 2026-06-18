package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.logic.MobAttributeLogic;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.potion.ModMobEffects;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

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
    public static final String NBT_SCORCHED_AURA_LOGGED = "ec_scorched_aura_logged";
    public static final String NBT_TEMP_SCORCH = "ec_temp_scorch";
    public static final String NBT_TEMP_SCORCH_STRENGTH = "ec_temp_scorch_str";
    private static final String NBT_CREEPER_POISON_ENHANCED = "ec_creeper_poison_enhanced";
    private static final String NBT_CREEPER_ORIGINAL_MAX_SWELL = "ec_creeper_orig_max_swell";
    private static final int CREEPER_ACCELERATED_FUSE_TICKS = 10;
    private static final java.lang.reflect.Field MAX_SWELL_FIELD;
    private static final java.lang.reflect.Field SWELL_DIR_FIELD;

    static {
        java.lang.reflect.Field f1 = null;
        java.lang.reflect.Field f2 = null;
        try {
            f1 = Creeper.class.getDeclaredField("maxSwell");
            f1.setAccessible(true);
            f2 = Creeper.class.getDeclaredField("swellDir");
            f2.setAccessible(true);
        } catch (Exception ignored) {}
        MAX_SWELL_FIELD = f1;
        SWELL_DIR_FIELD = f2;
    }
    public static final String NBT_TEMP_SCORCH_TTL = "ec_temp_scorch_ttl";

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
        if (blacklist != null && blacklist.contains(entityId)) {
            return ScorchedApplyResult.FAILED;
        }
        CompoundTag targetData = target.getPersistentData();
        if (targetData.contains(NBT_SCORCHED_TICKS) && targetData.getInt(NBT_SCORCHED_TICKS) > 0) {
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
        if (attacker != null) {
            targetData.putUUID(NBT_SCORCHED_ATTACKER, attacker.getUUID());
        }

        if (attacker != null && !bypassCooldown) {
            CompoundTag attackerData = attacker.getPersistentData();
            attackerData.putLong(NBT_ATTACKER_SCORCHED_COOLDOWN, gameTime + adjustedDuration + ElementalFireNatureReactionsConfig.scorchedCooldown);
        }

        target.setRemainingFireTicks(adjustedDuration);

        if (target.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LAVA, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 20, 0.2, 0.2, 0.2, 0.0);
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
                int swellDir = SWELL_DIR_FIELD.getInt(creeper);
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
        net.minecraft.world.level.Explosion explosion = event.getExplosion();
        if (!(explosion.getExploder() instanceof Creeper creeper)) return;
        restoreMaxSwell(creeper);
        if (!creeper.getPersistentData().getBoolean(NBT_CREEPER_POISON_ENHANCED)) return;
        creeper.getPersistentData().remove(NBT_CREEPER_POISON_ENHANCED);
        event.setCanceled(true);
        float baseRadius = creeper.isPowered() ? 12.0f : 6.0f;
        creeperPoisonEnhancedActive = true;
        level.explode(creeper, creeper.getX(), creeper.getY(), creeper.getZ(),
                baseRadius, Level.ExplosionInteraction.MOB);
        creeperPoisonEnhancedActive = false;
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!creeperPoisonEnhancedActive) return;
        if (!(event.getLevel() instanceof ServerLevel)) return;
        net.minecraft.world.level.Explosion explosion = event.getExplosion();
        if (!(explosion.getExploder() instanceof Creeper creeper)) return;
        net.minecraft.world.phys.Vec3 pos = explosion.getPosition();
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
        entity.clearFire();
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
        target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), ttl));
    }

    public static void clearTempScorch(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        data.remove(NBT_TEMP_SCORCH);
        data.remove(NBT_TEMP_SCORCH_STRENGTH);
        data.remove(NBT_TEMP_SCORCH_TTL);
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
            if (data.getBoolean(NBT_TEMP_SCORCH)) {
                int ttl = data.getInt(NBT_TEMP_SCORCH_TTL) - 1;
                if (ttl <= 0) {
                    clearTempScorch(entity);
                    return;
                }
                data.putInt(NBT_TEMP_SCORCH_TTL, ttl);

                int tempResist = ElementUtils.getDisplayResistance(entity, ElementType.FIRE);
                if (tempResist >= ElementalFireNatureReactionsConfig.scorchedResistThreshold) {
                    clearTempScorch(entity);
                    return;
                }

                int tempStrength = data.getInt(NBT_TEMP_SCORCH_STRENGTH);

                if (entity.hasEffect(net.minecraft.world.effect.MobEffects.POISON)) {
                    float poisonMult = (float) ElementalFireNatureReactionsConfig.poisonScorchDamageMultiplier;
                    data.putFloat(NBT_SCORCHED_DAMAGE_MULT, poisonMult);
                    data.putString(NBT_SCORCHED_DAMAGE_MULT_SRC, "poison");
                    entity.removeEffect(net.minecraft.world.effect.MobEffects.POISON);
                    if (entity.getRemainingFireTicks() < ttl) {
                        entity.setRemainingFireTicks(ttl);
                    }
                }

                if (handleFrozenToSteam(entity, data, tempStrength, "temp_scorch_frozen_steam")) {
                    clearTempScorch(entity);
                    return;
                }

                handleFrostbiteToWetness(entity, "temp_scorch_frostbite_to_wetness");

                if (WetnessHandler.getWetnessLevel(entity) > 0
                        && ElementalFireNatureReactionsConfig.steamHighHeatMaxLevel > 0) {
                    int wetness = WetnessHandler.getWetnessLevel(entity);
                    int steamLevel = Math.max(1, Math.min(wetness, ElementalFireNatureReactionsConfig.steamHighHeatMaxLevel));
                    WetnessHandler.clearWetnessData(entity);
                    clearTempScorch(entity);
                    SteamReactionHandler.spawnSteamCloud(entity, true, steamLevel);
                    DebugCommand.sendReactionSuccess(entity, "temp_scorch_wet_steam",
                            entity.getDisplayName(),
                            Component.literal(String.valueOf(steamLevel)));
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

                int auraInterval = ElementalFireNatureReactionsConfig.scorchedAuraDamageInterval;
                if (auraInterval < 1) auraInterval = 20;
                if (entity.tickCount % auraInterval == 0) {
                    int fs = data.getInt(NBT_TEMP_SCORCH_STRENGTH);
                    float baseDamage = calculateScorchedDamage(fs, entity);
                    ElementType entityElement = ElementUtils.getConsistentAttackElement(entity);
                    float elementMult = getElementDurationMultiplier(entityElement);
                    applyScorchedTickDamage(entity, baseDamage, entityElement, elementMult, (ServerLevel) entity.level(), false);
                    igniteCreeperIfScorched(entity);
                }
            }
            return;
        }
        int ticks = data.getInt(NBT_SCORCHED_TICKS);

        if (ticks <= 0) {
            clearScorched(entity);
            return;
        }

        int resistPoints = ElementUtils.getDisplayResistance(entity, ElementType.FIRE);
        if (resistPoints >= ElementalFireNatureReactionsConfig.scorchedResistThreshold) {
            clearScorched(entity);
            return;
        }

        if (handleFrozenToSteam(entity, data, data.getInt(NBT_SCORCHED_SOURCE_FIRE_POWER), "scorch_frozen_steam")) {
            clearScorched(entity);
            return;
        }

        handleFrostbiteToWetness(entity, "scorched_frostbite_to_wetness");

        if (entity.hasEffect(net.minecraft.world.effect.MobEffects.POISON)) {
            int enhancedDuration = (int) (ElementalFireNatureReactionsConfig.scorchedDuration
                    * ElementalFireNatureReactionsConfig.poisonScorchDurationMultiplier);
            ticks = enhancedDuration;
            data.putInt(NBT_SCORCHED_TICKS, ticks);
            data.putFloat(NBT_SCORCHED_DAMAGE_MULT, (float) ElementalFireNatureReactionsConfig.poisonScorchDamageMultiplier);
            data.putString(NBT_SCORCHED_DAMAGE_MULT_SRC, "poison");
            entity.removeEffect(net.minecraft.world.effect.MobEffects.POISON);
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

        int auraInterval = ElementalFireNatureReactionsConfig.scorchedAuraDamageInterval;
        if (auraInterval < 1) auraInterval = 20;

        if (entity.tickCount % auraInterval == 0) {
            ScorchedDamageResult detail = calculateScorchedDamageDetailed(fireStrength, entity);
            ElementType entityElement = ElementUtils.getConsistentAttackElement(entity);
            float elementMult = getElementDurationMultiplier(entityElement);
            applyScorchedTickDamage(entity, detail.finalDamage, entityElement, elementMult, level, true, detail);
            igniteCreeperIfScorched(entity);
        }

        if (ElementalFireNatureReactionsConfig.scorchedAuraFirePowerThreshold > 0 && entity.tickCount % auraInterval == 0) {
            int sourceFirePower = data.getInt(NBT_SCORCHED_SOURCE_FIRE_POWER);
            int threshold = ElementalFireNatureReactionsConfig.scorchedAuraFirePowerThreshold;
            if (sourceFirePower >= threshold) {
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
            if (ModMobEffects.SPORES.isPresent() && ModMobEffects.SPORES.get() != null && entity.hasEffect(ModMobEffects.SPORES.get())) {
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
        java.util.List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != source && Math.abs(e.getY() - sourceY) < 1.0);
        if (nearby.isEmpty()) return;

        int resistThreshold = ElementalFireNatureReactionsConfig.scorchedResistThreshold;
        int auraInterval = ElementalFireNatureReactionsConfig.scorchedAuraDamageInterval;
        if (auraInterval < 1) auraInterval = 20;
        int tempTtl = auraInterval + 10;

        for (LivingEntity target : nearby) {
            if (target.isDeadOrDying()) continue;

            int resist = ElementUtils.getDisplayResistance(target, ElementType.FIRE);
            if (resist >= resistThreshold) continue;

            double dx = target.getX() - source.getX();
            double dz = target.getZ() - source.getZ();
            if (Math.sqrt(dx * dx + dz * dz) > radius) continue;

            if (isScorched(target)) continue;
            if (target.getPersistentData().getBoolean("EC_FleeActive")) continue;

            applyTempScorch(target, fireStrength, tempTtl);
            igniteCreeperIfScorched(target);

            float auraDamage = calculateScorchedDamage(fireStrength, target);
            ElementType targetElement = ElementUtils.getConsistentAttackElement(target);
            float elementMult = getElementDurationMultiplier(targetElement);
            auraDamage *= elementMult;
            if (auraDamage > 0) {
                ElementDamageHelper.applyDamage(target, auraDamage, ModDamageTypes.source(level, ModDamageTypes.LAVA_MAGIC));
                if (source.getPersistentData().getInt(NBT_SCORCHED_AURA_LOGGED) == 0) {
                    float dmgMult = target.getPersistentData().getFloat(NBT_SCORCHED_DAMAGE_MULT);
                    if (dmgMult <= 0.0f) dmgMult = 1.0f;
                    float rawBase = auraDamage / (elementMult * dmgMult);
                    String multSrc = target.getPersistentData().getString(NBT_SCORCHED_DAMAGE_MULT_SRC);
                    DebugCommand.sendScorchedAuraLog(source, target, rawBase, targetElement, elementMult, auraDamage, dmgMult, multSrc);
                    source.getPersistentData().putInt(NBT_SCORCHED_AURA_LOGGED, 1);
                }
                level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 0.1, target.getZ(), 5, 0.3, 0.1, 0.3, 0.01);
            }
            MobAttributeLogic.processFlee(target, source, radius);
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

        int fireProtLevel = 0;
        int genProtLevel = 0;
        for (ItemStack stack : target.getArmorSlots()) {
            fireProtLevel += stack.getEnchantmentLevel(Enchantments.FIRE_PROTECTION);
            genProtLevel += stack.getEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION);
        }

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

    static float calculateScorchedDamage(int fireStrength, LivingEntity target) {
        return calculateScorchedDamageDetailed(fireStrength, target).finalDamage;
    }

    private static float getElementDurationMultiplier(ElementType element) {
        if (element == ElementType.FIRE) return (float) ElementalFireNatureReactionsConfig.scorchedFireDurationMultiplier;
        if (element == ElementType.NATURE) return (float) ElementalFireNatureReactionsConfig.scorchedNatureDurationMultiplier;
        if (element == ElementType.THUNDER) return (float) ElementalFireNatureReactionsConfig.scorchedThunderDurationMultiplier;
        if (element == ElementType.FROST) return (float) ElementalFireNatureReactionsConfig.scorchedFrostDurationMultiplier;
        return 1.0f;
    }

    private static boolean handleFrozenToSteam(LivingEntity entity, CompoundTag data, int fireStrength, String debugKey) {
        if (!FrostbiteHandler.isFrozen(entity)) return false;
        if (!ElementalThunderFrostReactionsConfig.frostScorchSteamReactionEnabled) return false;
        if (ElementalFireNatureReactionsConfig.steamLowHeatMaxLevel <= 0) return false;

        int frozenStacks = data.getInt(FrostbiteHandler.NBT_FREEZE_STACKS);
        if (frozenStacks <= 0) frozenStacks = 1;
        int fireStep = 20;
        int steamLevel = Math.max(1, Math.min(fireStrength / fireStep + frozenStacks, ElementalFireNatureReactionsConfig.steamLowHeatMaxLevel));

        entity.removeEffect(ModMobEffects.FREEZE.get());
        data.remove(FrostbiteHandler.NBT_FREEZE_STACKS);
        data.remove(FrostbiteHandler.NBT_FROZEN_FROSTBITE_STACKS);
        data.putLong(FrostbiteHandler.NBT_FREEZE_COOLDOWN,
                entity.level().getGameTime() + ElementalThunderFrostReactionsConfig.freezeCooldownTicks);
        FrostbiteHandler.clearFrostbite(entity);

        if (!SteamReactionHandler.isOnSteamCooldown(entity)) {
            SteamReactionHandler.spawnSteamCloud(entity, false, steamLevel);
            SteamReactionHandler.applySteamCooldown(entity, SteamReactionHandler.computeCloudDuration(false, steamLevel));
        }
        DebugCommand.sendReactionSuccess(entity, debugKey,
                entity.getDisplayName(),
                Component.literal(String.valueOf(steamLevel)));
        return true;
    }

    private static boolean handleFrostbiteToWetness(LivingEntity entity, String debugKey) {
        if (!FrostbiteHandler.hasFrostbite(entity) || FrostbiteHandler.isFrozen(entity)) return false;
        if (!ElementalThunderFrostReactionsConfig.scorchedFrostbiteToWetnessEnabled) return false;
        if (ElementalThunderFrostReactionsConfig.scorchedFrostbiteToWetnessRatio <= 0) return false;

        int frostbiteStacks = FrostbiteHandler.getFrostbiteStacks(entity);
        int wetnessToAdd = frostbiteStacks * ElementalThunderFrostReactionsConfig.scorchedFrostbiteToWetnessRatio;
        if (wetnessToAdd > 0) {
            WetnessHandler.updateWetnessLevel(entity, wetnessToAdd);
        }
        FrostbiteHandler.clearFrostbite(entity);
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
        level.sendParticles(ParticleTypes.LAVA, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 10, 0.2, 0.2, 0.2, 0.0);
        level.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 5, 0.2, 0.2, 0.2, 0.0);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.2f, 1.0f);
        if (logTick) {
            CompoundTag data = entity.getPersistentData();
            if (data.getInt(NBT_SCORCHED_TICK_LOGGED) == 0) {
                float dmgMult = data.getFloat(NBT_SCORCHED_DAMAGE_MULT);
                if (dmgMult <= 0.0f) dmgMult = 1.0f;
                float rawBase = baseDamage / dmgMult;
                String multSrc = data.getString(NBT_SCORCHED_DAMAGE_MULT_SRC);
                if (detail != null) {
                    DebugCommand.sendScorchedTickLog(entity, detail.rawDamage / dmgMult, entityElement, elementMult, damage, dmgMult, multSrc, detail.fireProtLevel, detail.genProtLevel, detail.enchReduction);
                } else {
                    DebugCommand.sendScorchedTickLog(entity, rawBase, entityElement, elementMult, damage, dmgMult, multSrc, 0, 0, 0);
                }
                data.putInt(NBT_SCORCHED_TICK_LOGGED, 1);
            }
        }
    }
}
