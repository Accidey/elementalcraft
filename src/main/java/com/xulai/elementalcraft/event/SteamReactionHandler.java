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
 * <p>
 * 中文说明：
 * 处理蒸汽云元素反应的核心逻辑类。
 * 包含触发判定（双轨制：赤焰vs潮湿/冰霜，冰霜vs赤焰）、防御计算（分层防御）和持续环境效果。
 * 实现了动态蒸汽云机制：
 * 1. 触发后生成 AreaEffectCloud，其半径、持续时间和伤害随触发源的属性强度及目标潮湿层数动态缩放。
 * 2. 视觉特效使用服务端 Tick 维护的上升烟雾粒子，模拟真实蒸汽。
 * 3. 包含对服务器性能的优化（限制 Tick 频率）。
 * <p>
 * English Description:
 * Core logic class for Steam Cloud elemental reactions.
 * Includes trigger logic (Dual-Track: Fire vs Wet/Frost, Frost vs Fire), defense calculation (Layered Defense), and persistent environmental effects.
 * Implements dynamic Steam Cloud mechanism:
 * 1. Generates AreaEffectCloud upon trigger; Radius, Duration, and Damage scale dynamically based on source attribute strength and target wetness level.
 * 2. Visual effects use rising smoke particles maintained by Server Tick to simulate realistic steam.
 * 3. Includes server performance optimizations (Tick frequency limiting).
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class SteamReactionHandler {

    public static final String TAG_STEAM_CLOUD = "EC_SteamCloud";
    public static final String TAG_HIGH_HEAT = "EC_HighHeat";
    public static final String TAG_LEVEL_PREFIX = "EC_Level_";

    /**
     * 触发逻辑监听器。
     * 监听实体受伤事件，判断是否满足蒸汽反应的触发条件（如火打水、冰打火）。
     * <p>
     * Trigger Logic Listener.
     * Listens to LivingHurtEvent to determine if conditions for steam reaction are met (e.g., Fire hitting Water, Frost hitting Fire).
     *
     * @param event 受伤事件 / Hurt event
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!ElementalReactionConfig.steamReactionEnabled) return;

        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            processTriggerLogic(event, attacker, event.getEntity());
        }
    }

    /**
     * 防御逻辑监听器。
     * 监听实体受到蒸汽烫伤的事件，计算减伤（抗性、附魔）和保底伤害。
     * <p>
     * Defense Logic Listener.
     * Listens to LivingDamageEvent for steam scalding, calculating damage reduction (Resistance, Enchantments) and floor damage.
     *
     * @param event 伤害事件 / Damage event
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getSource().is(ModDamageTypes.STEAM_SCALDING)) {
            processDefenseLogic(event);
        }
    }

    /**
     * 持续效果监听器。
     * 监听生物 Tick 事件，检测生物是否处于蒸汽云中并应用持续效果。
     * <p>
     * Tick Logic Listener.
     * Listens to LivingTickEvent to check if entities are inside steam clouds and apply persistent effects.
     *
     * @param event 生物 Tick 事件 / Living Tick event
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!ElementalReactionConfig.steamReactionEnabled) return;

        // 性能优化：每 10 tick (0.5秒) 检测一次，避免每 tick 都进行 AABB 搜索
        // Performance optimization: Check every 10 ticks (0.5s) to avoid AABB search every tick
        if (event.getEntity().tickCount % 10 != 0) return;

        processCloudEffects(event.getEntity());
    }

    /**
     * 级别 Tick 监听器 (视觉特效)。
     * 在服务端维护蒸汽云的粒子效果，使其看起来像是在持续上升。
     * <p>
     * Level Tick Listener (Visual Effects).
     * Maintains steam cloud particle effects on the server side to make them look like they are continuously rising.
     *
     * @param event 级别 Tick 事件 / Level Tick event
     */
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide) return;

        // 性能优化：每 20 tick (1秒) 执行一次全服实体遍历，防止 TPS 下降
        // Performance optimization: Execute global entity iteration every 20 ticks (1s) to prevent TPS drop
        if (event.level.getGameTime() % 20 != 0) return;

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
    //                              Logic Implementation / 逻辑实现
    // =================================================================================================

    /**
     * 处理蒸汽反应的触发逻辑。
     * 判断攻击元素与目标状态，决定是否生成蒸汽云或执行自我干燥。
     * <p>
     * Processes the trigger logic for steam reactions.
     * Determines whether to spawn a steam cloud or perform self-drying based on attack element and target state.
     *
     * @param event    受伤事件 / Hurt event
     * @param attacker 攻击者 / Attacker
     * @param target   目标 / Target
     */
    private static void processTriggerLogic(LivingHurtEvent event, LivingEntity attacker, LivingEntity target) {
        ElementType attackElement = ElementUtils.getConsistentAttackElement(attacker);

        // 兼容原版伤害类型标记
        // Compatible with vanilla damage type tags
        if (event.getSource().is(DamageTypeTags.IS_FIRE)) attackElement = ElementType.FIRE;
        if (event.getSource().is(DamageTypeTags.IS_FREEZING)) attackElement = ElementType.FROST;

        int firePower = ElementUtils.getDisplayEnhancement(attacker, ElementType.FIRE);
        int frostPower = ElementUtils.getDisplayEnhancement(attacker, ElementType.FROST);

        boolean targetIsWet = target.getPersistentData().getInt(WetnessHandler.NBT_WETNESS) > 0;
        int targetWetness = target.getPersistentData().getInt(WetnessHandler.NBT_WETNESS);
        ElementType targetElement = ElementUtils.getElementType(target);

        if (attackElement == ElementType.FIRE) {
            // 检查攻击者自身是否潮湿，若是则优先执行自我干燥
            // Check if the attacker is wet; if so, prioritize self-drying
            int attackerWetness = attacker.getPersistentData().getInt(WetnessHandler.NBT_WETNESS);
            if (attackerWetness > 0) {
                applySelfDrying(attacker, event, attackerWetness);
                return;
            }

            // 赤焰攻击潮湿目标或冰霜生物
            // Fire attacks Wet target or Frost mob
            if (targetIsWet || targetElement == ElementType.FROST) {
                int threshold = ElementalReactionConfig.steamTriggerThresholdFire;

                if (firePower >= threshold) {
                    // 触发成功：生成高温蒸汽
                    // Trigger success: Spawn high-heat steam
                    int fuelLevel = targetIsWet ? targetWetness : 1;
                    spawnSteamCloud(target, true, fuelLevel);

                    if (targetIsWet) {
                        removeWetness(target);
                    }
                } else {
                    // 未达标：仅移除潮湿（烘干）
                    // Threshold not met: Only remove wetness (Dry out)
                    if (targetIsWet) {
                        removeWetness(target);
                    }
                }
            }
        } else if (attackElement == ElementType.FROST) {
            // 冰霜攻击赤焰生物
            // Frost attacks Fire mob
            if (targetElement == ElementType.FIRE) {
                int threshold = ElementalReactionConfig.steamTriggerThresholdFrost;
                if (frostPower >= threshold) {
                    // 触发成功：生成低温蒸汽（默认3层规模）
                    // Trigger success: Spawn low-heat steam (Default scale level 3)
                    spawnSteamCloud(target, false, 3);
                }
            }
        }
    }

    /**
     * 处理防御逻辑。
     * 计算抗性、附魔减伤及易伤保底机制。
     * <p>
     * Processes defense logic.
     * Calculates resistance, enchantment damage reduction, and vulnerability floor mechanisms.
     *
     * @param event 伤害事件 / Damage event
     */
    private static void processDefenseLogic(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        float rawDamage = event.getAmount();

        // 1. 免疫判定
        // 1. Immunity Check
        if (checkImmunity(target)) {
            event.setAmount(0);
            event.setCanceled(true);
            return;
        }

        // 2. 附魔减伤计算 (火焰保护 + 普通保护)
        // 2. Enchantment Reduction Calculation (Fire Protection + Protection)
        int fireProtLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.FIRE_PROTECTION, target);
        int protLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, target);

        double fireProtReduction = Math.min(fireProtLevel * 0.08, ElementalReactionConfig.steamMaxFireProtCap);
        double protReduction = Math.min(protLevel * 0.04, ElementalReactionConfig.steamMaxGeneralProtCap);

        double totalReduction = Math.min(fireProtReduction + protReduction, 0.8);
        float reducedDamage = rawDamage * (float) (1.0 - totalReduction);

        // 3. 易伤保底机制 (冰霜/自然生物受到的伤害不能被减免得太低)
        // 3. Vulnerability Floor Mechanism (Frost/Nature mobs cannot reduce damage too much)
        ElementType type = ElementUtils.getElementType(target);
        if (type == ElementType.FROST || type == ElementType.NATURE) {
            float floorRatio = (float) ElementalReactionConfig.steamDamageFloorRatio;
            float floorLimit = rawDamage * floorRatio;

            if (reducedDamage < floorLimit) {
                reducedDamage = floorLimit;
            }
        }

        event.setAmount(reducedDamage);
    }

    /**
     * 处理蒸汽云对内部实体的持续效果。
     * 包括高温烫伤、低温冷凝（增加潮湿）等。
     * <p>
     * Processes persistent effects of steam clouds on entities inside.
     * Includes high-heat scalding and low-heat condensation (increasing wetness).
     *
     * @param entity 目标实体 / Target entity
     */
    private static void processCloudEffects(LivingEntity entity) {
        if (entity.level().isClientSide) return;

        double searchRadius = ElementalReactionConfig.steamCloudRadius * 3.0; // 搜索范围基于配置放大 / Expand search radius based on config
        AABB box = entity.getBoundingBox().inflate(searchRadius);
        List<AreaEffectCloud> clouds = entity.level().getEntitiesOfClass(AreaEffectCloud.class, box,
                c -> c.getTags().contains(TAG_STEAM_CLOUD));

        if (clouds.isEmpty()) return;

        boolean isHighHeat = false;
        boolean isCondensing = false;
        int cloudLevel = 1;

        // 遍历所有云，判断实体是否在半径内，并确定最高优先级的效果
        // Iterate all clouds, check if entity is within radius, and determine highest priority effect
        for (AreaEffectCloud cloud : clouds) {
            if (entity.distanceToSqr(cloud) > cloud.getRadius() * cloud.getRadius()) continue;

            if (ElementalReactionConfig.steamClearAggro && entity instanceof Mob mob) {
                mob.setTarget(null);
            }

            if (cloud.getTags().contains(TAG_HIGH_HEAT)) {
                isHighHeat = true;
                // 从 Tag 获取层数
                // Get level from Tag
                for (String tag : cloud.getTags()) {
                    if (tag.startsWith(TAG_LEVEL_PREFIX)) {
                        try {
                            cloudLevel = Integer.parseInt(tag.replace(TAG_LEVEL_PREFIX, ""));
                        } catch (NumberFormatException ignored) {
                        }
                        break;
                    }
                }
            } else isCondensing = true;
        }

        if (isHighHeat) {
            // 控制伤害频率为每秒一次 (20 ticks)
            // Control damage frequency to once per second (20 ticks)
            if (entity.tickCount % 20 == 0) {
                // 动态伤害计算：基础伤害 * (1 + 层数 * 每层倍率)
                // Dynamic Damage Calculation: Base * (1 + Level * Scale)
                float baseDamage = (float) ElementalReactionConfig.steamScaldingDamage;
                float scale = (float) ElementalReactionConfig.steamDamageScalePerLevel;
                float levelMultiplier = 1.0f + (cloudLevel * scale);
                float damage = baseDamage * levelMultiplier;

                ElementType type = ElementUtils.getElementType(entity);
                if (type == ElementType.FROST || type == ElementType.NATURE) {
                    double weaknessMult = ElementalReactionConfig.steamScaldingMultiplierWeakness;
                    damage *= (float) weaknessMult;
                }

                if (damage > 0) {
                    entity.invulnerableTime = 0;
                    entity.hurt(ModDamageTypes.source(entity.level(), ModDamageTypes.STEAM_SCALDING), damage);

                    if (entity.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(), 1, 0.2, 0.2, 0.2, 0.01);
                    }
                }

                // 高温会移除潮湿
                // High heat removes wetness
                if (entity.getPersistentData().getInt(WetnessHandler.NBT_WETNESS) > 0) {
                    removeWetness(entity);
                }
            }
        } else if (isCondensing) {
            // 低温冷凝：增加潮湿层数
            // Low-heat Condensation: Increase wetness level
            int interval = Math.max(1, ElementalReactionConfig.steamCondensationInterval);
            if (entity.tickCount % interval == 0) {
                int current = entity.getPersistentData().getInt(WetnessHandler.NBT_WETNESS);
                int max = ElementalReactionConfig.wetnessMaxLevel;
                if (current < max) {
                    entity.getPersistentData().putInt(WetnessHandler.NBT_WETNESS, current + 1);
                }
            }
        }
    }

    // =================================================================================================
    //                              Helper Methods / 辅助方法
    // =================================================================================================

    /**
     * 执行攻击者的自我干燥逻辑。
     * 消耗一定的伤害输出，移除自身的潮湿状态。
     * <p>
     * Executes self-drying logic for the attacker.
     * Consumes a portion of damage output to remove own wetness.
     *
     * @param attacker       攻击者 / Attacker
     * @param event          受伤事件（用于减少伤害） / Hurt event (to reduce damage)
     * @param currentWetness 当前潮湿层数 / Current wetness level
     */
    private static void applySelfDrying(LivingEntity attacker, LivingHurtEvent event, int currentWetness) {
        float penaltyRatio = (float) ElementalReactionConfig.wetnessSelfDryingDamagePenalty;
        event.setAmount(event.getAmount() * (1.0f - penaltyRatio));

        int fireEnhancement = ElementUtils.getDisplayEnhancement(attacker, ElementType.FIRE);
        int threshold = Math.max(1, ElementalReactionConfig.wetnessDryingThreshold);
        int layersToRemove = 1 + (fireEnhancement / threshold);

        int newLevel = Math.max(0, currentWetness - layersToRemove);

        if (newLevel == 0) removeWetness(attacker);
        else attacker.getPersistentData().putInt(WetnessHandler.NBT_WETNESS, newLevel);

        playSteamBurstEffect((ServerLevel) attacker.level(), attacker, 0.5f, Math.min(layersToRemove, 5), true);
    }

    /**
     * 移除实体的所有潮湿相关数据。
     * <p>
     * Removes all wetness-related data from the entity.
     *
     * @param entity 实体 / Entity
     */
    private static void removeWetness(LivingEntity entity) {
        entity.getPersistentData().remove(WetnessHandler.NBT_WETNESS);
        entity.getPersistentData().remove(WetnessHandler.NBT_RAIN_TIMER);
        entity.getPersistentData().remove(WetnessHandler.NBT_DECAY_TIMER);
        entity.removeEffect(com.xulai.elementalcraft.potion.ModMobEffects.WETNESS.get());
    }

    /**
     * 检查实体是否免疫蒸汽伤害。
     * 基于：原版火焰免疫、抗火药水、黑名单配置、以及赤焰抗性点数。
     * <p>
     * Checks if the entity is immune to steam damage.
     * Based on: Vanilla fire immunity, Fire Resistance potion, config blacklist, and Fire Resistance points.
     *
     * @param entity 实体 / Entity
     * @return 是否免疫 / Whether immune
     */
    private static boolean checkImmunity(LivingEntity entity) {
        if (entity.fireImmune() || entity.hasEffect(MobEffects.FIRE_RESISTANCE)) return true;

        String id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        if (ElementalReactionConfig.cachedSteamBlacklist.contains(id)) return true;

        int resist = ElementUtils.getDisplayResistance(entity, ElementType.FIRE);
        int threshold = ElementalReactionConfig.steamImmunityThreshold;
        return resist >= threshold;
    }

    /**
     * 生成动态蒸汽云实体。
     * 设置云的半径、持续时间、粒子效果以及相关 Tag。
     * <p>
     * Spawns a dynamic steam cloud entity.
     * Sets radius, duration, particles, and related tags.
     *
     * @param target      目标实体 / Target entity
     * @param isHighHeat  是否为高温蒸汽 / Whether it is high-heat steam
     * @param fuelLevel   燃料等级（通常为目标潮湿层数） / Fuel level (usually target wetness level)
     */
    private static void spawnSteamCloud(LivingEntity target, boolean isHighHeat, int fuelLevel) {
        if (!(target.level() instanceof ServerLevel serverLevel)) return;

        // 限制最大层数
        // Cap max level
        int level = Math.max(1, Math.min(fuelLevel, 5));

        // 动态半径计算
        // Dynamic Radius Calculation
        float baseRadius = (float) ElementalReactionConfig.steamCloudRadius;
        float radiusInc = (float) ElementalReactionConfig.steamRadiusPerLevel;
        float radius = baseRadius + (level - 1.0f) * radiusInc;

        // 动态时间计算
        // Dynamic Duration Calculation
        int baseDuration = ElementalReactionConfig.steamCloudDuration;
        int durationInc = ElementalReactionConfig.steamDurationPerLevel;
        int duration = baseDuration + (level * durationInc);

        if (!isHighHeat) duration /= 2;

        AreaEffectCloud cloud = new AreaEffectCloud(serverLevel, target.getX(), target.getY(), target.getZ());
        cloud.setRadius(radius);
        cloud.setRadiusOnUse(0F);
        cloud.setRadiusPerTick(0F); // 保持最大范围直到消失 / Keep max radius until vanish
        cloud.setDuration(duration);

        // 隐藏原版粒子，使用自定义粒子逻辑
        // Hide vanilla particles, use custom particle logic
        cloud.setParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AIR.defaultBlockState()));

        cloud.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ElementalReactionConfig.steamBlindnessDuration));

        cloud.addTag(TAG_STEAM_CLOUD);
        if (isHighHeat) {
            cloud.addTag(TAG_HIGH_HEAT);
            cloud.addTag(TAG_LEVEL_PREFIX + level);
        }

        serverLevel.addFreshEntity(cloud);

        // 播放初始爆发特效
        // Play initial burst FX
        playSteamBurstEffect(serverLevel, target, radius, level, isHighHeat);
    }

    /**
     * 持续生成上升的蒸汽粒子。
     * 由 Level Tick 事件调用，模拟热气上升的视觉效果。
     * <p>
     * Continuously spawns rising steam particles.
     * Called by Level Tick event to simulate the visual effect of rising heat.
     *
     * @param level 服务端世界 / Server Level
     * @param cloud 药水云实体 / Cloud entity
     */
    private static void spawnRisingSteamParticles(ServerLevel level, AreaEffectCloud cloud) {
        boolean isHighHeat = cloud.getTags().contains(TAG_HIGH_HEAT);
        float radius = cloud.getRadius();
        if (radius < 0.2f) return;

        Random random = new Random();
        int count = Math.max(1, (int) (radius * 0.8)); 

        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = Math.sqrt(random.nextDouble()) * radius;
            double x = cloud.getX() + Math.cos(angle) * dist;
            double z = cloud.getZ() + Math.sin(angle) * dist;
            double y = cloud.getY();

            // 模拟热气上升速度
            // Simulate rising speed
            double upSpeed = 0.05 + random.nextDouble() * 0.08;

            // 使用白色篝火烟雾 
            // Use white campfire smoke
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0, 0, upSpeed, 0, 1.0);

            if (isHighHeat) {
                // 高温混合火焰和熔岩粒子，无黑烟
                // High heat mixes flame and lava particles, no black smoke
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
     * 播放一次性的蒸汽爆发特效（音效+粒子）。
     * <p>
     * Plays a one-time steam burst effect (Sound + Particles).
     *
     * @param level      服务端世界 / Server Level
     * @param target     目标实体 / Target entity
     * @param radius     爆发半径 / Burst radius
     * @param intensity  强度等级 / Intensity level
     * @param isHighHeat 是否为高温 / Whether high heat
     */
    private static void playSteamBurstEffect(ServerLevel level, LivingEntity target, float radius, int intensity, boolean isHighHeat) {
        Random random = new Random();

        float volume = isHighHeat ? 0.8F : 0.6F;
        float pitch = isHighHeat ? 1.0F : 1.2F;

        // 音效分级处理
        // Sound tiering
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
                // High heat burst: White smoke + Flame + Lava
                if (random.nextFloat() < 0.2f)
                    level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0, 0, 0.1, 0, speed);
                if (random.nextFloat() < 0.3f)
                    level.sendParticles(ParticleTypes.FLAME, x, y, z, 0, 0, 0.05, 0, speed * 0.5);
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