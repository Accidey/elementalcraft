// src/main/java/com/xulai/elementalcraft/event/TooltipEvents.java
package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * TooltipEvents
 * <p>
 * 中文说明：
 * 负责处理物品提示框（Tooltip）的自定义显示逻辑。
 * 主要功能：
 * 1. 修改物品名称：为拥有主导属性的物品添加带有颜色的【元素名】前缀。
 * 2. 动态着色：前缀和分隔符 "●" 会跟随元素颜色变化，而物品原名保持原有颜色（如稀有度颜色）。
 * 3. 显示属性：在物品名称下方显示具体的属性强化值和抗性值。
 * <p>
 * English Description:
 * Handles custom display logic for item tooltips.
 * Main features:
 * 1. Modify Item Name: Adds a colored [Element Name] prefix to items with a dominant attribute.
 * 2. Dynamic Coloring: The prefix and the separator "●" follow the element's color, while the original item name retains its original color (e.g., rarity color).
 * 3. Display Attributes: Displays specific attribute enhancement and resistance values below the item name.
 */
@Mod.EventBusSubscriber(modid = "elementalcraft")
public class TooltipEvents {

    /**
     * 监听物品 Tooltip 事件（最高优先级）。
     * 用于处理物品名称的前缀添加逻辑。
     * 格式：[元素名] ● 物品原名
     * <p>
     * Listens to the Item Tooltip event (Highest Priority).
     * Handles the logic for adding prefixes to item names.
     * Format: [Element Name] ● Original Name
     *
     * @param event 物品 Tooltip 事件 / Item Tooltip event
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();
        if (tooltip.isEmpty() || stack.isEmpty()) return;

        // 防止原版附魔将物品名称强制染成紫色（或其他稀有度颜色覆盖），重置为物品本身的悬停名称
        // Prevent vanilla enchantments from forcing the item name to purple (or other rarity overrides), reset to the item's own hover name
        if (!stack.hasCustomHoverName()) {
            tooltip.set(0, stack.getHoverName());
        }

        // 获取物品的主导元素属性
        // Get the dominant elemental attribute of the item
        ElementType element = ElementUtils.getDominantElement(stack);
        if (element == ElementType.NONE) return;

        // 获取原始物品名称（保留所有原有样式，如颜色、斜体等）
        // Get the original item name (preserve all existing styles, such as color, italics, etc.)
        Component originalName = tooltip.get(0);

        // 构建【元素名】前缀（使用对应元素颜色并加粗）
        // Build [Element Name] prefix (Use corresponding element color and bold)
        MutableComponent prefix = Component.literal("[")
                .append(Component.translatable("element." + element.getId()))
                .append("]")
                .withStyle(style -> style.withColor(element.getColor()).withBold(true));

        // 构建分隔符 "●"（使用对应元素颜色）
        // Build separator "●" (Use corresponding element color)
        MutableComponent dot = Component.literal(" ●")
                .withStyle(style -> style.withColor(element.getColor()));

        // 拼接最终名称：前缀 + 分隔符 + 空格 + 原名称
        // Concatenate final name: Prefix + Separator + Space + Original Name
        MutableComponent finalName = Component.literal("")
                .append(prefix)
                .append(dot)
                .append(Component.literal(" "))
                .append(originalName);

        // 替换 Tooltip 的第一行（即物品名称）
        // Replace the first line of the Tooltip (i.e., the item name)
        tooltip.set(0, finalName);
    }

    /**
     * 监听物品 Tooltip 事件（最高优先级）。
     * 用于在名称下方添加具体的属性强化和抗性数值信息。
     * <p>
     * Listens to the Item Tooltip event (Highest Priority).
     * Used to add specific attribute enhancement and resistance values below the name.
     *
     * @param event 物品 Tooltip 事件 / Item Tooltip event
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTooltipLower(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();

        // ==================== 属性强化显示 / Elemental Enhancement Display ====================
        // 遍历所有元素类型，检查物品是否含有强化附魔
        // Iterate through all element types to check if the item has enhancement enchantments
        for (ElementType type : ElementType.values()) {
            if (type == ElementType.NONE) continue;
            
            int level = ElementUtils.getEnhancementLevel(stack, type);
            if (level > 0) {
                // 计算具体的强化数值：等级 * 每级数值配置
                // Calculate specific enhancement value: Level * Configured value per level
                int value = level * ElementalConfig.getStrengthPerLevel();

                // 构建显示文本，整行文字跟随对应元素颜色显示
                // Build display text, the entire line follows the corresponding element color
                MutableComponent line = Component.literal("")
                        .append(Component.translatable("tooltip.elementalcraft.enhancement"))
                        .append(" ")
                        .append(Component.translatable("element." + type.getId()))
                        .append(" ")
                        .append(Component.literal("+" + value))
                        .withStyle(style -> style.withColor(type.getColor()));

                tooltip.add(line);
            }
        }

        // ==================== 属性抗性显示 / Elemental Resistance Display ====================
        // 遍历所有元素类型，检查物品是否含有抗性附魔
        // Iterate through all element types to check if the item has resistance enchantments
        for (ElementType type : ElementType.values()) {
            if (type == ElementType.NONE) continue;
            
            int level = ElementUtils.getResistanceLevel(stack, type);
            if (level > 0) {
                // 计算具体的抗性数值：等级 * 每级数值配置
                // Calculate specific resistance value: Level * Configured value per level
                int totalValue = level * ElementalConfig.getResistPerLevel();

                // 构建显示文本，整行文字跟随对应元素颜色显示
                // Build display text, the entire line follows the corresponding element color
                MutableComponent line = Component.literal("")
                        .append(Component.translatable("tooltip.elementalcraft.resistance"))
                        .append(" ")
                        .append(Component.translatable("element." + type.getId()))
                        .append(" ")
                        .append(Component.literal("+" + totalValue))
                        .withStyle(style -> style.withColor(type.getColor()));

                tooltip.add(line);
            }
        }
    }
}