package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.util.EffectHelper;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
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
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

    private static final List<AreaEffectCloud> ACTIVE_STEAM_CLOUDS = new ArrayList<>();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!ElementalFireNatureReactionsConfig.steamReactionEnabled) return;

        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            processTriggerLogic(event, attacker, event.getEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
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

        Iterator<AreaEffectCloud> iterator = ACTIVE_STEAM_CLOUDS.iterator();
        while (iterator.hasNext()) {
            AreaEffectCloud cloud = iterator.next();

            if (cloud.isRemoved()) {
                iterator.remove();
                continue;
            }

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

    private static void processTriggerLogic(LivingHurtEvent event, LivingEntity attacker, LivingEntity target) {
        ElementType attackElement = ElementUtils.getConsistentAttackElement(attacker);

        if (event.getSource().is(DamageTypeTags.IS_FIRE)) attackElement = ElementType.FIRE;
        if (event.getSource().is(DamageTypeTags.IS_FREEZING)) attackElement = ElementType.FROST;

        int firePower = ElementUtils.getDisplayEnhancement(attacker, ElementType.FIRE);
        int frostPower = ElementUtils.getDisplayEnhancement(attacker, ElementType.FROST);

        boolean targetIsWet = target.getPersistentData().getInt(WetnessHandler.NBT_WETNESS) > 0;
        int targetWetness = target.getPersistentData().getInt(WetnessHandler.NBT_WETNESS);
        ElementType targetElement = ElementUtils.getElementType(target);

        if (attackElement == ElementType.FIRE) {
            if (targetIsWet || targetElement == ElementType.FROST) {
                int threshold = ElementalFireNatureReactionsConfig.steamTriggerThresholdFire;

                if (firePower >= threshold) {
                    if (isTriggerBlocked(target)) return;

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

                    spawnSteamCloud(target, true, fuelLevel);
                    setTriggerCooldown(target);

                    DebugCommand.sendSteamTriggerLog(attacker, true, fuelLevel);

                    if (targetIsWet) {
                        target.addTag("EC_WetnessSnapshot_" + targetWetness);
                        removeWetness(target);
                    }
                } else {
                    if (targetIsWet) {
                        removeWetness(target);
                    }
                }
            }
        } 
        else if (attackElement == ElementType.FROST) {
            if (targetElement == ElementType.FIRE) {
                if (target.level().dimension() == Level.NETHER) {
                    return;
                }

                int threshold = ElementalFireNatureReactionsConfig.steamTriggerThresholdFrost;

                if (frostPower >= threshold) {
                    if (isTriggerBlocked(target)) return;

                    int targetFirePower = ElementUtils.getDisplayEnhancement(target, ElementType.FIRE);
                    int step = Math.max(1, ElementalFireNatureReactionsConfig.steamCondensationStepFire);

                    int level = 1 + (targetFirePower / step);
                    int maxLevel = ElementalFireNatureReactionsConfig.steamLowHeatMaxLevel;
                    level = Math.min(level, maxLevel);

                    spawnSteamCloud(target, false, level);
                    setTriggerCooldown(target);

                    DebugCommand.sendSteamTriggerLog(attacker, false, level);
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

        ElementType type = ElementUtils.getElementType(target);
        if (type == ElementType.FROST || type == ElementType.NATURE) {
            float floorRatio = (float) ElementalFireNatureReactionsConfig.steamDamageFloorRatio;
            float floorLimit = trueRawDamage * floorRatio;

            if (reducedDamage < floorLimit) {
                reducedDamage = floorLimit;
            }
        }

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

        double searchRadius = ElementalFireNatureReactionsConfig.steamCloudRadius * ElementalFireNatureReactionsConfig.steamScanRadiusMultiplier;
        AABB box = entity.getBoundingBox().inflate(searchRadius);
        List<AreaEffectCloud> clouds = entity.level().getEntitiesOfClass(AreaEffectCloud.class, box,
                c -> c.getTags().contains(TAG_STEAM_CLOUD));

        if (clouds.isEmpty()) return;

        boolean isHighHeat = false;
        boolean isCondensing = false;
        int cloudLevel = 1;
        AreaEffectCloud heatSource = null;

        for (AreaEffectCloud cloud : clouds) {
            double dx = entity.getX() - cloud.getX();
            double dz = entity.getZ() - cloud.getZ();
            double radius = cloud.getRadius();
            double distSqr = dx * dx + dz * dz;
            
            double effectiveRadius = radius + (entity.getBbWidth() / 2.0);
            if (distSqr > effectiveRadius * effectiveRadius) continue;

            double dy = entity.getY() - cloud.getY();

            if (dy < -0.5) continue;
            if (dy > ElementalFireNatureReactionsConfig.steamCloudHeightCeiling) continue;

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

        if (isHighHeat) {
            if (entity.getPersistentData().contains(NBT_CONDENSATION_TIMER)) {
                entity.getPersistentData().remove(NBT_CONDENSATION_TIMER);
            }

            if (entity.tickCount % 20 == 0) {
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

                if (entity.getPersistentData().getInt(WetnessHandler.NBT_WETNESS) > 0) {
                    removeWetness(entity);
                }
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
                }

                currentTimer = 0;
            }

            entity.getPersistentData().putInt(NBT_CONDENSATION_TIMER, currentTimer);

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
                    }
                    sporeTimer = 0;
                }
                entity.getPersistentData().putInt(NBT_SPORE_GROWTH_TIMER, sporeTimer);
            }
        }
    }

    private static boolean isTriggerBlocked(LivingEntity entity) {
        if (entity.getPersistentData().getInt(NBT_STEAM_TRIGGER_COOLDOWN) > 0) return true;

        if (entity.level().isClientSide) return false;

        double searchRadius = ElementalFireNatureReactionsConfig.steamCloudRadius * 2.0; 
        AABB box = entity.getBoundingBox().inflate(searchRadius);
        List<AreaEffectCloud> clouds = entity.level().getEntitiesOfClass(AreaEffectCloud.class, box,
                c -> c.getTags().contains(TAG_STEAM_CLOUD));

        for (AreaEffectCloud cloud : clouds) {
            double dx = entity.getX() - cloud.getX();
            double dz = entity.getZ() - cloud.getZ();
            double distSqr = dx * dx + dz * dz;
            double radius = cloud.getRadius();
            
            if (distSqr < radius * radius) {
                double dy = entity.getY() - cloud.getY();
                if (dy > -0.5 && dy < ElementalFireNatureReactionsConfig.steamCloudHeightCeiling) {
                    return true; 
                }
            }
        }
        return false;
    }

    private static void setTriggerCooldown(LivingEntity entity) {
        entity.getPersistentData().putInt(NBT_STEAM_TRIGGER_COOLDOWN, ElementalFireNatureReactionsConfig.steamTriggerCooldown);
    }

    private static void removeWetness(LivingEntity entity) {
        entity.getPersistentData().remove(WetnessHandler.NBT_WETNESS);
        entity.getPersistentData().remove(WetnessHandler.NBT_RAIN_TIMER);
        entity.getPersistentData().remove(WetnessHandler.NBT_DECAY_TIMER);
        entity.removeEffect(com.xulai.elementalcraft.potion.ModMobEffects.WETNESS.get());
    }

    private static boolean checkImmunity(LivingEntity entity) {
        if (entity.fireImmune() || entity.hasEffect(MobEffects.FIRE_RESISTANCE)) return true;

        String id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        if (ElementalFireNatureReactionsConfig.cachedSteamBlacklist.contains(id)) return true;

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

        cloud.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ElementalFireNatureReactionsConfig.steamBlindnessDuration));

        cloud.addTag(TAG_STEAM_CLOUD);
        cloud.addTag(TAG_LEVEL_PREFIX + level);

        if (isHighHeat) {
            cloud.addTag(TAG_HIGH_HEAT);
        }

        serverLevel.addFreshEntity(cloud);

        ACTIVE_STEAM_CLOUDS.add(cloud);

        EffectHelper.playSteamBurst(serverLevel, target, radius, level, isHighHeat);
    }
}