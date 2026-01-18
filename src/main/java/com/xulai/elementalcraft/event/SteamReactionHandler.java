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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
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
 * 中文说明：
 * 蒸汽反应处理类。
 * 负责管理游戏中所有的蒸汽元素反应逻辑，包括触发条件判定、生成蒸汽云实体、计算蒸汽伤害与防御减免，
 * 以及维护蒸汽云的生命周期和视觉效果。
 * <p>
 * English Description:
 * Steam Reaction Handler Class.
 * Responsible for managing all steam elemental reaction logic in the game, including determining trigger conditions,
 * spawning steam cloud entities, calculating steam damage and defense reduction,
 * and maintaining the lifecycle and visual effects of steam clouds.
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
@SuppressWarnings("null")
public class SteamReactionHandler {

    // NBT & Tags
    public static final String TAG_STEAM_CLOUD = "EC_SteamCloud";
    public static final String TAG_HIGH_HEAT = "EC_HighHeat";
    public static final String TAG_LEVEL_PREFIX = "EC_Level_";
    public static final String TAG_SELF_DRYING_PENALTY = "EC_SelfDryingPenalty";

    private static final String NBT_CONDENSATION_TIMER = "EC_SteamCondensationTimer";
    private static final String NBT_SPORE_GROWTH_TIMER = "EC_SporeGrowthTimer";
    
    // Hardcoded Cooldown NBT & Constant (10 Seconds)
    private static final String NBT_STEAM_TRIGGER_COOLDOWN = "EC_SteamTriggerCooldown";
    private static final int TRIGGER_COOLDOWN_TICKS = 200; // 10 Seconds / 10秒冷却

    /**
     * 活跃蒸汽云追踪列表。
     * 用于在服务端Tick中快速访问所有蒸汽云以生成粒子效果，避免遍历全图实体。
     * <p>
     * Active Steam Cloud Tracking List.
     * Used to quickly access all steam clouds during server ticks to generate particle effects, avoiding iterating through all entities in the map.
     */
    private static final List<AreaEffectCloud> ACTIVE_STEAM_CLOUDS = new ArrayList<>();

