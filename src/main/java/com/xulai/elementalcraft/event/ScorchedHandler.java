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
    public static final String NBT_SCORCHED_SOURCE_FIRE_POWER = "EC_ScorchedSourceFirePower";
    public static final String NBT_ATTACKER_SCORCHED_COOLDOWN = "ec_scorched_attacker_cd";
    public static final String NBT_SCORCHED_DAMAGE_MULT = "ec_scorched_dmg_mult";

    public static void applyScorched(LivingEntity target, LivingEntity attacker, int fireStrength, int duration, int sourceFirePower, float damageMultiplier, boolean bypassCooldown) {
        if (target.level().isClientSide) return;
        var key = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (key == null) return;
        String entityId = key.toString();
        var blacklist = ElementalFireNatureReactionsConfig.cachedScorchedBlacklist;
        if (blacklist != null && blacklist.contains(entityId)) {
            return;
        }
        if (attacker != null && !bypassCooldown) {
            CompoundTag attackerData = attacker.getPersistentData();
            long gameTime = target.level().getGameTime();
            if (attackerData.contains(NBT_ATTACKER_SCORCHED_COOLDOWN)) {
                long cd = attackerData.getLong(NBT_ATTACKER_SCORCHED_COOLDOWN);
                if (gameTime < cd) {
                    return;
                }
            }
        }
        CompoundTag targetData = target.getPersistentData();
        if (targetData.contains(NBT_SCORCHED_TICKS) && targetData.getInt(NBT_SCORCHED_TICKS) > 0) {
            return;
        }

        int adjustedDuration = duration;
        boolean isNature = isNatureAligned(target);
        boolean isFrost = isFrostAligned(target);

        if (isNature) {
            double multiplier = ElementalFireNatureReactionsConfig.scorchedNatureDurationMultiplier;
            adjustedDuration = (int) Math.round(duration * multiplier);
        } else if (isFrost) {
            double multiplier = ElementalFireNatureReactionsConfig.scorchedFrostDurationMultiplier;
            adjustedDuration = (int) Math.round(duration * multiplier);
        }

        if (adjustedDuration < 1) adjustedDuration = 1;
        long gameTime = target.level().getGameTime();
        targetData.putInt(NBT_SCORCHED_TICKS, adjustedDuration);
        targetData.putInt(NBT_SCORCHED_STRENGTH, fireStrength);
        targetData.putInt(NBT_SCORCHED_SOURCE_FIRE_POWER, sourceFirePower);
        targetData.putFloat(NBT_SCORCHED_DAMAGE_MULT, damageMultiplier);

        if (attacker != null && !bypassCooldown) {
            CompoundTag attackerData = attacker.getPersistentData();
            attackerData.putLong(NBT_ATTACKER_SCORCHED_COOLDOWN, gameTime + ElementalFireNatureReactionsConfig.scorchedCooldown);
        }

        target.setRemainingFireTicks(adjustedDuration);

        if (target.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LAVA, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 20, 0.2, 0.2, 0.2, 0.0);
        }
    }

    public static void applyScorched(LivingEntity target, LivingEntity attacker, int fireStrength, int duration, int sourceFirePower, float damageMultiplier) {
        applyScorched(target, attacker, fireStrength, duration, sourceFirePower, damageMultiplier, false);
    }

    public static void applyScorched(LivingEntity target, LivingEntity attacker, int fireStrength, int duration, int sourceFirePower) {
        applyScorched(target, attacker, fireStrength, duration, sourceFirePower, 1.0f, false);
    }

    private static boolean isNatureAligned(LivingEntity entity) {
        return ElementUtils.getConsistentAttackElement(entity) == ElementType.NATURE;
    }

    private static boolean isFrostAligned(LivingEntity entity) {
        return ElementUtils.getConsistentAttackElement(entity) == ElementType.FROST;
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        CompoundTag data = entity.getPersistentData();
        if (data.contains(NBT_ATTACKER_SCORCHED_COOLDOWN)) {
            long cd = data.getLong(NBT_ATTACKER_SCORCHED_COOLDOWN);
            if (entity.level().getGameTime() >= cd) {
                data.remove(NBT_ATTACKER_SCORCHED_COOLDOWN);
            }
        }

        if (!data.contains(NBT_SCORCHED_TICKS)) return;
        int ticks = data.getInt(NBT_SCORCHED_TICKS);

        if (ticks <= 0) {
            entity.clearFire();
            data.remove(NBT_SCORCHED_TICKS);
            data.remove(NBT_SCORCHED_STRENGTH);
            data.remove(NBT_SCORCHED_SOURCE_FIRE_POWER);
            data.remove(NBT_SCORCHED_DAMAGE_MULT);
            return;
        }

        int resistPoints = ElementUtils.getDisplayResistance(entity, ElementType.FIRE);
        if (resistPoints >= ElementalFireNatureReactionsConfig.scorchedResistThreshold) {
            entity.clearFire();
            data.remove(NBT_SCORCHED_TICKS);
            data.remove(NBT_SCORCHED_STRENGTH);
            data.remove(NBT_SCORCHED_SOURCE_FIRE_POWER);
            data.remove(NBT_SCORCHED_DAMAGE_MULT);
            return;
        }

        data.putInt(NBT_SCORCHED_TICKS, ticks - 1);
        int fireStrength = data.getInt(NBT_SCORCHED_STRENGTH);
        ServerLevel level = (ServerLevel) entity.level();

        if (entity.isInWater()) {
            triggerThermalShock(entity, level, ticks, fireStrength);
            return;
        }

        if (entity.getRemainingFireTicks() < ticks) {
            entity.setRemainingFireTicks(ticks);
        }

        if (entity.tickCount % 20 == 0) {
            float damage = calculateScorchedDamage(fireStrength, entity);
            if (damage > 0) {
                entity.hurt(ModDamageTypes.source(level, ModDamageTypes.LAVA_MAGIC), damage);
                level.sendParticles(ParticleTypes.LAVA, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 10, 0.2, 0.2, 0.2, 0.0);
                level.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 5, 0.2, 0.2, 0.2, 0.0);
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.2f, 1.0f);
            }
        }
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().getEffect() == ModMobEffects.WETNESS.get()) {
            boolean blocked = event.getEntity().getPersistentData().contains(NBT_SCORCHED_TICKS);
            if (blocked) {
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
            entity.hurt(ModDamageTypes.source(level, ModDamageTypes.LAVA_MAGIC), shockDamage);
        }

        entity.clearFire();
        CompoundTag data = entity.getPersistentData();
        data.remove(NBT_SCORCHED_TICKS);
        data.remove(NBT_SCORCHED_STRENGTH);
        data.remove(NBT_SCORCHED_SOURCE_FIRE_POWER);
        data.remove(NBT_SCORCHED_DAMAGE_MULT);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f, 2.0f);
        level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 20, 0.5, 0.5, 0.5, 0.05);
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

        int fireProtLevel = 0;
        int genProtLevel = 0;
        for (ItemStack stack : target.getArmorSlots()) {
            fireProtLevel += EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_PROTECTION, stack);
            genProtLevel += EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, stack);
        }

        double denom = Math.max(1.0, ElementalFireNatureReactionsConfig.enchantmentCalculationDenominator);
        double fireProtReduction = (Math.min(fireProtLevel, denom) / denom) * ElementalFireNatureReactionsConfig.scorchedFireProtReduction;
        double genProtReduction = (Math.min(genProtLevel, denom) / denom) * ElementalFireNatureReactionsConfig.scorchedGenProtReduction;

        double finalDamage = rawDamage * (1.0 - fireProtReduction) * (1.0 - genProtReduction);

        float damageMult = target.getPersistentData().getFloat(NBT_SCORCHED_DAMAGE_MULT);
        if (damageMult <= 0.0f) damageMult = 1.0f;
        finalDamage *= damageMult;

        return (float) finalDamage;
    }
}
