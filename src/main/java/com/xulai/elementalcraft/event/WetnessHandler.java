package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalReactionConfig;
import com.xulai.elementalcraft.potion.ModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * WetnessHandler
 *
 * 中文说明：
 * 负责处理潮湿系统的核心逻辑。
 * 主要功能包括：
 * 1. 免疫检查：在执行逻辑前检查生物是否属于水生生物、是否在下界或在黑名单中。
 * 2. 状态机逻辑：处理在水中（湿透）、雨中（积累）、干燥处（衰减）的状态切换。
 * 3. 属性抗性修正：根据潮湿层数增加受到的雷/冰伤害，减少受到的火伤害。
 * 4. 饱食度惩罚：潮湿状态下会额外消耗少量饱食度。
 *
 * English Description:
 * Handles the core logic for the Wetness System.
 * Main features include:
 * 1. Immunity Check: Checks if the mob is a water animal, in the Nether, or blacklisted before processing.
 * 2. State Machine: Handles state transitions between In-Water (Soaked), In-Rain (Accumulation), and Dry (Decay).
 * 3. Resistance Modification: Increases Thunder/Frost damage taken and decreases Fire damage taken based on wetness level.
 * 4. Exhaustion Penalty: Consumes extra food exhaustion when wet.
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class WetnessHandler {

    public static final String NBT_WETNESS = "EC_WetnessLevel";
    public static final String NBT_RAIN_TIMER = "EC_WetnessRainTimer";
    public static final String NBT_DECAY_TIMER = "EC_WetnessDecayTimer";
    public static final String NBT_LAST_EXHAUSTION = "EC_LastExhaustion";

    /**
     * 监听生物 Tick 事件。
     * 每秒（20 ticks）执行一次环境检测、潮湿层数更新和药水效果同步。
     *
     * Listens to Living Tick events.
     * Executes environment detection, wetness level updates, and potion effect synchronization every second (20 ticks).
     *
     * @param event 生物 Tick 事件 / Living tick event
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        if (entity.tickCount % 20 == 0) {
            handleWetnessLogic(entity);
            handleExhaustion(entity);
        }
    }

    /**
     * 监听生物受伤事件。
     * 根据目标的潮湿层数，调整受到的属性伤害（雷/冰增伤，火减伤）。
     *
     * Listens to Living Hurt events.
     * Adjusts elemental damage taken (Thunder/Frost bonus, Fire reduction) based on the target's wetness level.
     *
     * @param event 生物受伤事件 / Living hurt event
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();

        CompoundTag data = entity.getPersistentData();
        if (data.contains(NBT_WETNESS)) {
            int currentLevel = data.getInt(NBT_WETNESS);

            if (currentLevel > 0) {
                float originalDamage = event.getAmount();
                float finalDamage = originalDamage;

                if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
                    float factor = (float) ElementalReactionConfig.wetnessFireReduction * currentLevel;
                    factor = Math.min(1.0f, factor);

                    finalDamage = originalDamage * (1.0f - factor);
                }
                else if (event.getSource().is(DamageTypeTags.IS_LIGHTNING) || event.getSource().is(DamageTypeTags.IS_FREEZING)) {
                    float factor = (float) ElementalReactionConfig.wetnessResistModifier * currentLevel;

                    finalDamage = originalDamage * (1.0f + factor);
                }
                event.setAmount(finalDamage);
            }
        }
    }

    /**
     * 处理潮湿积累与衰减的核心状态机逻辑。
     * 包含环境检测（雨/雪/水）和相应的计时器处理。
     *
     * Handles the core state machine logic for wetness accumulation and decay.
     * Includes environment detection (Rain/Snow/Water) and corresponding timer processing.
     *
     * @param entity 目标实体 / Target entity
     */
    private static void handleWetnessLogic(LivingEntity entity) {
        if (isImmune(entity)) {
            clearWetnessData(entity);
            return;
        }

        Level level = entity.level();
        BlockPos pos = entity.blockPosition();
        CompoundTag data = entity.getPersistentData();

        int currentLevel = data.getInt(NBT_WETNESS);
        int maxLevel = ElementalReactionConfig.wetnessMaxLevel;

        boolean inWater = entity.isInWater();
        boolean isRainingHere = level.isRainingAt(pos);

        boolean isSnowingHere = level.isRaining() && level.canSeeSky(pos)
                && level.getBiome(pos).value().getPrecipitationAt(pos) == Biome.Precipitation.SNOW;

        boolean inPrecipitation = isRainingHere || isSnowingHere;

        if (inWater) {
            double fluidHeight = entity.getFluidHeight(FluidTags.WATER);
            double entityHeight = entity.getBbHeight();
            
            // 浅水判定：若流体高度未超过实体高度，则视为浅水
            // Shallow water check: If fluid height does not exceed entity height, consider as shallow water
            int targetLevel = maxLevel;
            
            if (fluidHeight < entityHeight) {
                // 浅水上限限制
                // Shallow water cap
                double ratio = ElementalReactionConfig.wetnessShallowWaterCapRatio;
                targetLevel = (int) Math.floor(maxLevel * ratio);
                // 保证至少有1层
                // Ensure at least level 1
                targetLevel = Math.max(1, targetLevel);
            }

            if (currentLevel < targetLevel) {
                currentLevel = targetLevel;
                updateWetnessLevel(entity, currentLevel);
            }
            
            // 如果已经在水中但层数超过了浅水上限（例如从深水移动到浅水），保持当前层数不衰减
            // If already in water but level exceeds shallow cap (e.g. moved from deep to shallow), keep current level.

            data.putInt(NBT_RAIN_TIMER, 0);
            data.putInt(NBT_DECAY_TIMER, 0);

        } else if (inPrecipitation) {
            data.putInt(NBT_DECAY_TIMER, 0);

            if (currentLevel < maxLevel) {
                int rainTimer = data.getInt(NBT_RAIN_TIMER) + 1;

                int thresholdSeconds = ElementalReactionConfig.wetnessRainGainInterval;

                if (rainTimer >= thresholdSeconds) {
                    currentLevel++;
                    updateWetnessLevel(entity, currentLevel);
                    data.putInt(NBT_RAIN_TIMER, 0);
                } else {
                    data.putInt(NBT_RAIN_TIMER, rainTimer);
                }
            }
        } else {
            data.putInt(NBT_RAIN_TIMER, 0);

            if (currentLevel > 0) {
                int decayTimer = data.getInt(NBT_DECAY_TIMER) + 1;

                int durationForCurrentLevel = currentLevel * ElementalReactionConfig.wetnessDecayBaseTime;

                if (decayTimer >= durationForCurrentLevel) {
                    currentLevel--;
                    updateWetnessLevel(entity, currentLevel);

                    data.putInt(NBT_DECAY_TIMER, 0);
                } else {
                    data.putInt(NBT_DECAY_TIMER, decayTimer);
                }
            }
        }

        syncEffect(entity, currentLevel, inWater || inPrecipitation, data.getInt(NBT_DECAY_TIMER));
    }

    /**
     * 检查实体是否对潮湿效果免疫。
     * 基于配置检查：水生生物、下界维度、黑名单。
     *
     * Checks if the entity is immune to the wetness effect.
     * Checks based on config: Water animals, Nether dimension, Blacklist.
     *
     * @param entity 目标实体 / Target Entity
     * @return true 如果免疫 / true if immune
     */
    private static boolean isImmune(LivingEntity entity) {
        if (ElementalReactionConfig.wetnessWaterAnimalImmune) {
            if (entity instanceof WaterAnimal) {
                return true;
            }
        }

        if (ElementalReactionConfig.wetnessNetherDimensionImmune) {
            if (entity.level().dimension() == Level.NETHER) {
                return true;
            }
        }

        if (!ElementalReactionConfig.cachedWetnessBlacklist.isEmpty()) {
            String entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
            if (ElementalReactionConfig.cachedWetnessBlacklist.contains(entityId)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 清除实体的潮湿数据和药水效果。
     *
     * Clears wetness data and potion effects from the entity.
     *
     * @param entity 目标实体 / Target entity
     */
    private static void clearWetnessData(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (data.contains(NBT_WETNESS)) {
            data.remove(NBT_WETNESS);
            data.remove(NBT_RAIN_TIMER);
            data.remove(NBT_DECAY_TIMER);
        }

        if (entity.hasEffect(ModMobEffects.WETNESS.get())) {
            entity.removeEffect(ModMobEffects.WETNESS.get());
        }
    }

    /**
     * 同步药水效果到实体，用于 UI 显示。
     *
     * Syncs potion effect to the entity for UI display.
     *
     * @param entity     目标实体 / Target entity
     * @param level      当前潮湿等级 / Current wetness level
     * @param isPaused   是否处于暂停倒计时状态（雨中/水中） / Is countdown paused (Rain/Water)
     * @param decayTimer 当前衰减计时器(秒) / Current decay timer (seconds)
     */
    private static void syncEffect(LivingEntity entity, int level, boolean isPaused, int decayTimer) {
        if (level <= 0) {
            if (entity.hasEffect(ModMobEffects.WETNESS.get())) {
                entity.removeEffect(ModMobEffects.WETNESS.get());
            }
            return;
        }

        int amplifier = level - 1;
        int durationTicks;

        int baseTime = ElementalReactionConfig.wetnessDecayBaseTime;

        if (isPaused) {
            durationTicks = (level * baseTime) * 20;
        } else {
            int maxDurationSeconds = level * baseTime;
            int remainingSeconds = maxDurationSeconds - decayTimer;
            durationTicks = Math.max(0, remainingSeconds * 20);
        }

        if (durationTicks > 0) {
            entity.addEffect(new MobEffectInstance(
                    ModMobEffects.WETNESS.get(),
                    durationTicks,
                    amplifier,
                    true,
                    true,
                    true
            ));
        }
    }

    /**
     * 更新实体的潮湿层数 NBT。
     *
     * Updates the entity's wetness level NBT.
     *
     * @param entity 目标实体 / Target entity
     * @param level  新的潮湿等级 / New wetness level
     */
    private static void updateWetnessLevel(LivingEntity entity, int level) {
        entity.getPersistentData().putInt(NBT_WETNESS, level);
    }

    /**
     * 处理饱食度惩罚逻辑。
     * 潮湿状态下会额外增加饱食度消耗。
     * 如果有额外的消耗，会通知调试系统。
     *
     * Handles hunger exhaustion penalty logic.
     * Adds extra hunger exhaustion when wet.
     * Notifies the debug system if there is extra consumption.
     *
     * @param entity 目标实体 / Target entity
     */
    private static void handleExhaustion(LivingEntity entity) {
        if (entity instanceof Player player && !player.isCreative() && !player.isSpectator()) {
            CompoundTag data = player.getPersistentData();
            int currentLevel = data.getInt(NBT_WETNESS);

            float currentExhaustion = player.getFoodData().getExhaustionLevel();
            float lastExhaustion = data.getFloat(NBT_LAST_EXHAUSTION);

            if (currentExhaustion > lastExhaustion) {
                float delta = currentExhaustion - lastExhaustion;

                if (delta > 0.0001f) {
                    float extra = 0;
                    if (currentLevel > 0) {
                        extra = delta * currentLevel * (float) ElementalReactionConfig.wetnessExhaustionIncrease;
                        player.getFoodData().addExhaustion(extra);
                        currentExhaustion += extra;
                    }

                    if (extra > 0) {
                        DebugCommand.sendExhaustionLog(player, delta, extra, currentLevel);
                    }
                }
            }
            data.putFloat(NBT_LAST_EXHAUSTION, currentExhaustion);
        }
    }

    /**
     * 监听投掷物击中事件。
     * 所有投掷型药水（喷溅/滞留）击中生物时，均增加固定的潮湿层数。
     *
     * Listens to Projectile Impact events.
     * All thrown potions (Splash/Lingering) increase wetness level by a fixed amount when hitting a mob.
     *
     * @param event 投掷物撞击事件 / Projectile impact event
     */
    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getRayTraceResult().getType() != HitResult.Type.ENTITY) return;

        Entity projectile = event.getProjectile();
        Entity target = ((EntityHitResult) event.getRayTraceResult()).getEntity();

        if (!(target instanceof LivingEntity livingTarget)) return;

        if (isImmune(livingTarget)) return;

        if (projectile instanceof ThrownPotion) {

            int add = ElementalReactionConfig.wetnessPotionAddLevel;

            CompoundTag data = livingTarget.getPersistentData();
            int current = data.getInt(NBT_WETNESS);
            int max = ElementalReactionConfig.wetnessMaxLevel;

            int newLevel = Math.min(max, current + add);
            updateWetnessLevel(livingTarget, newLevel);

            data.putInt(NBT_DECAY_TIMER, 0);

            syncEffect(livingTarget, newLevel, livingTarget.isInWater() || livingTarget.level().isRainingAt(livingTarget.blockPosition()), 0);
        }
    }
}