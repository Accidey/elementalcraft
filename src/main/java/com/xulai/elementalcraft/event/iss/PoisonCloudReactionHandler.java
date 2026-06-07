package com.xulai.elementalcraft.event.iss;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.config.ElementalISSIntegrationConfig;
import com.xulai.elementalcraft.enchantment.ModEnchantments;
import com.xulai.elementalcraft.event.ScorchedHandler;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.util.ElementDamageHelper;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.util.List;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class PoisonCloudReactionHandler {

    private static final String ISS_NAMESPACE = "irons_spellbooks";

    private static EntityType<?> cachedPoisonCloudType;
    private static boolean cacheInitialized;

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.level.isClientSide()) return;
        if (event.phase != TickEvent.Phase.END) return;

        int tierStep = ElementalISSIntegrationConfig.poisonCloudFireTierStep;
        if (tierStep <= 0) return;

        if (!cacheInitialized) {
            cachedPoisonCloudType = ForgeRegistries.ENTITY_TYPES.getValue(
                    new ResourceLocation("irons_spellbooks:poison_cloud"));
            cacheInitialized = true;
        }
        if (cachedPoisonCloudType == null) return;

        ServerLevel level = (ServerLevel) event.level;

        for (Entity entity : level.getEntities().getAll()) {
            if (entity.getType() != cachedPoisonCloudType) continue;

            AABB cloudBox = entity.getBoundingBox().inflate(0.5);
            List<Projectile> projectiles = level.getEntitiesOfClass(Projectile.class, cloudBox,
                    p -> p.isAlive() && p != entity
                            && !isIssEntity(p)
                            && getFireStrikeLevel(p) > 0);

            if (!projectiles.isEmpty()) {
                triggerReaction(entity, projectiles.get(0));
                break;
            }

            LivingEntity auraSource = findScorchedAuraOverlap(level, entity, cloudBox);
            if (auraSource != null) {
                triggerReactionFromScorch(entity, auraSource);
                break;
            }
        }
    }

    private static void triggerReaction(Entity cloud, Projectile projectile) {
        LivingEntity attacker = null;
        if (projectile.getOwner() instanceof LivingEntity living) {
            attacker = living;
        }

        int firePower = 0;
        if (attacker != null) {
            firePower = ElementUtils.getDisplayEnhancement(attacker, ElementType.FIRE);
        }

        executeBlast(cloud, attacker, firePower);
    }

    private static void triggerReactionFromScorch(Entity cloud, LivingEntity scorchedEntity) {
        CompoundTag data = scorchedEntity.getPersistentData();
        int firePower = ScorchedHandler.getScorchFireStrength(scorchedEntity);

        LivingEntity attacker = null;
        UUID attackerUUID = ScorchedHandler.getScorchedAttackerUUID(scorchedEntity);
        if (attackerUUID != null) {
            Level level = cloud.level();
            if (level instanceof ServerLevel serverLevel) {
                for (Entity candidate : serverLevel.getAllEntities()) {
                    if (candidate instanceof LivingEntity living && living.getUUID().equals(attackerUUID)) {
                        attacker = living;
                        break;
                    }
                }
            }
        }

        executeBlast(cloud, attacker, firePower);
    }

    private static void executeBlast(Entity cloud, LivingEntity attacker, int firePower) {
        int tierStep = ElementalISSIntegrationConfig.poisonCloudFireTierStep;
        int extraStacks = (firePower + tierStep - 1) / tierStep;
        double effectiveFirePower = Math.max(firePower,
                ElementalFireNatureReactionsConfig.blastTriggerThreshold);
        float baseDamage = (float) (ElementalFireNatureReactionsConfig.blastBaseDamage
                + extraStacks * ElementalFireNatureReactionsConfig.blastGrowthDamage);
        double blastRadius = ElementalFireNatureReactionsConfig.blastBaseRange
                + extraStacks * ElementalFireNatureReactionsConfig.blastGrowthRange;
        int scorchDuration = (int) ((ElementalFireNatureReactionsConfig.blastBaseScorchTime
                + extraStacks * ElementalFireNatureReactionsConfig.blastGrowthScorchTime) * 20);

        LivingEntity killCredit = attacker;
        Level level = cloud.level();
        double x = cloud.getX();
        double y = cloud.getY();
        double z = cloud.getZ();
        AABB cloudBox = cloud.getBoundingBox();

        cloud.discard();

        level.playSound(null, x, y, z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F, 0.7F);

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    x, y + 1.0, z, 1, 0, 0, 0, 0);
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    x, y + 0.5, z, 50, 1.5, 1.5, 1.5, 0.2);
            serverLevel.sendParticles(ParticleTypes.LAVA,
                    x, y + 0.5, z, 20, 1.0, 1.0, 1.0, 0.0);

            AABB area = cloudBox.inflate(blastRadius);
            List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area);
            for (LivingEntity entity : nearby) {
                float mitigation = calculateBlastMitigation(entity);
                float finalDamage = baseDamage * (1.0f - mitigation);
                LivingEntity credit = killCredit != null ? killCredit : entity;
                ElementDamageHelper.applyDamage(entity, finalDamage,
                        ModDamageTypes.source(level, ModDamageTypes.TOXIC_BLAST, credit));
                int actualDuration = scorchDuration;
                float actualDmgMult = 1.0f;
                if (entity.hasEffect(MobEffects.POISON)) {
                    entity.removeEffect(MobEffects.POISON);
                    actualDuration = (int) (scorchDuration * ElementalFireNatureReactionsConfig.poisonScorchDurationMultiplier);
                    actualDmgMult = (float) ElementalFireNatureReactionsConfig.poisonScorchDamageMultiplier;
                }
                ScorchedHandler.applyScorched(entity, credit, (int) effectiveFirePower,
                        actualDuration, (int) effectiveFirePower, actualDmgMult, true);
            }
        }
    }

    private static LivingEntity findScorchedAuraOverlap(ServerLevel level, Entity cloud, AABB cloudBox) {
        double auraRadius = ElementalFireNatureReactionsConfig.scorchedAuraRadius;
        int auraThreshold = ElementalFireNatureReactionsConfig.scorchedAuraFirePowerThreshold;
        if (auraRadius <= 0 || auraThreshold <= 0) return null;

        AABB searchBox = cloudBox.inflate(auraRadius);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e.isAlive() && ScorchedHandler.isScorched(e));

        for (LivingEntity e : candidates) {
            CompoundTag data = e.getPersistentData();
            int sourceFirePower = ScorchedHandler.getScorchFireStrength(e);
            if (sourceFirePower < auraThreshold) continue;

            double dx = Math.max(cloudBox.minX - e.getX(), Math.max(0, e.getX() - cloudBox.maxX));
            double dy = Math.max(cloudBox.minY - e.getY(), Math.max(0, e.getY() - cloudBox.maxY));
            double dz = Math.max(cloudBox.minZ - e.getZ(), Math.max(0, e.getZ() - cloudBox.maxZ));
            if (dx * dx + dy * dy + dz * dz <= auraRadius * auraRadius) {
                return e;
            }
        }
        return null;
    }

    private static int getFireStrikeLevel(Projectile projectile) {
        if (projectile instanceof ThrownTrident trident) {
            try {
                Field f = ThrownTrident.class.getDeclaredField("tridentItem");
                f.setAccessible(true);
                ItemStack tridentStack = (ItemStack) f.get(trident);
                if (tridentStack != null && !tridentStack.isEmpty()) {
                    return tridentStack.getEnchantmentLevel(ModEnchantments.FIRE_STRIKE.get());
                }
            } catch (Exception ignored) {
            }
        }
        if (projectile instanceof AbstractArrow arrow) {
            if (arrow.isOnFire()) {
                return 1;
            }
            if (isArrowInGround(arrow)) {
                return 0;
            }
        }
        if (projectile.getDeltaMovement().lengthSqr() >= 0.01) {
            if (projectile.getOwner() instanceof LivingEntity living) {
                ItemStack mainHand = living.getMainHandItem();
                ItemStack offHand = living.getOffhandItem();
                int level = mainHand.getEnchantmentLevel(ModEnchantments.FIRE_STRIKE.get());
                if (level > 0) return level;
                level = offHand.getEnchantmentLevel(ModEnchantments.FIRE_STRIKE.get());
                if (level > 0) return level;
                level = mainHand.getEnchantmentLevel(Enchantments.FLAMING_ARROWS);
                if (level > 0) return level;
                level = offHand.getEnchantmentLevel(Enchantments.FLAMING_ARROWS);
                if (level > 0) return level;
            }
        }
        return 0;
    }

    private static boolean isArrowInGround(AbstractArrow arrow) {
        return !arrow.level().noCollision(arrow, arrow.getBoundingBox().inflate(0.06));
    }

    private static boolean isIssEntity(Entity entity) {
        return ForgeRegistries.ENTITY_TYPES.getKey(entity.getType())
                .getNamespace().equals(ISS_NAMESPACE);
    }

    private static float calculateBlastMitigation(LivingEntity entity) {
        int blastProtLevel = 0;
        int generalProtLevel = 0;
        for (ItemStack stack : entity.getArmorSlots()) {
            blastProtLevel += stack.getEnchantmentLevel(Enchantments.BLAST_PROTECTION);
            generalProtLevel += stack.getEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION);
        }
        double maxBlastCap = ElementalFireNatureReactionsConfig.blastMaxBlastProtCap;
        double maxGeneralCap = ElementalFireNatureReactionsConfig.blastMaxGeneralProtCap;
        double denominator = ElementalFireNatureReactionsConfig.enchantmentCalculationDenominator;
        double blastFactor = maxBlastCap / denominator;
        double generalFactor = maxGeneralCap / denominator;
        double actualBlastRed = Math.min(blastProtLevel * blastFactor, maxBlastCap);
        double actualGeneralRed = Math.min(generalProtLevel * generalFactor, maxGeneralCap);
        return (float) Math.min(actualBlastRed + actualGeneralRed, 1.0);
    }
}
