package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.util.EffectHelper;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import com.xulai.elementalcraft.util.ElementDamageHelper;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.PathfinderMob;

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
    public static final String TAG_STATIC_CHARGED = "EC_StaticCharged";
    public static final String TAG_FROSTED = "EC_Frosted";
    private static final String TAG_CLOUD_UUID_PREFIX = "EC_CloudUUID_";

    private static final String NBT_CONDENSATION_TIMER = "EC_SteamCondensationTimer";
    private static final String NBT_STEAM_ATTACKER_COOLDOWN = "EC_SteamAttackerCooldown";
    private static final String NBT_STEAM_BLINDNESS = "EC_SteamBlindness";
    private static final String NBT_STEAM_SCALDING_LOGGED = "EC_SteamScaldingLogged";
    public static final String NBT_FROSTED_CLOUD_UUID = "EC_FrostedCloudUUID";
    private static final String NBT_STATIC_CLOUD_UUID = "EC_StaticCloudUUID";
    private static final String NBT_STATIC_DMG_CLOUD_UUID = "EC_StaticDmgCloudUUID";


    private static final double STEAM_SCAN_RADIUS_MULTIPLIER = 1.0;
    private static final List<AreaEffectCloud> ACTIVE_STEAM_CLOUDS = new CopyOnWriteArrayList<>();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.isCanceled()) return;
        float originalDamage = event.getAmount();
        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            processTriggerLogic(event, attacker, event.getEntity());
        }
        event.setAmount(originalDamage);
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
        LivingEntity entity = event.getEntity();
        CompoundTag data = entity.getPersistentData();

        if (data.contains(NBT_STEAM_ATTACKER_COOLDOWN)) {
            int cooldown = data.getInt(NBT_STEAM_ATTACKER_COOLDOWN);
            if (cooldown > 1) {
                data.putInt(NBT_STEAM_ATTACKER_COOLDOWN, cooldown - 1);
            } else {
                data.remove(NBT_STEAM_ATTACKER_COOLDOWN);
            }
        }

        if (entity.tickCount % 10 != 0) return;
        processCloudEffects(entity);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide) return;
        if (event.level.getGameTime() % 10 != 0) return;
        if (ACTIVE_STEAM_CLOUDS.isEmpty()) return;
        ACTIVE_STEAM_CLOUDS.removeIf(AreaEffectCloud::isRemoved);
        for (AreaEffectCloud cloud : ACTIVE_STEAM_CLOUDS) {
            if (cloud.level() == event.level) {
            boolean isHighHeat = cloud.getTags().contains(TAG_HIGH_HEAT);
                boolean isStaticCharged = cloud.getTags().contains(TAG_STATIC_CHARGED);
                boolean isFrosted = cloud.getTags().contains(TAG_FROSTED);
                EffectHelper.playSteamCloudTick((ServerLevel) event.level, cloud, isHighHeat);
                if (isFrosted) {
                    EffectHelper.playFrostedCloudTick((ServerLevel) event.level, cloud);
                } else if (isStaticCharged) {
                    EffectHelper.playStaticChargedCloudTick((ServerLevel) event.level, cloud);
                }
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
        List<AreaEffectCloud> clouds = entity.level().getEntitiesOfClass(AreaEffectCloud.class, box, c -> c.getTags().contains(TAG_STEAM_CLOUD) && !c.getTags().contains(TAG_HIGH_HEAT));
        for (AreaEffectCloud cloud : clouds) {
            if (isEntityInCloud(entity, cloud)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isInHighHeatCloud(LivingEntity entity) {
        if (entity.level().isClientSide) return false;
        double searchRadius = ElementalFireNatureReactionsConfig.steamCloudRadius * STEAM_SCAN_RADIUS_MULTIPLIER;
        AABB box = entity.getBoundingBox().inflate(searchRadius);
        List<AreaEffectCloud> clouds = entity.level().getEntitiesOfClass(AreaEffectCloud.class, box, c -> c.getTags().contains(TAG_STEAM_CLOUD) && c.getTags().contains(TAG_HIGH_HEAT));
        for (AreaEffectCloud cloud : clouds) {
            if (isEntityInCloud(entity, cloud)) {
                return true;
            }
        }
        return false;
    }

    private static void processTriggerLogic(LivingDamageEvent event, LivingEntity attacker, LivingEntity target) {
        if (target.getPersistentData().getBoolean("EC_FireFrostMeltResolved")) {
            target.getPersistentData().remove("EC_FireFrostMeltResolved");
            return;
        }
        if (isOnSteamCooldown(attacker)) {
            int remaining = DebugCommand.getRemainingCooldownCountdown(attacker, NBT_STEAM_ATTACKER_COOLDOWN);
            DebugCommand.sendReactionCooldownBlock(attacker, "steam", remaining);
            return;
        }

        ElementType attackElement = ElementUtils.getConsistentAttackElement(attacker);

        if (event.getSource().is(DamageTypeTags.IS_FIRE)) attackElement = ElementType.FIRE;
        if (event.getSource().is(DamageTypeTags.IS_FREEZING)) attackElement = ElementType.FROST;

        int firePower = ElementUtils.getDisplayEnhancement(attacker, ElementType.FIRE);
        int frostPower = ElementUtils.getDisplayEnhancement(attacker, ElementType.FROST);
        int targetWetness = WetnessHandler.getWetnessLevel(target);
        boolean targetIsWet = targetWetness > 0;

        if (attackElement == ElementType.FIRE) {
            int attackerWetness = WetnessHandler.getWetnessLevel(attacker);
            if (attackerWetness > 0) {
                return;
            }
            if (targetIsWet) {
                if (ElementalFireNatureReactionsConfig.steamHighHeatMaxLevel <= 0) return;
                int threshold = ElementalFireNatureReactionsConfig.steamHighHeatTriggerThreshold;
                if (firePower >= threshold) {
                    if (isTriggerBlocked(target)) {
                        return;
                    }
                    int fuelLevel = Math.max(1, Math.min(targetWetness, ElementalFireNatureReactionsConfig.steamHighHeatMaxLevel));
                    spawnSteamCloud(target, true, fuelLevel);
                    applySteamCooldown(attacker, computeCloudDuration(true, fuelLevel));

                    float baseDmg = (float) ElementalFireNatureReactionsConfig.steamScaldingDamage;
                    float lvMult = 1.0f + ((fuelLevel - 1) * (float) ElementalFireNatureReactionsConfig.steamDamageScalePerLevel);
                    float previewDmg = baseDmg * lvMult;
                    float elemMult = 1.0f;
                    ElementType elemType = ElementUtils.getConsistentAttackElement(target);
                    if (elemType == ElementType.FIRE) elemMult = (float) ElementalFireNatureReactionsConfig.steamScaldingMultiplierFire;
                    else if (elemType == ElementType.NATURE) elemMult = (float) ElementalFireNatureReactionsConfig.steamScaldingMultiplierNature;
                    else if (elemType == ElementType.THUNDER) elemMult = (float) ElementalFireNatureReactionsConfig.steamScaldingMultiplierThunder;
                    else if (elemType == ElementType.FROST) elemMult = (float) ElementalFireNatureReactionsConfig.steamScaldingMultiplierFrost;
                    previewDmg *= elemMult;
                    boolean fImmune = target.fireImmune();
                    if (fImmune) previewDmg *= (float) ElementalFireNatureReactionsConfig.scorchedImmuneModifier;
                    float baseR = (float) ElementalFireNatureReactionsConfig.steamCloudRadius;
                    float rInc = (float) ElementalFireNatureReactionsConfig.steamRadiusPerLevel;
                    float rad = baseR + (fuelLevel - 1.0f) * rInc;
                    int dur = ElementalFireNatureReactionsConfig.steamCloudDuration + fuelLevel * ElementalFireNatureReactionsConfig.steamDurationPerLevel;
                    DebugCommand.sendSteamCloudCombinedLog(target, attacker, true, fuelLevel, baseDmg, lvMult, rad, dur,
                            ElementalFireNatureReactionsConfig.steamCloudHeightCeiling, ElementalFireNatureReactionsConfig.steamClearAggro,
                            elemType, elemMult, fImmune, previewDmg);

                    removeWetness(target);
                } else {
                    DebugCommand.sendReactionFailed(attacker, "steam", "power_low",
                            Component.translatable("element.fire"),
                            firePower, threshold);
                }
            }
        } else if (attackElement == ElementType.FROST) {
            if (targetIsWet) {
                int threshold = ElementalFireNatureReactionsConfig.steamLowHeatTriggerThreshold;
                if (frostPower >= threshold && ElementalFireNatureReactionsConfig.steamLowHeatMaxLevel > 0) {
                    if (isTriggerBlocked(target)) {
                        return;
                    }
                    int fuelLevel = Math.max(1, Math.min(targetWetness, ElementalFireNatureReactionsConfig.steamLowHeatMaxLevel));
                    spawnSteamCloud(target, false, fuelLevel);
                    applySteamCooldown(attacker, computeCloudDuration(false, fuelLevel));
                    removeWetness(target);

                    float baseDmgL = (float) ElementalFireNatureReactionsConfig.steamScaldingDamage;
                    float radL = (float) ElementalFireNatureReactionsConfig.steamCloudRadius;
                    int durL = ElementalFireNatureReactionsConfig.steamCondensationDurationBase + fuelLevel * ElementalFireNatureReactionsConfig.steamCondensationDurationPerLevel;
                    DebugCommand.sendSteamCloudCombinedLog(target, attacker, false, fuelLevel, baseDmgL, 1.0f, radL, durL,
                            ElementalFireNatureReactionsConfig.steamCloudHeightCeiling, ElementalFireNatureReactionsConfig.steamClearAggro,
                            ElementType.NONE, 1.0f, false, baseDmgL);
                } else {
                    DebugCommand.sendReactionFailed(attacker, "steam", "power_low",
                            Component.translatable("element.frost"),
                            frostPower, threshold);
                }
            }
        }
    }

    private static void processDefenseLogic(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        float currentDamage = event.getAmount();

        if (checkImmunity(target)) {
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

        double totalReduction = Math.min(actualFireRed + actualProtRed, 1.0);
        float reducedDamage = trueRawDamage * (float) (1.0 - totalReduction);
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
        if (entity instanceof Player player && player.isCreative()) return;
        double searchRadius = ElementalFireNatureReactionsConfig.steamCloudRadius * STEAM_SCAN_RADIUS_MULTIPLIER;
        AABB box = entity.getBoundingBox().inflate(searchRadius);
        List<AreaEffectCloud> clouds = entity.level().getEntitiesOfClass(AreaEffectCloud.class, box, c -> c.getTags().contains(TAG_STEAM_CLOUD));

        if (clouds.isEmpty()) {
            CompoundTag data = entity.getPersistentData();
            if (data.contains(NBT_STEAM_BLINDNESS)) {
                entity.removeEffect(MobEffects.BLINDNESS);
                data.remove(NBT_STEAM_BLINDNESS);
            }
            if (data.contains(NBT_STATIC_CLOUD_UUID)) {
                entity.removeEffect(ModMobEffects.PARALYSIS.get());
                data.remove(NBT_STATIC_CLOUD_UUID);
            }
            if (data.contains(NBT_FROSTED_CLOUD_UUID)) {
                entity.removeEffect(ModMobEffects.FREEZE.get());
                data.remove(NBT_FROSTED_CLOUD_UUID);
                data.remove(FrostbiteHandler.NBT_FREEZE_STACKS);
                data.remove(FrostbiteHandler.NBT_FREEZE_COOLDOWN);
                data.remove(FrostbiteHandler.NBT_FROZEN_FROSTBITE_STACKS);
            }
            data.remove(NBT_STATIC_DMG_CLOUD_UUID);
            data.remove(NBT_STEAM_SCALDING_LOGGED);
            return;
        }

        boolean isHighHeat = false;
        boolean isCondensing = false;
        boolean isStaticCharged = false;
        boolean isFrosted = false;
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
            } else {
                isCondensing = true;
                if (cloud.getTags().contains(TAG_STATIC_CHARGED)) {
                    isStaticCharged = true;
                }
                if (cloud.getTags().contains(TAG_FROSTED)) {
                    isFrosted = true;
                }
            }
        }

        int auraStaticStacks = entity.getPersistentData().getInt("ec_static_stacks");
        int auraFrostStacks = FrostbiteHandler.getFrostbiteStacks(entity);
        boolean hasStaticAura = ElementalThunderFrostReactionsConfig.staticAuraThreshold > 0 && auraStaticStacks >= ElementalThunderFrostReactionsConfig.staticAuraThreshold;
        boolean hasFrostAura = ElementalThunderFrostReactionsConfig.frostbiteAuraThreshold > 0 && auraFrostStacks >= ElementalThunderFrostReactionsConfig.frostbiteAuraThreshold
                && FrostbiteHandler.hasFrostbite(entity);

        if ((hasStaticAura || hasFrostAura) && !clouds.isEmpty()) {
            double staticRange = hasStaticAura ? auraStaticStacks * ElementalThunderFrostReactionsConfig.staticAuraBaseRange : 0;
            double frostRange = hasFrostAura ? FrostbiteHandler.getAuraRange(auraFrostStacks) : 0;

            for (AreaEffectCloud cloud : clouds) {
                if (cloud.getTags().contains(TAG_HIGH_HEAT)) continue;
                if (cloud.getTags().contains(TAG_STATIC_CHARGED) || cloud.getTags().contains(TAG_FROSTED)) continue;

                double dx = cloud.getX() - entity.getX();
                double dz = cloud.getZ() - entity.getZ();
                double hDist = Math.sqrt(dx * dx + dz * dz);

                boolean inStaticRange = hasStaticAura && hDist <= staticRange + cloud.getRadius();
                boolean inFrostRange = hasFrostAura && hDist <= frostRange + cloud.getRadius();

                if (!inStaticRange && !inFrostRange) continue;

                boolean convertToStatic = false;
                boolean convertToFrost = false;

                if (inStaticRange && inFrostRange) {
                    if (auraStaticStacks > auraFrostStacks) {
                        convertToStatic = true;
                    } else if (auraFrostStacks > auraStaticStacks) {
                        convertToFrost = true;
                    } else {
                        if (entity.level().random.nextBoolean()) {
                            convertToStatic = true;
                        } else {
                            convertToFrost = true;
                        }
                    }
                } else if (inStaticRange) {
                    convertToStatic = true;
                } else {
                    convertToFrost = true;
                }

                if (convertToStatic && ElementalThunderFrostReactionsConfig.staticSteamCloudTriggerStacks > 0) {
                    int sourceTimer = entity.getPersistentData().getInt("ec_static_timer");
                    int interval = ElementalThunderFrostReactionsConfig.staticDamageIntervalTicks;
                    int remainingHits = Math.max(1, (sourceTimer + interval - 1) / interval);
                    float settlementDamage = 0;
                    for (int i = 0; i < remainingHits; i++) {
                        settlementDamage += StaticShockHandler.getRandomStaticDamage(entity);
                    }
                    cloud.addTag(TAG_STATIC_CHARGED);
                    cloud.addTag("EC_StaticDmg_" + String.format("%.2f", settlementDamage));
                    if (!entity.level().isClientSide) {
                        entity.level().playSound(null, cloud.getX(), cloud.getY(), cloud.getZ(),
                                SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.5f, 1.2f);
                    }
                    DebugCommand.StaticSteamCloudLogContext sctx = new DebugCommand.StaticSteamCloudLogContext();
                    sctx.source = entity;
                    sctx.triggerStacks = auraStaticStacks;
                    sctx.settlementDamage = settlementDamage;
                    sctx.cloudDuration = cloud.getDuration();
                    DebugCommand.sendStaticSteamCloudLog(sctx);
                } else if (convertToFrost && ElementalThunderFrostReactionsConfig.frostedSteamCloudTriggerStacks > 0) {
                    cloud.addTag(TAG_FROSTED);
                    if (!entity.level().isClientSide) {
                        entity.level().playSound(null, cloud.getX(), cloud.getY(), cloud.getZ(),
                                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 0.6f);
                    }
                    DebugCommand.FrostedSteamCloudLogContext fsctx = new DebugCommand.FrostedSteamCloudLogContext();
                    fsctx.source = entity;
                    fsctx.triggerStacks = auraFrostStacks;
                    fsctx.cloudDuration = cloud.getDuration();
                    fsctx.affectedCount = 0;
                    DebugCommand.sendFrostedSteamCloudLog(fsctx);
                }
            }
        }

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
                }
            }
            if (entity.tickCount % 20 == 0 && !aboveCeiling) {
                float baseDamage = (float) ElementalFireNatureReactionsConfig.steamScaldingDamage;
                float scale = (float) ElementalFireNatureReactionsConfig.steamDamageScalePerLevel;
                float levelMultiplier = 1.0f + ((cloudLevel - 1) * scale);
                float damage = baseDamage * levelMultiplier;

                ElementType type = ElementUtils.getConsistentAttackElement(entity);
                float elementMultiplier = 1.0f;
                if (type == ElementType.FIRE) {
                    elementMultiplier = (float) ElementalFireNatureReactionsConfig.steamScaldingMultiplierFire;
                } else if (type == ElementType.NATURE) {
                    elementMultiplier = (float) ElementalFireNatureReactionsConfig.steamScaldingMultiplierNature;
                } else if (type == ElementType.THUNDER) {
                    elementMultiplier = (float) ElementalFireNatureReactionsConfig.steamScaldingMultiplierThunder;
                } else if (type == ElementType.FROST) {
                    elementMultiplier = (float) ElementalFireNatureReactionsConfig.steamScaldingMultiplierFrost;
                }
                damage *= elementMultiplier;

                if (entity.hasEffect(ModMobEffects.SPORES.get())) {
                    ReactionHandler.triggerToxicBlast(entity.level(), null, entity, (double) cloudLevel);
                }
                boolean fireImmune = entity.fireImmune();
                if (fireImmune) {
                    damage *= (float) ElementalFireNatureReactionsConfig.scorchedImmuneModifier;
                }

                if (damage > 0 && !checkImmunity(entity)) {
                    ElementDamageHelper.applyDamage(entity, damage, ModDamageTypes.source(entity.level(), ModDamageTypes.STEAM_SCALDING, heatSource));
                    if (!entity.getPersistentData().getBoolean(NBT_STEAM_SCALDING_LOGGED)) {
                        DebugCommand.sendSteamScaldingTickLog(entity, baseDamage, levelMultiplier, type, elementMultiplier, damage);
                        entity.getPersistentData().putBoolean(NBT_STEAM_SCALDING_LOGGED, true);
                    }
                }

            }
            if (entity instanceof PathfinderMob mob && heatSource != null && !checkImmunity(entity)) {
                mob.setTarget(null);
                if (mob.getNavigation().isDone()) {
                    double margin = 4.0;
                    double dx = mob.getX() - heatSource.getX();
                    double dz = mob.getZ() - heatSource.getZ();
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist < 0.01) {
                        dx = mob.level().random.nextFloat() - 0.5;
                        dz = mob.level().random.nextFloat() - 0.5;
                        dist = Math.sqrt(dx * dx + dz * dz);
                    }
                    double targetX = heatSource.getX() + (dx / dist) * (heatSource.getRadius() + margin);
                    double targetZ = heatSource.getZ() + (dz / dist) * (heatSource.getRadius() + margin);
                    mob.getNavigation().moveTo(targetX, mob.getY(), targetZ, 1.5);
                }
            }
            if (WetnessHandler.getWetnessLevel(entity) > 0) {
                removeWetness(entity);
            }
            if (FrostbiteHandler.hasFrostbite(entity)) {
                FrostbiteHandler.clearFrostbite(entity);
                if (!entity.level().isClientSide) {
                    entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                            SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.0f);
                }
            }
        } else if (isCondensing) {
            // Trigger: Static entity enters condensing cloud → electrify the cloud
            if (!isStaticCharged && ElementalThunderFrostReactionsConfig.staticSteamCloudTriggerStacks > 0) {
                CompoundTag data = entity.getPersistentData();
                int staticStacks = data.getInt("ec_static_stacks");
                if (staticStacks >= ElementalThunderFrostReactionsConfig.staticSteamCloudTriggerStacks) {
                    int sourceTimer = data.getInt("ec_static_timer");
                    int interval = ElementalThunderFrostReactionsConfig.staticDamageIntervalTicks;
                    if (interval < 1) interval = 1;
                    int remainingHits = (sourceTimer + interval - 1) / interval;
                    float settlementDamage = 0;
                    for (int i = 0; i < remainingHits; i++) {
                        settlementDamage += StaticShockHandler.getRandomStaticDamage(entity);
                    }
                    for (AreaEffectCloud cloud : clouds) {
                        if (!isEntityInCloud(entity, cloud)) continue;
                        if (!cloud.getTags().contains(TAG_HIGH_HEAT) && !cloud.getTags().contains(TAG_STATIC_CHARGED)) {
                            cloud.addTag(TAG_STATIC_CHARGED);
                            cloud.addTag("EC_StaticDmg_" + String.format("%.2f", settlementDamage));
                        }
                    }
                    isStaticCharged = true;
                    data.remove("ec_static_stacks");
                    data.remove("ec_static_timer");
                    data.remove("ec_static_damage_timer");
                    if (entity.hasEffect(ModMobEffects.STATIC_SHOCK.get())) {
                        entity.removeEffect(ModMobEffects.STATIC_SHOCK.get());
                    }
                    if (!entity.level().isClientSide) {
                        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                                net.minecraft.sounds.SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.5f, 1.2f);
                    }
                    int cloudDuration = 0;
                    for (AreaEffectCloud cloud : clouds) {
                        if (isEntityInCloud(entity, cloud) && cloud.getTags().contains(TAG_STATIC_CHARGED)) {
                            cloudDuration = cloud.getDuration();
                            break;
                        }
                    }
                    DebugCommand.StaticSteamCloudLogContext sctx = new DebugCommand.StaticSteamCloudLogContext();
                    sctx.source = entity;
                    sctx.triggerStacks = staticStacks;
                    sctx.settlementDamage = settlementDamage;
                    sctx.cloudDuration = cloudDuration;
                    DebugCommand.sendStaticSteamCloudLog(sctx);
                }
            }

            if (isStaticCharged && ElementalThunderFrostReactionsConfig.staticSteamCloudTriggerStacks > 0) {
                String staticCloudUUID = getFirstCloudUUIDWithTag(clouds, entity, TAG_STATIC_CHARGED);
                if (isImmuneToThunderResist(entity)) {
                    if (entity.getPersistentData().contains(NBT_STATIC_CLOUD_UUID)) {
                        entity.removeEffect(ModMobEffects.PARALYSIS.get());
                        entity.getPersistentData().remove(NBT_STATIC_CLOUD_UUID);
                    }
                } else {
                    String appliedDmgUUID = entity.getPersistentData().getString(NBT_STATIC_DMG_CLOUD_UUID);
                    float settlementDamage = getCloudStaticDamage(clouds, entity);
                    if (settlementDamage > 0 && !staticCloudUUID.equals(appliedDmgUUID)) {
                        ElementDamageHelper.applyDamage(entity, settlementDamage, ModDamageTypes.source(entity.level(), ModDamageTypes.STATIC_SHOCK));
                        entity.getPersistentData().putString(NBT_STATIC_DMG_CLOUD_UUID, staticCloudUUID);
                    }
                    if (!isImmuneToParalysis(entity)) {
                        int paralysisDuration = getStaticCloudRemainingDuration(clouds, entity);
                        if (paralysisDuration > 0) {
                            CompoundTag data = entity.getPersistentData();
                            int paralysisAmplifier = 2;
                            MobEffectInstance currentParalysis = entity.getEffect(ModMobEffects.PARALYSIS.get());
                            if ((currentParalysis == null || currentParalysis.getDuration() < paralysisDuration)
                                    && ElementalThunderFrostReactionsConfig.paralysisMaxStacks > 0) {
                                entity.addEffect(new MobEffectInstance(
                                        ModMobEffects.PARALYSIS.get(), paralysisDuration, paralysisAmplifier, false, false, true
                                ));
                                data.putString(NBT_STATIC_CLOUD_UUID, staticCloudUUID);
                            }
                        }
                    }
                }
            } else {
                if (entity.getPersistentData().contains(NBT_STATIC_CLOUD_UUID)) {
                    entity.removeEffect(ModMobEffects.PARALYSIS.get());
                    entity.getPersistentData().remove(NBT_STATIC_CLOUD_UUID);
                }
            }

            if (!isFrosted && ElementalThunderFrostReactionsConfig.frostedSteamCloudTriggerStacks > 0) {
                int frostbiteStacks = FrostbiteHandler.getFrostbiteStacks(entity);
                if (frostbiteStacks >= ElementalThunderFrostReactionsConfig.frostedSteamCloudTriggerStacks) {
                    for (AreaEffectCloud cloud : clouds) {
                        if (!isEntityInCloud(entity, cloud)) continue;
                        if (!cloud.getTags().contains(TAG_HIGH_HEAT) && !cloud.getTags().contains(TAG_FROSTED)) {
                            cloud.addTag(TAG_FROSTED);
                        }
                    }
                    isFrosted = true;
                    FrostbiteHandler.clearFrostbite(entity);
                    int fcloudDuration = 0;
                    for (AreaEffectCloud cloud : clouds) {
                        if (isEntityInCloud(entity, cloud) && cloud.getTags().contains(TAG_FROSTED)) {
                            fcloudDuration = cloud.getDuration();
                            break;
                        }
                    }
                    DebugCommand.FrostedSteamCloudLogContext fsctx = new DebugCommand.FrostedSteamCloudLogContext();
                    fsctx.source = entity;
                    fsctx.triggerStacks = frostbiteStacks;
                    fsctx.cloudDuration = fcloudDuration;
                    fsctx.affectedCount = 0;
                    DebugCommand.sendFrostedSteamCloudLog(fsctx);
                    if (!entity.level().isClientSide) {
                        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 0.6f);
                    }
                }
            }

            if (isFrosted) {
                if (!FrostbiteHandler.isFrostbiteImmune(entity) && !FrostbiteHandler.isFreezeImmune(entity)) {
                    String frostedCloudUUID = getFirstCloudUUIDWithTag(clouds, entity, TAG_FROSTED);
                    if (!FrostbiteHandler.isFrozen(entity)) {
                        int wetnessLevel = WetnessHandler.getWetnessLevel(entity);
                        int freezeStacks = Math.max(1, wetnessLevel);
                        int maxStacks = ElementalThunderFrostReactionsConfig.freezeMaxStacks;
                        if (maxStacks <= 0) return;
                        if (freezeStacks > maxStacks) freezeStacks = maxStacks;
                        int freezeDuration = freezeStacks * ElementalThunderFrostReactionsConfig.freezeDurationPerStackTicks;
                        if (freezeDuration < 20) freezeDuration = 20;
                        entity.addEffect(new MobEffectInstance(ModMobEffects.FREEZE.get(), freezeDuration, freezeStacks - 1, false, false, true));
                        entity.getPersistentData().putString(NBT_FROSTED_CLOUD_UUID, frostedCloudUUID);
                        entity.getPersistentData().putInt(FrostbiteHandler.NBT_FREEZE_STACKS, freezeStacks);
                        entity.getPersistentData().putLong(FrostbiteHandler.NBT_FREEZE_COOLDOWN,
                            entity.level().getGameTime() + freezeDuration + ElementalThunderFrostReactionsConfig.freezeCooldownTicks);
                        WetnessHandler.clearWetnessData(entity);
                        if (FrostbiteHandler.hasFrostbite(entity)) {
                            FrostbiteHandler.clearFrostbite(entity);
                        }
                    } else {
                        MobEffectInstance currentFreeze = entity.getEffect(ModMobEffects.FREEZE.get());
                        if (currentFreeze != null) {
                            int currentAmplifier = currentFreeze.getAmplifier();
                            int fullDuration = (currentAmplifier + 1) * ElementalThunderFrostReactionsConfig.freezeDurationPerStackTicks;
                            if (currentFreeze.getDuration() < fullDuration - 20) {
                                entity.addEffect(new MobEffectInstance(ModMobEffects.FREEZE.get(), fullDuration, currentAmplifier, false, false, true));
                                entity.getPersistentData().putString(NBT_FROSTED_CLOUD_UUID, frostedCloudUUID);
                            }
                        }
                    }
                }
            }
            // Clear frosted cloud freeze when the specific frosted cloud is gone or no longer frosted
            if (entity.getPersistentData().contains(NBT_FROSTED_CLOUD_UUID)) {
                String savedUUID = entity.getPersistentData().getString(NBT_FROSTED_CLOUD_UUID);
                AreaEffectCloud matchedCloud = null;
                for (AreaEffectCloud cloud : clouds) {
                    if (cloud.getTags().contains(TAG_FROSTED) && isEntityInCloud(entity, cloud)) {
                        String uuid = getCloudUUID(cloud);
                        if (savedUUID.equals(uuid)) {
                            matchedCloud = cloud;
                            break;
                        }
                    }
                }
                if (matchedCloud == null) {
                    entity.removeEffect(ModMobEffects.FREEZE.get());
                    entity.getPersistentData().remove(NBT_FROSTED_CLOUD_UUID);
                    entity.getPersistentData().remove(FrostbiteHandler.NBT_FREEZE_STACKS);
                    entity.getPersistentData().remove(FrostbiteHandler.NBT_FREEZE_COOLDOWN);
                    entity.getPersistentData().remove(FrostbiteHandler.NBT_FROZEN_FROSTBITE_STACKS);
                } else if (FrostbiteHandler.isFrozen(entity) && !FrostbiteHandler.isFreezeImmune(entity)) {
                    MobEffectInstance currentFreeze = entity.getEffect(ModMobEffects.FREEZE.get());
                    if (currentFreeze != null && matchedCloud.getDuration() > currentFreeze.getDuration()) {
                        entity.addEffect(new MobEffectInstance(ModMobEffects.FREEZE.get(),
                            matchedCloud.getDuration(), currentFreeze.getAmplifier(), false, false, true));
                    }
                }
            }

            // Static-charged and frosted clouds prevent condensation (no wetness gain)
            if (!isStaticCharged && !isFrosted) {
                int currentTimer = entity.getPersistentData().getInt(NBT_CONDENSATION_TIMER);
                currentTimer += 10;
                int delayThreshold = Math.max(10, ElementalFireNatureReactionsConfig.steamCondensationDelay);
                if (currentTimer >= delayThreshold) {
                    int currentWet = WetnessHandler.getWetnessLevel(entity);
                    int max = ElementalFireNatureReactionsConfig.wetnessMaxLevel;
                    if (currentWet < max) {
                        WetnessHandler.updateWetnessLevel(entity, currentWet + 1);
                        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.NEUTRAL, 1.0f, 1.0f);
                    }
                    currentTimer = 0;
                }
                entity.getPersistentData().putInt(NBT_CONDENSATION_TIMER, currentTimer);


            } else {
                entity.getPersistentData().remove(NBT_CONDENSATION_TIMER);

            }
        }
    }

    private static float getCloudStaticDamage(List<AreaEffectCloud> clouds, LivingEntity entity) {
        for (AreaEffectCloud cloud : clouds) {
            if (!isEntityInCloud(entity, cloud)) continue;
            for (String tag : cloud.getTags()) {
                if (tag.startsWith("EC_StaticDmg_")) {
                    try {
                        return Float.parseFloat(tag.substring("EC_StaticDmg_".length()));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return 0;
    }

    private static int getStaticCloudRemainingDuration(List<AreaEffectCloud> clouds, LivingEntity entity) {
        for (AreaEffectCloud cloud : clouds) {
            if (!isEntityInCloud(entity, cloud)) continue;
            if (cloud.getTags().contains(TAG_STATIC_CHARGED)) {
                return cloud.getDuration();
            }
        }
        return 0;
    }

    private static boolean isEntityInCloud(LivingEntity entity, AreaEffectCloud cloud) {
        if (cloud.getTags().contains(TAG_STEAM_CLOUD)) {
            double heightCeiling = ElementalFireNatureReactionsConfig.steamCloudHeightCeiling;
            double dx = entity.getX() - cloud.getX();
            double dz = entity.getZ() - cloud.getZ();
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);
            double dy = entity.getY() - cloud.getY();
            return horizontalDist <= cloud.getRadius() && dy >= 0 && dy <= heightCeiling;
        }
        if (cloud.getBoundingBox().inflate(0.1).intersects(entity.getBoundingBox())) {
            return true;
        }
        return false;
    }

    private static boolean isTriggerBlocked(LivingEntity entity) {
        if (entity.level().isClientSide) return false;
        double searchRadius = ElementalFireNatureReactionsConfig.steamCloudRadius * 2.0;
        AABB box = entity.getBoundingBox().inflate(searchRadius);
        List<AreaEffectCloud> clouds = entity.level().getEntitiesOfClass(AreaEffectCloud.class, box, c -> c.getTags().contains(TAG_STEAM_CLOUD));
        for (AreaEffectCloud cloud : clouds) {
            if (isEntityInCloud(entity, cloud)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isOnSteamCooldown(LivingEntity entity) {
        return entity.getPersistentData().contains(NBT_STEAM_ATTACKER_COOLDOWN);
    }

    public static void applySteamCooldown(LivingEntity entity, int cloudDuration) {
        int cooldownTicks = ElementalFireNatureReactionsConfig.steamTriggerCooldown;
        if (cooldownTicks > 0) {
            entity.getPersistentData().putInt(NBT_STEAM_ATTACKER_COOLDOWN, cloudDuration + cooldownTicks);
        }
    }

    public static int computeCloudDuration(boolean isHighHeat, int level) {
        int baseDuration;
        int durationInc;
        if (isHighHeat) {
            baseDuration = ElementalFireNatureReactionsConfig.steamCloudDuration;
            durationInc = ElementalFireNatureReactionsConfig.steamDurationPerLevel;
        } else {
            baseDuration = ElementalFireNatureReactionsConfig.steamCondensationDurationBase;
            durationInc = ElementalFireNatureReactionsConfig.steamCondensationDurationPerLevel;
        }
        return baseDuration + (level * durationInc);
    }

    private static void removeWetness(LivingEntity entity) {
        WetnessHandler.clearWetnessData(entity);
    }

    private static boolean checkImmunity(LivingEntity entity) {
        if (entity.hasEffect(MobEffects.FIRE_RESISTANCE)) return true;
        var key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (key != null && ElementalFireNatureReactionsConfig.cachedSteamBlacklist.contains(key.toString())) return true;
        int resist = ElementUtils.getDisplayResistance(entity, ElementType.FIRE);
        int threshold = ElementalFireNatureReactionsConfig.steamImmunityThreshold;
        return resist >= threshold;
    }

    private static boolean isImmuneToThunderResist(LivingEntity entity) {
        var key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (key != null && ElementalThunderFrostReactionsConfig.cachedStaticImmunityBlacklist.contains(key.toString())) {
            return true;
        }
        int resist = ElementUtils.getDisplayResistance(entity, ElementType.THUNDER);
        return resist >= ElementalThunderFrostReactionsConfig.staticResistImmunityThreshold;
    }

    private static boolean isImmuneToParalysis(LivingEntity entity) {
        var key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return key != null && ElementalThunderFrostReactionsConfig.cachedParalysisImmunityBlacklist.contains(key.toString());
    }

    public static AreaEffectCloud spawnSteamCloud(LivingEntity target, boolean isHighHeat, int fuelLevel) {
        if (!(target.level() instanceof ServerLevel serverLevel)) return null;
        return spawnSteamCloud(serverLevel, target.getX(), target.getY(), target.getZ(), target.getBbHeight(), isHighHeat, fuelLevel);
    }

    public static AreaEffectCloud spawnSteamCloud(ServerLevel serverLevel, double x, double y, double z, boolean isHighHeat, int fuelLevel) {
        return spawnSteamCloud(serverLevel, x, y, z, 1.8f, isHighHeat, fuelLevel);
    }

    private static AreaEffectCloud spawnSteamCloud(ServerLevel serverLevel, double x, double y, double z, float height, boolean isHighHeat, int fuelLevel) {
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

        AreaEffectCloud cloud = new AreaEffectCloud(serverLevel, x, y, z);
        cloud.setRadius(radius);
        cloud.setRadiusOnUse(0F);
        cloud.setRadiusPerTick(0F);
        cloud.setDuration(duration);
        cloud.setParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AIR.defaultBlockState()));
        cloud.addTag(TAG_STEAM_CLOUD);
        cloud.addTag(TAG_LEVEL_PREFIX + level);
        cloud.addTag(TAG_CLOUD_UUID_PREFIX + java.util.UUID.randomUUID().toString());
        if (isHighHeat) {
            cloud.addTag(TAG_HIGH_HEAT);
        }
        serverLevel.addFreshEntity(cloud);
        ACTIVE_STEAM_CLOUDS.add(cloud);
        EffectHelper.playSteamBurst(serverLevel, x, y, z, height, radius, level, isHighHeat);
        return cloud;
    }

    public static String getCloudUUID(AreaEffectCloud cloud) {
        for (String tag : cloud.getTags()) {
            if (tag.startsWith(TAG_CLOUD_UUID_PREFIX)) {
                return tag.substring(TAG_CLOUD_UUID_PREFIX.length());
            }
        }
        return "";
    }

    private static String getFirstCloudUUIDWithTag(List<AreaEffectCloud> clouds, LivingEntity entity, String requiredTag) {
        for (AreaEffectCloud cloud : clouds) {
            if (cloud.getTags().contains(requiredTag) && isEntityInCloud(entity, cloud)) {
                return getCloudUUID(cloud);
            }
        }
        return "";
    }

    public static AreaEffectCloud findCloudByUUID(LivingEntity entity, String uuid) {
        if (entity.level().isClientSide || uuid == null || uuid.isEmpty()) return null;
        String targetTag = TAG_CLOUD_UUID_PREFIX + uuid;
        double searchRadius = ElementalFireNatureReactionsConfig.steamCloudRadius * STEAM_SCAN_RADIUS_MULTIPLIER;
        AABB box = entity.getBoundingBox().inflate(searchRadius);
        List<AreaEffectCloud> clouds = entity.level().getEntitiesOfClass(AreaEffectCloud.class, box,
                c -> c.getTags().contains(TAG_STEAM_CLOUD) && c.getTags().contains(targetTag));
        for (AreaEffectCloud cloud : clouds) {
            if (isEntityInCloud(entity, cloud)) {
                return cloud;
            }
        }
        return null;
    }
}
