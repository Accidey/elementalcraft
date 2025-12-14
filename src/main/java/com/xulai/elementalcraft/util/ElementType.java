// src/main/java/com/xulai/elementalcraft/util/ElementType.java
package com.xulai.elementalcraft.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * ElementType 枚举定义了模组中的所有元素属性类型。
 * 包括：无属性（NONE）、自然（NATURE）、雷霆（THUNDER）、冰霜（FROST）、赤焰（FIRE）。
 * 每个元素拥有唯一的 ID 和对应的颜色，用于 Tooltip 显示、调试日志、Jade 信息等。
 *
 * ElementType enum defines all elemental attribute types in the mod.
 * Including: no element (NONE), Nature, Thunder, Frost, and Flame.
 * Each element has a unique ID and corresponding color used for tooltip display, debug logs, Jade info, etc.
 */
public enum ElementType {
    /**
     * 无元素属性（用于表示没有元素的情况）。
     *
     * No elemental attribute (used to indicate no element present).
     */
    NONE("none", ChatFormatting.WHITE),

    /**
     * 自然元素（绿色）。
     *
     * Nature element (green).
     */
    NATURE("nature", ChatFormatting.GREEN),

    /**
     * 雷霆元素（浅紫色）。
     *
     * Thunder element (light purple).
     */
    THUNDER("thunder", ChatFormatting.LIGHT_PURPLE),

    /**
     * 冰霜元素（蓝色）。
     *
     * Frost element (blue).
     */
    FROST("frost", ChatFormatting.BLUE),

    /**
     * 赤焰元素（红色）。
     *
     * Flame element (red).
     */
    FIRE("fire", ChatFormatting.RED);

    /**
     * 元素属性的唯一字符串 ID，用于配置、NBT 存储和命令解析。
     *
     * Unique string ID for the elemental attribute, used in configuration, NBT storage, and command parsing.
     */
    private final String id;

    /**
     * 元素属性对应的颜色（ChatFormatting 枚举）。
     *
     * Corresponding color for the elemental attribute (ChatFormatting enum).
     */
    private final ChatFormatting color;

    /**
     * 枚举构造函数，初始化 ID 和颜色。
     *
     * Enum constructor to initialize ID and color.
     *
     * @param id 元素 ID / Element ID
     * @param color 对应颜色 / Corresponding color
     */
    ElementType(String id, ChatFormatting color) {
        this.id = id;
        this.color = color;
    }

    /**
     * 获取元素属性的字符串 ID。
     *
     * Get the string ID of the elemental attribute.
     *
     * @return 元素 ID / Element ID
     */
    public String getId() {
        return id;
    }

    /**
     * 获取元素属性对应的颜色。
     *
     * Get the corresponding color of the elemental attribute.
     *
     * @return ChatFormatting 颜色 / ChatFormatting color
     */
    public ChatFormatting getColor() {
        return color;
    }

    /**
     * 获取元素属性的本地化显示名称（带颜色样式）。
     *
     * Get the localized display name of the elemental attribute (with color style).
     *
     * @return 带颜色的本地化 Component / Localized Component with color
     */
    public Component getDisplayName() {
        return Component.translatable("element." + id).withStyle(color);
    }

    /**
     * 根据字符串 ID 反查对应的元素类型。
     * 用于从配置、NBT 或命令输入中恢复元素类型。
     *
     * Look up the corresponding ElementType by string ID.
     * Used to recover element type from configuration, NBT, or command input.
     *
     * @param id 元素 ID 字符串 / Element ID string
     * @return 对应的 ElementType 或 null（无效 ID） / Corresponding ElementType or null (invalid ID)
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