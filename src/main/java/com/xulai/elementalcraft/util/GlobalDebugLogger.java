// src/main/java/com/xulai/elementalcraft/util/GlobalDebugLogger.java
package com.xulai.elementalcraft.util;

import com.mojang.logging.LogUtils;
import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ElementalReactionConfig;
import com.xulai.elementalcraft.event.ScorchedHandler;
import com.xulai.elementalcraft.event.SteamReactionHandler;
import com.xulai.elementalcraft.event.WetnessHandler;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.potion.ModMobEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.*;

/**
 * GlobalDebugLogger
 * <p>
 * 中文说明：
 * 全局调试监控器。
 * 负责汇集模组内各个系统的调试信息，并将其格式化输出到服务器控制台。
 * <p>
 * English Description:
 * Global Debug Logger.
 * Responsible for aggregating debug information from various systems within the mod and formatting it for output to the server console.
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GlobalDebugLogger {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String LOG_PREFIX = "§e[EC-Debug] §r";

    // 缓存防止 Tick 刷屏
    // Cache to prevent log spamming per tick
    private static final Map<Integer, Integer> wetnessCache = new WeakHashMap<>();
    private static final Map<Integer, Boolean> scorchedCache = new WeakHashMap<>();

    private static boolean isDebugEnabled() {
        return DebugMode.hasAnyDebugEnabled();
    }

    /**
     * 配置加载监控事件。
     * 当模组配置加载或重载时，打印关键参数的当前值，用于确认服务器配置是否生效。
     * <p>
     * Config Load Monitor Event.
     * Prints current values of key parameters when mod config is loaded or reloaded, ensuring server config is active.
     */
    @Mod.EventBusSubscriber(modid = ElementalCraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void onConfigLoad(ModConfigEvent event) {
            String fileName = event.getConfig().getFileName();
            LOGGER.info(LOG_PREFIX + "📂 配置加载: {}", fileName);

            if (fileName.contains("common")) {
                LOGGER.info(LOG_PREFIX + "  > 基础倍率: 伤害 x{}, 抗性 x{}",
                        ElementalConfig.ELEMENTAL_DAMAGE_MULTIPLIER.get(),
                        ElementalConfig.ELEMENTAL_RESISTANCE_MULTIPLIER.get());
            } else if (fileName.contains("reactions")) {
                ElementalReactionConfig.refreshCache();
                LOGGER.info(LOG_PREFIX + "================ [ElementalCraft] 配置重载清单 ================");
                
                // 1. 潮湿系统 / Wetness System
                LOGGER.info(LOG_PREFIX + "💧 [潮湿系统]");
                LOGGER.info(LOG_PREFIX + "   > 上限: Lv.{} | 衰减: {}s | 雨中获取: {}s", 
                    ElementalReactionConfig.wetnessMaxLevel, ElementalReactionConfig.wetnessDecayBaseTime, ElementalReactionConfig.wetnessRainGainInterval);
                // [Modified] Removed wetnessResistModifier log
                LOGGER.info(LOG_PREFIX + "   > 火伤减免: +{}% | 饱食度惩罚: +{}", 
                    (int)(ElementalReactionConfig.wetnessFireReduction * 100), ElementalReactionConfig.wetnessExhaustionIncrease);

                // 2. 孢子系统 / Spore System
                LOGGER.info(LOG_PREFIX + "🍄 [孢子系统]");
                LOGGER.info(LOG_PREFIX + "   > 堆叠上限: {} | 持续: {}s/层 | 凋零伤害: {}/s", 
                    ElementalReactionConfig.sporeMaxStacks, ElementalReactionConfig.sporeDurationPerStack, ElementalReactionConfig.sporePoisonDamage);
                LOGGER.info(LOG_PREFIX + "   > 减速: {}% | 物抗: {}% | 易伤: {}%", 
                    (int)(ElementalReactionConfig.sporeSpeedReduction * 100), (int)(ElementalReactionConfig.sporePhysResist * 100), (int)(ElementalReactionConfig.sporeFireVulnPerStack * 100));

                // 3. 传染系统 / Contagion System
                LOGGER.info(LOG_PREFIX + "☣️ [传染系统]");
                LOGGER.info(LOG_PREFIX + "   > 周期: {} tick | 半径: {} (+{}/层)", 
                    ElementalReactionConfig.contagionCheckInterval, ElementalReactionConfig.contagionBaseRadius, ElementalReactionConfig.contagionRadiusPerStack);

                // 4. 自然反应 / Nature Reaction
                LOGGER.info(LOG_PREFIX + "🌿 [自然反应]");
                LOGGER.info(LOG_PREFIX + "   > 寄生: 阈值 {} | 几率 {}% (+{}%/级)", 
                    ElementalReactionConfig.natureParasiteBaseThreshold, (int)(ElementalReactionConfig.natureParasiteBaseChance * 100), (int)(ElementalReactionConfig.natureParasiteScalingChance * 100));
                LOGGER.info(LOG_PREFIX + "   > 吸取: 阈值 {} | 回血: {}/层 | 冷却: {} tick", 
                    ElementalReactionConfig.natureSiphonThreshold, ElementalReactionConfig.natureSiphonHeal, ElementalReactionConfig.natureDrainCooldown);
                LOGGER.info(LOG_PREFIX + "   > 野火: 阈值 {} | 半径: {} | 击退: {}", 
                    ElementalReactionConfig.wildfireTriggerThreshold, ElementalReactionConfig.wildfireRadius, ElementalReactionConfig.wildfireKnockback);

                // 5. 赤焰反应 / Fire Reaction
                LOGGER.info(LOG_PREFIX + "🔥 [赤焰反应]");
                LOGGER.info(LOG_PREFIX + "   > 爆燃阈值: {} | 弱效倍率: x{}", ElementalReactionConfig.blastTriggerThreshold, ElementalReactionConfig.blastWeakIgniteMult);
                LOGGER.info(LOG_PREFIX + "   > 终结爆燃: 伤 {} (+{}/层) | 半径 {} (+{}/层)", 
                    ElementalReactionConfig.blastBaseDamage, ElementalReactionConfig.blastGrowthDamage, ElementalReactionConfig.blastBaseRange, ElementalReactionConfig.blastGrowthRange);
                LOGGER.info(LOG_PREFIX + "   > 防御上限: 爆保 {}% | 普保 {}%", 
                    (int)(ElementalReactionConfig.blastMaxBlastProtCap * 100), (int)(ElementalReactionConfig.blastMaxGeneralProtCap * 100));

                // 6. 蒸汽反应 / Steam Reaction
                LOGGER.info(LOG_PREFIX + "☁️ [蒸汽反应]");
                LOGGER.info(LOG_PREFIX + "   > 开关: {} | 触发: 火>{} / 冰>{}", 
                    ElementalReactionConfig.steamReactionEnabled, ElementalReactionConfig.steamTriggerThresholdFire, ElementalReactionConfig.steamTriggerThresholdFrost);
                LOGGER.info(LOG_PREFIX + "   > 烫伤: {} (+{}%/级) | 保底: {}%", 
                    ElementalReactionConfig.steamScaldingDamage, (int)(ElementalReactionConfig.steamDamageScalePerLevel * 100), (int)(ElementalReactionConfig.steamDamageFloorRatio * 100));
                LOGGER.info(LOG_PREFIX + "   > 防御上限: 火保 {}% | 普保 {}%", 
                    (int)(ElementalReactionConfig.steamMaxFireProtCap * 100), (int)(ElementalReactionConfig.steamMaxGeneralProtCap * 100));

                // 7. 灼烧机制 / Scorched Mechanic
                LOGGER.info(LOG_PREFIX + "🌋 [灼烧机制]");
                LOGGER.info(LOG_PREFIX + "   > 阈值: {} | 基础几率: {}%", ElementalReactionConfig.scorchedTriggerThreshold, (int)(ElementalReactionConfig.scorchedBaseChance * 100));
                LOGGER.info(LOG_PREFIX + "   > 伤害: {} (+0.5 每 {} 点)", ElementalReactionConfig.scorchedDamageBase, ElementalReactionConfig.scorchedDamageScalingStep);
                LOGGER.info(LOG_PREFIX + "   > 免疫阈值: {} | 免疫怪修正: x{}", ElementalReactionConfig.scorchedResistThreshold, ElementalReactionConfig.scorchedImmuneModifier);
                
                LOGGER.info(LOG_PREFIX + "=========================================================");
            }
        }
    }

    /**
     * 反应监控事件。
     * 监控主动攻击触发的反应，包括自然属性的动态寄生、寄生吸取，以及赤焰属性的毒火爆燃。
     * 使用 HIGH 优先级以在实际伤害逻辑前捕获状态。
     * <p>
     * Reaction Monitor Event.
     * Monitors reactions triggered by attacks, including Dynamic Parasitism/Siphon (Nature) and Toxic Blast (Fire).
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void monitorReactions(LivingDamageEvent event) {
        if (!isDebugEnabled() || event.getEntity().level().isClientSide) return;

        Entity source = event.getSource().getEntity();
        if (!(source instanceof LivingEntity attacker)) return;
        LivingEntity target = event.getEntity();

        com.xulai.elementalcraft.util.ElementType attackType = ElementUtils.getConsistentAttackElement(attacker);
        double naturePower = ElementUtils.getDisplayEnhancement(attacker, com.xulai.elementalcraft.util.ElementType.NATURE);
        double firePower = ElementUtils.getDisplayEnhancement(attacker, com.xulai.elementalcraft.util.ElementType.FIRE);

        // A. 自然反应：动态寄生 & 吸取
        // A. Nature Reaction: Dynamic Parasitism & Siphon
        if (attackType == com.xulai.elementalcraft.util.ElementType.NATURE) {
            // 1. 动态寄生概率计算
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
                
                int attackerWetness = attacker.getPersistentData().getInt("EC_WetnessLevel");
                if (attackerWetness > 0) {
                    chance += attackerWetness * ElementalReactionConfig.natureParasiteWetnessBonus;
                }
                
                if (chance > 0.01) {
                    LOGGER.info(LOG_PREFIX + "🎲 [自然-动态寄生] 攻击者:{} (自然:{}) | 寄生概率: {}% (含潮湿加成)",
                            attacker.getName().getString(), (int)naturePower, String.format("%.1f", chance * 100));
                }
            }

            // 2. 寄生吸取预计算
            CompoundTag targetData = target.getPersistentData();
            int wetnessLevel = targetData.getInt("EC_WetnessLevel");
            if (wetnessLevel > 0 && naturePower >= ElementalReactionConfig.natureSiphonThreshold) {
                boolean onCooldown = attacker.getPersistentData().getLong("ec_drain_cd") > attacker.level().getGameTime();
                
                if (!onCooldown) {
                    double step = ElementalReactionConfig.natureDrainPowerStep;
                    int drainCapacity = (int) Math.floor(naturePower / step);
                    if (drainCapacity < 1) drainCapacity = 1;
                    int actualDrain = Math.min(wetnessLevel, drainCapacity);
                    float healAmount = (float) (actualDrain * ElementalReactionConfig.natureSiphonHeal);

                    LOGGER.info(LOG_PREFIX + "🌿 [自然-寄生吸取] 触发预判! 目标潮湿: Lv.{}", wetnessLevel);
                    LOGGER.info(LOG_PREFIX + "   > 预计吸取层数: {} (能力上限: {})", actualDrain, drainCapacity);
                    LOGGER.info(LOG_PREFIX + "   > 预计回复血量: {}", String.format("%.1f", healAmount));
                }
            }
        }

        // B. 赤焰反应：毒火爆燃 & 灼烧联动
        // B. Fire Reaction: Toxic Blast & Scorched Linkage
        if (attackType == com.xulai.elementalcraft.util.ElementType.FIRE) {
            if (target.hasEffect(Objects.requireNonNull(ModMobEffects.SPORES.get())) && !event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
                
                if (firePower >= ElementalReactionConfig.blastTriggerThreshold) {
                    var effectInstance = target.getEffect(Objects.requireNonNull(ModMobEffects.SPORES.get()));
                    int stacks = (effectInstance != null) ? (effectInstance.getAmplifier() + 1) : 0;
                    
                    if (stacks < 3) {
                        LOGGER.info(LOG_PREFIX + "🔥 [赤焰-弱效引燃] 目标:{} (孢子:{}层) | 将转化为灼烧", target.getName().getString(), stacks);
                        // 预判灼烧施加
                        logScorchedApplication(target, (int)firePower, (int)(ElementalReactionConfig.blastScorchBase * 20));
                    } else {
                        // 终结爆燃计算
                        int extraStacks = stacks - 3;
                        float rawBaseDamage = (float) (ElementalReactionConfig.blastBaseDamage + (extraStacks * ElementalReactionConfig.blastGrowthDamage));
                        double radius = ElementalReactionConfig.blastBaseRange + (extraStacks * ElementalReactionConfig.blastGrowthRange);

                        int blastProtLevel = getTotalEnchantmentLevel(Enchantments.BLAST_PROTECTION, target);
                        int generalProtLevel = getTotalEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, target);

                        double maxBlastCap = ElementalReactionConfig.blastMaxBlastProtCap;
                        double maxGeneralCap = ElementalReactionConfig.blastMaxGeneralProtCap;

                        double blastFactor = maxBlastCap / 16.0;
                        double generalFactor = maxGeneralCap / 16.0;

                        double actualBlastRed = Math.min(blastProtLevel * blastFactor, maxBlastCap);
                        double actualGeneralRed = Math.min(generalProtLevel * generalFactor, maxGeneralCap);

                        float mitigation = (float) Math.min(actualBlastRed + actualGeneralRed, 1.0);
                        float finalDamage = rawBaseDamage * (1.0f - mitigation);

                        LOGGER.info(LOG_PREFIX + "💥 [赤焰-终结爆燃] 触发预判! 目标: {} (孢子: {}层)", target.getName().getString(), stacks);
                        LOGGER.info(LOG_PREFIX + "   > 💥 爆炸半径: {}", String.format("%.1f", radius));
                        LOGGER.info(LOG_PREFIX + "   > 🔢 原始伤害: {}", String.format("%.2f", rawBaseDamage));
                        LOGGER.info(LOG_PREFIX + "   > 🛡️ 防御检测: 爆炸保护Lv.{} (抵消{}%), 普通保护Lv.{} (抵消{}%)", 
                                blastProtLevel, String.format("%.1f", actualBlastRed * 100),
                                generalProtLevel, String.format("%.1f", actualGeneralRed * 100));
                        LOGGER.info(LOG_PREFIX + "   > 🩸 预计最终伤害: {}", String.format("%.2f", finalDamage));
                    }
                }
            }
        }
    }

    /**
     * 受伤反应监控事件。
     * 监控蒸汽反应的触发条件（赤焰/冰霜攻击）以及野火喷射的反击逻辑。
     * 同时监控灼烧状态下对原版火焰伤害的拦截。
     * <p>
     * Hurt Reaction Monitor Event.
     * Monitors Steam Reaction trigger conditions (Fire/Frost attacks) and Wildfire Ejection logic.
     * Also monitors blockage of vanilla fire damage under Scorched status.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void monitorHurtReactions(LivingHurtEvent event) {
        if (!isDebugEnabled() || event.getEntity().level().isClientSide) return;

        LivingEntity victim = event.getEntity();
        Entity source = event.getSource().getEntity();
        
        // 1. 灼烧伤害拦截监控 (Scorched Damage Block Monitor)
        if (victim.getPersistentData().contains(ScorchedHandler.NBT_SCORCHED_TICKS)) {
            if (event.getSource().is(DamageTypeTags.IS_FIRE) && !event.getSource().is(ModDamageTypes.LAVA_MAGIC)) {
                LOGGER.info(LOG_PREFIX + "🛡️ [灼烧-伤害拦截] 目标处于灼烧状态，已拦截原版火焰伤害: {}", String.format("%.2f", event.getAmount()));
            }
        }

        // 2. 蒸汽反应触发监控 (Steam Trigger Monitor)
        if (source instanceof LivingEntity attacker) {
            com.xulai.elementalcraft.util.ElementType attackElement = ElementUtils.getConsistentAttackElement(attacker);
            if (event.getSource().is(DamageTypeTags.IS_FIRE)) attackElement = com.xulai.elementalcraft.util.ElementType.FIRE;
            if (event.getSource().is(DamageTypeTags.IS_FREEZING)) attackElement = com.xulai.elementalcraft.util.ElementType.FROST;

            int firePower = ElementUtils.getDisplayEnhancement(attacker, com.xulai.elementalcraft.util.ElementType.FIRE);
            int frostPower = ElementUtils.getDisplayEnhancement(attacker, com.xulai.elementalcraft.util.ElementType.FROST);

            boolean targetIsWet = victim.getPersistentData().getInt(WetnessHandler.NBT_WETNESS) > 0;
            int targetWetness = victim.getPersistentData().getInt(WetnessHandler.NBT_WETNESS);
            com.xulai.elementalcraft.util.ElementType targetElement = ElementUtils.getElementType(victim);

            // A. 赤焰攻击 -> 高温蒸汽 / 自我干燥
            if (attackElement == com.xulai.elementalcraft.util.ElementType.FIRE) {
                int attackerWetness = attacker.getPersistentData().getInt(WetnessHandler.NBT_WETNESS);
                if (attackerWetness > 0) {
                    int threshold = Math.max(1, ElementalReactionConfig.wetnessDryingThreshold);
                    int layersToRemove = Math.max(1, firePower / threshold);
                    int maxBurst = ElementalReactionConfig.steamHighHeatMaxLevel;
                    LOGGER.info(LOG_PREFIX + "🧖 [蒸汽-自我干燥] 触发预判! 攻击者: {}", attacker.getName().getString());
                    LOGGER.info(LOG_PREFIX + "   > 消耗潮湿: {} 层, 爆发等级: {}", layersToRemove, Math.min(layersToRemove, maxBurst));
                    return; 
                }

                if (targetIsWet || targetElement == com.xulai.elementalcraft.util.ElementType.FROST) {
                    if (firePower >= ElementalReactionConfig.steamTriggerThresholdFire) {
                        boolean blocked = isSteamTriggerBlocked(victim);
                        int fuelLevel = 1;
                        if (targetIsWet) fuelLevel = targetWetness;
                        else if (targetElement == com.xulai.elementalcraft.util.ElementType.FROST) {
                            int tFrost = ElementUtils.getDisplayEnhancement(victim, com.xulai.elementalcraft.util.ElementType.FROST);
                            int step = Math.max(1, ElementalReactionConfig.steamCondensationStepFrost);
                            fuelLevel = 1 + (tFrost / step);
                        }
                        int maxLevel = ElementalReactionConfig.steamHighHeatMaxLevel;
                        fuelLevel = Math.min(fuelLevel, maxLevel);

                        if (!blocked) {
                            LOGGER.info(LOG_PREFIX + "☁️ [蒸汽-高温] 触发成功! 目标: {}", victim.getName().getString());
                            LOGGER.info(LOG_PREFIX + "   > 燃料来源: {}, 预计等级: Lv.{}", targetIsWet ? "潮湿" : "冰霜属性", fuelLevel);
                        } else {
                            LOGGER.info(LOG_PREFIX + "☁️ [蒸汽-高温] 触发被阻止 (冷却中 或 已在云内)");
                        }
                    }
                }
            }
            // B. 冰霜攻击 -> 低温蒸汽
            else if (attackElement == com.xulai.elementalcraft.util.ElementType.FROST) {
                if (targetElement == com.xulai.elementalcraft.util.ElementType.FIRE) {
                    if (victim.level().dimension() != Level.NETHER) {
                        if (frostPower >= ElementalReactionConfig.steamTriggerThresholdFrost) {
                            boolean blocked = isSteamTriggerBlocked(victim);
                            int tFire = ElementUtils.getDisplayEnhancement(victim, com.xulai.elementalcraft.util.ElementType.FIRE);
                            int step = Math.max(1, ElementalReactionConfig.steamCondensationStepFire);
                            int level = Math.min(1 + (tFire / step), ElementalReactionConfig.steamLowHeatMaxLevel);

                            if (!blocked) {
                                LOGGER.info(LOG_PREFIX + "🌫️ [蒸汽-低温] 触发成功! 目标: {}", victim.getName().getString());
                                LOGGER.info(LOG_PREFIX + "   > 目标赤焰强度: {}, 预计等级: Lv.{}", tFire, level);
                            } else {
                                LOGGER.info(LOG_PREFIX + "🌫️ [蒸汽-低温] 触发被阻止 (冷却中 或 已在云内)");
                            }
                        }
                    }
                }
            }
        }

        // 3. 自然反应：野火喷射 (Wildfire Ejection)
        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
            double naturePower = ElementUtils.getDisplayEnhancement(victim, com.xulai.elementalcraft.util.ElementType.NATURE);
            boolean onCooldown = victim.getPersistentData().getLong("ec_wildfire_cd") > victim.level().getGameTime();

            if (naturePower >= ElementalReactionConfig.wildfireTriggerThreshold) {
                if (!onCooldown) {
                    double radius = ElementalReactionConfig.wildfireRadius;
                    double knockback = ElementalReactionConfig.wildfireKnockback;
                    LOGGER.info(LOG_PREFIX + "🔊 [自然-野火喷射] 触发! 目标: {} (自然:{})", victim.getName().getString(), (int)naturePower);
                    LOGGER.info(LOG_PREFIX + "   > 范围: {} 格, 击退力度: {}, 附加孢子: {}层", radius, knockback, ElementalReactionConfig.wildfireSporeAmount);
                }
            }
        }
    }

    /**
     * 状态与环境监控事件。
     * 监控生物的潮湿层数变化、灼烧状态变化以及环境传染的扫描逻辑。
     * <p>
     * Status & Environment Monitor Event.
     * Monitors changes in mob wetness levels, scorched status, and environmental contagion scanning logic.
     */
    @SubscribeEvent
    public static void monitorStatusAndContagion(LivingEvent.LivingTickEvent event) {
        if (!isDebugEnabled() || event.getEntity().level().isClientSide) return;
        LivingEntity entity = event.getEntity();
        if (entity.tickCount % 10 != 0) return;

        int id = entity.getId();
        CompoundTag data = entity.getPersistentData();

        // A. 潮湿监控
        int curWet = data.getInt("EC_WetnessLevel");
        int lastWet = wetnessCache.getOrDefault(id, 0);
        if (curWet != lastWet) {
            LOGGER.info(LOG_PREFIX + "💧 [潮湿变动] {}: {} -> {}", entity.getName().getString(), lastWet, curWet);
            if (curWet == 0) wetnessCache.remove(id);
            else wetnessCache.put(id, curWet);
        }

        // B. 灼烧监控 (Scorched) - 深度监控
        boolean isScorched = data.contains(ScorchedHandler.NBT_SCORCHED_TICKS);
        boolean wasScorched = scorchedCache.getOrDefault(id, false);
        
        if (isScorched != wasScorched) {
            if (isScorched) {
                int duration = data.getInt(ScorchedHandler.NBT_SCORCHED_TICKS);
                int strength = data.getInt(ScorchedHandler.NBT_SCORCHED_STRENGTH);
                LOGGER.info(LOG_PREFIX + "🔥 [灼烧-开始] {}: 强度 {}, 持续 {} tick", entity.getName().getString(), strength, duration);
            } else {
                LOGGER.info(LOG_PREFIX + "🔥 [灼烧-结束] {}: 状态已移除", entity.getName().getString());
            }
            scorchedCache.put(id, isScorched);
        }
        
        // 灼烧伤害预判 (每秒/20tick)
        if (isScorched && entity.tickCount % 20 == 0) {
            int fireStrength = data.getInt(ScorchedHandler.NBT_SCORCHED_STRENGTH);
            logScorchedDamageCalculation(entity, fireStrength);
        }

        // C. 环境传染监控
        if (ModMobEffects.SPORES.isPresent() && entity.hasEffect(ModMobEffects.SPORES.get())) {
            var effect = entity.getEffect(ModMobEffects.SPORES.get());
            int stacks = (effect != null) ? effect.getAmplifier() + 1 : 0;
            if (stacks >= 3 && !data.getBoolean("ec_spreaded") && !data.getBoolean("ec_infected")) {
                if (entity.tickCount % ElementalReactionConfig.contagionCheckInterval == 0) {
                    logContagionEvent(entity, stacks);
                }
            }
        }
    }

    /**
     * 伤害详细监控事件。
     * 分析蒸汽烫伤的伤害构成（初始伤害、附魔减免、保底机制）以及其他特殊伤害类型（如孢子易伤）。
     * <p>
     * Damage Detail Monitor Event.
     * Analyzes steam scalding damage composition (initial damage, enchantment reduction, floor mechanism) and other special damage types (e.g., spore vulnerability).
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamageDebug(LivingDamageEvent event) {
        if (!isDebugEnabled() || event.getEntity().level().isClientSide) return;
        LivingEntity target = event.getEntity();
        float amount = event.getAmount();

        // 1. 蒸汽烫伤防御计算
        if (event.getSource().is(Objects.requireNonNull(ModDamageTypes.STEAM_SCALDING))) {
            LOGGER.info(LOG_PREFIX + "♨️ [蒸汽烫伤-防御分析] 目标: {}", target.getName().getString());
            LOGGER.info(LOG_PREFIX + "   > 🛑 初始伤害: {}", String.format("%.2f", amount));

            float trueRaw = amount;
            
            // 模组自定义计算
            int fireProtLv = getTotalEnchantmentLevel(Enchantments.FIRE_PROTECTION, target);
            int genProtLv = getTotalEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, target);
            
            double maxFireCap = ElementalReactionConfig.steamMaxFireProtCap;
            double maxGenCap = ElementalReactionConfig.steamMaxGeneralProtCap;
            
            double fireFactor = maxFireCap / 16.0;
            double genFactor = maxGenCap / 16.0;
            
            double actFireRed = Math.min(fireProtLv * fireFactor, maxFireCap);
            double actGenRed = Math.min(genProtLv * genFactor, maxGenCap);
            double totalRed = Math.min(actFireRed + actGenRed, 1.0);
            
            float reduced = trueRaw * (float)(1.0 - totalRed);
            
            LOGGER.info(LOG_PREFIX + "   > 🛡️ 模组减免: 火保Lv.{} ({}%) + 普保Lv.{} ({}%) = 总计 {}%",
                    fireProtLv, String.format("%.1f", actFireRed*100),
                    genProtLv, String.format("%.1f", actGenRed*100),
                    String.format("%.1f", totalRed*100));
            
            // 保底伤害
            com.xulai.elementalcraft.util.ElementType type = ElementUtils.getElementType(target);
            if (type == com.xulai.elementalcraft.util.ElementType.FROST || type == com.xulai.elementalcraft.util.ElementType.NATURE) {
                float floor = trueRaw * (float)ElementalReactionConfig.steamDamageFloorRatio;
                LOGGER.info(LOG_PREFIX + "   > 📉 弱点保底: 目标为 {}, 最低伤害限制: {}", type.getDisplayName().getString(), String.format("%.2f", floor));
                if (reduced < floor) {
                    LOGGER.info(LOG_PREFIX + "   > ⚠️ 触发保底! 伤害提升至 {}", String.format("%.2f", floor));
                }
            } else {
                LOGGER.info(LOG_PREFIX + "   > ✅ 最终计算: {}", String.format("%.2f", reduced));
            }
        }
        
        // 2. 灼烧伤害 (Lava Magic)
        if (event.getSource().is(ModDamageTypes.LAVA_MAGIC)) {
            LOGGER.info(LOG_PREFIX + "🔥 [灼烧伤害] {} 受到 {} 点体内高热伤害", target.getName().getString(), String.format("%.2f", amount));
        }
        
        // 3. 孢子修正
        if (target.hasEffect(Objects.requireNonNull(ModMobEffects.SPORES.get()))) {
            int stacks = target.getEffect(ModMobEffects.SPORES.get()).getAmplifier() + 1;
            boolean isFire = event.getSource().is(DamageTypeTags.IS_FIRE);
            boolean isPhysical = !isFire && !event.getSource().is(DamageTypes.MAGIC) && !event.getSource().is(DamageTypeTags.IS_EXPLOSION);

            if (isFire) LOGGER.info(LOG_PREFIX + "🔥 [孢子-火伤加深] 目标:{} ({}层) | 受到额外火伤", target.getName().getString(), stacks);
            else if (isPhysical) LOGGER.info(LOG_PREFIX + "🛡️ [孢子-物理硬化] 目标:{} ({}层) | 物理减伤生效", target.getName().getString(), stacks);
        }
    }

    /**
     * 实体生成监控事件。
     * 监控蒸汽云的生成参数以及特殊生物的生成属性。
     * <p>
     * Entity Join Monitor Event.
     * Monitors steam cloud spawn parameters and special entity spawn attributes.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!isDebugEnabled() || event.getLevel().isClientSide) return;
        
        if (event.getEntity() instanceof AreaEffectCloud cloud) {
            if (cloud.getTags().contains(SteamReactionHandler.TAG_STEAM_CLOUD)) {
                String type = cloud.getTags().contains(SteamReactionHandler.TAG_HIGH_HEAT) ? "高温" : "低温";
                LOGGER.info(LOG_PREFIX + "☁️ [蒸汽生成] 类型: {}, 半径: {}", type, String.format("%.1f", cloud.getRadius()));
            }
        }
        
        if (event.getEntity() instanceof LivingEntity entity && !(entity instanceof Player)) {
            logEntitySpawnAttributes(entity);
        }
    }
    
    // ================= 私有辅助方法 (Private Helpers) =================
    
    // 恢复详细的传染日志
    private static void logContagionEvent(LivingEntity source, int stacks) {
        LOGGER.info(LOG_PREFIX + "☣️ [环境传染] 宿主: {} (孢子:{}层) -> 尝试扩散...", source.getName().getString(), stacks);
        
        double radius = ElementalReactionConfig.contagionBaseRadius + ((stacks - 3) * ElementalReactionConfig.contagionRadiusPerStack);
        AABB area = source.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = source.level().getEntitiesOfClass(LivingEntity.class, area);
        
        LOGGER.info(LOG_PREFIX + "   > 扫描半径: {}, 潜在目标: {}", String.format("%.1f", radius), targets.size());
        
        for (LivingEntity target : targets) {
            if (target == source) continue;
            boolean wasInfected = target.getPersistentData().getBoolean("ec_infected");
            if (!wasInfected) {
                LOGGER.info(LOG_PREFIX + "   > 💉 传染目标: {} | 位置: [{}, {}, {}]", 
                        target.getName().getString(), 
                        (int)target.getX(), (int)target.getY(), (int)target.getZ());
            }
        }
    }

    private static void logScorchedApplication(LivingEntity target, int strength, int duration) {
        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).toString();
        if (ElementalReactionConfig.cachedScorchedBlacklist.contains(entityId)) {
            LOGGER.info(LOG_PREFIX + "🚫 [灼烧-施加] 目标 {} 在黑名单中，操作取消", target.getName().getString());
            return;
        }
        
        if (target.getPersistentData().contains(ScorchedHandler.NBT_SCORCHED_COOLDOWN)) {
            long cd = target.getPersistentData().getLong(ScorchedHandler.NBT_SCORCHED_COOLDOWN);
            if (target.level().getGameTime() < cd) {
                LOGGER.info(LOG_PREFIX + "⏳ [灼烧-施加] 目标 {} 冷却中，操作取消", target.getName().getString());
                return;
            }
        }
        
        LOGGER.info(LOG_PREFIX + "🔥 [灼烧-施加] 目标: {}, 强度: {}, 时长: {} tick", target.getName().getString(), strength, duration);
    }

    private static void logScorchedDamageCalculation(LivingEntity target, int fireStrength) {
        int resistPoints = ElementUtils.getDisplayResistance(target, com.xulai.elementalcraft.util.ElementType.FIRE);
        if (resistPoints >= ElementalReactionConfig.scorchedResistThreshold) {
            LOGGER.info(LOG_PREFIX + "🛡️ [灼烧-周期] 目标火抗 {} >= 阈值 {}, 伤害免疫", resistPoints, ElementalReactionConfig.scorchedResistThreshold);
            return;
        }

        double base = ElementalReactionConfig.scorchedDamageBase;
        int step = Math.max(1, ElementalReactionConfig.scorchedDamageScalingStep);
        double bonus = (double) fireStrength / step * 0.5;
        double rawDamage = base + bonus;
        
        LOGGER.info(LOG_PREFIX + "🔥 [灼烧-周期] 基础: {} + 加成: {} = 原始: {}", String.format("%.2f", base), String.format("%.2f", bonus), String.format("%.2f", rawDamage));

        if (target.fireImmune()) {
            double old = rawDamage;
            rawDamage *= ElementalReactionConfig.scorchedImmuneModifier;
            LOGGER.info(LOG_PREFIX + "   > ⚠️ 目标火焰免疫! 伤害衰减: {} -> {}", String.format("%.2f", old), String.format("%.2f", rawDamage));
        }

        if (ElementUtils.getDisplayEnhancement(target, com.xulai.elementalcraft.util.ElementType.NATURE) > 0 ||
            ElementUtils.getDisplayResistance(target, com.xulai.elementalcraft.util.ElementType.NATURE) > 0) {
            double old = rawDamage;
            rawDamage *= ElementalReactionConfig.scorchedNatureMultiplier;
            LOGGER.info(LOG_PREFIX + "   > 🌿 自然属性易伤! 伤害加深: {} -> {}", String.format("%.2f", old), String.format("%.2f", rawDamage));
        }

        int fireProtLevel = getTotalEnchantmentLevel(Enchantments.FIRE_PROTECTION, target);
        int genProtLevel = getTotalEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, target);

        double fireProtReduction = (Math.min(fireProtLevel, 16) / 16.0) * ElementalReactionConfig.scorchedFireProtReduction;
        double genProtReduction = (Math.min(genProtLevel, 16) / 16.0) * ElementalReactionConfig.scorchedGenProtReduction;

        double finalDamage = rawDamage * (1.0 - fireProtReduction) * (1.0 - genProtReduction);
        
        LOGGER.info(LOG_PREFIX + "   > 🛡️ 附魔减免: 火保 {}% + 普保 {}%", String.format("%.1f", fireProtReduction*100), String.format("%.1f", genProtReduction*100));
        LOGGER.info(LOG_PREFIX + "   > 🩸 预计最终伤害: {}", String.format("%.2f", finalDamage));
    }

    private static void logEntitySpawnAttributes(LivingEntity entity) {
        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        if (checkIsForcedEntity(entityId) || ElementalConfig.cachedBlacklist.contains(entityId)) {
            LOGGER.info(LOG_PREFIX + "🌱 生物生成: {} (ID: {})", entity.getName().getString(), entityId);
        }
    }
    
    private static boolean checkIsForcedEntity(String entityId) {
        return ElementalConfig.FORCED_ENTITIES.get().stream()
                .anyMatch(s -> s.replace("\"", "").trim().startsWith(entityId + ","));
    }

    private static int getTotalEnchantmentLevel(Enchantment ench, LivingEntity entity) {
        int total = 0;
        for (ItemStack stack : entity.getArmorSlots()) total += stack.getEnchantmentLevel(ench);
        return total;
    }

    private static boolean isSteamTriggerBlocked(LivingEntity entity) {
        if (entity.getPersistentData().getInt("EC_SteamTriggerCooldown") > 0) return true;
        if (entity.level().isClientSide) return false;

        double searchRadius = 10.0; 
        AABB box = entity.getBoundingBox().inflate(searchRadius);
        List<AreaEffectCloud> clouds = entity.level().getEntitiesOfClass(AreaEffectCloud.class, box,
                c -> c.getTags().contains(SteamReactionHandler.TAG_STEAM_CLOUD));

        for (AreaEffectCloud cloud : clouds) {
            double dx = entity.getX() - cloud.getX();
            double dz = entity.getZ() - cloud.getZ();
            if ((dx*dx + dz*dz) < cloud.getRadius() * cloud.getRadius()) {
                double dy = entity.getY() - cloud.getY();
                if (dy > -0.5 && dy < ElementalReactionConfig.steamCloudHeightCeiling) return true; 
            }
        }
        return false;
    }
}