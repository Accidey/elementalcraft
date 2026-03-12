package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.util.EffectHelper;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import com.xulai.elementalcraft.event.ScorchedHandler;
import com.xulai.elementalcraft.event.WetnessHandler;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class ReactionHandler {

    private static final Random RANDOM = new Random();

    private static final String NBT_DRAIN_COOLDOWN = "ec_drain_cd";
    private static final String NBT_WILDFIRE_COOLDOWN = "ec_wildfire_cd";
    private static final String NBT_SPREADED = "ec_spreaded";
    private static final String NBT_INFECTED = "ec_infected";

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

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

    @SubscribeEvent
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

                if (RANDOM.nextDouble() < chance) {
                    stackSporeEffect(target, ElementalFireNatureReactionsConfig.natureParasiteAmount, attacker);
                    EffectHelper.playSporeAmbient(target);
                }
            }

            if (checkCooldown(attacker, NBT_DRAIN_COOLDOWN)) {
                int wetnessLevel = WetnessHandler.getWetnessLevel(target);

                if (wetnessLevel > 0 && naturePower >= ElementalFireNatureReactionsConfig.natureSiphonThreshold) {
                    triggerParasiticDrain(attacker, target, wetnessLevel, naturePower);
                }
            }
        } else if (attackType == ElementType.FIRE) {
            if (ModMobEffects.SPORES.isPresent() && ModMobEffects.SPORES.get() != null && target.hasEffect(ModMobEffects.SPORES.get())
                    && !event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {

                if (firePower >= ElementalFireNatureReactionsConfig.blastTriggerThreshold) {
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

        int maxStacks = ElementalFireNatureReactionsConfig.sporeMaxStacks;

        if (currentStacks >= maxStacks) {
            return;
        }

        int newStacks = Math.min(maxStacks, currentStacks + layersToAdd);
        int durationTicks = newStacks * ElementalFireNatureReactionsConfig.sporeDurationPerStack * 20;

        boolean isThunder = ElementUtils.getDisplayEnhancement(target, ElementType.THUNDER) > 0 ||
                ElementUtils.getDisplayResistance(target, ElementType.THUNDER) > 0;
        boolean isFire = ElementUtils.getDisplayEnhancement(target, ElementType.FIRE) > 0 ||
                ElementUtils.getDisplayResistance(target, ElementType.FIRE) > 0;

        if (isThunder) {
            durationTicks = (int) (durationTicks * ElementalFireNatureReactionsConfig.sporeThunderMultiplier);
        }
        if (isFire) {
            durationTicks = (int) (durationTicks * ElementalFireNatureReactionsConfig.sporeFireDurationReduction);
        }

        if (newStacks > 0) {
            target.addEffect(new MobEffectInstance(ModMobEffects.SPORES.get(), durationTicks, newStacks - 1));

            if (target.getPersistentData().contains(ScorchedHandler.NBT_SCORCHED_TICKS)) {
                int sourceFirePower = target.getPersistentData().getInt(ScorchedHandler.NBT_SCORCHED_SOURCE_FIRE_POWER);
                triggerToxicBlastFromScorched(target, newStacks, sourceFirePower, applier);
            }
        }
    }

    private static void processContagion(LivingEntity source, int stacks) {
        CompoundTag data = source.getPersistentData();

        boolean isSpreaded = data.getBoolean(NBT_SPREADED);
        boolean isInfected = data.getBoolean(NBT_INFECTED);

        if (isSpreaded || isInfected) return;

        data.putBoolean(NBT_SPREADED, true);

        double radius = ElementalFireNatureReactionsConfig.contagionBaseRadius + ((stacks - ElementalFireNatureReactionsConfig.sporeReactionThreshold) * ElementalFireNatureReactionsConfig.contagionRadiusPerStack);

        int transferStacks = (int) Math.floor(stacks * ElementalFireNatureReactionsConfig.contagionIntensityRatio);
        if (transferStacks < 1) transferStacks = 1;

        AABB sourceBox = source.getBoundingBox();
        if (sourceBox == null) return;

        AABB area = sourceBox.inflate(radius);
        List<LivingEntity> targets = source.level().getEntitiesOfClass(LivingEntity.class, area);

        List<LivingEntity> infectedTargets = new ArrayList<>();

        for (LivingEntity target : targets) {
            if (target == source) continue;

            if (ElementalFireNatureReactionsConfig.contagionOnlyHostile && !(target instanceof Enemy)) {
                continue;
            }

            target.getPersistentData().putBoolean(NBT_INFECTED, true);

            int wetnessLevel = WetnessHandler.getWetnessLevel(target);
            int wetnessBonus = 0;

            if (wetnessLevel > ElementalFireNatureReactionsConfig.contagionWetnessThreshold) {
                int effectiveWetness = wetnessLevel - ElementalFireNatureReactionsConfig.contagionWetnessThreshold;
                wetnessBonus = (int) Math.floor(effectiveWetness * ElementalFireNatureReactionsConfig.contagionWetnessConversionRatio);
                wetnessBonus = Math.min(wetnessBonus, ElementalFireNatureReactionsConfig.contagionWetnessMaxBonus);
            }

            if (wetnessBonus > 0 && ElementalFireNatureReactionsConfig.contagionConsumesWetness) {
                WetnessHandler.updateWetnessLevel(target, 0);
            }

            stackSporeEffect(target, transferStacks + wetnessBonus);
            infectedTargets.add(target);
        }

        EffectHelper.playSporeContagion(source, infectedTargets, radius);
    }

    private static void triggerParasiticDrain(LivingEntity attacker, LivingEntity target, int currentWetness, double naturePower) {
        double step = ElementalFireNatureReactionsConfig.natureDrainPowerStep;

        int baseDrain = ElementalFireNatureReactionsConfig.natureDrainAmount;
        int bonusDrain = (int) Math.floor(naturePower / step);
        int drainCapacity = baseDrain + bonusDrain;

        if (drainCapacity < 1) drainCapacity = 1;

        int actualDrain = Math.min(currentWetness, drainCapacity);
        if (actualDrain <= 0) return;

        int newTargetWetness = currentWetness - actualDrain;
        WetnessHandler.updateWetnessLevel(target, newTargetWetness);

        int attackerWetness = WetnessHandler.getWetnessLevel(attacker);
        int newAttackerWetness = Math.min(ElementalFireNatureReactionsConfig.wetnessMaxLevel, attackerWetness + actualDrain);
        WetnessHandler.updateWetnessLevel(attacker, newAttackerWetness);

        stackSporeEffect(target, actualDrain);

        float healAmount = (float) (actualDrain * ElementalFireNatureReactionsConfig.natureSiphonHeal);
        attacker.heal(healAmount);

        DebugCommand.sendNatureSiphonLog(attacker, target, actualDrain, healAmount);

        setCooldown(attacker, NBT_DRAIN_COOLDOWN, ElementalFireNatureReactionsConfig.natureDrainCooldown);

        EffectHelper.playDrainEffect(attacker, target);
        EffectHelper.playSound(target.level(), attacker, SoundEvents.ZOMBIE_VILLAGER_CONVERTED, 0.5f, 1.5f);
    }

    private static void triggerToxicBlast(Level level, LivingEntity attacker, LivingEntity target, double firePower) {
        triggerToxicBlast(level, attacker, target, firePower, attacker);
    }

    private static void triggerToxicBlast(Level level, LivingEntity attacker, LivingEntity target, double firePower, LivingEntity killCredit) {
        if (ModMobEffects.SPORES.get() == null) return;
        MobEffectInstance sporeEffect = target.getEffect(ModMobEffects.SPORES.get());
        int amplifier = (sporeEffect != null) ? sporeEffect.getAmplifier() : -1;
        int stacks = amplifier + 1;

        target.removeEffect(ModMobEffects.SPORES.get());

        if (stacks < ElementalFireNatureReactionsConfig.sporeReactionThreshold) {
            int scorchDuration = (int) (ElementalFireNatureReactionsConfig.blastScorchBase * 20);
            int damageStrength = (int) (firePower * ElementalFireNatureReactionsConfig.blastWeakIgniteMult);

            ScorchedHandler.applyScorched(target, damageStrength, scorchDuration, (int) firePower);
            EffectHelper.playSound(level, target, SoundEvents.FIRECHARGE_USE, 1.0f, 1.2f);
        } else {
            int extraStacks = stacks - ElementalFireNatureReactionsConfig.sporeReactionThreshold;

            double fireStep = ElementalFireNatureReactionsConfig.blastDmgStep;
            double dmgPerStep = ElementalFireNatureReactionsConfig.blastDmgAmount;
            double bonusFromStats = 0;

            if (fireStep > 0) {
                bonusFromStats = (firePower / fireStep) * dmgPerStep;
            }

            float rawBaseDamage = (float) (ElementalFireNatureReactionsConfig.blastBaseDamage
                    + (extraStacks * ElementalFireNatureReactionsConfig.blastGrowthDamage)
                    + bonusFromStats);

            double radius = ElementalFireNatureReactionsConfig.blastBaseRange + (extraStacks * ElementalFireNatureReactionsConfig.blastGrowthRange);
            int scorchDuration = (int) ((ElementalFireNatureReactionsConfig.blastBaseScorchTime + (extraStacks * ElementalFireNatureReactionsConfig.blastGrowthScorchTime)) * 20);

            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.BLOCKS, 4.0F, (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F);

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                        target.getX(), target.getY() + 1.0, target.getZ(), 1, 0, 0, 0, 0);

                serverLevel.sendParticles(ParticleTypes.FLAME,
                        target.getX(), target.getY() + 0.5, target.getZ(),
                        50, 1.5, 1.5, 1.5, 0.2);

                serverLevel.sendParticles(ParticleTypes.LAVA,
                        target.getX(), target.getY() + 0.5, target.getZ(),
                        20, 1.0, 1.0, 1.0, 0.0);

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

                        entity.invulnerableTime = 0;

                        if (ElementalFireNatureReactionsConfig.blastChainReaction
                                && ModMobEffects.SPORES.isPresent()
                                && ModMobEffects.SPORES.get() != null
                                && entity.hasEffect(ModMobEffects.SPORES.get())) {
                            triggerToxicBlast(level, attacker, entity, firePower, killCredit);
                        }

                        float mitigation = calculateBlastMitigation(entity);
                        float finalDamage = rawBaseDamage * (1.0f - mitigation);

                        entity.hurt(ModDamageTypes.source(level, ModDamageTypes.LAVA_MAGIC, killCredit), finalDamage);

                        ScorchedHandler.applyScorched(entity, (int) firePower, scorchDuration, (int) firePower);
                        affectedCount++;
                    }

                    DebugCommand.sendToxicBlastLog(attacker, target, stacks, radius, affectedCount);
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

        return (float) Math.min(actualBlastRed + actualGeneralRed, 1.0);
    }

    private static void triggerWildfireEjection(LivingEntity victim, Entity attacker) {
        double radius = ElementalFireNatureReactionsConfig.wildfireRadius;
        EffectHelper.playWildfireEjection(victim, radius);

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

            if (ElementalFireNatureReactionsConfig.wildfireClearBurning) {
                enemy.clearFire();

                if (enemy.getPersistentData().contains(ScorchedHandler.NBT_SCORCHED_TICKS)) {
                    enemy.getPersistentData().remove(ScorchedHandler.NBT_SCORCHED_TICKS);
                    enemy.getPersistentData().remove(ScorchedHandler.NBT_SCORCHED_STRENGTH);
                }
            }

            stackSporeEffect(enemy, ElementalFireNatureReactionsConfig.wildfireSporeAmount);
            affectedCount++;
        }

        DebugCommand.sendWildfireLog(victim, radius, affectedCount);

        setCooldown(victim, NBT_WILDFIRE_COOLDOWN, ElementalFireNatureReactionsConfig.wildfireCooldown);
    }

    private static boolean checkCooldown(LivingEntity entity, String key) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(key)) return true;

        long endTick = data.getLong(key);
        return entity.level().getGameTime() >= endTick;
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
        target.clearFire();

        DebugCommand.sendScorchedSporeReactionLog(target, killCredit, stacks);
    }
}