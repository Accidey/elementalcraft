package com.xulai.elementalcraft.logic;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.event.ScorchedHandler;
import com.xulai.elementalcraft.event.WetnessHandler;
import com.xulai.elementalcraft.event.iss.ISSCore;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.ThreadLocalRandom;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class MobPotionThrowLogic {

    private static final double THROW_RANGE = 12.0;
    private static final int MAX_MISSES = 3;
    private static final int ATTEMPT_INTERVAL = 20;

    private static final String NBT_BOTTLE_ROLLED = "EC_BottleRolled";
    private static final String NBT_BOTTLE_CD = "EC_BottleCd";
    private static final String NBT_BOTTLE_MISS = "EC_BottleMiss";
    private static final String NBT_BOTTLE_VERDICT = "EC_BottleVerdict";

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!ElementalConfig.mobPotionThrowEnabled) return;
        if (!(event.getEntity() instanceof Mob mob)) return;

        CompoundTag data = mob.getPersistentData();
        if (!data.getBoolean("ElementalCraft_AttributesSet")) return;

        if (!data.getBoolean(NBT_BOTTLE_ROLLED)) {
            tryEquip(mob, data);
            data.putBoolean(NBT_BOTTLE_ROLLED, true);
        }

        tryThrow(mob, data);
    }

    private static void tryEquip(Mob mob, CompoundTag data) {
        if (!mob.getOffhandItem().isEmpty()) return;
        if (ElementUtils.getConsistentAttackElement(mob) == ElementType.NONE) return;
        if (data.getBoolean("EC_ISS_MobCaster")) return;
        if (ElementalFireNatureReactionsConfig.wetnessNetherDimensionImmune
                && mob.level().dimension() == Level.NETHER) return;
        if (ThreadLocalRandom.current().nextDouble() >= ElementalConfig.mobBottleEquipChance) return;

        ItemStack waterBottle = new ItemStack(Items.SPLASH_POTION);
        PotionUtils.setPotion(waterBottle, Potions.WATER);
        mob.setItemSlot(EquipmentSlot.OFFHAND, waterBottle);
        mob.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
    }

    private static void tryThrow(Mob mob, CompoundTag data) {
        if (data.getBoolean("EC_ISS_MobCaster")) return;

        ItemStack offhand = mob.getOffhandItem();
        if (!offhand.is(Items.SPLASH_POTION) || PotionUtils.getPotion(offhand) != Potions.WATER) return;

        long gameTime = mob.level().getGameTime();
        if (gameTime < data.getLong(NBT_BOTTLE_CD)) return;

        long verdict = data.getLong(NBT_BOTTLE_VERDICT);
        if (verdict > 0) {
            if (gameTime < verdict) return;
            LivingEntity target = mob.getTarget();
            if (target != null && target.isAlive() && WetnessHandler.getWetnessLevel(target) > 0) {
                data.putInt(NBT_BOTTLE_MISS, 0);
                data.remove(NBT_BOTTLE_VERDICT);
            } else {
                int miss = data.getInt(NBT_BOTTLE_MISS) + 1;
                if (miss >= MAX_MISSES) {
                    data.putLong(NBT_BOTTLE_CD, gameTime + ElementalConfig.mobBottleThrowCooldown);
                    data.putInt(NBT_BOTTLE_MISS, 0);
                    data.remove(NBT_BOTTLE_VERDICT);
                    return;
                }
                data.putInt(NBT_BOTTLE_MISS, miss);
                data.remove(NBT_BOTTLE_VERDICT);
            }
        }

        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return;

        if (mob.distanceToSqr(target) > THROW_RANGE * THROW_RANGE) return;
        if (!mob.getSensing().hasLineOfSight(target)) return;

        if (ScorchedHandler.isScorched(target)) return;
        if (ElementalFireNatureReactionsConfig.wetnessMaxLevel <= 0) return;
        if (ElementalFireNatureReactionsConfig.wetnessNetherDimensionImmune
                && target.level().dimension() == Level.NETHER) return;
        if (WetnessHandler.getWetnessLevel(target) > 0) return;

        ISSCore.throwSplashWaterBottle(mob, target);
        data.putLong(NBT_BOTTLE_VERDICT, gameTime + ATTEMPT_INTERVAL);
    }
}
