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
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * MobAttributeLogic
 *
 * 中文说明：
 * 包含生物属性生成的决策逻辑。
 * 修复：现在强制属性配置也会为无护甲生物穿戴隐形头盔，确保护甲属性生效。
 *
 * English description:
 * Contains the decision logic for mob attribute generation.
 * Fix: Forced attribute config now correctly applies invisible helmets to unarmored mobs.
 */
public class MobAttributeLogic {

    private static final Random RANDOM = new Random();
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    /**
     * 处理单个生物的属性赋予逻辑。
     * Processes attribute assignment logic for a single mob.
     */
    public static void processMob(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        if (data.getBoolean("ElementalCraft_AttributesSet")) return;

        String entityId = net.minecraft.world.entity.EntityType.getKey(mob.getType()).toString();

        // Blacklist check
        if (ElementalConfig.BLACKLISTED_ENTITIES.get().contains(entityId)) {
            data.putBoolean("ElementalCraft_AttributesSet", true);
            return;
        }

        // 1. Forced Config
        ForcedAttributeHelper.ForcedData forced = ForcedAttributeHelper.getForcedData(mob.getType());
        boolean dimensionForced = false;

        // 2. Nether Forced
        if (forced == null && ElementalConfig.NETHER_DIMENSION_FORCED_FIRE.get()
                && mob.level().dimension() == Level.NETHER) {
            int points = ElementalConfig.NETHER_FIRE_POINTS.get();
            forced = new ForcedAttributeHelper.ForcedData(
                    ElementType.FIRE, ElementType.FIRE, points,
                    ElementType.FIRE, points
            );
            dimensionForced = true;
        }

        // 3. End Forced
        if (forced == null && ElementalConfig.END_DIMENSION_FORCED_THUNDER.get()
                && mob.level().dimension() == Level.END) {
            int points = ElementalConfig.END_THUNDER_POINTS.get();
            forced = new ForcedAttributeHelper.ForcedData(
                    ElementType.THUNDER, ElementType.THUNDER, points,
                    ElementType.THUNDER, points
            );
            dimensionForced = true;
        }

        // Apply Forced
        if (forced != null) {
            applyForcedAttributes(mob, forced, dimensionForced);
            data.putBoolean("ElementalCraft_AttributesSet", true);
            return;
        }

        // 4. Skip non-hostile
        if (!(mob instanceof Monster)) {
            data.putBoolean("ElementalCraft_AttributesSet", true);
            return;
        }

        // 5. Global Chance
        if (RANDOM.nextDouble() >= ElementalConfig.MOB_ATTRIBUTE_CHANCE_HOSTILE.get()) {
            data.putBoolean("ElementalCraft_AttributesSet", true);
            return;
        }

        // 6. Random Generation
        applyRandomAttributes(mob);
        data.putBoolean("ElementalCraft_AttributesSet", true);
    }

    /**
     * 应用随机生成的属性。
     * Applies randomly generated attributes.
     */
    private static void applyRandomAttributes(Mob mob) {
        ItemStack mainHand = mob.getMainHandItem();
        ItemStack offHand = mob.getOffhandItem();
        boolean hasHandItem = !mainHand.isEmpty() || !offHand.isEmpty();

        List<ItemStack> armorPieces = new ArrayList<>();
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack item = mob.getItemBySlot(slot);
            if (!item.isEmpty()) armorPieces.add(item);
        }
        int pieceCount = armorPieces.isEmpty() ? 1 : armorPieces.size();

        ElementType mainType = BiomeAttributeBias.getBiasedElement((ServerLevel) mob.level(), mob.blockPosition());

        ElementType attackType = hasHandItem ? mainType : null;
        ElementType enhanceType = mainType;
        int enhanceTotalPoints = hasHandItem ? ElementalConfig.rollMonsterStrength() : 0;

        ElementType resistType;
        if (attackType != null && RANDOM.nextDouble() < ElementalConfig.COUNTER_RESIST_CHANCE.get()) {
            resistType = AttributeEquipUtils.getCounterElement(attackType);
        } else {
            resistType = AttributeEquipUtils.randomNonNoneElement();
        }
        int resistTotalPoints = ElementalConfig.rollMonsterResist();

