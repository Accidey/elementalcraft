// src/main/java/com/xulai/elementalcraft/event/SteamReactionHandler.java
package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalReactionConfig;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
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
 * 处理蒸汽云元素反应的核心逻辑类。
 * 包含触发判定（双轨制 + 严格一致性）、防御计算（分层防御）和持续环境效果。
 * 实现了 v3.0 动态蒸汽云：根据潮湿层数动态缩放半径、持续时间和视听特效。
 *
 * English description:
 * Core logic class for Steam Cloud elemental reactions.
 * Includes trigger logic (Dual-Track + Strict Consistency), defense calculation (Layered Defense), and persistent environmental effects.
 * Implements v3.0 Dynamic Steam Cloud: Radius, duration, and AV effects scale with wetness levels.
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class SteamReactionHandler {

    public static final String TAG_STEAM_CLOUD = "EC_SteamCloud";
    public static final String TAG_HIGH_HEAT = "EC_HighHeat";

    /**
     * Trigger Logic Listener
     * 触发逻辑监听器
     *
     * Listens to LivingHurtEvent to determine if a steam reaction should be triggered based on thresholds and attributes.
     * 监听生物受伤事件，根据阈值和属性判定是否触发蒸汽反应。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!ElementalReactionConfig.steamReactionEnabled) return;
        
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            processTriggerLogic(event, attacker, event.getEntity());
        }
    }

    /**
     * Defense Logic Listener
     * 防御逻辑监听器
     *
     * Intercepts STEAM_SCALDING damage to apply enchantment caps and damage floors.
     * 拦截蒸汽烫伤伤害，应用附魔上限截断和伤害保底逻辑。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getSource().is(ModDamageTypes.STEAM_SCALDING)) {
            processDefenseLogic(event);
        }
    }

    /**
     * Tick Logic Listener
     * 持续效果监听器
     *
     * Handles effects for entities inside steam clouds (Scalding, Condensation, Blindness).
     * 处理位于蒸汽云内生物的效果（烫伤、冷凝、失明）。
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!ElementalReactionConfig.steamReactionEnabled) return;
        processCloudEffects(event.getEntity());
    }

    // =================================================================================================
    //                                      Logic Implementation
    //                                      逻辑实现
    // =================================================================================================

    /**
     * Dual-Track Trigger Processing with Strict Consistency
     * 带严格一致性的双轨触发处理
     *
     * Evaluates Fire vs Wet/Frost and Frost vs Fire scenarios.
     * Requires the attacker to have consistent weapon and armor attributes.
     * 评估“火对湿/冰”和“冰对火”的情景。要求攻击者具备一致的武器和装备属性。
     */
    private static void processTriggerLogic(LivingHurtEvent event, LivingEntity attacker, LivingEntity target) {
        // Strict consistency check: returns NONE if weapon attribute does not match armor enhancement
        // 严格一致性检查：如果武器属性与护甲强化不匹配，则返回 NONE
        ElementType attackElement = ElementUtils.getConsistentAttackElement(attacker);
        
        // Compatibility for environmental damage sources
        // 环境伤害源兼容
        if (event.getSource().is(DamageTypeTags.IS_FIRE)) attackElement = ElementType.FIRE;
        if (event.getSource().is(DamageTypeTags.IS_FREEZING)) attackElement = ElementType.FROST;

        int firePower = ElementUtils.getDisplayEnhancement(attacker, ElementType.FIRE);
        int frostPower = ElementUtils.getDisplayEnhancement(attacker, ElementType.FROST);
        
        boolean targetIsWet = target.getPersistentData().getInt(WetnessHandler.NBT_WETNESS) > 0;
        int targetWetness = target.getPersistentData().getInt(WetnessHandler.NBT_WETNESS);
        ElementType targetElement = ElementUtils.getElementType(target);

        // Fire Attack Scenario / 赤焰攻击情景
        if (attackElement == ElementType.FIRE) {
            // Self-Drying Logic / 自我干燥逻辑
            int attackerWetness = attacker.getPersistentData().getInt(WetnessHandler.NBT_WETNESS);
            if (attackerWetness > 0) {
                applySelfDrying(attacker, event, attackerWetness);
                return;
            }

            // Fire vs Wet or Frost / 火 对 潮湿或冰霜
            if (targetIsWet || targetElement == ElementType.FROST) {
                int threshold = ElementalReactionConfig.steamTriggerThresholdFire;
                
                if (firePower >= threshold) {
                    // Trigger Burst / 触发爆发
                    ElementalCraft.LOGGER.info("[蒸汽日志] 赤焰触发成功! 强化点: {} >= 阈值: {}", firePower, threshold);
                    
                    // Fuel level: Wetness level or 1 for Frost entities
                    // 燃料等级：潮湿层数或冰霜生物默认为1
                    int fuelLevel = targetIsWet ? targetWetness : 1;
                    spawnSteamCloud(target, true, fuelLevel); 
                    
                    if (targetIsWet) removeWetness(target);
                } 
                else {
                    if (targetIsWet) removeWetness(target); // Only dry / 仅烘干
                }
            }
        }
        // Frost Attack Scenario / 冰霜攻击情景
        else if (attackElement == ElementType.FROST) {
            // Frost vs Fire / 冰霜 对 赤焰
            if (targetElement == ElementType.FIRE) {
                int threshold = ElementalReactionConfig.steamTriggerThresholdFrost;
                
                if (frostPower >= threshold) {
                    // Trigger Condensation / 触发冷凝
                    ElementalCraft.LOGGER.info("[蒸汽日志] 冰霜触发成功! 强化点: {} >= 阈值: {}", frostPower, threshold);
                    // Condensation default equivalent to level 3
                    // 冷凝默认相当于3层规模
                    spawnSteamCloud(target, false, 3); 
                } 
            }
        }
    }

    /**
     * Layered Defense Processing
     * 分层防御处理
     *
     * Calculates final damage after applying custom enchantment caps and vulnerability floors.
     * 在应用自定义附魔上限和易伤保底后计算最终伤害。
     */
    private static void processDefenseLogic(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        float rawDamage = event.getAmount();
        
        // Immunity Check / 免疫检查
        if (checkImmunity(target)) {
            event.setAmount(0);
            event.setCanceled(true);
            return;
        }

        // Enchantment Calculation / 附魔计算
        int fireProtLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.FIRE_PROTECTION, target);
        int protLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, target);

        double fireProtReduction = Math.min(fireProtLevel * 0.08, ElementalReactionConfig.steamMaxFireProtCap);
        double protReduction = Math.min(protLevel * 0.04, ElementalReactionConfig.steamMaxGeneralProtCap);
        
        double totalReduction = Math.min(fireProtReduction + protReduction, 0.8);
        float reducedDamage = rawDamage * (float)(1.0 - totalReduction);

        // Floor Logic / 保底逻辑
        ElementType type = ElementUtils.getElementType(target);
        if (type == ElementType.FROST || type == ElementType.NATURE) {
            float floorLimit = rawDamage * (float) ElementalReactionConfig.steamDamageFloorRatio;
            if (reducedDamage < floorLimit) {
                ElementalCraft.LOGGER.info("[蒸汽日志] 触发保底伤害! 减免后: {} -> 保底值: {}", reducedDamage, floorLimit);
                reducedDamage = floorLimit;
            }
        }
        
        event.setAmount(reducedDamage);
    }

    /**
     * Cloud Effect Processing
     * 云效果处理
     *
     * Applies scalding, drying, or condensation effects to entities within cloud range.
     * 对云范围内的实体应用烫伤、干燥或冷凝效果。
     */
    private static void processCloudEffects(LivingEntity entity) {
        if (entity.level().isClientSide || entity.tickCount % 20 != 0) return;

        // Expanded search radius for large clouds
        // 扩大搜索范围以适应大半径云
        double searchRadius = ElementalReactionConfig.steamCloudRadius * 3.0; 
        AABB box = entity.getBoundingBox().inflate(searchRadius);
        List<AreaEffectCloud> clouds = entity.level().getEntitiesOfClass(AreaEffectCloud.class, box,
                c -> c.getTags().contains(TAG_STEAM_CLOUD));

        if (clouds.isEmpty()) return;

        boolean isHighHeat = false;
        boolean isCondensing = false;

        for (AreaEffectCloud cloud : clouds) {
            // Precise distance check / 精确距离检查
            if (entity.distanceToSqr(cloud) > cloud.getRadius() * cloud.getRadius()) continue;
            
            // Aggro Clearing / 仇恨清除
            if (ElementalReactionConfig.steamClearAggro && entity instanceof Mob mob) {
                mob.setTarget(null);
            }

            if (cloud.getTags().contains(TAG_HIGH_HEAT)) isHighHeat = true;
            else isCondensing = true;
        }

        if (isHighHeat) {
            // Scalding Damage / 烫伤伤害
            float damage = (float) ElementalReactionConfig.steamScaldingDamage;
            ElementType type = ElementUtils.getElementType(entity);
            if (type == ElementType.FROST || type == ElementType.NATURE) {
                damage *= (float) ElementalReactionConfig.steamScaldingMultiplierWeakness;
            }

            if (damage > 0) {
                entity.invulnerableTime = 0; 
                entity.hurt(ModDamageTypes.source(entity.level(), ModDamageTypes.STEAM_SCALDING), damage);
            }

            removeWetness(entity);
        } else if (isCondensing) {
            // Condensation Effect / 冷凝效果
            int current = entity.getPersistentData().getInt(WetnessHandler.NBT_WETNESS);
            if (current < ElementalReactionConfig.wetnessMaxLevel) {
                entity.getPersistentData().putInt(WetnessHandler.NBT_WETNESS, current + 1);
            }
        }
    }

    // =================================================================================================
    //                                      Helper Methods
    //                                      辅助方法
    // =================================================================================================

    private static void applySelfDrying(LivingEntity attacker, LivingHurtEvent event, int currentWetness) {
        float penaltyRatio = (float) ElementalReactionConfig.wetnessSelfDryingDamagePenalty;
        event.setAmount(event.getAmount() * (1.0f - penaltyRatio));

        int fireEnhancement = ElementUtils.getDisplayEnhancement(attacker, ElementType.FIRE);
        int threshold = Math.max(1, ElementalReactionConfig.wetnessDryingThreshold);
        int layersToRemove = 1 + (fireEnhancement / threshold);

        int newLevel = Math.max(0, currentWetness - layersToRemove);
        
        if (newLevel == 0) removeWetness(attacker);
        else attacker.getPersistentData().putInt(WetnessHandler.NBT_WETNESS, newLevel);

        // Dynamic AV Feedback / 动态视听反馈
        if (attacker.level() instanceof ServerLevel sl) {
            int actualRemoved = Math.min(layersToRemove, currentWetness);
            int intensity = Math.min(actualRemoved, 5); 

            // Volume increases, Pitch decreases (deeper sound)
            // 音量增加，音调降低（更深沉）
            float volume = 0.5F + (intensity * 0.1F); 
            float pitch = 1.8F - (intensity * 0.2F) + (sl.random.nextFloat() * 0.2F);
            
            sl.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), 
                SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, volume, pitch);

            int particleCount = intensity * 8; 
            double speed = 0.05 + (intensity * 0.03);
            
            sl.sendParticles(ParticleTypes.CLOUD,
                attacker.getX(), attacker.getY() + attacker.getBbHeight() * 0.5, attacker.getZ(),
                particleCount, 0.3, 0.5, 0.3, speed);

            if (intensity >= 3) {
                sl.sendParticles(ParticleTypes.POOF,
                    attacker.getX(), attacker.getY() + attacker.getBbHeight() * 0.5, attacker.getZ(),
                    intensity * 2, 0.2, 0.2, 0.2, 0.05);
            }
        }
        
        ElementalCraft.LOGGER.info("[蒸汽日志] 触发自我干燥! 移除潮湿层数: {}", layersToRemove);
    }

    private static void removeWetness(LivingEntity entity) {
        entity.getPersistentData().remove(WetnessHandler.NBT_WETNESS);
        entity.getPersistentData().remove(WetnessHandler.NBT_RAIN_TIMER);
        entity.getPersistentData().remove(WetnessHandler.NBT_DECAY_TIMER);
        entity.removeEffect(com.xulai.elementalcraft.potion.ModMobEffects.WETNESS.get());
    }

    private static boolean checkImmunity(LivingEntity entity) {
        if (entity.fireImmune() || entity.hasEffect(MobEffects.FIRE_RESISTANCE)) return true;
        
        String id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        if (ElementalReactionConfig.cachedSteamBlacklist.contains(id)) return true;
        
        int resist = ElementUtils.getDisplayResistance(entity, ElementType.FIRE);
        return resist >= ElementalReactionConfig.steamImmunityThreshold;
    }

    /**
     * Spawn Dynamic Steam Cloud (v3.0)
     * 生成动态蒸汽云 (v3.0)
     *
     * Radius and duration scale with fuel level (wetness).
     * 半径和持续时间随燃料等级（潮湿）缩放。
     *
     * @param fuelLevel Wetness level / 潮湿层数
     */
    private static void spawnSteamCloud(LivingEntity target, boolean isHighHeat, int fuelLevel) {
        if (!(target.level() instanceof ServerLevel serverLevel)) return;
        
        // Cap level at 5 / 限制最大等级为 5
        int level = Math.max(1, Math.min(fuelLevel, 5));
        
        // Radius: 1.0 + level -> L1=2.0, L5=6.0 (Based on Config default 2.0 as base for L1 logic if we follow strict design, 
        // but here we implement: ConfigBase + (Level-1))
        // 半径：配置基数 + (等级 - 1)
        float baseRadius = (float) ElementalReactionConfig.steamCloudRadius; 
        float radius = baseRadius + (level - 1.0f); 
        
        // Duration: Level * 2s (40 ticks) -> L1=2s, L5=10s
        // 持续时间：等级 * 2秒
        int duration = level * 40;
        if (!isHighHeat) duration /= 2; // Halve for Low Heat / 低温减半

        AreaEffectCloud cloud = new AreaEffectCloud(serverLevel, target.getX(), target.getY(), target.getZ());
        cloud.setRadius(radius);
        cloud.setRadiusOnUse(0F);
        // Dynamic shrinking to reach 0 at end of duration
        // 动态收缩：确保结束时半径归零
        cloud.setRadiusPerTick(-(radius / (float) duration));
        cloud.setDuration(duration);
        cloud.setParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AIR.defaultBlockState()));
        cloud.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ElementalReactionConfig.steamBlindnessDuration));
        
        cloud.addTag(TAG_STEAM_CLOUD);
        if (isHighHeat) cloud.addTag(TAG_HIGH_HEAT);
        
        serverLevel.addFreshEntity(cloud);
        
        spawnDynamicFX(serverLevel, target, radius, level, isHighHeat);
    }
    
    private static void spawnDynamicFX(ServerLevel level, LivingEntity target, float radius, int intensity, boolean isHighHeat) {
        Random random = new Random();
        
        // Dynamic Sound: Higher intensity = Lower pitch (Deeper)
        // 动态音效：强度越高，音调越低（沉闷）
        float volume = isHighHeat ? (0.8F + intensity * 0.1F) : 0.6F;
        float pitch = isHighHeat ? (1.8F - intensity * 0.2F) : 1.0F; 
        
        level.playSound(null, target.getX(), target.getY(), target.getZ(), 
            SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, volume, pitch);

        // Dynamic Particles / 动态粒子
        int count = (int) (radius * (isHighHeat ? 20 : 10) * intensity); 
        double speed = isHighHeat ? (0.05 + intensity * 0.02) : 0.05;

        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = Math.sqrt(random.nextDouble()) * radius;
            double x = target.getX() + Math.cos(angle) * dist;
            double z = target.getZ() + Math.sin(angle) * dist;
            double y = target.getY() + random.nextDouble() * target.getBbHeight();
            
            if (isHighHeat) {
                // Main: White Smoke / 主粒子：白烟
                level.sendParticles(ParticleTypes.CLOUD, x, y, z, 0, 0, 0.1, 0, speed);
                
                // High Intensity: Explosion / 高强度：爆炸烟雾
                if (intensity >= 3 && random.nextFloat() < 0.2f) {
                    level.sendParticles(ParticleTypes.POOF, x, y, z, 0, 0, 0, 0, speed);
                }
            } else {
                level.sendParticles(ParticleTypes.CLOUD, x, y, z, 0, 0, 0.05, 0, speed * 0.5);
            }
        }
    }
}