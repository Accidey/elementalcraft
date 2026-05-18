package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.enchantment.ModEnchantments;
import com.xulai.elementalcraft.util.ElementType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class ElementalBookDropHandler {

    private static final String NBT_DROP_ELEMENT = "EC_DropElementType";
    private static final String NBT_DROP_ATTACK_TYPE = "EC_DropAttackType";
    private static final String NBT_DROP_ENHANCE_POINTS = "EC_DropEnhancePoints";
    private static final String NBT_DROP_RESIST_POINTS = "EC_DropResistPoints";
    private static final String NBT_DROP_RESIST_TYPE = "EC_DropResistType";
    private static final Random RANDOM = new Random();

    private enum BookType {
        ATTACK, ENHANCE, RESIST
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        CompoundTag data = entity.getPersistentData();
        if (!data.contains(NBT_DROP_ELEMENT)) return;

        int lootingLevel = event.getLootingLevel();
        double dropChance = ElementalConfig.enchantedBookDropChance + lootingLevel * ElementalConfig.enchantedBookLootingBonus;
        if (entity.level().random.nextDouble() >= dropChance) return;

        String elementId = data.getString(NBT_DROP_ELEMENT);
        ElementType elementType = ElementType.fromId(elementId);

        String attackTypeId = data.getString(NBT_DROP_ATTACK_TYPE);
        ElementType attackType = ElementType.fromId(attackTypeId);

        String resistTypeId = data.contains(NBT_DROP_RESIST_TYPE)
                ? data.getString(NBT_DROP_RESIST_TYPE)
                : elementId;
        ElementType resistType = ElementType.fromId(resistTypeId);

        int enhancePoints = data.getInt(NBT_DROP_ENHANCE_POINTS);
        int resistPoints = data.getInt(NBT_DROP_RESIST_POINTS);

        int strengthPerLevel = ElementalConfig.getStrengthPerLevel();
        int resistPerLevel = ElementalConfig.getResistPerLevel();
        int maxEnhanceLevel = Math.max(1, ElementalConfig.getMaxStatCap() / strengthPerLevel);
        int maxResistLevel = Math.max(1, ElementalConfig.getMaxStatCap() / resistPerLevel);

        List<BookCandidate> candidates = new ArrayList<>();
        if (attackType != null && attackType != ElementType.NONE) {
            candidates.add(new BookCandidate(BookType.ATTACK, attackType, 1));
        }
        if (elementType != null && elementType != ElementType.NONE && enhancePoints > 0) {
            float quality = (float) enhancePoints / 4 / strengthPerLevel;
            int maxLevel = Math.max(1, Math.min(maxEnhanceLevel, Math.round(quality)));
            int level = rollRandomLevel(quality, maxLevel, ElementalConfig.enchantedBookLevelSpread);
            candidates.add(new BookCandidate(BookType.ENHANCE, elementType, level));
        }
        if (resistType != null && resistType != ElementType.NONE && resistPoints > 0) {
            float quality = (float) resistPoints / 4 / resistPerLevel;
            int maxLevel = Math.max(1, Math.min(maxResistLevel, Math.round(quality)));
            int level = rollRandomLevel(quality, maxLevel, ElementalConfig.enchantedBookLevelSpread);
            candidates.add(new BookCandidate(BookType.RESIST, resistType, level));
        }

        if (candidates.isEmpty()) return;

        BookCandidate chosen = candidates.get(entity.level().random.nextInt(candidates.size()));
        ItemStack book = createEnchantedBook(chosen);

        ItemEntity drop = new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), book);
        event.getDrops().add(drop);
    }

    private static ItemStack createEnchantedBook(BookCandidate candidate) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        Enchantment ench = getEnchantment(candidate.type, candidate.element);
        if (ench != null) {
            EnchantedBookItem.addEnchantment(book, new EnchantmentInstance(ench, candidate.level));
        }
        return book;
    }

    private static Enchantment getEnchantment(BookType type, ElementType element) {
        return switch (type) {
            case ATTACK -> switch (element) {
                case FIRE -> ModEnchantments.FIRE_STRIKE.get();
                case NATURE -> ModEnchantments.NATURE_STRIKE.get();
                case FROST -> ModEnchantments.FROST_STRIKE.get();
                case THUNDER -> ModEnchantments.THUNDER_STRIKE.get();
                default -> null;
            };
            case ENHANCE -> switch (element) {
                case FIRE -> ModEnchantments.FIRE_ENHANCE.get();
                case NATURE -> ModEnchantments.NATURE_ENHANCE.get();
                case FROST -> ModEnchantments.FROST_ENHANCE.get();
                case THUNDER -> ModEnchantments.THUNDER_ENHANCE.get();
                default -> null;
            };
            case RESIST -> switch (element) {
                case FIRE -> ModEnchantments.FIRE_RESIST.get();
                case NATURE -> ModEnchantments.NATURE_RESIST.get();
                case FROST -> ModEnchantments.FROST_RESIST.get();
                case THUNDER -> ModEnchantments.THUNDER_RESIST.get();
                default -> null;
            };
        };
    }

    private static int rollRandomLevel(float quality, int maxLevel, double spread) {
        if (maxLevel <= 1) {
            return maxLevel;
        }

        double[] weights = new double[maxLevel];
        double totalWeight = 0.0;
        for (int i = 0; i < maxLevel; i++) {
            int level = i + 1;
            double distance = Math.abs(quality - level);
            weights[i] = 1.0 / (1.0 + distance * spread);
            totalWeight += weights[i];
        }

        double roll = RANDOM.nextDouble() * totalWeight;
        double cumulative = 0.0;
        for (int i = 0; i < maxLevel; i++) {
            cumulative += weights[i];
            if (roll < cumulative) {
                return i + 1;
            }
        }
        return maxLevel;
    }

    private record BookCandidate(BookType type, ElementType element, int level) {}
}