// src/main/java/com/xulai/elementalcraft/event/PlayerTrackEvents.java
package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.enchantment.ModEnchantments;
import com.xulai.elementalcraft.util.BiomeAttributeBias;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ForcedAttributeHelper;
import com.xulai.elementalcraft.util.ForcedItemHelper;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/**
 * PlayerTrackEvents
 *
 * 中文说明：
 * 该类负责周期性扫描玩家周围的生物，并为其生成或应用元素属性（附魔/强化/抗性）。
 * 修正说明：当生物被维度强制（下界或末地）时，如果生物已有部分护甲，则为已有护甲写入最高等级的属性；
 * 若某些护甲槽为空，则仅在维度强制的情况下补齐铁质护甲并为其写入对应的最高等级属性。
 *
 * English description:
 * This class periodically scans mobs around players and applies elemental attributes
 * (attack enchants, enhancement/resistance enchants) to them.
 * Fix summary: when mobs are forced by dimension (Nether or End), if the mob has some armor pieces,
 * write the highest-level attributes to existing pieces; if some armor slots are empty, fill them with iron armor
 * and apply the corresponding highest-level attributes. This "fill missing armor" behavior is applied only
 * for the two dimension-forced cases (Nether/End).
 */
@Mod.EventBusSubscriber(modid = "elementalcraft")
@SuppressWarnings("null")
public class PlayerTrackEvents {

    private static final Random RANDOM = new Random();
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    /**
     * 周期性扫描玩家周围的生物以应用属性（服务端，每秒一次）。
     *
     * Periodically scans for mobs around the player to apply attributes (server-side, once per second).
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 仅在服务端执行 / Server-side only
        if (event.side.isClient()) return;

        // 在 tick 开始时执行 / Execute at the start of the tick
        if (event.phase != TickEvent.Phase.START) return;

        // 仅处理服务端玩家 / Ensure it's a server player
        if (!(event.player instanceof ServerPlayer player)) return;

        // 每秒执行一次（20 ticks）/ Run logic once every second (20 ticks)
        if (player.tickCount % 20 != 0) return;

        ServerLevel level = player.serverLevel();
        // 扫描 16x8x16 区域内的生物 / Scan for mobs within a 16x8x16 area
        level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(16, 8, 16),
                mob -> mob.isAlive() && !mob.getPersistentData().getBoolean("ElementalCraft_AttributesSet")
        ).forEach(PlayerTrackEvents::applyElementalAttributes);
    }

    /**
     * 为单个生物确定并应用元素属性的核心逻辑。
     *
     * Core logic to determine and apply attributes to a single mob.
     */
    private static void applyElementalAttributes(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        if (data.getBoolean("ElementalCraft_AttributesSet")) return;

        String entityId = net.minecraft.world.entity.EntityType.getKey(mob.getType()).toString();

        // 优先级 1：黑名单 / Priority 1: Blacklist
        if (ElementalConfig.BLACKLISTED_ENTITIES.get().contains(entityId)) {
            data.putBoolean("ElementalCraft_AttributesSet", true);
            return;
        }

        // 优先级 2：配置强制属性 / Priority 2: Custom Forced Attributes (Config)
        ForcedAttributeHelper.ForcedData forced = ForcedAttributeHelper.getForcedData(mob.getType());
        boolean dimensionForced = false; // 标记是否为维度强制（仅下界/末地）/ whether forced by dimension

        // 优先级 3：下界强制赤焰 / Priority 3: Nether Dimension Forced Fire
        if (forced == null && ElementalConfig.NETHER_DIMENSION_FORCED_FIRE.get()
                && mob.level().dimension() == Level.NETHER) {
            int points = ElementalConfig.NETHER_FIRE_POINTS.get();
            forced = new ForcedAttributeHelper.ForcedData(
                    ElementType.FIRE, ElementType.FIRE, points,
                    ElementType.FIRE, points
            );
            dimensionForced = true; // 来自维度强制，下界 / dimension-forced (Nether)
        }

        // 优先级 4：末地强制雷霆 / Priority 4: End Dimension Forced Thunder
        if (forced == null && ElementalConfig.END_DIMENSION_FORCED_THUNDER.get()
                && mob.level().dimension() == Level.END) {
            int points = ElementalConfig.END_THUNDER_POINTS.get();
            forced = new ForcedAttributeHelper.ForcedData(
                    ElementType.THUNDER, ElementType.THUNDER, points,
                    ElementType.THUNDER, points
            );
            dimensionForced = true; // 来自维度强制，末地 / dimension-forced (End)
        }

        // 如果存在强制属性，应用并标记
        // Apply forced attributes if present. Pass dimensionForced flag so that "fill missing armor"
        // behavior is only executed when dimensionForced == true (Nether or End).
        if (forced != null) {
            applyForcedAttributes(mob, forced, dimensionForced);
            data.putBoolean("ElementalCraft_AttributesSet", true);
            return;
        }

        // 跳过非怪物实体 / Skip non-monster entities
        if (!(mob instanceof Monster)) {
            data.putBoolean("ElementalCraft_AttributesSet", true);
            return;
        }

        // 随机概率决定是否生成属性 / Global probability check for random generation
        if (RANDOM.nextDouble() >= ElementalConfig.MOB_ATTRIBUTE_CHANCE_HOSTILE.get()) {
            data.putBoolean("ElementalCraft_AttributesSet", true);
            return;
        }

        // 标记为已处理 / Mark as processed
        data.putBoolean("ElementalCraft_AttributesSet", true);

        // ==================== 随机生成逻辑 / Core Random Generation Logic ====================

        boolean hasHandItem = !mob.getMainHandItem().isEmpty() || !mob.getOffhandItem().isEmpty();

        List<ItemStack> armorPieces = new ArrayList<>();
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack item = mob.getItemBySlot(slot);
            if (!item.isEmpty()) armorPieces.add(item);
        }
        int pieceCount = armorPieces.isEmpty() ? 1 : armorPieces.size();

