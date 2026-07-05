package com.xulai.elementalcraft.event.iss;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.config.ElementalISSIntegrationConfig;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.enchantment.ModEnchantments;
import com.xulai.elementalcraft.event.CombatEvents;
import com.xulai.elementalcraft.event.FrostbiteHandler;
import com.xulai.elementalcraft.event.ReactionHandler;
import com.xulai.elementalcraft.event.ScorchedHandler;
import com.xulai.elementalcraft.event.SteamReactionHandler;
import com.xulai.elementalcraft.event.WetnessHandler;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class FireSpellHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onFireSpellHit(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!ISSCore.ISS_LOADED) return;
        if (!isFireSpellDamage(event.getSource())) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;

        if (attacker instanceof Mob mob && mob.getPersistentData().getBoolean(ISSCore.NBT_MOB_CASTER)) {
            CompoundTag mobData = mob.getPersistentData();
            mobData.remove("EC_ISS_MissCount");
            mobData.remove("EC_ISS_PendingCast");
            int cd = mob.getHealth() / mob.getMaxHealth() < ElementalISSIntegrationConfig.mobLowHealthThreshold
                    ? ElementalISSIntegrationConfig.mobAggressiveCastCooldown
                    : ElementalISSIntegrationConfig.mobNormalCastCooldown;
            mobData.putLong(ISSCore.NBT_MOB_CAST_CD, mob.level().getGameTime() + cd);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlazeStormHit(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!ISSCore.ISS_LOADED) return;

        Entity direct = event.getSource().getDirectEntity();
        if (direct == null) return;
        if (!direct.getClass().getName().equals("io.redspace.ironsspellbooks.entity.spells.fireball.SmallMagicFireball")) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;

        int firePower = ElementUtils.getDisplayEnhancement(attacker, ElementType.FIRE);
        CompoundTag data = attacker.getPersistentData();

        if (data.getBoolean("EC_BlazeStormScorched")) return;

        if (data.contains("EC_BlazeStormCdScheduled")) {
            long scheduled = data.getLong("EC_BlazeStormCdScheduled");
            if (event.getEntity().level().getGameTime() >= scheduled) {
                int cd = ElementalFireNatureReactionsConfig.scorchedCooldown;
                data.putLong(ScorchedHandler.NBT_ATTACKER_SCORCHED_COOLDOWN,
                        event.getEntity().level().getGameTime() + cd);
                data.remove("EC_BlazeStormScorched");
                data.remove("EC_BlazeStormCdScheduled");
            }
        }

        if (tryReplaceIgnitionWithScorch(event.getEntity(), attacker, firePower, true)) {
            data.putBoolean("EC_BlazeStormScorched", true);

            int spellLevel = 1;
            if (data.contains("EC_ISS_SpellLevel")) {
                spellLevel = data.getInt("EC_ISS_SpellLevel");
            } else if (data.contains("EC_LastSpellLevel")) {
                spellLevel = data.getInt("EC_LastSpellLevel");
            }
            int castTicks = 55 + 5 * spellLevel;
            data.putLong("EC_BlazeStormCdScheduled",
                    event.getEntity().level().getGameTime() + castTicks);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFireFieldHit(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!ISSCore.ISS_LOADED) return;

        Entity direct = event.getSource().getDirectEntity();
        if (direct == null) return;
        String cls = direct.getClass().getName();
        if (!cls.equals("io.redspace.ironsspellbooks.entity.spells.magma_ball.FireField")
                && !cls.equals("io.redspace.ironsspellbooks.entity.spells.wall_of_fire.WallOfFireEntity")) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;

        event.getEntity().getPersistentData().putUUID("EC_FireFieldOwner", attacker.getUUID());
        event.getEntity().getPersistentData().putBoolean("EC_FireFieldHit", true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAfterFireFieldTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        if (!ISSCore.ISS_LOADED) return;

        CompoundTag data = entity.getPersistentData();
        if (!data.getBoolean("EC_FireFieldHit")) return;

        data.remove("EC_FireFieldHit");

        UUID ownerUUID = data.getUUID("EC_FireFieldOwner");
        data.remove("EC_FireFieldOwner");

        if (entity.level() instanceof ServerLevel sl) {
            Entity ownerEntity = sl.getEntity(ownerUUID);
            if (ownerEntity instanceof LivingEntity attacker) {
                entity.setRemainingFireTicks(0);
                int firePower = ElementUtils.getDisplayEnhancement(attacker, ElementType.FIRE);

                if (FrostbiteHandler.isFrozen(entity)
                        && ElementalThunderFrostReactionsConfig.frostbiteFireSteamThreshold > 0
                        && firePower >= ElementalThunderFrostReactionsConfig.frostbiteFireSteamThreshold) {
                    CombatEvents.applyFireFreezeMelt(entity, attacker, firePower);
                    data.putBoolean("EC_FireFieldJustMelted", true);
                    return;
                }

                if (data.getBoolean("EC_FireFieldJustMelted")) {
                    data.remove("EC_FireFieldJustMelted");
                } else {
                    net.minecraft.world.effect.MobEffect wetnessEffect = ModMobEffects.WETNESS.get();
                    if (wetnessEffect != null && entity.hasEffect(wetnessEffect)) {
                        SteamReactionHandler.spawnSteamCloud(entity, true, WetnessHandler.getWetnessLevel(entity));
                        WetnessHandler.clearWetnessData(entity);
                        return;
                    }
                }

                int threshold = ElementalFireNatureReactionsConfig.scorchedTriggerThreshold;
                if (threshold > 0 && firePower >= threshold) {
                    int duration = ElementalFireNatureReactionsConfig.scorchedDuration;
                    ScorchedHandler.applyScorched(entity, attacker, firePower, duration, firePower, 1.0f, true);
                }
            }
        }
    }

    static void handleReaction(LivingEntity target, LivingEntity attacker, DamageSource source, int spellStacks) {
        if (target.level().isClientSide) return;
        if (!ISSCore.ISS_LOADED) return;

        int firePower = ElementUtils.getDisplayEnhancement(attacker, ElementType.FIRE);

        if (ElementalThunderFrostReactionsConfig.frostbiteFireSteamThreshold > 0
                && firePower >= ElementalThunderFrostReactionsConfig.frostbiteFireSteamThreshold
                && FrostbiteHandler.isFrozen(target)) {
            CombatEvents.applyFireFreezeMelt(target, attacker, firePower);
            return;
        }

        net.minecraft.world.effect.MobEffect wetnessEffect = ModMobEffects.WETNESS.get();
        if (wetnessEffect != null && target.hasEffect(wetnessEffect)) {
            if (target.level() instanceof ServerLevel sl) {
                SteamReactionHandler.spawnSteamCloud(target, true, WetnessHandler.getWetnessLevel(target));
                WetnessHandler.clearWetnessData(target);
            }
            return;
        }

        int threshold = ElementalFireNatureReactionsConfig.scorchedTriggerThreshold;
        if (threshold <= 0 || firePower < threshold) {
            DebugCommand.sendReactionFailed(target, "scorched", "power_low",
                    attacker.getDisplayName(), target.getDisplayName(),
                    Component.literal(String.valueOf(firePower)).withStyle(ChatFormatting.RED),
                    Component.literal(String.valueOf(threshold)).withStyle(ChatFormatting.GOLD));
            return;
        }

        net.minecraft.world.effect.MobEffect sporeEffect = ModMobEffects.SPORES.get();
        boolean hasSpores = sporeEffect != null && target.hasEffect(sporeEffect);

        if (hasSpores
                && ElementUtils.getConsistentAttackElement(target) == ElementType.NATURE
                && ScorchedHandler.isScorched(target)
                && ReactionHandler.checkCooldown(target, ReactionHandler.NBT_WILDFIRE_COOLDOWN)
                && ElementalFireNatureReactionsConfig.wildfireTriggerThreshold > 0
                && ElementUtils.getDisplayEnhancement(target, ElementType.NATURE) >= ElementalFireNatureReactionsConfig.wildfireTriggerThreshold) {
            ReactionHandler.triggerWildfireEjection(target, attacker);
            return;
        }

        if (ScorchedHandler.isScorched(target)) {
            DebugCommand.sendReactionFailed(target, "scorched", "already",
                    attacker.getDisplayName(), target.getDisplayName());
            return;
        }

        boolean hasPoison = target.hasEffect(net.minecraft.world.effect.MobEffects.POISON);

        if (!hasSpores) {
            CompoundTag attackerData = attacker.getPersistentData();
            long gameTime = target.level().getGameTime();
            if (attackerData.contains(ScorchedHandler.NBT_ATTACKER_SCORCHED_COOLDOWN)) {
                long cd = attackerData.getLong(ScorchedHandler.NBT_ATTACKER_SCORCHED_COOLDOWN);
                if (gameTime < cd) {
                    DebugCommand.sendReactionCooldownBlock(attacker, "scorched_attack", cd - gameTime);
                    return;
                }
            }
        }

        double baseChance = ElementalFireNatureReactionsConfig.scorchedBaseChance;
        int pointsPerStep = ElementalFireNatureReactionsConfig.scorchedChancePerPoint;
        double growth = Math.floor((firePower - threshold) / (double) pointsPerStep) * 0.05;
        double totalChance = Math.min(1.0, Math.max(0.0, baseChance + growth));
        boolean triggered;
        if (hasPoison || hasSpores) {
            totalChance = 1.0;
            triggered = true;
        } else {
            triggered = ISSCore.RANDOM.nextDouble() < totalChance;
        }

        if (triggered) {
            int duration = ElementalFireNatureReactionsConfig.scorchedDuration;
            ScorchedHandler.ScorchedApplyResult result = ScorchedHandler.applyScorched(
                    target, attacker, firePower, duration, firePower, 1.0f, false);
            if (result != ScorchedHandler.ScorchedApplyResult.FAILED) {
                ScorchedHandler.igniteCreeperIfScorched(target);
            }

            target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 0.8f);

            String durationInfo;
            double durationSec = duration / 20.0;
            double adjustedSec = result.adjustedDuration / 20.0;
            if (result.targetElement != ElementType.NONE && result.multiplier != 1.0f) {
                durationInfo = Component.translatable("debug.elementalcraft.reaction.scorched.duration_element_enhanced",
                        String.format("%.1f", durationSec), String.format("%.1f", adjustedSec),
                        result.targetElement.getDisplayName(), String.format("%.1f", result.multiplier)).getString();
            } else {
                durationInfo = Component.translatable("debug.elementalcraft.reaction.scorched.duration_seconds",
                        String.format("%.1f", adjustedSec)).getString();
            }

            String chanceInfo;
            if (hasPoison && hasSpores) {
                chanceInfo = String.format("%.0f%%", totalChance * 100)
                        + "(" + Component.translatable("effect.minecraft.poison").getString()
                        + "+" + Component.translatable("debug.elementalcraft.reaction.scorched.spore_label").getString() + ")";
            } else if (hasPoison) {
                chanceInfo = String.format("%.0f%%", totalChance * 100)
                        + "(" + Component.translatable("effect.minecraft.poison").getString() + ")";
            } else if (hasSpores) {
                chanceInfo = String.format("%.0f%%", totalChance * 100)
                        + "(" + Component.translatable("debug.elementalcraft.reaction.scorched.spore_label").getString() + ")";
            } else {
                chanceInfo = String.format("%.0f%%", totalChance * 100);
            }

            String poisonDurationInfo = null;
            if (hasPoison) {
                double enhancedSec = (int) (duration * ElementalFireNatureReactionsConfig.poisonScorchDurationMultiplier) / 20.0;
                poisonDurationInfo = Component.translatable("debug.elementalcraft.reaction.scorched.duration_poison_enhanced",
                        String.format("%.1f", durationSec), String.format("%.1f", enhancedSec)).getString()
                        + "(" + Component.translatable("effect.minecraft.poison").getString() + ")";
            }

            float baseDamage = ScorchedHandler.calculateScorchedDamage(firePower, target);
            DebugCommand.sendReactionSuccess(target, "scorched",
                    attacker.getDisplayName(),
                    target.getDisplayName(),
                    Component.literal(String.valueOf(firePower)).withStyle(ChatFormatting.RED),
                    chanceInfo,
                    poisonDurationInfo != null ? poisonDurationInfo : durationInfo,
                    String.format("%.1f", baseDamage));
        } else if (totalChance > 0.01) {
            DebugCommand.sendReactionFailed(target, "scorched", "chance",
                    attacker.getDisplayName(), target.getDisplayName(),
                    String.format("%.0f", totalChance * 100));
        }
    }

    static boolean tryReplaceIgnitionWithScorch(LivingEntity target, LivingEntity attacker, int firePower, boolean bypassCooldown) {
        int threshold = ElementalFireNatureReactionsConfig.scorchedTriggerThreshold;
        if (threshold <= 0 || firePower < threshold) return false;

        double baseChance = ElementalFireNatureReactionsConfig.scorchedBaseChance;
        int pointsPerStep = ElementalFireNatureReactionsConfig.scorchedChancePerPoint;
        double growth = Math.floor((firePower - threshold) / (double) pointsPerStep) * 0.05;
        double chance = Math.min(1.0, Math.max(0.0, baseChance + growth));

        if (ISSCore.RANDOM.nextDouble() >= chance) return false;

        target.setRemainingFireTicks(0);
        int duration = ElementalFireNatureReactionsConfig.scorchedDuration;
        ScorchedHandler.applyScorched(target, attacker, firePower, duration, firePower, 1.0f, bypassCooldown);
        return true;
    }

    static void onMobTick(Mob mob, CompoundTag data) {
        long gameTime = mob.level().getGameTime();

        if (mob.getHealth() / mob.getMaxHealth() < ElementalISSIntegrationConfig.mobLowHealthThreshold) {
            ISSCore.tryAggressiveCast(mob, data, gameTime);
            return;
        }

        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return;

        if (!ISSCore.tryThrowPoisonBottle(mob, data, target, gameTime)) return;

        if (gameTime >= data.getLong(ISSCore.NBT_MOB_CAST_CD)) {
            ISSCore.castSpell(mob, target, false);
            data.putLong(ISSCore.NBT_MOB_CAST_CD, gameTime + ElementalISSIntegrationConfig.mobNormalCastCooldown);
        }
    }

    static void equipSlots(Mob mob, ItemStack scrollStack) {
        mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        mob.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        mob.setItemSlot(EquipmentSlot.MAINHAND, scrollStack);
        mob.setDropChance(EquipmentSlot.MAINHAND, ElementalISSIntegrationConfig.scrollDropChance);
        scrollStack.enchant(ModEnchantments.FIRE_STRIKE.get(), 1);

        ItemStack waterBottle = new ItemStack(Items.SPLASH_POTION);
        PotionUtils.setPotion(waterBottle, Potions.WATER);
        mob.setItemSlot(EquipmentSlot.OFFHAND, waterBottle);
        mob.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
    }

    static boolean isFireSpellDamage(DamageSource source) {
        if (!ISSCore.ISS_LOADED) return false;
        return source.is(ISSCore.ISS_FIRE_MAGIC);
    }
}
