package com.xulai.elementalcraft.logic;

import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.event.FrostbiteHandler;
import com.xulai.elementalcraft.event.ScorchedHandler;
import com.xulai.elementalcraft.event.StaticShockHandler;
import com.xulai.elementalcraft.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import com.xulai.elementalcraft.config.ElementalISSIntegrationConfig;

import java.util.concurrent.ThreadLocalRandom;

@Mod.EventBusSubscriber(modid = com.xulai.elementalcraft.ElementalCraft.MODID)
public class MobAttributeLogic {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public static void processMob(Mob mob) {
        if (mob.getClass().getName().equals("io.redspace.ironsspellbooks.entity.mobs.SummonedPolarBear")) {
            return;
        }
        CompoundTag data = mob.getPersistentData();
        if (data.getBoolean("ElementalCraft_AttributesSet")) return;

        String entityId = net.minecraft.world.entity.EntityType.getKey(mob.getType()).toString();

        if (ElementalConfig.cachedBlacklist.contains(entityId)) {
            data.putBoolean("ElementalCraft_AttributesSet", true);
            return;
        }

        java.util.List<ForcedAttributeHelper.ForcedData> forcedList = ForcedAttributeHelper.getForcedDataList(mob.getType());
        ForcedAttributeHelper.ForcedData forced = null;
        if (!forcedList.isEmpty()) {
            forced = forcedList.get(ThreadLocalRandom.current().nextInt(forcedList.size()));
        }

        if (forced == null && ElementalConfig.netherForcedFire
                && mob.level().dimension() == Level.NETHER) {
            int points = ElementalConfig.netherFirePoints;
            forced = new ForcedAttributeHelper.ForcedData(
                    ElementType.FIRE, ElementType.FIRE, points,
                    ElementType.FIRE, points
            );
        }

        if (forced == null && ElementalConfig.endForcedThunder
                && mob.level().dimension() == Level.END) {
            int points = ElementalConfig.endThunderPoints;
            forced = new ForcedAttributeHelper.ForcedData(
                    ElementType.THUNDER, ElementType.THUNDER, points,
                    ElementType.THUNDER, points
            );
        }

        if (forced != null) {
            applyForcedAttributes(mob, data, forced);
            return;
        }

        boolean isNeutral = (mob instanceof NeutralMob) || entityId.equals("minecraft:piglin");
        boolean isMonster = (mob instanceof Monster);

        if (!isMonster && !isNeutral) {
            data.putBoolean("ElementalCraft_AttributesSet", true);
            return;
        }

        double chance = isNeutral ? ElementalConfig.mobChanceNeutral : ElementalConfig.mobChanceHostile;
        boolean willGenerate = ThreadLocalRandom.current().nextDouble() < chance;

        if (!willGenerate) {
            data.putBoolean("ElementalCraft_AttributesSet", true);
            return;
        }

        applyRandomAttributes(mob);
        data.putBoolean("ElementalCraft_AttributesSet", true);
    }

