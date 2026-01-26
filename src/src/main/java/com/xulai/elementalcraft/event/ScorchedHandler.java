// src/main/java/com/xulai/elementalcraft/event/ScorchedHandler.java
package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalReactionConfig;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * ScorchedHandler
 * <p>
 * 灼烧机制的核心事件处理器。
 * 该类负责管理实体的“灼烧”状态，这是一种基于 NBT 的持久化状态，不依赖于原版药水系统。
 * 它处理状态的施加、周期性逻辑（伤害与特效）、环境交互（如遇水爆炸）以及伤害计算公式。
 * <p>
 * Core event handler for the Scorched mechanic.
 * This class manages the "Scorched" status of entities, which is a persistent state based on NBT and does not rely on the vanilla potion system.
 * It handles application, periodic logic (damage and effects), environmental interactions (e.g., explosion in water), and damage calculation formulas.
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class ScorchedHandler {

    /**
     * NBT Data Keys
     * NBT 数据键名
     * <p>
     * Keys used to store scorched status data in the entity's persistent data tag.
     * 用于在实体持久化数据标签中存储灼烧状态数据的键名。
     */
    public static final String NBT_SCORCHED_TICKS = "ec_scorched_ticks";
    public static final String NBT_SCORCHED_STRENGTH = "ec_scorched_str";
    public static final String NBT_SCORCHED_COOLDOWN = "ec_scorched_cd";

    /**
     * Apply Scorched Status
     * 施加灼烧状态
     * <p>
     * Applies the scorched effect to a target entity.
     * This method performs blacklist checks and cooldown validation before applying the state.
     * <p>
     * 对目标实体施加灼烧效果。
     * 此方法在施加状态前会执行黑名单检查和冷却时间验证。
     *
     * @param target       The target entity. (目标实体)
     * @param fireStrength The strength of the fire element attacking. (攻击的火属性强度)
     * @param duration     The duration of the effect in ticks. (效果持续时间，单位：Tick)
     */
    public static void applyScorched(LivingEntity target, int fireStrength, int duration) {
        if (target.level().isClientSide) return;

        // Blacklist Check
        // 黑名单检查
        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).toString();
        if (ElementalReactionConfig.cachedScorchedBlacklist.contains(entityId)) {
            return;
        }

        // Cooldown Check
        // 冷却时间检查
        CompoundTag data = target.getPersistentData();
        long gameTime = target.level().getGameTime();
        if (data.contains(NBT_SCORCHED_COOLDOWN)) {
            if (gameTime < data.getLong(NBT_SCORCHED_COOLDOWN)) {
                return; // Entity is on cooldown (处于冷却中)
            }
        }

        // Set NBT Data and Status
        // 设置 NBT 数据与状态
        data.putInt(NBT_SCORCHED_TICKS, duration);
        data.putInt(NBT_SCORCHED_STRENGTH, fireStrength);
        
        // Set Cooldown (Current Time + Duration + Configured Cooldown)
        // 设置冷却时间（当前时间 + 持续时间 + 配置的冷却时间）
        data.putLong(NBT_SCORCHED_COOLDOWN, gameTime + duration + ElementalReactionConfig.scorchedCooldown);

        // Force Vanilla Fire Visuals
        // Forces the entity to visually burn for the configured lock duration to sync with the scorched effect.
        // 强制显示原版火焰视觉效果
        // 强制实体在配置的锁定时间内显示燃烧效果，以与灼烧状态同步。
        target.setSecondsOnFire((int) ElementalReactionConfig.scorchedBurningLockDuration);

        // Spawn Initial Particles
        // 生成初始粒子效果
        if (target.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LAVA,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    5, 0.2, 0.2, 0.2, 0.0);
        }
    }

    /**
     * Living Tick Event
     * 生物 Tick 事件
     * <p>
     * Main logic loop for the scorched status.
     * Handles countdown, cleanup, water interaction (Thermal Shock), visual locking, and periodic damage.
     * <p>
     * 灼烧状态的主逻辑循环。
     * 处理倒计时、清理、水交互（热休克/爆炸）、视觉锁定以及周期性伤害。
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        CompoundTag data = entity.getPersistentData();
        if (!data.contains(NBT_SCORCHED_TICKS)) return;

        int ticks = data.getInt(NBT_SCORCHED_TICKS);
        
        // Cleanup when duration expires
        // 持续时间结束后清理数据
        if (ticks <= 0) {
            data.remove(NBT_SCORCHED_TICKS);
            data.remove(NBT_SCORCHED_STRENGTH);
            return;
        }

        // Decrement Timer
        // 倒计时递减
        data.putInt(NBT_SCORCHED_TICKS, ticks - 1);
        int fireStrength = data.getInt(NBT_SCORCHED_STRENGTH);
        ServerLevel level = (ServerLevel) entity.level();

        // 1. Environmental Interaction: Deep Water Check
        // 1. 环境交互：深水判定
        // If the entity is submerged in water, trigger Thermal Shock (explosion) and remove the effect.
        // 如果实体浸没在水中，触发热休克（爆炸）并移除效果。
        if (entity.isInWater() && entity.isEyeInFluidType(net.minecraftforge.common.ForgeMod.WATER_TYPE.get())) {
            triggerThermalShock(entity, level, ticks, fireStrength);
            return;
        }

        // 2. Burning Visual Lock
        // 2. 燃烧视觉锁定
        // Keeps the entity visually burning as long as the scorched effect persists.
        // 只要灼烧效果存在，就保持实体的燃烧视觉状态。
        int lockTicks = (int) (ElementalReactionConfig.scorchedBurningLockDuration * 20);
        if (entity.getRemainingFireTicks() < lockTicks) {
            entity.setSecondsOnFire((int) ElementalReactionConfig.scorchedBurningLockDuration);
        }

        // 3. Periodic Damage and Effects
        // 3. 周期性伤害与特效
        // Deals damage every second (20 ticks).
        // 每秒（20 Tick）造成一次伤害。
        if (entity.tickCount % 20 == 0) {
            float damage = calculateScorchedDamage(fireStrength, entity);

            if (damage > 0) {
                entity.hurt(ModDamageTypes.source(level, ModDamageTypes.LAVA_MAGIC), damage);

                level.sendParticles(ParticleTypes.LAVA,
                        entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                        1, 0.2, 0.2, 0.2, 0.0);

                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.2f, 1.0f);
            }
        }
    }

    /**
     * Effect Applicability Check
     * 效果适用性检查
     * <p>
     * Prevents the "Wetness" effect from being applied if the entity is currently Scorched.
     * High temperature evaporates moisture immediately.
     * <p>
     * 如果实体当前处于灼烧状态，则阻止“潮湿”效果的施加。
     * 高温会立即蒸发水分。
     */
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().getEffect() == ModMobEffects.WETNESS.get()) {
            if (event.getEntity().getPersistentData().contains(NBT_SCORCHED_TICKS)) {
                event.setResult(Event.Result.DENY);
            }
        }
    }

    /**
     * Living Hurt Event
     * 生物受伤事件
     * <p>
     * If the entity is Scorched, it cancels standard fire damage to prevent double damage calculations,
     * relying solely on the custom scorched damage logic (except for specific custom damage sources).
     * <p>
     * 如果实体处于灼烧状态，取消标准火属性伤害以防止双重伤害计算，
     * 仅依赖自定义的灼烧伤害逻辑（特定的自定义伤害源除外）。
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        if (!entity.getPersistentData().contains(NBT_SCORCHED_TICKS)) return;

        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.IS_FIRE) && !source.is(ModDamageTypes.LAVA_MAGIC)) {
            event.setCanceled(true);
        }
    }

    /**
     * Trigger Thermal Shock
     * 触发热休克
     * <p>
     * Executes when a scorched entity enters water.
     * Deals burst damage based on remaining duration and plays explosion effects.
     * Burst damage ratio is controlled by configuration.
     * <p>
     * 当灼烧实体进入水中时执行。
     * 根据剩余持续时间造成爆发伤害并播放爆炸特效。
     * 爆发伤害比例由配置控制。
     */
    private static void triggerThermalShock(LivingEntity entity, ServerLevel level, int remainingTicks, int fireStrength) {
        double remainingSeconds = remainingTicks / 20.0;
        float dps = calculateScorchedDamage(fireStrength, entity);
        float totalRemainingDamage = (float) (remainingSeconds * dps);
        
        // Deal configured percentage of remaining DOT as instant damage
        // 将剩余持续伤害总和的配置比例作为瞬间伤害结算
        float ratio = (float) ElementalReactionConfig.scorchedShockDamageRatio;
        float shockDamage = totalRemainingDamage * ratio;

        if (shockDamage > 0.5f) {
            entity.hurt(entity.damageSources().generic(), shockDamage);
        }

        // Clear Status
        // 清除状态
        entity.clearFire();
        CompoundTag data = entity.getPersistentData();
        data.remove(NBT_SCORCHED_TICKS);
        data.remove(NBT_SCORCHED_STRENGTH);

        // Play Effects
        // 播放特效
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f, 2.0f);

        level.sendParticles(ParticleTypes.CLOUD,
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                20, 0.5, 0.5, 0.5, 0.05);
    }

    /**
     * Calculate Scorched Damage
     * 计算灼烧伤害
     * <p>
     * Determines the damage per second based on fire strength, target resistance, enchantments, and natural immunities.
     * <p>
     * 根据火属性强度、目标抗性、附魔和自然免疫力确定每秒伤害。
     *
     * @param fireStrength The attacker's fire attribute strength. (攻击者的火属性强度)
     * @param target       The victim entity. (受害者实体)
     * @return The calculated damage amount. (计算出的伤害数值)
     */
    private static float calculateScorchedDamage(int fireStrength, LivingEntity target) {
        int resistPoints = ElementUtils.getDisplayResistance(target, ElementType.FIRE);

        // Check Resistance Threshold
        // 检查抗性阈值
        if (resistPoints >= ElementalReactionConfig.scorchedResistThreshold) {
            return 0.0f;
        }

        // Calculate Base Damage
        // 计算基础伤害
        double base = ElementalReactionConfig.scorchedDamageBase;
        int step = Math.max(1, ElementalReactionConfig.scorchedDamageScalingStep);
        double bonus = (double) fireStrength / step * 0.5;
        double rawDamage = base + bonus;

        // Immunity Check
        // 免疫检查
        // If the mob is naturally immune to fire, reduce damage by a configured modifier.
        // 如果生物天生免疫火焰，则按配置的倍率降低伤害。
        if (target.fireImmune()) {
            rawDamage *= ElementalReactionConfig.scorchedImmuneModifier;
        }

        // Nature Vulnerability
        // 自然属性易伤
        // Nature entities take extra damage.
        // 自然属性实体受到额外伤害。
        if (ElementUtils.getDisplayEnhancement(target, ElementType.NATURE) > 0 ||
                ElementUtils.getDisplayResistance(target, ElementType.NATURE) > 0) {
            rawDamage *= ElementalReactionConfig.scorchedNatureMultiplier;
        }

        // Enchantment Reduction
        // 附魔减伤
        int fireProtLevel = 0;
        int genProtLevel = 0;

        for (ItemStack stack : target.getArmorSlots()) {
            fireProtLevel += EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_PROTECTION, stack);
            genProtLevel += EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, stack);
        }

        // Calculate reduction percentages capped by config denominator
        // 计算受配置分母限制的减伤百分比
        double denom = ElementalReactionConfig.enchantmentCalculationDenominator;
        
        double fireProtReduction = (Math.min(fireProtLevel, denom) / denom) * ElementalReactionConfig.scorchedFireProtReduction;
        double genProtReduction = (Math.min(genProtLevel, denom) / denom) * ElementalReactionConfig.scorchedGenProtReduction;

        rawDamage *= (1.0 - fireProtReduction);
        rawDamage *= (1.0 - genProtReduction);

        return (float) rawDamage;
    }
}