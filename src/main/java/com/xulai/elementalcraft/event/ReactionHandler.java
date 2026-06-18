package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.sound.ModSounds;
import com.xulai.elementalcraft.client.ModParticles;
import com.xulai.elementalcraft.util.EffectHelper;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import com.xulai.elementalcraft.event.ScorchedHandler;
import com.xulai.elementalcraft.event.WetnessHandler;
import com.xulai.elementalcraft.util.ElementDamageHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;
import net.minecraftforge.eventbus.api.EventPriority;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class ReactionHandler {
    private static final Random RANDOM = new Random();
    private static final String NBT_WILDFIRE_COOLDOWN = "ec_wildfire_cd";
    private static final String NBT_WAS_ON_FIRE = "ec_was_on_fire";

    public enum SporeApplyResult {
        SUCCESS, BLACKLISTED, IMMUNE, FROZEN, MAX_STACKS, NOT_REGISTERED
    }
    private static final String NBT_SPREADED = "ec_spreaded";
    private static final String NBT_CONTAGION_SOURCE = "ec_contagion_source";

    public static final String NBT_INFECTED = "ec_infected";

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        if (ElementalFireNatureReactionsConfig.contagionAllowReinfected) {
            CompoundTag data = entity.getPersistentData();
            boolean hasSpores = ModMobEffects.SPORES.isPresent() && ModMobEffects.SPORES.get() != null
                    && entity.hasEffect(ModMobEffects.SPORES.get());
            if (!hasSpores) {
                boolean changed = false;
                if (data.getBoolean(NBT_INFECTED)) {
                    data.putBoolean(NBT_INFECTED, false);
                    changed = true;
                }
                if (data.getBoolean(NBT_SPREADED)) {
                    data.putBoolean(NBT_SPREADED, false);
                    changed = true;
                }
                if (data.contains(NBT_CONTAGION_SOURCE)) {
                    data.remove(NBT_CONTAGION_SOURCE);
                    changed = true;
                }
            }
        }

        if (entity.hasEffect(ModMobEffects.PARALYSIS.get())) {
            if (ModMobEffects.SPORES.isPresent() && ModMobEffects.SPORES.get() != null
                    && entity.hasEffect(ModMobEffects.SPORES.get())) {
                double firePower = ElementalFireNatureReactionsConfig.scorchedTriggerThreshold;
                triggerStaticSporeBlast(entity, firePower);
            }
        }

        if (ModMobEffects.SPORES.isPresent() && ModMobEffects.SPORES.get() != null
                && entity.hasEffect(ModMobEffects.SPORES.get())
                && ScorchedHandler.isScorched(entity)) {
            MobEffectInstance sporeEffect = entity.getEffect(ModMobEffects.SPORES.get());
            if (sporeEffect != null) {
                int stacks = sporeEffect.getAmplifier() + 1;
                int sourceFirePower = ScorchedHandler.getScorchFireStrength(entity);
                triggerToxicBlastFromScorched(entity, stacks, sourceFirePower, entity);
            }
        }

        if (ElementalFireNatureReactionsConfig.sporeEnvironmentalBlastEnabled
                && ModMobEffects.SPORES.isPresent() && ModMobEffects.SPORES.get() != null
                && entity.hasEffect(ModMobEffects.SPORES.get())) {
            boolean triggered = false;
            int envFirePower = (int) ElementalFireNatureReactionsConfig.scorchedTriggerThreshold;
            if (entity.level().dimension() == Level.NETHER) {
                ScorchedHandler.applyScorched(entity, entity, envFirePower, 100, envFirePower, 1.0f, true);
                triggered = true;
            }
            if (!triggered) {
                BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
                BlockPos entityPos = entity.blockPosition();
                for (int x = -2; x <= 2 && !triggered; x++) {
                    for (int y = -2; y <= 2 && !triggered; y++) {
                        for (int z = -2; z <= 2 && !triggered; z++) {
                            mutablePos.set(entityPos.getX() + x, entityPos.getY() + y, entityPos.getZ() + z);
                            if (entity.level().getFluidState(mutablePos).is(FluidTags.LAVA)) {
                                ScorchedHandler.applyScorched(entity, entity, envFirePower, 100, envFirePower, 1.0f, true);
                                triggered = true;
                            }
                        }
                    }
                }
            }
            if (!triggered) {
                BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
                BlockPos entityPos = entity.blockPosition();
                for (int x = -1; x <= 1 && !triggered; x++) {
                    for (int y = -1; y <= 1 && !triggered; y++) {
                        for (int z = -1; z <= 1 && !triggered; z++) {
                            mutablePos.set(entityPos.getX() + x, entityPos.getY() + y, entityPos.getZ() + z);
                            if (entity.level().getBlockState(mutablePos).is(Blocks.MAGMA_BLOCK)) {
                                boolean inWater = false;
                                BlockPos.MutableBlockPos waterCheck = new BlockPos.MutableBlockPos();
                                for (int wx = -1; wx <= 1 && !inWater; wx++) {
                                    for (int wy = -1; wy <= 1 && !inWater; wy++) {
                                        for (int wz = -1; wz <= 1 && !inWater; wz++) {
                                            waterCheck.set(mutablePos.getX() + wx, mutablePos.getY() + wy, mutablePos.getZ() + wz);
                                            if (entity.level().getFluidState(waterCheck).is(FluidTags.WATER)) {
                                                inWater = true;
                                            }
                                        }
                                    }
                                }
                                if (!inWater) {
                                    ScorchedHandler.applyScorched(entity, entity, envFirePower, 100, envFirePower, 1.0f, true);
                                    triggered = true;
                                }
                            }
                        }
                    }
                }
            }
            if (!triggered && entity.isOnFire() && !entity.getPersistentData().getBoolean(NBT_WAS_ON_FIRE)) {
                entity.getPersistentData().putBoolean(NBT_WAS_ON_FIRE, true);
                ScorchedHandler.applyScorched(entity, entity, envFirePower, 100, envFirePower, 1.0f, true);
                triggered = true;
            }
            if (!entity.isOnFire()) {
                entity.getPersistentData().putBoolean(NBT_WAS_ON_FIRE, false);
            }
        }

        if (entity.tickCount % ElementalFireNatureReactionsConfig.CONTAGION_CHECK_INTERVAL != 0) return;

        if (ModMobEffects.SPORES.isPresent() && ModMobEffects.SPORES.get() != null && entity.hasEffect(ModMobEffects.SPORES.get())) {
            MobEffectInstance sporeEffect = entity.getEffect(ModMobEffects.SPORES.get());
            if (sporeEffect == null) return;
            int amplifier = sporeEffect.getAmplifier();
            int stacks = amplifier + 1;
            if (ElementalFireNatureReactionsConfig.sporeReactionThreshold > 0
                    && stacks >= ElementalFireNatureReactionsConfig.sporeReactionThreshold) {
                processContagion(entity, stacks);
            }
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
        double naturePower = ElementUtils.getDisplayEnhancement(attacker, ElementType.NATURE);
        double firePower = ElementUtils.getDisplayEnhancement(attacker, ElementType.FIRE);

        if (attackType == ElementType.NATURE) {
            if (ElementalFireNatureReactionsConfig.natureParasiteBaseThreshold > 0
                    && naturePower >= ElementalFireNatureReactionsConfig.natureParasiteBaseThreshold) {
                double baseChance = ElementalFireNatureReactionsConfig.natureParasiteBaseChance;
                double scalingChance = ElementalFireNatureReactionsConfig.natureParasiteScalingChance;
                double scalingStep = ElementalFireNatureReactionsConfig.natureParasiteScalingStep;
                int scalingSteps = (scalingStep > 0 && naturePower >= scalingStep)
                        ? (int) ((naturePower - scalingStep) / scalingStep) : 0;
                double chance = Math.min(1.0, baseChance + scalingSteps * scalingChance);
                double scaledBase = chance;

                int attackerWetness = WetnessHandler.getWetnessLevel(attacker);
                double wetnessCfg = ElementalFireNatureReactionsConfig.natureParasiteWetnessBonus;
                if (attackerWetness > 0) {
                    chance += attackerWetness * wetnessCfg;
                    chance = Math.min(1.0, chance);
                }

                double stackingBonus = target.hasEffect(ModMobEffects.SPORES.get())
                        ? ElementalFireNatureReactionsConfig.natureParasiteStackingBonus : 0.0;
                if (stackingBonus > 0) {
                    chance += stackingBonus;
                    chance = Math.min(1.0, chance);
                }

                boolean triggered = RANDOM.nextDouble() < chance;
                if (triggered) {
                    SporeApplyResult result = stackSporeEffect(target, ElementalFireNatureReactionsConfig.natureParasiteAmount, attacker);
                    if (result == SporeApplyResult.SUCCESS) {
                        EffectHelper.playSporeAmbient(target);
                        DebugCommand.sendNatureParasiteSuccess(attacker, target, ElementalFireNatureReactionsConfig.natureParasiteAmount, baseChance, scalingSteps, scalingChance, stackingBonus, attackerWetness, wetnessCfg, chance);
                    } else {
                        String reasonKey = switch (result) {
                            case BLACKLISTED -> "blacklist";
                            case IMMUNE -> "immune";
                            case FROZEN -> "frozen";
                            case MAX_STACKS -> "max_stacks";
                            default -> "unknown";
                        };
                        if (result == SporeApplyResult.IMMUNE) {
                            double res = ElementUtils.getDisplayResistance(target, ElementType.NATURE);
                            DebugCommand.sendReactionFailed(target, "nature_parasite", reasonKey,
                                    attacker.getDisplayName(),
                                    target.getDisplayName(),
                                    String.format("%.0f", res),
                                    String.valueOf(ElementalFireNatureReactionsConfig.natureImmunityThreshold));
                        } else {
                            DebugCommand.sendReactionFailed(target, "nature_parasite", reasonKey,
                                    attacker.getDisplayName(),
                                    target.getDisplayName());
                        }
                    }
                } else {
                    DebugCommand.sendNatureParasiteChanceFailed(attacker, target, baseChance, scalingSteps, scalingChance, stackingBonus, attackerWetness, wetnessCfg, chance);
                }
            } else if (ElementalFireNatureReactionsConfig.natureParasiteBaseThreshold > 0
                    && naturePower < ElementalFireNatureReactionsConfig.natureParasiteBaseThreshold) {
                DebugCommand.sendReactionFailed(target, "nature_parasite", "threshold",
                        attacker.getDisplayName(),
                        target.getDisplayName(),
                        String.format("%.0f", naturePower),
                        String.format("%.0f", ElementalFireNatureReactionsConfig.natureParasiteBaseThreshold));
            }
        } else if (attackType == ElementType.FIRE) {
            if (ModMobEffects.SPORES.isPresent() && ModMobEffects.SPORES.get() != null && target.hasEffect(ModMobEffects.SPORES.get()) && !event.getSource().is(DamageTypeTags.IS_EXPLOSION) && !(ElementalThunderFrostReactionsConfig.frostbiteReduceSporesEnabled && (FrostbiteHandler.hasFrostbite(target) || target.getPersistentData().getBoolean("EC_FireFrostMeltResolved")))) {
                // 赤焰攻击不再直接触发毒火爆燃，改为通过灼烧→灼烧+孢子触发
            }

            double victimNaturePower = ElementUtils.getDisplayEnhancement(target, ElementType.NATURE);
            boolean isNatureTarget = ElementUtils.getConsistentAttackElement(target) == ElementType.NATURE;
            boolean hasScorched = ScorchedHandler.isScorched(target);
            boolean cooldownOk = checkCooldown(target, NBT_WILDFIRE_COOLDOWN);
            boolean powerOk = ElementalFireNatureReactionsConfig.wildfireTriggerThreshold > 0
                    && victimNaturePower >= ElementalFireNatureReactionsConfig.wildfireTriggerThreshold;

            if (isNatureTarget && powerOk && hasScorched && cooldownOk) {
                triggerWildfireEjection(target, attacker);
            } else if (isNatureTarget && !powerOk && hasScorched) {
                DebugCommand.sendReactionFailed(target, "wildfire", "power_low",
                        target.getDisplayName(),
                        String.format("%.0f", victimNaturePower),
                        String.valueOf(ElementalFireNatureReactionsConfig.wildfireTriggerThreshold));
            } else if (isNatureTarget && powerOk && hasScorched && !cooldownOk) {
                long remaining = DebugCommand.getRemainingCooldown(target, NBT_WILDFIRE_COOLDOWN);
                DebugCommand.sendReactionCooldownBlock(target, "wildfire", remaining);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!ElementalFireNatureReactionsConfig.sporeEnvironmentalBlastEnabled) return;
        LivingEntity target = event.getEntity();
        if (event.getSource().is(DamageTypeTags.IS_FIRE) && event.getSource().getEntity() == null) {
            if (ModMobEffects.SPORES.isPresent() && ModMobEffects.SPORES.get() != null
                    && target.hasEffect(ModMobEffects.SPORES.get())) {
                int envFirePower = (int) ElementalFireNatureReactionsConfig.scorchedTriggerThreshold;
                ScorchedHandler.applyScorched(target, target, envFirePower, 100, envFirePower, 1.0f, true);
            }
        }
    }

    public static boolean isSporeImmune(LivingEntity target) {
        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).toString();
        if (ElementalFireNatureReactionsConfig.cachedSporeBlacklist != null && ElementalFireNatureReactionsConfig.cachedSporeBlacklist.contains(entityId)) {
            return true;
        }
        double natureResistance = ElementUtils.getDisplayResistance(target, ElementType.NATURE);
        if (natureResistance >= ElementalFireNatureReactionsConfig.natureImmunityThreshold) {
            return true;
        }
        return false;
    }

    public static void stackSporeEffect(LivingEntity target, int layersToAdd) {
        stackSporeEffect(target, layersToAdd, null);
    }

    public static SporeApplyResult stackSporeEffect(LivingEntity target, int layersToAdd, LivingEntity applier) {
        if (!ModMobEffects.SPORES.isPresent() || ModMobEffects.SPORES.get() == null) return SporeApplyResult.NOT_REGISTERED;

        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).toString();
        if (ElementalFireNatureReactionsConfig.cachedSporeBlacklist != null && ElementalFireNatureReactionsConfig.cachedSporeBlacklist.contains(entityId)) {
            return SporeApplyResult.BLACKLISTED;
        }

        double natureResistance = ElementUtils.getDisplayResistance(target, ElementType.NATURE);
        if (natureResistance >= ElementalFireNatureReactionsConfig.natureImmunityThreshold) {
            return SporeApplyResult.IMMUNE;
        }

        if (ElementalThunderFrostReactionsConfig.freezeClearSporesEnabled && target.hasEffect(ModMobEffects.FREEZE.get())) {
            return SporeApplyResult.FROZEN;
        }

        MobEffectInstance currentEffect = target.getEffect(ModMobEffects.SPORES.get());
        int currentAmp = (currentEffect != null) ? currentEffect.getAmplifier() : -1;
        int currentStacks = currentAmp + 1;
        boolean isNewEffect = (currentEffect == null);
        int maxStacks = ElementalFireNatureReactionsConfig.sporeMaxStacks;

        if (currentStacks >= maxStacks) {
            return SporeApplyResult.MAX_STACKS;
        }

        int newStacks = Math.min(maxStacks, currentStacks + layersToAdd);
        int durationTicks = newStacks * ElementalFireNatureReactionsConfig.sporeDurationPerStack * 20;

        boolean isThunder = ElementUtils.getConsistentAttackElement(target) == ElementType.THUNDER;
        boolean isFire = ElementUtils.getConsistentAttackElement(target) == ElementType.FIRE;
        boolean isNature = ElementUtils.getConsistentAttackElement(target) == ElementType.NATURE;
        boolean isFrost = ElementUtils.getConsistentAttackElement(target) == ElementType.FROST;

        if (isThunder) {
            durationTicks = (int) (durationTicks * ElementalFireNatureReactionsConfig.sporeThunderMultiplier);
        }
        if (isFire) {
            durationTicks = (int) (durationTicks * ElementalFireNatureReactionsConfig.sporeFireDurationReduction);
        }
        if (isNature) {
            durationTicks = (int) (durationTicks * ElementalFireNatureReactionsConfig.sporeNatureDurationMultiplier);
        }
        if (isFrost) {
            durationTicks = (int) (durationTicks * ElementalFireNatureReactionsConfig.sporeFrostDurationMultiplier);
        }

        if (newStacks > 0) {
            target.addEffect(new MobEffectInstance(ModMobEffects.SPORES.get(), durationTicks, newStacks - 1));
            target.getPersistentData().putLong("EC_SporeApplyTick", target.level().getGameTime());
            if (isNewEffect && !target.level().isClientSide) {
                target.level().playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.SPORE_GAIN.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            if(target.hasEffect(ModMobEffects.STATIC_SHOCK.get())){
                StaticShockHandler.tryTriggerSporeBlast(target);
            }
            if (ScorchedHandler.isScorched(target)) {
                int sourceFirePower = ScorchedHandler.getScorchFireStrength(target);
                triggerToxicBlastFromScorched(target, newStacks, sourceFirePower, applier);
            }
        }
        return SporeApplyResult.SUCCESS;
    }

    private static void processContagion(LivingEntity source, int stacks) {
        CompoundTag data = source.getPersistentData();
        boolean isSpreaded = data.getBoolean(NBT_SPREADED);
        boolean isInfected = data.getBoolean(NBT_INFECTED);

        boolean blockByInfected = isInfected && !ElementalFireNatureReactionsConfig.contagionAllowInfectedSpread;
        if (isSpreaded || blockByInfected) {
            return;
        }

        double radius = ElementalFireNatureReactionsConfig.contagionBaseRadius + ((stacks - ElementalFireNatureReactionsConfig.sporeReactionThreshold) * ElementalFireNatureReactionsConfig.contagionRadiusPerStack);
        int transferStacks = Math.max(1, stacks - ElementalFireNatureReactionsConfig.contagionTransferBase);

        AABB sourceBox = source.getBoundingBox();
        if (sourceBox == null) return;
        AABB area = sourceBox.inflate(radius);
        List<LivingEntity> allNearby = source.level().getEntitiesOfClass(LivingEntity.class, area);
        List<LivingEntity> validTargets = new ArrayList<>();

        UUID contagionSourceUUID = null;
        if (data.contains(NBT_CONTAGION_SOURCE)) {
            try {
                contagionSourceUUID = data.getUUID(NBT_CONTAGION_SOURCE);
            } catch (Exception ignored) {}
        }

        for (LivingEntity target : allNearby) {
            if (target == source) continue;

            if (ModMobEffects.SPORES.isPresent() && target.hasEffect(ModMobEffects.SPORES.get())) {
                continue;
            }

            if (contagionSourceUUID != null && target.getUUID().equals(contagionSourceUUID)) {
                continue;
            }

            boolean onlyHostile = ElementalFireNatureReactionsConfig.contagionOnlyHostile;
            if (onlyHostile && !(target instanceof Enemy)) {
                continue;
            }

            if (target instanceof Player) {
                continue;
            }

            if (target instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) {
                continue;
            }
            if (target instanceof AbstractHorse horse && horse.getOwnerUUID() != null) {
                continue;
            }

            String entityId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).toString();
            if (ElementalFireNatureReactionsConfig.cachedSporeBlacklist != null
                    && ElementalFireNatureReactionsConfig.cachedSporeBlacklist.contains(entityId)) {
                continue;
            }
            double natureResistance = ElementUtils.getDisplayResistance(target, ElementType.NATURE);
            if (natureResistance >= ElementalFireNatureReactionsConfig.natureImmunityThreshold) {
                continue;
            }

            validTargets.add(target);
        }

        if (validTargets.isEmpty()) {
            return;
        }
        data.putBoolean(NBT_SPREADED, true);

        List<LivingEntity> infectedTargets = new ArrayList<>();
        for (LivingEntity target : validTargets) {
            CompoundTag targetData = target.getPersistentData();
            targetData.putUUID(NBT_CONTAGION_SOURCE, source.getUUID());
            targetData.putBoolean(NBT_INFECTED, true);

            WetnessHandler.convertWetnessToSpores(target);
            stackSporeEffect(target, transferStacks);

            infectedTargets.add(target);
        }

        EffectHelper.playSporeContagion(source, infectedTargets, radius);

        DebugCommand.ContagionLogContext cctx = new DebugCommand.ContagionLogContext();
        cctx.source = source;
        cctx.sourceStacks = stacks;
        cctx.transferStacks = transferStacks;
        cctx.radius = radius;
        cctx.affectedCount = validTargets.size();
        DebugCommand.sendContagionLog(cctx);
    }


    public static void triggerToxicBlast(Level level, LivingEntity attacker, LivingEntity target, double firePower) {
        triggerToxicBlast(level, attacker, target, firePower, attacker, 0);
    }

    public static void triggerToxicBlast(Level level, LivingEntity attacker, LivingEntity target, double firePower, LivingEntity killCredit) {
        triggerToxicBlast(level, attacker, target, firePower, killCredit, 0, new HashSet<>());
    }

    public static void triggerToxicBlast(Level level, LivingEntity attacker, LivingEntity target, double firePower, LivingEntity killCredit, int minStacks) {
        triggerToxicBlast(level, attacker, target, firePower, killCredit, minStacks, new HashSet<>());
    }

    private static void triggerToxicBlast(Level level, LivingEntity attacker, LivingEntity target, double firePower, LivingEntity killCredit, int minStacks, Set<LivingEntity> visited) {
        if (ElementalFireNatureReactionsConfig.sporeReactionThreshold <= 0) return;
        if (ModMobEffects.SPORES.get() == null) return;
        if (!visited.add(target)) return;
        MobEffectInstance sporeEffect = target.getEffect(ModMobEffects.SPORES.get());
        int amplifier = (sporeEffect != null) ? sporeEffect.getAmplifier() : -1;
        int stacks = amplifier + 1;

        target.removeEffect(ModMobEffects.SPORES.get());

        int effectiveStacks = Math.max(stacks, minStacks);
        if (effectiveStacks < ElementalFireNatureReactionsConfig.sporeReactionThreshold) {
            int scorchDuration = ElementalFireNatureReactionsConfig.scorchedDuration;
            float damageMultiplier = (float) ElementalFireNatureReactionsConfig.blastWeakIgniteMult;
            ScorchedHandler.clearScorched(target);
            if (ScorchedHandler.applyScorched(target, attacker, (int) firePower, scorchDuration, (int) firePower, damageMultiplier, true) != ScorchedHandler.ScorchedApplyResult.FAILED) {
                target.getPersistentData().putString(ScorchedHandler.NBT_SCORCHED_DAMAGE_MULT_SRC, "weak_blast");
                ScorchedHandler.igniteCreeperIfScorched(target);
            }
            EffectHelper.playSound(level, target, SoundEvents.FIRECHARGE_USE, 1.0f, 1.2f);
        } else {
            int extraStacks = effectiveStacks - ElementalFireNatureReactionsConfig.sporeReactionThreshold;

            float rawBaseDamage = (float) (ElementalFireNatureReactionsConfig.blastBaseDamage + (extraStacks * ElementalFireNatureReactionsConfig.blastGrowthDamage));
            int scorchDuration = (int) (ElementalFireNatureReactionsConfig.scorchedDuration + (extraStacks * ElementalFireNatureReactionsConfig.blastGrowthScorchTime * 20));

            double blastRadius = ElementalFireNatureReactionsConfig.blastBaseRange + (extraStacks * ElementalFireNatureReactionsConfig.blastGrowthRange);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.BLOCKS, 4.0F, (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F);

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, target.getX(), target.getY() + 1.0, target.getZ(), 1, 0, 0, 0, 0);
                EffectHelper.playToxicBlastSmokeFog(serverLevel, target.getX(), target.getY() + 0.5, target.getZ(), blastRadius);

                int primaryActualDuration = scorchDuration;
                float primaryActualDmgMult = 1.0f;
                if (target.hasEffect(MobEffects.POISON)) {
                    target.removeEffect(MobEffects.POISON);
                    primaryActualDuration = (int) (scorchDuration * ElementalFireNatureReactionsConfig.poisonScorchDurationMultiplier);
                    primaryActualDmgMult = (float) ElementalFireNatureReactionsConfig.poisonScorchDamageMultiplier;
                }
                if (ScorchedHandler.applyScorched(target, killCredit, (int) firePower, primaryActualDuration, (int) firePower, primaryActualDmgMult, true) != ScorchedHandler.ScorchedApplyResult.FAILED)
                    ScorchedHandler.igniteCreeperIfScorched(target);

                serverLevel.getServer().execute(() -> {
                    AABB targetBox = target.getBoundingBox();
                    if (targetBox == null) return;
                    AABB area = targetBox.inflate(blastRadius);
                    List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, area);
                    int affectedCount = 0;
                    List<LivingEntity> chainTargets = new ArrayList<>();
                    for (LivingEntity entity : nearbyEntities) {
                        if (entity == target) continue;
                        float mitigation = calculateBlastMitigation(entity);
                        float finalDamage = rawBaseDamage * (1.0f - mitigation);
                        ElementDamageHelper.applyDamage(entity, finalDamage, ModDamageTypes.source(entity.level(), ModDamageTypes.TOXIC_BLAST, killCredit));
                        int actualDuration = scorchDuration;
                        float actualDmgMult = 1.0f;
                        if (entity.hasEffect(MobEffects.POISON)) {
                            entity.removeEffect(MobEffects.POISON);
                            actualDuration = (int) (scorchDuration * ElementalFireNatureReactionsConfig.poisonScorchDurationMultiplier);
                            actualDmgMult = (float) ElementalFireNatureReactionsConfig.poisonScorchDamageMultiplier;
                        }
                        if (ScorchedHandler.applyScorched(entity, killCredit, (int) firePower, actualDuration, (int) firePower, actualDmgMult, true) != ScorchedHandler.ScorchedApplyResult.FAILED)
                            ScorchedHandler.igniteCreeperIfScorched(entity);
                        affectedCount++;
                        if (ModMobEffects.SPORES.isPresent() && ModMobEffects.SPORES.get() != null) {
                            MobEffectInstance entitySpore = entity.getEffect(ModMobEffects.SPORES.get());
                            if (entitySpore != null && (entitySpore.getAmplifier() + 1) >= ElementalFireNatureReactionsConfig.sporeReactionThreshold) {
                                chainTargets.add(entity);
                            }
                        }
                    }

                    float targetMitigation = calculateBlastMitigation(target);
                    DebugCommand.ToxicBlastLogContext blastCtx = new DebugCommand.ToxicBlastLogContext();
                    blastCtx.attacker = attacker;
                    blastCtx.target = target;
                    blastCtx.stacks = stacks;
                    blastCtx.radius = blastRadius;
                    blastCtx.affectedCount = affectedCount;
                    blastCtx.rawBaseDamage = rawBaseDamage;
                    blastCtx.mitigation = targetMitigation;
                    blastCtx.finalDamage = rawBaseDamage * (1.0f - targetMitigation);
                    blastCtx.blastProtLevel = getTotalEnchantmentLevel(Enchantments.BLAST_PROTECTION, target);
                    blastCtx.generalProtLevel = getTotalEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, target);
                    DebugCommand.sendToxicBlastLog(blastCtx);

                    if (!chainTargets.isEmpty()) {
                        net.minecraft.server.MinecraftServer server = serverLevel.getServer();
                        int scheduleTick = server.getTickCount() + 20;
                        for (LivingEntity chainTarget : chainTargets) {
                            server.tell(new net.minecraft.server.TickTask(scheduleTick, () -> {
                                triggerToxicBlast(level, killCredit, chainTarget,
                                        ElementalFireNatureReactionsConfig.scorchedTriggerThreshold, killCredit, 0, visited);
                            }));
                        }
                    }
                });
            }
        }
    }

    private static float calculateBlastMitigation(LivingEntity entity) {
        int blastProtLevel = getTotalEnchantmentLevel(Enchantments.BLAST_PROTECTION, entity);
        int generalProtLevel = getTotalEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, entity);
        double maxBlastCap = ElementalFireNatureReactionsConfig.blastMaxBlastProtCap;
        double maxGeneralCap = ElementalFireNatureReactionsConfig.blastMaxGeneralProtCap;
        double blastFactor = maxBlastCap / ElementalFireNatureReactionsConfig.enchantmentCalculationDenominator;
        double generalFactor = maxGeneralCap / ElementalFireNatureReactionsConfig.enchantmentCalculationDenominator;
        double calculatedBlastRed = blastProtLevel * blastFactor;
        double calculatedGeneralRed = generalProtLevel * generalFactor;
        double actualBlastRed = Math.min(calculatedBlastRed, maxBlastCap);
        double actualGeneralRed = Math.min(calculatedGeneralRed, maxGeneralCap);
        float mitigation = (float) Math.min(actualBlastRed + actualGeneralRed, 1.0);
        return mitigation;
    }

    private static void triggerWildfireEjection(LivingEntity victim, Entity attacker) {
        double radius = ElementalFireNatureReactionsConfig.wildfireRadius;
        EffectHelper.playWildfireEjection(victim, radius);

        if (ElementalFireNatureReactionsConfig.wildfireClearBurning) {
            ScorchedHandler.clearScorched(victim);
        }

        AABB victimBox = victim.getBoundingBox();
        if (victimBox == null) return;
        AABB area = victimBox.inflate(radius);
        List<LivingEntity> enemies = victim.level().getEntitiesOfClass(LivingEntity.class, area);
        int affectedCount = 0;
        for (LivingEntity enemy : enemies) {
            boolean isHostile = (enemy == attacker) || (enemy instanceof Enemy);
            if (enemy == victim || !isHostile) continue;
            Vec3 enemyPos = enemy.position();
            Vec3 victimPos = victim.position();
            if (enemyPos == null || victimPos == null) continue;
            Vec3 delta = enemyPos.subtract(victimPos);
            if (delta.lengthSqr() < 1e-7) {
                delta = new Vec3(RANDOM.nextDouble() - 0.5, 0, RANDOM.nextDouble() - 0.5).normalize();
            } else {
                delta = delta.normalize();
            }
            Vec3 vec = delta.scale(ElementalFireNatureReactionsConfig.wildfireKnockback);
            enemy.push(vec.x, ElementalFireNatureReactionsConfig.wildfireVerticalKnockback, vec.z);
            enemy.hurtMarked = true;
            stackSporeEffect(enemy, ElementalFireNatureReactionsConfig.wildfireSporeAmount);
            affectedCount++;
        }

        DebugCommand.WildfireLogContext wildfireCtx = new DebugCommand.WildfireLogContext();
        wildfireCtx.victim = victim;
        wildfireCtx.radius = radius;
        wildfireCtx.affectedCount = affectedCount;
        DebugCommand.sendWildfireLog(wildfireCtx);

        setCooldown(victim, NBT_WILDFIRE_COOLDOWN, ElementalFireNatureReactionsConfig.wildfireCooldown);
    }

    private static boolean checkCooldown(LivingEntity entity, String key) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(key)) return true;
        long endTick = data.getLong(key);
        boolean ready = entity.level().getGameTime() >= endTick;
        return ready;
    }

    private static void setCooldown(LivingEntity entity, String key, int durationTicks) {
        entity.getPersistentData().putLong(key, entity.level().getGameTime() + durationTicks);
    }

    private static int getTotalEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantment ench, LivingEntity entity) {
        int total = 0;
        for (ItemStack stack : entity.getArmorSlots()) {
            total += stack.getEnchantmentLevel(ench);
        }
        return total;
    }

    public static void triggerToxicBlastFromScorched(LivingEntity target, int stacks, int sourceFirePower, LivingEntity killCredit) {
        if (ElementalFireNatureReactionsConfig.sporeReactionThreshold <= 0) return;
        if (target.level().isClientSide) return;
        Level level = target.level();
        if (killCredit == null) {
            killCredit = target;
        }
        triggerToxicBlast(level, killCredit, target, sourceFirePower, killCredit, 0);

        if (stacks >= ElementalFireNatureReactionsConfig.sporeReactionThreshold) {
            ScorchedHandler.clearScorched(target);

            int scorchDuration = (int) (ElementalFireNatureReactionsConfig.scorchedDuration
                    + ((stacks - ElementalFireNatureReactionsConfig.sporeReactionThreshold) * ElementalFireNatureReactionsConfig.blastGrowthScorchTime * 20));
            if (ScorchedHandler.applyScorched(target, killCredit, (int) sourceFirePower, scorchDuration, (int) sourceFirePower, 1.0f, true) != ScorchedHandler.ScorchedApplyResult.FAILED)
                ScorchedHandler.igniteCreeperIfScorched(target);
        }

        DebugCommand.ScorchedSporeReactionLogContext scorchedCtx = new DebugCommand.ScorchedSporeReactionLogContext();
        scorchedCtx.target = target;
        scorchedCtx.applier = killCredit;
        scorchedCtx.stacks = stacks;
        DebugCommand.sendScorchedSporeReactionLog(scorchedCtx);
    }

    public static void triggerStaticSporeBlast(LivingEntity target, double firePower) {
    triggerToxicBlast(target.level(), target, target, firePower, target);
    }
}
