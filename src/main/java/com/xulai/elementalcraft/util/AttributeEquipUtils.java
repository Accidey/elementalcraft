// src/main/java/com/xulai/elementalcraft/util/AttributeEquipUtils.java
package com.xulai.elementalcraft.util;

import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.enchantment.ModEnchantments;
import net.minecraft.nbt.ByteTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.*;

/**
 * AttributeEquipUtils
 *
 * 中文说明：
 * 提供装备、附魔和数值计算的静态工具方法。
 * 包含附魔获取、点数分配算法、铁甲创建等底层逻辑。
 *
 * English description:
 * Provides static utility methods for equipment, enchantments, and value calculations.
 * Includes enchantment retrieval, point distribution algorithms, iron armor creation, etc.
 */
public class AttributeEquipUtils {

    private static final Random RANDOM = new Random();

    /**
     * 将总点数分配到多个护甲部位并转换为附魔等级。
     * 采用“基础分配 + 随机余数”的策略，确保点数分配尽可能均匀且符合总值。
     * 最大等级限制基于配置文件的点数上限动态计算。
     *
     * Distributes total points across armor pieces and converts to enchantment levels.
     * Uses a "base distribution + random remainder" strategy to ensure points are distributed as evenly as possible while adhering to the total value.
     * The maximum level limit is dynamically calculated based on the config's point cap.
     *
     * @param totalPoints 总点数 / Total points
     * @param pointsPerLevel 每级点数配置 / Configured points per level
     * @param pieceCount 护甲件数 / Number of armor pieces
     * @return 每个部位的等级数组 / Array of levels for each piece
     */
    public static int[] distributePointsToLevels(int totalPoints, int pointsPerLevel, int pieceCount) {
        // 防止除以零错误 / Prevent divide by zero error
        if (pointsPerLevel <= 0) pointsPerLevel = 1;

        // 计算所需的总等级数
        // Calculate total levels needed
        int totalLevelsNeeded = totalPoints / pointsPerLevel;
        int[] levels = new int[pieceCount];
        
        // 初始化等级数组，默认值为 0
        // Initialize level array with default value 0
        Arrays.fill(levels, 0);

        // 如果总点数不足以产生任何等级，直接返回全 0 数组
        // If total points are insufficient for any level, return array of zeros
        if (totalLevelsNeeded <= 0) return levels;

        // 计算每件装备的基础等级和剩余等级
        // Calculate base level per piece and remaining levels
        int baseLevel = totalLevelsNeeded / pieceCount;
        int remainingLevels = totalLevelsNeeded % pieceCount;

        // 为所有部位填充基础等级
        // Fill base level for all parts
        Arrays.fill(levels, baseLevel);

        // 将剩余的等级随机分配给不同的护甲部位
        // Randomly distribute the remaining levels to different armor pieces
        for (int i = 0; i < remainingLevels; i++) {
            int chosen = RANDOM.nextInt(pieceCount);
            levels[chosen]++;
        }

        // 根据配置计算允许的最大等级（总上限 / 单级点数）
        // Calculate allowed max level based on config (Total Cap / Points Per Level)
        int maxConfigLevel = ElementalConfig.getMaxStatCap() / pointsPerLevel;
        if (maxConfigLevel <= 0) maxConfigLevel = 1; // 至少允许1级 / Allow at least level 1

        // 确保每个部位的等级不超过配置允许的最大值
        // Ensure level for each piece does not exceed the max value allowed by config
        for (int i = 0; i < pieceCount; i++) {
            if (levels[i] > maxConfigLevel) levels[i] = maxConfigLevel;
        }

        return levels;
    }

    /**
     * 为物品应用攻击附魔。
     *
     * Apply attack enchantment to an item.
     *
     * @param stack 物品栈 / Item stack
     * @param type 元素类型 / Element type
     */
    public static void applyAttackEnchant(ItemStack stack, ElementType type) {
        if (stack.isEmpty() || type == null || type == ElementType.NONE) return;
        Enchantment ench = getAttackEnchantment(type);
        if (ench != null) stack.enchant(ench, 1);
    }

