package com.xulai.elementalcraft;

import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public class JadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(Provider.INSTANCE, LivingEntity.class);
    }

    private enum Provider implements IEntityComponentProvider {
        INSTANCE;

        @Override
        public ResourceLocation getUid() {
            return ResourceLocation.fromNamespaceAndPath(ElementalCraft.MODID, "elemental_info");
        }

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            if (!(accessor.getEntity() instanceof LivingEntity living)) return;

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

            int totalEnhance = 0;
            ElementType enhanceType = ElementType.NONE;

            for (ElementType t : ElementType.values()) {
                if (t == ElementType.NONE) continue;
                int val = ElementUtils.getDisplayEnhancement(living, t);
                if (val > 0) {
                    totalEnhance = val;
                    enhanceType = t;
                    break;
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

            Player player = accessor.getPlayer();
            if (player != null) {
                ElementType playerAttack = ElementUtils.getAttackElement(player.getMainHandItem());
                if (playerAttack == ElementType.NONE) {
                    playerAttack = ElementUtils.getAttackElement(player.getOffhandItem());
                }
                ElementType targetElement = ElementUtils.getElementType(living);

                if (playerAttack != null && playerAttack != ElementType.NONE && targetElement != ElementType.NONE) {
                    float multiplier = ElementalConfig.getRestraintMultiplier(playerAttack, targetElement);
                    ChatFormatting color;
                    String relationKey;
                    if (multiplier > 1.0f) {
                        color = ChatFormatting.GREEN;
                        relationKey = "jade.elementalcraft.element_relation.restrain";
                    } else if (multiplier < 1.0f) {
                        color = ChatFormatting.RED;
                        relationKey = "jade.elementalcraft.element_relation.weak";
                    } else {
                        color = ChatFormatting.GRAY;
                        relationKey = "jade.elementalcraft.element_relation.none";
                    }
                    tooltip.add(Component.translatable("jade.elementalcraft.element_relation.title")
                            .append(Component.literal(": "))
                            .append(Component.translatable(relationKey))
                            .withStyle(color));
                } else {
                    tooltip.add(Component.translatable("jade.elementalcraft.element_relation.title")
                            .append(Component.literal(": "))
                            .append(Component.translatable("jade.elementalcraft.element_relation.none"))
                            .withStyle(ChatFormatting.GRAY));
                }
            }
        }

        private String getElementSymbol(ElementType type) {
            return switch (type) {
                case FIRE -> "\uD83D\uDD25 ";
                case NATURE -> "\uD83C\uDF3F ";
                case THUNDER -> "\u26A1 ";
                case FROST -> "\u2744 ";
                default -> "";
            };
        }
    }
}