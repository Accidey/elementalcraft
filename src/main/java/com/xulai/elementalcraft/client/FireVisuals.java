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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
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

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class FireVisuals {

    private static final String TAG_ELEMENTAL_PROJECTILE = "EC_ElementalType";
    private static final String TAG_PROJECTILE_TIER = "EC_VisualTier";
    private static final String TAG_SPAWN_TICK = "EC_SpawnTick";

    private static final Random RANDOM = new Random();

    private static final Set<Projectile> ACTIVE_PROJECTILES = Collections.synchronizedSet(new HashSet<>());

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

    // ======================== 客户端近战特效 ========================
    @Mod.EventBusSubscriber(modid = ElementalCraft.MODID, value = Dist.CLIENT)
    public static class FireClientEvents {

        @SubscribeEvent
        public static void onLivingTick(LivingEvent.LivingTickEvent event) {
            LivingEntity entity = event.getEntity();
            if (!entity.level().isClientSide) return;
            if (!ElementalVisualConfig.fireMeleeEnabled) return;

            if (entity.swinging && entity.swingTime == 1) {
                ItemStack stack = entity.getMainHandItem();
                // 与自然属性一致：只检查是否有攻击伤害（剑/斧等）
                if (!stack.getAttributeModifiers(EquipmentSlot.MAINHAND).containsKey(Attributes.ATTACK_DAMAGE)) {
                    return;
                }
                ElementType type = ElementUtils.getAttackElement(stack);
                if (type == ElementType.FIRE) {
                    int tier = calculateVisualTier(entity, type);
                    if (tier > 0) {
                        playFlameMeleeSwing(entity, tier);
                    }
                }
            }
        }

        /**
         * 火焰近战挥动特效
         * 弧形轨迹，等级越高粒子种类越丰富：
         * tier 1: 火焰粒子
         * tier 2: 增加灵魂火焰粒子
         * tier 3: 增加熔岩粒子
         * tier 4: 增加烟雾粒子
         */
        private static void playFlameMeleeSwing(LivingEntity entity, int tier) {
            Level level = entity.level();
            Vec3 look = entity.getLookAngle();
            Vec3 up = new Vec3(0, 1, 0);
            if (Math.abs(look.y) > 0.95) {
                up = new Vec3(1, 0, 0);
            }
            Vec3 right = look.cross(up).normalize();
            if (right.lengthSqr() < 0.001) right = new Vec3(1, 0, 0);

            double radius = ElementalVisualConfig.fireMeleeRadius;
            Vec3 centerPos = entity.getEyePosition();

            double baseTotalAngle = Math.toRadians(ElementalVisualConfig.fireMeleeBaseAngleDegrees);
            double angleMultiplier = ElementalVisualConfig.fireMeleeAngleMultiplierBase
                    + tier * ElementalVisualConfig.fireMeleeAngleMultiplierPerTier;
            double actualAngle = baseTotalAngle * angleMultiplier;
            double startAngle = actualAngle / 2.0;
            double endAngle = -actualAngle / 2.0;

            int particleCount = (int) (ElementalVisualConfig.fireMeleeParticleCountBase * angleMultiplier)
                    + ElementalVisualConfig.fireMeleeParticleCountOffset;

            for (int i = 0; i <= particleCount; i++) {
                double progress = (double) i / particleCount;
                double angle = startAngle + (endAngle - startAngle) * progress;

                // 正弦波偏移，增强火焰飘动感
                double waveOffset = Math.sin(progress * Math.PI * ElementalVisualConfig.fireMeleeWaveFrequency)
                        * ElementalVisualConfig.fireMeleeWaveAmplitude;
                Vec3 horizontalOffset = right.scale(Math.sin(angle) * (radius + waveOffset));
                Vec3 forwardOffset = look.scale(Math.cos(angle) * (radius + waveOffset) * 0.7);
                Vec3 pos = centerPos.add(horizontalOffset).add(forwardOffset);

                // 基础火焰粒子（始终存在）
                level.addParticle(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 0, -0.01, 0);

                // 等级2：增加灵魂火焰粒子
                if (tier >= 2 && ElementalVisualConfig.fireMeleeEnableSoulFlame
                        && RANDOM.nextFloat() < ElementalVisualConfig.fireMeleeSoulFlameChance) {
                    level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y, pos.z, 0, -0.01, 0);
                }

                // 等级3：增加熔岩粒子
                if (tier >= 3 && ElementalVisualConfig.fireMeleeEnableLava
                        && RANDOM.nextFloat() < ElementalVisualConfig.fireMeleeLavaChance) {
                    level.addParticle(ParticleTypes.LAVA, pos.x, pos.y, pos.z, 0, -0.02, 0);
                }

                // 等级4：增加烟雾粒子
                if (tier >= 4 && ElementalVisualConfig.fireMeleeEnableSoul
                        && RANDOM.nextFloat() < ElementalVisualConfig.fireMeleeSoulChance) {
                    level.addParticle(ParticleTypes.SOUL, pos.x, pos.y, pos.z, 0, 0.02, 0);
                }
            }
        }
    }

    // ======================== 服务端远程特效（螺旋线结构） ========================

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
                        CompoundTag data = projectile.getPersistentData();
                        data.putString(TAG_ELEMENTAL_PROJECTILE, type.getId());
                        data.putInt(TAG_PROJECTILE_TIER, tier);
                        data.putInt(TAG_SPAWN_TICK, projectile.tickCount);
                        ACTIVE_PROJECTILES.add(projectile);
                    }
                }
            }
        }
    }

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

                CompoundTag data = p.getPersistentData();
                if (data == null) {
                    iterator.remove();
                    continue;
                }

                Vec3 velocity = p.getDeltaMovement();
                double currentSpeedSq = velocity.lengthSqr();
                double movedDistSq = p.position().distanceToSqr(p.xo, p.yo, p.zo);
                boolean isStuck = movedDistSq < 0.000001;

                boolean shouldRemove = false;
                if (currentSpeedSq < 1e-7) {
                    shouldRemove = true;
                } else if (isStuck) {
                    shouldRemove = true;
                }

                if (shouldRemove) {
                    iterator.remove();
                    continue;
                }

                if (enabled && p.level() instanceof ServerLevel serverLevel) {
                    boolean visible = true;
                    if (ElementalVisualConfig.globalVisibilityCheckEnabled) {
                        visible = isProjectileVisible(serverLevel, p);
                    }
                    if (!visible) {
                        continue;
                    }
                    int tier = data.getInt(TAG_PROJECTILE_TIER);
                    spawnFireHelixTrail(serverLevel, p, tier);
                }
            }
        }
    }

    private static boolean isProjectileVisible(ServerLevel level, Projectile projectile) {
        int viewDistanceBlocks = level.getServer().getPlayerList().getViewDistance() * 16;
        double multiplier = ElementalVisualConfig.globalViewDistanceMultiplier;
        double thresholdSq = Math.pow(viewDistanceBlocks * multiplier, 2);
        Vec3 pos = projectile.position();
        for (Player player : level.players()) {
            if (player.distanceToSqr(pos) <= thresholdSq) {
                return true;
            }
        }
        return false;
    }

    private static void spawnFireHelixTrail(ServerLevel level, Projectile p, int tier) {
        if (tier <= 0) return;
        Vec3 velocity = p.getDeltaMovement();
        if (velocity.lengthSqr() < 1e-7) return;

        CompoundTag data = p.getPersistentData();
        if (data == null) return;
        int spawnTick = data.getInt(TAG_SPAWN_TICK);
        int elapsed = p.tickCount - spawnTick;

        // 外圈螺旋线（火焰粒子）
        int totalHelicesOuter = tier * ElementalVisualConfig.fireRangedOuterHelixCountPerTier;
        int activationInterval = ElementalVisualConfig.fireRangedActivationInterval;
        int activatedHelicesOuter = Math.min(totalHelicesOuter, elapsed / activationInterval + 1);
        if (activatedHelicesOuter < 1) activatedHelicesOuter = 1;

        Vec3 dir = velocity.normalize();
        Vec3 up = new Vec3(0, 1, 0);
        if (Math.abs(dir.y) > 0.95) up = new Vec3(1, 0, 0);
        Vec3 right = dir.cross(up).normalize();
        Vec3 realUp = right.cross(dir).normalize();

        double outerDirection = ElementalVisualConfig.fireRangedOuterReverseRotation ? -1 : 1;
        double baseAngle = outerDirection * p.tickCount * ElementalVisualConfig.fireRangedRotationSpeed;

        // 外圈螺旋线生成
        if (ElementalVisualConfig.fireRangedEnableOuterHelix) {
            for (int h = 0; h < activatedHelicesOuter; h++) {
                double helixAngle = baseAngle + (2 * Math.PI * h) / totalHelicesOuter;
                double radius = ElementalVisualConfig.fireRangedConeMaxRadius;
                double backDist = ElementalVisualConfig.fireRangedBackOffsetStart;

                Vec3 radial = right.scale(Math.cos(helixAngle) * radius)
                        .add(realUp.scale(Math.sin(helixAngle) * radius));
                Vec3 pos = p.position().subtract(dir.scale(backDist)).add(radial);

                level.sendParticles(ParticleTypes.FLAME,
                        pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            }
        }

        // 内圈螺旋线（灵魂火焰粒子）
        int totalHelicesInner = tier * ElementalVisualConfig.fireRangedInnerHelixCountPerTier;
        int innerDelay = ElementalVisualConfig.fireRangedInnerDelayTicks;
        int innerElapsed = Math.max(0, elapsed - innerDelay);
        int activatedHelicesInner = Math.min(totalHelicesInner, innerElapsed / activationInterval + 1);
        if (activatedHelicesInner < 1) activatedHelicesInner = 0;

        Vec3 tailPos = p.position().subtract(dir.scale(ElementalVisualConfig.fireRangedBackOffsetStart));
        double innerRadius = ElementalVisualConfig.fireRangedConeMaxRadius
                * ElementalVisualConfig.fireRangedInnerRadiusFactor;
        double innerDirection = ElementalVisualConfig.fireRangedInnerReverseRotation ? -1 : 1;
        double innerBaseAngle = innerDirection * p.tickCount * ElementalVisualConfig.fireRangedRotationSpeed;

        if (ElementalVisualConfig.fireRangedEnableInnerHelix) {
            for (int h = 0; h < activatedHelicesInner; h++) {
                double helixAngle = innerBaseAngle + (2 * Math.PI * h) / totalHelicesInner;
                Vec3 radial = right.scale(Math.cos(helixAngle) * innerRadius)
                        .add(realUp.scale(Math.sin(helixAngle) * innerRadius));
                Vec3 pos = tailPos.add(radial);
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            }
        }

        // 尾部特效：熔岩 + 灵魂粒子（散落）
        if (ElementalVisualConfig.fireRangedEnableTrailParticles) {
            int lavaCount = ElementalVisualConfig.fireRangedTrailLavaParticleCount;
            double lavaSpread = ElementalVisualConfig.fireRangedTrailLavaSpread;
            for (int i = 0; i < lavaCount; i++) {
                double offsetX = (RANDOM.nextDouble() - 0.5) * lavaSpread;
                double offsetY = (RANDOM.nextDouble() - 0.5) * lavaSpread;
                double offsetZ = (RANDOM.nextDouble() - 0.5) * lavaSpread;
                level.sendParticles(ParticleTypes.LAVA,
                        tailPos.x + offsetX, tailPos.y + offsetY, tailPos.z + offsetZ,
                        1, 0, 0, 0, 0);
            }

            int soulCount = ElementalVisualConfig.fireRangedTrailSoulParticleCount;
            double soulSpread = ElementalVisualConfig.fireRangedTrailSoulSpread;
            for (int i = 0; i < soulCount; i++) {
                double offsetX = (RANDOM.nextDouble() - 0.5) * soulSpread;
                double offsetY = (RANDOM.nextDouble() - 0.5) * soulSpread;
                double offsetZ = (RANDOM.nextDouble() - 0.5) * soulSpread;
                level.sendParticles(ParticleTypes.SOUL,
                        tailPos.x + offsetX, tailPos.y + offsetY, tailPos.z + offsetZ,
                        1, 0, 0, 0, 0);
            }
        }
    }

    // ======================== 命中特效 ========================
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
            if (data != null && data.contains(TAG_ELEMENTAL_PROJECTILE) && data.getString(TAG_ELEMENTAL_PROJECTILE).equals("fire")) {
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

    private static void playTieredImpactExplosion(Entity target, int tier) {
        if (!(target.level() instanceof ServerLevel serverLevel)) return;

        // 火焰粒子
        int flameCount = ElementalVisualConfig.fireImpactFlameParticleCountPerTier * tier;
        double flameSpread = ElementalVisualConfig.fireImpactFlameSpread;
        serverLevel.sendParticles(ParticleTypes.FLAME,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                flameCount, flameSpread, flameSpread, flameSpread, 0.1);

        // 等级 ≥ 3 的额外特效
        if (tier >= 3) {
            // 熔岩粒子
            if (ElementalVisualConfig.fireImpactLavaEnabled) {
                int lavaCount = ElementalVisualConfig.fireImpactLavaParticleCountPerTier * tier;
                double lavaSpread = ElementalVisualConfig.fireImpactLavaSpread;
                serverLevel.sendParticles(ParticleTypes.LAVA,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                        lavaCount, lavaSpread, lavaSpread, lavaSpread, 0.2);
            }

            // 灵魂火焰粒子
            if (ElementalVisualConfig.fireImpactSoulFlameEnabled) {
                int soulFlameCount = ElementalVisualConfig.fireImpactSoulFlameCount;
                double soulFlameSpread = ElementalVisualConfig.fireImpactSoulFlameSpread;
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                        soulFlameCount, soulFlameSpread, soulFlameSpread, soulFlameSpread, 0.005);
            }

            // 篝火烟雾粒子
            if (ElementalVisualConfig.fireImpactCampfireSmokeEnabled) {
                int smokeCount = ElementalVisualConfig.fireImpactSmokeCount;
                double smokeSpreadXZ = ElementalVisualConfig.fireImpactSmokeSpreadXZ;
                double smokeSpreadY = ElementalVisualConfig.fireImpactSmokeSpreadY;
                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        target.getX(), target.getY() + target.getBbHeight() * 0.8, target.getZ(),
                        smokeCount, smokeSpreadXZ, smokeSpreadY, smokeSpreadXZ, 0.02);
            }
        }
    }
}