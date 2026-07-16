package com.xulai.elementalcraft.client;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ElementalVisualConfig;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import com.xulai.elementalcraft.client.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
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
public class FrostVisuals {

    private static final String TAG_ELEMENTAL_PROJECTILE = "EC_ElementalType";
    private static final String TAG_PROJECTILE_TIER = "EC_VisualTier";
    private static final String TAG_SPAWN_TICK = "EC_SpawnTick";

    private static final Random RANDOM = new Random();

    private static final Set<Projectile> ACTIVE_PROJECTILES = Collections.synchronizedSet(new HashSet<>());

    public static int calculateVisualTier(LivingEntity entity, ElementType type) {
        if (type != ElementType.FROST) return 0;
        int totalPoints = ElementUtils.getDisplayEnhancement(entity, type);
        int cap = ElementalConfig.getMaxStatCap();
        if (cap <= 0) cap = 100;
        if (totalPoints < cap) return 0;
        if (totalPoints < cap * 2) return 1;
        if (totalPoints < cap * 3) return 2;
        if (totalPoints < cap * 4) return 3;
        return 4;
    }


    @Mod.EventBusSubscriber(modid = ElementalCraft.MODID, value = Dist.CLIENT)
    public static class FrostClientEvents {

        @SubscribeEvent
        public static void onLivingTick(LivingEvent.LivingTickEvent event) {
            LivingEntity entity = event.getEntity();
            if (!entity.level().isClientSide) return;
            if (!ElementalVisualConfig.frostMeleeEnabled) return;

            if (entity.swinging && entity.swingTime == 1) {
                ItemStack stack = entity.getMainHandItem();
                if (!stack.getAttributeModifiers(EquipmentSlot.MAINHAND).containsKey(Attributes.ATTACK_DAMAGE)) {
                    return;
                }
                ElementType type = ElementUtils.getConsistentAttackElement(entity);
                if (type == ElementType.FROST) {
                    int tier = calculateVisualTier(entity, type);
                    if (tier > 0) {
                        playFrostMeleeSwing(entity, tier);
                    }
                }
            }
        }

