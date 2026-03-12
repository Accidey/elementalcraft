package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
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

        if (getWetnessLevel(entity) > 0) {
            if (entity.isOnFire()) {
                entity.clearFire();
            }
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
            if (entity.getPersistentData().contains(NBT_FIRE_STAND_TIMER)) {
                entity.getPersistentData().remove(NBT_FIRE_STAND_TIMER);
            }
        }

        if (entity.tickCount % ElementalFireNatureReactionsConfig.wetnessTickInterval == 0) {
            handleWetnessLogic(entity);
            handleExhaustion(entity);
        }

        spawnWetnessParticles(entity);

        int wetnessLevel = getWetnessLevel(entity);
        if (wetnessLevel > 0 && entity.tickCount % 40 == 0) {
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
            net.minecraft.sounds.SoundEvents.POINTED_DRIPSTONE_DRIP_WATER,
            net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    private static void spawnWetnessParticles(LivingEntity entity) {
        int wetnessLevel = getWetnessLevel(entity);
        if (wetnessLevel <= 0) return;
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

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

            serverLevel.sendParticles(ParticleTypes.FALLING_WATER,
                    x, y, z,
                    1, 0, 0, 0, 0);
        }

        if (entity.onGround() && entity.getDeltaMovement().horizontalDistanceSqr() > 0.01) {
            int splashCount = Math.max(1, baseCount / 2);
            double footY = entity.getY();
            for (int i = 0; i < splashCount; i++) {
                double offsetX = (RANDOM.nextDouble() - 0.5) * width;
                double offsetZ = (RANDOM.nextDouble() - 0.5) * width;

                double x = entity.getX() + offsetX;
                double z = entity.getZ() + offsetZ;

                serverLevel.sendParticles(ParticleTypes.SPLASH,
                        x, footY + 0.1, z,
                        1, 0, 0, 0, 0.02);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        int currentLevel = getWetnessLevel(entity);

        if (currentLevel > 0) {
            float originalDamage = event.getAmount();
            float finalDamage = originalDamage;
            DamageSource source = event.getSource();

            ElementType attackerElement = ElementType.NONE;
            if (source.getEntity() instanceof LivingEntity attacker) {
                attackerElement = ElementUtils.getConsistentAttackElement(attacker);
            }

            if (source.is(DamageTypeTags.IS_FIRE) || attackerElement == ElementType.FIRE) {
                float factor = (float) ElementalFireNatureReactionsConfig.wetnessFireReduction * currentLevel;
                
                float maxReduction = (float) ElementalFireNatureReactionsConfig.wetnessMaxReduction;
                factor = Math.min(maxReduction, factor);

                finalDamage = originalDamage * (1.0f - factor);
                
                event.setAmount(finalDamage);
            } 
        }
    }

    private static void handleWetnessLogic(LivingEntity entity) {
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
                entity.playSound(Objects.requireNonNull(net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH), 1.0f, 1.0f);
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

        if (inWater) {
            @SuppressWarnings("deprecation")
            double fluidHeight = entity.getFluidHeight(FluidTags.WATER);
            double entityHeight = entity.getBbHeight();
            
            int targetLevel = maxLevel;
            
            if (fluidHeight < entityHeight) {
                double ratio = ElementalFireNatureReactionsConfig.wetnessShallowWaterCapRatio;
                targetLevel = (int) Math.floor(maxLevel * ratio);
                targetLevel = Math.max(1, targetLevel);
            }

            if (currentLevel < targetLevel) {
                currentLevel = targetLevel;
                updateWetnessLevel(entity, currentLevel);
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
                } else {
                    data.putInt(NBT_RAIN_TIMER, rainTimer);
                }
            }
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
                } else {
                    data.putInt(NBT_DECAY_TIMER, decayTimer);
                }
            }
        }

        syncEffect(entity, currentLevel, inWater || inPrecipitation, data.getInt(NBT_DECAY_TIMER));
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
        if (ElementalFireNatureReactionsConfig.wetnessWaterAnimalImmune) {
            if (entity instanceof WaterAnimal) {
                return true;
            }
        }

        if (ElementalFireNatureReactionsConfig.wetnessNetherDimensionImmune) {
            if (entity.level().dimension() == Level.NETHER) {
                return true;
            }
        }

        if (!ElementalFireNatureReactionsConfig.cachedWetnessBlacklist.isEmpty()) {
            String entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
            if (ElementalFireNatureReactionsConfig.cachedWetnessBlacklist.contains(entityId)) {
                return true;
            }
        }

        return false;
    }

    private static void clearWetnessData(LivingEntity entity) {
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

    private static void syncEffect(LivingEntity entity, int level, boolean isPaused, int decayTimer) {
        if (level <= 0) {
            if (entity.hasEffect(Objects.requireNonNull(ModMobEffects.WETNESS.get()))) {
                entity.removeEffect(ModMobEffects.WETNESS.get());
            }
            return;
        }

        if (ModMobEffects.SPORES.isPresent() && entity.hasEffect(Objects.requireNonNull(ModMobEffects.SPORES.get()))) {
            int sporesToAdd = level;
            ReactionHandler.stackSporeEffect(entity, sporesToAdd, null);
            clearWetnessData(entity);
            return;
        }

        int amplifier = level - 1;
        int durationTicks;

        int baseTime = ElementalFireNatureReactionsConfig.wetnessDecayBaseTime;

        if (isPaused) {
            durationTicks = 24000;
        } else {
            int maxDurationSeconds = level * baseTime;
            int elapsedSeconds = (int) (decayTimer * ElementalFireNatureReactionsConfig.wetnessTickInterval / 20.0);
            int remainingSeconds = Math.max(0, maxDurationSeconds - elapsedSeconds);
            durationTicks = remainingSeconds * 20;
        }

        if (durationTicks > 0) {
            entity.addEffect(new MobEffectInstance(
                    Objects.requireNonNull(ModMobEffects.WETNESS.get()),
                    durationTicks,
                    amplifier,
                    true,
                    false,
                    true
            ));
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

        if (isImmune(livingTarget)) return;

        if (projectile instanceof ThrownPotion) {

            int add = ElementalFireNatureReactionsConfig.wetnessPotionAddLevel;

            int current = getWetnessLevel(livingTarget);
            int max = ElementalFireNatureReactionsConfig.wetnessMaxLevel;

            int newLevel = Math.min(max, current + add);
            updateWetnessLevel(livingTarget, newLevel);

            livingTarget.getPersistentData().putInt(NBT_DECAY_TIMER, 0);

            syncEffect(livingTarget, newLevel, livingTarget.isInWater() || livingTarget.level().isRainingAt(Objects.requireNonNull(livingTarget.blockPosition())), 0);
        }
    }
}