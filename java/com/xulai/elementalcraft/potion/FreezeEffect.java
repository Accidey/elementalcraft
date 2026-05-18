package com.xulai.elementalcraft.potion;

import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.event.FrostbiteHandler;
import com.xulai.elementalcraft.event.WetnessHandler;
import com.xulai.elementalcraft.util.ElementUtils;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.EffectHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FreezeEffect extends MobEffect {

    private static final String NBT_SPREAD_COOLDOWN = "EC_FreezeSpreadCooldown";
    private static final String NBT_SPREADED = "EC_FreezeSpreaded";
    private static final String NBT_INFECTED = "EC_FreezeInfected";
    private static final String NBT_CONTAGION_SOURCE = "EC_FreezeContagionSource";

    public FreezeEffect() {
        super(MobEffectCategory.HARMFUL, 0x00BFFF);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide) {
            disableAI(entity);
            checkAndSpreadFreeze(entity, amplifier);

            if (entity.isInWater()) {
                entity.move(MoverType.SELF, new Vec3(0, -0.1, 0));
                entity.setDeltaMovement(0, 0, 0);

                CompoundTag data = entity.getPersistentData();
                int drownTimer = data.getInt("EC_DrownTimer") + 1;
                if (drownTimer >= 20) {
                    drownTimer = 0;
                    entity.hurt(entity.damageSources().drown(), 2.0F);
                }
                data.putInt("EC_DrownTimer", drownTimer);
            } else {
                entity.setDeltaMovement(0, entity.getDeltaMovement().y, 0);
                entity.getPersistentData().remove("EC_DrownTimer");
            }
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap pAttributeMap, int pAmplifier) {
        super.removeAttributeModifiers(entity, pAttributeMap, pAmplifier);
        restoreAI(entity);
        resetContagionFlags(entity);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }

    private static void resetContagionFlags(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
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

    private static final String NBT_SHARED_ORIGINAL_NO_AI = "EC_SharedOriginalNoAI";

    private static void disableAI(LivingEntity entity) {
        if (!(entity instanceof Mob mob)) return;
        CompoundTag data = entity.getPersistentData();
        if (data.getBoolean(FrostbiteHandler.NBT_FREEZE_AI_DISABLED)) return;
        if (!data.contains(NBT_SHARED_ORIGINAL_NO_AI)) {
            data.putBoolean(NBT_SHARED_ORIGINAL_NO_AI, mob.isNoAi());
        }
        mob.setNoAi(true);
        data.putBoolean(FrostbiteHandler.NBT_FREEZE_AI_DISABLED, true);
    }

    private static void restoreAI(LivingEntity entity) {
        if (!(entity instanceof Mob mob)) return;
        CompoundTag data = entity.getPersistentData();
        if (!data.getBoolean(FrostbiteHandler.NBT_FREEZE_AI_DISABLED)) return;
        data.remove(FrostbiteHandler.NBT_FREEZE_AI_DISABLED);
        if (entity.hasEffect(ModMobEffects.PARALYSIS.get())) return;
        boolean wasNoAi = data.getBoolean(NBT_SHARED_ORIGINAL_NO_AI);
        mob.setNoAi(wasNoAi);
        data.remove(NBT_SHARED_ORIGINAL_NO_AI);
    }

    private void checkAndSpreadFreeze(LivingEntity entity, int amplifier) {
        CompoundTag data = entity.getPersistentData();

        int cooldown = data.getInt(NBT_SPREAD_COOLDOWN);
        if (cooldown > 0) {
            data.putInt(NBT_SPREAD_COOLDOWN, cooldown - 1);
            return;
        }

        int freezeStacks = amplifier + 1;
        int thresholdStacks = ElementalThunderFrostReactionsConfig.freezeSpreadThresholdStacks;
        if (freezeStacks < thresholdStacks) {
            return;
        }

        boolean isSpreaded = data.getBoolean(NBT_SPREADED);
        boolean isInfected = data.getBoolean(NBT_INFECTED);
        boolean blockByInfected = isInfected && !ElementalThunderFrostReactionsConfig.freezeSpreadAllowChain;

        if (isSpreaded || blockByInfected) {
            return;
        }

        int baseRange = ElementalThunderFrostReactionsConfig.freezeSpreadBaseRange;
        int extraStacks = freezeStacks - thresholdStacks;
        int rangePerExtraStack = ElementalThunderFrostReactionsConfig.freezeSpreadRangePerExtraStack;
        int spreadRange = baseRange + (extraStacks * rangePerExtraStack);
        if (spreadRange < 1) {
            spreadRange = 1;
        }

        AABB area = new AABB(
            entity.getX() - spreadRange, entity.getY() - spreadRange, entity.getZ() - spreadRange,
            entity.getX() + spreadRange, entity.getY() + spreadRange, entity.getZ() + spreadRange
        );
        List<LivingEntity> allNearby = entity.level().getEntitiesOfClass(LivingEntity.class, area);
        List<LivingEntity> validTargets = new ArrayList<>();

        UUID contagionSourceUUID = null;
        if (data.contains(NBT_CONTAGION_SOURCE)) {
            try {
                contagionSourceUUID = data.getUUID(NBT_CONTAGION_SOURCE);
            } catch (Exception ignored) {}
        }

        for (LivingEntity target : allNearby) {
            if (target == entity) {
                continue;
            }

            if (target.hasEffect(ModMobEffects.FREEZE.get())) {
                continue;
            }

            if (FrostbiteHandler.isOnFreezeCooldown(target)) {
                continue;
            }

            if (!target.hasEffect(ModMobEffects.WETNESS.get())) {
                continue;
            }

            String entityId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).toString();
            if (ElementalThunderFrostReactionsConfig.cachedFreezeImmunityBlacklist.contains(entityId)) {
                continue;
            }

            if (contagionSourceUUID != null && target.getUUID().equals(contagionSourceUUID)) {
                if (!ElementalThunderFrostReactionsConfig.freezeSpreadAllowToSource) {
                    continue;
                }
            }

            if (ElementalThunderFrostReactionsConfig.freezeSpreadExcludeFriendlyEntities) {
                if (target instanceof Player) {
                    continue;
                }
                if (target instanceof TamableAnimal pet && pet.isTame() && pet.getOwner() != null) {
                    continue;
                }
                if (target instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) {
                    continue;
                }
                if (target instanceof AbstractHorse horse && horse.getOwnerUUID() != null) {
                    continue;
                }
            }

            if (ElementalThunderFrostReactionsConfig.freezeSpreadOnlyHostile) {
                if (!(target instanceof Enemy)) {
                    continue;
                }
            }

            int targetFrostResist = ElementUtils.getDisplayResistance(target, ElementType.FROST);
            int immunityThreshold = ElementalThunderFrostReactionsConfig.frostbiteResistImmunityThreshold;
            if (targetFrostResist >= immunityThreshold) {
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
            targetData.putUUID(NBT_CONTAGION_SOURCE, entity.getUUID());
            targetData.putBoolean(NBT_INFECTED, true);

            int wetnessLevel = WetnessHandler.getWetnessLevel(target);
            applyFreezeToTarget(target, wetnessLevel);
            infectedTargets.add(target);
        }

        if (entity.level() instanceof ServerLevel) {
            EffectHelper.playFreezeSpread(entity, infectedTargets, spreadRange);
        }

        DebugCommand.FreezeSpreadLogContext fctx = new DebugCommand.FreezeSpreadLogContext();
        fctx.source = entity;
        fctx.sourceStacks = freezeStacks;
        fctx.spreadRange = spreadRange;
        fctx.affectedCount = validTargets.size();
        DebugCommand.sendFreezeSpreadLog(fctx);

        data.putInt(NBT_SPREAD_COOLDOWN, 20);
    }

    private void applyFreezeToTarget(LivingEntity target, int freezeStacks) {
        CompoundTag data = target.getPersistentData();

        if (FrostbiteHandler.isOnFreezeCooldown(target)) {
            return;
        }

        int maxStacks = ElementalThunderFrostReactionsConfig.freezeMaxStacks;
        if (freezeStacks > maxStacks) {
            freezeStacks = maxStacks;
        }
        int durationPerStack = ElementalThunderFrostReactionsConfig.freezeDurationPerStackTicks;
        int totalDuration = freezeStacks * durationPerStack;
        if (totalDuration < 20) {
            totalDuration = 20;
        }
        target.addEffect(new MobEffectInstance(
            ModMobEffects.FREEZE.get(),
            totalDuration,
            freezeStacks - 1,
            false,
            false,
            true
        ));

        long gameTime = target.level().getGameTime();
        int cooldownTicks = ElementalThunderFrostReactionsConfig.freezeCooldownTicks;
        data.putLong(FrostbiteHandler.NBT_FREEZE_COOLDOWN, gameTime + totalDuration + cooldownTicks);
    }
}