        // 1. 基于生物群系和天气确定主元素 / Determine Main Element based on Biome and Weather
        ElementType mainType = BiomeAttributeBias.getBiasedRandomElement((ServerLevel) mob.level(), mob.blockPosition());

        // 2. 确定攻击和强化类型（稍后可能被强制物品覆盖） / Determine Attack and Enhance types
        ElementType attackType = hasHandItem ? mainType : null;
        ElementType enhanceType = mainType;
        int enhanceTotalPoints = hasHandItem ? ElementalConfig.rollMonsterStrength() : 0;

        // 3. 确定抗性类型 / Determine Resistance Type
        ElementType resistType;
        if (attackType != null && RANDOM.nextDouble() < ElementalConfig.COUNTER_RESIST_CHANCE.get()) {
            ElementType weakness = switch (attackType) {
                case FIRE -> ElementType.FROST;
                case FROST -> ElementType.NATURE;
                case NATURE -> ElementType.THUNDER;
                case THUNDER -> ElementType.FIRE;
                default -> null;
            };
            resistType = (weakness != null) ? weakness : mainType;
        } else {
            resistType = randomNonNoneElement();
        }
        int resistTotalPoints = ElementalConfig.rollMonsterResist();

        // ==================== 强制物品逻辑（高优先级） / Forced Item Logic (High Priority) ====================

        // 武器强制属性检查 / Check Weapon for Forced Attributes
        if (hasHandItem) {
            ItemStack weapon = !mob.getMainHandItem().isEmpty() ? mob.getMainHandItem() : mob.getOffhandItem();
            ForcedItemHelper.WeaponData weaponData = ForcedItemHelper.getForcedWeapon(weapon.getItem());
            if (weaponData != null && weaponData.attackType() != null) {
                Enchantment ench = getAttackEnchantment(weaponData.attackType());
                if (ench != null) {
                    weapon.enchant(ench, 1);
                }
                attackType = null; // 防止随机覆盖 / Prevent random overwrite
            }
        }

