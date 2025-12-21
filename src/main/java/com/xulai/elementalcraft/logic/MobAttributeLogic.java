// src/main/java/com/xulai/elementalcraft/logic/MobAttributeLogic.java
package com.xulai.elementalcraft.logic;

import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * MobAttributeLogic
 *
 * 中文说明：
 * 包含生物属性生成的决策逻辑。
 * 处理黑名单检查、强制属性（配置/维度）、随机生成算法以及最终的属性应用流程。
 * 已对接热重载系统，并处理无护甲生物的属性承载问题。
 *
 * English description:
 * Contains the decision logic for mob attribute generation.
 * Handles blacklist checks, forced attributes (config/dimension), random generation algorithms, and the final attribute application flow.
 * Integrated with the hot-reload system and handles attribute holding for unarmored mobs.
 */
public class MobAttributeLogic {

    private static final Random RANDOM = new Random();

    // 遍历顺序改为从头盔开始优先（HEAD -> CHEST -> LEGS -> FEET）
    // Traversal order changed to prioritize helmet first (HEAD -> CHEST -> LEGS -> FEET)
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    /**
     * 处理单个生物的属性赋予逻辑。
     *
     * Processes attribute assignment logic for a single mob.
     *
     * @param mob 目标生物 / Target mob
     */
    public static void processMob(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        if (data.getBoolean("ElementalCraft_AttributesSet")) return;

        String entityId = net.minecraft.world.entity.EntityType.getKey(mob.getType()).toString();

        // 黑名单检查（使用静态缓存）
        // Blacklist check (using static cache)
        if (ElementalConfig.cachedBlacklist.contains(entityId)) {
            data.putBoolean("ElementalCraft_AttributesSet", true);
            return;
        }

        // 1. 获取强制属性配置
        // 1. Get forced attribute configuration
        ForcedAttributeHelper.ForcedData forced = ForcedAttributeHelper.getForcedData(mob.getType());

        // 2. 下界维度强制属性逻辑（使用静态缓存）
        // 2. Nether dimension forced attribute logic (using static cache)
        if (forced == null && ElementalConfig.netherForcedFire
                && mob.level().dimension() == Level.NETHER) {
            int points = ElementalConfig.netherFirePoints;
            forced = new ForcedAttributeHelper.ForcedData(
                    ElementType.FIRE, ElementType.FIRE, points,
                    ElementType.FIRE, points
            );
        }

        // 3. 末地维度强制属性逻辑（使用静态缓存）
        // 3. End dimension forced attribute logic (using static cache)
        if (forced == null && ElementalConfig.endForcedThunder
                && mob.level().dimension() == Level.END) {
            int points = ElementalConfig.endThunderPoints;
            forced = new ForcedAttributeHelper.ForcedData(
                    ElementType.THUNDER, ElementType.THUNDER, points,
                    ElementType.THUNDER, points
            );
        }

        // 应用强制属性
        // Apply forced attributes
        if (forced != null) {
            applyForcedAttributes(mob, forced);
            data.putBoolean("ElementalCraft_AttributesSet", true);
            return;
        }

        // 4. 非敌对且非强制生物跳过
        // 4. Skip non-hostile and non-forced mobs
        if (!(mob instanceof Monster)) {
            data.putBoolean("ElementalCraft_AttributesSet", true);
            return;
        }

        // 5. 全局生成概率检查（使用静态缓存）
        // 5. Global generation chance check (using static cache)
        double chance = (mob instanceof net.minecraft.world.entity.monster.Enemy) 
                ? ElementalConfig.mobChanceHostile 
                : ElementalConfig.mobChanceNeutral;
        
        if (RANDOM.nextDouble() >= chance) {
            data.putBoolean("ElementalCraft_AttributesSet", true);
            return;
        }

        // 6. 执行随机生成逻辑
        // 6. Execute random generation logic
        applyRandomAttributes(mob);
        data.putBoolean("ElementalCraft_AttributesSet", true);
    }

