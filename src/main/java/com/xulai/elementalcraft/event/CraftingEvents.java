// src/main/java/com/xulai/elementalcraft/event/CraftingEvents.java
package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.enchantment.ModEnchantments;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ForcedItemHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.nbt.ByteTag;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/**
 * CraftingEvents 类负责在物品通过合成、熔炼或捡起等方式获得时，自动应用强制物品属性配置。
 * 当玩家合成、熔炼或捡起匹配配置文件的物品时，该类会根据 ForcedItemConfig 中的规则为其添加对应的元素攻击、强化或抗性附魔。
 * 这确保了强制属性在所有物品获取途径下都能生效（而不仅仅是怪物装备时）。
 *
 * CraftingEvents class is responsible for automatically applying forced item attributes when items are obtained through crafting, smelting, or picking up.
 * When a player crafts, smelts, or picks up an item that matches the rules in ForcedItemConfig, this class adds the corresponding elemental attack, enhancement, or resistance enchantments.
 * This ensures that forced attributes take effect across all item acquisition methods (not just when mobs equip them).
 */
@Mod.EventBusSubscriber(modid = "elementalcraft")
public class CraftingEvents {

    /**
     * 监听物品合成事件，当玩家合成物品时应用强制属性。
     *
     * Listens to item crafting event and applies forced attributes to the crafted item.
     *
     * @param event 物品合成事件 / Item crafted event
     */
    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        applyForcedAttributes(event.getCrafting());
    }

    /**
     * 监听物品熔炼事件，当玩家通过熔炉熔炼获得物品时应用强制属性。
     *
     * Listens to item smelting event and applies forced attributes to the smelted item.
     *
     * @param event 物品熔炼事件 / Item smelted event
     */
    @SubscribeEvent
    public static void onItemSmelted(PlayerEvent.ItemSmeltedEvent event) {
        applyForcedAttributes(event.getSmelting());
    }

    /**
     * 监听物品捡起事件，当玩家捡起物品时应用强制属性（彻底覆盖所有获取途径）。
     *
     * Listens to item pickup event and applies forced attributes when the player picks up an item (complete coverage of all acquisition methods).
     *
     * @param event 物品捡起事件 / Item pickup event
     */
    @SubscribeEvent
    public static void onItemPickup(PlayerEvent.ItemPickupEvent event) {
        applyForcedAttributes(event.getStack());
    }

    /**
     * 核心方法：根据 ForcedItemConfig 配置为指定物品栈应用强制属性附魔。
     * 会检查武器强制攻击属性和装备强制强化/抗性属性，并相应添加附魔。
     *
     * Core method: applies forced attribute enchantments to the given item stack based on ForcedItemConfig.
     * Checks for forced weapon attack attributes and forced armor enhancement/resistance attributes, adding enchantments accordingly.
     *
     * @param stack 要处理的物品栈 / Item stack to process
     */
    private static void applyForcedAttributes(ItemStack stack) {
        if (stack.isEmpty()) return;

        boolean applied = false;

        // 应用强制武器攻击属性 / Apply forced weapon attack attribute
        ForcedItemHelper.WeaponData weaponData = ForcedItemHelper.getForcedWeapon(stack.getItem());
        if (weaponData != null && weaponData.attackType() != null) {
            Enchantment ench = getAttackEnchantment(weaponData.attackType());
            if (ench != null) {
                stack.enchant(ench, 1);
                applied = true;
            }
        }

        // 应用强制装备强化和抗性属性 / Apply forced armor enhancement and resistance attributes
        ForcedItemHelper.ArmorData armorData = ForcedItemHelper.getForcedArmor(stack.getItem());
        if (armorData != null) {
            Map<Enchantment, Integer> map = new HashMap<>();

            // 处理强化属性 / Handle enhancement attribute
            if (armorData.enhanceType() != null && armorData.enhancePoints() > 0) {
                int level = Math.max(1, Math.min(5, armorData.enhancePoints() / ElementalConfig.getStrengthPerLevel()));
                Enchantment ench = getEnhancementEnchantment(armorData.enhanceType());
                if (ench != null) map.put(ench, level);
            }

            // 处理抗性属性 / Handle resistance attribute
            if (armorData.resistType() != null && armorData.resistPoints() > 0) {
                int level = Math.max(1, Math.min(5, armorData.resistPoints() / ElementalConfig.getResistPerLevel()));
                Enchantment ench = getResistanceEnchantment(armorData.resistType());
                if (ench != null) map.put(ench, level);
            }

            // 如果有任何强化或抗性附魔，应用并隐藏附魔光效
            // If any enhancement or resistance enchantments exist, apply them and hide enchantment glint
            if (!map.isEmpty()) {
                EnchantmentHelper.setEnchantments(map, stack);
                stack.addTagElement("HideFlags", ByteTag.valueOf((byte)2));
                applied = true;
            }
        }

        // 可选：标记已应用强制属性（用于未来扩展） / Optional: mark that forced attributes have been applied (for future extensions)
        // if (applied) stack.getOrCreateTag().putBoolean("elementalcraft_forced", true);
    }

    /**
     * 根据元素类型获取对应的攻击附魔实例。
     *
     * Get the corresponding attack enchantment instance based on the element type.
     *
     * @param type 元素类型 / Element type
     * @return 对应的附魔或 null / Corresponding enchantment or null
     */
    private static Enchantment getAttackEnchantment(ElementType type) {
        return switch (type) {
            case FIRE -> ModEnchantments.FIRE_STRIKE.get();
            case FROST -> ModEnchantments.FROST_STRIKE.get();
            case THUNDER -> ModEnchantments.THUNDER_STRIKE.get();
            case NATURE -> ModEnchantments.NATURE_STRIKE.get();
            default -> null;
        };
    }

    /**
     * 根据元素类型获取对应的强化附魔实例。
     *
     * Get the corresponding enhancement enchantment instance based on the element type.
     *
     * @param type 元素类型 / Element type
     * @return 对应的附魔或 null / Corresponding enchantment or null
     */
    private static Enchantment getEnhancementEnchantment(ElementType type) {
        return switch (type) {
            case FIRE -> ModEnchantments.FIRE_ENHANCE.get();
            case FROST -> ModEnchantments.FROST_ENHANCE.get();
            case THUNDER -> ModEnchantments.THUNDER_ENHANCE.get();
            case NATURE -> ModEnchantments.NATURE_ENHANCE.get();
            default -> null;
        };
    }

    /**
     * 根据元素类型获取对应的抗性附魔实例（隐藏附魔）。
     *
     * Get the corresponding resistance enchantment instance (hidden enchantment) based on the element type.
     *
     * @param type 元素类型 / Element type
     * @return 对应的附魔或 null / Corresponding enchantment or null
     */
    private static Enchantment getResistanceEnchantment(ElementType type) {
        return switch (type) {
            case FIRE -> ModEnchantments.FIRE_RESIST.get();
            case FROST -> ModEnchantments.FROST_RESIST.get();
            case THUNDER -> ModEnchantments.THUNDER_RESIST.get();
            case NATURE -> ModEnchantments.NATURE_RESIST.get();
            default -> null;
        };
    }
}