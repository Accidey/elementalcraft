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
 * Handles logic for scanning nearby mobs and applying elemental attributes to them.
 * <p>
 * 处理扫描附近生物并为其应用元素属性的逻辑。
 */
@Mod.EventBusSubscriber(modid = "elementalcraft")
@SuppressWarnings("null") // Suppress Null type safety warnings (Safe and recommended) / 压制空指针安全警告（安全且推荐）
public class PlayerTrackEvents {

    private static final Random RANDOM = new Random();
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    /**
     * Periodically scans for mobs around the player to apply attributes.
     * <p>
     * 周期性扫描玩家周围的生物以应用属性。
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // Server-side only check
        // 仅在服务端执行
        if (event.side.isClient())  return;
        
        // Execute at the start of the tick
        // 在 tick 开始时执行
        if (event.phase != TickEvent.Phase.START) return;
        
        // Ensure it's a server player
        // 确保是服务端玩家
        if (!(event.player instanceof ServerPlayer player)) return;
        
        // Run logic once every second (20 ticks)
        // 每秒（20 ticks）执行一次逻辑
        if (player.tickCount % 20 != 0) return;

        ServerLevel level = player.serverLevel();
        // Scan for mobs within a 16x8x16 area
        // 扫描 16x8x16 区域内的生物
        level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(16, 8, 16),
                // Filter: Must be alive and not processed yet
                // 过滤：必须存活且尚未处理过
                mob -> mob.isAlive() && !mob.getPersistentData().getBoolean("ElementalCraft_AttributesSet")
        ).forEach(PlayerTrackEvents::applyElementalAttributes);
    }

    /**
     * Core logic to determine and apply attributes to a single mob.
     * <p>
     * 确定并应用属性到单个生物的核心逻辑。
     */
    private static void applyElementalAttributes(Mob mob) {
        CompoundTag data = mob.getPersistentData();
        // Double-check to prevent duplicate processing
        // 二次检查以防止重复处理
        if (data.getBoolean("ElementalCraft_AttributesSet")) return;

        String entityId = net.minecraft.world.entity.EntityType.getKey(mob.getType()).toString();

        // Priority 1: Blacklist System
        // 优先级 1：黑名单系统
        if (ElementalConfig.BLACKLISTED_ENTITIES.get().contains(entityId)) {
            data.putBoolean("ElementalCraft_AttributesSet", true);
            return;
        }

        // Priority 2: Custom Forced Attributes (Config)
        // 优先级 2：自定义强制属性（配置）
        ForcedAttributeHelper.ForcedData forced = ForcedAttributeHelper.getForcedData(mob.getType());

        // Priority 3: Nether Dimension Forced Fire
        // 优先级 3：下界维度强制赤焰属性
        if (forced == null && ElementalConfig.NETHER_DIMENSION_FORCED_FIRE.get()
                && mob.level().dimension() == Level.NETHER) {
            int points = ElementalConfig.NETHER_FIRE_POINTS.get();
            forced = new ForcedAttributeHelper.ForcedData(
                    ElementType.FIRE, ElementType.FIRE, points,
                    ElementType.FIRE, points
            );
        }

        // Priority 4: End Dimension Forced Thunder
        // 优先级 4：末地维度强制雷霆属性
        if (forced == null && ElementalConfig.END_DIMENSION_FORCED_THUNDER.get()
                && mob.level().dimension() == Level.END) {
            int points = ElementalConfig.END_THUNDER_POINTS.get();
            forced = new ForcedAttributeHelper.ForcedData(
                    ElementType.THUNDER, ElementType.THUNDER, points,
                    ElementType.THUNDER, points
            );
        }

        // Apply forced attributes if any condition above is met
        // 如果满足上述任一条件，应用强制属性
        if (forced != null) {
            applyForcedAttributes(mob, forced);
            data.putBoolean("ElementalCraft_AttributesSet", true);
            return;
        }

        // Skip non-monster entities (Passive mobs)
        // 跳过非怪物实体（被动生物）
        if (!(mob instanceof Monster)) {
            data.putBoolean("ElementalCraft_AttributesSet", true);
            return;
        }

        // Global probability check for random generation
        // 随机生成的全局概率检查
        if (RANDOM.nextDouble() >= ElementalConfig.MOB_ATTRIBUTE_CHANCE_HOSTILE.get()) {
            data.putBoolean("ElementalCraft_AttributesSet", true);
            return;
        }

        // Mark as processed
        // 标记为已处理
        data.putBoolean("ElementalCraft_AttributesSet", true);

        // ==================== Core Random Generation Logic / 核心随机生成逻辑 ====================

        boolean hasHandItem = !mob.getMainHandItem().isEmpty() || !mob.getOffhandItem().isEmpty();

        List<ItemStack> armorPieces = new ArrayList<>();
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack item = mob.getItemBySlot(slot);
            if (!item.isEmpty()) armorPieces.add(item);
        }
        int pieceCount = armorPieces.isEmpty() ? 1 : armorPieces.size();

