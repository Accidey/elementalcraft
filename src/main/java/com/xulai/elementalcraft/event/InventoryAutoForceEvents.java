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
 * 为了性能优化，扫描频率限制为每秒一次。
 *
 * English description:
 * This class listens to server-side player tick events and scans the player's inventory.
 * It automatically applies forced attributes (such as elemental enchantments or resistances)
 * to items defined in the configuration. This covers all acquisition methods,
 * including smithing table, crafting table, furnace, item pickup, chest loot, and command-given items.
 * Scan frequency is limited to once per second for performance optimization.
 */
@Mod.EventBusSubscriber(modid = "elementalcraft")
public class InventoryAutoForceEvents {

    /** NBT 标记，用于避免对同一物品重复应用强制属性 / NBT flag to prevent re-processing the same item */
    private static final String TAG_FORCED = "elementalcraft_forced";

    /**
     * 监听玩家 Tick 事件（仅在 END 阶段处理），扫描玩家背包并应用强制属性。
     * 包含性能优化：每 20 ticks (1秒) 执行一次。
     *
     * Listen to player tick event (process only in END phase), scan inventory and apply forced attributes.
     * Includes performance optimization: Execute once every 20 ticks (1 second).
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player == null || player.level().isClientSide()) return;

        // 性能优化：每秒仅检查一次（20 ticks）
        // Performance optimization: Check only once per second (20 ticks)
        if (player.tickCount % 20 != 0) return;

        processList(player.getInventory().items, player);   // 主物品栏 / Main inventory
        processList(player.getInventory().armor, player);   // 护甲栏 / Armor slots
        processList(player.getInventory().offhand, player); // 副手栏 / Offhand slot
    }

    /**
     * 遍历指定物品列表，检查并应用强制属性。
     *
     * Iterate through the given item list, check and apply forced attributes.
     */
    private static void processList(List<ItemStack> stacks, Player player) {
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;

            // 如果已经处理过（有 NBT 标记），则跳过
            // Skip if already processed (has NBT flag)
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.getBoolean(TAG_FORCED)) continue;

            // 检查该物品是否在强制配置中（包括武器和护甲）
            // Check if item is in forced configuration (weapon or armor)
            boolean shouldForce =
                    ForcedItemHelper.getForcedWeapon(stack.getItem()) != null
                    || ForcedItemHelper.getForcedArmor(stack.getItem()) != null;
            if (!shouldForce) continue;

            // 应用强制属性
            // Apply forced attributes
            applyForcedAttributes(stack);
            
            // 标记为已处理，防止后续重复计算
            // Mark as processed to prevent duplicate calculations later
            stack.getOrCreateTag().putBoolean(TAG_FORCED, true);
            ElementalCraft.LOGGER.debug("[ElementalCraft] Auto-forced attributes applied to: {}", stack.getItem());
        }
    }

    /**
     * 根据强制物品配置为指定物品栈应用元素附魔。
     * 仅在对应附魔不存在时才添加，避免覆盖玩家已有的附魔。
     *
     * Apply elemental enchantments to the given item stack based on forced item configuration.
     * Only add if the corresponding enchantment is not already present, to avoid overriding player-obtained enchantments.
     *
     * @param stack 要处理的物品栈 / Item stack to process
     */
    public static void applyForcedAttributes(ItemStack stack) {
        if (stack.isEmpty()) return;

        boolean applied = false;

        // ==================== 处理强制武器攻击属性 / Handle forced weapon attack attribute ====================
        ForcedItemHelper.WeaponData weaponData = ForcedItemHelper.getForcedWeapon(stack.getItem());
        if (weaponData != null && weaponData.attackType() != null) {
            // 检查是否已经存在任意一种元素攻击附魔 / Check if any elemental attack enchantment already exists
            boolean hasAnyAttackEnchant = false;
            for (ElementType type : ElementType.values()) {
                if (type == ElementType.NONE) continue;
                Enchantment ench = getAttackEnchantment(type);
                if (ench != null && EnchantmentHelper.getTagEnchantmentLevel(ench, stack) > 0) {
                    hasAnyAttackEnchant = true;
                    break;
                }
            }

            // 如果没有元素附魔，则添加强制的攻击属性
            // If no elemental enchantment exists, add the forced attack attribute
            if (!hasAnyAttackEnchant) {
                Enchantment ench = getAttackEnchantment(weaponData.attackType());
                if (ench != null) {
                    stack.enchant(ench, 1);
                    applied = true;
                }
            }
        }

        // ==================== 处理强制装备强化和抗性属性 / Handle forced armor enhancement and resistance attributes ====================
        ForcedItemHelper.ArmorData armorData = ForcedItemHelper.getForcedArmor(stack.getItem());
        if (armorData != null) {
            Map<Enchantment, Integer> map = new HashMap<>();

            // 处理强化属性（仅当该元素强化附魔不存在时添加） / Handle enhancement (add only if missing)
            if (armorData.enhanceType() != null && armorData.enhancePoints() > 0) {
                Enchantment ench = getEnhancementEnchantment(armorData.enhanceType());
                // 检查是否已有该附魔，若无则添加
                if (ench != null && EnchantmentHelper.getTagEnchantmentLevel(ench, stack) == 0) {
                    int level = Math.max(1, Math.min(5, armorData.enhancePoints() / ElementalConfig.getStrengthPerLevel()));
                    map.put(ench, level);
                }
            }

            // 处理抗性属性（仅当该元素抗性附魔不存在时添加） / Handle resistance (add only if missing)
            if (armorData.resistType() != null && armorData.resistPoints() > 0) {
                Enchantment ench = getResistanceEnchantment(armorData.resistType());
                // 检查是否已有该附魔，若无则添加
                if (ench != null && EnchantmentHelper.getTagEnchantmentLevel(ench, stack) == 0) {
                    int level = Math.max(1, Math.min(5, armorData.resistPoints() / ElementalConfig.getResistPerLevel()));
                    map.put(ench, level);
                }
            }

            // 应用附魔 / Apply enchantments
            if (!map.isEmpty()) {
                EnchantmentHelper.setEnchantments(map, stack);
                applied = true;
            }
        }

        // 如果有任何强制属性被应用，标记物品 / Mark item if any forced attribute was applied
        if (applied) {
            stack.getOrCreateTag().putBoolean(TAG_FORCED, true);
        }
    }

    /** 根据元素类型获取对应的攻击附魔实例 / Get attack enchantment instance by element type */
    private static Enchantment getAttackEnchantment(ElementType type) {
        return switch (type) {
            case FIRE -> ModEnchantments.FIRE_STRIKE.get();
            case FROST -> ModEnchantments.FROST_STRIKE.get();
            case THUNDER -> ModEnchantments.THUNDER_STRIKE.get();
            case NATURE -> ModEnchantments.NATURE_STRIKE.get();
            default -> null;
        };
    }

    /** 根据元素类型获取对应的强化附魔实例 / Get enhancement enchantment instance by element type */
    private static Enchantment getEnhancementEnchantment(ElementType type) {
        return switch (type) {
            case FIRE -> ModEnchantments.FIRE_ENHANCE.get();
            case FROST -> ModEnchantments.FROST_ENHANCE.get();
            case THUNDER -> ModEnchantments.THUNDER_ENHANCE.get();
            case NATURE -> ModEnchantments.NATURE_ENHANCE.get();
            default -> null;
        };
    }

    /** 根据元素类型获取对应的抗性附魔实例 / Get resistance enchantment instance by element type */
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