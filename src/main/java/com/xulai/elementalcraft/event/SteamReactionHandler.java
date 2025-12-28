package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
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
 * 1. 触发后生成 AreaEffectCloud。
 * - 高温蒸汽：半径和持续时间随层数增加。层数基于目标冰霜/潮湿强度。
 * - 低温蒸汽：半径固定，持续时间随层数增加。层数基于目标赤焰强度。
 * 2. 视觉特效使用服务端 Tick 维护的上升烟雾粒子，模拟真实蒸汽。
 * 3. 包含对服务器性能的优化（限制 Tick 频率）。
 * 4. 处理死亡逻辑，使被蒸汽烫死的生物掉落熟食。
 * <p>
 * English Description:
 * Core logic class for Steam Cloud elemental reactions.
 * Includes trigger logic (Dual-Track: Fire vs Wet/Frost, Frost vs Fire), defense calculation (Layered Defense), and persistent environmental effects.
 * Implements dynamic Steam Cloud mechanism:
 * 1. Generates AreaEffectCloud upon trigger.
 * - High-Heat: Radius and Duration scale with level. Level based on target's Frost/Wetness intensity.
 * - Low-Heat: Fixed Radius, Duration scales with level. Level based on target's Fire intensity.
 * 2. Visual effects use rising smoke particles maintained by Server Tick to simulate realistic steam.
 * 3. Includes server performance optimizations (Tick frequency limiting).
 * 4. Handles death logic to allow mobs scalded by steam to drop cooked items.
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class SteamReactionHandler {

    public static final String TAG_STEAM_CLOUD = "EC_SteamCloud";
    public static final String TAG_HIGH_HEAT = "EC_HighHeat";
    public static final String TAG_LEVEL_PREFIX = "EC_Level_";
    
    // NBT Key for tracking time spent inside low-heat steam
    // 用于追踪在低温蒸汽中停留时间的 NBT 键
    private static final String NBT_CONDENSATION_TIMER = "EC_SteamCondensationTimer";

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
     * 包含智能原始伤害还原逻辑，以防止原版火焰保护机制导致的双重减伤。
     * 同时引入了配置文件定义的减伤上限限制。
     * <p>
     * Defense Logic Listener.
     * Listens to LivingDamageEvent for steam scalding, calculating damage reduction (Resistance, Enchantments) and floor damage.
     * Includes smart raw damage restoration logic to prevent double reduction caused by vanilla Fire Protection mechanics.
     * Also implements configurable reduction caps.
     *
     * @param event 伤害事件 / Damage event
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        // 只处理蒸汽烫伤类型的伤害
        // Only process steam scalding damage type
        if (event.getSource().is(ModDamageTypes.STEAM_SCALDING)) {
            processDefenseLogic(event);
        }
    }

    /**
     * 死亡逻辑监听器。
     * 处理被蒸汽烫死的生物掉落物逻辑。
     * 如果死因是蒸汽烫伤，强制将实体设为燃烧状态，以触发原版战利品表的熟食掉落（如熟牛肉、熟猪排）。
     * <p>
     * Death Logic Listener.
     * Handles drops for mobs killed by steam scalding.
     * If the cause of death is steam scalding, forcibly sets the entity on fire to trigger Vanilla LootTable cooked drops (e.g., Cooked Beef, Cooked Porkchop).
     *
     * @param event 死亡事件 / Death event
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().is(ModDamageTypes.STEAM_SCALDING)) {
            // 设置 1 秒着火时间，足以触发 loot table 的 furnace_smelt 条件
            // Set 1 second on fire, enough to trigger furnace_smelt condition in loot table
            event.getEntity().setSecondsOnFire(1);
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
    //                                  Logic Implementation / 逻辑实现
    // =================================================================================================

    /**
     * 处理蒸汽反应的触发逻辑。
     * 判断攻击元素与目标状态，决定是否生成蒸汽云或执行自我干燥。
     * 更新：
     * 1. 低温蒸汽：基于目标(Target)的赤焰强化分级，使用 steamCondensationStepFire。
     * 2. 高温蒸汽：基于目标(Target)的冰霜强化，使用 steamCondensationStepFrost。
     * <p>
     * Processes the trigger logic for steam reactions.
     * Determines whether to spawn a steam cloud or perform self-drying based on attack element and target state.
     * Update:
     * 1. Low-Heat Steam: Leveled based on Target's Fire Enhancement using steamCondensationStepFire.
     * 2. High-Heat Steam: Leveled based on Target's Frost Enhancement using steamCondensationStepFrost.
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

                // 攻击者赤焰强度 >= 阈值 (默认50) 才能触发
                // Attacker Fire Power >= Threshold (Default 50) to trigger
                if (firePower >= threshold) {
                    // 触发成功：生成高温蒸汽
                    // Trigger success: Spawn high-heat steam
                    int fuelLevel = 1;

                    if (targetIsWet) {
                        // 燃料：潮湿层数
                        // Fuel: Wetness Level
                        fuelLevel = targetWetness;
                    } else if (targetElement == ElementType.FROST) {
                        // 燃料：目标冰霜强化
                        // Fuel: Target Frost Enhancement
                        // 计算公式：1 + (目标冰霜 / 冰霜步长)
                        // Formula: 1 + (Target Frost / Frost Step)
                        int targetFrostPower = ElementUtils.getDisplayEnhancement(target, ElementType.FROST);
                        int step = Math.max(1, ElementalReactionConfig.steamCondensationStepFrost); // 使用冰霜步长配置
                        fuelLevel = 1 + (targetFrostPower / step);
                    }
                    
                    // 限制层数 1~5
                    // Clamp level 1~5
                    fuelLevel = Math.min(fuelLevel, 5);

                    spawnSteamCloud(target, true, fuelLevel);
                    // Debug 提示
                    DebugCommand.sendSteamTriggerLog(attacker, true, fuelLevel);

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
                
                // 攻击者冰霜强度 >= 阈值 才能触发
                // Attacker Frost Power >= Threshold to trigger
                if (frostPower >= threshold) {
                    // 燃料：目标赤焰强化
                    // Fuel: Target Fire Enhancement
                    // 计算公式：1 + (目标赤焰 / 赤焰步长)
                    // Formula: 1 + (Target Fire / Fire Step)
                    int targetFirePower = ElementUtils.getDisplayEnhancement(target, ElementType.FIRE);
                    int step = Math.max(1, ElementalReactionConfig.steamCondensationStepFire); // 使用赤焰步长配置
                    
                    int level = 1 + (targetFirePower / step);
                    level = Math.min(level, 5);

                    spawnSteamCloud(target, false, level);
                    // Debug 提示
                    DebugCommand.sendSteamTriggerLog(attacker, false, level);
                }
            }
        }
    }

    /**
     * 处理防御逻辑。
     * 计算抗性、附魔减伤及易伤保底机制。
     * <p>
     * 关键修复：
     * 1. 还原真实伤害，防止原版双重减伤。
     * 2. 严格执行用户公式（16级=50%）。
     * 3. 重新引入配置文件上限限制，确保 Config 调整生效。
     * <p>
     * Processes defense logic.
     * Calculates resistance, enchantment damage reduction, and vulnerability floor mechanisms.
     * <p>
     * Critical Fixes:
     * 1. Restore true raw damage to prevent vanilla double reduction.
     * 2. Strictly enforce user formula (16 levels = 50%).
     * 3. Re-introduce configuration caps to ensure config adjustments take effect.
     *
     * @param event 伤害事件 / Damage event
     */
    private static void processDefenseLogic(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        float currentDamage = event.getAmount();

        // 1. 免疫判定
        // 1. Immunity Check
        if (checkImmunity(target)) {
            event.setAmount(0);
            event.setCanceled(true);
            return;
        }

        // 2. 智能还原原始伤害
        // 2. Smart Raw Damage Restoration
        float trueRawDamage = currentDamage;
        boolean wasReducedByVanilla = false;

        // 只有当伤害类型是火焰，且受害者有附魔保护时，才尝试还原
        // Only attempt restoration if damage type is fire and victim has enchantment protection
        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
            // 获取原版计算的保护点数 (EPF)
            // Get Vanilla calculated protection points (EPF)
            int vanillaEPF = EnchantmentHelper.getDamageProtection(target.getArmorSlots(), event.getSource());
            
            if (vanillaEPF > 0) {
                // 原版公式：Damage * (1 - clamp(EPF, 0, 20) * 0.04)
                // Vanilla Formula: Damage * (1 - clamp(EPF, 0, 20) * 0.04)
                float reductionRatio = Math.min(vanillaEPF, 20) * 0.04f;
                
                // 避免除以0
                // Avoid division by zero
                if (reductionRatio < 1.0f) {
                    trueRawDamage = currentDamage / (1.0f - reductionRatio);
                    wasReducedByVanilla = true;
                }
            }
        }

        // 3. 自定义附魔减伤计算
        // 3. Custom Enchantment Reduction Calculation
        
        int totalFireProtLevel = getTotalEnchantmentLevel(Enchantments.FIRE_PROTECTION, target);
        int totalProtLevel = getTotalEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, target);

        // 系数定义 (固定公式)：
        // 火焰保护：16级抵消0.50 -> 每级 0.03125
        // 保护：16级抵消0.25 -> 每级 0.015625
        // Coefficient Definitions (Fixed Formula):
        // Fire Protection: 16 levels offset 0.50 -> 0.03125 per level
        // Protection: 16 levels offset 0.25 -> 0.015625 per level
        final double FIRE_PROT_FACTOR = 0.03125;
        final double PROT_FACTOR = 0.015625;

        // 读取配置文件上限
        // Read caps from configuration
        double maxFireCap = ElementalReactionConfig.steamMaxFireProtCap;
        double maxGeneralCap = ElementalReactionConfig.steamMaxGeneralProtCap;

        // 计算理论减免值
        // Calculate theoretical reduction values
        double calculatedFireRed = totalFireProtLevel * FIRE_PROT_FACTOR;
        double calculatedProtRed = totalProtLevel * PROT_FACTOR;

        // 应用配置文件上限限制
        // Apply configuration caps
        double actualFireRed = Math.min(calculatedFireRed, maxFireCap);
        double actualProtRed = Math.min(calculatedProtRed, maxGeneralCap);

        // 总减伤比例（直接累加，上限设为 1.0 即 100%）
        // Total reduction ratio (Sum directly, cap at 1.0 aka 100%)
        double totalReduction = Math.min(actualFireRed + actualProtRed, 1.0);
        
        // 基于“真·原始伤害”应用减伤
        // Apply reduction based on "True Raw Damage"
        float reducedDamage = trueRawDamage * (float) (1.0 - totalReduction);

        // 4. 易伤保底机制 (冰霜/自然生物受到的伤害不能被减免得太低)
        // 4. Vulnerability Floor Mechanism (Frost/Nature mobs cannot reduce damage too much)
        ElementType type = ElementUtils.getElementType(target);
        if (type == ElementType.FROST || type == ElementType.NATURE) {
            float floorRatio = (float) ElementalReactionConfig.steamDamageFloorRatio;
            float floorLimit = trueRawDamage * floorRatio; // 保底基于原始伤害计算 / Floor based on raw damage

            if (reducedDamage < floorLimit) {
                reducedDamage = floorLimit;
            }
        }

        // Debug Log
        // 只有当玩家或其坐骑受到伤害时打印，避免刷屏
        if (!target.level().isClientSide) {
             System.out.println(String.format(
                "§e[EC-Debug] §r[蒸汽防御] 目标: %s | 还原伤害: %.2f | 火保(Lv%d): 理论%.2f/配置上限%.2f -> 实际%.2f | 保护(Lv%d): 理论%.2f/配置上限%.2f -> 实际%.2f | 总减免: %.2f%% | 最终伤害: %.2f",
                target.getName().getString(), 
                trueRawDamage,
                totalFireProtLevel,
                calculatedFireRed, maxFireCap, actualFireRed,
                totalProtLevel,
                calculatedProtRed, maxGeneralCap, actualProtRed,
                totalReduction * 100, 
                reducedDamage
            ));
        }

        event.setAmount(reducedDamage);
    }

    /**
     * 辅助方法：计算附魔总等级。
     * 遍历实体所有护甲槽位，累加指定附魔的等级。
     * <p>
     * Helper Method: Calculate total enchantment level.
     * Iterates through all armor slots of the entity and accumulates the level of the specified enchantment.
     *
     * @param ench   附魔类型 / Enchantment type
     * @param entity 实体 / Entity
     * @return 总等级 / Total Level
     */
    private static int getTotalEnchantmentLevel(Enchantment ench, LivingEntity entity) {
        int total = 0;
        for (ItemStack stack : entity.getArmorSlots()) {
            total += EnchantmentHelper.getItemEnchantmentLevel(ench, stack);
        }
        return total;
    }

    /**
     * 处理蒸汽云对内部实体的持续效果。
     * 包括高温烫伤、低温冷凝（增加潮湿）等。
     * 更新：低温蒸汽现在需要实体停留一段时间才会获得潮湿效果。
     * <p>
     * Processes persistent effects of steam clouds on entities inside.
     * Includes high-heat scalding and low-heat condensation (increasing wetness).
     * Update: Low-heat steam now requires entities to stay for a duration to gain wetness.
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
            // 高温逻辑保持不变：每秒伤害，立即移除潮湿
            // High-Heat Logic Unchanged: Damage per second, remove wetness immediately
            
            // 清除冷凝计时器 (因为高温优先)
            // Clear condensation timer (High heat priority)
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
            // 低温冷凝：滞留累计逻辑
            // Low-heat Condensation: Accumulation Logic
            
            // 获取当前计时器
            // Get current timer
            int currentTimer = entity.getPersistentData().getInt(NBT_CONDENSATION_TIMER);
            
            // 每次检测(每10 tick)增加10，模拟 tick 累计
            // Increment by 10 (since this method runs every 10 ticks)
            currentTimer += 10;
            
            // 获取配置的延迟时间 (默认 100 tick = 5秒)
            // Get configured delay time (Default 100 ticks)
            int delayThreshold = Math.max(10, ElementalReactionConfig.steamCondensationDelay);

            if (currentTimer >= delayThreshold) {
                // 达到阈值：增加潮湿
                // Threshold reached: Add wetness
                int currentWet = entity.getPersistentData().getInt(WetnessHandler.NBT_WETNESS);
                int max = ElementalReactionConfig.wetnessMaxLevel;
                
                if (currentWet < max) {
                    entity.getPersistentData().putInt(WetnessHandler.NBT_WETNESS, currentWet + 1);
                    // 播放一个气泡音效提示获得潮湿
                    // Play bubble sound to indicate wetness gain
                    entity.playSound(SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 1.0f, 1.0f);
                }
                
                // 重置计时器
                // Reset timer
                currentTimer = 0;
            }
            
            // 保存计时器
            // Save timer
            entity.getPersistentData().putInt(NBT_CONDENSATION_TIMER, currentTimer);
        }
    }

    // =================================================================================================
    //                                  Helper Methods / 辅助方法
    // =================================================================================================

    /**
     * 执行攻击者的自我干燥逻辑。
     * 消耗一定的伤害输出，移除自身的潮湿状态。
     * 更新：
     * 1. 同步更新 NBT 和 药水效果等级。
     * 2. 添加了 Debug 日志显示层数变化。
     * <p>
     * Executes self-drying logic for the attacker.
     * Consumes a portion of damage output to remove own wetness.
     * Update:
     * 1. Synchronize NBT and Potion Effect level.
     * 2. Added Debug logs to show level changes.
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

        // 更新 NBT 和 药水效果
        // Update NBT and Potion Effect
        if (newLevel == 0) {
            removeWetness(attacker);
        } else {
            attacker.getPersistentData().putInt(WetnessHandler.NBT_WETNESS, newLevel);
            
            // 同步更新药水效果显示（解决图标不刷新问题）
            // Sync update potion effect display (Fix icon not refreshing)
            attacker.removeEffect(com.xulai.elementalcraft.potion.ModMobEffects.WETNESS.get());
            attacker.addEffect(new MobEffectInstance(
                com.xulai.elementalcraft.potion.ModMobEffects.WETNESS.get(), 
                ElementalReactionConfig.wetnessDecayBaseTime * 20, 
                newLevel - 1, 
                true, 
                true
            ));
        }

        playSteamBurstEffect((ServerLevel) attacker.level(), attacker, 0.5f, Math.min(layersToRemove, 5), true);

        // 发送调试日志 (使用本地化方法)
        // Send Debug Log (Use localized method)
        DebugCommand.sendDryLog(attacker, currentWetness, newLevel, layersToRemove, fireEnhancement);
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
     * 更新：
     * 1. 只有高温蒸汽半径随层数增加。
     * 2. 根据是高温还是低温，读取不同的持续时间配置。
     * <p>
     * Spawns a dynamic steam cloud entity.
     * Sets radius, duration, particles, and related tags.
     * Update:
     * 1. Only high-heat steam radius scales with level.
     * 2. Reads different duration configs based on high/low heat.
     *
     * @param target     目标实体 / Target entity
     * @param isHighHeat 是否为高温蒸汽 / Whether it is high-heat steam
     * @param fuelLevel  燃料等级（通常为目标潮湿层数） / Fuel level (usually target wetness level)
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
        
        // 只有高温蒸汽半径才随层数增加
        // Only high-heat steam radius scales with level
        float radius = isHighHeat ? baseRadius + (level - 1.0f) * radiusInc : baseRadius;

        // 动态时间计算 (区分高温/低温配置)
        // Dynamic Duration Calculation (Distinct High/Low configs)
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
     * @param level       服务端世界 / Server Level
     * @param target      目标实体 / Target entity
     * @param radius      爆发半径 / Burst radius
     * @param intensity   强度等级 / Intensity level
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