// src/main/java/com/xulai/elementalcraft/event/SteamReactionHandler.java
package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalReactionConfig;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Random;

/**
 * SteamReactionHandler
 *
 * 中文说明：
 * 负责处理 "蒸汽云 (Steam Cloud)" 元素反应。
 * 包含以下机制：
 * 1. 自我干燥 (Self-Drying)：攻击者(火)自身潮湿时，消耗热量烘干自己，不触发外部蒸汽云。
 * 2. 蒸汽反应 (Steam Reaction)：
 * - 火 -> 湿/冰 = 高温蒸汽 (High Heat)：造成烫伤，移除潮湿。
 * - 冰 -> 火 = 低温蒸汽 (Low Heat)：无伤，施加潮湿。
 * 3. 持续效果：烫伤(支持黑名单/抗性免疫)、冷凝、失明、仇恨清除。
 *
 * English description:
 * Handles the "Steam Cloud" elemental reaction.
 * Mechanisms included:
 * 1. Self-Drying: Wet Fire attacker dries self using heat, preventing external steam cloud.
 * 2. Steam Reaction:
 * - Fire -> Wet/Ice = High Heat Steam: Scalding damage, removes wetness.
 * - Frost -> Fire = Low Heat Steam: No damage, adds wetness.
 * 3. Tick Effects: Scalding (supports blacklist/resistance immunity), Condensation, Blindness, Aggro clear.
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class SteamReactionHandler {

    public static final String TAG_STEAM_CLOUD = "EC_SteamCloud";
    public static final String TAG_HIGH_HEAT = "EC_HighHeat";

    /**
     * 监听生物受伤事件。
     * 使用 LOWEST 优先级，确保在 CombatEvents 计算完基础元素伤害后再进行修正。
     *
     * Listens for living entity hurt events.
     * Uses LOWEST priority to ensure it runs after CombatEvents calculates base elemental damage.
     *
     * @param event 受伤事件 / Hurt event
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!ElementalReactionConfig.steamReactionEnabled) return;

        LivingEntity target = event.getEntity();
        Entity attackerEntity = event.getSource().getEntity();

        // --------------------------------------------------------
        // 0. 判断元素属性 (Identify Elements)
        // --------------------------------------------------------
        ElementType attackElement = ElementType.NONE;
        
        // 检查攻击属性 (Check attack element)
        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
            attackElement = ElementType.FIRE;
        } else if (event.getSource().is(DamageTypeTags.IS_FREEZING)) { 
            attackElement = ElementType.FROST;
        } else if (attackerEntity instanceof LivingEntity attacker) {
            attackElement = ElementUtils.getAttackElement(attacker);
        }

        // --------------------------------------------------------
        // 1. 自我干燥机制 (Self-Drying Mechanism) - 优先判定
        // --------------------------------------------------------
        // 场景：攻击者是火属性，但自身带有潮湿效果
        // Scenario: Attacker is Fire element but has Wetness effect
        if (attackElement == ElementType.FIRE && attackerEntity instanceof LivingEntity attacker) {
            CompoundTag data = attacker.getPersistentData();
            if (data.contains(WetnessHandler.NBT_WETNESS)) {
                int attackerWetness = data.getInt(WetnessHandler.NBT_WETNESS);
                
                if (attackerWetness > 0) {
                    // 执行自我干燥逻辑
                    // Execute Self-Drying Logic
                    performSelfDrying(attacker, event, attackerWetness);
                    
                    // 【重要】既然热量被用于烘干自己，就不会再传递给目标生成蒸汽云
                    // Stop here. Heat is used internally, no steam cloud on target.
                    return; 
                }
            }
        }

        // --------------------------------------------------------
        // 2. 目标反应逻辑 (Target Reaction Logic)
        // --------------------------------------------------------
        // 如果攻击者没有潮湿（或不是火属性），则继续判断目标的状态
        
        // 检查目标是否潮湿 (Check if target is wet)
        boolean targetIsWet = target.getPersistentData().getInt(WetnessHandler.NBT_WETNESS) > 0;
        // 检查目标元素属性 (Check target element type)
        ElementType targetElement = ElementUtils.getElementType(target); 

        // 情况 A: 火 -> 湿 / 火 -> 冰 (高温蒸汽 / High Heat)
        // Case A: Fire -> Wet / Fire -> Frost
        if (attackElement == ElementType.FIRE) {
            if (targetIsWet || targetElement == ElementType.FROST) {
                // 火打冰已被克制减伤，不再叠加减伤；火打湿需应用动态减伤
                // Fire vs Frost is already reduced by restraint; Fire vs Wet needs dynamic reduction
                boolean shouldReduceDamage = targetIsWet && targetElement != ElementType.FROST; 
                triggerSteamReaction(target, event, shouldReduceDamage, true); // true = High Heat
            }
        }
        
        // 情况 B: 冰 -> 火 (低温蒸汽 / Low Heat)
        // Case B: Frost -> Fire
        else if (attackElement == ElementType.FROST) {
            if (targetElement == ElementType.FIRE) {
                // 冰打火已有增伤，只负责出特效
                // Frost vs Fire has bonus damage, only spawn effects here
                triggerSteamReaction(target, event, false, false); // false = Low Heat
            }
        }
    }

    /**
     * 执行自我干燥逻辑。
     * 消耗热量减少自身潮湿层数，同时降低本次攻击伤害。
     *
     * Executes Self-Drying logic.
     * Consumes heat to reduce own wetness, reducing attack damage.
     */
    private static void performSelfDrying(LivingEntity attacker, LivingHurtEvent event, int currentWetness) {
        // 1. 计算伤害惩罚 (Damage Penalty)
        // 能量内耗：伤害降低一定比例 (读取配置)
        // Energy Loss: Damage reduced by a percentage (Read from config)
        float penaltyRatio = (float) ElementalReactionConfig.wetnessSelfDryingDamagePenalty;
        event.setAmount(event.getAmount() * (1.0f - penaltyRatio));

        // 2. 计算烘干层数 (Calculate Drying)
        // 基础移除 1 层 + (赤焰强化 / 阈值)
        // Base 1 layer + (Fire Enhancement / Threshold)
        int fireEnhancement = ElementUtils.getDisplayEnhancement(attacker, ElementType.FIRE);
        int threshold = ElementalReactionConfig.wetnessDryingThreshold;
        if (threshold <= 0) threshold = 20;
        
        int layersToRemove = 1 + (fireEnhancement / threshold);
        
        // 3. 更新 NBT (Update NBT)
        int newLevel = Math.max(0, currentWetness - layersToRemove);
        if (newLevel == 0) {
            attacker.getPersistentData().remove(WetnessHandler.NBT_WETNESS);
            attacker.getPersistentData().remove(WetnessHandler.NBT_RAIN_TIMER);
            attacker.getPersistentData().remove(WetnessHandler.NBT_DECAY_TIMER);
            attacker.removeEffect(com.xulai.elementalcraft.potion.ModMobEffects.WETNESS.get());
        } else {
            attacker.getPersistentData().putInt(WetnessHandler.NBT_WETNESS, newLevel);
        }

        // 4. 视觉与听觉反馈 (FX)
        // Visual & Sound FX
        if (!attacker.level().isClientSide) {
            ServerLevel serverLevel = (ServerLevel) attacker.level();
            
            // 声音 (Sound)
            serverLevel.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), 
                SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 
                0.8F, 1.0F + serverLevel.random.nextFloat() * 0.4F);
            
            // 粒子 (少量，表示内耗)
            // Particles (Small amount, indicating internal loss)
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                attacker.getX(), attacker.getY() + attacker.getBbHeight() * 0.5, attacker.getZ(),
                5, 0.2, 0.2, 0.2, 0.05);
            
            // 发送调试信息
            DebugCommand.sendDebugMessage(attacker, 
                Component.literal("§6[Steam] Self-Drying triggered! Removed " + layersToRemove + " layers."));
        }
    }

    /**
     * 触发蒸汽反应逻辑 (目标身上)。
     * 处理伤害减免并生成云雾。
     *
     * Trigger Steam Reaction on target.
     * Handles damage reduction and spawns cloud.
     */
    private static void triggerSteamReaction(LivingEntity target, LivingHurtEvent event, boolean applyDamageReduction, boolean isHighHeat) {
        // 1. 处理伤害修正
        // 1. Handle Damage Modification
        if (applyDamageReduction) {
            int wetnessLevel = target.getPersistentData().getInt(WetnessHandler.NBT_WETNESS);
            float originalDamage = event.getAmount();
            // 读取配置静态缓存并强转
            float reductionPerLevel = (float) ElementalReactionConfig.wetnessFireReduction;
            float maxReduction = (float) ElementalReactionConfig.steamMaxReduction;
            float reductionRatio = Math.min(wetnessLevel * reductionPerLevel, maxReduction);
            
            float newDamage = originalDamage * (1.0f - reductionRatio);
            event.setAmount(newDamage);
            
            if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                DebugCommand.sendDebugMessage(attacker,
                    Component.translatable("debug.elementalcraft.reaction.steam", (int)(reductionRatio * 100))
                );
            }
        }

        // 2. 生成蒸汽云
        // 2. Spawn Steam Cloud
        if (!target.level().isClientSide) {
            spawnSteamCloud(target, isHighHeat);
        }
    }

    /**
     * 在目标位置生成蒸汽云 (Spawn Cloud Logic)。
     *
     * @param target 目标实体
     * @param isHighHeat true=高温蒸汽(Fire->Wet/Ice), false=低温蒸汽(Ice->Fire)
     */
    private static void spawnSteamCloud(LivingEntity target, boolean isHighHeat) {
        if (!(target.level() instanceof ServerLevel serverLevel)) return;

        float radius = (float) ElementalReactionConfig.steamCloudRadius;
        // 低温蒸汽持续时间减半
        // Low heat duration is halved
        int duration = isHighHeat ? ElementalReactionConfig.steamCloudDuration : (ElementalReactionConfig.steamCloudDuration / 2);

        // 1. 机制层 (隐形判定框)
        // 1. Logic Layer (Invisible Hitbox)
        AreaEffectCloud cloud = new AreaEffectCloud(serverLevel, target.getX(), target.getY(), target.getZ());
        cloud.setRadius(radius);
        cloud.setRadiusOnUse(0F);
        cloud.setRadiusPerTick(- (radius / (float) duration));
        cloud.setDuration(duration);
        
        // 空气粒子 (隐形)
        cloud.setParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AIR.defaultBlockState()));
        
        int blindDuration = ElementalReactionConfig.steamBlindnessDuration;
        // 失明药水 (无粒子)
        cloud.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, blindDuration, 0, false, false, true));
        
        cloud.addTag(TAG_STEAM_CLOUD);
        if (isHighHeat) {
            cloud.addTag(TAG_HIGH_HEAT);
        }
        
        cloud.setFixedColor(0xFFFFFF);
        serverLevel.addFreshEntity(cloud);

        // 2. 听觉层
        // 2. Audio Layer
        float volume = isHighHeat ? 1.0F : 0.6F;
        serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(), 
            SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 
            volume, (0.9F + serverLevel.random.nextFloat() * 0.2F));

        // 3. 视觉层 (立体扩散)
        // 3. Visual Layer (Volumetric spread)
        Random random = new Random();
        int particleCount = (int) (radius * (isHighHeat ? 45 : 25)); 

        for (int i = 0; i < particleCount; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = Math.sqrt(random.nextDouble()) * radius;
            double offsetX = Math.cos(angle) * dist;
            double offsetZ = Math.sin(angle) * dist;
            double offsetY = random.nextDouble() * target.getBbHeight() * 0.8 + 0.2;

            // 粒子差异化
            // Particle differentiation
            double driftX = (random.nextDouble() - 0.5) * 0.04; 
            double driftZ = (random.nextDouble() - 0.5) * 0.04;
            double riseY;
            
            if (isHighHeat) {
                // 高温：快速上升，混入黑烟和微量熔岩
                // High Heat: Fast rise, mix with smoke/lava
                riseY = 0.05 + random.nextDouble() * 0.08;
                
                // 主粒子：白烟
                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    target.getX() + offsetX, target.getY() + offsetY, target.getZ() + offsetZ,
                    0, driftX, riseY, driftZ, 1.0);
                
                // 辅粒子：黑烟 (20% 概率)
                if (random.nextFloat() < 0.2f) {
                    serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                        target.getX() + offsetX, target.getY() + offsetY, target.getZ() + offsetZ,
                        0, driftX * 2, riseY * 1.2, driftZ * 2, 1.0);
                }
                
                // 辅粒子：熔岩 (2% 概率)
                if (random.nextFloat() < 0.02f) {
                    serverLevel.sendParticles(ParticleTypes.LAVA,
                        target.getX() + offsetX, target.getY() + offsetY, target.getZ() + offsetZ,
                        0, 0, 0, 0, 1.0);
                }
                
            } else {
                // 低温：悬停/下沉，混入水滴
                // Low Heat: Hover/Sink, mix with water drops
                riseY = (random.nextDouble() - 0.3) * 0.02; // 微弱的上下浮动
                
                // 主粒子：云片
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                    target.getX() + offsetX, target.getY() + offsetY, target.getZ() + offsetZ,
                    0, driftX * 0.5, riseY, driftZ * 0.5, 0.1);
                
                // 辅粒子：落水 (15% 概率)
                if (random.nextFloat() < 0.15f) {
                    serverLevel.sendParticles(ParticleTypes.FALLING_WATER,
                        target.getX() + offsetX, target.getY() + offsetY + 0.5, target.getZ() + offsetZ,
                        1, 0, 0, 0, 0.0);
                }
            }
        }
    }

    /**
     * 监听生物 Tick 事件。
     * 处理位于蒸汽云内的生物逻辑：烫伤(High Heat)、干燥(High Heat)、冷凝(Low Heat)。
     *
     * Listens to LivingEntity tick.
     * Handles logic inside steam cloud: Scalding/Drying (High Heat), Condensation (Low Heat).
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!ElementalReactionConfig.steamReactionEnabled) return;
        
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        // 每秒判定一次 (20 ticks)
        // Check once per second
        if (entity.tickCount % 20 != 0) return;

        // 【修正】使用 inflate 扩大搜索范围，而不是仅使用生物本身的 Hitbox
        // AreaEffectCloud 的 Hitbox 极小，必须主动扩大搜索框来找到周围的云
        // [FIX] Inflate search box by max steam radius to detect nearby clouds
        double searchRadius = ElementalReactionConfig.steamCloudRadius;
        AABB box = entity.getBoundingBox().inflate(searchRadius);
        
        List<AreaEffectCloud> clouds = entity.level().getEntitiesOfClass(AreaEffectCloud.class, box, 
            c -> c.getTags().contains(TAG_STEAM_CLOUD));

        if (!clouds.isEmpty()) {
            boolean isScalding = false;
            boolean isCondensing = false;
            boolean isInAnyCloud = false;

            // 检查周围云的类型和距离
            // Check cloud types and distance
            for (AreaEffectCloud cloud : clouds) {
                // 【修正】手动判断距离：确保生物在云的半径内
                // [FIX] Manual distance check: Ensure entity is within cloud radius
                float cloudRadius = cloud.getRadius();
                // 忽略距离过远的云
                if (entity.distanceToSqr(cloud) > cloudRadius * cloudRadius) continue;
                
                isInAnyCloud = true;

                if (cloud.getTags().contains(TAG_HIGH_HEAT)) {
                    isScalding = true; // 只要有一个高温云，就判定为高温环境
                } else {
                    isCondensing = true; // 有低温云
                }
            }
            
            if (!isInAnyCloud) return;

            // 1. 清除仇恨 (通用)
            // 1. Clear Aggro (Common)
            if (ElementalReactionConfig.steamClearAggro && entity instanceof Mob mob) {
                if (mob.getTarget() != null) {
                    mob.setTarget(null);
                    mob.getNavigation().stop();
                }
            }

            // 2. 高温环境：烫伤 + 瞬间干燥 (High Heat)
            // 2. High Heat: Scalding + Instant Drying
            if (isScalding) {
                // --- 免疫判定 (Immunity Checks) ---
                boolean isImmune = entity.fireImmune() || entity.hasEffect(MobEffects.FIRE_RESISTANCE);
                
                if (!isImmune) {
                    // 黑名单检查 (Check Blacklist)
                    String id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
                    if (ElementalReactionConfig.cachedSteamBlacklist.contains(id)) isImmune = true;
                }
                
                if (!isImmune) {
                    // 抗性阈值检查 (Check Resistance Threshold)
                    int fireResist = ElementUtils.getDisplayResistance(entity, ElementType.FIRE);
                    if (fireResist >= ElementalReactionConfig.steamImmunityThreshold) isImmune = true;
                }

                // --- 伤害执行 (Apply Damage) ---
                if (!isImmune) {
                    float damage = (float) ElementalReactionConfig.steamScaldingDamage;
                    
                    // 易伤检查 (Vulnerability: Frost/Nature)
                    // 需要使用 getElementType 获取生物属性
                    ElementType type = ElementUtils.getElementType(entity);
                    if (type == ElementType.FROST || type == ElementType.NATURE) {
                        damage *= (float) ElementalReactionConfig.steamScaldingMultiplierWeakness;
                    }
                    
                    if (damage > 0) {
                        // 【修正】重置无敌时间，确保环境伤害生效
                        // [FIX] Reset invulnerable time to ensure environmental damage applies
                        entity.invulnerableTime = 0;
                        // 【修正】使用 inFire 替代 hotFloor，判定更稳定
                        // [FIX] Use inFire instead of hotFloor for better detection
                        entity.hurt(entity.damageSources().inFire(), damage);
                    }
                }
                
                // --- 干燥逻辑 (Drying Logic) ---
                // 只要在高温云里，无论是否免疫伤害，潮湿效果都会被蒸发
                // Wetness evaporates in high heat regardless of immunity
                if (entity.getPersistentData().contains(WetnessHandler.NBT_WETNESS)) {
                    entity.getPersistentData().remove(WetnessHandler.NBT_WETNESS);
                    entity.getPersistentData().remove(WetnessHandler.NBT_RAIN_TIMER);
                    entity.getPersistentData().remove(WetnessHandler.NBT_DECAY_TIMER);
                    entity.removeEffect(com.xulai.elementalcraft.potion.ModMobEffects.WETNESS.get());
                }
            } 
            // 3. 低温环境：冷凝潮湿 (Low Heat)
            // 3. Low Heat: Condensation
            // 仅在没有高温干扰的情况下生效
            else if (isCondensing) {
                int currentLevel = entity.getPersistentData().getInt(WetnessHandler.NBT_WETNESS);
                int maxLevel = ElementalReactionConfig.wetnessMaxLevel;
                
                // 增加层数 (Condensation)
                if (currentLevel < maxLevel) {
                    entity.getPersistentData().putInt(WetnessHandler.NBT_WETNESS, currentLevel + 1);
                    // 图标会在下一次 WetnessHandler tick 更新
                }
            }
        }
    }
}