// src/main/java/com/xulai/elementalcraft/client/NatureVisuals.java
package com.xulai.elementalcraft.client;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ElementalVisualConfig;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/**
 * NatureVisuals
 * <p>
 * 中文说明：
 * 自然属性视觉特效管理类。
 * 负责处理所有与自然元素战斗相关的粒子特效和音效反馈。
 * 包含三个核心模块：
 * 1. 近战挥动特效：“繁花”波浪圆弧，模拟藤蔓或花瓣随风飘动。
 * 2. 远程飞行特效：双螺旋藤蔓轨迹，高等级时呈现 DNA 链状结构。
 * 3. 命中爆裂特效：树叶飞散与生长反馈。
 * <p>
 * English Description:
 * Nature Visuals Manager Class.
 * Responsible for handling all particle effects and sound feedback related to nature combat.
 * Contains three core modules:
 * 1. Melee Swing FX: "Blossom" wave arc, simulating vines or petals drifting in the wind.
 * 2. Ranged Flight FX: Double-helix vine trail, appearing as a DNA-like structure at high tiers.
 * 3. Impact FX: Leaf burst and growth feedback.
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class NatureVisuals {

    private static final String TAG_ELEMENTAL_PROJECTILE = "EC_ElementalType";
    private static final String TAG_PROJECTILE_TIER = "EC_VisualTier";
    private static final Random RANDOM = new Random();

    /**
     * 计算视觉特效等级 (Tier 1 - Tier 4)。
     * 基于配置中的 MAX_STAT_CAP 进行倍率计算。
     * 直接读取客户端物品 NBT 数据。
     * <p>
     * Calculates visual effect tier (Tier 1 - Tier 4).
     * Based on MAX_STAT_CAP in configuration for multiplier calculation.
     * Reads client-side item NBT data directly.
     *
     * @param entity 实体 / Entity
     * @param type   元素类型 / Element Type
     * @return 特效等级 / Visual Tier
     */
    public static int calculateVisualTier(LivingEntity entity, ElementType type) {
        if (type == ElementType.NONE) return 0;
        
        int totalPoints = ElementUtils.getDisplayEnhancement(entity, type);
        int cap = ElementalConfig.getMaxStatCap();
        if (cap <= 0) cap = 100;

        if (totalPoints < cap) return 0;
        if (totalPoints < cap * 2) return 1;
        if (totalPoints < cap * 3) return 2;
        if (totalPoints < cap * 4) return 3;
        return 4;
    }

    /**
     * Client Side Events
     * <p>
     * 中文说明：
     * 客户端事件内部类，处理近战挥动等仅客户端可见的特效。
     * <p>
     * English Description:
     * Inner class for client-side events, handling client-only effects like melee swings.
     */
    @Mod.EventBusSubscriber(modid = ElementalCraft.MODID, value = Dist.CLIENT)
    public static class NatureClientEvents {

        /**
         * 监听生物 Tick 更新。
         * 检测攻击动作并播放近战挥动特效。
         * 在挥剑动作的起始帧触发，并检查物品是否具有攻击伤害属性（排除弓箭等）。
         * <p>
         * Listens for living entity ticks.
         * Detects attack actions and plays melee swing effects.
         * Triggers at the start frame of the swing action and checks if the item has attack damage attributes (excluding Bows, etc.).
         */
        @SubscribeEvent
        public static void onLivingTick(LivingEvent.LivingTickEvent event) {
            LivingEntity entity = event.getEntity();
            if (!entity.level().isClientSide) return;

            if (!ElementalVisualConfig.natureMeleeEnabled) return;

            if (entity.swinging && entity.swingTime == 1) {
                ItemStack stack = entity.getMainHandItem();
                
                if (!stack.getAttributeModifiers(EquipmentSlot.MAINHAND).containsKey(Attributes.ATTACK_DAMAGE)) {
                    return;
                }

                ElementType type = ElementUtils.getAttackElement(stack);

                if (type == ElementType.NATURE) {
                    int tier = calculateVisualTier(entity, type);
                    if (tier > 0) {
                        playBlossomMeleeSwing(entity, tier);
                    }
                }
            }
        }        

        /**
         * 播放“繁花”近战挥动特效。
         * 生成带有正弦波偏移的水平圆弧，模拟自然有机的波动感。
         * <p>
         * Plays "Blossom" melee swing effects.
         * Generates a horizontal arc with sine wave offsets, simulating natural organic fluctuation.
         *
         * @param entity 施法实体 / Casting entity
         * @param tier   视觉等级 / Visual tier
         */
        private static void playBlossomMeleeSwing(LivingEntity entity, int tier) {
            Level level = entity.level();
            
            Vec3 look = entity.getLookAngle();
            Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
            
            double radius = 2.2; 
            Vec3 centerPos = entity.getEyePosition();

            double baseTotalAngle = Math.toRadians(50); 
            double angleMultiplier = 0.3 + (tier * 0.2); 
            double actualAngle = baseTotalAngle * angleMultiplier;
            double startAngle = actualAngle / 2.0;
            double endAngle = -actualAngle / 2.0;

            int particleCount = (int) (15 * angleMultiplier) + 3; 

            for (int i = 0; i <= particleCount; i++) {
                double progress = (double) i / particleCount;
                double angle = startAngle + (endAngle - startAngle) * progress;

                double waveOffset = Math.sin(progress * Math.PI * 4) * 0.1;
                
                Vec3 horizontalOffset = right.scale(Math.sin(angle) * (radius + waveOffset));
                Vec3 forwardOffset = look.scale(Math.cos(angle) * (radius + waveOffset) * 0.7); 

                Vec3 p = centerPos.add(horizontalOffset).add(forwardOffset);

                double velX = look.x * 0.1;
                double velZ = look.z * 0.1;

                level.addParticle(Objects.requireNonNull(ParticleTypes.COMPOSTER), p.x, p.y, p.z, velX, 0, velZ);
                
                if (RANDOM.nextFloat() < 0.4f) {
                    level.addParticle(Objects.requireNonNull(ParticleTypes.SPORE_BLOSSOM_AIR), p.x, p.y, p.z, 0, 0, 0);
                }

                if (tier >= 3 && progress > 0.3 && RANDOM.nextFloat() < 0.5f) {
                    level.addParticle(Objects.requireNonNull(ParticleTypes.CHERRY_LEAVES), p.x, p.y, p.z, 0, 0, 0);
                }

                if (tier >= 4 && progress > 0.8) {
                    level.addParticle(Objects.requireNonNull(ParticleTypes.WAX_ON), p.x, p.y, p.z, 0, 0, 0);
                }
            }
        }
    }

    // ========================================================================================================
    // Server Side Events / 服务端事件
    // ========================================================================================================
    
    private static final Set<Projectile> ACTIVE_PROJECTILES = Collections.newSetFromMap(new WeakHashMap<>());

    /**
     * 监听实体加入世界事件。
     * 用于捕获并标记自然属性的弹射物。
     * <p>
     * Listens for entity join level events.
     * Used to capture and tag nature elemental projectiles.
     */
    @SubscribeEvent
    public static void onProjectileJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (!ElementalVisualConfig.natureRangedEnabled) return;

        if (event.getEntity() instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();
            if (owner instanceof LivingEntity shooter) {
                ItemStack weapon = shooter.getMainHandItem();
                ElementType type = ElementUtils.getAttackElement(weapon);
                
                if (type == ElementType.NATURE) {
                    int tier = calculateVisualTier(shooter, type);
                    if (tier > 0) {
                        projectile.getPersistentData().putString(TAG_ELEMENTAL_PROJECTILE, type.getId());
                        projectile.getPersistentData().putInt(TAG_PROJECTILE_TIER, tier);
                        synchronized (ACTIVE_PROJECTILES) {
                            ACTIVE_PROJECTILES.add(projectile);
                        }
                    }
                }
            }
        }
    }

    /**
     * 服务端 Tick 事件。
     * 处理活跃弹射物的双螺旋飞行粒子特效。
     * <p>
     * Server Tick Event.
     * Handles double-helix flight particle effects for active projectiles.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        boolean enabled = ElementalVisualConfig.natureRangedEnabled;

        synchronized (ACTIVE_PROJECTILES) {
            Iterator<Projectile> iterator = ACTIVE_PROJECTILES.iterator();
            while (iterator.hasNext()) {
                Projectile p = iterator.next();
                if (p == null || p.isRemoved()) {
                    iterator.remove();
                    continue;
                }

                double movedDistSq = p.position().distanceToSqr(p.xo, p.yo, p.zo);
                boolean isStuck = movedDistSq < 0.000001; 

                if (p.tickCount > 2 && isStuck) {
                    iterator.remove(); 
                    continue;
                }

                if (enabled && p.level() instanceof ServerLevel serverLevel) {
                    int tier = p.getPersistentData().getInt(TAG_PROJECTILE_TIER);
                    spawnVineFlightParticles(serverLevel, p, tier);
                }
            }
        }
    }

    /**
     * 生成双螺旋藤蔓飞行粒子。
     * 高等级时使用正弦和余弦函数构建围绕弹道旋转的粒子轨迹。
     * <p>
     * Spawns double-helix vine flight particles.
     * Uses sine and cosine functions to build particle trails rotating around the trajectory at high tiers.
     *
     * @param level 服务端世界 / Server level
     * @param p     弹射物实体 / Projectile entity
     * @param tier  视觉等级 / Visual tier
     */
    private static void spawnVineFlightParticles(ServerLevel level, Projectile p, int tier) {
        Vec3 velocity = p.getDeltaMovement();
        if (velocity.lengthSqr() < 0.0001) return;

        if (tier < 3) {
            level.sendParticles(Objects.requireNonNull(ParticleTypes.FALLING_SPORE_BLOSSOM), 
                    p.getX(), p.getY(), p.getZ(), 
                    1, 0, 0, 0, 0.01);
            return;
        }

        Vec3 dir = velocity.normalize();
        Vec3 up = new Vec3(0, 1, 0);
        if (Math.abs(dir.y) > 0.95) up = new Vec3(1, 0, 0); 
        
        Vec3 right = dir.cross(up).normalize();
        Vec3 realUp = right.cross(dir).normalize();

        double frequency = 0.8; 
        double radius = 0.35;   
        double time = p.tickCount * frequency;

        double offsetX1 = Math.cos(time) * radius;
        double offsetY1 = Math.sin(time) * radius;
        Vec3 pos1 = p.position().add(right.scale(offsetX1)).add(realUp.scale(offsetY1));

        double offsetX2 = Math.cos(time + Math.PI) * radius;
        double offsetY2 = Math.sin(time + Math.PI) * radius;
        Vec3 pos2 = p.position().add(right.scale(offsetX2)).add(realUp.scale(offsetY2));

        level.sendParticles(Objects.requireNonNull(ParticleTypes.CHERRY_LEAVES), pos1.x, pos1.y, pos1.z, 0, 0, 0, 0, 0);
        level.sendParticles(Objects.requireNonNull(ParticleTypes.SPORE_BLOSSOM_AIR), pos2.x, pos2.y, pos2.z, 0, 0, 0, 0, 0);
        level.sendParticles(Objects.requireNonNull(ParticleTypes.COMPOSTER), p.getX(), p.getY(), p.getZ(), 1, 0, 0, 0, 0);
    }

    /**
     * 监听生物受伤事件。
     * 触发命中爆裂特效（树叶和生长粒子）。
     * <p>
     * Listens for living entity damage events.
     * Triggers impact burst effects (leaves and growth particles).
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;

        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) return;

        int tier = 0;
        boolean isNature = false;
        boolean isRanged = false;

        Entity directEntity = event.getSource().getDirectEntity();
        if (directEntity instanceof Projectile projectile) {
            isRanged = true;
            CompoundTag data = projectile.getPersistentData();
            if (data.contains(TAG_ELEMENTAL_PROJECTILE) && data.getString(TAG_ELEMENTAL_PROJECTILE).equals("nature")) {
                isNature = true;
                tier = data.getInt(TAG_PROJECTILE_TIER);
            }
        } else {
            ItemStack weapon = livingAttacker.getMainHandItem();
            ElementType type = ElementUtils.getAttackElement(weapon);
            if (type == ElementType.NATURE) {
                isNature = true;
                tier = calculateVisualTier(livingAttacker, type);
            }
        }

        if (isRanged) {
            if (!ElementalVisualConfig.natureRangedEnabled) return;
        } else {
            if (!ElementalVisualConfig.natureMeleeEnabled) return;
        }

        if (isNature && tier > 0) {
            playOvergrowthImpact(event.getEntity(), tier);
        }
    }

    /**
     * 播放分级命中爆裂特效。
     * <p>
     * Plays tiered impact burst effects.
     *
     * @param target 受击目标 / Target entity
     * @param tier   视觉等级 / Visual tier
     */
    private static void playOvergrowthImpact(Entity target, int tier) {
        if (!(target.level() instanceof ServerLevel serverLevel)) return;
        
        serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                Objects.requireNonNull(SoundEvents.CHERRY_LEAVES_BREAK), SoundSource.PLAYERS, 1.0f + (tier * 0.2f), 1.0f);
        
        if (tier >= 3) {
            serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                Objects.requireNonNull(SoundEvents.BONE_MEAL_USE), SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        int count = 4 * tier;
        serverLevel.sendParticles(Objects.requireNonNull(ParticleTypes.HAPPY_VILLAGER), 
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 
                count, 0.4, 0.4, 0.4, 0.1);

        if (tier >= 3) {
            serverLevel.sendParticles(Objects.requireNonNull(ParticleTypes.SPORE_BLOSSOM_AIR),
                    target.getX(), target.getY() + 0.2, target.getZ(),
                    12, 0.5, 0.2, 0.5, 0.01); 

            serverLevel.sendParticles(Objects.requireNonNull(ParticleTypes.CHERRY_LEAVES),
                    target.getX(), target.getY() + target.getBbHeight() + 0.5, target.getZ(),
                    8, 0.4, 0.1, 0.4, 0.05);
        }
    }
}