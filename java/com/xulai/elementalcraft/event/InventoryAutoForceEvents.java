// src/main/java/com/xulai/elementalcraft/event/InventoryAutoForceEvents.java
package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.enchantment.ModEnchantments;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ForcedItemHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * InventoryAutoForceEvents
 *
 * 中文说明：
 * 该类通过监听服务端玩家的 Tick 事件，遍历玩家背包中的物品，
 * 自动为符合配置的物品应用强制属性（如元素附魔、抗性等）。
 * 这样可以覆盖所有物品获取途径，包括锻造台、工作台、熔炉、捡起、开箱、命令给予等。
 *
 * English description:
 * This class listens to server-side player tick events and scans the player's inventory.
 * It automatically applies forced attributes (such as elemental enchantments or resistances)
 * to items defined in the configuration. This covers all acquisition methods,
 * including smithing table, crafting table, furnace, item pickup, chest loot, and command-given items.
 */
@Mod.EventBusSubscriber(modid = "elementalcraft")
public class InventoryAutoForceEvents {

    /** 我们用这个 NBT 标记避免重复处理 / NBT flag to avoid re-processing */
    private static final String TAG_FORCED = "elementalcraft_forced";

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player == null || player.level().isClientSide()) return;

        processList(player.getInventory().items, player);   // 主物品栏 / Main inventory
        processList(player.getInventory().armor, player);   // 护甲栏 / Armor slots
        processList(player.getInventory().offhand, player); // 副手 / Offhand slot
    }

    private static void processList(List<ItemStack> stacks, Player player) {
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;

            CompoundTag tag = stack.getTag();
            if (tag != null && tag.getBoolean(TAG_FORCED)) continue;

            boolean shouldForce =
                    ForcedItemHelper.getForcedWeapon(stack.getItem()) != null
                    || ForcedItemHelper.getForcedArmor(stack.getItem()) != null;

            if (!shouldForce) continue;

            applyForcedAttributes(stack);

            stack.getOrCreateTag().putBoolean(TAG_FORCED, true);
            ElementalCraft.LOGGER.debug("[ElementalCraft] Auto-forced attributes applied to: {}", stack.getItem());
        }
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
    public static void applyForcedAttributes(ItemStack stack) {
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
            if (!map.isEmpty()) {
                EnchantmentHelper.setEnchantments(map, stack);
                CompoundTag tag = stack.getOrCreateTag();
                tag.putByte("HideFlags", (byte) 2);
                applied = true;
            }
        }

        // 可选：标记已应用强制属性 / Optional: mark that forced attributes have been applied
        if (applied) stack.getOrCreateTag().putBoolean(TAG_FORCED, true);
    }

    /** 根据元素类型获取对应的攻击附魔实例 / Get attack enchantment by element type */
    private static Enchantment getAttackEnchantment(ElementType type) {
        return switch (type) {
            case FIRE -> ModEnchantments.FIRE_STRIKE.get();
            case FROST -> ModEnchantments.FROST_STRIKE.get();
            case THUNDER -> ModEnchantments.THUNDER_STRIKE.get();
            case NATURE -> ModEnchantments.NATURE_STRIKE.get();
            default -> null;
        };
    }

    /** 根据元素类型获取对应的强化附魔实例 / Get enhancement enchantment by element type */
    private static Enchantment getEnhancementEnchantment(ElementType type) {
        return switch (type) {
            case FIRE -> ModEnchantments.FIRE_ENHANCE.get();
            case FROST -> ModEnchantments.FROST_ENHANCE.get();
            case THUNDER -> ModEnchantments.THUNDER_ENHANCE.get();
            case NATURE -> ModEnchantments.NATURE_ENHANCE.get();
            default -> null;
        };
    }

    /** 根据元素类型获取对应的抗性附魔实例 / Get resistance enchantment by element type */
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
