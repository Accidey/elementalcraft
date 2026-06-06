package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import com.xulai.elementalcraft.util.ElementDamageHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class ScorchedHandler {
    public static final String NBT_SCORCHED_TICKS = "ec_scorched_ticks";
    public static final String NBT_SCORCHED_STRENGTH = "ec_scorched_str";
    public static final String NBT_SCORCHED_SOURCE_FIRE_POWER = "EC_ScorchedSourceFirePower";
    public static final String NBT_ATTACKER_SCORCHED_COOLDOWN = "ec_scorched_attacker_cd";
    public static final String NBT_SCORCHED_DAMAGE_MULT = "ec_scorched_dmg_mult";
    public static final String NBT_SCORCHED_ATTACKER = "ec_scorched_attacker";
    public static final String NBT_SCORCHED_TICK_LOGGED = "ec_scorched_tick_logged";

    public static class ScorchedApplyResult {
        public static final ScorchedApplyResult FAILED = new ScorchedApplyResult(0, ElementType.NONE, 1.0f);
        public final int adjustedDuration;
        public final ElementType targetElement;
        public final float multiplier;

        public ScorchedApplyResult(int adjustedDuration, ElementType targetElement, float multiplier) {
            this.adjustedDuration = adjustedDuration;
            this.targetElement = targetElement;
            this.multiplier = multiplier;
        }
    }

    public static ScorchedApplyResult applyScorched(LivingEntity target, LivingEntity attacker, int fireStrength, int duration, int sourceFirePower, float damageMultiplier, boolean bypassCooldown) {
        if (ElementalFireNatureReactionsConfig.scorchedTriggerThreshold <= 0) return ScorchedApplyResult.FAILED;
        if (target.level().isClientSide) return ScorchedApplyResult.FAILED;
        if (target instanceof Player player && player.isCreative()) return ScorchedApplyResult.FAILED;
        var key = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (key == null) return ScorchedApplyResult.FAILED;
        String entityId = key.toString();
        var blacklist = ElementalFireNatureReactionsConfig.cachedScorchedBlacklist;
        if (blacklist != null && blacklist.contains(entityId)) {
            return ScorchedApplyResult.FAILED;
        }
        CompoundTag targetData = target.getPersistentData();
        if (targetData.contains(NBT_SCORCHED_TICKS) && targetData.getInt(NBT_SCORCHED_TICKS) > 0) {
            return ScorchedApplyResult.FAILED;
        }

        int adjustedDuration = duration;
        float multiplier = 1.0f;

        ElementType targetElement = ElementUtils.getConsistentAttackElement(target);
        if (targetElement == ElementType.NATURE) {
            multiplier = (float) ElementalFireNatureReactionsConfig.scorchedNatureDurationMultiplier;
            adjustedDuration = (int) Math.round(duration * multiplier);
        } else if (targetElement == ElementType.FROST) {
            multiplier = (float) ElementalFireNatureReactionsConfig.scorchedFrostDurationMultiplier;
            adjustedDuration = (int) Math.round(duration * multiplier);
        } else if (targetElement == ElementType.FIRE) {
            multiplier = (float) ElementalFireNatureReactionsConfig.scorchedFireDurationMultiplier;
            adjustedDuration = (int) Math.round(duration * multiplier);
        } else if (targetElement == ElementType.THUNDER) {
            multiplier = (float) ElementalFireNatureReactionsConfig.scorchedThunderDurationMultiplier;
            adjustedDuration = (int) Math.round(duration * multiplier);
        }

        if (adjustedDuration < 1) adjustedDuration = 1;
        long gameTime = target.level().getGameTime();
        targetData.putInt(NBT_SCORCHED_TICKS, adjustedDuration);
        targetData.putInt(NBT_SCORCHED_STRENGTH, fireStrength);
        targetData.putInt(NBT_SCORCHED_SOURCE_FIRE_POWER, sourceFirePower);
        targetData.putFloat(NBT_SCORCHED_DAMAGE_MULT, damageMultiplier);
        targetData.putInt(NBT_SCORCHED_TICK_LOGGED, 0);
        if (attacker != null) {
            targetData.putUUID(NBT_SCORCHED_ATTACKER, attacker.getUUID());
        }

        if (attacker != null && !bypassCooldown) {
            CompoundTag attackerData = attacker.getPersistentData();
            attackerData.putLong(NBT_ATTACKER_SCORCHED_COOLDOWN, gameTime + adjustedDuration + ElementalFireNatureReactionsConfig.scorchedCooldown);
        }

        target.setRemainingFireTicks(adjustedDuration);

        if (target.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LAVA, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 20, 0.2, 0.2, 0.2, 0.0);
        }
        return new ScorchedApplyResult(adjustedDuration, targetElement, multiplier);
    }

    public static int applyScorched(LivingEntity target, LivingEntity attacker, int fireStrength, int duration, int sourceFirePower, float damageMultiplier) {
        return applyScorched(target, attacker, fireStrength, duration, sourceFirePower, damageMultiplier, false).adjustedDuration;
    }

    public static int applyScorched(LivingEntity target, LivingEntity attacker, int fireStrength, int duration, int sourceFirePower) {
        return applyScorched(target, attacker, fireStrength, duration, sourceFirePower, 1.0f, false).adjustedDuration;
    }

    public static boolean isScorched(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        return data.contains(NBT_SCORCHED_TICKS) && data.getInt(NBT_SCORCHED_TICKS) > 0;
    }

    public static UUID getScorchedAttackerUUID(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (data.hasUUID(NBT_SCORCHED_ATTACKER)) {
            return data.getUUID(NBT_SCORCHED_ATTACKER);
        }
        return null;
    }

    public static void clearScorched(LivingEntity entity) {
        entity.clearFire();
        CompoundTag data = entity.getPersistentData();
        data.remove(NBT_SCORCHED_TICKS);
        data.remove(NBT_SCORCHED_STRENGTH);
        data.remove(NBT_SCORCHED_SOURCE_FIRE_POWER);
        data.remove(NBT_SCORCHED_DAMAGE_MULT);
        data.remove(NBT_SCORCHED_ATTACKER);
        data.remove(NBT_SCORCHED_TICK_LOGGED);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        if (entity instanceof Player player && player.isCreative()) return;

        CompoundTag data = entity.getPersistentData();
        if (data.contains(NBT_ATTACKER_SCORCHED_COOLDOWN)) {
            long cd = data.getLong(NBT_ATTACKER_SCORCHED_COOLDOWN);
            if (entity.level().getGameTime() >= cd) {
                data.remove(NBT_ATTACKER_SCORCHED_COOLDOWN);
            }
        }

        if (!data.contains(NBT_SCORCHED_TICKS)) return;
        int ticks = data.getInt(NBT_SCORCHED_TICKS);

        if (ticks <= 0) {
            clearScorched(entity);
            return;
        }

        int resistPoints = ElementUtils.getDisplayResistance(entity, ElementType.FIRE);
        if (resistPoints >= ElementalFireNatureReactionsConfig.scorchedResistThreshold) {
            clearScorched(entity);
            return;
        }

        if (FrostbiteHandler.isFrozen(entity) && ElementalThunderFrostReactionsConfig.frostScorchSteamReactionEnabled
                && ElementalFireNatureReactionsConfig.steamLowHeatMaxLevel > 0) {
            int sourceFirePower = data.getInt(NBT_SCORCHED_SOURCE_FIRE_POWER);
            int frozenStacks = data.getInt(FrostbiteHandler.NBT_FREEZE_STACKS);
            if (frozenStacks <= 0) frozenStacks = 1;
            int fireStep = 20;
            int level = Math.max(1, Math.min(sourceFirePower / fireStep + frozenStacks, ElementalFireNatureReactionsConfig.steamLowHeatMaxLevel));

            entity.removeEffect(ModMobEffects.FREEZE.get());
            data.remove(FrostbiteHandler.NBT_FREEZE_STACKS);
            data.remove(FrostbiteHandler.NBT_FROZEN_FROSTBITE_STACKS);
            data.putLong(FrostbiteHandler.NBT_FREEZE_COOLDOWN,
                    entity.level().getGameTime() + ElementalThunderFrostReactionsConfig.freezeCooldownTicks);
            FrostbiteHandler.clearFrostbite(entity);
            clearScorched(entity);

            if (!SteamReactionHandler.isOnSteamCooldown(entity)) {
                SteamReactionHandler.spawnSteamCloud(entity, false, level);
                SteamReactionHandler.applySteamCooldown(entity, SteamReactionHandler.computeCloudDuration(false, level));
            }
            DebugCommand.sendReactionSuccess(entity, "scorch_frozen_steam",
                    entity.getDisplayName(),
                    Component.literal(String.valueOf(level)));
            return;
        }

        if (entity.hasEffect(net.minecraft.world.effect.MobEffects.POISON)) {
            int enhancedDuration = (int) (ElementalFireNatureReactionsConfig.scorchedDuration
                    * ElementalFireNatureReactionsConfig.poisonScorchDurationMultiplier);
            ticks = enhancedDuration;
            data.putInt(NBT_SCORCHED_TICKS, ticks);
            data.putFloat(NBT_SCORCHED_DAMAGE_MULT, (float) ElementalFireNatureReactionsConfig.poisonScorchDamageMultiplier);
            entity.removeEffect(net.minecraft.world.effect.MobEffects.POISON);
            if (entity.getRemainingFireTicks() < ticks) {
                entity.setRemainingFireTicks(ticks);
            }
        }

        data.putInt(NBT_SCORCHED_TICKS, ticks - 1);
        int fireStrength = data.getInt(NBT_SCORCHED_STRENGTH);
        ServerLevel level = (ServerLevel) entity.level();

        if (entity.isInWater()) {
            triggerThermalShock(entity, level, ticks, fireStrength);
            return;
        }

        if (entity.getRemainingFireTicks() < ticks) {
            entity.setRemainingFireTicks(ticks);
        }

        int auraInterval = ElementalFireNatureReactionsConfig.scorchedAuraDamageInterval;
        if (auraInterval < 1) auraInterval = 20;

        if (entity.tickCount % auraInterval == 0) {
            float baseDamage = calculateScorchedDamage(fireStrength, entity);
            float poisonMult = data.getFloat(NBT_SCORCHED_DAMAGE_MULT);
            if (poisonMult <= 0.0f) poisonMult = 1.0f;
            float rawBase = baseDamage / poisonMult;
            float damage = baseDamage;
            ElementType entityElement = ElementUtils.getConsistentAttackElement(entity);
            float elementMult = 1.0f;
            if (entityElement == ElementType.FIRE) {
                elementMult = (float) ElementalFireNatureReactionsConfig.scorchedFireDurationMultiplier;
            } else if (entityElement == ElementType.NATURE) {
                elementMult = (float) ElementalFireNatureReactionsConfig.scorchedNatureDurationMultiplier;
            } else if (entityElement == ElementType.THUNDER) {
                elementMult = (float) ElementalFireNatureReactionsConfig.scorchedThunderDurationMultiplier;
            } else if (entityElement == ElementType.FROST) {
                elementMult = (float) ElementalFireNatureReactionsConfig.scorchedFrostDurationMultiplier;
            }
            damage *= elementMult;
            if (damage > 0) {
                ElementDamageHelper.applyDamage(entity, damage, ModDamageTypes.source(entity.level(), ModDamageTypes.LAVA_MAGIC));
                level.sendParticles(ParticleTypes.LAVA, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 10, 0.2, 0.2, 0.2, 0.0);
                level.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 5, 0.2, 0.2, 0.2, 0.0);
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.2f, 1.0f);
                if (data.getInt(NBT_SCORCHED_TICK_LOGGED) == 0) {
                    DebugCommand.sendScorchedTickLog(entity, rawBase, entityElement, elementMult, damage, poisonMult);
                    data.putInt(NBT_SCORCHED_TICK_LOGGED, 1);
                }
            }
        }

        if (ElementalFireNatureReactionsConfig.scorchedAuraFirePowerThreshold > 0 && entity.tickCount % auraInterval == 0) {
            int sourceFirePower = data.getInt(NBT_SCORCHED_SOURCE_FIRE_POWER);
            int threshold = ElementalFireNatureReactionsConfig.scorchedAuraFirePowerThreshold;
            if (sourceFirePower >= threshold) {
                applyAuraDamage(entity, sourceFirePower, fireStrength, level);
            }
        }
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().getEffect() == ModMobEffects.WETNESS.get()) {
            boolean blocked = event.getEntity().getPersistentData().contains(NBT_SCORCHED_TICKS);
            if (blocked) {
                event.setResult(Event.Result.DENY);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        if (!entity.getPersistentData().contains(NBT_SCORCHED_TICKS)) return;
        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.IS_FIRE) && !source.is(ModDamageTypes.LAVA_MAGIC)) {
            if (ModMobEffects.SPORES.isPresent() && ModMobEffects.SPORES.get() != null && entity.hasEffect(ModMobEffects.SPORES.get())) {
                event.setAmount(0.001f);
            } else {
                event.setCanceled(true);
            }
        }
    }

    private static double findWaterSurfaceY(ServerLevel level, double x, double startY, double z) {
        int ix = (int) Math.floor(x);
        int iz = (int) Math.floor(z);
        int maxY = level.getMaxBuildHeight();
        for (int iy = (int) Math.floor(startY); iy < maxY; iy++) {
            if (!level.getFluidState(new BlockPos(ix, iy, iz)).is(FluidTags.WATER)) {
                return iy;
            }
        }
        return startY;
    }

    private static void triggerThermalShock(LivingEntity entity, ServerLevel level, int remainingTicks, int fireStrength) {
        double remainingSeconds = remainingTicks / 20.0;
        float dps = calculateScorchedDamage(fireStrength, entity);
        float totalRemainingDamage = (float) (remainingSeconds * dps);
        float ratio = (float) ElementalFireNatureReactionsConfig.scorchedShockDamageRatio;
        float shockDamage = totalRemainingDamage * ratio;

        if (shockDamage > 0.5f) {
            ElementDamageHelper.applyDamage(entity, shockDamage, ModDamageTypes.source(entity.level(), ModDamageTypes.LAVA_MAGIC));
        }

        int steamLevel = 0;
        if (ElementalFireNatureReactionsConfig.steamLowHeatMaxLevel > 0) {
            steamLevel = Math.max(1, Math.min(fireStrength / 20, ElementalFireNatureReactionsConfig.steamLowHeatMaxLevel));
            double surfaceY = findWaterSurfaceY(level, entity.getX(), entity.getY(), entity.getZ());
            SteamReactionHandler.spawnSteamCloud(level, entity.getX(), surfaceY, entity.getZ(), false, steamLevel);
        }

        DebugCommand.ThermalShockLogContext tsctx = new DebugCommand.ThermalShockLogContext();
        tsctx.target = entity;
        tsctx.remainingTicks = remainingTicks;
        tsctx.totalRemainingDamage = totalRemainingDamage;
        tsctx.ratio = ratio;
        tsctx.shockDamage = shockDamage;
        tsctx.steamLevel = steamLevel;
        DebugCommand.sendThermalShockLog(tsctx);

        BlockPos entityPos = entity.blockPosition();
        if (level.getFluidState(entityPos).is(FluidTags.WATER)) {
            level.setBlock(entityPos, Blocks.AIR.defaultBlockState(), 3);
        }

        clearScorched(entity);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f, 2.0f);
        level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 20, 0.5, 0.5, 0.5, 0.05);
    }

    private static void applyAuraDamage(LivingEntity source, int sourceFirePower, int fireStrength, ServerLevel level) {
        double radius = ElementalFireNatureReactionsConfig.scorchedAuraRadius;
        double sourceY = source.getY();
        AABB box = new AABB(
                source.getX() - radius, sourceY - 0.5, source.getZ() - radius,
                source.getX() + radius, sourceY + 1.5, source.getZ() + radius
        );
        java.util.List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, box, e -> e != source && Math.abs(e.getY() - sourceY) < 1.0);
        if (nearby.isEmpty()) return;

        int resistThreshold = ElementalFireNatureReactionsConfig.scorchedResistThreshold;

        boolean steamEnabled = ElementalFireNatureReactionsConfig.scorchedAuraSteamEnabled;

        for (LivingEntity target : nearby) {
            if (target.isDeadOrDying()) continue;
            boolean fireImmune = target.fireImmune();

            int resist = ElementUtils.getDisplayResistance(target, ElementType.FIRE);
            if (resist >= resistThreshold) continue;

            double dx = target.getX() - source.getX();
            double dz = target.getZ() - source.getZ();
            if (Math.sqrt(dx * dx + dz * dz) > radius) continue;

            if (steamEnabled) {
                int wetness = WetnessHandler.getWetnessLevel(target);
                if (wetness > 0 && ElementalFireNatureReactionsConfig.steamHighHeatMaxLevel > 0) {
                    int steamLevel = Math.max(1, Math.min(wetness, ElementalFireNatureReactionsConfig.steamHighHeatMaxLevel));
                    WetnessHandler.clearWetnessData(target);
                    SteamReactionHandler.spawnSteamCloud(target, true, steamLevel);
                    continue;
                }
            }

            if (ElementalThunderFrostReactionsConfig.frostbiteAuraScorchedSteamEnabled
                    && FrostbiteHandler.isFrozen(target)
                    && ElementalFireNatureReactionsConfig.steamHighHeatMaxLevel > 0) {
                CompoundTag frozenData = target.getPersistentData();
                int frozenStacks = frozenData.getInt(FrostbiteHandler.NBT_FREEZE_STACKS);
                if (frozenStacks <= 0) frozenStacks = 1;
                target.removeEffect(ModMobEffects.FREEZE.get());
                frozenData.remove(FrostbiteHandler.NBT_FREEZE_STACKS);
                frozenData.remove(FrostbiteHandler.NBT_FROZEN_FROSTBITE_STACKS);
                frozenData.putLong(FrostbiteHandler.NBT_FREEZE_COOLDOWN,
                        target.level().getGameTime() + ElementalThunderFrostReactionsConfig.freezeCooldownTicks);
                FrostbiteHandler.clearFrostbite(target);
                clearScorched(target);
                int steamLevel = Math.max(1, Math.min(frozenStacks, ElementalFireNatureReactionsConfig.steamHighHeatMaxLevel));
                SteamReactionHandler.spawnSteamCloud(target, true, steamLevel);
                continue;
            }

            if (ElementalFireNatureReactionsConfig.scorchedAuraSporeDetonationEnabled
                    && ModMobEffects.SPORES.isPresent() && ModMobEffects.SPORES.get() != null
                    && target.hasEffect(ModMobEffects.SPORES.get())
                    && !(ElementalThunderFrostReactionsConfig.frostbiteReduceSporesEnabled && FrostbiteHandler.hasFrostbite(target))) {
                ReactionHandler.triggerToxicBlast(level, source, target, sourceFirePower, source);
                continue;
            }

            float baseAuraDamage = calculateScorchedDamage(fireStrength, target);
            float auraPoisonMult = source.getPersistentData().getFloat(NBT_SCORCHED_DAMAGE_MULT);
            if (auraPoisonMult <= 0.0f) auraPoisonMult = 1.0f;
            float rawAuraBase = baseAuraDamage / auraPoisonMult;
            float auraDamage = baseAuraDamage;
            ElementType targetElement = ElementUtils.getConsistentAttackElement(target);
            float auraElementMult = 1.0f;
            if (targetElement == ElementType.FIRE) {
                auraElementMult = (float) ElementalFireNatureReactionsConfig.scorchedFireDurationMultiplier;
            } else if (targetElement == ElementType.NATURE) {
                auraElementMult = (float) ElementalFireNatureReactionsConfig.scorchedNatureDurationMultiplier;
            } else if (targetElement == ElementType.THUNDER) {
                auraElementMult = (float) ElementalFireNatureReactionsConfig.scorchedThunderDurationMultiplier;
            } else if (targetElement == ElementType.FROST) {
                auraElementMult = (float) ElementalFireNatureReactionsConfig.scorchedFrostDurationMultiplier;
            }
            auraDamage *= auraElementMult;
            ElementDamageHelper.applyDamage(target, auraDamage, ModDamageTypes.source(level, ModDamageTypes.LAVA_MAGIC));
            DebugCommand.sendScorchedAuraLog(source, target, rawAuraBase, targetElement, auraElementMult, auraDamage, auraPoisonMult);
            level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 0.1, target.getZ(), 5, 0.3, 0.1, 0.3, 0.01);
        }
    }

    static float calculateScorchedDamage(int fireStrength, LivingEntity target) {
        int resistPoints = ElementUtils.getDisplayResistance(target, ElementType.FIRE);
        if (resistPoints >= ElementalFireNatureReactionsConfig.scorchedResistThreshold) {
            return 0.0f;
        }

        double base = ElementalFireNatureReactionsConfig.scorchedDamageBase;
        int step = Math.max(1, ElementalFireNatureReactionsConfig.scorchedDamageScalingStep);
        int threshold = ElementalFireNatureReactionsConfig.scorchedTriggerThreshold;
        double bonus = Math.max(0, (double)(fireStrength - threshold) / step * 0.5);
        double rawDamage = base + bonus;

        if (target.fireImmune()) {
            rawDamage *= ElementalFireNatureReactionsConfig.scorchedImmuneModifier;
        }

        int fireProtLevel = 0;
        int genProtLevel = 0;
        for (ItemStack stack : target.getArmorSlots()) {
            fireProtLevel += stack.getEnchantmentLevel(Enchantments.FIRE_PROTECTION);
            genProtLevel += stack.getEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION);
        }

        double denom = Math.max(1.0, ElementalFireNatureReactionsConfig.enchantmentCalculationDenominator);
        double fireProtReduction = (Math.min(fireProtLevel, denom) / denom) * ElementalFireNatureReactionsConfig.scorchedFireProtReduction;
        double genProtReduction = (Math.min(genProtLevel, denom) / denom) * ElementalFireNatureReactionsConfig.scorchedGenProtReduction;

        double finalDamage = rawDamage * (1.0 - fireProtReduction) * (1.0 - genProtReduction);

        float damageMult = target.getPersistentData().getFloat(NBT_SCORCHED_DAMAGE_MULT);
        if (damageMult <= 0.0f) damageMult = 1.0f;
        finalDamage *= damageMult;

        return (float) finalDamage;
    }
}
