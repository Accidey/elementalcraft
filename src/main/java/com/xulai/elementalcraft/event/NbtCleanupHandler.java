package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.potion.ModMobEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class NbtCleanupHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        cleanupStaleData(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        cleanupStaleData(event.getEntity());
    }

    private static void cleanupStaleData(Player player) {
        CompoundTag data = player.getPersistentData();

        if (!player.hasEffect(ModMobEffects.STATIC_SHOCK.get())) {
            StaticShockHandler.clearStaticShock(player);
            data.remove("ec_static_aura_spore_cd");
            data.remove("ec_aura_sync_phase");
            data.remove("ec_last_aura_log_damage");
            data.remove("ec_last_static_base_damage");
            data.remove("ec_last_static_element");
            data.remove("ec_last_static_element_mult");
            data.remove("ec_static_aura_tracked");
        }

        if (!player.hasEffect(ModMobEffects.PARALYSIS.get())) {
            data.remove("ec_paralysis_stacks");
            data.remove("ec_paralysis_timer");
            data.remove("ec_paralysis_cooldown_timer");
        }

        if (!player.hasEffect(ModMobEffects.FROSTBITE.get())) {
            FrostbiteHandler.clearFrostbite(player);
            FrostbiteHandler.clearTempFrostbite(player);
            data.remove(FrostbiteHandler.NBT_FROST_AURA_TRACKED);
            data.remove(FrostbiteHandler.NBT_FROSTBITE_PERIODIC_LOGGED);
            data.remove(FrostbiteHandler.NBT_FROSTBITE_LAST_PERIODIC_DMG);
        }

        if (!player.hasEffect(ModMobEffects.FREEZE.get())) {
            data.remove(FrostbiteHandler.NBT_FREEZE_COOLDOWN);
            data.remove(FrostbiteHandler.NBT_FREEZE_STACKS);
            data.remove(FrostbiteHandler.NBT_FROZEN_FROSTBITE_STACKS);
            data.remove(FrostbiteHandler.NBT_FREEZE_AI_DISABLED);
            data.remove(FrostbiteHandler.NBT_FREEZE_ORIGINAL_NO_AI);
            data.remove("EC_SharedOriginalNoAI");
            data.remove("EC_DrownTimer");
            player.setTicksFrozen(0);
        }

        if (!player.hasEffect(ModMobEffects.WETNESS.get())) {
            WetnessHandler.clearWetnessData(player);
        }

        if (!ScorchedHandler.isScorched(player)) {
            ScorchedHandler.clearScorched(player);
            data.remove(ScorchedHandler.NBT_ATTACKER_SCORCHED_COOLDOWN);
            data.remove(ScorchedHandler.NBT_WETNESS_STEAM_COOLDOWN);
        }

        data.remove("EC_SteamCondensationTimer");
        data.remove("EC_SteamAttackerCooldown");
        data.remove("EC_SteamBlindness");
        data.remove("EC_SteamScaldingLogged");
        data.remove("EC_FrostedCloudUUID");
        data.remove("EC_StaticCloudUUID");
        data.remove("EC_StaticDmgCloudUUID");

        data.remove("EC_FireFrostMeltResolved");
        data.remove("EC_SelfDryingPenalty");
        data.remove("EC_LastSelfDryTick");
        data.remove("EC_NatureAttackCooldown");
    }
}