        // 护甲强制属性检查 / Check Armor for Forced Attributes
        boolean armorForced = false;
        if (!armorPieces.isEmpty()) {
            for (ItemStack armor : armorPieces) {
                ForcedItemHelper.ArmorData armorData = ForcedItemHelper.getForcedArmor(armor.getItem());
                if (armorData != null) {
                    Map<Enchantment, Integer> map = new HashMap<>();
                    if (armorData.enhanceType() != null && armorData.enhancePoints() > 0) {
                        int[] lv = distributePointsToLevels(armorData.enhancePoints(), ElementalConfig.getStrengthPerLevel(), 1);
                        Enchantment ench = getEnhancementEnchantment(armorData.enhanceType());
                        if (ench != null) map.put(ench, lv[0]);
                    }
                    if (armorData.resistType() != null && armorData.resistPoints() > 0) {
                        int[] lv = distributePointsToLevels(armorData.resistPoints(), ElementalConfig.getResistPerLevel(), 1);
                        Enchantment ench = getResistanceEnchantment(armorData.resistType());
                        if (ench != null) map.put(ench, lv[0]);
                    }
                    if (!map.isEmpty()) {
                        EnchantmentHelper.setEnchantments(map, armor);
                        armor.addTagElement("HideFlags", ByteTag.valueOf((byte)2));
                    }
                    armorForced = true;
                }
            }
            // 如果护甲被强制，清除标准计算以防冲突 / If armor is forced, clear standard calculations
            if (armorForced) {
                enhanceType = null;
                resistType = null;
                enhanceTotalPoints = 0;
                resistTotalPoints = 0;
            }
        }

        // ==================== 应用标准随机附魔 / Apply Standard Random Enchantments ====================

        int[] resistLevels = distributePointsToLevels(resistTotalPoints, ElementalConfig.getResistPerLevel(), pieceCount);
        int[] enhanceLevels = enhanceTotalPoints > 0
                ? distributePointsToLevels(enhanceTotalPoints, ElementalConfig.getStrengthPerLevel(), pieceCount)
                : null;

        // 武器应用 / Apply to Weapon (If not overridden)
        if (hasHandItem && attackType != null) {
            ItemStack weapon = !mob.getMainHandItem().isEmpty() ? mob.getMainHandItem() : mob.getOffhandItem();
            Enchantment ench = getAttackEnchantment(attackType);
            if (ench != null) weapon.enchant(ench, 1);
        }

