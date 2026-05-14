package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.sound.ModSounds;
import com.xulai.elementalcraft.util.EffectHelper;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import com.xulai.elementalcraft.event.ScorchedHandler;
import com.xulai.elementalcraft.event.WetnessHandler;
import com.xulai.elementalcraft.util.ElementDamageHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;
import net.minecraftforge.eventbus.api.EventPriority;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class ReactionHandler {
    private static final Random RANDOM = new Random();
    private static final String NBT_WILDFIRE_COOLDOWN = "ec_wildfire_cd";
    private static final String NBT_SPREADED = "ec_spreaded";
    private static final String NBT_CONTAGION_SOURCE = "ec_contagion_source";

    public static final String NBT_INFECTED = "ec_infected";

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        if (ElementalFireNatureReactionsConfig.contagionAllowReinfected) {
            CompoundTag data = entity.getPersistentData();
            boolean hasSpores = ModMobEffects.SPORES.isPresent() && ModMobEffects.SPORES.get() != null
                    && entity.hasEffect(ModMobEffects.SPORES.get());
            if (!hasSpores) {
                boolean changed = false;
                if (data.getBoolean(NBT_INFECTED)) {
                    data.putBoolean(NBT_INFECTED, false);
                    changed = true;
                }
                if (data.getBoolean(NBT_SPREADED)) {
                    data.putBoolean(NBT_SPREADED, false);
                    changed = true;
                }
                if (data.contains(NBT_CONTAGION_SOURCE)) {
                    data.remove(NBT_CONTAGION_SOURCE);
                    changed = true;
                }
            }
        }

        if (entity.tickCount % ElementalFireNatureReactionsConfig.contagionCheckInterval != 0) return;

        if (ModMobEffects.SPORES.isPresent() && ModMobEffects.SPORES.get() != null && entity.hasEffect(ModMobEffects.SPORES.get())) {
            MobEffectInstance sporeEffect = entity.getEffect(ModMobEffects.SPORES.get());
            if (sporeEffect == null) return;
            int amplifier = sporeEffect.getAmplifier();
            int stacks = amplifier + 1;
            if (stacks >= ElementalFireNatureReactionsConfig.sporeReactionThreshold) {
                processContagion(entity, stacks);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof LivingEntity attacker)) return;
        LivingEntity target = event.getEntity();
        Level level = target.level();

        ElementType attackType = ElementUtils.getConsistentAttackElement(attacker);
        double naturePower = ElementUtils.getDisplayEnhancement(attacker, ElementType.NATURE);
        double firePower = ElementUtils.getDisplayEnhancement(attacker, ElementType.FIRE);

        if (attackType == ElementType.NATURE) {
            if (naturePower >= ElementalFireNatureReactionsConfig.natureParasiteBaseThreshold) {
                double chance;
                double scalingStep = ElementalFireNatureReactionsConfig.natureParasiteScalingStep;
                if (naturePower < scalingStep) {
                    chance = ElementalFireNatureReactionsConfig.natureParasiteBaseChance;
                } else {
                    int steps = (int) ((naturePower - scalingStep) / scalingStep);
                    chance = ElementalFireNatureReactionsConfig.natureParasiteBaseChance + (steps * ElementalFireNatureReactionsConfig.natureParasiteScalingChance);
                }
                chance = Math.min(1.0, chance);

                int attackerWetness = WetnessHandler.getWetnessLevel(attacker);
                if (attackerWetness > 0) {
                    chance += attackerWetness * ElementalFireNatureReactionsConfig.natureParasiteWetnessBonus;
                    chance = Math.min(1.0, chance);
                }
                boolean triggered = RANDOM.nextDouble() < chance;
                if (triggered) {
                    stackSporeEffect(target, ElementalFireNatureReactionsConfig.natureParasiteAmount, attacker);
                    EffectHelper.playSporeAmbient(target);
                }
            }
        } else if (attackType == ElementType.FIRE) {
            if (ModMobEffects.SPORES.isPresent() && ModMobEffects.SPORES.get() != null && target.hasEffect(ModMobEffects.SPORES.get()) && !event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
                if (WetnessHandler.getWetnessLevel(target) > 0 && ElementUtils.getConsistentAttackElement(target) == ElementType.NATURE) {
                } else if (firePower >= ElementalFireNatureReactionsConfig.blastTriggerThreshold) {
                    triggerToxicBlast(level, attacker, target, firePower);
                }
            }

            double victimNaturePower = ElementUtils.getDisplayEnhancement(target, ElementType.NATURE);
            boolean isNatureTarget = ElementUtils.getConsistentAttackElement(target) == ElementType.NATURE;
            boolean hasScorched = target.getPersistentData().contains(ScorchedHandler.NBT_SCORCHED_TICKS);
            boolean cooldownOk = checkCooldown(target, NBT_WILDFIRE_COOLDOWN);
            boolean powerOk = victimNaturePower >= ElementalFireNatureReactionsConfig.wildfireTriggerThreshold;

            if (isNatureTarget && powerOk && hasScorched && cooldownOk) {
                triggerWildfireEjection(target, attacker);
            }
        }
    }

    public static boolean isSporeImmune(LivingEntity target) {
        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).toString();
        if (ElementalFireNatureReactionsConfig.cachedSporeBlacklist != null && ElementalFireNatureReactionsConfig.cachedSporeBlacklist.contains(entityId)) {
            return true;
        }
        double natureResistance = ElementUtils.getDisplayResistance(target, ElementType.NATURE);
        if (natureResistance >= ElementalFireNatureReactionsConfig.natureImmunityThreshold) {
            return true;
        }
        return false;
    }

    public static void stackSporeEffect(LivingEntity target, int layersToAdd) {
        stackSporeEffect(target, layersToAdd, null);
    }

    public static void stackSporeEffect(LivingEntity target, int layersToAdd, LivingEntity applier) {
        if (!ModMobEffects.SPORES.isPresent() || ModMobEffects.SPORES.get() == null) return;

        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).toString();
        if (ElementalFireNatureReactionsConfig.cachedSporeBlacklist != null && ElementalFireNatureReactionsConfig.cachedSporeBlacklist.contains(entityId)) {
            return;
        }

        double natureResistance = ElementUtils.getDisplayResistance(target, ElementType.NATURE);
        if (natureResistance >= ElementalFireNatureReactionsConfig.natureImmunityThreshold) {
            return;
        }

        MobEffectInstance currentEffect = target.getEffect(ModMobEffects.SPORES.get());
        int currentAmp = (currentEffect != null) ? currentEffect.getAmplifier() : -1;
        int currentStacks = currentAmp + 1;
        boolean isNewEffect = (currentEffect == null);
        int maxStacks = ElementalFireNatureReactionsConfig.sporeMaxStacks;

        if (currentStacks >= maxStacks) {
            return;
        }

        int newStacks = Math.min(maxStacks, currentStacks + layersToAdd);
        int durationTicks = newStacks * ElementalFireNatureReactionsConfig.sporeDurationPerStack * 20;

        boolean isThunder = ElementUtils.getConsistentAttackElement(target) == ElementType.THUNDER;
        boolean isFire = ElementUtils.getConsistentAttackElement(target) == ElementType.FIRE;
        boolean isNature = ElementUtils.getConsistentAttackElement(target) == ElementType.NATURE;
        boolean isFrost = ElementUtils.getConsistentAttackElement(target) == ElementType.FROST;

        if (isThunder) {
            durationTicks = (int) (durationTicks * ElementalFireNatureReactionsConfig.sporeThunderMultiplier);
        }
        if (isFire) {
            durationTicks = (int) (durationTicks * ElementalFireNatureReactionsConfig.sporeFireDurationReduction);
        }
        if (isNature) {
            durationTicks = (int) (durationTicks * ElementalFireNatureReactionsConfig.sporeNatureDurationMultiplier);
        }
        if (isFrost) {
            durationTicks = (int) (durationTicks * ElementalFireNatureReactionsConfig.sporeFrostDurationMultiplier);
        }

        if (newStacks > 0) {
            target.addEffect(new MobEffectInstance(ModMobEffects.SPORES.get(), durationTicks, newStacks - 1));
            target.getPersistentData().putLong("EC_SporeApplyTick", target.level().getGameTime());
            if (isNewEffect && !target.level().isClientSide) {
                target.level().playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.SPORE_GAIN.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            if(target.hasEffect(ModMobEffects.STATIC_SHOCK.get())){
                StaticShockHandler.tryTriggerSporeBlast(target);
            }
            if (target.getPersistentData().contains(ScorchedHandler.NBT_SCORCHED_TICKS)) {
                int sourceFirePower = target.getPersistentData().getInt(ScorchedHandler.NBT_SCORCHED_SOURCE_FIRE_POWER);
                triggerToxicBlastFromScorched(target, newStacks, sourceFirePower, applier);
            }
            if (ElementalThunderFrostReactionsConfig.frostbiteClearSporesEnabled && FrostbiteHandler.hasFrostbite(target)) {
                target.removeEffect(ModMobEffects.SPORES.get());
                if (!target.level().isClientSide) {
                    target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SNOW_PLACE, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 0.5f);
                }
            }
        }
    }

    private static void processContagion(LivingEntity source, int stacks) {
        CompoundTag data = source.getPersistentData();
        boolean isSpreaded = data.getBoolean(NBT_SPREADED);
        boolean isInfected = data.getBoolean(NBT_INFECTED);

        boolean blockByInfected = isInfected && !ElementalFireNatureReactionsConfig.contagionAllowInfectedSpread;
        if (isSpreaded || blockByInfected) {
            return;
        }

        double radius = ElementalFireNatureReactionsConfig.contagionBaseRadius + ((stacks - ElementalFireNatureReactionsConfig.sporeReactionThreshold) * ElementalFireNatureReactionsConfig.contagionRadiusPerStack);
        int transferStacks = (int) Math.floor(stacks * ElementalFireNatureReactionsConfig.contagionIntensityRatio);
        if (transferStacks < 1) transferStacks = 1;

        AABB sourceBox = source.getBoundingBox();
        if (sourceBox == null) return;
        AABB area = sourceBox.inflate(radius);
        List<LivingEntity> allNearby = source.level().getEntitiesOfClass(LivingEntity.class, area);
        List<LivingEntity> validTargets = new ArrayList<>();

        UUID contagionSourceUUID = null;
        if (data.contains(NBT_CONTAGION_SOURCE)) {
            try {
                contagionSourceUUID = data.getUUID(NBT_CONTAGION_SOURCE);
            } catch (Exception ignored) {}
        }

        for (LivingEntity target : allNearby) {
            if (target == source) continue;

            if (ModMobEffects.SPORES.isPresent() && target.hasEffect(ModMobEffects.SPORES.get())) {
                continue;
            }

            if (contagionSourceUUID != null && target.getUUID().equals(contagionSourceUUID)) {
                continue;
            }

            boolean onlyHostile = ElementalFireNatureReactionsConfig.contagionOnlyHostile;
            if (onlyHostile && !(target instanceof Enemy)) {
                continue;
            }

            if (target instanceof Player) {
                continue;
            }

            if (target instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) {
                continue;
            }
            if (target instanceof AbstractHorse horse && horse.getOwnerUUID() != null) {
                continue;
            }

            validTargets.add(target);
        }

        if (validTargets.isEmpty()) {
            return;
        }
        data.putBoolean(NBT_SPREADED, true);

        List<LivingEntity> infectedTargets = new ArrayList<>();
        for (LivingEntity target : validTargets) {
            CompoundTag targetData = target.getPersistentData();
            targetData.putUUID(NBT_CONTAGION_SOURCE, source.getUUID());
            targetData.putBoolean(NBT_INFECTED, true);

            WetnessHandler.convertWetnessToSpores(target);
            stackSporeEffect(target, transferStacks);

            infectedTargets.add(target);
        }

        EffectHelper.playSporeContagion(source, infectedTargets, radius);
    }


    private static void triggerToxicBlast(Level level, LivingEntity attacker, LivingEntity target, double firePower) {
        triggerToxicBlast(level, attacker, target, firePower, attacker);
    }

    public static void triggerToxicBlast(Level level, LivingEntity attacker, LivingEntity target, double firePower, LivingEntity killCredit) {
        if (ModMobEffects.SPORES.get() == null) return;
        MobEffectInstance sporeEffect = target.getEffect(ModMobEffects.SPORES.get());
        int amplifier = (sporeEffect != null) ? sporeEffect.getAmplifier() : -1;
        int stacks = amplifier + 1;

        target.removeEffect(ModMobEffects.SPORES.get());

        if (stacks < ElementalFireNatureReactionsConfig.sporeReactionThreshold) {
            int scorchDuration = (int) (ElementalFireNatureReactionsConfig.blastScorchBase * 20);
            float damageMultiplier = (float) ElementalFireNatureReactionsConfig.blastWeakIgniteMult;
            ScorchedHandler.applyScorched(target, attacker, (int) firePower, scorchDuration, (int) firePower, damageMultiplier, true);
            EffectHelper.playSound(level, target, SoundEvents.FIRECHARGE_USE, 1.0f, 1.2f);
        } else {
            int extraStacks = stacks - ElementalFireNatureReactionsConfig.sporeReactionThreshold;
            double fireStep = ElementalFireNatureReactionsConfig.blastDmgStep;
            double dmgPerStep = ElementalFireNatureReactionsConfig.blastDmgAmount;
            double bonusFromStats = 0;
            double effectiveFirePower = firePower - ElementalFireNatureReactionsConfig.blastTriggerThreshold;
            if (effectiveFirePower > 0 && fireStep > 0) {
                bonusFromStats = (effectiveFirePower / fireStep) * dmgPerStep;
            }

            float rawBaseDamage = (float) (ElementalFireNatureReactionsConfig.blastBaseDamage + (extraStacks * ElementalFireNatureReactionsConfig.blastGrowthDamage) + bonusFromStats);
            double radius = ElementalFireNatureReactionsConfig.blastBaseRange + (extraStacks * ElementalFireNatureReactionsConfig.blastGrowthRange);
            int scorchDuration = (int) ((ElementalFireNatureReactionsConfig.blastBaseScorchTime + (extraStacks * ElementalFireNatureReactionsConfig.blastGrowthScorchTime)) * 20);

            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.BLOCKS, 4.0F, (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F);

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, target.getX(), target.getY() + 1.0, target.getZ(), 1, 0, 0, 0, 0);
                serverLevel.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 0.5, target.getZ(), 50, 1.5, 1.5, 1.5, 0.2);
                serverLevel.sendParticles(ParticleTypes.LAVA, target.getX(), target.getY() + 0.5, target.getZ(), 20, 1.0, 1.0, 1.0, 0.0);

                serverLevel.getServer().execute(() -> {
                    AABB targetBox = target.getBoundingBox();
                    if (targetBox == null) return;
                    AABB area = targetBox.inflate(radius);
                    List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, area);
                    int affectedCount = 0;
                    for (LivingEntity entity : nearbyEntities) {
                        if (entity == attacker) continue;
                        boolean isPet = false;
                        if (entity instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null && ownable.getOwnerUUID().equals(attacker.getUUID())) {
                            isPet = true;
                        } else if (entity instanceof AbstractHorse horse && horse.getOwnerUUID() != null && horse.getOwnerUUID().equals(attacker.getUUID())) {
                            isPet = true;
                        }
                        if (isPet) continue;

                        if (ElementalFireNatureReactionsConfig.blastChainReaction && ModMobEffects.SPORES.isPresent() && ModMobEffects.SPORES.get() != null && entity.hasEffect(ModMobEffects.SPORES.get())) {
                            triggerToxicBlast(level, attacker, entity, firePower, killCredit);
                        }
                        float mitigation = calculateBlastMitigation(entity);
                        float finalDamage = rawBaseDamage * (1.0f - mitigation);
                        ElementDamageHelper.applyDamage(entity, finalDamage, ModDamageTypes.source(entity.level(), ModDamageTypes.LAVA_MAGIC, killCredit));
                        ScorchedHandler.applyScorched(entity, killCredit, (int) firePower, scorchDuration, (int) firePower, 1.0f, true);
                        affectedCount++;
                    }

                    DebugCommand.ToxicBlastLogContext blastCtx = new DebugCommand.ToxicBlastLogContext();
                    blastCtx.attacker = attacker;
                    blastCtx.target = target;
                    blastCtx.stacks = stacks;
                    blastCtx.radius = radius;
                    blastCtx.affectedCount = affectedCount;
                    blastCtx.rawBaseDamage = rawBaseDamage;
                    DebugCommand.sendToxicBlastLog(blastCtx);
                });
            }
        }
    }

    private static float calculateBlastMitigation(LivingEntity entity) {
        int blastProtLevel = getTotalEnchantmentLevel(Enchantments.BLAST_PROTECTION, entity);
        int generalProtLevel = getTotalEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, entity);
        double maxBlastCap = ElementalFireNatureReactionsConfig.blastMaxBlastProtCap;
        double maxGeneralCap = ElementalFireNatureReactionsConfig.blastMaxGeneralProtCap;
        double blastFactor = maxBlastCap / ElementalFireNatureReactionsConfig.enchantmentCalculationDenominator;
        double generalFactor = maxGeneralCap / ElementalFireNatureReactionsConfig.enchantmentCalculationDenominator;
        double calculatedBlastRed = blastProtLevel * blastFactor;
        double calculatedGeneralRed = generalProtLevel * generalFactor;
        double actualBlastRed = Math.min(calculatedBlastRed, maxBlastCap);
        double actualGeneralRed = Math.min(calculatedGeneralRed, maxGeneralCap);
        float mitigation = (float) Math.min(actualBlastRed + actualGeneralRed, 1.0);
        return mitigation;
    }

    private static void triggerWildfireEjection(LivingEntity victim, Entity attacker) {
        double radius = ElementalFireNatureReactionsConfig.wildfireRadius;
        EffectHelper.playWildfireEjection(victim, radius);

        if (ElementalFireNatureReactionsConfig.wildfireClearBurning) {
            victim.clearFire();
            CompoundTag victimData = victim.getPersistentData();
            victimData.remove(ScorchedHandler.NBT_SCORCHED_TICKS);
            victimData.remove(ScorchedHandler.NBT_SCORCHED_STRENGTH);
            victimData.remove(ScorchedHandler.NBT_SCORCHED_SOURCE_FIRE_POWER);
            victimData.remove(ScorchedHandler.NBT_SCORCHED_DAMAGE_MULT);
        }

        AABB victimBox = victim.getBoundingBox();
        if (victimBox == null) return;
        AABB area = victimBox.inflate(radius);
        List<LivingEntity> enemies = victim.level().getEntitiesOfClass(LivingEntity.class, area);
        int affectedCount = 0;
        for (LivingEntity enemy : enemies) {
            boolean isHostile = (enemy == attacker) || (enemy instanceof Enemy);
            if (enemy == victim || !isHostile) continue;
            Vec3 enemyPos = enemy.position();
            Vec3 victimPos = victim.position();
            if (enemyPos == null || victimPos == null) continue;
            Vec3 delta = enemyPos.subtract(victimPos);
            if (delta.lengthSqr() < 1e-7) {
                delta = new Vec3(RANDOM.nextDouble() - 0.5, 0, RANDOM.nextDouble() - 0.5).normalize();
            } else {
                delta = delta.normalize();
            }
            Vec3 vec = delta.scale(ElementalFireNatureReactionsConfig.wildfireKnockback);
            enemy.push(vec.x, ElementalFireNatureReactionsConfig.wildfireVerticalKnockback, vec.z);
            enemy.hurtMarked = true;
            stackSporeEffect(enemy, ElementalFireNatureReactionsConfig.wildfireSporeAmount);
            affectedCount++;
        }

        DebugCommand.WildfireLogContext wildfireCtx = new DebugCommand.WildfireLogContext();
        wildfireCtx.victim = victim;
        wildfireCtx.radius = radius;
        wildfireCtx.affectedCount = affectedCount;
        DebugCommand.sendWildfireLog(wildfireCtx);

        setCooldown(victim, NBT_WILDFIRE_COOLDOWN, ElementalFireNatureReactionsConfig.wildfireCooldown);
    }

    private static boolean checkCooldown(LivingEntity entity, String key) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(key)) return true;
        long endTick = data.getLong(key);
        boolean ready = entity.level().getGameTime() >= endTick;
        return ready;
    }

    private static void setCooldown(LivingEntity entity, String key, int durationTicks) {
        entity.getPersistentData().putLong(key, entity.level().getGameTime() + durationTicks);
    }

    private static int getTotalEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantment ench, LivingEntity entity) {
        int total = 0;
        for (ItemStack stack : entity.getArmorSlots()) {
            total += stack.getEnchantmentLevel(ench);
        }
        return total;
    }

    public static void triggerToxicBlastFromScorched(LivingEntity target, int stacks, int sourceFirePower, LivingEntity killCredit) {
        if (target.level().isClientSide) return;
        Level level = target.level();
        if (killCredit == null) {
            killCredit = target;
        }
        triggerToxicBlast(level, killCredit, target, sourceFirePower, killCredit);

        target.getPersistentData().remove(ScorchedHandler.NBT_SCORCHED_TICKS);
        target.getPersistentData().remove(ScorchedHandler.NBT_SCORCHED_STRENGTH);
        target.getPersistentData().remove(ScorchedHandler.NBT_SCORCHED_SOURCE_FIRE_POWER);
        target.getPersistentData().remove(ScorchedHandler.NBT_SCORCHED_DAMAGE_MULT);
        target.clearFire();

        DebugCommand.ScorchedSporeReactionLogContext scorchedCtx = new DebugCommand.ScorchedSporeReactionLogContext();
        scorchedCtx.target = target;
        scorchedCtx.applier = killCredit;
        scorchedCtx.stacks = stacks;
        DebugCommand.sendScorchedSporeReactionLog(scorchedCtx);
    }

    public static void triggerStaticSporeBlast(LivingEntity target, double firePower) {
    triggerToxicBlast(target.level(), target, target, firePower, target);
    }
}
