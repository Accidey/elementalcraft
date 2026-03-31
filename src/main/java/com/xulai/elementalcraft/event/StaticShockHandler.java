package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import net.minecraft.world.entity.LightningBolt;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.sound.ModSounds;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import com.xulai.elementalcraft.event.WetnessHandler;
import com.xulai.elementalcraft.util.GlobalDebugLogger;
import com.xulai.elementalcraft.util.DebugMode;
import com.xulai.elementalcraft.util.EffectHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;
import java.util.Random;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class StaticShockHandler {
    private static final Random RANDOM = new Random();

    private static final String NBT_STATIC_STACKS = "ec_static_stacks";
    private static final String NBT_STATIC_TIMER = "ec_static_timer";
    private static final String NBT_STATIC_DAMAGE_TIMER = "ec_static_damage_timer";
    private static final String NBT_PARALYSIS_STACKS = "ec_paralysis_stacks";
    private static final String NBT_PARALYSIS_TIMER = "ec_paralysis_timer";
    private static final String NBT_FROM_SPREAD = "ec_from_spread"; 

    private static boolean isImmuneToStatic(LivingEntity entity) {
        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        if (ElementalThunderFrostReactionsConfig.cachedStaticImmunityBlacklist.contains(entityId)) {
            return true;
        }
        int resist = ElementUtils.getDisplayResistance(entity, ElementType.THUNDER);
        return resist >= ElementalThunderFrostReactionsConfig.staticResistImmunityThreshold;
    }

    private static boolean isImmuneToParalysis(LivingEntity entity) {
        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        return ElementalThunderFrostReactionsConfig.cachedParalysisImmunityBlacklist.contains(entityId);
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        LivingEntity target = event.getEntity();

        if (isImmuneToStatic(target)) {
            clearStaticShock(target);
            String entityId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).toString();
            boolean isBlacklisted = ElementalThunderFrostReactionsConfig.cachedStaticImmunityBlacklist.contains(entityId);
            if (isBlacklisted) {
                Debug.logBlacklistImmune(target);
            } else {
                int resist = ElementUtils.getDisplayResistance(target, ElementType.THUNDER);
                Debug.logImmune(target, resist);
            }
            return;
        }

        ElementType consistentElement = ElementUtils.getConsistentAttackElement(attacker);
        if (consistentElement != ElementType.THUNDER) {
            return;
        }
        
        int thunderStrength = ElementUtils.getDisplayEnhancement(attacker, ElementType.THUNDER);
        int threshold = ElementalThunderFrostReactionsConfig.thunderStrengthThreshold;
        if (thunderStrength < threshold) {
            Debug.logStrengthBelowThreshold(attacker, thunderStrength, threshold);
            return;
        }
        
        boolean hasWetness = target.hasEffect(ModMobEffects.WETNESS.get());
        int wetnessLevel = 0;
        if (hasWetness) {
            MobEffectInstance wetnessEffect = target.getEffect(ModMobEffects.WETNESS.get());
            if (wetnessEffect != null) {
                wetnessLevel = wetnessEffect.getAmplifier() + 1;
            }
        }
        
        double chance = calculateTriggerChance(thunderStrength, wetnessLevel, target);
        boolean triggered = RANDOM.nextDouble() < chance;
        Debug.logTriggerChance(attacker, target, thunderStrength, wetnessLevel, chance, triggered);
        
        if (!triggered) return;

        if (wetnessLevel > 0) {
            CompoundTag data = target.getPersistentData();
            int currentStacks = data.getInt(NBT_STATIC_STACKS);
            int maxStacks = ElementalThunderFrostReactionsConfig.staticMaxTotalStacks;
            if (currentStacks >= maxStacks) {
                Debug.logMaxStacksReached(target, currentStacks);
                return;
            }
            int addStacks = ElementalThunderFrostReactionsConfig.staticMaxStacksPerAttack;
            int newStacks = Math.min(maxStacks, currentStacks + addStacks);
            int durationPerStack = ElementalThunderFrostReactionsConfig.staticDurationPerStackTicks;
            int addTicks = addStacks * durationPerStack;
            int newTotalTicks = data.getInt(NBT_STATIC_TIMER) + addTicks;
            data.putInt(NBT_STATIC_STACKS, newStacks);
            data.putInt(NBT_STATIC_TIMER, newTotalTicks);
            Debug.logStaticApplied(target, currentStacks, newStacks, newTotalTicks, wetnessLevel);
            triggerParalysisReaction(attacker, target);
            return;
        }

        CompoundTag data = target.getPersistentData();
        int currentStacks = data.getInt(NBT_STATIC_STACKS);
        int maxStacks = ElementalThunderFrostReactionsConfig.staticMaxTotalStacks;
        if (currentStacks >= maxStacks) {
            Debug.logMaxStacksReached(target, currentStacks);
            return;
        }
        int addStacks = ElementalThunderFrostReactionsConfig.staticMaxStacksPerAttack;
        int newStacks = Math.min(maxStacks, currentStacks + addStacks);
        int durationPerStack = ElementalThunderFrostReactionsConfig.staticDurationPerStackTicks;
        int addTicks = addStacks * durationPerStack;
        int currentTimer = data.getInt(NBT_STATIC_TIMER);
        int newTotalTicks = currentTimer + addTicks;
        data.putInt(NBT_STATIC_STACKS, newStacks);
        data.putInt(NBT_STATIC_TIMER, newTotalTicks);
        data.putInt(NBT_STATIC_DAMAGE_TIMER, data.getInt(NBT_STATIC_DAMAGE_TIMER));
        data.remove(NBT_FROM_SPREAD);
        Debug.logStaticApplied(target, currentStacks, newStacks, newTotalTicks, 0);
        updateEffect(target, newStacks, newTotalTicks);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        if (isImmuneToStatic(entity)) {
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
            if (!data.contains(NBT_FROM_SPREAD)) {
                data.putBoolean(NBT_FROM_SPREAD, false);
            }
            Debug.logSyncFromEffect(entity, stacks, remainingTicks);
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
            Debug.logWetnessDuringTick(entity, wetnessLevel);
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
            Debug.logStackDecay(entity, stacks, newStacks);
            data.putInt(NBT_STATIC_STACKS, newStacks);
        }

        if (totalTimer <= 0) {
            clearStaticShock(entity);
            return;
        }

        updateEffect(entity, newStacks, totalTimer);

        if (entity.hasEffect(ModMobEffects.STATIC_SHOCK.get())) {
            if (entity.tickCount % 2 == 0) {
                if (entity.level() instanceof ServerLevel serverLevel) {
                    EffectHelper.playStaticShockParticles(serverLevel, entity);
                }
            }
        }
    }

    private static void triggerStaticDamage(LivingEntity entity) {
        if (isImmuneToStatic(entity)) {
            clearStaticShock(entity);
            return;
        }

        float damage = getRandomStaticDamage(entity);
        Debug.logDamageTrigger(entity, damage);
        DamageSource damageSource = ModDamageTypes.source(entity.level(), ModDamageTypes.STATIC_SHOCK);
        boolean hurtResult = entity.hurt(damageSource, damage);

        if (!entity.level().isClientSide) {
            float pitch = 0.8f + RANDOM.nextFloat() * 0.4f;
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    ModSounds.ELECTRIC_ZAP.get(), SoundSource.PLAYERS, 0.8f, pitch);
        }
        if (entity.level() instanceof ServerLevel serverLevel) {
            EffectHelper.playStaticBurst(serverLevel, entity);
        }

        if (hurtResult && ElementalThunderFrostReactionsConfig.staticSplashEnabled) {
            applySplashDamage(entity, damage, damageSource);
        }
    }

    private static float getRandomStaticDamage(LivingEntity entity) {
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
        return damage;
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
        
        if (wetnessLevel > 0) {
            double wetnessBonusChance = ElementalThunderFrostReactionsConfig.staticWetnessBonusChancePerLevel;
            totalChance += wetnessLevel * wetnessBonusChance;
        }
        
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
        data.remove(NBT_FROM_SPREAD);
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
        if (isImmuneToParalysis(entity)) {
            Debug.logParalysisBlacklistImmune(entity);
            return;
        }

        if (isImmuneToStatic(entity)) {
            clearStaticShock(entity);
            return;
        }

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
        for (int i = 0; i < remainingHits; i++) {
            totalDamage += getRandomStaticDamage(entity);
        }
        totalDamage *= ElementalThunderFrostReactionsConfig.paralysisDamagePercentage;

        Debug.logParalysisTrigger(entity, attacker, staticStacks, wetnessLevel, paralysisStacks, remainingHits, totalDamage);

        if (totalDamage > 0) {
            DamageSource damageSource = ModDamageTypes.source(entity.level(), ModDamageTypes.STATIC_SHOCK);
            entity.hurt(damageSource, (float) totalDamage);
        }

        clearStaticShock(entity);
        if (entity.hasEffect(ModMobEffects.WETNESS.get())) {
            entity.removeEffect(ModMobEffects.WETNESS.get());
        }
        WetnessHandler.updateWetnessLevel(entity, 0);
        data.remove(WetnessHandler.NBT_RAIN_TIMER);
        data.remove(WetnessHandler.NBT_DECAY_TIMER);

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

    private static void applySplashDamage(LivingEntity source, float originalDamage, DamageSource damageSource) {
        CompoundTag data = source.getPersistentData();
        int stacks = data.getInt(NBT_STATIC_STACKS);
        if (stacks <= 0) return;

        boolean fromSpread = data.getBoolean(NBT_FROM_SPREAD);
        if (fromSpread && !ElementalThunderFrostReactionsConfig.staticSplashAllowFromSpread) {
            Debug.logSplashSkipFromSpread(source);
            return;
        }

        int baseRange = ElementalThunderFrostReactionsConfig.staticSplashBaseRange;
        int perStack = ElementalThunderFrostReactionsConfig.staticSplashRangePerStack;
        int maxRange = ElementalThunderFrostReactionsConfig.staticSplashMaxRange;
        int range = baseRange + (stacks - 1) * perStack;
        if (range > maxRange) range = maxRange;
        if (range < 1) range = 1;

        AABB area = new AABB(
                source.getX() - range, source.getY() - range, source.getZ() - range,
                source.getX() + range, source.getY() + range, source.getZ() + range
        );
        java.util.List<LivingEntity> nearby = source.level().getEntitiesOfClass(LivingEntity.class, area);

        int affectedCount = 0;
        int paralysisCount = 0;
        for (LivingEntity target : nearby) {
            if (target == source) continue;
            if (target.isDeadOrDying()) continue;

            if (isImmuneToStatic(target)) {
                Debug.logSplashImmune(target, ElementUtils.getDisplayResistance(target, ElementType.THUNDER));
                continue;
            }

            if (ElementalThunderFrostReactionsConfig.staticSplashExcludePlayers && target instanceof Player) {
                Debug.logSplashExclude(target, "玩家");
                continue;
            }
            if (ElementalThunderFrostReactionsConfig.staticSplashExcludePets && target instanceof TamableAnimal pet) {
                if (pet.isTame() && pet.getOwner() != null) {
                    Debug.logSplashExclude(target, "宠物");
                    continue;
                }
            }

            boolean targetHasStatic = target.hasEffect(ModMobEffects.STATIC_SHOCK.get());
            if (ElementalThunderFrostReactionsConfig.staticSplashSkipIfTargetHasStatic && targetHasStatic) {
                Debug.logSplashSkipHasStatic(target);
                continue;
            }

            boolean targetHasWetness = target.hasEffect(ModMobEffects.WETNESS.get());
            if (ElementalThunderFrostReactionsConfig.staticSplashTriggerParalysisOnWet && targetHasWetness) {
                Debug.logSplashWetToParalysis(target);
                triggerParalysisFromSplash(source, target);
                affectedCount++;
                paralysisCount++;
                continue;
            }

            float splashDamage = originalDamage * (float) ElementalThunderFrostReactionsConfig.staticSplashDamagePercentage;
            if (splashDamage > 0) {
                DamageSource splashSource = damageSource; 
                target.hurt(splashSource, splashDamage);
                Debug.logSplashDamage(source, target, splashDamage);
               if (target.level() instanceof ServerLevel serverLevel) {
                    EffectHelper.playStaticSplashParticles(serverLevel, source, target);
                }
                affectedCount++;
            }
        }

        Debug.logSplashSummary(source, range, affectedCount, paralysisCount);

        if (affectedCount > 0 || paralysisCount > 0) {
            DebugCommand.sendStaticConductionLog(source, stacks, range, affectedCount, paralysisCount);
        }
    }

    private static void triggerParalysisFromSplash(LivingEntity source, LivingEntity target) {
        if (isImmuneToParalysis(target)) {
            Debug.logParalysisBlacklistImmune(target);
            return;
        }

        if (isImmuneToStatic(target)) {
            clearStaticShock(target);
            return;
        }

        CompoundTag targetData = target.getPersistentData();
        int staticStacks = targetData.getInt(NBT_STATIC_STACKS);
        int wetnessLevel = 0;
        MobEffectInstance wetnessEffect = target.getEffect(ModMobEffects.WETNESS.get());
        if (wetnessEffect != null) {
            wetnessLevel = wetnessEffect.getAmplifier() + 1;
        }

        int paralysisStacks = staticStacks + wetnessLevel;
        int maxParalysisStacks = ElementalThunderFrostReactionsConfig.paralysisMaxStacks;
        if (paralysisStacks > maxParalysisStacks) {
            paralysisStacks = maxParalysisStacks;
        }

        int totalTimer = targetData.getInt(NBT_STATIC_TIMER);
        int interval = ElementalThunderFrostReactionsConfig.staticDamageIntervalTicks;
        if (interval < 1) interval = 1;
        int remainingHits = (totalTimer + interval - 1) / interval;
        double totalDamage = 0;
        for (int i = 0; i < remainingHits; i++) {
            totalDamage += getRandomStaticDamage(target);
        }
        totalDamage *= ElementalThunderFrostReactionsConfig.paralysisDamagePercentage;

        Debug.logParalysisFromSplash(source, target, staticStacks, wetnessLevel, paralysisStacks, remainingHits, totalDamage);

        if (totalDamage > 0) {
            DamageSource damageSource = ModDamageTypes.source(target.level(), ModDamageTypes.STATIC_SHOCK);
            target.hurt(damageSource, (float) totalDamage);
        }

        clearStaticShock(target);
        if (target.hasEffect(ModMobEffects.WETNESS.get())) {
            target.removeEffect(ModMobEffects.WETNESS.get());
        }
        WetnessHandler.updateWetnessLevel(target, 0);
        targetData.remove(WetnessHandler.NBT_RAIN_TIMER);
        targetData.remove(WetnessHandler.NBT_DECAY_TIMER);

        int paralysisDuration = ElementalThunderFrostReactionsConfig.paralysisDurationPerStackTicks * paralysisStacks;
        target.addEffect(new MobEffectInstance(
                ModMobEffects.PARALYSIS.get(),
                paralysisDuration,
                paralysisStacks - 1,
                false,
                false,
                true
        ));
        targetData.putInt(NBT_PARALYSIS_STACKS, paralysisStacks);
        targetData.putInt(NBT_PARALYSIS_TIMER, paralysisDuration);

        if (!target.level().isClientSide) {
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    ModSounds.ELECTRIC_ZAP.get(), SoundSource.PLAYERS, 0.7f, 0.4f);
        }
        if (target.level() instanceof ServerLevel serverLevel) {
            EffectHelper.playStaticSplashParticles(serverLevel, source, target);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onKeyPress(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.hasEffect(ModMobEffects.PARALYSIS.get())) {
            if (event.getKey() != GLFW.GLFW_KEY_ESCAPE) {
                for (KeyMapping key : mc.options.keyMappings) {
                    if (key.matches(event.getKey(), 0)) {
                        key.setDown(false);
                        break;
                    }
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.hasEffect(ModMobEffects.PARALYSIS.get())) {
            for (KeyMapping key : mc.options.keyMappings) {
                if (key.matchesMouse(event.getButton())) {
                    key.setDown(false);
                    break;
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.hasEffect(ModMobEffects.PARALYSIS.get())) {
            event.setCanceled(true);
        }
    }

    // ==================== 调试内部类 ====================
    private static final class Debug {
        private static void logImmune(LivingEntity target, int resist) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "静电",
                    String.format("%s 雷霆抗性 %d ≥ %d，免疫静电",
                            target.getName().getString(), resist,
                            ElementalThunderFrostReactionsConfig.staticResistImmunityThreshold));
        }

        private static void logBlacklistImmune(LivingEntity target) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "静电",
                    String.format("%s 位于静电免疫黑名单，免疫静电",
                            target.getName().getString()));
        }

        private static void logParalysisBlacklistImmune(LivingEntity target) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "麻痹",
                    String.format("%s 位于麻痹免疫黑名单，免疫麻痹",
                            target.getName().getString()));
        }

        private static void logStrengthBelowThreshold(LivingEntity attacker, int strength, int threshold) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(attacker.level(), "静电",
                    String.format("%s 雷霆强化 %d < %d，无法触发静电",
                            attacker.getName().getString(), strength, threshold));
        }

        private static void logTriggerChance(LivingEntity attacker, LivingEntity target, int strength, int wetness, double chance, boolean triggered) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(attacker.level(), "静电",
                    String.format("%s 攻击 %s：雷霆强化 %d，目标潮湿 %d，触发概率 %.1f%%，结果 %s",
                            attacker.getName().getString(), target.getName().getString(),
                            strength, wetness, chance * 100,
                            triggered ? "§a成功" : "§c失败"));
        }

        private static void logMaxStacksReached(LivingEntity target, int current) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "静电",
                    String.format("%s 静电已达上限 %d，不再叠加", target.getName().getString(), current));
        }

        private static void logStaticApplied(LivingEntity target, int oldStacks, int newStacks, int totalTicks, int wetness) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "静电",
                    String.format("%s 施加静电：%d → %d 层，持续 %d 刻，目标潮湿 %d",
                            target.getName().getString(), oldStacks, newStacks, totalTicks, wetness));
        }

        private static void logSyncFromEffect(LivingEntity target, int stacks, int ticks) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "静电",
                    String.format("%s 从药水效果同步：层数 %d，剩余 %d 刻",
                            target.getName().getString(), stacks, ticks));
        }

        private static void logWetnessDuringTick(LivingEntity target, int wetness) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "静电",
                    String.format("%s 检测到潮湿 %d，触发麻痹转化", target.getName().getString(), wetness));
        }

        private static void logStackDecay(LivingEntity target, int oldStacks, int newStacks) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "静电",
                    String.format("%s 静电衰减：%d → %d 层", target.getName().getString(), oldStacks, newStacks));
        }

        private static void logDamageTrigger(LivingEntity target, float damage) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "静电",
                    String.format("%s 受到静电伤害：%.2f", target.getName().getString(), damage));
        }

        private static void logParalysisTrigger(LivingEntity target, LivingEntity attacker, int staticStacks, int wetness, int paralysisStacks, int hits, double damage) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "麻痹",
                    String.format("%s 触发麻痹：静电 %d + 潮湿 %d = 麻痹 %d 层，剩余 %d 次伤害，总伤害 %.2f",
                            target.getName().getString(), staticStacks, wetness, paralysisStacks, hits, damage));
        }

        private static void logSplashSkipFromSpread(LivingEntity source) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(source.level(), "静电传导",
                    String.format("%s 的静电来自传播，禁止传导", source.getName().getString()));
        }

        private static void logSplashImmune(LivingEntity target, int resist) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "静电传导",
                    String.format("%s 雷霆抗性 %d ≥ %d，免疫传导",
                            target.getName().getString(), resist,
                            ElementalThunderFrostReactionsConfig.staticResistImmunityThreshold));
        }

        private static void logSplashExclude(LivingEntity target, String reason) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "静电传导",
                    String.format("排除 %s：%s", target.getName().getString(), reason));
        }

        private static void logSplashSkipHasStatic(LivingEntity target) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "静电传导",
                    String.format("跳过 %s：已有静电", target.getName().getString()));
        }

        private static void logSplashWetToParalysis(LivingEntity target) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "静电传导",
                    String.format("%s 潮湿目标，转为麻痹", target.getName().getString()));
        }

        private static void logSplashDamage(LivingEntity source, LivingEntity target, float damage) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(source.level(), "静电传导",
                    String.format("%s 传导 %s 造成 %.2f 伤害", source.getName().getString(), target.getName().getString(), damage));
        }

        private static void logSplashSummary(LivingEntity source, int range, int affectedCount, int paralysisCount) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(source.level(), "静电传导",
                    String.format("%s 传导范围 %d 格，影响 %d 个目标（其中 %d 个转为麻痹）",
                            source.getName().getString(), range, affectedCount, paralysisCount));
        }

        private static void logParalysisFromSplash(LivingEntity source, LivingEntity target, int staticStacks, int wetness, int paralysisStacks, int hits, double damage) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "静电传导",
                    String.format("%s 传导触发 %s 麻痹：静电 %d + 潮湿 %d = 麻痹 %d 层，剩余 %d 次伤害，总伤害 %.2f",
                            source.getName().getString(), target.getName().getString(),
                            staticStacks, wetness, paralysisStacks, hits, damage));
        }
    }
}