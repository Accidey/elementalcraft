package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalReactionConfig;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class WetnessHandler {

    public static final String NBT_WETNESS = "EC_WetnessLevel";
    public static final String NBT_RAIN_TIMER = "EC_WetnessRainTimer";
    public static final String NBT_DECAY_TIMER = "EC_WetnessDecayTimer";
    public static final String NBT_LAST_EXHAUSTION = "EC_LastExhaustion";
    public static final String NBT_FIRE_STAND_TIMER = "EC_WetnessFireStandTimer";

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        if (entity.getPersistentData().getInt(NBT_WETNESS) > 0) {
            if (entity.isOnFire()) {
                entity.clearFire();
            }
        }

        BlockPos pos = entity.blockPosition();
        BlockState state = entity.level().getBlockState(pos);
        
        if (state.is(Objects.requireNonNull(Blocks.FIRE)) || state.is(Objects.requireNonNull(Blocks.SOUL_FIRE))) {
            CompoundTag data = entity.getPersistentData();
            int timer = data.getInt(NBT_FIRE_STAND_TIMER) + 1;
            
            int threshold = ElementalReactionConfig.wetnessFireDryingTime * 20;

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

        if (entity.tickCount % ElementalReactionConfig.wetnessTickInterval == 0) {
            handleWetnessLogic(entity);
            handleExhaustion(entity);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        CompoundTag data = entity.getPersistentData();

        if (data.contains(NBT_WETNESS)) {
            int currentLevel = data.getInt(NBT_WETNESS);

            if (currentLevel > 0) {
                float originalDamage = event.getAmount();
                float finalDamage = originalDamage;
                DamageSource source = event.getSource();

                ElementType attackerElement = ElementType.NONE;
                if (source.getEntity() instanceof LivingEntity attacker) {
                    attackerElement = ElementUtils.getConsistentAttackElement(attacker);
                }

                if (source.is(DamageTypeTags.IS_FIRE) || attackerElement == ElementType.FIRE) {
                    float factor = (float) ElementalReactionConfig.wetnessFireReduction * currentLevel;
                    
                    float maxReduction = (float) ElementalReactionConfig.wetnessMaxReduction;
                    factor = Math.min(maxReduction, factor);

                    finalDamage = originalDamage * (1.0f - factor);
                    
                    event.setAmount(finalDamage);
                } 
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
            if (data.getInt(NBT_WETNESS) > 0) {
                clearWetnessData(entity);
                entity.playSound(Objects.requireNonNull(net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH), 1.0f, 1.0f);
            }
            return;
        }

        int currentLevel = data.getInt(NBT_WETNESS);
        int maxLevel = ElementalReactionConfig.wetnessMaxLevel;

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
                double ratio = ElementalReactionConfig.wetnessShallowWaterCapRatio;
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
                int thresholdSeconds = ElementalReactionConfig.wetnessRainGainInterval;

                if (rainTimer >= thresholdSeconds) {
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
                int durationForCurrentLevel = currentLevel * ElementalReactionConfig.wetnessDecayBaseTime;

                if (decayTimer >= durationForCurrentLevel) {
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
        
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos checkWaterPos = new BlockPos.MutableBlockPos();

        double configRadius = ElementalReactionConfig.wetnessHeatSearchRadius;
        
        int lavaRange = (int) Math.ceil(configRadius);
        
        int magmaRange = Math.max(1, lavaRange - 1);

        for (int x = -lavaRange; x <= lavaRange; x++) {
            for (int z = -lavaRange; z <= lavaRange; z++) {
                for (int y = -lavaRange; y <= lavaRange; y++) {
                    
                    int absX = Math.abs(x);
                    int absY = Math.abs(y);
                    int absZ = Math.abs(z);
                    
                    mutablePos.set(cx + x, cy + y, cz + z);
                    
                    if (absX <= lavaRange && absZ <= lavaRange && absY <= lavaRange) {
                        if (level.getFluidState(mutablePos).is(Objects.requireNonNull(FluidTags.LAVA))) {
                            return true;
                        }
                    }
                    
                    if (absX <= magmaRange && absZ <= magmaRange && absY <= magmaRange) {
                        if (level.getBlockState(mutablePos).is(Objects.requireNonNull(Blocks.MAGMA_BLOCK))) {
                             boolean hasWaterNearby = false;
                             
                             searchWater:
                             for (int dx = -1; dx <= 1; dx++) {
                                 for (int dy = -1; dy <= 1; dy++) {
                                     for (int dz = -1; dz <= 1; dz++) {
                                         checkWaterPos.set(mutablePos.getX() + dx, mutablePos.getY() + dy, mutablePos.getZ() + dz);
                                         if (level.getFluidState(checkWaterPos).is(Objects.requireNonNull(FluidTags.WATER))) {
                                             hasWaterNearby = true;
                                             break searchWater;
                                         }
                                     }
                                 }
                             }

                             if (!hasWaterNearby) {
                                 return true;
                             }
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean isImmune(LivingEntity entity) {
        if (ElementalReactionConfig.wetnessWaterAnimalImmune) {
            if (entity instanceof WaterAnimal) {
                return true;
            }
        }

        if (ElementalReactionConfig.wetnessNetherDimensionImmune) {
            if (entity.level().dimension() == Level.NETHER) {
                return true;
            }
        }

        if (!ElementalReactionConfig.cachedWetnessBlacklist.isEmpty()) {
            String entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
            if (ElementalReactionConfig.cachedWetnessBlacklist.contains(entityId)) {
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

        // 检查实体是否有易燃孢子效果
        if (ModMobEffects.SPORES.isPresent() && entity.hasEffect(Objects.requireNonNull(ModMobEffects.SPORES.get()))) {
            // 如果有易燃孢子效果，将潮湿层数转换为孢子层数增加
            // 计算要增加的孢子层数（潮湿层数）
            int sporesToAdd = level;
            
            // 调用ReactionHandler中的stackSporeEffect方法来增加孢子层数
            // 注意：这里需要传递null作为施加者，因为潮湿效果是环境效果
            ReactionHandler.stackSporeEffect(entity, sporesToAdd, null);
            
            // 清除潮湿数据，因为潮湿层数已经转换为孢子层数
            clearWetnessData(entity);
            return;
        }

        int amplifier = level - 1;
        int durationTicks;

        int baseTime = ElementalReactionConfig.wetnessDecayBaseTime;

        if (isPaused) {
            durationTicks = (level * baseTime) * 20;
        } else {
            int maxDurationSeconds = level * baseTime;
            int remainingSeconds = maxDurationSeconds - decayTimer;
            durationTicks = Math.max(0, remainingSeconds * 20);
        }

        if (durationTicks > 0) {
            entity.addEffect(new MobEffectInstance(
                    Objects.requireNonNull(ModMobEffects.WETNESS.get()),
                    durationTicks,
                    amplifier,
                    true,
                    true,
                    true
            ));
        }
    }

    private static void updateWetnessLevel(LivingEntity entity, int level) {
        entity.getPersistentData().putInt(NBT_WETNESS, level);
    }

    private static void handleExhaustion(LivingEntity entity) {
        if (entity instanceof Player player && !player.isCreative() && !player.isSpectator()) {
            CompoundTag data = player.getPersistentData();
            int currentLevel = data.getInt(NBT_WETNESS);

            float currentExhaustion = player.getFoodData().getExhaustionLevel();
            float lastExhaustion = data.getFloat(NBT_LAST_EXHAUSTION);

            if (currentExhaustion > lastExhaustion) {
                float delta = currentExhaustion - lastExhaustion;

                if (delta > 0.0001f) {
                    float extra = 0;
                    if (currentLevel > 0) {
                        extra = delta * currentLevel * (float) ElementalReactionConfig.wetnessExhaustionIncrease;
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

            int add = ElementalReactionConfig.wetnessPotionAddLevel;

            CompoundTag data = livingTarget.getPersistentData();
            int current = data.getInt(NBT_WETNESS);
            int max = ElementalReactionConfig.wetnessMaxLevel;

            int newLevel = Math.min(max, current + add);
            updateWetnessLevel(livingTarget, newLevel);

            data.putInt(NBT_DECAY_TIMER, 0);

            syncEffect(livingTarget, newLevel, livingTarget.isInWater() || livingTarget.level().isRainingAt(Objects.requireNonNull(livingTarget.blockPosition())), 0);
        }
    }
}