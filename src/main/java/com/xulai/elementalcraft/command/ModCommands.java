// src/main/java/com/xulai/elementalcraft/command/ModCommands.java
package com.xulai.elementalcraft.command;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * ModCommands 类是模组命令系统的统一入口。
 * 它监听服务器命令注册事件，并负责手动注册那些没有使用 @SubscribeEvent 自动注册的命令。
 *
 * ModCommands class serves as the unified entry point for the mod's command system.
 * It listens to the server command registration event and manually registers commands that do not use @SubscribeEvent for automatic registration.
 */
@Mod.EventBusSubscriber(modid = "elementalcraft")
public class ModCommands {

    /**
     * 在服务器命令注册事件中手动注册部分命令。
     * DebugCommand, BiomeBiasCommand 和 SteamBlacklistCommand 使用传统方式通过 dispatcher.register() 注册。
     *
     * Manually register certain commands during the server command registration event.
     * DebugCommand, BiomeBiasCommand, and SteamBlacklistCommand use the traditional dispatcher.register() approach.
     *
     * @param event 命令注册事件 / Command registration event
     */
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        // 手动注册 DebugCommand / Manually register DebugCommand
        DebugCommand.register(event.getDispatcher());

        // 手动注册 BiomeBiasCommand / Manually register BiomeBiasCommand
        BiomeBiasCommand.register(event.getDispatcher());

        // 手动注册 SteamBlacklistCommand (新添加：蒸汽免疫黑名单指令)
        // Manually register SteamBlacklistCommand (New: Steam Immunity Blacklist Command)
        SteamBlacklistCommand.register(event.getDispatcher());

        // 注意：其他命令（如 ForcedEntityCommand）已通过各自类中的 @SubscribeEvent 自动注册，无需在此处添加
        // Note: Other commands (like ForcedEntityCommand) are automatically registered via @SubscribeEvent in their own classes and do not need to be added here
    }
}