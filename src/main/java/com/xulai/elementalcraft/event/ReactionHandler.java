// src/main/java/com/xulai/elementalcraft/event/ReactionHandler.java
package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalReactionConfig;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.util.EffectHelper;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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

import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * ReactionHandler
 * <p>
 * 中文说明：
 * 元素反应系统的核心事件处理器。
 * 负责监听游戏内实体的状态更新与伤害交互，依据攻击者的元素属性与受击者的状态触发相应的元素反应链。
 * 主要功能模块包含：
 * 1. 易燃孢子的环境扩散与传染机制。
 * 2. 自然属性攻击触发的动态寄生与吸取回血逻辑。
 * 3. 赤焰属性攻击触发的毒火爆燃（灼烧与爆炸）逻辑。
 * 4. 防御性的野火喷射反击机制。
 * <p>
 * English Description:
 * Core event handler for the Elemental Reaction System.
 * Listens to entity ticks and damage interactions, triggering elemental reaction chains based on the attacker's element and the victim's status.
 * Main functional modules include:
 * 1. Environmental spread and contagion mechanics of Flammable Spores.
 * 2. Dynamic Parasitism and Siphon healing logic triggered by Nature attribute attacks.
 * 3. Toxic Blast (Scorched and Explosion) logic triggered by Fire attribute attacks.
 * 4. Defensive Wildfire Ejection counter-attack mechanism.
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
     * 用于处理易燃孢子的环境传染逻辑。
     * 当实体携带高层数孢子时，定期检测并向周围未感染的实体传播孢子效果。
     * <p>
     * Living Entity Tick Event Listener.
     * Handles the environmental contagion logic of Flammable Spores.
     * Periodically checks and spreads spore effects to nearby uninfected entities when the host carries high-stack spores.
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        if (entity.tickCount % ElementalReactionConfig.contagionCheckInterval != 0) return;

        if (ModMobEffects.SPORES.isPresent() && entity.hasEffect(ModMobEffects.SPORES.get())) {
            MobEffectInstance sporeEffect = entity.getEffect(ModMobEffects.SPORES.get());
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
     * 用于处理由主动攻击触发的元素反应。
     * 识别攻击者的自然或赤焰属性，判定是否触发动态寄生、寄生吸取或毒火爆燃效果。
     * <p>
     * Living Damage Event Listener.
     * Handles elemental reactions triggered by active attacks.
     * Identifies the attacker's Nature or Fire attributes to determine whether to trigger Dynamic Parasitism, Parasitic Drain, or Toxic Blast effects.
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
                    stackSporeEffect(target, 1);
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
            if (ModMobEffects.SPORES.isPresent() && target.hasEffect(ModMobEffects.SPORES.get())
                    && !event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {

                if (firePower >= ElementalReactionConfig.blastTriggerThreshold) {
                    triggerToxicBlast(level, attacker, target, firePower);
                }
            }
        }
    }

    /**
     * 受到伤害事件监听器。
     * 用于处理防御性被动反应。
     * 当自然属性实体受到火焰伤害时，触发野火喷射进行反击和自保。
     * <p>
     * Living Hurt Event Listener.
     * Handles defensive passive reactions.
     * Triggers Wildfire Ejection for counter-attack and self-preservation when a Nature attribute entity takes Fire damage.
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;

        LivingEntity victim = event.getEntity();

        if (event.getSource().is(DamageTypeTags.IS_FIRE) && checkCooldown(victim, NBT_WILDFIRE_COOLDOWN)) {
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
     * 计算并更新目标身上的孢子效果层数与持续时间。
     * 1. 易燃孢子不增加伤害，只增加持续时间（伤害值为固定配置值）。
     * 2. 雷霆属性目标：获得的层数翻倍（即持续时间翻倍）。
     * 3. 赤焰属性目标：层数正常叠加（不再减少），但持续时间按配置减少。
     * <p>
     * Flammable Spores stacking logic.
     * Calculates and updates the spore effect stacks and duration on the target.
     * 1. Flammable Spores do not increase damage, only duration (damage is fixed by config).
     * 2. Thunder Attribute Targets: Received stacks are doubled (effectively doubling duration).
     * 3. Fire Attribute Targets: Stacks add normally (no longer reduced), but duration is reduced by config.
     */
    private static void stackSporeEffect(LivingEntity target, int layersToAdd) {
        if (!ModMobEffects.SPORES.isPresent()) return;

        // 获取当前效果
        // Get current effect
        MobEffectInstance currentEffect = target.getEffect(ModMobEffects.SPORES.get());
        int currentAmp = (currentEffect != null) ? currentEffect.getAmplifier() : -1;
        int currentStacks = currentAmp + 1;

        int maxStacks = ElementalReactionConfig.sporeMaxStacks;

        if (currentStacks >= maxStacks) {
            // 即使满层也刷新时间
            // Refresh duration even if max stacks reached
            currentStacks = maxStacks;
            // layersToAdd 设为 0 以防止进一步增加层数，但仍需计算属性修正后的刷新逻辑
            // Set layersToAdd to 0 to prevent further stack increase, but logic below still needs to run for refresh
            layersToAdd = 0;
        }

        // 属性判定
        // Attribute checks
        boolean isThunder = ElementUtils.getDisplayEnhancement(target, ElementType.THUNDER) > 0 ||
                ElementUtils.getDisplayResistance(target, ElementType.THUNDER) > 0;
        boolean isFire = ElementUtils.getDisplayEnhancement(target, ElementType.FIRE) > 0 ||
                ElementUtils.getDisplayResistance(target, ElementType.FIRE) > 0;

        // 雷霆属性：层数翻倍
        // Thunder Attribute: Double the stacks
        int effectiveLayersToAdd = layersToAdd;
        if (isThunder) {
            effectiveLayersToAdd = (int) (layersToAdd * ElementalReactionConfig.sporeThunderMultiplier);
        }
        // 赤焰属性：正常层数 (Explicitly normal stacks for Fire)

        // 计算新的总层数
        // Calculate new total stacks
        int newStacks = Math.min(maxStacks, currentStacks + effectiveLayersToAdd);

        // 计算持续时间：基于总层数，每层增加固定时间
        // Calculate Duration: Based on total stacks, each stack adds fixed duration
        int durationTicks = newStacks * ElementalReactionConfig.sporeDurationPerStack * 20;

        // 赤焰属性：持续时间减少
        // Fire Attribute: Reduce the duration
        if (isFire) {
            durationTicks = (int) (durationTicks * ElementalReactionConfig.sporeFireDurationReduction);
        }

        // 应用效果
        // Apply effect
        if (newStacks > 0) {
            target.addEffect(new MobEffectInstance(Objects.requireNonNull(ModMobEffects.SPORES.get()), durationTicks, newStacks - 1));
        }
    }

    /**
     * 孢子传染逻辑。
     * 检测源实体周围的其他生物，并根据传染系数将一部分孢子层数复制给目标。
     * 同时检测目标的潮湿状态，根据配置将潮湿层数转化为额外的孢子层数，并可选择性地消耗潮湿状态。
     * <p>
     * Spore contagion logic.
     * Detects other living entities around the source and copies a portion of the spore stacks to the targets based on the contagion ratio.
     * Also checks the target's wetness status, converting wetness levels into extra spore stacks based on config, and optionally consuming the wetness.
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

        AABB area = Objects.requireNonNull(source.getBoundingBox()).inflate(radius);
        List<LivingEntity> targets = source.level().getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity target : targets) {
            if (target == source) continue;

            target.getPersistentData().putBoolean(NBT_INFECTED, true);

            // 潮湿转化逻辑
            // Wetness conversion logic
            int wetnessLevel = target.getPersistentData().getInt(NBT_WETNESS);
            int wetnessBonus = 0;

            // 只有当潮湿层数超过配置的阈值时才进行计算
            // Only calculate if wetness level exceeds the configured threshold
            if (wetnessLevel > ElementalReactionConfig.contagionWetnessThreshold) {
                int effectiveWetness = wetnessLevel - ElementalReactionConfig.contagionWetnessThreshold;
                wetnessBonus = (int) Math.floor(effectiveWetness * ElementalReactionConfig.contagionWetnessConversionRatio);
                
                // 限制最大加成
                // Cap the bonus
                wetnessBonus = Math.min(wetnessBonus, ElementalReactionConfig.contagionWetnessMaxBonus);
            }

            // 应用消耗逻辑：如果有转化加成，并且配置开启了消耗
            // Apply consumption logic: If there is a conversion bonus and consumption is enabled
            if (wetnessBonus > 0 && ElementalReactionConfig.contagionConsumesWetness) {
                // 移除潮湿数据
                // Remove wetness data
                target.getPersistentData().remove(NBT_WETNESS);
                
                // 移除潮湿效果
                // Remove wetness effect
                if (ModMobEffects.WETNESS.isPresent() && target.hasEffect(ModMobEffects.WETNESS.get())) {
                    target.removeEffect(ModMobEffects.WETNESS.get());
                }
            }

            // 这里的 transferStacks + wetnessBonus 就是传递给 stackSporeEffect 的 layersToAdd
            // stackSporeEffect 内部会根据目标的雷霆/赤焰属性再次进行修正
            // The transferStacks + wetnessBonus here is the layersToAdd passed to stackSporeEffect
            // stackSporeEffect will internally adjust again based on the target's Thunder/Fire attributes
            stackSporeEffect(target, transferStacks + wetnessBonus);
        }

        EffectHelper.playSporeContagion(source, radius);
    }

    /**
     * 寄生吸取执行逻辑。
     * 计算吸取量，移除目标的潮湿层数，转化为攻击者的生命回复，并给目标施加孢子效果。
     * 成功触发后，会通过 DebugCommand 发送调试日志。
     * <p>
     * Parasitic Drain execution logic.
     * Calculates drain amount, removes wetness levels from the target, converts them into health restoration for the attacker, and applies spore effects to the target.
     * Sends debug log via DebugCommand upon successful trigger.
     */
    private static void triggerParasiticDrain(LivingEntity attacker, LivingEntity target, int currentWetness, double naturePower) {
        double step = ElementalReactionConfig.natureDrainPowerStep;
        int drainCapacity = (int) Math.floor(naturePower / step);
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
        EffectHelper.playSound(target.level(), attacker, SoundEvents.ZOMBIE_VILLAGER_CONVERTED, 0.5f, 1.5f);
    }

    /**
     * 毒火爆燃执行逻辑。
     * 根据目标身上的孢子层数决定反应类型。低层数触发弱效灼烧，高层数触发终结爆燃。
     * 终结爆燃包含物理爆炸、粒子特效以及基于附魔计算的伤害结算。
     * 在伤害结算阶段会统计受影响实体数量并发送调试日志。
     * <p>
     * Toxic Blast execution logic.
     * Determines the reaction type based on the spore stacks on the target. Low stacks trigger weak scorching, while high stacks trigger Terminal Blast.
     * Terminal Blast includes physical explosions, particle effects, and damage calculation based on enchantments.
     * Counts affected entities and sends debug log during damage calculation phase.
     */
    private static void triggerToxicBlast(Level level, LivingEntity attacker, LivingEntity target, double firePower) {
        MobEffectInstance sporeEffect = target.getEffect(Objects.requireNonNull(ModMobEffects.SPORES.get()));
        int amplifier = (sporeEffect != null) ? sporeEffect.getAmplifier() : -1;
        int stacks = amplifier + 1;

        target.removeEffect(ModMobEffects.SPORES.get());

        if (stacks < 3) {
            int scorchDuration = (int) (ElementalReactionConfig.blastScorchBase * 20);
            int damageStrength = (int) (firePower * ElementalReactionConfig.blastWeakIgniteMult);

            ScorchedHandler.applyScorched(target, damageStrength, scorchDuration);
            EffectHelper.playSound(level, target, SoundEvents.FIRECHARGE_USE, 1.0f, 1.2f);
        } else {
            int extraStacks = stacks - 3;

            float rawBaseDamage = (float) (ElementalReactionConfig.blastBaseDamage + (extraStacks * ElementalReactionConfig.blastGrowthDamage));
            double radius = ElementalReactionConfig.blastBaseRange + (extraStacks * ElementalReactionConfig.blastGrowthRange);
            int scorchDuration = (int) ((ElementalReactionConfig.blastBaseScorchTime + (extraStacks * ElementalReactionConfig.blastGrowthScorchTime)) * 20);

            level.explode(attacker, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 3.0F, Level.ExplosionInteraction.NONE);

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                        target.getX(), target.getY() + 1.0, target.getZ(), 1, 0, 0, 0, 0);

                serverLevel.sendParticles(ParticleTypes.FLAME,
                        target.getX(), target.getY() + 0.5, target.getZ(),
                        50, 1.5, 1.5, 1.5, 0.2);

                serverLevel.sendParticles(ParticleTypes.LAVA,
                        target.getX(), target.getY() + 0.5, target.getZ(),
                        20, 1.0, 1.0, 1.0, 0.0);
            }

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.getServer().execute(() -> {
                    AABB area = target.getBoundingBox().inflate(radius);
                    List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, area);

                    int affectedCount = 0;

                    for (LivingEntity entity : nearbyEntities) {
                        if (entity == attacker) continue;

                        entity.invulnerableTime = 0;

                        float mitigation = calculateBlastMitigation(entity);
                        float finalDamage = rawBaseDamage * (1.0f - mitigation);

                        entity.hurt(level.damageSources().explosion(attacker, attacker), finalDamage);

                        ScorchedHandler.applyScorched(entity, (int) firePower, scorchDuration);
                        affectedCount++;
                    }

                    DebugCommand.sendToxicBlastLog(attacker, target, stacks, radius, affectedCount);
                });
            }
        }
    }

    /**
     * 爆炸伤害防御计算逻辑。
     * 根据实体的护甲附魔情况（爆炸保护和普通保护），计算对终结爆燃伤害的抵消百分比。
     * <p>
     * Blast damage defense calculation logic.
     * Calculates the percentage of mitigation against Terminal Blast damage based on the entity's armor enchantments (Blast Protection and Protection).
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
     * 移除自身的燃烧状态，并对周围敌人造成物理击退和孢子感染效果。
     * 触发后会统计被击退的敌对实体数量，并发送调试日志。
     * <p>
     * Wildfire Ejection execution logic.
     * Removes the burning status from self and inflicts physical knockback and spore infection on surrounding enemies.
     * Counts knocked-back hostile entities and sends debug log upon trigger.
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

        AABB area = victim.getBoundingBox().inflate(radius);
        List<LivingEntity> enemies = victim.level().getEntitiesOfClass(LivingEntity.class, area);

        int affectedCount = 0;

        for (LivingEntity enemy : enemies) {
            boolean isHostile = (enemy == attacker) || (enemy instanceof Enemy);
            if (enemy == victim || !isHostile) continue;

            Vec3 vec = enemy.position().subtract(victim.position()).normalize().scale(ElementalReactionConfig.wildfireKnockback);
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
}