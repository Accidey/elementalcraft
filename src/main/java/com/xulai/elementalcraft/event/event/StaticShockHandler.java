package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import net.minecraft.world.entity.LightningBolt;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.sound.ModSounds;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import com.xulai.elementalcraft.util.ElementDamageHelper;
import com.xulai.elementalcraft.event.WetnessHandler;
import com.xulai.elementalcraft.util.EffectHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;

import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
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
    private static final String NBT_PARALYSIS_COOLDOWN_TIMER = "ec_paralysis_cooldown_timer";

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

    private static void trimStaticStacks(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(NBT_STATIC_STACKS)) return;
        int currentStacks = data.getInt(NBT_STATIC_STACKS);
        int maxStacks = ElementalThunderFrostReactionsConfig.staticMaxTotalStacks;
        if (currentStacks > maxStacks) {
            int totalTimer = data.getInt(NBT_STATIC_TIMER);
            int durationPerStack = ElementalThunderFrostReactionsConfig.staticDurationPerStackTicks;
            int excess = currentStacks - maxStacks;
            totalTimer = Math.max(1, totalTimer - excess * durationPerStack);
            data.putInt(NBT_STATIC_STACKS, maxStacks);
            data.putInt(NBT_STATIC_TIMER, totalTimer);
            updateEffect(entity, maxStacks, totalTimer);
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        LivingEntity target = event.getEntity();

        if (isImmuneToStatic(target)) {
            clearStaticShock(target);
            return;
        }

        trimStaticStacks(target);

        ElementType consistentElement = ElementUtils.getConsistentAttackElement(attacker);
        if (consistentElement != ElementType.THUNDER) {
            return;
        }

        int thunderStrength = ElementUtils.getDisplayEnhancement(attacker, ElementType.THUNDER);
        int threshold = ElementalThunderFrostReactionsConfig.thunderStrengthThreshold;
        if (thunderStrength < threshold) {
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

        if (!triggered) return;

        if (wetnessLevel > 0) {
            CompoundTag data = target.getPersistentData();
            int currentStacks = data.getInt(NBT_STATIC_STACKS);
            int maxStacks = ElementalThunderFrostReactionsConfig.staticMaxTotalStacks;
            if (currentStacks >= maxStacks) {
                return;
            }
            int addStacks = ElementalThunderFrostReactionsConfig.staticMaxStacksPerAttack;
            int newStacks = Math.min(maxStacks, currentStacks + addStacks);
            int actualAdded = newStacks - currentStacks;
            int durationPerStack = ElementalThunderFrostReactionsConfig.staticDurationPerStackTicks;
            int addTicks = actualAdded * durationPerStack;
            int newTotalTicks = data.getInt(NBT_STATIC_TIMER) + addTicks;
            data.putInt(NBT_STATIC_STACKS, newStacks);
            data.putInt(NBT_STATIC_TIMER, newTotalTicks);
            triggerParalysisReaction(attacker, target);
            if (target.hasEffect(ModMobEffects.SPORES.get())) {
                tryTriggerSporeBlast(target);
            }
            return;
        }

        CompoundTag data = target.getPersistentData();
        int currentStacks = data.getInt(NBT_STATIC_STACKS);
        int maxStacks = ElementalThunderFrostReactionsConfig.staticMaxTotalStacks;
        if (currentStacks >= maxStacks) {
            return;
        }
        int addStacks = ElementalThunderFrostReactionsConfig.staticMaxStacksPerAttack;
        int newStacks = Math.min(maxStacks, currentStacks + addStacks);
        int actualAdded = newStacks - currentStacks;
        int durationPerStack = ElementalThunderFrostReactionsConfig.staticDurationPerStackTicks;
        int addTicks = actualAdded * durationPerStack;
        int currentTimer = data.getInt(NBT_STATIC_TIMER);
        int newTotalTicks = currentTimer + addTicks;
        data.putInt(NBT_STATIC_STACKS, newStacks);
        data.putInt(NBT_STATIC_TIMER, newTotalTicks);
        data.putInt(NBT_STATIC_DAMAGE_TIMER, data.getInt(NBT_STATIC_DAMAGE_TIMER));
        updateEffect(target, newStacks, newTotalTicks);
        if (target.hasEffect(ModMobEffects.SPORES.get())) {
            tryTriggerSporeBlast(target);
        }
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
        int cooldownTimer = data.getInt(NBT_PARALYSIS_COOLDOWN_TIMER);
        if (cooldownTimer > 0) {
            cooldownTimer--;
            data.putInt(NBT_PARALYSIS_COOLDOWN_TIMER, cooldownTimer);
        }

        trimStaticStacks(entity);
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
            if (effectInstance.getDuration() != remainingTicks || effectInstance.getAmplifier() != amplifier) {
                entity.removeEffect(ModMobEffects.STATIC_SHOCK.get());
                entity.addEffect(new MobEffectInstance(
                        ModMobEffects.STATIC_SHOCK.get(), remainingTicks, amplifier, false, false, true
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

        if (entity.level() instanceof ServerLevel serverLevel) {
            EffectHelper.playStaticShockParticles(serverLevel, entity);
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

        if (totalTimer <= 0) {
            clearStaticShock(entity);
            return;
        }

        updateEffect(entity, stacks, totalTimer);
    }

    private static float applyEnchantmentReduction(LivingEntity entity, float damage) {
        int totalProtLevel = getTotalEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, entity);
        int totalProjectileProtLevel = getTotalEnchantmentLevel(Enchantments.PROJECTILE_PROTECTION, entity);

        double maxProtCap = ElementalThunderFrostReactionsConfig.staticMaxProtCap;
        double maxProjectileProtCap = ElementalThunderFrostReactionsConfig.staticMaxProjectileProtCap;
        double denom = ElementalFireNatureReactionsConfig.enchantmentCalculationDenominator;

        double protFactor = maxProtCap / denom;
        double projectileProtFactor = maxProjectileProtCap / denom;

        double calculatedProtRed = totalProtLevel * protFactor;
        double calculatedProjectileProtRed = totalProjectileProtLevel * projectileProtFactor;

        double actualProtRed = Math.min(calculatedProtRed, maxProtCap);
        double actualProjectileProtRed = Math.min(calculatedProjectileProtRed, maxProjectileProtCap);

        double totalReduction = Math.min(actualProtRed + actualProjectileProtRed, 1.0);

        return damage * (float) (1.0 - totalReduction);
    }

    private static void triggerStaticDamage(LivingEntity entity) {
        if (isImmuneToStatic(entity)) {
            clearStaticShock(entity);
            return;
        }
        float rawDamage = getRandomStaticDamage(entity);

        float finalDamage = applyEnchantmentReduction(entity, rawDamage);

        ElementDamageHelper.applyDamage(entity, finalDamage, ModDamageTypes.source(entity.level(), ModDamageTypes.STATIC_SHOCK));

        if (!entity.level().isClientSide) {
            float pitch = 0.8f + RANDOM.nextFloat() * 0.4f;
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.ELECTRIC_ZAP.get(), SoundSource.PLAYERS, 0.8f, pitch);
        }
        if (entity.level() instanceof ServerLevel serverLevel) {
            EffectHelper.playStaticBurst(serverLevel, entity);
        }
        if (ElementalThunderFrostReactionsConfig.staticSplashEnabled) {
            applySplashDamage(entity, finalDamage);
        }

        tryTriggerSporeBlast(entity);
    }

    private static int getTotalEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantment ench, LivingEntity entity) {
        int total = 0;
        for (ItemStack stack : entity.getArmorSlots()) {
            total += stack.getEnchantmentLevel(ench);
        }
        return total;
    }


    public static void tryTriggerSporeBlast(LivingEntity target) {
        if (!target.hasEffect(ModMobEffects.SPORES.get())) return;
        MobEffectInstance sporeEffect = target.getEffect(ModMobEffects.SPORES.get());
        if (sporeEffect == null) return;
        int sporeStacks = sporeEffect.getAmplifier() + 1;
        CompoundTag data = target.getPersistentData();
        int staticStacks = data.getInt(NBT_STATIC_STACKS);
        if (staticStacks <= 0) return;

        double baseChance = ElementalThunderFrostReactionsConfig.staticSporeBlastBaseChance;
        double perStatic = ElementalThunderFrostReactionsConfig.staticSporeBlastPerStaticStack;
        double perSpore = ElementalThunderFrostReactionsConfig.staticSporeBlastPerSporeStack;
        double totalChance = Math.min(1.0, baseChance + staticStacks * perStatic + sporeStacks * perSpore);
        boolean triggered = RANDOM.nextDouble() < totalChance;
        if (!triggered) return;

        double firePower = ElementalFireNatureReactionsConfig.blastTriggerThreshold;
        ReactionHandler.triggerStaticSporeBlast(target, firePower);
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

        // Frostbite stacks increase thunder damage vulnerability (Frostbite + Thunder reaction)
        CompoundTag data = entity.getPersistentData();
        if (data.contains(FrostbiteHandler.NBT_FROSTBITE_STACKS)) {
            int frostbiteStacks = data.getInt(FrostbiteHandler.NBT_FROSTBITE_STACKS);
            if (frostbiteStacks > 0) {
                double vulnPerStack = ElementalThunderFrostReactionsConfig.frostbiteThunderVulnerabilityPerStack;
                double totalMultiplier = 1.0 + (frostbiteStacks * vulnPerStack);
                double maxVuln = 1.0 + (5 * vulnPerStack);
                totalMultiplier = Math.min(totalMultiplier, maxVuln);
                damage *= (float) totalMultiplier;
            }
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
                    ModMobEffects.STATIC_SHOCK.get(), totalTicks, amplifier, false, false, true
            ));
        }
    }

    private static void triggerParalysisReaction(LivingEntity attacker, LivingEntity entity) {
        if (isImmuneToParalysis(entity)) {
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

        int cooldownRemaining = data.getInt(NBT_PARALYSIS_COOLDOWN_TIMER);
        int cooldownTicks = ElementalThunderFrostReactionsConfig.paralysisCooldownTicks;
        if (cooldownTicks > 0 && cooldownRemaining > 0) {
            clearStaticShock(entity);
            if (entity.hasEffect(ModMobEffects.WETNESS.get())) {
                entity.removeEffect(ModMobEffects.WETNESS.get());
            }
            WetnessHandler.updateWetnessLevel(entity, 0);
            data.remove(WetnessHandler.NBT_RAIN_TIMER);
            data.remove(WetnessHandler.NBT_DECAY_TIMER);
            return;
        }

        int wetnessLevel = 0;
        if (entity.hasEffect(ModMobEffects.WETNESS.get())) {
            MobEffectInstance wetnessEffect = entity.getEffect(ModMobEffects.WETNESS.get());
            if (wetnessEffect != null) {
                wetnessLevel = wetnessEffect.getAmplifier() + 1;
            }
        }

        int paralysisStacks = Math.max(staticStacks, wetnessLevel);
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

        float finalDamage = 0;
        if (totalDamage > 0) {
            finalDamage = applyEnchantmentReduction(entity, (float) totalDamage);
            ElementDamageHelper.applyDamage(entity, finalDamage, ModDamageTypes.source(entity.level(), ModDamageTypes.STATIC_SHOCK));
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
                ModMobEffects.PARALYSIS.get(), paralysisDuration, paralysisStacks - 1, false, false, true
        ));
        data.putInt(NBT_PARALYSIS_STACKS, paralysisStacks);
        data.putInt(NBT_PARALYSIS_TIMER, paralysisDuration);

        if (cooldownTicks > 0) {
            data.putInt(NBT_PARALYSIS_COOLDOWN_TIMER, paralysisDuration + cooldownTicks);
        }

        if (!entity.level().isClientSide) {
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.ELECTRIC_ZAP.get(), SoundSource.PLAYERS, 1.0f, 0.5f);
        }

        DebugCommand.ParalysisLogContext pCtx = new DebugCommand.ParalysisLogContext();
        pCtx.attacker = attacker;
        pCtx.target = entity;
        pCtx.paralysisStacks = paralysisStacks;
        pCtx.remainingHits = remainingHits;
        pCtx.totalDamage = finalDamage;
        DebugCommand.sendParalysisLog(pCtx);
    }

    private static boolean isHostile(LivingEntity entity) {
        return entity.getType().getCategory() == MobCategory.MONSTER;
    }

    private static void applySplashDamage(LivingEntity source, float originalDamage) {
        CompoundTag data = source.getPersistentData();
        int stacks = data.getInt(NBT_STATIC_STACKS);
        if (stacks <= 0) return;

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
        float accumulatedDamage = 0;

        for (LivingEntity target : nearby) {
            if (target == source) continue;
            if (target.isDeadOrDying()) continue;
            if (isImmuneToStatic(target)) {
                continue;
            }
            if (ElementalThunderFrostReactionsConfig.staticSplashExcludeFriendlyEntities) {
                if (target instanceof Player) {
                    continue;
                }
                if (target instanceof TamableAnimal pet && pet.isTame() && pet.getOwner() != null) {
                    continue;
                }
            }
            if (ElementalThunderFrostReactionsConfig.staticSplashOnlyHostile && !isHostile(target)) {
                continue;
            }
            boolean targetHasStatic = target.hasEffect(ModMobEffects.STATIC_SHOCK.get());
            if (ElementalThunderFrostReactionsConfig.staticSplashSkipIfTargetHasStatic && targetHasStatic) {
                continue;
            }

            boolean targetHasWetness = target.hasEffect(ModMobEffects.WETNESS.get());
            if (ElementalThunderFrostReactionsConfig.staticSplashTriggerParalysisOnWet && targetHasWetness) {
                triggerParalysisFromSplash(source, target);
                affectedCount++;
                paralysisCount++;
                continue;
            }

            float splashDamage = originalDamage * (float) ElementalThunderFrostReactionsConfig.staticSplashDamagePercentage;
            ElementType splashTargetElement = ElementUtils.getElementType(target);
            if (splashTargetElement == ElementType.NATURE) {
                splashDamage *= (float) ElementalThunderFrostReactionsConfig.staticDamageNatureMultiplier;
            } else if (splashTargetElement == ElementType.FROST) {
                splashDamage *= (float) ElementalThunderFrostReactionsConfig.staticDamageFrostMultiplier;
            }
            splashDamage = applyEnchantmentReduction(target, splashDamage);
            if (splashDamage > 0) {
                ElementDamageHelper.applyDamage(target, splashDamage, ModDamageTypes.source(target.level(), ModDamageTypes.STATIC_SHOCK));
                accumulatedDamage += splashDamage;
                if (target.level() instanceof ServerLevel serverLevel) {
                    EffectHelper.playStaticSplashParticles(serverLevel, source, target);
                }
                affectedCount++;
            }
        }
        if (affectedCount > 0 || paralysisCount > 0) {
            DebugCommand.StaticConductionLogContext cCtx = new DebugCommand.StaticConductionLogContext();
            cCtx.source = source;
            cCtx.stacks = stacks;
            cCtx.range = range;
            cCtx.affectedCount = affectedCount;
            cCtx.paralysisCount = paralysisCount;
            cCtx.totalDamage = accumulatedDamage;
            DebugCommand.sendStaticConductionLog(cCtx);
        }
    }

    private static void triggerParalysisFromSplash(LivingEntity source, LivingEntity target) {
        if (isImmuneToParalysis(target)) {
            return;
        }
        if (isImmuneToStatic(target)) {
            clearStaticShock(target);
            return;
        }

        CompoundTag targetData = target.getPersistentData();

        int cooldownRemaining = targetData.getInt(NBT_PARALYSIS_COOLDOWN_TIMER);
        int cooldownTicks = ElementalThunderFrostReactionsConfig.paralysisCooldownTicks;
        if (cooldownTicks > 0 && cooldownRemaining > 0) {
            clearStaticShock(target);
            if (target.hasEffect(ModMobEffects.WETNESS.get())) {
                target.removeEffect(ModMobEffects.WETNESS.get());
            }
            WetnessHandler.updateWetnessLevel(target, 0);
            targetData.remove(WetnessHandler.NBT_RAIN_TIMER);
            targetData.remove(WetnessHandler.NBT_DECAY_TIMER);
            return;
        }

        int staticStacks = targetData.getInt(NBT_STATIC_STACKS);
        int wetnessLevel = 0;
        MobEffectInstance wetnessEffect = target.getEffect(ModMobEffects.WETNESS.get());
        if (wetnessEffect != null) {
            wetnessLevel = wetnessEffect.getAmplifier() + 1;
        }

        int paralysisStacks = Math.max(staticStacks, wetnessLevel);
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

        if (totalDamage > 0) {
            float finalDamage = applyEnchantmentReduction(target, (float) totalDamage);
            ElementDamageHelper.applyDamage(target, finalDamage, ModDamageTypes.source(target.level(), ModDamageTypes.STATIC_SHOCK));
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
                ModMobEffects.PARALYSIS.get(), paralysisDuration, paralysisStacks - 1, false, false, true
        ));
        targetData.putInt(NBT_PARALYSIS_STACKS, paralysisStacks);
        targetData.putInt(NBT_PARALYSIS_TIMER, paralysisDuration);

        if (cooldownTicks > 0) {
            targetData.putInt(NBT_PARALYSIS_COOLDOWN_TIMER, paralysisDuration + cooldownTicks);
        }

        if (!target.level().isClientSide) {
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.ELECTRIC_ZAP.get(), SoundSource.PLAYERS, 0.7f, 0.4f);
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
}
