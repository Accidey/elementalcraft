package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalReactionConfig;
import com.xulai.elementalcraft.init.ModDamageTypes;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantment.Rarity;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Random;

/**
 * SteamReactionHandler
 *
 * 中文说明：
 * 处理蒸汽云元素反应的核心逻辑类。
 * 包含触发判定（双轨制：赤焰vs潮湿/冰霜，冰霜vs赤焰）、防御计算（分层防御）和持续环境效果。
 *
 * English Description:
 * Core logic class for Steam Cloud elemental reactions.
 * Includes trigger logic (Dual-Track: Fire vs Wet/Frost, Frost vs Fire), defense calculation (Layered Defense), and persistent environmental effects.
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class SteamReactionHandler {

    public static final String TAG_STEAM_CLOUD = "EC_SteamCloud";
    public static final String TAG_HIGH_HEAT = "EC_HighHeat";
    public static final String TAG_LEVEL_PREFIX = "EC_Level_";
    public static final String TAG_SELF_DRYING_PENALTY = "EC_SelfDryingPenalty";

    // NBT Key for tracking time spent inside low-heat steam
    // 用于追踪在低温蒸汽中停留时间的 NBT 键
    private static final String NBT_CONDENSATION_TIMER = "EC_SteamCondensationTimer";

    /**
     * 触发逻辑监听器。
     * 监听实体受伤事件，判断是否满足蒸汽反应的触发条件。
     *
     * Trigger Logic Listener.
     * Listens to LivingHurtEvent to determine if conditions for steam reaction are met.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!ElementalReactionConfig.steamReactionEnabled) return;

        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            processTriggerLogic(event, attacker, event.getEntity());
        }
    }

    /**
     * 防御逻辑监听器。
     * 监听实体受到蒸汽烫伤的事件，计算减伤。
     *
     * Defense Logic Listener.
     * Listens to LivingDamageEvent for steam scalding, calculating damage reduction.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getSource().is(ModDamageTypes.STEAM_SCALDING)) {
            processDefenseLogic(event);
        }
    }

    /**
     * 死亡逻辑监听器。
     * 处理被蒸汽烫死的生物掉落物逻辑。
     *
     * Death Logic Listener.
     * Handles drops for mobs killed by steam scalding.
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().is(ModDamageTypes.STEAM_SCALDING)) {
            event.getEntity().setSecondsOnFire(1);
        }
    }

    /**
     * 持续效果监听器。
     * 监听生物 Tick 事件，检测生物是否处于蒸汽云中并应用持续效果。
     *
     * Tick Logic Listener.
     * Listens to LivingTickEvent to check if entities are inside steam clouds and apply persistent effects.
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!ElementalReactionConfig.steamReactionEnabled) return;

        if (event.getEntity().tickCount % 10 != 0) return;

        processCloudEffects(event.getEntity());
    }

    /**
     * 级别 Tick 监听器 (视觉特效)。
     * 在服务端维护蒸汽云的粒子效果。
     *
     * Level Tick Listener (Visual Effects).
     * Maintains steam cloud particle effects on the server side.
     */
    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide) return;

        if (event.level.getGameTime() % 20 != 0) return;

        if (event.level instanceof ServerLevel serverLevel) {
            Iterable<Entity> entities = serverLevel.getAllEntities();

            for (Entity entity : entities) {
                if (entity instanceof AreaEffectCloud cloud) {
                    if (cloud.getTags().contains(TAG_STEAM_CLOUD)) {
                        spawnRisingSteamParticles(serverLevel, cloud);
                    }
                }
            }
        }
    }

    // =================================================================================================
    //                                  Logic Implementation / 逻辑实现
    // =================================================================================================

    /**
     * 处理蒸汽反应的触发逻辑。
     * 判断攻击元素与目标状态，决定是否生成蒸汽云或执行自我干燥。
     *
     * Processes the trigger logic for steam reactions.
     * Determines whether to spawn a steam cloud or perform self-drying based on attack element and target state.
     */
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
            int attackerWetness = attacker.getPersistentData().getInt(WetnessHandler.NBT_WETNESS);
            if (attackerWetness > 0) {
                applySelfDrying(attacker, event, attackerWetness);
                return;
            }

            if (targetIsWet || targetElement == ElementType.FROST) {
                int threshold = ElementalReactionConfig.steamTriggerThresholdFire;

                if (firePower >= threshold) {
                    int fuelLevel = 1;

                    if (targetIsWet) {
                        fuelLevel = targetWetness;
                    } else if (targetElement == ElementType.FROST) {
                        int targetFrostPower = ElementUtils.getDisplayEnhancement(target, ElementType.FROST);
                        int step = Math.max(1, ElementalReactionConfig.steamCondensationStepFrost);
                        fuelLevel = 1 + (targetFrostPower / step);
                    }

                    int maxLevel = ElementalReactionConfig.steamHighHeatMaxLevel;
                    fuelLevel = Math.min(fuelLevel, maxLevel);

                    spawnSteamCloud(target, true, fuelLevel);
                    DebugCommand.sendSteamTriggerLog(attacker, true, fuelLevel);

                    if (targetIsWet) {
                        // [关键修复] 在移除前，添加一个快照标签，供 CombatEvents 读取
                        // [Critical Fix] Add snapshot tag before removal for CombatEvents to read
                        target.addTag("EC_WetnessSnapshot_" + targetWetness);
                        removeWetness(target);
                    }
                } else {
                    if (targetIsWet) {
                        removeWetness(target);
                    }
                }
            }
        } else if (attackElement == ElementType.FROST) {
            if (targetElement == ElementType.FIRE) {
                int threshold = ElementalReactionConfig.steamTriggerThresholdFrost;

                if (frostPower >= threshold) {
                    int targetFirePower = ElementUtils.getDisplayEnhancement(target, ElementType.FIRE);
                    int step = Math.max(1, ElementalReactionConfig.steamCondensationStepFire);

                    int level = 1 + (targetFirePower / step);
                    int maxLevel = ElementalReactionConfig.steamLowHeatMaxLevel;
                    level = Math.min(level, maxLevel);

                    spawnSteamCloud(target, false, level);
                    DebugCommand.sendSteamTriggerLog(attacker, false, level);
                }
            }
        }
    }

    /**
     * 处理防御逻辑。
     * 计算抗性、附魔减伤及易伤保底机制。
     *
     * Processes defense logic.
     * Calculates resistance, enchantment damage reduction, and vulnerability floor mechanisms.
     */
    private static void processDefenseLogic(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        float currentDamage = event.getAmount();

        if (checkImmunity(target)) {
            event.setAmount(0);
            event.setCanceled(true);
            return;
        }

        float trueRawDamage = currentDamage;
        boolean wasReducedByVanilla = false;

        if (event.getSource().is(DamageTypeTags.IS_FIRE)) {
            int vanillaEPF = EnchantmentHelper.getDamageProtection(target.getArmorSlots(), event.getSource());

            if (vanillaEPF > 0) {
                float reductionRatio = Math.min(vanillaEPF, 20) * 0.04f;

                if (reductionRatio < 1.0f) {
                    trueRawDamage = currentDamage / (1.0f - reductionRatio);
                    wasReducedByVanilla = true;
                }
            }
        }

        int totalFireProtLevel = getTotalEnchantmentLevel(Enchantments.FIRE_PROTECTION, target);
        int totalProtLevel = getTotalEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, target);

        final double FIRE_PROT_FACTOR = 0.03125;
        final double PROT_FACTOR = 0.015625;

        double maxFireCap = ElementalReactionConfig.steamMaxFireProtCap;
        double maxGeneralCap = ElementalReactionConfig.steamMaxGeneralProtCap;

        double calculatedFireRed = totalFireProtLevel * FIRE_PROT_FACTOR;
        double calculatedProtRed = totalProtLevel * PROT_FACTOR;

        double actualFireRed = Math.min(calculatedFireRed, maxFireCap);
        double actualProtRed = Math.min(calculatedProtRed, maxGeneralCap);

        double totalReduction = Math.min(actualFireRed + actualProtRed, 1.0);

        float reducedDamage = trueRawDamage * (float) (1.0 - totalReduction);

        ElementType type = ElementUtils.getElementType(target);
        if (type == ElementType.FROST || type == ElementType.NATURE) {
            float floorRatio = (float) ElementalReactionConfig.steamDamageFloorRatio;
            float floorLimit = trueRawDamage * floorRatio;

            if (reducedDamage < floorLimit) {
                reducedDamage = floorLimit;
            }
        }

        event.setAmount(reducedDamage);
    }

    /**
     * 辅助方法：计算附魔总等级。
     *
     * Helper Method: Calculate total enchantment level.
     */
    private static int getTotalEnchantmentLevel(Enchantment ench, LivingEntity entity) {
        int total = 0;
        for (ItemStack stack : entity.getArmorSlots()) {
            total += EnchantmentHelper.getItemEnchantmentLevel(ench, stack);
        }
        return total;
    }

    /**
     * 处理蒸汽云对内部实体的持续效果。
     * 包括高温烫伤、低温冷凝（增加潮湿）等。
     *
     * Processes persistent effects of steam clouds on entities inside.
     * Includes high-heat scalding and low-heat condensation (increasing wetness).
     */
    private static void processCloudEffects(LivingEntity entity) {
        if (entity.level().isClientSide) return;

        double searchRadius = ElementalReactionConfig.steamCloudRadius * 3.0;
        AABB box = entity.getBoundingBox().inflate(searchRadius);
        List<AreaEffectCloud> clouds = entity.level().getEntitiesOfClass(AreaEffectCloud.class, box,
                c -> c.getTags().contains(TAG_STEAM_CLOUD));

        if (clouds.isEmpty()) return;

        boolean isHighHeat = false;
        boolean isCondensing = false;
        int cloudLevel = 1;

        for (AreaEffectCloud cloud : clouds) {
            if (entity.distanceToSqr(cloud) > cloud.getRadius() * cloud.getRadius()) continue;

            if (ElementalReactionConfig.steamClearAggro && entity instanceof Mob mob) {
                mob.setTarget(null);
            }

            if (cloud.getTags().contains(TAG_HIGH_HEAT)) {
                isHighHeat = true;
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
                float baseDamage = (float) ElementalReactionConfig.steamScaldingDamage;
                float scale = (float) ElementalReactionConfig.steamDamageScalePerLevel;
                float levelMultiplier = 1.0f + (cloudLevel * scale);
                float damage = baseDamage * levelMultiplier;

                ElementType type = ElementUtils.getElementType(entity);
                if (type == ElementType.FROST || type == ElementType.NATURE) {
                    double weaknessMult = ElementalReactionConfig.steamScaldingMultiplierWeakness;
                    damage *= (float) weaknessMult;
                }

                if (damage > 0) {
                    entity.invulnerableTime = 0;
                    entity.hurt(ModDamageTypes.source(entity.level(), ModDamageTypes.STEAM_SCALDING), damage);

                    if (entity.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.FLAME, entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(), 1, 0.2, 0.2, 0.2, 0.01);
                    }
                }

                if (entity.getPersistentData().getInt(WetnessHandler.NBT_WETNESS) > 0) {
                    removeWetness(entity);
                }
            }
        } else if (isCondensing) {
            int currentTimer = entity.getPersistentData().getInt(NBT_CONDENSATION_TIMER);
            currentTimer += 10;

            int delayThreshold = Math.max(10, ElementalReactionConfig.steamCondensationDelay);

            if (currentTimer >= delayThreshold) {
                int currentWet = entity.getPersistentData().getInt(WetnessHandler.NBT_WETNESS);
                int max = ElementalReactionConfig.wetnessMaxLevel;

                if (currentWet < max) {
                    entity.getPersistentData().putInt(WetnessHandler.NBT_WETNESS, currentWet + 1);
                    entity.playSound(SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 1.0f, 1.0f);
                }

                currentTimer = 0;
            }

            entity.getPersistentData().putInt(NBT_CONDENSATION_TIMER, currentTimer);
        }
    }

    // =================================================================================================
    //                                  Helper Methods / 辅助方法
    // =================================================================================================

    /**
     * 执行攻击者的自我干燥逻辑。
     * 消耗一定的伤害输出，移除自身的潮湿状态。
     *
     * Executes self-drying logic for the attacker.
     * Consumes a portion of damage output to remove own wetness.
     *
     * @param attacker       攻击者 / Attacker
     * @param event          受伤事件（用于减少伤害） / Hurt event (to reduce damage)
     * @param currentWetness 当前潮湿层数 / Current wetness level
     */
    private static void applySelfDrying(LivingEntity attacker, LivingHurtEvent event, int currentWetness) {
        attacker.addTag(TAG_SELF_DRYING_PENALTY);

        int fireEnhancement = ElementUtils.getDisplayEnhancement(attacker, ElementType.FIRE);
        int layersToRemove = Math.max(1, fireEnhancement / 20);

        int newLevel = Math.max(0, currentWetness - layersToRemove);

        if (newLevel == 0) {
            removeWetness(attacker);
        } else {
            attacker.getPersistentData().putInt(WetnessHandler.NBT_WETNESS, newLevel);
            attacker.removeEffect(com.xulai.elementalcraft.potion.ModMobEffects.WETNESS.get());
            attacker.addEffect(new MobEffectInstance(
                    com.xulai.elementalcraft.potion.ModMobEffects.WETNESS.get(),
                    ElementalReactionConfig.wetnessDecayBaseTime * 20,
                    newLevel - 1,
                    true,
                    true
            ));
        }

        int maxBurstLevel = ElementalReactionConfig.steamHighHeatMaxLevel;
        playSteamBurstEffect((ServerLevel) attacker.level(), attacker, 0.5f, Math.min(layersToRemove, maxBurstLevel), true);
        DebugCommand.sendDryLog(attacker, currentWetness, newLevel, layersToRemove, fireEnhancement);
    }

    /**
     * 移除实体的所有潮湿相关数据。
     *
     * Removes all wetness-related data from the entity.
     *
     * @param entity 实体 / Entity
     */
    private static void removeWetness(LivingEntity entity) {
        entity.getPersistentData().remove(WetnessHandler.NBT_WETNESS);
        entity.getPersistentData().remove(WetnessHandler.NBT_RAIN_TIMER);
        entity.getPersistentData().remove(WetnessHandler.NBT_DECAY_TIMER);
        entity.removeEffect(com.xulai.elementalcraft.potion.ModMobEffects.WETNESS.get());
    }

    /**
     * 检查实体是否免疫蒸汽伤害。
     *
     * Checks if the entity is immune to steam damage.
     *
     * @param entity 实体 / Entity
     * @return 是否免疫 / Whether immune
     */
    private static boolean checkImmunity(LivingEntity entity) {
        if (entity.fireImmune() || entity.hasEffect(MobEffects.FIRE_RESISTANCE)) return true;

        String id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        if (ElementalReactionConfig.cachedSteamBlacklist.contains(id)) return true;

        int resist = ElementUtils.getDisplayResistance(entity, ElementType.FIRE);
        int threshold = ElementalReactionConfig.steamImmunityThreshold;
        return resist >= threshold;
    }

    /**
     * 生成动态蒸汽云实体。
     *
     * Spawns a dynamic steam cloud entity.
     *
     * @param target     目标实体 / Target entity
     * @param isHighHeat 是否为高温蒸汽 / Whether it is high-heat steam
     * @param fuelLevel  燃料等级（通常为目标潮湿层数） / Fuel level (usually target wetness level)
     */
    private static void spawnSteamCloud(LivingEntity target, boolean isHighHeat, int fuelLevel) {
        if (!(target.level() instanceof ServerLevel serverLevel)) return;

        int maxLevel = isHighHeat ? ElementalReactionConfig.steamHighHeatMaxLevel : ElementalReactionConfig.steamLowHeatMaxLevel;
        int level = Math.max(1, Math.min(fuelLevel, maxLevel));

        float baseRadius = (float) ElementalReactionConfig.steamCloudRadius;
        float radiusInc = (float) ElementalReactionConfig.steamRadiusPerLevel;

        float radius = isHighHeat ? baseRadius + (level - 1.0f) * radiusInc : baseRadius;

        int baseDuration;
        int durationInc;

        if (isHighHeat) {
            baseDuration = ElementalReactionConfig.steamCloudDuration;
            durationInc = ElementalReactionConfig.steamDurationPerLevel;
        } else {
            baseDuration = ElementalReactionConfig.steamCondensationDurationBase;
            durationInc = ElementalReactionConfig.steamCondensationDurationPerLevel;
        }

        int duration = baseDuration + (level * durationInc);

        AreaEffectCloud cloud = new AreaEffectCloud(serverLevel, target.getX(), target.getY(), target.getZ());
        cloud.setRadius(radius);
        cloud.setRadiusOnUse(0F);
        cloud.setRadiusPerTick(0F);
        cloud.setDuration(duration);
        cloud.setParticle(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AIR.defaultBlockState()));

        cloud.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ElementalReactionConfig.steamBlindnessDuration));

        cloud.addTag(TAG_STEAM_CLOUD);
        if (isHighHeat) {
            cloud.addTag(TAG_HIGH_HEAT);
            cloud.addTag(TAG_LEVEL_PREFIX + level);
        }

        serverLevel.addFreshEntity(cloud);
        playSteamBurstEffect(serverLevel, target, radius, level, isHighHeat);
    }

    /**
     * 持续生成上升的蒸汽粒子。
     *
     * Continuously spawns rising steam particles.
     *
     * @param level 服务端世界 / Server Level
     * @param cloud 药水云实体 / Cloud entity
     */
    private static void spawnRisingSteamParticles(ServerLevel level, AreaEffectCloud cloud) {
        boolean isHighHeat = cloud.getTags().contains(TAG_HIGH_HEAT);
        float radius = cloud.getRadius();
        if (radius < 0.2f) return;

        Random random = new Random();
        int count = Math.max(1, (int) (radius * 0.8));

        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = Math.sqrt(random.nextDouble()) * radius;
            double x = cloud.getX() + Math.cos(angle) * dist;
            double z = cloud.getZ() + Math.sin(angle) * dist;
            double y = cloud.getY();

            double upSpeed = 0.05 + random.nextDouble() * 0.08;

            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0, 0, upSpeed, 0, 1.0);

            if (isHighHeat) {
                if (random.nextFloat() < 0.1f) {
                    level.sendParticles(ParticleTypes.FLAME, x, y, z, 0, 0, upSpeed * 0.8, 0, 0.5);
                }
                if (random.nextFloat() < 0.05f) {
                    level.sendParticles(ParticleTypes.LAVA, x, y, z, 0, 0, 0, 0, 0);
                }
            }
        }
    }

    /**
     * 播放一次性的蒸汽爆发特效（音效+粒子）。
     *
     * Plays a one-time steam burst effect (Sound + Particles).
     *
     * @param level       服务端世界 / Server Level
     * @param target      目标实体 / Target entity
     * @param radius      爆发半径 / Burst radius
     * @param intensity   强度等级 / Intensity level
     * @param isHighHeat 是否为高温 / Whether high heat
     */
    private static void playSteamBurstEffect(ServerLevel level, LivingEntity target, float radius, int intensity, boolean isHighHeat) {
        Random random = new Random();

        float volume = isHighHeat ? 0.8F : 0.6F;
        float pitch = isHighHeat ? 1.0F : 1.2F;

        if (isHighHeat && intensity >= 3) {
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, volume, 0.8F);
        } else {
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, volume, pitch);
        }

        int count = (int) (Math.max(1.0, radius) * (isHighHeat ? 20 : 10) * intensity);
        double speed = isHighHeat ? (0.05 + intensity * 0.02) : 0.05;

        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = Math.sqrt(random.nextDouble()) * radius;
            double x = target.getX() + Math.cos(angle) * dist;
            double z = target.getZ() + Math.sin(angle) * dist;
            double y = target.getY() + random.nextDouble() * target.getBbHeight() + 0.2;

            if (isHighHeat) {
                if (random.nextFloat() < 0.2f)
                    level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0, 0, 0.1, 0, speed);
                if (random.nextFloat() < 0.3f)
                    level.sendParticles(ParticleTypes.FLAME, x, y, z, 0, 0, 0.05, 0, speed * 0.5);
                if (random.nextFloat() < 0.1f) level.sendParticles(ParticleTypes.LAVA, x, y, z, 0, 0, 0, 0, 0);

                if (intensity >= 3 && random.nextFloat() < 0.1f) {
                    level.sendParticles(ParticleTypes.POOF, x, y, z, 0, 0, 0, 0, speed * 1.5);
                }
            } else {
                if (random.nextBoolean()) {
                    level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0, 0, 0.05, 0, speed * 0.5);
                }
            }
        }
    }
}