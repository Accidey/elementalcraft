package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.sound.ModSounds;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import com.xulai.elementalcraft.util.ElementDamageHelper;
import com.xulai.elementalcraft.util.EffectHelper;
import com.xulai.elementalcraft.event.SteamReactionHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
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
    public static final String NBT_FREEZE_STACKS = "EC_FreezeStacks";

    public static final String NBT_FROSTBITE_FIRE_STAND_TIMER = "EC_FrostbiteFireStandTimer";
    public static final String NBT_FROSTBITE_SOURCE_FROST_POWER = "EC_FrostbiteSourceFrostPower";

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
        boolean thresholdMet = threshold > 0 && frostPower >= threshold;

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

        int targetWetness = WetnessHandler.getWetnessLevel(target);
        if (targetWetness > 0) {
            double bonus = targetWetness * wetnessBonus;
            chance += bonus;
            chance = Math.min(1.0, chance);
            debugMsgAttacker(attacker, target, "目标潮湿 " + targetWetness + " 层 | 额外概率: +" + String.format("%.1f", bonus * 100) + "% | 当前总概率: " + String.format("%.1f", chance * 100) + "%");
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
        if (target instanceof Player player && player.isCreative()) return false;

        if (SteamReactionHandler.isInHighHeatCloud(target)) {
            return false;
        }

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
        int frostPower = ElementUtils.getDisplayEnhancement(attacker, ElementType.FROST);
        data.putInt(NBT_FROSTBITE_SOURCE_FROST_POWER, frostPower);

        if (!target.level().isClientSide) {
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.5f, 1.5f);
            DebugCommand.FrostbiteLogContext fctx = new DebugCommand.FrostbiteLogContext();
            fctx.attacker = attacker;
            fctx.target = target;
            fctx.stacksApplied = layersToAdd;
            fctx.totalStacks = newStacks;
            fctx.chance = 1.0;
            DebugCommand.sendFrostbiteLog(fctx);
        }

        syncFrostbiteEffect(target, newStacks, durationTicks);

        checkFreezeFromWetness(target, attacker, frostPower);

        return true;
    }

    public static void checkFreezeFromWetness(LivingEntity target, LivingEntity attacker, int frostPower) {
        if (target.level().isClientSide) return;
        if (!hasFrostbite(target)) return;
        if (WetnessHandler.getWetnessLevel(target) <= 0) return;

        if (isFrozen(target) || isOnFreezeCooldown(target)) {
            if (isOnFreezeCooldown(target)) {
                DebugCommand.sendReactionCooldownBlock(target, "freeze", DebugCommand.getRemainingCooldown(target, NBT_FREEZE_COOLDOWN));
            }
            int auraStacks = target.getPersistentData().getInt(NBT_FROSTBITE_STACKS);
            if (ElementalThunderFrostReactionsConfig.frostbiteAuraThreshold > 0 && auraStacks >= ElementalThunderFrostReactionsConfig.frostbiteAuraThreshold) {
                clearFrostbiteAuraEffects(target, auraStacks);
            }
            clearFrostbite(target);
            WetnessHandler.clearWetnessData(target);
            return;
        }

        triggerFreeze(target, attacker, frostPower);
    }

    public static void triggerFreeze(LivingEntity target, LivingEntity attacker, int frostPower) {
        if (target.level().isClientSide) return;
        if (target instanceof Player player && player.isCreative()) return;
        CompoundTag data = target.getPersistentData();
        long gameTime = target.level().getGameTime();

        if (isFrozen(target)) return;
        if (isOnFreezeCooldown(target)) {
            DebugCommand.sendReactionCooldownBlock(target, "freeze", DebugCommand.getRemainingCooldown(target, NBT_FREEZE_COOLDOWN));
            return;
        }
        if (isFreezeImmune(target)) {
            int auraStacks = data.getInt(NBT_FROSTBITE_STACKS);
            if (ElementalThunderFrostReactionsConfig.frostbiteAuraThreshold > 0 && auraStacks >= ElementalThunderFrostReactionsConfig.frostbiteAuraThreshold) {
                clearFrostbiteAuraEffects(target, auraStacks);
            }
            clearFrostbite(target);
            WetnessHandler.clearWetnessData(target);
            DebugCommand.sendReactionFailed(target, "freeze", "immune", target.getDisplayName());
            return;
        }

        if (ElementalThunderFrostReactionsConfig.freezeClearSporesEnabled) {
            if (ModMobEffects.SPORES.isPresent() && ModMobEffects.SPORES.get() != null
                    && target.hasEffect(ModMobEffects.SPORES.get())) {
                target.removeEffect(ModMobEffects.SPORES.get());
            }
        }

        int frostbiteStacks = data.getInt(NBT_FROSTBITE_STACKS);
        if (frostbiteStacks <= 0) return;
        if (ElementalThunderFrostReactionsConfig.freezeMaxStacks <= 0) return;

        int wetnessLevel = WetnessHandler.getWetnessLevel(target);
        int freezeStacks = Math.max(frostbiteStacks, wetnessLevel);
        int maxStacks = ElementalThunderFrostReactionsConfig.freezeMaxStacks;
        if (freezeStacks > maxStacks) {
            freezeStacks = maxStacks;
        }

        float settlementDamage = frostbiteStacks * (float) ElementalThunderFrostReactionsConfig.freezeSettlementDamagePerStack * (float) ElementalThunderFrostReactionsConfig.frostbitePeriodicDamage;
        if (settlementDamage > 0) {
            ElementalCraft.LOGGER.info("[Frostbite] 冻结结算霜冻伤害: {} 点伤害 ({} 层 × {} × {})", String.format("%.1f", settlementDamage), frostbiteStacks, String.format("%.1f", ElementalThunderFrostReactionsConfig.freezeSettlementDamagePerStack), String.format("%.1f", ElementalThunderFrostReactionsConfig.frostbitePeriodicDamage));
            ElementDamageHelper.applyDamage(target, settlementDamage, ModDamageTypes.source(target.level(), ModDamageTypes.FROSTBITE_THERMAL_SHOCK));
        }

        data.putInt(NBT_FROZEN_FROSTBITE_STACKS, frostbiteStacks);

        boolean fromWetness = WetnessHandler.getWetnessLevel(target) > 0;

        int freezeDuration = freezeStacks * ElementalThunderFrostReactionsConfig.freezeDurationPerStackTicks;
        if (freezeDuration < 20) {
            freezeDuration = 20;
        }

        DebugCommand.FreezeLogContext freezeCtx = new DebugCommand.FreezeLogContext();
        freezeCtx.target = target;
        freezeCtx.frostbiteStacks = frostbiteStacks;
        freezeCtx.freezeStacks = freezeStacks;
        freezeCtx.damage = settlementDamage;
        freezeCtx.fromWetness = fromWetness;
        freezeCtx.freezeDuration = freezeDuration;
        DebugCommand.sendFreezeLog(freezeCtx);

        target.addEffect(new MobEffectInstance(ModMobEffects.FREEZE.get(), freezeDuration, freezeStacks - 1, false, false, true));

        if (!target.level().isClientSide) {
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    ModSounds.FREEZING.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        data.putInt(NBT_FREEZE_STACKS, freezeStacks);
        data.putLong(NBT_FREEZE_COOLDOWN, gameTime + freezeDuration + ElementalThunderFrostReactionsConfig.freezeCooldownTicks);

        if (target.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 30, 0.3, 0.3, 0.3, 0.05);
        }

        if (ElementalThunderFrostReactionsConfig.frostbiteAuraThreshold > 0 && frostbiteStacks >= ElementalThunderFrostReactionsConfig.frostbiteAuraThreshold) {
            clearFrostbiteAuraEffects(target, frostbiteStacks);
        }
        clearFrostbite(target);
        WetnessHandler.clearWetnessData(target);

        if (attacker != null && !target.level().isClientSide && ElementalFireNatureReactionsConfig.steamLowHeatMaxLevel > 0
                && frostPower >= ElementalFireNatureReactionsConfig.steamLowHeatTriggerThreshold
                && !SteamReactionHandler.isOnSteamCooldown(attacker)) {
            int steamLevel = Math.max(1, Math.min(wetnessLevel, ElementalFireNatureReactionsConfig.steamLowHeatMaxLevel));
            SteamReactionHandler.applySteamCooldown(attacker, SteamReactionHandler.computeCloudDuration(false, steamLevel));
            AreaEffectCloud steamCloud = SteamReactionHandler.spawnSteamCloud(target, false, steamLevel);
            if (steamCloud != null) {
                steamCloud.addTag(SteamReactionHandler.TAG_FROSTED);
                String cloudUUID = SteamReactionHandler.getCloudUUID(steamCloud);
                if (!cloudUUID.isEmpty()) {
                    target.getPersistentData().putString(SteamReactionHandler.NBT_FROSTED_CLOUD_UUID, cloudUUID);
                }
            }
        }

        if (target.level() instanceof ServerLevel serverLevel && frostbiteStacks >= 3) {
            double radius = 3.0 + (frostbiteStacks - 3) * 1.0;
            EffectHelper.spawnFreezeColdCloud(serverLevel, target, radius, 100);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            if (entity.hasEffect(ModMobEffects.FROSTBITE.get())) {
                MobEffectInstance effect = entity.getEffect(ModMobEffects.FROSTBITE.get());
                if (effect != null) {
                    int stacks = effect.getAmplifier() + 1;
                    int maxStacks = ElementalThunderFrostReactionsConfig.frostbiteMaxTotalStacks;
                    int ticksFrozen = Math.max(1, stacks * 300 / maxStacks);
                    entity.setTicksFrozen(ticksFrozen);
                }
            } else if (entity instanceof Player) {
                entity.setTicksFrozen(0);
            }
            return;
        }

        if (entity instanceof Player player && player.isCreative()) return;

        CompoundTag data = entity.getPersistentData();
        long gameTime = entity.level().getGameTime();

        if (data.contains(NBT_FREEZE_COOLDOWN) && gameTime >= data.getLong(NBT_FREEZE_COOLDOWN)) {
            data.remove(NBT_FREEZE_COOLDOWN);
        }

        if (data.contains(NBT_FREEZE_STACKS) && !entity.hasEffect(ModMobEffects.FREEZE.get())) {
            if (!entity.level().isClientSide) {
                entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 1.0f);
            }
            data.remove(NBT_FREEZE_STACKS);
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
            return;
        }

        int stacks = data.getInt(NBT_FROSTBITE_STACKS);
        int duration = data.getInt(NBT_FROSTBITE_DURATION);

        if (duration <= 0 || stacks <= 0) {
            clearFrostbite(entity);
            return;
        }

        boolean auraActive = ElementalThunderFrostReactionsConfig.frostbiteAuraThreshold > 0 && stacks >= ElementalThunderFrostReactionsConfig.frostbiteAuraThreshold;

        if (auraActive) {
            applyFrostbiteAuraEffects(entity, stacks);
        }

        if (ElementalThunderFrostReactionsConfig.frostbiteClearByHeatEnabled) {
            if (entity.isOnFire()) {
                boolean hasScorch = entity.getPersistentData().contains(ScorchedHandler.NBT_SCORCHED_TICKS);
                if (!hasScorch) {
                    if (auraActive) {
                        clearFrostbiteAuraEffects(entity, stacks);
                    }
                    clearFrostbite(entity);
                    entity.playSound(SoundEvents.FIRE_EXTINGUISH, 1.0f, 1.0f);
                    return;
                }
            }

            if (ElementalThunderFrostReactionsConfig.frostbiteNetherClearEnabled
                    && entity.level().dimension() == Level.NETHER) {
                if (auraActive) {
                    clearFrostbiteAuraEffects(entity, stacks);
                }
                clearFrostbite(entity);
                entity.playSound(SoundEvents.FIRE_EXTINGUISH, 1.0f, 1.0f);
                return;
            }

            if (checkHeatSource(entity.level(), entity.blockPosition())) {
                if (auraActive) {
                    clearFrostbiteAuraEffects(entity, stacks);
                }
                clearFrostbite(entity);
                entity.playSound(SoundEvents.FIRE_EXTINGUISH, 1.0f, 1.0f);
                return;
            }

            BlockPos pos = entity.blockPosition();
            BlockState state = entity.level().getBlockState(pos);
            if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
                int timer = data.getInt(NBT_FROSTBITE_FIRE_STAND_TIMER) + 1;
                int threshold = ElementalThunderFrostReactionsConfig.frostbiteFireStandClearingTime * 20;
                if (timer >= threshold) {
                    if (auraActive) {
                        clearFrostbiteAuraEffects(entity, stacks);
                    }
                    clearFrostbite(entity);
                    entity.playSound(SoundEvents.FIRE_EXTINGUISH, 1.0f, 1.0f);
                    return;
                }
                data.putInt(NBT_FROSTBITE_FIRE_STAND_TIMER, timer);
            } else {
                data.remove(NBT_FROSTBITE_FIRE_STAND_TIMER);
            }
        }

        data.putInt(NBT_FROSTBITE_DURATION, duration - 1);

        boolean hasWetness = WetnessHandler.getWetnessLevel(entity) > 0;
        if (hasWetness) {
            if (isFrozen(entity) || isOnFreezeCooldown(entity)) {
                if (isOnFreezeCooldown(entity)) {
                    DebugCommand.sendReactionCooldownBlock(entity, "freeze", DebugCommand.getRemainingCooldown(entity, NBT_FREEZE_COOLDOWN));
                }
                if (auraActive) {
                    clearFrostbiteAuraEffects(entity, stacks);
                }
                clearFrostbite(entity);
                WetnessHandler.clearWetnessData(entity);
            } else {
                int storedFrostPower = data.getInt(NBT_FROSTBITE_SOURCE_FROST_POWER);
                triggerFreeze(entity, null, storedFrostPower);
            }
        }

        if (auraActive) {
            int damageInterval = ElementalThunderFrostReactionsConfig.frostbiteAuraDamageIntervalTicks;
            if (damageInterval < 1) damageInterval = 1;
            if (entity.tickCount % damageInterval == 0) {
                applyFrostbiteAuraDamage(entity, stacks);
            }
        }

        if (ElementalThunderFrostReactionsConfig.frostbiteReduceSporesEnabled && hasFrostbite(entity)) {
            MobEffectInstance sporeInstance = entity.getEffect(ModMobEffects.SPORES.get());
            if (sporeInstance != null) {
                int extraDecay = (int) ElementalThunderFrostReactionsConfig.frostbiteSporeDecaySpeed - 1;
                if (extraDecay > 0) {
                    int newDuration = sporeInstance.getDuration() - extraDecay;
                    if (newDuration <= 0) {
                        entity.removeEffect(ModMobEffects.SPORES.get());
                    } else {
                        entity.removeEffect(ModMobEffects.SPORES.get());
                        entity.addEffect(new MobEffectInstance(ModMobEffects.SPORES.get(), newDuration,
                                sporeInstance.getAmplifier(), sporeInstance.isAmbient(), sporeInstance.isVisible(), sporeInstance.showIcon()));
                    }
                }
            }
        }
    }

    public static void clearFrostbite(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        data.remove(NBT_FROSTBITE_STACKS);
        data.remove(NBT_FROSTBITE_DURATION);
        data.remove(NBT_FROSTBITE_APPLY_TICK);
        data.remove(NBT_FROSTBITE_FIRE_STAND_TIMER);
        data.remove(NBT_FROSTBITE_SOURCE_FROST_POWER);

        if (entity.hasEffect(ModMobEffects.FROSTBITE.get())) {
            entity.removeEffect(ModMobEffects.FROSTBITE.get());
        }
    }

    private static boolean checkHeatSource(Level level, BlockPos center) {
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();
        double configRadius = ElementalThunderFrostReactionsConfig.frostbiteHeatSearchRadius;
        int lavaRange = (int) Math.ceil(configRadius);
        int magmaRange = Math.max(1, lavaRange - 1);

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int x = -lavaRange; x <= lavaRange; x++) {
            for (int y = -lavaRange; y <= lavaRange; y++) {
                for (int z = -lavaRange; z <= lavaRange; z++) {
                    mutablePos.set(cx + x, cy + y, cz + z);
                    if (level.getFluidState(mutablePos).is(FluidTags.LAVA)) {
                        return true;
                    }
                }
            }
        }
        for (int x = -magmaRange; x <= magmaRange; x++) {
            for (int y = -magmaRange; y <= magmaRange; y++) {
                for (int z = -magmaRange; z <= magmaRange; z++) {
                    mutablePos.set(cx + x, cy + y, cz + z);
                    if (level.getBlockState(mutablePos).is(Blocks.MAGMA_BLOCK)) {
                        boolean hasWaterNearby = false;
                        for (int dir = 0; dir < 6; dir++) {
                            BlockPos neighbor = mutablePos.relative(Direction.values()[dir]);
                            if (level.getFluidState(neighbor).is(FluidTags.WATER)) {
                                hasWaterNearby = true;
                                break;
                            }
                        }
                        if (!hasWaterNearby) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean isOnFreezeCooldown(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(NBT_FREEZE_COOLDOWN)) return false;
        return entity.level().getGameTime() < data.getLong(NBT_FREEZE_COOLDOWN);
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
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getEntity().hasEffect(ModMobEffects.FROSTBITE.get())
                && event.getSource().is(DamageTypeTags.IS_FREEZING)
                && !event.getSource().is(ModDamageTypes.FROSTBITE_THERMAL_SHOCK)) {
            event.setCanceled(true);
        }
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

    private static boolean isFriendlyTo(LivingEntity target, LivingEntity source) {
        if (target instanceof Player) return true;
        if (target instanceof TamableAnimal tamable && tamable.isTame()) return true;
        if (source != null && target instanceof OwnableEntity ownable) {
            Entity owner = ownable.getOwner();
            if (owner != null && owner.getUUID().equals(source.getUUID())) return true;
        }
        return false;
    }

    private static boolean shouldSkipFrostbiteAuraTarget(LivingEntity target, LivingEntity source) {
        if (target instanceof Player player && player.isCreative()) return true;
        if (target.isDeadOrDying()) return true;
        if (ElementalThunderFrostReactionsConfig.frostbiteAuraExcludeFriendly && isFriendlyTo(target, source)) return true;
        if (ElementalThunderFrostReactionsConfig.frostbiteAuraOnlyHostile && target.getType().getCategory() != MobCategory.MONSTER) return true;
        return false;
    }

    private static int targetInStaticAuraRange(LivingEntity target, LivingEntity exclude) {
        double maxPossibleStaticRange = ElementalThunderFrostReactionsConfig.staticAuraBaseRange * ElementalThunderFrostReactionsConfig.staticMaxTotalStacks;
        AABB searchArea = new AABB(
                target.getX() - maxPossibleStaticRange, target.getY() - maxPossibleStaticRange, target.getZ() - maxPossibleStaticRange,
                target.getX() + maxPossibleStaticRange, target.getY() + maxPossibleStaticRange, target.getZ() + maxPossibleStaticRange
        );
        int maxStacks = 0;
        for (LivingEntity source : target.level().getEntitiesOfClass(LivingEntity.class, searchArea)) {
            if (source == target || source == exclude) continue;
            CompoundTag data = source.getPersistentData();
            if (!data.contains(StaticShockHandler.NBT_STATIC_STACKS)) continue;
            int stacks = data.getInt(StaticShockHandler.NBT_STATIC_STACKS);
            if (ElementalThunderFrostReactionsConfig.staticAuraThreshold <= 0 || stacks < ElementalThunderFrostReactionsConfig.staticAuraThreshold) continue;
            double range = stacks * ElementalThunderFrostReactionsConfig.staticAuraBaseRange;
            if (source.distanceToSqr(target) > range * range) continue;
            if (stacks > maxStacks) maxStacks = stacks;
        }
        return maxStacks;
    }

    private static void applyFrostbiteAuraEffects(LivingEntity source, int stacks) {
        double range = getAuraRange(stacks);

        AABB area = new AABB(
                source.getX() - range, source.getY() - range, source.getZ() - range,
                source.getX() + range, source.getY() + range, source.getZ() + range
        );
        List<LivingEntity> nearby = source.level().getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity target : nearby) {
            if (target == source) continue;
            if (shouldSkipFrostbiteAuraTarget(target, source)) continue;

            double dx = target.getX() - source.getX();
            double dz = target.getZ() - source.getZ();
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            if (horizontalDist > range) continue;

            if (WetnessHandler.getWetnessLevel(target) > 0 && !isFrostbiteImmune(target) && !isFreezeImmune(target)) {
                boolean sourceHasStaticAura = source.getPersistentData().contains(StaticShockHandler.NBT_STATIC_STACKS)
                        && ElementalThunderFrostReactionsConfig.staticAuraThreshold > 0
                        && source.getPersistentData().getInt(StaticShockHandler.NBT_STATIC_STACKS) >= ElementalThunderFrostReactionsConfig.staticAuraThreshold;
                if (sourceHasStaticAura) {
                    int staticStacks = source.getPersistentData().getInt(StaticShockHandler.NBT_STATIC_STACKS);
                    if (stacks < staticStacks) {
                        continue;
                    }
                    if (stacks == staticStacks && RANDOM.nextBoolean()) {
                        continue;
                    }
                }
                int targetStaticStacks = target.getPersistentData().getInt(StaticShockHandler.NBT_STATIC_STACKS);
                if (ElementalThunderFrostReactionsConfig.staticAuraThreshold > 0 && targetStaticStacks >= ElementalThunderFrostReactionsConfig.staticAuraThreshold) {
                    if (stacks < targetStaticStacks) {
                        continue;
                    }
                    if (stacks == targetStaticStacks && RANDOM.nextBoolean()) {
                        continue;
                    }
                }
                int nearbyStaticStacks = targetInStaticAuraRange(target, source);
                if (nearbyStaticStacks > 0) {
                    if (stacks < nearbyStaticStacks) {
                        continue;
                    }
                    if (stacks == nearbyStaticStacks && RANDOM.nextBoolean()) {
                        continue;
                    }
                }
                if (ElementalThunderFrostReactionsConfig.freezeMaxStacks <= 0) continue;
                int wetnessLevel = WetnessHandler.getWetnessLevel(target);
                int freezeStacks = Math.min(wetnessLevel, ElementalThunderFrostReactionsConfig.freezeMaxStacks);
                WetnessHandler.clearWetnessData(target);
                target.addEffect(new MobEffectInstance(
                        ModMobEffects.FREEZE.get(), 60, freezeStacks - 1, false, false, true));
            } else if (isFrozen(target)) {
                CompoundTag targetData = target.getPersistentData();
                int existingStacks = targetData.getInt(NBT_FREEZE_STACKS);
                if (existingStacks <= 0) existingStacks = 1;
                target.addEffect(new MobEffectInstance(
                        ModMobEffects.FREEZE.get(), 60, existingStacks - 1, false, false, true));
            }

            if (ElementalThunderFrostReactionsConfig.frostbiteReduceSporesEnabled
                    && ModMobEffects.SPORES.isPresent() && ModMobEffects.SPORES.get() != null
                    && target.hasEffect(ModMobEffects.SPORES.get())) {
                MobEffectInstance sporeInstance = target.getEffect(ModMobEffects.SPORES.get());
                if (sporeInstance != null) {
                    int extraDecay = (int) ElementalThunderFrostReactionsConfig.frostbiteSporeDecaySpeed - 1;
                    if (extraDecay > 0) {
                        int newDuration = sporeInstance.getDuration() - extraDecay;
                        if (newDuration <= 0) {
                            target.removeEffect(ModMobEffects.SPORES.get());
                        } else {
                            target.removeEffect(ModMobEffects.SPORES.get());
                            target.addEffect(new MobEffectInstance(ModMobEffects.SPORES.get(), newDuration,
                                    sporeInstance.getAmplifier(), sporeInstance.isAmbient(), sporeInstance.isVisible(), sporeInstance.showIcon()));
                        }
                    }
                }
            }
        }
    }

    private static void applyFrostbiteAuraDamage(LivingEntity source, int stacks) {
        double range = getAuraRange(stacks);

        AABB area = new AABB(
                source.getX() - range, source.getY() - range, source.getZ() - range,
                source.getX() + range, source.getY() + range, source.getZ() + range
        );
        List<LivingEntity> nearby = source.level().getEntitiesOfClass(LivingEntity.class, area);

        float baseDamage = (float) ElementalThunderFrostReactionsConfig.frostbitePeriodicDamage;

        for (LivingEntity target : nearby) {
            if (target == source) continue;
            if (shouldSkipFrostbiteAuraTarget(target, source)) continue;

            double dx = target.getX() - source.getX();
            double dz = target.getZ() - source.getZ();
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            if (horizontalDist > range) continue;

            float damage = baseDamage;
            ElementType targetElement = ElementUtils.getConsistentAttackElement(target);
            if (targetElement == ElementType.FIRE) {
                damage *= (float) ElementalThunderFrostReactionsConfig.frostbiteDamageFireMultiplier;
            } else if (targetElement == ElementType.NATURE) {
                damage *= (float) ElementalThunderFrostReactionsConfig.frostbiteDamageNatureMultiplier;
            } else if (targetElement == ElementType.THUNDER) {
                damage *= (float) ElementalThunderFrostReactionsConfig.frostbiteDamageThunderMultiplier;
            } else if (targetElement == ElementType.FROST) {
                damage *= (float) ElementalThunderFrostReactionsConfig.frostbiteDamageFrostMultiplier;
            }
            ElementDamageHelper.applyDamage(target, damage, target.damageSources().freeze());

            if (ElementalThunderFrostReactionsConfig.frostScorchSteamReactionEnabled
                    && ElementalFireNatureReactionsConfig.steamLowHeatMaxLevel > 0) {
                CompoundTag targetData = target.getPersistentData();
                if (isFrozen(target) && targetData.contains(ScorchedHandler.NBT_SCORCHED_TICKS)) {
                    int firePower = targetData.getInt(ScorchedHandler.NBT_SCORCHED_SOURCE_FIRE_POWER);
                    int frozenStacks = targetData.getInt(NBT_FREEZE_STACKS);
                    if (frozenStacks <= 0) frozenStacks = 1;

                    target.removeEffect(ModMobEffects.FREEZE.get());
                    targetData.remove(NBT_FREEZE_STACKS);
                    targetData.remove(NBT_FROZEN_FROSTBITE_STACKS);
                    targetData.putLong(NBT_FREEZE_COOLDOWN,
                            target.level().getGameTime() + ElementalThunderFrostReactionsConfig.freezeCooldownTicks);
                    clearFrostbite(target);
                    target.clearFire();
                    targetData.remove(ScorchedHandler.NBT_SCORCHED_TICKS);
                    targetData.remove(ScorchedHandler.NBT_SCORCHED_STRENGTH);
                    targetData.remove(ScorchedHandler.NBT_SCORCHED_SOURCE_FIRE_POWER);
                    targetData.remove(ScorchedHandler.NBT_SCORCHED_DAMAGE_MULT);

                    int step = 20;
                    int level = Math.max(1, Math.min(firePower / step + frozenStacks, ElementalFireNatureReactionsConfig.steamLowHeatMaxLevel));
                    if (!SteamReactionHandler.isOnSteamCooldown(target)) {
                        SteamReactionHandler.spawnSteamCloud(target, false, level);
                        SteamReactionHandler.applySteamCooldown(target, SteamReactionHandler.computeCloudDuration(false, level));
                    }
                }
            }

            DebugCommand.AuraDamageLogContext actx = new DebugCommand.AuraDamageLogContext();
            actx.source = source;
            actx.target = target;
            actx.damage = damage;
            actx.reactionKey = "frostbite";
            DebugCommand.sendAuraDamageLog(actx);
        }
    }

    private static void clearFrostbiteAuraEffects(LivingEntity source, int stacks) {
        double range = getAuraRange(stacks);

        AABB area = new AABB(
                source.getX() - range, source.getY() - range, source.getZ() - range,
                source.getX() + range, source.getY() + range, source.getZ() + range
        );
        List<LivingEntity> nearby = source.level().getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity target : nearby) {
            if (target == source) continue;
            if (shouldSkipFrostbiteAuraTarget(target, source)) continue;

            double dx = target.getX() - source.getX();
            double dz = target.getZ() - source.getZ();
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            if (horizontalDist > range) continue;

            if (target.hasEffect(ModMobEffects.FREEZE.get())) {
                target.removeEffect(ModMobEffects.FREEZE.get());
            }
            CompoundTag targetData = target.getPersistentData();
            targetData.remove(NBT_FREEZE_COOLDOWN);
            targetData.remove(NBT_FREEZE_STACKS);
        }
    }
}
