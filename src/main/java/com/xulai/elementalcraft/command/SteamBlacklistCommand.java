// src/main/java/com/xulai/elementalcraft/command/SteamBlacklistCommand.java
package com.xulai.elementalcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.xulai.elementalcraft.config.ElementalReactionConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * SteamBlacklistCommand
 * <p>
 * 中文说明：
 * 注册并处理 /elementalcraft steam_blacklist 指令。
 * 该指令允许 OP 玩家通过手持刷怪蛋，将对应的实体类型动态添加或移除出蒸汽烫伤免疫黑名单。
 * 修改后的配置将直接保存到配置文件中，并立即刷新缓存。
 * <p>
 * English Description:
 * Registers and handles the /elementalcraft steam_blacklist command.
 * This command allows OP players to dynamically add or remove entity types from the steam scald immunity blacklist by holding their spawn eggs.
 * Modified configurations are saved directly to the config file and the cache is refreshed immediately.
 */
public class SteamBlacklistCommand {

    /**
     * 注册指令逻辑到调度器。
     * 指令结构：/elementalcraft steam_blacklist <add|remove|list>
     * <p>
     * Registers command logic to the dispatcher.
     * Command structure: /elementalcraft steam_blacklist <add|remove|list>
     *
     * @param dispatcher 命令调度器 / Command dispatcher
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("elementalcraft")
                .then(Commands.literal("steamblacklist")
                        // 需要 OP 权限 (Level 2)
                        // Requires OP permission (Level 2)
                        .requires(source -> source.hasPermission(2))

                        // 子指令：添加 (add) - 读取手持物品
                        // Sub-command: add - Reads held item
                        .then(Commands.literal("add")
                                .executes(SteamBlacklistCommand::addFromHand)
                        )

                        // 子指令：移除 (remove) - 读取手持物品
                        // Sub-command: remove - Reads held item
                        .then(Commands.literal("remove")
                                .executes(SteamBlacklistCommand::removeFromHand)
                        )

                        // 子指令：列表 (list) - 显示所有黑名单实体
                        // Sub-command: list - Show all blacklisted entities
                        .then(Commands.literal("list")
                                .executes(SteamBlacklistCommand::listEntities)
                        )
                )
        );
    }

    /**
     * 执行从手持刷怪蛋添加实体到黑名单的逻辑。
     * 验证玩家手持物品，获取实体 ID，更新配置并保存。
     * <p>
     * Executes the logic to add an entity to the blacklist from the held Spawn Egg.
     * Validates the held item, retrieves the Entity ID, updates the configuration, and saves it.
     *
     * @param context 命令上下文 / Command context
     * @return 执行结果 / Execution result
     * @throws CommandSyntaxException 如果玩家无效 / If player is invalid
     */
    private static int addFromHand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack heldItem = player.getMainHandItem();

        // 1. 检查手持物品是否为刷怪蛋
        // 1. Check if the held item is a Spawn Egg
        if (!(heldItem.getItem() instanceof SpawnEggItem egg)) {
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.steam_blacklist.not_egg"));
            return 0;
        }

        // 2. 获取实体 ID
        // 2. Get Entity ID
        EntityType<?> type = egg.getType(heldItem.getTag());
        ResourceLocation idLoc = ForgeRegistries.ENTITY_TYPES.getKey(type);

        if (idLoc == null) return 0;
        String entityId = idLoc.toString();

        // 3. 读取当前配置列表并检查是否已存在
        // 3. Read current config list and check if it already exists
        List<String> currentList = new ArrayList<>(ElementalReactionConfig.STEAM_IMMUNITY_BLACKLIST.get());

        if (currentList.contains(entityId)) {
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.steam_blacklist.exists", entityId));
            return 0;
        }

        // 4. 添加到列表、保存配置并刷新缓存
        // 4. Add to list, save config, and refresh cache
        currentList.add(entityId);
        ElementalReactionConfig.STEAM_IMMUNITY_BLACKLIST.set(currentList);
        ElementalReactionConfig.SPEC.save();
        ElementalReactionConfig.refreshCache();

        // 发送成功反馈
        // Send success feedback
        context.getSource().sendSuccess(() -> Component.translatable("command.elementalcraft.steam_blacklist.add.success", entityId), true);
        return 1;
    }

    /**
     * 执行从手持刷怪蛋移除实体的逻辑。
     * 验证玩家手持物品，获取实体 ID，从配置中移除并保存。
     * <p>
     * Executes the logic to remove an entity from the blacklist using the held Spawn Egg.
     * Validates the held item, retrieves the Entity ID, removes it from the configuration, and saves.
     *
     * @param context 命令上下文 / Command context
     * @return 执行结果 / Execution result
     * @throws CommandSyntaxException 如果玩家无效 / If player is invalid
     */
    private static int removeFromHand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack heldItem = player.getMainHandItem();

        // 1. 检查手持物品是否为刷怪蛋
        // 1. Check if the held item is a Spawn Egg
        if (!(heldItem.getItem() instanceof SpawnEggItem egg)) {
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.steam_blacklist.not_egg"));
            return 0;
        }

        // 2. 获取实体 ID
        // 2. Get Entity ID
        EntityType<?> type = egg.getType(heldItem.getTag());
        ResourceLocation idLoc = ForgeRegistries.ENTITY_TYPES.getKey(type);

        if (idLoc == null) return 0;
        String entityId = idLoc.toString();

        // 3. 读取当前配置列表并检查是否存在
        // 3. Read current config list and check if it exists
        List<String> currentList = new ArrayList<>(ElementalReactionConfig.STEAM_IMMUNITY_BLACKLIST.get());

        if (!currentList.contains(entityId)) {
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.steam_blacklist.not_found", entityId));
            return 0;
        }

        // 4. 从列表中移除、保存配置并刷新缓存
        // 4. Remove from list, save config, and refresh cache
        currentList.remove(entityId);
        ElementalReactionConfig.STEAM_IMMUNITY_BLACKLIST.set(currentList);
        ElementalReactionConfig.SPEC.save();
        ElementalReactionConfig.refreshCache();

        // 发送成功反馈
        // Send success feedback
        context.getSource().sendSuccess(() -> Component.translatable("command.elementalcraft.steam_blacklist.remove.success", entityId), true);
        return 1;
    }

    /**
     * 列出黑名单中所有实体的逻辑。
     * 读取缓存的黑名单列表并发送给执行者。
     * <p>
     * Executes the logic to list all entities in the blacklist.
     * Reads the cached blacklist and sends it to the executor.
     *
     * @param context 命令上下文 / Command context
     * @return 列表大小 / List size
     */
    private static int listEntities(CommandContext<CommandSourceStack> context) {
        // 使用缓存的列表进行展示
        // Use cached list for display
        List<? extends String> list = ElementalReactionConfig.cachedSteamBlacklist;

        if (list.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.translatable("command.elementalcraft.steam_blacklist.empty"), false);
        } else {
            context.getSource().sendSuccess(() -> Component.translatable("command.elementalcraft.steam_blacklist.list.header"), false);
            for (String id : list) {
                context.getSource().sendSuccess(() -> Component.literal(" - " + id), false);
            }
        }
        return list.size();
    }
}