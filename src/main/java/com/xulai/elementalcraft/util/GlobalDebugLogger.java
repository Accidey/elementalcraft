// src/main/java/com/xulai/elementalcraft/util/GlobalDebugLogger.java
package com.xulai.elementalcraft.util;

import com.mojang.logging.LogUtils;
import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ElementalReactionConfig;
import com.xulai.elementalcraft.config.ForcedItemConfig;
import com.xulai.elementalcraft.event.SteamReactionHandler;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.potion.ModMobEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
 * ==========================================
 * 中文说明：
 * 全局调试监控器（独立旁路版 - V1.5.1 Full Integration）。
 * 负责汇集模组内各个系统的调试信息，并将其格式化输出到服务器控制台。
 *
 * 【V1.5.1 集成更新】：
 * 1. 完整迁移了 ReactionHandler 中的传染机制 (Contagion) 调试日志。
 * 2. 补全了自然寄生、寄生吸取、毒火爆燃的详细触发判定日志。
 * 3. 保持了对配置加载、生物生成、状态变化的监控。
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GlobalDebugLogger {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String LOG_PREFIX = "§e[EC-Debug] §r";

    // 缓存防止 Tick 刷屏
    private static final Map<Integer, Integer> wetnessCache = new WeakHashMap<>();
    private static final Map<Integer, Boolean> scorchedCache = new WeakHashMap<>();
    private static final Map<Integer, Boolean> spreadCache = new WeakHashMap<>(); // 传染状态缓存

    private static boolean isDebugEnabled() {
        return DebugMode.hasAnyDebugEnabled();
    }

    // ================= 1. 配置加载监控 (Config) =================
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
                // 基础反应开关与潮湿
                LOGGER.info(LOG_PREFIX + "  > [基础] 反应开关: 蒸汽 {}, 潮湿上限 Lv.{}",
                        ElementalReactionConfig.steamReactionEnabled,
                        ElementalReactionConfig.wetnessMaxLevel);
                
                // V1.5 新增配置监控
                LOGGER.info(LOG_PREFIX + "  > [V1.5] 孢子配置: 堆叠 {}, 伤害 {}, 减速 {}%",
                        ElementalReactionConfig.sporeMaxStacks, ElementalReactionConfig.sporePoisonDamage,
                        (int)(ElementalReactionConfig.sporeSpeedReduction * 100));
                
                // 传染配置
                LOGGER.info(LOG_PREFIX + "  > [V1.5] 传染配置: 半径 {}, 步长 {}, 强度 {}",
                        ElementalReactionConfig.contagionBaseRadius,
                        ElementalReactionConfig.contagionRadiusPerStack,
                        ElementalReactionConfig.contagionIntensityRatio);
            }
        }
    }

    // ================= 2. 元素反应监控 (Reactions V1.5) =================
    
    /**
     * 监控主动攻击触发的反应（动态寄生、寄生吸取、毒火爆燃）。
     * 使用 HIGH 优先级，以便在功能代码（NORMAL 优先级）执行之前捕获状态。
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void monitorReactions(LivingDamageEvent event) {
        if (!isDebugEnabled() || event.getEntity().level().isClientSide) return;

        Entity source = event.getSource().getEntity();
        if (!(source instanceof LivingEntity attacker)) return;
        LivingEntity target = event.getEntity();

        // 获取一致性攻击属性
        com.xulai.elementalcraft.util.ElementType attackType = ElementUtils.getConsistentAttackElement(attacker);
        double naturePower = ElementUtils.getDisplayEnhancement(attacker, com.xulai.elementalcraft.util.ElementType.NATURE);
        double firePower = ElementUtils.getDisplayEnhancement(attacker, com.xulai.elementalcraft.util.ElementType.FIRE);

        // ---------------------------------------------------------
        // A. 自然反应：动态寄生 & 吸取
        // ---------------------------------------------------------
        if (attackType == com.xulai.elementalcraft.util.ElementType.NATURE) {
            // 寄生判定
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
                
                if (chance > 0.05) {
                    LOGGER.info(LOG_PREFIX + "🎲 [动态寄生] 判定: 攻击者 {} (自然:{}, 潮湿:{}) -> 挂标概率: {}%",
                            attacker.getName().getString(), (int)naturePower, attackerWetness, String.format("%.1f", chance * 100));
                }
            }

            // 吸取判定
            CompoundTag targetData = target.getPersistentData();
            if (targetData.getInt("EC_WetnessLevel") > 0) {
                boolean onCooldown = attacker.getPersistentData().getLong("ec_drain_cd") > attacker.level().getGameTime();
                if (naturePower >= ElementalReactionConfig.natureSiphonThreshold) {
                    if (!onCooldown) {
                        LOGGER.info(LOG_PREFIX + "🌿 [寄生吸取-触发] {} (自然:{}) -> {} (潮湿:Lv.{}) | 准备吸取", 
                                attacker.getName().getString(), (int)naturePower, target.getName().getString(), targetData.getInt("EC_WetnessLevel"));
                    }
                }
            }
        }

        // ---------------------------------------------------------
        // B. 赤焰反应：毒火爆燃 (Toxic Blast)
        // ---------------------------------------------------------
        if (attackType == com.xulai.elementalcraft.util.ElementType.FIRE) {
            if (target.hasEffect(Objects.requireNonNull(ModMobEffects.SPORES.get())) && !event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
                
                if (firePower >= ElementalReactionConfig.blastTriggerThreshold) {
                    var effectInstance = target.getEffect(Objects.requireNonNull(ModMobEffects.SPORES.get()));
                    int stacks = (effectInstance != null) ? (effectInstance.getAmplifier() + 1) : 0;
                    
                    if (stacks < 3) {
                        LOGGER.info(LOG_PREFIX + "🔥 [毒火爆燃-弱效引燃] {} (赤焰:{}) -> {} (孢子:{}层) | 转化为灼烧", 
                                attacker.getName().getString(), (int)firePower, target.getName().getString(), stacks);
                    } else {
                        double radius = ElementalReactionConfig.blastBaseRange + ((stacks - 3) * ElementalReactionConfig.blastGrowthRange);
                        LOGGER.info(LOG_PREFIX + "💥 [毒火爆燃-终结爆燃] {} (赤焰:{}) -> {} (孢子:{}层) | 爆炸半径: {}", 
                                attacker.getName().getString(), (int)firePower, target.getName().getString(), stacks, String.format("%.1f", radius));
                    }
                }
            }
        }
    }

    /**
     * 监控防御反制反应（野火喷射）。
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void monitorCounterReactions(LivingHurtEvent event) {
        if (!isDebugEnabled() || event.getEntity().level().isClientSide) return;

        LivingEntity victim = event.getEntity();

        // ---------------------------------------------------------
        // D. 自然反应：野火喷射 (Wildfire Ejection)
        // ---------------------------------------------------------
        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
            double naturePower = ElementUtils.getDisplayEnhancement(victim, com.xulai.elementalcraft.util.ElementType.NATURE);
            boolean onCooldown = victim.getPersistentData().getLong("ec_wildfire_cd") > victim.level().getGameTime();

            if (naturePower >= ElementalReactionConfig.wildfireTriggerThreshold) {
                if (!onCooldown) {
                    LOGGER.info(LOG_PREFIX + "🔊 [野火喷射-触发] {} (自然:{}) 受到火伤 | 激活 Warden 声波反制!", 
                            victim.getName().getString(), (int)naturePower);
                }
            }
        }
    }

    // ================= 3. 状态与环境监控 (Status & Environment) =================

    /**
     * 监控 NBT 状态变化（潮湿、灼烧）以及专门的传染扩散监控。
     */
    @SubscribeEvent
    public static void monitorStatusAndContagion(LivingEvent.LivingTickEvent event) {
        if (!isDebugEnabled() || event.getEntity().level().isClientSide) return;
        LivingEntity entity = event.getEntity();
        
        // 性能优化：每 10 tick 检查一次状态
        if (entity.tickCount % 10 != 0) return;

        int id = entity.getId();
        CompoundTag data = entity.getPersistentData();

        // --- A. 潮湿监控 ---
        int curWet = data.getInt("EC_WetnessLevel");
        int lastWet = wetnessCache.getOrDefault(id, 0);
        if (curWet != lastWet) {
            LOGGER.info(LOG_PREFIX + "💧 [潮湿变动] {}: {} -> {}", entity.getName().getString(), lastWet, curWet);
            if (curWet == 0) wetnessCache.remove(id);
            else wetnessCache.put(id, curWet);
        }

        // --- B. 灼烧监控 (Scorched) ---
        boolean isScorched = data.contains("ec_scorched_ticks");
        boolean wasScorched = scorchedCache.getOrDefault(id, false);
        
        if (isScorched != wasScorched) {
            if (isScorched) {
                int duration = data.getInt("ec_scorched_ticks");
                int strength = data.getInt("ec_scorched_str");
                LOGGER.info(LOG_PREFIX + "🔥 [灼烧-开始] {}: 强度 {}, 持续 {} tick", entity.getName().getString(), strength, duration);
            } else {
                LOGGER.info(LOG_PREFIX + "🔥 [灼烧-结束] {}: 状态已移除", entity.getName().getString());
            }
            scorchedCache.put(id, isScorched);
        }

        // --- C. 环境传染监控 (Contagion) ---
        // 这里复刻了 ReactionHandler 的判断逻辑，用于旁路监控扩散事件
        if (ModMobEffects.SPORES.isPresent() && entity.hasEffect(ModMobEffects.SPORES.get())) {
            var effect = entity.getEffect(ModMobEffects.SPORES.get());
            int stacks = (effect != null) ? effect.getAmplifier() + 1 : 0;
            
            // 只有当符合触发条件（>=3层）且未被标记扩散时，才模拟扫描并输出日志
            if (stacks >= 3 && !data.getBoolean("ec_spreaded") && !data.getBoolean("ec_infected")) {
                // 为了避免重复刷屏，只在特定的检查周期内输出
                if (entity.tickCount % ElementalReactionConfig.contagionCheckInterval == 0) {
                    logContagionEvent(entity, stacks);
                }
            }
        }
    }
    
    private static void logContagionEvent(LivingEntity source, int stacks) {
        LOGGER.info(LOG_PREFIX + "☣️ [环境传染] 宿主: {} -> 孢子浓度过高，触发扩散!", source.getName().getString());
        
        double radius = ElementalReactionConfig.contagionBaseRadius + ((stacks - 3) * ElementalReactionConfig.contagionRadiusPerStack);
        AABB area = source.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = source.level().getEntitiesOfClass(LivingEntity.class, area);
        
        LOGGER.info(LOG_PREFIX + "  > 扫描半径: {}, 潜在目标: {}", String.format("%.1f", radius), targets.size());
        
        for (LivingEntity target : targets) {
            if (target == source) continue;
            boolean wasInfected = target.getPersistentData().getBoolean("ec_infected");
            if (!wasInfected) {
                LOGGER.info(LOG_PREFIX + "  > 💉 传染目标: {} | 位置: [{}, {}, {}]", 
                        target.getName().getString(), 
                        (int)target.getX(), (int)target.getY(), (int)target.getZ());
            }
        }
    }

    // ================= 4. 伤害修正与特殊伤害监控 =================
    
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamageDebug(LivingDamageEvent event) {
        if (!isDebugEnabled() || event.getEntity().level().isClientSide) return;
        
        LivingEntity target = event.getEntity();
        float amount = event.getAmount();

        // --- 1. 孢子修正监控 ---
        if (target.hasEffect(Objects.requireNonNull(ModMobEffects.SPORES.get()))) {
            var effectInstance = target.getEffect(Objects.requireNonNull(ModMobEffects.SPORES.get()));
            int stacks = (effectInstance != null) ? (effectInstance.getAmplifier() + 1) : 0;
            
            // 物理硬化检测
            boolean isFire = event.getSource().is(DamageTypeTags.IS_FIRE);
            boolean isPhysical = !isFire 
                              && !event.getSource().is(DamageTypes.MAGIC) 
                              && !event.getSource().is(DamageTypes.INDIRECT_MAGIC) 
                              && !event.getSource().is(DamageTypeTags.IS_EXPLOSION);

            if (isFire) {
                LOGGER.info(LOG_PREFIX + "🔥 [孢子易伤] 目标:{} ({}层) | 受到火伤加成", target.getName().getString(), stacks);
            } else if (isPhysical) {
                LOGGER.info(LOG_PREFIX + "🛡️ [孢子硬化] 目标:{} ({}层) | 物理减伤生效", target.getName().getString(), stacks);
            }
        }

        // --- 2. 蒸汽与热冲击监控 ---
        if (event.getSource().is(Objects.requireNonNull(ModDamageTypes.STEAM_SCALDING))) {
            LOGGER.info(LOG_PREFIX + "♨️ [蒸汽烫伤] {} 受到 {} 点伤害", target.getName().getString(), String.format("%.2f", amount));
        }
        
        if (event.getSource().is(DamageTypes.GENERIC) && scorchedCache.getOrDefault(target.getId(), false)) {
            if (target.isInWater()) {
                LOGGER.info(LOG_PREFIX + "🌊 [热冲击] {} 入水淬火! 受到 {} 点物理冲击", target.getName().getString(), String.format("%.2f", amount));
            }
        }
        
        // --- 3. 战斗数值监控 (基础) ---
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            logCombatDetails(attacker, target, amount);
        }
    }

    // ================= 5. 实体生成与消失 (Spawning) =================
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!isDebugEnabled() || event.getLevel().isClientSide) return;
        
        // --- 蒸汽云监控 ---
        if (event.getEntity() instanceof AreaEffectCloud cloud) {
            logSteamCloudSpawn(cloud);
        }
        
        // --- 生物属性生成监控 ---
        if (event.getEntity() instanceof LivingEntity entity && !(entity instanceof Player)) {
            logEntitySpawnAttributes(entity);
        }
    }
    
    // ================= 私有辅助方法 (Private Helpers) =================
    
    private static void logEntitySpawnAttributes(LivingEntity entity) {
        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        boolean isForced = checkIsForcedEntity(entityId);
        
        boolean hasAttributes = false;
        for (com.xulai.elementalcraft.util.ElementType type : com.xulai.elementalcraft.util.ElementType.values()) {
            if (type != com.xulai.elementalcraft.util.ElementType.NONE && 
               (ElementUtils.getDisplayEnhancement(entity, type) > 0 || ElementUtils.getDisplayResistance(entity, type) > 0)) {
                hasAttributes = true;
                break;
            }
        }
        
        if (hasAttributes || ElementalConfig.cachedBlacklist.contains(entityId) || isForced) {
            StringBuilder log = new StringBuilder();
            log.append(LOG_PREFIX).append("🌱 生物生成: ").append(entity.getName().getString()).append(" (ID: ").append(entityId).append(")\n");
            
            if (ElementalConfig.cachedBlacklist.contains(entityId)) {
                log.append("   🚫 [黑名单]: 禁止生成属性");
            } else {
                Map<String, String> results = new LinkedHashMap<>();
                for (com.xulai.elementalcraft.util.ElementType type : com.xulai.elementalcraft.util.ElementType.values()) {
                    if (type == com.xulai.elementalcraft.util.ElementType.NONE) continue;
                    int str = ElementUtils.getDisplayEnhancement(entity, type);
                    int res = ElementUtils.getDisplayResistance(entity, type);
                    if (str > 0 || res > 0) {
                        results.put(type.getDisplayName().getString(), String.format("强:%d/抗:%d", str, res));
                    }
                }
                if (!results.isEmpty()) log.append("   ✅ [属性]: ").append(results);
            }
            LOGGER.info(log.toString());
        }
    }

    private static void logSteamCloudSpawn(AreaEffectCloud cloud) {
        if (cloud.getTags().contains(SteamReactionHandler.TAG_STEAM_CLOUD)) {
            String type = cloud.getTags().contains(SteamReactionHandler.TAG_HIGH_HEAT) ? "高温" : "低温";
            LOGGER.info(LOG_PREFIX + "☁️ [蒸汽生成] 类型: {}, 半径: {}", type, String.format("%.1f", cloud.getRadius()));
        }
    }

    private static void logCombatDetails(LivingEntity attacker, LivingEntity target, float finalDamage) {
        com.xulai.elementalcraft.util.ElementType atkElem = ElementUtils.getConsistentAttackElement(attacker);
        if (atkElem != com.xulai.elementalcraft.util.ElementType.NONE || finalDamage > 2.0f) {
            LOGGER.info(LOG_PREFIX + "⚔️ [战斗结算] {} (属性: {}) -> {} | 伤害: {}", 
                    attacker.getName().getString(), atkElem.getDisplayName().getString(), 
                    target.getName().getString(), String.format("%.2f", finalDamage));
        }
    }
    
    private static boolean checkIsForcedEntity(String entityId) {
        return ElementalConfig.FORCED_ENTITIES.get().stream()
                .anyMatch(s -> s.replace("\"", "").trim().startsWith(entityId + ","));
    }
}