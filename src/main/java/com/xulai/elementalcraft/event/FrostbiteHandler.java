package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.logic.MobAttributeLogic;
import com.xulai.elementalcraft.sound.ModSounds;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.potion.FrostbiteEffect;
import com.xulai.elementalcraft.potion.ModMobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import com.xulai.elementalcraft.util.ElementDamageHelper;
import com.xulai.elementalcraft.util.EffectHelper;
import com.xulai.elementalcraft.event.SteamReactionHandler;
import com.xulai.elementalcraft.event.WetnessHandler;
import com.xulai.elementalcraft.event.iss.ISSCore;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
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

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class FrostbiteHandler {
    private static final Random RANDOM = new Random();

    public static final String NBT_FROST_COUNTER_COOLDOWN = "ec_frost_counter_cd";
    public static final String NBT_FROSTBITE_STACKS = "EC_FrostbiteStacks";
    public static final String NBT_FROSTBITE_DURATION = "EC_FrostbiteDuration";
    public static final String NBT_FROST_HEAT_ACCEL = "EC_FrostbiteHeatAccel";
    public static final String NBT_FROST_HEAT_MULT = "EC_FrostbiteHeatMult";
    public static final String NBT_FROSTBITE_APPLY_TICK = "EC_FrostbiteApplyTick";
    public static final String NBT_FREEZE_COOLDOWN = "EC_FreezeCooldown";

    public static final String NBT_FREEZE_ORIGINAL_NO_AI = "EC_FreezeOriginalNoAI";
    public static final String NBT_FREEZE_AI_DISABLED = "EC_FreezeAIDisabled";

    public static final String NBT_FROZEN_FROSTBITE_STACKS = "EC_FrozenFrostbiteStacks";
    public static final String NBT_FREEZE_STACKS = "EC_FreezeStacks";

    public static final String NBT_FROSTBITE_FIRE_STAND_TIMER = "EC_FrostbiteFireStandTimer";
    public static final String NBT_FROSTBITE_SOURCE_FROST_POWER = "EC_FrostbiteSourceFrostPower";
    public static final String NBT_FROSTBITE_PERIODIC_LOGGED = "EC_FrostbitePeriodicLogged";
    public static final String NBT_FROSTBITE_LAST_PERIODIC_DMG = "EC_FrostbiteLastPeriodicDmg";

    public static final String NBT_TEMP_FROSTBITE = "EC_TempFrostbite";
    public static final String NBT_TEMP_FROSTBITE_STACKS = "EC_TempFrostbiteStacks";
    public static final String NBT_FROSTBITE_AURA_LOGGED = "EC_FrostbiteAuraLogged";
    public static final String NBT_FROST_AURA_TRACKED = "EC_FrostAuraTracked";

    private static final Set<UUID> removedByClear = new HashSet<>();
    private static volatile boolean suppressRemoveCleanup = false;

    private static final java.util.Map<net.minecraft.resources.ResourceKey<Level>, ActiveFrostBurst> activeFrostBursts = new java.util.HashMap<>();

    static class ActiveFrostBurst {
        final double x, y, z;
        final long startTick;
        final UUID ownerUUID;
        final double heightCeiling;
        final double maxRadius;
        final double expansionSpeed;
        double currentRadius;
        int tickCount;
        int dwellTicks;
        final Set<UUID> hitEntities = new HashSet<>();

        ActiveFrostBurst(double x, double y, double z, long startTick, UUID ownerUUID) {
            this.x = x; this.y = y; this.z = z;
            this.startTick = startTick;
            this.ownerUUID = ownerUUID;
            this.heightCeiling = ElementalThunderFrostReactionsConfig.frostCounterHeightCeiling;
            this.maxRadius = ElementalThunderFrostReactionsConfig.frostCounterMaxRadius;
            this.expansionSpeed = ElementalThunderFrostReactionsConfig.frostCounterExpansionSpeed;
            this.currentRadius = 0;
            this.tickCount = 0;
            this.dwellTicks = 0;
        }
    }

    public static void triggerFrostBurst(LivingEntity source) {
        if (!(source.level() instanceof ServerLevel sl)) return;
        net.minecraft.resources.ResourceKey<Level> dim = source.level().dimension();
        activeFrostBursts.put(dim, new ActiveFrostBurst(
            source.getX(), source.getY(), source.getZ(),
            source.level().getGameTime(), source.getUUID()));

        AreaEffectCloud cloud = SteamReactionHandler.spawnSteamCloud(source, false, 1);
        if (cloud != null) {
            cloud.addTag(SteamReactionHandler.TAG_FROSTED);
            cloud.addTag("EC_FrostOwner_" + source.getUUID());
            float expansionPerTick = (float) (ElementalThunderFrostReactionsConfig.frostCounterExpansionSpeed / 20.0);
            cloud.setRadiusPerTick(expansionPerTick);
            cloud.setWaitTime(0);
            cloud.setRadius(0.5F);
            int duration = (int) (ElementalThunderFrostReactionsConfig.frostCounterMaxRadius / ElementalThunderFrostReactionsConfig.frostCounterExpansionSpeed * 20);
            cloud.setDuration(duration);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTryFrostCounter(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        LivingEntity target = event.getEntity();
        double bloodThreshold = ElementalThunderFrostReactionsConfig.frostCounterBloodThreshold;
        if (bloodThreshold <= 0) return;
        float currentHP = target.getHealth() + target.getAbsorptionAmount();
        if (currentHP - event.getAmount() >= target.getMaxHealth() * bloodThreshold) return;
        if (ElementUtils.getConsistentAttackElement(target) != ElementType.FROST) return;
        int frostPower = ElementUtils.getDisplayEnhancement(target, ElementType.FROST);
        int threshold = ElementalThunderFrostReactionsConfig.frostCounterStrengthThreshold;
        if (threshold <= 0 || frostPower < threshold) {
            if (threshold > 0) {
                DebugCommand.sendReactionFailed(target, "frost_counter", "power_low",
                        target.getDisplayName(),
                        String.valueOf(frostPower),
                        String.valueOf(threshold));
            }
            return;
        }
        if (!ReactionHandler.checkHealthRecovery(target, NBT_FROST_COUNTER_COOLDOWN)) return;
        triggerFrostBurst(target);
        ReactionHandler.setHealthRecoveryThreshold(target, NBT_FROST_COUNTER_COOLDOWN,
                target.getMaxHealth(), ElementalThunderFrostReactionsConfig.frostCounterHealthRecoveryThreshold);
    }

    public static final String NBT_FROST_BURST_FROZEN = "EC_FrostBurstFrozen";

    @SubscribeEvent
    public static void onLevelTick(net.minecraftforge.event.TickEvent.LevelTickEvent event) {
        if (event.level.isClientSide()) return;
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (activeFrostBursts.isEmpty()) return;

        net.minecraft.resources.ResourceKey<Level> dim = event.level.dimension();
        ActiveFrostBurst burst = activeFrostBursts.get(dim);
        if (burst == null) return;

        if (!(event.level instanceof ServerLevel sl)) return;

        burst.tickCount++;

        if (burst.dwellTicks > 0) {
            burst.dwellTicks--;
            if (burst.dwellTicks == 0) {
                for (UUID trackedId : burst.hitEntities) {
                    Entity tracked = sl.getEntity(trackedId);
                    if (tracked instanceof LivingEntity le) {
                        le.getPersistentData().remove(NBT_FROST_BURST_FROZEN);
                    }
                }
                activeFrostBursts.remove(dim);
                return;
            }
        } else {
            burst.currentRadius += burst.expansionSpeed / 20.0;
            if (burst.currentRadius >= burst.maxRadius) {
                burst.currentRadius = burst.maxRadius;
                burst.dwellTicks = 40;
            }
        }

        EffectHelper.playFrostBurstRing(sl, burst.x, burst.y, burst.z, burst.currentRadius);

        AABB box = new AABB(
            burst.x - burst.currentRadius, burst.y - burst.heightCeiling, burst.z - burst.currentRadius,
            burst.x + burst.currentRadius, burst.y + burst.heightCeiling, burst.z + burst.currentRadius);

        java.util.Set<UUID> currentlyInRange = new HashSet<>();

        for (LivingEntity target : sl.getEntitiesOfClass(LivingEntity.class, box)) {
            if (burst.ownerUUID != null && target.getUUID().equals(burst.ownerUUID)) continue;
            if (target instanceof Player player && player.isCreative()) continue;
            if (target.isDeadOrDying()) continue;
            if (isFreezeImmune(target)) continue;

            double dx = target.getX() - burst.x;
            double dz = target.getZ() - burst.z;
            if (Math.sqrt(dx * dx + dz * dz) > burst.currentRadius) continue;
            if (Math.abs(target.getY() - burst.y) > burst.heightCeiling) continue;

            UUID targetId = target.getUUID();
            boolean wasNew = !burst.hitEntities.contains(targetId);
            currentlyInRange.add(targetId);
            target.getPersistentData().putBoolean(NBT_FROST_BURST_FROZEN, true);
            burst.hitEntities.add(targetId);
        }

        java.util.Set<UUID> leftRange = new HashSet<>(burst.hitEntities);
        leftRange.removeAll(currentlyInRange);
        for (UUID leftId : leftRange) {
            Entity leftEntity = sl.getEntity(leftId);
            if (leftEntity instanceof LivingEntity le) {
                le.getPersistentData().remove(NBT_FROST_BURST_FROZEN);
            }
        }
        burst.hitEntities.retainAll(currentlyInRange);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getSource().typeHolder().unwrapKey().isPresent()) {
            var key = event.getSource().typeHolder().unwrapKey().get();
            if ("irons_spellbooks".equals(key.location().getNamespace())
                    && key.location().getPath().endsWith("_magic")) return;
        }
        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof LivingEntity attacker)) return;
        LivingEntity target = event.getEntity();

        ElementType attackType = ElementUtils.getConsistentAttackElement(attacker);
        double frostPower = ElementUtils.getDisplayEnhancement(attacker, ElementType.FROST);

        if (attackType != ElementType.FROST) return;

        double threshold = ElementalThunderFrostReactionsConfig.frostStrengthThreshold;
        boolean thresholdMet = threshold > 0 && frostPower >= threshold;

        if (!thresholdMet) {
            DebugCommand.sendReactionFailed(target, "frostbite", "threshold",
                    attacker.getDisplayName(), target.getDisplayName(),
                    Component.literal(String.format("%.1f", frostPower)).withStyle(ChatFormatting.AQUA),
                    Component.literal(String.format("%.1f", threshold)).withStyle(ChatFormatting.GOLD));
            return;
        }

        double baseChance = ElementalThunderFrostReactionsConfig.frostbiteBaseChance;
        double scalingStep = ElementalThunderFrostReactionsConfig.frostbiteScalingStep;
        double scalingChance = ElementalThunderFrostReactionsConfig.frostbiteScalingChance;
        double stackingBonus = ElementalThunderFrostReactionsConfig.frostbiteStackingBonusChance;
        double wetnessBonus = ElementalThunderFrostReactionsConfig.frostbiteWetnessBonusChance;

        int scalingSteps = 0;
        double chance;
        if (frostPower < threshold + scalingStep) {
            chance = baseChance;
        } else {
            scalingSteps = (int) ((frostPower - threshold) / scalingStep);
            chance = baseChance + (scalingSteps * scalingChance);
        }
        chance = Math.min(1.0, chance);

        double wetBonus = 0;
        int targetWetness = WetnessHandler.getWetnessLevel(target);
        if (targetWetness > 0) {
            wetBonus = targetWetness * wetnessBonus;
            chance += wetBonus;
            chance = Math.min(1.0, chance);
        }

        boolean hasExistingFrostbite = hasFrostbite(target);
        double appliedStackingBonus = 0;
        if (hasExistingFrostbite) {
            appliedStackingBonus = stackingBonus;
            chance += stackingBonus;
            chance = Math.min(1.0, chance);
        }

        double biomeBonus = 0;
        String biomeTag = "";
        if (target.level().canSeeSky(target.blockPosition())) {
            var biome = target.level().getBiome(target.blockPosition()).value();
            double temp = biome.getBaseTemperature();
            if (temp >= 2.0) {
                biomeBonus = -ElementalThunderFrostReactionsConfig.frostbiteHotBiomeChancePenalty;
                biomeTag = Component.translatable("debug.elementalcraft.reaction.biome.hot").getString();
            } else if (temp <= 0.3) {
                biomeBonus = ElementalThunderFrostReactionsConfig.frostbiteColdBiomeChanceBonus;
                biomeTag = Component.translatable("debug.elementalcraft.reaction.biome.cold").getString();
            }
            chance += biomeBonus;
            chance = Math.min(1.0, Math.max(0.0, chance));
        }

        if (RANDOM.nextDouble() >= chance) {
            DebugCommand.sendFrostbiteChanceFailed(attacker, target, frostPower, baseChance, scalingSteps, scalingChance, appliedStackingBonus, wetBonus, biomeBonus, biomeTag);
            return;
        }

        int stacksToApply = ElementalThunderFrostReactionsConfig.frostbiteMaxStacksPerAttack;
        applyFrostbite(target, attacker, stacksToApply, chance, frostPower, baseChance, scalingSteps, scalingChance, appliedStackingBonus, wetBonus, biomeBonus, biomeTag);
    }

    public static boolean applyFrostbite(LivingEntity target, LivingEntity attacker, int layersToAdd, double chance, double frostPower, double baseChance, int scalingSteps, double scalingChance, double stackingBonus, double wetBonus, double biomeBonus, String biomeTag) {
        if (target.level().isClientSide) return false;
        if (target instanceof Player player && player.isCreative()) return false;

        if (ElementalThunderFrostReactionsConfig.frostbiteClearByHeatEnabled
                && target.level().dimension() == Level.NETHER) {
            if (attacker != null) {
                DebugCommand.sendReactionFailed(target, "frostbite", "nether_heat",
                        attacker.getDisplayName(), target.getDisplayName());
            }
            return false;
        }

        if (SteamReactionHandler.isInHighHeatCloud(target)) {
            if (attacker != null) {
                DebugCommand.sendReactionFailed(target, "frostbite", "high_heat_cloud",
                        attacker.getDisplayName(), target.getDisplayName());
            }
            return false;
        }

        if (isFrostbiteImmune(target)) {
            if (attacker != null) {
                DebugCommand.sendReactionFailed(target, "frostbite", "immune",
                        attacker.getDisplayName(), target.getDisplayName());
            }
            return false;
        }

        if (isFrozen(target)) {
            if (attacker != null) {
                DebugCommand.sendReactionFailed(target, "frostbite", "freeze",
                        attacker.getDisplayName(), target.getDisplayName());
            }
            return false;
        }

        CompoundTag data = target.getPersistentData();
        long gameTime = target.level().getGameTime();

        if (data.contains(NBT_FROSTBITE_APPLY_TICK)) {
            if (data.getLong(NBT_FROSTBITE_APPLY_TICK) == gameTime) {
                if (attacker != null) {
                    DebugCommand.sendReactionFailed(target, "frostbite", "same_tick",
                            attacker.getDisplayName(), target.getDisplayName());
                }
                return false;
            }
        }

        int currentStacks = data.getInt(NBT_FROSTBITE_STACKS);
        int maxStacks = ElementalThunderFrostReactionsConfig.frostbiteMaxTotalStacks;

        if (currentStacks >= maxStacks) {
            if (attacker != null) {
                DebugCommand.sendReactionFailed(target, "frostbite", "max_stacks",
                        attacker.getDisplayName(), target.getDisplayName(),
                        Component.literal(String.valueOf(maxStacks)).withStyle(ChatFormatting.RED));
            }
            return false;
        }

        int newStacks = Math.min(maxStacks, currentStacks + layersToAdd);

        int baseDuration = ElementalThunderFrostReactionsConfig.frostbiteBaseDurationTicks;
        int perExtraStack = ElementalThunderFrostReactionsConfig.frostbiteDurationPerExtraStackTicks;
        int durationTicks = baseDuration + (newStacks - 1) * perExtraStack;
        durationTicks = Math.max(1, durationTicks);

        double speedReduction = ElementalThunderFrostReactionsConfig.frostbiteSpeedReductionPerStack;

        data.putBoolean(NBT_FROST_HEAT_ACCEL, false);
        data.remove(NBT_FROST_HEAT_MULT);
        data.remove(NBT_FROSTBITE_FIRE_STAND_TIMER);
        data.putInt(NBT_FROSTBITE_STACKS, newStacks);
        data.putInt(NBT_FROSTBITE_DURATION, durationTicks);
        data.putLong(NBT_FROSTBITE_APPLY_TICK, gameTime);
        data.putInt(NBT_FROSTBITE_SOURCE_FROST_POWER, (int) frostPower);
        data.putInt(NBT_FROSTBITE_PERIODIC_LOGGED, 0);

        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.5f, 1.5f);
        DebugCommand.sendFrostbiteLog(attacker, target, layersToAdd, chance, durationTicks, speedReduction * newStacks, frostPower, baseChance, scalingSteps, scalingChance, stackingBonus, wetBonus, biomeBonus, biomeTag);

        syncFrostbiteEffect(target, newStacks, durationTicks);

        target.getPersistentData().remove(WetnessHandler.NBT_REACTION_RESOLVED);

        return true;
    }

    public static void checkFreezeFromWetness(LivingEntity target, LivingEntity attacker, int frostPower) {
        if (target.level().isClientSide) return;
        if (!hasFrostbite(target) && !isTempFrostbite(target)) return;
        if (WetnessHandler.getWetnessLevel(target) <= 0) return;

        if (isFrozen(target) || isOnFreezeCooldown(target)) {
            if (isOnFreezeCooldown(target)) {
                DebugCommand.sendReactionCooldownBlock(target, "freeze", DebugCommand.getRemainingCooldown(target, NBT_FREEZE_COOLDOWN));
            }
            if (hasFrostbite(target)) {
                int auraStacks = target.getPersistentData().getInt(NBT_FROSTBITE_STACKS);
                if (ElementalThunderFrostReactionsConfig.frostbiteAuraThreshold > 0 && auraStacks >= ElementalThunderFrostReactionsConfig.frostbiteAuraThreshold) {
                    clearFrostbiteAuraEffects(target, auraStacks);
                }
                clearFrostbite(target);
            } else {
                clearTempFrostbite(target);
            }
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
            if (hasFrostbite(target)) {
                int auraStacks = data.getInt(NBT_FROSTBITE_STACKS);
                if (ElementalThunderFrostReactionsConfig.frostbiteAuraThreshold > 0 && auraStacks >= ElementalThunderFrostReactionsConfig.frostbiteAuraThreshold) {
                    clearFrostbiteAuraEffects(target, auraStacks);
                }
            }
            clearFrostbite(target);
            WetnessHandler.clearWetnessData(target);
            DebugCommand.sendReactionFailed(target, "freeze", "immune", target.getDisplayName());
            return;
        }

        if (ElementalThunderFrostReactionsConfig.freezeClearSporesEnabled) {
            if (ModMobEffects.SPORES.isPresent()
                    && target.hasEffect(ModMobEffects.SPORES.get())) {
                target.removeEffect(ModMobEffects.SPORES.get());
            }
        }

        int frostbiteStacks = data.getInt(NBT_FROSTBITE_STACKS);
        if (frostbiteStacks <= 0) {
            frostbiteStacks = data.getInt(NBT_TEMP_FROSTBITE_STACKS);
        }
        if (frostbiteStacks <= 0) return;
        if (ElementalThunderFrostReactionsConfig.freezeMaxStacks <= 0) return;

        int wetnessLevel = WetnessHandler.getWetnessLevel(target);
        int freezeStacks = Math.max(frostbiteStacks, wetnessLevel);
        int maxStacks = ElementalThunderFrostReactionsConfig.freezeMaxStacks;
        if (freezeStacks > maxStacks) {
            freezeStacks = maxStacks;
        }

        float settlementDamage = frostbiteStacks * (float) ElementalThunderFrostReactionsConfig.freezeSettlementDamagePerStack * (float) ElementalThunderFrostReactionsConfig.frostbitePeriodicDamage;

        data.putInt(NBT_FROZEN_FROSTBITE_STACKS, frostbiteStacks);

        int freezeDuration = freezeStacks * ElementalThunderFrostReactionsConfig.freezeDurationPerStackTicks;
        if (freezeDuration < 20) {
            freezeDuration = 20;
        }

        DebugCommand.FreezeLogContext freezeCtx = new DebugCommand.FreezeLogContext();
        freezeCtx.target = target;
        freezeCtx.frostbiteStacks = frostbiteStacks;
        freezeCtx.freezeStacks = freezeStacks;
        freezeCtx.damage = settlementDamage;
        freezeCtx.wetnessLevel = wetnessLevel;
        freezeCtx.freezeDuration = freezeDuration;
        DebugCommand.sendFreezeLog(freezeCtx);

        target.addEffect(new MobEffectInstance(ModMobEffects.FREEZE.get(), freezeDuration, freezeStacks - 1, false, true, true));

        target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                ModSounds.FREEZING.get(), SoundSource.PLAYERS, 1.0f, 1.0f);

        data.putInt(NBT_FREEZE_STACKS, freezeStacks);
        data.putLong(NBT_FREEZE_COOLDOWN, gameTime + freezeDuration + ElementalThunderFrostReactionsConfig.freezeCooldownTicks);

        if (target.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 30, 0.3, 0.3, 0.3, 0.05);
        }

        if (settlementDamage > 0) {
            ElementDamageHelper.applyDamage(target, settlementDamage, ModDamageTypes.source(target.level(), ModDamageTypes.FROSTBITE));
        }

        if (hasFrostbite(target) && ElementalThunderFrostReactionsConfig.frostbiteAuraThreshold > 0 && frostbiteStacks >= ElementalThunderFrostReactionsConfig.frostbiteAuraThreshold) {
            clearFrostbiteAuraEffects(target, frostbiteStacks);
        }
        clearFrostbite(target);
        WetnessHandler.clearWetnessData(target);

        if (target.level() instanceof ServerLevel serverLevel && frostbiteStacks >= 3) {
            double radius = 3.0 + (frostbiteStacks - 3) * 1.0;
            EffectHelper.spawnFreezeColdCloud(serverLevel, target, radius, 100);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            if (entity.hasEffect(ModMobEffects.FREEZE.get())) {
                entity.setTicksFrozen(300);
            } else {
                if (entity.getTicksFrozen() > 0 && !entity.isInPowderSnow) {
                    entity.setTicksFrozen(0);
                }
            }
            return;
        }

        if (entity instanceof Player player && player.isCreative()) return;

        if (entity.hasEffect(ModMobEffects.FREEZE.get())) {
            entity.setTicksFrozen(300);
        } else {
            if (entity.getTicksFrozen() > 0 && !entity.isInPowderSnow) {
                entity.setTicksFrozen(0);
            }
        }

        processRemovedByClear(entity);

        CompoundTag data = entity.getPersistentData();
        long gameTime = entity.level().getGameTime();

        if (data.contains(NBT_FREEZE_COOLDOWN) && gameTime >= data.getLong(NBT_FREEZE_COOLDOWN)) {
            data.remove(NBT_FREEZE_COOLDOWN);
        }

        if (data.contains(NBT_FREEZE_STACKS) && !entity.hasEffect(ModMobEffects.FREEZE.get())) {
            boolean inBurst = data.getBoolean(NBT_FROST_BURST_FROZEN);
            if (!inBurst) {
                entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 1.0f);
                data.remove(NBT_FREEZE_STACKS);
                data.remove(NBT_FROST_BURST_FROZEN);
            }
        }

        if (data.contains(NBT_FROSTBITE_STACKS) && !entity.hasEffect(ModMobEffects.FROSTBITE.get()) && !isFrozen(entity)) {
            clearFrostbite(entity);
            return;
        }

        if (!data.contains(NBT_FROSTBITE_STACKS)) {
            if (isTempFrostbite(entity)) {
                int damageInterval = ElementalThunderFrostReactionsConfig.frostbiteDamageIntervalTicks;
                if (damageInterval < 1) damageInterval = 1;
                if (entity.tickCount % damageInterval == 0) {
                    float baseDamage = (float) ElementalThunderFrostReactionsConfig.frostbitePeriodicDamage;
                    float damage = baseDamage;
                    ElementType targetElement = ElementUtils.getConsistentAttackElement(entity);
                    float elementMult = 1.0f;
                    if (targetElement == ElementType.FIRE) {
                        elementMult = (float) ElementalThunderFrostReactionsConfig.frostbiteDamageFireMultiplier;
                    } else if (targetElement == ElementType.NATURE) {
                        elementMult = (float) ElementalThunderFrostReactionsConfig.frostbiteDamageNatureMultiplier;
                    } else if (targetElement == ElementType.THUNDER) {
                        elementMult = (float) ElementalThunderFrostReactionsConfig.frostbiteDamageThunderMultiplier;
                    } else if (targetElement == ElementType.FROST) {
                        elementMult = (float) ElementalThunderFrostReactionsConfig.frostbiteDamageFrostMultiplier;
                    }
                    damage *= elementMult;
                    ElementDamageHelper.applyDamage(entity, damage, ModDamageTypes.source(entity.level(), ModDamageTypes.FROSTBITE));
                    entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                            SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.0f, 1.0f);

                    if (data.getInt(NBT_FROSTBITE_AURA_LOGGED) == 0) {
                        DebugCommand.FrostbiteAuraDamageLogContext actx = new DebugCommand.FrostbiteAuraDamageLogContext();
                        actx.source = null;
                        actx.target = entity;
                        actx.baseDamage = baseDamage;
                        actx.element = targetElement;
                        actx.elementMult = elementMult;
                        actx.finalDamage = damage;
                        DebugCommand.sendFrostbiteAuraDamageLog(actx);
                        data.putInt(NBT_FROSTBITE_AURA_LOGGED, 1);
                    }
                }
                boolean hasWetness = WetnessHandler.getWetnessLevel(entity) > 0;
                if (hasWetness) {
                    if (isFrozen(entity) || isOnFreezeCooldown(entity)) {
                        if (isOnFreezeCooldown(entity)) {
                            DebugCommand.sendReactionCooldownBlock(entity, "freeze", DebugCommand.getRemainingCooldown(entity, NBT_FREEZE_COOLDOWN));
                        }
                        clearTempFrostbite(entity);
                        WetnessHandler.clearWetnessData(entity);
                    } else {
                        WetnessHandler.resolveElementReactionConflict(entity, null);
                    }
                }

                decaySpores(entity);
                return;
            }
            if (!isFrozen(entity) && !isTempFrostbite(entity) && entity.hasEffect(ModMobEffects.FROSTBITE.get())) {
                MobEffectInstance effectInstance = entity.getEffect(ModMobEffects.FROSTBITE.get());
                if (effectInstance != null) {
                    int effectStacks = effectInstance.getAmplifier() + 1;
                    int effectDuration = effectInstance.getDuration();
                    data.putInt(NBT_FROSTBITE_STACKS, effectStacks);
                    data.putInt(NBT_FROSTBITE_DURATION, effectDuration);
                    data.putBoolean(NBT_FROST_HEAT_ACCEL, false);
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
            BlockPos firePos = entity.blockPosition();
            BlockState fireState = entity.level().getBlockState(firePos);
            if (fireState.is(Blocks.FIRE) || fireState.is(Blocks.SOUL_FIRE)) {
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
            } else if (!data.contains(NBT_FROSTBITE_FIRE_STAND_TIMER) && entity.isOnFire()) {
                if (auraActive) {
                    clearFrostbiteAuraEffects(entity, stacks);
                }
                clearFrostbite(entity);
                entity.playSound(SoundEvents.FIRE_EXTINGUISH, 1.0f, 1.0f);
                return;
            }

            if (WetnessHandler.checkHeatSource(entity.level(), entity.blockPosition(),
                    ElementalThunderFrostReactionsConfig.frostbiteHeatSearchRadius)) {
                if (auraActive) {
                    clearFrostbiteAuraEffects(entity, stacks);
                }
                clearFrostbite(entity);
                entity.playSound(SoundEvents.FIRE_EXTINGUISH, 1.0f, 1.0f);
                return;
            }
        }


        // Heat source snap
        double heatMult = checkFrostbiteHeatAccelerator(entity, entity.level(), entity.blockPosition());
        boolean hasHeat = heatMult > 1.0;
        boolean hadHeat = data.getBoolean(NBT_FROST_HEAT_ACCEL);
        if (hasHeat != hadHeat) {
            data.putBoolean(NBT_FROST_HEAT_ACCEL, hasHeat);
            if (hasHeat) {
                data.putDouble(NBT_FROST_HEAT_MULT, heatMult);
                duration = Math.max(1, (int)(duration / heatMult));
            } else {
                double storedMult = data.getDouble(NBT_FROST_HEAT_MULT);
                data.remove(NBT_FROST_HEAT_MULT);
                if (storedMult > 1.0) {
                    duration = Math.max(1, (int)(duration * storedMult));
                }
            }
            data.putInt(NBT_FROSTBITE_DURATION, duration);
            suppressRemoveCleanup = true;
            try {
                entity.removeEffect(ModMobEffects.FROSTBITE.get());
                entity.addEffect(new MobEffectInstance(ModMobEffects.FROSTBITE.get(),
                        duration, stacks - 1, false, false, true));
            } finally {
                suppressRemoveCleanup = false;
            }
        }

        // Normal per-tick decay
        duration--;
        data.putInt(NBT_FROSTBITE_DURATION, duration);

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
                WetnessHandler.resolveElementReactionConflict(entity, null);
            }
        }

        if (hasFrostbite(entity)) {
            decaySpores(entity);
        }
    }

    private static void cleanupFrostbitePersistentData(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        int auraStacks = data.getInt(NBT_FROSTBITE_STACKS);

        data.remove(NBT_FROSTBITE_STACKS);
        data.remove(NBT_FROSTBITE_DURATION);
        data.remove(NBT_FROST_HEAT_ACCEL);
        data.remove(NBT_FROST_HEAT_MULT);
        data.remove(NBT_FROSTBITE_APPLY_TICK);
        data.remove(NBT_FROSTBITE_FIRE_STAND_TIMER);
        data.remove(NBT_FROSTBITE_SOURCE_FROST_POWER);
        data.remove(NBT_FROSTBITE_PERIODIC_LOGGED);
        data.remove(NBT_FROSTBITE_LAST_PERIODIC_DMG);

        if (ElementalThunderFrostReactionsConfig.frostbiteAuraThreshold > 0 && auraStacks >= ElementalThunderFrostReactionsConfig.frostbiteAuraThreshold) {
            clearFrostbiteAuraEffects(entity, auraStacks);
        }

        data.remove(NBT_FROST_AURA_TRACKED);
        data.remove(NBT_TEMP_FROSTBITE);
        data.remove(NBT_TEMP_FROSTBITE_STACKS);
        entity.setTicksFrozen(0);
    }

    public static void clearFrostbite(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(NBT_FROSTBITE_STACKS) && !data.contains(NBT_FROSTBITE_DURATION)
                && !entity.hasEffect(ModMobEffects.FROSTBITE.get())) {
            return;
        }

        cleanupFrostbitePersistentData(entity);

        if (entity.hasEffect(ModMobEffects.FROSTBITE.get())) {
            entity.removeEffect(ModMobEffects.FROSTBITE.get());
        }

        if (entity instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundRemoveMobEffectPacket(sp.getId(), ModMobEffects.FROSTBITE.get()));
        }
    }

    @SubscribeEvent
    public static void onMobEffectRemoved(net.minecraftforge.event.entity.living.MobEffectEvent.Remove event) {
        if (suppressRemoveCleanup) return;
        if (event.getEffect() != ModMobEffects.FROSTBITE.get()) return;
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(NBT_FROSTBITE_STACKS) && !data.contains(NBT_FROSTBITE_DURATION)) return;
        cleanupFrostbitePersistentData(entity);
        removedByClear.add(entity.getUUID());
    }

    @SubscribeEvent
    public static void onFreezeEffectAdded(net.minecraftforge.event.entity.living.MobEffectEvent.Added event) {
        if (event.getEffectInstance() == null || event.getEffectInstance().getEffect() != ModMobEffects.FREEZE.get()) return;
        if (!ElementalThunderFrostReactionsConfig.freezeClearSporesEnabled) return;
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        if (ModMobEffects.SPORES.isPresent()
                && entity.hasEffect(ModMobEffects.SPORES.get())) {
            entity.removeEffect(ModMobEffects.SPORES.get());
        }
    }

    private static void processRemovedByClear(LivingEntity entity) {
        if (removedByClear.remove(entity.getUUID())) {
            if (entity instanceof ServerPlayer sp) {
                sp.connection.send(new ClientboundRemoveMobEffectPacket(sp.getId(), ModMobEffects.FROSTBITE.get()));
            }
        }
    }

    @SubscribeEvent
    public static void onMobEffectExpired(net.minecraftforge.event.entity.living.MobEffectEvent.Expired event) {
        if (event.getEffectInstance() == null || event.getEffectInstance().getEffect() != ModMobEffects.FROSTBITE.get()) return;
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(NBT_FROSTBITE_STACKS) && !data.contains(NBT_FROSTBITE_DURATION)) return;
        clearFrostbite(entity);
    }

    public static boolean isOnFreezeCooldown(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(NBT_FREEZE_COOLDOWN)) return false;
        return entity.level().getGameTime() < data.getLong(NBT_FREEZE_COOLDOWN);
    }

    public static boolean isFrostbiteImmune(LivingEntity target) {
        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).toString();
        if (ElementalConfig.matchesBlacklist(ElementalThunderFrostReactionsConfig.cachedFrostbiteImmunityBlacklist, entityId)) {
            return true;
        }
        double frostResistance = ElementUtils.getDisplayResistance(target, ElementType.FROST);
        double threshold = ElementalThunderFrostReactionsConfig.frostbiteResistImmunityThreshold;
        if (frostResistance >= threshold) {
            return true;
        }
        return false;
    }

    public static boolean isFreezeImmune(LivingEntity target) {
        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).toString();
        if (ElementalConfig.matchesBlacklist(ElementalThunderFrostReactionsConfig.cachedFreezeImmunityBlacklist, entityId)) {
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
        suppressRemoveCleanup = true;
        try {
            if (stacks <= 0) {
                if (entity.hasEffect(ModMobEffects.FROSTBITE.get())) {
                    entity.removeEffect(ModMobEffects.FROSTBITE.get());
                }
                return;
            }

            MobEffectInstance existing = entity.getEffect(ModMobEffects.FROSTBITE.get());
            if (existing != null && existing.getAmplifier() == stacks - 1) {
                if (existing.getDuration() < durationTicks) {
                    entity.removeEffect(ModMobEffects.FROSTBITE.get());
                    entity.addEffect(new MobEffectInstance(ModMobEffects.FROSTBITE.get(), durationTicks, stacks - 1, false, false, true));
                }
                return;
            }
            if (existing != null) {
                entity.removeEffect(ModMobEffects.FROSTBITE.get());
            }
            entity.addEffect(new MobEffectInstance(ModMobEffects.FROSTBITE.get(), durationTicks, stacks - 1, false, false, true));
        } finally {
            suppressRemoveCleanup = false;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if ((event.getEntity().hasEffect(ModMobEffects.FROSTBITE.get()) || isTempFrostbite(event.getEntity()))
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
            boolean isElemental = source.is(DamageTypeTags.IS_FIRE) || source.is(ModDamageTypes.FROSTBITE_THERMAL_SHOCK) || source.is(ModDamageTypes.LAVA_MAGIC) || source.is(ModDamageTypes.STATIC_SHOCK) || source.is(ModDamageTypes.SPORES) || source.is(ModDamageTypes.STEAM_SCALDING) || ISSCore.isISSMagicDamage(source);

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

    public static double getAuraRange(int stacks) {
        double base = ElementalThunderFrostReactionsConfig.frostbiteAuraBaseRange;
        double perExtra = ElementalThunderFrostReactionsConfig.frostbiteAuraRangePerStack;
        int threshold = ElementalThunderFrostReactionsConfig.frostbiteAuraThreshold;
        return base + (stacks - threshold) * perExtra;
    }

    public static boolean isTempFrostbite(LivingEntity entity) {
        return entity.getPersistentData().getBoolean(NBT_TEMP_FROSTBITE);
    }

    public static void applyTempFrostbite(LivingEntity target, int stacks) {
        CompoundTag data = target.getPersistentData();
        boolean isNew = !data.getBoolean(NBT_TEMP_FROSTBITE);
        data.putBoolean(NBT_TEMP_FROSTBITE, true);
        data.putInt(NBT_TEMP_FROSTBITE_STACKS, stacks);
        if (isNew) data.putInt(NBT_FROSTBITE_AURA_LOGGED, 0);
        applyTempFrostbiteSlowness(target, stacks);
        if (WetnessHandler.getWetnessLevel(target) > 0 && !isFrostbiteImmune(target) && !isFreezeImmune(target)) {
            target.getPersistentData().remove(WetnessHandler.NBT_REACTION_RESOLVED);
        }
    }

    public static void clearTempFrostbite(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        data.remove(NBT_TEMP_FROSTBITE);
        data.remove(NBT_TEMP_FROSTBITE_STACKS);
        data.remove(NBT_FROSTBITE_AURA_LOGGED);
        removeTempFrostbiteSlowness(entity);
    }

    private static void applyTempFrostbiteSlowness(LivingEntity entity, int stacks) {
        double reduction = ElementalThunderFrostReactionsConfig.frostbiteSpeedReductionPerStack;
        if (reduction <= 0) return;
        double value = Math.max(-reduction * stacks, -0.9);
        AttributeInstance speedAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance attackAttr = entity.getAttribute(Attributes.ATTACK_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(FrostbiteEffect.SPEED_MODIFIER_UUID);
            speedAttr.addPermanentModifier(new AttributeModifier(FrostbiteEffect.SPEED_MODIFIER_UUID, "temp_frostbite_speed", value, AttributeModifier.Operation.MULTIPLY_BASE));
        }
        if (attackAttr != null) {
            attackAttr.removeModifier(FrostbiteEffect.ATTACK_SPEED_MODIFIER_UUID);
            attackAttr.addPermanentModifier(new AttributeModifier(FrostbiteEffect.ATTACK_SPEED_MODIFIER_UUID, "temp_frostbite_attack_speed", value, AttributeModifier.Operation.MULTIPLY_BASE));
        }
    }

    private static void removeTempFrostbiteSlowness(LivingEntity entity) {
        AttributeInstance speedAttr = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance attackAttr = entity.getAttribute(Attributes.ATTACK_SPEED);
        if (speedAttr != null) speedAttr.removeModifier(FrostbiteEffect.SPEED_MODIFIER_UUID);
        if (attackAttr != null) attackAttr.removeModifier(FrostbiteEffect.ATTACK_SPEED_MODIFIER_UUID);
    }

    private static boolean shouldSkipFrostbiteAuraTarget(LivingEntity target, LivingEntity source) {
        if (target instanceof Player player && player.isCreative()) return true;
        if (target.isDeadOrDying()) return true;
        return false;
    }

    private static double checkFrostbiteHeatAccelerator(LivingEntity entity, Level level, BlockPos center) {
        double mult = ElementalThunderFrostReactionsConfig.frostbiteHeatAccelerateMultiplier;
        if (mult <= 1.0) return 1.0;
        double radius = ElementalThunderFrostReactionsConfig.frostbiteHeatAccelerateRadius;
        if (radius <= 0) return 1.0;
        if (entity.isInWater()) return 1.0;
        if (level.getBiome(center).value().getPrecipitationAt(center) != Biome.Precipitation.NONE) return 1.0;
        int range = (int)Math.ceil(radius);
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    mutablePos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    BlockState state = level.getBlockState(mutablePos);
                    if (state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)) {
                        if (state.getValue(CampfireBlock.LIT)) {
                            return ElementalThunderFrostReactionsConfig.frostbiteHeatAccelerateMultiplier;
                        }
                    }
                    if (state.getBlock() instanceof AbstractFurnaceBlock) {
                        if (state.getValue(AbstractFurnaceBlock.LIT)) {
                            return ElementalThunderFrostReactionsConfig.frostbiteHeatAccelerateMultiplier;
                        }
                    }
                }
            }
        }
        return 1.0;
    }

    private static void decaySpores(LivingEntity entity) {
        double speed = ElementalThunderFrostReactionsConfig.frostbiteSporeDecaySpeed;
        if (speed <= 1) return;
        MobEffectInstance sporeInstance = entity.getEffect(ModMobEffects.SPORES.get());
        if (sporeInstance == null) return;
        int extraDecay = (int) speed - 1;
        if (extraDecay <= 0) return;
        int newDuration = sporeInstance.getDuration() - extraDecay;
        if (newDuration <= 0) {
            entity.removeEffect(ModMobEffects.SPORES.get());
        } else {
            entity.removeEffect(ModMobEffects.SPORES.get());
            entity.addEffect(new MobEffectInstance(ModMobEffects.SPORES.get(), newDuration,
                    sporeInstance.getAmplifier(), sporeInstance.isAmbient(), sporeInstance.isVisible(), sporeInstance.showIcon()));
        }
    }

    private static void applyFrostbiteAuraEffects(LivingEntity source, int stacks) {
        double range = getAuraRange(stacks);

        CompoundTag sourceData = source.getPersistentData();
        String trackedStr = sourceData.getString(NBT_FROST_AURA_TRACKED);
        Set<UUID> oldTracked = new HashSet<>();
        if (!trackedStr.isEmpty()) {
            for (String s : trackedStr.split(",")) {
                try { oldTracked.add(UUID.fromString(s)); } catch (Exception ignored) {}
            }
        }

        AABB area = new AABB(
                source.getX() - range, source.getY() - range, source.getZ() - range,
                source.getX() + range, source.getY() + range, source.getZ() + range
        );
        List<LivingEntity> nearby = source.level().getEntitiesOfClass(LivingEntity.class, area);

        Set<UUID> currentTracked = new HashSet<>();

        for (LivingEntity target : nearby) {
            if (target == source) continue;
            if (shouldSkipFrostbiteAuraTarget(target, source)) continue;

            double dx = target.getX() - source.getX();
            double dy = target.getY() - source.getY();
            double dz = target.getZ() - source.getZ();
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            if (horizontalDist > range) continue;
            if (dy > 2.0) continue;

            if (hasFrostbite(target)) continue;
            if (isFrostbiteImmune(target)) continue;
            if (target.getPersistentData().getBoolean("EC_FleeActive") && target instanceof Mob) continue;

            currentTracked.add(target.getUUID());
            if (ScorchedHandler.isScorched(target)) {
                ScorchedHandler.clearScorched(target);
                continue;
            }
            applyTempFrostbite(target, stacks);

            if (target instanceof Mob) MobAttributeLogic.processFlee(target, source, range);
        }

        for (UUID oldId : oldTracked) {
            if (currentTracked.contains(oldId)) continue;
            if (source.level() instanceof ServerLevel sl) {
                Entity e = sl.getEntity(oldId);
                if (e instanceof LivingEntity le) {
                    clearTempFrostbite(le);
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        for (UUID id : currentTracked) {
            if (sb.length() > 0) sb.append(",");
            sb.append(id.toString());
        }
        sourceData.putString(NBT_FROST_AURA_TRACKED, sb.toString());
    }

    private static void clearFrostbiteAuraEffects(LivingEntity source, int stacks) {
        CompoundTag sourceData = source.getPersistentData();
        String trackedStr = sourceData.getString(NBT_FROST_AURA_TRACKED);
        if (!trackedStr.isEmpty()) {
            for (String s : trackedStr.split(",")) {
                try {
                    UUID id = UUID.fromString(s);
                    if (source.level() instanceof ServerLevel sl) {
                        Entity e = sl.getEntity(id);
                        if (e instanceof LivingEntity le) {
                            if (le.hasEffect(ModMobEffects.FREEZE.get())) {
                                le.removeEffect(ModMobEffects.FREEZE.get());
                            }
                            CompoundTag targetData = le.getPersistentData();
                            targetData.remove(NBT_FREEZE_COOLDOWN);
                            targetData.remove(NBT_FREEZE_STACKS);
                            clearTempFrostbite(le);
                        }
                    }
                } catch (Exception ignored) {}
            }
            sourceData.remove(NBT_FROST_AURA_TRACKED);
        }

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
            clearTempFrostbite(target);
        }
    }
}