        private static void playFrostMeleeSwing(LivingEntity entity, int tier) {
            Level level = entity.level();
            Vec3 look = entity.getLookAngle();
            Vec3 up = new Vec3(0, 1, 0);
            if (Math.abs(look.y) > 0.95) {
                up = new Vec3(1, 0, 0);
            }
            Vec3 right = look.cross(up).normalize();
            if (right.lengthSqr() < 0.001) right = new Vec3(1, 0, 0);

            Vec3 centerPos = entity.getEyePosition();

            double baseTotalAngle = Math.toRadians(ElementalVisualConfig.frostMeleeBaseAngleDegrees);
            double angleMultiplier = ElementalVisualConfig.frostMeleeAngleMultiplierBase
                    + tier * ElementalVisualConfig.frostMeleeAngleMultiplierPerTier;
            double actualAngle = baseTotalAngle * angleMultiplier;
            double startAngle = actualAngle / 2.0;
            double endAngle = -actualAngle / 2.0;

            int particleCount = (int) (ElementalVisualConfig.frostMeleeParticleCountBase * angleMultiplier)
                    + ElementalVisualConfig.frostMeleeParticleCountOffset;

            Vec3 startPos = null;
            Vec3 endPos = null;

            for (int i = 0; i <= particleCount; i++) {
                double progress = (double) i / particleCount;
                double angle = startAngle + (endAngle - startAngle) * progress;

                double waveOffset = Math.sin(progress * Math.PI * ElementalVisualConfig.frostMeleeWaveFrequency)
                        * ElementalVisualConfig.frostMeleeWaveAmplitude;
                Vec3 horizontalOffset = right.scale(Math.sin(angle) * (ElementalVisualConfig.frostMeleeRadius + waveOffset));
                Vec3 forwardOffset = look.scale(Math.cos(angle) * (ElementalVisualConfig.frostMeleeRadius + waveOffset) * 0.7);
                Vec3 pos = centerPos.add(horizontalOffset).add(forwardOffset);

                level.addParticle(ParticleTypes.SNOWFLAKE, pos.x, pos.y, pos.z, 0, -0.01, 0);

                if (tier >= 2 && RANDOM.nextFloat() < ElementalVisualConfig.frostMeleeGlowChanceTier2) {
                    level.addParticle(ModParticles.frostSnowflake(), pos.x, pos.y, pos.z, 0, 0, 0);
                }

                if (tier >= 3) {
                    if (RANDOM.nextFloat() < ElementalVisualConfig.frostMeleeShardChanceTier3) {
                        double vx = (RANDOM.nextDouble() - 0.5) * 0.1;
                        double vy = -0.05 + RANDOM.nextDouble() * 0.02;
                        double vz = (RANDOM.nextDouble() - 0.5) * 0.1;
                        level.addParticle(ParticleTypes.ITEM_SNOWBALL, pos.x, pos.y, pos.z, vx, vy, vz);
                    }
                    if (RANDOM.nextFloat() < ElementalVisualConfig.frostMeleeMistChanceTier3) {
                        level.addParticle(ParticleTypes.SNOWFLAKE, pos.x, pos.y, pos.z, 0, 0.01, 0);
                    }
                }

                if (tier >= 4 && RANDOM.nextFloat() < ElementalVisualConfig.frostMeleeAshChanceTier4) {
                    level.addParticle(ParticleTypes.SNOWFLAKE, pos.x, pos.y, pos.z, 0, -0.05, 0);
                }

                if (i == 0) {
                    startPos = pos;
                }
                if (i == particleCount) {
                    endPos = pos;
                }
            }

            if (tier >= 4 && ElementalVisualConfig.frostMeleeMistLineEnabled && startPos != null && endPos != null) {
                double dist = startPos.distanceTo(endPos);
                int steps = (int) (dist * ElementalVisualConfig.frostMeleeMistLineStepFactor);
                for (int s = 0; s <= steps; s++) {
                    double t = (double) s / steps;
                    double x = startPos.x + (endPos.x - startPos.x) * t;
                    double y = startPos.y + (endPos.y - startPos.y) * t;
                    double z = startPos.z + (endPos.z - startPos.z) * t;
                    if (s % 3 == 0) {
                        level.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, 0, 0.01, 0);
                    }
                }
            }
        }
    }


    @SubscribeEvent
    public static void onProjectileJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (!ElementalVisualConfig.frostRangedEnabled) return;

        if (event.getEntity() instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();
            if (owner instanceof LivingEntity shooter) {
                ElementType type = ElementUtils.getConsistentAttackElement(shooter);
                if (type == ElementType.FROST) {
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

        boolean enabled = ElementalVisualConfig.frostRangedEnabled;

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
                    spawnFrostHelixTrail(serverLevel, p, tier);
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

    private static void spawnFrostHelixTrail(ServerLevel level, Projectile p, int tier) {
        if (tier <= 0) return;
        Vec3 velocity = p.getDeltaMovement();
        if (velocity.lengthSqr() < 1e-7) return;

        CompoundTag data = p.getPersistentData();
        if (data == null) return;
        int spawnTick = data.getInt(TAG_SPAWN_TICK);
        int elapsed = p.tickCount - spawnTick;

        Vec3 dir = velocity.normalize();
        Vec3 up = new Vec3(0, 1, 0);
        if (Math.abs(dir.y) > 0.95) up = new Vec3(1, 0, 0);
        Vec3 right = dir.cross(up).normalize();
        Vec3 realUp = right.cross(dir).normalize();

        int totalHelicesOuter = tier * ElementalVisualConfig.frostRangedHelixCountPerTier;
        int activationInterval = ElementalVisualConfig.frostRangedActivationInterval;
        int activatedHelicesOuter = Math.min(totalHelicesOuter, elapsed / activationInterval + 1);
        if (activatedHelicesOuter < 1) activatedHelicesOuter = 1;

        double baseAngle = p.tickCount * ElementalVisualConfig.frostRangedRotationSpeed;

        for (int h = 0; h < activatedHelicesOuter; h++) {
            double helixAngle = baseAngle + (2 * Math.PI * h) / totalHelicesOuter;
            double radius = ElementalVisualConfig.frostRangedConeMaxRadius;
            double backDist = ElementalVisualConfig.frostRangedBackOffsetStart;

            Vec3 radial = right.scale(Math.cos(helixAngle) * radius)
                    .add(realUp.scale(Math.sin(helixAngle) * radius));
            Vec3 pos = p.position().subtract(dir.scale(backDist)).add(radial);

            level.sendParticles(ParticleTypes.SNOWFLAKE,
                    pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        }

        if (ElementalVisualConfig.frostRangedInnerCoreEnabled) {
            int innerDelay = ElementalVisualConfig.frostRangedInnerDelayTicks;
            int innerElapsed = Math.max(0, elapsed - innerDelay);
            int totalHelicesInner = tier * ElementalVisualConfig.frostRangedHelixCountPerTier;
            int activatedHelicesInner = Math.min(totalHelicesInner, innerElapsed / activationInterval + 1);
            if (activatedHelicesInner < 1) activatedHelicesInner = 0;

            Vec3 corePos = p.position().subtract(dir.scale(ElementalVisualConfig.frostRangedBackOffsetStart));
            double innerRadius = ElementalVisualConfig.frostRangedConeMaxRadius * ElementalVisualConfig.frostRangedInnerRadiusFactor;
            double innerBaseAngle = -p.tickCount * ElementalVisualConfig.frostRangedRotationSpeed * 1.5;

            for (int h = 0; h < activatedHelicesInner; h++) {
                double helixAngle = innerBaseAngle + (2 * Math.PI * h) / totalHelicesInner;
                Vec3 radial = right.scale(Math.cos(helixAngle) * innerRadius)
                        .add(realUp.scale(Math.sin(helixAngle) * innerRadius));
                Vec3 pos = corePos.add(radial);

                level.sendParticles(ModParticles.frostSnowflake(),
                        pos.x, pos.y, pos.z, ElementalVisualConfig.frostRangedInnerCoreCount, 0, 0, 0, 0);
            }
        }

        Vec3 tailPos = p.position().subtract(dir.scale(ElementalVisualConfig.frostRangedBackOffsetStart));

        if (tier >= 2 && ElementalVisualConfig.frostRangedTailMistEnabled) {
            for (int i = 0; i < ElementalVisualConfig.frostRangedTailMistCount; i++) {
                double offsetX = (RANDOM.nextDouble() - 0.5) * ElementalVisualConfig.frostRangedTailMistSpread;
                double offsetY = (RANDOM.nextDouble() - 0.5) * ElementalVisualConfig.frostRangedTailMistSpread;
                double offsetZ = (RANDOM.nextDouble() - 0.5) * ElementalVisualConfig.frostRangedTailMistSpread;
                level.sendParticles(ParticleTypes.SNOWFLAKE,
                        tailPos.x + offsetX, tailPos.y + offsetY, tailPos.z + offsetZ,
                        1, 0, 0.01, 0, 0);
            }
        }

        if (tier >= 3 && ElementalVisualConfig.frostRangedTailShardEnabled) {
            for (int i = 0; i < ElementalVisualConfig.frostRangedTailShardCount; i++) {
                double offsetX = (RANDOM.nextDouble() - 0.5) * ElementalVisualConfig.frostRangedTailShardSpread;
                double offsetY = (RANDOM.nextDouble() - 0.5) * ElementalVisualConfig.frostRangedTailShardSpread;
                double offsetZ = (RANDOM.nextDouble() - 0.5) * ElementalVisualConfig.frostRangedTailShardSpread;
                double vx = (RANDOM.nextDouble() - 0.5) * 0.05;
                double vy = -0.03;
                double vz = (RANDOM.nextDouble() - 0.5) * 0.05;
                level.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                        tailPos.x + offsetX, tailPos.y + offsetY, tailPos.z + offsetZ,
                        1, vx, vy, vz, 0);
            }
        }

        if (tier >= 4) {
            level.sendParticles(ParticleTypes.WHITE_ASH,
                    tailPos.x, tailPos.y, tailPos.z, 3, 0.3, 0.1, 0.3, 0);
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;

        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) return;

        int tier = 0;
        boolean isFrost = false;
        boolean isRanged = false;

        Entity directEntity = event.getSource().getDirectEntity();
        if (directEntity instanceof Projectile projectile) {
            isRanged = true;
            CompoundTag data = projectile.getPersistentData();
            if (data != null && data.contains(TAG_ELEMENTAL_PROJECTILE) && data.getString(TAG_ELEMENTAL_PROJECTILE).equals("frost")) {
                isFrost = true;
                tier = data.getInt(TAG_PROJECTILE_TIER);
            }
        } else {
            ElementType type = ElementUtils.getConsistentAttackElement(livingAttacker);
            if (type == ElementType.FROST) {
                isFrost = true;
                tier = calculateVisualTier(livingAttacker, type);
            }
        }

        if (isRanged) {
            if (!ElementalVisualConfig.frostRangedEnabled) return;
        } else {
            if (!ElementalVisualConfig.frostMeleeEnabled) return;
        }

        if (isFrost && tier > 0) {
            playFrostImpact(event.getEntity(), tier);
        }
    }

    private static void playFrostImpact(Entity target, int tier) {
        if (!(target.level() instanceof ServerLevel serverLevel)) return;

        double centerX = target.getX();
        double centerY = target.getY() + target.getBbHeight() * 0.5;
        double centerZ = target.getZ();

        int snowflakeCount = ElementalVisualConfig.frostImpactSnowflakeCountPerTier * tier;
        serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                centerX, centerY, centerZ,
                snowflakeCount,
                ElementalVisualConfig.frostImpactSnowflakeSpread,
                ElementalVisualConfig.frostImpactSnowflakeSpread,
                ElementalVisualConfig.frostImpactSnowflakeSpread,
                ElementalVisualConfig.frostImpactSnowflakeSpeed);

        int shardCount = ElementalVisualConfig.frostImpactShardCountPerTier * tier;
        serverLevel.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                centerX, centerY, centerZ,
                shardCount,
                ElementalVisualConfig.frostImpactShardSpread,
                ElementalVisualConfig.frostImpactShardSpread,
                ElementalVisualConfig.frostImpactShardSpread,
                ElementalVisualConfig.frostImpactShardSpeed);

        if (tier >= 2 && ElementalVisualConfig.frostImpactGlowEnabled) {
            int glowCount = ElementalVisualConfig.frostImpactGlowCountPerTier * tier;
            double hSpread = ElementalVisualConfig.frostImpactGlowSpread;
            for (int i = 0; i < glowCount; i++) {
                double offsetX = (RANDOM.nextDouble() - 0.5) * hSpread;
                double offsetZ = (RANDOM.nextDouble() - 0.5) * hSpread;
                double offsetY = RANDOM.nextDouble() * target.getBbHeight();
                serverLevel.sendParticles(ModParticles.frostSnowflake(),
                        target.getX() + offsetX, target.getY() + offsetY, target.getZ() + offsetZ,
                        1, 0, 0, 0, 0);
            }
        }

        if (tier >= 3 && ElementalVisualConfig.frostImpactMistEnabled) {
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                    centerX, centerY, centerZ,
                    ElementalVisualConfig.frostImpactMistCount * 2,
                    ElementalVisualConfig.frostImpactMistSpread,
                    ElementalVisualConfig.frostImpactMistSpread * 0.5,
                    ElementalVisualConfig.frostImpactMistSpread,
                    ElementalVisualConfig.frostImpactMistSpeed * 2.5);
        }

        if (tier >= 4 && ElementalVisualConfig.frostImpactBlizzardEnabled) {
            double ringRadius = ElementalVisualConfig.frostImpactBlizzardRingRadius;
            int ringCount = ElementalVisualConfig.frostImpactBlizzardRingCount;
            for (int i = 0; i < ringCount; i++) {
                double angle = (2 * Math.PI * i) / ringCount;
                double x = centerX + Math.cos(angle) * ringRadius;
                double z = centerZ + Math.sin(angle) * ringRadius;
                double y = centerY + (RANDOM.nextDouble() - 0.5) * 0.3;
                serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                        x, y, z, 1, 0, 0, 0, 0);
                if (i % 2 == 0) {
                    serverLevel.sendParticles(ModParticles.frostSnowflake(),
                            x, y, z, 1, 0, 0, 0, 0);
                }
            }

            for (int i = 0; i < ElementalVisualConfig.frostImpactBlizzardMistCount; i++) {
                double angle = RANDOM.nextDouble() * 2 * Math.PI;
                double dist = RANDOM.nextDouble() * ringRadius;
                double x = centerX + Math.cos(angle) * dist;
                double z = centerZ + Math.sin(angle) * dist;
                double y = centerY + (RANDOM.nextDouble() - 0.5) * 0.8;
                serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                        x, y, z, 1, 0, 0.01, 0, 0);
            }
        }
    }
}
