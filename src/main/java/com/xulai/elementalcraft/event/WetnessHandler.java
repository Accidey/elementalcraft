package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.event.FrostbiteHandler;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.util.RandomSource;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class WetnessHandler {
    public static final String NBT_WETNESS = "EC_WetnessLevel";
    public static final String NBT_RAIN_TIMER = "EC_WetnessRainTimer";
    public static final String NBT_DECAY_TIMER = "EC_WetnessDecayTimer";
    public static final String NBT_LAST_EXHAUSTION = "EC_LastExhaustion";
    public static final String NBT_FIRE_STAND_TIMER = "EC_WetnessFireStandTimer";
    public static final String NBT_REACTION_RESOLVED = "EC_ReactionResolved";

    private static final RandomSource RANDOM = RandomSource.create();
    private static final int PAUSED_DURATION_TICKS = 24000;
    private static boolean intentionalClear = false;
    private static boolean suppressRemoveCleanup = false;
    private static long lastHeatCheckGameTime = -20;
    private static int lastHeatCheckX = Integer.MIN_VALUE;
    private static int lastHeatCheckY = Integer.MIN_VALUE;
    private static int lastHeatCheckZ = Integer.MIN_VALUE;
    private static boolean lastHeatResult = false;

    public static int getWetnessLevel(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (data.contains(NBT_WETNESS)) {
            return data.getInt(NBT_WETNESS);
        }
        MobEffectInstance effect = entity.getEffect(ModMobEffects.WETNESS.get());
        if (effect != null) {
            return effect.getAmplifier() + 1;
        }
        return 0;
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        if (ScorchedHandler.isScorched(entity)) {
            return;
        }

        int wetnessLevel = getWetnessLevel(entity);
        if (wetnessLevel > 0 && entity.isOnFire() && entity.tickCount % 5 == 0) {
            entity.clearFire();
        }

        BlockPos pos = entity.blockPosition();
        boolean isSnowing = isSnowingHere(entity);

        BlockState state = entity.level().getBlockState(pos);
        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
            CompoundTag data = entity.getPersistentData();
            if (wetnessLevel > 0) {
                int timer = data.getInt(NBT_FIRE_STAND_TIMER) + 1;
                int threshold = ElementalFireNatureReactionsConfig.wetnessFireDryingTime * 20;
                if (timer >= threshold) {
                    clearWetnessData(entity);
                    entity.playSound(net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, 1.0f, 1.0f);
                    return;
                }
                data.putInt(NBT_FIRE_STAND_TIMER, timer);
            } else {
                data.remove(NBT_FIRE_STAND_TIMER);
            }
        } else {
            entity.getPersistentData().remove(NBT_FIRE_STAND_TIMER);
        }

        if (entity.tickCount % 20 == 0) {
            handleWetnessLogic(entity, isSnowing);
            handleExhaustion(entity);
            wetnessLevel = getWetnessLevel(entity);
        }

        spawnWetnessParticles(entity, isSnowing);

        if (wetnessLevel > 0 && entity.tickCount % 40 == 0) {
            if (!(entity.isInWater() || entity.level().isRainingAt(entity.blockPosition()) || isSnowing)) {
                entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), net.minecraft.sounds.SoundEvents.POINTED_DRIPSTONE_DRIP_WATER, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
            }
        }
    }

    private static void spawnWetnessParticles(LivingEntity entity, boolean isSnowing) {
        if (ScorchedHandler.isScorched(entity)) {
            return;
        }
        int wetnessLevel = getWetnessLevel(entity);
        if (wetnessLevel <= 0) return;
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;
        if (entity.isInWater() || entity.level().isRainingAt(entity.blockPosition()) || isSnowing) return;
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

    private static void handleWetnessLogic(LivingEntity entity, boolean isSnowing) {
        if (ScorchedHandler.isScorched(entity)) {
            if (getWetnessLevel(entity) > 0) {
                clearWetnessData(entity);
            }
            return;
        }
        if (isImmune(entity)) {
            clearWetnessData(entity);
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
                entity.playSound(net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, 1.0f, 1.0f);
            }
            return;
        }

        int currentLevel = getWetnessLevel(entity);
        int maxLevel = ElementalFireNatureReactionsConfig.wetnessMaxLevel;
        boolean inWater = entity.isInWater();
        boolean isRainingHere = level.isRainingAt(pos);
        boolean inPrecipitation = isRainingHere || isSnowing;
        boolean inCondensingCloud = SteamReactionHandler.isInCondensingCloud(entity);

        if (inWater) {
            int targetLevel;
            if (entity.isUnderWater()) {
                targetLevel = maxLevel;
            } else {
                int shallowCap = ElementalFireNatureReactionsConfig.wetnessShallowWaterCap;
                targetLevel = Math.min(shallowCap, maxLevel);
            }
            if (currentLevel < targetLevel && !blockWetnessIfParalyzed(entity) && !blockWetnessIfFrozen(entity)) {
                updateWetnessLevel(entity, targetLevel);
                currentLevel = data.getInt(NBT_WETNESS);
            }
            data.putInt(NBT_RAIN_TIMER, 0);
            data.putInt(NBT_DECAY_TIMER, 0);
        } else if (inPrecipitation) {
            data.putInt(NBT_DECAY_TIMER, 0);
            if (currentLevel < maxLevel) {
                int rainTimer = data.getInt(NBT_RAIN_TIMER) + 1;
                int requiredRainCount = Math.max(1, ElementalFireNatureReactionsConfig.wetnessRainGainInterval);
                if (rainTimer >= requiredRainCount && !blockWetnessIfParalyzed(entity) && !blockWetnessIfFrozen(entity)) {
                    updateWetnessLevel(entity, currentLevel + 1);
                    currentLevel = data.getInt(NBT_WETNESS);
                    data.putInt(NBT_RAIN_TIMER, 0);
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
                int requiredDecayCount = Math.max(1, currentLevel * ElementalFireNatureReactionsConfig.wetnessDecayBaseTime);
                if (decayTimer >= requiredDecayCount) {
                    currentLevel--;
                    updateWetnessLevel(entity, currentLevel);
                    data.putInt(NBT_DECAY_TIMER, 0);
                } else {
                    data.putInt(NBT_DECAY_TIMER, decayTimer);
                }
            }
        }
        if (currentLevel > 0 && !data.getBoolean(NBT_REACTION_RESOLVED)) {
            boolean hasStatic = data.getInt(StaticShockHandler.NBT_STATIC_STACKS) > 0;
            boolean hasFrostbite = FrostbiteHandler.hasFrostbite(entity) || FrostbiteHandler.isTempFrostbite(entity);
            if (hasStatic || hasFrostbite) {
                if (!(inWater && hasStatic)) {
                    resolveElementReactionConflict(entity, null);
                    data.putBoolean(NBT_REACTION_RESOLVED, true);
                }
                currentLevel = getWetnessLevel(entity);
            }
        }

        syncEffect(entity, currentLevel, inWater || inPrecipitation || inCondensingCloud);
    }

    private static boolean isSnowingHere(LivingEntity entity) {
        BlockPos pos = entity.blockPosition();
        Level level = entity.level();
        if (!level.isRaining() || !level.canSeeSky(pos)) return false;
        var biome = level.getBiome(pos).value();
        return biome != null && biome.getPrecipitationAt(pos) == Biome.Precipitation.SNOW;
    }

    static boolean checkHeatSource(Level level, BlockPos center) {
        long gt = level.getGameTime();
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();
        if (gt - lastHeatCheckGameTime < 20
                && cx == lastHeatCheckX && cy == lastHeatCheckY && cz == lastHeatCheckZ) {
            return lastHeatResult;
        }
        lastHeatCheckGameTime = gt;
        lastHeatCheckX = cx;
        lastHeatCheckY = cy;
        lastHeatCheckZ = cz;
        double configRadius = ElementalFireNatureReactionsConfig.wetnessHeatSearchRadius;
        int lavaRange = (int) Math.ceil(configRadius);
        int magmaRange = Math.max(1, lavaRange - 1);

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int x = -lavaRange; x <= lavaRange; x++) {
            for (int y = -lavaRange; y <= lavaRange; y++) {
                for (int z = -lavaRange; z <= lavaRange; z++) {
                    mutablePos.set(cx + x, cy + y, cz + z);
                    if (level.getFluidState(mutablePos).is(FluidTags.LAVA)) {
                        lastHeatResult = true;
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
                            lastHeatResult = true;
                            return true;
                        }
                    }
                }
            }
        }
        lastHeatResult = false;
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
        data.remove(NBT_WETNESS);
        data.remove(NBT_RAIN_TIMER);
        data.remove(NBT_DECAY_TIMER);
        data.remove(NBT_FIRE_STAND_TIMER);
        data.remove(NBT_LAST_EXHAUSTION);
        data.remove(NBT_REACTION_RESOLVED);
        data.remove("EC_WetnessParalysisLogged");
        data.remove("EC_WetnessFrozenLogged");
        if (entity.hasEffect(ModMobEffects.WETNESS.get())) {
            intentionalClear = true;
            try {
                entity.removeEffect(ModMobEffects.WETNESS.get());
            } finally {
                intentionalClear = false;
            }
        }
    }

    public static int convertWetnessToSpores(LivingEntity entity) {
        int wetnessLevel = getWetnessLevel(entity);
        if (wetnessLevel <= 0) return 0;

        if (!ReactionHandler.isSporeImmune(entity)) {
            MobEffectInstance currentSpores = entity.getEffect(ModMobEffects.SPORES.get());
            int currentStacks = (currentSpores != null) ? currentSpores.getAmplifier() + 1 : 0;
            if (currentStacks >= ElementalFireNatureReactionsConfig.sporeMaxStacks) {
                return 0;
            }
            ReactionHandler.stackSporeEffect(entity, wetnessLevel, null);
            clearWetnessData(entity);
            return wetnessLevel;
        }
        return 0;
    }

    private static void syncEffect(LivingEntity entity, int level, boolean isPaused) {
        if (level <= 0) {
            if (entity.hasEffect(ModMobEffects.WETNESS.get())) {
                entity.removeEffect(ModMobEffects.WETNESS.get());
            }
            return;
        }

        if (ModMobEffects.SPORES.isPresent() && entity.hasEffect(ModMobEffects.SPORES.get())) {
            boolean hasStatic = entity.getPersistentData().getInt(StaticShockHandler.NBT_STATIC_STACKS) > 0;
            boolean hasFrostbite = FrostbiteHandler.hasFrostbite(entity) || FrostbiteHandler.isTempFrostbite(entity);
            if (!hasStatic && !hasFrostbite) {
                convertWetnessToSpores(entity);
                return;
            }
        }

        int amplifier = level - 1;
        int baseTime = ElementalFireNatureReactionsConfig.wetnessDecayBaseTime;
        int durationTicks;
        if (isPaused) {
            durationTicks = PAUSED_DURATION_TICKS;
        } else {
            int decayTimer = entity.getPersistentData().getInt(NBT_DECAY_TIMER);
            int maxDurationSeconds = level * baseTime;
            int remainingSeconds = Math.max(0, maxDurationSeconds - decayTimer);
            durationTicks = remainingSeconds * 20 + 5;
        }

        if (durationTicks > 0) {
            MobEffectInstance existing = entity.getEffect(ModMobEffects.WETNESS.get());
            if (existing != null && existing.getAmplifier() == amplifier) {
                if (isPaused) {
                    if (existing.getDuration() >= durationTicks) return;
                } else {
                    if (existing.getDuration() == durationTicks) return;
                }
            }
            suppressRemoveCleanup = true;
            try {
                if (existing != null) {
                    entity.removeEffect(ModMobEffects.WETNESS.get());
                }
                entity.addEffect(new MobEffectInstance(
                        ModMobEffects.WETNESS.get(),
                        durationTicks,
                        amplifier,
                        true, false, true
                ));
            } finally {
                suppressRemoveCleanup = false;
            }
        }
    }

    public static void updateWetnessLevel(LivingEntity entity, int level) {
        if (ElementalFireNatureReactionsConfig.wetnessMaxLevel <= 0) return;
        entity.getPersistentData().putInt(NBT_WETNESS, level);
    }

    static boolean blockWetnessIfParalyzed(LivingEntity entity) {
        if (!entity.hasEffect(ModMobEffects.PARALYSIS.get())) return false;
        CompoundTag data = entity.getPersistentData();
        if (!data.getBoolean("EC_WetnessParalysisLogged")) {
            data.putBoolean("EC_WetnessParalysisLogged", true);
            DebugCommand.sendWetnessReactionFailed(entity, "paralysis", entity.getDisplayName());
        }
        return true;
    }

    static boolean blockWetnessIfFrozen(LivingEntity entity) {
        if (!FrostbiteHandler.isFrozen(entity)) return false;
        CompoundTag data = entity.getPersistentData();
        if (!data.getBoolean("EC_WetnessFrozenLogged")) {
            data.putBoolean("EC_WetnessFrozenLogged", true);
            DebugCommand.sendWetnessReactionFailed(entity, "freeze", entity.getDisplayName());
        }
        return true;
    }

    private static void handleExhaustion(LivingEntity entity) {
        if (entity instanceof Player player && !player.isCreative() && !player.isSpectator()) {
            CompoundTag data = player.getPersistentData();
            int currentLevel = getWetnessLevel(player);
            float currentExhaustion = player.getFoodData().getExhaustionLevel();
            float lastExhaustion = data.getFloat(NBT_LAST_EXHAUSTION);
            float delta;
            if (currentExhaustion >= lastExhaustion) {
                delta = currentExhaustion - lastExhaustion;
            } else {
                delta = currentExhaustion;
            }
            if (delta > 0.0001f) {
                float extra = 0;
                if (currentLevel > 0) {
                    extra = currentLevel * (float) ElementalFireNatureReactionsConfig.wetnessExhaustionIncrease;
                    player.getFoodData().addExhaustion(extra);
                    currentExhaustion = player.getFoodData().getExhaustionLevel();
                }
                if (extra > 0) {
                    DebugCommand.ExhaustionLogContext exCtx = new DebugCommand.ExhaustionLogContext();
                    exCtx.player = player;
                    exCtx.baseDelta = delta;
                    exCtx.wetnessDelta = extra;
                    exCtx.wetnessLevel = currentLevel;
                    DebugCommand.sendExhaustionLog(exCtx);
                }
            }
            data.putFloat(NBT_LAST_EXHAUSTION, currentExhaustion);
        }
    }

    public static void resolveElementReactionConflict(LivingEntity entity, LivingEntity attacker) {
        CompoundTag data = entity.getPersistentData();

        int staticStacks = data.getInt(StaticShockHandler.NBT_STATIC_STACKS);
        int frostbiteStacks = FrostbiteHandler.getFrostbiteStacks(entity);
        if (frostbiteStacks <= 0 && FrostbiteHandler.isTempFrostbite(entity)) {
            frostbiteStacks = data.getInt(FrostbiteHandler.NBT_TEMP_FROSTBITE_STACKS);
        }
        boolean hasSpores = ModMobEffects.SPORES.isPresent() && entity.hasEffect(ModMobEffects.SPORES.get());

        boolean hasStatic = staticStacks > 0;
        boolean hasFrostbite = frostbiteStacks > 0;

        if (!hasStatic && !hasFrostbite) return;

        int frostPower = attacker != null
                ? (int) ElementUtils.getDisplayEnhancement(attacker, ElementType.FROST)
                : data.getInt(FrostbiteHandler.NBT_FROSTBITE_SOURCE_FROST_POWER);

        if (hasStatic && !hasFrostbite) {
            applyStaticReaction(entity, attacker, hasSpores);
            return;
        }

        if (!hasStatic && hasFrostbite) {
            applyFrostReaction(entity, attacker, frostPower, hasSpores, false);
            return;
        }

        if (staticStacks > frostbiteStacks || (staticStacks == frostbiteStacks && RANDOM.nextBoolean())) {
            applyStaticReaction(entity, attacker, hasSpores);
        } else {
            applyFrostReaction(entity, attacker, frostPower, hasSpores, true);
        }
    }

    private static void applyStaticReaction(LivingEntity entity, LivingEntity attacker, boolean hasSpores) {
        StaticShockHandler.triggerParalysisReaction(attacker, entity);
        if (hasSpores) {
            StaticShockHandler.tryTriggerSporeBlast(entity);
        }
    }

    private static void applyFrostReaction(LivingEntity entity, LivingEntity attacker, int frostPower, boolean hasSpores, boolean clearFrostbite) {
        FrostbiteHandler.triggerFreeze(entity, attacker, frostPower);
        if (clearFrostbite) {
            FrostbiteHandler.clearFrostbite(entity);
        }
        if (hasSpores) {
            entity.removeEffect(ModMobEffects.SPORES.get());
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getRayTraceResult().getType() != HitResult.Type.ENTITY) return;
        Entity projectile = event.getProjectile();
        if (!(projectile instanceof ThrownPotion)) return;
        Entity target = ((EntityHitResult) event.getRayTraceResult()).getEntity();
        if (!(target instanceof LivingEntity livingTarget)) return;
        if (ScorchedHandler.isScorched(livingTarget)) {
            return;
        }
        if (isImmune(livingTarget)) return;
        if (blockWetnessIfParalyzed(livingTarget) || blockWetnessIfFrozen(livingTarget)) return;
        int add = ElementalFireNatureReactionsConfig.wetnessPotionAddLevel;
        int current = getWetnessLevel(livingTarget);
        int max = ElementalFireNatureReactionsConfig.wetnessMaxLevel;
        int newLevel = Math.min(max, current + add);
        CompoundTag data = livingTarget.getPersistentData();
        data.putInt(NBT_DECAY_TIMER, 0);
        data.remove(NBT_REACTION_RESOLVED);
        updateWetnessLevel(livingTarget, newLevel);
        syncEffect(livingTarget, getWetnessLevel(livingTarget), livingTarget.isInWater() || livingTarget.level().isRainingAt(livingTarget.blockPosition()) || isSnowingHere(livingTarget));
    }

    @SubscribeEvent
    public static void onMobEffectRemoved(net.minecraftforge.event.entity.living.MobEffectEvent.Remove event) {
        if (suppressRemoveCleanup) return;
        if (event.getEffect() != ModMobEffects.WETNESS.get()) return;
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        if (!intentionalClear) {
            CompoundTag data = entity.getPersistentData();
            data.remove(NBT_WETNESS);
            data.remove(NBT_RAIN_TIMER);
            data.remove(NBT_DECAY_TIMER);
            data.remove(NBT_FIRE_STAND_TIMER);
            data.remove(NBT_LAST_EXHAUSTION);
            data.remove(NBT_REACTION_RESOLVED);
            data.remove("EC_WetnessParalysisLogged");
            data.remove("EC_WetnessFrozenLogged");
        }
    }

    @SubscribeEvent
    public static void onMobEffectExpired(net.minecraftforge.event.entity.living.MobEffectEvent.Expired event) {
        if (event.getEffectInstance() == null || event.getEffectInstance().getEffect() != ModMobEffects.WETNESS.get()) return;
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        clearWetnessData(entity);
    }
}