    private static void applyRandomAttributes(Mob mob) {
        ItemStack mainHand = mob.getMainHandItem();
        ItemStack offHand = mob.getOffhandItem();
        boolean hasHandItem = !mainHand.isEmpty() || !offHand.isEmpty();

        ElementType mainType = BiomeAttributeBias.getBiasedElement((ServerLevel) mob.level(), mob.blockPosition());

        ElementType attackType = null;
        if (ThreadLocalRandom.current().nextDouble() < ElementalConfig.attackChance) {
            attackType = mainType;
        }

        ElementType enhanceType = mainType;
        int enhanceTotalPoints = (hasHandItem || attackType != null) ? ElementalConfig.rollMonsterStrength() : 0;

        ElementType resistType;
        if (attackType != null && ThreadLocalRandom.current().nextDouble() < ElementalConfig.counterResistChance) {
            resistType = AttributeEquipUtils.getCounterElement(attackType);
        } else {
            resistType = AttributeEquipUtils.randomNonNoneElement();
        }
        int resistTotalPoints = ElementalConfig.rollMonsterResist();

        if (attackType != null) {
            String entityId = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()).toString();
            boolean isBlacklisted = ModList.get() != null && ModList.get().isLoaded("irons_spellbooks")
                    && ElementalISSIntegrationConfig.cachedCasterBlacklist != null
                    && ElementalISSIntegrationConfig.cachedCasterBlacklist.contains(entityId);
            double casterChance = ModList.get() != null && ModList.get().isLoaded("irons_spellbooks")
                    ? ElementalISSIntegrationConfig.casterMobChance : 0.0;
            boolean issCaster = !isBlacklisted && attackType == ElementType.THUNDER && ThreadLocalRandom.current().nextDouble() < casterChance;
            boolean natureCaster = !isBlacklisted && !issCaster && attackType == ElementType.NATURE && ThreadLocalRandom.current().nextDouble() < casterChance;
            boolean frostCaster = !isBlacklisted && !issCaster && !natureCaster && attackType == ElementType.FROST && ThreadLocalRandom.current().nextDouble() < casterChance;
            boolean fireCaster = !isBlacklisted && !issCaster && !natureCaster && !frostCaster && attackType == ElementType.FIRE && ThreadLocalRandom.current().nextDouble() < casterChance;
            if (issCaster) {
                mob.getPersistentData().putBoolean("EC_ISS_MobCaster", true);
                mob.getPersistentData().putString("EC_ISS_MobElement", "thunder");
            } else if (natureCaster) {
                mob.getPersistentData().putBoolean("EC_ISS_MobCaster", true);
                mob.getPersistentData().putString("EC_ISS_MobElement", "nature");
                if (!hasHandItem) {
                    ItemStack weapon = AttributeEquipUtils.createRandomWeapon();
                    AttributeEquipUtils.applyAttackEnchant(weapon, attackType);
                    AttributeEquipUtils.applyUnbreaking(weapon, 3);
                    mob.setItemSlot(EquipmentSlot.MAINHAND, weapon);
                    mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
                }
            } else if (frostCaster) {
                mob.getPersistentData().putBoolean("EC_ISS_MobCaster", true);
                mob.getPersistentData().putString("EC_ISS_MobElement", "frost");
            } else if (fireCaster) {
                mob.getPersistentData().putBoolean("EC_ISS_MobCaster", true);
                mob.getPersistentData().putString("EC_ISS_MobElement", "fire");
            } else if (hasHandItem) {
                if (!mainHand.isEmpty()) {
                    AttributeEquipUtils.applyAttackEnchant(mainHand, attackType);
                    AttributeEquipUtils.applyUnbreaking(mainHand, 3);
                }
                if (!offHand.isEmpty()) {
                    AttributeEquipUtils.applyAttackEnchant(offHand, attackType);
                    AttributeEquipUtils.applyUnbreaking(offHand, 3);
                }
            } else {
                ItemStack weapon = AttributeEquipUtils.createRandomWeapon();
                AttributeEquipUtils.applyAttackEnchant(weapon, attackType);
                AttributeEquipUtils.applyUnbreaking(weapon, 3);
                mob.setItemSlot(EquipmentSlot.MAINHAND, weapon);
                mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
            }
        }

        applyArmorAttributes(mob, enhanceType, enhanceTotalPoints, resistType, resistTotalPoints);

