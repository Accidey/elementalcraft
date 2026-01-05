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
 * 负责管理游戏中的潮湿系统生态，包括生物的潮湿状态机维护、环境交互检测、属性修正计算以及相关副作用处理。
 * 核心功能：
 * 1. 状态管理：根据生物所处环境（水、雨、干燥）动态调整潮湿层数。
 * 2. 属性修正：根据潮湿层数提供对火属性的伤害减免，以及对雷/冰属性的易伤加成。
 * 3. 环境交互：检测热源（熔岩、岩浆块）实现瞬间干燥，以及在火中站立时的快速干燥。
 * 注意：岩浆块作为热源时，要求其周围 1 格范围内不能有水。
 * 4. 副作用：潮湿状态下自动灭火、玩家饱食度消耗增加。
 * <p>
 * English Description:
 * Core Wetness Handler Class.
 * Responsible for managing the wetness system ecosystem in the game, including maintaining the wetness state machine of mobs,
 * environmental interaction detection, attribute modification calculations, and handling related side effects.
 * Core Features:
 * 1. State Management: Dynamically adjusts wetness levels based on the entity's environment (Water, Rain, Dry).
 * 2. Attribute Modification: Provides damage reduction against Fire and increased vulnerability to Thunder/Frost based on wetness levels.
 * 3. Environmental Interaction: Detects heat sources (Lava, Magma Blocks) for instant drying, and rapid drying when standing in fire.
 * Note: Magma Blocks require no water within a 1-block radius to function as a heat source.
 * 4. Side Effects: Automatic fire extinguishing when wet, increased player food exhaustion.
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
     * 监听生物Tick事件。
     * 每秒执行一次核心潮湿逻辑（环境检测、状态更新）。
     * 同时处理实时的副作用，如潮湿状态下自动灭火，以及处于火焰方块中时的快速干燥逻辑。
     * <p>
     * Listens to LivingTickEvent.
     * Executes core wetness logic (environment detection, state update) once per second.
     * Also handles real-time side effects such as automatically extinguishing fire when wet,
     * and rapid drying logic when standing in fire blocks.
     *
     * @param event 生物 Tick 事件 / Living Tick event
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
     * 监听生物受伤事件。
     * 根据目标的潮湿等级动态调整受到的元素伤害。
     * 潮湿会提供对赤焰伤害的减免，但会增加受到的雷霆和冰霜伤害。
     * <p>
     * Listens to LivingHurtEvent.
     * Dynamically adjusts elemental damage received based on the target's wetness level.
     * Wetness provides reduction against Fire damage but increases vulnerability to Thunder and Frost damage.
     *
     * @param event 生物受伤事件 / Living Hurt event
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

                // 1. 赤焰伤害减免
                // 判定条件：原版火焰伤害标签 或 攻击者为赤焰属性
                // 1. Fire Damage Reduction
                // Condition: Vanilla Fire damage tag OR Attacker is Fire element
                if (source.is(DamageTypeTags.IS_FIRE) || attackerElement == ElementType.FIRE) {
                    float factor = (float) ElementalReactionConfig.wetnessFireReduction * currentLevel;
                    
                    // 使用配置上限
                    // Use configured cap
                    float maxReduction = (float) ElementalReactionConfig.steamMaxReduction;
                    factor = Math.min(maxReduction, factor);

                    finalDamage = originalDamage * (1.0f - factor);
                } 
                // 2. 雷霆、冰霜伤害加成
                // 2. Thunder/Frost Damage Amplification
                else {
                    float multiplier = 0.0f;

                    // 雷霆 (Thunder)
                    if (source.is(DamageTypeTags.IS_LIGHTNING) || attackerElement == ElementType.THUNDER) {
                        multiplier = (float) ElementalReactionConfig.wetnessResistModifier * currentLevel;
                    }
                    // 冰霜 (Frost)
                    else if (source.is(DamageTypeTags.IS_FREEZING) || attackerElement == ElementType.FROST) {
                        multiplier = (float) ElementalReactionConfig.wetnessResistModifier * currentLevel;
                    }

                    if (multiplier > 0) {
                        finalDamage = originalDamage * (1.0f + multiplier);
                    }
                }

                event.setAmount(finalDamage);
            }
        }
    }

    /**
     * 核心状态机逻辑。
     * 判断生物所处的环境（水中、雨中、干燥、热源旁）并相应地更新潮湿等级。
     * 处理层数的增加（在水/雨中）或自然衰减。检测到强热源（熔岩/岩浆块）时触发瞬间干燥。
     * <p>
     * Core State Machine Logic.
     * Determines the environment the entity is in (In Water, In Rain, Dry, Near Heat Source) and updates the wetness level accordingly.
     * Handles level accumulation (in water/rain) or natural decay. Triggers instant drying upon detecting strong heat sources (Lava/Magma Blocks).
     */
    private static void handleWetnessLogic(LivingEntity entity) {
        if (isImmune(entity)) {
            clearWetnessData(entity);
            return;
        }

        Level level = entity.level();
        BlockPos pos = entity.blockPosition();
        CompoundTag data = entity.getPersistentData();

        // 1. 热源检测：如果在熔岩中，或者靠近热源（熔岩2格/岩浆块1格且周围无水），或者在下界
        // 1. Heat Check: If in Lava, or near heat source (Lava 2 blocks / Dry Magma 1 block), or in Nether
        boolean inLava = entity.isInLava();
        boolean nearHeatSource = checkHeatSource(level, pos);
        boolean inNether = level.dimension() == Level.NETHER;

        if (inLava || nearHeatSource || (inNether && !ElementalReactionConfig.wetnessNetherDimensionImmune)) {
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
     * 热源检测逻辑。
     * 扫描生物周围的方块，查找熔岩或岩浆块。使用优化后的嵌套循环以降低性能开销。
     * 规则：
     * 1. 熔岩 (Lava)：水平半径 2 (5x5)，垂直范围 [-3, +3]。
     * 2. 岩浆块 (Magma Block)：水平半径 1 (3x3)，垂直范围 [-2, +2]。
     * 额外条件：岩浆块周围 1 格范围内（3x3x3 区域）不允许有水。
     * <p>
     * Heat Source Detection Logic.
     * Scans blocks around the entity for Lava or Magma Blocks. Uses optimized nested loops to reduce performance overhead.
     * Rules:
     * 1. Lava: Horiz Radius 2, Vert Range [-3, +3].
     * 2. Magma Block: Horiz Radius 1, Vert Range [-2, +2].
     * Extra Condition: No water allowed within 1 block radius (3x3x3 area) of the Magma Block.
     *
     * @param level 世界 / Level
     * @param center 中心坐标 (生物位置) / Center Pos
     * @return 是否存在有效热源 / True if valid heat source exists
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
     * 免疫性检查。
     * 根据配置文件判断生物是否免疫潮湿效果（如水生生物、位于下界、或在黑名单中）。
     * <p>
     * Immunity Check.
     * Determines if the entity is immune to wetness effects based on configuration (e.g., Water Animals, in Nether, or Blacklisted).
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
     * 清理数据。
     * 移除实体的所有潮湿相关NBT数据和药水效果。
     * <p>
     * Clear Data.
     * Removes all wetness-related NBT data and potion effects from the entity.
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
     * 同步药水效果。
     * 向实体应用无粒子的药水效果，用于在UI上直观显示当前的潮湿等级和持续时间。
     * <p>
     * Sync Potion Effect.
     * Applies a particle-free potion effect to the entity for visually displaying the current wetness level and duration on the UI.
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
     * 更新NBT数据。
     * 辅助方法，用于设置实体的潮湿等级。
     * <p>
     * Update NBT Data.
     * Helper method to set the entity's wetness level.
     */
    private static void updateWetnessLevel(LivingEntity entity, int level) {
        entity.getPersistentData().putInt(NBT_WETNESS, level);
    }

    /**
     * 饱食度惩罚逻辑。
     * 当玩家处于潮湿状态并消耗饱食度时，根据潮湿等级施加额外的饱食度消耗。
     * <p>
     * Food Exhaustion Penalty Logic.
     * Applies extra food exhaustion based on wetness level when a player consumes exhaustion while wet.
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
     * 监听投掷物撞击事件。
     * 当生物被药水投掷物（喷溅/滞留药水）击中时，增加固定的潮湿层数。
     * <p>
     * Listens to ProjectileImpactEvent.
     * Increases wetness level by a fixed amount when a living entity is hit by a potion projectile (Splash/Lingering Potion).
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