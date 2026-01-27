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

/**
 * 元素反应系统的核心事件处理器。
 * <p>
 * Core event handler for the Elemental Reaction System.
 *
 * @author ElementalCraft Dev Team
 * @since 1.0.0
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class ReactionHandler {

    /**
     * 全局随机数实例，用于概率判定、粒子生成等随机逻辑
     * <p>
     * Global random instance for probability judgment, particle generation and other random logic
     */
    private static final Random RANDOM = new Random();

    /**
     * 寄生吸取冷却的NBT存储键
     * <p>
     * NBT key for Parasitic Drain cooldown
     */
    private static final String NBT_DRAIN_COOLDOWN = "ec_drain_cd";
    /**
     * 野火喷射冷却的NBT存储键
     * <p>
     * NBT key for Wildfire Ejection cooldown
     */
    private static final String NBT_WILDFIRE_COOLDOWN = "ec_wildfire_cd";
    /**
     * 孢子已触发传染的NBT标记键
     * <p>
     * NBT key for spore spreaded mark
     */
    private static final String NBT_SPREADED = "ec_spreaded";
    /**
     * 实体已被孢子感染的NBT标记键
     * <p>
     * NBT key for entity infected mark
     */
    private static final String NBT_INFECTED = "ec_infected";
    /**
     * 实体湿润度等级的NBT存储键
     * <p>
     * NBT key for entity wetness level
     */
    private static final String NBT_WETNESS = "EC_WetnessLevel";

    /**
     * 生物实体Tick周期事件监听器，处理孢子传染的周期性检测逻辑。
     * <p>
     * Living entity tick event listener, handles periodic detection logic for spore contagion.
     *
     * @param event 生物Tick事件对象 / Living tick event instance
     */
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

    /**
     * 生物实体受伤害事件监听器，处理所有元素攻击相关的反应逻辑，包括孢子叠加、寄生吸取、毒火爆燃、野火喷射。
     * <p>
     * Living entity damage event listener, handles all reaction logic related to elemental attacks,
     * including spore stacking, parasitic drain, toxic blast and wildfire ejection.
     *
     * @param event 生物受伤害事件对象 / Living damage event instance
     */
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
            // 判定目标是否为自然属性实体，且自然强化点数达标、冷却结束时触发野火喷射
            if (ElementUtils.getConsistentAttackElement(target) == ElementType.NATURE
                    && victimNaturePower >= ElementalReactionConfig.wildfireTriggerThreshold
                    && checkCooldown(target, NBT_WILDFIRE_COOLDOWN)) {

                target.clearFire();
                CompoundTag data = target.getPersistentData();
                data.remove(ScorchedHandler.NBT_SCORCHED_TICKS);
                data.remove(ScorchedHandler.NBT_SCORCHED_STRENGTH);

                triggerWildfireEjection(target, attacker);
            }
        }
    }

    /**
     * 易燃孢子效果的叠加与刷新逻辑，处理层数限制、元素抗性/增强的时长修正。
     * <p>
     * Flammable Spores stacking and refreshing logic, handles stack limit and
     * duration correction by elemental resistance/enhancement.
     *
     * @param target      孢子效果目标实体 / Target entity for spore effect
     * @param layersToAdd 要叠加的孢子层数 / Spore layers to add
     */
    private static void stackSporeEffect(LivingEntity target, int layersToAdd) {
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
            currentStacks = maxStacks;
            layersToAdd = 0;
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
        }
    }

    /**
     * 孢子传染的核心执行逻辑，处理范围检测、目标过滤、湿润度加成与孢子层数传递。
     * <p>
     * Core execution logic for spore contagion, handles range detection, target filtering,
     * wetness bonus and spore layer transfer.
     *
     * @param source 孢子传染的源实体 / Source entity for spore contagion
     * @param stacks 源实体的孢子层数 / Spore stacks of source entity
     */
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

    /**
     * 寄生吸取的核心执行逻辑，处理湿润度吸取、转移、生命恢复与孢子叠加。
     * <p>
     * Core execution logic for Parasitic Drain, handles wetness drain, transfer,
     * health recovery and spore stacking.
     *
     * @param attacker     发动寄生吸取的攻击者 / Attacker that triggers Parasitic Drain
     * @param target       寄生吸取的目标 / Target of Parasitic Drain
     * @param currentWetness 目标当前的湿润度等级 / Current wetness level of target
     * @param naturePower  攻击者的自然元素强化点数 / Nature element enhancement points of attacker
     */
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

    /**
     * 毒火爆燃的核心执行逻辑，分低层数弱灼烧和高层数范围爆炸两种分支处理，支持连锁反应。
     * <p>
     * Core execution logic for Toxic Blast, handles two branches: weak scorch for low stacks
     * and area explosion for high stacks, supports chain reaction.
     *
     * @param level       游戏世界等级 / Game world level
     * @param attacker    发动毒火爆燃的攻击者 / Attacker that triggers Toxic Blast
     * @param target      毒火爆燃的触发目标 / Trigger target of Toxic Blast
     * @param firePower   攻击者的火焰元素强化点数 / Fire element enhancement points of attacker
     */
    private static void triggerToxicBlast(Level level, LivingEntity attacker, LivingEntity target, double firePower) {
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
                            triggerToxicBlast(level, attacker, entity, firePower);
                        }

                        float mitigation = calculateBlastMitigation(entity);
                        float finalDamage = rawBaseDamage * (1.0f - mitigation);

                        entity.hurt(Objects.requireNonNull(ModDamageTypes.source(level, ModDamageTypes.LAVA_MAGIC)), finalDamage);

                        ScorchedHandler.applyScorched(entity, (int) firePower, scorchDuration);
                        affectedCount++;
                    }

                    DebugCommand.sendToxicBlastLog(attacker, target, stacks, radius, affectedCount);
                });
            }
        }
    }

    /**
     * 计算实体对毒火爆燃的伤害减免比例，综合爆炸保护和通用保护附魔的效果，有上限限制。
     * <p>
     * Calculates the damage mitigation ratio of entity to Toxic Blast, integrates the effects
     * of Blast Protection and All Damage Protection enchantments with upper limit.
     *
     * @param entity 要计算减免的实体 / Entity to calculate mitigation for
     * @return 0~1之间的伤害减免比例 / Damage mitigation ratio between 0 and 1
     */
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

    /**
     * 野火喷射的核心执行逻辑，处理范围敌对检测、击退效果、孢子叠加与特效播放。
     * <p>
     * Core execution logic for Wildfire Ejection, handles area hostile detection,
     * knockback effect, spore stacking and effect playback.
     *
     * @param victim   发动野火喷射的受害实体（自然属性） / Victim entity (nature type) that triggers Wildfire Ejection
     * @param attacker 触发野火喷射的攻击者 / Attacker that triggers Wildfire Ejection
     */
    private static void triggerWildfireEjection(LivingEntity victim, Entity attacker) {
        if (victim.level() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().execute(() -> {
                if (victim.isAlive()) {
                    victim.clearFire();
                    victim.getPersistentData().remove(ScorchedHandler.NBT_SCORCHED_TICKS);
                }
            });
        }

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

            stackSporeEffect(enemy, ElementalReactionConfig.wildfireSporeAmount);
            affectedCount++;
        }

        DebugCommand.sendWildfireLog(victim, radius, affectedCount);

        setCooldown(victim, NBT_WILDFIRE_COOLDOWN, ElementalReactionConfig.wildfireCooldown);
    }

    /**
     * 检查实体指定键的冷却是否结束，基于游戏刻进行判定。
     * <p>
     * Checks if the cooldown of the specified key for the entity has ended,
     * judged based on game ticks.
     *
     * @param entity 要检查的实体 / Entity to check
     * @param key    冷却的NBT键 / NBT key of cooldown
     * @return 冷却结束返回true，否则返回false / True if cooldown ended, false otherwise
     */
    private static boolean checkCooldown(LivingEntity entity, String key) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(Objects.requireNonNull(key))) return true;

        long endTick = data.getLong(Objects.requireNonNull(key));
        return entity.level().getGameTime() >= endTick;
    }

    /**
     * 为实体指定键设置冷却时间，基于游戏刻进行计算。
     * <p>
     * Sets the cooldown time for the specified key of the entity,
     * calculated based on game ticks.
     *
     * @param entity        要设置冷却的实体 / Entity to set cooldown
     * @param key           冷却的NBT键 / NBT key of cooldown
     * @param durationTicks 冷却持续的游戏刻数 / Cooldown duration in game ticks
     */
    private static void setCooldown(LivingEntity entity, String key, int durationTicks) {
        entity.getPersistentData().putLong(Objects.requireNonNull(key), entity.level().getGameTime() + durationTicks);
    }

    /**
     * 计算实体所有护甲槽位指定附魔的总等级。
     * <p>
     * Calculates the total level of the specified enchantment on all armor slots of the entity.
     *
     * @param ench   要计算的附魔 / Enchantment to calculate
     * @param entity 要计算的实体 / Entity to calculate
     * @return 附魔的总等级 / Total level of the enchantment
     */
    private static int getTotalEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantment ench, LivingEntity entity) {
        int total = 0;
        for (ItemStack stack : entity.getArmorSlots()) {
            total += stack.getEnchantmentLevel(ench);
        }
        return total;
    }
}