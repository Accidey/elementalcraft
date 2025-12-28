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

import java.util.List;
import java.util.Map;

/**
 * InventoryAutoForceEvents
 * <p>
 * 中文说明：
 * 负责自动应用和同步强制物品属性的事件处理类。
 * 通过监听服务端玩家的 Tick 事件，定期扫描玩家背包。
 * 如果发现配置中定义的强制物品（如特定武器或护甲），会自动为其应用对应的元素附魔。
 * 同时支持配置变更后的属性同步，以及当物品被移除配置时自动清理相关属性。
 * 使用 NBT 数据追踪系统添加的附魔，确保在移除时不会误删玩家手动添加的其他附魔。
 * <p>
 * English Description:
 * Event handler class responsible for automatically applying and synchronizing forced item attributes.
 * Listens to the server-side player Tick event to periodically scan the player's inventory.
 * If a configured forced item (e.g., specific weapon or armor) is found, the corresponding elemental enchantments are automatically applied.
 * Supports attribute synchronization upon configuration changes and automatic cleanup when items are removed from the config.
 * Uses NBT data to track enchantments added by the system, ensuring that player-added enchantments are not accidentally removed during cleanup.
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class InventoryAutoForceEvents {

    /**
     * NBT 标记：用于标记该物品已被强制属性系统接管。
     * NBT flag: Indicates the item is managed by the forced attribute system.
     */
    private static final String TAG_FORCED = "elementalcraft_forced";

    /**
     * NBT 标记：用于存储系统具体强制了哪些属性的数据，以便后续安全移除。
     * NBT tag: Stores data on which specific attributes were forced by the system for safe removal later.
     */
    private static final String TAG_FORCED_DATA = "elementalcraft_forced_data";

    /**
     * 监听玩家 Tick 事件（仅在 END 阶段处理）。
     * 扫描玩家背包并应用或更新强制属性。
     * <p>
     * Listen to player Tick event (processed only in the END phase).
     * Scans player inventory to apply or update forced attributes.
     *
     * @param event 玩家 Tick 事件 / Player Tick event
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
     * <p>
     * Iterate through the specified item list to check and apply or remove forced attributes.
     *
     * @param stacks 物品栈列表 / List of item stacks
     * @param player 玩家实体 / Player entity
     */
    private static void processList(List<ItemStack> stacks, Player player) {
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;

            // 检查该物品是否在强制配置中（包括武器和护甲）
            // Check if the item is in the forced configuration (including weapons and armor)
            boolean isForced =
                    ForcedItemHelper.getForcedWeapon(stack.getItem()) != null
                            || ForcedItemHelper.getForcedArmor(stack.getItem()) != null;

            CompoundTag tag = stack.getTag();
            boolean wasForced = tag != null && tag.getBoolean(TAG_FORCED);

            if (isForced) {
                // 如果物品在配置中，强制应用（同步）属性
                // If the item is in the config, enforce (sync) attributes
                applyForcedAttributes(stack, wasForced);
            } else if (wasForced) {
                // 如果物品不在配置中，但有强制标记，说明配置已被移除，需要清理属性
                // If the item is not in the config but has the forced tag, the config was removed, so cleanup attributes
                removeForcedAttributes(stack);
                stack.removeTagKey(TAG_FORCED);
                stack.removeTagKey(TAG_FORCED_DATA); // 清理数据标记 / Cleanup data tag
            }
        }
    }

    /**
     * 根据强制物品配置为指定物品栈应用元素附魔。
     * 支持属性更新：如果配置变更，会自动替换旧的元素附魔。
     * 会记录应用的附魔类型到 NBT，防止误删。
     * <p>
     * Apply elemental enchantments to the specified item stack based on forced item configuration.
     * Supports attribute updates: automatically replaces old elemental enchantments if the config changes.
     * Records the applied enchantment types to NBT to prevent accidental deletion.
     *
     * @param stack     要处理的物品栈 / Item stack to process
     * @param isTracked 是否已有强制标记（即是否已被系统接管过） / Whether it already has the forced tag (i.e., managed by the system)
     */
    public static void applyForcedAttributes(ItemStack stack, boolean isTracked) {
        if (stack.isEmpty()) return;

        boolean changed = false;
        Map<Enchantment, Integer> currentEnchants = EnchantmentHelper.getEnchantments(stack);
        CompoundTag forcedData = new CompoundTag(); // 用于记录本次强制的属性 / Used to record attributes forced this time

        // ==================== 处理强制武器攻击属性 / Handle forced weapon attack attribute ====================
        ForcedItemHelper.WeaponData weaponData = ForcedItemHelper.getForcedWeapon(stack.getItem());
        if (weaponData != null && weaponData.attackType() != null) {
            Enchantment targetEnchant = getAttackEnchantment(weaponData.attackType());

            if (targetEnchant != null) {
                forcedData.putString("attack", weaponData.attackType().getId()); // 记录 / Record

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
                    // If first time processing, only add if no conflict (avoid overwriting player's manual enchantments)
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
            // 处理强化属性 / Handle enhancement attribute
            if (armorData.enhanceType() != null && armorData.enhancePoints() > 0) {
                Enchantment targetEnhance = getEnhancementEnchantment(armorData.enhanceType());
                int level = Math.max(1, Math.min(5, armorData.enhancePoints() / ElementalConfig.getStrengthPerLevel()));

                if (targetEnhance != null) {
                    forcedData.putString("enhance", armorData.enhanceType().getId()); // 记录 / Record

                    if (isTracked) {
                        // 强制同步：移除不匹配的强化附魔，设置正确的
                        // Force sync: Remove mismatched enhancement, set the correct one
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

            // 处理抗性属性 / Handle resistance attribute
            if (armorData.resistType() != null && armorData.resistPoints() > 0) {
                Enchantment targetResist = getResistanceEnchantment(armorData.resistType());
                int level = Math.max(1, Math.min(5, armorData.resistPoints() / ElementalConfig.getResistPerLevel()));

                if (targetResist != null) {
                    forcedData.putString("resist", armorData.resistType().getId()); // 记录 / Record

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

        // 应用更改并更新 NBT 标记
        // Apply changes and update NBT tags
        if (changed) {
            EnchantmentHelper.setEnchantments(currentEnchants, stack);
        }

        // 始终更新追踪 NBT (即使没变也可能需要更新 data 标签以保持最新状态)
        // Always update tracking NBT (even if enchantments didn't change, data tag might need update)
        CompoundTag stackTag = stack.getOrCreateTag();
        stackTag.putBoolean(TAG_FORCED, true);
        if (!forcedData.isEmpty()) {
            stackTag.put(TAG_FORCED_DATA, forcedData);
        }
    }

    /**
     * 移除物品上所有的模组元素附魔。
     * 用于当物品不再被强制时的清理工作。
     * 优先读取 NBT 中的追踪数据，仅移除系统添加的附魔。
     * <p>
     * Remove all mod elemental enchantments from the item.
     * Used for cleanup when the item is no longer forced.
     * Prioritizes reading tracking data from NBT, removing only system-added enchantments.
     *
     * @param stack 要处理的物品栈 / Item stack to process
     */
    private static void removeForcedAttributes(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) return;

        Map<Enchantment, Integer> currentEnchants = EnchantmentHelper.getEnchantments(stack);
        boolean changed = false;

        // 如果存在追踪数据，仅移除记录在案的附魔
        // If tracking data exists, remove only recorded enchantments
        if (tag.contains(TAG_FORCED_DATA)) {
            CompoundTag data = tag.getCompound(TAG_FORCED_DATA);

            if (data.contains("attack")) {
                ElementType type = ElementType.fromId(data.getString("attack"));
                Enchantment ench = getAttackEnchantment(type);
                if (ench != null && currentEnchants.remove(ench) != null) changed = true;
            }
            if (data.contains("enhance")) {
                ElementType type = ElementType.fromId(data.getString("enhance"));
                Enchantment ench = getEnhancementEnchantment(type);
                if (ench != null && currentEnchants.remove(ench) != null) changed = true;
            }
            if (data.contains("resist")) {
                ElementType type = ElementType.fromId(data.getString("resist"));
                Enchantment ench = getResistanceEnchantment(type);
                if (ench != null && currentEnchants.remove(ench) != null) changed = true;
            }
        } else {
            // 回退逻辑：如果没有追踪数据（例如旧版本物品），为了安全起见，只能遍历清除所有元素附魔
            // Fallback logic: If no tracking data (e.g., legacy items), scan and clear all elemental enchantments for safety
            for (ElementType type : ElementType.values()) {
                if (type == ElementType.NONE) continue;

                Enchantment attack = getAttackEnchantment(type);
                if (attack != null && currentEnchants.remove(attack) != null) changed = true;

                Enchantment enhance = getEnhancementEnchantment(type);
                if (enhance != null && currentEnchants.remove(enhance) != null) changed = true;

                Enchantment resist = getResistanceEnchantment(type);
                if (resist != null && currentEnchants.remove(resist) != null) changed = true;
            }
        }

        if (changed) {
            EnchantmentHelper.setEnchantments(currentEnchants, stack);
        }
    }

    /**
     * 获取指定元素类型的攻击附魔。
     * <p>
     * Get the attack enchantment for the specified element type.
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
     * 获取指定元素类型的强化附魔。
     * <p>
     * Get the enhancement enchantment for the specified element type.
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
     * 获取指定元素类型的抗性附魔。
     * <p>
     * Get the resistance enchantment for the specified element type.
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