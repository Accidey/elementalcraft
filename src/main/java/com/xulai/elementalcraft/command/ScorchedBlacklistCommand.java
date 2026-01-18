// src/main/java/com/xulai/elementalcraft/command/ScorchedBlacklistCommand.java
package com.xulai.elementalcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.xulai.elementalcraft.config.ElementalReactionConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * ScorchedBlacklistCommand
 * <p>
 * 中文说明：
 * 负责注册和管理 /elementalcraft scorched blacklist 指令。
 * 该指令允许拥有 OP 权限的玩家通过手持生物刷怪蛋，将特定的生物类型加入或移出“灼烧免疫”黑名单。
 * 包含重复添加检测功能：如果目标已在黑名单中，会拒绝添加并提示使用 remove 命令。
 * <p>
 * English Description:
 * Registers and handles the /elementalcraft scorched blacklist command.
 * Allows OP players to add/remove entities from the "Scorched Immunity" blacklist by holding a spawn egg.
 * Includes duplicate check: If target is already in blacklist, refuses to add and prompts to use remove command.
 */
public class ScorchedBlacklistCommand {

    /**
     * 注册指令。
     * <p>
     * Register command.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("elementalcraft")
                .then(Commands.literal("scorched")
                        .then(Commands.literal("blacklist")
                                .requires(source -> source.hasPermission(2)) // 需 OP 权限 / Requires OP
                                .then(Commands.literal("add")
                                        .executes(ScorchedBlacklistCommand::addEntity))
                                .then(Commands.literal("remove")
                                        .executes(ScorchedBlacklistCommand::removeEntity))
                                .then(Commands.literal("list")
                                        .executes(ScorchedBlacklistCommand::listEntities))
                        )
                )
        );
    }

    /**
     * 添加实体到黑名单。
     * 包含重复性检查，防止配置冗余。
     * <p>
     * Add entity to blacklist.
     * Includes duplicate check to prevent config redundancy.
     */
    private static int addEntity(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        String entityId = getHeldEntityId(player);
        if (entityId == null) {
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.scorched.blacklist.no_egg"));
            return 0;
        }

        // 获取当前黑名单的可变副本
        List<String> currentList = new ArrayList<>(ElementalReactionConfig.SCORCHED_ENTITY_BLACKLIST.get());
        
        // 检查是否已存在
        // Check if already exists
        if (currentList.contains(entityId)) {
            // 发送"已存在"错误提示
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.scorched.blacklist.already_exists", entityId));
            // 额外提示：请使用 remove 命令 (复用通用语言键，避免硬编码)
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.blacklist.use_remove_first"));
            return 0;
        }

        // 添加并保存
        currentList.add(entityId);
        ElementalReactionConfig.SCORCHED_ENTITY_BLACKLIST.set(currentList);
        ElementalReactionConfig.SPEC.save();
        ElementalReactionConfig.refreshCache();

        context.getSource().sendSuccess(() -> Component.translatable("command.elementalcraft.scorched.blacklist.added", entityId)
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    /**
     * 从黑名单移除实体。
     * <p>
     * Remove entity from blacklist.
     */
    private static int removeEntity(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        String entityId = getHeldEntityId(player);
        if (entityId == null) {
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.scorched.blacklist.no_egg"));
            return 0;
        }

        List<String> currentList = new ArrayList<>(ElementalReactionConfig.SCORCHED_ENTITY_BLACKLIST.get());

        if (!currentList.contains(entityId)) {
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.scorched.blacklist.not_found", entityId));
            return 0;
        }

        currentList.remove(entityId);
        
        ElementalReactionConfig.SCORCHED_ENTITY_BLACKLIST.set(currentList);
        ElementalReactionConfig.SPEC.save();
        ElementalReactionConfig.refreshCache();

        context.getSource().sendSuccess(() -> Component.translatable("command.elementalcraft.scorched.blacklist.removed", entityId)
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    /**
     * 列出当前黑名单。
     * <p>
     * List current blacklist.
     */
    private static int listEntities(CommandContext<CommandSourceStack> context) {
        List<? extends String> list = ElementalReactionConfig.cachedScorchedBlacklist;
        CommandSourceStack source = context.getSource();

        if (list.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.elementalcraft.scorched.blacklist.empty")
                    .withStyle(ChatFormatting.GRAY), false);
        } else {
            source.sendSuccess(() -> Component.translatable("command.elementalcraft.scorched.blacklist.header", list.size())
                    .withStyle(ChatFormatting.GOLD), false);
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
     */
    private static String getHeldEntityId(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof SpawnEggItem egg)) {
            return null;
        }
        var type = egg.getType(stack.getTag());
        return ForgeRegistries.ENTITY_TYPES.getKey(type).toString();
    }
}