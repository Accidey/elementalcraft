package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class ScorchedHandler {

    public static final String NBT_SCORCHED_TICKS = "ec_scorched_ticks";
    public static final String NBT_SCORCHED_STRENGTH = "ec_scorched_str";
    public static final String NBT_SCORCHED_COOLDOWN = "ec_scorched_cd";

    public static void applyScorched(LivingEntity target, int fireStrength, int duration) {
        if (target.level().isClientSide) return;

        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).toString();
        if (ElementalFireNatureReactionsConfig.cachedScorchedBlacklist.contains(entityId)) {
            return;
        }

        CompoundTag data = target.getPersistentData();

        if (data.contains(NBT_SCORCHED_TICKS) && data.getInt(NBT_SCORCHED_TICKS) > 0) {
            return;
        }

        long gameTime = target.level().getGameTime();
        if (data.contains(NBT_SCORCHED_COOLDOWN)) {
            if (gameTime < data.getLong(NBT_SCORCHED_COOLDOWN)) {
                return;
            }
        }

        data.putInt(NBT_SCORCHED_TICKS, duration);
        data.putInt(NBT_SCORCHED_STRENGTH, fireStrength);
        
        data.putLong(NBT_SCORCHED_COOLDOWN, gameTime + duration + ElementalFireNatureReactionsConfig.scorchedCooldown);

        target.setSecondsOnFire((int) ElementalFireNatureReactionsConfig.scorchedBurningLockDuration);

        if (target.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LAVA,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    5, 0.2, 0.2, 0.2, 0.0);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        CompoundTag data = entity.getPersistentData();
        if (!data.contains(NBT_SCORCHED_TICKS)) return;

        int ticks = data.getInt(NBT_SCORCHED_TICKS);
        
        if (ticks <= 0) {
            data.remove(NBT_SCORCHED_TICKS);
            data.remove(NBT_SCORCHED_STRENGTH);
            return;
        }

        data.putInt(NBT_SCORCHED_TICKS, ticks - 1);
        int fireStrength = data.getInt(NBT_SCORCHED_STRENGTH);
        ServerLevel level = (ServerLevel) entity.level();

        if (entity.isInWater() && entity.isEyeInFluidType(net.minecraftforge.common.ForgeMod.WATER_TYPE.get())) {
            triggerThermalShock(entity, level, ticks, fireStrength);
            return;
        }

        int lockTicks = (int) (ElementalFireNatureReactionsConfig.scorchedBurningLockDuration * 20);
        if (entity.getRemainingFireTicks() < lockTicks) {
            entity.setSecondsOnFire((int) ElementalFireNatureReactionsConfig.scorchedBurningLockDuration);
        }

        if (entity.tickCount % 20 == 0) {
            float damage = calculateScorchedDamage(fireStrength, entity);

            if (damage > 0) {
                entity.hurt(ModDamageTypes.source(level, ModDamageTypes.LAVA_MAGIC), damage);

                level.sendParticles(ParticleTypes.LAVA,
                        entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                        1, 0.2, 0.2, 0.2, 0.0);

                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.2f, 1.0f);
            }
        }
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().getEffect() == ModMobEffects.WETNESS.get()) {
            if (event.getEntity().getPersistentData().contains(NBT_SCORCHED_TICKS)) {
                event.setResult(Event.Result.DENY);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        if (!entity.getPersistentData().contains(NBT_SCORCHED_TICKS)) return;

        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.IS_FIRE) && !source.is(ModDamageTypes.LAVA_MAGIC)) {
            event.setCanceled(true);
        }
    }

    private static void triggerThermalShock(LivingEntity entity, ServerLevel level, int remainingTicks, int fireStrength) {
        double remainingSeconds = remainingTicks / 20.0;
        float dps = calculateScorchedDamage(fireStrength, entity);
        float totalRemainingDamage = (float) (remainingSeconds * dps);
        
        float ratio = (float) ElementalFireNatureReactionsConfig.scorchedShockDamageRatio;
        float shockDamage = totalRemainingDamage * ratio;

        if (shockDamage > 0.5f) {
            entity.hurt(entity.damageSources().generic(), shockDamage);
        }

        entity.clearFire();
        CompoundTag data = entity.getPersistentData();
        data.remove(NBT_SCORCHED_TICKS);
        data.remove(NBT_SCORCHED_STRENGTH);

        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f, 2.0f);

        level.sendParticles(ParticleTypes.CLOUD,
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                20, 0.5, 0.5, 0.5, 0.05);
    }

    private static float calculateScorchedDamage(int fireStrength, LivingEntity target) {
        int resistPoints = ElementUtils.getDisplayResistance(target, ElementType.FIRE);

        if (resistPoints >= ElementalFireNatureReactionsConfig.scorchedResistThreshold) {
            return 0.0f;
        }

        double base = ElementalFireNatureReactionsConfig.scorchedDamageBase;
        int step = Math.max(1, ElementalFireNatureReactionsConfig.scorchedDamageScalingStep);
        double bonus = (double) fireStrength / step * 0.5;
        double rawDamage = base + bonus;

        if (target.fireImmune()) {
            rawDamage *= ElementalFireNatureReactionsConfig.scorchedImmuneModifier;
        }

        if (ElementUtils.getDisplayEnhancement(target, ElementType.NATURE) > 0 ||
                ElementUtils.getDisplayResistance(target, ElementType.NATURE) > 0) {
            rawDamage *= ElementalFireNatureReactionsConfig.scorchedNatureMultiplier;
        }

        int fireProtLevel = 0;
        int genProtLevel = 0;

        for (ItemStack stack : target.getArmorSlots()) {
            fireProtLevel += EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_PROTECTION, stack);
            genProtLevel += EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, stack);
        }

        double denom = ElementalFireNatureReactionsConfig.enchantmentCalculationDenominator;
        
        double fireProtReduction = (Math.min(fireProtLevel, denom) / denom) * ElementalFireNatureReactionsConfig.scorchedFireProtReduction;
        double genProtReduction = (Math.min(genProtLevel, denom) / denom) * ElementalFireNatureReactionsConfig.scorchedGenProtReduction;

        rawDamage *= (1.0 - fireProtReduction);
        rawDamage *= (1.0 - genProtReduction);

        return (float) rawDamage;
    }
}