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
 * 负责处理所有与元素属性相关的交互反应，包括易燃孢子的叠加、传染、寄生吸取、
 * 毒火爆燃以及野火喷射等逻辑。所有反应均基于配置参数动态调整，支持热重载。
 * <p>
 * Core event handler for the Elemental Reaction System.
 * Responsible for handling all interactions related to elemental attributes,
 * including spore stacking, contagion, parasitic drain, toxic blast, and wildfire ejection.
 * All reactions are dynamically adjustable based on configuration and support hot-reloading.
 *
 * @author ElementalCraft Dev Team
 * @since 1.0.0
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class ReactionHandler {

    /**
     * 全局随机数实例，用于概率判定、粒子生成等随机逻辑。
     * <p>
     * Global random instance for probability checks, particle generation and other random logic.
     */
    private static final Random RANDOM = new Random();

    /**
     * 寄生吸取冷却的 NBT 存储键。
     * <p>
     * NBT key for Parasitic Drain cooldown.
     */
    private static final String NBT_DRAIN_COOLDOWN = "ec_drain_cd";

    /**
     * 野火喷射冷却的 NBT 存储键。
     * <p>
     * NBT key for Wildfire Ejection cooldown.
     */
    private static final String NBT_WILDFIRE_COOLDOWN = "ec_wildfire_cd";

    /**
     * 孢子已触发传染的 NBT 标记键。
     * <p>
     * NBT key indicating that spore contagion has already been triggered from this source.
     */
    private static final String NBT_SPREADED = "ec_spreaded";

    /**
     * 实体已被孢子感染的 NBT 标记键，用于防止同一目标在单次传染中被重复处理。
     * <p>
     * NBT key indicating that an entity has already been infected during current contagion cycle.
     */
    private static final String NBT_INFECTED = "ec_infected";

    /**
     * 实体湿润度等级的 NBT 存储键。
     * <p>
     * NBT key for entity wetness level.
     */
    private static final String NBT_WETNESS = "EC_WetnessLevel";

    /**
     * 生物实体 Tick 周期事件监听器，处理孢子传染的周期性检测逻辑。
     * 根据配置的检测间隔，检查生物是否达到触发传染所需的孢子层数。
     * <p>
     * Living entity tick event listener that handles periodic detection of spore contagion.
     * Checks whether the entity has reached the required spore stacks to trigger contagion,
     * based on the configured check interval.
     *
     * @param event 生物 Tick 事件对象 / Living tick event instance
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
     * 生物实体受伤害事件监听器，处理所有与元素攻击相关的反应逻辑。
     * 根据攻击者的元素类型分别处理：
     * - 自然属性：孢子叠加与寄生吸取
     * - 赤焰属性：毒火爆燃（引爆孢子）以及目标自然属性且处于灼烧状态时的野火喷射反击
     * <p>
     * Living entity damage event listener that handles all reaction logic related to elemental attacks.
     * Depending on the attacker's element:
     * - Nature: spore stacking and parasitic drain
     * - Fire: toxic blast (detonate spores) and, if the target is a nature entity and currently scorched,
     *         triggers wildfire ejection as a counterattack.
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
            // 自然属性攻击：孢子叠加与寄生吸取
            // Nature attribute attack: spore stacking and parasitic drain
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
            // 赤焰属性攻击：毒火爆燃（引爆孢子）
            // Fire attribute attack: Toxic Blast (detonate spores)
            if (ModMobEffects.SPORES.isPresent() && target.hasEffect(Objects.requireNonNull(ModMobEffects.SPORES.get()))
                    && !event.getSource().is(Objects.requireNonNull(DamageTypeTags.IS_EXPLOSION))) {

                if (firePower >= ElementalReactionConfig.blastTriggerThreshold) {
                    triggerToxicBlast(level, attacker, target, firePower);
                }
            }

            // 自然属性目标反击：野火喷射（仅当目标处于灼烧状态时触发）
            // Nature target counterattack: Wildfire Ejection (only triggered when target is scorched)
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

    /**
     * 易燃孢子效果的叠加与刷新逻辑。
     * 处理层数上限、免疫检查、元素抗性/增强对持续时间的影响。
     * 当目标已经达到最大层数时，无法再次施加或刷新效果，必须等待效果自然结束后才能重新施加。
     * <p>
     * Flammable Spores stacking and refreshing logic.
     * Handles stack limits, immunity checks, and duration adjustments based on elemental resistance/enhancement.
     * If the target has already reached maximum stacks, no further application or refresh is allowed;
     * the effect must expire completely before new stacks can be applied.
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

        // 如果已经达到最大层数，则无法再次施加或刷新效果
        // If already at maximum stacks, cannot apply or refresh
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
        }
    }

    /**
     * 孢子传染的核心执行逻辑。
     * 在源实体周围指定半径内搜索其他生物，根据配置传递部分孢子层数，
     * 并根据目标身上的潮湿层数提供额外孢子层数加成。传染后会消耗目标的潮湿状态（如果配置允许）。
     * <p>
     * Core execution logic for spore contagion.
     * Scans for other entities within a radius around the source, transfers a portion of the spore stacks
     * based on configuration, and grants bonus stacks depending on the target's wetness level.
     * Optionally consumes the target's wetness if configured.
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
     * 寄生吸取的核心执行逻辑。
     * 攻击者从目标身上吸取潮湿层数，转移部分到自身，并根据吸取的层数恢复生命值，
     * 同时对目标施加相应层数的孢子效果。
     * <p>
     * Core execution logic for Parasitic Drain.
     * The attacker drains wetness levels from the target, transfers part to itself,
     * restores health based on drained levels, and applies corresponding spore stacks to the target.
     *
     * @param attacker       发动寄生吸取的攻击者 / Attacker that triggers Parasitic Drain
     * @param target         寄生吸取的目标 / Target of Parasitic Drain
     * @param currentWetness 目标当前的湿润度等级 / Current wetness level of target
     * @param naturePower    攻击者的自然元素强化点数 / Nature element enhancement points of attacker
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
     * 毒火爆燃的核心执行逻辑。
     * 根据目标身上的孢子层数分为两种分支：
     * - 低层数（小于反应阈值）：施加弱灼烧效果。
     * - 高层数（达到或超过阈值）：引发范围爆炸，伤害周围生物并施加灼烧，支持连锁反应。
     * <p>
     * Core execution logic for Toxic Blast.
     * Branches based on the target's spore stacks:
     * - Low stacks (below reaction threshold): applies a weak scorch effect.
     * - High stacks (at or above threshold): triggers an area explosion, damages nearby entities,
     *   applies scorch, and supports chain reaction.
     *
     * @param level      游戏世界等级 / Game world level
     * @param attacker   发动毒火爆燃的攻击者 / Attacker that triggers Toxic Blast
     * @param target     毒火爆燃的触发目标 / Trigger target of Toxic Blast
     * @param firePower  攻击者的火焰元素强化点数 / Fire element enhancement points of attacker
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
     * 计算实体对毒火爆燃的伤害减免比例。
     * 综合考虑爆炸保护和通用保护附魔的效果，并应用配置的上限限制。
     * <p>
     * Calculates the damage mitigation ratio of an entity against Toxic Blast.
     * Combines the effects of Blast Protection and All Damage Protection enchantments,
     * applying configurable caps.
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
     * 野火喷射的核心执行逻辑。
     * 自然属性的受害者在受到赤焰攻击且满足条件时触发反击，将周围敌对生物击退，
     * 并对其施加孢子效果。根据配置可选择是否清除燃烧和灼烧效果。
     * <p>
     * Core execution logic for Wildfire Ejection.
     * A nature-attribute victim triggers a counterattack when hit by fire under certain conditions,
     * knocking back nearby hostile entities and applying spore effects.
     * Optionally clears burning and scorched effects based on configuration.
     *
     * @param victim   发动野火喷射的受害实体（自然属性） / Victim entity (nature type) that triggers Wildfire Ejection
     * @param attacker 触发野火喷射的攻击者 / Attacker that triggers Wildfire Ejection
     */
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

            // 根据配置决定是否清除燃烧和灼烧效果
            // Clear burning and scorched effects based on configuration
            if (ElementalReactionConfig.wildfireClearBurning) {
                // 清除原版燃烧效果
                // Clear vanilla burning effect
                enemy.clearFire();
                
                // 清除灼烧效果（如果存在）
                // Clear scorched effect if present
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

    /**
     * 检查实体指定键的冷却是否结束。
     * 基于游戏刻进行判定，如果冷却键不存在或当前时间已达到或超过结束时间，则返回 true。
     * <p>
     * Checks if the cooldown for the specified key on an entity has ended.
     * Determined by game ticks; returns true if the key does not exist or the current time
     * is greater than or equal to the stored end time.
     *
     * @param entity 要检查的实体 / Entity to check
     * @param key    冷却的 NBT 键 / NBT key of the cooldown
     * @return 冷却结束返回 true，否则返回 false / True if cooldown ended, false otherwise
     */
    private static boolean checkCooldown(LivingEntity entity, String key) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(Objects.requireNonNull(key))) return true;

        long endTick = data.getLong(Objects.requireNonNull(key));
        return entity.level().getGameTime() >= endTick;
    }

    /**
     * 为实体指定键设置冷却时间。
     * 将结束时间设置为当前游戏时间加上指定的持续时间（刻）。
     * <p>
     * Sets the cooldown time for the specified key on an entity.
     * The end time is set to the current game time plus the specified duration in ticks.
     *
     * @param entity        要设置冷却的实体 / Entity to set cooldown on
     * @param key           冷却的 NBT 键 / NBT key of the cooldown
     * @param durationTicks 冷却持续的游戏刻数 / Cooldown duration in game ticks
     */
    private static void setCooldown(LivingEntity entity, String key, int durationTicks) {
        entity.getPersistentData().putLong(Objects.requireNonNull(key), entity.level().getGameTime() + durationTicks);
    }

    /**
     * 计算实体所有护甲槽位指定附魔的总等级。
     * <p>
     * Calculates the total level of a specified enchantment across all armor slots of an entity.
     *
     * @param ench   要计算的附魔 / Enchantment to calculate
     * @param entity 要计算的实体 / Entity to check
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