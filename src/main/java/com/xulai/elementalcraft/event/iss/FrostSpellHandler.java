package com.xulai.elementalcraft.event.iss;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.config.ElementalISSIntegrationConfig;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.enchantment.ModEnchantments;
import com.xulai.elementalcraft.event.FrostbiteHandler;
import com.xulai.elementalcraft.event.WetnessHandler;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.util.AttributeEquipUtils;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class FrostSpellHandler {

    private static net.minecraft.world.effect.MobEffect chilledEffect() {
        return ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("irons_spellbooks", "chilled"));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIceSpellHit(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!ISSCore.ISS_LOADED) return;
        if (!isIceSpellDamage(event.getSource())) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;

        if (attacker instanceof Mob mob && mob.getPersistentData().getBoolean(ISSCore.NBT_MOB_CASTER)) {
            mob.getPersistentData().putBoolean("EC_ISS_SpellHit", true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onIceSpellRefreshFreeze(LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!ISSCore.ISS_LOADED) return;
        if (!isIceSpellDamage(event.getSource())) return;
        Entity directEntity = event.getSource().getDirectEntity();
        if (directEntity != null && directEntity.getClass().getName()
                .equals("io.redspace.ironsspellbooks.entity.mobs.SummonedPolarBear")) return;
        LivingEntity target = event.getEntity();
        if (!FrostbiteHandler.isFrozen(target)) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        if ("SCROLL".equals(attacker.getPersistentData().getString("EC_LastCastSource"))
                && attacker.getPersistentData().getLong(ISSCore.NBT_ISS_REFRESH_CD) > target.level().getGameTime()) return;

        int frostPower = ElementUtils.getDisplayEnhancement(attacker, ElementType.FROST);
        double threshold = ElementalThunderFrostReactionsConfig.frostStrengthThreshold;
        if (threshold <= 0 || frostPower < threshold) return;

        CompoundTag data = target.getPersistentData();
        int freezeStacks = Math.max(1, data.getInt(FrostbiteHandler.NBT_FREEZE_STACKS));
        int freezeDuration = freezeStacks * ElementalThunderFrostReactionsConfig.freezeDurationPerStackTicks;
        if (freezeDuration < 20) freezeDuration = 20;
        int amplifier = freezeStacks - 1;

        target.removeEffect(ModMobEffects.FREEZE.get());
        target.addEffect(new MobEffectInstance(
                ModMobEffects.FREEZE.get(), freezeDuration, amplifier, false, true, true));
        data.putInt(FrostbiteHandler.NBT_FREEZE_STACKS, freezeStacks);
        if ("SCROLL".equals(attacker.getPersistentData().getString("EC_LastCastSource"))) {
            attacker.getPersistentData().putLong(ISSCore.NBT_ISS_REFRESH_CD,
                    target.level().getGameTime() + ElementalThunderFrostReactionsConfig.freezeCooldownTicks);
        }
    }

    @SubscribeEvent
    public static void onBlizzardAoeTick(TickEvent.LevelTickEvent event) {
        if (event.level.isClientSide()) return;
        if (event.phase != TickEvent.Phase.END) return;
        if (!ISSCore.ISS_LOADED) return;
        if (!(event.level instanceof ServerLevel serverLevel)) return;
        if (event.level.getGameTime() % 20 != 0) return;

        for (Entity blizzardEntity : serverLevel.getEntities().getAll()) {
            if (!blizzardEntity.getClass().getName().equals("io.redspace.ironsspellbooks.entity.spells.BlizzardAoe")) continue;
            if (!(blizzardEntity instanceof net.minecraft.world.entity.projectile.Projectile projectile)) continue;
            Entity owner = projectile.getOwner();
            if (!(owner instanceof LivingEntity livingOwner)) continue;
            int frostPower = ElementUtils.getDisplayEnhancement(livingOwner, ElementType.FROST);
            double threshold = ElementalThunderFrostReactionsConfig.frostStrengthThreshold;
            if (threshold <= 0 || frostPower < threshold) continue;
            double baseChance = ElementalThunderFrostReactionsConfig.frostbiteBaseChance;
            double scalingStep = ElementalThunderFrostReactionsConfig.frostbiteScalingStep;
            double scalingChance = ElementalThunderFrostReactionsConfig.frostbiteScalingChance;
            int scalingSteps = (scalingStep > 0 && frostPower >= scalingStep)
                    ? (int) ((frostPower - scalingStep) / scalingStep) : 0;
            double chance = Math.min(1.0, baseChance + scalingSteps * scalingChance);

            AABB box = blizzardEntity.getBoundingBox();
            int rarityStacks = ISSCore.getStacksFromLastCast(livingOwner);
            int step = (int) Math.max(1, ElementalThunderFrostReactionsConfig.frostbiteScalingStep);
            int eStacks = 1 + (frostPower - (int) threshold) / step;
            int stacksToAdd = rarityStacks > 0 ? Math.min(rarityStacks, eStacks) : eStacks;
            for (LivingEntity target : blizzardEntity.level().getEntitiesOfClass(LivingEntity.class, box)) {
                if (target == livingOwner) continue;
                if (ISSCore.RANDOM.nextDouble() < chance) {
                    FrostbiteHandler.applyFrostbite(target, livingOwner, stacksToAdd, chance, frostPower,
                            baseChance, scalingSteps, scalingChance, 0, 0, 0, "");
                }
            }
        }
    }

    @SubscribeEvent
    public static void onCasterDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!ISSCore.ISS_LOADED) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        CompoundTag data = mob.getPersistentData();
        if (!data.getBoolean(ISSCore.NBT_POLAR_BEAR_CAST)) return;
        data.putInt(ISSCore.NBT_POLAR_BEAR_COUNT, 3);
        if (!(mob.level() instanceof ServerLevel sl)) return;
        for (Entity e : sl.getEntities().getAll()) {
            if (!e.getClass().getName().equals("io.redspace.ironsspellbooks.entity.mobs.SummonedPolarBear")) continue;
            try {
                var getSummoner = e.getClass().getMethod("getSummoner");
                Entity owner = (Entity) getSummoner.invoke(e);
                if (owner != null && mob.getUUID().equals(owner.getUUID())) {
                    e.discard();
                }
            } catch (Exception ex) {
                ElementalCraft.LOGGER.error("Failed to discard polar bear on caster death", ex);
            }
        }
    }

    @SubscribeEvent
    public static void onChilledWetnessFreeze(LivingEvent.LivingTickEvent event) {
        if (!ISSCore.ISS_LOADED) return;
        net.minecraft.world.effect.MobEffect chilled = chilledEffect();
        if (chilled == null) return;
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        MobEffectInstance chilledEffectInstance = entity.getEffect(chilled);
        if (chilledEffectInstance == null) return;

        int wetnessLevel = WetnessHandler.getWetnessLevel(entity);
        CompoundTag data = entity.getPersistentData();

        if (FrostbiteHandler.isFrozen(entity)) {
            int freezeStacks = Math.max(1, data.getInt(FrostbiteHandler.NBT_FREEZE_STACKS));
            int freezeDuration = freezeStacks * ElementalThunderFrostReactionsConfig.freezeDurationPerStackTicks;
            if (freezeDuration > 10) {
                int amplifier = freezeStacks - 1;
                entity.removeEffect(ModMobEffects.FREEZE.get());
                entity.addEffect(new MobEffectInstance(
                        ModMobEffects.FREEZE.get(), freezeDuration, amplifier, false, true, true));
                data.putInt(FrostbiteHandler.NBT_FREEZE_STACKS, freezeStacks);
            }
            return;
        }

        if (wetnessLevel <= 0) return;
        if (FrostbiteHandler.isOnFreezeCooldown(entity)) return;

        int chilledLevel = chilledEffectInstance.getAmplifier() + 1;
        int freezeStacks = Math.max(chilledLevel, wetnessLevel);
        int freezeDuration = freezeStacks * ElementalThunderFrostReactionsConfig.freezeDurationPerStackTicks;
        if (freezeDuration < 20) freezeDuration = 20;

        entity.removeEffect(chilled);
        WetnessHandler.clearWetnessData(entity);

        entity.addEffect(new MobEffectInstance(
                ModMobEffects.FREEZE.get(), freezeDuration, freezeStacks - 1, false, true, true));
        data.putInt(FrostbiteHandler.NBT_FREEZE_STACKS, freezeStacks);
        data.putLong(FrostbiteHandler.NBT_FREEZE_COOLDOWN,
                entity.level().getGameTime() + freezeDuration + ElementalThunderFrostReactionsConfig.freezeCooldownTicks);
    }

    static void handleReaction(LivingEntity target, LivingEntity attacker, DamageSource source, int spellStacks) {
        int frostPower = ElementUtils.getDisplayEnhancement(attacker, ElementType.FROST);
        double threshold = ElementalThunderFrostReactionsConfig.frostStrengthThreshold;
        if (threshold <= 0 || frostPower < threshold) return;
        double baseChance = ElementalThunderFrostReactionsConfig.frostbiteBaseChance;
        double scalingStep = ElementalThunderFrostReactionsConfig.frostbiteScalingStep;
        double scalingChance = ElementalThunderFrostReactionsConfig.frostbiteScalingChance;
        int scalingSteps = (scalingStep > 0 && frostPower >= scalingStep)
                ? (int) ((frostPower - scalingStep) / scalingStep) : 0;
        double chance = Math.min(1.0, baseChance + scalingSteps * scalingChance);
        if (ISSCore.RANDOM.nextDouble() < chance) {
            int step = (int) Math.max(1, ElementalThunderFrostReactionsConfig.frostbiteScalingStep);
            int eStacks = 1 + (frostPower - (int) threshold) / step;
            int stacksToAdd = spellStacks > 0 ? Math.min(spellStacks, eStacks) : eStacks;
            FrostbiteHandler.applyFrostbite(target, attacker, stacksToAdd, chance, frostPower,
                    baseChance, scalingSteps, scalingChance, 0, 0, 0, "");
        } else {
            com.xulai.elementalcraft.command.DebugCommand.sendReactionFailed(attacker, "frostbite", "chance",
                    attacker.getDisplayName(), target.getDisplayName(),
                    Component.translatable("debug.elementalcraft.breakdown.chance",
                            Component.literal(String.format("%.0f", chance * 100)).withStyle(ChatFormatting.YELLOW)));
        }
    }

    static void onMobTick(Mob mob, CompoundTag data) {
        long gameTime = mob.level().getGameTime();
        String spellId = data.getString("EC_ISS_SpellId");
        boolean isPolarBear = "irons_spellbooks:summon_polar_bear".equals(spellId);
        boolean polarBearCast = data.getBoolean(ISSCore.NBT_POLAR_BEAR_CAST);

        if (isPolarBear && polarBearCast) {
            if (mob.tickCount % 20 == 0 && mob.level() instanceof ServerLevel sl) {
                boolean bearAlive = false;
                AABB searchBox = mob.getBoundingBox().inflate(48);
                for (Entity e : sl.getEntities(mob, searchBox)) {
                    if (!e.getClass().getName().equals("io.redspace.ironsspellbooks.entity.mobs.SummonedPolarBear")) continue;
                    try {
                        Entity owner = (Entity) e.getClass().getMethod("getSummoner").invoke(e);
                        if (owner != null && mob.getUUID().equals(owner.getUUID())) {
                            bearAlive = true;
                            break;
                        }
                    } catch (Exception ignored) {}
                }
                if (!bearAlive) {
                    data.remove(ISSCore.NBT_POLAR_BEAR_CAST);
                }
            }
            return;
        }

        if (isPolarBear) {
            LivingEntity target = mob.getTarget();
            if (target != null && target.isAlive()) {
                int count = data.getInt(ISSCore.NBT_POLAR_BEAR_COUNT);
                if (count >= 3) {
                    data.putBoolean(ISSCore.NBT_POLAR_BEAR_CAST, true);
                    return;
                }
                data.putInt(ISSCore.NBT_POLAR_BEAR_COUNT, count + 1);
                data.putBoolean(ISSCore.NBT_POLAR_BEAR_CAST, true);
                ISSCore.castSpell(mob, target, true);
            }
            return;
        }

        if (mob.getHealth() / mob.getMaxHealth() < ElementalISSIntegrationConfig.mobLowHealthThreshold) {
            ISSCore.tryAggressiveCast(mob, data, gameTime);
            return;
        }

        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) return;

        boolean netherImmune = ElementalFireNatureReactionsConfig.wetnessNetherDimensionImmune
                && mob.level().dimension() == Level.NETHER;

        if (netherImmune) {
            if (gameTime >= data.getLong(ISSCore.NBT_MOB_CAST_CD)) {
                ISSCore.castSpell(mob, target, false);
                data.putBoolean("EC_ISS_PendingIsSpell", true);
                data.putLong("EC_ISS_PendingCast", gameTime + 40);
                data.putLong(ISSCore.NBT_MOB_CAST_CD, gameTime + ElementalISSIntegrationConfig.mobNormalCastCooldown);
                data.putInt("EC_ISS_MissCount", 0);
            }
            return;
        }

        if (!ISSCore.tryThrowWaterBottle(mob, data, target, gameTime)) return;

        if (gameTime >= data.getLong(ISSCore.NBT_MOB_CAST_CD)) {
            ISSCore.castSpell(mob, target, false);
            data.putBoolean("EC_ISS_PendingIsSpell", true);
            data.putLong("EC_ISS_PendingCast", gameTime + 40);
            data.putLong(ISSCore.NBT_MOB_CAST_CD, gameTime + ElementalISSIntegrationConfig.mobNormalCastCooldown);
            data.putInt("EC_ISS_MissCount", 0);
        }
    }

    static void equipSlots(Mob mob, ItemStack scrollStack, String spellId) {
        boolean isPolarBear = "irons_spellbooks:summon_polar_bear".equals(spellId);
        mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        mob.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        if (isPolarBear) {
            mob.setItemSlot(EquipmentSlot.OFFHAND, scrollStack);
            mob.setDropChance(EquipmentSlot.OFFHAND, ElementalISSIntegrationConfig.scrollDropChance);
            scrollStack.enchant(ModEnchantments.FROST_STRIKE.get(), 1);
            ItemStack weapon = AttributeEquipUtils.createRandomWeapon();
            AttributeEquipUtils.applyAttackEnchant(weapon, ElementType.FROST);
            mob.setItemSlot(EquipmentSlot.MAINHAND, weapon);
            mob.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        } else {
            mob.setItemSlot(EquipmentSlot.MAINHAND, scrollStack);
            mob.setDropChance(EquipmentSlot.MAINHAND, ElementalISSIntegrationConfig.scrollDropChance);
            scrollStack.enchant(ModEnchantments.FROST_STRIKE.get(), 1);
            boolean netherImmune = ElementalFireNatureReactionsConfig.wetnessNetherDimensionImmune
                    && mob.level().dimension() == Level.NETHER;
            if (!netherImmune) {
                ItemStack waterBottle = new ItemStack(Items.SPLASH_POTION);
                PotionUtils.setPotion(waterBottle, Potions.WATER);
                mob.setItemSlot(EquipmentSlot.OFFHAND, waterBottle);
                mob.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
            }
        }
    }

    static boolean isIceSpellDamage(DamageSource source) {
        if (!ISSCore.ISS_LOADED) return false;
        return source.is(ISSCore.ISS_ICE_MAGIC);
    }
}
