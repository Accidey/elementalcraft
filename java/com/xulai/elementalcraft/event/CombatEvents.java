// src/main/java/com/xulai/elementalcraft/event/CombatEvents.java
package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.util.DebugMode;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * CombatEvents 类负责处理实体受到伤害时的元素属性伤害计算逻辑。
 * 当攻击者拥有元素攻击属性时，会根据强化、抗性、克制关系等计算额外元素伤害，并可选转为治疗。
 * 同时支持调试模式下向开启调试的玩家发送详细战斗日志。
 *
 * CombatEvents class handles the elemental attribute damage calculation logic when an entity takes damage.
 * When the attacker has an elemental attack attribute, additional elemental damage is calculated based on enhancement, resistance, restraint relationships, etc.,
 * and can optionally be converted to healing under high resistance conditions.
 * It also supports sending detailed combat logs to players with debug mode enabled.
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class CombatEvents {

    /**
     * 检查是否有任何玩家开启了属性调试模式。
     *
     * Checks if any player has elemental debug mode enabled.
     *
     * @return 是否有玩家开启调试 / Whether any player has debug enabled
     */
    private static boolean isAnyDebugEnabled() {
        return DebugMode.hasAnyDebugEnabled();
    }

    /**
     * 监听 LivingDamageEvent，处理元素属性伤害计算。
     * 仅在攻击者为活体实体且拥有有效攻击属性时触发。
     *
     * Listens to LivingDamageEvent to handle elemental attribute damage calculation.
     * Only triggers when the attacker is a living entity and has a valid attack element.
     *
     * @param event 实体伤害事件 / Living damage event
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        // 攻击者必须是活体实体 / Attacker must be a living entity
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        LivingEntity target = event.getEntity();

        // 获取攻击者的攻击属性 / Get attacker's attack element
        ElementType attackElement = ElementUtils.getAttackElement(attacker);
        if (attackElement == ElementType.NONE) return;

        // 获取目标的攻击属性（用于克制判断），若无则为 null / Get target's attack element for restraint check, null if none
        ElementType targetElement = ElementUtils.getAttackElement(target);
        if (targetElement == ElementType.NONE) targetElement = null;

        // 获取攻击者的总强化值 / Get attacker's total enhancement for the attack element
        int totalStrength = ElementUtils.getTotalEnhancement(attacker, attackElement);
        if (totalStrength <= 0) return;

        // 获取目标对该攻击属性的总抗性值 / Get target's total resistance against the attack element
        int resistance = ElementUtils.getTotalResistance(target, attackElement);

        // 计算基础元素伤害和减免 / Calculate base elemental damage and reduction
        float baseDmg = totalStrength / (float) ElementalConfig.getStrengthPerHalfDamage();
        float baseRed = resistance / (float) ElementalConfig.getResistPerHalfReduction();

        float elementalDmg = baseDmg * (float) ElementalConfig.getDamageMultiplier();
        float reduction    = baseRed * (float) ElementalConfig.getResistanceMultiplier();
        float finalEle     = Math.max(0f, elementalDmg - reduction);

        // 计算克制倍率 / Calculate restraint multiplier
        float restraintMultiplier = 1.0f;
        if (targetElement != null) {
            String forward  = attackElement.getId() + "->" + targetElement.getId();
            String backward = targetElement.getId() + "->" + attackElement.getId();
            List<? extends String> rules = ElementalConfig.ELEMENT_RESTRAINTS.get();
            if (rules.contains(forward)) {
                restraintMultiplier = ElementalConfig.RESTRAINT_MULTIPLIER.get().floatValue();
            } else if (rules.contains(backward)) {
                restraintMultiplier = ElementalConfig.WEAK_MULTIPLIER.get().floatValue();
            }
        }

        float finalWithRestraint = finalEle * restraintMultiplier;

        // 高抗性转治疗逻辑 / High resistance to healing conversion logic
        boolean convertedToHeal = false;
        float healAmount = 0f;
        if (ElementalConfig.RESISTANCE_TO_HEAL_ENABLED.get()
                && resistance >= ElementalConfig.RESISTANCE_TO_HEAL_THRESHOLD.get()
                && elementalDmg > 0f && elementalDmg <= reduction) {
            convertedToHeal = true;
            healAmount = elementalDmg * ElementalConfig.RESISTANCE_TO_HEAL_MULTIPLIER.get().floatValue();
            finalWithRestraint = 0f;
            if (target.isAlive()) target.heal(healAmount);
        }

        // 应用最终元素伤害 / Apply final elemental damage
        if (elementalDmg > 0f) {
            event.setAmount(event.getAmount() + finalWithRestraint);

            // 调试模式日志输出 / Debug mode logging
            if (isAnyDebugEnabled() && target.level() instanceof ServerLevel serverLevel) {
                Component status = Component.translatable(
                    restraintMultiplier > 1.0f ? "debug.elementalcraft.restraint" :
                    restraintMultiplier < 1.0f ? "debug.elementalcraft.weakness" :
                                                "debug.elementalcraft.neutral");

                Component msg = Component.translatable("debug.elementalcraft.damage_log",
                    attacker.getDisplayName().getString(), status,
                    String.format("%.2f", elementalDmg), String.format("%.2f", reduction),
                    String.format("%.2f", restraintMultiplier), String.format("%.2f", finalWithRestraint),
                    String.format("%.2f", event.getAmount()));

                if (convertedToHeal) {
                    msg = msg.copy().append(" ")
                        .append(Component.translatable("debug.elementalcraft.converted_to_heal"))
                        .append(Component.translatable("debug.elementalcraft.heal_amount", String.format("%.2f", healAmount)));
                }

                final Component finalMsg = msg;
                ElementalCraft.LOGGER.info("[ElementalCraft Debug] " + stripColor(finalMsg.getString()));

                // 向开启调试的玩家发送客户端消息 / Send client message to players with debug enabled
                serverLevel.getServer().getPlayerList().getPlayers().stream()
                    .filter(DebugMode::isEnabled)
                    .forEach(p -> p.displayClientMessage(Component.literal("§e[属性调试] ").append(finalMsg), false));
            }
        }
    }

    /**
     * 去除颜色代码，用于日志输出（去除 § 颜色符）。
     *
     * Strips color codes for logging (removes § color symbols).
     *
     * @param s 输入字符串 / Input string
     * @return 去除颜色后的字符串 / String without color codes
     */
    private static String stripColor(String s) {
        return s.replaceAll("§[0-9a-fk-or]", "");
    }
}