    /**
     * 应用随机生成的属性。
     *
     * Applies randomly generated attributes.
     */
    private static void applyRandomAttributes(Mob mob) {
        ItemStack mainHand = mob.getMainHandItem();
        ItemStack offHand = mob.getOffhandItem();
        boolean hasHandItem = !mainHand.isEmpty() || !offHand.isEmpty();

        // 确定主元素
        // Determine primary element
        ElementType mainType = BiomeAttributeBias.getBiasedElement((ServerLevel) mob.level(), mob.blockPosition());

        ElementType attackType = null;
        // 攻击属性概率检查（使用静态缓存）
        // Attack attribute chance check (using static cache)
        if (RANDOM.nextDouble() < ElementalConfig.attackChance) {
            attackType = mainType;
        }
        
        // 如果有攻击属性或手持物品，才生成强化点数
        // Generate enhancement points only if there is an attack attribute or held item
        ElementType enhanceType = mainType;
        int enhanceTotalPoints = (hasHandItem || attackType != null) ? ElementalConfig.rollMonsterStrength() : 0;

        // 确定抗性元素
        // Determine resistance element
        ElementType resistType;
        // 抗性反制概率检查（使用静态缓存）
        // Counter resistance chance check (using static cache)
        if (attackType != null && RANDOM.nextDouble() < ElementalConfig.counterResistChance) {
            resistType = AttributeEquipUtils.getCounterElement(attackType);
        } else {
            resistType = AttributeEquipUtils.randomNonNoneElement();
        }
        int resistTotalPoints = ElementalConfig.rollMonsterResist();

        // 应用武器攻击附魔
        // Apply weapon attack enchantment
        if (attackType != null) {
            if (hasHandItem) {
                if (!mainHand.isEmpty()) AttributeEquipUtils.applyAttackEnchant(mainHand, attackType);
                if (!offHand.isEmpty()) AttributeEquipUtils.applyAttackEnchant(offHand, attackType);
            } else {
                // 如果没有手持物品但获得了攻击属性，给予一把铁剑
                // If unarmed but gained attack attribute, give an Iron Sword
                ItemStack sword = new ItemStack(Items.IRON_SWORD);
                AttributeEquipUtils.applyAttackEnchant(sword, attackType);
                mob.setItemSlot(EquipmentSlot.MAINHAND, sword);
                mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
            }
        }

        // 统一护甲处理逻辑（随机属性与强制属性共用）
        // Unified armor handling logic (shared with forced attributes)
        applyArmorAttributes(mob, enhanceType, enhanceTotalPoints, resistType, resistTotalPoints);
    }

    /**
     * 应用强制属性（来自配置文件或维度规则）。
     * 使用 TickTask 延迟 1 tick 执行，以兼容其他模组的装备生成逻辑。
     * 逻辑流程：等待1tick -> 检查当前装备 -> 附魔已有装备 -> 补全缺失部位 -> 附魔新部位。
     *
     * Apply forced attributes (from config file or dimension rules).
     * Uses TickTask to delay execution by 1 tick to accommodate other mods' equipment generation logic.
     * Logic flow: Wait 1 tick -> Check current equipment -> Enchant existing -> Fill missing slots -> Enchant new slots.
     */
    private static void applyForcedAttributes(Mob mob, ForcedAttributeHelper.ForcedData data) {
        MinecraftServer server = mob.level().getServer();
        if (server == null) return;

        // 延迟 1 tick 执行，确保兼容性
        // Delay execution by 1 tick for compatibility
        server.tell(new TickTask(server.getTickCount() + 1, () -> {
            if (!mob.isAlive()) return;

            ElementType attackType = data.attackType();
            ElementType enhanceType = data.enhanceType();
            int enhancePoints = data.enhancePoints();
            ElementType resistType = data.resistType();
            int resistPoints = data.resistPoints();

            // 1. 武器逻辑处理
            // 1. Weapon logic handling
            ItemStack mainHand = mob.getMainHandItem();
            ItemStack offHand = mob.getOffhandItem();
            boolean hasWeapon = !mainHand.isEmpty() || !offHand.isEmpty();

            if (attackType != null && attackType != ElementType.NONE) {
                if (hasWeapon) {
                    if (!mainHand.isEmpty()) AttributeEquipUtils.applyAttackEnchant(mainHand, attackType);
                    if (!offHand.isEmpty()) AttributeEquipUtils.applyAttackEnchant(offHand, attackType);
                } else {
                    ItemStack sword = new ItemStack(Items.IRON_SWORD);
                    AttributeEquipUtils.applyAttackEnchant(sword, attackType);
                    mob.setItemSlot(EquipmentSlot.MAINHAND, sword);
                    mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
                }
            }

            // 统一护甲处理逻辑
            // Unified armor handling logic
            applyArmorAttributes(mob, enhanceType, enhancePoints, resistType, resistPoints);
        }));
    }