        // 1. Determine Main Element based on Biome and Weather
        // 1. 基于生物群系和天气确定主元素
        ElementType mainType = BiomeAttributeBias.getBiasedRandomElement((ServerLevel) mob.level(), mob.blockPosition());

        // 2. Determine Attack and Enhance types (May be overridden by forced items later)
        // 2. 确定攻击和强化类型（稍后可能会被强制物品覆盖）
        ElementType attackType = hasHandItem ? mainType : null;
        ElementType enhanceType = mainType;
        int enhanceTotalPoints = hasHandItem ? ElementalConfig.rollMonsterStrength() : 0;

        // 3. Determine Resistance Type (Chance to counter weakness or random)
        // 3. 确定抗性类型（有概率获得针对弱点的抗性，或随机）
        ElementType resistType;
        if (attackType != null && RANDOM.nextDouble() < ElementalConfig.COUNTER_RESIST_CHANCE.get()) {
            // Calculate weakness to cover own weakness
            // 计算弱点以弥补自身弱点
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

        // ==================== Forced Item Logic (High Priority) / 强制物品逻辑（高优先级） ====================

        // Check Weapon for Forced Attributes
        // 检查武器是否有强制属性
        if (hasHandItem) {
            ItemStack weapon = !mob.getMainHandItem().isEmpty() ? mob.getMainHandItem() : mob.getOffhandItem();
            ForcedItemHelper.WeaponData weaponData = ForcedItemHelper.getForcedWeapon(weapon.getItem());
            if (weaponData != null && weaponData.attackType() != null) {
                Enchantment ench = getAttackEnchantment(weaponData.attackType());
                if (ench != null) {
                    weapon.enchant(ench, 1);
                }
                attackType = null; // Prevent random overwrite / 阻止随机覆盖
            }
        }

        // Check Armor for Forced Attributes
        // 检查护甲是否有强制属性
        boolean armorForced = false;
        if (!armorPieces.isEmpty()) {
            for (ItemStack armor : armorPieces) {
                ForcedItemHelper.ArmorData armorData = ForcedItemHelper.getForcedArmor(armor.getItem());
                if (armorData != null) {
                    Map<Enchantment, Integer> map = new HashMap<>();
                    if (armorData.enhanceType() != null && armorData.enhancePoints() > 0) {
                        int[] lv = distributePointsToLevels(armorData.enhancePoints(), ElementalConfig.getStrengthPerLevel(), 1);
                        map.put(getEnhancementEnchantment(armorData.enhanceType()), lv[0]);
                    }
                    if (armorData.resistType() != null && armorData.resistPoints() > 0) {
                        int[] lv = distributePointsToLevels(armorData.resistPoints(), ElementalConfig.getResistPerLevel(), 1);
                        map.put(getResistanceEnchantment(armorData.resistType()), lv[0]);
                    }
                    if (!map.isEmpty()) {
                        EnchantmentHelper.setEnchantments(map, armor);
                        armor.addTagElement("HideFlags", ByteTag.valueOf((byte)2));
                    }
                    armorForced = true;
                }
            }
            // If armor is forced, clear standard calculations to prevent conflict
            // 如果护甲被强制，清除标准计算以防止冲突
            if (armorForced) {
                enhanceType = null;
                resistType = null;
                enhanceTotalPoints = 0;
                resistTotalPoints = 0;
            }
        }

        // ==================== Apply Standard Random Enchantments / 应用标准随机附魔 ====================
        
        // Distribute points to levels
        // 将点数分配到等级
        int[] resistLevels = distributePointsToLevels(resistTotalPoints, ElementalConfig.getResistPerLevel(), pieceCount);
        int[] enhanceLevels = enhanceTotalPoints > 0
                ? distributePointsToLevels(enhanceTotalPoints, ElementalConfig.getStrengthPerLevel(), pieceCount)
                : null;

        // Apply to Weapon (If not overridden)
        // 应用到武器（如果未被覆盖）
        if (hasHandItem && attackType != null) {
            ItemStack weapon = !mob.getMainHandItem().isEmpty() ? mob.getMainHandItem() : mob.getOffhandItem();
            Enchantment ench = getAttackEnchantment(attackType);
            if (ench != null) weapon.enchant(ench, 1);
        }

        // Apply to Armor
        // 应用到护甲
        if (!armorPieces.isEmpty() && !armorForced) {
            for (int i = 0; i < armorPieces.size(); i++) {
                ItemStack armor = armorPieces.get(i);
                Map<Enchantment, Integer> map = new HashMap<>();
                map.put(getResistanceEnchantment(resistType), resistLevels[i]);
                if (enhanceLevels != null) {
                    map.put(getEnhancementEnchantment(enhanceType), enhanceLevels[i]);
                }
                if (!map.isEmpty()) {
                    EnchantmentHelper.setEnchantments(map, armor);
                    armor.addTagElement("HideFlags", ByteTag.valueOf((byte)2));
                }
            }
        } else if (armorPieces.isEmpty() && !armorForced) {
            // No Armor -> Create Invisible Helmet to hold stats
            // 无护甲 -> 创建隐形头盔以存储属性
            ItemStack helmet = createInvisibleHelmet();
            Map<Enchantment, Integer> map = new HashMap<>();
            int resistLv = Math.max(1, Math.min(5, resistTotalPoints / ElementalConfig.getResistPerLevel()));
            map.put(getResistanceEnchantment(resistType), resistLv);
            if (enhanceLevels != null) {
                int enhanceLv = Math.max(1, Math.min(5, enhanceTotalPoints / ElementalConfig.getStrengthPerLevel()));
                map.put(getEnhancementEnchantment(enhanceType), enhanceLv);
            }
            EnchantmentHelper.setEnchantments(map, helmet);
            mob.setItemSlot(EquipmentSlot.HEAD, helmet);
        }
    }

