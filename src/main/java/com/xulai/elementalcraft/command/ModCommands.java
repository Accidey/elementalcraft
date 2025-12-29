// src/main/java/com/xulai/elementalcraft/command/ModCommands.java
package com.xulai.elementalcraft.command;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * ModCommands
 * <p>
 * 中文说明：
 * 模组命令系统的统一注册入口类。
 * 该类使用 @Mod.EventBusSubscriber 注解，自动监听 Forge 的事件总线。
 * 它负责在服务器启动时的 RegisterCommandsEvent 事件中，手动调用那些没有自行处理注册逻辑的命令类（如 DebugCommand 等）。
 * <p>
 * English Description:
 * Unified entry point class for the mod's command system registration.
 * This class uses the @Mod.EventBusSubscriber annotation to automatically listen to the Forge event bus.
 * It is responsible for manually calling command classes (such as DebugCommand) that do not handle their own registration logic during the RegisterCommandsEvent at server startup.
 */
@Mod.EventBusSubscriber(modid = "elementalcraft")
public class ModCommands {

    /**
     * 监听命令注册事件并注册模组指令。
     * 这里主要注册那些需要显式传递 Dispatcher 的命令类。
     * <p>
     * Listens for the command registration event and registers mod commands.
     * Primarily registers command classes that require the Dispatcher to be explicitly passed.
     *
     * @param event 命令注册事件 / Command registration event
     */
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        // 注册调试命令
        // Register DebugCommand
        DebugCommand.register(event.getDispatcher());

        // 注册群系元素偏向命令
        // Register BiomeBiasCommand
        BiomeBiasCommand.register(event.getDispatcher());

        // 注册蒸汽效果黑名单命令
        // Register SteamBlacklistCommand
        SteamBlacklistCommand.register(event.getDispatcher());

        // 注意：部分命令（如 ForcedEntityCommand, BlacklistCommand 等）
        // 可能在其自身的类中通过 @SubscribeEvent 进行了自动注册，因此无需在此重复调用。
        // Note: Some commands (such as ForcedEntityCommand, BlacklistCommand, etc.)
        // may be automatically registered via @SubscribeEvent in their own classes, so they do not need to be called here again.
    }
}