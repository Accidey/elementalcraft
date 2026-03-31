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

import java.util.concurrent.ThreadLocalRandom;

public class MobAttributeLogic {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public static void processMob(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        if (data.getBoolean("ElementalCraft_AttributesSet")) return;

        String entityId = net.minecraft.world.entity.EntityType.getKey(mob.getType()).toString();

        if (ElementalConfig.cachedBlacklist.contains(entityId)) {
            data.putBoolean("ElementalCraft_AttributesSet", true);
            Debug.logBlacklisted(mob, entityId);
            return;
        }

        java.util.List<ForcedAttributeHelper.ForcedData> forcedList = ForcedAttributeHelper.getForcedDataList(mob.getType());
        ForcedAttributeHelper.ForcedData forced = null;
        if (!forcedList.isEmpty()) {
            forced = forcedList.get(ThreadLocalRandom.current().nextInt(forcedList.size()));
        }

        if (forced == null && ElementalConfig.netherForcedFire
                && mob.level().dimension() == Level.NETHER) {
            int points = ElementalConfig.netherFirePoints;
            forced = new ForcedAttributeHelper.ForcedData(
                    ElementType.FIRE, ElementType.FIRE, points,
                    ElementType.FIRE, points
            );
            Debug.logNetherForced(mob, points);
        }

        if (forced == null && ElementalConfig.endForcedThunder
                && mob.level().dimension() == Level.END) {
            int points = ElementalConfig.endThunderPoints;
            forced = new ForcedAttributeHelper.ForcedData(
                    ElementType.THUNDER, ElementType.THUNDER, points,
                    ElementType.THUNDER, points
            );
            Debug.logEndForced(mob, points);
        }

        if (forced != null) {
            applyForcedAttributes(mob, data, forced);
            Debug.logForcedApplied(mob, forced);
            return;
        }

        boolean isNeutral = (mob instanceof NeutralMob) || entityId.equals("minecraft:piglin");
        boolean isMonster = (mob instanceof Monster);

        if (!isMonster && !isNeutral) {
            data.putBoolean("ElementalCraft_AttributesSet", true);
            return;
        }

        double chance = isNeutral ? ElementalConfig.mobChanceNeutral : ElementalConfig.mobChanceHostile;
        boolean willGenerate = ThreadLocalRandom.current().nextDouble() < chance;
        Debug.logChanceCheck(mob, isNeutral, chance, willGenerate);

        if (!willGenerate) {
            data.putBoolean("ElementalCraft_AttributesSet", true);
            return;
        }

        applyRandomAttributes(mob);
        data.putBoolean("ElementalCraft_AttributesSet", true);
    }

    private static void applyRandomAttributes(Mob mob) {
        ItemStack mainHand = mob.getMainHandItem();
        ItemStack offHand = mob.getOffhandItem();
        boolean hasHandItem = !mainHand.isEmpty() || !offHand.isEmpty();

        ElementType mainType = BiomeAttributeBias.getBiasedElement((ServerLevel) mob.level(), mob.blockPosition());
        Debug.logBiasedElement(mob, mainType);

        ElementType attackType = null;
        if (ThreadLocalRandom.current().nextDouble() < ElementalConfig.attackChance) {
            attackType = mainType;
        }

        ElementType enhanceType = mainType;
        int enhanceTotalPoints = (hasHandItem || attackType != null) ? ElementalConfig.rollMonsterStrength() : 0;

        ElementType resistType;
        if (attackType != null && ThreadLocalRandom.current().nextDouble() < ElementalConfig.counterResistChance) {
            resistType = AttributeEquipUtils.getCounterElement(attackType);
        } else {
            resistType = AttributeEquipUtils.randomNonNoneElement();
        }
        int resistTotalPoints = ElementalConfig.rollMonsterResist();

        Debug.logRandomGeneration(mob, mainType, attackType, enhanceType, enhanceTotalPoints, resistType, resistTotalPoints, hasHandItem);

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

        applyArmorAttributes(mob, enhanceType, enhanceTotalPoints, resistType, resistTotalPoints);
    }

