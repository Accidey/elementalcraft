package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.util.EffectHelper;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import com.xulai.elementalcraft.util.GlobalDebugLogger;
import com.xulai.elementalcraft.util.DebugMode;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
@SuppressWarnings("null")
public class SteamReactionHandler {

    public static final String TAG_STEAM_CLOUD = "EC_SteamCloud";
    public static final String TAG_HIGH_HEAT = "EC_HighHeat";
    public static final String TAG_LEVEL_PREFIX = "EC_Level_";
    public static final String TAG_SELF_DRYING_PENALTY = "EC_SelfDryingPenalty";

    private static final String NBT_CONDENSATION_TIMER = "EC_SteamCondensationTimer";
    private static final String NBT_SPORE_GROWTH_TIMER = "EC_SporeGrowthTimer";
    private static final String NBT_STEAM_TRIGGER_COOLDOWN = "EC_SteamTriggerCooldown";
    private static final String NBT_STEAM_BLINDNESS = "EC_SteamBlindness";

    private static final double STEAM_SCAN_RADIUS_MULTIPLIER = 3.0;

    private static final List<AreaEffectCloud> ACTIVE_STEAM_CLOUDS = new CopyOnWriteArrayList<>();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!ElementalFireNatureReactionsConfig.steamReactionEnabled) return;
        if (event.isCanceled()) return;

        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            processTriggerLogic(event, attacker, event.getEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamageSteamDefense(LivingDamageEvent event) {
        if (event.getSource().is(ModDamageTypes.STEAM_SCALDING)) {
            processDefenseLogic(event);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().is(ModDamageTypes.STEAM_SCALDING)) {
            event.getEntity().setSecondsOnFire(1);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!ElementalFireNatureReactionsConfig.steamReactionEnabled) return;

        LivingEntity entity = event.getEntity();

        CompoundTag data = entity.getPersistentData();
        if (data.contains(NBT_STEAM_TRIGGER_COOLDOWN)) {
            int cooldown = data.getInt(NBT_STEAM_TRIGGER_COOLDOWN);
            if (cooldown > 0) {
                data.putInt(NBT_STEAM_TRIGGER_COOLDOWN, cooldown - 1);
            } else {
                data.remove(NBT_STEAM_TRIGGER_COOLDOWN);
            }
        }

        if (entity.tickCount % ElementalFireNatureReactionsConfig.steamCheckInterval != 0) return;
        processCloudEffects(entity);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide) return;
        if (event.level.getGameTime() % 20 != 0) return;

        if (ACTIVE_STEAM_CLOUDS.isEmpty()) return;

        ACTIVE_STEAM_CLOUDS.removeIf(AreaEffectCloud::isRemoved);

        for (AreaEffectCloud cloud : ACTIVE_STEAM_CLOUDS) {
            if (cloud.level() == event.level) {
                boolean isHighHeat = cloud.getTags().contains(TAG_HIGH_HEAT);
                EffectHelper.playSteamCloudTick((ServerLevel) event.level, cloud, isHighHeat);
            }
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) return;
        ACTIVE_STEAM_CLOUDS.removeIf(cloud -> cloud.level() == event.getLevel());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ACTIVE_STEAM_CLOUDS.clear();
    }

    public static boolean isInCondensingCloud(LivingEntity entity) {
        if (entity.level().isClientSide) return false;
        double searchRadius = ElementalFireNatureReactionsConfig.steamCloudRadius * STEAM_SCAN_RADIUS_MULTIPLIER;
        AABB box = entity.getBoundingBox().inflate(searchRadius);
        List<AreaEffectCloud> clouds = entity.level().getEntitiesOfClass(AreaEffectCloud.class, box,
                c -> c.getTags().contains(TAG_STEAM_CLOUD) && !c.getTags().contains(TAG_HIGH_HEAT));
        for (AreaEffectCloud cloud : clouds) {
            if (isEntityInCloud(entity, cloud)) {
                return true;
            }
        }
        return false;
    }

    private static void processTriggerLogic(LivingDamageEvent event, LivingEntity attacker, LivingEntity target) {
        ElementType attackElement = ElementUtils.getConsistentAttackElement(attacker);
        Debug.logTriggerStart(attacker, target, attackElement);

        if (event.getSource().is(DamageTypeTags.IS_FIRE)) attackElement = ElementType.FIRE;
        if (event.getSource().is(DamageTypeTags.IS_FREEZING)) attackElement = ElementType.FROST;

        int firePower = ElementUtils.getDisplayEnhancement(attacker, ElementType.FIRE);
        int frostPower = ElementUtils.getDisplayEnhancement(attacker, ElementType.FROST);

        boolean targetIsWet = target.getPersistentData().getInt(WetnessHandler.NBT_WETNESS) > 0;
        int targetWetness = target.getPersistentData().getInt(WetnessHandler.NBT_WETNESS);
        ElementType targetElement = ElementUtils.getElementType(target);

        Debug.logTriggerValues(attacker, target, attackElement, firePower, frostPower, targetIsWet, targetWetness, targetElement);

        if (attackElement == ElementType.FIRE) {
            int attackerWetness = attacker.getPersistentData().getInt(WetnessHandler.NBT_WETNESS);
            if (attackerWetness > 0) {
                Debug.logSelfDryPrevent(attacker, target, attackerWetness);
                return;
            }

            if (targetIsWet || targetElement == ElementType.FROST) {
                int threshold = ElementalFireNatureReactionsConfig.steamTriggerThresholdFire;

                if (firePower >= threshold) {
                    if (isTriggerBlocked(target)) {
                        Debug.logTriggerBlocked(target);
                        return;
                    }

                    int fuelLevel = 1;

                    if (targetIsWet) {
                        fuelLevel = targetWetness;
                    } else if (targetElement == ElementType.FROST) {
                        int targetFrostPower = ElementUtils.getDisplayEnhancement(target, ElementType.FROST);
                        int step = Math.max(1, ElementalFireNatureReactionsConfig.steamCondensationStepFrost);
                        fuelLevel = 1 + (targetFrostPower / step);
                    }

                    int maxLevel = ElementalFireNatureReactionsConfig.steamHighHeatMaxLevel;
                    fuelLevel = Math.min(fuelLevel, maxLevel);

                    Debug.logFuelLevel(attacker, fuelLevel, true);
                    spawnSteamCloud(target, true, fuelLevel);
                    setTriggerCooldown(target);

                    DebugCommand.sendSteamTriggerLog(attacker, true, fuelLevel);

                    if (targetIsWet) {
                        removeWetness(target);
                    }
                } else {
                    Debug.logThresholdNotMet(attacker, firePower, threshold, true);
                    if (targetIsWet) {
                        removeWetness(target);
                    }
                }
            }
        }
        else if (attackElement == ElementType.FROST) {
            if (targetElement == ElementType.FIRE) {
                if (target.level().dimension() == Level.NETHER) {
                    Debug.logNetherPrevent(target);
                    return;
                }

                int threshold = ElementalFireNatureReactionsConfig.steamTriggerThresholdFrost;

                if (frostPower >= threshold) {
                    if (isTriggerBlocked(target)) {
                        Debug.logTriggerBlocked(target);
                        return;
                    }

                    int targetFirePower = ElementUtils.getDisplayEnhancement(target, ElementType.FIRE);
                    int step = Math.max(1, ElementalFireNatureReactionsConfig.steamCondensationStepFire);

                    int level = 1 + (targetFirePower / step);
                    int maxLevel = ElementalFireNatureReactionsConfig.steamLowHeatMaxLevel;
                    level = Math.min(level, maxLevel);

                    Debug.logFuelLevel(attacker, level, false);
                    spawnSteamCloud(target, false, level);
                    setTriggerCooldown(target);

                    DebugCommand.sendSteamTriggerLog(attacker, false, level);
                } else {
                    Debug.logThresholdNotMet(attacker, frostPower, threshold, false);
                }
            }
        }
    }

    private static void processDefenseLogic(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        float currentDamage = event.getAmount();

        Debug.logDefenseStart(target, currentDamage);

        if (checkImmunity(target)) {
            Debug.logImmunity(target);
            event.setAmount(0);
            event.setCanceled(true);
            return;
        }

        float trueRawDamage = currentDamage;

        int totalFireProtLevel = getTotalEnchantmentLevel(Enchantments.FIRE_PROTECTION, target);
        int totalProtLevel = getTotalEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, target);

        double maxFireCap = ElementalFireNatureReactionsConfig.steamMaxFireProtCap;
        double maxGeneralCap = ElementalFireNatureReactionsConfig.steamMaxGeneralProtCap;
        double denom = ElementalFireNatureReactionsConfig.enchantmentCalculationDenominator;

        double fireProtFactor = maxFireCap / denom;
        double protFactor = maxGeneralCap / denom;

        double calculatedFireRed = totalFireProtLevel * fireProtFactor;
        double calculatedProtRed = totalProtLevel * protFactor;

        double actualFireRed = Math.min(calculatedFireRed, maxFireCap);
        double actualProtRed = Math.min(calculatedProtRed, maxGeneralCap);

        double globalCap = ElementalFireNatureReactionsConfig.steamMaxReduction;
        double totalReduction = Math.min(actualFireRed + actualProtRed, globalCap);

        float reducedDamage = trueRawDamage * (float) (1.0 - totalReduction);

        ElementType type = ElementUtils.getElementType(target);
        if (type == ElementType.FROST || type == ElementType.NATURE) {
            float floorRatio = (float) ElementalFireNatureReactionsConfig.steamDamageFloorRatio;
            float floorLimit = trueRawDamage * floorRatio;

            if (reducedDamage < floorLimit) {
                reducedDamage = floorLimit;
                Debug.logFloorApplied(target, trueRawDamage, floorLimit, reducedDamage);
            }
        }

        Debug.logDefenseResult(target, trueRawDamage, totalFireProtLevel, totalProtLevel,
                actualFireRed, actualProtRed, totalReduction, reducedDamage);

        event.setAmount(reducedDamage);
    }

    private static int getTotalEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantment ench, LivingEntity entity) {
        int total = 0;
        for (ItemStack stack : entity.getArmorSlots()) {
            total += stack.getEnchantmentLevel(ench);
        }
        return total;
    }

    private static void processCloudEffects(LivingEntity entity) {
        if (entity.level().isClientSide) return;

        double searchRadius = ElementalFireNatureReactionsConfig.steamCloudRadius * STEAM_SCAN_RADIUS_MULTIPLIER;
        AABB box = entity.getBoundingBox().inflate(searchRadius);
        List<AreaEffectCloud> clouds = entity.level().getEntitiesOfClass(AreaEffectCloud.class, box,
                c -> c.getTags().contains(TAG_STEAM_CLOUD));

        if (clouds.isEmpty()) {
            if (entity.getPersistentData().contains(NBT_STEAM_BLINDNESS)) {
                entity.removeEffect(MobEffects.BLINDNESS);
                entity.getPersistentData().remove(NBT_STEAM_BLINDNESS);
            }
            return;
        }

        boolean isHighHeat = false;
        boolean isCondensing = false;
        int cloudLevel = 1;
        AreaEffectCloud heatSource = null;

        for (AreaEffectCloud cloud : clouds) {
            if (!isEntityInCloud(entity, cloud)) continue;

            if (ElementalFireNatureReactionsConfig.steamClearAggro && entity instanceof Mob mob) {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }

            if (cloud.getTags().contains(TAG_HIGH_HEAT)) {
                isHighHeat = true;
                heatSource = cloud;
                for (String tag : cloud.getTags()) {
                    if (tag.startsWith(TAG_LEVEL_PREFIX)) {
                        try {
                            cloudLevel = Integer.parseInt(tag.replace(TAG_LEVEL_PREFIX, ""));
                        } catch (NumberFormatException ignored) {
                        }
                        break;
                    }
                }
            } else isCondensing = true;
        }

        Debug.logCloudEffect(entity, isHighHeat, isCondensing, cloudLevel);

        if (isHighHeat || isCondensing) {
            if (!entity.getPersistentData().contains(NBT_STEAM_BLINDNESS)) {
                entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 1000000, 0, false, false, true));
                entity.getPersistentData().putBoolean(NBT_STEAM_BLINDNESS, true);
            }
        } else {
            if (entity.getPersistentData().contains(NBT_STEAM_BLINDNESS)) {
                entity.removeEffect(MobEffects.BLINDNESS);
                entity.getPersistentData().remove(NBT_STEAM_BLINDNESS);
            }
        }

        if (isHighHeat) {
            if (entity.getPersistentData().contains(NBT_CONDENSATION_TIMER)) {
                entity.getPersistentData().remove(NBT_CONDENSATION_TIMER);
            }

            boolean aboveCeiling = false;
            if (heatSource != null) {
                double heightCeiling = ElementalFireNatureReactionsConfig.steamCloudHeightCeiling;
                if (entity.getY() - heatSource.getY() > heightCeiling) {
                    aboveCeiling = true;
                    Debug.logHeightCeilingSkip(entity, heatSource.getY(), heightCeiling);
                } else {
                    Debug.logHeightCeilingPass(entity, heatSource.getY(), heightCeiling);
                }
            }

            if (entity.tickCount % 20 == 0 && !aboveCeiling) {
                float baseDamage = (float) ElementalFireNatureReactionsConfig.steamScaldingDamage;
                float scale = (float) ElementalFireNatureReactionsConfig.steamDamageScalePerLevel;
                float levelMultiplier = 1.0f + (cloudLevel * scale);
                float damage = baseDamage * levelMultiplier;

                ElementType type = ElementUtils.getElementType(entity);
                if (type == ElementType.FROST || type == ElementType.NATURE) {
                    double weaknessMult = ElementalFireNatureReactionsConfig.steamScaldingMultiplierWeakness;
                    damage *= (float) weaknessMult;
                }

                if (entity.hasEffect(ModMobEffects.SPORES.get())) {
                    damage *= (float) ElementalFireNatureReactionsConfig.steamScaldingMultiplierSpore;
                }

                Debug.logScaldingDamage(entity, baseDamage, cloudLevel, levelMultiplier, damage, type);

                if (damage > 0) {
                    entity.invulnerableTime = 0;
                    boolean hurtSuccess = entity.hurt(ModDamageTypes.source(entity.level(), ModDamageTypes.STEAM_SCALDING), damage);

                    if (hurtSuccess) {
                        if (entity instanceof PathfinderMob mob && heatSource != null) {
                            mob.setTarget(null);

                            int fleeDist = (int) (heatSource.getRadius() + 2);

                            Vec3 escapePos = DefaultRandomPos.getPosAway(mob, fleeDist, 4, heatSource.position());
                            if (escapePos != null) {
                                mob.getNavigation().moveTo(escapePos.x, escapePos.y, escapePos.z, 1.5);
                            }
                        }
                    }
                }
            }

            if (entity.getPersistentData().getInt(WetnessHandler.NBT_WETNESS) > 0) {
                removeWetness(entity);
            }
        }
        else if (isCondensing) {
            int currentTimer = entity.getPersistentData().getInt(NBT_CONDENSATION_TIMER);
            currentTimer += ElementalFireNatureReactionsConfig.steamCheckInterval;

            int delayThreshold = Math.max(10, ElementalFireNatureReactionsConfig.steamCondensationDelay);

            if (currentTimer >= delayThreshold) {
                int currentWet = entity.getPersistentData().getInt(WetnessHandler.NBT_WETNESS);
                int max = ElementalFireNatureReactionsConfig.wetnessMaxLevel;

                if (currentWet < max) {
                    entity.getPersistentData().putInt(WetnessHandler.NBT_WETNESS, currentWet + 1);
                    entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.NEUTRAL, 1.0f, 1.0f);
                    Debug.logCondensationGain(entity, currentWet + 1, currentTimer);
                }

                currentTimer = 0;
            }

            entity.getPersistentData().putInt(NBT_CONDENSATION_TIMER, currentTimer);
            Debug.logCondensationTimer(entity, currentTimer, delayThreshold);

            if (entity.hasEffect(ModMobEffects.SPORES.get())) {
                int sporeTimer = entity.getPersistentData().getInt(NBT_SPORE_GROWTH_TIMER);
                sporeTimer += ElementalFireNatureReactionsConfig.steamCheckInterval;

                int growthRate = Math.max(10, ElementalFireNatureReactionsConfig.steamSporeGrowthRate);

                if (sporeTimer >= growthRate) {
                    MobEffectInstance effect = entity.getEffect(ModMobEffects.SPORES.get());
                    int amp = effect.getAmplifier();
                    int maxStacks = ElementalFireNatureReactionsConfig.sporeMaxStacks;

                    if (amp + 1 < maxStacks) {
                        entity.addEffect(new MobEffectInstance(ModMobEffects.SPORES.get(), 200, amp + 1));
                        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.COMPOSTER_READY, SoundSource.NEUTRAL, 1.0f, 1.0f);
                        Debug.logSporeGrowth(entity, amp + 1);
                    }
                    sporeTimer = 0;
                }
                entity.getPersistentData().putInt(NBT_SPORE_GROWTH_TIMER, sporeTimer);
                Debug.logSporeTimer(entity, sporeTimer, growthRate);
            }
        }
    }

    // 修复：蒸汽云使用有效高度判断碰撞，不再受限于 AreaEffectCloud 的 0.5 格高度
    private static boolean isEntityInCloud(LivingEntity entity, AreaEffectCloud cloud) {
        if (cloud.getBoundingBox().inflate(0.1).intersects(entity.getBoundingBox())) {
            return true;
        }

        // 标准碰撞检测失败，检查是否为蒸汽云且在有效高度范围内
        if (cloud.getTags().contains(TAG_STEAM_CLOUD)) {
            double heightCeiling = ElementalFireNatureReactionsConfig.steamCloudHeightCeiling;
            double dx = entity.getX() - cloud.getX();
            double dz = entity.getZ() - cloud.getZ();
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            double dy = entity.getY() - cloud.getY();

            if (horizontalDist <= cloud.getRadius() && dy >= 0 && dy <= heightCeiling) {
                return true;
            }
        }

        return false;
    }

    private static boolean isTriggerBlocked(LivingEntity entity) {
        if (entity.getPersistentData().getInt(NBT_STEAM_TRIGGER_COOLDOWN) > 0) return true;

        if (entity.level().isClientSide) return false;

        double searchRadius = ElementalFireNatureReactionsConfig.steamCloudRadius * 2.0;
        AABB box = entity.getBoundingBox().inflate(searchRadius);
        List<AreaEffectCloud> clouds = entity.level().getEntitiesOfClass(AreaEffectCloud.class, box,
                c -> c.getTags().contains(TAG_STEAM_CLOUD));

        for (AreaEffectCloud cloud : clouds) {
            if (isEntityInCloud(entity, cloud)) {
                return true;
            }
        }
        return false;
    }

    private static void setTriggerCooldown(LivingEntity entity) {
        entity.getPersistentData().putInt(NBT_STEAM_TRIGGER_COOLDOWN, ElementalFireNatureReactionsConfig.steamTriggerCooldown);
    }

    private static void removeWetness(LivingEntity entity) {
        WetnessHandler.clearWetnessData(entity);
    }

    private static boolean checkImmunity(LivingEntity entity) {
        if (entity.fireImmune() || entity.hasEffect(MobEffects.FIRE_RESISTANCE)) return true;

        var key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (key != null && ElementalFireNatureReactionsConfig.cachedSteamBlacklist.contains(key.toString())) return true;

        int resist = ElementUtils.getDisplayResistance(entity, ElementType.FIRE);
        int threshold = ElementalFireNatureReactionsConfig.steamImmunityThreshold;
        return resist >= threshold;
    }

    private static void spawnSteamCloud(LivingEntity target, boolean isHighHeat, int fuelLevel) {
        if (!(target.level() instanceof ServerLevel serverLevel)) return;

        int maxLevel = isHighHeat ? ElementalFireNatureReactionsConfig.steamHighHeatMaxLevel : ElementalFireNatureReactionsConfig.steamLowHeatMaxLevel;
        int level = Math.max(1, Math.min(fuelLevel, maxLevel));

        float baseRadius = (float) ElementalFireNatureReactionsConfig.steamCloudRadius;
        float radiusInc = (float) ElementalFireNatureReactionsConfig.steamRadiusPerLevel;
        float radius = isHighHeat ? baseRadius + (level - 1.0f) * radiusInc : baseRadius;

        int baseDuration;
        int durationInc;

        if (isHighHeat) {
            baseDuration = ElementalFireNatureReactionsConfig.steamCloudDuration;
            durationInc = ElementalFireNatureReactionsConfig.steamDurationPerLevel;
        } else {
            baseDuration = ElementalFireNatureReactionsConfig.steamCondensationDurationBase;
            durationInc = ElementalFireNatureReactionsConfig.steamCondensationDurationPerLevel;
        }

        int duration = baseDuration + (level * durationInc);

        AreaEffectCloud cloud = new AreaEffectCloud(serverLevel, target.getX(), target.getY(), target.getZ());
        cloud.setRadius(radius);
        cloud.setRadiusOnUse(0F);
        cloud.setRadiusPerTick(0F);
        cloud.setDuration(duration);

        cloud.setParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AIR.defaultBlockState()));

        cloud.addTag(TAG_STEAM_CLOUD);
        cloud.addTag(TAG_LEVEL_PREFIX + level);

        if (isHighHeat) {
            cloud.addTag(TAG_HIGH_HEAT);
        }

        serverLevel.addFreshEntity(cloud);

        ACTIVE_STEAM_CLOUDS.add(cloud);

        EffectHelper.playSteamBurst(serverLevel, target, radius, level, isHighHeat);

        Debug.logCloudSpawn(target, isHighHeat, level, radius, duration);
    }

    private static final class Debug {
        private static void logTriggerStart(LivingEntity attacker, LivingEntity target, ElementType attackElement) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(attacker.level(), "蒸汽触发",
                    String.format("%s 攻击 %s：初始攻击元素 %s", attacker.getName().getString(), target.getName().getString(), attackElement));
        }

        private static void logTriggerValues(LivingEntity attacker, LivingEntity target, ElementType attackElement,
                                             int firePower, int frostPower, boolean targetIsWet, int targetWetness, ElementType targetElement) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(attacker.level(), "蒸汽触发",
                    String.format("%s 攻击 %s：攻击元素 %s，火点数 %d，冰点数 %d，目标潮湿 %s(%d)，目标元素 %s",
                            attacker.getName().getString(), target.getName().getString(),
                            attackElement, firePower, frostPower, targetIsWet, targetWetness, targetElement));
        }

        private static void logTriggerBlocked(LivingEntity target) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "蒸汽触发",
                    String.format("%s 触发被阻止（冷却或已在云中）", target.getName().getString()));
        }

        private static void logFuelLevel(LivingEntity attacker, int fuelLevel, boolean isHighHeat) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(attacker.level(), "蒸汽触发",
                    String.format("%s 燃料等级 %d，高温 %s", attacker.getName().getString(), fuelLevel, isHighHeat));
        }

        private static void logThresholdNotMet(LivingEntity attacker, int power, int threshold, boolean isFire) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(attacker.level(), "蒸汽触发",
                    String.format("%s 点数 %d 未达到阈值 %d，%s", attacker.getName().getString(), power, threshold,
                            isFire ? "清除潮湿" : "无反应"));
        }

        private static void logNetherPrevent(LivingEntity target) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "蒸汽触发",
                    String.format("%s 在下界，防止冰霜触发蒸汽", target.getName().getString()));
        }

        private static void logSelfDryPrevent(LivingEntity attacker, LivingEntity target, int attackerWetness) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(attacker.level(), "蒸汽触发",
                    String.format("%s 自身潮湿层数 %d，阻止高温蒸汽云触发，交由自我干燥处理",
                            attacker.getName().getString(), attackerWetness));
        }

        private static void logDefenseStart(LivingEntity target, float damage) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "蒸汽防御",
                    String.format("%s 受到蒸汽伤害 %.2f", target.getName().getString(), damage));
        }

        private static void logImmunity(LivingEntity target) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "蒸汽防御",
                    String.format("%s 免疫蒸汽，伤害取消", target.getName().getString()));
        }

        private static void logFloorApplied(LivingEntity target, float raw, float floor, float finalDamage) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "蒸汽防御",
                    String.format("%s 保底生效：原始 %.2f，保底 %.2f，最终 %.2f", target.getName().getString(), raw, floor, finalDamage));
        }

        private static void logDefenseResult(LivingEntity target, float raw, int fireProt, int genProt,
                                             double fireRed, double genRed, double totalRed, float finalDamage) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "蒸汽防御",
                    String.format("%s 伤害计算：原始 %.2f，火保 %d(%.1f%%)，通保 %d(%.1f%%)，总减免 %.1f%%，最终 %.2f",
                            target.getName().getString(), raw,
                            fireProt, fireRed * 100,
                            genProt, genRed * 100,
                            totalRed * 100, finalDamage));
        }

        private static void logCloudEffect(LivingEntity entity, boolean isHighHeat, boolean isCondensing, int level) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(entity.level(), "蒸汽云",
                    String.format("%s 在云中：高温 %s，冷凝 %s，等级 %d",
                            entity.getName().getString(), isHighHeat, isCondensing, level));
        }

        private static void logScaldingDamage(LivingEntity entity, float base, int level, float mult, float damage, ElementType type) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(entity.level(), "蒸汽云",
                    String.format("%s 烫伤：基础 %.2f，等级 %d，倍率 %.2f，元素 %s，最终 %.2f",
                            entity.getName().getString(), base, level, mult, type, damage));
        }

        private static void logCondensationGain(LivingEntity entity, int newWet, int timer) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(entity.level(), "蒸汽冷凝",
                    String.format("%s 获得潮湿 %d（计时器 %d）", entity.getName().getString(), newWet, timer));
        }

        private static void logCondensationTimer(LivingEntity entity, int timer, int threshold) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(entity.level(), "蒸汽冷凝",
                    String.format("%s 冷凝计时器 %d/%d", entity.getName().getString(), timer, threshold));
        }

        private static void logSporeGrowth(LivingEntity entity, int newAmp) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(entity.level(), "孢子繁殖",
                    String.format("%s 孢子增长至 %d 层", entity.getName().getString(), newAmp + 1));
        }

        private static void logSporeTimer(LivingEntity entity, int timer, int rate) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(entity.level(), "孢子繁殖",
                    String.format("%s 孢子计时器 %d/%d", entity.getName().getString(), timer, rate));
        }

        private static void logCloudSpawn(LivingEntity target, boolean isHighHeat, int level, float radius, int duration) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            GlobalDebugLogger.log(target.level(), "蒸汽云",
                    String.format("%s 生成蒸汽云：高温 %s，等级 %d，半径 %.1f，持续 %d 刻，位置 [%.1f, %.1f, %.1f]",
                            target.getName().getString(), isHighHeat, level, radius, duration,
                            target.getX(), target.getY(), target.getZ()));
        }

        private static void logHeightCeilingSkip(LivingEntity entity, double cloudY, double ceiling) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            double immuneY = cloudY + ceiling;
            GlobalDebugLogger.log(entity.level(), "蒸汽云",
                    String.format("%s 超出蒸汽云高度上限：实体位置 [%.1f, %.1f, %.1f]，云位置 [Y=%.1f]，上限 %.1f格，免疫高度 Y>%.1f",
                            entity.getName().getString(),
                            entity.getX(), entity.getY(), entity.getZ(),
                            cloudY, ceiling, immuneY));
        }

        private static void logHeightCeilingPass(LivingEntity entity, double cloudY, double ceiling) {
            if (!DebugMode.hasAnyDebugEnabled()) return;
            double immuneY = cloudY + ceiling;
            GlobalDebugLogger.log(entity.level(), "蒸汽云",
                    String.format("%s 在蒸汽云有效高度内：实体位置 [%.1f, %.1f, %.1f]，云位置 [Y=%.1f]，上限 %.1f格，免疫高度 Y>%.1f",
                            entity.getName().getString(),
                            entity.getX(), entity.getY(), entity.getZ(),
                            cloudY, ceiling, immuneY));
        }
    }
}
