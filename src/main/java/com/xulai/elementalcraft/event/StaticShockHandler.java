package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.sound.ModSounds;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class StaticShockHandler {

    private static final Random RANDOM = new Random();

    private static final String NBT_STATIC_STACKS = "EC_StaticStacks";
    private static final String NBT_STATIC_TIMER = "EC_StaticTimer";
    private static final String NBT_STATIC_DAMAGE_TIMER = "EC_StaticDamageTimer";
    private static final String NBT_STATIC_DAMAGE_COUNT = "EC_StaticDamageCount";
    private static final String NBT_STATIC_TARGET_COUNT = "EC_StaticTargetCount";

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;

        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;

        LivingEntity target = event.getEntity();

        int targetThunderResist = ElementUtils.getDisplayResistance(target, ElementType.THUNDER);
        int immunityThreshold = ElementalThunderFrostReactionsConfig.staticResistImmunityThreshold;
        if (targetThunderResist >= immunityThreshold) {
            clearStaticShock(target);
            return;
        }

        int thunderStrength = ElementUtils.getDisplayEnhancement(attacker, ElementType.THUNDER);
        int threshold = ElementalThunderFrostReactionsConfig.thunderStrengthThreshold;
        if (thunderStrength < threshold) return;

        double chance = calculateTriggerChance(thunderStrength);
        if (RANDOM.nextDouble() >= chance) return;

        CompoundTag data = target.getPersistentData();
        int currentStacks = data.getInt(NBT_STATIC_STACKS);
        int maxStacks = ElementalThunderFrostReactionsConfig.staticMaxTotalStacks;

        if (currentStacks >= maxStacks) return;

        int addStacks = ElementalThunderFrostReactionsConfig.staticMaxStacksPerAttack;
        int newStacks = Math.min(maxStacks, currentStacks + addStacks);

        int durationPerStack = ElementalThunderFrostReactionsConfig.staticDurationPerStackTicks;
        int newTotalTicks = newStacks * durationPerStack;

        data.putInt(NBT_STATIC_STACKS, newStacks);
        data.putInt(NBT_STATIC_TIMER, newTotalTicks);
        data.putInt(NBT_STATIC_DAMAGE_TIMER, 0);

        int newTargetCount = newStacks * ElementalThunderFrostReactionsConfig.staticHitsPerStack;
        data.putInt(NBT_STATIC_TARGET_COUNT, newTargetCount);

        updateEffect(target, newStacks, newTotalTicks);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        int targetThunderResist = ElementUtils.getDisplayResistance(entity, ElementType.THUNDER);
        int immunityThreshold = ElementalThunderFrostReactionsConfig.staticResistImmunityThreshold;
        if (targetThunderResist >= immunityThreshold) {
            clearStaticShock(entity);
            return;
        }

        CompoundTag data = entity.getPersistentData();
        MobEffectInstance effectInstance = entity.getEffect(ModMobEffects.STATIC_SHOCK.get());

        if (effectInstance != null && !data.contains(NBT_STATIC_STACKS)) {
            int amplifier = effectInstance.getAmplifier();
            int remainingTicks = effectInstance.getDuration();
            int stacks = amplifier + 1;
            int durationPerStack = ElementalThunderFrostReactionsConfig.staticDurationPerStackTicks;

            int maxStacks = ElementalThunderFrostReactionsConfig.staticMaxTotalStacks;
            stacks = Math.min(stacks, maxStacks);

            int minRequiredTicks = stacks * durationPerStack;
            if (remainingTicks < minRequiredTicks) {
                stacks = Math.max(1, remainingTicks / durationPerStack);
                amplifier = stacks - 1;
            }

            data.putInt(NBT_STATIC_STACKS, stacks);
            data.putInt(NBT_STATIC_TIMER, remainingTicks);
            data.putInt(NBT_STATIC_DAMAGE_TIMER, 0);
            data.putInt(NBT_STATIC_DAMAGE_COUNT, 0);
            int targetCount = stacks * ElementalThunderFrostReactionsConfig.staticHitsPerStack;
            data.putInt(NBT_STATIC_TARGET_COUNT, targetCount);

            if (effectInstance.getDuration() != remainingTicks || effectInstance.getAmplifier() != amplifier) {
                entity.removeEffect(ModMobEffects.STATIC_SHOCK.get());
                // 静电效果期间禁止药水粒子效果：设置visible为false，showIcon为true（显示图标但不显示粒子）
                entity.addEffect(new MobEffectInstance(
                        ModMobEffects.STATIC_SHOCK.get(),
                        remainingTicks,
                        amplifier,
                        false,  // ambient
                        false,  // visible - 设置为false禁止药水粒子效果
                        true    // showIcon - 设置为true显示图标
                ));
            }
        }

        if (!data.contains(NBT_STATIC_STACKS)) {
            if (entity.hasEffect(ModMobEffects.STATIC_SHOCK.get())) {
                entity.removeEffect(ModMobEffects.STATIC_SHOCK.get());
            }
            return;
        }

        int stacks = data.getInt(NBT_STATIC_STACKS);
        if (stacks <= 0) {
            clearStaticShock(entity);
            return;
        }

        int durationPerStack = ElementalThunderFrostReactionsConfig.staticDurationPerStackTicks;
        int totalTimer = data.getInt(NBT_STATIC_TIMER);
        int damageTimer = data.getInt(NBT_STATIC_DAMAGE_TIMER);
        int damageCount = data.getInt(NBT_STATIC_DAMAGE_COUNT);
        int targetCount = data.getInt(NBT_STATIC_TARGET_COUNT);
        if (targetCount == 0) {
            targetCount = stacks * ElementalThunderFrostReactionsConfig.staticHitsPerStack;
            data.putInt(NBT_STATIC_TARGET_COUNT, targetCount);
        }

        boolean damageTriggered = false;
        int damageInterval = ElementalThunderFrostReactionsConfig.staticDamageIntervalTicks;

        if (damageCount < targetCount) {
            if (damageInterval > 0) {
                damageTimer++;
                if (damageTimer >= damageInterval) {
                    double minDmg = ElementalThunderFrostReactionsConfig.staticDamageMin;
                    double maxDmg = ElementalThunderFrostReactionsConfig.staticDamageMax;
                    // 确保maxDmg不小于minDmg
                    if (maxDmg < minDmg) {
                        maxDmg = minDmg;
                    }
                    float damage = (float) (minDmg + RANDOM.nextDouble() * (maxDmg - minDmg));

                    DamageSource damageSource = ModDamageTypes.source(entity.level(), ModDamageTypes.STATIC_SHOCK);
                    entity.hurt(damageSource, damage);

                    if (!entity.level().isClientSide) {
                        float pitch = 0.8f + RANDOM.nextFloat() * 0.4f;
                        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                                ModSounds.ELECTRIC_ZAP.get(), SoundSource.PLAYERS, 0.8f, pitch);
                    }

                    if (entity.level() instanceof ServerLevel serverLevel) {
                        spawnBurstParticles(serverLevel, entity);
                    }

                    damageTimer = 0;
                    damageCount++;
                    damageTriggered = true;
                }
            }

            int remainingHits = targetCount - damageCount;
            if (remainingHits > 0 && totalTimer < remainingHits * damageInterval) {
                if (!damageTriggered) {
                    double minDmg = ElementalThunderFrostReactionsConfig.staticDamageMin;
                    double maxDmg = ElementalThunderFrostReactionsConfig.staticDamageMax;
                    // 确保maxDmg不小于minDmg
                    if (maxDmg < minDmg) {
                        maxDmg = minDmg;
                    }
                    float damage = (float) (minDmg + RANDOM.nextDouble() * (maxDmg - minDmg));

                    DamageSource damageSource = ModDamageTypes.source(entity.level(), ModDamageTypes.STATIC_SHOCK);
                    entity.hurt(damageSource, damage);

                    if (!entity.level().isClientSide) {
                        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                                ModSounds.ELECTRIC_ZAP.get(), SoundSource.PLAYERS, 0.5f, 1.0f);
                    }

                    if (entity.level() instanceof ServerLevel serverLevel) {
                        spawnBurstParticles(serverLevel, entity);
                    }

                    damageCount++;
                    damageTimer = 0;
                    damageTriggered = true;
                }
            }

            data.putInt(NBT_STATIC_DAMAGE_TIMER, damageTimer);
            data.putInt(NBT_STATIC_DAMAGE_COUNT, damageCount);
        } else {
            data.putInt(NBT_STATIC_DAMAGE_TIMER, 0);
        }

        boolean isWet = entity.isInWater() || entity.level().isRainingAt(entity.blockPosition());

        if (!isWet) {
            if (totalTimer > 0) {
                totalTimer--;
                data.putInt(NBT_STATIC_TIMER, totalTimer);
            }

            if (stacks > 1 && totalTimer < (stacks - 1) * durationPerStack) {
                stacks--;
                data.putInt(NBT_STATIC_STACKS, stacks);
            }

            if (totalTimer <= 0) {
                clearStaticShock(entity);
                return;
            }
        }

        

        updateEffect(entity, stacks, totalTimer);
    }

    private static void spawnBurstParticles(ServerLevel level, LivingEntity entity) {
        int count = 10 + RANDOM.nextInt(6);
        for (int i = 0; i < count; i++) {
            double x = entity.getX() + (RANDOM.nextDouble() - 0.5) * entity.getBbWidth() * 2.0;
            double y = entity.getY() + RANDOM.nextDouble() * entity.getBbHeight();
            double z = entity.getZ() + (RANDOM.nextDouble() - 0.5) * entity.getBbWidth() * 2.0;
            double vx = (RANDOM.nextDouble() - 0.5) * 0.6;
            double vy = (RANDOM.nextDouble() - 0.5) * 0.6;
            double vz = (RANDOM.nextDouble() - 0.5) * 0.6;
            if (Math.abs(vx) < 0.1) vx = (vx > 0 ? 0.1 : -0.1);
            if (Math.abs(vy) < 0.1) vy = (vy > 0 ? 0.1 : -0.1);
            if (Math.abs(vz) < 0.1) vz = (vz > 0 ? 0.1 : -0.1);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 1, vx, vy, vz, 0.0);
        }
    }

    private static void updateEffect(LivingEntity entity, int stacks, int totalTicks) {
        if (stacks <= 0) {
            if (entity.hasEffect(ModMobEffects.STATIC_SHOCK.get())) {
                entity.removeEffect(ModMobEffects.STATIC_SHOCK.get());
            }
            return;
        }

        int amplifier = stacks - 1;
        int duration = Math.max(1, totalTicks);

        MobEffectInstance current = entity.getEffect(ModMobEffects.STATIC_SHOCK.get());
        if (current != null && current.getDuration() == duration && current.getAmplifier() == amplifier) {
            return;
        }

        // 静电效果期间禁止药水粒子效果：设置visible为false，showIcon为true（显示图标但不显示粒子）
        entity.addEffect(new MobEffectInstance(
                ModMobEffects.STATIC_SHOCK.get(),
                duration,
                amplifier,
                false,  // ambient
                false,  // visible - 设置为false禁止药水粒子效果
                true    // showIcon - 设置为true显示图标
        ));
    }

    private static void clearStaticShock(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        data.remove(NBT_STATIC_STACKS);
        data.remove(NBT_STATIC_TIMER);
        data.remove(NBT_STATIC_DAMAGE_TIMER);
        data.remove(NBT_STATIC_DAMAGE_COUNT);
        data.remove(NBT_STATIC_TARGET_COUNT);
        if (entity.hasEffect(ModMobEffects.STATIC_SHOCK.get())) {
            entity.removeEffect(ModMobEffects.STATIC_SHOCK.get());
        }
    }

    private static double calculateTriggerChance(int thunderStrength) {
        int threshold = ElementalThunderFrostReactionsConfig.thunderStrengthThreshold;
        double baseChance = ElementalThunderFrostReactionsConfig.staticBaseChance;
        int scalingStep = ElementalThunderFrostReactionsConfig.staticScalingStep;
        double scalingChance = ElementalThunderFrostReactionsConfig.staticScalingChance;

        if (thunderStrength < threshold) return 0.0;

        int excess = thunderStrength - threshold;
        int steps = excess / scalingStep;
        double totalChance = baseChance + steps * scalingChance;
        return Math.min(1.0, totalChance);
    }

    private StaticShockHandler() {}
}