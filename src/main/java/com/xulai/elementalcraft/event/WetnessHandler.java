// src/main/java/com/xulai/elementalcraft/event/WetnessHandler.java
package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalReactionConfig;
import com.xulai.elementalcraft.potion.ModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * WetnessHandler
 * <p>
 * 中文说明：
 * 负责处理潮湿系统的核心逻辑。
 * 主要功能包括：
 * 1. 免疫检查：在执行逻辑前检查生物是否属于水生生物、是否在下界或在黑名单中。
 * 2. 状态机逻辑：处理在水中（湿透）、雨中（积累）、干燥处（衰减）的状态切换。
 * 3. 属性抗性修正：根据潮湿层数增加受到的雷/冰伤害，减少受到的火伤害。
 * 4. 饱食度惩罚：潮湿状态下会额外消耗少量饱食度。
 * <p>
 * English Description:
 * Handles the core logic for the Wetness System.
 * Main features include:
 * 1. Immunity Check: Checks if the mob is a water animal, in the Nether, or blacklisted before processing.
 * 2. State Machine: Handles state transitions between In-Water (Soaked), In-Rain (Accumulation), and Dry (Decay).
 * 3. Resistance Modification: Increases Thunder/Frost damage taken and decreases Fire damage taken based on wetness level.
 * 4. Exhaustion Penalty: Consumes extra food exhaustion when wet.
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class WetnessHandler {

    // NBT 键名 / NBT Keys
    public static final String NBT_WETNESS = "EC_WetnessLevel";
    public static final String NBT_RAIN_TIMER = "EC_WetnessRainTimer";   // 积累计时器 / Accumulation Timer
    public static final String NBT_DECAY_TIMER = "EC_WetnessDecayTimer"; // 衰减计时器 / Decay Timer
    public static final String NBT_LAST_EXHAUSTION = "EC_LastExhaustion";

    /**
     * 监听生物 Tick 事件。
     * 每秒（20 ticks）执行一次环境检测、潮湿层数更新和药水效果同步。
     * <p>
     * Listens to Living Tick events.
     * Executes environment detection, wetness level updates, and potion effect synchronization every second (20 ticks).
     *
     * @param event 生物 Tick 事件 / Living tick event
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        // 每20 tick (1秒) 执行一次核心逻辑，减少性能消耗
        // Execute core logic every 20 ticks (1 second) to reduce performance cost
        if (entity.tickCount % 20 == 0) {
            handleWetnessLogic(entity);
            handleExhaustion(entity);
        }
    }

    /**
     * 监听生物受伤事件。
     * 根据目标的潮湿层数，调整受到的属性伤害（雷/冰增伤，火减伤）。
     * <p>
     * Listens to Living Hurt events.
     * Adjusts elemental damage taken (Thunder/Frost bonus, Fire reduction) based on the target's wetness level.
     *
     * @param event 生物受伤事件 / Living hurt event
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();

        // 检查实体是否有潮湿数据
        // Check if entity has wetness data
        CompoundTag data = entity.getPersistentData();
        if (data.contains(NBT_WETNESS)) {
            int currentLevel = data.getInt(NBT_WETNESS);

            if (currentLevel > 0) {
                float originalDamage = event.getAmount();
                float finalDamage = originalDamage;

                // 1. 赤焰伤害 (Fire Damage)
                // ▲ 赤焰抗性：每层 +X% (受到伤害减少)
                // ▲ Flame Resistance: +X% per level (Damage Reduced)
                if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
                    float factor = (float) ElementalReactionConfig.wetnessFireReduction * currentLevel;
                    // 限制最高 100% (免疫)
                    // Cap at 100% (Immunity)
                    factor = Math.min(1.0f, factor);

                    // 减少伤害 / Reduce damage
                    finalDamage = originalDamage * (1.0f - factor);
                }
                // 2. 雷霆或冰霜伤害 (Thunder or Frost Damage)
                // ▼ 雷霆/冰霜抗性：每层 -X% (受到伤害增加)
                // ▼ Thunder/Frost Resistance: -X% per level (Damage Increased)
                else if (event.getSource().is(DamageTypeTags.IS_LIGHTNING) || event.getSource().is(DamageTypeTags.IS_FREEZING)) {
                    float factor = (float) ElementalReactionConfig.wetnessResistModifier * currentLevel;

                    // 增加伤害 / Increase damage
                    finalDamage = originalDamage * (1.0f + factor);
                }
                event.setAmount(finalDamage);
            }
        }
    }

    /**
     * 处理潮湿积累与衰减的核心状态机逻辑。
     * 包含环境检测（雨/雪/水）和相应的计时器处理。
     * <p>
     * Handles the core state machine logic for wetness accumulation and decay.
     * Includes environment detection (Rain/Snow/Water) and corresponding timer processing.
     *
     * @param entity 目标实体 / Target entity
     */
    private static void handleWetnessLogic(LivingEntity entity) {
        // =======================================================================
        // 0. 免疫检查 (Immunity Check)
        // =======================================================================
        if (isImmune(entity)) {
            // 如果该生物免疫，但身上有残留的潮湿数据，立即清除
            // If immune but has residual wetness data, clear immediately
            clearWetnessData(entity);
            return; // 只要免疫，后续逻辑完全不执行 / Stop execution if immune
        }

        Level level = entity.level();
        BlockPos pos = entity.blockPosition();
        CompoundTag data = entity.getPersistentData();

        int currentLevel = data.getInt(NBT_WETNESS);
        int maxLevel = ElementalReactionConfig.wetnessMaxLevel;

        // -----------------------------------------------------------------------
        // 1. 环境检测 (Environment Detection)
        // -----------------------------------------------------------------------

        // 检测是否在水中
        // Check if in water
        boolean inWater = entity.isInWater();

        // 检测该位置是否正在下雨（排除干燥群系）
        // Check if it is raining at the position (excludes dry biomes)
        boolean isRainingHere = level.isRainingAt(pos);

        // 检测是否在下雪：全局下雨 + 能看见天空 + 群系降水类型是雪
        // Check if snowing: Global Rain + Can see sky + Biome precipitation is SNOW
        boolean isSnowingHere = level.isRaining() && level.canSeeSky(pos)
                && level.getBiome(pos).value().getPrecipitationAt(pos) == Biome.Precipitation.SNOW;

        // 是否处于降水状态（雨 或 雪）
        // Is in precipitation (Rain OR Snow)
        boolean inPrecipitation = isRainingHere || isSnowingHere;

        // -----------------------------------------------------------------------
        // 2. 状态机逻辑 (State Machine Logic)
        // -----------------------------------------------------------------------

        if (inWater) {
            // === 状态：在水中 (In Water) ===
            // 中文：在水中时，倒计时不开始（重置为0）。只要没满级，就强制设为满级（模拟湿透）。
            // English: In water, countdown does not start (reset to 0). Force max level if not already max (simulate soaking).

            if (currentLevel < maxLevel) {
                currentLevel = maxLevel;
                updateWetnessLevel(entity, currentLevel);
            }

            // 重置所有计时器：只要在水中，就不会开始衰减
            // Reset all timers: As long as in water, decay does not start
            data.putInt(NBT_RAIN_TIMER, 0);
            data.putInt(NBT_DECAY_TIMER, 0);

        } else if (inPrecipitation) {
            // === 状态：在雨/雪中 (In Rain/Snow) ===
            // 中文：积累阶段。倒计时不开始。达到时间阈值后层数 +1。
            // English: Accumulation phase. Countdown does not start. +1 Level when threshold reached.

            data.putInt(NBT_DECAY_TIMER, 0); // 只要在雨中，衰减倒计时就不开始 / Decay timer doesn't start in rain

            if (currentLevel < maxLevel) {
                int rainTimer = data.getInt(NBT_RAIN_TIMER) + 1; // +1s

                int thresholdSeconds = ElementalReactionConfig.wetnessRainGainInterval;

                if (rainTimer >= thresholdSeconds) {
                    currentLevel++;
                    updateWetnessLevel(entity, currentLevel);
                    data.putInt(NBT_RAIN_TIMER, 0); // 清除上一层的积累，开始下一层的倒计时 / Clear accumulation, start next count
                } else {
                    data.putInt(NBT_RAIN_TIMER, rainTimer);
                }
            }
        } else {
            // === 状态：干燥/离开雨水 (Dry/Left Water) ===
            // 中文：衰减阶段。倒计时开始。倒计时结束后层数 -1。
            // English: Decay phase. Countdown starts. -1 Level when countdown finishes.

            data.putInt(NBT_RAIN_TIMER, 0); // 离开雨中，积累进度丢失 / Accumulation progress lost

            if (currentLevel > 0) {
                int decayTimer = data.getInt(NBT_DECAY_TIMER) + 1; // +1s

                // 计算当前层数应该持续的总时间
                // Calculate total duration for current level
                int durationForCurrentLevel = currentLevel * ElementalReactionConfig.wetnessDecayBaseTime;

                if (decayTimer >= durationForCurrentLevel) {
                    // 倒计时结束，降级
                    // Countdown finished, downgrade
                    currentLevel--;
                    updateWetnessLevel(entity, currentLevel);

                    // 重置计时器，准备开始下一层的倒计时
                    // Reset timer, ready to start countdown for the next level
                    data.putInt(NBT_DECAY_TIMER, 0);
                } else {
                    // 还在当前层数的倒计时中
                    // Still in countdown for current level
                    data.putInt(NBT_DECAY_TIMER, decayTimer);
                }
            }
        }

        // -----------------------------------------------------------------------
        // 3. 视觉效果同步 (Visual Effect Sync)
        // -----------------------------------------------------------------------
        // 负责根据 NBT 数据赋予实际的药水效果，用于显示图标和时间
        // Responsible for applying actual potion effects based on NBT data for icon and time display

        syncEffect(entity, currentLevel, inWater || inPrecipitation, data.getInt(NBT_DECAY_TIMER));
    }

    /**
     * 检查实体是否对潮湿效果免疫。
     * 基于配置检查：水生生物、下界维度、黑名单。
     * <p>
     * Checks if the entity is immune to the wetness effect.
     * Checks based on config: Water animals, Nether dimension, Blacklist.
     *
     * @param entity 目标实体 / Target Entity
     * @return true 如果免疫 / true if immune
     */
    private static boolean isImmune(LivingEntity entity) {
        // 1. 水生生物免疫 (Water Animal Immunity)
        if (ElementalReactionConfig.wetnessWaterAnimalImmune) {
            if (entity instanceof WaterAnimal) {
                return true;
            }
        }

        // 2. 下界维度免疫 (Nether Dimension Immunity)
        if (ElementalReactionConfig.wetnessNetherDimensionImmune) {
            if (entity.level().dimension() == Level.NETHER) {
                return true;
            }
        }

        // 3. 黑名单检查 (Blacklist Check)
        if (!ElementalReactionConfig.cachedWetnessBlacklist.isEmpty()) {
            String entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
            if (ElementalReactionConfig.cachedWetnessBlacklist.contains(entityId)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 清除实体的潮湿数据和药水效果。
     * <p>
     * Clears wetness data and potion effects from the entity.
     *
     * @param entity 目标实体 / Target entity
     */
    private static void clearWetnessData(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        // 优化：仅在存在键时执行移除
        // Optimization: Only remove if key exists
        if (data.contains(NBT_WETNESS)) {
            data.remove(NBT_WETNESS);
            data.remove(NBT_RAIN_TIMER);
            data.remove(NBT_DECAY_TIMER);
        }

        if (entity.hasEffect(ModMobEffects.WETNESS.get())) {
            entity.removeEffect(ModMobEffects.WETNESS.get());
        }
    }

    /**
     * 同步药水效果到实体，用于 UI 显示。
     * <p>
     * Syncs potion effect to the entity for UI display.
     *
     * @param entity     目标实体 / Target entity
     * @param level      当前潮湿等级 / Current wetness level
     * @param isPaused   是否处于暂停倒计时状态（雨中/水中） / Is countdown paused (Rain/Water)
     * @param decayTimer 当前衰减计时器(秒) / Current decay timer (seconds)
     */
    private static void syncEffect(LivingEntity entity, int level, boolean isPaused, int decayTimer) {
        if (level <= 0) {
            // 移除效果
            // Remove effect
            if (entity.hasEffect(ModMobEffects.WETNESS.get())) {
                entity.removeEffect(ModMobEffects.WETNESS.get());
            }
            return;
        }

        int amplifier = level - 1;
        int durationTicks;

        // 使用配置的基础时间计算显示时长
        // Use config base time to calculate display duration
        int baseTime = ElementalReactionConfig.wetnessDecayBaseTime;

        if (isPaused) {
            // 在雨中/水中：赋予当前层数的满额时间
            // In Rain/Water: Grant full duration for current level
            durationTicks = (level * baseTime) * 20;
        } else {
            // 在干燥处：赋予剩余时间
            // In Dry area: Grant remaining time
            int maxDurationSeconds = level * baseTime;
            int remainingSeconds = maxDurationSeconds - decayTimer;
            durationTicks = Math.max(0, remainingSeconds * 20);
        }

        if (durationTicks > 0) {
            // 添加或更新效果
            // Add or update effect
            entity.addEffect(new MobEffectInstance(
                    ModMobEffects.WETNESS.get(),
                    durationTicks,
                    amplifier,
                    true, // ambient
                    true, // visible (particles)
                    true  // showIcon
            ));
        }
    }

    /**
     * 更新实体的潮湿层数 NBT。
     * <p>
     * Updates the entity's wetness level NBT.
     *
     * @param entity 目标实体 / Target entity
     * @param level  新的潮湿等级 / New wetness level
     */
    private static void updateWetnessLevel(LivingEntity entity, int level) {
        entity.getPersistentData().putInt(NBT_WETNESS, level);
    }

    /**
     * 处理饱食度惩罚逻辑。
     * 潮湿状态下会额外增加饱食度消耗。
     * 如果有额外的消耗，会通知调试系统。
     * <p>
     * Handles hunger exhaustion penalty logic.
     * Adds extra hunger exhaustion when wet.
     * Notifies the debug system if there is extra consumption.
     *
     * @param entity 目标实体 / Target entity
     */
    private static void handleExhaustion(LivingEntity entity) {
        if (entity instanceof Player player && !player.isCreative() && !player.isSpectator()) {
            CompoundTag data = player.getPersistentData();
            // 如果已经被免疫系统清除了数据，这里的 currentLevel 默认是 0，逻辑自动跳过
            // If data was cleared by immunity system, currentLevel defaults to 0, logic skips automatically
            int currentLevel = data.getInt(NBT_WETNESS);

            float currentExhaustion = player.getFoodData().getExhaustionLevel();
            float lastExhaustion = data.getFloat(NBT_LAST_EXHAUSTION);

            // 如果当前耗竭度大于上一次记录，说明发生了消耗
            // If current exhaustion is greater than last record, consumption happened
            if (currentExhaustion > lastExhaustion) {
                float delta = currentExhaustion - lastExhaustion;

                // 过滤极小的变动（例如自然恢复时的微小浮动）
                // Filter out tiny fluctuations (e.g., natural regeneration)
                if (delta > 0.0001f) {
                    float extra = 0;
                    if (currentLevel > 0) {
                        // ▲ 饱食度消耗：略微增加（模拟湿冷消耗热量）
                        // ▲ Hunger Consumption: Slightly increased (Simulating wet/cold heat loss)
                        extra = delta * currentLevel * (float) ElementalReactionConfig.wetnessExhaustionIncrease;
                        player.getFoodData().addExhaustion(extra);
                        currentExhaustion += extra;
                    }

                    // 发送调试日志给开启了调试模式的玩家
                    // Send debug log to players with debug mode enabled
                    if (extra > 0) {
                        DebugCommand.sendExhaustionLog(player, delta, extra, currentLevel);
                    }
                }
            }
            data.putFloat(NBT_LAST_EXHAUSTION, currentExhaustion);
        }
    }

    /**
     * 监听投掷物击中事件。
     * 所有投掷型药水（喷溅/滞留）击中生物时，均增加固定的潮湿层数。
     * <p>
     * Listens to Projectile Impact events.
     * All thrown potions (Splash/Lingering) increase wetness level by a fixed amount when hitting a mob.
     *
     * @param event 投掷物撞击事件 / Projectile impact event
     */
    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getRayTraceResult().getType() != HitResult.Type.ENTITY) return;

        Entity projectile = event.getProjectile();
        Entity target = ((EntityHitResult) event.getRayTraceResult()).getEntity();

        if (!(target instanceof LivingEntity livingTarget)) return;

        // 在应用药水效果前，同样检查免疫状态
        // Check immunity status before applying potion effect
        if (isImmune(livingTarget)) return;

        // 检查是否为投掷型药水 (包括喷溅药水和滞留药水)
        // Check if it is a thrown potion (includes Splash Potion and Lingering Potion)
        if (projectile instanceof ThrownPotion) {

            // 无论药水类型如何，都使用配置的数值（统一数值）
            // Regardless of potion type, use the configured value (unified value)
            int add = ElementalReactionConfig.wetnessPotionAddLevel;

            CompoundTag data = livingTarget.getPersistentData();
            int current = data.getInt(NBT_WETNESS);
            int max = ElementalReactionConfig.wetnessMaxLevel;

            // 增加层数
            // Increase level
            int newLevel = Math.min(max, current + add);
            updateWetnessLevel(livingTarget, newLevel);

            // 被药水砸中，重置衰减计时器
            // Hit by potion, reset decay timer
            data.putInt(NBT_DECAY_TIMER, 0);

            // 立即刷新视觉效果
            // Refresh visual effect immediately
            syncEffect(livingTarget, newLevel, livingTarget.isInWater() || livingTarget.level().isRainingAt(livingTarget.blockPosition()), 0);
        }
    }
}