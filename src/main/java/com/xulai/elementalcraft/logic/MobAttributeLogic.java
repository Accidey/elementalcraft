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
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
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

        List<ItemStack> armorPieces = new ArrayList<>();
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack item = mob.getItemBySlot(slot);
            if (!item.isEmpty()) armorPieces.add(item);
        }
        int pieceCount = armorPieces.isEmpty() ? 1 : armorPieces.size();

        // 确定主元素
        // Determine primary element
        ElementType mainType = BiomeAttributeBias.getBiasedElement((ServerLevel) mob.level(), mob.blockPosition());

        ElementType attackType = null;
        // 攻击属性概率检查（使用静态缓存）
        // Attack attribute chance check (using static cache)
        if (hasHandItem && RANDOM.nextDouble() < ElementalConfig.attackChance) {
            attackType = mainType;
        }
        
        ElementType enhanceType = mainType;
        int enhanceTotalPoints = hasHandItem ? ElementalConfig.rollMonsterStrength() : 0;

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

        // 应用武器攻击附魔（完全由生物属性决定，忽略物品强制配置）
        // Apply weapon attack enchantment (Determined solely by mob attribute, ignoring item forced config)
        if (attackType != null) {
            if (!mainHand.isEmpty()) {
                AttributeEquipUtils.applyAttackEnchant(mainHand, attackType);
            }
            if (!offHand.isEmpty()) {
                AttributeEquipUtils.applyAttackEnchant(offHand, attackType);
            }
        }

        // 应用护甲附魔
        // 移除了对护甲强制配置的检查，确保装备属性始终跟随生物生成的随机属性
        // Apply armor enchantments
        // Removed check for forced armor configuration to ensure equipment attributes always follow the mob's randomly generated attributes
        if (!armorPieces.isEmpty()) {
            int[] resistLevels = AttributeEquipUtils.distributePointsToLevels(resistTotalPoints, ElementalConfig.getResistPerLevel(), pieceCount);
            int[] enhanceLevels = enhanceTotalPoints > 0
                    ? AttributeEquipUtils.distributePointsToLevels(enhanceTotalPoints, ElementalConfig.getStrengthPerLevel(), pieceCount)
                    : new int[pieceCount];

            for (int i = 0; i < armorPieces.size(); i++) {
                AttributeEquipUtils.applyArmorEnchantsLevel(armorPieces.get(i),
                        enhanceType, enhanceLevels[i],
                        resistType, resistLevels[i]);
            }
        } else {
            // 无护甲：创建隐形头盔承载属性
            // No armor: Create invisible helmet to hold attributes
            ItemStack helmet = AttributeEquipUtils.createInvisibleHelmet();
            
            int resPerLv = ElementalConfig.getResistPerLevel();
            int enhPerLv = ElementalConfig.getStrengthPerLevel();

            int resistLv = Math.max(1, Math.min(10, resistTotalPoints / resPerLv));
            int enhanceLv = 0;
            if (enhanceTotalPoints > 0) {
                enhanceLv = Math.max(1, Math.min(10, enhanceTotalPoints / enhPerLv));
            }
            
            AttributeEquipUtils.applyArmorEnchantsLevel(helmet, enhanceType, enhanceLv, resistType, resistLv);
            mob.setItemSlot(EquipmentSlot.HEAD, helmet);
            
            // 防止玩家刷取隐形头盔
            // Prevent players from farming invisible helmets
            mob.setDropChance(EquipmentSlot.HEAD, 0.0F);
        }
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
                }
            }

            // 2. 护甲逻辑处理 - 补全与附魔
            // 2. Armor logic handling - Fill and Enchant
            
            int enhancePerLevel = ElementalConfig.getStrengthPerLevel();
            int resistPerLevel = ElementalConfig.getResistPerLevel();

            // 始终按照4个部位来分配总点数
            // Always distribute total points across 4 slots
            int[] enhanceLv = AttributeEquipUtils.distributePointsToLevels(enhancePoints, enhancePerLevel, 4);
            int[] resistLv = AttributeEquipUtils.distributePointsToLevels(resistPoints, resistPerLevel, 4);

            // 遍历所有4个护甲槽位（FEET, LEGS, CHEST, HEAD）
            // Iterate through all 4 armor slots (FEET, LEGS, CHEST, HEAD)
            for (int i = 0; i < 4; i++) {
                EquipmentSlot slot = ARMOR_SLOTS[i];
                ItemStack currentStack = mob.getItemBySlot(slot);

                if (!currentStack.isEmpty()) {
                    // 情况A：已有装备（可能是原版生成或其他模组添加），直接应用附魔
                    // Case A: Has equipment (vanilla or other mods), apply enchantment directly
                    AttributeEquipUtils.applyArmorEnchantsLevel(currentStack, enhanceType, enhanceLv[i], resistType, resistLv[i]);
                } else {
                    // 情况B：没有装备，创建铁质装备并附魔
                    // Case B: No equipment, create Iron equipment and apply enchantment
                    ItemStack newArmor = AttributeEquipUtils.createIronArmor(i);
                    AttributeEquipUtils.applyArmorEnchantsLevel(newArmor, enhanceType, enhanceLv[i], resistType, resistLv[i]);

                    // 装备新物品
                    // Equip new item
                    mob.setItemSlot(ARMOR_SLOTS[i], newArmor);
                    mob.setDropChance(ARMOR_SLOTS[i], 0.0F);
                }
            }
        }));
    }
}