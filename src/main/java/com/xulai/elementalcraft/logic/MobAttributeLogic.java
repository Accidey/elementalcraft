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
 * [修复] 将属性分配逻辑改为“均匀分配”，解决低点数时只有头盔有属性的问题。
 *
 * English description:
 * Contains the decision logic for mob attribute generation.
 * Handles blacklist checks, forced attributes (config/dimension), random generation algorithms, and the final attribute application flow.
 * Integrated with the hot-reload system and handles attribute holding for unarmored mobs.
 * [Fix] Changed attribute distribution logic to "Even Distribution" to fix the issue where only the helmet has attributes when points are low.
 */
public class MobAttributeLogic {

    private static final Random RANDOM = new Random();

    // 遍历顺序：头 -> 胸 -> 腿 -> 脚
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public static void processMob(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        if (data.getBoolean("ElementalCraft_AttributesSet")) return;

        String entityId = net.minecraft.world.entity.EntityType.getKey(mob.getType()).toString();

        // 黑名单检查
        if (ElementalConfig.cachedBlacklist.contains(entityId)) {
            data.putBoolean("ElementalCraft_AttributesSet", true);
            return;
        }

        // 1. 获取强制属性配置
        ForcedAttributeHelper.ForcedData forced = ForcedAttributeHelper.getForcedData(mob.getType());

        // 2. 下界维度强制属性
        if (forced == null && ElementalConfig.netherForcedFire
                && mob.level().dimension() == Level.NETHER) {
            int points = ElementalConfig.netherFirePoints;
            forced = new ForcedAttributeHelper.ForcedData(
                    ElementType.FIRE, ElementType.FIRE, points,
                    ElementType.FIRE, points
            );
        }

        // 3. 末地维度强制属性
        if (forced == null && ElementalConfig.endForcedThunder
                && mob.level().dimension() == Level.END) {
            int points = ElementalConfig.endThunderPoints;
            forced = new ForcedAttributeHelper.ForcedData(
                    ElementType.THUNDER, ElementType.THUNDER, points,
                    ElementType.THUNDER, points
            );
        }

        // 应用强制属性
        if (forced != null) {
            applyForcedAttributes(mob, forced);
            data.putBoolean("ElementalCraft_AttributesSet", true);
            return;
        }

        // 4. 非敌对且非强制生物跳过
        if (!(mob instanceof Monster)) {
            data.putBoolean("ElementalCraft_AttributesSet", true);
            return;
        }

        // 5. 全局生成概率检查
        double chance = (mob instanceof net.minecraft.world.entity.monster.Enemy) 
                ? ElementalConfig.mobChanceHostile 
                : ElementalConfig.mobChanceNeutral;
        
        if (RANDOM.nextDouble() >= chance) {
            data.putBoolean("ElementalCraft_AttributesSet", true);
            return;
        }

        // 6. 执行随机生成逻辑
        applyRandomAttributes(mob);
        data.putBoolean("ElementalCraft_AttributesSet", true);
    }

    private static void applyRandomAttributes(Mob mob) {
        ItemStack mainHand = mob.getMainHandItem();
        ItemStack offHand = mob.getOffhandItem();
        boolean hasHandItem = !mainHand.isEmpty() || !offHand.isEmpty();

        // 确定主元素
        ElementType mainType = BiomeAttributeBias.getBiasedElement((ServerLevel) mob.level(), mob.blockPosition());

        ElementType attackType = null;
        if (RANDOM.nextDouble() < ElementalConfig.attackChance) {
            attackType = mainType;
        }
        
        ElementType enhanceType = mainType;
        int enhanceTotalPoints = (hasHandItem || attackType != null) ? ElementalConfig.rollMonsterStrength() : 0;

        ElementType resistType;
        if (attackType != null && RANDOM.nextDouble() < ElementalConfig.counterResistChance) {
            resistType = AttributeEquipUtils.getCounterElement(attackType);
        } else {
            resistType = AttributeEquipUtils.randomNonNoneElement();
        }
        int resistTotalPoints = ElementalConfig.rollMonsterResist();

        // 应用武器攻击附魔
        if (attackType != null) {
            if (hasHandItem) {
                if (!mainHand.isEmpty()) AttributeEquipUtils.applyAttackEnchant(mainHand, attackType);
                if (!offHand.isEmpty()) AttributeEquipUtils.applyAttackEnchant(offHand, attackType);
            } else {
                ItemStack sword = new ItemStack(Items.IRON_SWORD);
                AttributeEquipUtils.applyAttackEnchant(sword, attackType);
                mob.setItemSlot(EquipmentSlot.MAINHAND, sword);
                mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
            }
        }

        // 应用护甲属性（使用新的分配逻辑）
        applyArmorAttributes(mob, enhanceType, enhanceTotalPoints, resistType, resistTotalPoints);
    }

    private static void applyForcedAttributes(Mob mob, ForcedAttributeHelper.ForcedData data) {
        MinecraftServer server = mob.level().getServer();
        if (server == null) return;

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

            applyArmorAttributes(mob, enhanceType, enhancePoints, resistType, resistPoints);
        }));
    }

    /**
     * [重要修改] 统一的护甲属性应用逻辑。
     * 改用“均匀分配”算法 (distributePointsToLevels)，而不是原来的“贪婪分配”。
     * 这样可以确保当总点数较少时（例如25点），点数会分散到4件装备上，而不是全被头盔吸走。
     */
    private static void applyArmorAttributes(Mob mob, ElementType enhanceType, int enhanceTotalPoints,
                                             ElementType resistType, int resistTotalPoints) {

        int enhancePerLevel = ElementalConfig.getStrengthPerLevel();
        int resistPerLevel = ElementalConfig.getResistPerLevel();

        // 1. 使用均匀分配算法计算每件装备的等级
        // 这将把 totalPoints 尽量平均分给 4 个部位
        int[] enhanceLevels = AttributeEquipUtils.distributePointsToLevels(enhanceTotalPoints, enhancePerLevel, 4);
        int[] resistLevels = AttributeEquipUtils.distributePointsToLevels(resistTotalPoints, resistPerLevel, 4);

        // 2. 循环所有4个护甲槽位并应用等级
        for (int i = 0; i < 4; i++) {
            // 如果该部位没有任何等级，且该槽位为空，则跳过（不生成多余的铁甲）
            if (enhanceLevels[i] <= 0 && resistLevels[i] <= 0 && mob.getItemBySlot(ARMOR_SLOTS[i]).isEmpty()) {
                continue;
            }

            EquipmentSlot slot = ARMOR_SLOTS[i];
            ItemStack stack = mob.getItemBySlot(slot);

            // 如果槽位为空，补铁甲
            if (stack.isEmpty()) {
                stack = AttributeEquipUtils.createIronArmor(i);
                mob.setItemSlot(slot, stack);
                mob.setDropChance(slot, 0.0F);
            }

            // 应用附魔
            AttributeEquipUtils.applyArmorEnchantsLevel(stack, enhanceType, enhanceLevels[i], resistType, resistLevels[i]);
        }
    }
}