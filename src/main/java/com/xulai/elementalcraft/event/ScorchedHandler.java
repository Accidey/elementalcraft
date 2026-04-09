package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import com.xulai.elementalcraft.util.GlobalDebugLogger;
import com.xulai.elementalcraft.util.DebugMode;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class ScorchedHandler {
    public static final String NBT_SCORCHED_TICKS = "ec_scorched_ticks";
    public static final String NBT_SCORCHED_STRENGTH = "ec_scorched_str";
    public static final String NBT_SCORCHED_SOURCE_FIRE_POWER = "EC_ScorchedSourceFirePower";
    public static final String NBT_ATTACKER_SCORCHED_COOLDOWN = "ec_scorched_attacker_cd";

    public static void applyScorched(LivingEntity target, LivingEntity attacker, int fireStrength, int duration, int sourceFirePower) {
        if (target.level().isClientSide) return;
        var key = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (key == null) return;
        String entityId = key.toString();
        var blacklist = ElementalFireNatureReactionsConfig.cachedScorchedBlacklist;
        if (blacklist != null && blacklist.contains(entityId)) {
            Debug.logApplyBlacklisted(target, entityId);
            return;
        }
        if (attacker != null) {
            CompoundTag attackerData = attacker.getPersistentData();
            long gameTime = target.level().getGameTime();
            if (attackerData.contains(NBT_ATTACKER_SCORCHED_COOLDOWN)) {
                long cd = attackerData.getLong(NBT_ATTACKER_SCORCHED_COOLDOWN);
                if (gameTime < cd) {
                    Debug.logAttackerCooldown(attacker, target, cd - gameTime);
                    return;
                }
            }
        }
        CompoundTag targetData = target.getPersistentData();
        if (targetData.contains(NBT_SCORCHED_TICKS) && targetData.getInt(NBT_SCORCHED_TICKS) > 0) {
            Debug.logApplyAlreadyScorched(target, targetData.getInt(NBT_SCORCHED_TICKS));
            return;
        }

        int adjustedDuration = duration;
        boolean isNature = isNatureAligned(target);
        boolean isFrost = isFrostAligned(target);

        if (isNature) {
            double multiplier = ElementalFireNatureReactionsConfig.scorchedNatureDurationMultiplier;
            adjustedDuration = (int) Math.round(duration * multiplier);
            Debug.logDurationModifier(target, "自然", multiplier, adjustedDuration);
        } else if (isFrost) {
            double multiplier = ElementalFireNatureReactionsConfig.scorchedFrostDurationMultiplier;
            adjustedDuration = (int) Math.round(duration * multiplier);
            Debug.logDurationModifier(target, "冰霜", multiplier, adjustedDuration);
        }

        if (adjustedDuration < 1) adjustedDuration = 1;
        long gameTime = target.level().getGameTime();
        targetData.putInt(NBT_SCORCHED_TICKS, adjustedDuration);
        targetData.putInt(NBT_SCORCHED_STRENGTH, fireStrength);
        targetData.putInt(NBT_SCORCHED_SOURCE_FIRE_POWER, sourceFirePower);

        if (attacker != null) {
            CompoundTag attackerData = attacker.getPersistentData();
            attackerData.putLong(NBT_ATTACKER_SCORCHED_COOLDOWN, gameTime + ElementalFireNatureReactionsConfig.scorchedCooldown);
        }

        target.setRemainingFireTicks(adjustedDuration);
        Debug.logApplySuccess(target, attacker, fireStrength, adjustedDuration, sourceFirePower, isNature, isFrost);

        if (target.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LAVA, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 5, 0.2, 0.2, 0.2, 0.0);
        }
    }

    private static boolean isNatureAligned(LivingEntity entity) {
        return ElementUtils.getConsistentAttackElement(entity) == ElementType.NATURE;
    }

    private static boolean isFrostAligned(LivingEntity entity) {
        return ElementUtils.getConsistentAttackElement(entity) == ElementType.FROST;
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

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
            entity.clearFire();
            data.remove(NBT_SCORCHED_TICKS);
            data.remove(NBT_SCORCHED_STRENGTH);
            data.remove(NBT_SCORCHED_SOURCE_FIRE_POWER);
            Debug.logTickExpired(entity);
            return;
        }

        int resistPoints = ElementUtils.getDisplayResistance(entity, ElementType.FIRE);
        if (resistPoints >= ElementalFireNatureReactionsConfig.scorchedResistThreshold) {
            entity.clearFire();
            data.remove(NBT_SCORCHED_TICKS);
            data.remove(NBT_SCORCHED_STRENGTH);
            data.remove(NBT_SCORCHED_SOURCE_FIRE_POWER);
            Debug.logDamageImmune(entity, resistPoints);
            return;
        }

        data.putInt(NBT_SCORCHED_TICKS, ticks - 1);
        int fireStrength = data.getInt(NBT_SCORCHED_STRENGTH);
        ServerLevel level = (ServerLevel) entity.level();

        if (entity.isInWater()) {
            Debug.logTickWater(entity, ticks, fireStrength);
            triggerThermalShock(entity, level, ticks, fireStrength);
            return;
        }

        if (entity.getRemainingFireTicks() < ticks) {
            entity.setRemainingFireTicks(ticks);
        }

        if (entity.tickCount % 20 == 0) {
            float damage = calculateScorchedDamage(fireStrength, entity);
            Debug.logTickDamage(entity, ticks, fireStrength, damage);
            if (damage > 0) {
                entity.hurt(ModDamageTypes.source(level, ModDamageTypes.LAVA_MAGIC), damage);
                level.sendParticles(ParticleTypes.LAVA, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 1, 0.2, 0.2, 0.2, 0.0);
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.2f, 1.0f);
            }
        }
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().getEffect() == ModMobEffects.WETNESS.get()) {
            boolean blocked = event.getEntity().getPersistentData().contains(NBT_SCORCHED_TICKS);
            Debug.logEffectApplicable(event.getEntity(), blocked);
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
            Debug.logFireDamageCancelled(entity, source);
            event.setCanceled(true);
        }
    }

    private static void triggerThermalShock(LivingEntity entity, ServerLevel level, int remainingTicks, int fireStrength) {
        double remainingSeconds = remainingTicks / 20.0;
        float dps = calculateScorchedDamage(fireStrength, entity);
        float totalRemainingDamage = (float) (remainingSeconds * dps);
        float ratio = (float) ElementalFireNatureReactionsConfig.scorchedShockDamageRatio;
        float shockDamage = totalRemainingDamage * ratio;

        Debug.logThermalShock(entity, remainingTicks, dps, totalRemainingDamage, shockDamage);
        if (shockDamage > 0.5f) {
            entity.hurt(ModDamageTypes.source(level, ModDamageTypes.LAVA_MAGIC), shockDamage);
        }

        entity.clearFire();
        CompoundTag data = entity.getPersistentData();
        data.remove(NBT_SCORCHED_TICKS);
        data.remove(NBT_SCORCHED_STRENGTH);
        data.remove(NBT_SCORCHED_SOURCE_FIRE_POWER);
        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f, 2.0f);
        level.sendParticles(ParticleTypes.CLOUD, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 20, 0.5, 0.5, 0.5, 0.05);
    }

    private static float calculateScorchedDamage(int fireStrength, LivingEntity target) {
        int resistPoints = ElementUtils.getDisplayResistance(target, ElementType.FIRE);
        if (resistPoints >= ElementalFireNatureReactionsConfig.scorchedResistThreshold) {
            Debug.logDamageImmune(target, resistPoints);
            return 0.0f;
        }

        double base = ElementalFireNatureReactionsConfig.scorchedDamageBase;
        int step = Math.max(1, ElementalFireNatureReactionsConfig.scorchedDamageScalingStep);
        double bonus = (double) fireStrength / step * 0.5;
        double rawDamage = base + bonus;
        double beforeMultipliers = rawDamage;

        if (target.fireImmune()) {
            rawDamage *= ElementalFireNatureReactionsConfig.scorchedImmuneModifier;
        }

        int fireProtLevel = 0;
        int genProtLevel = 0;
        for (ItemStack stack : target.getArmorSlots()) {
            fireProtLevel += EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_PROTECTION, stack);
            genProtLevel += EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, stack);
        }

        double denom = Math.max(1.0, ElementalFireNatureReactionsConfig.enchantmentCalculationDenominator);
        double fireProtReduction = (Math.min(fireProtLevel, denom) / denom) * ElementalFireNatureReactionsConfig.scorchedFireProtReduction;
        double genProtReduction = (Math.min(genProtLevel, denom) / denom) * ElementalFireNatureReactionsConfig.scorchedGenProtReduction;

        double finalDamage = rawDamage * (1.0 - fireProtReduction) * (1.0 - genProtReduction);

        Debug.logDamageCalculation(target, fireStrength, resistPoints, beforeMultipliers, rawDamage, fireProtLevel, genProtLevel, fireProtReduction, genProtReduction, finalDamage);
        return (float) finalDamage;
    }

    private static final class Debug {
        private static void logApplyBlacklisted(LivingEntity target, String id) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "灼烧", String.format("%s 在黑名单中，不施加灼烧", id));
        }

        private static void logApplyAlreadyScorched(LivingEntity target, int remaining) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "灼烧", String.format("%s 已有灼烧剩余 %d 刻，不重复施加", target.getName().getString(), remaining));
        }

        private static void logAttackerCooldown(LivingEntity attacker, LivingEntity target, long remainingTicks) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(attacker.level(), "灼烧", String.format("%s 攻击者冷却剩余 %d 刻，无法对 %s 施加灼烧", attacker.getName().getString(), remainingTicks, target.getName().getString()));
        }

        private static void logDurationModifier(LivingEntity target, String type, double multiplier, int adjustedDuration) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "灼烧", String.format("%s 为%s属性，持续时间倍率 %.2f，最终持续时间 %d 刻", target.getName().getString(), type, multiplier, adjustedDuration));
        }

        private static void logApplySuccess(LivingEntity target, LivingEntity attacker, int strength, int duration, int sourcePower, boolean isNature, boolean isFrost) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            String attackerName = attacker != null ? attacker.getName().getString() : "未知";
            String attr = isNature ? "自然" : (isFrost ? "冰霜" : "普通");
            GlobalDebugLogger.log(target.level(), "灼烧", String.format("%s 对 %s (%s) 施加灼烧：强度 %d，持续 %d 刻，源火点数 %d", attackerName, target.getName().getString(), attr, strength, duration, sourcePower));
        }

        private static void logTickExpired(LivingEntity target) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "灼烧", String.format("%s 灼烧自然结束，数据已清除", target.getName().getString()));
        }

        private static void logTickWater(LivingEntity target, int remainingTicks, int strength) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "灼烧", String.format("%s 入水触发热休克：剩余 %d 刻，强度 %d", target.getName().getString(), remainingTicks, strength));
        }

        private static void logTickDamage(LivingEntity target, int remainingTicks, int strength, float damage) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "灼烧", String.format("%s 灼烧伤害：剩余 %d 刻，强度 %d，伤害 %.2f", target.getName().getString(), remainingTicks, strength, damage));
        }

        private static void logEffectApplicable(LivingEntity target, boolean blocked) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "灼烧", String.format("%s 尝试施加潮湿：%s", target.getName().getString(), blocked ? "§c被灼烧阻止" : "§a允许"));
        }

        private static void logFireDamageCancelled(LivingEntity target, DamageSource source) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "灼烧", String.format("%s 受到火焰伤害 %s，已被灼烧免疫取消", target.getName().getString(), source.getMsgId()));
        }

        private static void logThermalShock(LivingEntity target, int remainingTicks, float dps, float totalRemaining, float shockDamage) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "灼烧", String.format("%s 热休克：剩余 %d 刻，DPS %.2f，剩余总伤 %.2f，冲击伤害 %.2f", target.getName().getString(), remainingTicks, dps, totalRemaining, shockDamage));
        }

        private static void logDamageImmune(LivingEntity target, int resist) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "灼烧", String.format("%s 火焰抗性 %d ≥ %d，免疫灼烧伤害", target.getName().getString(), resist, ElementalFireNatureReactionsConfig.scorchedResistThreshold));
        }

        private static void logDamageCalculation(LivingEntity target, int strength, int resistPoints, double beforeMultipliers, double afterMultipliers, int fireProt, int genProt, double fireRed, double genRed, double finalDamage) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "灼烧", String.format("%s 伤害计算：强度 %d，抗性 %d，基础 %.2f，修正后 %.2f，火保 %d(%.1f%%)，通保 %d(%.1f%%)，最终 %.2f", target.getName().getString(), strength, resistPoints, beforeMultipliers, afterMultipliers, fireProt, fireRed * 100, genProt, genRed * 100, finalDamage));
        }
    }
}
