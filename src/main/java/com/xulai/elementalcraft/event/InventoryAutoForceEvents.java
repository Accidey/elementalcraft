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
 * 包含了属性同步逻辑：如果配置发生变化或物品被移除配置，会自动更新或清除属性。
 *
 * English description:
 * This class listens to server-side player tick events and scans the player's inventory.
 * It automatically applies forced attributes (such as elemental enchantments or resistances)
 * to items defined in the configuration.
 * Includes attribute synchronization logic: automatically updates or clears attributes if the configuration changes or the item is removed from config.
 */
@Mod.EventBusSubscriber(modid = "elementalcraft")
public class InventoryAutoForceEvents {

    /** NBT 标记，用于标记该物品已被强制属性系统接管 / NBT flag indicating the item is managed by the forced attribute system */
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
     * 遍历指定物品列表，检查并应用或移除强制属性。
     *
     * Iterate through the given item list, check and apply or remove forced attributes.
     */
    private static void processList(List<ItemStack> stacks, Player player) {
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;

            // 检查该物品是否在强制配置中（包括武器和护甲）
            // Check if item is in forced configuration (weapon or armor)
            boolean isForced =
                    ForcedItemHelper.getForcedWeapon(stack.getItem()) != null
                    || ForcedItemHelper.getForcedArmor(stack.getItem()) != null;

            CompoundTag tag = stack.getTag();
            boolean wasForced = tag != null && tag.getBoolean(TAG_FORCED);

            if (isForced) {
                // 如果物品在配置中，强制应用（同步）属性
                // If item is in config, enforce (sync) attributes
                applyForcedAttributes(stack, wasForced);
            } else if (wasForced) {
                // 如果物品不在配置中，但有强制标记，说明配置已被移除，需要清理属性
                // If item is not in config but has forced tag, config was removed, cleanup attributes
                removeForcedAttributes(stack);
                stack.removeTagKey(TAG_FORCED);
                ElementalCraft.LOGGER.debug("[ElementalCraft] Removed forced attributes from: {}", stack.getItem());
            }
        }
    }

    /**
     * 根据强制物品配置为指定物品栈应用元素附魔。
     * 支持属性更新：如果配置变更，会自动替换旧的元素附魔。
     *
     * Apply elemental enchantments to the given item stack based on forced item configuration.
     * Supports attribute update: automatically replaces old elemental enchantments if config changes.
     *
     * @param stack 要处理的物品栈 / Item stack to process
     * @param isTracked 是否已有强制标记 / Whether already tracked by forced tag
     */
    public static void applyForcedAttributes(ItemStack stack, boolean isTracked) {
        if (stack.isEmpty()) return;

        boolean changed = false;
        Map<Enchantment, Integer> currentEnchants = EnchantmentHelper.getEnchantments(stack);

        // ==================== 处理强制武器攻击属性 / Handle forced weapon attack attribute ====================
        ForcedItemHelper.WeaponData weaponData = ForcedItemHelper.getForcedWeapon(stack.getItem());
        if (weaponData != null && weaponData.attackType() != null) {
            Enchantment targetEnchant = getAttackEnchantment(weaponData.attackType());
            
            if (targetEnchant != null) {
                // 检查是否存在其他（旧的）元素攻击附魔
                // Check for other (old) elemental attack enchantments
                Enchantment currentAttackEnchant = null;
                for (ElementType type : ElementType.values()) {
                    if (type == ElementType.NONE) continue;
                    Enchantment e = getAttackEnchantment(type);
                    if (currentEnchants.containsKey(e)) {
                        currentAttackEnchant = e;
                        break;
                    }
                }

                if (isTracked) {
                    // 如果已被追踪（即属性由模组管理），强制同步配置
                    // If tracked (managed by mod), force sync with config
                    if (currentAttackEnchant != targetEnchant) {
                        if (currentAttackEnchant != null) currentEnchants.remove(currentAttackEnchant);
                        currentEnchants.put(targetEnchant, 1);
                        changed = true;
                    }
                } else {
                    // 如果是首次处理，仅在无冲突时添加（避免覆盖玩家手动附魔）
                    // If first time, only add if no conflict (avoid overwriting player manual enchant)
                    if (currentAttackEnchant == null) {
                        currentEnchants.put(targetEnchant, 1);
                        changed = true;
                    }
                }
            }
        }

        // ==================== 处理强制装备强化和抗性属性 / Handle forced armor enhancement and resistance attributes ====================
        ForcedItemHelper.ArmorData armorData = ForcedItemHelper.getForcedArmor(stack.getItem());
        if (armorData != null) {
            // 处理强化属性 / Handle enhancement
            if (armorData.enhanceType() != null && armorData.enhancePoints() > 0) {
                Enchantment targetEnhance = getEnhancementEnchantment(armorData.enhanceType());
                int level = Math.max(1, Math.min(5, armorData.enhancePoints() / ElementalConfig.getStrengthPerLevel()));
                
                if (targetEnhance != null) {
                    if (isTracked) {
                        // 强制同步：移除不匹配的强化附魔，设置正确的
                        // Force sync: Remove mismatched enhancement, set correct one
                        for (ElementType type : ElementType.values()) {
                            if (type == ElementType.NONE) continue;
                            Enchantment e = getEnhancementEnchantment(type);
                            if (e != null && e != targetEnhance && currentEnchants.containsKey(e)) {
                                currentEnchants.remove(e);
                                changed = true;
                            }
                        }
                        if (currentEnchants.getOrDefault(targetEnhance, 0) != level) {
                            currentEnchants.put(targetEnhance, level);
                            changed = true;
                        }
                    } else {
                        // 首次处理：仅当不存在时添加
                        // First time: Add only if missing
                        boolean hasEnhance = false;
                        for (ElementType type : ElementType.values()) {
                            if (type == ElementType.NONE) continue;
                            if (currentEnchants.containsKey(getEnhancementEnchantment(type))) {
                                hasEnhance = true;
                                break;
                            }
                        }
                        if (!hasEnhance) {
                            currentEnchants.put(targetEnhance, level);
                            changed = true;
                        }
                    }
                }
            }

            // 处理抗性属性 / Handle resistance
            if (armorData.resistType() != null && armorData.resistPoints() > 0) {
                Enchantment targetResist = getResistanceEnchantment(armorData.resistType());
                int level = Math.max(1, Math.min(5, armorData.resistPoints() / ElementalConfig.getResistPerLevel()));

                if (targetResist != null) {
                    if (isTracked) {
                        // 强制同步
                        // Force sync
                        for (ElementType type : ElementType.values()) {
                            if (type == ElementType.NONE) continue;
                            Enchantment e = getResistanceEnchantment(type);
                            if (e != null && e != targetResist && currentEnchants.containsKey(e)) {
                                currentEnchants.remove(e);
                                changed = true;
                            }
                        }
                        if (currentEnchants.getOrDefault(targetResist, 0) != level) {
                            currentEnchants.put(targetResist, level);
                            changed = true;
                        }
                    } else {
                        // 首次处理
                        // First time
                        boolean hasResist = false;
                        for (ElementType type : ElementType.values()) {
                            if (type == ElementType.NONE) continue;
                            if (currentEnchants.containsKey(getResistanceEnchantment(type))) {
                                hasResist = true;
                                break;
                            }
                        }
                        if (!hasResist) {
                            currentEnchants.put(targetResist, level);
                            changed = true;
                        }
                    }
                }
            }
        }

        // 应用更改并标记 / Apply changes and mark
        if (changed) {
            EnchantmentHelper.setEnchantments(currentEnchants, stack);
            stack.getOrCreateTag().putBoolean(TAG_FORCED, true);
            ElementalCraft.LOGGER.debug("[ElementalCraft] Auto-forced attributes applied/synced to: {}", stack.getItem());
        } else if (!isTracked && (weaponData != null || armorData != null)) {
            // 即使没有改变附魔（例如已有正确附魔），也标记为已追踪，以便后续同步
            // Even if no enchant changed (e.g. already correct), mark as tracked for future sync
            stack.getOrCreateTag().putBoolean(TAG_FORCED, true);
        }
    }

    /**
     * 移除物品上所有的模组元素附魔。
     * 用于当物品不再被强制时的清理工作。
     *
     * Remove all mod elemental enchantments from the item.
     * Used for cleanup when the item is no longer forced.
     */
    private static void removeForcedAttributes(ItemStack stack) {
        Map<Enchantment, Integer> currentEnchants = EnchantmentHelper.getEnchantments(stack);
        boolean changed = false;

        for (ElementType type : ElementType.values()) {
            if (type == ElementType.NONE) continue;

            Enchantment attack = getAttackEnchantment(type);
            if (attack != null && currentEnchants.remove(attack) != null) changed = true;

            Enchantment enhance = getEnhancementEnchantment(type);
            if (enhance != null && currentEnchants.remove(enhance) != null) changed = true;

            Enchantment resist = getResistanceEnchantment(type);
            if (resist != null && currentEnchants.remove(resist) != null) changed = true;
        }

        if (changed) {
            EnchantmentHelper.setEnchantments(currentEnchants, stack);
        }
    }

    private static Enchantment getAttackEnchantment(ElementType type) {
        return switch (type) {
            case FIRE -> ModEnchantments.FIRE_STRIKE.get();
            case FROST -> ModEnchantments.FROST_STRIKE.get();
            case THUNDER -> ModEnchantments.THUNDER_STRIKE.get();
            case NATURE -> ModEnchantments.NATURE_STRIKE.get();
            default -> null;
        };
    }

    private static Enchantment getEnhancementEnchantment(ElementType type) {
        return switch (type) {
            case FIRE -> ModEnchantments.FIRE_ENHANCE.get();
            case FROST -> ModEnchantments.FROST_ENHANCE.get();
            case THUNDER -> ModEnchantments.THUNDER_ENHANCE.get();
            case NATURE -> ModEnchantments.NATURE_ENHANCE.get();
            default -> null;
        };
    }

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