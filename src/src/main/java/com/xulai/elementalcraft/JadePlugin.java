// src/main/java/com/xulai/elementalcraft/JadePlugin.java
package com.xulai.elementalcraft;

import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * JadePlugin
 * <p>
 * 中文说明：
 * 模组对 Jade (Waila 分支) 的集成插件。
 * 负责在实体的 HUD 信息栏（Tooltip）中显示生物的元素属性信息。
 * 包括：攻击属性（检查主副手）、属性强化总值（所有装备累加）和属性抗性总值（所有装备累加）。
 * <p>
 * English Description:
 * Integration plugin for Jade (The Jade Waila Fork).
 * Responsible for displaying the elemental attribute information of mobs in the entity HUD tooltip.
 * Includes: Attack Element (checks main/offhand), Total Enhancement (sum of all equipment), and Total Resistance (sum of all equipment).
 */
@WailaPlugin
public class JadePlugin implements IWailaPlugin {

    /**
     * 服务端注册方法。
     * 本模组无需服务端数据同步（属性数据已通过 NBT 或附魔同步到客户端），因此留空。
     * <p>
     * Server-side registration method.
     * Left empty as this mod does not require specific server-side data syncing for Jade (attribute data is already synced via NBT or enchantments).
     *
     * @param registration Jade 服务端注册器 / Jade server registration
     */
    @Override
    public void register(IWailaCommonRegistration registration) {
    }

    /**
     * 客户端注册方法。
     * 为 LivingEntity（生物实体）注册元素属性信息提供器。
     * <p>
     * Client-side registration method.
     * Registers the elemental info provider for LivingEntity.
     *
     * @param registration Jade 客户端注册器 / Jade client registration
     */
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(Provider.INSTANCE, LivingEntity.class);
    }

    /**
     * 内部枚举，实现 IEntityComponentProvider 接口。
     * 负责构建实体 Tooltip 的具体内容。
     * <p>
     * Internal enum implementing IEntityComponentProvider interface.
     * Responsible for building the specific content of the entity tooltip.
     */
    private enum Provider implements IEntityComponentProvider {
        INSTANCE;

        /**
         * 获取该提供器的唯一 ID。
         * <p>
         * Gets the unique ID of this provider.
         *
         * @return 提供器资源位置 ID / Provider ResourceLocation ID
         */
        @Override
        public ResourceLocation getUid() {
            return ResourceLocation.fromNamespaceAndPath(ElementalCraft.MODID, "elemental_info");
        }

        /**
         * 在 Jade Tooltip 中追加元素属性信息。
         * 逻辑顺序：
         * 1. 攻击属性：优先显示主手，若无则显示副手。
         * 2. 属性强化：遍历所有元素，显示总强化点数（>0时）。
         * 3. 属性抗性：遍历所有元素，显示总抗性点数（>0时）。
         * <p>
         * Appends elemental attribute information to the Jade tooltip.
         * Logic order:
         * 1. Attack Element: Prioritizes main hand, then offhand.
         * 2. Enhancement: Iterates all elements, displays total enhancement points (if > 0).
         * 3. Resistance: Iterates all elements, displays total resistance points (if > 0).
         *
         * @param tooltip  Jade Tooltip 构建器 / Jade tooltip builder
         * @param accessor 实体访问器 / Entity accessor
         * @param config   Jade 插件配置 / Jade plugin config
         */
        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (!(accessor.getEntity() instanceof LivingEntity living)) return;

            // 1. 显示攻击属性（检查主手和副手，只要任意一只手有攻击附魔即显示）
            // 1. Display attack element (check main hand and off hand)
            ElementType attack = ElementUtils.getAttackElement(living.getMainHandItem());
            if (attack == ElementType.NONE) {
                attack = ElementUtils.getAttackElement(living.getOffhandItem());
            }

            if (attack != null && attack != ElementType.NONE) {
                ChatFormatting c = attack.getColor();
                tooltip.add(Component.translatable("jade.elementalcraft.attack_title")
                        .withStyle(ChatFormatting.BOLD, c));
                tooltip.add(Component.literal("  ")
                        .append(getElementSymbol(attack))
                        .append(attack.getDisplayName())
                        .append(Component.translatable("jade.elementalcraft.attack_suffix"))
                        .withStyle(c));
            }

            // 2. 显示属性强化（遍历所有装备累加总点数）
            // 2. Display enhancement (sum points across all equipment)
            int totalEnhance = 0;
            ElementType enhanceType = ElementType.NONE;

            for (ElementType t : ElementType.values()) {
                if (t == ElementType.NONE) continue;
                // 获取用于显示的总点数（不受单件上限限制）
                // Get display total points (not capped by per-piece limit)
                int val = ElementUtils.getDisplayEnhancement(living, t);
                if (val > 0) {
                    totalEnhance = val;
                    enhanceType = t;
                    break;  // 强化通常只有一种主导元素 / Enhancement usually has only one dominant element
                }
            }

            if (totalEnhance > 0) {
                ChatFormatting c = enhanceType.getColor();
                tooltip.add(Component.translatable("jade.elementalcraft.strengths_title")
                        .withStyle(ChatFormatting.BOLD, c));
                tooltip.add(Component.literal("  ")
                        .append(getElementSymbol(enhanceType))
                        .append(enhanceType.getDisplayName())
                        .append(Component.translatable("jade.elementalcraft.strength_prefix"))
                        .append(Component.literal(String.valueOf(totalEnhance)))
                        .withStyle(c));
            }

            // 3. 显示属性抗性（遍历所有装备累加总点数）
            // 3. Display resistance (sum points across all equipment)
            boolean headerAdded = false;

            for (ElementType t : ElementType.values()) {
                if (t == ElementType.NONE) continue;

                int val = ElementUtils.getDisplayResistance(living, t);
                if (val <= 0) continue;

                ChatFormatting c = t.getColor();

                if (!headerAdded) {
                    tooltip.add(Component.translatable("jade.elementalcraft.resistances_title")
                            .withStyle(ChatFormatting.BOLD, c));
                    headerAdded = true;
                }

                tooltip.add(Component.literal("  ")
                        .append(getElementSymbol(t))
                        .append(t.getDisplayName())
                        .append(Component.translatable("jade.elementalcraft.resist_prefix"))
                        .append(Component.literal(String.valueOf(val)))
                        .withStyle(c));
            }
        }

        /**
         * 获取元素对应的符号字符串。
         * 使用 Java Unicode 转义序列以确保在不同环境下的显示一致性。
         * <p>
         * Gets the symbol string corresponding to the element.
         * Uses Java Unicode escape sequences to ensure display consistency across different environments.
         *
         * @param type 元素类型 / Element Type
         * @return 符号字符串 (带空格) / Symbol string (with space)
         */
        private String getElementSymbol(ElementType type) {
            return switch (type) {
                case FIRE -> "\uD83D\uDD25 ";   // 🔥 Fire
                case NATURE -> "\uD83C\uDF3F "; // 🌿 Herb
                case THUNDER -> "\u26A1 ";      // ⚡ Lightning
                case FROST -> "\u2744 ";        // ❄ Snowflake
                default -> "";
            };
        }
    }
}