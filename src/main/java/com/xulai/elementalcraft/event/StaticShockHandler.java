// src/main/java/com/xulai/elementalcraft/event/StaticShockHandler.java
package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.sound.ModSounds;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * 静电效果核心处理类。
 * 负责雷霆属性攻击触发静电效果的逻辑，包括触发概率计算、层数叠加、持续时间刷新、
 * 周期性伤害（DOT）以及静电的自然衰减（仅在干燥环境中）。
 * 同时负责同步 {@link com.xulai.elementalcraft.potion.StaticShockEffect} 状态效果，
 * 使玩家 GUI 能正确显示静电层数和剩余时间。
 * 如果目标雷霆抗性点数达到配置的免疫阈值，则完全免疫静电的叠加和伤害。
 * 保底触发机制：静电在整个持续时间内必须触发（层数 × 每层次数）次伤害。
 * 当层数叠加时，所需总伤害次数更新为（新层数 × 每层次数），已触发的次数保留，只需补充缺少的次数。
 * 一旦达到所需次数，后续即使效果未结束也不再触发任何伤害。
 * 保底触发条件优化：当剩余时间不足以按固定间隔完成剩余次数时，立即强制触发一次伤害。
 * 修复了效果结束时伤害可能无法触发的问题：调整了伤害触发与时间衰减的顺序，确保在效果结束前完成最后一次伤害。
 * 添加了音效：每次触发静电伤害时，会在目标位置播放自定义电击音效（{@link ModSounds#ELECTRIC_ZAP}），
 * 音调在 0.8~1.2 之间随机，音量固定为 0.8，以增强反馈。
 * 添加了视觉粒子效果：
 * - 拥有静电效果的生物周身会持续环绕蓝紫色光点（{@link ParticleTypes#ENTITY_EFFECT}），每 5 tick 生成 2-3 个，
 *   颜色为蓝紫色 (R=0.3, G=0.4, B=1.0)，速度极小，营造悬浮感。
 * - 当静电伤害触发时，目标位置会迸发大量白色电火花（{@link ParticleTypes#ELECTRIC_SPARK}），
 *   数量为 10-15 个，速度随机向外 (0.1~0.3)，模拟放电冲击。
 * 所有配置参数均来自 {@link ElementalThunderFrostReactionsConfig}，支持热重载。
 * <p>
 * Core handler for the Static Shock effect.
 * Manages the logic for triggering Static Shock from Thunder attribute attacks,
 * including probability calculation, stack accumulation, duration refresh,
 * periodic damage (DOT), and natural decay (only in dry environments).
 * Also responsible for synchronizing the {@link com.xulai.elementalcraft.potion.StaticShockEffect}
 * status effect so that the player GUI correctly displays static shock stacks and remaining time.
 * If the target's Thunder resistance reaches the configured immunity threshold,
 * it becomes completely immune to Static Shock stacking and damage.
 * Guaranteed minimum trigger mechanism: Static Shock must trigger (stacks × hits per stack) damage ticks
 * during its entire duration.
 * When stacks are increased, the total required damage count updates to (new stacks × hits per stack),
 * while already triggered counts are retained, only the missing ones need to be supplemented.
 * Once the required count is reached, no further damage will be triggered even if the effect persists.
 * Optimized guaranteed trigger condition: Forces an immediate damage tick when remaining time is insufficient
 * to complete the remaining required hits at the fixed interval.
 * Fixed the issue where damage might not trigger when the effect expires: Adjusted the order of damage processing
 * and time decay to ensure the final damage tick occurs before the effect ends.
 * Added sound effects: Every time Static Shock damage is triggered, a custom electric zap sound ({@link ModSounds#ELECTRIC_ZAP})
 * is played at the target's position, with a random pitch between 0.8 and 1.2 and a fixed volume of 0.8,
 * enhancing feedback.
 * Added visual particle effects:
 * - Entities with Static Shock constantly emit blue-purple glowing particles ({@link ParticleTypes#ENTITY_EFFECT})
 *   every 5 ticks (2-3 particles), colored blue-purple (R=0.3, G=0.4, B=1.0), with minimal speed to create a hovering effect.
 * - When Static Shock damage is triggered, the target location erupts with white electric sparks
 *   ({@link ParticleTypes#ELECTRIC_SPARK}) (10-15 particles) with random outward velocity (0.1~0.3),
 *   simulating a discharge impact.
 * All configuration parameters are sourced from {@link ElementalThunderFrostReactionsConfig}
 * and support hot-reloading.
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class StaticShockHandler {

    /**
     * 全局随机数生成器，用于概率判定、伤害随机浮动、粒子生成随机以及音调随机。
     * <p>
     * Global random generator for probability checks, damage randomisation, particle randomisation and pitch randomisation.
     */
    private static final Random RANDOM = new Random();

    /**
     * 存储静电层数的 NBT 键名。
     * <p>
     * NBT key for storing static shock stacks.
     */
    private static final String NBT_STATIC_STACKS = "EC_StaticStacks";

    /**
     * 存储当前总剩余时间（刻）的 NBT 键名。
     * <p>
     * NBT key for storing total remaining time (in ticks).
     */
    private static final String NBT_STATIC_TIMER = "EC_StaticTimer";

    /**
     * 存储距离下次伤害触发剩余时间（刻）的 NBT 键名。
     * <p>
     * NBT key for storing ticks remaining until next damage tick.
     */
    private static final String NBT_STATIC_DAMAGE_TIMER = "EC_StaticDamageTimer";

    /**
     * 存储已触发的静电伤害次数，用于保底触发机制。
     * <p>
     * NBT key for storing the count of triggered static shock damage ticks, used for guaranteed minimum trigger mechanism.
     */
    private static final String NBT_STATIC_DAMAGE_COUNT = "EC_StaticDamageCount";

    /**
     * 存储需要触发的总伤害次数（即当前层数 × 每层次数），用于保底机制。
     * <p>
     * NBT key for storing the total required damage ticks (i.e., current stacks × hits per stack), used for guaranteed mechanism.
     */
    private static final String NBT_STATIC_TARGET_COUNT = "EC_StaticTargetCount";

    /**
     * 监听生物受伤事件，处理雷霆属性攻击触发静电的叠加。
     * <p>
     * Listens to living hurt events to handle Static Shock application from Thunder attacks.
     *
     * @param event 生物受伤事件 / The living damage event
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;

        // 仅当攻击者存在且为生物时处理
        // Process only if the attacker exists and is a living entity
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;

        LivingEntity target = event.getEntity();

        // 检查目标雷霆抗性是否达到免疫阈值，如果免疫则直接返回
        // Check if target's Thunder resistance meets immunity threshold; if immune, return immediately
        int targetThunderResist = ElementUtils.getDisplayResistance(target, ElementType.THUNDER);
        int immunityThreshold = ElementalThunderFrostReactionsConfig.staticResistImmunityThreshold;
        if (targetThunderResist >= immunityThreshold) {
            // 如果目标已有静电效果，清除它
            // If target already has Static Shock, clear it
            clearStaticShock(target);
            return;
        }

        // 获取攻击者的雷霆属性强化点数
        // Retrieve attacker's Thunder attribute strength points
        int thunderStrength = ElementUtils.getDisplayEnhancement(attacker, ElementType.THUNDER);
        int threshold = ElementalThunderFrostReactionsConfig.thunderStrengthThreshold;

        // 未达到强化门槛，无法触发
        // Trigger impossible if below threshold
        if (thunderStrength < threshold) return;

        // 计算触发概率
        // Calculate trigger chance
        double chance = calculateTriggerChance(thunderStrength);
        if (RANDOM.nextDouble() >= chance) return;

        // 获取目标当前静电层数
        // Get target's current static stacks
        CompoundTag data = target.getPersistentData();
        int currentStacks = data.getInt(NBT_STATIC_STACKS);
        int maxStacks = ElementalThunderFrostReactionsConfig.staticMaxTotalStacks;

        // 已达最大层数，不再叠加
        // Stop stacking if max stacks reached
        if (currentStacks >= maxStacks) return;

        // 单次攻击最多叠加层数
        // Max stacks per attack
        int addStacks = ElementalThunderFrostReactionsConfig.staticMaxStacksPerAttack;
        int newStacks = Math.min(maxStacks, currentStacks + addStacks);

        // 获取每层持续时间（刻）
        // Get duration per stack (in ticks)
        int durationPerStack = ElementalThunderFrostReactionsConfig.staticDurationPerStackTicks;
        int newTotalTicks = newStacks * durationPerStack;

        // 更新NBT
        // Update NBT data
        data.putInt(NBT_STATIC_STACKS, newStacks);
        data.putInt(NBT_STATIC_TIMER, newTotalTicks);

        // 重置伤害计时器（让伤害从叠加后重新计时）
        // Reset damage timer so damage interval restarts after stacking
        data.putInt(NBT_STATIC_DAMAGE_TIMER, 0);

        // 计算新的目标次数：层数 × 每层次数
        // Calculate new target count: stacks × hits per stack
        int newTargetCount = newStacks * ElementalThunderFrostReactionsConfig.staticHitsPerStack;
        data.putInt(NBT_STATIC_TARGET_COUNT, newTargetCount);
        // 已触发的伤害次数保持不变（damageCount不重置）

        // 同步药水效果（GUI显示）
        // Synchronise potion effect for GUI display
        updateEffect(target, newStacks, newTotalTicks);
    }

    /**
     * 监听生物每 Tick 事件，处理静电的持续时间递减、衰减和周期性伤害，并同步药水效果。
     * 如果目标雷霆抗性达到免疫阈值，则清除所有静电效果并跳过后续逻辑。
     * 保底触发机制：以（当前层数 × 每层次数）为基准，确保在效果持续期间触发足够次数的伤害。
     * 一旦已触发次数达到目标次数，后续不再触发任何伤害。
     * 当剩余时间不足以按固定间隔完成剩余次数时，立即强制触发一次伤害。
     * 修复：先处理伤害触发，再处理时间衰减，确保效果结束时最后一次伤害能够正确触发。
     * 每次触发伤害时，会在目标位置播放自定义电击音效，音调随机，音量固定，并迸发白色电火花粒子。
     * 此外，拥有静电效果的生物会持续环绕蓝紫色光点粒子。
     * <p>
     * Listens to living tick events to handle duration decay, stack reduction, periodic damage,
     * and synchronize the potion effect.
     * If the target's Thunder resistance meets the immunity threshold, clears all Static Shock effects
     * and skips subsequent logic.
     * Guaranteed minimum trigger: Based on (current stacks × hits per stack), ensures enough damage ticks occur
     * during the effect duration. Once the triggered count reaches the target count,
     * no further damage will be triggered.
     * Forces an immediate damage tick when remaining time is insufficient to complete the remaining
     * required hits at the fixed interval.
     * Fixed: Process damage triggers before time decay to ensure the final damage tick occurs
     * before the effect expires.
     * Every time damage is triggered, a custom electric zap sound is played at the target's position,
     * with random pitch and fixed volume, and white electric spark particles erupt.
     * Additionally, entities with Static Shock constantly emit blue-purple glowing particles around them.
     *
     * @param event 生物 Tick 事件 / The living tick event
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        // 检查目标雷霆抗性是否达到免疫阈值
        // Check if target's Thunder resistance meets immunity threshold
        int targetThunderResist = ElementUtils.getDisplayResistance(entity, ElementType.THUNDER);
        int immunityThreshold = ElementalThunderFrostReactionsConfig.staticResistImmunityThreshold;
        if (targetThunderResist >= immunityThreshold) {
            // 如果免疫，清除所有静电相关数据并返回
            // If immune, clear all static shock related data and return
            clearStaticShock(entity);
            return;
        }

        CompoundTag data = entity.getPersistentData();
        MobEffectInstance effectInstance = entity.getEffect(ModMobEffects.STATIC_SHOCK.get());

        // 情况1：有静电效果但无 NBT 数据（通过 /effect give 添加）
        // Case 1: Has effect but no NBT data (added via /effect give)
        if (effectInstance != null && !data.contains(NBT_STATIC_STACKS)) {
            // 从效果实例中获取放大器和剩余时间
            // Get amplifier and remaining duration from effect instance
            int amplifier = effectInstance.getAmplifier();
            int remainingTicks = effectInstance.getDuration();
            int stacks = amplifier + 1; // 层数 = 放大器 + 1
            int durationPerStack = ElementalThunderFrostReactionsConfig.staticDurationPerStackTicks;

            // 根据剩余时间估算合理层数，但以效果实例的放大器为准
            // Estimate reasonable stacks based on remaining time, but prioritize the effect's amplifier
            // 防止配置变更导致持续时间与层数不匹配
            // Prevent mismatch between duration and stacks due to config changes
            int maxStacks = ElementalThunderFrostReactionsConfig.staticMaxTotalStacks;
            stacks = Math.min(stacks, maxStacks);

            // 如果剩余时间小于当前层数应有的最小时间，则调整层数
            // If remaining time is less than the minimum required for current stacks, adjust stacks downward
            int minRequiredTicks = stacks * durationPerStack;
            if (remainingTicks < minRequiredTicks) {
                // 重新计算层数：向下取整，至少1层
                // Recalculate stacks: floor division, at least 1
                stacks = Math.max(1, remainingTicks / durationPerStack);
                amplifier = stacks - 1;
            }

            // 初始化 NBT 数据
            // Initialize NBT data
            data.putInt(NBT_STATIC_STACKS, stacks);
            data.putInt(NBT_STATIC_TIMER, remainingTicks);
            data.putInt(NBT_STATIC_DAMAGE_TIMER, 0); // 伤害计时器从头开始
            data.putInt(NBT_STATIC_DAMAGE_COUNT, 0); // 伤害计数清零
            // 目标次数 = 层数 × 每层次数
            int targetCount = stacks * ElementalThunderFrostReactionsConfig.staticHitsPerStack;
            data.putInt(NBT_STATIC_TARGET_COUNT, targetCount);

            // 更新效果实例的持续时间和放大器，以匹配调整后的层数
            // Update effect instance duration and amplifier to match adjusted stacks
            if (effectInstance.getDuration() != remainingTicks || effectInstance.getAmplifier() != amplifier) {
                entity.removeEffect(ModMobEffects.STATIC_SHOCK.get());
                entity.addEffect(new MobEffectInstance(
                        ModMobEffects.STATIC_SHOCK.get(),
                        remainingTicks,
                        amplifier,
                        effectInstance.isAmbient(),
                        effectInstance.isVisible(),
                        effectInstance.showIcon()
                ));
            }
        }

        if (!data.contains(NBT_STATIC_STACKS)) {
            // 确保没有残留的药水效果
            // Ensure no lingering potion effect
            if (entity.hasEffect(ModMobEffects.STATIC_SHOCK.get())) {
                entity.removeEffect(ModMobEffects.STATIC_SHOCK.get());
            }
            return;
        }

        int stacks = data.getInt(NBT_STATIC_STACKS);
        if (stacks <= 0) {
            clearStaticShock(entity);
            return;
        }

        int durationPerStack = ElementalThunderFrostReactionsConfig.staticDurationPerStackTicks;
        int totalTimer = data.getInt(NBT_STATIC_TIMER); // 当前剩余时间（递减前）
        int damageTimer = data.getInt(NBT_STATIC_DAMAGE_TIMER);
        int damageCount = data.getInt(NBT_STATIC_DAMAGE_COUNT);
        int targetCount = data.getInt(NBT_STATIC_TARGET_COUNT);
        // 如果目标次数未设置（旧数据），根据当前层数和配置重新计算
        // If target count not set (old data), recalculate based on current stacks and config
        if (targetCount == 0) {
            targetCount = stacks * ElementalThunderFrostReactionsConfig.staticHitsPerStack;
            data.putInt(NBT_STATIC_TARGET_COUNT, targetCount);
        }

        // ==================== 先处理伤害触发（依赖当前剩余时间） ====================
        // Process damage triggers first (depends on current remaining time)

        boolean damageTriggered = false;
        int damageInterval = ElementalThunderFrostReactionsConfig.staticDamageIntervalTicks;

        if (damageCount < targetCount) {
            // 正常间隔触发
            if (damageInterval > 0) {
                damageTimer++;
                if (damageTimer >= damageInterval) {
                    // 触发伤害
                    // Trigger damage
                    double minDmg = ElementalThunderFrostReactionsConfig.staticDamageMin;
                    double maxDmg = ElementalThunderFrostReactionsConfig.staticDamageMax;
                    float damage = (float) (minDmg + RANDOM.nextDouble() * (maxDmg - minDmg));

                    DamageSource damageSource = entity.damageSources().magic();
                    entity.hurt(damageSource, damage);

                    // 播放自定义电击音效（仅在服务端）
                    // Play custom electric zap sound (server side only)
                    if (!entity.level().isClientSide) {
                        float pitch = 0.8f + RANDOM.nextFloat() * 0.4f; // 0.8 ~ 1.2
                        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                                ModSounds.ELECTRIC_ZAP.get(), SoundSource.PLAYERS, 0.8f, pitch);
                    }

                    // 生成迸发电火花粒子
                    // Spawn burst electric spark particles
                    if (entity.level() instanceof ServerLevel serverLevel) {
                        spawnBurstParticles(serverLevel, entity);
                    }

                    damageTimer = 0;
                    damageCount++;
                    damageTriggered = true;
                }
            }

            // 保底触发：如果剩余时间不足以完成剩余次数，则强制触发一次
            int remainingHits = targetCount - damageCount;
            if (remainingHits > 0 && totalTimer < remainingHits * damageInterval) {
                if (!damageTriggered) {
                    double minDmg = ElementalThunderFrostReactionsConfig.staticDamageMin;
                    double maxDmg = ElementalThunderFrostReactionsConfig.staticDamageMax;
                    float damage = (float) (minDmg + RANDOM.nextDouble() * (maxDmg - minDmg));

                    DamageSource damageSource = entity.damageSources().magic();
                    entity.hurt(damageSource, damage);

                    // 播放自定义电击音效（仅在服务端）
                    // Play custom electric zap sound (server side only)
                    if (!entity.level().isClientSide) {
                        // 保底触发使用固定音高 2.0，音量 0.5（可调整）
                        // Forced trigger uses fixed pitch 2.0 and volume 0.5 (adjustable)
                        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                                ModSounds.ELECTRIC_ZAP.get(), SoundSource.PLAYERS, 0.5f, 2.0f);
                    }

                    // 生成迸发电火花粒子
                    // Spawn burst electric spark particles
                    if (entity.level() instanceof ServerLevel serverLevel) {
                        spawnBurstParticles(serverLevel, entity);
                    }

                    damageCount++;
                    damageTimer = 0;
                    damageTriggered = true;
                }
            }

            // 更新伤害计时器和次数
            data.putInt(NBT_STATIC_DAMAGE_TIMER, damageTimer);
            data.putInt(NBT_STATIC_DAMAGE_COUNT, damageCount);
        } else {
            // 已达成目标，不再触发伤害，但需重置计时器以避免残留
            data.putInt(NBT_STATIC_DAMAGE_TIMER, 0);
        }

        // ==================== 再处理时间衰减 ====================
        // Then handle time decay

        // 检查环境：是否在水中或雨中
        // Check environment: in water or rain
        boolean isWet = entity.isInWater() || entity.level().isRainingAt(entity.blockPosition());

        if (!isWet) {
            // 干燥环境：计时器递减
            // Dry environment: decrement timer
            if (totalTimer > 0) {
                totalTimer--;
                data.putInt(NBT_STATIC_TIMER, totalTimer);
            }

            // 检查是否需要减少层数（一层到期）
            // Check if one stack should expire
            if (stacks > 1 && totalTimer < (stacks - 1) * durationPerStack) {
                stacks--;
                data.putInt(NBT_STATIC_STACKS, stacks);
                // 注意：totalTimer 保持不变，即剩余时间不变
                // Note: totalTimer remains unchanged, representing remaining time
                // 层数减少不影响目标次数 targetCount
                // Stack reduction does not affect targetCount
            }

            // 如果 totalTimer <= 0，层数应归零
            // If totalTimer <= 0, all stacks have expired
            if (totalTimer <= 0) {
                clearStaticShock(entity);
                return;
            }
        }

        // ==================== 生成环绕粒子（悬浮效果） ====================
        // Generate ambient particles (hover effect)
        if (entity.level() instanceof ServerLevel serverLevel && entity.tickCount % 5 == 0) {
            // 每 5 tick 生成 2-3 个蓝紫色光点
            // Generate 2-3 blue-purple glowing particles every 5 ticks
            int count = 2 + RANDOM.nextInt(2); // 2 or 3
            for (int i = 0; i < count; i++) {
                double x = entity.getX() + (RANDOM.nextDouble() - 0.5) * entity.getBbWidth() * 1.5;
                double y = entity.getY() + RANDOM.nextDouble() * entity.getBbHeight();
                double z = entity.getZ() + (RANDOM.nextDouble() - 0.5) * entity.getBbWidth() * 1.5;
                // 使用 ENTITY_EFFECT 粒子，颜色为蓝紫色 (R=0.3, G=0.4, B=1.0)
                // Use ENTITY_EFFECT particle with blue-purple color (R=0.3, G=0.4, B=1.0)
                float red = 0.3f;
                float green = 0.4f;
                float blue = 1.0f;
                // 速度极小，营造漂浮感
                // Minimal speed to create floating effect
                double vx = (RANDOM.nextDouble() - 0.5) * 0.02;
                double vy = (RANDOM.nextDouble() - 0.5) * 0.02;
                double vz = (RANDOM.nextDouble() - 0.5) * 0.02;
                serverLevel.sendParticles(ParticleTypes.ENTITY_EFFECT, x, y, z, 1, vx, vy, vz, 0.0);
            }
        }

        // 同步药水效果（GUI显示）
        // Synchronise potion effect for GUI display
        updateEffect(entity, stacks, totalTimer);
    }

    /**
     * 生成迸发电火花粒子（当静电伤害触发时）。
     * 在目标周围随机位置生成 10-15 个白色电火花，速度随机向外 (0.1~0.3)。
     * <p>
     * Spawns burst electric spark particles (when Static Shock damage triggers).
     * Generates 10-15 white electric sparks around the target with random outward velocity (0.1~0.3).
     *
     * @param level  服务端世界 / Server level
     * @param entity 目标实体 / Target entity
     */
    private static void spawnBurstParticles(ServerLevel level, LivingEntity entity) {
        int count = 10 + RANDOM.nextInt(6); // 10~15
        for (int i = 0; i < count; i++) {
            double x = entity.getX() + (RANDOM.nextDouble() - 0.5) * entity.getBbWidth() * 2.0;
            double y = entity.getY() + RANDOM.nextDouble() * entity.getBbHeight();
            double z = entity.getZ() + (RANDOM.nextDouble() - 0.5) * entity.getBbWidth() * 2.0;
            // 随机向外速度
            double vx = (RANDOM.nextDouble() - 0.5) * 0.6;
            double vy = (RANDOM.nextDouble() - 0.5) * 0.6;
            double vz = (RANDOM.nextDouble() - 0.5) * 0.6;
            // 限制最小速度，确保粒子向外扩散
            // Ensure minimum speed to guarantee outward spread
            if (Math.abs(vx) < 0.1) vx = (vx > 0 ? 0.1 : -0.1);
            if (Math.abs(vy) < 0.1) vy = (vy > 0 ? 0.1 : -0.1);
            if (Math.abs(vz) < 0.1) vz = (vz > 0 ? 0.1 : -0.1);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 1, vx, vy, vz, 0.0);
        }
    }

    /**
     * 更新实体上的静电药水效果，用于 GUI 显示。
     * <p>
     * Updates the Static Shock potion effect on the entity for GUI display.
     *
     * @param entity      目标实体 / The target entity
     * @param stacks      当前静电层数 / Current static shock stacks
     * @param totalTicks  剩余总刻数 / Total remaining ticks
     */
    private static void updateEffect(LivingEntity entity, int stacks, int totalTicks) {
        if (stacks <= 0) {
            if (entity.hasEffect(ModMobEffects.STATIC_SHOCK.get())) {
                entity.removeEffect(ModMobEffects.STATIC_SHOCK.get());
            }
            return;
        }

        // 放大器 = 层数 - 1 (0 代表 1 层)
        // Amplifier = stacks - 1 (0 represents 1 stack)
        int amplifier = stacks - 1;
        // 持续时间必须至少为 1 tick，否则不会显示
        // Duration must be at least 1 tick to display
        int duration = Math.max(1, totalTicks);

        MobEffectInstance current = entity.getEffect(ModMobEffects.STATIC_SHOCK.get());
        // 如果效果已经存在且持续时间和放大器匹配，则无需重复添加
        // Skip if effect already exists with matching duration and amplifier
        if (current != null && current.getDuration() == duration && current.getAmplifier() == amplifier) {
            return;
        }

        // 添加新效果（隐藏粒子，显示图标）
        // Add new effect (hide particles, show icon)
        entity.addEffect(new MobEffectInstance(
                ModMobEffects.STATIC_SHOCK.get(),
                duration,
                amplifier,
                false,   // ambient / 环境效果
                false,   // showParticles / 显示粒子
                true     // showIcon / 显示图标
        ));
    }

    /**
     * 清除目标实体上的所有静电效果（NBT 数据和药水效果）。
     * <p>
     * Clears all Static Shock effects (NBT data and potion effect) from the target entity.
     *
     * @param entity 目标实体 / The target entity
     */
    private static void clearStaticShock(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        data.remove(NBT_STATIC_STACKS);
        data.remove(NBT_STATIC_TIMER);
        data.remove(NBT_STATIC_DAMAGE_TIMER);
        data.remove(NBT_STATIC_DAMAGE_COUNT);
        data.remove(NBT_STATIC_TARGET_COUNT);
        if (entity.hasEffect(ModMobEffects.STATIC_SHOCK.get())) {
            entity.removeEffect(ModMobEffects.STATIC_SHOCK.get());
        }
    }

    /**
     * 计算触发静电的概率。
     * <p>
     * Calculates the chance to trigger Static Shock.
     *
     * @param thunderStrength 攻击者的雷霆属性强化点数 / Attacker's Thunder attribute strength points
     * @return 触发概率（0~1） / Trigger chance (0~1)
     */
    private static double calculateTriggerChance(int thunderStrength) {
        int threshold = ElementalThunderFrostReactionsConfig.thunderStrengthThreshold;
        double baseChance = ElementalThunderFrostReactionsConfig.staticBaseChance;
        int scalingStep = ElementalThunderFrostReactionsConfig.staticScalingStep;
        double scalingChance = ElementalThunderFrostReactionsConfig.staticScalingChance;

        if (thunderStrength < threshold) return 0.0;

        // 超出门槛的部分按步长增加概率
        // Increase chance per step beyond threshold
        int excess = thunderStrength - threshold;
        int steps = excess / scalingStep;
        double totalChance = baseChance + steps * scalingChance;
        return Math.min(1.0, totalChance);
    }

    /**
     * 私有构造方法，防止实例化。
     * <p>
     * Private constructor to prevent instantiation.
     */
    private StaticShockHandler() {}
}