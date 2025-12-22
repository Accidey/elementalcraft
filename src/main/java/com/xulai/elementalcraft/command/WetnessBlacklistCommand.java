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
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * WetnessBlacklistCommand
 *
 * 中文说明：
 * 负责注册和管理 /elementalcraft wetness blacklist 指令。
 * 允许 OP 玩家通过手持刷怪蛋，将特定生物加入或移出潮湿效果黑名单。
 * 修改配置后会自动刷新缓存，实现热重载。
 *
 * English description:
 * Responsible for registering and handling the /elementalcraft wetness blacklist command.
 * Allows OP players to add or remove specific mobs from the wetness effect blacklist by holding a spawn egg.
 * Automatically refreshes the cache after modifying the config, enabling hot-reloading.
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class WetnessBlacklistCommand {

    /**
     * 注册命令到 Brigadier 系统。
     * 命令结构：/elementalcraft wetness blacklist [add|remove|list]
     *
     * Registers commands to the Brigadier system.
     * Command structure: /elementalcraft wetness blacklist [add|remove|list]
     */
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("elementalcraft")
                .requires(source -> source.hasPermission(2)) // 需要 OP 权限 / Requires OP permission
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
     *
     * Executes add command: Adds the mob corresponding to the held spawn egg to the blacklist.
     */
    private static int executeAdd(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        String entityId = getHeldEntityId(player);
        if (entityId == null) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.wetness.blacklist.no_egg")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        List<String> list = new ArrayList<>(ElementalReactionConfig.WETNESS_ENTITY_BLACKLIST.get());
        
        // 检查是否已存在
        // Check if already exists
        if (list.contains(entityId)) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.wetness.blacklist.already_exists", entityId)
                    .withStyle(ChatFormatting.YELLOW));
            return 0;
        }

        // 添加并保存
        // Add and save
        list.add(entityId);
        ElementalReactionConfig.WETNESS_ENTITY_BLACKLIST.set(list);
        ElementalReactionConfig.SPEC.save();
        ElementalReactionConfig.refreshCache();

        player.sendSystemMessage(Component.translatable("command.elementalcraft.wetness.blacklist.added", entityId)
                .withStyle(ChatFormatting.GREEN));
        player.sendSystemMessage(Component.translatable("command.elementalcraft.wetness.blacklist.saved")
                .withStyle(ChatFormatting.GRAY));
        return 1;
    }

    /**
     * 执行 remove 命令：将手持刷怪蛋对应的生物移出黑名单。
     *
     * Executes remove command: Removes the mob corresponding to the held spawn egg from the blacklist.
     */
    private static int executeRemove(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        String entityId = getHeldEntityId(player);
        if (entityId == null) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.wetness.blacklist.no_egg")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        List<String> list = new ArrayList<>(ElementalReactionConfig.WETNESS_ENTITY_BLACKLIST.get());

        // 尝试移除
        // Try to remove
        if (list.remove(entityId)) {
            ElementalReactionConfig.WETNESS_ENTITY_BLACKLIST.set(list);
            ElementalReactionConfig.SPEC.save();
            ElementalReactionConfig.refreshCache();

            player.sendSystemMessage(Component.translatable("command.elementalcraft.wetness.blacklist.removed", entityId)
                    .withStyle(ChatFormatting.GREEN));
            player.sendSystemMessage(Component.translatable("command.elementalcraft.wetness.blacklist.saved")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.wetness.blacklist.not_found", entityId)
                    .withStyle(ChatFormatting.YELLOW));
        }
        return 1;
    }

    /**
     * 执行 list 命令：列出当前所有黑名单生物。
     *
     * Executes list command: Lists all mobs currently in the blacklist.
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
     * 从玩家主手刷怪蛋解析实体ID。
     *
     * Get entity registry ID from held spawn egg.
     */
    private static String getHeldEntityId(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof SpawnEggItem egg)) {
            return null;
        }
        // 兼容 Forge 1.20.1，getTag() 可能为 null，但 SpawnEggItem 会处理
        var type = egg.getType(stack.getTag());
        return player.getServer()
                .registryAccess()
                .registryOrThrow(Registries.ENTITY_TYPE)
                .getKey(type)
                .toString();
    }
}