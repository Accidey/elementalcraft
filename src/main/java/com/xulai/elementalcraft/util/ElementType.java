// src/main/java/com/xulai/elementalcraft/util/ElementType.java
package com.xulai.elementalcraft.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * ElementType
 * <p>
 * 中文说明：
 * 元素类型枚举。
 * 定义了模组中所有的元素属性类型（自然、雷霆、冰霜、赤焰）以及无属性状态。
 * 每个元素包含唯一的字符串 ID 和对应的聊天颜色，用于数据存储、配置解析及 UI 显示。
 * <p>
 * English Description:
 * Element Type Enum.
 * Defines all elemental attribute types (Nature, Thunder, Frost, Fire) and the non-elemental state in the mod.
 * Each element contains a unique string ID and a corresponding chat color, used for data storage, configuration parsing, and UI display.
 */
public enum ElementType {
    /**
     * 无元素属性。
     * 用于表示物品或生物没有激活任何元素属性的情况。
     * 颜色：白色。
     * <p>
     * No elemental attribute.
     * Used to indicate that an item or mob has no active elemental attributes.
     * Color: White.
     */
    NONE("none", ChatFormatting.WHITE),

    /**
     * 自然元素。
     * 代表生命与自然的属性。
     * 颜色：绿色。
     * <p>
     * Nature element.
     * Represents the attribute of life and nature.
     * Color: Green.
     */
    NATURE("nature", ChatFormatting.GREEN),

    /**
     * 雷霆元素。
     * 代表雷电与能量的属性。
     * 颜色：浅紫色。
     * <p>
     * Thunder element.
     * Represents the attribute of lightning and energy.
     * Color: Light Purple.
     */
    THUNDER("thunder", ChatFormatting.LIGHT_PURPLE),

    /**
     * 冰霜元素。
     * 代表寒冷与冻结的属性。
     * 颜色：蓝色。
     * <p>
     * Frost element.
     * Represents the attribute of cold and freezing.
     * Color: Blue.
     */
    FROST("frost", ChatFormatting.BLUE),

    /**
     * 赤焰元素。
     * 代表火焰与热量的属性。
     * 颜色：红色。
     * <p>
     * Fire element.
     * Represents the attribute of fire and heat.
     * Color: Red.
     */
    FIRE("fire", ChatFormatting.RED);

    /**
     * 元素属性的唯一字符串 ID。
     * 用于配置文件、NBT 数据存储和命令参数解析。
     * <p>
     * Unique string ID for the elemental attribute.
     * Used for configuration files, NBT data storage, and command argument parsing.
     */
    private final String id;

    /**
     * 元素属性对应的文本颜色。
     * 用于 Tooltip、Jade 显示和调试信息。
     * <p>
     * Corresponding text color for the elemental attribute.
     * Used for tooltips, Jade display, and debug information.
     */
    private final ChatFormatting color;

    /**
     * 枚举构造函数。
     * 初始化元素的 ID 和颜色。
     * <p>
     * Enum constructor.
     * Initializes the element's ID and color.
     *
     * @param id    元素 ID / Element ID
     * @param color 对应颜色 / Corresponding color
     */
    ElementType(String id, ChatFormatting color) {
        this.id = id;
        this.color = color;
    }

    /**
     * 获取元素属性的字符串 ID。
     * <p>
     * Gets the string ID of the elemental attribute.
     *
     * @return 元素 ID / Element ID
     */
    public String getId() {
        return id;
    }

    /**
     * 获取元素属性对应的颜色。
     * <p>
     * Gets the corresponding color of the elemental attribute.
     *
     * @return ChatFormatting 颜色枚举 / ChatFormatting color enum
     */
    public ChatFormatting getColor() {
        return color;
    }

    /**
     * 获取元素属性的本地化显示名称组件。
     * 已应用对应的颜色样式。
     * 键名格式：element.{id} (例如 element.fire)
     * <p>
     * Gets the localized display name component for the elemental attribute.
     * Applies the corresponding color style.
     * Key format: element.{id} (e.g., element.fire)
     *
     * @return 带颜色的本地化组件 / Localized component with color
     */
    public Component getDisplayName() {
        return Component.translatable("element." + id).withStyle(color);
    }

    /**
     * 根据字符串 ID 反查对应的元素类型。
     * 常用于从配置文件、NBT 数据或命令输入中恢复元素类型对象。
     * <p>
     * Looks up the corresponding ElementType by string ID.
     * Commonly used to recover ElementType objects from configuration files, NBT data, or command inputs.
     *
     * @param id 元素 ID 字符串 / Element ID string
     * @return 对应的 ElementType，如果 ID 无效或为空则返回 null / Corresponding ElementType, or null if ID is invalid or empty
     */
    public static ElementType fromId(String id) {
        if (id == null || id.isEmpty()) return null;
        for (ElementType type : values()) {
            if (type.getId().equals(id)) {
                return type;
            }
        }
        return null;
    }
}