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
 *
 * 中文说明：
 * 注册 /elementalcraft steam_blacklist 指令。
 * 通过检测玩家手持的刷怪蛋，动态添加或移除蒸汽烫伤免疫黑名单。
 * 支持本地化语言提示。
 *
 * English description:
 * Registers the /elementalcraft steam_blacklist command.
 * Dynamically adds/removes entities from the steam immunity blacklist by detecting the Spawn Egg held by the player.
 * Supports localized messages.
 */
public class SteamBlacklistCommand {

    /**
     * 注册指令逻辑。
     * 指令结构：/elementalcraft steam_blacklist <add|remove|list>
     *
     * Register command logic.
     * Structure: /elementalcraft steam_blacklist <add|remove|list>
     *
     * @param dispatcher 命令调度器 / Command dispatcher
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("elementalcraft")
            .then(Commands.literal("SteamBlacklist")
                .requires(source -> source.hasPermission(2)) // 需要 OP 权限 (Level 2) / Requires OP permission
                
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
                
                // 子指令：列表 (list) - 显示所有
                // Sub-command: list - Show all
                .then(Commands.literal("list")
                    .executes(SteamBlacklistCommand::listEntities)
                )
            )
        );
    }

    /**
     * 执行从手持刷怪蛋添加实体到黑名单的逻辑。
     *
     * Executes the logic to add an entity to the blacklist from the held Spawn Egg.
     */
    private static int addFromHand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack heldItem = player.getMainHandItem();

        // 1. 检查手持物品是否为刷怪蛋
        // 1. Check if the held item is a Spawn Egg
        if (!(heldItem.getItem() instanceof SpawnEggItem egg)) {
            // 发送本地化错误提示：请手持刷怪蛋
            // Send localized error: Please hold a spawn egg
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.steam_blacklist.not_egg"));
            return 0;
        }

        // 2. 获取实体 ID
        // 2. Get Entity ID
        EntityType<?> type = egg.getType(heldItem.getTag());
        ResourceLocation idLoc = ForgeRegistries.ENTITY_TYPES.getKey(type);
        
        if (idLoc == null) return 0;
        String entityId = idLoc.toString();

        // 3. 读取并检查配置
        // 3. Read and check config
        List<String> currentList = new ArrayList<>(ElementalReactionConfig.STEAM_IMMUNITY_BLACKLIST.get());

        if (currentList.contains(entityId)) {
            // 发送本地化错误提示：该生物已存在
            // Send localized error: Entity already exists
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.steam_blacklist.exists", entityId));
            return 0;
        }

        // 4. 添加、保存并刷新
        // 4. Add, save, and refresh
        currentList.add(entityId);
        ElementalReactionConfig.STEAM_IMMUNITY_BLACKLIST.set(currentList);
        ElementalReactionConfig.SPEC.save();
        ElementalReactionConfig.refreshCache();

        // 发送本地化成功提示
        // Send localized success message
        context.getSource().sendSuccess(() -> Component.translatable("command.elementalcraft.steam_blacklist.add.success", entityId), true);
        return 1;
    }

    /**
     * 执行从手持刷怪蛋移除实体的逻辑。
     *
     * Executes the logic to remove an entity from the blacklist from the held Spawn Egg.
     */
    private static int removeFromHand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack heldItem = player.getMainHandItem();

        // 1. 检查手持物品
        // 1. Check held item
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

        // 3. 读取并检查配置
        // 3. Read and check config
        List<String> currentList = new ArrayList<>(ElementalReactionConfig.STEAM_IMMUNITY_BLACKLIST.get());

        if (!currentList.contains(entityId)) {
            // 发送本地化错误提示：黑名单中未找到该生物
            // Send localized error: Entity not found in blacklist
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.steam_blacklist.not_found", entityId));
            return 0;
        }

        // 4. 移除、保存并刷新
        // 4. Remove, save, and refresh
        currentList.remove(entityId);
        ElementalReactionConfig.STEAM_IMMUNITY_BLACKLIST.set(currentList);
        ElementalReactionConfig.SPEC.save();
        ElementalReactionConfig.refreshCache();

        // 发送本地化成功提示
        // Send localized success message
        context.getSource().sendSuccess(() -> Component.translatable("command.elementalcraft.steam_blacklist.remove.success", entityId), true);
        return 1;
    }

    /**
     * 列出黑名单中所有实体的逻辑。
     *
     * Executes the logic to list all entities in the blacklist.
     */
    private static int listEntities(CommandContext<CommandSourceStack> context) {
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