    /**
     * 统一的护甲属性应用逻辑（用于随机属性和强制属性）。
     * 实现思路：
     * 1. 强化和抗性独立处理
     * 2. 从头盔开始顺序遍历护甲槽位
     * 3. 每件装备的强化/抗性先堆到单属性上限（maxStatCap）
     * 4. 当前件堆满后，剩余点数自动补下一件铁甲，继续堆满
     * 5. 直到点数用完或护甲补满4件
     * 6. 定值和范围值统一处理（范围值已在 ForcedAttributeHelper 中每怪独立随机）
     *
     * Unified armor attribute application logic (used for both random and forced attributes).
     * Implementation:
     * 1. Enhancement and resistance handled independently
     * 2. Iterate armor slots starting from helmet
     * 3. Fill each piece's enhancement/resistance to per-attribute cap (maxStatCap)
     * 4. When current piece is full, remaining points trigger adding next iron armor piece and continue filling
     * 5. Continue until points exhausted or 4 pieces equipped
     * 6. Fixed and range values use same logic (range randomness handled per-entity in ForcedAttributeHelper)
     */
    private static void applyArmorAttributes(Mob mob, ElementType enhanceType, int enhanceTotalPoints,
                                             ElementType resistType, int resistTotalPoints) {

        int enhancePerLevel = ElementalConfig.getStrengthPerLevel();
        int resistPerLevel = ElementalConfig.getResistPerLevel();
        int maxPointsPerAttr = ElementalConfig.getMaxStatCap();  // 单属性上限，例如25

        int maxEnhLvPerPiece = Math.max(1, maxPointsPerAttr / enhancePerLevel);
        int maxResLvPerPiece = Math.max(1, maxPointsPerAttr / resistPerLevel);

        int remainingEnhance = enhanceTotalPoints;
        int remainingResist = resistTotalPoints;

        // 循环所有4个护甲槽位（包括补的）
        // Loop through all 4 armor slots (including added ones)
        for (int i = 0; i < 4 && (remainingEnhance > 0 || remainingResist > 0); i++) {
            EquipmentSlot slot = ARMOR_SLOTS[i];
            ItemStack stack = mob.getItemBySlot(slot);

            // 如果槽位为空且还有点数需要分配，则补铁甲
            // If slot empty and points remain, add iron armor
            if (stack.isEmpty()) {
                stack = AttributeEquipUtils.createIronArmor(i);
                mob.setItemSlot(slot, stack);
                mob.setDropChance(slot, 0.0F);
            }

            // 计算这件装备本次应堆的强化等级（最多堆到上限）
            // Calculate enhancement levels to add this piece (up to cap)
            int enhLvToAdd = 0;
            if (remainingEnhance > 0) {
                int possibleLv = remainingEnhance / enhancePerLevel;
                enhLvToAdd = Math.min(possibleLv, maxEnhLvPerPiece);
                remainingEnhance -= enhLvToAdd * enhancePerLevel;
            }

            // 计算这件装备本次应堆的抗性等级（最多堆到上限）
            // Calculate resistance levels to add this piece (up to cap)
            int resLvToAdd = 0;
            if (remainingResist > 0) {
                int possibleLv = remainingResist / resistPerLevel;
                resLvToAdd = Math.min(possibleLv, maxResLvPerPiece);
                remainingResist -= resLvToAdd * resistPerLevel;
            }

            // 直接覆盖附魔（怪物装备无原有附魔，安全）
            // Directly apply enchantments (safe for mobs as they have no pre-existing enchants)
            AttributeEquipUtils.applyArmorEnchantsLevel(stack, enhanceType, enhLvToAdd, resistType, resLvToAdd);
        }

        // 如果还有剩余点数（4件已满），浪费掉（符合单件上限设计）
        // If points remain after 4 pieces, they are wasted (consistent with per-piece cap design)
    }
}