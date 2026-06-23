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
public class NatureVisuals {

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


    @Mod.EventBusSubscriber(modid = ElementalCraft.MODID, value = Dist.CLIENT)
    public static class NatureClientEvents {

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
                ElementType type = ElementUtils.getConsistentAttackElement(entity);
                if (type == ElementType.NATURE) {
                    int tier = calculateVisualTier(entity, type);
                    if (tier > 0) {
                        playBlossomMeleeSwing(entity, tier);
                    }
                }
            }
        }

        private static void playBlossomMeleeSwing(LivingEntity entity, int tier) {
            Level level = entity.level();
            Vec3 look = entity.getLookAngle();
            Vec3 up = new Vec3(0, 1, 0);
            if (Math.abs(look.y) > 0.95) {
                up = new Vec3(1, 0, 0);
            }
            Vec3 right = look.cross(up).normalize();
            if (right.lengthSqr() < 0.001) right = new Vec3(1, 0, 0);

            double radius = ElementalVisualConfig.natureMeleeRadius;
            Vec3 centerPos = entity.getEyePosition();

            double baseTotalAngle = Math.toRadians(ElementalVisualConfig.natureMeleeBaseAngleDegrees);
            double angleMultiplier = ElementalVisualConfig.natureMeleeAngleMultiplierBase
                    + tier * ElementalVisualConfig.natureMeleeAngleMultiplierPerTier;
            double actualAngle = baseTotalAngle * angleMultiplier;
            double startAngle = actualAngle / 2.0;
            double endAngle = -actualAngle / 2.0;

            int particleCount = (int) (ElementalVisualConfig.natureMeleeParticleCountBase * angleMultiplier)
                    + ElementalVisualConfig.natureMeleeParticleCountOffset;

            for (int i = 0; i <= particleCount; i++) {
                double progress = (double) i / particleCount;
                double angle = startAngle + (endAngle - startAngle) * progress;

                double waveOffset = Math.sin(progress * Math.PI * ElementalVisualConfig.natureMeleeWaveFrequency)
                        * ElementalVisualConfig.natureMeleeWaveAmplitude;
                Vec3 horizontalOffset = right.scale(Math.sin(angle) * (radius + waveOffset));
                Vec3 forwardOffset = look.scale(Math.cos(angle) * (radius + waveOffset) * 0.7);
                Vec3 p = centerPos.add(horizontalOffset).add(forwardOffset);

                double velX = look.x * ElementalVisualConfig.natureMeleeComposterSpeedXZ;
                double velZ = look.z * ElementalVisualConfig.natureMeleeComposterSpeedXZ;
                level.addParticle(ParticleTypes.COMPOSTER, p.x, p.y, p.z, velX, 0, velZ);


                if (RANDOM.nextFloat() < ElementalVisualConfig.natureMeleeSporeBlossomChance) {
                    level.addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, p.x, p.y, p.z, 0, 0, 0);
                }


                if (tier >= 3 && ElementalVisualConfig.natureMeleeCherryLeavesEnabled
                        && progress > ElementalVisualConfig.natureMeleeCherryLeavesMinProgress
                        && RANDOM.nextFloat() < ElementalVisualConfig.natureMeleeCherryLeavesChance) {
                    level.addParticle(ParticleTypes.CHERRY_LEAVES, p.x, p.y, p.z, 0, 0, 0);
                }


                if (tier >= 4 && ElementalVisualConfig.natureMeleeWaxOnEnabled
                        && progress > ElementalVisualConfig.natureMeleeWaxOnMinProgress) {
                    level.addParticle(ParticleTypes.WAX_ON, p.x, p.y, p.z, 0, 0, 0);
                }
            }
        }
    }



    @SubscribeEvent
    public static void onProjectileJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (!ElementalVisualConfig.natureRangedEnabled) return;

        if (event.getEntity() instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();
            if (owner instanceof LivingEntity shooter) {
                ElementType type = ElementUtils.getConsistentAttackElement(shooter);
                if (type == ElementType.NATURE) {
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
        boolean enabled = ElementalVisualConfig.natureRangedEnabled;

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
                    spawnVineHelixTrail(serverLevel, p, tier);
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

    private static void spawnVineHelixTrail(ServerLevel level, Projectile p, int tier) {
        if (tier <= 0) return;
        Vec3 velocity = p.getDeltaMovement();
        if (velocity.lengthSqr() < 1e-7) return;

        CompoundTag data = p.getPersistentData();
        if (data == null) return;
        int spawnTick = data.getInt(TAG_SPAWN_TICK);
        int elapsed = p.tickCount - spawnTick;


        int totalHelices = tier * ElementalVisualConfig.natureRangedOuterHelixCountPerTier;
        int activationInterval = ElementalVisualConfig.natureRangedActivationInterval;
        int activatedHelices = Math.min(totalHelices, elapsed / activationInterval + 1);
        if (activatedHelices < 1) activatedHelices = 1;

        Vec3 dir = velocity.normalize();
        Vec3 up = new Vec3(0, 1, 0);
        if (Math.abs(dir.y) > 0.95) up = new Vec3(1, 0, 0);
        Vec3 right = dir.cross(up).normalize();
        Vec3 realUp = right.cross(dir).normalize();

        double outerDirection = ElementalVisualConfig.natureRangedOuterReverseRotation ? -1 : 1;
        double baseAngle = outerDirection * p.tickCount * ElementalVisualConfig.natureRangedRotationSpeed;


        for (int h = 0; h < activatedHelices; h++) {
            double helixAngle = baseAngle + (2 * Math.PI * h) / totalHelices;
            double radius = ElementalVisualConfig.natureRangedConeMaxRadius;
            double backDist = ElementalVisualConfig.natureRangedBackOffsetStart;

            Vec3 radial = right.scale(Math.cos(helixAngle) * radius)
                    .add(realUp.scale(Math.sin(helixAngle) * radius));
            Vec3 pos = p.position().subtract(dir.scale(backDist)).add(radial);

            level.sendParticles(ParticleTypes.CHERRY_LEAVES,
                    pos.x, pos.y, pos.z,
                    ElementalVisualConfig.natureRangedMainParticleCount, 0, 0, 0, 0);
        }


        int tailTotalHelices = tier * ElementalVisualConfig.natureRangedTailHelixCountPerTier;
        int tailDelay = ElementalVisualConfig.natureRangedTailDelayTicks;
        int tailElapsed = Math.max(0, elapsed - tailDelay);
        int tailActivatedHelices = Math.min(tailTotalHelices, tailElapsed / activationInterval + 1);
        if (tailActivatedHelices < 1) tailActivatedHelices = 0;

        Vec3 tailPos = p.position().subtract(dir.scale(ElementalVisualConfig.natureRangedBackOffsetStart));
        double tailRadius = ElementalVisualConfig.natureRangedConeMaxRadius
                * ElementalVisualConfig.natureRangedTailRadiusFactor;
        double tailDirection = ElementalVisualConfig.natureRangedTailReverseRotation ? -1 : 1;
        double tailBaseAngle = tailDirection * p.tickCount * ElementalVisualConfig.natureRangedRotationSpeed;


        for (int h = 0; h < tailActivatedHelices; h++) {
            double helixAngle = tailBaseAngle + (2 * Math.PI * h) / tailTotalHelices;
            Vec3 radial = right.scale(Math.cos(helixAngle) * tailRadius)
                    .add(realUp.scale(Math.sin(helixAngle) * tailRadius));
            Vec3 pos = tailPos.add(radial);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    pos.x, pos.y, pos.z,
                    ElementalVisualConfig.natureRangedTailParticleCount, 0, 0, 0, 0);
        }


        if (ElementalVisualConfig.natureRangedCenterParticleEnabled) {
            level.sendParticles(ParticleTypes.CHERRY_LEAVES,
                    tailPos.x, tailPos.y, tailPos.z,
                    ElementalVisualConfig.natureRangedCenterParticleCount, 0, 0, 0, 0);
        }
    }


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
            if (data != null && data.contains(TAG_ELEMENTAL_PROJECTILE) && data.getString(TAG_ELEMENTAL_PROJECTILE).equals("nature")) {
                isNature = true;
                tier = data.getInt(TAG_PROJECTILE_TIER);
            }
        } else {
            ElementType type = ElementUtils.getConsistentAttackElement(livingAttacker);
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

    private static void playOvergrowthImpact(Entity target, int tier) {
        if (!(target.level() instanceof ServerLevel serverLevel)) return;


        int happyCount = ElementalVisualConfig.natureImpactHappyVillagerCountPerTier * tier;
        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                happyCount,
                ElementalVisualConfig.natureImpactHappyVillagerSpread,
                ElementalVisualConfig.natureImpactHappyVillagerSpread,
                ElementalVisualConfig.natureImpactHappyVillagerSpread,
                ElementalVisualConfig.natureImpactHappyVillagerSpeed);


        if (tier >= 3) {

            if (ElementalVisualConfig.natureImpactSporeBlossomEnabled) {
                serverLevel.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                        target.getX(), target.getY() + 0.2, target.getZ(),
                        ElementalVisualConfig.natureImpactSporeBlossomCount,
                        ElementalVisualConfig.natureImpactSporeBlossomSpreadXZ,
                        ElementalVisualConfig.natureImpactSporeBlossomSpreadY,
                        ElementalVisualConfig.natureImpactSporeBlossomSpreadXZ,
                        ElementalVisualConfig.natureImpactSporeBlossomSpeed);
            }


            if (ElementalVisualConfig.natureImpactCherryLeavesEnabled) {
                serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES,
                        target.getX(), target.getY() + target.getBbHeight() + 0.5, target.getZ(),
                        ElementalVisualConfig.natureImpactCherryLeavesCount,
                        ElementalVisualConfig.natureImpactCherryLeavesSpreadXZ,
                        ElementalVisualConfig.natureImpactCherryLeavesSpreadY,
                        ElementalVisualConfig.natureImpactCherryLeavesSpreadXZ,
                        ElementalVisualConfig.natureImpactCherryLeavesSpeed);
            }
        }
    }
}
