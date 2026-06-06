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
        }

        if (forced == null && ElementalConfig.endForcedThunder
                && mob.level().dimension() == Level.END) {
            int points = ElementalConfig.endThunderPoints;
            forced = new ForcedAttributeHelper.ForcedData(
                    ElementType.THUNDER, ElementType.THUNDER, points,
                    ElementType.THUNDER, points
            );
        }

        if (forced != null) {
            applyForcedAttributes(mob, data, forced);
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

        if (attackType != null) {
            boolean issCaster = attackType == ElementType.THUNDER && ThreadLocalRandom.current().nextDouble() < 0.5;
            boolean natureCaster = !issCaster && attackType == ElementType.NATURE && ThreadLocalRandom.current().nextDouble() < 0.5;
            if (issCaster) {
                mob.getPersistentData().putBoolean("EC_ISS_MobCaster", true);
                mob.getPersistentData().putString("EC_ISS_MobElement", "thunder");
            } else if (natureCaster) {
                mob.getPersistentData().putBoolean("EC_ISS_MobCaster", true);
                mob.getPersistentData().putString("EC_ISS_MobElement", "nature");
                if (!hasHandItem) {
                    ItemStack sword = new ItemStack(Items.IRON_SWORD);
                    AttributeEquipUtils.applyAttackEnchant(sword, attackType);
                    mob.setItemSlot(EquipmentSlot.MAINHAND, sword);
                    mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
                }
            } else if (hasHandItem) {
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

        CompoundTag dropData = mob.getPersistentData();
        dropData.putString("EC_DropElementType", mainType.getId());
        if (attackType != null) {
            dropData.putString("EC_DropAttackType", attackType.getId());
        }
        dropData.putInt("EC_DropEnhancePoints", enhanceTotalPoints);
        dropData.putInt("EC_DropResistPoints", resistTotalPoints);
        dropData.putString("EC_DropResistType", resistType.getId());

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            mob.setDropChance(slot, 0.0F);
        }
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
                boolean issCaster = attackType == ElementType.THUNDER && ThreadLocalRandom.current().nextDouble() < 0.5;
                boolean natureCaster = !issCaster && attackType == ElementType.NATURE && ThreadLocalRandom.current().nextDouble() < 0.5;
                if (issCaster) {
                    persistentData.putBoolean("EC_ISS_MobCaster", true);
                    persistentData.putString("EC_ISS_MobElement", "thunder");
                } else if (natureCaster) {
                    persistentData.putBoolean("EC_ISS_MobCaster", true);
                    persistentData.putString("EC_ISS_MobElement", "nature");
                    if (!hasWeapon) {
                        ItemStack sword = new ItemStack(Items.IRON_SWORD);
                        AttributeEquipUtils.applyAttackEnchant(sword, attackType);
                        mob.setItemSlot(EquipmentSlot.MAINHAND, sword);
                        mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
                    }
                } else if (hasWeapon) {
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

            persistentData.putString("EC_DropElementType", enhanceType != null ? enhanceType.getId() : "");
            if (attackType != null && attackType != ElementType.NONE) {
                persistentData.putString("EC_DropAttackType", attackType.getId());
            }
            persistentData.putInt("EC_DropEnhancePoints", enhancePoints);
            persistentData.putInt("EC_DropResistPoints", resistPoints);
            persistentData.putString("EC_DropResistType", resistType != null ? resistType.getId() : "");

            persistentData.putBoolean("ElementalCraft_AttributesSet", true);

            for (EquipmentSlot slot : EquipmentSlot.values()) {
                mob.setDropChance(slot, 0.0F);
            }
        }));
    }

    private static void applyArmorAttributes(Mob mob, ElementType enhanceType, int enhanceTotalPoints,
                                             ElementType resistType, int resistTotalPoints) {

        int enhancePerLevel = ElementalConfig.getStrengthPerLevel();
        int resistPerLevel = ElementalConfig.getResistPerLevel();

        int[] enhanceLevels = AttributeEquipUtils.distributePointsToLevels(enhanceTotalPoints, enhancePerLevel, 4);
        int[] resistLevels = AttributeEquipUtils.distributePointsToLevels(resistTotalPoints, resistPerLevel, 4);

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
            }

            AttributeEquipUtils.applyArmorEnchantsLevel(stack, enhanceType, enhanceLevels[i], resistType, resistLevels[i]);
        }
    }
}
