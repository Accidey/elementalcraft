package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.logic.MobAttributeLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;



@Mod.EventBusSubscriber(modid = "elementalcraft")
public class PlayerTrackEvents {




    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {


        if (event.side.isClient()) return;



        if (event.phase != TickEvent.Phase.START) return;

        if (!(event.player instanceof ServerPlayer player)) return;



        if (player.tickCount % 40 != 0) return;

        ServerLevel level = player.serverLevel();



        level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(20, 5, 20),
                mob -> mob.isAlive() && !mob.getPersistentData().getBoolean("ElementalCraft_AttributesSet")
        ).forEach(MobAttributeLogic::processMob);
    }
}
