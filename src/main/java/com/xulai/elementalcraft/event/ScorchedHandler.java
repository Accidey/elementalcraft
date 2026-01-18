// src/main/java/com/xulai/elementalcraft/event/ScorchedHandler.java
package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalReactionConfig;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
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

/**
 * ScorchedHandler (V1.5.0 Final - No Hardcoding)
 * <p>
 * 中文说明：
 * 灼烧机制的核心逻辑处理器（燃烧变种版）。
 * 不依赖药水效果，而是通过 NBT 标记实体处于“灼烧”状态。
 * V1.5.0 更新：
 * 1. 冷却机制：新增内置冷却时间，防止连续触发。
 * 2. 免疫减伤：免疫火焰的生物受到伤害减半。
 * 3. 黑名单支持：黑名单内的生物无法被灼烧。
 * <p>
 * English Description:
 * Core logic handler for the Scorched mechanic (Burning Variant).
 * Uses NBT to mark entities in the "Scorched" state.
 * V1.5.0 Updates:
 * 1. Cooldown: Added internal cooldown to prevent spamming.
 * 2. Immunity Modifier: Fire-immune mobs take reduced damage.
 * 3. Blacklist Support: Blacklisted mobs cannot be scorched.
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class ScorchedHandler {

    // NBT Keys
    public static final String NBT_SCORCHED_TICKS = "ec_scorched_ticks";
    public static final String NBT_SCORCHED_STRENGTH = "ec_scorched_str";
    public static final String NBT_SCORCHED_COOLDOWN = "ec_scorched_cd";

    /**
     * 外部 API：对目标施加灼烧效果。
     * 包含黑名单检查和冷却检查。
     */
    public static void applyScorched(LivingEntity target, int fireStrength, int duration) {
        if (target.level().isClientSide) return;

        // 1. 黑名单检查
        String entityId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).toString();
        if (ElementalReactionConfig.cachedScorchedBlacklist.contains(entityId)) {
            return;
        }

        // 2. 冷却检查
        CompoundTag data = target.getPersistentData();
        long gameTime = target.level().getGameTime();
        if (data.contains(NBT_SCORCHED_COOLDOWN)) {
            if (gameTime < data.getLong(NBT_SCORCHED_COOLDOWN)) {
                return; // 冷却中
            }
        }

        // 3. 设置 NBT 数据与状态
        data.putInt(NBT_SCORCHED_TICKS, duration);
        data.putInt(NBT_SCORCHED_STRENGTH, fireStrength);
        
        // 设置冷却时间 (使用静态变量，不调用 .get())
        data.putLong(NBT_SCORCHED_COOLDOWN, gameTime + duration + ElementalReactionConfig.scorchedCooldown);

        // 设置原版燃烧状态（视觉效果）
        target.setSecondsOnFire(3);

        // 播放粒子效果
        if (target.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LAVA,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    5, 0.2, 0.2, 0.2, 0.0);
        }
    }

    /**
     * 生物 Tick 事件监听。
     * 处理灼烧状态的主循环逻辑。
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        CompoundTag data = entity.getPersistentData();
        if (!data.contains(NBT_SCORCHED_TICKS)) return;

        int ticks = data.getInt(NBT_SCORCHED_TICKS);
        
        // 倒计时结束清理
        if (ticks <= 0) {
            data.remove(NBT_SCORCHED_TICKS);
            data.remove(NBT_SCORCHED_STRENGTH);
            return;
        }

        // 倒计时递减
        data.putInt(NBT_SCORCHED_TICKS, ticks - 1);
        int fireStrength = data.getInt(NBT_SCORCHED_STRENGTH);
        ServerLevel level = (ServerLevel) entity.level();

        // ========================== 1. 环境博弈：深水判定 ==========================
        if (entity.isInWater() && entity.isEyeInFluidType(net.minecraftforge.common.ForgeMod.WATER_TYPE.get())) {
            triggerThermalShock(entity, level, ticks, fireStrength);
            return;
        }

        // ========================== 2. 燃烧锁定 ==========================
        // 使用静态变量
        int lockTicks = (int) (ElementalReactionConfig.scorchedBurningLockDuration * 20);
        if (entity.getRemainingFireTicks() < lockTicks) {
            entity.setSecondsOnFire((int) ElementalReactionConfig.scorchedBurningLockDuration);
        }

        // ========================== 3. 周期性伤害与特效 ==========================
        if (entity.tickCount % 20 == 0) {
            float damage = calculateScorchedDamage(fireStrength, entity);

            if (damage > 0) {
                entity.hurt(ModDamageTypes.source(level, ModDamageTypes.LAVA_MAGIC), damage);

                level.sendParticles(ParticleTypes.LAVA,
                        entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                        1, 0.2, 0.2, 0.2, 0.0);

                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.2f, 1.0f);
            }
        }
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEffectInstance().getEffect() == ModMobEffects.WETNESS.get()) {
            if (event.getEntity().getPersistentData().contains(NBT_SCORCHED_TICKS)) {
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
            event.setCanceled(true);
        }
    }

    private static void triggerThermalShock(LivingEntity entity, ServerLevel level, int remainingTicks, int fireStrength) {
        double remainingSeconds = remainingTicks / 20.0;
        float dps = calculateScorchedDamage(fireStrength, entity);
        float totalRemainingDamage = (float) (remainingSeconds * dps);
        float shockDamage = totalRemainingDamage * 0.5f;

        if (shockDamage > 0.5f) {
            entity.hurt(entity.damageSources().generic(), shockDamage);
        }

        entity.clearFire();
        CompoundTag data = entity.getPersistentData();
        data.remove(NBT_SCORCHED_TICKS);
        data.remove(NBT_SCORCHED_STRENGTH);

        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f, 2.0f);

        level.sendParticles(ParticleTypes.CLOUD,
                entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                20, 0.5, 0.5, 0.5, 0.05);
    }

    /**
     * 计算单次（每秒）灼烧伤害。
     * <p>
     * Calculates single (per second) scorched damage.
     */
    private static float calculateScorchedDamage(int fireStrength, LivingEntity target) {
        int resistPoints = ElementUtils.getDisplayResistance(target, ElementType.FIRE);

        // 使用静态变量
        if (resistPoints >= ElementalReactionConfig.scorchedResistThreshold) {
            return 0.0f;
        }

        double base = ElementalReactionConfig.scorchedDamageBase;
        int step = Math.max(1, ElementalReactionConfig.scorchedDamageScalingStep);
        double bonus = (double) fireStrength / step * 0.5;
        double rawDamage = base + bonus;

        // 免疫检测：如果生物免疫火焰，伤害减半 (可配置)
        if (target.fireImmune()) {
            rawDamage *= ElementalReactionConfig.scorchedImmuneModifier;
        }

        // 自然属性易伤
        if (ElementUtils.getDisplayEnhancement(target, ElementType.NATURE) > 0 ||
                ElementUtils.getDisplayResistance(target, ElementType.NATURE) > 0) {
            rawDamage *= ElementalReactionConfig.scorchedNatureMultiplier;
        }

        // 附魔减伤
        int fireProtLevel = 0;
        int genProtLevel = 0;

        for (ItemStack stack : target.getArmorSlots()) {
            fireProtLevel += EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_PROTECTION, stack);
            genProtLevel += EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, stack);
        }

        double fireProtReduction = (Math.min(fireProtLevel, 16) / 16.0) * ElementalReactionConfig.scorchedFireProtReduction;
        double genProtReduction = (Math.min(genProtLevel, 16) / 16.0) * ElementalReactionConfig.scorchedGenProtReduction;

        rawDamage *= (1.0 - fireProtReduction);
        rawDamage *= (1.0 - genProtReduction);

        return (float) rawDamage;
    }
}