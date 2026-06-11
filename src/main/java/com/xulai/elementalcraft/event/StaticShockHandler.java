package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LightningBolt;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.sound.ModSounds;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import com.xulai.elementalcraft.util.ElementDamageHelper;
import com.xulai.elementalcraft.event.WetnessHandler;
import com.xulai.elementalcraft.util.EffectHelper;
import com.xulai.elementalcraft.client.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;

import com.xulai.elementalcraft.event.ScorchedHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class StaticShockHandler {
    private static final Random RANDOM = new Random();
    public static final String NBT_STATIC_STACKS = "ec_static_stacks";
    private static final String NBT_STATIC_TIMER = "ec_static_timer";
    private static final String NBT_STATIC_DAMAGE_TIMER = "ec_static_damage_timer";
    private static final String NBT_STATIC_AURA_DAMAGE_TIMER = "ec_static_aura_damage_timer";
    private static final String NBT_PARALYSIS_STACKS = "ec_paralysis_stacks";
    private static final String NBT_PARALYSIS_TIMER = "ec_paralysis_timer";
    private static final String NBT_PARALYSIS_COOLDOWN_TIMER = "ec_paralysis_cooldown_timer";
    private static final String NBT_STATIC_PRIMED = "ec_static_primed";
    private static final Map<ResourceKey<Level>, ActiveElectrification> activeElectrifications = new HashMap<>();

    private static class ActiveElectrification {
        final double x, y, z, range;
        final long startTick;
        final int duration;
        final float settlementDamage;
        final Set<UUID> damagedEntities = new HashSet<>();
        long lastParticleTick;
        ActiveElectrification(double x, double y, double z, double range, long startTick, int duration, float settlementDamage) {
            this.x = x; this.y = y; this.z = z;
            this.range = range; this.startTick = startTick; this.duration = duration;
            this.settlementDamage = settlementDamage;
        }
    }

    private static boolean isImmuneToStatic(LivingEntity entity) {
        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        if (ElementalThunderFrostReactionsConfig.cachedStaticImmunityBlacklist.contains(entityId)) {
            return true;
        }
        int resist = ElementUtils.getDisplayResistance(entity, ElementType.THUNDER);
        return resist >= ElementalThunderFrostReactionsConfig.staticResistImmunityThreshold;
    }

    private static boolean isImmuneToParalysis(LivingEntity entity) {
        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        return ElementalThunderFrostReactionsConfig.cachedParalysisImmunityBlacklist.contains(entityId);
    }

    private static boolean isInOrOnWater(LivingEntity entity) {
        if (entity.isInWater()) return true;
        return entity.level().getFluidState(entity.blockPosition()).is(FluidTags.WATER);
    }

    private static boolean isFriendlyToSource(LivingEntity target, LivingEntity source) {
        if (target instanceof Player) return true;
        if (target instanceof TamableAnimal tamable && tamable.isTame()) return true;
        if (source != null) {
            if (target instanceof OwnableEntity ownable) {
                Entity owner = ownable.getOwner();
                if (owner != null && owner.getUUID().equals(source.getUUID())) return true;
            }
        }
        return false;
    }

    private static boolean shouldSkipAuraTarget(LivingEntity target, LivingEntity source) {
        if (target instanceof Player player && player.isCreative()) return true;
        if (target.isDeadOrDying()) return true;
        if (ElementalThunderFrostReactionsConfig.staticAuraExcludeFriendly && isFriendlyToSource(target, source)) return true;
        if (ElementalThunderFrostReactionsConfig.staticAuraOnlyHostile && target.getType().getCategory() != MobCategory.MONSTER) return true;
        return false;
    }

    private static void trimStaticStacks(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(NBT_STATIC_STACKS)) return;
        int currentStacks = data.getInt(NBT_STATIC_STACKS);
        int maxStacks = ElementalThunderFrostReactionsConfig.staticMaxTotalStacks;
        if (currentStacks > maxStacks) {
            int totalTimer = data.getInt(NBT_STATIC_TIMER);
            int durationPerStack = ElementalThunderFrostReactionsConfig.staticDurationPerStackTicks;
            int excess = currentStacks - maxStacks;
            totalTimer = Math.max(1, totalTimer - excess * durationPerStack);
            data.putInt(NBT_STATIC_STACKS, maxStacks);
            data.putInt(NBT_STATIC_TIMER, totalTimer);
            updateEffect(entity, maxStacks, totalTimer);
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        LivingEntity target = event.getEntity();
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        if (target instanceof Player player && player.isCreative()) return;

        if (isImmuneToStatic(target)) {
            CompoundTag data = target.getPersistentData();
            int targetStacks = data.getInt(NBT_STATIC_STACKS);
            if (ElementalThunderFrostReactionsConfig.staticAuraThreshold > 0 && targetStacks >= ElementalThunderFrostReactionsConfig.staticAuraThreshold) {
                clearStaticAuraEffects(target, targetStacks);
            }
            clearStaticShock(target);
            return;
        }

        trimStaticStacks(target);

        ElementType consistentElement = ElementUtils.getConsistentAttackElement(attacker);
        if (consistentElement != ElementType.THUNDER) {
            return;
        }

        int thunderStrength = ElementUtils.getDisplayEnhancement(attacker, ElementType.THUNDER);
        int threshold = ElementalThunderFrostReactionsConfig.thunderStrengthThreshold;
        if (threshold <= 0 || thunderStrength < threshold) {
            return;
        }

        boolean hasWetness = target.hasEffect(ModMobEffects.WETNESS.get());
        int wetnessLevel = 0;
        if (hasWetness) {
            MobEffectInstance wetnessEffect = target.getEffect(ModMobEffects.WETNESS.get());
            if (wetnessEffect != null) {
                wetnessLevel = wetnessEffect.getAmplifier() + 1;
            }
        }

        double chance = calculateTriggerChance(thunderStrength, wetnessLevel, target);
        boolean triggered = RANDOM.nextDouble() < chance;

        if (!triggered) {
            DebugCommand.sendReactionFailed(target, "static_shock", "chance",
                    attacker.getDisplayName(), target.getDisplayName(), String.format("%.0f", chance * 100));
            return;
        }

        if (wetnessLevel > 0) {
            CompoundTag data = target.getPersistentData();
            int currentStacks = data.getInt(NBT_STATIC_STACKS);
            int maxStacks = ElementalThunderFrostReactionsConfig.staticMaxTotalStacks;
            if (currentStacks >= maxStacks) {
                return;
            }
            int addStacks = ElementalThunderFrostReactionsConfig.staticMaxStacksPerAttack;
            int newStacks = Math.min(maxStacks, currentStacks + addStacks);
            int actualAdded = newStacks - currentStacks;
            int durationPerStack = ElementalThunderFrostReactionsConfig.staticDurationPerStackTicks;
            int addTicks = actualAdded * durationPerStack;
            int newTotalTicks = data.getInt(NBT_STATIC_TIMER) + addTicks;
            data.putInt(NBT_STATIC_STACKS, newStacks);
            data.putInt(NBT_STATIC_TIMER, newTotalTicks);
            DebugCommand.sendReactionSuccess(target, "static_shock",
                    attacker.getDisplayName(), target.getDisplayName(),
                    Component.literal(String.valueOf(actualAdded)).withStyle(ChatFormatting.LIGHT_PURPLE),
                    String.format("%.0f", chance * 100));
            if (!target.isInWater() || ElementalThunderFrostReactionsConfig.waterElectrificationRangeBase <= 0) {
                WetnessHandler.resolveElementReactionConflict(target, attacker);
            }
            if (target.hasEffect(ModMobEffects.SPORES.get())) {
                tryTriggerSporeBlast(target);
            }
            return;
        }

        CompoundTag data = target.getPersistentData();
        int currentStacks = data.getInt(NBT_STATIC_STACKS);
        int maxStacks = ElementalThunderFrostReactionsConfig.staticMaxTotalStacks;
        if (currentStacks >= maxStacks) {
            return;
        }
        int addStacks = ElementalThunderFrostReactionsConfig.staticMaxStacksPerAttack;
        int newStacks = Math.min(maxStacks, currentStacks + addStacks);
        int actualAdded = newStacks - currentStacks;
        int durationPerStack = ElementalThunderFrostReactionsConfig.staticDurationPerStackTicks;
        int addTicks = actualAdded * durationPerStack;
        int currentTimer = data.getInt(NBT_STATIC_TIMER);
        int newTotalTicks = currentTimer + addTicks;
        data.putInt(NBT_STATIC_STACKS, newStacks);
        data.putInt(NBT_STATIC_TIMER, newTotalTicks);
        data.putInt(NBT_STATIC_DAMAGE_TIMER, data.getInt(NBT_STATIC_DAMAGE_TIMER));
        updateEffect(target, newStacks, newTotalTicks);
        DebugCommand.sendReactionSuccess(target, "static_shock",
                attacker.getDisplayName(), target.getDisplayName(),
                Component.literal(String.valueOf(actualAdded)).withStyle(ChatFormatting.LIGHT_PURPLE),
                String.format("%.0f", chance * 100));
        if (target.hasEffect(ModMobEffects.SPORES.get())) {
            tryTriggerSporeBlast(target);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        if (entity instanceof Player player && player.isCreative()) return;

        if (isImmuneToStatic(entity)) {
            CompoundTag data = entity.getPersistentData();
            int staticStacks = data.getInt(NBT_STATIC_STACKS);
            if (ElementalThunderFrostReactionsConfig.staticAuraThreshold > 0 && staticStacks >= ElementalThunderFrostReactionsConfig.staticAuraThreshold) {
                clearStaticAuraEffects(entity, staticStacks);
            }
            clearStaticShock(entity);
            return;
        }

        CompoundTag data = entity.getPersistentData();
        int cooldownTimer = data.getInt(NBT_PARALYSIS_COOLDOWN_TIMER);
        if (cooldownTimer > 0) {
            cooldownTimer--;
            data.putInt(NBT_PARALYSIS_COOLDOWN_TIMER, cooldownTimer);
        }

        trimStaticStacks(entity);
        MobEffectInstance effectInstance = entity.getEffect(ModMobEffects.STATIC_SHOCK.get());
        if (effectInstance != null && !data.contains(NBT_STATIC_STACKS)) {
            int amplifier = effectInstance.getAmplifier();
            int remainingTicks = effectInstance.getDuration();
            int stacks = amplifier + 1;
            int durationPerStack = ElementalThunderFrostReactionsConfig.staticDurationPerStackTicks;
            int maxStacks = ElementalThunderFrostReactionsConfig.staticMaxTotalStacks;
            stacks = Math.min(stacks, maxStacks);
            int minRequiredTicks = stacks * durationPerStack;
            if (remainingTicks < minRequiredTicks) {
                stacks = Math.max(1, remainingTicks / durationPerStack);
                amplifier = stacks - 1;
            }
            data.putInt(NBT_STATIC_STACKS, stacks);
            data.putInt(NBT_STATIC_TIMER, remainingTicks);
            data.putInt(NBT_STATIC_DAMAGE_TIMER, 0);
            if (effectInstance.getDuration() != remainingTicks || effectInstance.getAmplifier() != amplifier) {
                entity.removeEffect(ModMobEffects.STATIC_SHOCK.get());
                entity.addEffect(new MobEffectInstance(
                        ModMobEffects.STATIC_SHOCK.get(), remainingTicks, amplifier, false, false, true
                ));
            }
        }

        if (effectInstance != null && data.contains(NBT_STATIC_STACKS)) {
            if (effectInstance.getDuration() > data.getInt(NBT_STATIC_TIMER)) {
                data.putInt(NBT_STATIC_TIMER, effectInstance.getDuration());
            }
        }

        if (!data.contains(NBT_STATIC_STACKS)) {
            if (entity.hasEffect(ModMobEffects.STATIC_SHOCK.get())) {
                entity.removeEffect(ModMobEffects.STATIC_SHOCK.get());
            }
            return;
        }

        int stacks = data.getInt(NBT_STATIC_STACKS);
        if (stacks <= 0) {
            clearStaticShock(entity);
            return;
        }

        if (ElementalThunderFrostReactionsConfig.thunderBreakFreezeWetnessLayers > 0
                && ElementalThunderFrostReactionsConfig.thunderBreakFreezeChance > 0
                && ElementalThunderFrostReactionsConfig.staticAuraThreshold > 0 && stacks >= ElementalThunderFrostReactionsConfig.staticAuraThreshold
                && FrostbiteHandler.isFrozen(entity)
                && !FrostbiteHandler.isFreezeImmune(entity)
                && RANDOM.nextDouble() < stacks * ElementalThunderFrostReactionsConfig.thunderBreakFreezeChance) {
            entity.removeEffect(ModMobEffects.FREEZE.get());
            CompoundTag fd = entity.getPersistentData();
            fd.remove(FrostbiteHandler.NBT_FROZEN_FROSTBITE_STACKS);
            fd.remove(FrostbiteHandler.NBT_FREEZE_STACKS);
            fd.remove(FrostbiteHandler.NBT_FREEZE_AI_DISABLED);
            fd.remove("EC_SharedOriginalNoAI");
            fd.remove("EC_DrownTimer");
            fd.putLong(FrostbiteHandler.NBT_FREEZE_COOLDOWN,
                    entity.level().getGameTime() + ElementalThunderFrostReactionsConfig.freezeCooldownTicks);
            int layers = ElementalThunderFrostReactionsConfig.thunderBreakFreezeWetnessLayers;
            WetnessHandler.updateWetnessLevel(entity, layers);
            if (entity.hasEffect(ModMobEffects.WETNESS.get())) {
                entity.removeEffect(ModMobEffects.WETNESS.get());
            }
            entity.addEffect(new MobEffectInstance(
                    ModMobEffects.WETNESS.get(),
                    layers * 200,
                    layers - 1,
                    true, false, true
            ));
        }

        if (entity.level() instanceof ServerLevel serverLevel) {
            EffectHelper.playStaticShockParticles(serverLevel, entity);
        }

        boolean hasWetness = entity.hasEffect(ModMobEffects.WETNESS.get());
        int wetnessLevel = 0;
        if (hasWetness) {
            MobEffectInstance wetnessEffect = entity.getEffect(ModMobEffects.WETNESS.get());
            if (wetnessEffect != null) {
                wetnessLevel = wetnessEffect.getAmplifier() + 1;
            }
        }

        if (wetnessLevel > 0) {
            if (processWaterElectrification(entity, stacks)) {
                return;
            }
        }

        int totalTimer = data.getInt(NBT_STATIC_TIMER);
        int damageTimer = data.getInt(NBT_STATIC_DAMAGE_TIMER);
        int interval = ElementalThunderFrostReactionsConfig.staticDamageIntervalTicks;
        if (interval < 1) interval = 1;

        boolean auraActive = ElementalThunderFrostReactionsConfig.staticAuraThreshold > 0 && stacks >= ElementalThunderFrostReactionsConfig.staticAuraThreshold;

        if (auraActive) {
            applyStaticAuraEffects(entity, stacks);
        }

        damageTimer++;
        if (damageTimer >= interval) {
            if (totalTimer > 0) {
                triggerStaticDamage(entity);
                tryPrimingOrIgniteCreeper(entity);
            }
            damageTimer = 0;
        }
        data.putInt(NBT_STATIC_DAMAGE_TIMER, damageTimer);

        if (auraActive && totalTimer > 0) {
            int auraInterval = ElementalThunderFrostReactionsConfig.staticAuraDamageIntervalTicks;
            if (auraInterval < 1) auraInterval = 1;
            int auraDamageTimer = data.getInt(NBT_STATIC_AURA_DAMAGE_TIMER);
            auraDamageTimer++;
            if (auraDamageTimer >= auraInterval) {
                applyStaticAuraDamage(entity, stacks);
                auraDamageTimer = 0;
            }
            data.putInt(NBT_STATIC_AURA_DAMAGE_TIMER, auraDamageTimer);
        }

        if (totalTimer > 0) {
            totalTimer--;
            data.putInt(NBT_STATIC_TIMER, totalTimer);
        }

        if (totalTimer <= 0) {
            if (auraActive) {
                clearStaticAuraEffects(entity, stacks);
            }
            clearStaticShock(entity);
            return;
        }

        updateEffect(entity, stacks, totalTimer);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.level.isClientSide()) return;
        if (event.phase != TickEvent.Phase.END) return;
        if (activeElectrifications.isEmpty()) return;

        ResourceKey<Level> dim = event.level.dimension();
        ActiveElectrification elec = activeElectrifications.get(dim);
        if (elec == null) return;

        long now = event.level.getGameTime();

        if (now - elec.startTick >= elec.duration) {
            if (event.level instanceof ServerLevel sl) {
                AABB area = new AABB(
                        elec.x - elec.range, elec.y - elec.range, elec.z - elec.range,
                        elec.x + elec.range, elec.y + elec.range, elec.z + elec.range);
                for (LivingEntity target : sl.getEntitiesOfClass(LivingEntity.class, area)) {
                    if (target.hasEffect(ModMobEffects.PARALYSIS.get())) {
                        target.removeEffect(ModMobEffects.PARALYSIS.get());
                    }
                }
            }
            activeElectrifications.remove(dim);
            return;
        }

        if (!(event.level instanceof ServerLevel sl)) return;

        AABB area = new AABB(
                elec.x - elec.range, elec.y - elec.range, elec.z - elec.range,
                elec.x + elec.range, elec.y + elec.range, elec.z + elec.range);

        for (LivingEntity entity : sl.getEntitiesOfClass(LivingEntity.class, area)) {
            if (!isInOrOnWater(entity)) continue;

            double dx = entity.getX() - elec.x;
            double dy = entity.getY() - elec.y;
            double dz = entity.getZ() - elec.z;
            if (dx * dx + dy * dy + dz * dz > elec.range * elec.range) continue;

            if (!isImmuneToStatic(entity) && !elec.damagedEntities.contains(entity.getUUID())) {
                elec.damagedEntities.add(entity.getUUID());
                float dmg = applyEnchantmentReduction(entity, elec.settlementDamage);
                if (dmg > 0) {
                    ElementDamageHelper.applyDamage(entity, dmg, ModDamageTypes.source(sl, ModDamageTypes.STATIC_SHOCK));
                }
            }

            if (!isImmuneToStatic(entity)) {
                long remaining = elec.duration - (now - elec.startTick);
                if (remaining > 0 && ElementalThunderFrostReactionsConfig.paralysisMaxStacks > 0) {
                    entity.addEffect(new MobEffectInstance(ModMobEffects.PARALYSIS.get(), (int)remaining, 0, false, false, true));
                }
            }
        }

        long elapsed = now - elec.startTick;
        if (elapsed % 60 == 0 && elec.lastParticleTick != now) {
            elec.lastParticleTick = now;
            double ex = elec.x, ey = elec.y, ez = elec.z, er = elec.range;
            int mX = (int)Math.floor(ex - er), MX = (int)Math.ceil(ex + er);
            int mY = (int)Math.floor(ey - er), MY = (int)Math.ceil(ey + er);
            int mZ = (int)Math.floor(ez - er), MZ = (int)Math.ceil(ez + er);
            BlockPos.betweenClosed(mX, mY, mZ, MX, MY, MZ).forEach(pos -> {
                boolean onBoundary = pos.getX() == mX || pos.getX() == MX
                        || pos.getY() == mY || pos.getY() == MY
                        || pos.getZ() == mZ || pos.getZ() == MZ;
                if (onBoundary && sl.getFluidState(pos).is(FluidTags.WATER)) {
                    sl.sendParticles(ModParticles.THUNDER_SPARK_PERSISTENT.get(),
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            1, 0, 0, 0, 0);
                    if (sl.isEmptyBlock(pos.above())) {
                        sl.sendParticles(ModParticles.THUNDER_SPARK_PERSISTENT.get(),
                                pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                                1, 0, 0, 0, 0);
                    }
                }
            });
        }
    }

    private static boolean processWaterElectrification(LivingEntity source, int stacks) {
        if (ElementalThunderFrostReactionsConfig.waterElectrificationRangeBase <= 0) return false;
        if (stacks <= 0) return false;
        if (!isInOrOnWater(source)) return false;

        double range = ElementalThunderFrostReactionsConfig.waterElectrificationRangeBase
                + stacks * ElementalThunderFrostReactionsConfig.waterElectrificationRangePerStack;
        range = Math.min(range, ElementalThunderFrostReactionsConfig.waterElectrificationMaxRange);

        ActiveElectrification existing = activeElectrifications.get(source.level().dimension());
        if (existing != null && source.level().getGameTime() - existing.startTick >= existing.duration) {
            activeElectrifications.remove(source.level().dimension());
        }

        CompoundTag sourceData = source.getPersistentData();
        boolean firstTrigger = !activeElectrifications.containsKey(source.level().dimension());

        if (firstTrigger) {
            int sourceTimer = sourceData.getInt(NBT_STATIC_TIMER);
            int interval = ElementalThunderFrostReactionsConfig.staticDamageIntervalTicks;
            if (interval < 1) interval = 1;
            int remainingHits = (sourceTimer + interval - 1) / interval;

            double baseSettlementDamage = 0;
            for (int i = 0; i < remainingHits; i++) {
                baseSettlementDamage += getRandomStaticDamage(source);
            }
            baseSettlementDamage *= ElementalThunderFrostReactionsConfig.paralysisDamagePercentage;

            int paralysisDuration = ElementalThunderFrostReactionsConfig.waterElectrificationParalysisDuration;

            if (ElementalThunderFrostReactionsConfig.staticAuraThreshold > 0 && stacks >= ElementalThunderFrostReactionsConfig.staticAuraThreshold) {
                clearStaticAuraEffects(source, stacks);
            }
            clearStaticShock(source);
            if (WetnessHandler.getWetnessLevel(source) > 0) {
                WetnessHandler.clearWetnessData(source);
            }

            ActiveElectrification newElec = new ActiveElectrification(
                source.getX(), source.getY(), source.getZ(), range, source.level().getGameTime(), paralysisDuration, (float)baseSettlementDamage);
            activeElectrifications.put(source.level().dimension(), newElec);
            newElec.damagedEntities.add(source.getUUID());

            if (paralysisDuration > 0 && ElementalThunderFrostReactionsConfig.paralysisMaxStacks > 0) {
                source.addEffect(new MobEffectInstance(ModMobEffects.PARALYSIS.get(), paralysisDuration, 0, false, false, true));
            }

            AABB area = new AABB(
                    source.getX() - range, source.getY() - range, source.getZ() - range,
                    source.getX() + range, source.getY() + range, source.getZ() + range
            );
            java.util.List<LivingEntity> affectedTargets = new java.util.ArrayList<>();
            java.util.List<LivingEntity> nearby = source.level().getEntitiesOfClass(LivingEntity.class, area);
            for (LivingEntity target : nearby) {
                if (target instanceof Player player && player.isCreative()) continue;
                if (target.isDeadOrDying()) continue;
                if (!isInOrOnWater(target)) continue;
                if (isImmuneToStatic(target)) continue;

                if (paralysisDuration > 0 && ElementalThunderFrostReactionsConfig.paralysisMaxStacks > 0) {
                    target.addEffect(new MobEffectInstance(ModMobEffects.PARALYSIS.get(), paralysisDuration, 0, false, false, true));
                }
                if (source.level() instanceof ServerLevel serverLevel) {
                    EffectHelper.playStaticSplashParticles(serverLevel, source, target);
                }
                newElec.damagedEntities.add(target.getUUID());
                affectedTargets.add(target);
            }

            for (LivingEntity target : affectedTargets) {
                float finalDamage = applyEnchantmentReduction(target, (float) baseSettlementDamage);
                if (finalDamage > 0) {
                    ElementDamageHelper.applyDamage(target, finalDamage, ModDamageTypes.source(target.level(), ModDamageTypes.STATIC_SHOCK));
                }
            }

            DebugCommand.WaterElectrificationLogContext logCtx = new DebugCommand.WaterElectrificationLogContext();
            logCtx.source = source;
            logCtx.stacks = stacks;
            logCtx.range = range;
            logCtx.affectedCount = affectedTargets.size() + 1;
            logCtx.settlementDamage = (float)baseSettlementDamage;
            logCtx.paralysisDuration = paralysisDuration;
            DebugCommand.sendWaterElectrificationLog(logCtx);

            return true;
        }
        return false;
    }

    public static boolean tryTriggerWaterElectrification(LivingEntity source, int paralysisDuration) {
        int stacks = source.getPersistentData().getInt(NBT_STATIC_STACKS);
        if (processWaterElectrification(source, stacks)) {
            return true;
        }
        int paralysisCooldown = ElementalThunderFrostReactionsConfig.paralysisCooldownTicks;
        source.getPersistentData().putInt(NBT_PARALYSIS_COOLDOWN_TIMER, paralysisDuration + paralysisCooldown);
        return false;
    }

    private static int targetInFrostAuraRange(LivingEntity target, LivingEntity exclude) {
        double searchRadius = ElementalThunderFrostReactionsConfig.frostbiteAuraMaxRange;
        AABB searchArea = new AABB(
                target.getX() - searchRadius, target.getY() - searchRadius, target.getZ() - searchRadius,
                target.getX() + searchRadius, target.getY() + searchRadius, target.getZ() + searchRadius
        );
        int maxStacks = 0;
        for (LivingEntity source : target.level().getEntitiesOfClass(LivingEntity.class, searchArea)) {
            if (source == target || source == exclude) continue;
            int stacks = source.getPersistentData().getInt(FrostbiteHandler.NBT_FROSTBITE_STACKS);
            if (ElementalThunderFrostReactionsConfig.frostbiteAuraThreshold <= 0 || stacks < ElementalThunderFrostReactionsConfig.frostbiteAuraThreshold) continue;
            double range = FrostbiteHandler.getAuraRange(stacks);
            if (source.distanceToSqr(target) > range * range) continue;
            if (stacks > maxStacks) maxStacks = stacks;
        }
        return maxStacks;
    }

    private static void applyStaticAuraEffects(LivingEntity source, int stacks) {
        double range = stacks * ElementalThunderFrostReactionsConfig.staticAuraBaseRange;

        AABB auraArea = new AABB(
                source.getX() - range, source.getY() - range, source.getZ() - range,
                source.getX() + range, source.getY() + range, source.getZ() + range
        );
        java.util.List<LivingEntity> nearby = source.level().getEntitiesOfClass(LivingEntity.class, auraArea);

        for (LivingEntity target : nearby) {
            if (target == source) continue;
            if (shouldSkipAuraTarget(target, source)) continue;

            double dx = target.getX() - source.getX();
            double dz = target.getZ() - source.getZ();
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            if (horizontalDist > range) continue;

            double dy = target.getY() - source.getY();
            if (dy > ElementalThunderFrostReactionsConfig.staticAuraHeightCeiling) continue;

            if (isImmuneToStatic(target)) continue;

            int targetStaticStacks = target.getPersistentData().getInt(NBT_STATIC_STACKS);
            if (ElementalThunderFrostReactionsConfig.thunderBreakFreezeWetnessLayers > 0
                    && ElementalThunderFrostReactionsConfig.thunderBreakFreezeChance > 0
                    && FrostbiteHandler.isFrozen(target)
                    && !FrostbiteHandler.isFreezeImmune(target)
                    && RANDOM.nextDouble() < targetStaticStacks * ElementalThunderFrostReactionsConfig.thunderBreakFreezeChance) {
                target.removeEffect(ModMobEffects.FREEZE.get());
                CompoundTag fd = target.getPersistentData();
                fd.remove(FrostbiteHandler.NBT_FROZEN_FROSTBITE_STACKS);
                fd.remove(FrostbiteHandler.NBT_FREEZE_STACKS);
                fd.remove(FrostbiteHandler.NBT_FREEZE_AI_DISABLED);
                fd.remove("EC_SharedOriginalNoAI");
                fd.remove("EC_DrownTimer");
                fd.putLong(FrostbiteHandler.NBT_FREEZE_COOLDOWN,
                        target.level().getGameTime() + ElementalThunderFrostReactionsConfig.freezeCooldownTicks);
                int layers = ElementalThunderFrostReactionsConfig.thunderBreakFreezeWetnessLayers;
                WetnessHandler.updateWetnessLevel(target, layers);
                if (target.hasEffect(ModMobEffects.WETNESS.get())) {
                    target.removeEffect(ModMobEffects.WETNESS.get());
                }
                target.addEffect(new MobEffectInstance(
                        ModMobEffects.WETNESS.get(),
                        layers * 200,
                        layers - 1,
                        true, false, true
                ));
                continue;
            }

            boolean targetHasWetness = target.hasEffect(ModMobEffects.WETNESS.get());
            boolean targetHasParalysis = target.hasEffect(ModMobEffects.PARALYSIS.get());
            if (targetHasWetness && !isImmuneToParalysis(target)) {
                boolean sourceHasFrostbiteAura = FrostbiteHandler.hasFrostbite(source)
                        && ElementalThunderFrostReactionsConfig.frostbiteAuraThreshold > 0
                        && source.getPersistentData().getInt(FrostbiteHandler.NBT_FROSTBITE_STACKS) >= ElementalThunderFrostReactionsConfig.frostbiteAuraThreshold;
                if (sourceHasFrostbiteAura) {
                    int frostbiteStacks = source.getPersistentData().getInt(FrostbiteHandler.NBT_FROSTBITE_STACKS);
                    if (stacks < frostbiteStacks) {
                        continue;
                    }
                    if (stacks == frostbiteStacks && RANDOM.nextBoolean()) {
                        continue;
                    }
                }
                int targetFrostbiteStacks = FrostbiteHandler.getFrostbiteStacks(target);
                if (ElementalThunderFrostReactionsConfig.frostbiteAuraThreshold > 0 && targetFrostbiteStacks >= ElementalThunderFrostReactionsConfig.frostbiteAuraThreshold) {
                    if (stacks < targetFrostbiteStacks) {
                        continue;
                    }
                    if (stacks == targetFrostbiteStacks && RANDOM.nextBoolean()) {
                        continue;
                    }
                }
                int nearbyFrostStacks = targetInFrostAuraRange(target, source);
                if (nearbyFrostStacks > 0) {
                    if (stacks < nearbyFrostStacks) {
                        continue;
                    }
                    if (stacks == nearbyFrostStacks && RANDOM.nextBoolean()) {
                        continue;
                    }
                }
                int wetnessLevel = WetnessHandler.getWetnessLevel(target);
                if (ElementalThunderFrostReactionsConfig.paralysisMaxStacks <= 0) continue;
                int paralysisStacks = Math.max(wetnessLevel, stacks);
                paralysisStacks = Math.min(paralysisStacks, ElementalThunderFrostReactionsConfig.paralysisMaxStacks);
                WetnessHandler.clearWetnessData(target);
                target.addEffect(new MobEffectInstance(
                        ModMobEffects.PARALYSIS.get(), 60, paralysisStacks - 1, false, false, true));
                CompoundTag targetData = target.getPersistentData();
                targetData.putInt(NBT_PARALYSIS_STACKS, paralysisStacks);
                targetData.putInt(NBT_PARALYSIS_TIMER, 60);
            } else if (targetHasParalysis) {
                CompoundTag targetData = target.getPersistentData();
                int existingStacks = targetData.getInt(NBT_PARALYSIS_STACKS);
                if (existingStacks <= 0) existingStacks = 1;
                target.addEffect(new MobEffectInstance(
                        ModMobEffects.PARALYSIS.get(), 60, existingStacks - 1, false, false, true));
            }

            if (ModMobEffects.SPORES.isPresent() && ModMobEffects.SPORES.get() != null
                    && target.hasEffect(ModMobEffects.SPORES.get())) {
                CompoundTag targetData = target.getPersistentData();
                long gameTime = target.level().getGameTime();
                if (targetData.contains("ec_static_aura_spore_cd") && targetData.getLong("ec_static_aura_spore_cd") > gameTime) continue;

                MobEffectInstance sporeEffect = target.getEffect(ModMobEffects.SPORES.get());
                if (sporeEffect == null) continue;
                int sporeStacks = sporeEffect.getAmplifier() + 1;
                int sourceStacks = source.getPersistentData().getInt(NBT_STATIC_STACKS);
                if (sourceStacks <= 0) continue;

                if (ElementalThunderFrostReactionsConfig.staticSporeBlastBaseChance <= 0) continue;
                double totalChance = Math.min(1.0,
                        ElementalThunderFrostReactionsConfig.staticSporeBlastBaseChance
                        + sourceStacks * ElementalThunderFrostReactionsConfig.staticSporeBlastPerStaticStack
                        + sporeStacks * ElementalThunderFrostReactionsConfig.staticSporeBlastPerSporeStack);
                if (RANDOM.nextDouble() < totalChance) {
                    targetData.putLong("ec_static_aura_spore_cd", gameTime + 100);
                    ReactionHandler.triggerToxicBlast(target.level(), source, target,
                            ElementalFireNatureReactionsConfig.scorchedTriggerThreshold, source,
                            ElementalFireNatureReactionsConfig.sporeReactionThreshold);
                }
            }
        }
    }

    private static void applyStaticAuraDamage(LivingEntity source, int stacks) {
        double range = stacks * ElementalThunderFrostReactionsConfig.staticAuraBaseRange;

        AABB auraArea = new AABB(
                source.getX() - range, source.getY() - range, source.getZ() - range,
                source.getX() + range, source.getY() + range, source.getZ() + range
        );
        java.util.List<LivingEntity> nearby = source.level().getEntitiesOfClass(LivingEntity.class, auraArea);

        for (LivingEntity target : nearby) {
            if (target == source) continue;
            if (shouldSkipAuraTarget(target, source)) continue;

            double dx = target.getX() - source.getX();
            double dz = target.getZ() - source.getZ();
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            if (horizontalDist > range) continue;

            double dy = target.getY() - source.getY();
            if (dy > ElementalThunderFrostReactionsConfig.staticAuraHeightCeiling) continue;

            if (isImmuneToStatic(target)) continue;

            float auraDamage = getRandomStaticDamage(target);
            auraDamage = applyEnchantmentReduction(target, auraDamage);
            if (auraDamage > 0) {
                ElementDamageHelper.applyDamage(target, auraDamage, ModDamageTypes.source(source.level(), ModDamageTypes.STATIC_SHOCK, source));
                DebugCommand.AuraDamageLogContext actx = new DebugCommand.AuraDamageLogContext();
                actx.source = source;
                actx.target = target;
                actx.damage = auraDamage;
                actx.reactionKey = "static";
                DebugCommand.sendAuraDamageLog(actx);
                if (source.level() instanceof ServerLevel serverLevel) {
                    EffectHelper.playStaticSplashParticles(serverLevel, source, target);
                }
            }
        }
    }

    private static void clearStaticAuraEffects(LivingEntity source, int stacks) {
        double range = stacks * ElementalThunderFrostReactionsConfig.staticAuraBaseRange;

        AABB auraArea = new AABB(
                source.getX() - range, source.getY() - range, source.getZ() - range,
                source.getX() + range, source.getY() + range, source.getZ() + range
        );
        java.util.List<LivingEntity> nearby = source.level().getEntitiesOfClass(LivingEntity.class, auraArea);

        for (LivingEntity target : nearby) {
            if (target == source) continue;
            if (shouldSkipAuraTarget(target, source)) continue;

            double dx = target.getX() - source.getX();
            double dz = target.getZ() - source.getZ();
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            if (horizontalDist > range) continue;

            if (target.hasEffect(ModMobEffects.PARALYSIS.get())) {
                target.removeEffect(ModMobEffects.PARALYSIS.get());
            }
            CompoundTag targetData = target.getPersistentData();
            targetData.remove(NBT_PARALYSIS_STACKS);
            targetData.remove(NBT_PARALYSIS_TIMER);
            targetData.remove(NBT_PARALYSIS_COOLDOWN_TIMER);
        }
    }

    private static float applyEnchantmentReduction(LivingEntity entity, float damage) {
        int totalProtLevel = getTotalEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, entity);
        int totalProjectileProtLevel = getTotalEnchantmentLevel(Enchantments.PROJECTILE_PROTECTION, entity);

        double maxProtCap = ElementalThunderFrostReactionsConfig.staticMaxProtCap;
        double maxProjectileProtCap = ElementalThunderFrostReactionsConfig.staticMaxProjectileProtCap;
        double denom = ElementalFireNatureReactionsConfig.enchantmentCalculationDenominator;

        double protFactor = maxProtCap / denom;
        double projectileProtFactor = maxProjectileProtCap / denom;

        double calculatedProtRed = totalProtLevel * protFactor;
        double calculatedProjectileProtRed = totalProjectileProtLevel * projectileProtFactor;

        double actualProtRed = Math.min(calculatedProtRed, maxProtCap);
        double actualProjectileProtRed = Math.min(calculatedProjectileProtRed, maxProjectileProtCap);

        double totalReduction = Math.min(actualProtRed + actualProjectileProtRed, 1.0);

        return damage * (float) (1.0 - totalReduction);
    }

    private static void triggerStaticDamage(LivingEntity entity) {
        if (isImmuneToStatic(entity)) {
            clearStaticShock(entity);
            return;
        }
        float rawDamage = getRandomStaticDamage(entity);

        float finalDamage = applyEnchantmentReduction(entity, rawDamage);

        ElementDamageHelper.applyDamage(entity, finalDamage, ModDamageTypes.source(entity.level(), ModDamageTypes.STATIC_SHOCK));

        if (!entity.level().isClientSide) {
            float pitch = 0.8f + RANDOM.nextFloat() * 0.4f;
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.ELECTRIC_ZAP.get(), SoundSource.PLAYERS, 0.8f, pitch);
        }
        if (entity.level() instanceof ServerLevel serverLevel) {
            EffectHelper.playStaticBurst(serverLevel, entity);
        }
        tryTriggerSporeBlast(entity);
    }

    private static int getTotalEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantment ench, LivingEntity entity) {
        int total = 0;
        for (ItemStack stack : entity.getArmorSlots()) {
            total += stack.getEnchantmentLevel(ench);
        }
        return total;
    }


    private static void tryPrimingOrIgniteCreeper(LivingEntity target) {
        double chance = ElementalThunderFrostReactionsConfig.staticCreeperIgniteChance;
        if (chance <= 0) return;
        if (!(target instanceof Creeper creeper) || !creeper.isAlive()) return;

        CompoundTag data = creeper.getPersistentData();
        boolean alreadyPrimed = data.getBoolean(NBT_STATIC_PRIMED);
        boolean isLightningCharged = creeper.isPowered();

        ElementalCraft.LOGGER.info("[静电苦力怕] 判定: primed={}, powered={}, chance={}", alreadyPrimed, isLightningCharged, chance);

        if (isLightningCharged) {
            ElementalCraft.LOGGER.info("[静电苦力怕] → 高压, 执行引爆");
            if (alreadyPrimed) {
                creeper.clearFire();
            }
            data.putBoolean(NBT_STATIC_PRIMED, true);
            ScorchedHandler.igniteCreeperIfScorched(creeper);
            data.remove(NBT_STATIC_PRIMED);
            if (creeper.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.FLASH,
                    creeper.getX(), creeper.getY() + 1, creeper.getZ(), 1, 0, 0, 0, 0);
            }
        } else if (!alreadyPrimed) {
            double roll = RANDOM.nextDouble();
            ElementalCraft.LOGGER.info("[静电苦力怕] → 首次判定: roll={}, 需<{}", roll, chance);
            if (roll >= chance) {
                ElementalCraft.LOGGER.info("[静电苦力怕] → 概率未通过, 跳过");
                return;
            }
            ElementalCraft.LOGGER.info("[静电苦力怕] → 概率通过, 召唤闪电变高压(不引爆)");
            data.putBoolean(NBT_STATIC_PRIMED, true);
            if (creeper.level() instanceof ServerLevel sl) {
                net.minecraft.world.entity.LightningBolt lightning =
                    net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(sl);
                if (lightning != null) {
                    lightning.setDamage(0);
                    lightning.setPos(creeper.getX(), creeper.getY(), creeper.getZ());
                    sl.addFreshEntity(lightning);
                }
            }
        } else {
            ElementalCraft.LOGGER.info("[静电苦力怕] → 已标记未高压, 等闪电打击");
        }
    }

    public static void tryTriggerSporeBlast(LivingEntity target) {
        if (!target.hasEffect(ModMobEffects.SPORES.get())) return;
        if (ElementalThunderFrostReactionsConfig.frostbiteReduceSporesEnabled && FrostbiteHandler.hasFrostbite(target)) return;
        MobEffectInstance sporeEffect = target.getEffect(ModMobEffects.SPORES.get());
        if (sporeEffect == null) return;
        int sporeStacks = sporeEffect.getAmplifier() + 1;
        CompoundTag data = target.getPersistentData();
        int staticStacks = data.getInt(NBT_STATIC_STACKS);
        if (staticStacks <= 0) return;

        if (ElementalThunderFrostReactionsConfig.staticSporeBlastBaseChance <= 0) return;
        double baseChance = ElementalThunderFrostReactionsConfig.staticSporeBlastBaseChance;
        double perStatic = ElementalThunderFrostReactionsConfig.staticSporeBlastPerStaticStack;
        double perSpore = ElementalThunderFrostReactionsConfig.staticSporeBlastPerSporeStack;
        double totalChance = Math.min(1.0, baseChance + staticStacks * perStatic + sporeStacks * perSpore);
        boolean triggered = RANDOM.nextDouble() < totalChance;
        if (!triggered) return;

        double firePower = ElementalFireNatureReactionsConfig.scorchedTriggerThreshold;
        ReactionHandler.triggerStaticSporeBlast(target, firePower);
    }

    static float getRandomStaticDamage(LivingEntity entity) {
        double minDmg = ElementalThunderFrostReactionsConfig.staticDamageMin;
        double maxDmg = ElementalThunderFrostReactionsConfig.staticDamageMax;
        if (maxDmg < minDmg) maxDmg = minDmg;
        float damage = (float) (minDmg + RANDOM.nextDouble() * (maxDmg - minDmg));
        ElementType element = ElementUtils.getConsistentAttackElement(entity);
        if (element == ElementType.FIRE) {
            damage *= (float) ElementalThunderFrostReactionsConfig.staticDamageFireMultiplier;
        } else if (element == ElementType.THUNDER) {
            damage *= (float) ElementalThunderFrostReactionsConfig.staticDamageThunderMultiplier;
        } else if (element == ElementType.NATURE) {
            damage *= (float) ElementalThunderFrostReactionsConfig.staticDamageNatureMultiplier;
        } else if (element == ElementType.FROST) {
            damage *= (float) ElementalThunderFrostReactionsConfig.staticDamageFrostMultiplier;
        }

        return damage;
    }

    private static double calculateTriggerChance(int thunderStrength, int wetnessLevel, LivingEntity target) {
        int threshold = ElementalThunderFrostReactionsConfig.thunderStrengthThreshold;
        if (threshold <= 0 || thunderStrength < threshold) return 0.0;
        double baseChance = ElementalThunderFrostReactionsConfig.staticBaseChance;
        double scalingChance = ElementalThunderFrostReactionsConfig.staticScalingChance;
        int scalingStep = ElementalThunderFrostReactionsConfig.staticScalingStep;
        int extraStrength = thunderStrength - threshold;
        int extraSteps = extraStrength / scalingStep;
        double totalChance = baseChance + (extraSteps * scalingChance);
        if (wetnessLevel > 0) {
            double wetnessBonusChance = ElementalThunderFrostReactionsConfig.staticWetnessBonusChancePerLevel;
            totalChance += wetnessLevel * wetnessBonusChance;
        }
        if (target.hasEffect(ModMobEffects.STATIC_SHOCK.get())) {
            totalChance += ElementalThunderFrostReactionsConfig.staticStackingBonusChance;
        }
        return Math.min(totalChance, 1.0);
    }

    private static void clearStaticShock(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        data.remove(NBT_STATIC_STACKS);
        data.remove(NBT_STATIC_TIMER);
        data.remove(NBT_STATIC_DAMAGE_TIMER);
        data.remove(NBT_STATIC_AURA_DAMAGE_TIMER);
        data.remove(NBT_STATIC_PRIMED);
        if (entity.hasEffect(ModMobEffects.STATIC_SHOCK.get())) {
            entity.removeEffect(ModMobEffects.STATIC_SHOCK.get());
        }
    }

    private static void updateEffect(LivingEntity entity, int stacks, int totalTicks) {
        if (stacks <= 0) {
            clearStaticShock(entity);
            return;
        }
        int amplifier = stacks - 1;
        MobEffectInstance currentEffect = entity.getEffect(ModMobEffects.STATIC_SHOCK.get());
        if (currentEffect == null || currentEffect.getAmplifier() != amplifier || currentEffect.getDuration() != totalTicks) {
            entity.removeEffect(ModMobEffects.STATIC_SHOCK.get());
            entity.addEffect(new MobEffectInstance(
                    ModMobEffects.STATIC_SHOCK.get(), totalTicks, amplifier, false, false, true
            ));
        }
    }

    public static void triggerParalysisReaction(LivingEntity attacker, LivingEntity entity) {
        if (isImmuneToParalysis(entity)) {
            return;
        }
        if (isImmuneToStatic(entity)) {
            CompoundTag data = entity.getPersistentData();
            int auraStacks = data.getInt(NBT_STATIC_STACKS);
            if (ElementalThunderFrostReactionsConfig.staticAuraThreshold > 0 && auraStacks >= ElementalThunderFrostReactionsConfig.staticAuraThreshold) {
                clearStaticAuraEffects(entity, auraStacks);
            }
            clearStaticShock(entity);
            return;
        }

        CompoundTag data = entity.getPersistentData();
        int staticStacks = data.getInt(NBT_STATIC_STACKS);
        int totalTimer = data.getInt(NBT_STATIC_TIMER);
        if (staticStacks <= 0 || totalTimer <= 0) return;

        int cooldownRemaining = data.getInt(NBT_PARALYSIS_COOLDOWN_TIMER);
        int cooldownTicks = ElementalThunderFrostReactionsConfig.paralysisCooldownTicks;
        if (cooldownTicks > 0 && cooldownRemaining > 0) {
            DebugCommand.sendReactionCooldownBlock(entity, "paralysis", cooldownRemaining);
            if (ElementalThunderFrostReactionsConfig.staticAuraThreshold > 0 && staticStacks >= ElementalThunderFrostReactionsConfig.staticAuraThreshold) {
                clearStaticAuraEffects(entity, staticStacks);
            }
            clearStaticShock(entity);
            return;
        }

        int wetnessLevel = 0;
        if (entity.hasEffect(ModMobEffects.WETNESS.get())) {
            MobEffectInstance wetnessEffect = entity.getEffect(ModMobEffects.WETNESS.get());
            if (wetnessEffect != null) {
                wetnessLevel = wetnessEffect.getAmplifier() + 1;
            }
        }

        int maxParalysisStacks = ElementalThunderFrostReactionsConfig.paralysisMaxStacks;
        if (maxParalysisStacks <= 0) return;
        int paralysisStacks = Math.max(staticStacks, wetnessLevel);
        if (paralysisStacks > maxParalysisStacks) {
            paralysisStacks = maxParalysisStacks;
        }

        int interval = ElementalThunderFrostReactionsConfig.staticDamageIntervalTicks;
        if (interval < 1) interval = 1;
        int remainingTicks = totalTimer;
        int remainingHits = (remainingTicks + interval - 1) / interval;

        double totalDamage = 0;
        for (int i = 0; i < remainingHits; i++) {
            totalDamage += getRandomStaticDamage(entity);
        }
        totalDamage *= ElementalThunderFrostReactionsConfig.paralysisDamagePercentage;

        float finalDamage = 0;
        if (totalDamage > 0) {
            finalDamage = applyEnchantmentReduction(entity, (float) totalDamage);
            ElementDamageHelper.applyDamage(entity, finalDamage, ModDamageTypes.source(entity.level(), ModDamageTypes.STATIC_SHOCK));
        }

        if (ElementalThunderFrostReactionsConfig.staticAuraThreshold > 0 && staticStacks >= ElementalThunderFrostReactionsConfig.staticAuraThreshold) {
            clearStaticAuraEffects(entity, staticStacks);
        }
        clearStaticShock(entity);

        if (WetnessHandler.getWetnessLevel(entity) > 0) {
            WetnessHandler.clearWetnessData(entity);
        }

        int paralysisDuration = ElementalThunderFrostReactionsConfig.paralysisDurationPerStackTicks * paralysisStacks;
        entity.addEffect(new MobEffectInstance(
                ModMobEffects.PARALYSIS.get(), paralysisDuration, paralysisStacks - 1, false, false, true
        ));
        data.putInt(NBT_PARALYSIS_STACKS, paralysisStacks);
        data.putInt(NBT_PARALYSIS_TIMER, paralysisDuration);

        if (cooldownTicks > 0) {
            data.putInt(NBT_PARALYSIS_COOLDOWN_TIMER, paralysisDuration + cooldownTicks);
        }

        if (!entity.level().isClientSide) {
            entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.ELECTRIC_ZAP.get(), SoundSource.PLAYERS, 1.0f, 0.5f);
        }

        DebugCommand.ParalysisLogContext pCtx = new DebugCommand.ParalysisLogContext();
        pCtx.attacker = attacker;
        pCtx.target = entity;
        pCtx.paralysisStacks = paralysisStacks;
        pCtx.remainingHits = remainingHits;
        pCtx.totalDamage = finalDamage;
        DebugCommand.sendParalysisLog(pCtx);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onKeyPress(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && (mc.player.hasEffect(ModMobEffects.PARALYSIS.get()) || mc.player.hasEffect(ModMobEffects.FREEZE.get()))) {
            if (event.getKey() != GLFW.GLFW_KEY_ESCAPE) {
                for (KeyMapping key : mc.options.keyMappings) {
                    if (key.matches(event.getKey(), 0)) {
                        key.setDown(false);
                        break;
                    }
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && (mc.player.hasEffect(ModMobEffects.PARALYSIS.get()) || mc.player.hasEffect(ModMobEffects.FREEZE.get()))) {
            for (KeyMapping key : mc.options.keyMappings) {
                if (key.matchesMouse(event.getButton())) {
                    key.setDown(false);
                    break;
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && (mc.player.hasEffect(ModMobEffects.PARALYSIS.get()) || mc.player.hasEffect(ModMobEffects.FREEZE.get()))) {
            event.setCanceled(true);
        }
    }
}