    /**
     * Logic for applying attributes when a mob is forced by config or dimension.
     * Includes creating weapons/armor if the mob is naked.
     * <p>
     * 当生物被配置或维度强制时的属性应用逻辑。
     * 包括在怪物无装备时创建武器/护甲。
     */
    private static void applyForcedAttributes(Mob mob, ForcedAttributeHelper.ForcedData data) {
        ElementType attackType   = data.attackType();
        ElementType enhanceType  = data.enhanceType();
        int enhancePoints        = data.enhancePoints();
        ElementType resistType   = data.resistType();
        int resistPoints         = data.resistPoints();

        int enhancePerLevel = ElementalConfig.getStrengthPerLevel();
        int resistPerLevel  = ElementalConfig.getResistPerLevel();

        int[] enhanceLv = distributePointsToLevels(enhancePoints, enhancePerLevel, 4);
        int[] resistLv  = distributePointsToLevels(resistPoints, resistPerLevel, 4);

        ItemStack mainHand = mob.getMainHandItem();
        ItemStack offHand  = mob.getOffhandItem();
        boolean hasWeapon = !mainHand.isEmpty() || !offHand.isEmpty();

        // Weapon Logic: Check for forced item overrides first
        // 武器逻辑：优先检查强制物品覆盖
        if (hasWeapon) {
            ItemStack weapon = !mainHand.isEmpty() ? mainHand : offHand;
            ForcedItemHelper.WeaponData weaponData = ForcedItemHelper.getForcedWeapon(weapon.getItem());
            if (weaponData != null && weaponData.attackType() != null) {
                attackType = weaponData.attackType(); // Item overrides entity config / 物品覆盖实体配置
            }
        }

        // Apply Attack Enchantment or Create Sword
        // 应用攻击附魔或创建剑
        if (attackType != null && attackType != ElementType.NONE) {
            Enchantment attackEnchant = getAttackEnchantment(attackType);
            if (attackEnchant != null) {
                if (hasWeapon) {
                    if (!mainHand.isEmpty()) mainHand.enchant(attackEnchant, 1);
                    if (!offHand.isEmpty()) offHand.enchant(attackEnchant, 1);
                } else {
                    // Give Iron Sword if no weapon
                    // 如果没有武器，给予铁剑
                    ItemStack sword = new ItemStack(Items.IRON_SWORD);
                    sword.enchant(attackEnchant, 1);
                    mob.setItemSlot(EquipmentSlot.MAINHAND, sword);
                }
            }
        }

        ItemStack[] currentArmor = {
                mob.getItemBySlot(EquipmentSlot.FEET),
                mob.getItemBySlot(EquipmentSlot.LEGS),
                mob.getItemBySlot(EquipmentSlot.CHEST),
                mob.getItemBySlot(EquipmentSlot.HEAD)
        };

        boolean hasAnyArmor = Arrays.stream(currentArmor).anyMatch(s -> !s.isEmpty());

        if (hasAnyArmor) {
            // Mob has armor, enchant existing items
            // 怪物有护甲，附魔现有物品
            for (int i = 0; i < currentArmor.length; i++) {
                ItemStack armor = currentArmor[i];
                if (armor.isEmpty()) continue;

                // Priority Check: Forced Item Attributes
                // 优先级检查：强制物品属性
                ForcedItemHelper.ArmorData armorData = ForcedItemHelper.getForcedArmor(armor.getItem());
                Map<Enchantment, Integer> map = new HashMap<>();

                if (armorData != null) {
                    // Apply Item specific config
                    // 应用物品特定配置
                    if (armorData.enhanceType() != null && armorData.enhancePoints() > 0) {
                        int[] lv = distributePointsToLevels(armorData.enhancePoints(), enhancePerLevel, 1);
                        map.put(getEnhancementEnchantment(armorData.enhanceType()), lv[0]);
                    }
                    if (armorData.resistType() != null && armorData.resistPoints() > 0) {
                        int[] lv = distributePointsToLevels(armorData.resistPoints(), resistPerLevel, 1);
                        map.put(getResistanceEnchantment(armorData.resistType()), lv[0]);
                    }
                } else {
                    // Apply Entity specific config
                    // 应用实体特定配置
                    if (enhanceType != null && enhanceType != ElementType.NONE) {
                        map.put(getEnhancementEnchantment(enhanceType), enhanceLv[i]);
                    }
                    if (resistType != null && resistType != ElementType.NONE) {
                        map.put(getResistanceEnchantment(resistType), resistLv[i]);
                    }
                }

                if (!map.isEmpty()) {
                    EnchantmentHelper.setEnchantments(map, armor);
                    armor.addTagElement("HideFlags", ByteTag.valueOf((byte)2));
                }
            }
        } else {
            // No Armor -> Create full Iron Armor set with attributes
            // 无护甲 -> 创建带有属性的全套铁甲
            ItemStack[] newArmor = {
                    new ItemStack(Items.IRON_BOOTS),
                    new ItemStack(Items.IRON_LEGGINGS),
                    new ItemStack(Items.IRON_CHESTPLATE),
                    new ItemStack(Items.IRON_HELMET)
            };

            for (int i = 0; i < 4; i++) {
                Map<Enchantment, Integer> map = new HashMap<>();
                if (enhanceType != null && enhanceType != ElementType.NONE) {
                    map.put(getEnhancementEnchantment(enhanceType), enhanceLv[i]);
                }
                if (resistType != null && resistType != ElementType.NONE) {
                    map.put(getResistanceEnchantment(resistType), resistLv[i]);
                }
                if (!map.isEmpty()) {
                    EnchantmentHelper.setEnchantments(map, newArmor[i]);
                    newArmor[i].addTagElement("HideFlags", ByteTag.valueOf((byte)2));
                }
            }

            // Use TickTask to safely equip items (Prevents visual sync issues)
            // 使用 TickTask 安全地装备物品（防止视觉同步问题）
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

    // ==================== Utility Methods / 工具方法 ====================

    /**
     * Returns a random valid element (excludes NONE).
     * <p>
     * 返回一个随机的有效元素（排除 NONE）。
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
     * Creates an invisible Leather Helmet with a marker tag.
     * Used for storing attributes on mobs without armor.
     * <p>
     * 创建一个带有标记标签的隐形皮革头盔。
     * 用于在无护甲的生物上存储属性。
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
     * Distributes total attribute points into enchantment levels across available armor pieces.
     * Ensures levels do not exceed vanilla limits (usually 5) where possible.
     * <p>
     * 将总属性点数分配到可用护甲部位的附魔等级上。
     * 尽可能确保等级不超过原版限制（通常为5）。
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