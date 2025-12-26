// src/main/java/com/xulai/elementalcraft/event/SteamReactionHandler.java
package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalReactionConfig;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
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
 * 实现了 v3.0 动态蒸汽云：
 * - 每一层潮湿都会触发，半径/持续时间/伤害均随层数动态缩放（读取配置）。
 * - 视觉特效使用 LevelTickEvent 维护的上升篝火烟雾，模拟真实蒸汽。
 * - 音效根据层数分级处理。
 *
 * English description:
 * Core logic class for Steam Cloud elemental reactions.
 * Includes trigger logic (Dual-Track + Strict Consistency), defense calculation (Layered Defense), and persistent environmental effects.
 * Implements v3.0 Dynamic Steam Cloud:
 * - Triggered by every wetness level; Radius/Duration/Damage scale dynamically with level (Configurable).
 * - Visual effects use rising campfire smoke maintained by LevelTickEvent, simulating realistic steam.
 * - Sound effects are tiered based on level.
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class SteamReactionHandler {

    public static final String TAG_STEAM_CLOUD = "EC_SteamCloud";
    public static final String TAG_HIGH_HEAT = "EC_HighHeat";
    public static final String TAG_LEVEL_PREFIX = "EC_Level_";

    /**
     * Trigger Logic Listener / 触发逻辑监听器
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!ElementalReactionConfig.steamReactionEnabled) return;
        
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            processTriggerLogic(event, attacker, event.getEntity());
        }
    }

    /**
     * Defense Logic Listener / 防御逻辑监听器
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getSource().is(ModDamageTypes.STEAM_SCALDING)) {
            processDefenseLogic(event);
        }
    }

    /**
     * Tick Logic Listener / 持续效果监听器
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!ElementalReactionConfig.steamReactionEnabled) return;
        processCloudEffects(event.getEntity());
    }

    /**
     * Level Tick Listener (Visual Effects) / 级别 Tick 监听器 (视觉特效)
     */
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide) return;
        
        if (event.level instanceof ServerLevel serverLevel) {
            Iterable<Entity> entities = serverLevel.getAllEntities();
            
            for (Entity entity : entities) {
                if (entity instanceof AreaEffectCloud cloud) {
                    if (cloud.getTags().contains(TAG_STEAM_CLOUD)) {
                        spawnRisingSteamParticles(serverLevel, cloud);
                    }
                }
            }
        }
    }

    // =================================================================================================
    //                                      Logic Implementation
    // =================================================================================================

    private static void processTriggerLogic(LivingHurtEvent event, LivingEntity attacker, LivingEntity target) {
        ElementType attackElement = ElementUtils.getConsistentAttackElement(attacker);
        
        if (event.getSource().is(DamageTypeTags.IS_FIRE)) attackElement = ElementType.FIRE;
        if (event.getSource().is(DamageTypeTags.IS_FREEZING)) attackElement = ElementType.FROST;

        int firePower = ElementUtils.getDisplayEnhancement(attacker, ElementType.FIRE);
        int frostPower = ElementUtils.getDisplayEnhancement(attacker, ElementType.FROST);
        
        boolean targetIsWet = target.getPersistentData().getInt(WetnessHandler.NBT_WETNESS) > 0;
        int targetWetness = target.getPersistentData().getInt(WetnessHandler.NBT_WETNESS);
        ElementType targetElement = ElementUtils.getElementType(target);

        if (attackElement == ElementType.FIRE) {
            int attackerWetness = attacker.getPersistentData().getInt(WetnessHandler.NBT_WETNESS);
            if (attackerWetness > 0) {
                ElementalCraft.LOGGER.info("[蒸汽调试] 检测到攻击者潮湿，准备自我干燥。当前层数: {}", attackerWetness);
                applySelfDrying(attacker, event, attackerWetness);
                return;
            }

            if (targetIsWet || targetElement == ElementType.FROST) {
                int threshold = ElementalReactionConfig.steamTriggerThresholdFire;
                
                if (firePower >= threshold) {
                    ElementalCraft.LOGGER.info("[蒸汽调试] 赤焰触发成功! 强化点: {} >= 阈值: {}", firePower, threshold);
                    
                    int fuelLevel = targetIsWet ? targetWetness : 1;
                    spawnSteamCloud(target, true, fuelLevel); 
                    
                    if (targetIsWet) {
                        ElementalCraft.LOGGER.info("[蒸汽调试] 移除目标潮湿状态");
                        removeWetness(target);
                    }
                } 
                else {
                    if (targetIsWet) {
                        ElementalCraft.LOGGER.info("[蒸汽调试] 赤焰未达标，仅执行烘干");
                        removeWetness(target);
                    }
                }
            }
        }
        else if (attackElement == ElementType.FROST) {
            if (targetElement == ElementType.FIRE) {
                int threshold = ElementalReactionConfig.steamTriggerThresholdFrost;
                if (frostPower >= threshold) {
                    ElementalCraft.LOGGER.info("[蒸汽调试] 冰霜触发成功! 强化点: {} >= 阈值: {}", frostPower, threshold);
                    spawnSteamCloud(target, false, 3); // 默认3层
                } else {
                    ElementalCraft.LOGGER.info("[蒸汽调试] 冰霜未达标");
                }
            }
        }
    }

    private static void processDefenseLogic(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        float rawDamage = event.getAmount();
        
        ElementalCraft.LOGGER.info("[蒸汽调试] 开始防御计算: 原始伤害={}", rawDamage);

        if (checkImmunity(target)) {
            ElementalCraft.LOGGER.info("[蒸汽调试] 目标免疫蒸汽伤害");
            event.setAmount(0);
            event.setCanceled(true);
            return;
        }

        int fireProtLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.FIRE_PROTECTION, target);
        int protLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, target);

        double fireProtReduction = Math.min(fireProtLevel * 0.08, ElementalReactionConfig.steamMaxFireProtCap);
        double protReduction = Math.min(protLevel * 0.04, ElementalReactionConfig.steamMaxGeneralProtCap);
        
        double totalReduction = Math.min(fireProtReduction + protReduction, 0.8);
        float reducedDamage = rawDamage * (float)(1.0 - totalReduction);

        ElementalCraft.LOGGER.info("[蒸汽调试] 附魔减伤: 火保={}, 普保={}, 总减免={}, 最终伤害={}", fireProtReduction, protReduction, totalReduction, reducedDamage);

        ElementType type = ElementUtils.getElementType(target);
        if (type == ElementType.FROST || type == ElementType.NATURE) {
            float floorRatio = (float) ElementalReactionConfig.steamDamageFloorRatio;
            float floorLimit = rawDamage * floorRatio;
            
            if (reducedDamage < floorLimit) {
                ElementalCraft.LOGGER.info("[蒸汽调试] 触发易伤保底! 修正: {} -> {}", reducedDamage, floorLimit);
                reducedDamage = floorLimit;
            }
        }
        
        event.setAmount(reducedDamage);
    }

    private static void processCloudEffects(LivingEntity entity) {
        if (entity.level().isClientSide) return;

        double searchRadius = ElementalReactionConfig.steamCloudRadius * 3.0; // 搜索范围基于配置放大
        AABB box = entity.getBoundingBox().inflate(searchRadius);
        List<AreaEffectCloud> clouds = entity.level().getEntitiesOfClass(AreaEffectCloud.class, box,
                c -> c.getTags().contains(TAG_STEAM_CLOUD));

        if (clouds.isEmpty()) return;

        boolean isHighHeat = false;
        boolean isCondensing = false;
        int cloudLevel = 1;

        for (AreaEffectCloud cloud : clouds) {
            if (entity.distanceToSqr(cloud) > cloud.getRadius() * cloud.getRadius()) continue;
            
            if (ElementalReactionConfig.steamClearAggro && entity instanceof Mob mob) {
                mob.setTarget(null);
            }

            if (cloud.getTags().contains(TAG_HIGH_HEAT)) {
                isHighHeat = true;
                // 从 Tag 获取层数
                for(String tag : cloud.getTags()) {
                    if(tag.startsWith(TAG_LEVEL_PREFIX)) {
                        try {
                            cloudLevel = Integer.parseInt(tag.replace(TAG_LEVEL_PREFIX, ""));
                        } catch (NumberFormatException ignored) {}
                        break;
                    }
                }
            }
            else isCondensing = true;
        }

        if (isHighHeat) {
            if (entity.tickCount % 20 == 0) {
                // 动态伤害计算：基础伤害 * (1 + 层数 * 每层倍率)
                float baseDamage = (float) ElementalReactionConfig.steamScaldingDamage;
                float scale = (float) ElementalReactionConfig.steamDamageScalePerLevel;
                float levelMultiplier = 1.0f + (cloudLevel * scale);
                float damage = baseDamage * levelMultiplier;

                ElementType type = ElementUtils.getElementType(entity);
                if (type == ElementType.FROST || type == ElementType.NATURE) {
                    double weaknessMult = ElementalReactionConfig.steamScaldingMultiplierWeakness;
                    damage *= (float) weaknessMult;
                    ElementalCraft.LOGGER.info("[蒸汽调试] 区域伤害(Lv{}): 目标易伤, 基础={}, 最终={}", cloudLevel, baseDamage, damage);
                } else {
                    ElementalCraft.LOGGER.info("[蒸汽调试] 区域伤害(Lv{}): 基础={}, 最终={}", cloudLevel, baseDamage, damage);
                }

                if (damage > 0) {
                    entity.invulnerableTime = 0; 
                    entity.hurt(ModDamageTypes.source(entity.level(), ModDamageTypes.STEAM_SCALDING), damage);
                    
                    if (entity.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(), 1, 0.2, 0.2, 0.2, 0.01);
                    }
                }

                if (entity.getPersistentData().getInt(WetnessHandler.NBT_WETNESS) > 0) {
                    removeWetness(entity);
                }
            }
        } 
        else if (isCondensing) {
            int interval = Math.max(1, ElementalReactionConfig.steamCondensationInterval);
            if (entity.tickCount % interval == 0) {
                int current = entity.getPersistentData().getInt(WetnessHandler.NBT_WETNESS);
                int max = ElementalReactionConfig.wetnessMaxLevel;
                if (current < max) {
                    entity.getPersistentData().putInt(WetnessHandler.NBT_WETNESS, current + 1);
                    ElementalCraft.LOGGER.info("[蒸汽调试] 区域效果: 低温冷凝，潮湿层数 {} -> {}", current, current + 1);
                }
            }
        }
    }

    // =================================================================================================
    //                                      Helper Methods
    // =================================================================================================

    private static void applySelfDrying(LivingEntity attacker, LivingHurtEvent event, int currentWetness) {
        float penaltyRatio = (float) ElementalReactionConfig.wetnessSelfDryingDamagePenalty;
        event.setAmount(event.getAmount() * (1.0f - penaltyRatio));

        int fireEnhancement = ElementUtils.getDisplayEnhancement(attacker, ElementType.FIRE);
        int threshold = Math.max(1, ElementalReactionConfig.wetnessDryingThreshold);
        int layersToRemove = 1 + (fireEnhancement / threshold);

        int newLevel = Math.max(0, currentWetness - layersToRemove);
        
        ElementalCraft.LOGGER.info("[蒸汽调试] 自我干燥执行: 强化={}, 计划移除={}, {} -> {}", fireEnhancement, layersToRemove, currentWetness, newLevel);

        if (newLevel == 0) removeWetness(attacker);
        else attacker.getPersistentData().putInt(WetnessHandler.NBT_WETNESS, newLevel);

        playSteamBurstEffect((ServerLevel)attacker.level(), attacker, 0.5f, Math.min(layersToRemove, 5), true);
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
        int threshold = ElementalReactionConfig.steamImmunityThreshold;
        boolean result = resist >= threshold;
        
        if (result) {
            ElementalCraft.LOGGER.info("[蒸汽调试] 免疫判定: 抗性点数 {} >= 阈值(配置) {}", resist, threshold);
        }
        return result;
    }

    /**
     * 生成动态蒸汽云 (v3.0)
     */
    private static void spawnSteamCloud(LivingEntity target, boolean isHighHeat, int fuelLevel) {
        if (!(target.level() instanceof ServerLevel serverLevel)) return;
        
        // 限制最大层数
        int level = Math.max(1, Math.min(fuelLevel, 5));
        
        // 动态半径：基数 + (层数-1)*每层增量
        float baseRadius = (float) ElementalReactionConfig.steamCloudRadius; 
        float radiusInc = (float) ElementalReactionConfig.steamRadiusPerLevel;
        float radius = baseRadius + (level - 1.0f) * radiusInc; 
        
        // 动态时间：基数 + (层数*每层增量)
        int baseDuration = ElementalReactionConfig.steamCloudDuration;
        int durationInc = ElementalReactionConfig.steamDurationPerLevel;
        int duration = baseDuration + (level * durationInc);
        
        if (!isHighHeat) duration /= 2;

        ElementalCraft.LOGGER.info("[蒸汽调试] 生成蒸汽云: 类型={}, 层数={}, 半径={}, 持续={}", isHighHeat ? "高温" : "低温", level, radius, duration);

        AreaEffectCloud cloud = new AreaEffectCloud(serverLevel, target.getX(), target.getY(), target.getZ());
        cloud.setRadius(radius);
        cloud.setRadiusOnUse(0F);
        cloud.setRadiusPerTick(0F); // 保持最大范围直到消失
        cloud.setDuration(duration);
        
        // 隐藏原版粒子
        cloud.setParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AIR.defaultBlockState()));
        
        cloud.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ElementalReactionConfig.steamBlindnessDuration));
        
        cloud.addTag(TAG_STEAM_CLOUD);
        if (isHighHeat) {
            cloud.addTag(TAG_HIGH_HEAT);
            cloud.addTag(TAG_LEVEL_PREFIX + level);
        }
        
        serverLevel.addFreshEntity(cloud);
        
        // 初始爆发特效
        playSteamBurstEffect(serverLevel, target, radius, level, isHighHeat);
    }
    
    /**
     * 持续生成上升的蒸汽粒子 (绑定 Level Tick)
     */
    private static void spawnRisingSteamParticles(ServerLevel level, AreaEffectCloud cloud) {
        boolean isHighHeat = cloud.getTags().contains(TAG_HIGH_HEAT);
        float radius = cloud.getRadius();
        if (radius < 0.2f) return;

        Random random = new Random();
        int count = Math.max(1, (int)(radius * 0.8)); // 降低密度

        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = Math.sqrt(random.nextDouble()) * radius;
            double x = cloud.getX() + Math.cos(angle) * dist;
            double z = cloud.getZ() + Math.sin(angle) * dist;
            double y = cloud.getY();

            // 模拟热气上升
            double upSpeed = 0.05 + random.nextDouble() * 0.08;

            // 使用白色篝火烟雾
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0, 0, upSpeed, 0, 1.0);

            if (isHighHeat) {
                // 仅混合火焰和熔岩，无黑烟
                if (random.nextFloat() < 0.1f) {
                    level.sendParticles(ParticleTypes.FLAME, x, y, z, 0, 0, upSpeed * 0.8, 0, 0.5);
                }
                if (random.nextFloat() < 0.05f) {
                    level.sendParticles(ParticleTypes.LAVA, x, y, z, 0, 0, 0, 0, 0);
                }
            }
        }
    }
    
    /**
     * 播放一次性的蒸汽爆发特效
     */
    private static void playSteamBurstEffect(ServerLevel level, LivingEntity target, float radius, int intensity, boolean isHighHeat) {
        Random random = new Random();
        
        float volume = isHighHeat ? 0.8F : 0.6F;
        float pitch = isHighHeat ? 1.0F : 1.2F; 
        
        // 音效分级：层数越高，声音越低沉震撼
        if (isHighHeat && intensity >= 3) {
            level.playSound(null, target.getX(), target.getY(), target.getZ(), 
                SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, volume, 0.8F);
        } else {
            level.playSound(null, target.getX(), target.getY(), target.getZ(), 
                SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, volume, pitch);
        }

        int count = (int) (Math.max(1.0, radius) * (isHighHeat ? 20 : 10) * intensity); 
        double speed = isHighHeat ? (0.05 + intensity * 0.02) : 0.05;

        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = Math.sqrt(random.nextDouble()) * radius;
            double x = target.getX() + Math.cos(angle) * dist;
            double z = target.getZ() + Math.sin(angle) * dist;
            double y = target.getY() + random.nextDouble() * target.getBbHeight() + 0.2;
            
            if (isHighHeat) {
                // 高温爆发：白色烟雾 + 火焰 + 熔岩
                if (random.nextFloat() < 0.2f) level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0, 0, 0.1, 0, speed);
                if (random.nextFloat() < 0.3f) level.sendParticles(ParticleTypes.FLAME, x, y, z, 0, 0, 0.05, 0, speed * 0.5);
                if (random.nextFloat() < 0.1f) level.sendParticles(ParticleTypes.LAVA, x, y, z, 0, 0, 0, 0, 0);

                if (intensity >= 3 && random.nextFloat() < 0.1f) {
                    level.sendParticles(ParticleTypes.POOF, x, y, z, 0, 0, 0, 0, speed * 1.5);
                }
            } else {
                if (random.nextBoolean()) {
                    level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0, 0, 0.05, 0, speed * 0.5);
                }
            }
        }
    }
}