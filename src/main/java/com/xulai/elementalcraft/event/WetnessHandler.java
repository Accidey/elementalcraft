// src/main/java/com/xulai/elementalcraft/event/WetnessHandler.java
package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalReactionConfig;
import com.xulai.elementalcraft.potion.ModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * WetnessHandler
 *
 * 中文说明：
 * 负责处理潮湿系统的核心逻辑。
 * 包含：
 * 1. 动态衰减时间（级联降级）：4层(40s)->3层(30s)...
 * 2. 环境积累逻辑：雨中每10秒增加一层。
 * 3. 属性抗性变化：
 * - ▼ 雷霆/冰霜抗性：每层 -10%（受到伤害增加）。
 * - ▲ 赤焰抗性：每层 +10%（受到伤害减少）。
 * - ▲ 饱食度消耗：略微增加。
 *
 * English description:
 * Handles the core logic for the Wetness System.
 * Includes:
 * 1. Dynamic decay time (cascading downgrade): Lvl 4(40s) -> Lvl 3(30s)...
 * 2. Environmental accumulation logic: Gain one level every 10s in rain.
 * 3. Attribute Resistance Changes:
 * - ▼ Thunder/Frost Resistance: -10% per level (Increased damage taken).
 * - ▲ Flame Resistance: +10% per level (Decreased damage taken).
 * - ▲ Hunger Consumption: Slightly increased.
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
     * 处理环境检测（雨/雪/水）、潮湿层数更新、药水效果同步。
     *
     * Listens to Living Tick events.
     * Handles environment detection (Rain/Snow/Water), wetness level updates, and potion effect synchronization.
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
     * 处理抗性变化：雷霆/冰霜易伤，赤焰减伤。
     *
     * Listens to Living Hurt events.
     * Handles resistance changes: Vulnerability to Thunder/Frost, Resistance to Fire.
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
                // 基础倍率：每层 10% (0.1)
                // Base multiplier: 10% per level (0.1)
                float factor = 0.1f * currentLevel;
                // 限制最高 50% (0.5)
                // Cap at 50% (0.5)
                factor = Math.min(0.5f, factor);

                float originalDamage = event.getAmount();
                float finalDamage = originalDamage;

                // 1. 赤焰伤害 (Fire Damage)
                // ▲ 赤焰抗性：每层 +10% (受到伤害减少)
                // ▲ Flame Resistance: +10% per level (Damage Reduced)
                if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
                    // 减少伤害 / Reduce damage
                    finalDamage = originalDamage * (1.0f - factor);
                } 
                // 2. 雷霆或冰霜伤害 (Thunder or Frost Damage)
                // ▼ 雷霆/冰霜抗性：每层 -10% (受到伤害增加)
                // ▼ Thunder/Frost Resistance: -10% per level (Damage Increased)
                else if (event.getSource().is(DamageTypeTags.IS_LIGHTNING) || event.getSource().is(DamageTypeTags.IS_FREEZING)) {
                    // 增加伤害 / Increase damage
                    finalDamage = originalDamage * (1.0f + factor);
                }
                event.setAmount(finalDamage);
            }
        }
    }

    /**
     * 处理潮湿积累与衰减的核心逻辑。
     *
     * Handles the core logic for wetness accumulation and decay.
     */
    private static void handleWetnessLogic(LivingEntity entity) {
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
            // 中文：在水中时，倒计时不开始（重置为0）。
            // 只要没满级，就强制设为满级（模拟湿透）。
            // English: In water, countdown does not start (reset to 0).
            // Force max level if not already max (simulate soaking).
            
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
            // 中文：积累阶段。倒计时不开始（衰减计时器重置）。
            // 规则：达到10秒后进入下一层。
            // English: Accumulation phase. Countdown does not start (decay timer reset).
            // Rule: Reach 10 seconds to enter next level.
            
            data.putInt(NBT_DECAY_TIMER, 0); // 只要在雨中，衰减倒计时就不开始 / Decay timer doesn't start in rain

            if (currentLevel < maxLevel) {
                int rainTimer = data.getInt(NBT_RAIN_TIMER) + 1; // +1s
                
                // 检查是否达到积累阈值 (10秒)
                // Check if accumulation threshold is reached (10 seconds)
                int thresholdSeconds = 10; 
                
                if (rainTimer >= thresholdSeconds) {
                    currentLevel++;
                    updateWetnessLevel(entity, currentLevel);
                    data.putInt(NBT_RAIN_TIMER, 0); // 清除上一层的积累，开始下一层的10秒倒计时 / Clear accumulation, start next 10s count
                } else {
                    data.putInt(NBT_RAIN_TIMER, rainTimer);
                }
            }
        } else {
            // === 状态：干燥/离开雨水 (Dry/Left Water) ===
            // 中文：衰减阶段。倒计时开始。
            // 规则：根据身上的潮湿层数赋予对应的时间。倒计时结束后赋予下一层和对应的时间。
            // English: Decay phase. Countdown starts.
            // Rule: Assign time corresponding to wetness level. When finished, grant next level and its time.

            data.putInt(NBT_RAIN_TIMER, 0); // 离开雨中，积累进度丢失 / Accumulation progress lost

            if (currentLevel > 0) {
                int decayTimer = data.getInt(NBT_DECAY_TIMER) + 1; // +1s
                
                // 计算当前层数应该持续的总时间
                // 公式：层数 * 10秒 (例如: 4层=40s, 3层=30s)
                // Calculate total duration for current level
                // Formula: Level * 10s (e.g., Lvl 4 = 40s, Lvl 3 = 30s)
                int durationForCurrentLevel = currentLevel * 10; 

                if (decayTimer >= durationForCurrentLevel) {
                    // 倒计时结束，降级
                    // Countdown finished, downgrade
                    currentLevel--;
                    updateWetnessLevel(entity, currentLevel);
                    
                    // 重置计时器，准备开始下一层的倒计时
                    // Reset timer, ready to start countdown for the next level
                    data.putInt(NBT_DECAY_TIMER, 0);
                    
                    // 注意：如果降级到0，updateWetnessLevel 会移除效果
                    // Note: If downgraded to 0, updateWetnessLevel removes the effect
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
     * 同步药水效果到实体。
     * * Syncs potion effect to the entity.
     * * @param entity 目标实体 / Target entity
     * @param level 当前潮湿等级 / Current wetness level
     * @param isPaused 是否处于暂停倒计时状态（雨中/水中） / Is countdown paused (Rain/Water)
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

        if (isPaused) {
            // 在雨中/水中：
            // 赋予当前层数的满额时间，并且持续刷新，让玩家看到时间是“满的”或者“暂停的”。
            // In Rain/Water:
            // Grant full duration for current level and refresh constantly, making time look "full" or "paused".
            // Level 4 -> 40s, Level 1 -> 10s
            durationTicks = (level * 10) * 20; 
        } else {
            // 在干燥处：
            // 赋予剩余时间。
            // In Dry area:
            // Grant remaining time.
            int maxDurationSeconds = level * 10;
            int remainingSeconds = maxDurationSeconds - decayTimer;
            durationTicks = Math.max(0, remainingSeconds * 20);
        }

        if (durationTicks > 0) {
            // 添加或更新效果。设置 showIcon=true, visible=true。
            // Add or update effect. Set showIcon=true, visible=true.
            // 使用 false, false 避免覆盖时产生过多的粒子动画
            // Use false, false to avoid excessive particle animations when overriding
            entity.addEffect(new MobEffectInstance(
                ModMobEffects.WETNESS.get(), 
                durationTicks, 
                amplifier, 
                true, // ambient
                false, // visible (particles) - 可以根据需求设为true / Can be true if needed
                true   // showIcon
            ));
        }
    }

    /**
     * 更新实体的潮湿层数 NBT。
     * * Updates the entity's wetness level NBT.
     */
    private static void updateWetnessLevel(LivingEntity entity, int level) {
        entity.getPersistentData().putInt(NBT_WETNESS, level);
    }

    /**
     * 处理饱食度惩罚逻辑。
     * * Handles hunger exhaustion penalty logic.
     */
    private static void handleExhaustion(LivingEntity entity) {
        if (entity instanceof Player player && !player.isCreative() && !player.isSpectator()) {
            CompoundTag data = player.getPersistentData();
            int currentLevel = data.getInt(NBT_WETNESS);
            
            float currentExhaustion = player.getFoodData().getExhaustionLevel();
            float lastExhaustion = data.getFloat(NBT_LAST_EXHAUSTION);

            // 如果当前耗竭度大于上一次记录，说明发生了消耗
            // If current exhaustion is greater than last record, consumption happened
            if (currentExhaustion > lastExhaustion) {
                float delta = currentExhaustion - lastExhaustion;
                if (delta > 0.0001f && currentLevel > 0) {
                    // ▲ 饱食度消耗：略微增加（模拟湿冷消耗热量）
                    // ▲ Hunger Consumption: Slightly increased (Simulating wet/cold heat loss)
                    float extra = delta * currentLevel * (float) ElementalReactionConfig.wetnessExhaustionIncrease;
                    player.getFoodData().addExhaustion(extra);
                    currentExhaustion += extra;
                }
            }
            data.putFloat(NBT_LAST_EXHAUSTION, currentExhaustion);
        }
    }

    /**
     * 监听投掷物击中事件（水瓶增加潮湿）。
     * * Listens to Projectile Impact events (Water bottle increases wetness).
     */
    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getRayTraceResult().getType() != HitResult.Type.ENTITY) return;
        
        Entity projectile = event.getProjectile();
        Entity target = ((EntityHitResult) event.getRayTraceResult()).getEntity();

        if (!(target instanceof LivingEntity livingTarget)) return;

        if (projectile instanceof ThrownPotion potionEntity) {
            ItemStack stack = potionEntity.getItem();
            if (PotionUtils.getPotion(stack) == Potions.WATER) {
                CompoundTag data = livingTarget.getPersistentData();
                int current = data.getInt(NBT_WETNESS);
                int max = ElementalReactionConfig.wetnessMaxLevel;
                int add = ElementalReactionConfig.wetnessPotionAddLevel;
                
                // 增加层数
                // Increase level
                int newLevel = Math.min(max, current + add);
                updateWetnessLevel(livingTarget, newLevel);
                
                // 被水瓶砸中，重置衰减计时器，但不重置积累计时器
                // Hit by water bottle, reset decay timer
                data.putInt(NBT_DECAY_TIMER, 0);

                // 立即刷新视觉效果
                // Refresh visual effect immediately
                syncEffect(livingTarget, newLevel, livingTarget.isInWater() || livingTarget.level().isRainingAt(livingTarget.blockPosition()), 0);
            }
        }
    }
}