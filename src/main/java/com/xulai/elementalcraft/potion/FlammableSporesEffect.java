// src/main/java/com/xulai/elementalcraft/potion/FlammableSporesEffect.java
package com.xulai.elementalcraft.potion;

import com.xulai.elementalcraft.config.ElementalReactionConfig;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

/**
 * FlammableSporesEffect
 * <p>
 * 中文说明：
 * 易燃孢子效果定义类
 * 这是一个“双刃剑”状态，它是元素反应系统从数值对抗转向状态博弈的核心。
 * <p>
 * 主要功能：
 * 1. **周期性伤害**：每秒对宿主造成无视护甲和魔法抗性的伤害（配置项：spore_poison_damage）。
 * 2. **属性减益**：每层效果降低宿主的移动速度和攻击速度（配置项：spore_speed_reduction）。
 * 3. **热重载支持**：重写了属性修饰值的计算逻辑，直接读取配置，确保数值调整即时生效。
 * <p>
 * English Description:
 * Flammable Spores Effect Definition Class
 * This is a "double-edged sword" status and the core of the shift from numerical combat to status-based gameplay.
 * <p>
 * Key Features:
 * 1. **Periodic Damage**: Deals damage ignoring armor and magic resistance to the host every second (Config: spore_poison_damage).
 * 2. **Attribute Debuff**: Reduces movement speed and attack speed per stack (Config: spore_speed_reduction).
 * 3. **Hot-Reload Support**: Overrides attribute modifier calculation to read directly from config, ensuring real-time updates.
 */
public class FlammableSporesEffect extends MobEffect {

    // 唯一的属性修饰符 UUID，用于标识孢子带来的减益
    // Unique Attribute Modifier UUIDs to identify debuffs caused by Spores
    private static final UUID SPEED_MODIFIER_UUID = UUID.fromString("7107DE5E-7CE8-4030-940E-514C1F160890");
    private static final UUID ATTACK_SPEED_MODIFIER_UUID = UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");

    /**
     * 构造函数。
     * 定义效果为有害（HARMFUL），颜色为深绿色（0x2E8B57）。
     * 同时在此处注册默认的属性修饰符（移动速度和攻击速度）。
     * <p>
     * Constructor.
     * Defines the effect as HARMFUL, with a Sea Green color (0x2E8B57).
     * Registers default attribute modifiers (Movement Speed and Attack Speed) here.
     */
    public FlammableSporesEffect() {
        super(MobEffectCategory.HARMFUL, 0x2E8B57);

        // 注册移动速度修饰符 (MULTIPLY_TOTAL 表示最终乘算)
        // 这里的数值 (-0.1) 只是占位符，实际值由 getAttributeModifierValue 动态决定
        // Register Movement Speed modifier (MULTIPLY_TOTAL means final multiplication)
        // The value (-0.1) here is just a placeholder; the actual value is determined dynamically by getAttributeModifierValue
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, SPEED_MODIFIER_UUID.toString(),
                -0.1, AttributeModifier.Operation.MULTIPLY_TOTAL);

        // 注册攻击速度修饰符
        // Register Attack Speed modifier
        this.addAttributeModifier(Attributes.ATTACK_SPEED, ATTACK_SPEED_MODIFIER_UUID.toString(),
                -0.1, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    /**
     * 每 Tick 执行的逻辑。
     * 负责处理周期性的伤害。
     * 注：传染逻辑 (Contagion) 位于 ReactionHandler.onLivingTick 中处理，以优化性能。
     * <p>
     * Per-tick logic.
     * Handles periodic damage.
     * Note: Contagion logic is handled in ReactionHandler.onLivingTick for performance optimization.
     *
     * @param pLivingEntity 拥有该效果的实体 / The entity with the effect
     * @param pAmplifier    效果等级 (0 = Lv1) / Effect amplifier
     */
    @Override
    public void applyEffectTick(LivingEntity pLivingEntity, int pAmplifier) {
        // 仅在服务端执行，且每秒（20 ticks）触发一次
        // Run only on server side, triggers once every second (20 ticks)
        if (!pLivingEntity.level().isClientSide && (pLivingEntity.tickCount % 20 == 0)) {
            double damagePerStack = ElementalReactionConfig.sporePoisonDamage;

            // 造成伤害
            // Deal damage
            if (damagePerStack > 0) {
                // 伤害随层数线性叠加：基础伤害 * (层数 + 1)
                // Damage scales linearly with stacks: Base Damage * (Stacks + 1)
                float totalDamage = (float) (damagePerStack * (pAmplifier + 1));

                // 使用 Wither (凋零) 伤害源。
                // 凋零伤害特性：无视护甲，且不属于 Magic 类型，因此能无视女巫等生物的魔法抗性。
                // Use Wither damage source.
                // Wither damage traits: Bypasses armor and is NOT classified as Magic, ignoring magic resistance of mobs like Witches.
                pLivingEntity.hurt(pLivingEntity.damageSources().wither(), totalDamage);
            }
        }
    }

    /**
     * 检查是否需要执行 applyEffectTick。
     * 这里返回 true 以支持周期性伤害。
     * <p>
     * Checks if applyEffectTick should be executed.
     * Returns true to support periodic damage.
     *
     * @param pDuration    剩余时间 / Remaining duration
     * @param pAmplifier   效果等级 / Effect amplifier
     * @return 是否执行 / Whether to execute
     */
    @Override
    public boolean isDurationEffectTick(int pDuration, int pAmplifier) {
        return true;
    }

    /**
     * 动态计算属性修饰值。
     * 这是实现配置热重载的关键。标准 MobEffect 会使用注册时的固定值，
     * 而这里我们会忽略 modifier 中的预设值，转而读取 ElementalReactionConfig 中的实时配置。
     * <p>
     * Dynamically calculates the attribute modifier value.
     * This is key to implementing config hot-reloading. Standard MobEffect uses fixed values from registration,
     * whereas here we ignore the preset value in modifier and read the real-time config from ElementalReactionConfig.
     *
     * @param pAmplifier 效果等级 / Effect amplifier
     * @param pModifier  属性修饰符 / Attribute modifier
     * @return 计算后的修饰值 / Calculated modifier value
     */
    @Override
    public double getAttributeModifierValue(int pAmplifier, AttributeModifier pModifier) {
        // 检查是否是我们注册的减速修饰符
        // Check if it is the speed reduction modifier we registered
        if (pModifier.getId().equals(SPEED_MODIFIER_UUID) || pModifier.getId().equals(ATTACK_SPEED_MODIFIER_UUID)) {
            // 读取配置中的单层减益比例 (例如 0.1 表示 10%)
            // Read per-stack reduction ratio from config (e.g., 0.1 means 10%)
            double reductionPerStack = ElementalReactionConfig.sporeSpeedReduction;

            // 计算总减益： -1 * 单层比例 * (等级 + 1)
            // Calculate total debuff: -1 * Ratio Per Stack * (Amplifier + 1)
            double totalReduction = -reductionPerStack * (pAmplifier + 1);

            // 限制最大减益为 -0.95 (保留 5% 属性)，防止数值溢出或生物完全定身无法移动
            // Cap maximum debuff at -0.95 (retain 5% attribute) to prevent overflow or complete immobilization
            return Math.max(totalReduction, -0.95);
        }

        return super.getAttributeModifierValue(pAmplifier, pModifier);
    }
}