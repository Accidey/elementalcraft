// src/main/java/com/xulai/elementalcraft/event/WetnessHandler.java
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

/**
 * WetnessHandler
 * <p>
 * 中文说明：
 * 潮湿状态核心处理类。
 * 负责管理游戏中的潮湿系统生态，包括生物的潮湿状态机维护、环境交互检测以及相关副作用处理。
 * <p>
 * English Description:
 * Core Wetness Handler Class.
 * Responsible for managing the wetness system ecosystem, including maintaining the wetness state machine,
 * environmental interaction detection, and handling related side effects.
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class WetnessHandler {

    // NBT Keys
    public static final String NBT_WETNESS = "EC_WetnessLevel";
    public static final String NBT_RAIN_TIMER = "EC_WetnessRainTimer";
    public static final String NBT_DECAY_TIMER = "EC_WetnessDecayTimer";
    public static final String NBT_LAST_EXHAUSTION = "EC_LastExhaustion";
    public static final String NBT_FIRE_STAND_TIMER = "EC_WetnessFireStandTimer";

    /**
     * 生物每刻更新事件。
     * 处理实时副作用（如自动灭火、火中干燥）以及定期运行的潮湿状态逻辑（每秒一次）。
     * <p>
     * Living Entity Tick Event.
     * Handles real-time side effects (e.g., auto-extinguish, fire drying) and periodic wetness state logic (once per second).
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        // 实时灭火逻辑：只要有潮湿层数，就强制熄灭身上的火焰
        // Real-time extinguish logic: Extinguish fire immediately if wetness level exists
        if (entity.getPersistentData().getInt(NBT_WETNESS) > 0) {
            if (entity.isOnFire()) {
                entity.clearFire();
            }
        }

        // 火中干燥逻辑：检查生物是否处于燃烧的火焰方块中 (普通火/灵魂火)
        // Fire Drying Logic: Check if the mob is standing in burning fire blocks (Fire / Soul Fire)
        BlockPos pos = entity.blockPosition();
        BlockState state = entity.level().getBlockState(pos);
        
        if (state.is(Objects.requireNonNull(Blocks.FIRE)) || state.is(Objects.requireNonNull(Blocks.SOUL_FIRE))) {
            CompoundTag data = entity.getPersistentData();
            int timer = data.getInt(NBT_FIRE_STAND_TIMER) + 1;
            
            // 使用配置的时间阈值 (秒转Ticks)
            // Use configured time threshold (Seconds to Ticks)
            int threshold = ElementalReactionConfig.wetnessFireDryingTime * 20;

            if (timer >= threshold) {
                clearWetnessData(entity);
                entity.playSound(Objects.requireNonNull(net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH), 1.0f, 1.0f);
                timer = 0;
            }
            data.putInt(NBT_FIRE_STAND_TIMER, timer);
        } else {
            // 如果离开火焰，重置计时器
            // Reset timer if leaving fire
            if (entity.getPersistentData().contains(NBT_FIRE_STAND_TIMER)) {
                entity.getPersistentData().remove(NBT_FIRE_STAND_TIMER);
            }
        }

        // 每20 tick (1秒) 执行一次主要逻辑
        // Execute main logic every 20 ticks (1 second)
        if (entity.tickCount % 20 == 0) {
            handleWetnessLogic(entity);
            handleExhaustion(entity);
        }
    }

    /**
     * 生物受伤事件。
     * 根据目标的潮湿等级提供对赤焰（Fire）属性伤害的减免保护。
     * <p>
     * Living Hurt Event.
     * Provides damage reduction against Fire attribute damage based on the target's wetness level.
     */
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

                // 获取攻击者的元素属性
                // Get Attacker Element
                ElementType attackerElement = ElementType.NONE;
                if (source.getEntity() instanceof LivingEntity attacker) {
                    attackerElement = ElementUtils.getConsistentAttackElement(attacker);
                }

                // 赤焰伤害减免
                // 判定条件：原版火焰伤害标签 或 攻击者为赤焰属性
                // Fire Damage Reduction
                // Condition: Vanilla Fire damage tag OR Attacker is Fire element
                if (source.is(DamageTypeTags.IS_FIRE) || attackerElement == ElementType.FIRE) {
                    float factor = (float) ElementalReactionConfig.wetnessFireReduction * currentLevel;
                    
                    // 使用配置上限
                    // Use configured cap
                    float maxReduction = (float) ElementalReactionConfig.steamMaxReduction;
                    factor = Math.min(maxReduction, factor);

                    finalDamage = originalDamage * (1.0f - factor);
                    
                    event.setAmount(finalDamage);
                } 
            }
        }
    }

    /**
     * 核心潮湿状态逻辑。
     * 扫描环境（水、雨、雪、热源）并更新潮湿层数。
     * 包含增加层数（水源/降雨）和减少层数（干燥/自然衰减）的逻辑。
     * <p>
     * Core Wetness Logic.
     * Scans the environment (Water, Rain, Snow, Heat Source) and updates the wetness level.
     * Includes logic for increasing levels (Water/Precipitation) and decreasing levels (Dry/Natural Decay).
     */
    private static void handleWetnessLogic(LivingEntity entity) {
        // 第一道关卡：通用免疫检测（含配置的下界免疫）
        // 如果配置 nether_dimension_immune = false，此处将返回 false，允许后续逻辑执行
        // First check: General immunity check (includes configured Nether immunity)
        if (isImmune(entity)) {
            clearWetnessData(entity);
            return;
        }

        Level level = entity.level();
        BlockPos pos = entity.blockPosition();
        CompoundTag data = entity.getPersistentData();

        // 热源检测：如果在熔岩中，或者靠近热源（熔岩2格/岩浆块1格且周围无水）
        // Heat Check: If in Lava, or near heat source (Lava 2 blocks / Dry Magma 1 block)
        boolean inLava = entity.isInLava();
        boolean nearHeatSource = checkHeatSource(level, pos);

        // 如果处于极端热源环境，强制蒸发
        // Force evaporation if in extreme heat source environment
        if (inLava || nearHeatSource) {
            // 瞬间蒸发逻辑
            // Instant evaporation logic
            if (data.getInt(NBT_WETNESS) > 0) {
                clearWetnessData(entity);
                // 播放蒸发音效
                // Play evaporation sound
                entity.playSound(Objects.requireNonNull(net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH), 1.0f, 1.0f);
            }
            return; // 结束逻辑，不再进行后续积累或衰减
        }

        int currentLevel = data.getInt(NBT_WETNESS);
        int maxLevel = ElementalReactionConfig.wetnessMaxLevel;

        boolean inWater = entity.isInWater();
        boolean isRainingHere = level.isRainingAt(pos);

        boolean isSnowingHere = level.isRaining() && level.canSeeSky(pos)
                && Objects.requireNonNull(level.getBiome(pos).value()).getPrecipitationAt(pos) == Biome.Precipitation.SNOW;

        boolean inPrecipitation = isRainingHere || isSnowingHere;

        if (inWater) {
            // 在水中：检查水位深度决定最大潮湿等级
            // In Water: Check water depth to determine max wetness level
            @SuppressWarnings("deprecation")
            double fluidHeight = entity.getFluidHeight(FluidTags.WATER);
            double entityHeight = entity.getBbHeight();
            
            int targetLevel = maxLevel;
            
            // 浅水限制
            // Shallow water cap
            if (fluidHeight < entityHeight) {
                double ratio = ElementalReactionConfig.wetnessShallowWaterCapRatio;
                targetLevel = (int) Math.floor(maxLevel * ratio);
                targetLevel = Math.max(1, targetLevel);
            }

            if (currentLevel < targetLevel) {
                currentLevel = targetLevel;
                updateWetnessLevel(entity, currentLevel);
            }
            
            // 重置其他计时器
            // Reset other timers
            data.putInt(NBT_RAIN_TIMER, 0);
            data.putInt(NBT_DECAY_TIMER, 0);

        } else if (inPrecipitation) {
            // 在雨/雪中：缓慢积累
            // In Rain/Snow: Accumulate slowly
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
            // 干燥环境：自然衰减
            // Dry Environment: Natural decay
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

        // 同步状态到药水效果
        // Sync status to potion effect
        syncEffect(entity, currentLevel, inWater || inPrecipitation, data.getInt(NBT_DECAY_TIMER));
    }

    /**
     * 优化后的热源检测。
     * 检测周围的熔岩或干燥的岩浆块。
     * <p>
     * Optimized Heat Source Detection.
     * Detects nearby Lava or dry Magma Blocks.
     */
    private static boolean checkHeatSource(Level level, BlockPos center) {
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();
        
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos checkWaterPos = new BlockPos.MutableBlockPos();

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = -3; y <= 3; y++) {
                    
                    int absX = Math.abs(x);
                    int absY = Math.abs(y);
                    int absZ = Math.abs(z);
                    
                    boolean isLavaRange = absX <= 2 && absZ <= 2 && absY <= 3;
                    boolean isMagmaRange = absX <= 1 && absZ <= 1 && absY <= 2;
                    
                    if (!isLavaRange) continue;
                    
                    mutablePos.set(cx + x, cy + y, cz + z);
                    
                    // 1. 熔岩检测
                    // 1. Lava Check
                    if (level.getFluidState(mutablePos).is(Objects.requireNonNull(FluidTags.LAVA))) {
                        return true;
                    }
                    
                    // 2. 岩浆块检测
                    // 2. Magma Block Check
                    if (isMagmaRange) {
                         if (level.getBlockState(mutablePos).is(Objects.requireNonNull(Blocks.MAGMA_BLOCK))) {
                             // 检查岩浆块周围 1 格范围内（3x3x3 区域）是否有水
                             // Check if there is water within 1 block radius of the Magma Block
                             boolean hasWaterNearby = false;
                             
                             // 遍历 3x3x3 区域
                             // Iterate 3x3x3 area
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

                             // 只有周围没有水，才视为有效热源
                             // Only considered a valid heat source if there is no water nearby
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

    /**
     * 免疫检测。
     * 根据生物类型（水生生物）、维度（下界）或黑名单判断是否免疫潮湿。
     * <p>
     * Immunity Check.
     * Checks wetness immunity based on mob type (Water Animal), dimension (Nether), or blacklist.
     */
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

    /**
     * 清理所有潮湿数据。
     * 移除NBT标记和药水效果。
     * <p>
     * Clear all wetness data.
     * Removes NBT tags and potion effects.
     */
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

    /**
     * 同步并应用无粒子药水效果。
     * 用于在客户端UI上显示潮湿状态。
     * <p>
     * Sync and apply particle-free potion effect.
     * Used to display wetness status on the client UI.
     */
    private static void syncEffect(LivingEntity entity, int level, boolean isPaused, int decayTimer) {
        if (level <= 0) {
            if (entity.hasEffect(Objects.requireNonNull(ModMobEffects.WETNESS.get()))) {
                entity.removeEffect(ModMobEffects.WETNESS.get());
            }
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

    /**
     * 更新潮湿等级NBT。
     * <p>
     * Update Wetness Level NBT.
     */
    private static void updateWetnessLevel(LivingEntity entity, int level) {
        entity.getPersistentData().putInt(NBT_WETNESS, level);
    }

    /**
     * 饱食度消耗惩罚。
     * 潮湿状态下，玩家消耗饱食度时会额外增加消耗量。
     * <p>
     * Food Exhaustion Penalty.
     * Applies extra food exhaustion when a wet player consumes exhaustion.
     */
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

    /**
     * 投掷物撞击事件。
     * 检测是否被喷溅药水击中，并增加潮湿层数。
     * <p>
     * Projectile Impact Event.
     * Detects if hit by a splash potion and increases wetness level.
     */
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

            // 投掷物击中时立即同步一次效果
            // Sync effect immediately upon impact
            syncEffect(livingTarget, newLevel, livingTarget.isInWater() || livingTarget.level().isRainingAt(Objects.requireNonNull(livingTarget.blockPosition())), 0);
        }
    }
}