// src/main/java/com/xulai/elementalcraft/event/SteamReactionHandler.java
package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalReactionConfig;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.util.EffectHelper;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * SteamReactionHandler
 * <p>
 * 蒸汽反应处理类。
 * 负责管理游戏中所有的蒸汽元素反应逻辑，包括触发条件判定、生成蒸汽云实体、计算蒸汽伤害与防御减免，
 * 以及维护蒸汽云的生命周期和视觉效果。
 * <p>
 * Steam Reaction Handler Class.
 * Responsible for managing all steam elemental reaction logic in the game, including determining trigger conditions,
 * spawning steam cloud entities, calculating steam damage and defense reduction,
 * and maintaining the lifecycle and visual effects of steam clouds.
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
@SuppressWarnings("null")
public class SteamReactionHandler {

    /**
     * Tags and NBT Keys
     * 标签与 NBT 键名
     * <p>
     * Constants for entity tags and NBT keys used in steam reaction logic.
     * 用于蒸汽反应逻辑的实体标签和 NBT 键名常量。
     */
    public static final String TAG_STEAM_CLOUD = "EC_SteamCloud";
    public static final String TAG_HIGH_HEAT = "EC_HighHeat";
    public static final String TAG_LEVEL_PREFIX = "EC_Level_";
    public static final String TAG_SELF_DRYING_PENALTY = "EC_SelfDryingPenalty";

    private static final String NBT_CONDENSATION_TIMER = "EC_SteamCondensationTimer";
    private static final String NBT_SPORE_GROWTH_TIMER = "EC_SporeGrowthTimer";
    
    // Cooldown NBT & Constant (10 Seconds)
    private static final String NBT_STEAM_TRIGGER_COOLDOWN = "EC_SteamTriggerCooldown";
    private static final int TRIGGER_COOLDOWN_TICKS = 200;

    /**
     * Active Steam Clouds List
     * 活跃蒸汽云列表
     * <p>
     * Tracks all active steam clouds to optimize particle rendering on server ticks.
     * 追踪所有活跃的蒸汽云，以优化服务端 Tick 时的粒子渲染性能。
     */
    private static final List<AreaEffectCloud> ACTIVE_STEAM_CLOUDS = new ArrayList<>();

