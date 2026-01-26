// src/main/java/com/xulai/elementalcraft/command/WetnessBlacklistCommand.java
package com.xulai.elementalcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalReactionConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

import java.util.ArrayList;
import java.util.List;

/**
 * WetnessBlacklistCommand
 * <p>
 * 中文说明：
 * 负责注册和管理 /elementalcraft wetness blacklist 指令。
 * 该指令允许拥有 OP 权限的玩家通过手持生物刷怪蛋，将特定的生物类型加入或移出“潮湿效果”黑名单。
 * 修改配置后会自动保存并刷新缓存，实现即时热重载。
 * <p>
 * English Description:
 * Responsible for registering and managing the /elementalcraft wetness blacklist command.
 * This command allows players with OP permissions to add or remove specific mob types from the "Wetness Effect" blacklist by holding their spawn eggs.
 * Automatically saves and refreshes the cache after modification, enabling instant hot-reloading.
 */
public class WetnessBlacklistCommand {

    /**
     * 注册命令到 Brigadier 系统。
     * 命令结构：/elementalcraft wetness blacklist [add|remove|list]
     * <p>
     * Registers commands to the Brigadier system.
     * Command structure: /elementalcraft wetness blacklist [add|remove|list]
     *
     * @param dispatcher 命令分发器 / Command Dispatcher
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("elementalcraft")
                        // 需要 2 级权限（OP）
                        // Requires permission level 2 (OP)
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("wetness")
                                .then(Commands.literal("blacklist")

                                        // ===== add: 添加黑名单 =====
                                        // ===== add: Add to blacklist =====
                                        .then(Commands.literal("add")
                                                .executes(WetnessBlacklistCommand::executeAdd)
                                        )

                                        // ===== remove: 移除黑名单 =====
                                        // ===== remove: Remove from blacklist =====
                                        .then(Commands.literal("remove")
                                                .executes(WetnessBlacklistCommand::executeRemove)
                                        )

                                        // ===== list: 列出黑名单 =====
                                        // ===== list: List blacklist =====
                                        .then(Commands.literal("list")
                                                .executes(WetnessBlacklistCommand::executeList)
                                        )
                                )
                        )
        );
    }

    /**
     * 执行 add 命令：将手持刷怪蛋对应的生物加入黑名单。
     * 检查重复项，写入配置，并刷新缓存。
     * <p>
     * Executes the add command: Adds the mob corresponding to the held spawn egg to the blacklist.
     * Checks for duplicates, writes to config, and refreshes the cache.
     *
     * @param ctx 命令上下文 / Command context
     * @return 1 表示成功，0 表示失败 / 1 for success, 0 for failure
     */
    private static int executeAdd(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        // 获取手持刷怪蛋对应的实体 ID
        // Get Entity ID corresponding to the held spawn egg
        String entityId = getHeldEntityId(player);
        if (entityId == null) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.wetness.blacklist.no_egg")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        // 获取当前黑名单的可变副本
        // Get a mutable copy of the current blacklist
        List<String> list = new ArrayList<>(ElementalReactionConfig.WETNESS_ENTITY_BLACKLIST.get());

        // 检查是否已存在于黑名单中
        // Check if it already exists in the blacklist
        if (list.contains(entityId)) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.wetness.blacklist.already_exists", entityId)
                    .withStyle(ChatFormatting.YELLOW));
            return 0;
        }

        // 添加实体 ID，保存配置并刷新缓存
        // Add entity ID, save config, and refresh cache
        list.add(entityId);
        ElementalReactionConfig.WETNESS_ENTITY_BLACKLIST.set(list);
        ElementalReactionConfig.SPEC.save();
        ElementalReactionConfig.refreshCache();

        // 发送成功反馈
        // Send success feedback
        player.sendSystemMessage(Component.translatable("command.elementalcraft.wetness.blacklist.added", entityId)
                .withStyle(ChatFormatting.GREEN));
        player.sendSystemMessage(Component.translatable("command.elementalcraft.wetness.blacklist.saved")
                .withStyle(ChatFormatting.GRAY));
        return 1;
    }

    /**
     * 执行 remove 命令：将手持刷怪蛋对应的生物移出黑名单。
     * 检查存在性，更新配置，并刷新缓存。
     * <p>
     * Executes the remove command: Removes the mob corresponding to the held spawn egg from the blacklist.
     * Checks for existence, updates config, and refreshes the cache.
     *
     * @param ctx 命令上下文 / Command context
     * @return 1 表示成功 / 1 for success
     */
    private static int executeRemove(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        // 获取手持刷怪蛋对应的实体 ID
        // Get Entity ID corresponding to the held spawn egg
        String entityId = getHeldEntityId(player);
        if (entityId == null) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.wetness.blacklist.no_egg")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        List<String> list = new ArrayList<>(ElementalReactionConfig.WETNESS_ENTITY_BLACKLIST.get());

        // 尝试从列表中移除
        // Try to remove from the list
        if (list.remove(entityId)) {
            // 保存更改并刷新缓存
            // Save changes and refresh cache
            ElementalReactionConfig.WETNESS_ENTITY_BLACKLIST.set(list);
            ElementalReactionConfig.SPEC.save();
            ElementalReactionConfig.refreshCache();

            // 发送移除成功反馈
            // Send success feedback
            player.sendSystemMessage(Component.translatable("command.elementalcraft.wetness.blacklist.removed", entityId)
                    .withStyle(ChatFormatting.GREEN));
            player.sendSystemMessage(Component.translatable("command.elementalcraft.wetness.blacklist.saved")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            // 发送未找到反馈
            // Send not found feedback
            player.sendSystemMessage(Component.translatable("command.elementalcraft.wetness.blacklist.not_found", entityId)
                    .withStyle(ChatFormatting.YELLOW));
        }
        return 1;
    }

    /**
     * 执行 list 命令：列出当前所有黑名单生物。
     * <p>
     * Executes the list command: Lists all mobs currently in the blacklist.
     *
     * @param ctx 命令上下文 / Command context
     * @return 黑名单条目数量 / Number of blacklist entries
     */
    private static int executeList(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        List<? extends String> list = ElementalReactionConfig.WETNESS_ENTITY_BLACKLIST.get();

        if (list.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.elementalcraft.wetness.blacklist.empty")
                    .withStyle(ChatFormatting.GRAY), false);
        } else {
            source.sendSuccess(() -> Component.translatable("command.elementalcraft.wetness.blacklist.header", list.size())
                    .withStyle(ChatFormatting.YELLOW), false);
            for (String entry : list) {
                source.sendSuccess(() -> Component.literal(" - " + entry).withStyle(ChatFormatting.WHITE), false);
            }
        }
        return list.size();
    }

    /**
     * 从玩家主手刷怪蛋解析实体 ID。
     * <p>
     * Helper method to parse the entity registry ID from the spawn egg held in the player's main hand.
     *
     * @param player 玩家实体 / Player entity
     * @return 实体资源 ID 字符串（例如 "minecraft:creeper"），若未持有刷怪蛋则返回 null / Entity resource ID string (e.g., "minecraft:creeper"), or null if not holding a spawn egg
     */
    private static String getHeldEntityId(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof SpawnEggItem egg)) {
            return null;
        }
        // 兼容 Forge 1.20.1，getTag() 可能为 null，但 SpawnEggItem 会处理
        // Compatible with Forge 1.20.1, getTag() may be null, but SpawnEggItem handles it
        var type = egg.getType(stack.getTag());
        return player.getServer()
                .registryAccess()
                .registryOrThrow(Registries.ENTITY_TYPE)
                .getKey(type)
                .toString();
    }
}