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
import com.xulai.elementalcraft.util.GlobalDebugLogger;
import com.xulai.elementalcraft.util.DebugMode;
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

            Debug.logContagionCheck(entity, stacks);

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

        Debug.logReactionStart(attacker, target, attackType, naturePower, firePower);

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
                Debug.logParasiteTrigger(attacker, target, chance, triggered);

                if (triggered) {
                    stackSporeEffect(target, ElementalFireNatureReactionsConfig.natureParasiteAmount, attacker);
                    EffectHelper.playSporeAmbient(target);
                }
            }

            if (checkCooldown(attacker, NBT_DRAIN_COOLDOWN)) {
                int wetnessLevel = WetnessHandler.getWetnessLevel(target);

                if (wetnessLevel > 0 && naturePower >= ElementalFireNatureReactionsConfig.natureSiphonThreshold) {
                    triggerParasiticDrain(attacker, target, wetnessLevel, naturePower);
                } else {
                    Debug.logDrainConditionFailed(attacker, target, wetnessLevel, naturePower);
                }
            }
        } else if (attackType == ElementType.FIRE) {
            if (ModMobEffects.SPORES.isPresent() && ModMobEffects.SPORES.get() != null && target.hasEffect(ModMobEffects.SPORES.get())
                    && !event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {

                if (firePower >= ElementalFireNatureReactionsConfig.blastTriggerThreshold) {
                    triggerToxicBlast(level, attacker, target, firePower);
                } else {
                    Debug.logBlastThresholdFailed(attacker, target, firePower);
                }
            }

            double victimNaturePower = ElementUtils.getDisplayEnhancement(target, ElementType.NATURE);
            boolean isNatureTarget = ElementUtils.getConsistentAttackElement(target) == ElementType.NATURE;
            boolean hasScorched = target.getPersistentData().contains(ScorchedHandler.NBT_SCORCHED_TICKS);
            boolean cooldownOk = checkCooldown(target, NBT_WILDFIRE_COOLDOWN);
            boolean powerOk = victimNaturePower >= ElementalFireNatureReactionsConfig.wildfireTriggerThreshold;

            Debug.logWildfireCheck(target, victimNaturePower, isNatureTarget, hasScorched, cooldownOk, powerOk);

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
            Debug.logSporeBlacklist(target, entityId);
            return;
        }

        double natureResistance = ElementUtils.getDisplayResistance(target, ElementType.NATURE);
        if (natureResistance >= ElementalFireNatureReactionsConfig.natureImmunityThreshold) {
            Debug.logSporeImmune(target, natureResistance);
            return;
        }

        MobEffectInstance currentEffect = target.getEffect(ModMobEffects.SPORES.get());
        int currentAmp = (currentEffect != null) ? currentEffect.getAmplifier() : -1;
        int currentStacks = currentAmp + 1;

        int maxStacks = ElementalFireNatureReactionsConfig.sporeMaxStacks;

        if (currentStacks >= maxStacks) {
            Debug.logSporeMaxStacks(target, currentStacks);
            return;
        }

        int newStacks = Math.min(maxStacks, currentStacks + layersToAdd);
        int durationTicks = newStacks * ElementalFireNatureReactionsConfig.sporeDurationPerStack * 20;

        boolean isThunder = ElementUtils.getDisplayEnhancement(target, ElementType.THUNDER) > 0 ||
                ElementUtils.getDisplayResistance(target, ElementType.THUNDER) > 0;
        boolean isFire = ElementUtils.getDisplayEnhancement(target, ElementType.FIRE) > 0 ||
                ElementUtils.getDisplayResistance(target, ElementType.FIRE) > 0;

        Debug.logSporeStack(target, layersToAdd, currentStacks, newStacks, durationTicks, isThunder, isFire);

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

        if (isSpreaded || isInfected) {
            Debug.logContagionBlocked(source, isSpreaded, isInfected);
            return;
        }

        data.putBoolean(NBT_SPREADED, true);

        double radius = ElementalFireNatureReactionsConfig.contagionBaseRadius + ((stacks - ElementalFireNatureReactionsConfig.sporeReactionThreshold) * ElementalFireNatureReactionsConfig.contagionRadiusPerStack);

        int transferStacks = (int) Math.floor(stacks * ElementalFireNatureReactionsConfig.contagionIntensityRatio);
        if (transferStacks < 1) transferStacks = 1;

        AABB sourceBox = source.getBoundingBox();
        if (sourceBox == null) return;

        AABB area = sourceBox.inflate(radius);
        List<LivingEntity> targets = source.level().getEntitiesOfClass(LivingEntity.class, area);

        List<LivingEntity> infectedTargets = new ArrayList<>();

        Debug.logContagionStart(source, stacks, radius, transferStacks, targets.size());

        for (LivingEntity target : targets) {
            if (target == source) continue;

            if (ElementalFireNatureReactionsConfig.contagionOnlyHostile && !(target instanceof Enemy)) {
                Debug.logContagionSkipNonHostile(target);
                continue;
            }

            target.getPersistentData().putBoolean(NBT_INFECTED, true);

            int wetnessLevel = WetnessHandler.getWetnessLevel(target);
            int wetnessBonus = 0;

            if (wetnessLevel > ElementalFireNatureReactionsConfig.contagionWetnessThreshold) {
                int effectiveWetness = wetnessLevel - ElementalFireNatureReactionsConfig.contagionWetnessThreshold;
                wetnessBonus = (int) Math.floor(effectiveWetness * ElementalFireNatureReactionsConfig.contagionWetnessConversionRatio);
                wetnessBonus = Math.min(wetnessBonus, ElementalFireNatureReactionsConfig.contagionWetnessMaxBonus);
                Debug.logContagionWetnessBonus(target, wetnessLevel, wetnessBonus);
            }

            if (wetnessBonus > 0 && ElementalFireNatureReactionsConfig.contagionConsumesWetness) {
                WetnessHandler.updateWetnessLevel(target, 0);
                Debug.logContagionConsumeWetness(target);
            }

            int finalStacks = transferStacks + wetnessBonus;
            stackSporeEffect(target, finalStacks);
            infectedTargets.add(target);
            Debug.logContagionInfect(target, finalStacks);
        }

        EffectHelper.playSporeContagion(source, infectedTargets, radius);
        Debug.logContagionEnd(source, infectedTargets.size());
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

        Debug.logParasiticDrain(attacker, target, currentWetness, actualDrain, newTargetWetness, attackerWetness, newAttackerWetness, healAmount);

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

        Debug.logToxicBlastStart(attacker, target, firePower, stacks);

        target.removeEffect(ModMobEffects.SPORES.get());

        if (stacks < ElementalFireNatureReactionsConfig.sporeReactionThreshold) {
            int scorchDuration = (int) (ElementalFireNatureReactionsConfig.blastScorchBase * 20);
            int damageStrength = (int) (firePower * ElementalFireNatureReactionsConfig.blastWeakIgniteMult);

            ScorchedHandler.applyScorched(target, damageStrength, scorchDuration, (int) firePower);
            EffectHelper.playSound(level, target, SoundEvents.FIRECHARGE_USE, 1.0f, 1.2f);
            Debug.logToxicBlastWeak(target, scorchDuration, damageStrength);
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

            Debug.logToxicBlastStrong(target, extraStacks, rawBaseDamage, radius, scorchDuration, bonusFromStats);

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
                            Debug.logToxicBlastChain(entity);
                            triggerToxicBlast(level, attacker, entity, firePower, killCredit);
                        }

                        float mitigation = calculateBlastMitigation(entity);
                        float finalDamage = rawBaseDamage * (1.0f - mitigation);

                        entity.hurt(ModDamageTypes.source(level, ModDamageTypes.LAVA_MAGIC, killCredit), finalDamage);

                        ScorchedHandler.applyScorched(entity, (int) firePower, scorchDuration, (int) firePower);
                        affectedCount++;
                    }

                    DebugCommand.sendToxicBlastLog(attacker, target, stacks, radius, affectedCount);
                    Debug.logToxicBlastAffected(attacker, target, affectedCount);
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
        Debug.logBlastMitigation(entity, blastProtLevel, generalProtLevel, mitigation);
        return mitigation;
    }

    private static void triggerWildfireEjection(LivingEntity victim, Entity attacker) {
        double radius = ElementalFireNatureReactionsConfig.wildfireRadius;
        EffectHelper.playWildfireEjection(victim, radius);

        Debug.logWildfireStart(victim, radius);

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
            Debug.logWildfireAffected(enemy, vec);
        }

        DebugCommand.sendWildfireLog(victim, radius, affectedCount);
        Debug.logWildfireEnd(victim, affectedCount);

        setCooldown(victim, NBT_WILDFIRE_COOLDOWN, ElementalFireNatureReactionsConfig.wildfireCooldown);
    }

    private static boolean checkCooldown(LivingEntity entity, String key) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(key)) return true;

        long endTick = data.getLong(key);
        boolean ready = entity.level().getGameTime() >= endTick;
        Debug.logCooldownCheck(entity, key, endTick, ready);
        return ready;
    }

    private static void setCooldown(LivingEntity entity, String key, int durationTicks) {
        entity.getPersistentData().putLong(key, entity.level().getGameTime() + durationTicks);
        Debug.logCooldownSet(entity, key, durationTicks);
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

        Debug.logScorchedBlastTrigger(target, stacks, sourceFirePower);

        triggerToxicBlast(level, killCredit, target, sourceFirePower, killCredit);

        target.getPersistentData().remove(ScorchedHandler.NBT_SCORCHED_TICKS);
        target.getPersistentData().remove(ScorchedHandler.NBT_SCORCHED_STRENGTH);
        target.getPersistentData().remove(ScorchedHandler.NBT_SCORCHED_SOURCE_FIRE_POWER);
        target.clearFire();

        DebugCommand.sendScorchedSporeReactionLog(target, killCredit, stacks);
    }

    // ==================== 调试内部类（所有调试代码集中于此，便于删除） ====================
    private static final class Debug {
        private static void logContagionCheck(LivingEntity entity, int stacks) {
            GlobalDebugLogger.log(entity.level(), "孢子传染",
                    String.format("%s 检查传染：孢子层数 %d", entity.getName().getString(), stacks));
        }

        private static void logReactionStart(LivingEntity attacker, LivingEntity target, ElementType attackType, double naturePower, double firePower) {
            GlobalDebugLogger.log(attacker.level(), "反应触发",
                    String.format("%s 攻击 %s：攻击元素 %s，自然强化 %.1f，赤焰强化 %.1f",
                            attacker.getName().getString(), target.getName().getString(),
                            attackType, naturePower, firePower));
        }

        private static void logParasiteTrigger(LivingEntity attacker, LivingEntity target, double chance, boolean triggered) {
            GlobalDebugLogger.log(attacker.level(), "自然寄生",
                    String.format("%s 对 %s 触发寄生：概率 %.1f%%，结果 %s",
                            attacker.getName().getString(), target.getName().getString(),
                            chance * 100, triggered ? "§a成功" : "§c失败"));
        }

        private static void logDrainConditionFailed(LivingEntity attacker, LivingEntity target, int wetnessLevel, double naturePower) {
            GlobalDebugLogger.log(attacker.level(), "自然吸取",
                    String.format("%s 吸取条件未满足：目标潮湿 %d，自然强化 %.1f",
                            attacker.getName().getString(), wetnessLevel, naturePower));
        }

        private static void logBlastThresholdFailed(LivingEntity attacker, LivingEntity target, double firePower) {
            GlobalDebugLogger.log(attacker.level(), "毒火爆燃",
                    String.format("%s 未达到引爆阈值：赤焰强化 %.1f < %d",
                            attacker.getName().getString(), firePower,
                            ElementalFireNatureReactionsConfig.blastTriggerThreshold));
        }

        private static void logWildfireCheck(LivingEntity target, double naturePower, boolean isNature, boolean hasScorched, boolean cooldownOk, boolean powerOk) {
            GlobalDebugLogger.log(target.level(), "野火喷射",
                    String.format("%s 检查条件：自然强化 %.1f，是自然属性 %s，有灼烧 %s，冷却就绪 %s，强化达标 %s",
                            target.getName().getString(), naturePower,
                            isNature, hasScorched, cooldownOk, powerOk));
        }

        private static void logSporeBlacklist(LivingEntity target, String entityId) {
            GlobalDebugLogger.log(target.level(), "孢子免疫",
                    String.format("%s 在黑名单中，不施加孢子", entityId));
        }

        private static void logSporeImmune(LivingEntity target, double resistance) {
            GlobalDebugLogger.log(target.level(), "孢子免疫",
                    String.format("%s 自然抗性 %.1f ≥ %d，免疫孢子",
                            target.getName().getString(), resistance,
                            ElementalFireNatureReactionsConfig.natureImmunityThreshold));
        }

        private static void logSporeMaxStacks(LivingEntity target, int currentStacks) {
            GlobalDebugLogger.log(target.level(), "孢子叠加",
                    String.format("%s 孢子已达上限 %d，不再叠加", target.getName().getString(), currentStacks));
        }

        private static void logSporeStack(LivingEntity target, int add, int current, int newStacks, int duration, boolean isThunder, boolean isFire) {
            GlobalDebugLogger.log(target.level(), "孢子叠加",
                    String.format("%s 孢子 %d + %d = %d，持续时间 %d 刻，雷属性 %s，火属性 %s",
                            target.getName().getString(), current, add, newStacks, duration,
                            isThunder, isFire));
        }

        private static void logContagionBlocked(LivingEntity source, boolean spreaded, boolean infected) {
            GlobalDebugLogger.log(source.level(), "孢子传染",
                    String.format("%s 已被标记传染/感染 (%s/%s)，跳过", source.getName().getString(), spreaded, infected));
        }

        private static void logContagionStart(LivingEntity source, int stacks, double radius, int transferStacks, int totalTargets) {
            GlobalDebugLogger.log(source.level(), "孢子传染",
                    String.format("%s 触发传染：孢子层数 %d，半径 %.1f，基础传递层数 %d，检测到 %d 个目标",
                            source.getName().getString(), stacks, radius, transferStacks, totalTargets));
        }

        private static void logContagionSkipNonHostile(LivingEntity target) {
            GlobalDebugLogger.log(target.level(), "孢子传染",
                    String.format("跳过非敌对目标 %s", target.getName().getString()));
        }

        private static void logContagionWetnessBonus(LivingEntity target, int wetness, int bonus) {
            GlobalDebugLogger.log(target.level(), "孢子传染",
                    String.format("%s 潮湿层数 %d，额外获得 %d 层孢子", target.getName().getString(), wetness, bonus));
        }

        private static void logContagionConsumeWetness(LivingEntity target) {
            GlobalDebugLogger.log(target.level(), "孢子传染",
                    String.format("%s 潮湿被消耗", target.getName().getString()));
        }

        private static void logContagionInfect(LivingEntity target, int stacks) {
            GlobalDebugLogger.log(target.level(), "孢子传染",
                    String.format("%s 被感染，获得 %d 层孢子", target.getName().getString(), stacks));
        }

        private static void logContagionEnd(LivingEntity source, int infectedCount) {
            GlobalDebugLogger.log(source.level(), "孢子传染",
                    String.format("%s 传染结束，成功感染 %d 个目标", source.getName().getString(), infectedCount));
        }

        private static void logParasiticDrain(LivingEntity attacker, LivingEntity target, int oldTargetWet, int drain, int newTargetWet,
                                               int oldAttackerWet, int newAttackerWet, float heal) {
            GlobalDebugLogger.log(attacker.level(), "自然吸取",
                    String.format("%s 从 %s 吸取 %d 层潮湿：目标 %d → %d，自身 %d → %d，恢复 %.1f 生命",
                            attacker.getName().getString(), target.getName().getString(),
                            drain, oldTargetWet, newTargetWet, oldAttackerWet, newAttackerWet, heal));
        }

        private static void logToxicBlastStart(LivingEntity attacker, LivingEntity target, double firePower, int stacks) {
            GlobalDebugLogger.log(attacker.level(), "毒火爆燃",
                    String.format("%s 引爆 %s：赤焰强化 %.1f，孢子层数 %d",
                            attacker.getName().getString(), target.getName().getString(),
                            firePower, stacks));
        }

        private static void logToxicBlastWeak(LivingEntity target, int scorchDuration, int damageStrength) {
            GlobalDebugLogger.log(target.level(), "毒火爆燃",
                    String.format("%s 弱效引燃：灼烧 %d 刻，强度 %d", target.getName().getString(), scorchDuration, damageStrength));
        }

        private static void logToxicBlastStrong(LivingEntity target, int extraStacks, float damage, double radius, int scorchDuration, double bonus) {
            GlobalDebugLogger.log(target.level(), "毒火爆燃",
                    String.format("%s 强效爆炸：额外层数 %d，基础伤害 %.2f，半径 %.1f，灼烧 %d 刻，属性加成 %.2f",
                            target.getName().getString(), extraStacks, damage, radius, scorchDuration, bonus));
        }

        private static void logToxicBlastChain(LivingEntity entity) {
            GlobalDebugLogger.log(entity.level(), "毒火爆燃",
                    String.format("%s 触发连锁爆炸", entity.getName().getString()));
        }

        private static void logToxicBlastAffected(LivingEntity attacker, LivingEntity target, int count) {
            GlobalDebugLogger.log(attacker.level(), "毒火爆燃",
                    String.format("%s 的爆炸影响 %d 个实体", target.getName().getString(), count));
        }

        private static void logBlastMitigation(LivingEntity entity, int blastProt, int genProt, float mitigation) {
            GlobalDebugLogger.log(entity.level(), "爆炸减伤",
                    String.format("%s 爆炸保护 %d，通用保护 %d，最终减伤 %.1f%%",
                            entity.getName().getString(), blastProt, genProt, mitigation * 100));
        }

        private static void logWildfireStart(LivingEntity victim, double radius) {
            GlobalDebugLogger.log(victim.level(), "野火喷射",
                    String.format("%s 触发野火喷射，半径 %.1f", victim.getName().getString(), radius));
        }

        private static void logWildfireAffected(LivingEntity enemy, Vec3 knockback) {
            GlobalDebugLogger.log(enemy.level(), "野火喷射",
                    String.format("%s 被击退：%.2f %.2f %.2f",
                            enemy.getName().getString(), knockback.x, knockback.y, knockback.z));
        }

        private static void logWildfireEnd(LivingEntity victim, int affectedCount) {
            GlobalDebugLogger.log(victim.level(), "野火喷射",
                    String.format("%s 野火结束，影响 %d 个敌人", victim.getName().getString(), affectedCount));
        }

        private static void logCooldownCheck(LivingEntity entity, String key, long endTick, boolean ready) {
            GlobalDebugLogger.log(entity.level(), "冷却检查",
                    String.format("%s 冷却 %s：结束刻 %d，当前 %d，就绪 %s",
                            entity.getName().getString(), key, endTick, entity.level().getGameTime(), ready));
        }

        private static void logCooldownSet(LivingEntity entity, String key, int duration) {
            GlobalDebugLogger.log(entity.level(), "冷却设置",
                    String.format("%s 设置冷却 %s：%d 刻", entity.getName().getString(), key, duration));
        }

        private static void logScorchedBlastTrigger(LivingEntity target, int stacks, int firePower) {
            GlobalDebugLogger.log(target.level(), "灼烧孢子反应",
                    String.format("%s 灼烧状态触发孢子爆炸：孢子层数 %d，原赤焰点数 %d",
                            target.getName().getString(), stacks, firePower));
        }
    }
}