package com.xulai.elementalcraft.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public enum ElementType {
    NONE("none", ChatFormatting.WHITE, ""),
    NATURE("nature", ChatFormatting.GREEN, "\uD83C\uDF3F "),
    THUNDER("thunder", ChatFormatting.LIGHT_PURPLE, "\u26A1 "),
    FROST("frost", ChatFormatting.BLUE, "\u2744 "),
    FIRE("fire", ChatFormatting.RED, "\uD83D\uDD25 ");

    private final String id;
    private final ChatFormatting color;
    private final String symbol;

    ElementType(String id, ChatFormatting color, String symbol) {
        this.id = id;
        this.color = color;
        this.symbol = symbol;
    }

    public String getId() {
        return id;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public String getSymbol() {
        return symbol;
    }

    public Component getDisplayName() {
        return Component.translatable("element." + id);
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