    /**
     * 为护甲应用强化和抗性附魔（基于点数）。
     * 自动将点数转换为等级，最大等级受配置限制。
     *
     * Apply enhancement and resistance enchantments to armor (based on points).
     * Automatically converts points to levels, max level is limited by config.
     *
     * @param stack 物品栈 / Item stack
     * @param enhType 强化元素类型 / Enhancement element type
     * @param enhPoints 强化点数 / Enhancement points
     * @param resType 抗性元素类型 / Resistance element type
     * @param resPoints 抗性点数 / Resistance points
     * @param pointsPerLevelDivider 每级点数除数 / Divider for points per level
     */
    public static void applyArmorEnchants(ItemStack stack, ElementType enhType, int enhPoints, ElementType resType, int resPoints, int pointsPerLevelDivider) {
        if (stack.isEmpty()) return;
        
        // 防止除以零 / Prevent divide by zero
        if (pointsPerLevelDivider <= 0) pointsPerLevelDivider = 1;

        // 根据配置动态计算最大等级
        // Dynamically calculate max level based on config
        int maxConfigLevel = ElementalConfig.getMaxStatCap() / pointsPerLevelDivider;
        if (maxConfigLevel <= 0) maxConfigLevel = 1;

        int enhLv = (enhPoints > 0) ? Math.max(1, Math.min(maxConfigLevel, enhPoints / pointsPerLevelDivider)) : 0;
        int resLv = (resPoints > 0) ? Math.max(1, Math.min(maxConfigLevel, resPoints / pointsPerLevelDivider)) : 0;
        
        applyArmorEnchantsLevel(stack, enhType, enhLv, resType, resLv);
    }

    /**
     * 为护甲应用强化和抗性附魔（基于等级）。
     * 合并到现有附魔中（不覆盖其他附魔），并设置 HideFlags 隐藏附魔信息，保持工具提示整洁。
     *
     * Apply enhancement and resistance enchantments to armor (based on levels).
     * Merges with existing enchantments (does not override other enchantments).
     * Also sets HideFlags to hide enchantment info, keeping tooltip clean.
     *
     * @param stack 物品栈 / Item stack
     * @param enhType 强化元素类型 / Enhancement element type
     * @param enhLv 强化等级 / Enhancement level
     * @param resType 抗性元素类型 / Resistance element type
     * @param resLv 抗性等级 / Resistance level
     */
    public static void applyArmorEnchantsLevel(ItemStack stack, ElementType enhType, int enhLv, ElementType resType, int resLv) {
        if (stack.isEmpty()) return;

        // 获取当前已有附魔
        // Get existing enchantments
        Map<Enchantment, Integer> existing = EnchantmentHelper.getEnchantments(stack);
        Map<Enchantment, Integer> newMap = new HashMap<>(existing);

        // 添加或更新强化附魔（取更高等级）
        // Add or update enhancement enchantment (take higher level)
        if (enhType != null && enhType != ElementType.NONE && enhLv > 0) {
            Enchantment ench = getEnhancementEnchantment(enhType);
            if (ench != null) {
                newMap.put(ench, Math.max(enhLv, newMap.getOrDefault(ench, 0)));
            }
        }

        // 添加或更新抗性附魔（取更高等级）
        // Add or update resistance enchantment (take higher level)
        if (resType != null && resType != ElementType.NONE && resLv > 0) {
            Enchantment ench = getResistanceEnchantment(resType);
            if (ench != null) {
                newMap.put(ench, Math.max(resLv, newMap.getOrDefault(ench, 0)));
            }
        }

        // 只有发生变化才写回，减少不必要的 NBT 操作
        // Write back only if changed, reduce unnecessary NBT operations
        if (!newMap.equals(existing)) {
            EnchantmentHelper.setEnchantments(newMap, stack);
            stack.addTagElement("HideFlags", ByteTag.valueOf((byte)2)); // 隐藏附魔显示 / Hide enchantment display
        }
    }

    /**
     * 根据索引创建对应的铁甲部件。
     * 索引顺序已调整为与 MobAttributeLogic.ARMOR_SLOTS 一致：0=头盔, 1=胸甲, 2=护腿, 3=靴子。
     *
     * Creates iron armor piece based on slot index.
     * Index order adjusted to match MobAttributeLogic.ARMOR_SLOTS: 0=Helmet, 1=Chestplate, 2=Leggings, 3=Boots.
     *
     * @param slotIndex 槽位索引 / Slot index
     * @return 铁甲物品栈 / Iron armor item stack
     */
    public static ItemStack createIronArmor(int slotIndex) {
        return switch (slotIndex) {
            case 0 -> new ItemStack(Items.IRON_HELMET);    // HEAD
            case 1 -> new ItemStack(Items.IRON_CHESTPLATE); // CHEST
            case 2 -> new ItemStack(Items.IRON_LEGGINGS);   // LEGS
            case 3 -> new ItemStack(Items.IRON_BOOTS);      // FEET
            default -> ItemStack.EMPTY;
        };
    }

