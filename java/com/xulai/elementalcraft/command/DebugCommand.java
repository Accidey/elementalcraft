// src/main/java/com/xulai/elementalcraft/command/DebugCommand.java
package com.xulai.elementalcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.xulai.elementalcraft.util.DebugMode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * DebugCommand
 *
 * 中文：
 * 该命令用于为玩家切换 ElementalCraft 的属性调试模式。
 * 调试模式开启后，玩家可以看到额外的属性伤害计算信息，
 * 主要用于 Mod 开发、数值测试或高阶玩家分析战斗过程。
 *
 * English:
 * This command toggles ElementalCraft's debug mode for the executing player.
 * When enabled, additional elemental damage calculation details will be shown.
 */
public class DebugCommand {

    /**
     * 注册 /elementalcraft debug 命令
     *
     * 中文：
     * - 命令路径：/elementalcraft debug
     * - 权限等级：0（所有玩家可用）
     * - 仅允许玩家实体执行（控制台与命令方块无效）
     *
     * English:
     * - Command path: /elementalcraft debug
     * - Permission level: 0 (all players)
     * - Player-only execution
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
            Commands.literal("elementalcraft")
                .requires(source -> source.hasPermission(0))

                .then(
                    Commands.literal("debug")
                        .executes(context -> {

                            CommandSourceStack source = context.getSource();

                            // ==================== 玩家实体校验 ====================
                            // 中文：确保命令只能由玩家执行
                            // English: Ensure the command is executed by a player
                            if (!(source.getEntity() instanceof Player player)) {
                                source.sendFailure(
                                    Component.translatable("command.elementalcraft.only_players")
                                );
                                return 0;
                            }

                            // ==================== 调试模式状态切换 ====================
                            // 中文：记录切换前的调试状态
                            // English: Store previous debug state
                            boolean wasEnabled = DebugMode.isEnabled(player);

                            // 中文：切换当前玩家的调试模式
                            // English: Toggle debug mode for this player
                            DebugMode.toggle(player);

                            // 中文：获取切换后的调试状态
                            // English: Get current debug state
                            boolean nowEnabled = DebugMode.isEnabled(player);

                            // ==================== 开启调试模式提示 ====================
                            // 中文：仅在“关闭 → 开启”时发送提示
                            // English: Send enable message only when toggled on
                            if (!wasEnabled && nowEnabled) {

                                // 使用语言文件的“开启提示”
                                player.displayClientMessage(
                                    Component.translatable("command.elementalcraft.debug.enabled")
                                        .withStyle(style -> style.withColor(0x55FF55)),
                                    false
                                );

                                // 使用语言文件的调试模式说明提示（原硬编码文本）
                                player.displayClientMessage(
                                    Component.translatable("command.elementalcraft.debug.global_notice")
                                        .withStyle(style -> style.withBold(true)),
                                    false
                                );

                            }
                            // ==================== 关闭调试模式提示 ====================
                            // 中文：仅在“开启 → 关闭”时发送提示
                            // English: Send disable message only when toggled off
                            else if (wasEnabled && !nowEnabled) {

                                player.displayClientMessage(
                                    Component.translatable("command.elementalcraft.debug.disabled")
                                        .withStyle(style -> style.withColor(0xFF5555)),
                                    false
                                );
                            }

                            // 中文：命令成功执行
                            // English: Command executed successfully
                            return 1;
                        })
                )
        );
    }
}
