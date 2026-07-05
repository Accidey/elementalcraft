package com.xulai.elementalcraft.event.iss;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.config.ElementalISSIntegrationConfig;
import com.xulai.elementalcraft.enchantment.ModEnchantments;
import com.xulai.elementalcraft.event.ReactionHandler;
import com.xulai.elementalcraft.event.ScorchedHandler;
import com.xulai.elementalcraft.logic.MobAttributeLogic;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class NatureSpellHandler {

    private static final net.minecraft.world.effect.MobEffect REND_EFFECT =
            ForgeRegistries.MOB_EFFECTS.getValue(new net.minecraft.resources.ResourceLocation("irons_spellbooks", "rend"));

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onNatureSpellHit(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!ISSCore.ISS_LOADED) return;
        if (!isNatureSpellDamage(event.getSource())) return;
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

    @SubscribeEvent
    public static void onLivingTickCheckRoot(LivingEvent.LivingTickEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!ISSCore.ISS_LOADED) return;

        LivingEntity target = event.getEntity();
        Entity vehicle = target.getVehicle();
        if (vehicle == null || !isRootEntity(vehicle)) return;

        if (target.getPersistentData().getBoolean("EC_FleeActive")) {
            MobAttributeLogic.stopFlee(target);
        }

        CompoundTag data = target.getPersistentData();
        String NBT_ROOT_SPORE_APPLIED = "ec_root_spore_applied";

        UUID currentRootUUID = vehicle.getUUID();
        if (!data.hasUUID(NBT_ROOT_SPORE_APPLIED) || !data.getUUID(NBT_ROOT_SPORE_APPLIED).equals(currentRootUUID)) {
            int perStack = (int) ElementalFireNatureReactionsConfig.natureParasiteBaseThreshold;
            if (perStack > 0) {
                LivingEntity owner = getRootOwner(vehicle);
                if (owner != null) {
                    int natureEnhancement = ElementUtils.getDisplayEnhancement(owner, ElementType.NATURE);
                    int amount = natureEnhancement / perStack;
                    if (amount > 0) {
                        ElementalCraft.LOGGER.info("[EC ROOT DEBUG] Tick applying {} spores to {}", amount, target.getName().getString());
                        ReactionHandler.stackSporeEffect(target, amount, null);
                    }
                }
            }
            data.putUUID(NBT_ROOT_SPORE_APPLIED, currentRootUUID);
        }

        if (!ScorchedHandler.isScorched(target)) return;

        ElementalCraft.LOGGER.info("[EC ROOT DEBUG] Tick detected scorched ROOT target, removing ROOT!");

        vehicle.ejectPassengers();
        vehicle.discard();

        if (data.contains(ScorchedHandler.NBT_SCORCHED_TICKS)) {
            int currentTicks = data.getInt(ScorchedHandler.NBT_SCORCHED_TICKS);
            int enhancedTicks = (int) (currentTicks * ElementalFireNatureReactionsConfig.poisonScorchDurationMultiplier);
            data.putInt(ScorchedHandler.NBT_SCORCHED_TICKS, enhancedTicks);

            float enhancedDmgMult = (float) ElementalFireNatureReactionsConfig.poisonScorchDamageMultiplier;
            data.putFloat(ScorchedHandler.NBT_SCORCHED_DAMAGE_MULT, enhancedDmgMult);
        }
    }

    static void handleReaction(LivingEntity target, LivingEntity attacker, DamageSource source, int spellStacks) {
        String spellId = attacker.getPersistentData().getString("EC_ISS_SpellId");
        if (spellId.isEmpty()) spellId = attacker.getPersistentData().getString("EC_LastSpellId");
        if (ISSCore.NATURE_NO_SPORE_SPELLS.contains(spellId)) return;

        int naturePower = ElementUtils.getDisplayEnhancement(attacker, ElementType.NATURE);
        double threshold = ElementalFireNatureReactionsConfig.natureParasiteBaseThreshold;
        if (threshold <= 0 || naturePower < threshold) return;
        double baseChance = ElementalFireNatureReactionsConfig.natureParasiteBaseChance;
        double scalingStep = ElementalFireNatureReactionsConfig.natureParasiteScalingStep;
        double scalingChance = ElementalFireNatureReactionsConfig.natureParasiteScalingChance;
        int scalingSteps = (scalingStep > 0 && naturePower >= scalingStep)
                ? (int) ((naturePower - scalingStep) / scalingStep) : 0;
        double chance = Math.min(1.0, baseChance + scalingSteps * scalingChance);
        if (ISSCore.RANDOM.nextDouble() < chance) {
            int step = (int) Math.max(1, ElementalFireNatureReactionsConfig.natureParasiteScalingStep);
            int eStacks = 1 + (naturePower - (int) threshold) / step;
            int amount = spellStacks > 0 ? Math.min(spellStacks, eStacks) : eStacks;
            ReactionHandler.stackSporeEffect(target, amount, attacker);
            com.xulai.elementalcraft.command.DebugCommand.sendNatureParasiteSuccess(attacker, target, amount,
                    ElementType.NATURE, naturePower, baseChance, scalingSteps, scalingChance, 0, 0, 0, chance);
        } else {
            com.xulai.elementalcraft.command.DebugCommand.sendReactionFailed(attacker, "nature_parasite", "chance",
                    attacker.getDisplayName(), target.getDisplayName(),
                    "",
                    Component.literal(String.format("%.0f", chance * 100)).withStyle(ChatFormatting.YELLOW));
        }
    }

    @SubscribeEvent
    public static void onAcidOrbImpact(ProjectileImpactEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!ISSCore.ISS_LOADED) return;
        Entity projectile = event.getEntity();
        if (!projectile.getClass().getName().equals("io.redspace.ironsspellbooks.entity.spells.acid_orb.AcidOrb")) return;
        if (!(event.getRayTraceResult() instanceof EntityHitResult entityHit)) return;
        if (!(entityHit.getEntity() instanceof LivingEntity target)) return;
        if (!(projectile instanceof Projectile proj)) return;
        if (!(proj.getOwner() instanceof LivingEntity attacker)) return;

        int spellStacks = ISSCore.tryGetStacksFromItem(attacker);
        handleReaction(target, attacker, null, spellStacks);
        for (LivingEntity nearby : target.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(3.5))) {
            if (nearby == target || nearby == attacker) continue;
            handleReaction(nearby, attacker, null, spellStacks);
        }
    }

    static void onMobTick(Mob mob, CompoundTag data) {
        long gameTime = mob.level().getGameTime();
        String spellId = data.getString("EC_ISS_SpellId");

        long pendingCast = data.getLong("EC_ISS_PendingCast");
        if (pendingCast > 0 && gameTime >= pendingCast) {
            data.remove("EC_ISS_PendingCast");
            int missCount = data.getInt("EC_ISS_MissCount");
            boolean aggressive = mob.getHealth() / mob.getMaxHealth() < ElementalISSIntegrationConfig.mobLowHealthThreshold;
            int cooldown = aggressive ? ElementalISSIntegrationConfig.mobAggressiveCastCooldown : ElementalISSIntegrationConfig.mobNormalCastCooldown;
            if (missCount >= ElementalISSIntegrationConfig.mobMaxMissCount) {
                data.remove("EC_ISS_MissCount");
                cooldown = ElementalISSIntegrationConfig.mobNormalCastCooldown;
            }
            data.putLong(ISSCore.NBT_MOB_CAST_CD, gameTime + cooldown);
            return;
        }
        if (pendingCast > 0) return;

        if ("irons_spellbooks:acid_orb".equals(spellId)) {
            LivingEntity target = mob.getTarget();
            if (target == null || !target.isAlive()) return;
            int phase = data.getInt("EC_ISS_AcidPhase");
            if (phase == 0) {
                if (REND_EFFECT != null && target.hasEffect(REND_EFFECT)) return;
                ISSCore.castSpell(mob, target, false);
                data.putLong("EC_ISS_PendingCast", gameTime + 40);
                data.putInt("EC_ISS_AcidPhase", 1);
                return;
            }
            if (phase == 1) {
                if (gameTime < data.getLong("EC_ISS_PendingCast")) return;
                if (REND_EFFECT != null && target.hasEffect(REND_EFFECT)) {
                    data.putInt("EC_ISS_AcidPhase", 3);
                    data.remove("EC_ISS_AcidMisses");
                } else {
                    int misses = data.getInt("EC_ISS_AcidMisses") + 1;
                    if (misses >= 2) {
                        data.putInt("EC_ISS_AcidPhase", 2);
                        data.putLong(ISSCore.NBT_MOB_CAST_CD, gameTime + ElementalISSIntegrationConfig.mobNormalCastCooldown);
                        data.remove("EC_ISS_AcidMisses");
                    } else {
                        data.putInt("EC_ISS_AcidMisses", misses);
                        data.putInt("EC_ISS_AcidPhase", 0);
                    }
                }
                return;
            }
            if (phase == 2) {
                if (gameTime >= data.getLong(ISSCore.NBT_MOB_CAST_CD)) {
                    data.putInt("EC_ISS_AcidPhase", 0);
                }
                return;
            }
            if (phase == 3) {
                if (REND_EFFECT == null || !target.hasEffect(REND_EFFECT)) {
                    data.putInt("EC_ISS_AcidPhase", 0);
                }
                return;
            }
            return;
        }

        if (mob.getHealth() / mob.getMaxHealth() < ElementalISSIntegrationConfig.mobLowHealthThreshold) {
            ISSCore.tryAggressiveCast(mob, data, gameTime);
            return;
        }

        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return;

        if (ISSCore.isImmuneToParalysis(target)) {
            if (gameTime >= data.getLong(ISSCore.NBT_MOB_CAST_CD)) {
                ISSCore.castSpell(mob, target, false);
                data.putLong(ISSCore.NBT_MOB_CAST_CD, gameTime + ElementalISSIntegrationConfig.mobNormalCastCooldown);
            }
            return;
        }

        if (gameTime >= data.getLong(ISSCore.NBT_MOB_CAST_CD)) {
            ISSCore.castSpell(mob, target, false);
            data.putLong("EC_ISS_PendingCast", gameTime + ElementalISSIntegrationConfig.mobWaterBottleInterval);
            data.putLong(ISSCore.NBT_MOB_CAST_CD, gameTime + ElementalISSIntegrationConfig.mobNormalCastCooldown);
            data.putInt("EC_ISS_MissCount", 0);
        }
    }

    static void equipSlots(Mob mob, ItemStack scrollStack, String spellId) {
        mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        mob.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        if ("irons_spellbooks:acid_orb".equals(spellId)) {
            mob.setItemSlot(EquipmentSlot.OFFHAND, scrollStack);
            mob.setDropChance(EquipmentSlot.OFFHAND, ElementalISSIntegrationConfig.scrollDropChance);
            scrollStack.enchant(ModEnchantments.NATURE_STRIKE.get(), 1);
            ItemStack weapon = com.xulai.elementalcraft.util.AttributeEquipUtils.createRandomWeapon();
            mob.setItemSlot(EquipmentSlot.MAINHAND, weapon);
            mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        } else {
            mob.setItemSlot(EquipmentSlot.MAINHAND, scrollStack);
            mob.setDropChance(EquipmentSlot.MAINHAND, ElementalISSIntegrationConfig.scrollDropChance);
            scrollStack.enchant(ModEnchantments.NATURE_STRIKE.get(), 1);
        }
    }

    static boolean isNatureSpellDamage(DamageSource source) {
        if (!ISSCore.ISS_LOADED) return false;
        return source.is(ISSCore.ISS_NATURE_MAGIC);
    }

    private static boolean isRootEntity(Entity entity) {
        if (entity == null) return false;
        net.minecraft.resources.ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return key != null && "irons_spellbooks".equals(key.getNamespace()) && "root".equals(key.getPath());
    }

    private static LivingEntity getRootOwner(Entity rootEntity) {
        try {
            return (LivingEntity) rootEntity.getClass().getMethod("getOwner").invoke(rootEntity);
        } catch (Exception e) {
            return null;
        }
    }
}
