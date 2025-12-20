package com.xulai.elementalcraft;

import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
import snownee.jade.api.ui.IElementHelper;

/**
 * JadePlugin 类是模组对 Jade (The Jade Waila Fork) 的集成插件。
 * 它为活体实体（LivingEntity）添加元素属性信息显示，包括攻击属性、属性强化和属性抗性。
 * 信息仅在客户端显示，支持 Jade 的 Tooltip 系统。
 *
 * JadePlugin class is the mod's integration plugin for Jade (The Jade Waila Fork).
 * It adds elemental attribute information display for living entities (LivingEntity), including attack element, enhancement, and resistance.
 * Information is displayed only on the client side and supports Jade's tooltip system.
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
        // 本模组无需服务端数据同步 / No server-side data sync required for this mod
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
        // 为所有活体实体注册元素属性信息提供器 / Register elemental info provider for all living entities
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
         * 显示顺序：攻击属性 > 属性强化 > 属性抗性（每个类别最多显示一种）。
         * 如果存在潮湿效果，会在抗性数值后显示对应的百分比修正。
         *
         * Appends elemental attribute information to the Jade tooltip.
         * Display order: attack element > enhancement > resistance (at most one per category).
         * If wetness effect is present, displays corresponding percentage modification after resistance values.
         *
         * @param tooltip Jade Tooltip 构建器 / Jade tooltip builder
         * @param accessor 实体访问器 / Entity accessor
         * @param config Jade 插件配置 / Jade plugin config
         */
        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (!(accessor.getEntity() instanceof LivingEntity living)) return;

            // 获取元素帮助器（用于未来扩展图标等） / Get element helper (for future icon extensions)
            IElementHelper elements = tooltip.getElementHelper();

            // 获取潮湿等级 (直接读取 NBT 以避免循环依赖)
            // Get wetness level (Read NBT directly to avoid circular dependency)
            int wetnessLevel = 0;
            if (living.getPersistentData().contains("EC_WetnessLevel")) {
                wetnessLevel = living.getPersistentData().getInt("EC_WetnessLevel");
            }
            int wetnessPercent = wetnessLevel * 10; // e.g. 3 -> 30%

            // 1. 显示攻击属性（最多一种） / 1. Display attack element (at most one)
            ElementType attack = ElementUtils.getAttackElement(living);
            if (attack != null && attack != ElementType.NONE) {
                ChatFormatting c = attack.getColor();
                tooltip.add(Component.translatable("jade.elementalcraft.attack_title")
                        .withStyle(ChatFormatting.BOLD, c));
                tooltip.add(Component.literal("  ")
                        .append(getElementSymbol(attack)) // 添加符号 / Add symbol
                        .append(attack.getDisplayName())
                        .append(Component.translatable("jade.elementalcraft.attack_suffix"))
                        .withStyle(c));
            }

            // 2. 显示属性强化（最多一种） / 2. Display enhancement (at most one)
            for (ElementType t : ElementType.values()) {
                if (t == ElementType.NONE) continue;
                int val = ElementUtils.getTotalEnhancement(living, t);
                if (val <= 0) continue;

                ChatFormatting c = t.getColor();
                tooltip.add(Component.translatable("jade.elementalcraft.strengths_title")
                        .withStyle(ChatFormatting.BOLD, c));
                tooltip.add(Component.literal("  ")
                        .append(getElementSymbol(t)) // 添加符号 / Add symbol
                        .append(t.getDisplayName())
                        .append(Component.translatable("jade.elementalcraft.strength_prefix"))
                        .append(Component.literal(String.valueOf(val)))
                        .withStyle(c));
                break;
            }

            // 3. 显示属性抗性（最多一种） / 3. Display resistance (at most one)
            for (ElementType t : ElementType.values()) {
                if (t == ElementType.NONE) continue;
                int val = ElementUtils.getTotalResistance(living, t);
                if (val <= 0) continue;

                ChatFormatting c = t.getColor();
                tooltip.add(Component.translatable("jade.elementalcraft.resistances_title")
                        .withStyle(ChatFormatting.BOLD, c));
                
                MutableComponent resistLine = Component.literal("  ")
                        .append(getElementSymbol(t)) // 添加符号 / Add symbol
                        .append(t.getDisplayName())
                        .append(Component.translatable("jade.elementalcraft.resist_prefix"))
                        .append(Component.literal(String.valueOf(val)));
                
                // 潮湿修正显示逻辑
                // Wetness modification display logic
                if (wetnessLevel > 0) {
                    if (t == ElementType.THUNDER || t == ElementType.FROST) {
                        // 雷/冰：抗性削弱 (易伤) -> 显示为负修正
                        // Thunder/Frost: Resistance weakened (Vulnerability) -> Show as negative modification
                        resistLine.append(Component.translatable("jade.elementalcraft.wetness_penalty", wetnessPercent)
                                .withStyle(ChatFormatting.RED));
                    } else if (t == ElementType.FIRE) {
                        // 火：抗性增强 (减伤) -> 显示为正修正
                        // Fire: Resistance strengthened (Damage reduction) -> Show as positive modification
                        resistLine.append(Component.translatable("jade.elementalcraft.wetness_bonus", wetnessPercent)
                                .withStyle(ChatFormatting.AQUA));
                    }
                }

                tooltip.add(resistLine.withStyle(c));
                break;
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
                case FIRE:
                    return "🔥 ";
                case NATURE:
                    return "🌿 ";
                case THUNDER:
                    return "⚡ ";
                case FROST:
                    return "❄️ ";
                default:
                    return "";
            }
        }
    }
}