        CompoundTag dropData = mob.getPersistentData();
        dropData.putString("EC_DropElementType", mainType.getId());
        if (attackType != null) {
            dropData.putString("EC_DropAttackType", attackType.getId());
        }
        dropData.putInt("EC_DropEnhancePoints", enhanceTotalPoints);
        dropData.putInt("EC_DropResistPoints", resistTotalPoints);
        dropData.putString("EC_DropResistType", resistType.getId());

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            mob.setDropChance(slot, 0.0F);
        }
    }

    private static void applyForcedAttributes(Mob mob, CompoundTag persistentData, ForcedAttributeHelper.ForcedData data) {
        MinecraftServer server = mob.level().getServer();
        if (server == null) return;

        server.tell(new TickTask(server.getTickCount() + 1, () -> {
            if (!mob.isAlive()) return;

            ElementType attackType = data.attackType();
            ElementType enhanceType = data.enhanceType();
            int enhancePoints = data.enhancePoints();
            ElementType resistType = data.resistType();
            int resistPoints = data.resistPoints();

            ItemStack mainHand = mob.getMainHandItem();
            ItemStack offHand = mob.getOffhandItem();
            boolean hasWeapon = !mainHand.isEmpty() || !offHand.isEmpty();

            if (attackType != null && attackType != ElementType.NONE) {
                String entityId = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()).toString();
                boolean isBlacklisted = ModList.get() != null && ModList.get().isLoaded("irons_spellbooks")
                        && ElementalISSIntegrationConfig.cachedCasterBlacklist != null
                        && ElementalISSIntegrationConfig.cachedCasterBlacklist.contains(entityId);
                double casterChance = ModList.get() != null && ModList.get().isLoaded("irons_spellbooks")
                        ? ElementalISSIntegrationConfig.casterMobChance : 0.0;
                boolean issCaster = !isBlacklisted && attackType == ElementType.THUNDER && ThreadLocalRandom.current().nextDouble() < casterChance;
                boolean natureCaster = !isBlacklisted && !issCaster && attackType == ElementType.NATURE && ThreadLocalRandom.current().nextDouble() < casterChance;
                boolean frostCaster = !isBlacklisted && !issCaster && !natureCaster && attackType == ElementType.FROST && ThreadLocalRandom.current().nextDouble() < casterChance;
                boolean fireCaster = !isBlacklisted && !issCaster && !natureCaster && !frostCaster && attackType == ElementType.FIRE && ThreadLocalRandom.current().nextDouble() < casterChance;
                if (issCaster) {
                    persistentData.putBoolean("EC_ISS_MobCaster", true);
                    persistentData.putString("EC_ISS_MobElement", "thunder");
                } else if (natureCaster) {
                    persistentData.putBoolean("EC_ISS_MobCaster", true);
                    persistentData.putString("EC_ISS_MobElement", "nature");
                    if (!hasWeapon) {
                        ItemStack weapon = AttributeEquipUtils.createRandomWeapon();
                        AttributeEquipUtils.applyAttackEnchant(weapon, attackType);
                        AttributeEquipUtils.applyUnbreaking(weapon, 3);
                        mob.setItemSlot(EquipmentSlot.MAINHAND, weapon);
                        mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
                    }
                } else if (frostCaster) {
                    persistentData.putBoolean("EC_ISS_MobCaster", true);
                    persistentData.putString("EC_ISS_MobElement", "frost");
                } else if (fireCaster) {
                    persistentData.putBoolean("EC_ISS_MobCaster", true);
                    persistentData.putString("EC_ISS_MobElement", "fire");
                } else if (hasWeapon) {
                    if (!mainHand.isEmpty()) {
                        AttributeEquipUtils.applyAttackEnchant(mainHand, attackType);
                        AttributeEquipUtils.applyUnbreaking(mainHand, 3);
                    }
                    if (!offHand.isEmpty()) {
                        AttributeEquipUtils.applyAttackEnchant(offHand, attackType);
                        AttributeEquipUtils.applyUnbreaking(offHand, 3);
                    }
                } else {
                    ItemStack weapon = AttributeEquipUtils.createRandomWeapon();
                    AttributeEquipUtils.applyAttackEnchant(weapon, attackType);
                    AttributeEquipUtils.applyUnbreaking(weapon, 3);
                    mob.setItemSlot(EquipmentSlot.MAINHAND, weapon);
                    mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
                }
            }

            applyArmorAttributes(mob, enhanceType, enhancePoints, resistType, resistPoints);

            persistentData.putString("EC_DropElementType", enhanceType != null ? enhanceType.getId() : "");
            if (attackType != null && attackType != ElementType.NONE) {
                persistentData.putString("EC_DropAttackType", attackType.getId());
            }
            persistentData.putInt("EC_DropEnhancePoints", enhancePoints);
            persistentData.putInt("EC_DropResistPoints", resistPoints);
            persistentData.putString("EC_DropResistType", resistType != null ? resistType.getId() : "");

            persistentData.putBoolean("ElementalCraft_AttributesSet", true);

            for (EquipmentSlot slot : EquipmentSlot.values()) {
                mob.setDropChance(slot, 0.0F);
            }
        }));
    }

    private static void applyArmorAttributes(Mob mob, ElementType enhanceType, int enhanceTotalPoints,
                                             ElementType resistType, int resistTotalPoints) {

        int enhancePerLevel = ElementalConfig.getStrengthPerLevel();
        int resistPerLevel = ElementalConfig.getResistPerLevel();

        int[] enhanceLevels = AttributeEquipUtils.distributePointsToLevels(enhanceTotalPoints, enhancePerLevel, 4);
        int[] resistLevels = AttributeEquipUtils.distributePointsToLevels(resistTotalPoints, resistPerLevel, 4);

        for (int i = 0; i < 4; i++) {
            if (enhanceLevels[i] <= 0 && resistLevels[i] <= 0 && mob.getItemBySlot(ARMOR_SLOTS[i]).isEmpty()) {
                continue;
            }

            EquipmentSlot slot = ARMOR_SLOTS[i];
            ItemStack stack = mob.getItemBySlot(slot);

            if (stack.isEmpty()) {
                stack = AttributeEquipUtils.createRandomArmor(i);
                mob.setItemSlot(slot, stack);
                mob.setDropChance(slot, 0.0F);
            }

            AttributeEquipUtils.applyArmorEnchantsLevel(stack, enhanceType, enhanceLevels[i], resistType, resistLevels[i]);
            AttributeEquipUtils.applyUnbreaking(stack, 3);
        }
    }

    public static void clearAggro(LivingEntity target) {
        if (target instanceof Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
            target.getPersistentData().putInt(NBT_DISORIENTED, DISORIENTED_TICKS);
        }
    }

    private static final String NBT_FLEE_TARGET_X = "EC_FleeTargetX";
    private static final String NBT_FLEE_TARGET_Z = "EC_FleeTargetZ";
    private static final String NBT_FLEE_SOURCE_UUID = "EC_FleeSourceUUID";
    private static final String NBT_FLEE_SOURCE_RANGE = "EC_FleeSourceRange";
    private static final String NBT_FLEE_TICKS = "EC_FleeTicks";
    private static final String NBT_FLEE_STUCK_TICKS = "EC_FleeStuckTicks";
    private static final String NBT_FLEE_LAST_X = "EC_FleeLastX";
    private static final String NBT_FLEE_LAST_Z = "EC_FleeLastZ";
    private static final int MAX_FLEE_TICKS = 200;
    private static final String NBT_DISORIENTED = "ec_disoriented";
    private static final int DISORIENTED_TICKS = 10;
    private static final double FLEE_TRIGGER_DIST = 6.0;

    private static boolean isPathClear(LivingEntity entity, double startX, double startZ, double dirX, double dirZ, int distance) {
        for (int i = 1; i <= distance; i++) {
            int bx = (int) Math.floor(startX + dirX * i);
            int bz = (int) Math.floor(startZ + dirZ * i);
            int by = entity.getBlockY();
            BlockPos feet = new BlockPos(bx, by, bz);
            BlockPos head = feet.above();
            net.minecraft.world.level.block.state.BlockState feetState = entity.level().getBlockState(feet);
            net.minecraft.world.level.block.state.BlockState headState = entity.level().getBlockState(head);
            if (!feetState.getCollisionShape(entity.level(), feet).isEmpty()
                    || !headState.getCollisionShape(entity.level(), head).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static double[] findClearPath(LivingEntity entity, double sourceX, double sourceZ, int fleeDist) {
        double baseAngle = Math.atan2(entity.getZ() - sourceZ, entity.getX() - sourceX);
        double bestAngle = baseAngle;
        int bestClearance = -1;

        for (int i = 0; i < 8; i++) {
            double angle = baseAngle + (i - 3.5) * (Math.PI / 4.0);
            double dirX = Math.cos(angle);
            double dirZ = Math.sin(angle);
            if (isPathClear(entity, entity.getX(), entity.getZ(), dirX, dirZ, fleeDist)) {
                return new double[]{entity.getX() + dirX * fleeDist, entity.getZ() + dirZ * fleeDist};
            }
            int clearance = 0;
            for (int d = 1; d <= fleeDist; d++) {
                int bx = (int) Math.floor(entity.getX() + dirX * d);
                int bz = (int) Math.floor(entity.getZ() + dirZ * d);
                BlockPos feet = new BlockPos(bx, entity.getBlockY(), bz);
                net.minecraft.world.level.block.state.BlockState fs = entity.level().getBlockState(feet);
                net.minecraft.world.level.block.state.BlockState hs = entity.level().getBlockState(feet.above());
                if (fs.getCollisionShape(entity.level(), feet).isEmpty()
                        && hs.getCollisionShape(entity.level(), feet.above()).isEmpty()) {
                    clearance++;
                } else {
                    break;
                }
            }
            if (clearance > bestClearance) {
                bestClearance = clearance;
                bestAngle = angle;
            }
        }

        double dirX = Math.cos(bestAngle);
        double dirZ = Math.sin(bestAngle);
        int actualDist = Math.max(1, bestClearance);
        return new double[]{entity.getX() + dirX * actualDist, entity.getZ() + dirZ * actualDist};
    }

    private static boolean isRootImmobilized(LivingEntity entity) {
        Entity vehicle = entity.getVehicle();
        if (vehicle == null) return false;
        net.minecraft.resources.ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(vehicle.getType());
        return key != null && "irons_spellbooks".equals(key.getNamespace()) && "root".equals(key.getPath());
    }

    public static void processFlee(LivingEntity target, LivingEntity source, double range) {
        if (!ElementalConfig.mobFleeEnabled) return;
        if (isRootImmobilized(target)) return;
        if (isRootImmobilized(source)) return;
        if (target.level().isClientSide) return;
        CompoundTag data = target.getPersistentData();
        double dx = target.getX() - source.getX();
        double dz = target.getZ() - source.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist > FLEE_TRIGGER_DIST) return;
        if (data.getBoolean("EC_FleeActive")) return;

        int fleeDist = 10 + target.level().random.nextInt(11);
        double[] path = findClearPath(target, source.getX(), source.getZ(), fleeDist);

        data.putDouble(NBT_FLEE_TARGET_X, path[0]);
        data.putDouble(NBT_FLEE_TARGET_Z, path[1]);
        data.putString(NBT_FLEE_SOURCE_UUID, source.getStringUUID());
        data.putDouble(NBT_FLEE_SOURCE_RANGE, range);
        data.putInt(NBT_FLEE_TICKS, 0);
        data.putBoolean("EC_FleeActive", true);
        if (target instanceof Mob mob) {
            mob.setTarget(null);
            mob.setLastHurtByMob(null);
            mob.setNoAi(true);
        }
        double moveDx = path[0] - target.getX();
        double moveDz = path[1] - target.getZ();
        double moveDist = Math.sqrt(moveDx * moveDx + moveDz * moveDz);
        if (moveDist > 0.1) {
            float yaw = (float) (Math.atan2(moveDz, moveDx) * (180.0 / Math.PI)) - 90.0f;
            target.setYRot(yaw);
            target.setYHeadRot(yaw);
            float speed = 0.30f;
            float rad = (float) Math.toRadians(yaw);
            float moveX = -net.minecraft.util.Mth.sin(rad) * speed;
            float moveZ = net.minecraft.util.Mth.cos(rad) * speed;
            target.setDeltaMovement(moveX, target.getDeltaMovement().y, moveZ);
            target.hurtMarked = true;
            target.move(net.minecraft.world.entity.MoverType.SELF, target.getDeltaMovement());
        }
    }

    public static void processFlee(LivingEntity target, double fleeFromX, double fleeFromZ, double range) {
        if (!ElementalConfig.mobFleeEnabled) return;
        if (isRootImmobilized(target)) return;
        if (!(target instanceof PathfinderMob pfMob)) return;
        if (!pfMob.getNavigation().isDone()) return;
        double margin = 4.0;
        double dx = pfMob.getX() - fleeFromX;
        double dz = pfMob.getZ() - fleeFromZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.01) {
            dx = pfMob.level().random.nextFloat() - 0.5;
            dz = pfMob.level().random.nextFloat() - 0.5;
            dist = Math.sqrt(dx * dx + dz * dz);
        }
        double targetX = fleeFromX + (dx / dist) * (range + margin);
        double targetZ = fleeFromZ + (dz / dist) * (range + margin);
        pfMob.getNavigation().moveTo(targetX, pfMob.getY(), targetZ, 1.5);
    }

    private static boolean hasAuraEntityNearby(LivingEntity entity, double range) {
        java.util.List<LivingEntity> nearby = entity.level().getEntitiesOfClass(
                LivingEntity.class,
                new net.minecraft.world.phys.AABB(
                        entity.getX() - range, entity.getY() - range, entity.getZ() - range,
                        entity.getX() + range, entity.getY() + range, entity.getZ() + range));
        for (LivingEntity le : nearby) {
            if (le == entity) continue;
            if (ScorchedHandler.isScorched(le) || FrostbiteHandler.hasFrostbite(le)
                    || le.getPersistentData().getInt(StaticShockHandler.NBT_STATIC_STACKS) > 0) {
                return true;
            }
        }
        return false;
    }

    public static void tickFlee(LivingEntity entity) {
        if (entity.level().isClientSide) return;
        CompoundTag data = entity.getPersistentData();
        if (!data.getBoolean("EC_FleeActive")) return;
        if (isRootImmobilized(entity)) { stopFlee(entity); return; }
        int ticks = data.getInt(NBT_FLEE_TICKS) + 1;
        data.putInt(NBT_FLEE_TICKS, ticks);
        if (ticks >= MAX_FLEE_TICKS) {
            stopFlee(entity);
            return;
        }
        String sourceUUID = data.getString(NBT_FLEE_SOURCE_UUID);
        double auraRange = data.getDouble(NBT_FLEE_SOURCE_RANGE);
        LivingEntity source = findSourceEntity(entity, sourceUUID);

        double targetX = data.getDouble(NBT_FLEE_TARGET_X);
        double targetZ = data.getDouble(NBT_FLEE_TARGET_Z);
        double dx = targetX - entity.getX();
        double dz = targetZ - entity.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist < 1.0) {
            if (!hasAuraEntityNearby(entity, FLEE_TRIGGER_DIST)) {
                stopFlee(entity);
                return;
            }
            int fleeDist = 10 + entity.level().random.nextInt(11);
            double[] path;
            if (source != null && source.isAlive()) {
                path = findClearPath(entity, source.getX(), source.getZ(), fleeDist);
            } else {
                double angle = entity.level().random.nextDouble() * 2 * Math.PI;
                double fakeSrcX = entity.getX() - Math.cos(angle) * 10;
                double fakeSrcZ = entity.getZ() - Math.sin(angle) * 10;
                path = findClearPath(entity, fakeSrcX, fakeSrcZ, fleeDist);
            }
            data.putDouble(NBT_FLEE_TARGET_X, path[0]);
            data.putDouble(NBT_FLEE_TARGET_Z, path[1]);
            data.putInt(NBT_FLEE_TICKS, 0);
            targetX = path[0];
            targetZ = path[1];
            dx = targetX - entity.getX();
            dz = targetZ - entity.getZ();
            dist = Math.sqrt(dx * dx + dz * dz);
        }

        if (dist < 0.5) return;

        float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0f;
        entity.setYRot(yaw);
        entity.setYHeadRot(yaw);
        float speed = 0.30f;
        float rad = (float) Math.toRadians(yaw);
        float moveX = -net.minecraft.util.Mth.sin(rad) * speed;
        float moveZ = net.minecraft.util.Mth.cos(rad) * speed;
        entity.setDeltaMovement(moveX, entity.getDeltaMovement().y, moveZ);
        entity.hurtMarked = true;
        entity.move(net.minecraft.world.entity.MoverType.SELF, entity.getDeltaMovement());
        double lastX = data.getDouble(NBT_FLEE_LAST_X);
        double lastZ = data.getDouble(NBT_FLEE_LAST_Z);
        double movedXZ = Math.abs(entity.getX() - lastX) + Math.abs(entity.getZ() - lastZ);
        if (movedXZ < 0.05) {
            int stuckTicks = data.getInt(NBT_FLEE_STUCK_TICKS) + 1;
            data.putInt(NBT_FLEE_STUCK_TICKS, stuckTicks);
            if (stuckTicks >= 5) {
                int ax = entity.getBlockX() + (moveX > 0 ? 1 : moveX < 0 ? -1 : 0);
                int az = entity.getBlockZ() + (moveZ > 0 ? 1 : moveZ < 0 ? -1 : 0);
                BlockPos ahead = new BlockPos(ax, entity.getBlockY(), az);
                if (!entity.level().getBlockState(ahead).isAir()) {
                    BlockPos above = new BlockPos(ax, entity.getBlockY() + 1, az);
                    BlockPos above2 = above.above();
                    if (entity.level().getBlockState(above).isAir() && entity.level().getBlockState(above2).isAir()) {
                        entity.setPos(ax + 0.5, entity.getY() + 1.0, az + 0.5);
                        data.putInt(NBT_FLEE_STUCK_TICKS, 0);
                    }
                }
            }
        } else {
            data.putInt(NBT_FLEE_STUCK_TICKS, 0);
        }
        data.putDouble(NBT_FLEE_LAST_X, entity.getX());
        data.putDouble(NBT_FLEE_LAST_Z, entity.getZ());
        if (entity instanceof Mob mob) {
            mob.setTarget(null);
            mob.setLastHurtByMob(null);
        }
    }

    public static void stopFlee(LivingEntity entity) {
        CompoundTag data = entity.getPersistentData();
        data.remove(NBT_FLEE_TARGET_X);
        data.remove(NBT_FLEE_TARGET_Z);
        data.remove(NBT_FLEE_SOURCE_UUID);
        data.remove(NBT_FLEE_SOURCE_RANGE);
        data.remove(NBT_FLEE_TICKS);
        data.remove(NBT_FLEE_STUCK_TICKS);
        data.remove(NBT_FLEE_LAST_X);
        data.remove(NBT_FLEE_LAST_Z);
        data.putBoolean("EC_FleeActive", false);
        if (entity instanceof Mob mob) {
            mob.setNoAi(false);
        }
    }

    private static LivingEntity findSourceEntity(LivingEntity self, String uuid) {
        if (uuid.isEmpty()) return null;
        if (self.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            java.util.UUID id;
            try { id = java.util.UUID.fromString(uuid); } catch (IllegalArgumentException e) { return null; }
            net.minecraft.world.entity.Entity e = serverLevel.getEntity(id);
            return e instanceof LivingEntity le ? le : null;
        }
        return null;
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getNewTarget() == null) return;
        if (event.getEntity().getPersistentData().getInt(NBT_DISORIENTED) > 0) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide) return;
        CompoundTag data = event.getEntity().getPersistentData();
        int disoriented = data.getInt(NBT_DISORIENTED);
        if (disoriented > 0) {
            data.putInt(NBT_DISORIENTED, disoriented - 1);
            if (event.getEntity() instanceof Mob mob) {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }
        }
        if (!(event.getEntity() instanceof net.minecraft.world.entity.player.Player)) {
            tickFlee(event.getEntity());
        }
    }
}
