package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import com.xulai.elementalcraft.util.ElementDamageHelper;
import com.xulai.elementalcraft.util.EffectHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Random;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class FrostbiteHandler {
    private static final Random RANDOM = new Random();

    public static final String NBT_FROSTBITE_STACKS = "EC_FrostbiteStacks";
    public static final String NBT_FROSTBITE_DURATION = "EC_FrostbiteDuration";
    public static final String NBT_FROSTBITE_APPLY_TICK = "EC_FrostbiteApplyTick";
    public static final String NBT_FREEZE_COOLDOWN = "EC_FreezeCooldown";

    public static final String NBT_FREEZE_ORIGINAL_NO_AI = "EC_FreezeOriginalNoAI";
    public static final String NBT_FREEZE_AI_DISABLED = "EC_FreezeAIDisabled";

    public static final String NBT_FROZEN_FROSTBITE_STACKS = "EC_FrozenFrostbiteStacks";

    public static final String NBT_FROSTBITE_AURA_DAMAGE_TIMER = "EC_FrostbiteAuraDamageTimer";

    private static void debugMsg(LivingEntity target, String msg) {
        if (target instanceof Player player) {
            player.sendSystemMessage(Component.literal("[FrostbiteDebug] " + msg).withStyle(ChatFormatting.YELLOW));
        }
    }

    private static void debugMsgAttacker(LivingEntity attacker, LivingEntity target, String msg) {
        if (attacker instanceof Player player) {
            player.sendSystemMessage(Component.literal("[FrostbiteDebug] " + msg).withStyle(ChatFormatting.YELLOW));
        }
        if (target instanceof Player player && player != attacker) {
            player.sendSystemMessage(Component.literal("[FrostbiteDebug] " + msg).withStyle(ChatFormatting.YELLOW));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof LivingEntity attacker)) return;
        LivingEntity target = event.getEntity();
        Level level = target.level();

        ElementType attackType = ElementUtils.getConsistentAttackElement(attacker);
        double frostPower = ElementUtils.getDisplayEnhancement(attacker, ElementType.FROST);

        if (attackType != ElementType.FROST) return;

        double threshold = ElementalThunderFrostReactionsConfig.frostStrengthThreshold;
        boolean thresholdMet = frostPower >= threshold;

        debugMsgAttacker(attacker, target, "=== 霜冻触发判定 ===");
        debugMsgAttacker(attacker, target, "攻击者冰霜强化: " + String.format("%.1f", frostPower) + " | 门槛: " + String.format("%.1f", threshold) + " | " + (thresholdMet ? "已达到" : "未达到"));

        if (!thresholdMet) return;

        double baseChance = ElementalThunderFrostReactionsConfig.frostbiteBaseChance;
        double scalingStep = ElementalThunderFrostReactionsConfig.frostbiteScalingStep;
        double scalingChance = ElementalThunderFrostReactionsConfig.frostbiteScalingChance;
        double stackingBonus = ElementalThunderFrostReactionsConfig.frostbiteStackingBonusChance;
        double wetnessBonus = ElementalThunderFrostReactionsConfig.frostbiteWetnessBonusChance;

        double chance;
        int steps = 0;
        if (frostPower < scalingStep) {
            chance = baseChance;
            debugMsgAttacker(attacker, target, "基础触发概率: " + String.format("%.1f", baseChance * 100) + "% (未达到步长 " + String.format("%.1f", scalingStep) + ")");
        } else {
            steps = (int) ((frostPower - scalingStep) / scalingStep);
            chance = baseChance + (steps * scalingChance);
            debugMsgAttacker(attacker, target, "基础触发概率: " + String.format("%.1f", baseChance * 100) + "% | 步长数: " + steps + " | 每步长增加: " + String.format("%.1f", scalingChance * 100) + "% | 步长后概率: " + String.format("%.1f", chance * 100) + "%");
        }
        chance = Math.min(1.0, chance);

        int attackerWetness = WetnessHandler.getWetnessLevel(attacker);
        if (attackerWetness > 0) {
            double bonus = attackerWetness * wetnessBonus;
            chance += bonus;
            chance = Math.min(1.0, chance);
            debugMsgAttacker(attacker, target, "攻击者潮湿 " + attackerWetness + " 层 | 额外概率: +" + String.format("%.1f", bonus * 100) + "% | 当前总概率: " + String.format("%.1f", chance * 100) + "%");
        }

        int targetWetness = WetnessHandler.getWetnessLevel(target);
        if (targetWetness > 0) {
            double bonus = targetWetness * wetnessBonus;
            chance += bonus;
            chance = Math.min(1.0, chance);
            debugMsgAttacker(attacker, target, "目标潮湿 " + targetWetness + " 层 | 额外概率: +" + String.format("%.1f", bonus * 100) + "% | 当前总概率: " + String.format("%.1f", chance * 100) + "%");
        }

        boolean inSteamCloud = SteamReactionHandler.isInCondensingCloud(target);
        if (inSteamCloud) {
            double bonus = ElementalThunderFrostReactionsConfig.frostbiteSteamCloudBonusChance;
            chance += bonus;
            chance = Math.min(1.0, chance);
            debugMsgAttacker(attacker, target, "目标在冷凝蒸汽云中 | 额外概率: +" + String.format("%.1f", bonus * 100) + "% | 当前总概率: " + String.format("%.1f", chance * 100) + "%");
        }

        boolean hasExistingFrostbite = hasFrostbite(target);
        if (hasExistingFrostbite) {
            chance += stackingBonus;
            chance = Math.min(1.0, chance);
            debugMsgAttacker(attacker, target, "目标已有霜冻效果 | 额外叠加概率: +" + String.format("%.1f", stackingBonus * 100) + "% | 当前总概率: " + String.format("%.1f", chance * 100) + "%");
        }

        debugMsgAttacker(attacker, target, "最终触发概率: " + String.format("%.1f", chance * 100) + "%");

        if (RANDOM.nextDouble() >= chance) {
            debugMsgAttacker(attacker, target, "随机判定: 未触发 (骰子值 >= " + String.format("%.2f", chance) + ")");
            return;
        }

        debugMsgAttacker(attacker, target, "随机判定: 触发成功!");

        int stacksToApply = ElementalThunderFrostReactionsConfig.frostbiteMaxStacksPerAttack;
        applyFrostbite(target, attacker, stacksToApply);
    }

    public static boolean applyFrostbite(LivingEntity target, LivingEntity attacker, int layersToAdd) {
        if (target.level().isClientSide) return false;

        if (isFrostbiteImmune(target)) {
            debugMsgAttacker(attacker, target, "目标免疫霜冻，跳过施加");
            return false;
        }

        CompoundTag data = target.getPersistentData();
        long gameTime = target.level().getGameTime();

        if (data.contains(NBT_FROSTBITE_APPLY_TICK)) {
            if (data.getLong(NBT_FROSTBITE_APPLY_TICK) == gameTime) {
                debugMsgAttacker(attacker, target, "同一 tick 已施加过霜冻，跳过");
                return false;
            }
        }


        int currentStacks = data.getInt(NBT_FROSTBITE_STACKS);
        int maxStacks = ElementalThunderFrostReactionsConfig.frostbiteMaxTotalStacks;

        if (currentStacks >= maxStacks) {
            debugMsgAttacker(attacker, target, "霜冻层数已达上限 " + maxStacks + " 层，无法继续叠加");
            return false;
        }

        int newStacks = Math.min(maxStacks, currentStacks + layersToAdd);

        int baseDuration = ElementalThunderFrostReactionsConfig.frostbiteBaseDurationTicks;
        int perExtraStack = ElementalThunderFrostReactionsConfig.frostbiteDurationPerExtraStackTicks;
        int durationTicks = baseDuration + (newStacks - 1) * perExtraStack;

        boolean isFire = ElementUtils.getConsistentAttackElement(target) == ElementType.FIRE;
        if (isFire) {
            durationTicks = (int) (durationTicks * ElementalThunderFrostReactionsConfig.frostbiteFireDurationMultiplier);
        }
        if (target.level().dimension() == Level.NETHER) {
            durationTicks = (int) (durationTicks * ElementalThunderFrostReactionsConfig.frostbiteNetherDurationMultiplier);
        }

        double speedReduction = ElementalThunderFrostReactionsConfig.frostbiteSpeedReductionPerStack;

        debugMsgAttacker(attacker, target, "=== 施加霜冻 ===");
        debugMsgAttacker(attacker, target, "当前层数: " + currentStacks + " | 施加层数: " + layersToAdd + " | 新层数: " + newStacks + " | 最大层数: " + maxStacks);
        debugMsgAttacker(attacker, target, "持续时间: " + durationTicks + " tick (" + (durationTicks / 20) + " 秒) | 每层减速: " + String.format("%.0f", speedReduction * 100) + "%");

        data.putInt(NBT_FROSTBITE_STACKS, newStacks);
        data.putInt(NBT_FROSTBITE_DURATION, durationTicks);
        data.putLong(NBT_FROSTBITE_APPLY_TICK, gameTime);

        if (!target.level().isClientSide) {
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.5f, 1.5f);
        }

        syncFrostbiteEffect(target, newStacks, durationTicks);

        checkFreezeFromWetness(target, attacker);

        if (ElementalThunderFrostReactionsConfig.frostbiteClearSporesEnabled) {
            if (ModMobEffects.SPORES.isPresent() && ModMobEffects.SPORES.get() != null && target.hasEffect(ModMobEffects.SPORES.get())) {
                target.removeEffect(ModMobEffects.SPORES.get());
                if (!target.level().isClientSide) {
                    target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SNOW_PLACE, SoundSource.PLAYERS, 0.5f, 0.5f);
                }
            }
        }

        return true;
    }

    public static void checkFreezeFromWetness(LivingEntity target, LivingEntity attacker) {
        if (target.level().isClientSide) return;
        if (!hasFrostbite(target)) return;
        if (WetnessHandler.getWetnessLevel(target) <= 0) return;

        if (isFrozen(target)) {
            clearFrostbite(target);
            WetnessHandler.clearWetnessData(target);
            return;
        }

        triggerFreeze(target, attacker);
    }

    public static void triggerFreeze(LivingEntity target, LivingEntity attacker) {
        if (target.level().isClientSide) return;
        CompoundTag data = target.getPersistentData();
        long gameTime = target.level().getGameTime();

        if (isFrozen(target)) return;
        if (data.contains(NBT_FREEZE_COOLDOWN)) {
            if (gameTime < data.getLong(NBT_FREEZE_COOLDOWN)) return;
        }
        if (isFreezeImmune(target)) return;

        int frostbiteStacks = data.getInt(NBT_FROSTBITE_STACKS);
        if (frostbiteStacks <= 0) return;
        float settlementDamage = frostbiteStacks * (float) ElementalThunderFrostReactionsConfig.freezeSettlementDamagePerStack * (float) ElementalThunderFrostReactionsConfig.frostbitePeriodicDamage;
        if (settlementDamage > 0) {
            ElementalCraft.LOGGER.info("[Frostbite] 冻结结算霜冻伤害: {} 点伤害 ({} 层 × {} × {})", String.format("%.1f", settlementDamage), frostbiteStacks, String.format("%.1f", ElementalThunderFrostReactionsConfig.freezeSettlementDamagePerStack), String.format("%.1f", ElementalThunderFrostReactionsConfig.frostbitePeriodicDamage));
            ElementDamageHelper.applyDamage(target, settlementDamage, ModDamageTypes.source(target.level(), ModDamageTypes.FROSTBITE_THERMAL_SHOCK));
        }

        data.putInt(NBT_FROZEN_FROSTBITE_STACKS, frostbiteStacks);

        boolean fromWetness = WetnessHandler.getWetnessLevel(target) > 0;

        DebugCommand.FreezeLogContext freezeCtx = new DebugCommand.FreezeLogContext();
        freezeCtx.target = target;
        freezeCtx.frostbiteStacks = frostbiteStacks;
        freezeCtx.damage = settlementDamage;
        freezeCtx.fromWetness = fromWetness;
        freezeCtx.freezeDuration = ElementalThunderFrostReactionsConfig.freezeDurationTicks;
        DebugCommand.sendFreezeLog(freezeCtx);

        int freezeDuration = ElementalThunderFrostReactionsConfig.freezeDurationTicks;
        target.addEffect(new MobEffectInstance(ModMobEffects.FREEZE.get(), freezeDuration, 0, false, false, true));

        data.putLong(NBT_FREEZE_COOLDOWN, gameTime + freezeDuration + ElementalThunderFrostReactionsConfig.freezeCooldownTicks);

        if (target.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 30, 0.3, 0.3, 0.3, 0.05);
        }

        if (WetnessHandler.getWetnessLevel(target) > 0) {
            WetnessHandler.clearWetnessData(target);
        }

        if (target.level() instanceof ServerLevel serverLevel && ElementalThunderFrostReactionsConfig.freezeColdCloudEnabled) {
            int triggerStacks = ElementalThunderFrostReactionsConfig.freezeColdCloudTriggerStacks;
            if (frostbiteStacks >= triggerStacks) {
                double baseRadius = ElementalThunderFrostReactionsConfig.freezeColdCloudBaseRadius;
                double perStack = ElementalThunderFrostReactionsConfig.freezeColdCloudRadiusPerStack;
                int duration = ElementalThunderFrostReactionsConfig.freezeColdCloudDuration;
                double radius = baseRadius + (frostbiteStacks - triggerStacks) * perStack;
                EffectHelper.spawnFreezeColdCloud(serverLevel, target, radius, duration);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        CompoundTag data = entity.getPersistentData();
        long gameTime = entity.level().getGameTime();

        if (data.contains(NBT_FREEZE_COOLDOWN) && gameTime >= data.getLong(NBT_FREEZE_COOLDOWN)) {
            data.remove(NBT_FREEZE_COOLDOWN);
        }

        if (!data.contains(NBT_FROSTBITE_STACKS)) {
            if (!isFrozen(entity) && entity.hasEffect(ModMobEffects.FROSTBITE.get())) {
                MobEffectInstance effectInstance = entity.getEffect(ModMobEffects.FROSTBITE.get());
                if (effectInstance != null) {
                    int effectStacks = effectInstance.getAmplifier() + 1;
                    int effectDuration = effectInstance.getDuration();
                    data.putInt(NBT_FROSTBITE_STACKS, effectStacks);
                    data.putInt(NBT_FROSTBITE_DURATION, effectDuration);
                    data.putLong(NBT_FROSTBITE_APPLY_TICK, gameTime);
                    return;
                }
            }
            data.remove(NBT_FROSTBITE_AURA_DAMAGE_TIMER);
            return;
        }

        int stacks = data.getInt(NBT_FROSTBITE_STACKS);
        int duration = data.getInt(NBT_FROSTBITE_DURATION);

        if (duration <= 0 || stacks <= 0) {
            clearFrostbite(entity);
            return;
        }

        data.putInt(NBT_FROSTBITE_DURATION, duration - 1);

        boolean hasWetness = WetnessHandler.getWetnessLevel(entity) > 0;
        if (hasWetness) {
            if (isFrozen(entity)) {
                clearFrostbite(entity);
                WetnessHandler.clearWetnessData(entity);
            } else if (data.contains(NBT_FREEZE_COOLDOWN) && gameTime < data.getLong(NBT_FREEZE_COOLDOWN)) {
                clearFrostbite(entity);
            } else {
                triggerFreeze(entity, null);
            }
        }

        if (stacks >= ElementalThunderFrostReactionsConfig.frostbiteAuraThreshold) {
            processAuraTick(entity, stacks);
        } else {
            data.remove(NBT_FROSTBITE_AURA_DAMAGE_TIMER);
        }
    }

    public static void clearFrostbite(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        data.remove(NBT_FROSTBITE_STACKS);
        data.remove(NBT_FROSTBITE_DURATION);
        data.remove(NBT_FROSTBITE_APPLY_TICK);
        data.remove(NBT_FROSTBITE_AURA_DAMAGE_TIMER);

        if (entity.hasEffect(ModMobEffects.FROSTBITE.get())) {
            entity.removeEffect(ModMobEffects.FROSTBITE.get());
        }
    }

    public static void triggerThermalShock(LivingEntity target, LivingEntity attacker) {
        if (target.level().isClientSide) return;
        CompoundTag data = target.getPersistentData();
        int stacks = data.getInt(NBT_FROZEN_FROSTBITE_STACKS);

        float damage = (float) (ElementalThunderFrostReactionsConfig.frostbiteThermalShockBaseDamage + stacks * ElementalThunderFrostReactionsConfig.frostbiteThermalShockPerStack);

        if (target.hasEffect(ModMobEffects.FREEZE.get())) {
            target.removeEffect(ModMobEffects.FREEZE.get());
        }
        data.remove(NBT_FROZEN_FROSTBITE_STACKS);
        data.putLong(NBT_FREEZE_COOLDOWN, target.level().getGameTime() + ElementalThunderFrostReactionsConfig.freezeCooldownTicks);

        ElementDamageHelper.applyDamage(target, damage, ModDamageTypes.source(target.level(), ModDamageTypes.FROSTBITE_THERMAL_SHOCK, attacker));

        SteamReactionHandler.spawnSteamCloud(target, true, stacks);

        if (target.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 1, target.getZ(), 30, 1.0, 1.0, 1.0, 0.1);
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + 1, target.getZ(), 20, 0.5, 0.5, 0.5, 0.05);
        }
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0f, 0.5f);
    }

    public static boolean isFrostbiteImmune(LivingEntity target) {
        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).toString();
        if (ElementalThunderFrostReactionsConfig.cachedFrostbiteImmunityBlacklist != null && ElementalThunderFrostReactionsConfig.cachedFrostbiteImmunityBlacklist.contains(entityId)) {
            debugMsg(target, "免疫霜冻: 实体ID " + entityId + " 在黑名单中");
            return true;
        }
        double frostResistance = ElementUtils.getDisplayResistance(target, ElementType.FROST);
        double threshold = ElementalThunderFrostReactionsConfig.frostbiteResistImmunityThreshold;
        if (frostResistance >= threshold) {
            debugMsg(target, "免疫霜冻: 冰霜抗性 " + String.format("%.1f", frostResistance) + " >= 阈值 " + String.format("%.1f", threshold));
            return true;
        }
        return false;
    }

    public static boolean isFreezeImmune(LivingEntity target) {
        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).toString();
        if (ElementalThunderFrostReactionsConfig.cachedFreezeImmunityBlacklist != null && ElementalThunderFrostReactionsConfig.cachedFreezeImmunityBlacklist.contains(entityId)) {
            return true;
        }
        return false;
    }

    public static boolean isFrozen(LivingEntity entity) {
        return entity.hasEffect(ModMobEffects.FREEZE.get());
    }

    public static boolean hasFrostbite(LivingEntity entity) {
        return entity.getPersistentData().contains(NBT_FROSTBITE_STACKS) && entity.getPersistentData().getInt(NBT_FROSTBITE_STACKS) > 0;
    }

    public static int getFrostbiteStacks(LivingEntity entity) {
        return entity.getPersistentData().getInt(NBT_FROSTBITE_STACKS);
    }

    public static int getFrozenFrostbiteStacks(LivingEntity entity) {
        return entity.getPersistentData().getInt(NBT_FROZEN_FROSTBITE_STACKS);
    }

    private static void syncFrostbiteEffect(LivingEntity entity, int stacks, int durationTicks) {
        if (stacks <= 0) {
            if (entity.hasEffect(ModMobEffects.FROSTBITE.get())) {
                entity.removeEffect(ModMobEffects.FROSTBITE.get());
            }
            return;
        }

        if (entity.hasEffect(ModMobEffects.FROSTBITE.get())) {
            entity.removeEffect(ModMobEffects.FROSTBITE.get());
        }
        entity.addEffect(new MobEffectInstance(ModMobEffects.FROSTBITE.get(), durationTicks, stacks - 1, false, false, true));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide) return;
        LivingEntity target = event.getEntity();

        if (isFrozen(target)) {
            DamageSource source = event.getSource();
            boolean isElemental = source.is(DamageTypeTags.IS_FIRE) || source.is(ModDamageTypes.FROSTBITE_THERMAL_SHOCK) || source.is(ModDamageTypes.LAVA_MAGIC) || source.is(ModDamageTypes.STATIC_SHOCK) || source.is(ModDamageTypes.SPORES) || source.is(ModDamageTypes.STEAM_SCALDING);

            if (source.getEntity() instanceof LivingEntity attacker) {
                ElementType attackElement = ElementUtils.getConsistentAttackElement(attacker);
                if (attackElement != ElementType.NONE) {
                    isElemental = true;
                }
            }

            if (!isElemental) {
                boolean isMeleeOrProjectile = source.getDirectEntity() instanceof LivingEntity || source.is(DamageTypeTags.IS_PROJECTILE);
                if (isMeleeOrProjectile && !source.is(DamageTypeTags.BYPASSES_ARMOR) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                    event.setCanceled(true);
                    return;
                }
            }
        }

        if (sourceIsFire(event.getSource()) && isFrozen(target)) {
            event.setCanceled(true);
            Entity attackerEntity = event.getSource().getEntity();
            if (attackerEntity instanceof LivingEntity attacker) {
                triggerThermalShock(target, attacker);
            } else {
                triggerThermalShock(target, target);
            }
        }
    }

    private static boolean sourceIsFire(DamageSource source) {
        return source.is(DamageTypeTags.IS_FIRE);
    }

    public static void spawnSteamCloud(LivingEntity target, boolean isHighHeat, int fuelLevel) {
        SteamReactionHandler.spawnSteamCloud(target, isHighHeat, fuelLevel);
    }

    public static double getAuraRange(int stacks) {
        double base = ElementalThunderFrostReactionsConfig.frostbiteAuraBaseRange;
        double perExtra = ElementalThunderFrostReactionsConfig.frostbiteAuraRangePerStack;
        int threshold = ElementalThunderFrostReactionsConfig.frostbiteAuraThreshold;
        double maxRange = ElementalThunderFrostReactionsConfig.frostbiteAuraMaxRange;
        double range = base + (stacks - threshold) * perExtra;
        return Math.min(range, maxRange);
    }

    private static void processAuraTick(LivingEntity source, int stacks) {
        CompoundTag data = source.getPersistentData();
        long gameTime = source.level().getGameTime();
        int timer = data.getInt(NBT_FROSTBITE_AURA_DAMAGE_TIMER) + 1;
        int interval = ElementalThunderFrostReactionsConfig.frostbiteAuraDamageIntervalTicks;
        if (interval < 1) interval = 1;
        data.putInt(NBT_FROSTBITE_AURA_DAMAGE_TIMER, timer);

        if (timer < interval) return;
        data.putInt(NBT_FROSTBITE_AURA_DAMAGE_TIMER, 0);

        double range = getAuraRange(stacks);

        AABB area = new AABB(
                source.getX() - range, source.getY() - range, source.getZ() - range,
                source.getX() + range, source.getY() + range, source.getZ() + range
        );
        List<LivingEntity> nearby = source.level().getEntitiesOfClass(LivingEntity.class, area);

        float baseDamage = (float) ElementalThunderFrostReactionsConfig.frostbitePeriodicDamage;

        for (LivingEntity target : nearby) {
            if (target == source) continue;
            if (target.isDeadOrDying()) continue;

            double dx = target.getX() - source.getX();
            double dz = target.getZ() - source.getZ();
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            if (horizontalDist > range) continue;

            if (ElementalThunderFrostReactionsConfig.frostbiteAuraExcludeFriendly) {
                if (target instanceof Player) continue;
                if (target instanceof TamableAnimal pet && pet.isTame() && pet.getOwner() != null) continue;
                if (target instanceof AbstractHorse horse && horse.getOwnerUUID() != null) continue;
            }

            if (ElementalThunderFrostReactionsConfig.frostbiteAuraOnlyHostile) {
                if (target.getType().getCategory() != MobCategory.MONSTER) continue;
            }

            if (WetnessHandler.getWetnessLevel(target) > 0 && hasFrostbite(target)) {
                if (isFrozen(target)) {
                    clearFrostbite(target);
                    continue;
                }
                if (data.contains(NBT_FREEZE_COOLDOWN) && gameTime < data.getLong(NBT_FREEZE_COOLDOWN)) {
                    clearFrostbite(target);
                    continue;
                }
                triggerFreeze(target, source);
                continue;
            }

            float damage = baseDamage;
            ElementType targetElement = ElementUtils.getConsistentAttackElement(target);
            if (targetElement == ElementType.FIRE) {
                damage *= (float) ElementalThunderFrostReactionsConfig.frostbiteDamageFireMultiplier;
            } else if (targetElement == ElementType.NATURE) {
                damage *= (float) ElementalThunderFrostReactionsConfig.frostbiteDamageNatureMultiplier;
            } else if (targetElement == ElementType.FROST) {
                damage *= (float) ElementalThunderFrostReactionsConfig.frostbiteDamageFrostMultiplier;
            }
            ElementDamageHelper.applyDamage(target, damage, target.damageSources().freeze());
        }
    }
}
