// src/main/java/com/xulai/elementalcraft/event/ReactionHandler.java
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
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * ReactionHandler
 * <p>
 * 元素反应系统的核心事件处理器。
 * 负责监听游戏内实体的状态更新与伤害交互，依据攻击者的元素属性与受击者的状态触发相应的元素反应链。
 * 主要包含：易燃孢子传染、自然系寄生吸取、赤焰系毒火爆燃（含连锁爆炸）、防御性野火喷射。
 * <p>
 * Core event handler for the Elemental Reaction System.
 * Listens to entity ticks and damage interactions, triggering elemental reaction chains based on attacker element and victim status.
 * Includes: Flammable Spore contagion, Nature Parasitism/Siphon, Fire Toxic Blast (w/ Chain Reaction), and Defensive Wildfire Ejection.
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class ReactionHandler {

    private static final Random RANDOM = new Random();

    // NBT Keys
    private static final String NBT_DRAIN_COOLDOWN = "ec_drain_cd";
    private static final String NBT_WILDFIRE_COOLDOWN = "ec_wildfire_cd";
    private static final String NBT_SPREADED = "ec_spreaded";
    private static final String NBT_INFECTED = "ec_infected";
    private static final String NBT_WETNESS = "EC_WetnessLevel";

    /**
     * 生物 Tick 事件监听器。
     * 处理易燃孢子的环境传染逻辑：当实体携带高层数孢子时，定期向周围未感染实体传播。
     * <p>
     * Living Entity Tick Event Listener.
     * Handles Flammable Spore contagion: Periodically spreads spores to nearby uninfected entities when the host has high stacks.
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        if (entity.tickCount % ElementalReactionConfig.contagionCheckInterval != 0) return;

        // 确保效果实例非空，避免空指针异常
        // Ensure effect instance is non-null to avoid NullPointerException
        if (ModMobEffects.SPORES.isPresent() && entity.hasEffect(Objects.requireNonNull(ModMobEffects.SPORES.get()))) {
            MobEffectInstance sporeEffect = entity.getEffect(Objects.requireNonNull(ModMobEffects.SPORES.get()));
            if (sporeEffect == null) return;

            int amplifier = sporeEffect.getAmplifier();
            int stacks = amplifier + 1;

            if (stacks >= 3) {
                processContagion(entity, stacks);
            }
        }
    }

    /**
     * 造成伤害事件监听器。
     * 处理主动攻击触发的反应：
     * 1. 自然属性：触发动态寄生（挂孢子）或寄生吸取（吸潮湿回血）。
     * 2. 赤焰属性：触发毒火爆燃（引爆孢子）。
     * <p>
     * Living Damage Event Listener.
     * Handles reactions from active attacks:
     * 1. Nature: Triggers Dynamic Parasitism (apply spores) or Parasitic Drain (drain wetness to heal).
     * 2. Fire: Triggers Toxic Blast (detonate spores).
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
            // 动态寄生逻辑：根据自然属性强度概率性施加孢子
            // Dynamic Parasitism Logic: Apply spores based on Nature attribute strength and chance
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
                    stackSporeEffect(target, 1);
                    EffectHelper.playSporeAmbient(target);
                }
            }

            // 寄生吸取逻辑：攻击潮湿目标时吸取水分并回血
            // Parasitic Drain Logic: Drain moisture and heal when attacking a wet target
            if (checkCooldown(attacker, NBT_DRAIN_COOLDOWN)) {
                CompoundTag targetData = target.getPersistentData();
                int wetnessLevel = targetData.getInt(NBT_WETNESS);

                if (wetnessLevel > 0 && naturePower >= ElementalReactionConfig.natureSiphonThreshold) {
                    triggerParasiticDrain(attacker, target, wetnessLevel, naturePower);
                }
            }
        } else if (attackType == ElementType.FIRE) {
            // 毒火爆燃逻辑：引爆目标身上的孢子，造成范围伤害
            // Toxic Blast Logic: Detonate spores on the target, causing area damage
            // 确保对象非空并处理标签空安全
            // Ensure objects non-null and handle tag null safety
            if (ModMobEffects.SPORES.isPresent() && target.hasEffect(Objects.requireNonNull(ModMobEffects.SPORES.get()))
                    && !event.getSource().is(Objects.requireNonNull(DamageTypeTags.IS_EXPLOSION))) {

                if (firePower >= ElementalReactionConfig.blastTriggerThreshold) {
                    triggerToxicBlast(level, attacker, target, firePower);
                }
            }
        }
    }

    /**
     * 受到伤害事件监听器。
     * 处理防御性被动反应：当自然属性实体受到火伤时，触发野火喷射进行反击。
     * <p>
     * Living Hurt Event Listener.
     * Handles defensive passive reactions: Triggers Wildfire Ejection when a Nature entity takes Fire damage.
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;

        LivingEntity victim = event.getEntity();

        // 确保伤害类型标签非空，并在冷却就绪时触发
        // Ensure damage type tag is non-null and trigger when cooldown is ready
        if (event.getSource().is(Objects.requireNonNull(DamageTypeTags.IS_FIRE)) && checkCooldown(victim, NBT_WILDFIRE_COOLDOWN)) {
            double naturePower = ElementUtils.getDisplayEnhancement(victim, ElementType.NATURE);

            if (naturePower >= ElementalReactionConfig.wildfireTriggerThreshold) {
                triggerWildfireEjection(victim, event.getSource().getEntity());
            }
        }
    }

    // ================================================================================================================
    // Logic Implementations / 逻辑实现
    // ================================================================================================================

    /**
     * 易燃孢子叠加逻辑。
     * 1. 全局黑名单检查：如果目标在黑名单中，拒绝施加。
     * 2. 自然抗性免疫检查：如果目标自然抗性达到阈值，拒绝施加。
     * 3. 正常叠加层数。
     * 4. 计算持续时间：雷霆属性目标时间翻倍，赤焰属性目标时间减少。
     * <p>
     * Flammable Spores stacking logic.
     * 1. Global Blacklist Check: If target is blacklisted, deny application.
     * 2. Nature Resistance Immunity Check: If target's Nature resistance meets threshold, deny application.
     * 3. Stacks add normally.
     * 4. Duration calculation: Doubled for Thunder targets, reduced for Fire targets.
     */
    private static void stackSporeEffect(LivingEntity target, int layersToAdd) {
        if (!ModMobEffects.SPORES.isPresent()) return;

        // 全局黑名单检查：禁止黑名单内的生物获得孢子
        // Global Blacklist Check: Prevent blacklisted entities from getting spores
        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).toString();
        if (ElementalReactionConfig.cachedSporeBlacklist != null && ElementalReactionConfig.cachedSporeBlacklist.contains(entityId)) {
            return;
        }

        // 自然抗性免疫检查：如果目标自然抗性达到阈值，则免疫孢子
        // Nature Resistance Immunity Check: If target's Nature resistance reaches the threshold, they are immune to spores
        double natureResistance = ElementUtils.getDisplayResistance(target, ElementType.NATURE);
        if (natureResistance >= ElementalReactionConfig.natureImmunityThreshold) {
            return;
        }

        // 确保获取效果非空
        // Ensure get effect non-null
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
            // 确保添加效果非空
            // Ensure add effect non-null
            target.addEffect(new MobEffectInstance(Objects.requireNonNull(ModMobEffects.SPORES.get()), durationTicks, newStacks - 1));
        }
    }

    /**
     * 孢子传染逻辑。
     * 将孢子传播给周围实体，支持将目标的潮湿层数转化为额外孢子。
     * 包含可配置的“仅传染敌对生物”过滤器。
     * <p>
     * Spore contagion logic.
     * Spreads spores to nearby entities, supporting conversion of target's wetness levels into extra spores.
     * Includes a configurable "Spread to Hostile Only" filter.
     */
    private static void processContagion(LivingEntity source, int stacks) {
        CompoundTag data = source.getPersistentData();

        boolean isSpreaded = data.getBoolean(NBT_SPREADED);
        boolean isInfected = data.getBoolean(NBT_INFECTED);

        if (isSpreaded || isInfected) return;

        data.putBoolean(NBT_SPREADED, true);

        double radius = ElementalReactionConfig.contagionBaseRadius + ((stacks - 3) * ElementalReactionConfig.contagionRadiusPerStack);

        int transferStacks = (int) Math.floor(stacks * ElementalReactionConfig.contagionIntensityRatio);
        if (transferStacks < 1) transferStacks = 1;

        // 确保边界框非空并进行空检查
        // Ensure bounding box non-null and check for null
        AABB sourceBox = source.getBoundingBox();
        if (sourceBox == null) return;
        
        AABB area = sourceBox.inflate(radius);
        List<LivingEntity> targets = source.level().getEntitiesOfClass(LivingEntity.class, area);
        
        List<LivingEntity> infectedTargets = new ArrayList<>();

        for (LivingEntity target : targets) {
            if (target == source) continue;

            // 传染过滤器：如果开启“仅传染敌对生物”，则跳过非敌对生物（如玩家、动物）
            // Contagion Filter: If "Spread to Hostile Only" is enabled, skip non-hostile entities (e.g., Players, Animals)
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
                // 确保移除效果非空
                // Ensure remove effect non-null
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
     * 寄生吸取执行逻辑。
     * 吸取目标潮湿层数，回复攻击者生命值并施加孢子。
     * <p>
     * Parasitic Drain execution logic.
     * Drains target wetness to heal attacker and apply spores.
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
            // 确保移除效果非空
            // Ensure remove effect non-null
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
        // 确保音效事件非空
        // Ensure sound event non-null
        EffectHelper.playSound(target.level(), attacker, Objects.requireNonNull(SoundEvents.ZOMBIE_VILLAGER_CONVERTED), 0.5f, 1.5f);
    }

    /**
     * 毒火爆燃执行逻辑。
     * 1. 立即移除目标身上的孢子，防止递归死循环。
     * 2. 低层数触发弱灼烧，高层数触发终结爆燃。
     * 3. 终结爆燃支持连锁反应：如果范围内其他生物也有孢子，将立即触发它们的爆炸。
     * 4. 包含宠物保护机制：攻击者及其宠物不受爆炸伤害。
     * <p>
     * Toxic Blast execution logic.
     * 1. Immediately removes spores from target to prevent recursive loops.
     * 2. Low stacks trigger weak scorch; High stacks trigger Terminal Blast.
     * 3. Terminal Blast supports Chain Reaction: Recursively detonates nearby spore-infected entities immediately.
     * 4. Includes Pet Protection: Attacker and their pets are immune to explosion damage.
     */
    private static void triggerToxicBlast(Level level, LivingEntity attacker, LivingEntity target, double firePower) {
        // 确保获取效果非空
        // Ensure get effect non-null
        MobEffectInstance sporeEffect = target.getEffect(Objects.requireNonNull(ModMobEffects.SPORES.get()));
        int amplifier = (sporeEffect != null) ? sporeEffect.getAmplifier() : -1;
        int stacks = amplifier + 1;

        // 立即移除孢子以防止死循环
        // Immediately remove spores to prevent infinite recursion
        target.removeEffect(Objects.requireNonNull(ModMobEffects.SPORES.get()));

        if (stacks < 3) {
            int scorchDuration = (int) (ElementalReactionConfig.blastScorchBase * 20);
            int damageStrength = (int) (firePower * ElementalReactionConfig.blastWeakIgniteMult);

            ScorchedHandler.applyScorched(target, damageStrength, scorchDuration);
            // 确保音效事件非空
            // Ensure sound event non-null
            EffectHelper.playSound(level, target, Objects.requireNonNull(SoundEvents.FIRECHARGE_USE), 1.0f, 1.2f);
        } else {
            int extraStacks = stacks - 3;

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
                // 确保粒子类型非空
                // Ensure particle types non-null
                serverLevel.sendParticles(Objects.requireNonNull(ParticleTypes.EXPLOSION_EMITTER),
                        target.getX(), target.getY() + 1.0, target.getZ(), 1, 0, 0, 0, 0);

                serverLevel.sendParticles(Objects.requireNonNull(ParticleTypes.FLAME),
                        target.getX(), target.getY() + 0.5, target.getZ(),
                        50, 1.5, 1.5, 1.5, 0.2);

                serverLevel.sendParticles(Objects.requireNonNull(ParticleTypes.LAVA),
                        target.getX(), target.getY() + 0.5, target.getZ(),
                        20, 1.0, 1.0, 1.0, 0.0);

                // 执行延迟逻辑处理范围伤害
                // Execute deferred logic for area damage
                serverLevel.getServer().execute(() -> {
                    // 确保边界框非空并检查空值
                    // Ensure bounding box non-null and check for null
                    AABB targetBox = target.getBoundingBox();
                    if (targetBox == null) return;
                    
                    AABB area = targetBox.inflate(radius);
                    List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, area);

                    int affectedCount = 0;

                    for (LivingEntity entity : nearbyEntities) {
                        // 1. 绝对不炸攻击者本人
                        // 1. Absolutely do not damage the attacker themselves
                        if (entity == attacker) continue;

                        // 2. 宠物保护：如果目标是攻击者的宠物或坐骑，则跳过
                        // 2. Pet Protection: Skip if the entity is a pet or mount owned by the attacker
                        boolean isPet = false;
                        if (entity instanceof OwnableEntity ownable && Objects.equals(ownable.getOwnerUUID(), attacker.getUUID())) {
                            isPet = true;
                        } else if (entity instanceof AbstractHorse horse && Objects.equals(horse.getOwnerUUID(), attacker.getUUID())) {
                            isPet = true;
                        }
                        if (isPet) continue;

                        entity.invulnerableTime = 0;

                        // 连锁爆炸检测：确保效果非空
                        // Chain Reaction Check: Ensure effect non-null
                        if (ElementalReactionConfig.blastChainReaction 
                                && ModMobEffects.SPORES.isPresent() 
                                && entity.hasEffect(Objects.requireNonNull(ModMobEffects.SPORES.get()))) {
                            // 递归调用，传递相同的攻击者和火力强度
                            // Recursive call, passing the same attacker and fire power
                            triggerToxicBlast(level, attacker, entity, firePower);
                        }

                        float mitigation = calculateBlastMitigation(entity);
                        float finalDamage = rawBaseDamage * (1.0f - mitigation);

                        // 确保伤害源非空
                        // Ensure damage source non-null
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
     * 爆炸防御计算逻辑。
     * 计算爆炸保护和普通保护附魔对终结爆燃伤害的减免比例。
     * <p>
     * Blast Defense Calculation Logic.
     * Calculates mitigation percentage against Terminal Blast based on Blast Protection and Protection enchantments.
     */
    private static float calculateBlastMitigation(LivingEntity entity) {
        int blastProtLevel = getTotalEnchantmentLevel(Enchantments.BLAST_PROTECTION, entity);
        int generalProtLevel = getTotalEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, entity);

        double maxBlastCap = ElementalReactionConfig.blastMaxBlastProtCap;
        double maxGeneralCap = ElementalReactionConfig.blastMaxGeneralProtCap;

        double blastFactor = maxBlastCap / 16.0;
        double generalFactor = maxGeneralCap / 16.0;

        double calculatedBlastRed = blastProtLevel * blastFactor;
        double calculatedGeneralRed = generalProtLevel * generalFactor;

        double actualBlastRed = Math.min(calculatedBlastRed, maxBlastCap);
        double actualGeneralRed = Math.min(calculatedGeneralRed, maxGeneralCap);

        return (float) Math.min(actualBlastRed + actualGeneralRed, 1.0);
    }

    /**
     * 野火喷射执行逻辑。
     * 移除自身负面状态，击退周围敌人并反向施加孢子。
     * <p>
     * Wildfire Ejection execution logic.
     * Removes negative status from self, knocks back enemies, and applies spores defensively.
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

        // 确保边界框非空并检查空值
        // Ensure bounding box non-null and check for null
        AABB victimBox = victim.getBoundingBox();
        if (victimBox == null) return;
        
        AABB area = victimBox.inflate(radius);
        List<LivingEntity> enemies = victim.level().getEntitiesOfClass(LivingEntity.class, area);

        int affectedCount = 0;

        for (LivingEntity enemy : enemies) {
            boolean isHostile = (enemy == attacker) || (enemy instanceof Enemy);
            if (enemy == victim || !isHostile) continue;

            // 确保向量对象非空并检查空值
            // Ensure vector objects non-null and check for null
            Vec3 enemyPos = enemy.position();
            Vec3 victimPos = victim.position();
            if (enemyPos == null || victimPos == null) continue;

            Vec3 vec = enemyPos.subtract(victimPos).normalize().scale(ElementalReactionConfig.wildfireKnockback);
            enemy.push(vec.x, 0.5, vec.z);
            enemy.hurtMarked = true;

            stackSporeEffect(enemy, ElementalReactionConfig.wildfireSporeAmount);
            affectedCount++;
        }

        DebugCommand.sendWildfireLog(victim, radius, affectedCount);

        setCooldown(victim, NBT_WILDFIRE_COOLDOWN, ElementalReactionConfig.wildfireCooldown);
    }

    private static boolean checkCooldown(LivingEntity entity, String key) {
        CompoundTag data = entity.getPersistentData();
        // 确保键值非空
        // Ensure key non-null
        if (!data.contains(Objects.requireNonNull(key))) return true;

        long endTick = data.getLong(Objects.requireNonNull(key));
        return entity.level().getGameTime() >= endTick;
    }

    private static void setCooldown(LivingEntity entity, String key, int durationTicks) {
        // 确保键值非空
        // Ensure key non-null
        entity.getPersistentData().putLong(Objects.requireNonNull(key), entity.level().getGameTime() + durationTicks);
    }

    private static int getTotalEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantment ench, LivingEntity entity) {
        int total = 0;
        for (ItemStack stack : entity.getArmorSlots()) {
            total += stack.getEnchantmentLevel(ench);
        }
        return total;
    }
}