        // Check forced weapon
        if (hasHandItem) {
            ItemStack weapon = !mainHand.isEmpty() ? mainHand : offHand;
            ForcedItemHelper.WeaponData weaponData = ForcedItemHelper.getForcedWeapon(weapon.getItem());
            if (weaponData != null && weaponData.attackType() != null) {
                AttributeEquipUtils.applyAttackEnchant(weapon, weaponData.attackType());
                attackType = null;
            }
        }

        // Check forced armor
        boolean armorForced = false;
        if (!armorPieces.isEmpty()) {
            for (ItemStack armor : armorPieces) {
                ForcedItemHelper.ArmorData armorData = ForcedItemHelper.getForcedArmor(armor.getItem());
                if (armorData != null) {
                    AttributeEquipUtils.applyArmorEnchants(armor, 
                            armorData.enhanceType(), armorData.enhancePoints(),
                            armorData.resistType(), armorData.resistPoints(), 
                            1);
                    armorForced = true;
                }
            }
            if (armorForced) {
                enhanceType = null;
                resistType = null;
                enhanceTotalPoints = 0;
                resistTotalPoints = 0;
            }
        }

        if (hasHandItem && attackType != null) {
            ItemStack weapon = !mainHand.isEmpty() ? mainHand : offHand;
            AttributeEquipUtils.applyAttackEnchant(weapon, attackType);
        }