    private static void applyForcedAttributes(Mob mob, CompoundTag persistentData, ForcedAttributeHelper.ForcedData data) {
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

            persistentData.putBoolean("ElementalCraft_AttributesSet", true);
        }));
    }

    private static void applyArmorAttributes(Mob mob, ElementType enhanceType, int enhanceTotalPoints,
                                             ElementType resistType, int resistTotalPoints) {

        int enhancePerLevel = ElementalConfig.getStrengthPerLevel();
        int resistPerLevel = ElementalConfig.getResistPerLevel();

        int[] enhanceLevels = AttributeEquipUtils.distributePointsToLevels(enhanceTotalPoints, enhancePerLevel, 4);
        int[] resistLevels = AttributeEquipUtils.distributePointsToLevels(resistTotalPoints, resistPerLevel, 4);

        Debug.logArmorDistribution(mob, enhanceTotalPoints, enhancePerLevel, enhanceLevels, resistTotalPoints, resistPerLevel, resistLevels);

        for (int i = 0; i < 4; i++) {
            if (enhanceLevels[i] <= 0 && resistLevels[i] <= 0 && mob.getItemBySlot(ARMOR_SLOTS[i]).isEmpty()) {
                continue;
            }

            EquipmentSlot slot = ARMOR_SLOTS[i];
            ItemStack stack = mob.getItemBySlot(slot);

            if (stack.isEmpty()) {
                stack = AttributeEquipUtils.createIronArmor(i);
                mob.setItemSlot(slot, stack);
                mob.setDropChance(slot, 0.0F);
                Debug.logArmorCreated(mob, slot);
            }

            AttributeEquipUtils.applyArmorEnchantsLevel(stack, enhanceType, enhanceLevels[i], resistType, resistLevels[i]);
            Debug.logArmorEnchanted(mob, slot, enhanceType, enhanceLevels[i], resistType, resistLevels[i]);
        }
    }

    private static final class Debug {
        private static void logBlacklisted(Mob mob, String id) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(mob.level(), "生物属性", String.format("%s 在黑名单中，跳过", id));
        }

        private static void logNetherForced(Mob mob, int points) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(mob.level(), "生物属性", String.format("%s 下界强制赤焰属性：点数 %d", mob.getName().getString(), points));
        }

        private static void logEndForced(Mob mob, int points) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(mob.level(), "生物属性", String.format("%s 末地强制雷霆属性：点数 %d", mob.getName().getString(), points));
        }

        private static void logForcedApplied(Mob mob, ForcedAttributeHelper.ForcedData data) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(mob.level(), "生物属性", String.format("%s 应用强制属性：攻击 %s，强化 %s %d，抗性 %s %d",
                    mob.getName().getString(), data.attackType(), data.enhanceType(), data.enhancePoints(),
                    data.resistType(), data.resistPoints()));
        }

        private static void logChanceCheck(Mob mob, boolean isNeutral, double chance, boolean willGenerate) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(mob.level(), "生物属性", String.format("%s 类型 %s，概率 %.2f，结果 %s",
                    mob.getName().getString(), isNeutral ? "中立" : "敌对", chance, willGenerate ? "生成" : "跳过"));
        }

        private static void logBiasedElement(Mob mob, ElementType element) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(mob.level(), "生物属性", String.format("%s 群系偏好元素 %s", mob.getName().getString(), element));
        }

        private static void logRandomGeneration(Mob mob, ElementType mainType, ElementType attackType,
                                                ElementType enhanceType, int enhancePoints,
                                                ElementType resistType, int resistPoints, boolean hasHandItem) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(mob.level(), "生物属性", String.format(
                    "%s 随机生成：主元素 %s，攻击 %s，强化 %s %d，抗性 %s %d，有手持 %s",
                    mob.getName().getString(), mainType, attackType, enhanceType, enhancePoints,
                    resistType, resistPoints, hasHandItem));
        }

        private static void logArmorDistribution(Mob mob, int enhanceTotal, int enhancePerLevel, int[] enhanceLevels,
                                                  int resistTotal, int resistPerLevel, int[] resistLevels) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            String enhanceStr = String.format("强化总%d(每级%d)分配: %d %d %d %d",
                    enhanceTotal, enhancePerLevel, enhanceLevels[0], enhanceLevels[1], enhanceLevels[2], enhanceLevels[3]);
            String resistStr = String.format("抗性总%d(每级%d)分配: %d %d %d %d",
                    resistTotal, resistPerLevel, resistLevels[0], resistLevels[1], resistLevels[2], resistLevels[3]);
            GlobalDebugLogger.log(mob.level(), "生物属性", mob.getName().getString() + " " + enhanceStr + "，" + resistStr);
        }

        private static void logArmorCreated(Mob mob, EquipmentSlot slot) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(mob.level(), "生物属性", String.format("%s 创建铁甲于 %s", mob.getName().getString(), slot));
        }

        private static void logArmorEnchanted(Mob mob, EquipmentSlot slot, ElementType enhanceType, int enhanceLevel,
                                              ElementType resistType, int resistLevel) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(mob.level(), "生物属性", String.format(
                    "%s 附魔 %s：强化 %s Lv.%d，抗性 %s Lv.%d",
                    mob.getName().getString(), slot, enhanceType, enhanceLevel, resistType, resistLevel));
        }
    }
}