    /**
     * Living Hurt Event Listener
     * 生物受伤事件监听
     * <p>
     * Detects if the attack meets the criteria for a steam reaction (e.g., Fire vs Wet, Frost vs Fire).
     * If conditions are met, triggers the steam cloud generation logic.
     * <p>
     * 检测攻击是否满足蒸汽反应的条件（例如：火 vs 潮湿，冰 vs 火）。
     * 如果条件满足，触发蒸汽云生成逻辑。
     *
     * @param event The living hurt event. (生物受伤事件)
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!ElementalReactionConfig.steamReactionEnabled) return;

        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            processTriggerLogic(event, attacker, event.getEntity());
        }
    }

    /**
     * Living Damage Event Listener
     * 生物伤害事件监听
     * <p>
     * Specifically handles damage calculation for "Steam Scalding" damage.
     * Applies damage reduction based on armor enchantments and elemental resistances.
     * <p>
     * 专门处理“蒸汽烫伤”伤害的计算。
     * 根据护甲附魔和元素抗性应用伤害减免。
     *
     * @param event The living damage event. (生物伤害事件)
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getSource().is(ModDamageTypes.STEAM_SCALDING)) {
            processDefenseLogic(event);
        }
    }

    /**
     * Living Death Event Listener
     * 生物死亡事件监听
     * <p>
     * If an entity dies from steam scalding, sets it on fire to ensure dropped items are cooked.
     * <p>
     * 如果实体死于蒸汽烫伤，将其设置为着火状态，以确掉落物品为熟食。
     *
     * @param event The living death event. (生物死亡事件)
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().is(ModDamageTypes.STEAM_SCALDING)) {
            event.getEntity().setSecondsOnFire(1);
        }
    }

    /**
     * Living Tick Event Listener
     * 生物 Tick 事件监听
     * <p>
     * Manages trigger cooldowns and processes ongoing effects if the entity is inside a steam cloud.
     * <p>
     * 管理触发冷却时间，并在实体位于蒸汽云内时处理持续效果。
     *
     * @param event The living tick event. (生物 Tick 事件)
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!ElementalReactionConfig.steamReactionEnabled) return;
        
        LivingEntity entity = event.getEntity();

        // Handle Cooldown Tick Down
        // 处理冷却时间递减
        CompoundTag data = entity.getPersistentData();
        if (data.contains(NBT_STEAM_TRIGGER_COOLDOWN)) {
            int cooldown = data.getInt(NBT_STEAM_TRIGGER_COOLDOWN);
            if (cooldown > 0) {
                data.putInt(NBT_STEAM_TRIGGER_COOLDOWN, cooldown - 1);
            } else {
                data.remove(NBT_STEAM_TRIGGER_COOLDOWN);
            }
        }

        // Process cloud effects every 10 ticks
        // 每 10 Tick 处理一次云效果
        if (entity.tickCount % 10 != 0) return;
        processCloudEffects(entity);
    }

    /**
     * Level Tick Event Listener
     * 世界 Tick 事件监听
     * <p>
     * Iterates through active steam clouds to spawn visual particles.
     * This avoids scanning all entities in the world for better performance.
     * <p>
     * 遍历活跃蒸汽云以生成视觉粒子。
     * 这样做避免了扫描世界中的所有实体，从而提高性能。
     *
     * @param event The level tick event. (世界 Tick 事件)
     */
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide) return;
        if (event.level.getGameTime() % 20 != 0) return;

        if (ACTIVE_STEAM_CLOUDS.isEmpty()) return;

        Iterator<AreaEffectCloud> iterator = ACTIVE_STEAM_CLOUDS.iterator();
        while (iterator.hasNext()) {
            AreaEffectCloud cloud = iterator.next();

            if (cloud.isRemoved()) {
                iterator.remove();
                continue;
            }

            if (cloud.level() == event.level) {
                boolean isHighHeat = cloud.getTags().contains(TAG_HIGH_HEAT);
                EffectHelper.playSteamCloudTick((ServerLevel) event.level, cloud, isHighHeat);
            }
        }
    }

    /**
     * Level Unload Event Listener
     * 维度卸载事件监听
     * <p>
     * Cleans up the active cloud list to prevent memory leaks when a dimension is unloaded.
     * <p>
     * 当维度卸载时清理活跃云列表，以防止内存泄漏。
     */
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) return;
        ACTIVE_STEAM_CLOUDS.removeIf(cloud -> cloud.level() == event.getLevel());
    }

    /**
     * Server Stopped Event Listener
     * 服务器停止事件监听
     * <p>
     * Clears all cached cloud references when the server stops.
     * <p>
     * 当服务器停止时清除所有缓存的云引用。
     */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ACTIVE_STEAM_CLOUDS.clear();
    }

    // =================================================================================================
    //                                  Logic Implementation / 逻辑实现
    // =================================================================================================

    /**
     * Process Trigger Logic
     * 处理触发逻辑
     * <p>
     * Determines if a steam reaction should occur based on the attacker's element and the target's status.
     * Handles both "Fire attacking Wet/Frost" (High Heat) and "Frost attacking Fire" (Low Heat).
     * <p>
     * 根据攻击者的元素和目标的状态确定是否应发生蒸汽反应。
     * 处理“火攻湿/冰”（高温）和“冰攻火”（低温）两种情况。
     *
     * @param event    The damage event. (伤害事件)
     * @param attacker The entity attacking. (攻击者实体)
     * @param target   The entity being attacked. (被攻击实体)
     */
    private static void processTriggerLogic(LivingHurtEvent event, LivingEntity attacker, LivingEntity target) {
        ElementType attackElement = ElementUtils.getConsistentAttackElement(attacker);

        if (event.getSource().is(DamageTypeTags.IS_FIRE)) attackElement = ElementType.FIRE;
        if (event.getSource().is(DamageTypeTags.IS_FREEZING)) attackElement = ElementType.FROST;

        int firePower = ElementUtils.getDisplayEnhancement(attacker, ElementType.FIRE);
        int frostPower = ElementUtils.getDisplayEnhancement(attacker, ElementType.FROST);

        boolean targetIsWet = target.getPersistentData().getInt(WetnessHandler.NBT_WETNESS) > 0;
        int targetWetness = target.getPersistentData().getInt(WetnessHandler.NBT_WETNESS);
        ElementType targetElement = ElementUtils.getElementType(target);

        // Case 1: Fire Attack -> Triggers High Heat Steam
        // 情况 1：火属性攻击 -> 触发高温蒸汽
        if (attackElement == ElementType.FIRE) {
            if (targetIsWet || targetElement == ElementType.FROST) {
                int threshold = ElementalReactionConfig.steamTriggerThresholdFire;

                if (firePower >= threshold) {
                    // Check Limits: Cooldown Active or Already Inside Cloud
                    // 检查限制：冷却中 或 已在云内
                    if (isTriggerBlocked(target)) return;

                    int fuelLevel = 1;

                    if (targetIsWet) {
                        fuelLevel = targetWetness;
                    } else if (targetElement == ElementType.FROST) {
                        int targetFrostPower = ElementUtils.getDisplayEnhancement(target, ElementType.FROST);
                        int step = Math.max(1, ElementalReactionConfig.steamCondensationStepFrost);
                        fuelLevel = 1 + (targetFrostPower / step);
                    }

                    int maxLevel = ElementalReactionConfig.steamHighHeatMaxLevel;
                    fuelLevel = Math.min(fuelLevel, maxLevel);

                    spawnSteamCloud(target, true, fuelLevel);
                    setTriggerCooldown(target); // Set cooldown / 设置冷却

                    DebugCommand.sendSteamTriggerLog(attacker, true, fuelLevel);

                    // Consume Wetness
                    // 消耗潮湿状态
                    if (targetIsWet) {
                        target.addTag("EC_WetnessSnapshot_" + targetWetness);
                        removeWetness(target);
                    }
                } else {
                    // Below threshold, just dry wetness without cloud
                    // 低于阈值，仅干燥潮湿而不生成云
                    if (targetIsWet) {
                        removeWetness(target);
                    }
                }
            }
        } 
        // Case 2: Frost Attack -> Triggers Low Heat Steam
        // 情况 2：冰属性攻击 -> 触发低温蒸汽
        else if (attackElement == ElementType.FROST) {
            if (targetElement == ElementType.FIRE) {
                if (target.level().dimension() == Level.NETHER) {
                    return; // Nether is too hot for condensation / 下界太热无法冷凝
                }

                int threshold = ElementalReactionConfig.steamTriggerThresholdFrost;

                if (frostPower >= threshold) {
                    if (isTriggerBlocked(target)) return;

                    int targetFirePower = ElementUtils.getDisplayEnhancement(target, ElementType.FIRE);
                    int step = Math.max(1, ElementalReactionConfig.steamCondensationStepFire);

                    int level = 1 + (targetFirePower / step);
                    int maxLevel = ElementalReactionConfig.steamLowHeatMaxLevel;
                    level = Math.min(level, maxLevel);

                    spawnSteamCloud(target, false, level);
                    setTriggerCooldown(target);

                    DebugCommand.sendSteamTriggerLog(attacker, false, level);
                }
            }
        }
    }

    /**
     * Process Defense Logic
     * 处理防御逻辑
     * <p>
     * Calculates the actual damage applied after checking for immunities and applying
     * reductions from Fire Protection and General Protection enchantments.
     * <p>
     * 在检查免疫并应用“火焰保护”和“普通保护”附魔的减免后，计算实际造成的伤害。
     *
     * @param event The damage event. (伤害事件)
     */
    private static void processDefenseLogic(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        float currentDamage = event.getAmount();

        if (checkImmunity(target)) {
            event.setAmount(0);
            event.setCanceled(true);
            return;
        }

        // Raw damage (assuming bypasses_enchantments is configured correctly)
        // 原始伤害（假设 bypasses_enchantments 配置正确）
        float trueRawDamage = currentDamage;

        // Calculate Protection Reduction
        // 计算保护附魔减免
        int totalFireProtLevel = getTotalEnchantmentLevel(Enchantments.FIRE_PROTECTION, target);
        int totalProtLevel = getTotalEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, target);

        // Dynamic Factor: Assume full vanilla (16 levels) equals config max cap
        // 动态系数：假设原版满级（16级）等于配置的最大上限
        double maxFireCap = ElementalReactionConfig.steamMaxFireProtCap;
        double maxGeneralCap = ElementalReactionConfig.steamMaxGeneralProtCap;

        double fireProtFactor = maxFireCap / 16.0;
        double protFactor = maxGeneralCap / 16.0;

        double calculatedFireRed = totalFireProtLevel * fireProtFactor;
        double calculatedProtRed = totalProtLevel * protFactor;

        double actualFireRed = Math.min(calculatedFireRed, maxFireCap);
        double actualProtRed = Math.min(calculatedProtRed, maxGeneralCap);

        double totalReduction = Math.min(actualFireRed + actualProtRed, 1.0);

        float reducedDamage = trueRawDamage * (float) (1.0 - totalReduction);

        // Damage Floor for Vulnerable Elements (Frost/Nature)
        // 弱势元素（冰/自然）的保底伤害
        ElementType type = ElementUtils.getElementType(target);
        if (type == ElementType.FROST || type == ElementType.NATURE) {
            float floorRatio = (float) ElementalReactionConfig.steamDamageFloorRatio;
            float floorLimit = trueRawDamage * floorRatio;

            if (reducedDamage < floorLimit) {
                reducedDamage = floorLimit;
            }
        }

        event.setAmount(reducedDamage);
    }

    /**
     * Helper: Get Total Enchantment Level
     * 辅助方法：获取附魔总等级
     * <p>
     * Sums up the level of a specific enchantment across all armor slots.
     * 汇总所有护甲槽位中指定附魔的等级。
     */
    private static int getTotalEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantment ench, LivingEntity entity) {
        int total = 0;
        for (ItemStack stack : entity.getArmorSlots()) {
            total += stack.getEnchantmentLevel(ench);
        }
        return total;
    }

    /**
     * Process Cloud Effects
     * 处理云效果
     * <p>
     * Scans surrounding steam clouds and applies effects to the entity.
     * High Heat: Scalding damage and forced fleeing.
     * Low Heat: Wetness accumulation and spore growth acceleration.
     * <p>
     * 扫描周围的蒸汽云并对实体施加效果。
     * 高温：烫伤伤害和强制逃离。
     * 低温：潮湿积累和孢子生长加速。
     *
     * @param entity The entity inside or near a cloud. (云内或附近的实体)
     */
    private static void processCloudEffects(LivingEntity entity) {
        if (entity.level().isClientSide) return;

        double searchRadius = ElementalReactionConfig.steamCloudRadius * 3.0;
        AABB box = entity.getBoundingBox().inflate(searchRadius);
        List<AreaEffectCloud> clouds = entity.level().getEntitiesOfClass(AreaEffectCloud.class, box,
                c -> c.getTags().contains(TAG_STEAM_CLOUD));

        if (clouds.isEmpty()) return;

        boolean isHighHeat = false;
        boolean isCondensing = false;
        int cloudLevel = 1;
        AreaEffectCloud heatSource = null;

        for (AreaEffectCloud cloud : clouds) {
            double dx = entity.getX() - cloud.getX();
            double dz = entity.getZ() - cloud.getZ();
            double radius = cloud.getRadius();
            double distSqr = dx * dx + dz * dz;
            
            double effectiveRadius = radius + (entity.getBbWidth() / 2.0);
            if (distSqr > effectiveRadius * effectiveRadius) continue;

            double dy = entity.getY() - cloud.getY();

            if (dy < -0.5) continue;
            if (dy > ElementalReactionConfig.steamCloudHeightCeiling) continue;

            if (ElementalReactionConfig.steamClearAggro && entity instanceof Mob mob) {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }

            if (cloud.getTags().contains(TAG_HIGH_HEAT)) {
                isHighHeat = true;
                heatSource = cloud;
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

        // High Heat Effects
        // 高温效果
        if (isHighHeat) {
            if (entity.getPersistentData().contains(NBT_CONDENSATION_TIMER)) {
                entity.getPersistentData().remove(NBT_CONDENSATION_TIMER);
            }

            if (entity.tickCount % 20 == 0) {
                float baseDamage = (float) ElementalReactionConfig.steamScaldingDamage;
                float scale = (float) ElementalReactionConfig.steamDamageScalePerLevel;
                float levelMultiplier = 1.0f + (cloudLevel * scale);
                float damage = baseDamage * levelMultiplier;

                ElementType type = ElementUtils.getElementType(entity);
                if (type == ElementType.FROST || type == ElementType.NATURE) {
                    double weaknessMult = ElementalReactionConfig.steamScaldingMultiplierWeakness;
                    damage *= (float) weaknessMult;
                }

                if (entity.hasEffect(ModMobEffects.SPORES.get())) {
                    damage *= (float) ElementalReactionConfig.steamScaldingMultiplierSpore;
                }

                if (damage > 0) {
                    entity.invulnerableTime = 0;
                    boolean hurtSuccess = entity.hurt(ModDamageTypes.source(entity.level(), ModDamageTypes.STEAM_SCALDING), damage);
                    
                    if (hurtSuccess) {
                        // Intelligent Fleeing
                        // 智能逃离
                        if (entity instanceof PathfinderMob mob && heatSource != null) {
                            mob.setTarget(null);
                            
                            // Dynamic flee distance
                            // 动态逃离距离
                            int fleeDist = (int) (heatSource.getRadius() + 2);
                            
                            Vec3 escapePos = DefaultRandomPos.getPosAway(mob, fleeDist, 4, heatSource.position());
                            if (escapePos != null) {
                                mob.getNavigation().moveTo(escapePos.x, escapePos.y, escapePos.z, 1.5);
                            }
                        }
                    }
                }

                if (entity.getPersistentData().getInt(WetnessHandler.NBT_WETNESS) > 0) {
                    removeWetness(entity);
                }
            }
        } 
        // Low Heat Effects
        // 低温效果
        else if (isCondensing) {
            int currentTimer = entity.getPersistentData().getInt(NBT_CONDENSATION_TIMER);
            currentTimer += 10;

            int delayThreshold = Math.max(10, ElementalReactionConfig.steamCondensationDelay);

            if (currentTimer >= delayThreshold) {
                int currentWet = entity.getPersistentData().getInt(WetnessHandler.NBT_WETNESS);
                int max = ElementalReactionConfig.wetnessMaxLevel;

                if (currentWet < max) {
                    entity.getPersistentData().putInt(WetnessHandler.NBT_WETNESS, currentWet + 1);
                    entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.NEUTRAL, 1.0f, 1.0f);
                }

                currentTimer = 0;
            }

            entity.getPersistentData().putInt(NBT_CONDENSATION_TIMER, currentTimer);

            if (entity.hasEffect(ModMobEffects.SPORES.get())) {
                int sporeTimer = entity.getPersistentData().getInt(NBT_SPORE_GROWTH_TIMER);
                sporeTimer += 10;
                
                int growthRate = Math.max(10, ElementalReactionConfig.steamSporeGrowthRate);
                
                if (sporeTimer >= growthRate) {
                    MobEffectInstance effect = entity.getEffect(ModMobEffects.SPORES.get());
                    int amp = effect.getAmplifier();
                    int maxStacks = ElementalReactionConfig.sporeMaxStacks;
                    
                    if (amp + 1 < maxStacks) {
                        entity.addEffect(new MobEffectInstance(ModMobEffects.SPORES.get(), 200, amp + 1));
                        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.COMPOSTER_READY, SoundSource.NEUTRAL, 1.0f, 1.0f);
                    }
                    sporeTimer = 0;
                }
                entity.getPersistentData().putInt(NBT_SPORE_GROWTH_TIMER, sporeTimer);
            }
        }
    }

    // =================================================================================================
    //                                  Helper Methods / 辅助方法
    // =================================================================================================

    /**
     * Check if Trigger is Blocked
     * 检查触发是否被阻止
     * <p>
     * Checks if the entity is currently in cooldown or already overlapping with an existing cloud.
     * <p>
     * 检查实体当前是否处于冷却中，或是否已与现有的云重叠。
     *
     * @param entity The target entity. (目标实体)
     * @return True if blocked, false otherwise. (被阻止返回 True，否则返回 False)
     */
    private static boolean isTriggerBlocked(LivingEntity entity) {
        // 1. Cooldown Check
        // 1. 冷却检查
        if (entity.getPersistentData().getInt(NBT_STEAM_TRIGGER_COOLDOWN) > 0) return true;

        // 2. Overlap Check (Server Only)
        // 2. 重叠检查（仅限服务端）
        if (entity.level().isClientSide) return false;

        double searchRadius = 10.0; 
        AABB box = entity.getBoundingBox().inflate(searchRadius);
        List<AreaEffectCloud> clouds = entity.level().getEntitiesOfClass(AreaEffectCloud.class, box,
                c -> c.getTags().contains(TAG_STEAM_CLOUD));

        for (AreaEffectCloud cloud : clouds) {
            double dx = entity.getX() - cloud.getX();
            double dz = entity.getZ() - cloud.getZ();
            double distSqr = dx * dx + dz * dz;
            double radius = cloud.getRadius();
            
            // Inside Cloud Check
            // 云内检查
            if (distSqr < radius * radius) {
                double dy = entity.getY() - cloud.getY();
                if (dy > -0.5 && dy < ElementalReactionConfig.steamCloudHeightCeiling) {
                    return true; 
                }
            }
        }
        return false;
    }

    /**
     * Set Trigger Cooldown
     * 设置触发冷却
     * <p>
     * Applies a hardcoded cooldown to the entity to prevent spamming steam triggers.
     * <p>
     * 对实体应用硬编码的冷却时间，以防止刷屏触发蒸汽。
     */
    private static void setTriggerCooldown(LivingEntity entity) {
        entity.getPersistentData().putInt(NBT_STEAM_TRIGGER_COOLDOWN, TRIGGER_COOLDOWN_TICKS);
    }

    /**
     * Remove Wetness
     * 移除潮湿
     * <p>
     * Completely clears wetness data and effects from the entity.
     * <p>
     * 完全清除实体的潮湿数据和效果。
     */
    private static void removeWetness(LivingEntity entity) {
        entity.getPersistentData().remove(WetnessHandler.NBT_WETNESS);
        entity.getPersistentData().remove(WetnessHandler.NBT_RAIN_TIMER);
        entity.getPersistentData().remove(WetnessHandler.NBT_DECAY_TIMER);
        entity.removeEffect(com.xulai.elementalcraft.potion.ModMobEffects.WETNESS.get());
    }

    /**
     * Check Immunity
     * 检查免疫
     * <p>
     * Verifies if the entity is immune to steam damage based on fire immunity, blacklist, or resistance.
     * <p>
     * 根据火焰免疫、黑名单或抗性验证实体是否免疫蒸汽伤害。
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
     * Spawn Steam Cloud
     * 生成蒸汽云
     * <p>
     * Creates and spawns an AreaEffectCloud entity with defined properties (radius, duration, type).
     * <p>
     * 创建并生成一个具有定义属性（半径、持续时间、类型）的 AreaEffectCloud 实体。
     *
     * @param target     The target entity to spawn the cloud at. (生成云的目标实体)
     * @param isHighHeat Whether this is a high heat cloud. (是否为高温云)
     * @param fuelLevel  The intensity level of the cloud. (云的强度等级)
     */
    private static void spawnSteamCloud(LivingEntity target, boolean isHighHeat, int fuelLevel) {
        if (!(target.level() instanceof ServerLevel serverLevel)) return;

        int maxLevel = isHighHeat ? ElementalReactionConfig.steamHighHeatMaxLevel : ElementalReactionConfig.steamLowHeatMaxLevel;
        int level = Math.max(1, Math.min(fuelLevel, maxLevel));

        float baseRadius = (float) ElementalReactionConfig.steamCloudRadius;
        float radiusInc = (float) ElementalReactionConfig.steamRadiusPerLevel;
        float radius = isHighHeat ? baseRadius + (level - 1.0f) * radiusInc : baseRadius;

        int baseDuration;
        int durationInc;

        if (isHighHeat) {
            baseDuration = ElementalReactionConfig.steamCloudDuration;
            durationInc = ElementalReactionConfig.steamDurationPerLevel;
        } else {
            baseDuration = ElementalReactionConfig.steamCondensationDurationBase;
            durationInc = ElementalReactionConfig.steamCondensationDurationPerLevel;
        }

        int duration = baseDuration + (level * durationInc);

        AreaEffectCloud cloud = new AreaEffectCloud(serverLevel, target.getX(), target.getY(), target.getZ());
        cloud.setRadius(radius);
        cloud.setRadiusOnUse(0F);
        cloud.setRadiusPerTick(0F);
        cloud.setDuration(duration);

        cloud.setParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AIR.defaultBlockState()));

        cloud.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ElementalReactionConfig.steamBlindnessDuration));

        cloud.addTag(TAG_STEAM_CLOUD);
        cloud.addTag(TAG_LEVEL_PREFIX + level);

        if (isHighHeat) {
            cloud.addTag(TAG_HIGH_HEAT);
        }

        serverLevel.addFreshEntity(cloud);

        ACTIVE_STEAM_CLOUDS.add(cloud);

        EffectHelper.playSteamBurst(serverLevel, target, radius, level, isHighHeat);
    }
}