    /**
     * 监听实体受伤事件（低优先级）。
     * 负责检测攻击是否满足蒸汽反应的触发条件（如赤焰攻击潮湿目标，或冰霜攻击赤焰目标），并调用触发逻辑。
     * <p>
     * Listens to LivingHurtEvent (Lowest Priority).
     * Responsible for detecting if the attack meets the trigger conditions for a steam reaction
     * (e.g., Fire attacking Wet target, or Frost attacking Fire target) and invoking the trigger logic.
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
     * 监听实体受到伤害事件（低优先级）。
     * 专门处理当伤害来源为蒸汽烫伤（Steam Scalding）时，根据目标的装备和抗性计算最终伤害。
     * <p>
     * Listens to LivingDamageEvent (Lowest Priority).
     * Specifically handles damage calculation when the source is Steam Scalding,
     * adjusting the final damage based on the target's equipment and resistance.
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
     * 监听实体死亡事件。
     * 如果实体死于蒸汽烫伤，将其状态设置为着火，以确保掉落物为熟食。
     * <p>
     * Listens to LivingDeathEvent.
     * If the entity dies from Steam Scalding, sets its state to on fire to ensure drops are cooked items.
     *
     * @param event 死亡事件 / Death event
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().is(ModDamageTypes.STEAM_SCALDING)) {
            event.getEntity().setSecondsOnFire(1);
        }
    }

    /**
     * 监听生物Tick事件。
     * 定期检查生物是否位于蒸汽云范围内，并根据云的类型（高温/低温）应用相应的持续效果（伤害或状态变化）。
     * 同时处理触发冷却的倒计时。
     * <p>
     * Listens to LivingTickEvent.
     * Periodically checks if the living entity is within the range of a steam cloud and applies
     * corresponding persistent effects. Also handles trigger cooldown countdown.
     *
     * @param event 实体 Tick 事件 / Living tick event
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!ElementalReactionConfig.steamReactionEnabled) return;
        
        LivingEntity entity = event.getEntity();

        // 处理冷却时间递减
        // Handle Cooldown Tick Down
        CompoundTag data = entity.getPersistentData();
        if (data.contains(NBT_STEAM_TRIGGER_COOLDOWN)) {
            int cooldown = data.getInt(NBT_STEAM_TRIGGER_COOLDOWN);
            if (cooldown > 0) {
                data.putInt(NBT_STEAM_TRIGGER_COOLDOWN, cooldown - 1);
            } else {
                data.remove(NBT_STEAM_TRIGGER_COOLDOWN);
            }
        }

        if (entity.tickCount % 10 != 0) return;
        processCloudEffects(entity);
    }

    /**
     * 监听服务端世界Tick事件。
     * 遍历活跃的蒸汽云列表，在云的位置生成自定义的视觉粒子效果（模拟蒸汽上升）。
     * <p>
     * Listens to LevelTickEvent.
     * Iterates through the active steam cloud list and generates custom visual particle effects
     * (simulating rising steam) at the cloud's location.
     *
     * @param event 世界 Tick 事件 / Level tick event
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
     * 监听维度卸载事件。
     * 清理追踪列表中属于被卸载维度的蒸汽云引用，防止内存泄漏。
     * <p>
     * Listens to LevelUnloadEvent.
     * Cleans up steam cloud references in the tracking list that belong to the unloaded dimension to prevent memory leaks.
     *
     * @param event 维度卸载事件 / Level Unload event
     */
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) return;
        ACTIVE_STEAM_CLOUDS.removeIf(cloud -> cloud.level() == event.getLevel());
    }

    /**
     * 监听服务器停止事件。
     * 清空整个蒸汽云追踪列表。
     * <p>
     * Listens to ServerStoppedEvent.
     * Clears the entire steam cloud tracking list.
     *
     * @param event 服务器停止事件 / Server stopped event
     */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ACTIVE_STEAM_CLOUDS.clear();
    }

    // =================================================================================================
    //                                  Logic Implementation / 逻辑实现
    // =================================================================================================

    /**
     * 核心触发逻辑处理。
     * 判断攻击者与目标的元素属性关系，确定是否触发蒸汽反应，以及反应的强度（云的等级）。
     * <p>
     * Core Trigger Logic Processing.
     * Determines the elemental attribute relationship between attacker and target to decide if a steam reaction is triggered
     * and the intensity of the reaction (cloud level).
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

        // 赤焰攻击逻辑：触发高温蒸汽
        // Fire Attack Logic: Triggers High Heat Steam
        if (attackElement == ElementType.FIRE) {
            // [Note] 自我干燥逻辑已移至 CombatEvents.java 处理，此处不再重复检测
            // [Note] Self-drying logic moved to CombatEvents.java, no duplicate check here

            if (targetIsWet || targetElement == ElementType.FROST) {
                int threshold = ElementalReactionConfig.steamTriggerThresholdFire;

                if (firePower >= threshold) {
                    // 检查触发限制：冷却中 或 已在云内
                    // Check Limits: Cooldown Active or Already Inside Cloud
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
                    setTriggerCooldown(target); // 设置冷却

                    DebugCommand.sendSteamTriggerLog(attacker, true, fuelLevel);

                    if (targetIsWet) {
                        target.addTag("EC_WetnessSnapshot_" + targetWetness);
                        removeWetness(target);
                    }
                } else {
                    if (targetIsWet) {
                        removeWetness(target);
                    }
                }
            }
        } 
        // 冰霜攻击逻辑：触发低温蒸汽
        // Frost Attack Logic: Triggers Low Heat Steam
        else if (attackElement == ElementType.FROST) {
            if (targetElement == ElementType.FIRE) {
                if (target.level().dimension() == Level.NETHER) {
                    return;
                }

                int threshold = ElementalReactionConfig.steamTriggerThresholdFrost;

                if (frostPower >= threshold) {
                    // 检查触发限制：冷却中 或 已在云内
                    // Check Limits: Cooldown Active or Already Inside Cloud
                    if (isTriggerBlocked(target)) return;

                    int targetFirePower = ElementUtils.getDisplayEnhancement(target, ElementType.FIRE);
                    int step = Math.max(1, ElementalReactionConfig.steamCondensationStepFire);

                    int level = 1 + (targetFirePower / step);
                    int maxLevel = ElementalReactionConfig.steamLowHeatMaxLevel;
                    level = Math.min(level, maxLevel);

                    spawnSteamCloud(target, false, level);
                    setTriggerCooldown(target); // 设置冷却

                    DebugCommand.sendSteamTriggerLog(attacker, false, level);
                }
            }
        }
    }

    /**
     * 核心防御逻辑处理。
     * 计算并应用针对蒸汽伤害的减免，包括护甲附魔提供的保护和元素抗性带来的抵消。
     * <p>
     * Core Defense Logic Processing.
     * Calculates and applies reduction for steam damage, including protection provided by armor enchantments
     * and offsets from elemental resistance.
     */
    private static void processDefenseLogic(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        float currentDamage = event.getAmount();

        if (checkImmunity(target)) {
            event.setAmount(0);
            event.setCanceled(true);
            return;
        }

        float trueRawDamage = currentDamage;

        // 计算原版火焰保护附魔减免
        // Calculate Vanilla Fire Protection Reduction
        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
            int vanillaEPF = EnchantmentHelper.getDamageProtection(target.getArmorSlots(), event.getSource());

            if (vanillaEPF > 0) {
                float reductionRatio = Math.min(vanillaEPF, 20) * 0.04f;
                if (reductionRatio < 1.0f) {
                    trueRawDamage = currentDamage / (1.0f - reductionRatio);
                }
            }
        }

        // 计算模组自定义抗性减免
        // Calculate Mod Custom Resistance Reduction
        int totalFireProtLevel = getTotalEnchantmentLevel(Enchantments.FIRE_PROTECTION, target);
        int totalProtLevel = getTotalEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, target);

        // 动态计算系数：假设原版满级附魔（4件 * 4级 = 16级）能达到配置设定的最大上限
        // 这样可以确保配置文件的调整能真实影响每级附魔的收益        
        // Dynamic Factor Calculation: Assume full vanilla enchant (16 levels) reaches the config max cap
        // This ensures config adjustments actually affect the benefit per level
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

        // 特定元素类型的保底伤害机制
        // Damage Floor Mechanism for Specific Element Types
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
     * 辅助方法：获取全身护甲指定附魔的总等级。
     * <p>
     * Helper Method: Get total level of specified enchantment on all armor slots.
     */
    private static int getTotalEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantment ench, LivingEntity entity) {
        int total = 0;
        for (ItemStack stack : entity.getArmorSlots()) {
            total += stack.getEnchantmentLevel(ench);
        }
        return total;
    }

    /**
     * 核心云效果逻辑处理。
     * 扫描实体周围的蒸汽云，判定实体是否处于云内，并根据云的属性（高温/低温）执行相应逻辑：
     * 1. 高温：造成周期性烫伤伤害。
     * 2. 低温：增加潮湿等级，促进孢子生长。
     * <p>
     * Core Cloud Effect Logic Processing.
     * Scans for steam clouds around the entity, determines if the entity is inside, and executes logic based on cloud properties (High/Low Heat):
     * 1. High Heat: Deals periodic scalding damage.
     * 2. Low Heat: Increases wetness level and promotes spore growth.
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

        // 高温蒸汽效果：伤害
        // High Heat Steam Effect: Damage
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
                    entity.hurt(ModDamageTypes.source(entity.level(), ModDamageTypes.STEAM_SCALDING), damage);
                }

                if (entity.getPersistentData().getInt(WetnessHandler.NBT_WETNESS) > 0) {
                    removeWetness(entity);
                }
            }
        } 
        // 低温蒸汽效果：冷凝与孢子催化
        // Low Heat Steam Effect: Condensation and Spore Catalysis
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
     * 检查是否可以触发新的蒸汽云（冷却与重叠检测）。
     * <p>
     * Checks if a new steam cloud can be triggered (Cooldown and Overlap check).
     *
     * @param entity 目标生物 / Target entity
     * @return 如果可以触发返回 false，如果被阻止（冷却中或已在云内）返回 true / returns true if blocked, false if allowed
     */
    private static boolean isTriggerBlocked(LivingEntity entity) {
        // 1. 冷却检查
        if (entity.getPersistentData().getInt(NBT_STEAM_TRIGGER_COOLDOWN) > 0) return true;

        // 2. 范围重叠检查 (服务端)
        if (entity.level().isClientSide) return false;

        // 搜索半径：使用一个较大的安全值，确保能覆盖到周围可能影响到实体的任何云
        double searchRadius = 10.0; 
        AABB box = entity.getBoundingBox().inflate(searchRadius);
        List<AreaEffectCloud> clouds = entity.level().getEntitiesOfClass(AreaEffectCloud.class, box,
                c -> c.getTags().contains(TAG_STEAM_CLOUD));

        for (AreaEffectCloud cloud : clouds) {
            // 计算水平距离平方
            double dx = entity.getX() - cloud.getX();
            double dz = entity.getZ() - cloud.getZ();
            double distSqr = dx * dx + dz * dz;
            double radius = cloud.getRadius();
            
            // 实体中心与云中心的距离 < 云半径，判定为“在云内”
            if (distSqr < radius * radius) {
                // 垂直高度检查，防止头顶的云阻止脚下的触发（虽然很少见）
                double dy = entity.getY() - cloud.getY();
                // -0.5 ~ +CloudCeiling 范围内视为受影响
                if (dy > -0.5 && dy < ElementalReactionConfig.steamCloudHeightCeiling) {
                    return true; 
                }
            }
        }
        return false;
    }

    /**
     * 设置触发冷却。
     * <p>
     * Sets the trigger cooldown.
     */
    private static void setTriggerCooldown(LivingEntity entity) {
        entity.getPersistentData().putInt(NBT_STEAM_TRIGGER_COOLDOWN, TRIGGER_COOLDOWN_TICKS);
    }

    /**
     * 移除实体的所有潮湿状态数据。
     * <p>
     * Removes all wetness status data from the entity.
     */
    private static void removeWetness(LivingEntity entity) {
        entity.getPersistentData().remove(WetnessHandler.NBT_WETNESS);
        entity.getPersistentData().remove(WetnessHandler.NBT_RAIN_TIMER);
        entity.getPersistentData().remove(WetnessHandler.NBT_DECAY_TIMER);
        entity.removeEffect(com.xulai.elementalcraft.potion.ModMobEffects.WETNESS.get());
    }

    /**
     * 检查实体是否免疫蒸汽伤害。
     * 依据：是否免疫火焰、是否在配置黑名单中、抗性是否达标。
     * <p>
     * Checks if the entity is immune to steam damage.
     * Criteria: Fire immunity, blacklist configuration, and resistance threshold.
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
     * 生成蒸汽云实体。
     * 初始化云的半径、持续时间、NBT数据，并将其加入活跃追踪列表。
     * <p>
     * Spawns a Steam Cloud entity.
     * Initializes cloud radius, duration, NBT data, and adds it to the active tracking list.
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