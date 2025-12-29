package com.xulai.elementalcraft.util;

import com.mojang.logging.LogUtils;
import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ElementalReactionConfig;
import com.xulai.elementalcraft.config.ForcedItemConfig;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.potion.ModMobEffects;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.*;

/**
 * GlobalDebugLogger (Fixed)
 * ==========================================
 * 全局调试监控器 - 修复版
 * 1. 修复了日志格式化导致参数错位的问题 ({:.2f} -> String.format)。
 * 2. 修复了附魔等级显示只显示单件最高的问题 (改为累加全身等级)。
 * 3. [Fix] 移除了 onLivingHeal 监听器，解决了自然回血被误报为“汲取回血”的问题。
 * 4. [New] 增加了潮湿状态下受赤焰攻击的详细减伤诊断日志。
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GlobalDebugLogger {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String LOG_PREFIX = "§e[EC-Debug] §r";

    // 缓存防止 Tick 刷屏
    private static final Map<Integer, Integer> wetnessCache = new WeakHashMap<>();
    private static final Map<Integer, Long> inventoryLogCooldown = new WeakHashMap<>();

    private static boolean isDebugEnabled() {
        return DebugMode.hasAnyDebugEnabled();
    }

    // ================= 1. 配置文件监控 (Config) =================
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
                LOGGER.info(LOG_PREFIX + "  > 生成概率: 敌对 {}, 中立 {}",
                        ElementalConfig.MOB_ATTRIBUTE_CHANCE_HOSTILE.get(),
                        ElementalConfig.MOB_ATTRIBUTE_CHANCE_NEUTRAL.get());
                LOGGER.info(LOG_PREFIX + "  > 群系偏好: 炎热(Fire) {}%, 寒冷(Frost) {}%, 森林(Nature) {}%, 雷雨(Thunder) {}%",
                        ElementalConfig.HOT_FIRE_BIAS.get(), ElementalConfig.COLD_FROST_BIAS.get(),
                        ElementalConfig.FOREST_NATURE_BIAS.get(), ElementalConfig.THUNDERSTORM_THUNDER_BIAS.get());
            } else if (fileName.contains("reactions")) {
                LOGGER.info(LOG_PREFIX + "  > 反应开关: 蒸汽 {}, 潮湿上限 Lv.{}",
                        ElementalReactionConfig.steamReactionEnabled,
                        ElementalReactionConfig.wetnessMaxLevel);
                LOGGER.info(LOG_PREFIX + "  > 阈值设置: 火触发 {}, 冰触发 {}",
                        ElementalReactionConfig.steamTriggerThresholdFire,
                        ElementalReactionConfig.steamTriggerThresholdFrost);
            } else if (fileName.contains("forced-items")) {
                LOGGER.info(LOG_PREFIX + "  > 强制物品: 武器 {} 项, 护甲 {} 项",
                        ForcedItemConfig.FORCED_WEAPONS.get().size(),
                        ForcedItemConfig.FORCED_ARMOR.get().size());
            }
        }
    }

    // ================= 2. 生物生成与属性 (Spawning) =================
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!isDebugEnabled() || event.getLevel().isClientSide) return;

        // --- A. 蒸汽云生成监控 ---
        if (event.getEntity() instanceof AreaEffectCloud cloud) {
            logSteamCloudSpawn(cloud);
            return;
        }

        // --- B. 生物属性生成监控 ---
        if (!(event.getEntity() instanceof LivingEntity entity) || entity instanceof Player) return;

        boolean hasAttributes = false;
        for (ElementType type : ElementType.values()) {
            if (type != ElementType.NONE && (ElementUtils.getDisplayEnhancement(entity, type) > 0 || ElementUtils.getDisplayResistance(entity, type) > 0)) {
                hasAttributes = true;
                break;
            }
        }

        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        boolean isForced = checkIsForcedEntity(entityId);
        boolean isInteresting = hasAttributes ||
                ElementalConfig.cachedBlacklist.contains(entityId) ||
                isForced;

        if (!isInteresting) return;

        StringBuilder log = new StringBuilder();
        log.append(LOG_PREFIX).append("🌱 生物生成判定: ").append(entity.getName().getString()).append("\n");
        log.append("  📍 位置: ").append(entity.blockPosition().toShortString()).append(" (ID: ").append(entityId).append(")\n");

        if (ElementalConfig.cachedBlacklist.contains(entityId)) {
            log.append("  🚫 [黑名单]: 禁止生成属性\n");
            LOGGER.info(log.toString());
            return;
        }
        if (isForced) {
            log.append("  ⚠️ [强制配置]: 应用 ForcedConfig，跳过随机\n");
        }

        Holder<Biome> biome = event.getLevel().getBiome(entity.blockPosition());
        String biomeId = biome.unwrapKey().map(k -> k.location().toString()).orElse("unknown");
        log.append("  🌍 [群系判定]: ").append(biomeId).append("\n");
        if (event.getLevel().isThundering()) log.append("    ⚡ 天气: 雷雨 (Thunder Bias)\n");

        Map<String, String> results = new LinkedHashMap<>();
        for (ElementType type : ElementType.values()) {
            if (type == ElementType.NONE) continue;
            int str = ElementUtils.getDisplayEnhancement(entity, type);
            int res = ElementUtils.getDisplayResistance(entity, type);
            if (str > 0 || res > 0) {
                results.put(type.getDisplayName().getString(), String.format("强:%d/抗:%d", str, res));
            }
        }

        if (!results.isEmpty()) {
            log.append("  ✅ [最终属性]: ").append(results).append("\n");
        } else {
            log.append("  🎲 [随机结果]: 未命中概率或无属性\n");
        }

        LOGGER.info(log.toString());
    }

    private static boolean checkIsForcedEntity(String entityId) {
        return ElementalConfig.FORCED_ENTITIES.get().stream()
                .anyMatch(s -> s.replace("\"", "").trim().startsWith(entityId + ","));
    }

    private static void logSteamCloudSpawn(AreaEffectCloud cloud) {
        ParticleOptions particle = cloud.getParticle();
        String type = "未知";
        if (particle.getType() == ParticleTypes.CAMPFIRE_COSY_SMOKE) type = "高温蒸汽 (Scalding)";
        else if (particle.getType() == ParticleTypes.CLOUD) type = "低温蒸汽 (Condensing)";
        else return;

        LOGGER.info(LOG_PREFIX + "☁️ 蒸汽云生成: {} @ {} (半径: {}, 持续: {} ticks)",
                type, cloud.blockPosition().toShortString(), cloud.getRadius(), cloud.getDuration());
    }

    // ================= 3. 战斗系统与元素反应 (Combat & Reactions) =================
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!isDebugEnabled() || event.getEntity().level().isClientSide) return;

        LivingEntity target = event.getEntity();
        float amount = event.getAmount();

        // --- A. 蒸汽烫伤 ---
        if (event.getSource().is(ModDamageTypes.STEAM_SCALDING)) {
            logSteamScalding(target, amount);
            return;
        }

        // --- B. 蒸汽反应 (吸收伤害) ---
        if (event.getSource().is(DamageTypeTags.IS_FIRE) && ElementalReactionConfig.steamReactionEnabled) {
            if (ModMobEffects.WETNESS != null && ModMobEffects.WETNESS.isPresent()) {
                boolean isWet = target.hasEffect(ModMobEffects.WETNESS.get());
                boolean isFrost = ElementUtils.getElementType(target) == ElementType.FROST;

                if (isWet || isFrost) {
                    float reduction = (float) ElementalReactionConfig.steamMaxReduction;

                    float original = amount / (1.0f - reduction);
                    LOGGER.info(LOG_PREFIX + "☁️ [蒸汽反应] 触发!");
                    LOGGER.info(String.format("  🛡️ 吸收: 原始 %.2f -> 结算 %.2f (减免 %d%%)",
                            original, amount, (int)(reduction * 100)));
                    return;
                }
            }
        }

        // --- C. 元素战斗公式详情 ---
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            logCombatDetails(attacker, target, amount);

            // --- D. 自然汲取 (Nature Siphon) ---
            ElementType atkType = ElementUtils.getConsistentAttackElement(attacker);
            if (atkType == ElementType.NATURE && target.hasEffect(ModMobEffects.WETNESS.get())) {
                LOGGER.info(LOG_PREFIX + "🌿 [自然汲取] 判定: 攻击者(自然) vs 目标(潮湿)");
            }
        }
    }

    private static void logSteamScalding(LivingEntity target, float damage) {
        int prot = getTotalEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, target);
        int fireProt = getTotalEnchantmentLevel(Enchantments.FIRE_PROTECTION, target);

        String damageStr = String.format("%.2f", damage);

        LOGGER.info(LOG_PREFIX + "♨️ [蒸汽烫伤]: {} 受到 {} 点伤害 (护甲附魔: 保护Lv.{}, 火保Lv.{})",
                target.getName().getString(), damageStr, prot, fireProt);
    }

    private static int getTotalEnchantmentLevel(Enchantment ench, LivingEntity entity) {
        int total = 0;
        for (ItemStack stack : entity.getArmorSlots()) {
            total += EnchantmentHelper.getItemEnchantmentLevel(ench, stack);
        }
        return total;
    }

    private static void logCombatDetails(LivingEntity attacker, LivingEntity target, float finalDamage) {
        ElementType atkElem = ElementUtils.getConsistentAttackElement(attacker);
        if (atkElem == ElementType.NONE && finalDamage < 2.0f) return;

        StringBuilder sb = new StringBuilder();
        sb.append(LOG_PREFIX).append("⚔️ [战斗结算] ").append(attacker.getName().getString()).append(" -> ").append(target.getName().getString()).append("\n");

        sb.append("  🗡️ 攻击属性: ").append(atkElem.getDisplayName().getString())
                .append(" (强化点: ").append(ElementUtils.getDisplayEnhancement(attacker, atkElem)).append(")\n");

        ElementType defElem = ElementType.NONE;
        int maxRes = 0;
        for(ElementType t : ElementType.values()) {
            if(t == ElementType.NONE) continue;
            int r = ElementUtils.getDisplayResistance(target, t);
            if(r > maxRes) { maxRes = r; defElem = t; }
        }

        sb.append("  🛡️ 防御属性: ").append(defElem == ElementType.NONE ? "无" : defElem.getDisplayName().getString())
                .append(" (最大抗性: ").append(maxRes).append(")\n");

        float restraint = ElementalConfig.getRestraintMultiplier(atkElem, defElem);
        String rel = "中立";
        if (restraint > 1.0) rel = "§c克制 (Restrain)";
        else if (restraint < 1.0 && restraint > 0) rel = "§9微弱 (Weak)";

        sb.append("  ⚖️ 关系修正: ").append(rel).append(" (x").append(String.format("%.1f", restraint)).append(")\n");
        sb.append("  💥 最终伤害: ").append(String.format("%.2f", finalDamage));

        // --- 强制潮湿诊断 (Force Wetness Diagnostic) ---
        // 只要攻击者是赤焰属性，就强制打印目标的所有潮湿相关状态，用于排查数据不同步问题
        if (atkElem == ElementType.FIRE) {
            sb.append("\n  🔍 [赤焰攻击-潮湿诊断]");
            
            // 1. 检查 NBT
            CompoundTag targetData = target.getPersistentData();
            boolean hasNbtKey = targetData.contains("EC_WetnessLevel");
            int nbtLevel = hasNbtKey ? targetData.getInt("EC_WetnessLevel") : -1;
            sb.append("\n    - NBT Key 存在: ").append(hasNbtKey);
            sb.append("\n    - NBT 层数: ").append(nbtLevel);

            // 2. 检查药水效果
            boolean hasEffect = target.hasEffect(ModMobEffects.WETNESS.get());
            int effectLevel = hasEffect ? (target.getEffect(ModMobEffects.WETNESS.get()).getAmplifier() + 1) : 0;
            sb.append("\n    - 药水效果存在: ").append(hasEffect);
            sb.append("\n    - 药水等级: ").append(effectLevel);

            // 3. 理论计算
            if (nbtLevel > 0) {
                float reductionPerLevel = (float) ElementalReactionConfig.wetnessFireReduction;
                float theoretical = nbtLevel * reductionPerLevel;
                sb.append("\n    - 理论减免: ").append((int)(theoretical * 100)).append("%");
            } else {
                sb.append("\n    - 结果: 未触发减伤 (NBT <= 0)");
            }
        }

        LOGGER.info(sb.toString());
    }

    // ================= 4. 状态系统 (Status - Wetness) =================
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!isDebugEnabled() || event.getEntity().level().isClientSide) return;

        LivingEntity entity = event.getEntity();
        if (entity.tickCount % 10 != 0) return;

        CompoundTag tag = entity.getPersistentData();
        if (!tag.contains("elementalcraft:wetness")) return;

        int currentLevel = tag.getInt("elementalcraft:wetness");
        int entityId = entity.getId();
        int lastLevel = wetnessCache.getOrDefault(entityId, 0);

        if (currentLevel != lastLevel) {
            String change = currentLevel > lastLevel ? "§b增加" : "§6减少";
            String reason = "未知";
            if (entity.isInWaterRainOrBubble()) reason = "雨/水环境";
            else if (entity.isOnFire()) reason = "烘干/燃烧";
            else if (currentLevel < lastLevel) reason = "自然衰减";

            LOGGER.info(LOG_PREFIX + "💧 [潮湿变更] {}: Lv.{} -> Lv.{} ({}) 原因推测: {}",
                    entity.getName().getString(), lastLevel, currentLevel, change, reason);

            if (currentLevel == 0) wetnessCache.remove(entityId);
            else wetnessCache.put(entityId, currentLevel);
        }
    }

    // ================= 5. 物品与强制属性同步 (Inventory) =================
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!isDebugEnabled() || event.phase != TickEvent.Phase.END || event.side.isClient()) return;

        ServerPlayer player = (ServerPlayer) event.player;
        if (player.tickCount % 40 != 0) return;

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) return;

        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return;

        boolean isForced = checkIsForcedWeapon(id.toString());

        if (isForced) {
            boolean hasEnchant = false;
            Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
            for (Enchantment e : enchants.keySet()) {
                if (e.getDescriptionId().contains("elementalcraft")) {
                    hasEnchant = true;
                    break;
                }
            }

            if (!hasEnchant) {
                long now = System.currentTimeMillis();
                if (now - inventoryLogCooldown.getOrDefault(player.getId(), 0L) > 5000) {
                    LOGGER.info(LOG_PREFIX + "🎒 [物品同步] 检测到强制武器未附魔: {}", stack.getHoverName().getString());
                    LOGGER.info("    期待: 配置中存在 ({})", id);
                    LOGGER.info("    状态: 等待 InventoryAutoForceEvents 同步...");
                    inventoryLogCooldown.put(player.getId(), now);
                }
            }
        }
    }

    private static boolean checkIsForcedWeapon(String itemId) {
         return ForcedItemConfig.FORCED_WEAPONS.get().stream()
                 .anyMatch(s -> s.replace("\"", "").trim().startsWith(itemId + ","));
    }
}