    /**
     * 获取指定元素的克制（反制）元素。
     * 动态读取配置文件中的 `cachedRestraints` 列表来查找“谁克制我”。
     * 如果 A 克制 B (A->B)，那么 B 的反制元素就是 A。
     * 例如配置为 fire->nature，那么 nature 的反制元素是 fire。
     *
     * Gets the counter element for the specified element.
     * Dynamically reads the `cachedRestraints` list from config to find "who counters me".
     * If A restrains B (A->B), then B's counter element is A.
     * Example: if configured fire->nature, then nature's counter is fire.
     *
     * @param type 源元素 (作为受害者) / Source element (as victim)
     * @return 克制它的元素 (攻击者) / Countering element (attacker)
     */
    public static ElementType getCounterElement(ElementType type) {
        if (type == null || type == ElementType.NONE) return ElementType.NONE;

        List<? extends String> restraints = ElementalConfig.cachedRestraints;
        if (restraints == null || restraints.isEmpty()) return ElementType.NONE;

        // 遍历配置列表，寻找 "X->type" 的关系，其中 X 就是克制 type 的元素
        // Iterate config list, look for "X->type" relation, where X is the element countering type
        for (String relation : restraints) {
            String[] split = relation.split("->");
            if (split.length == 2) {
                String attackerId = split[0].trim();
                String victimId = split[1].trim();

                // 如果我是受害者 (victimId == type.id)，那么攻击者 (attackerId) 就是我的反制元素
                // If I am the victim (victimId == type.id), then the attacker (attackerId) is my counter
                if (victimId.equalsIgnoreCase(type.getId())) {
                    return ElementType.fromId(attackerId);
                }
            }
        }

        return ElementType.NONE;
    }

    /**
     * 随机获取一个非 NONE 的元素类型。
     *
     * Randomly gets a non-NONE element type.
     *
     * @return 随机元素类型 / Random element type
     */
    public static ElementType randomNonNoneElement() {
        ElementType[] valid = {ElementType.FIRE, ElementType.NATURE, ElementType.FROST, ElementType.THUNDER};
        return valid[RANDOM.nextInt(valid.length)];
    }

    /**
     * 获取对应元素的攻击附魔实例。
     *
     * Gets the attack enchantment instance for the corresponding element.
     */
    private static Enchantment getAttackEnchantment(ElementType type) {
        return switch (type) {
            case FIRE -> ModEnchantments.FIRE_STRIKE.get();
            case NATURE -> ModEnchantments.NATURE_STRIKE.get();
            case FROST -> ModEnchantments.FROST_STRIKE.get();
            case THUNDER -> ModEnchantments.THUNDER_STRIKE.get();
            default -> null;
        };
    }

    /**
     * 获取对应元素的强化附魔实例。
     *
     * Gets the enhancement enchantment instance for the corresponding element.
     */
    private static Enchantment getEnhancementEnchantment(ElementType type) {
        return switch (type) {
            case FIRE -> ModEnchantments.FIRE_ENHANCE.get();
            case NATURE -> ModEnchantments.NATURE_ENHANCE.get();
            case FROST -> ModEnchantments.FROST_ENHANCE.get();
            case THUNDER -> ModEnchantments.THUNDER_ENHANCE.get();
            default -> null;
        };
    }

    /**
     * 获取对应元素的抗性附魔实例。
     *
     * Gets the resistance enchantment instance for the corresponding element.
     */
    private static Enchantment getResistanceEnchantment(ElementType type) {
        return switch (type) {
            case FIRE -> ModEnchantments.FIRE_RESIST.get();
            case NATURE -> ModEnchantments.NATURE_RESIST.get();
            case FROST -> ModEnchantments.FROST_RESIST.get();
            case THUNDER -> ModEnchantments.THUNDER_RESIST.get();
            default -> null;
        };
    }
}