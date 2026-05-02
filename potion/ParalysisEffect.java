package com.xulai.elementalcraft.potion;

import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.event.WetnessHandler;
import com.xulai.elementalcraft.potion.ModMobEffects;
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
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ParalysisEffect extends MobEffect {
    private static final String NBT_SPREAD_COOLDOWN = "EC_ParalysisSpreadCooldown";
    private static final String NBT_STATIC_STACKS = "EC_StaticStacks";
    private static final String NBT_STATIC_TIMER = "EC_StaticTimer";
    private static final String NBT_STATIC_DAMAGE_TIMER = "EC_StaticDamageTimer";
    private static final String NBT_PARALYSIS_STACKS = "EC_ParalysisStacks";
    private static final String NBT_PARALYSIS_TIMER = "EC_ParalysisTimer";
    private static final String NBT_ORIGINAL_NO_AI = "EC_OriginalNoAI";
    private static final String NBT_AI_DISABLED = "EC_AIDisabled";

    private static final String NBT_SPREADED = "EC_ParalysisSpreaded";
    private static final String NBT_INFECTED = "EC_ParalysisInfected";
    private static final String NBT_CONTAGION_SOURCE = "EC_ParalysisContagionSource";

    public ParalysisEffect() {
        super(MobEffectCategory.HARMFUL, 0x808080);
    }

    @Override
    public void applyEffectTick(LivingEntity pLivingEntity, int pAmplifier) {
        if (!pLivingEntity.level().isClientSide) {
            disableAI(pLivingEntity);

            if (pLivingEntity.hasEffect(ModMobEffects.WETNESS.get())) {
                pLivingEntity.removeEffect(ModMobEffects.WETNESS.get());
            }

            checkAndSpreadStaticShock(pLivingEntity, pAmplifier);
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap pAttributeMap, int pAmplifier) {
        super.removeAttributeModifiers(entity, pAttributeMap, pAmplifier);
        restoreAI(entity);
        resetContagionFlags(entity);
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

    @Override
    public boolean isDurationEffectTick(int pDuration, int pAmplifier) {
        return true;
    }

    private static void disableAI(LivingEntity entity) {
        if (!(entity instanceof Mob mob)) return;
        CompoundTag data = entity.getPersistentData();
        if (data.getBoolean(NBT_AI_DISABLED)) return;
        data.putBoolean(NBT_ORIGINAL_NO_AI, mob.isNoAi());
        mob.setNoAi(true);
        data.putBoolean(NBT_AI_DISABLED, true);
    }

    private static void restoreAI(LivingEntity entity) {
        if (!(entity instanceof Mob mob)) return;
        CompoundTag data = entity.getPersistentData();
        if (!data.getBoolean(NBT_AI_DISABLED)) return;
        boolean wasNoAi = data.getBoolean(NBT_ORIGINAL_NO_AI);
        mob.setNoAi(wasNoAi);
        data.remove(NBT_ORIGINAL_NO_AI);
        data.remove(NBT_AI_DISABLED);
    }

    private void checkAndSpreadStaticShock(LivingEntity entity, int amplifier) {
        CompoundTag data = entity.getPersistentData();

        int cooldown = data.getInt(NBT_SPREAD_COOLDOWN);
        if (cooldown > 0) {
            data.putInt(NBT_SPREAD_COOLDOWN, cooldown - 1);
            return;
        }

        int paralysisStacks = amplifier + 1;
        int thresholdStacks = ElementalThunderFrostReactionsConfig.paralysisSpreadThresholdStacks;
        if (paralysisStacks < thresholdStacks) {
            return;
        }

        boolean isSpreaded = data.getBoolean(NBT_SPREADED);
        boolean isInfected = data.getBoolean(NBT_INFECTED);
        boolean blockByInfected = isInfected && !ElementalThunderFrostReactionsConfig.paralysisSpreadAllowChain;

        if (isSpreaded || blockByInfected) {
            return;
        }

        int baseRange = ElementalThunderFrostReactionsConfig.paralysisSpreadBaseRange;
        int extraStacks = paralysisStacks - thresholdStacks;
        int rangePerExtraStack = ElementalThunderFrostReactionsConfig.paralysisSpreadRangePerExtraStack;
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

            if (target.hasEffect(ModMobEffects.PARALYSIS.get())) {
                continue;
            }

            if (target.getPersistentData().getInt("ec_paralysis_cooldown_timer") > 0) {
                continue;
            }

            if (!target.hasEffect(ModMobEffects.WETNESS.get())) {
                continue;
            }

            if (contagionSourceUUID != null && target.getUUID().equals(contagionSourceUUID)) {
                if (!ElementalThunderFrostReactionsConfig.paralysisSpreadAllowToSource) {
                    continue;
                }
            }

            if (ElementalThunderFrostReactionsConfig.paralysisSpreadExcludeFriendlyEntities) {
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

            if (ElementalThunderFrostReactionsConfig.paralysisSpreadOnlyHostile) {
                if (!(target instanceof Enemy)) {
                    continue;
                }
            }

            int targetThunderResist = ElementUtils.getDisplayResistance(target, ElementType.THUNDER);
            int immunityThreshold = ElementalThunderFrostReactionsConfig.staticResistImmunityThreshold;
            if (targetThunderResist >= immunityThreshold) {
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
            applyParalysisToTarget(target, wetnessLevel);
            infectedTargets.add(target);
        }

        if (entity.level() instanceof ServerLevel) {
            EffectHelper.playParalysisSpread(entity, infectedTargets, spreadRange);
        }

        data.putInt(NBT_SPREAD_COOLDOWN, 20);
    }

    private void applyParalysisToTarget(LivingEntity target, int paralysisStacks) {
        CompoundTag data = target.getPersistentData();

        int cooldown = data.getInt("ec_paralysis_cooldown_timer");
        if (cooldown > 0) {
            return;
        }

        int maxStacks = ElementalThunderFrostReactionsConfig.paralysisMaxStacks;
        if (paralysisStacks > maxStacks) {
            paralysisStacks = maxStacks;
        }
        WetnessHandler.clearWetnessData(target);
        int durationPerStack = ElementalThunderFrostReactionsConfig.paralysisDurationPerStackTicks;
        int totalDuration = paralysisStacks * durationPerStack;
        if (totalDuration < 20) {
            totalDuration = 20;
        }
        target.addEffect(new MobEffectInstance(
            ModMobEffects.PARALYSIS.get(),
            totalDuration,
            paralysisStacks - 1,
            false,
            false,
            true
        ));
        data.putInt(NBT_PARALYSIS_STACKS, paralysisStacks);
        data.putInt(NBT_PARALYSIS_TIMER, totalDuration);

        int cooldownTicks = ElementalThunderFrostReactionsConfig.paralysisCooldownTicks;
        if (cooldownTicks > 0) {
            data.putInt("ec_paralysis_cooldown_timer", totalDuration + cooldownTicks);
        }
    }
}