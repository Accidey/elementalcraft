package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.util.DebugMode;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.sound.ModSounds;
import com.xulai.elementalcraft.util.EffectHelper;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import com.xulai.elementalcraft.event.ScorchedHandler;
import com.xulai.elementalcraft.event.WetnessHandler;
import com.xulai.elementalcraft.util.ElementDamageHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

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
    private static final String NBT_SPORE_APPLY_TICK = "EC_SporeApplyTick";

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

        handleReinfection(entity);

        if (entity.tickCount % 5 == 0) {
            handleParalysisSporeBlast(entity);
            handleScorchedSporeBlast(entity);
        }

        if (entity.tickCount % 20 == 0) {
            handleCounterRecovery(entity);
            handleEnvironmentalBlast(entity);
        }

        if (entity.tickCount % ElementalFireNatureReactionsConfig.CONTAGION_CHECK_INTERVAL != 0) return;
        handleContagionSpread(entity);
    }

    private static void handleReinfection(LivingEntity entity) {
        if (!ElementalFireNatureReactionsConfig.contagionAllowReinfected) return;
        CompoundTag data = entity.getPersistentData();
        boolean hasSpores = ModMobEffects.SPORES.isPresent()
                && entity.hasEffect(ModMobEffects.SPORES.get());
        if (hasSpores) return;
        if (data.getBoolean(NBT_INFECTED)) {
            data.putBoolean(NBT_INFECTED, false);
        }
        if (data.getBoolean(NBT_SPREADED)) {
            data.putBoolean(NBT_SPREADED, false);
        }
        if (data.contains(NBT_CONTAGION_SOURCE)) {
            data.remove(NBT_CONTAGION_SOURCE);
        }
    }

    private static void handleCounterRecovery(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        clearHealthRecoveryIfHealed(entity, data, NBT_WILDFIRE_COOLDOWN, ElementalFireNatureReactionsConfig.wildfireHealthRecoveryThreshold);
        clearHealthRecoveryIfHealed(entity, data, ScorchedHandler.NBT_FIRE_COUNTER_CD, ElementalFireNatureReactionsConfig.fireCounterHealthRecoveryThreshold);
        clearHealthRecoveryIfHealed(entity, data, StaticShockHandler.NBT_THUNDER_COUNTER_COOLDOWN, ElementalThunderFrostReactionsConfig.thunderCounterHealthRecoveryThreshold);
        clearHealthRecoveryIfHealed(entity, data, FrostbiteHandler.NBT_FROST_COUNTER_COOLDOWN, ElementalThunderFrostReactionsConfig.frostCounterHealthRecoveryThreshold);
    }

    private static void clearHealthRecoveryIfHealed(LivingEntity entity, CompoundTag data, String key, double recoveryRatio) {
        if (!data.contains(key)) return;
        if (entity.getHealth() >= entity.getMaxHealth() * recoveryRatio) {
            data.remove(key);
        }
    }

    private static void handleParalysisSporeBlast(LivingEntity entity) {
        if (!entity.hasEffect(ModMobEffects.PARALYSIS.get())) return;
        if (!ModMobEffects.SPORES.isPresent()
                || !entity.hasEffect(ModMobEffects.SPORES.get())) return;
        double firePower = ElementalFireNatureReactionsConfig.scorchedTriggerThreshold;
        triggerStaticSporeBlast(entity, firePower);
    }

    private static void handleScorchedSporeBlast(LivingEntity entity) {
        if (!ModMobEffects.SPORES.isPresent()
                || !entity.hasEffect(ModMobEffects.SPORES.get())
                || !ScorchedHandler.isScorched(entity)) return;
        MobEffectInstance sporeEffect = entity.getEffect(ModMobEffects.SPORES.get());
        if (sporeEffect == null) return;
        int stacks = sporeEffect.getAmplifier() + 1;
        int sourceFirePower = ScorchedHandler.getScorchFireStrength(entity);
        triggerToxicBlastFromScorched(entity, stacks, sourceFirePower, entity);
    }

    private static void handleEnvironmentalBlast(LivingEntity entity) {
        if (!ElementalFireNatureReactionsConfig.sporeEnvironmentalBlastEnabled
                || !ModMobEffects.SPORES.isPresent()
                || !entity.hasEffect(ModMobEffects.SPORES.get())) return;
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

    private static void handleContagionSpread(LivingEntity entity) {
        if (!ModMobEffects.SPORES.isPresent() || !entity.hasEffect(ModMobEffects.SPORES.get())) return;
        MobEffectInstance sporeEffect = entity.getEffect(ModMobEffects.SPORES.get());
        if (sporeEffect == null) return;
        int amplifier = sporeEffect.getAmplifier();
        int stacks = amplifier + 1;
        if (ElementalFireNatureReactionsConfig.sporeReactionThreshold > 0
                && stacks >= ElementalFireNatureReactionsConfig.sporeReactionThreshold) {
            processContagion(entity, stacks);
        }
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
                        ? (int) (naturePower / scalingStep) : 0;
                double chance = Math.min(1.0, baseChance + scalingSteps * scalingChance);

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
                        DebugCommand.sendNatureParasiteSuccess(attacker, target, ElementalFireNatureReactionsConfig.natureParasiteAmount, ElementType.NATURE, naturePower, baseChance, scalingSteps, scalingChance, stackingBonus, attackerWetness, wetnessCfg, chance);
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
                    DebugCommand.sendNatureParasiteChanceFailed(attacker, target, ElementType.NATURE, naturePower, baseChance, scalingSteps, scalingChance, stackingBonus, attackerWetness, wetnessCfg, chance);
                }
            } else if (ElementalFireNatureReactionsConfig.natureParasiteBaseThreshold > 0
                    && naturePower < ElementalFireNatureReactionsConfig.natureParasiteBaseThreshold) {
                DebugCommand.sendReactionFailed(target, "nature_parasite", "threshold",
                        attacker.getDisplayName(),
                        target.getDisplayName(),
                        String.format("%.0f", naturePower),
                        String.format("%.0f", ElementalFireNatureReactionsConfig.natureParasiteBaseThreshold));
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!ElementalFireNatureReactionsConfig.sporeEnvironmentalBlastEnabled) return;
        LivingEntity target = event.getEntity();
        if (event.getSource().is(DamageTypeTags.IS_FIRE) && event.getSource().getEntity() == null) {
            if (ModMobEffects.SPORES.isPresent()
                    && target.hasEffect(ModMobEffects.SPORES.get())) {
                int envFirePower = (int) ElementalFireNatureReactionsConfig.scorchedTriggerThreshold;
                ScorchedHandler.applyScorched(target, target, envFirePower, 100, envFirePower, 1.0f, true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTryNatureCounter(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        LivingEntity target = event.getEntity();
        double bloodThreshold = ElementalFireNatureReactionsConfig.wildfireBloodThreshold;
        if (bloodThreshold <= 0) return;
        float currentHP = target.getHealth() + target.getAbsorptionAmount();
        if (currentHP - event.getAmount() >= target.getMaxHealth() * bloodThreshold) return;
        if (ElementUtils.getConsistentAttackElement(target) != ElementType.NATURE) return;
        double naturePower = ElementUtils.getDisplayEnhancement(target, ElementType.NATURE);
        int threshold = (int) ElementalFireNatureReactionsConfig.wildfireTriggerThreshold;
        if (threshold <= 0 || naturePower < threshold) {
            if (threshold > 0) {
                DebugCommand.sendReactionFailed(target, "wildfire", "power_low",
                        target.getDisplayName(),
                        String.format("%.0f", naturePower),
                        String.valueOf(threshold));
            }
            return;
        }
        if (!checkHealthRecovery(target, NBT_WILDFIRE_COOLDOWN)) return;
        Entity attacker = event.getSource().getEntity();
        triggerWildfireEjection(target, attacker);
    }

    public static boolean isSporeImmune(LivingEntity target) {
        var key = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (key == null) return true;
        String entityId = key.toString();
        if (ElementalConfig.matchesBlacklist(ElementalFireNatureReactionsConfig.cachedSporeBlacklist, entityId)) {
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
        if (!ModMobEffects.SPORES.isPresent()) return SporeApplyResult.NOT_REGISTERED;

        target.getPersistentData().remove("EC_SporeDamageLogged");

        var key = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (key == null) return SporeApplyResult.BLACKLISTED;
        String entityId = key.toString();
        if (ElementalConfig.matchesBlacklist(ElementalFireNatureReactionsConfig.cachedSporeBlacklist, entityId)) {
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

        ElementType consistentElement = ElementUtils.getConsistentAttackElement(target);
        boolean isThunder = consistentElement == ElementType.THUNDER;
        boolean isFire = consistentElement == ElementType.FIRE;
        boolean isNature = consistentElement == ElementType.NATURE;
        boolean isFrost = consistentElement == ElementType.FROST;

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
        double coldMult = ElementalFireNatureReactionsConfig.sporeColdBiomeDurationMultiplier;
        if (coldMult < 1.0) {
            Level level = target.level();
            BlockPos pos = target.blockPosition();
            Biome biome = level.getBiome(pos).value();
            if (biome != null && biome.getBaseTemperature() <= 0.3) {
                durationTicks = (int) (durationTicks * coldMult);
            }
        }

        if (newStacks > 0) {
            target.addEffect(new MobEffectInstance(ModMobEffects.SPORES.get(), durationTicks, newStacks - 1, false, false, true));
            target.getPersistentData().putLong(NBT_SPORE_APPLY_TICK, target.level().getGameTime());
            if (isNewEffect && !target.level().isClientSide) {
                target.level().playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.SPORE_GAIN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            if (DebugMode.hasAnyDebugEnabled()) {
                String elementSuffix = "";
                if (isFire) elementSuffix = "(" + Component.translatable("element.fire.bracket").getString() + " ×" + String.format("%.1f", ElementalFireNatureReactionsConfig.sporeFireDurationReduction) + ")";
                else if (isThunder) elementSuffix = "(" + Component.translatable("element.thunder.bracket").getString() + " ×" + String.format("%.1f", ElementalFireNatureReactionsConfig.sporeThunderMultiplier) + ")";
                else if (isNature) elementSuffix = "(" + Component.translatable("element.nature.bracket").getString() + " ×" + String.format("%.1f", ElementalFireNatureReactionsConfig.sporeNatureDurationMultiplier) + ")";
                else if (isFrost) elementSuffix = "(" + Component.translatable("element.frost.bracket").getString() + " ×" + String.format("%.1f", ElementalFireNatureReactionsConfig.sporeFrostDurationMultiplier) + ")";
                DebugCommand.sendReactionSuccess(target, "spore_stacks",
                        target.getDisplayName(),
                        currentStacks, newStacks,
                        elementSuffix,
                        durationTicks / 20);
            }
            if (target.hasEffect(ModMobEffects.STATIC_SHOCK.get())) {
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
            if (onlyHostile) {
                if (!(target instanceof Enemy)) continue;
                if (target instanceof Player) continue;
                if (target instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) continue;
                if (target instanceof AbstractHorse horse && horse.getOwnerUUID() != null) continue;
            }

            var entityKey = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
            if (entityKey == null) continue;
            String entityId = entityKey.toString();
            if (ElementalConfig.matchesBlacklist(ElementalFireNatureReactionsConfig.cachedSporeBlacklist, entityId)) {
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
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F, (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F);

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
                    AABB area = targetBox.inflate(blastRadius);
                    List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, area);
                    int affectedCount = 0;
                    List<LivingEntity> chainTargets = new ArrayList<>();
                    for (LivingEntity entity : nearbyEntities) {
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
                        if (FrostbiteHandler.isFrozen(entity) && !entity.getPersistentData().getBoolean(FrostbiteHandler.NBT_FROST_BURST_FROZEN)) {
                            entity.removeEffect(ModMobEffects.FREEZE.get());
                            entity.getPersistentData().remove(FrostbiteHandler.NBT_FREEZE_STACKS);
                            entity.getPersistentData().remove(FrostbiteHandler.NBT_FREEZE_COOLDOWN);
                            entity.getPersistentData().remove(FrostbiteHandler.NBT_FROZEN_FROSTBITE_STACKS);
                            entity.getPersistentData().remove(FrostbiteHandler.NBT_FROST_BURST_FROZEN);
                        }
                        if (FrostbiteHandler.hasFrostbite(entity)) {
                            FrostbiteHandler.clearFrostbite(entity);
                        } else if (FrostbiteHandler.isTempFrostbite(entity)) {
                            FrostbiteHandler.clearTempFrostbite(entity);
                        }
                        double dx = entity.getX() - target.getX();
                        double dz = entity.getZ() - target.getZ();
                        double hDist = Math.sqrt(dx * dx + dz * dz);
                        if (hDist <= 0.01) { dx = 0; dz = 0; } else { dx /= hDist; dz /= hDist; }
                        double scale = blastRadius * (1.0 - entity.distanceTo(target) / blastRadius) * 0.3;
                        entity.setDeltaMovement(entity.getDeltaMovement().add(dx * scale, 0.1 + scale * 0.5, dz * scale));
                        entity.hurtMarked = true;
                        affectedCount++;
                        if (ModMobEffects.SPORES.isPresent()) {
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
                        MinecraftServer server = serverLevel.getServer();
                        int scheduleTick = server.getTickCount() + 20;
                        for (LivingEntity chainTarget : chainTargets) {
                            server.tell(new TickTask(scheduleTick, () -> {
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
        double blastFactor = maxBlastCap / Math.max(1.0, ElementalFireNatureReactionsConfig.enchantmentCalculationDenominator);
        double generalFactor = maxGeneralCap / Math.max(1.0, ElementalFireNatureReactionsConfig.enchantmentCalculationDenominator);
        double calculatedBlastRed = blastProtLevel * blastFactor;
        double calculatedGeneralRed = generalProtLevel * generalFactor;
        double actualBlastRed = Math.min(calculatedBlastRed, maxBlastCap);
        double actualGeneralRed = Math.min(calculatedGeneralRed, maxGeneralCap);
        float mitigation = (float) Math.min(actualBlastRed + actualGeneralRed, 1.0);
        return mitigation;
    }

    private static boolean wildfireRunning = false;

    public static void triggerWildfireEjection(LivingEntity victim, Entity attacker) {
        if (wildfireRunning) return;
        wildfireRunning = true;
        try {
        double radius = ElementalFireNatureReactionsConfig.wildfireRadius;
        EffectHelper.playWildfireEjection(victim, radius);

        if (ElementalFireNatureReactionsConfig.wildfireClearBurning) {
            ScorchedHandler.clearScorched(victim);
        }

        if (!(victim.level() instanceof ServerLevel sl)) return;

        AABB victimBox = victim.getBoundingBox();
        AABB area = victimBox.inflate(radius);
        List<LivingEntity> enemies = victim.level().getEntitiesOfClass(LivingEntity.class, area);
        int affectedCount = 0;
        for (LivingEntity enemy : enemies) {
            if (enemy == victim) continue;
            Vec3 enemyPos = enemy.position();
            Vec3 victimPos = victim.position();
            Vec3 delta = enemyPos.subtract(victimPos);
            if (delta.lengthSqr() < 1e-7) {
                delta = new Vec3(RANDOM.nextDouble() - 0.5, 0, RANDOM.nextDouble() - 0.5).normalize();
            } else {
                delta = delta.normalize();
            }
            Vec3 vec = delta.scale(ElementalFireNatureReactionsConfig.wildfireKnockback);
            enemy.push(vec.x, ElementalFireNatureReactionsConfig.wildfireVerticalKnockback, vec.z);
            enemy.hurtMarked = true;

            sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                    enemy.getX(), enemy.getY() + enemy.getBbHeight() * 0.5, enemy.getZ(),
                    10, 0.5, 0.5, 0.5, 0.08);
            sl.sendParticles(ParticleTypes.COMPOSTER,
                    enemy.getX(), enemy.getY() + 0.1, enemy.getZ(),
                    4, 0.3, 0.1, 0.3, 0.02);

            stackSporeEffect(enemy, ElementalFireNatureReactionsConfig.wildfireSporeAmount);
            affectedCount++;
        }

        DebugCommand.WildfireLogContext wildfireCtx = new DebugCommand.WildfireLogContext();
        wildfireCtx.victim = victim;
        wildfireCtx.radius = radius;
        wildfireCtx.affectedCount = affectedCount;
        DebugCommand.sendWildfireLog(wildfireCtx);

        setHealthRecoveryThreshold(victim, NBT_WILDFIRE_COOLDOWN, victim.getMaxHealth(), ElementalFireNatureReactionsConfig.wildfireHealthRecoveryThreshold);
        } finally {
            wildfireRunning = false;
        }
    }

    public static boolean checkHealthRecovery(LivingEntity entity, String key) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(key)) return true;
        double thresholdHealth = data.getDouble(key);
        if (entity.getHealth() >= thresholdHealth) {
            data.remove(key);
            return true;
        }
        return false;
    }

    public static void setHealthRecoveryThreshold(LivingEntity entity, String key, double maxHealth, double recoveryRatio) {
        entity.getPersistentData().putDouble(key, maxHealth * recoveryRatio);
    }

    private static int getTotalEnchantmentLevel(Enchantment ench, LivingEntity entity) {
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

    public static double applySporeBiomeModifier(LivingEntity target, double chance) {
        double result = Math.min(1.0, chance);
        double coldMult = ElementalFireNatureReactionsConfig.sporeColdBiomeChanceMultiplier;
        boolean modified = false;
        CompoundTag data = target.getPersistentData();
        if (coldMult < 1.0 && !data.getBoolean("EC_SporeBiomeColdLogged")) {
            Biome biome = target.level().getBiome(target.blockPosition()).value();
            if (biome.getBaseTemperature() <= 0.3) {
                result *= coldMult;
                data.putBoolean("EC_SporeBiomeColdLogged", true);
                modified = true;
            }
        }
        result = Math.min(1.0, result);
        if (modified && DebugMode.hasAnyDebugEnabled()) {
            MutableComponent msg = Component.translatable("debug.elementalcraft.reaction.static_spore_blast.biome",
                    Component.literal(String.format("%.0f", Math.min(1.0, chance) * 100)).withStyle(ChatFormatting.GRAY),
                    Component.literal(String.format("%.0f", result * 100)).withStyle(ChatFormatting.GOLD));
            DebugCommand.sendDebugMessage(target, msg);
        }
        return result;
    }

    public static void triggerStaticSporeBlast(LivingEntity target, double firePower) {
        triggerToxicBlast(target.level(), target, target, firePower, target);
    }

    private static final String NBT_SPORE_HEAT_BLAST_CD = "EC_SporeHeatBlastCooldown";

    public static void checkHeatSporeBlast(LivingEntity entity) {
        if (entity.level().isClientSide) return;
        if (!ElementalFireNatureReactionsConfig.sporeHeatBlastEnabled) return;
        if (ElementalFireNatureReactionsConfig.sporeReactionThreshold <= 0) return;
        if (!ModMobEffects.SPORES.isPresent()) return;
        MobEffectInstance sporeEffect = entity.getEffect(ModMobEffects.SPORES.get());
        if (sporeEffect == null) return;
        int stacks = sporeEffect.getAmplifier() + 1;
        if (stacks < ElementalFireNatureReactionsConfig.sporeReactionThreshold) return;

        CompoundTag data = entity.getPersistentData();
        long cd = data.getLong(NBT_SPORE_HEAT_BLAST_CD);
        if (entity.level().getGameTime() < cd) return;

        BlockPos pos = entity.blockPosition();
        double radius = ElementalFireNatureReactionsConfig.wetnessHeatAccelerateRadius;
        if (radius <= 0) return;
        int range = (int) Math.ceil(radius);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        boolean found = false;
        outer:
        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    cursor.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    BlockState state = entity.level().getBlockState(cursor);
                    if (state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)) {
                        if (state.getValue(CampfireBlock.LIT)) {
                            found = true;
                            break outer;
                        }
                    }
                    if (state.getBlock() instanceof AbstractFurnaceBlock) {
                        if (state.getValue(AbstractFurnaceBlock.LIT)) {
                            found = true;
                            break outer;
                        }
                    }
                }
            }
        }
        if (!found) return;

        data.putLong(NBT_SPORE_HEAT_BLAST_CD, entity.level().getGameTime() + 40);
        double firePower = ElementUtils.getDisplayEnhancement(entity, ElementType.FIRE);
        triggerToxicBlast(entity.level(), entity, entity, firePower);
    }
}
