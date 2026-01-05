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
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * MobAttributeLogic
 * <p>
 * 中文说明：
 * 生物属性生成的决策核心类。
 * 负责处理生物的属性生成全流程，包括：
 * 1. 黑名单检查。
 * 2. 强制属性应用（基于配置文件或维度默认值）。
 * 3. 随机属性生成算法（基于生物类型和全局概率）。
 * 4. 属性的实际应用（为生物装备附魔武器和护甲）。
 * 已对接热重载系统，并优化了属性分配算法，确保属性均匀分布于护甲上。
 * <p>
 * English Description:
 * Core decision class for mob attribute generation.
 * Handles the entire attribute generation flow, including:
 * 1. Blacklist checks.
 * 2. Forced attribute application (based on config or dimension defaults).
 * 3. Random attribute generation algorithm (based on mob type and global chances).
 * 4. Actual attribute application (enchanting weapons and armor for mobs).
 * Integrated with the hot-reload system and optimized the attribute distribution algorithm to ensure even distribution across armor pieces.
 */
public class MobAttributeLogic {

    private static final Random RANDOM = new Random();

    // 护甲槽位遍历顺序：头 -> 胸 -> 腿 -> 脚
    // Armor slot iteration order: Head -> Chest -> Legs -> Feet
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    /**
     * 处理单个生物的属性生成逻辑。
     * 只有当该生物尚未被处理过（检查 NBT 标记）时才会执行。
     * <p>
     * Processes attribute generation for a single mob.
     * Only executes if the mob has not been processed yet (checks NBT tag).
     *
     * @param mob 目标生物 / Target mob
     */
    public static void processMob(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        if (data.getBoolean("ElementalCraft_AttributesSet")) return;

        String entityId = net.minecraft.world.entity.EntityType.getKey(mob.getType()).toString();

        // 0. 黑名单检查
        // 0. Blacklist Check
        if (ElementalConfig.cachedBlacklist.contains(entityId)) {
            data.putBoolean("ElementalCraft_AttributesSet", true);
            return;
        }

        // 1. 获取强制属性配置（优先检查配置文件中的特定生物设置）
        // 1. Get Forced Attribute Config (Prioritize specific mob settings in config)
        ForcedAttributeHelper.ForcedData forced = ForcedAttributeHelper.getForcedData(mob.getType());

        // 2. 下界维度默认强制属性
        // 2. Nether Dimension Default Forced Attributes
        if (forced == null && ElementalConfig.netherForcedFire
                && mob.level().dimension() == Level.NETHER) {
            int points = ElementalConfig.netherFirePoints;
            forced = new ForcedAttributeHelper.ForcedData(
                    ElementType.FIRE, ElementType.FIRE, points,
                    ElementType.FIRE, points
            );
        }

        // 3. 末地维度默认强制属性
        // 3. End Dimension Default Forced Attributes
        if (forced == null && ElementalConfig.endForcedThunder
                && mob.level().dimension() == Level.END) {
            int points = ElementalConfig.endThunderPoints;
            forced = new ForcedAttributeHelper.ForcedData(
                    ElementType.THUNDER, ElementType.THUNDER, points,
                    ElementType.THUNDER, points
            );
        }

        // 如果存在强制属性，则应用并结束
        // If forced attributes exist, apply and return
        if (forced != null) {
            applyForcedAttributes(mob, forced);
            data.putBoolean("ElementalCraft_AttributesSet", true);
            return;
        }

        // 4. 生物类型检查与概率判定
        // 4. Mob Type Check & Chance Determination

        // 判断是否为中立生物：实现了 NeutralMob 接口（如末影人、狼） 或者 是猪灵（猪灵未实现接口但行为中立）
        // Check if neutral mob: Implements NeutralMob (e.g., Enderman, Wolf) OR is Piglin (behaves neutral but doesn't implement interface)
        boolean isNeutral = (mob instanceof NeutralMob) || entityId.equals("minecraft:piglin");

        // 判断是否为怪物：Monster 类通常包含敌对生物，但也包含部分中立生物（如末影人）
        // Check if monster: Monster class usually contains hostile mobs, but also some neutral ones (e.g., Enderman)
        boolean isMonster = (mob instanceof Monster);

        // 如果既不是怪物也不是中立生物（例如纯被动生物：牛、羊），则跳过
        // If neither monster nor neutral mob (e.g., purely passive mobs like Cow, Sheep), skip
        if (!isMonster && !isNeutral) {
            data.putBoolean("ElementalCraft_AttributesSet", true);
            return;
        }

        // 5. 全局生成概率检查
        // 根据生物类型选择对应的配置概率。注意：末影人同时是 Monster 和 NeutralMob，优先按 Neutral 处理。
        // 5. Global Generation Chance Check
        // Select config probability based on mob type. Note: Enderman is both Monster and NeutralMob, treat as Neutral.
        double chance = isNeutral ? ElementalConfig.mobChanceNeutral : ElementalConfig.mobChanceHostile;

        if (RANDOM.nextDouble() >= chance) {
            data.putBoolean("ElementalCraft_AttributesSet", true);
            return;
        }

        // 6. 执行随机生成逻辑
        // 6. Execute Random Generation Logic
        applyRandomAttributes(mob);
        data.putBoolean("ElementalCraft_AttributesSet", true);
    }

