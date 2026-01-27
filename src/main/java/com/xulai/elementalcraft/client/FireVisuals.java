// src/main/java/com/xulai/elementalcraft/client/FireVisuals.java
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
 * FireVisuals
 * <p>
 * 中文说明：
 * 赤焰视觉特效管理类。
 * 这是一个独立的视觉逻辑处理器，负责处理所有与赤焰战斗相关的粒子特效和音效反馈。
 * 它包含三个核心模块：
 * 1. 近战挥动特效：采用“动态扇面”流动火焰设计，全等级固定2格距离，扇面角度宽度随等级变化。
 * 增加了攻击伤害属性检查，防止弓箭等非近战武器触发挥动特效。
 * 2. 远程飞行特效：追踪带有元素属性的弹射物，基础轨迹为火焰，满级叠加灵魂火焰与幽匿灵魂，粒子密度较高。
 * 3. 命中爆裂特效：监听伤害事件，在攻击生效位置播放爆裂反馈，支持持续性视觉残留。
 * 所有特效均受 {@link ElementalVisualConfig} 配置文件控制。
 * <p>
 * English Description:
 * Fire Visuals Manager Class.
 * An independent visual logic handler responsible for all particle effects and sound feedback related to fire combat.
 * It contains three core modules:
 * 1. Melee Swing FX: Uses a "Dynamic Sector" flowing flame design. Fixed 2-block distance, sector angle width scales with Tier.
 * Includes a check for attack damage attributes to prevent swing effects on non-melee items like bows.
 * 2. Ranged Flight FX: Tracks projectiles with elemental attributes. Base trail is Flame, max tier overlays Soul Fire and Sculk Soul with increased density.
 * 3. Impact Flare FX: Listens to damage events and plays burst feedback at the point of impact, supports persistent visual residue.
 * All effects are controlled by the {@link ElementalVisualConfig} configuration file.
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class FireVisuals {

    private static final String TAG_ELEMENTAL_PROJECTILE = "EC_ElementalType";
    private static final String TAG_PROJECTILE_TIER = "EC_VisualTier";
    private static final Random RANDOM = new Random();

    /**
     * 计算视觉特效等级 (Tier 1 - Tier 4)。
     * 基于配置中的 MAX_STAT_CAP 进行倍率计算。
     * 直接读取客户端物品 NBT 数据，确保更换装备后特效等级立即更新。
     * <p>
     * Calculates visual effect tier (Tier 1 - Tier 4).
     * Based on MAX_STAT_CAP in configuration for multiplier calculation.
     * Reads client-side item NBT data directly to ensure effect tier updates immediately upon equipment change.
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

    // ========================================================================================================
    // Client Side: Melee Swing / 客户端事件：近战挥动
    // ========================================================================================================
    @Mod.EventBusSubscriber(modid = ElementalCraft.MODID, value = Dist.CLIENT)
    public static class ClientEvents {

        /**
         * 监听生物 Tick 更新。
         * 用于检测攻击动作并播放近战挥动特效。
         * 在挥剑动作的起始帧触发，以保证视觉反馈的即时性并避免粒子重叠。
         * 额外检查主手物品是否具有攻击伤害属性，若无（如弓）则不触发特效。
         * <p>
         * Listens for living entity ticks.
         * Used to detect attack actions and play melee swing effects.
         * Triggers at the start frame of the swing action to ensure instant visual feedback and prevent particle overlapping.
         * Additionally checks if the main-hand item has attack damage attributes; if not (e.g., Bows), effects are skipped.
         */
        @SubscribeEvent
        public static void onLivingTick(LivingEvent.LivingTickEvent event) {
            LivingEntity entity = event.getEntity();
            if (!entity.level().isClientSide) return;

            if (!ElementalVisualConfig.fireMeleeEnabled) return;

            if (entity.swinging && entity.swingTime == 1) {
                ItemStack stack = entity.getMainHandItem();
                
                // 检查物品是否有攻击伤害属性（排除弓箭、空手等无明显近战能力的物品）
                // Check if the item has attack damage attribute (excludes items like Bows or empty hand without melee capability)
                if (!stack.getAttributeModifiers(EquipmentSlot.MAINHAND).containsKey(Attributes.ATTACK_DAMAGE)) {
                    return;
                }

                ElementType type = ElementUtils.getAttackElement(stack);

                if (type == ElementType.FIRE) {
                    int tier = calculateVisualTier(entity, type);
                    if (tier > 0) {
                        playFlowingMeleeSwing(entity, tier);
                    }
                }
            }
        }

        /**
         * 播放流动式近战挥动特效。
         * 实现“动态角度水平圆弧”跑马灯视觉效果。
         * 包含防止垂直视角下万向节死锁的计算逻辑。
         * 根据 Tier 等级调整配色：Tier 3 为暖色调（红+橙），Tier 4 为冷色调（蓝+青）。
         * <p>
         * Plays flowing melee swing effects.
         * Implements "Dynamic Angle Horizontal Arc" Marquee visual effect.
         * Includes calculation logic to prevent Gimbal Lock at vertical viewing angles.
         * Adjusts color scheme based on Tier: Tier 3 is warm (Red+Orange), Tier 4 is cold (Blue+Teal).
         *
         * @param entity 施法实体 / Casting entity
         * @param tier   视觉等级 / Visual tier
         */
        private static void playFlowingMeleeSwing(LivingEntity entity, int tier) {
            Level level = entity.level();
            
            Vec3 look = entity.getLookAngle();
            Vec3 up = new Vec3(0, 1, 0);

            // 当视线过于垂直时切换参考向量，防止叉乘结果为零
            // Switch reference vector when look angle is too vertical to prevent zero cross product
            if (Math.abs(look.y) > 0.95) {
                up = new Vec3(1, 0, 0);
            }

            Vec3 right = look.cross(up).normalize();
            if (right.lengthSqr() < 0.001) right = new Vec3(1, 0, 0);
            
            double radius = 2.0; 
            Vec3 centerPos = entity.getEyePosition();

            double baseTotalAngle = Math.toRadians(45); 
            double angleMultiplier = tier * 0.25; 
            double actualAngle = baseTotalAngle * angleMultiplier;

            double startAngle = actualAngle / 2.0;
            double endAngle = -actualAngle / 2.0;

            int particleCount = (int) (12 * angleMultiplier) + 2; 

            for (int i = 0; i <= particleCount; i++) {
                double progress = (double) i / particleCount;
                double angle = startAngle + (endAngle - startAngle) * progress;

                Vec3 horizontalOffset = right.scale(Math.sin(angle) * radius);
                Vec3 forwardOffset = look.scale(Math.cos(angle) * radius * 0.8); 

                Vec3 p = centerPos.add(horizontalOffset).add(forwardOffset);

                double dropVelocity = -0.15 * Math.pow(1.0 - progress, 2); 

                SimpleParticleType flameType;
                if (tier >= 4) {
                    flameType = ParticleTypes.SOUL_FIRE_FLAME;
                } else {
                    flameType = ParticleTypes.FLAME;
                }

                level.addParticle(Objects.requireNonNull(flameType), 
                        p.x, p.y, p.z, 
                        0, dropVelocity, 0);

                // Tier 3: 红色火焰 + 橙色熔岩 (暖色调)
                // Tier 3: Red flame + Orange lava (Warm tone)
                if (tier == 3 && progress > 0.7) {
                     level.addParticle(Objects.requireNonNull(ParticleTypes.LAVA), p.x, p.y, p.z, 0, 0, 0);
                }

                // Tier 4: 蓝色火焰 + 幽匿灵魂 (冷色调)
                // Tier 4: Blue flame + Sculk Soul (Cold tone)
                if (tier >= 4 && progress > 0.7) {
                    if (RANDOM.nextFloat() < 0.3f) {
                        level.addParticle(Objects.requireNonNull(ParticleTypes.SCULK_SOUL), p.x, p.y, p.z, 0, 0.05, 0);
                    }
                }

                // 刀尖高亮
                // Tip highlight
                if (tier >= 4 && progress > 0.9) {
                    level.addParticle(Objects.requireNonNull(ParticleTypes.END_ROD), p.x, p.y, p.z, 0, 0, 0);
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
        boolean enabled = ElementalVisualConfig.fireRangedEnabled;

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
                    spawnTieredFlightParticles(serverLevel, p, tier);
                }
            }
        }
    }

    /**
     * 生成分级飞行拖尾粒子。
     * 基础轨迹为火焰，满级时叠加灵魂火焰并附带幽匿灵魂。
     * <p>
     * Spawns tiered flight trail particles.
     * Base trail is Flame, max tier overlays Soul Fire and Sculk Soul.
     *
     * @param level 服务端世界 / Server level
     * @param p     弹射物实体 / Projectile entity
     * @param tier  视觉等级 / Visual tier
     */
    private static void spawnTieredFlightParticles(ServerLevel level, Projectile p, int tier) {
        // 基础粒子：火焰 
        // Base particle: Flame
        SimpleParticleType mainParticle = ParticleTypes.FLAME;
        double speed = 0.02;
        
        // 100% 几率生成 1 个，25% 几率生成第 2 个
        int baseCount = 1 + (RANDOM.nextFloat() < 0.25f ? 1 : 0);
        level.sendParticles(Objects.requireNonNull(mainParticle), p.getX(), p.getY(), p.getZ(), baseCount, 0, 0, 0, speed);

        // Tier 2+: 熔岩 (暖色调) 
        // Tier 2+: Lava (Warm tone) 
        if (tier >= 2 && RANDOM.nextFloat() < 0.375f) { // 0.3 * 1.25 = 0.375
            level.sendParticles(Objects.requireNonNull(ParticleTypes.LAVA), p.getX(), p.getY(), p.getZ(), 1, 0, 0, 0, 0);
        }

        // Tier 4: 叠加冷色调装饰 (灵魂火 + 幽匿灵魂)
        // 形成“暖色核心 + 冷色外溢”的混沌视觉效果
        // Tier 4: Overlay cold tone accents (Soul Fire + Sculk Soul)
        // Creates a chaotic visual effect of "Warm Core + Cold Spill"
        if (tier >= 4) {
            // 灵魂火焰 
            // Soul Fire 
            int soulCount = 1 + (RANDOM.nextFloat() < 0.25f ? 1 : 0);
            level.sendParticles(Objects.requireNonNull(ParticleTypes.SOUL_FIRE_FLAME), p.getX(), p.getY(), p.getZ(), soulCount, 0, 0, 0, 0.02);
            
            // 幽匿灵魂 
            // Sculk Soul 
            if (RANDOM.nextFloat() < 0.375f) { // 0.3 * 1.25 = 0.375
                level.sendParticles(Objects.requireNonNull(ParticleTypes.SCULK_SOUL), p.getX(), p.getY(), p.getZ(), 1, 0, 0, 0, 0.05);
            }
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
            isRanged = true;
            CompoundTag data = projectile.getPersistentData();
            if (data.contains(TAG_ELEMENTAL_PROJECTILE) && data.getString(TAG_ELEMENTAL_PROJECTILE).equals("fire")) {
                isFire = true;
                tier = data.getInt(TAG_PROJECTILE_TIER);
            }
        } else {
            ItemStack weapon = livingAttacker.getMainHandItem();
            ElementType type = ElementUtils.getAttackElement(weapon);
            if (type == ElementType.FIRE) {
                isFire = true;
                tier = calculateVisualTier(livingAttacker, type);
            }
        }

        if (isRanged) {
            if (!ElementalVisualConfig.fireRangedEnabled) return;
        } else {
            if (!ElementalVisualConfig.fireMeleeEnabled) return;
        }

        if (isFire && tier > 0) {
            playTieredImpactExplosion(event.getEntity(), tier);
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
    private static void playTieredImpactExplosion(Entity target, int tier) {
        if (!(target.level() instanceof ServerLevel serverLevel)) return;
        
        serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                Objects.requireNonNull(SoundEvents.FIRECHARGE_USE), SoundSource.PLAYERS, 1.0f + (tier * 0.2f), 1.0f - (tier * 0.1f));

        int count = 5 * tier;
        serverLevel.sendParticles(Objects.requireNonNull(ParticleTypes.FLAME), 
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 
                count, 0.3, 0.3, 0.3, 0.1);

        if (tier >= 3) {
            serverLevel.sendParticles(Objects.requireNonNull(ParticleTypes.LAVA), 
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 
                    tier * 2, 0.5, 0.5, 0.5, 0.2);
        }

        if (tier >= 3) {
            serverLevel.sendParticles(Objects.requireNonNull(ParticleTypes.SOUL_FIRE_FLAME),
                    target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                    15, 0.4, 0.4, 0.4, 0.005); 
            
            serverLevel.sendParticles(Objects.requireNonNull(ParticleTypes.CAMPFIRE_COSY_SMOKE),
                    target.getX(), target.getY() + target.getBbHeight() * 0.8, target.getZ(),
                    5, 0.2, 0.5, 0.2, 0.02);
        }
    }
}