package com.xulai.elementalcraft.potion;

import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.util.ElementUtils;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.EffectHelper;
import com.xulai.elementalcraft.util.GlobalDebugLogger;
import com.xulai.elementalcraft.util.DebugMode;
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
    private static final String NBT_ORIGINAL_NO_AI = "EC_OriginalNoAI";
    private static final String NBT_AI_DISABLED = "EC_AIDisabled";

    public ParalysisEffect() {
        super(MobEffectCategory.HARMFUL, 0x808080);
    }

    @Override
    public void applyEffectTick(LivingEntity pLivingEntity, int pAmplifier) {
        Debug.logTick(pLivingEntity, pAmplifier);
        if (!pLivingEntity.level().isClientSide) {
            disableAI(pLivingEntity);

            if (pLivingEntity.hasEffect(ModMobEffects.WETNESS.get())) {
                pLivingEntity.removeEffect(ModMobEffects.WETNESS.get());
                Debug.logRemoveWetness(pLivingEntity);
            }

            checkAndSpreadStaticShock(pLivingEntity, pAmplifier);
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, AttributeMap pAttributeMap, int pAmplifier) {
        super.removeAttributeModifiers(entity, pAttributeMap, pAmplifier);
        restoreAI(entity);
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
        Debug.logAIDisabled(entity);
    }

    private static void restoreAI(LivingEntity entity) {
        if (!(entity instanceof Mob mob)) return;
        CompoundTag data = entity.getPersistentData();
        if (!data.getBoolean(NBT_AI_DISABLED)) return;
        boolean wasNoAi = data.getBoolean(NBT_ORIGINAL_NO_AI);
        mob.setNoAi(wasNoAi);
        data.remove(NBT_ORIGINAL_NO_AI);
        data.remove(NBT_AI_DISABLED);
        Debug.logAIRestored(entity);
    }

    private void checkAndSpreadStaticShock(LivingEntity entity, int amplifier) {
        CompoundTag data = entity.getPersistentData();
        if (data.getBoolean(NBT_HAS_SPREAD)) {
            Debug.logSpreadAlreadyDone(entity);
            return;
        }
        int cooldown = data.getInt(NBT_SPREAD_COOLDOWN);
        if (cooldown > 0) {
            data.putInt(NBT_SPREAD_COOLDOWN, cooldown - 1);
            Debug.logSpreadCooldown(entity, cooldown - 1);
            return;
        }
        int paralysisStacks = amplifier + 1;
        int maxParalysisStacks = ElementalThunderFrostReactionsConfig.paralysisMaxStacks;
        double thresholdPercentage = ElementalThunderFrostReactionsConfig.paralysisSpreadThresholdPercentage;
        int thresholdStacks = (int) Math.ceil(maxParalysisStacks * thresholdPercentage);
        Debug.logSpreadCheck(entity, paralysisStacks, thresholdStacks);
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
        double paralysisPercentage = ElementalThunderFrostReactionsConfig.paralysisSpreadStaticPercentage;
        int spreadParalysisStacks = (int) Math.ceil(paralysisStacks * paralysisPercentage);
        if (spreadParalysisStacks < 1) {
            spreadParalysisStacks = 1;
        }
        int maxParalysis = ElementalThunderFrostReactionsConfig.paralysisMaxStacks;
        if (spreadParalysisStacks > maxParalysis) {
            spreadParalysisStacks = maxParalysis;
        }
        boolean spreadOccurred = false;
        List<LivingEntity> affectedTargets = new ArrayList<>();
        Debug.logSpreadStart(entity, spreadRange, paralysisStacks, spreadParalysisStacks, nearbyEntities.size());
        for (LivingEntity target : nearbyEntities) {
            if (target == entity) {
                continue;
            }
            if (ElementalThunderFrostReactionsConfig.paralysisSpreadExcludePlayers && target instanceof Player) {
                Debug.logSpreadExclude(target, "玩家");
                continue;
            }
            if (ElementalThunderFrostReactionsConfig.paralysisSpreadExcludePets && target instanceof TamableAnimal) {
                TamableAnimal pet = (TamableAnimal) target;
                if (pet.isTame() && pet.getOwner() != null) {
                    Debug.logSpreadExclude(target, "宠物");
                    continue;
                }
            }
            if (target.hasEffect(ModMobEffects.PARALYSIS.get())) {
                Debug.logSpreadExclude(target, "已有麻痹");
                continue;
            }
            if (!target.hasEffect(ModMobEffects.WETNESS.get())) {
                Debug.logSpreadExclude(target, "没有潮湿");
                continue;
            }
            int targetThunderResist = ElementUtils.getDisplayResistance(target, ElementType.THUNDER);
            int immunityThreshold = ElementalThunderFrostReactionsConfig.staticResistImmunityThreshold;
            if (targetThunderResist >= immunityThreshold) {
                Debug.logSpreadExclude(target, "雷霆免疫");
                continue;
            }
            if (!ElementalThunderFrostReactionsConfig.paralysisSpreadAllowChain) {
                CompoundTag targetData = target.getPersistentData();
                if (targetData.getBoolean(NBT_HAS_SPREAD)) {
                    Debug.logSpreadExclude(target, "已传染过");
                    continue;
                }
            }
            applyParalysisToTarget(target, spreadParalysisStacks);
            spreadOccurred = true;
            affectedTargets.add(target);
            Debug.logSpreadInfect(target, spreadParalysisStacks);
        }
        if (spreadOccurred) {
            if (entity.level() instanceof ServerLevel) {
                EffectHelper.playParalysisSpread(entity, affectedTargets, spreadRange);
            }
            data.putBoolean(NBT_HAS_SPREAD, true);
            data.putInt(NBT_SPREAD_COOLDOWN, 20);
            Debug.logSpreadSuccess(entity, affectedTargets.size());
        } else {
            Debug.logSpreadNoTarget(entity);
        }
    }

    private void applyParalysisToTarget(LivingEntity target, int paralysisStacks) {
        Debug.logApplyToTarget(target, paralysisStacks);
        if (target.hasEffect(ModMobEffects.WETNESS.get())) {
            target.removeEffect(ModMobEffects.WETNESS.get());
        }
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
        CompoundTag data = target.getPersistentData();
        data.putInt(NBT_PARALYSIS_STACKS, paralysisStacks);
        data.putInt(NBT_PARALYSIS_TIMER, totalDuration);
        if (!ElementalThunderFrostReactionsConfig.paralysisSpreadAllowChain) {
            data.putBoolean(NBT_HAS_SPREAD, true);
        }
    }

    // ==================== 调试内部类 ====================
    private static final class Debug {
        private static void logTick(LivingEntity entity, int amplifier) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(entity.level(), "麻痹", String.format("%s tick：层数 %d", entity.getName().getString(), amplifier + 1));
        }

        private static void logRemoveWetness(LivingEntity entity) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(entity.level(), "麻痹", String.format("%s 自动移除潮湿", entity.getName().getString()));
        }

        private static void logAIDisabled(LivingEntity entity) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(entity.level(), "麻痹", String.format("%s AI 已禁用", entity.getName().getString()));
        }

        private static void logAIRestored(LivingEntity entity) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(entity.level(), "麻痹", String.format("%s AI 已恢复", entity.getName().getString()));
        }

        private static void logSpreadAlreadyDone(LivingEntity entity) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(entity.level(), "麻痹传染", String.format("%s 已传染过，跳过", entity.getName().getString()));
        }

        private static void logSpreadCooldown(LivingEntity entity, int remaining) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(entity.level(), "麻痹传染", String.format("%s 传染冷却剩余 %d 刻", entity.getName().getString(), remaining));
        }

        private static void logSpreadCheck(LivingEntity entity, int current, int threshold) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(entity.level(), "麻痹传染", String.format("%s 层数 %d，阈值 %d", entity.getName().getString(), current, threshold));
        }

        private static void logSpreadStart(LivingEntity entity, int range, int paralysisStacks, int spreadStacks, int totalNearby) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(entity.level(), "麻痹传染", String.format("%s 开始传染：范围 %d，源麻痹层数 %d，传染麻痹层数 %d，附近实体 %d",
                    entity.getName().getString(), range, paralysisStacks, spreadStacks, totalNearby));
        }

        private static void logSpreadExclude(LivingEntity target, String reason) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "麻痹传染", String.format("排除 %s：%s", target.getName().getString(), reason));
        }

        private static void logSpreadInfect(LivingEntity target, int stacks) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "麻痹传染", String.format("%s 被感染，获得 %d 层麻痹", target.getName().getString(), stacks));
        }

        private static void logSpreadSuccess(LivingEntity source, int count) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(source.level(), "麻痹传染", String.format("%s 成功感染 %d 个目标", source.getName().getString(), count));
        }

        private static void logSpreadNoTarget(LivingEntity source) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(source.level(), "麻痹传染", String.format("%s 没有可感染的目标", source.getName().getString()));
        }

        private static void logApplyToTarget(LivingEntity target, int stacks) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "麻痹传染", String.format("对 %s 施加麻痹 %d 层", target.getName().getString(), stacks));
        }
    }
}