    /**
     * 应用随机属性生成逻辑。
     * 根据生物群系偏好确定主元素，并随机生成攻击、强化和抗性属性。
     * <p>
     * Applies random attribute generation logic.
     * Determines the main element based on biome bias and randomly generates attack, enhancement, and resistance attributes.
     *
     * @param mob 目标生物 / Target mob
     */
    private static void applyRandomAttributes(Mob mob) {
        ItemStack mainHand = mob.getMainHandItem();
        ItemStack offHand = mob.getOffhandItem();
        boolean hasHandItem = !mainHand.isEmpty() || !offHand.isEmpty();

        // 确定主元素（基于生物群系）
        // Determine main element (based on biome)
        ElementType mainType = BiomeAttributeBias.getBiasedElement((ServerLevel) mob.level(), mob.blockPosition());

        // 决定是否拥有攻击属性
        // Decide whether to have attack attribute
        ElementType attackType = null;
        if (RANDOM.nextDouble() < ElementalConfig.attackChance) {
            attackType = mainType;
        }

        // 决定强化属性（通常与主元素一致）
        // Decide enhancement attribute (usually same as main element)
        ElementType enhanceType = mainType;
        // 如果有手持物品或有攻击属性，则生成强化点数，否则为0
        // If holding item or has attack attribute, generate strength points, otherwise 0
        int enhanceTotalPoints = (hasHandItem || attackType != null) ? ElementalConfig.rollMonsterStrength() : 0;

        // 决定抗性属性
        // Decide resistance attribute
        ElementType resistType;
        // 有概率获得克制自己的属性的抗性（反克制）
        // Chance to gain resistance against the element that counters them (Counter-Resistance)
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
                // 如果没有手持物品，给予一把铁剑用于承载攻击属性
                // If no held item, give an Iron Sword to hold the attack attribute
                ItemStack sword = new ItemStack(Items.IRON_SWORD);
                AttributeEquipUtils.applyAttackEnchant(sword, attackType);
                mob.setItemSlot(EquipmentSlot.MAINHAND, sword);
                mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
            }
        }

        // 应用护甲属性（使用均匀分配逻辑）
        // Apply armor attributes (using even distribution logic)
        applyArmorAttributes(mob, enhanceType, enhanceTotalPoints, resistType, resistTotalPoints);
    }

    /**
     * 应用强制属性逻辑。
     * 将强制数据应用到生物上，并在下一 tick 执行以确保数据安全。
     * <p>
     * Applies forced attribute logic.
     * Applies forced data to the mob, scheduled for the next tick to ensure data safety.
     *
     * @param mob  目标生物 / Target mob
     * @param data 强制属性数据 / Forced attribute data
     */
    private static void applyForcedAttributes(Mob mob, ForcedAttributeHelper.ForcedData data) {
        MinecraftServer server = mob.level().getServer();
        if (server == null) return;

        // 调度到下一 tick 执行，防止在实体生成过程中修改导致的问题
        // Schedule for next tick to prevent issues during entity generation
        server.tell(new TickTask(server.getTickCount() + 1, () -> {
            if (!mob.isAlive()) return;

            ElementType attackType = data.attackType();
            ElementType enhanceType = data.enhanceType();
            int enhancePoints = data.enhancePoints();
            ElementType resistType = data.resistType();
            int resistPoints = data.resistPoints();

            ItemStack mainHand = mob.getMainHandItem();
            ItemStack offHand = mob.getOffhandItem();
            boolean hasWeapon = !mainHand.isEmpty() || !offHand.isEmpty();

            // 应用攻击属性
            // Apply attack attribute
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

            // 应用护甲属性
            // Apply armor attributes
            applyArmorAttributes(mob, enhanceType, enhancePoints, resistType, resistPoints);
        }));
    }

    /**
     * 统一的护甲属性应用逻辑。
     * 使用“均匀分配”算法，将总属性点数分散到 4 个护甲槽位上。
     * 如果某个槽位分配到了属性但为空，会自动生成铁甲。
     * <p>
     * Unified armor attribute application logic.
     * Uses "Even Distribution" algorithm to spread total attribute points across 4 armor slots.
     * If a slot receives attributes but is empty, Iron Armor is automatically generated.
     *
     * @param mob                目标生物 / Target mob
     * @param enhanceType        强化类型 / Enhancement type
     * @param enhanceTotalPoints 强化总点数 / Total enhancement points
     * @param resistType         抗性类型 / Resistance type
     * @param resistTotalPoints  抗性总点数 / Total resistance points
     */
    private static void applyArmorAttributes(Mob mob, ElementType enhanceType, int enhanceTotalPoints,
                                             ElementType resistType, int resistTotalPoints) {

        int enhancePerLevel = ElementalConfig.getStrengthPerLevel();
        int resistPerLevel = ElementalConfig.getResistPerLevel();

        // 1. 使用均匀分配算法计算每件装备的等级
        // 这将把 totalPoints 尽量平均分给 4 个部位
        // 1. Calculate levels for each armor piece using even distribution algorithm
        // This distributes totalPoints as evenly as possible across 4 slots
        int[] enhanceLevels = AttributeEquipUtils.distributePointsToLevels(enhanceTotalPoints, enhancePerLevel, 4);
        int[] resistLevels = AttributeEquipUtils.distributePointsToLevels(resistTotalPoints, resistPerLevel, 4);

        // 2. 循环所有4个护甲槽位并应用等级
        // 2. Iterate through all 4 armor slots and apply levels
        for (int i = 0; i < 4; i++) {
            // 如果该部位没有任何等级，且该槽位为空，则跳过（不生成多余的铁甲）
            // If the slot has no levels and is empty, skip (don't generate redundant iron armor)
            if (enhanceLevels[i] <= 0 && resistLevels[i] <= 0 && mob.getItemBySlot(ARMOR_SLOTS[i]).isEmpty()) {
                continue;
            }

            EquipmentSlot slot = ARMOR_SLOTS[i];
            ItemStack stack = mob.getItemBySlot(slot);

            // 如果槽位为空，补铁甲
            // If slot is empty, fill with Iron Armor
            if (stack.isEmpty()) {
                stack = AttributeEquipUtils.createIronArmor(i);
                mob.setItemSlot(slot, stack);
                mob.setDropChance(slot, 0.0F);
            }

            // 应用附魔
            // Apply enchantments
            AttributeEquipUtils.applyArmorEnchantsLevel(stack, enhanceType, enhanceLevels[i], resistType, resistLevels[i]);
        }
    }
}