        if (!armorPieces.isEmpty() && !armorForced) {
            int[] resistLevels = AttributeEquipUtils.distributePointsToLevels(resistTotalPoints, ElementalConfig.getResistPerLevel(), pieceCount);
            int[] enhanceLevels = enhanceTotalPoints > 0
                    ? AttributeEquipUtils.distributePointsToLevels(enhanceTotalPoints, ElementalConfig.getStrengthPerLevel(), pieceCount)
                    : new int[pieceCount];

            for (int i = 0; i < armorPieces.size(); i++) {
                AttributeEquipUtils.applyArmorEnchantsLevel(armorPieces.get(i),
                        enhanceType, enhanceLevels[i],
                        resistType, resistLevels[i]);
            }
        } else if (armorPieces.isEmpty() && !armorForced) {
            // Invisible Helmet Fallback
            ItemStack helmet = AttributeEquipUtils.createInvisibleHelmet();
            
            int resPerLv = Math.max(1, ElementalConfig.getResistPerLevel());
            int enhPerLv = Math.max(1, ElementalConfig.getStrengthPerLevel());

            int resistLv = Math.max(1, Math.min(10, resistTotalPoints / resPerLv));
            int enhanceLv = 0;
            if (enhanceTotalPoints > 0) {
                enhanceLv = Math.max(1, Math.min(10, enhanceTotalPoints / enhPerLv));
            }
            
            AttributeEquipUtils.applyArmorEnchantsLevel(helmet, enhanceType, enhanceLv, resistType, resistLv);
            mob.setItemSlot(EquipmentSlot.HEAD, helmet);
        }
    }

    /**
     * 应用强制属性（配置或维度）。
     * Apply forced attributes (Config or Dimension).
     */
    private static void applyForcedAttributes(Mob mob, ForcedAttributeHelper.ForcedData data, boolean dimensionForced) {
        ElementType attackType = data.attackType();
        ElementType enhanceType = data.enhanceType();
        int enhancePoints = data.enhancePoints();
        ElementType resistType = data.resistType();
        int resistPoints = data.resistPoints();

        // 1. Weapon Logic
        ItemStack mainHand = mob.getMainHandItem();
        ItemStack offHand = mob.getOffhandItem();
        boolean hasWeapon = !mainHand.isEmpty() || !offHand.isEmpty();

        if (attackType != null && attackType != ElementType.NONE) {
            if (hasWeapon) {
                if (!mainHand.isEmpty()) AttributeEquipUtils.applyAttackEnchant(mainHand, attackType);
                if (!offHand.isEmpty()) AttributeEquipUtils.applyAttackEnchant(offHand, attackType);
            } else {
                // If no weapon, give iron sword so attack element works
                ItemStack sword = new ItemStack(Items.IRON_SWORD);
                AttributeEquipUtils.applyAttackEnchant(sword, attackType);
                mob.setItemSlot(EquipmentSlot.MAINHAND, sword);
            }
        }

        MinecraftServer server = mob.level().getServer();
        if (server == null) return;

        ItemStack[] currentArmor = new ItemStack[4];
        for (int i = 0; i < 4; i++) {
            currentArmor[i] = mob.getItemBySlot(ARMOR_SLOTS[i]);
        }
        boolean hasAnyArmor = Arrays.stream(currentArmor).anyMatch(s -> !s.isEmpty());

        int enhancePerLevel = Math.max(1, ElementalConfig.getStrengthPerLevel());
        int resistPerLevel = Math.max(1, ElementalConfig.getResistPerLevel());

        // 2. Armor Logic
        if (hasAnyArmor) {
            // Case A: Mob HAS armor. Enchant existing pieces.
            // 情况A：生物有护甲。附魔现有护甲。
            
            int[] enhanceLv = AttributeEquipUtils.distributePointsToLevels(enhancePoints, enhancePerLevel, 4);
            int[] resistLv = AttributeEquipUtils.distributePointsToLevels(resistPoints, resistPerLevel, 4);

            for (int i = 0; i < 4; i++) {
                if (!currentArmor[i].isEmpty()) {
                    AttributeEquipUtils.applyArmorEnchantsLevel(currentArmor[i], enhanceType, enhanceLv[i], resistType, resistLv[i]);
                }
            }

            // Dimension forced: Fill empty slots with Iron Armor
            // 维度强制：如果有些部位没装备，强制补齐铁甲
            if (dimensionForced) {
                for (int i = 0; i < 4; i++) {
                    if (currentArmor[i].isEmpty()) {
                        final int slotIndex = i;
                        ItemStack newArmor = AttributeEquipUtils.createIronArmor(i);
                        AttributeEquipUtils.applyArmorEnchantsLevel(newArmor, enhanceType, enhanceLv[i], resistType, resistLv[i]);
                        server.tell(new TickTask(server.getTickCount() + i, () -> {
                            if (mob.isAlive()) mob.setItemSlot(ARMOR_SLOTS[slotIndex], newArmor);
                        }));
                    }
                }
            }
        } else {
            // Case B: Mob has NO armor (Naked).
            
            if (dimensionForced) {
                // If it's Dimension Forced (Nether/End), we usually want them to wear Iron Armor to be tough.
                // 如果是维度强制（下界/末地），我们通常希望它们穿铁甲变得更强。
                for (int i = 0; i < 4; i++) {
                    final int slotIndex = i;
                    // Calculate levels assuming 4 pieces distribution
                    int[] enhanceLv = AttributeEquipUtils.distributePointsToLevels(enhancePoints, enhancePerLevel, 4);
                    int[] resistLv = AttributeEquipUtils.distributePointsToLevels(resistPoints, resistPerLevel, 4);
                    
                    ItemStack armor = AttributeEquipUtils.createIronArmor(i);
                    AttributeEquipUtils.applyArmorEnchantsLevel(armor, enhanceType, enhanceLv[i], resistType, resistLv[i]);

                    server.tell(new TickTask(server.getTickCount() + i, () -> {
                        if (mob.isAlive()) mob.setItemSlot(ARMOR_SLOTS[slotIndex], armor);
                    }));
                }
            } else {
                        ItemStack helmet = AttributeEquipUtils.createInvisibleHelmet();            
                          
                // Calculate levels for single item (allow higher cap for helmet)
                // 计算单件装备等级（允许更高等级上限以容纳总点数）
                int resistLv = (resistPoints > 0) ? Math.max(1, resistPoints / resistPerLevel) : 0;
                int enhanceLv = (enhancePoints > 0) ? Math.max(1, enhancePoints / enhancePerLevel) : 0;

                AttributeEquipUtils.applyArmorEnchantsLevel(helmet, enhanceType, enhanceLv, resistType, resistLv);
                
                // Equip immediately
                mob.setItemSlot(EquipmentSlot.HEAD, helmet);
            }
        }
    }
}