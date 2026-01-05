// src/main/java/com/xulai/elementalcraft/event/ReactionHandler.java
package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
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
 * 元素反应核心逻辑处理器。
 * 负责处理基于元素属性攻击触发的各种特殊效果，包括：
 * 1. 自然系：动态寄生（施加孢子）、寄生吸取（吸取潮湿回血）。
 * 2. 赤焰系：毒火爆燃（引爆孢子产生爆炸或强力灼烧）。
 * 3. 环境系：易燃孢子的环境传染机制。
 * 4. 防御系：自然属性的野火喷射反制。
 * <p>
 * English Description:
 * Core logic handler for Elemental Reactions.
 * Handles special effects triggered by elemental attribute attacks, including:
 * 1. Nature: Dynamic Parasitism (applying spores), Parasitic Drain (draining wetness for health).
 * 2. Fire: Toxic Blast (detonating spores for explosion or potent scorched effect).
 * 3. Environmental: Flammable Spores contagion mechanics.
 * 4. Defensive: Wildfire Ejection counter-attack for Nature attribute.
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
     * 生物 Tick 事件监听。
     * 处理易燃孢子的环境传染逻辑。
     * <p>
     * Living Entity Tick Event Listener.
     * Handles the environmental contagion logic of Flammable Spores.
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        // 定期检查以优化性能
        // Check periodically to optimize performance
        if (entity.tickCount % ElementalReactionConfig.contagionCheckInterval != 0) return;

        if (ModMobEffects.SPORES.isPresent() && entity.hasEffect(ModMobEffects.SPORES.get())) {
            MobEffectInstance sporeEffect = entity.getEffect(ModMobEffects.SPORES.get());
            if (sporeEffect == null) return;

            int amplifier = sporeEffect.getAmplifier();
            int stacks = amplifier + 1;

            // 当孢子层数达到阈值时尝试触发传染
            // Attempt to trigger contagion when spore stacks reach the threshold
            if (stacks >= 3) {
                processContagion(entity, stacks);
            }
        }
    }

    /**
     * 造成伤害事件监听。
     * 处理主动攻击触发的反应：动态寄生、寄生吸取和毒火爆燃。
     * <p>
     * Living Damage Event Listener.
     * Handles reactions triggered by active attacks: Dynamic Parasitism, Parasitic Drain, and Toxic Blast.
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        
        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof LivingEntity attacker)) return;
        
        LivingEntity target = event.getEntity();
        Level level = target.level();

        // 获取一致性攻击属性（确保武器与强化匹配）
        // Get consistent attack element (ensure weapon matches enhancement)
        ElementType attackType = ElementUtils.getConsistentAttackElement(attacker);

        double naturePower = ElementUtils.getDisplayEnhancement(attacker, ElementType.NATURE);
        double firePower = ElementUtils.getDisplayEnhancement(attacker, ElementType.FIRE);

        // ======================== 自然属性分支 (Nature Branch) ========================
        if (attackType == ElementType.NATURE) {
            
            // 逻辑：动态寄生挂标
            // Logic: Dynamic Parasitism
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

                // 潮湿状态提供概率加成
                // Wetness status provides probability bonus
                CompoundTag attackerData = attacker.getPersistentData();
                if (attackerData.getInt(NBT_WETNESS) > 0) {
                    chance += attackerData.getInt(NBT_WETNESS) * ElementalReactionConfig.natureParasiteWetnessBonus;
                }

                if (RANDOM.nextDouble() < chance) {
                    stackSporeEffect(target, 1);
                    EffectHelper.playSporeAmbient(target);
                }
            }

            // 逻辑：寄生吸取
            // Logic: Parasitic Drain
            if (checkCooldown(attacker, NBT_DRAIN_COOLDOWN)) {
                CompoundTag targetData = target.getPersistentData();
                int wetnessLevel = targetData.getInt(NBT_WETNESS);
                
                if (wetnessLevel > 0 && naturePower >= ElementalReactionConfig.natureSiphonThreshold) {
                    triggerParasiticDrain(attacker, target, wetnessLevel, naturePower);
                }
            }
        }

        // ======================== 赤焰属性分支 (Fire Branch) ========================
        else if (attackType == ElementType.FIRE) {
            
            // 逻辑：毒火爆燃
            // Logic: Toxic Blast
            if (ModMobEffects.SPORES.isPresent() && target.hasEffect(ModMobEffects.SPORES.get()) 
                    && !event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
                
                if (firePower >= ElementalReactionConfig.blastTriggerThreshold) {
                    triggerToxicBlast(level, attacker, target, firePower);
                }
            }
        }
    }

    /**
     * 受到伤害事件监听。
     * 处理防御性反制反应：野火喷射。
     * <p>
     * Living Hurt Event Listener.
     * Handles defensive counter-reactions: Wildfire Ejection.
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;

        LivingEntity victim = event.getEntity();
        
        // 当自然属性者受到火焰伤害时触发
        // Triggered when a Nature-attribute entity takes Fire damage
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
     * 叠加易燃孢子效果。
     * 处理层数上限和持续时间的线性叠加。
     * <p>
     * Stacks Flammable Spores effect.
     * Handles stack limit and linear duration stacking.
     */
    private static void stackSporeEffect(LivingEntity target, int layersToAdd) {
        if (!ModMobEffects.SPORES.isPresent()) return;
        
        MobEffectInstance currentEffect = target.getEffect(ModMobEffects.SPORES.get());
        int currentAmp = (currentEffect != null) ? currentEffect.getAmplifier() : -1;
        
        int maxStacks = ElementalReactionConfig.sporeMaxStacks;
        
        if (currentAmp >= maxStacks - 1) {
            return;
        }

        int newAmp = Math.min(maxStacks - 1, currentAmp + layersToAdd);
        int stacks = newAmp + 1;
        
        int durationTicks = stacks * ElementalReactionConfig.sporeDurationPerStack * 20;

        applySporeEffectInternal(target, newAmp, durationTicks);
    }

    /**
     * 应用孢子效果的内部方法。
     * 包含针对雷霆和赤焰属性宿主的特殊修正逻辑。
     * <p>
     * Internal method to apply spore effect.
     * Includes special modifier logic for Thunder and Fire attribute hosts.
     */
    private static void applySporeEffectInternal(LivingEntity target, int amplifier, int duration) {
        boolean isThunder = ElementUtils.getDisplayEnhancement(target, ElementType.THUNDER) > 0 || 
                            ElementUtils.getDisplayResistance(target, ElementType.THUNDER) > 0;
        boolean isFire = ElementUtils.getDisplayEnhancement(target, ElementType.FIRE) > 0 || 
                         ElementUtils.getDisplayResistance(target, ElementType.FIRE) > 0;

        if (isThunder) {
            double mult = ElementalReactionConfig.sporeThunderMultiplier;
            int stacks = amplifier + 1;
            int newStacks = (int) (stacks * mult);
            amplifier = Math.max(0, newStacks - 1);
            duration = (int) (duration * mult);
        } else if (isFire) {
            double reduction = ElementalReactionConfig.sporeFireReduction;
            int stacks = amplifier + 1;
            int newStacks = (int) (stacks * reduction);
            if (newStacks < 1) return;
            
            amplifier = newStacks - 1;
            duration = (int) (duration * ElementalReactionConfig.sporeFireDurationReduction);
        }

        target.addEffect(new MobEffectInstance(Objects.requireNonNull(ModMobEffects.SPORES.get()), duration, amplifier));
    }

    /**
     * 处理环境传染逻辑。
     * 扫描周围实体并传播孢子，包含防无限循环机制。
     * <p>
     * Processes environmental contagion logic.
     * Scans nearby entities and spreads spores, including infinite loop prevention.
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

            int bonus = target.getPersistentData().getInt(NBT_WETNESS) > 0 ? 1 : 0;
            
            stackSporeEffect(target, transferStacks + bonus);
        }
        
        EffectHelper.playSporeContagion(source, radius);
    }

    /**
     * 触发寄生吸取。
     * 从目标身上移除潮湿，恢复攻击者生命值，并对目标施加孢子。
     * <p>
     * Triggers Parasitic Drain.
     * Removes wetness from target, restores attacker's health, and applies spores to target.
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
        
        setCooldown(attacker, NBT_DRAIN_COOLDOWN, ElementalReactionConfig.natureDrainCooldown);
        
        EffectHelper.playDrainEffect(attacker, target);
        EffectHelper.playSound(target.level(), attacker, SoundEvents.ZOMBIE_VILLAGER_CONVERTED, 0.5f, 1.5f);
    }

    /**
     * 触发毒火爆燃。
     * 根据孢子层数触发弱效引燃或终结爆燃。
     * <p>
     * Triggers Toxic Blast.
     * Triggers weak ignition or terminal blast based on spore layers.
     */
    private static void triggerToxicBlast(Level level, LivingEntity attacker, LivingEntity target, double firePower) {
        MobEffectInstance sporeEffect = target.getEffect(Objects.requireNonNull(ModMobEffects.SPORES.get()));
        int amplifier = (sporeEffect != null) ? sporeEffect.getAmplifier() : -1;
        int stacks = amplifier + 1;
        
        target.removeEffect(ModMobEffects.SPORES.get());

        if (stacks < 3) {
            // 弱效引燃：转化为灼烧
            // Weak Ignite: Convert to Scorched
            int scorchDuration = (int) (ElementalReactionConfig.blastScorchBase * 20); 
            int damageStrength = (int) (firePower * ElementalReactionConfig.blastWeakIgniteMult); 
            
            ScorchedHandler.applyScorched(target, damageStrength, scorchDuration);
            EffectHelper.playSound(level, target, SoundEvents.FIRECHARGE_USE, 1.0f, 1.2f);
        } else {
            // 终结爆燃：爆炸并造成大量伤害
            // Terminal Blast: Explode and deal massive damage
            int extraStacks = stacks - 3;
            
            float calculatedDamage = (float) (ElementalReactionConfig.blastBaseDamage + (extraStacks * ElementalReactionConfig.blastGrowthDamage));
            double radius = ElementalReactionConfig.blastBaseRange + (extraStacks * ElementalReactionConfig.blastGrowthRange);
            int scorchDuration = (int) ((ElementalReactionConfig.blastBaseScorchTime + (extraStacks * ElementalReactionConfig.blastGrowthScorchTime)) * 20);

            // 触发视觉爆炸（物理击退 + 声音 + 烟雾）
            level.explode(attacker, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 3.0F, Level.ExplosionInteraction.NONE);

            // 额外粒子效果
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

            // 延迟伤害结算（无视无敌帧）
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.getServer().execute(() -> {
                    AABB area = target.getBoundingBox().inflate(radius);
                    List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, area);
                    
                    for (LivingEntity entity : nearbyEntities) {
                        if (entity == attacker) continue;
                        
                        entity.invulnerableTime = 0; 
                        
                        entity.hurt(level.damageSources().explosion(attacker, attacker), calculatedDamage);
                        
                        ScorchedHandler.applyScorched(entity, (int) firePower, scorchDuration);
                    }
                });
            }
        }
    }

    /**
     * 触发野火喷射。
     * 清除负面状态，击退周围敌人并施加孢子。
     * <p>
     * Triggers Wildfire Ejection.
     * Clears negative status, knocks back surrounding enemies, and applies spores.
     */
    private static void triggerWildfireEjection(LivingEntity victim, Entity attacker) {
        // [Fixed] 延迟执行状态清除逻辑
        // 确保在当前 Tick 的所有伤害和状态应用完成后执行，防止被攻击者后续施加的燃烧覆盖
        // [Fixed] Delay status clearing logic
        // Ensure it runs after all damage and status applications in the current tick to prevent being overwritten by attacker's subsequent fire application
        if (victim.level() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().execute(() -> {
                if (victim.isAlive()) {
                    victim.clearFire();
                    victim.getPersistentData().remove(ScorchedHandler.NBT_SCORCHED_TICKS);
                }
            });
        }
        
        // 立即执行视觉和物理效果
        // Execute visual and physical effects immediately
        double radius = ElementalReactionConfig.wildfireRadius;
        EffectHelper.playWildfireEjection(victim, radius);

        AABB area = victim.getBoundingBox().inflate(radius);
        List<LivingEntity> enemies = victim.level().getEntitiesOfClass(LivingEntity.class, area);
        
        for (LivingEntity enemy : enemies) {
            boolean isHostile = (enemy == attacker) || (enemy instanceof Enemy);
            if (enemy == victim || !isHostile) continue;
            
            Vec3 vec = enemy.position().subtract(victim.position()).normalize().scale(ElementalReactionConfig.wildfireKnockback);
            enemy.push(vec.x, 0.5, vec.z);
            enemy.hurtMarked = true;
            
            stackSporeEffect(enemy, ElementalReactionConfig.wildfireSporeAmount);
        }

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
}