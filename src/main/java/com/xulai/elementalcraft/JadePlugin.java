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
 *
 * 中文说明：
 * 模组对 Jade (The Jade Waila Fork) 的集成插件。
 * 负责在实体 Tooltip 中显示元素属性信息（攻击、强化、抗性）。
 * 已移除潮湿效果的动态修正显示，保持界面简洁。
 *
 * English description:
 * Integration plugin for Jade (The Jade Waila Fork).
 * Responsible for displaying elemental attribute information (Attack, Enhancement, Resistance) in entity tooltips.
 * Removed dynamic wetness modifier display to keep the interface clean.
 */
@WailaPlugin
public class JadePlugin implements IWailaPlugin {

    /**
     * 服务端注册方法（本模组无需服务端数据同步，留空）。
     *
     * Server-side registration method (no server-side data sync needed for this mod, left empty).
     *
     * @param registration Jade 服务端注册器 / Jade server registration
     */
    @Override
    public void register(IWailaCommonRegistration registration) {
    }

    /**
     * 客户端注册方法，为 LivingEntity 注册元素属性信息提供器。
     *
     * Client-side registration method, registers the elemental info provider for LivingEntity.
     *
     * @param registration Jade 客户端注册器 / Jade client registration
     */
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(Provider.INSTANCE, LivingEntity.class);
    }

    /**
     * 内部枚举，实现 IEntityComponentProvider 接口，提供实体 Tooltip 中的元素属性信息。
     *
     * Internal enum implementing IEntityComponentProvider to provide elemental attribute information in entity tooltips.
     */
    private enum Provider implements IEntityComponentProvider {
        INSTANCE;

        /**
         * 获取该提供器的唯一 ID。
         *
         * Gets the unique ID of this provider.
         *
         * @return 提供器 ResourceLocation / Provider ResourceLocation
         */
        @Override
        public ResourceLocation getUid() {
            return ResourceLocation.fromNamespaceAndPath(ElementalCraft.MODID, "elemental_info");
        }

        /**
         * 在 Jade Tooltip 中追加元素属性信息。
         * 显示顺序：攻击属性 > 属性强化 > 属性抗性。
         *
         * Appends elemental attribute information to the Jade tooltip.
         * Display order: attack element > enhancement > resistance.
         *
         * @param tooltip Jade Tooltip 构建器 / Jade tooltip builder
         * @param accessor 实体访问器 / Entity accessor
         * @param config Jade 插件配置 / Jade plugin config
         */
        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (!(accessor.getEntity() instanceof LivingEntity living)) return;

            // 1. 显示攻击属性（最多一种）
            // 1. Display attack element (at most one)
            ElementType attack = ElementUtils.getAttackElement(living);
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

            // 2. 显示属性强化（最多一种）
            // 2. Display enhancement (at most one)
            for (ElementType t : ElementType.values()) {
                if (t == ElementType.NONE) continue;
                int val = ElementUtils.getTotalEnhancement(living, t);
                if (val <= 0) continue;

                ChatFormatting c = t.getColor();
                tooltip.add(Component.translatable("jade.elementalcraft.strengths_title")
                        .withStyle(ChatFormatting.BOLD, c));
                tooltip.add(Component.literal("  ")
                        .append(getElementSymbol(t))
                        .append(t.getDisplayName())
                        .append(Component.translatable("jade.elementalcraft.strength_prefix"))
                        .append(Component.literal(String.valueOf(val)))
                        .withStyle(c));
                break; 
            }

            // 3. 显示属性抗性（遍历所有）
            // 3. Display resistance (iterate all)
            boolean headerAdded = false;

            for (ElementType t : ElementType.values()) {
                if (t == ElementType.NONE) continue;
                
                int val = ElementUtils.getTotalResistance(living, t);
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
         *
         * Gets the symbol string corresponding to the element.
         *
         * @param type 元素类型 / Element Type
         * @return 符号字符串 (带空格) / Symbol string (with space)
         */
        private String getElementSymbol(ElementType type) {
            switch (type) {
                case FIRE: return "🔥 ";
                case NATURE: return "🌿 ";
                case THUNDER: return "⚡ ";
                case FROST: return "❄️ ";
                default: return "";
            }
        }
    }
}