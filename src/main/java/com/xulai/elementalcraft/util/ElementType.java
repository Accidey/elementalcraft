package com.xulai.elementalcraft.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum ElementType {
    NONE("none", ChatFormatting.WHITE),
    NATURE("nature", ChatFormatting.GREEN),
    THUNDER("thunder", ChatFormatting.LIGHT_PURPLE),
    FROST("frost", ChatFormatting.BLUE),
    FIRE("fire", ChatFormatting.RED);

    private final String id;
    private final ChatFormatting color;

    ElementType(String id, ChatFormatting color) {
        this.id = id;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public Component getDisplayName() {
        return Component.translatable("element." + id).withStyle(color);
    }

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