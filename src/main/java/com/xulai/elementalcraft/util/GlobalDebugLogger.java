package com.xulai.elementalcraft.util;

import com.mojang.logging.LogUtils;
import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.event.ScorchedHandler;
import com.xulai.elementalcraft.event.SteamReactionHandler;
import com.xulai.elementalcraft.event.WetnessHandler;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.potion.ModMobEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
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
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.*;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GlobalDebugLogger {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String LOG_PREFIX = "§e[EC-Debug] §r";

    private static final Map<Integer, Integer> wetnessCache = new WeakHashMap<>();
    private static final Map<Integer, Boolean> scorchedCache = new WeakHashMap<>();

    private static boolean isDebugEnabled() {
        return DebugMode.hasAnyDebugEnabled();
    }

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
                ElementalFireNatureReactionsConfig.refreshCache();
                LOGGER.info(LOG_PREFIX + "================ [ElementalCraft] 配置重载清单 ================");

                // 潮湿系统
                LOGGER.info(LOG_PREFIX + "💧 [潮湿系统]");
                LOGGER.info(LOG_PREFIX + "   > 上限: Lv.{} | 衰减: {}s | 雨中获取: {}s",
                        ElementalFireNatureReactionsConfig.wetnessMaxLevel, ElementalFireNatureReactionsConfig.wetnessDecayBaseTime, ElementalFireNatureReactionsConfig.wetnessRainGainInterval);
                LOGGER.info(LOG_PREFIX + "   > 火伤减免: +{}% | 饱食度惩罚: +{}",
                        (int)(ElementalFireNatureReactionsConfig.wetnessFireReduction * 100), ElementalFireNatureReactionsConfig.wetnessExhaustionIncrease);

                // 孢子系统
                LOGGER.info(LOG_PREFIX + "🍄 [孢子系统]");
                LOGGER.info(LOG_PREFIX + "   > 堆叠上限: {} | 持续: {}s/层 | 凋零伤害: {}/s",
                        ElementalFireNatureReactionsConfig.sporeMaxStacks, ElementalFireNatureReactionsConfig.sporeDurationPerStack, ElementalFireNatureReactionsConfig.sporePoisonDamage);
                LOGGER.info(LOG_PREFIX + "   > 减速: {}% | 物抗: {}% | 易伤: {}%",
                        (int)(ElementalFireNatureReactionsConfig.sporeSpeedReduction * 100), (int)(ElementalFireNatureReactionsConfig.sporePhysResist * 100), (int)(ElementalFireNatureReactionsConfig.sporeFireVulnPerStack * 100));

                // 传染系统
                LOGGER.info(LOG_PREFIX + "☣️ [传染系统]");
                LOGGER.info(LOG_PREFIX + "   > 周期: {} tick | 半径: {} (+{}/层)",
                        ElementalFireNatureReactionsConfig.contagionCheckInterval, ElementalFireNatureReactionsConfig.contagionBaseRadius, ElementalFireNatureReactionsConfig.contagionRadiusPerStack);

                // 自然反应
                LOGGER.info(LOG_PREFIX + "🌿 [自然反应]");
                LOGGER.info(LOG_PREFIX + "   > 寄生: 阈值 {} | 几率 {}% (+{}%/级)",
                        ElementalFireNatureReactionsConfig.natureParasiteBaseThreshold, (int)(ElementalFireNatureReactionsConfig.natureParasiteBaseChance * 100), (int)(ElementalFireNatureReactionsConfig.natureParasiteScalingChance * 100));
                LOGGER.info(LOG_PREFIX + "   > 吸取: 阈值 {} | 回血: {}/层 | 冷却: {} tick",
                        ElementalFireNatureReactionsConfig.natureSiphonThreshold, ElementalFireNatureReactionsConfig.natureSiphonHeal, ElementalFireNatureReactionsConfig.natureDrainCooldown);
                LOGGER.info(LOG_PREFIX + "   > 野火: 阈值 {} | 半径: {} | 击退: {}",
                        ElementalFireNatureReactionsConfig.wildfireTriggerThreshold, ElementalFireNatureReactionsConfig.wildfireRadius, ElementalFireNatureReactionsConfig.wildfireKnockback);

                // 赤焰反应
                LOGGER.info(LOG_PREFIX + "🔥 [赤焰反应]");
                LOGGER.info(LOG_PREFIX + "   > 爆燃阈值: {} | 弱效倍率: x{}", ElementalFireNatureReactionsConfig.blastTriggerThreshold, ElementalFireNatureReactionsConfig.blastWeakIgniteMult);
                LOGGER.info(LOG_PREFIX + "   > 终结爆燃: 伤 {} (+{}/层) | 半径 {} (+{}/层)",
                        ElementalFireNatureReactionsConfig.blastBaseDamage, ElementalFireNatureReactionsConfig.blastGrowthDamage, ElementalFireNatureReactionsConfig.blastBaseRange, ElementalFireNatureReactionsConfig.blastGrowthRange);
                LOGGER.info(LOG_PREFIX + "   > 防御上限: 爆保 {}% | 普保 {}%",
                        (int)(ElementalFireNatureReactionsConfig.blastMaxBlastProtCap * 100), (int)(ElementalFireNatureReactionsConfig.blastMaxGeneralProtCap * 100));

                // 蒸汽反应
                LOGGER.info(LOG_PREFIX + "☁️ [蒸汽反应]");
                LOGGER.info(LOG_PREFIX + "   > 开关: {} | 触发: 火>{} / 冰>{}",
                        ElementalFireNatureReactionsConfig.steamReactionEnabled, ElementalFireNatureReactionsConfig.steamTriggerThresholdFire, ElementalFireNatureReactionsConfig.steamTriggerThresholdFrost);
                LOGGER.info(LOG_PREFIX + "   > 烫伤: {} (+{}%/级) | 保底: {}%",
                        ElementalFireNatureReactionsConfig.steamScaldingDamage, (int)(ElementalFireNatureReactionsConfig.steamDamageScalePerLevel * 100), (int)(ElementalFireNatureReactionsConfig.steamDamageFloorRatio * 100));
                LOGGER.info(LOG_PREFIX + "   > 防御上限: 火保 {}% | 普保 {}%",
                        (int)(ElementalFireNatureReactionsConfig.steamMaxFireProtCap * 100), (int)(ElementalFireNatureReactionsConfig.steamMaxGeneralProtCap * 100));

                // 灼烧机制
                LOGGER.info(LOG_PREFIX + "🌋 [灼烧机制]");
                LOGGER.info(LOG_PREFIX + "   > 阈值: {} | 基础几率: {}%", ElementalFireNatureReactionsConfig.scorchedTriggerThreshold, (int)(ElementalFireNatureReactionsConfig.scorchedBaseChance * 100));
                LOGGER.info(LOG_PREFIX + "   > 伤害: {} (+0.5 每 {} 点)", ElementalFireNatureReactionsConfig.scorchedDamageBase, ElementalFireNatureReactionsConfig.scorchedDamageScalingStep);
                LOGGER.info(LOG_PREFIX + "   > 免疫阈值: {} | 免疫怪修正: x{}", ElementalFireNatureReactionsConfig.scorchedResistThreshold, ElementalFireNatureReactionsConfig.scorchedImmuneModifier);

                // 静电反应
                ElementalThunderFrostReactionsConfig.refreshCache();
                LOGGER.info(LOG_PREFIX + "⚡ [静电反应]");
                LOGGER.info(LOG_PREFIX + "   > 触发阈值: {} | 基础几率: {}% | 成长步长: {} | 成长几率: {}%",
                        ElementalThunderFrostReactionsConfig.thunderStrengthThreshold,
                        (int)(ElementalThunderFrostReactionsConfig.staticBaseChance * 100),
                        ElementalThunderFrostReactionsConfig.staticScalingStep,
                        (int)(ElementalThunderFrostReactionsConfig.staticScalingChance * 100));
                LOGGER.info(LOG_PREFIX + "   > 潮湿加成: 每层增加 {}% 触发概率",
                        (int)(ElementalThunderFrostReactionsConfig.staticWetnessBonusChancePerLevel * 100));
                LOGGER.info(LOG_PREFIX + "   > 最大层数: {} | 每层时长: {} tick | 每攻击叠加: {}层",
                        ElementalThunderFrostReactionsConfig.staticMaxTotalStacks,
                        ElementalThunderFrostReactionsConfig.staticDurationPerStackTicks,
                        ElementalThunderFrostReactionsConfig.staticMaxStacksPerAttack);
                LOGGER.info(LOG_PREFIX + "   > 免疫阈值: {} | 伤害范围: {}-{} | 间隔: {} tick",
                        ElementalThunderFrostReactionsConfig.staticResistImmunityThreshold,
                        ElementalThunderFrostReactionsConfig.staticDamageMin,
                        ElementalThunderFrostReactionsConfig.staticDamageMax,
                        ElementalThunderFrostReactionsConfig.staticDamageIntervalTicks);
                LOGGER.info(LOG_PREFIX + "   > 自然倍率: {} | 冰霜倍率: {}",
                        ElementalThunderFrostReactionsConfig.staticDamageNatureMultiplier,
                        ElementalThunderFrostReactionsConfig.staticDamageFrostMultiplier);

                // 麻痹反应
                LOGGER.info(LOG_PREFIX + "💫 [麻痹反应]");
                LOGGER.info(LOG_PREFIX + "   > 最大层数: {} | 每层时长: {} tick | 伤害百分比: {}%",
                        ElementalThunderFrostReactionsConfig.paralysisMaxStacks,
                        ElementalThunderFrostReactionsConfig.paralysisDurationPerStackTicks,
                        (int)(ElementalThunderFrostReactionsConfig.paralysisDamagePercentage * 100));
                LOGGER.info(LOG_PREFIX + "   > 传染阈值: {}% | 基础范围: {} | 每额外层增加: {} | 转换百分比: {}%",
                        (int)(ElementalThunderFrostReactionsConfig.paralysisSpreadThresholdPercentage * 100),
                        ElementalThunderFrostReactionsConfig.paralysisSpreadBaseRange,
                        ElementalThunderFrostReactionsConfig.paralysisSpreadRangePerExtraStack,
                        (int)(ElementalThunderFrostReactionsConfig.paralysisSpreadStaticPercentage * 100));
                LOGGER.info(LOG_PREFIX + "   > 链式传染: {} | 排除玩家: {} | 排除宠物: {}",
                        ElementalThunderFrostReactionsConfig.paralysisSpreadAllowChain,
                        ElementalThunderFrostReactionsConfig.paralysisSpreadExcludePlayers,
                        ElementalThunderFrostReactionsConfig.paralysisSpreadExcludePets);

                LOGGER.info(LOG_PREFIX + "=========================================================");
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void monitorReactions(LivingDamageEvent event) {
        if (!isDebugEnabled() || event.getEntity().level().isClientSide) return;

        Entity source = event.getSource().getEntity();
        if (!(source instanceof LivingEntity)) return;
        LivingEntity attacker = (LivingEntity) source;
        LivingEntity target = event.getEntity();

        ElementType attackType = ElementUtils.getConsistentAttackElement(attacker);
        double naturePower = ElementUtils.getDisplayEnhancement(attacker, ElementType.NATURE);
        double firePower = ElementUtils.getDisplayEnhancement(attacker, ElementType.FIRE);

        if (attackType == ElementType.NATURE) {
            if (naturePower >= ElementalFireNatureReactionsConfig.natureParasiteBaseThreshold) {
                double chance = 0.0;
                double scalingStep = ElementalFireNatureReactionsConfig.natureParasiteScalingStep;
                if (naturePower < scalingStep) {
                    chance = ElementalFireNatureReactionsConfig.natureParasiteBaseChance;
                } else {
                    int steps = (int) ((naturePower - scalingStep) / scalingStep);
                    chance = ElementalFireNatureReactionsConfig.natureParasiteBaseChance + (steps * ElementalFireNatureReactionsConfig.natureParasiteScalingChance);
                    chance += ElementalFireNatureReactionsConfig.natureParasiteScalingChance;
                }

                int attackerWetness = attacker.getPersistentData().getInt("EC_WetnessLevel");
                if (attackerWetness > 0) {
                    chance += attackerWetness * ElementalFireNatureReactionsConfig.natureParasiteWetnessBonus;
                }

                if (chance > 0.01) {
                    LOGGER.info(LOG_PREFIX + "🎲 [自然-动态寄生] 攻击者:{} (自然:{}) | 寄生概率: {}% (含潮湿加成)",
                            attacker.getName().getString(), (int)naturePower, String.format("%.1f", chance * 100));
                }
            }

            CompoundTag targetData = target.getPersistentData();
            int wetnessLevel = targetData.getInt("EC_WetnessLevel");
            if (wetnessLevel > 0 && naturePower >= ElementalFireNatureReactionsConfig.natureSiphonThreshold) {
                boolean onCooldown = attacker.getPersistentData().getLong("ec_drain_cd") > attacker.level().getGameTime();

                if (!onCooldown) {
                    double step = ElementalFireNatureReactionsConfig.natureDrainPowerStep;
                    int drainCapacity = (int) Math.floor(naturePower / step);
                    if (drainCapacity < 1) drainCapacity = 1;
                    int actualDrain = Math.min(wetnessLevel, drainCapacity);
                    float healAmount = (float) (actualDrain * ElementalFireNatureReactionsConfig.natureSiphonHeal);

                    LOGGER.info(LOG_PREFIX + "🌿 [自然-寄生吸取] 触发预判! 目标潮湿: Lv.{}", wetnessLevel);
                    LOGGER.info(LOG_PREFIX + "   > 预计吸取层数: {} (能力上限: {})", actualDrain, drainCapacity);
                    LOGGER.info(LOG_PREFIX + "   > 预计回复血量: {}", String.format("%.1f", healAmount));
                }
            }
        }

        if (attackType == ElementType.FIRE) {
            if (target.hasEffect(Objects.requireNonNull(ModMobEffects.SPORES.get())) && !event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {

                if (firePower >= ElementalFireNatureReactionsConfig.blastTriggerThreshold) {
                    var effectInstance = target.getEffect(Objects.requireNonNull(ModMobEffects.SPORES.get()));
                    int stacks = (effectInstance != null) ? (effectInstance.getAmplifier() + 1) : 0;

                    if (stacks < 3) {
                        LOGGER.info(LOG_PREFIX + "🔥 [赤焰-弱效引燃] 目标:{} (孢子:{}层) | 将转化为灼烧", target.getName().getString(), stacks);
                        logScorchedApplication(target, (int)firePower, (int)(ElementalFireNatureReactionsConfig.blastScorchBase * 20));
                    } else {
                        int extraStacks = stacks - 3;
                        float rawBaseDamage = (float) (ElementalFireNatureReactionsConfig.blastBaseDamage + (extraStacks * ElementalFireNatureReactionsConfig.blastGrowthDamage));
                        double radius = ElementalFireNatureReactionsConfig.blastBaseRange + (extraStacks * ElementalFireNatureReactionsConfig.blastGrowthRange);

                        int blastProtLevel = getTotalEnchantmentLevel(Enchantments.BLAST_PROTECTION, target);
                        int generalProtLevel = getTotalEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, target);

                        double maxBlastCap = ElementalFireNatureReactionsConfig.blastMaxBlastProtCap;
                        double maxGeneralCap = ElementalFireNatureReactionsConfig.blastMaxGeneralProtCap;

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

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void monitorHurtReactions(LivingHurtEvent event) {
        if (!isDebugEnabled() || event.getEntity().level().isClientSide) return;

        LivingEntity victim = event.getEntity();
        Entity source = event.getSource().getEntity();

        if (victim.getPersistentData().contains(ScorchedHandler.NBT_SCORCHED_TICKS)) {
            if (event.getSource().is(DamageTypeTags.IS_FIRE) && !event.getSource().is(ModDamageTypes.LAVA_MAGIC)) {
                LOGGER.info(LOG_PREFIX + "🛡️ [灼烧-伤害拦截] 目标处于灼烧状态，已拦截原版火焰伤害: {}", String.format("%.2f", event.getAmount()));
            }
        }

        if (source instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) source;
            ElementType attackElement = ElementUtils.getConsistentAttackElement(attacker);
            if (event.getSource().is(DamageTypeTags.IS_FIRE)) attackElement = ElementType.FIRE;
            if (event.getSource().is(DamageTypeTags.IS_FREEZING)) attackElement = ElementType.FROST;

            int firePower = ElementUtils.getDisplayEnhancement(attacker, ElementType.FIRE);
            int frostPower = ElementUtils.getDisplayEnhancement(attacker, ElementType.FROST);

            boolean targetIsWet = victim.getPersistentData().getInt(WetnessHandler.NBT_WETNESS) > 0;
            int targetWetness = victim.getPersistentData().getInt(WetnessHandler.NBT_WETNESS);
            ElementType targetElement = ElementUtils.getElementType(victim);

            if (attackElement == ElementType.FIRE) {
                int attackerWetness = attacker.getPersistentData().getInt(WetnessHandler.NBT_WETNESS);
                if (attackerWetness > 0) {
                    int threshold = Math.max(1, ElementalFireNatureReactionsConfig.wetnessDryingThreshold);
                    int layersToRemove = Math.max(1, firePower / threshold);
                    int maxBurst = ElementalFireNatureReactionsConfig.steamHighHeatMaxLevel;
                    LOGGER.info(LOG_PREFIX + "🧖 [蒸汽-自我干燥] 触发预判! 攻击者: {}", attacker.getName().getString());
                    LOGGER.info(LOG_PREFIX + "   > 消耗潮湿: {} 层, 爆发等级: {}", layersToRemove, Math.min(layersToRemove, maxBurst));
                    return;
                }

                if (targetIsWet || targetElement == ElementType.FROST) {
                    if (firePower >= ElementalFireNatureReactionsConfig.steamTriggerThresholdFire) {
                        boolean blocked = isSteamTriggerBlocked(victim);
                        int fuelLevel = 1;
                        if (targetIsWet) fuelLevel = targetWetness;
                        else if (targetElement == ElementType.FROST) {
                            int tFrost = ElementUtils.getDisplayEnhancement(victim, ElementType.FROST);
                            int step = Math.max(1, ElementalFireNatureReactionsConfig.steamCondensationStepFrost);
                            fuelLevel = 1 + (tFrost / step);
                        }
                        int maxLevel = ElementalFireNatureReactionsConfig.steamHighHeatMaxLevel;
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
            else if (attackElement == ElementType.FROST) {
                if (targetElement == ElementType.FIRE) {
                    if (victim.level().dimension() != Level.NETHER) {
                        if (frostPower >= ElementalFireNatureReactionsConfig.steamTriggerThresholdFrost) {
                            boolean blocked = isSteamTriggerBlocked(victim);
                            int tFire = ElementUtils.getDisplayEnhancement(victim, ElementType.FIRE);
                            int step = Math.max(1, ElementalFireNatureReactionsConfig.steamCondensationStepFire);
                            int level = Math.min(1 + (tFire / step), ElementalFireNatureReactionsConfig.steamLowHeatMaxLevel);

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

        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
            double naturePower = ElementUtils.getDisplayEnhancement(victim, ElementType.NATURE);
            boolean onCooldown = victim.getPersistentData().getLong("ec_wildfire_cd") > victim.level().getGameTime();

            if (naturePower >= ElementalFireNatureReactionsConfig.wildfireTriggerThreshold) {
                if (!onCooldown) {
                    double radius = ElementalFireNatureReactionsConfig.wildfireRadius;
                    double knockback = ElementalFireNatureReactionsConfig.wildfireKnockback;
                    LOGGER.info(LOG_PREFIX + "🔊 [自然-野火喷射] 触发! 目标: {} (自然:{})", victim.getName().getString(), (int)naturePower);
                    LOGGER.info(LOG_PREFIX + "   > 范围: {} 格, 击退力度: {}, 附加孢子: {}层", radius, knockback, ElementalFireNatureReactionsConfig.wildfireSporeAmount);
                }
            }
        }
    }

    @SubscribeEvent
    public static void monitorStatusAndContagion(LivingEvent.LivingTickEvent event) {
        if (!isDebugEnabled() || event.getEntity().level().isClientSide) return;
        LivingEntity entity = event.getEntity();
        if (entity.tickCount % 10 != 0) return;

        int id = entity.getId();
        CompoundTag data = entity.getPersistentData();

        int curWet = data.getInt("EC_WetnessLevel");
        int lastWet = wetnessCache.getOrDefault(id, 0);
        if (curWet != lastWet) {
            LOGGER.info(LOG_PREFIX + "💧 [潮湿变动] {}: {} -> {}", entity.getName().getString(), lastWet, curWet);
            if (curWet == 0) wetnessCache.remove(id);
            else wetnessCache.put(id, curWet);
        }

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

        if (isScorched && entity.tickCount % 20 == 0) {
            int fireStrength = data.getInt(ScorchedHandler.NBT_SCORCHED_STRENGTH);
            logScorchedDamageCalculation(entity, fireStrength);
        }

        if (ModMobEffects.SPORES.isPresent() && entity.hasEffect(ModMobEffects.SPORES.get())) {
            var effect = entity.getEffect(ModMobEffects.SPORES.get());
            int stacks = (effect != null) ? effect.getAmplifier() + 1 : 0;
            if (stacks >= 3 && !data.getBoolean("ec_spreaded") && !data.getBoolean("ec_infected")) {
                if (entity.tickCount % ElementalFireNatureReactionsConfig.contagionCheckInterval == 0) {
                    logContagionEvent(entity, stacks);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamageDebug(LivingDamageEvent event) {
        if (!isDebugEnabled() || event.getEntity().level().isClientSide) return;
        LivingEntity target = event.getEntity();
        float amount = event.getAmount();

        if (event.getSource().is(Objects.requireNonNull(ModDamageTypes.STEAM_SCALDING))) {
            LOGGER.info(LOG_PREFIX + "♨️ [蒸汽烫伤-防御分析] 目标: {}", target.getName().getString());
            LOGGER.info(LOG_PREFIX + "   > 🛑 初始伤害: {}", String.format("%.2f", amount));

            float trueRaw = amount;

            int fireProtLv = getTotalEnchantmentLevel(Enchantments.FIRE_PROTECTION, target);
            int genProtLv = getTotalEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, target);

            double maxFireCap = ElementalFireNatureReactionsConfig.steamMaxFireProtCap;
            double maxGenCap = ElementalFireNatureReactionsConfig.steamMaxGeneralProtCap;

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

            ElementType type = ElementUtils.getElementType(target);
            if (type == ElementType.FROST || type == ElementType.NATURE) {
                float floor = trueRaw * (float)ElementalFireNatureReactionsConfig.steamDamageFloorRatio;
                LOGGER.info(LOG_PREFIX + "   > 📉 弱点保底: 目标为 {}, 最低伤害限制: {}", type.getDisplayName().getString(), String.format("%.2f", floor));
                if (reduced < floor) {
                    LOGGER.info(LOG_PREFIX + "   > ⚠️ 触发保底! 伤害提升至 {}", String.format("%.2f", floor));
                }
            } else {
                LOGGER.info(LOG_PREFIX + "   > ✅ 最终计算: {}", String.format("%.2f", reduced));
            }
        }

        if (event.getSource().is(ModDamageTypes.LAVA_MAGIC)) {
            LOGGER.info(LOG_PREFIX + "🔥 [灼烧伤害] {} 受到 {} 点体内高热伤害", target.getName().getString(), String.format("%.2f", amount));
        }

        if (target.hasEffect(Objects.requireNonNull(ModMobEffects.SPORES.get()))) {
            int stacks = target.getEffect(ModMobEffects.SPORES.get()).getAmplifier() + 1;
            boolean isFire = event.getSource().is(DamageTypeTags.IS_FIRE);
            boolean isPhysical = !isFire && !event.getSource().is(DamageTypes.MAGIC) && !event.getSource().is(DamageTypeTags.IS_EXPLOSION);

            if (isFire) LOGGER.info(LOG_PREFIX + "🔥 [孢子-火伤加深] 目标:{} ({}层) | 受到额外火伤", target.getName().getString(), stacks);
            else if (isPhysical) LOGGER.info(LOG_PREFIX + "🛡️ [孢子-物理硬化] 目标:{} ({}层) | 物理减伤生效", target.getName().getString(), stacks);
        }
    }

    // 静电伤害日志
    @SubscribeEvent
    public static void onStaticShockDamage(LivingDamageEvent event) {
        if (!isDebugEnabled()) return;
        if (event.getEntity().level().isClientSide) return;
        if (!event.getSource().is(ModDamageTypes.STATIC_SHOCK)) return;

        LivingEntity target = event.getEntity();
        CompoundTag data = target.getPersistentData();

        int stacks = data.getInt("EC_StaticStacks");
        int totalTimer = data.getInt("EC_StaticTimer");
        float damage = event.getAmount();

        ElementType element = ElementUtils.getElementType(target);
        String elementName = element == ElementType.NONE ? "无属性" : element.getDisplayName().getString();
        double multiplier = 1.0;
        if (element == ElementType.NATURE) {
            multiplier = ElementalThunderFrostReactionsConfig.staticDamageNatureMultiplier;
        } else if (element == ElementType.FROST) {
            multiplier = ElementalThunderFrostReactionsConfig.staticDamageFrostMultiplier;
        }

        // 输出静电伤害详情及配置参数
        LOGGER.info(LOG_PREFIX + "⚡ [静电伤害] 目标: {}, 层数: {}, 本次伤害: {}, 剩余时间: {} tick, 间隔: {} tick, 元素类型: {}, 伤害倍率: {}",
                target.getName().getString(), stacks, String.format("%.2f", damage), totalTimer,
                ElementalThunderFrostReactionsConfig.staticDamageIntervalTicks,
                elementName, String.format("%.2f", multiplier));

        LOGGER.info(LOG_PREFIX + "   ⚙️ 配置: 触发阈值={}, 基础几率={}%, 成长步长={}, 成长几率={}%, 潮湿加成={}%/层, 最大层数={}, 每层时长={} tick, 免疫阈值={}, 伤害范围={}-{}, 自然倍率={}, 冰霜倍率={}",
                ElementalThunderFrostReactionsConfig.thunderStrengthThreshold,
                (int)(ElementalThunderFrostReactionsConfig.staticBaseChance * 100),
                ElementalThunderFrostReactionsConfig.staticScalingStep,
                (int)(ElementalThunderFrostReactionsConfig.staticScalingChance * 100),
                (int)(ElementalThunderFrostReactionsConfig.staticWetnessBonusChancePerLevel * 100),
                ElementalThunderFrostReactionsConfig.staticMaxTotalStacks,
                ElementalThunderFrostReactionsConfig.staticDurationPerStackTicks,
                ElementalThunderFrostReactionsConfig.staticResistImmunityThreshold,
                ElementalThunderFrostReactionsConfig.staticDamageMin,
                ElementalThunderFrostReactionsConfig.staticDamageMax,
                ElementalThunderFrostReactionsConfig.staticDamageNatureMultiplier,
                ElementalThunderFrostReactionsConfig.staticDamageFrostMultiplier);
    }

    // 效果添加事件：捕获静电叠加、麻痹触发
    @SubscribeEvent
    public static void onPotionAdded(MobEffectEvent.Added event) {
        if (!isDebugEnabled()) return;
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity entity = (LivingEntity) event.getEntity();

        MobEffectInstance effectInstance = event.getEffectInstance();
        if (effectInstance.getEffect() == ModMobEffects.STATIC_SHOCK.get()) {
            // 静电叠加或更新
            CompoundTag data = entity.getPersistentData();
            int stacks = data.getInt("EC_StaticStacks");
            int totalTimer = data.getInt("EC_StaticTimer");
            if (stacks <= 0 || totalTimer <= 0) return;

            LOGGER.info(LOG_PREFIX + "⚡ [静电叠加] 目标: {}, 层数: {}, 剩余时间: {} tick, 间隔: {} tick",
                    entity.getName().getString(), stacks, totalTimer,
                    ElementalThunderFrostReactionsConfig.staticDamageIntervalTicks);
            LOGGER.info(LOG_PREFIX + "   ⚙️ 配置: 触发阈值={}, 基础几率={}%, 成长步长={}, 成长几率={}%, 潮湿加成={}%/层, 最大层数={}, 每层时长={} tick, 免疫阈值={}, 伤害范围={}-{}",
                    ElementalThunderFrostReactionsConfig.thunderStrengthThreshold,
                    (int)(ElementalThunderFrostReactionsConfig.staticBaseChance * 100),
                    ElementalThunderFrostReactionsConfig.staticScalingStep,
                    (int)(ElementalThunderFrostReactionsConfig.staticScalingChance * 100),
                    (int)(ElementalThunderFrostReactionsConfig.staticWetnessBonusChancePerLevel * 100),
                    ElementalThunderFrostReactionsConfig.staticMaxTotalStacks,
                    ElementalThunderFrostReactionsConfig.staticDurationPerStackTicks,
                    ElementalThunderFrostReactionsConfig.staticResistImmunityThreshold,
                    ElementalThunderFrostReactionsConfig.staticDamageMin,
                    ElementalThunderFrostReactionsConfig.staticDamageMax);
        }
        else if (effectInstance.getEffect() == ModMobEffects.PARALYSIS.get()) {
            // 麻痹触发（包括传染）
            CompoundTag data = entity.getPersistentData();
            int paralysisStacks = data.getInt("EC_ParalysisStacks");
            int paralysisTimer = data.getInt("EC_ParalysisTimer");
            if (paralysisStacks <= 0 || paralysisTimer <= 0) return;

            int interval = ElementalThunderFrostReactionsConfig.staticDamageIntervalTicks;
            if (interval < 1) interval = 1;
            int remainingHits = (paralysisTimer + interval - 1) / interval;

            double totalDamage = 0;
            double minDmg = ElementalThunderFrostReactionsConfig.staticDamageMin;
            double maxDmg = ElementalThunderFrostReactionsConfig.staticDamageMax;
            if (maxDmg < minDmg) maxDmg = minDmg;
            for (int i = 0; i < remainingHits; i++) {
                double damage = minDmg + new Random().nextDouble() * (maxDmg - minDmg);
                totalDamage += damage;
            }
            totalDamage *= ElementalThunderFrostReactionsConfig.paralysisDamagePercentage;

            String sourceName = event.getEffectSource() instanceof LivingEntity living ? living.getName().getString() : "环境触发";

            LOGGER.info(LOG_PREFIX + "💫 [麻痹触发] 攻击者: {}, 目标: {}, 麻痹层数: {}, 剩余伤害次数: {}, 总伤害: {}",
                    sourceName, entity.getName().getString(), paralysisStacks, remainingHits, String.format("%.2f", totalDamage));

            LOGGER.info(LOG_PREFIX + "   ⚙️ 配置: 最大层数={}, 每层时长={} tick, 伤害百分比={}%, 传染阈值={}%, 基础范围={}, 每额外层增加={}, 转换百分比={}%, 链式传染={}, 排除玩家={}, 排除宠物={}",
                    ElementalThunderFrostReactionsConfig.paralysisMaxStacks,
                    ElementalThunderFrostReactionsConfig.paralysisDurationPerStackTicks,
                    (int)(ElementalThunderFrostReactionsConfig.paralysisDamagePercentage * 100),
                    (int)(ElementalThunderFrostReactionsConfig.paralysisSpreadThresholdPercentage * 100),
                    ElementalThunderFrostReactionsConfig.paralysisSpreadBaseRange,
                    ElementalThunderFrostReactionsConfig.paralysisSpreadRangePerExtraStack,
                    (int)(ElementalThunderFrostReactionsConfig.paralysisSpreadStaticPercentage * 100),
                    ElementalThunderFrostReactionsConfig.paralysisSpreadAllowChain,
                    ElementalThunderFrostReactionsConfig.paralysisSpreadExcludePlayers,
                    ElementalThunderFrostReactionsConfig.paralysisSpreadExcludePets);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!isDebugEnabled() || event.getLevel().isClientSide) return;

        if (event.getEntity() instanceof AreaEffectCloud) {
            AreaEffectCloud cloud = (AreaEffectCloud) event.getEntity();
            if (cloud.getTags().contains(SteamReactionHandler.TAG_STEAM_CLOUD)) {
                String type = cloud.getTags().contains(SteamReactionHandler.TAG_HIGH_HEAT) ? "高温" : "低温";
                LOGGER.info(LOG_PREFIX + "☁️ [蒸汽生成] 类型: {}, 半径: {}", type, String.format("%.1f", cloud.getRadius()));
            }
        }

        if (event.getEntity() instanceof LivingEntity && !(event.getEntity() instanceof Player)) {
            LivingEntity entity = (LivingEntity) event.getEntity();
            logEntitySpawnAttributes(entity);
        }
    }

    private static void logContagionEvent(LivingEntity source, int stacks) {
        LOGGER.info(LOG_PREFIX + "☣️ [环境传染] 宿主: {} (孢子:{}层) -> 尝试扩散...", source.getName().getString(), stacks);

        double radius = ElementalFireNatureReactionsConfig.contagionBaseRadius + ((stacks - 3) * ElementalFireNatureReactionsConfig.contagionRadiusPerStack);
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
        if (ElementalFireNatureReactionsConfig.cachedScorchedBlacklist.contains(entityId)) {
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
        int resistPoints = ElementUtils.getDisplayResistance(target, ElementType.FIRE);
        if (resistPoints >= ElementalFireNatureReactionsConfig.scorchedResistThreshold) {
            LOGGER.info(LOG_PREFIX + "🛡️ [灼烧-周期] 目标火抗 {} >= 阈值 {}, 伤害免疫", resistPoints, ElementalFireNatureReactionsConfig.scorchedResistThreshold);
            return;
        }

        double base = ElementalFireNatureReactionsConfig.scorchedDamageBase;
        int step = Math.max(1, ElementalFireNatureReactionsConfig.scorchedDamageScalingStep);
        double bonus = (double) fireStrength / step * 0.5;
        double rawDamage = base + bonus;

        LOGGER.info(LOG_PREFIX + "🔥 [灼烧-周期] 基础: {} + 加成: {} = 原始: {}", String.format("%.2f", base), String.format("%.2f", bonus), String.format("%.2f", rawDamage));

        if (target.fireImmune()) {
            double old = rawDamage;
            rawDamage *= ElementalFireNatureReactionsConfig.scorchedImmuneModifier;
            LOGGER.info(LOG_PREFIX + "   > ⚠️ 目标火焰免疫! 伤害衰减: {} -> {}", String.format("%.2f", old), String.format("%.2f", rawDamage));
        }

        if (ElementUtils.getDisplayEnhancement(target, ElementType.NATURE) > 0 ||
            ElementUtils.getDisplayResistance(target, ElementType.NATURE) > 0) {
            double old = rawDamage;
            rawDamage *= ElementalFireNatureReactionsConfig.scorchedNatureMultiplier;
            LOGGER.info(LOG_PREFIX + "   > 🌿 自然属性易伤! 伤害加深: {} -> {}", String.format("%.2f", old), String.format("%.2f", rawDamage));
        }

        int fireProtLevel = getTotalEnchantmentLevel(Enchantments.FIRE_PROTECTION, target);
        int genProtLevel = getTotalEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, target);

        double fireProtReduction = (Math.min(fireProtLevel, 16) / 16.0) * ElementalFireNatureReactionsConfig.scorchedFireProtReduction;
        double genProtReduction = (Math.min(genProtLevel, 16) / 16.0) * ElementalFireNatureReactionsConfig.scorchedGenProtReduction;

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
                if (dy > -0.5 && dy < ElementalFireNatureReactionsConfig.steamCloudHeightCeiling) return true;
            }
        }
        return false;
    }
}