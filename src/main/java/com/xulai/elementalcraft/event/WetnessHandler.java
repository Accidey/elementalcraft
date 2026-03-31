package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import com.xulai.elementalcraft.util.GlobalDebugLogger;
import com.xulai.elementalcraft.util.DebugMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;
import java.util.Random;

import com.xulai.elementalcraft.event.ScorchedHandler; // 导入灼烧处理器以使用其常量
import com.xulai.elementalcraft.event.SteamReactionHandler;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class WetnessHandler {

    public static final String NBT_WETNESS = "EC_WetnessLevel";
    public static final String NBT_RAIN_TIMER = "EC_WetnessRainTimer";
    public static final String NBT_DECAY_TIMER = "EC_WetnessDecayTimer";
    public static final String NBT_LAST_EXHAUSTION = "EC_LastExhaustion";
    public static final String NBT_FIRE_STAND_TIMER = "EC_WetnessFireStandTimer";

    private static final Random RANDOM = new Random();

    public static int getWetnessLevel(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (data.contains(NBT_WETNESS)) {
            return data.getInt(NBT_WETNESS);
        }
        MobEffectInstance effect = entity.getEffect(ModMobEffects.WETNESS.get());
        if (effect != null) {
            int level = effect.getAmplifier() + 1;
            data.putInt(NBT_WETNESS, level);
            return level;
        }
        return 0;
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        int wetnessBefore = getWetnessLevel(entity);

        if (wetnessBefore > 0 && entity.isOnFire()) {
            entity.clearFire();
        }

        BlockPos pos = entity.blockPosition();
        BlockState state = entity.level().getBlockState(pos);
        
        if (state.is(Objects.requireNonNull(Blocks.FIRE)) || state.is(Objects.requireNonNull(Blocks.SOUL_FIRE))) {
            CompoundTag data = entity.getPersistentData();
            int timer = data.getInt(NBT_FIRE_STAND_TIMER) + 1;
            int threshold = ElementalFireNatureReactionsConfig.wetnessFireDryingTime * 20;

            if (timer >= threshold) {
                clearWetnessData(entity);
                entity.playSound(Objects.requireNonNull(net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH), 1.0f, 1.0f);
                timer = 0;
            }
            data.putInt(NBT_FIRE_STAND_TIMER, timer);
        } else {
            entity.getPersistentData().remove(NBT_FIRE_STAND_TIMER);
        }

        if (entity.tickCount % ElementalFireNatureReactionsConfig.wetnessTickInterval == 0) {
            handleWetnessLogic(entity);
            handleExhaustion(entity);
        }

        spawnWetnessParticles(entity);

        int wetnessAfter = getWetnessLevel(entity);
        if (wetnessBefore != wetnessAfter) {
            Debug.logWetnessChange(entity, wetnessBefore, wetnessAfter);
        }

        int wetnessLevel = getWetnessLevel(entity);
        if (wetnessLevel > 0 && entity.tickCount % 40 == 0) {
            if (!(entity.isInWater() || entity.level().isRainingAt(entity.blockPosition()))) {
                entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        net.minecraft.sounds.SoundEvents.POINTED_DRIPSTONE_DRIP_WATER,
                        net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
            }
        }
    }

    private static void spawnWetnessParticles(LivingEntity entity) {
        int wetnessLevel = getWetnessLevel(entity);
        if (wetnessLevel <= 0) return;
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;
        if (entity.isInWater() || entity.level().isRainingAt(entity.blockPosition())) return;
        if (entity.tickCount % 10 != 0) return;

        double width = entity.getBbWidth();
        double height = entity.getBbHeight();
        int baseCount = Math.max(1, wetnessLevel);

        int dripCount = Math.max(1, baseCount / 2);
        for (int i = 0; i < dripCount; i++) {
            double offsetX = (RANDOM.nextDouble() - 0.5) * width * 1.5;
            double offsetY = RANDOM.nextDouble() * height;
            double offsetZ = (RANDOM.nextDouble() - 0.5) * width * 1.5;
            double x = entity.getX() + offsetX;
            double y = entity.getY() + offsetY;
            double z = entity.getZ() + offsetZ;
            serverLevel.sendParticles(ParticleTypes.FALLING_WATER, x, y, z, 1, 0, 0, 0, 0);
        }

        if (entity.onGround() && entity.getDeltaMovement().horizontalDistanceSqr() > 0.01) {
            int splashCount = Math.max(1, baseCount / 2);
            double footY = entity.getY();
            for (int i = 0; i < splashCount; i++) {
                double offsetX = (RANDOM.nextDouble() - 0.5) * width;
                double offsetZ = (RANDOM.nextDouble() - 0.5) * width;
                double x = entity.getX() + offsetX;
                double z = entity.getZ() + offsetZ;
                serverLevel.sendParticles(ParticleTypes.SPLASH, x, footY + 0.1, z, 1, 0, 0, 0, 0.02);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        int currentLevel = getWetnessLevel(entity);
        if (currentLevel <= 0) return;

        float originalDamage = event.getAmount();
        DamageSource source = event.getSource();
        ElementType attackerElement = ElementType.NONE;
        if (source.getEntity() instanceof LivingEntity attacker) {
            attackerElement = ElementUtils.getConsistentAttackElement(attacker);
        }

        if (source.is(DamageTypeTags.IS_FIRE) || attackerElement == ElementType.FIRE) {
            float factor = (float) ElementalFireNatureReactionsConfig.wetnessFireReduction * currentLevel;
            float maxReduction = (float) ElementalFireNatureReactionsConfig.wetnessMaxReduction;
            factor = Math.min(maxReduction, factor);
            // 实际伤害减免计算尚未实现，此处仅记录日志
            Debug.logFireDamageReduction(entity, currentLevel, factor, originalDamage, originalDamage);
        }
    }

    private static void handleWetnessLogic(LivingEntity entity) {
        // 如果实体处于灼烧状态，直接清除潮湿数据并返回
        if (entity.getPersistentData().contains(ScorchedHandler.NBT_SCORCHED_TICKS)) {
            clearWetnessData(entity);
            return;
        }

        if (isImmune(entity)) {
            clearWetnessData(entity);
            //Debug.logImmuneCleared(entity);
            return;
        }

        Level level = entity.level();
        BlockPos pos = entity.blockPosition();
        CompoundTag data = entity.getPersistentData();

        boolean inLava = entity.isInLava();
        boolean nearHeatSource = checkHeatSource(level, pos);

        if (inLava || nearHeatSource) {
            if (getWetnessLevel(entity) > 0) {
                clearWetnessData(entity);
                entity.playSound(Objects.requireNonNull(net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH), 1.0f, 1.0f);
                Debug.logHeatCleared(entity, inLava, nearHeatSource);
            }
            return;
        }

        int currentLevel = getWetnessLevel(entity);
        int maxLevel = ElementalFireNatureReactionsConfig.wetnessMaxLevel;

        boolean inWater = entity.isInWater();
        boolean isRainingHere = level.isRainingAt(pos);
        boolean isSnowingHere = level.isRaining() && level.canSeeSky(pos)
                && Objects.requireNonNull(level.getBiome(pos).value()).getPrecipitationAt(pos) == Biome.Precipitation.SNOW;
        boolean inPrecipitation = isRainingHere || isSnowingHere;

        boolean inCondensingCloud = SteamReactionHandler.isInCondensingCloud(entity);

        if (inWater) {
            @SuppressWarnings("deprecation")
            double fluidHeight = entity.getFluidHeight(FluidTags.WATER);
            double entityHeight = entity.getBbHeight();
            int targetLevel;
            if (entity.isUnderWater()) {
                targetLevel = maxLevel;
            } else {
                double ratio = ElementalFireNatureReactionsConfig.wetnessShallowWaterCapRatio;
                targetLevel = (int) Math.floor(maxLevel * ratio);
                targetLevel = Math.max(1, targetLevel);
            }
            if (currentLevel < targetLevel) {
                currentLevel = targetLevel;
                updateWetnessLevel(entity, currentLevel);
                Debug.logInWater(entity, fluidHeight, entityHeight, targetLevel, currentLevel);
            }
            data.putInt(NBT_RAIN_TIMER, 0);
            data.putInt(NBT_DECAY_TIMER, 0);

        } else if (inPrecipitation) {
            data.putInt(NBT_DECAY_TIMER, 0);
            if (currentLevel < maxLevel) {
                int rainTimer = data.getInt(NBT_RAIN_TIMER) + 1;
                int rainGainIntervalTicks = ElementalFireNatureReactionsConfig.wetnessRainGainInterval * 20;
                int requiredRainCount = (int) Math.ceil((double) rainGainIntervalTicks / ElementalFireNatureReactionsConfig.wetnessTickInterval);
                if (rainTimer >= requiredRainCount) {
                    currentLevel++;
                    updateWetnessLevel(entity, currentLevel);
                    data.putInt(NBT_RAIN_TIMER, 0);
                    Debug.logRainGain(entity, currentLevel, rainTimer, requiredRainCount);
                } else {
                    data.putInt(NBT_RAIN_TIMER, rainTimer);
                }
            }

        } else if (inCondensingCloud) {
            data.putInt(NBT_RAIN_TIMER, 0);
            data.putInt(NBT_DECAY_TIMER, 0);

        } else {
            data.putInt(NBT_RAIN_TIMER, 0);
            if (currentLevel > 0) {
                int decayTimer = data.getInt(NBT_DECAY_TIMER) + 1;
                int maxDurationTicks = currentLevel * ElementalFireNatureReactionsConfig.wetnessDecayBaseTime * 20;
                int requiredDecayCount = (int) Math.ceil((double) maxDurationTicks / ElementalFireNatureReactionsConfig.wetnessTickInterval);
                if (decayTimer >= requiredDecayCount) {
                    currentLevel--;
                    updateWetnessLevel(entity, currentLevel);
                    data.putInt(NBT_DECAY_TIMER, 0);
                    Debug.logDecayStep(entity, currentLevel+1, currentLevel, decayTimer, requiredDecayCount);
                } else {
                    data.putInt(NBT_DECAY_TIMER, decayTimer);
                }
            }
        }

        syncEffect(entity, currentLevel, inWater || inPrecipitation || inCondensingCloud);
    }

    private static boolean checkHeatSource(Level level, BlockPos center) {
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();
        double configRadius = ElementalFireNatureReactionsConfig.wetnessHeatSearchRadius;
        int lavaRange = (int) Math.ceil(configRadius);
        int magmaRange = Math.max(1, lavaRange - 1);
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int x = -lavaRange; x <= lavaRange; x++) {
            for (int y = -lavaRange; y <= lavaRange; y++) {
                for (int z = -lavaRange; z <= lavaRange; z++) {
                    mutablePos.set(cx + x, cy + y, cz + z);
                    if (level.getFluidState(mutablePos).is(FluidTags.LAVA)) {
                        return true;
                    }
                }
            }
        }

        for (int x = -magmaRange; x <= magmaRange; x++) {
            for (int y = -magmaRange; y <= magmaRange; y++) {
                for (int z = -magmaRange; z <= magmaRange; z++) {
                    mutablePos.set(cx + x, cy + y, cz + z);
                    if (level.getBlockState(mutablePos).is(Blocks.MAGMA_BLOCK)) {
                        boolean hasWaterNearby = false;
                        for (int dir = 0; dir < 6; dir++) {
                            BlockPos neighbor = mutablePos.relative(Direction.values()[dir]);
                            if (level.getFluidState(neighbor).is(FluidTags.WATER)) {
                                hasWaterNearby = true;
                                break;
                            }
                        }
                        if (!hasWaterNearby) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean isImmune(LivingEntity entity) {
        if (ElementalFireNatureReactionsConfig.wetnessWaterAnimalImmune && entity instanceof WaterAnimal) {
            return true;
        }
        if (ElementalFireNatureReactionsConfig.wetnessNetherDimensionImmune && entity.level().dimension() == Level.NETHER) {
            return true;
        }
        if (!ElementalFireNatureReactionsConfig.cachedWetnessBlacklist.isEmpty()) {
            var key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            if (key != null && ElementalFireNatureReactionsConfig.cachedWetnessBlacklist.contains(key.toString())) {
                return true;
            }
        }
        return false;
    }

    public static void clearWetnessData(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (data.contains(NBT_WETNESS)) {
            data.remove(NBT_WETNESS);
            data.remove(NBT_RAIN_TIMER);
            data.remove(NBT_DECAY_TIMER);
        }
        if (entity.hasEffect(Objects.requireNonNull(ModMobEffects.WETNESS.get()))) {
            entity.removeEffect(ModMobEffects.WETNESS.get());
        }
    }

    private static void syncEffect(LivingEntity entity, int level, boolean isPaused) {
        if (level <= 0) {
            if (entity.hasEffect(ModMobEffects.WETNESS.get())) {
                entity.removeEffect(ModMobEffects.WETNESS.get());
            }
            return;
        }

        if (ModMobEffects.SPORES.isPresent() && entity.hasEffect(ModMobEffects.SPORES.get())) {
            ReactionHandler.stackSporeEffect(entity, level, null);
            clearWetnessData(entity);
            Debug.logConvertToSpores(entity, level);
            return;
        }

        int amplifier = level - 1;
        int baseTime = ElementalFireNatureReactionsConfig.wetnessDecayBaseTime;
        int durationTicks;

        if (isPaused) {
            durationTicks = 24000;
        } else {
            int decayTimer = entity.getPersistentData().getInt(NBT_DECAY_TIMER);
            int maxDurationSeconds = level * baseTime;
            int elapsedSeconds = (int) (decayTimer * ElementalFireNatureReactionsConfig.wetnessTickInterval / 20.0);
            int remainingSeconds = Math.max(0, maxDurationSeconds - elapsedSeconds);
            durationTicks = remainingSeconds * 20;
        }

        if (durationTicks > 0) {
            if (entity.hasEffect(ModMobEffects.WETNESS.get())) {
                entity.removeEffect(ModMobEffects.WETNESS.get());
            }
            entity.addEffect(new MobEffectInstance(
                    Objects.requireNonNull(ModMobEffects.WETNESS.get()),
                    durationTicks,
                    amplifier,
                    true,
                    false,
                    true
            ));
            Debug.logEffectApplied(entity, level, isPaused, durationTicks);
        }
    }

    public static void updateWetnessLevel(LivingEntity entity, int level) {
        entity.getPersistentData().putInt(NBT_WETNESS, level);
    }

    private static void handleExhaustion(LivingEntity entity) {
        if (entity instanceof Player player && !player.isCreative() && !player.isSpectator()) {
            CompoundTag data = player.getPersistentData();
            int currentLevel = getWetnessLevel(player);
            float currentExhaustion = player.getFoodData().getExhaustionLevel();
            float lastExhaustion = data.getFloat(NBT_LAST_EXHAUSTION);

            if (currentExhaustion > lastExhaustion) {
                float delta = currentExhaustion - lastExhaustion;
                if (delta > 0.0001f) {
                    float extra = 0;
                    if (currentLevel > 0) {
                        extra = delta * currentLevel * (float) ElementalFireNatureReactionsConfig.wetnessExhaustionIncrease;
                        player.getFoodData().addExhaustion(extra);
                        currentExhaustion += extra;
                    }
                    if (extra > 0) {
                        DebugCommand.sendExhaustionLog(player, delta, extra, currentLevel);
                    }
                }
            }
            data.putFloat(NBT_LAST_EXHAUSTION, currentExhaustion);
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getRayTraceResult().getType() != HitResult.Type.ENTITY) return;
        Entity projectile = event.getProjectile();
        Entity target = ((EntityHitResult) event.getRayTraceResult()).getEntity();
        if (!(target instanceof LivingEntity livingTarget)) return;

        // 如果目标处于灼烧状态，直接返回，不增加潮湿等级
        if (livingTarget.getPersistentData().contains(ScorchedHandler.NBT_SCORCHED_TICKS)) {
            return;
        }

        if (isImmune(livingTarget)) return;

        if (projectile instanceof ThrownPotion) {
            int add = ElementalFireNatureReactionsConfig.wetnessPotionAddLevel;
            int current = getWetnessLevel(livingTarget);
            int max = ElementalFireNatureReactionsConfig.wetnessMaxLevel;
            int newLevel = Math.min(max, current + add);
            updateWetnessLevel(livingTarget, newLevel);
            livingTarget.getPersistentData().putInt(NBT_DECAY_TIMER, 0);
            syncEffect(livingTarget, newLevel, livingTarget.isInWater() || livingTarget.level().isRainingAt(livingTarget.blockPosition()));
            Debug.logPotionImpact(livingTarget, current, newLevel, add);
        }
    }

    // ==================== 调试内部类（所有调试代码集中于此，便于删除） ====================
    private static final class Debug {
        private static void logWetnessChange(LivingEntity entity, int before, int after) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(entity.level(), "潮湿", String.format("%s 潮湿变化：%d → %d", entity.getName().getString(), before, after));
        }

        private static void logFireDamageReduction(LivingEntity entity, int level, float factor, float original, float finalDamage) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(entity.level(), "潮湿", String.format("%s 火焰减免：潮湿 %d，减免 %.1f%%，伤害 %.2f → %.2f",
                    entity.getName().getString(), level, factor * 100, original, finalDamage));
        }

       /* private static void logImmuneCleared(LivingEntity entity) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(entity.level(), "潮湿", String.format("%s 免疫潮湿，已清除", entity.getName().getString()));
        }*/

        private static void logHeatCleared(LivingEntity entity, boolean inLava, boolean nearHeat) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(entity.level(), "潮湿", String.format("%s 因热源清除潮湿（熔岩:%s，近热:%s）", entity.getName().getString(), inLava, nearHeat));
        }

        private static void logInWater(LivingEntity entity, double fluidHeight, double entityHeight, int targetLevel, int currentLevel) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(entity.level(), "潮湿", String.format("%s 入水：液高 %.2f/%.2f，目标等级 %d，当前 %d",
                    entity.getName().getString(), fluidHeight, entityHeight, targetLevel, currentLevel));
        }

        private static void logRainGain(LivingEntity entity, int newLevel, int timer, int required) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(entity.level(), "潮湿", String.format("%s 雨中积累：计时器 %d/%d，潮湿升至 %d",
                    entity.getName().getString(), timer, required, newLevel));
        }

        private static void logDecayStep(LivingEntity entity, int oldLevel, int newLevel, int timer, int required) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(entity.level(), "潮湿", String.format("%s 衰减：%d → %d，计时器 %d/%d",
                    entity.getName().getString(), oldLevel, newLevel, timer, required));
        }

        private static void logConvertToSpores(LivingEntity entity, int level) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(entity.level(), "潮湿", String.format("%s 潮湿 %d 层转化为孢子", entity.getName().getString(), level));
        }

        private static void logEffectApplied(LivingEntity entity, int level, boolean isPaused, int duration) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(entity.level(), "潮湿", String.format("%s 应用效果：等级 %d，暂停 %s，持续 %d 刻",
                    entity.getName().getString(), level, isPaused, duration));
        }

        private static void logPotionImpact(LivingEntity target, int current, int newLevel, int add) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "潮湿", String.format("%s 被喷溅药水击中：潮湿 %d +%d → %d",
                    target.getName().getString(), current, add, newLevel));
        }
    }
}