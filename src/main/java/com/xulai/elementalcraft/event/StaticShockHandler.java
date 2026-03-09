package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
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
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;
import java.util.Random;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class StaticShockHandler {
    private static final Random RANDOM = new Random();
    private static final String NBT_STATIC_STACKS = "EC_StaticStacks";
    private static final String NBT_STATIC_TIMER = "EC_StaticTimer";
    private static final String NBT_STATIC_DAMAGE_TIMER = "EC_StaticDamageTimer";
    private static final String NBT_PARALYSIS_STACKS = "EC_ParalysisStacks";
    private static final String NBT_PARALYSIS_TIMER = "EC_ParalysisTimer";

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
        // 检查攻击属性一致性：手上的物品必须有雷霆属性攻击附魔，且身上的装备有雷霆属性强化点数
        ElementType consistentElement = ElementUtils.getConsistentAttackElement(attacker);
        if (consistentElement != ElementType.THUNDER) return;
        
        int thunderStrength = ElementUtils.getDisplayEnhancement(attacker, ElementType.THUNDER);
        int threshold = ElementalThunderFrostReactionsConfig.thunderStrengthThreshold;
        if (thunderStrength < threshold) return;
        
        boolean hasWetness = target.hasEffect(ModMobEffects.WETNESS.get());
        int wetnessLevel = 0;
        if (hasWetness) {
            MobEffectInstance wetnessEffect = target.getEffect(ModMobEffects.WETNESS.get());
            if (wetnessEffect != null) {
                wetnessLevel = wetnessEffect.getAmplifier() + 1;
            }
        }
        
        double chance = calculateTriggerChance(thunderStrength, wetnessLevel, target);
        
        if (RANDOM.nextDouble() >= chance) return;

        if (wetnessLevel > 0) {
            CompoundTag data = target.getPersistentData();
            int currentStacks = data.getInt(NBT_STATIC_STACKS);
            int maxStacks = ElementalThunderFrostReactionsConfig.staticMaxTotalStacks;
            if (currentStacks >= maxStacks) return;
            int addStacks = ElementalThunderFrostReactionsConfig.staticMaxStacksPerAttack;
            int newStacks = Math.min(maxStacks, currentStacks + addStacks);
            int durationPerStack = ElementalThunderFrostReactionsConfig.staticDurationPerStackTicks;
            int addTicks = addStacks * durationPerStack;
            int newTotalTicks = data.getInt(NBT_STATIC_TIMER) + addTicks;
            data.putInt(NBT_STATIC_STACKS, newStacks);
            data.putInt(NBT_STATIC_TIMER, newTotalTicks);
            triggerParalysisReaction(attacker, target);
            return;
        }

        CompoundTag data = target.getPersistentData();
        int currentStacks = data.getInt(NBT_STATIC_STACKS);
        int maxStacks = ElementalThunderFrostReactionsConfig.staticMaxTotalStacks;
        if (currentStacks >= maxStacks) return;
        int addStacks = ElementalThunderFrostReactionsConfig.staticMaxStacksPerAttack;
        int newStacks = Math.min(maxStacks, currentStacks + addStacks);
        int durationPerStack = ElementalThunderFrostReactionsConfig.staticDurationPerStackTicks;
        int adjustedDurationPerStack = Math.max(durationPerStack, 20);
        int newTotalTicks = newStacks * adjustedDurationPerStack;
        data.putInt(NBT_STATIC_STACKS, newStacks);
        data.putInt(NBT_STATIC_TIMER, newTotalTicks);
        data.putInt(NBT_STATIC_DAMAGE_TIMER, 0);
        updateEffect(target, newStacks, newTotalTicks);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        if (entity.hasEffect(ModMobEffects.PARALYSIS.get())) {
            entity.setJumping(false);
            entity.setShiftKeyDown(false);
            entity.setDeltaMovement(0, entity.getDeltaMovement().y, 0);
        }

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
            int adjustedDurationPerStack = Math.max(durationPerStack, 20);
            int maxStacks = ElementalThunderFrostReactionsConfig.staticMaxTotalStacks;
            stacks = Math.min(stacks, maxStacks);
            int minRequiredTicks = stacks * adjustedDurationPerStack;
            if (remainingTicks < minRequiredTicks) {
                stacks = Math.max(1, remainingTicks / adjustedDurationPerStack);
                amplifier = stacks - 1;
            }
            data.putInt(NBT_STATIC_STACKS, stacks);
            data.putInt(NBT_STATIC_TIMER, remainingTicks);
            data.putInt(NBT_STATIC_DAMAGE_TIMER, 0);
            if (effectInstance.getDuration() != remainingTicks || effectInstance.getAmplifier() != amplifier) {
                entity.removeEffect(ModMobEffects.STATIC_SHOCK.get());
                entity.addEffect(new MobEffectInstance(
                        ModMobEffects.STATIC_SHOCK.get(),
                        remainingTicks,
                        amplifier,
                        false,
                        false,
                        true
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

        int totalTimer = data.getInt(NBT_STATIC_TIMER);

        int damageTimer = data.getInt(NBT_STATIC_DAMAGE_TIMER);
        int interval = ElementalThunderFrostReactionsConfig.staticDamageIntervalTicks;
        if (interval < 1) interval = 1;

        damageTimer++;
        if (damageTimer >= interval) {
            if (totalTimer > 0) {
                triggerStaticDamage(entity);
            }
            damageTimer = 0;
        }
        data.putInt(NBT_STATIC_DAMAGE_TIMER, damageTimer);

        boolean hasWetness = entity.hasEffect(ModMobEffects.WETNESS.get());
        int wetnessLevel = 0;
        if (hasWetness) {
            MobEffectInstance wetnessEffect = entity.getEffect(ModMobEffects.WETNESS.get());
            if (wetnessEffect != null) {
                wetnessLevel = wetnessEffect.getAmplifier() + 1;
            }
        }
        if (wetnessLevel > 0) {
            triggerParalysisReaction(null, entity);
            return;
        }

        if (totalTimer > 0) {
            totalTimer--;
            data.putInt(NBT_STATIC_TIMER, totalTimer);
        }

        int durationPerStack = ElementalThunderFrostReactionsConfig.staticDurationPerStackTicks;
        int newStacks = (int) Math.ceil((double) totalTimer / durationPerStack);
        if (newStacks < 1) newStacks = 1;
        if (newStacks != stacks) {
            data.putInt(NBT_STATIC_STACKS, newStacks);
        }

        if (totalTimer <= 0) {
            clearStaticShock(entity);
            return;
        }

        updateEffect(entity, newStacks, totalTimer);

        if (entity.hasEffect(ModMobEffects.STATIC_SHOCK.get())) {
            if (entity.tickCount % 2 == 0) {
                spawnStaticShockParticles(entity);
            }
        }
    }

    private static void triggerStaticDamage(LivingEntity entity) {
        double minDmg = ElementalThunderFrostReactionsConfig.staticDamageMin;
        double maxDmg = ElementalThunderFrostReactionsConfig.staticDamageMax;
        if (maxDmg < minDmg) maxDmg = minDmg;
        float damage = (float) (minDmg + RANDOM.nextDouble() * (maxDmg - minDmg));

        ElementType element = ElementUtils.getElementType(entity);
        if (element == ElementType.NATURE) {
            damage *= (float) ElementalThunderFrostReactionsConfig.staticDamageNatureMultiplier;
        } else if (element == ElementType.FROST) {
            damage *= (float) ElementalThunderFrostReactionsConfig.staticDamageFrostMultiplier;
        }

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
    }

    private static void spawnBurstParticles(ServerLevel level, LivingEntity entity) {
        double x = entity.getX();
        double y = entity.getY() + entity.getBbHeight() * 0.5;
        double z = entity.getZ();
        for (int i = 0; i < 8; i++) {
            double offsetX = (RANDOM.nextDouble() - 0.5) * 0.5;
            double offsetY = (RANDOM.nextDouble() - 0.5) * 0.5;
            double offsetZ = (RANDOM.nextDouble() - 0.5) * 0.5;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    x + offsetX, y + offsetY, z + offsetZ,
                    1, 0, 0, 0, 0);
        }
    }

    private static double calculateTriggerChance(int thunderStrength, int wetnessLevel, LivingEntity target) {
        int threshold = ElementalThunderFrostReactionsConfig.thunderStrengthThreshold;
        if (thunderStrength < threshold) return 0.0;
        double baseChance = ElementalThunderFrostReactionsConfig.staticBaseChance;
        double scalingChance = ElementalThunderFrostReactionsConfig.staticScalingChance;
        int scalingStep = ElementalThunderFrostReactionsConfig.staticScalingStep;
        int extraStrength = thunderStrength - threshold;
        int extraSteps = extraStrength / scalingStep;
        double totalChance = baseChance + (extraSteps * scalingChance);
        
        // 添加潮湿效果加成
        if (wetnessLevel > 0) {
            double wetnessBonusChance = ElementalThunderFrostReactionsConfig.staticWetnessBonusChancePerLevel;
            totalChance += wetnessLevel * wetnessBonusChance;
        }
        
        // 添加目标已有静电效果的叠加概率加成
        if (target.hasEffect(ModMobEffects.STATIC_SHOCK.get())) {
            totalChance += ElementalThunderFrostReactionsConfig.staticStackingBonusChance;
        }
        
        return Math.min(totalChance, 1.0);
    }

    private static void clearStaticShock(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        data.remove(NBT_STATIC_STACKS);
        data.remove(NBT_STATIC_TIMER);
        data.remove(NBT_STATIC_DAMAGE_TIMER);
        if (entity.hasEffect(ModMobEffects.STATIC_SHOCK.get())) {
            entity.removeEffect(ModMobEffects.STATIC_SHOCK.get());
        }
    }

    private static void updateEffect(LivingEntity entity, int stacks, int totalTicks) {
        if (stacks <= 0) {
            clearStaticShock(entity);
            return;
        }
        int amplifier = stacks - 1;
        MobEffectInstance currentEffect = entity.getEffect(ModMobEffects.STATIC_SHOCK.get());
        if (currentEffect == null || currentEffect.getAmplifier() != amplifier || currentEffect.getDuration() != totalTicks) {
            entity.removeEffect(ModMobEffects.STATIC_SHOCK.get());
            entity.addEffect(new MobEffectInstance(
                    ModMobEffects.STATIC_SHOCK.get(),
                    totalTicks,
                    amplifier,
                    false,
                    false,
                    true
            ));
        }
    }

    private static void triggerParalysisReaction(LivingEntity attacker, LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        int staticStacks = data.getInt(NBT_STATIC_STACKS);
        int totalTimer = data.getInt(NBT_STATIC_TIMER);
        if (staticStacks <= 0 || totalTimer <= 0) return;

        int wetnessLevel = 0;
        if (entity.hasEffect(ModMobEffects.WETNESS.get())) {
            MobEffectInstance wetnessEffect = entity.getEffect(ModMobEffects.WETNESS.get());
            if (wetnessEffect != null) {
                wetnessLevel = wetnessEffect.getAmplifier() + 1;
            }
        }

        int paralysisStacks = staticStacks + wetnessLevel;
        int maxParalysisStacks = ElementalThunderFrostReactionsConfig.paralysisMaxStacks;
        if (paralysisStacks > maxParalysisStacks) {
            paralysisStacks = maxParalysisStacks;
        }

        int interval = ElementalThunderFrostReactionsConfig.staticDamageIntervalTicks;
        if (interval < 1) interval = 1;
        int remainingTicks = totalTimer;
        int remainingHits = (remainingTicks + interval - 1) / interval;

        double totalDamage = 0;
        double minDmg = ElementalThunderFrostReactionsConfig.staticDamageMin;
        double maxDmg = ElementalThunderFrostReactionsConfig.staticDamageMax;
        if (maxDmg < minDmg) maxDmg = minDmg;
        for (int i = 0; i < remainingHits; i++) {
            double damage = minDmg + RANDOM.nextDouble() * (maxDmg - minDmg);
            totalDamage += damage;
        }
        totalDamage *= 0.5;

        if (totalDamage > 0) {
            DamageSource damageSource = ModDamageTypes.source(entity.level(), ModDamageTypes.STATIC_SHOCK);
            entity.hurt(damageSource, (float) totalDamage);
        }

        clearStaticShock(entity);
        if (entity.hasEffect(ModMobEffects.WETNESS.get())) {
            entity.removeEffect(ModMobEffects.WETNESS.get());
        }
        data.remove("EC_WetnessLevel");
        data.remove("EC_WetnessRainTimer");
        data.remove("EC_WetnessDecayTimer");

        int paralysisDuration = ElementalThunderFrostReactionsConfig.paralysisDurationPerStackTicks * paralysisStacks;
        entity.addEffect(new MobEffectInstance(
                ModMobEffects.PARALYSIS.get(),
                paralysisDuration,
                paralysisStacks - 1,
                false,
                false,
                true
        ));
        data.putInt(NBT_PARALYSIS_STACKS, paralysisStacks);
        data.putInt(NBT_PARALYSIS_TIMER, paralysisDuration);

        if (!entity.level().isClientSide) {
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    ModSounds.ELECTRIC_ZAP.get(), SoundSource.PLAYERS, 1.0f, 0.5f);
        }
        DebugCommand.sendParalysisLog(attacker, entity, paralysisStacks, remainingHits, (float) totalDamage);
    }

    private static void spawnStaticShockParticles(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;
        Vector3f STATIC_PURPLE_BLUE = new Vector3f(0.5f, 0.2f, 1.0f);
        double radius = entity.getBbWidth() * 0.8 + 0.5;
        for (int i = 0; i < 2 + RANDOM.nextInt(2); i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double heightOffset = (RANDOM.nextDouble() - 0.5) * entity.getBbHeight() * 1.2;
            double x = entity.getX() + Math.cos(angle) * radius;
            double z = entity.getZ() + Math.sin(angle) * radius;
            double y = entity.getY() + entity.getBbHeight() / 2 + heightOffset;
            if (RANDOM.nextBoolean()) {
                serverLevel.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0, 0, 0.02);
            } else {
                serverLevel.sendParticles(new DustParticleOptions(STATIC_PURPLE_BLUE, 1.2f),
                        x, y, z, 1, 0, 0, 0, 0);
            }
        }
    }
}