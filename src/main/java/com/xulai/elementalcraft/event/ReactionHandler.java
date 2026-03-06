package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalReactionConfig;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.util.EffectHelper;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import com.xulai.elementalcraft.event.ScorchedHandler;
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
import java.util.Objects;
import java.util.Random;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class ReactionHandler {

    private static final Random RANDOM = new Random();

    private static final String NBT_DRAIN_COOLDOWN = "ec_drain_cd";

    private static final String NBT_WILDFIRE_COOLDOWN = "ec_wildfire_cd";

    private static final String NBT_SPREADED = "ec_spreaded";

    private static final String NBT_INFECTED = "ec_infected";

    private static final String NBT_WETNESS = "EC_WetnessLevel";
    
    private static final String NBT_SPORE_APPLIER = "ec_spore_applier";
    private static final String NBT_SPORE_APPLIER_TIMESTAMP = "ec_spore_applier_ts";

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        if (entity.tickCount % ElementalReactionConfig.contagionCheckInterval != 0) return;

        if (ModMobEffects.SPORES.isPresent() && entity.hasEffect(Objects.requireNonNull(ModMobEffects.SPORES.get()))) {
            MobEffectInstance sporeEffect = entity.getEffect(Objects.requireNonNull(ModMobEffects.SPORES.get()));
            if (sporeEffect == null) return;

            int amplifier = sporeEffect.getAmplifier();
            int stacks = amplifier + 1;

            if (stacks >= ElementalReactionConfig.sporeReactionThreshold) {
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
            if (naturePower >= ElementalReactionConfig.natureParasiteBaseThreshold) {
                double chance = 0.0;
                double scalingStep = ElementalReactionConfig.natureParasiteScalingStep;

                if (naturePower < scalingStep) {
                    chance = ElementalReactionConfig.natureParasiteBaseChance;
                } else {
                    int steps = (int) ((naturePower - scalingStep) / scalingStep);
                    chance = ElementalReactionConfig.natureParasiteBaseChance + (steps * ElementalReactionConfig.natureParasiteScalingChance);
                    chance += ElementalReactionConfig.natureParasiteScalingChance;
                }

                CompoundTag attackerData = attacker.getPersistentData();
                if (attackerData.getInt(NBT_WETNESS) > 0) {
                    chance += attackerData.getInt(NBT_WETNESS) * ElementalReactionConfig.natureParasiteWetnessBonus;
                }

                if (RANDOM.nextDouble() < chance) {
                    stackSporeEffect(target, ElementalReactionConfig.natureParasiteAmount);
                    EffectHelper.playSporeAmbient(target);
                }
            }

            if (checkCooldown(attacker, NBT_DRAIN_COOLDOWN)) {
                CompoundTag targetData = target.getPersistentData();
                int wetnessLevel = targetData.getInt(NBT_WETNESS);

                if (wetnessLevel > 0 && naturePower >= ElementalReactionConfig.natureSiphonThreshold) {
                    triggerParasiticDrain(attacker, target, wetnessLevel, naturePower);
                }
            }
        } else if (attackType == ElementType.FIRE) {
            if (ModMobEffects.SPORES.isPresent() && target.hasEffect(Objects.requireNonNull(ModMobEffects.SPORES.get()))
                    && !event.getSource().is(Objects.requireNonNull(DamageTypeTags.IS_EXPLOSION))) {

                if (firePower >= ElementalReactionConfig.blastTriggerThreshold) {
                    triggerToxicBlast(level, attacker, target, firePower);
                }
            }

            double victimNaturePower = ElementUtils.getDisplayEnhancement(target, ElementType.NATURE);
            boolean isNatureTarget = ElementUtils.getConsistentAttackElement(target) == ElementType.NATURE;
            boolean hasScorched = target.getPersistentData().contains(ScorchedHandler.NBT_SCORCHED_TICKS);
            boolean cooldownOk = checkCooldown(target, NBT_WILDFIRE_COOLDOWN);
            boolean powerOk = victimNaturePower >= ElementalReactionConfig.wildfireTriggerThreshold;

            if (isNatureTarget && powerOk && hasScorched && cooldownOk) {
                triggerWildfireEjection(target, attacker);
            }
        }
    }

    public static void stackSporeEffect(LivingEntity target, int layersToAdd) {
        stackSporeEffect(target, layersToAdd, null);
    }
    
    public static void stackSporeEffect(LivingEntity target, int layersToAdd, LivingEntity applier) {
        if (!ModMobEffects.SPORES.isPresent()) return;

        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).toString();
        if (ElementalReactionConfig.cachedSporeBlacklist != null && ElementalReactionConfig.cachedSporeBlacklist.contains(entityId)) {
            return;
        }

        double natureResistance = ElementUtils.getDisplayResistance(target, ElementType.NATURE);
        if (natureResistance >= ElementalReactionConfig.natureImmunityThreshold) {
            return;
        }

        MobEffectInstance currentEffect = target.getEffect(Objects.requireNonNull(ModMobEffects.SPORES.get()));
        int currentAmp = (currentEffect != null) ? currentEffect.getAmplifier() : -1;
        int currentStacks = currentAmp + 1;

        int maxStacks = ElementalReactionConfig.sporeMaxStacks;

        if (currentStacks >= maxStacks) {
            return;
        }

        int newStacks = Math.min(maxStacks, currentStacks + layersToAdd);
        int durationTicks = newStacks * ElementalReactionConfig.sporeDurationPerStack * 20;

        boolean isThunder = ElementUtils.getDisplayEnhancement(target, ElementType.THUNDER) > 0 ||
                ElementUtils.getDisplayResistance(target, ElementType.THUNDER) > 0;
        boolean isFire = ElementUtils.getDisplayEnhancement(target, ElementType.FIRE) > 0 ||
                ElementUtils.getDisplayResistance(target, ElementType.FIRE) > 0;

        if (isThunder) {
            durationTicks = (int) (durationTicks * ElementalReactionConfig.sporeThunderMultiplier);
        }
        if (isFire) {
            durationTicks = (int) (durationTicks * ElementalReactionConfig.sporeFireDurationReduction);
        }

        if (newStacks > 0) {
            target.addEffect(new MobEffectInstance(Objects.requireNonNull(ModMobEffects.SPORES.get()), durationTicks, newStacks - 1));
            
            // 记录孢子施加者
            if (applier != null) {
                CompoundTag targetData = target.getPersistentData();
                targetData.putString(NBT_SPORE_APPLIER, applier.getStringUUID());
                targetData.putLong(NBT_SPORE_APPLIER_TIMESTAMP, target.level().getGameTime());
            }
            
            // 检查目标是否有灼烧状态，如果有则触发毒火爆燃
            if (target.getPersistentData().contains(ScorchedHandler.NBT_SCORCHED_TICKS)) {
                triggerToxicBlastFromScorched(target, newStacks);
            }
        }
    }

    private static void processContagion(LivingEntity source, int stacks) {
        CompoundTag data = source.getPersistentData();

        boolean isSpreaded = data.getBoolean(NBT_SPREADED);
        boolean isInfected = data.getBoolean(NBT_INFECTED);

        if (isSpreaded || isInfected) return;

        data.putBoolean(NBT_SPREADED, true);

        double radius = ElementalReactionConfig.contagionBaseRadius + ((stacks - ElementalReactionConfig.sporeReactionThreshold) * ElementalReactionConfig.contagionRadiusPerStack);

        int transferStacks = (int) Math.floor(stacks * ElementalReactionConfig.contagionIntensityRatio);
        if (transferStacks < 1) transferStacks = 1;

        AABB sourceBox = source.getBoundingBox();
        if (sourceBox == null) return;
        
        AABB area = sourceBox.inflate(radius);
        List<LivingEntity> targets = source.level().getEntitiesOfClass(LivingEntity.class, area);
        
        List<LivingEntity> infectedTargets = new ArrayList<>();

        for (LivingEntity target : targets) {
            if (target == source) continue;

            if (ElementalReactionConfig.contagionOnlyHostile && !(target instanceof Enemy)) {
                continue;
            }

            target.getPersistentData().putBoolean(NBT_INFECTED, true);

            int wetnessLevel = target.getPersistentData().getInt(NBT_WETNESS);
            int wetnessBonus = 0;

            if (wetnessLevel > ElementalReactionConfig.contagionWetnessThreshold) {
                int effectiveWetness = wetnessLevel - ElementalReactionConfig.contagionWetnessThreshold;
                wetnessBonus = (int) Math.floor(effectiveWetness * ElementalReactionConfig.contagionWetnessConversionRatio);
                wetnessBonus = Math.min(wetnessBonus, ElementalReactionConfig.contagionWetnessMaxBonus);
            }

            if (wetnessBonus > 0 && ElementalReactionConfig.contagionConsumesWetness) {
                target.getPersistentData().remove(NBT_WETNESS);
                if (ModMobEffects.WETNESS.isPresent() && target.hasEffect(Objects.requireNonNull(ModMobEffects.WETNESS.get()))) {
                    target.removeEffect(Objects.requireNonNull(ModMobEffects.WETNESS.get()));
                }
            }

            stackSporeEffect(target, transferStacks + wetnessBonus);
            infectedTargets.add(target);
        }

        EffectHelper.playSporeContagion(source, infectedTargets, radius);
    }

    private static void triggerParasiticDrain(LivingEntity attacker, LivingEntity target, int currentWetness, double naturePower) {
        double step = ElementalReactionConfig.natureDrainPowerStep;
        
        int baseDrain = ElementalReactionConfig.natureDrainAmount;
        int bonusDrain = (int) Math.floor(naturePower / step);
        int drainCapacity = baseDrain + bonusDrain;
        
        if (drainCapacity < 1) drainCapacity = 1;

        int actualDrain = Math.min(currentWetness, drainCapacity);
        if (actualDrain <= 0) return;

        int newTargetWetness = currentWetness - actualDrain;
        if (newTargetWetness <= 0) {
            target.getPersistentData().remove(NBT_WETNESS);
            target.removeEffect(Objects.requireNonNull(ModMobEffects.WETNESS.get()));
        } else {
            target.getPersistentData().putInt(NBT_WETNESS, newTargetWetness);
        }

        int attackerWetness = attacker.getPersistentData().getInt(NBT_WETNESS);
        attacker.getPersistentData().putInt(NBT_WETNESS,
                Math.min(ElementalReactionConfig.wetnessMaxLevel, attackerWetness + actualDrain));

        stackSporeEffect(target, actualDrain);

        float healAmount = (float) (actualDrain * ElementalReactionConfig.natureSiphonHeal);
        attacker.heal(healAmount);

        DebugCommand.sendNatureSiphonLog(attacker, target, actualDrain, healAmount);

        setCooldown(attacker, NBT_DRAIN_COOLDOWN, ElementalReactionConfig.natureDrainCooldown);

        EffectHelper.playDrainEffect(attacker, target);
        EffectHelper.playSound(target.level(), attacker, Objects.requireNonNull(SoundEvents.ZOMBIE_VILLAGER_CONVERTED), 0.5f, 1.5f);
    }

    private static void triggerToxicBlast(Level level, LivingEntity attacker, LivingEntity target, double firePower) {
        triggerToxicBlast(level, attacker, target, firePower, attacker);
    }
    
    private static void triggerToxicBlast(Level level, LivingEntity attacker, LivingEntity target, double firePower, LivingEntity killCredit) {
        MobEffectInstance sporeEffect = target.getEffect(Objects.requireNonNull(ModMobEffects.SPORES.get()));
        int amplifier = (sporeEffect != null) ? sporeEffect.getAmplifier() : -1;
        int stacks = amplifier + 1;

        target.removeEffect(Objects.requireNonNull(ModMobEffects.SPORES.get()));

        if (stacks < ElementalReactionConfig.sporeReactionThreshold) {
            int scorchDuration = (int) (ElementalReactionConfig.blastScorchBase * 20);
            int damageStrength = (int) (firePower * ElementalReactionConfig.blastWeakIgniteMult);

            ScorchedHandler.applyScorched(target, damageStrength, scorchDuration);
            EffectHelper.playSound(level, target, Objects.requireNonNull(SoundEvents.FIRECHARGE_USE), 1.0f, 1.2f);
        } else {
            int extraStacks = stacks - ElementalReactionConfig.sporeReactionThreshold;

            double fireStep = ElementalReactionConfig.blastDmgStep;
            double dmgPerStep = ElementalReactionConfig.blastDmgAmount;
            double bonusFromStats = 0;

            if (fireStep > 0) {
                bonusFromStats = (firePower / fireStep) * dmgPerStep;
            }

            float rawBaseDamage = (float) (ElementalReactionConfig.blastBaseDamage 
                    + (extraStacks * ElementalReactionConfig.blastGrowthDamage)
                    + bonusFromStats);

            double radius = ElementalReactionConfig.blastBaseRange + (extraStacks * ElementalReactionConfig.blastGrowthRange);
            int scorchDuration = (int) ((ElementalReactionConfig.blastBaseScorchTime + (extraStacks * ElementalReactionConfig.blastGrowthScorchTime)) * 20);

            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.BLOCKS, 4.0F, (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F);

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(Objects.requireNonNull(ParticleTypes.EXPLOSION_EMITTER),
                        target.getX(), target.getY() + 1.0, target.getZ(), 1, 0, 0, 0, 0);

                serverLevel.sendParticles(Objects.requireNonNull(ParticleTypes.FLAME),
                        target.getX(), target.getY() + 0.5, target.getZ(),
                        50, 1.5, 1.5, 1.5, 0.2);

                serverLevel.sendParticles(Objects.requireNonNull(ParticleTypes.LAVA),
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
                        if (entity instanceof OwnableEntity ownable && Objects.equals(ownable.getOwnerUUID(), attacker.getUUID())) {
                            isPet = true;
                        } else if (entity instanceof AbstractHorse horse && Objects.equals(horse.getOwnerUUID(), attacker.getUUID())) {
                            isPet = true;
                        }
                        if (isPet) continue;

                        entity.invulnerableTime = 0;

                        if (ElementalReactionConfig.blastChainReaction 
                                && ModMobEffects.SPORES.isPresent() 
                                && entity.hasEffect(Objects.requireNonNull(ModMobEffects.SPORES.get()))) {
                            triggerToxicBlast(level, attacker, entity, firePower, killCredit);
                        }

                        float mitigation = calculateBlastMitigation(entity);
                        float finalDamage = rawBaseDamage * (1.0f - mitigation);

                        // 使用自定义伤害源来追踪击杀者
                        entity.hurt(Objects.requireNonNull(ModDamageTypes.source(level, ModDamageTypes.LAVA_MAGIC, killCredit)), finalDamage);

                        ScorchedHandler.applyScorched(entity, (int) firePower, scorchDuration);
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

        double maxBlastCap = ElementalReactionConfig.blastMaxBlastProtCap;
        double maxGeneralCap = ElementalReactionConfig.blastMaxGeneralProtCap;

        double blastFactor = maxBlastCap / ElementalReactionConfig.enchantmentCalculationDenominator;
        double generalFactor = maxGeneralCap / ElementalReactionConfig.enchantmentCalculationDenominator;

        double calculatedBlastRed = blastProtLevel * blastFactor;
        double calculatedGeneralRed = generalProtLevel * generalFactor;

        double actualBlastRed = Math.min(calculatedBlastRed, maxBlastCap);
        double actualGeneralRed = Math.min(calculatedGeneralRed, maxGeneralCap);

        return (float) Math.min(actualBlastRed + actualGeneralRed, 1.0);
    }

    private static void triggerWildfireEjection(LivingEntity victim, Entity attacker) {
        double radius = ElementalReactionConfig.wildfireRadius;
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

            Vec3 vec = enemyPos.subtract(victimPos).normalize().scale(ElementalReactionConfig.wildfireKnockback);
            enemy.push(vec.x, ElementalReactionConfig.wildfireVerticalKnockback, vec.z);
            enemy.hurtMarked = true;

            if (ElementalReactionConfig.wildfireClearBurning) {
                enemy.clearFire();
                
                if (enemy.getPersistentData().contains(ScorchedHandler.NBT_SCORCHED_TICKS)) {
                    enemy.getPersistentData().remove(ScorchedHandler.NBT_SCORCHED_TICKS);
                    enemy.getPersistentData().remove(ScorchedHandler.NBT_SCORCHED_STRENGTH);
                }
            }

            stackSporeEffect(enemy, ElementalReactionConfig.wildfireSporeAmount);
            affectedCount++;
        }

        DebugCommand.sendWildfireLog(victim, radius, affectedCount);

        setCooldown(victim, NBT_WILDFIRE_COOLDOWN, ElementalReactionConfig.wildfireCooldown);
    }

    private static boolean checkCooldown(LivingEntity entity, String key) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(Objects.requireNonNull(key))) return true;

        long endTick = data.getLong(Objects.requireNonNull(key));
        return entity.level().getGameTime() >= endTick;
    }

    private static void setCooldown(LivingEntity entity, String key, int durationTicks) {
        entity.getPersistentData().putLong(Objects.requireNonNull(key), entity.level().getGameTime() + durationTicks);
    }

    private static int getTotalEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantment ench, LivingEntity entity) {
        int total = 0;
        for (ItemStack stack : entity.getArmorSlots()) {
            total += stack.getEnchantmentLevel(ench);
        }
        return total;
    }
    
    public static void triggerToxicBlastFromScorched(LivingEntity target, int stacks) {
        if (target.level().isClientSide) return;
        
        Level level = target.level();
        
        // 获取孢子施加者
        LivingEntity applier = null;
        CompoundTag targetData = target.getPersistentData();
        if (targetData.contains(NBT_SPORE_APPLIER)) {
            String applierUUID = targetData.getString(NBT_SPORE_APPLIER);
            long timestamp = targetData.getLong(NBT_SPORE_APPLIER_TIMESTAMP);
            
            // 检查时间戳是否在有效范围内（30秒内）
            if (target.level().getGameTime() - timestamp < 600) { // 30秒 = 600 ticks
                for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(50))) {
                    if (entity.getStringUUID().equals(applierUUID)) {
                        applier = entity;
                        break;
                    }
                }
            }
        }
        
        // 如果没有找到施加者，使用目标自己作为攻击者（但不会造成连锁反应）
        if (applier == null) {
            applier = target;
        }
        
        // 使用基础火属性强度（如果没有火属性，使用默认值）
        double firePower = ElementUtils.getDisplayEnhancement(applier, ElementType.FIRE);
        if (firePower <= 0) {
            firePower = 20.0; // 默认火属性强度
        }
        
        // 触发毒火爆燃，使用孢子施加者作为击杀判定
        triggerToxicBlast(level, applier, target, firePower, applier);
        
        // 清除孢子效果（已经在triggerToxicBlast中清除）
        // 清除灼烧效果
        target.getPersistentData().remove(ScorchedHandler.NBT_SCORCHED_TICKS);
        target.getPersistentData().remove(ScorchedHandler.NBT_SCORCHED_STRENGTH);
        target.clearFire();
        
        DebugCommand.sendScorchedSporeReactionLog(target, applier, stacks);
    }
}
