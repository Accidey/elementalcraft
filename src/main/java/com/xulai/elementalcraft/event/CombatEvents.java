package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.util.EffectHelper;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import com.xulai.elementalcraft.event.SteamReactionHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Objects;
import java.util.Random;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class CombatEvents {

    private static final String NBT_WETNESS = "EC_WetnessLevel";
    private static final String NBT_LAST_DRY_TICK = "EC_LastSelfDryTick"; 
    
    private static final Random RANDOM = new Random();

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;

        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        float currentDamage = event.getAmount();

        if (target.hasEffect(Objects.requireNonNull(ModMobEffects.SPORES.get()))) {
            MobEffectInstance effect = target.getEffect(ModMobEffects.SPORES.get());
            int stacks = (effect != null) ? (effect.getAmplifier() + 1) : 0;

            if (stacks > 0) {
                if (!source.is(DamageTypeTags.IS_FIRE) 
                        && !source.is(DamageTypes.MAGIC) 
                        && !source.is(DamageTypes.INDIRECT_MAGIC) 
                        && !source.is(DamageTypeTags.IS_EXPLOSION)
                        && !source.is(DamageTypes.WITHER)) { 
                    
                    float resistPerStack = (float) ElementalFireNatureReactionsConfig.sporePhysResist;
                    float totalResist = Math.min(0.9f, stacks * resistPerStack);
                    currentDamage *= (1.0f - totalResist);
                }
                
                event.setAmount(currentDamage);
            }
        }

        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            return;
        }

        ItemStack weaponStack = ItemStack.EMPTY;
        Entity directEntity = source.getDirectEntity();

        if (directEntity instanceof ThrownTrident trident) {
            try {
                CompoundTag tag = new CompoundTag();
                trident.addAdditionalSaveData(tag);
                if (tag.contains("Trident", 10)) {
                    weaponStack = ItemStack.of(tag.getCompound("Trident"));
                }
            } catch (Exception ignored) {}
        }

        if (weaponStack.isEmpty()) {
            ItemStack mainHand = attacker.getMainHandItem();
            ItemStack offHand = attacker.getOffhandItem();

            if (ElementUtils.getAttackElement(mainHand) != ElementType.NONE) {
                weaponStack = mainHand;
            } else if (ElementUtils.getAttackElement(offHand) != ElementType.NONE) {
                weaponStack = offHand;
            }
        }

        ElementType attackElement = ElementUtils.getAttackElement(weaponStack);
        if (attackElement == ElementType.NONE) {
            return;
        }

        // Thunder Strike trigger conditions - 现在由 StaticShockHandler.onLivingDamage 处理
        // 这里不再需要单独触发，因为 StaticShockHandler 会处理所有雷霆攻击

        if (attackElement == ElementType.FIRE && target.hasEffect(Objects.requireNonNull(ModMobEffects.SPORES.get()))) {
            MobEffectInstance spore = target.getEffect(ModMobEffects.SPORES.get());
            int stacks = (spore != null) ? (spore.getAmplifier() + 1) : 0;
            
            if (stacks > 0) {
                double vulnPerStack = ElementalFireNatureReactionsConfig.sporeFireVulnPerStack;
                float vulnMultiplier = 1.0f + (float)(stacks * vulnPerStack);
                currentDamage *= vulnMultiplier;
                event.setAmount(currentDamage);
            }
        }

        if (attackElement == ElementType.FIRE) {
            CompoundTag attackerData = attacker.getPersistentData();
            int attackerWetness = attackerData.getInt(NBT_WETNESS);
            
            if (attackerWetness > 0) {
                long currentTick = attacker.level().getGameTime();
                long lastDryTick = attackerData.getLong(NBT_LAST_DRY_TICK);
                
                if (currentTick != lastDryTick) {
                    int firePower = ElementUtils.getDisplayEnhancement(attacker, ElementType.FIRE);
                    int threshold = Math.max(1, ElementalFireNatureReactionsConfig.wetnessDryingThreshold);
                    
                    int layersToRemove = firePower / threshold;
                    
                    if (layersToRemove > 0) {
                        int newLevel = Math.max(0, attackerWetness - layersToRemove);
                        int actuallyRemoved = attackerWetness - newLevel;
                        
                        attackerData.putInt(NBT_WETNESS, newLevel);
                        attackerData.putLong(NBT_LAST_DRY_TICK, currentTick);
                        
                        if (newLevel == 0 && attacker.hasEffect(ModMobEffects.WETNESS.get())) {
                            attacker.removeEffect(ModMobEffects.WETNESS.get());
                        }
                        
                        int maxBurstLevel = ElementalFireNatureReactionsConfig.steamHighHeatMaxLevel;
                        EffectHelper.playSteamBurst((ServerLevel) attacker.level(), attacker, 0.5f, Math.min(layersToRemove, maxBurstLevel), true);

                        attacker.addTag(SteamReactionHandler.TAG_SELF_DRYING_PENALTY);
                        
                        DebugCommand.sendDryLog(attacker, attackerWetness, newLevel, actuallyRemoved, firePower);
                    }
                } else {
                    attacker.addTag(SteamReactionHandler.TAG_SELF_DRYING_PENALTY);
                }
            }
        }

        float physicalDamage = currentDamage;
        int enhancementPoints = ElementUtils.getDisplayEnhancement(attacker, attackElement);
        int resistancePoints = ElementUtils.getDisplayResistance(target, attackElement);

        int strengthPerHalfDamage = ElementalConfig.getStrengthPerHalfDamage();
        int resistPerHalfReduction = ElementalConfig.getResistPerHalfReduction();

        float rawElementalDamage = enhancementPoints / (float) strengthPerHalfDamage * 0.5f;
        float rawResistReduction = resistancePoints / (float) resistPerHalfReduction * 0.5f;

        if (rawElementalDamage <= 0.0f) {
            return;
        }

        int wetnessLevel = target.getPersistentData().getInt(NBT_WETNESS);
        
        if (wetnessLevel <= 0) {
            for (String tag : target.getTags()) {
                if (tag.startsWith("EC_WetnessSnapshot_")) {
                    try {
                        wetnessLevel = Integer.parseInt(tag.substring(19)); 
                        target.removeTag(tag); 
                    } catch (NumberFormatException ignored) {}
                    break;
                }
            }
        }

        float wetnessMultiplier = 1.0f;
        float maxCap = (float) ElementalFireNatureReactionsConfig.steamMaxReduction;

        if (wetnessLevel > 0) {
            if (attackElement == ElementType.FIRE) {
                float reductionPerLevel = (float) ElementalFireNatureReactionsConfig.wetnessFireReduction;
                float finalReduction = Math.min(wetnessLevel * reductionPerLevel, maxCap);
                wetnessMultiplier = 1.0f - finalReduction;
            }
        }

        if (attacker.getTags().contains(SteamReactionHandler.TAG_SELF_DRYING_PENALTY) 
                && attackElement == ElementType.FIRE) {
            float penalty = 1.0f - (float) ElementalFireNatureReactionsConfig.wetnessSelfDryingDamagePenalty;
            wetnessMultiplier *= penalty;
            
            attacker.removeTag(SteamReactionHandler.TAG_SELF_DRYING_PENALTY);
        }

        ElementType targetDominant = ElementType.NONE;
        ItemStack targetWeapon = target.getMainHandItem();
        ElementType targetWeaponElement = ElementUtils.getAttackElement(targetWeapon);
        
        if (targetWeaponElement == ElementType.NONE) {
             targetWeapon = target.getOffhandItem();
             targetWeaponElement = ElementUtils.getAttackElement(targetWeapon);
        }

        if (targetWeaponElement != ElementType.NONE) {
            int targetEnhancement = ElementUtils.getDisplayEnhancement(target, targetWeaponElement);
            if (targetEnhancement > 0) {
                targetDominant = targetWeaponElement;
            }
        }

        float restraintMultiplier = ElementalConfig.getRestraintMultiplier(attackElement, targetDominant);

        float attackPart = rawElementalDamage
                * (float) ElementalConfig.elementalDamageMultiplier
                * wetnessMultiplier
                * restraintMultiplier;

        float finalElementalDmg;
        boolean isFloored = false;
        double minPercent = ElementalConfig.restraintMinDamagePercent;

        float benchmark = (float) ElementalConfig.getMaxStatCap();
        if (restraintMultiplier > 1.0f && resistancePoints >= benchmark) {
            float resistRatio = Math.min(resistancePoints / benchmark, 1.0f);
            double maxReductionAllowed = 1.0 - minPercent;
            double actualReduction = resistRatio * maxReductionAllowed;
            
            finalElementalDmg = attackPart * (float) (1.0 - actualReduction);
            isFloored = true; 
        } else {
            float defensePart = rawResistReduction * (float) ElementalConfig.elementalResistanceMultiplier;
            finalElementalDmg = Math.max(0.0f, attackPart - defensePart);
        }

        float totalDamage = physicalDamage + finalElementalDmg;
        event.setAmount(totalDamage);

        DebugCommand.sendCombatLog(
                attacker, target, directEntity,
                physicalDamage,
                rawElementalDamage,
                rawResistReduction,
                ElementalConfig.elementalDamageMultiplier,
                ElementalConfig.elementalResistanceMultiplier,
                restraintMultiplier,
                wetnessMultiplier,
                finalElementalDmg,
                totalDamage,
                isFloored,
                minPercent,
                wetnessLevel
        );

        if (attackElement == ElementType.FIRE) {
            tryTriggerScorched(attacker, target, enhancementPoints);
        }
    }


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide) return;
        
        LivingEntity target = event.getEntity();
        // 检查目标是否有麻痹效果
        if (target.hasEffect(ModMobEffects.PARALYSIS.get())) {
            event.setCanceled(true);
        }
    }

    private static void tryTriggerScorched(LivingEntity attacker, LivingEntity target, int firePower) {
        if (firePower < ElementalFireNatureReactionsConfig.scorchedTriggerThreshold) return;

        if (target.hasEffect(Objects.requireNonNull(ModMobEffects.WETNESS.get())) || 
            attacker.hasEffect(Objects.requireNonNull(ModMobEffects.WETNESS.get()))) {
            return; 
        }

        if (ElementUtils.getDisplayResistance(target, ElementType.FROST) > 0 || 
            ElementUtils.getDisplayEnhancement(target, ElementType.FROST) > 0) {
            return;
        }

        if (target.getPersistentData().contains(ScorchedHandler.NBT_SCORCHED_TICKS)) {
            return;
        }

        double baseChance = ElementalFireNatureReactionsConfig.scorchedBaseChance;
        double growth = firePower * ElementalFireNatureReactionsConfig.scorchedChancePerPoint;
        double totalChance = Math.min(1.0, baseChance + growth);

        if (RANDOM.nextDouble() < totalChance) {
            int duration = ElementalFireNatureReactionsConfig.scorchedDuration;
            ScorchedHandler.applyScorched(target, firePower, duration);

            target.level().playSound(null, target.getX(), target.getY(), target.getZ(), 
                    SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
    }
}