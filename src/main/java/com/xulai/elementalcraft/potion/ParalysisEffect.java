package com.xulai.elementalcraft.potion;

import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
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
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.AABB;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ParalysisEffect extends MobEffect {
    private static final Random RANDOM = new Random();
    private static final String NBT_SPREAD_COOLDOWN = "EC_ParalysisSpreadCooldown";
    private static final String NBT_HAS_SPREAD = "EC_HasSpreadStatic";
    private static final String NBT_STATIC_STACKS = "EC_StaticStacks";
    private static final String NBT_STATIC_TIMER = "EC_StaticTimer";
    private static final String NBT_STATIC_DAMAGE_TIMER = "EC_StaticDamageTimer";
    private static final String NBT_PARALYSIS_STACKS = "EC_ParalysisStacks";
    private static final String NBT_PARALYSIS_TIMER = "EC_ParalysisTimer";

    public ParalysisEffect() {
        super(MobEffectCategory.HARMFUL, 0x808080);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, "7107DE5E-7CE8-4030-940E-514C1F160890", -0.99, AttributeModifier.Operation.MULTIPLY_TOTAL);
        this.addAttributeModifier(Attributes.ATTACK_SPEED, "AF8B6E3F-3328-42ED-97BC-FBE9D9B99A20", -0.99, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public void applyEffectTick(LivingEntity pLivingEntity, int pAmplifier) {
        if (!pLivingEntity.level().isClientSide) {
            pLivingEntity.setJumping(false);
            pLivingEntity.setShiftKeyDown(false);
            pLivingEntity.setDeltaMovement(0, pLivingEntity.getDeltaMovement().y, 0);
            // 取消游泳状态
            if (pLivingEntity.isSwimming()) {
                pLivingEntity.setSwimming(false);
            }
            checkAndSpreadStaticShock(pLivingEntity, pAmplifier);
        }
    }

    @Override
    public boolean isDurationEffectTick(int pDuration, int pAmplifier) {
        return true;
    }

    private void checkAndSpreadStaticShock(LivingEntity entity, int amplifier) {
        CompoundTag data = entity.getPersistentData();
        if (data.getBoolean(NBT_HAS_SPREAD)) {
            return;
        }
        int cooldown = data.getInt(NBT_SPREAD_COOLDOWN);
        if (cooldown > 0) {
            data.putInt(NBT_SPREAD_COOLDOWN, cooldown - 1);
            return;
        }
        int paralysisStacks = amplifier + 1;
        int maxParalysisStacks = ElementalThunderFrostReactionsConfig.paralysisMaxStacks;
        double thresholdPercentage = ElementalThunderFrostReactionsConfig.paralysisSpreadThresholdPercentage;
        int thresholdStacks = (int) Math.ceil(maxParalysisStacks * thresholdPercentage);
        if (paralysisStacks < thresholdStacks) {
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
        List<LivingEntity> nearbyEntities = entity.level().getEntitiesOfClass(LivingEntity.class, area);
        double staticPercentage = ElementalThunderFrostReactionsConfig.paralysisSpreadStaticPercentage;
        int staticStacks = (int) Math.ceil(paralysisStacks * staticPercentage);
        if (staticStacks < 1) {
            staticStacks = 1;
        }
        int maxStaticStacks = ElementalThunderFrostReactionsConfig.staticMaxTotalStacks;
        if (staticStacks > maxStaticStacks) {
            staticStacks = maxStaticStacks;
        }
        boolean spreadOccurred = false;
        List<LivingEntity> affectedTargets = new ArrayList<>();
        for (LivingEntity target : nearbyEntities) {
            if (target == entity) {
                continue;
            }
            if (ElementalThunderFrostReactionsConfig.paralysisSpreadExcludePlayers && target instanceof Player) {
                continue;
            }
            if (ElementalThunderFrostReactionsConfig.paralysisSpreadExcludePets && target instanceof TamableAnimal) {
                TamableAnimal pet = (TamableAnimal) target;
                if (pet.isTame() && pet.getOwner() != null) {
                    continue;
                }
            }
            if (target.hasEffect(ModMobEffects.STATIC_SHOCK.get())) {
                continue;
            }
            int targetThunderResist = ElementUtils.getDisplayResistance(target, ElementType.THUNDER);
            int immunityThreshold = ElementalThunderFrostReactionsConfig.staticResistImmunityThreshold;
            if (targetThunderResist >= immunityThreshold) {
                continue;
            }
            if (!ElementalThunderFrostReactionsConfig.paralysisSpreadAllowChain) {
                CompoundTag targetData = target.getPersistentData();
                if (targetData.getBoolean(NBT_HAS_SPREAD)) {
                    continue;
                }
            }
            applyStaticShockToTarget(target, staticStacks);
            spreadOccurred = true;
            affectedTargets.add(target);
        }
        if (spreadOccurred) {
            if (entity.level() instanceof ServerLevel) {
                EffectHelper.playParalysisSpread(entity, affectedTargets, spreadRange);
            }
            data.putBoolean(NBT_HAS_SPREAD, true);
            data.putInt(NBT_SPREAD_COOLDOWN, 20);
        }
    }

    private void applyStaticShockToTarget(LivingEntity target, int staticStacks) {
        boolean hasWetnessEffect = target.hasEffect(ModMobEffects.WETNESS.get());
        if (hasWetnessEffect) {
            triggerParalysisReactionFromSpread(target, staticStacks);
        } else {
            applyStaticShockEffect(target, staticStacks);
        }
        if (!ElementalThunderFrostReactionsConfig.paralysisSpreadAllowChain) {
            CompoundTag targetData = target.getPersistentData();
            targetData.putBoolean(NBT_HAS_SPREAD, true);
        }
    }

    private void applyStaticShockEffect(LivingEntity target, int staticStacks) {
        int durationPerStack = ElementalThunderFrostReactionsConfig.staticDurationPerStackTicks;
        int totalDuration = staticStacks * durationPerStack;
        if (totalDuration < 20) {
            totalDuration = 20;
        }
        target.addEffect(new MobEffectInstance(
            ModMobEffects.STATIC_SHOCK.get(),
            totalDuration,
            staticStacks - 1,
            false,
            false,
            true
        ));
        CompoundTag data = target.getPersistentData();
        data.putInt(NBT_STATIC_STACKS, staticStacks);
        data.putInt(NBT_STATIC_TIMER, totalDuration);
        data.putInt(NBT_STATIC_DAMAGE_TIMER, 0);
    }

    private void triggerParalysisReactionFromSpread(LivingEntity target, int staticStacks) {
        int wetnessLevel = 0;
        MobEffectInstance wetnessEffect = target.getEffect(ModMobEffects.WETNESS.get());
        if (wetnessEffect != null) {
            wetnessLevel = wetnessEffect.getAmplifier() + 1;
        }
        int paralysisStacks = staticStacks + wetnessLevel;
        int maxParalysisStacks = ElementalThunderFrostReactionsConfig.paralysisMaxStacks;
        if (paralysisStacks > maxParalysisStacks) {
            paralysisStacks = maxParalysisStacks;
        }

        // 计算剩余时间内能触发的静电伤害次数
        int durationPerStack = ElementalThunderFrostReactionsConfig.staticDurationPerStackTicks;
        int totalTimer = staticStacks * durationPerStack;
        int interval = ElementalThunderFrostReactionsConfig.staticDamageIntervalTicks;
        if (interval < 1) interval = 1;
        int remainingHits = (totalTimer + interval - 1) / interval; // 向上取整

        double minDmg = ElementalThunderFrostReactionsConfig.staticDamageMin;
        double maxDmg = ElementalThunderFrostReactionsConfig.staticDamageMax;
        if (maxDmg < minDmg) maxDmg = minDmg;

        double totalDamage = 0;
        for (int i = 0; i < remainingHits; i++) {
            double damage = minDmg + RANDOM.nextDouble() * (maxDmg - minDmg);
            // 应用元素属性伤害修正
            ElementType element = ElementUtils.getElementType(target);
            if (element == ElementType.NATURE) {
                damage *= ElementalThunderFrostReactionsConfig.staticDamageNatureMultiplier;
            } else if (element == ElementType.FROST) {
                damage *= ElementalThunderFrostReactionsConfig.staticDamageFrostMultiplier;
            }
            totalDamage += damage;
        }
        totalDamage *= 0.5; 

        if (totalDamage > 0) {
            target.hurt(target.damageSources().magic(), (float) totalDamage);
        }

        target.removeEffect(ModMobEffects.WETNESS.get());

        int paralysisDuration = ElementalThunderFrostReactionsConfig.paralysisDurationPerStackTicks * paralysisStacks;
        target.addEffect(new MobEffectInstance(
            ModMobEffects.PARALYSIS.get(),
            paralysisDuration,
            paralysisStacks - 1,
            false,
            false,
            true
        ));
        CompoundTag data = target.getPersistentData();
        data.putInt(NBT_PARALYSIS_STACKS, paralysisStacks);
        data.putInt(NBT_PARALYSIS_TIMER, paralysisDuration);
    }
}