
package com.xulai.elementalcraft.command;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = "elementalcraft")
public class ModCommands {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        DebugCommand.register(event.getDispatcher());

        BiomeBiasCommand.register(event.getDispatcher());

        SteamBlacklistCommand.register(event.getDispatcher());

        WetnessBlacklistCommand.register(event.getDispatcher());

        ScorchedBlacklistCommand.register(event.getDispatcher());

        SporeBlacklistCommand.register(event.getDispatcher());

        StaticImmunityBlacklistCommand.register(event.getDispatcher());

        ParalysisBlacklistCommand.register(event.getDispatcher());
    }
}