package com.xulai.elementalcraft.client;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ElementalVisualConfig;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
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
public class ThunderVisuals {

    private static final String TAG_ELEMENTAL_PROJECTILE = "EC_ElementalType";
    private static final String TAG_PROJECTILE_TIER = "EC_VisualTier";
    private static final String TAG_SPAWN_TICK = "EC_SpawnTick";

    private static final Random RANDOM = new Random();

    private static final Set<Projectile> ACTIVE_PROJECTILES = Collections.synchronizedSet(new HashSet<>());

    public static int calculateVisualTier(LivingEntity entity, ElementType type) {
        if (type != ElementType.THUNDER) return 0;
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
    public static class ThunderClientEvents {

        @SubscribeEvent
        public static void onLivingTick(LivingEvent.LivingTickEvent event) {
            LivingEntity entity = event.getEntity();
            if (!entity.level().isClientSide) return;
            if (!ElementalVisualConfig.thunderMeleeEnabled) return;

            if (entity.swinging && entity.swingTime == 1) {
                ItemStack stack = entity.getMainHandItem();
                if (!stack.getAttributeModifiers(EquipmentSlot.MAINHAND).containsKey(Attributes.ATTACK_DAMAGE)) {
                    return;
                }
                ElementType type = ElementUtils.getAttackElement(stack);
                if (type == ElementType.THUNDER) {
                    int tier = calculateVisualTier(entity, type);
                    if (tier > 0) {
                        playArcMeleeSwing(entity, tier);
                    }
                }
            }
        }

        private static void playArcMeleeSwing(LivingEntity entity, int tier) {
            Level level = entity.level();

            Vec3 look = entity.getLookAngle();
            Vec3 up = new Vec3(0, 1, 0);
            if (Math.abs(look.y) > 0.95) {
                up = new Vec3(1, 0, 0);
            }
            Vec3 right = look.cross(up).normalize();
            if (right.lengthSqr() < 0.001) right = new Vec3(1, 0, 0);

            double radius = ElementalVisualConfig.thunderMeleeRadius;
            Vec3 centerPos = entity.getEyePosition();

            double baseTotalAngle = Math.toRadians(ElementalVisualConfig.thunderMeleeBaseAngleDegrees);
            double angleMultiplier = ElementalVisualConfig.thunderMeleeAngleMultiplierBase
                    + tier * ElementalVisualConfig.thunderMeleeAngleMultiplierPerTier;
            double actualAngle = baseTotalAngle * angleMultiplier;
            double startAngle = actualAngle / 2.0;
            double endAngle = -actualAngle / 2.0;

            int particleCount = (int) (ElementalVisualConfig.thunderMeleeParticleCountBase * angleMultiplier)
                    + ElementalVisualConfig.thunderMeleeParticleCountOffset;

            for (int i = 0; i <= particleCount; i++) {
                double progress = (double) i / particleCount;
                double angle = startAngle + (endAngle - startAngle) * progress;

                Vec3 offset = right.scale(Math.sin(angle) * radius)
                        .add(look.scale(Math.cos(angle) * radius * ElementalVisualConfig.thunderMeleeForwardOffsetFactor));
                Vec3 pos = centerPos.add(offset);

                level.addParticle(ParticleTypes.GLOW,
                        pos.x, pos.y, pos.z, 0, ElementalVisualConfig.thunderMeleeFallSpeed, 0);

                if (tier >= 2 && RANDOM.nextFloat() < ElementalVisualConfig.thunderMeleeGlowChanceTier2) {
                    level.addParticle(ParticleTypes.GLOW,
                            pos.x, pos.y, pos.z, 0, ElementalVisualConfig.thunderMeleeFallSpeed, 0);
                }
                if (tier >= 3 && RANDOM.nextFloat() < ElementalVisualConfig.thunderMeleeReversePortalChanceTier3) {
                    level.addParticle(ParticleTypes.REVERSE_PORTAL,
                            pos.x, pos.y, pos.z, 0, ElementalVisualConfig.thunderMeleeFallSpeed, 0);
                }
            }

            if (tier >= 4 && ElementalVisualConfig.thunderMeleeArcLineEnabled) {
                Vec3 start = centerPos.add(right.scale(Math.sin(startAngle) * radius)
                        .add(look.scale(Math.cos(startAngle) * radius * ElementalVisualConfig.thunderMeleeForwardOffsetFactor)));
                Vec3 end = centerPos.add(right.scale(Math.sin(endAngle) * radius)
                        .add(look.scale(Math.cos(endAngle) * radius * ElementalVisualConfig.thunderMeleeForwardOffsetFactor)));
                double dist = start.distanceTo(end);
                int steps = (int) (dist * ElementalVisualConfig.thunderMeleeArcLineStepFactor);
                for (int s = 0; s <= steps; s++) {
                    double t = (double) s / steps;
                    double x = start.x + (end.x - start.x) * t;
                    double y = start.y + (end.y - start.y) * t;
                    double z = start.z + (end.z - start.z) * t;
                    if (s % 2 == 0) {
                        level.addParticle(ParticleTypes.GLOW, x, y, z, 0, ElementalVisualConfig.thunderMeleeFallSpeed, 0);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onProjectileJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (!ElementalVisualConfig.thunderRangedEnabled) return;

        if (event.getEntity() instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();
            if (owner instanceof LivingEntity shooter) {
                ItemStack weapon = shooter.getMainHandItem();
                ElementType type = ElementUtils.getAttackElement(weapon);
                if (type == ElementType.THUNDER) {
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
        boolean enabled = ElementalVisualConfig.thunderRangedEnabled;

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
                    if (ElementalVisualConfig.globalVisibilityCheckEnabled && !isProjectileVisible(serverLevel, p)) {
                        continue;
                    }
                    int tier = data.getInt(TAG_PROJECTILE_TIER);
                    spawnHelixTrail(serverLevel, p, tier);
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

    private static void spawnHelixTrail(ServerLevel level, Projectile p, int tier) {
        if (tier <= 0) return;
        Vec3 velocity = p.getDeltaMovement();
        if (velocity.lengthSqr() < 1e-7) return;

        CompoundTag data = p.getPersistentData();
        if (data == null) return;
        int spawnTick = data.getInt(TAG_SPAWN_TICK);
        int elapsed = p.tickCount - spawnTick;

        int totalHelices = tier * ElementalVisualConfig.thunderRangedHelixCountPerTier;
        int activationInterval = ElementalVisualConfig.thunderRangedActivationInterval;
        int activatedHelices = Math.min(totalHelices, elapsed / activationInterval + 1);
        if (activatedHelices < 1) activatedHelices = 1;

        Vec3 dir = velocity.normalize();
        Vec3 up = new Vec3(0, 1, 0);
        if (Math.abs(dir.y) > 0.95) up = new Vec3(1, 0, 0);
        Vec3 right = dir.cross(up).normalize();
        Vec3 realUp = right.cross(dir).normalize();

        double baseAngle = -p.tickCount * ElementalVisualConfig.thunderRangedRotationSpeed;

        for (int h = 0; h < activatedHelices; h++) {
            double helixAngle = baseAngle + (2 * Math.PI * h) / totalHelices;
            double radius = ElementalVisualConfig.thunderRangedConeMaxRadius;
            double backDist = ElementalVisualConfig.thunderRangedBackOffsetStart;

            Vec3 radial = right.scale(Math.cos(helixAngle) * radius)
                    .add(realUp.scale(Math.sin(helixAngle) * radius));
            Vec3 pos = p.position().subtract(dir.scale(backDist)).add(radial);

            level.sendParticles(ModParticles.THUNDER_SPARK_PERSISTENT.get(),
                    pos.x, pos.y, pos.z, ElementalVisualConfig.thunderRangedMainParticleCount, 0, 0, 0, 0);
        }

        Vec3 tailPos = p.position().subtract(dir.scale(ElementalVisualConfig.thunderRangedBackOffsetStart));
        if (tier >= 2 && ElementalVisualConfig.thunderRangedTailEndRodEnabled) {
            level.sendParticles(ParticleTypes.END_ROD,
                    tailPos.x, tailPos.y, tailPos.z, ElementalVisualConfig.thunderRangedTailEndRodCount, 0, 0, 0, 0);
        }
        if (tier >= 3 && ElementalVisualConfig.thunderRangedTailReversePortalEnabled) {
            int groups = ElementalVisualConfig.thunderRangedTailReversePortalGroups;
            int countPerGroup = ElementalVisualConfig.thunderRangedTailReversePortalCount;
            double spread = ElementalVisualConfig.thunderRangedTailReversePortalSpread;
            for (int i = 0; i < groups; i++) {
                double offsetX = (RANDOM.nextDouble() - 0.5) * spread;
                double offsetY = (RANDOM.nextDouble() - 0.5) * spread;
                double offsetZ = (RANDOM.nextDouble() - 0.5) * spread;
                level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                        tailPos.x + offsetX, tailPos.y + offsetY, tailPos.z + offsetZ,
                        countPerGroup, 0, 0, 0, 0);
            }
        }
        if (tier >= 4 && ElementalVisualConfig.thunderRangedTailDragonBreathEnabled) {
            int groups = ElementalVisualConfig.thunderRangedTailDragonBreathGroups;
            int countPerGroup = ElementalVisualConfig.thunderRangedTailDragonBreathCount;
            double spread = ElementalVisualConfig.thunderRangedTailDragonBreathSpread;
            for (int i = 0; i < groups; i++) {
                double offsetX = (RANDOM.nextDouble() - 0.5) * spread;
                double offsetY = (RANDOM.nextDouble() - 0.5) * spread;
                double offsetZ = (RANDOM.nextDouble() - 0.5) * spread;
                level.sendParticles(ParticleTypes.DRAGON_BREATH,
                        tailPos.x + offsetX, tailPos.y + offsetY, tailPos.z + offsetZ,
                        countPerGroup, 0, 0, 0, 0);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;

        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) return;

        int tier = 0;
        boolean isThunder = false;
        boolean isRanged = false;

        Entity directEntity = event.getSource().getDirectEntity();
        if (directEntity instanceof Projectile projectile) {
            isRanged = true;
            CompoundTag data = projectile.getPersistentData();
            if (data != null && data.contains(TAG_ELEMENTAL_PROJECTILE) && data.getString(TAG_ELEMENTAL_PROJECTILE).equals("thunder")) {
                isThunder = true;
                tier = data.getInt(TAG_PROJECTILE_TIER);
            }
        } else {
            ItemStack weapon = livingAttacker.getMainHandItem();
            ElementType type = ElementUtils.getAttackElement(weapon);
            if (type == ElementType.THUNDER) {
                isThunder = true;
                tier = calculateVisualTier(livingAttacker, type);
            }
        }

        if (isRanged) {
            if (!ElementalVisualConfig.thunderRangedEnabled) return;
        } else {
            if (!ElementalVisualConfig.thunderMeleeEnabled) return;
        }

        if (isThunder && tier > 0) {
            playThunderImpact(event.getEntity(), tier);
        }
    }

    private static void playThunderImpact(LivingEntity target, int tier) {
        if (!(target.level() instanceof ServerLevel serverLevel)) return;

        int glowCount = ElementalVisualConfig.thunderImpactGlowCountPerTier * tier;
        serverLevel.sendParticles(ParticleTypes.GLOW,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                glowCount,
                ElementalVisualConfig.thunderImpactGlowSpread,
                ElementalVisualConfig.thunderImpactGlowSpread,
                ElementalVisualConfig.thunderImpactGlowSpread,
                ElementalVisualConfig.thunderImpactGlowSpeed);

        int endRodCount = ElementalVisualConfig.thunderImpactEndRodCountPerTier * tier;
        serverLevel.sendParticles(ParticleTypes.END_ROD,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                endRodCount,
                ElementalVisualConfig.thunderImpactEndRodSpread,
                ElementalVisualConfig.thunderImpactEndRodSpread,
                ElementalVisualConfig.thunderImpactEndRodSpread,
                ElementalVisualConfig.thunderImpactEndRodSpeed);

        if (tier >= 2 && ElementalVisualConfig.thunderImpactExtraEndRodEnabled) {
            int extraCount = ElementalVisualConfig.thunderImpactExtraEndRodCountPerTier * tier;
            double hSpread = ElementalVisualConfig.thunderImpactExtraEndRodHorizontalSpread;
            boolean randomY = ElementalVisualConfig.thunderImpactExtraEndRodVerticalRandom;
            for (int i = 0; i < extraCount; i++) {
                double offsetX = (RANDOM.nextDouble() - 0.5) * hSpread;
                double offsetZ = (RANDOM.nextDouble() - 0.5) * hSpread;
                double offsetY = randomY ? RANDOM.nextDouble() * target.getBbHeight() : target.getBbHeight() * 0.5;
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        target.getX() + offsetX, target.getY() + offsetY, target.getZ() + offsetZ,
                        1, 0, 0, 0, 0);
            }
        }
    }
}