        // 护甲应用 / Apply to Armor
        if (!armorPieces.isEmpty() && !armorForced) {
            for (int i = 0; i < armorPieces.size(); i++) {
                ItemStack armor = armorPieces.get(i);
                Map<Enchantment, Integer> map = new HashMap<>();
                Enchantment resistEnch = getResistanceEnchantment(resistType);
                if (resistEnch != null) map.put(resistEnch, resistLevels[i]);
                if (enhanceLevels != null) {
                    Enchantment enhEnch = getEnhancementEnchantment(enhanceType);
                    if (enhEnch != null) map.put(enhEnch, enhanceLevels[i]);
                }
                if (!map.isEmpty()) {
                    EnchantmentHelper.setEnchantments(map, armor);
                    armor.addTagElement("HideFlags", ByteTag.valueOf((byte)2));
                }
            }
        } else if (armorPieces.isEmpty() && !armorForced) {
            // 无护甲 -> 创建隐形头盔以存储属性 / No Armor -> Create Invisible Helmet to hold stats
            ItemStack helmet = createInvisibleHelmet();
            Map<Enchantment, Integer> map = new HashMap<>();
            int resistLv = Math.max(1, Math.min(5, resistTotalPoints / ElementalConfig.getResistPerLevel()));
            Enchantment resistEnch = getResistanceEnchantment(resistType);
            if (resistEnch != null) map.put(resistEnch, resistLv);
            if (enhanceLevels != null) {
                int enhanceLv = Math.max(1, Math.min(5, enhanceTotalPoints / ElementalConfig.getStrengthPerLevel()));
                Enchantment enhEnch = getEnhancementEnchantment(enhanceType);
                if (enhEnch != null) map.put(enhEnch, enhanceLv);
            }
            EnchantmentHelper.setEnchantments(map, helmet);
            mob.setItemSlot(EquipmentSlot.HEAD, helmet);
        }
    }

    /**
     * 当生物被配置或维度强制时应用属性。
     *
     * Apply attributes when a mob is forced by config or dimension.
     *
     * @param mob             目标生物 / target mob
     * @param data            强制属性数据 / forced attribute data
     * @param dimensionForced 是否来自维度强制（仅下界/末地）/ whether this forced data was created due to dimension (Nether/End)
     *
     * 修正要点（仅在 dimensionForced == true 时补齐缺失护甲槽）：
     * - 如果生物已有部分护甲：为已有护甲写入最高等级属性；若有缺失槽且 dimensionForced 为 true，则补齐铁质护甲并写入对应最高等级属性。
     * - 如果生物无护甲：保持原行为（创建全套铁甲并写入属性）。
     *
     * Fix summary (fill missing armor only when dimensionForced == true):
     * - If mob has some armor: write highest-level attributes to existing pieces; if some slots are missing and dimensionForced is true,
     *   fill missing slots with iron armor and apply highest-level attributes.
     * - If mob has no armor: create full iron set and apply attributes (same as original).
     */
    private static void applyForcedAttributes(Mob mob, ForcedAttributeHelper.ForcedData data, boolean dimensionForced) {
        ElementType attackType   = data.attackType();
        ElementType enhanceType  = data.enhanceType();
        int enhancePoints        = data.enhancePoints();
        ElementType resistType   = data.resistType();
        int resistPoints         = data.resistPoints();

        int enhancePerLevel = ElementalConfig.getStrengthPerLevel();
        int resistPerLevel  = ElementalConfig.getResistPerLevel();

        // 分配到 4 个护甲槽的等级（用于补齐或分配）
        int[] enhanceLv = distributePointsToLevels(enhancePoints, enhancePerLevel, 4);
        int[] resistLv  = distributePointsToLevels(resistPoints, resistPerLevel, 4);

        ItemStack mainHand = mob.getMainHandItem();
        ItemStack offHand  = mob.getOffhandItem();
        boolean hasWeapon = !mainHand.isEmpty() || !offHand.isEmpty();

        // 武器优先检查强制物品覆盖 / Weapon Logic: Check for forced item overrides first
        if (hasWeapon) {
            ItemStack weapon = !mainHand.isEmpty() ? mainHand : offHand;
            ForcedItemHelper.WeaponData weaponData = ForcedItemHelper.getForcedWeapon(weapon.getItem());
            if (weaponData != null && weaponData.attackType() != null) {
                attackType = weaponData.attackType(); // Item overrides entity config
            }
        }

        // 应用攻击附魔或创建武器 / Apply Attack Enchantment or Create Sword
        if (attackType != null && attackType != ElementType.NONE) {
            Enchantment attackEnchant = getAttackEnchantment(attackType);
            if (attackEnchant != null) {
                if (hasWeapon) {
                    if (!mainHand.isEmpty()) mainHand.enchant(attackEnchant, 1);
                    if (!offHand.isEmpty()) offHand.enchant(attackEnchant, 1);
                } else {
                    ItemStack sword = new ItemStack(Items.IRON_SWORD);
                    sword.enchant(attackEnchant, 1);
                    mob.setItemSlot(EquipmentSlot.MAINHAND, sword);
                }
            }
        }

        // 当前护甲数组（顺序：feet, legs, chest, head）
        ItemStack[] currentArmor = {
                mob.getItemBySlot(EquipmentSlot.FEET),
                mob.getItemBySlot(EquipmentSlot.LEGS),
                mob.getItemBySlot(EquipmentSlot.CHEST),
                mob.getItemBySlot(EquipmentSlot.HEAD)
        };

        boolean hasAnyArmor = Arrays.stream(currentArmor).anyMatch(s -> !s.isEmpty());

        if (hasAnyArmor) {
            // 生物已有部分或全部护甲：为已有护甲写入最高等级属性；对缺失槽在 dimensionForced 为 true 时补齐铁质护甲并写入最高等级属性
            List<Integer> missingIndices = new ArrayList<>();
            for (int i = 0; i < currentArmor.length; i++) {
                ItemStack armor = currentArmor[i];
                if (!armor.isEmpty()) {
                    // 优先检查物品强制配置
                    ForcedItemHelper.ArmorData armorData = ForcedItemHelper.getForcedArmor(armor.getItem());
                    Map<Enchantment, Integer> map = new HashMap<>();

                    if (armorData != null) {
                        // 应用物品特定配置
                        if (armorData.enhanceType() != null && armorData.enhancePoints() > 0) {
                            int[] lv = distributePointsToLevels(armorData.enhancePoints(), enhancePerLevel, 1);
                            Enchantment ench = getEnhancementEnchantment(armorData.enhanceType());
                            if (ench != null) map.put(ench, lv[0]);
                        }
                        if (armorData.resistType() != null && armorData.resistPoints() > 0) {
                            int[] lv = distributePointsToLevels(armorData.resistPoints(), resistPerLevel, 1);
                            Enchantment ench = getResistanceEnchantment(armorData.resistType());
                            if (ench != null) map.put(ench, lv[0]);
                        }
                    } else {
                        // 使用实体强制数据的最高等级（enhanceLv/resistLv）
                        if (enhanceType != null && enhanceType != ElementType.NONE) {
                            Enchantment ench = getEnhancementEnchantment(enhanceType);
                            if (ench != null) map.put(ench, Math.max(1, Math.min(5, enhanceLv[i])));
                        }
                        if (resistType != null && resistType != ElementType.NONE) {
                            Enchantment ench = getResistanceEnchantment(resistType);
                            if (ench != null) map.put(ench, Math.max(1, Math.min(5, resistLv[i])));
                        }
                    }

                    if (!map.isEmpty()) {
                        EnchantmentHelper.setEnchantments(map, armor);
                        armor.addTagElement("HideFlags", ByteTag.valueOf((byte)2));
                    }
                } else {
                    // 记录缺失槽，稍后根据 dimensionForced 决定是否补齐
                    missingIndices.add(i);
                }
            }

            // 仅在维度强制（Nether/End）时补齐缺失槽
            if (dimensionForced && !missingIndices.isEmpty()) {
                MinecraftServer server = mob.level().getServer();
                if (server != null) {
                    final ItemStack[] toEquip = new ItemStack[missingIndices.size()];
                    for (int idx = 0; idx < missingIndices.size(); idx++) {
                        int slotIndex = missingIndices.get(idx);
                        ItemStack newArmor = switch (slotIndex) {
                            case 0 -> new ItemStack(Items.IRON_BOOTS);
                            case 1 -> new ItemStack(Items.IRON_LEGGINGS);
                            case 2 -> new ItemStack(Items.IRON_CHESTPLATE);
                            case 3 -> new ItemStack(Items.IRON_HELMET);
                            default -> new ItemStack(Items.IRON_CHESTPLATE);
                        };

                        Map<Enchantment, Integer> map = new HashMap<>();
                        if (enhanceType != null && enhanceType != ElementType.NONE) {
                            Enchantment ench = getEnhancementEnchantment(enhanceType);
                            if (ench != null) map.put(ench, Math.max(1, Math.min(5, enhanceLv[slotIndex])));
                        }
                        if (resistType != null && resistType != ElementType.NONE) {
                            Enchantment ench = getResistanceEnchantment(resistType);
                            if (ench != null) map.put(ench, Math.max(1, Math.min(5, resistLv[slotIndex])));
                        }
                        if (!map.isEmpty()) {
                            EnchantmentHelper.setEnchantments(map, newArmor);
                            newArmor.addTagElement("HideFlags", ByteTag.valueOf((byte)2));
                        }
                        toEquip[idx] = newArmor.copy();
                    }

                    // 分别在后续 ticks 装备，避免视觉/同步问题
                    for (int i = 0; i < missingIndices.size(); i++) {
                        final int slotIndex = missingIndices.get(i);
                        final ItemStack armorToSet = toEquip[i];
                        server.tell(new TickTask(server.getTickCount() + i, () -> {
                            if (!mob.isAlive()) return;
                            mob.setItemSlot(EquipmentSlot.values()[slotIndex + 2], armorToSet);
                        }));
                    }
                }
            }
        } else {
            // 生物无护甲 -> 创建全套铁甲并写入属性（与原实现一致）
            ItemStack[] newArmor = {
                    new ItemStack(Items.IRON_BOOTS),
                    new ItemStack(Items.IRON_LEGGINGS),
                    new ItemStack(Items.IRON_CHESTPLATE),
                    new ItemStack(Items.IRON_HELMET)
            };

            for (int i = 0; i < 4; i++) {
                Map<Enchantment, Integer> map = new HashMap<>();
                if (enhanceType != null && enhanceType != ElementType.NONE) {
                    Enchantment ench = getEnhancementEnchantment(enhanceType);
                    if (ench != null) map.put(ench, Math.max(1, Math.min(5, enhanceLv[i])));
                }
                if (resistType != null && resistType != ElementType.NONE) {
                    Enchantment ench = getResistanceEnchantment(resistType);
                    if (ench != null) map.put(ench, Math.max(1, Math.min(5, resistLv[i])));
                }
                if (!map.isEmpty()) {
                    EnchantmentHelper.setEnchantments(map, newArmor[i]);
                    newArmor[i].addTagElement("HideFlags", ByteTag.valueOf((byte)2));
                }
            }

            MinecraftServer server = mob.level().getServer();
            if (server != null) {
                final ItemStack[] finalArmor = Arrays.stream(newArmor).map(ItemStack::copy).toArray(ItemStack[]::new);
                for (int i = 0; i < 4; i++) {
                    int slotIndex = i;
                    server.tell(new TickTask(server.getTickCount() + i, () -> {
                        if (!mob.isAlive()) return;
                        mob.setItemSlot(EquipmentSlot.values()[slotIndex + 2], finalArmor[slotIndex]);
                    }));
                }
            }
        }
    }

    // ==================== 工具方法 / Utility Methods ====================

    /**
     * 返回一个随机的有效元素（排除 NONE）。
     *
     * Returns a random valid element (excludes NONE).
     */
    private static ElementType randomNonNoneElement() {
        ElementType[] valid = {ElementType.FIRE, ElementType.NATURE, ElementType.FROST, ElementType.THUNDER};
        return valid[RANDOM.nextInt(valid.length)];
    }

    private static Enchantment getAttackEnchantment(ElementType type) {
        return switch (type) {
            case FIRE    -> ModEnchantments.FIRE_STRIKE.get();
            case NATURE  -> ModEnchantments.NATURE_STRIKE.get();
            case FROST   -> ModEnchantments.FROST_STRIKE.get();
            case THUNDER -> ModEnchantments.THUNDER_STRIKE.get();
            default      -> null;
        };
    }

    private static Enchantment getEnhancementEnchantment(ElementType type) {
        return switch (type) {
            case FIRE    -> ModEnchantments.FIRE_ENHANCE.get();
            case NATURE  -> ModEnchantments.NATURE_ENHANCE.get();
            case FROST   -> ModEnchantments.FROST_ENHANCE.get();
            case THUNDER -> ModEnchantments.THUNDER_ENHANCE.get();
            default      -> null;
        };
    }

    private static Enchantment getResistanceEnchantment(ElementType type) {
        return switch (type) {
            case FIRE    -> ModEnchantments.FIRE_RESIST.get();
            case NATURE  -> ModEnchantments.NATURE_RESIST.get();
            case FROST   -> ModEnchantments.FROST_RESIST.get();
            case THUNDER -> ModEnchantments.THUNDER_RESIST.get();
            default      -> null;
        };
    }

    /**
     * 创建一个带标记的隐形皮革头盔，用于在无护甲时存储属性。
     *
     * Creates an invisible Leather Helmet with a marker tag.
     * Used for storing attributes on mobs without armor.
     */
    private static ItemStack createInvisibleHelmet() {
        ItemStack helmet = new ItemStack(Items.LEATHER_HELMET);
        CompoundTag tag = helmet.getOrCreateTag();
        tag.putBoolean("Unbreakable", true);
        tag.putInt("HideFlags", 127);
        tag.putString("elementalcraft_marker", "invisible_resist");
        CompoundTag display = new CompoundTag();
        display.putInt("color", 0);
        tag.put("display", display);
        return helmet;
    }

    /**
     * 将总点数分配到护甲部位的附魔等级上，尽量均匀分配并限制最大等级（通常为5）。
     *
     * Distributes total attribute points into enchantment levels across available armor pieces.
     * Ensures levels do not exceed vanilla limits (usually 5) where possible.
     */
    private static int[] distributePointsToLevels(int totalPoints, int pointsPerLevel, int pieceCount) {
        int totalLevelsNeeded = totalPoints / pointsPerLevel;
        int[] levels = new int[pieceCount];
        Arrays.fill(levels, 1);
        int remaining = totalLevelsNeeded - pieceCount;
        if (remaining <= 0) return levels;

        for (int i = 0; i < remaining; i++) {
            List<Integer> candidates = new ArrayList<>();
            for (int j = 0; j < pieceCount; j++) {
                if (levels[j] < 5) candidates.add(j);
            }
            if (candidates.isEmpty()) break;
            int chosen = candidates.get(RANDOM.nextInt(candidates.size()));
            levels[chosen]++;
        }
        return levels;
    }
}