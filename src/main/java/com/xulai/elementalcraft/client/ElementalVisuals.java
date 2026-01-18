// src/main/java/com/xulai/elementalcraft/client/ElementalVisuals.java
package com.xulai.elementalcraft.client;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ElementalVisualConfig;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
 * ElementalVisuals
 * <p>
 * 中文说明：
 * 元素视觉特效管理类。
 * 这是一个独立的视觉逻辑处理器，负责处理所有与元素战斗相关的粒子特效和音效反馈。
 * 它包含三个核心模块：
 * 1. 近战挥动特效：支持玩家及所有生物，基于配置开关和 Tier 分级系统播放。
 * 2. 远程飞行特效：追踪带有元素属性的弹射物，支持位移检测以防止插墙特效残留。
 * 3. 命中爆裂特效：监听伤害事件，在攻击生效位置播放爆裂反馈，支持持续性视觉残留。
 * 所有特效均受 {@link ElementalVisualConfig} 配置文件控制。
 * <p>
 * English Description:
 * Elemental Visuals Manager Class.
 * An independent visual logic handler responsible for all particle effects and sound feedback related to elemental combat.
 * It contains three core modules:
 * 1. Melee Swing FX: Supports players and all mobs, plays based on config switches and the Tier system.
 * 2. Ranged Flight FX: Tracks projectiles with elemental attributes, supports position delta checks to prevent stuck effect residue.
 * 3. Impact Flare FX: Listens to damage events and plays burst feedback at the point of impact, supports persistent visual residue.
 * All effects are controlled by the {@link ElementalVisualConfig} configuration file.
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class ElementalVisuals {

    private static final String TAG_ELEMENTAL_PROJECTILE = "EC_ElementalType";
    private static final String TAG_PROJECTILE_TIER = "EC_VisualTier";
    private static final Random RANDOM = new Random();

    // ========================================================================================================
    // Helper: Visual Tier Calculation / 视觉等级计算辅助方法
    // ========================================================================================================

    /**
     * 计算视觉特效等级 (Tier 1 - Tier 4)。
     * 基于配置中的 MAX_STAT_CAP 进行倍率计算。
     * <p>
     * Calculates visual effect tier (Tier 1 - Tier 4).
     * Based on MAX_STAT_CAP in configuration for multiplier calculation.
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

        // Tier 0: 未达标
        if (totalPoints < cap) return 0;
        // Tier 1-4
        if (totalPoints < cap * 2) return 1;
        if (totalPoints < cap * 3) return 2;
        if (totalPoints < cap * 4) return 3;
        return 4;
    }

    // ========================================================================================================
    // Client Side: Melee Swing / 客户端事件：近战挥动
    // ========================================================================================================
    @Mod.EventBusSubscriber(modid = ElementalCraft.MODID, value = Dist.CLIENT)
    public static class ClientEvents {

        /**
         * 监听生物 Tick 更新。
         * 用于检测攻击动作并播放近战挥动特效。
         * <p>
         * Listens for living entity ticks.
         * Used to detect attack actions and play melee swing effects.
         */
        @SubscribeEvent
        public static void onLivingTick(LivingEvent.LivingTickEvent event) {
            LivingEntity entity = event.getEntity();
            if (!entity.level().isClientSide) return;

            // 检查配置：如果近战特效被禁用，则直接返回
            // Check config: If melee visuals are disabled, return immediately
            if (!ElementalVisualConfig.fireMeleeEnabled) return;

            if (entity.swinging && entity.swingTime == 1) {
                ItemStack stack = entity.getMainHandItem();
                ElementType type = ElementUtils.getAttackElement(stack);

                if (type == ElementType.FIRE) {
                    int tier = calculateVisualTier(entity, type);
                    if (tier > 0) {
                        playTieredMeleeSwing(entity, tier);
                    }
                }
            }
        }

        /**
         * 播放分级近战挥动特效。
         * <p>
         * Plays tiered melee swing effects.
         */
        private static void playTieredMeleeSwing(LivingEntity entity, int tier) {
            Level level = entity.level();
            Vec3 look = entity.getLookAngle();
            Vec3 pos = entity.getEyePosition().add(look.scale(1.2));
            Vec3 right = look.cross(new Vec3(0, 1, 0)).normalize();
            
            float volume = 0.5f + (tier * 0.1f);
            float pitch = 1.5f - (tier * 0.1f);

            entity.level().playSound(entity instanceof Player ? (Player) entity : null,
                    entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, 
                    entity instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE, 
                    volume, pitch);

            int particleCount = 3 + (tier * 3); 
            
            for (int i = -particleCount / 2; i <= particleCount / 2; i++) {
                double offset = i * (0.8 / particleCount);
                Vec3 p = pos.add(right.scale(offset)).add(0, -Math.abs(offset) * 0.3, 0);

                level.addParticle(ParticleTypes.FLAME, p.x, p.y, p.z, look.x * 0.1, look.y * 0.1, look.z * 0.1);

                if (tier >= 3 && RANDOM.nextBoolean()) {
                    level.addParticle(ParticleTypes.LAVA, p.x, p.y, p.z, 0, 0, 0);
                }

                if (tier >= 4) {
                    level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, 
                        p.x + (RANDOM.nextDouble() - 0.5) * 0.2, 
                        p.y + (RANDOM.nextDouble() - 0.5) * 0.2, 
                        p.z + (RANDOM.nextDouble() - 0.5) * 0.2, 
                        look.x * 0.05, look.y * 0.05, look.z * 0.05);
                }
            }
        }
    }

    // ========================================================================================================
    // Server Side: Ranged Flight / 服务端事件：远程飞行
    // ========================================================================================================
    
    private static final Set<Projectile> ACTIVE_PROJECTILES = Collections.newSetFromMap(new WeakHashMap<>());

    /**
     * 监听实体加入世界事件。
     * 用于捕获并标记元素弹射物。
     * <p>
     * Listens for entity join level events.
     * Used to capture and tag elemental projectiles.
     */
    @SubscribeEvent
    public static void onProjectileJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;

        // 检查配置：如果远程特效被禁用，则不进行追踪
        // Check config: If ranged visuals are disabled, do not track
        if (!ElementalVisualConfig.fireRangedEnabled) return;

        if (event.getEntity() instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();
            if (owner instanceof LivingEntity shooter) {
                ItemStack weapon = shooter.getMainHandItem();
                ElementType type = ElementUtils.getAttackElement(weapon);
                
                if (type == ElementType.FIRE) {
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
     * 处理活跃弹射物的飞行粒子特效。
     * <p>
     * Server Tick Event.
     * Handles flight particle effects for active projectiles.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // 检查配置：如果中途被禁用，停止生成粒子（但列表清理逻辑保留以防内存泄漏）
        // Check config: If disabled mid-game, stop spawning particles (but keep cleanup logic to prevent memory leaks)
        boolean enabled = ElementalVisualConfig.fireRangedEnabled;

        synchronized (ACTIVE_PROJECTILES) {
            Iterator<Projectile> iterator = ACTIVE_PROJECTILES.iterator();
            while (iterator.hasNext()) {
                Projectile p = iterator.next();
                
                // 1. 基础无效检查
                // 1. Basic validity check
                if (p == null || p.isRemoved()) {
                    iterator.remove();
                    continue;
                }

                // 2. 位移检测法 (Position Delta Check)
                // 防止箭矢插在方块上后持续播放特效
                // 2. Position Delta Check
                // Prevents effects from playing continuously after arrow sticks to a block
                double movedDistSq = p.position().distanceToSqr(p.xo, p.yo, p.zo);
                boolean isStuck = movedDistSq < 0.000001; 

                // 3. 辅助检测：给它 2 tick 的加速时间
                // 3. Auxiliary check: Give it 2 ticks to accelerate
                if (p.tickCount > 2 && isStuck) {
                    iterator.remove(); 
                    continue;
                }

                if (enabled && p.level() instanceof ServerLevel serverLevel) {
                    int tier = p.getPersistentData().getInt(TAG_PROJECTILE_TIER);
                    spawnTieredFlightParticles(serverLevel, p, tier);
                }
            }
        }
    }

    /**
     * 生成分级飞行拖尾粒子。
     * <p>
     * Spawns tiered flight trail particles.
     */
    private static void spawnTieredFlightParticles(ServerLevel level, Projectile p, int tier) {
        // Tier 1: 淡淡的烟雾
        SimpleParticleType mainParticle = ParticleTypes.CAMPFIRE_COSY_SMOKE;
        double speed = 0.01;
        
        // Tier 3+: 拖尾变成火焰
        if (tier >= 3) {
            mainParticle = ParticleTypes.FLAME;
            speed = 0.05;
        }

        level.sendParticles(mainParticle, p.getX(), p.getY(), p.getZ(), 1, 0, 0, 0, speed);

        // Tier 2+: 掉落熔岩
        if (tier >= 2 && RANDOM.nextFloat() < 0.3f) {
            level.sendParticles(ParticleTypes.LAVA, p.getX(), p.getY(), p.getZ(), 1, 0, 0, 0, 0);
        }

        // Tier 4: 末影烛光
        if (tier >= 4) {
            level.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY(), p.getZ(), 1, 0, 0, 0, 0.02);
        }
    }

    // ========================================================================================================
    // Server Side: Impact Effect / 服务端事件：命中特效
    // ========================================================================================================

    /**
     * 监听生物受伤事件。
     * 触发命中爆裂特效。
     * <p>
     * Listens for living entity damage events.
     * Triggers impact burst effects.
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;

        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) return;

        int tier = 0;
        boolean isFire = false;
        boolean isRanged = false;

        Entity directEntity = event.getSource().getDirectEntity();
        if (directEntity instanceof Projectile projectile) {
            // 远程攻击判定
            // Ranged attack check
            isRanged = true;
            CompoundTag data = projectile.getPersistentData();
            if (data.contains(TAG_ELEMENTAL_PROJECTILE) && data.getString(TAG_ELEMENTAL_PROJECTILE).equals("fire")) {
                isFire = true;
                tier = data.getInt(TAG_PROJECTILE_TIER);
            }
        } else {
            // 近战攻击判定
            // Melee attack check
            ItemStack weapon = livingAttacker.getMainHandItem();
            ElementType type = ElementUtils.getAttackElement(weapon);
            if (type == ElementType.FIRE) {
                isFire = true;
                tier = calculateVisualTier(livingAttacker, type);
            }
        }

        // 根据攻击类型检查对应的配置开关
        // Check corresponding config switch based on attack type
        if (isRanged) {
            if (!ElementalVisualConfig.fireRangedEnabled) return;
        } else {
            if (!ElementalVisualConfig.fireMeleeEnabled) return;
        }

        // 只有 Tier > 0 时才播放
        // Play only if Tier > 0
        if (isFire && tier > 0) {
            playTieredImpactExplosion(event.getEntity(), tier);
        }
    }

    /**
     * 播放分级命中爆裂特效。
     * <p>
     * Plays tiered impact burst effects.
     */
    private static void playTieredImpactExplosion(Entity target, int tier) {
        if (!(target.level() instanceof ServerLevel serverLevel)) return;
        
        // 瞬间爆发音效
        serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f + (tier * 0.2f), 1.0f - (tier * 0.1f));

        int count = 5 * tier;
        serverLevel.sendParticles(ParticleTypes.FLAME, 
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 
                count, 0.3, 0.3, 0.3, 0.1);

        if (tier >= 3) {
            serverLevel.sendParticles(ParticleTypes.LAVA, 
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 
                    tier * 2, 0.5, 0.5, 0.5, 0.2);
        }

        // [Feature] 持续 1-2 秒的视觉残留
        // Tier 4 (满配) 或 Tier 3 (史诗) 会触发
        if (tier >= 3) {
            // 生成一团速度极低（几乎悬停）的灵魂火/火焰粒子
            // 粒子的生命周期通常在 20-40 tick (1-2秒) 左右
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                    15, 0.4, 0.4, 0.4, 0.005); // Speed 极低，使粒子悬停
            
            // 再加一点烟雾增加持续感
            serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    target.getX(), target.getY() + target.getBbHeight() * 0.8, target.getZ(),
                    5, 0.2, 0.5, 0.2, 0.02);
        }
    }
}