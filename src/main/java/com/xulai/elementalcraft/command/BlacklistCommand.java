// src/main/java/com/xulai/elementalcraft/command/BlacklistCommand.java
package com.xulai.elementalcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.xulai.elementalcraft.config.ElementalConfig;
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

import java.util.List;

@Mod.EventBusSubscriber(modid = "elementalcraft")
public class BlacklistCommand {

    /**
     * 注册指令入口
     * Register command entry point.
     * 
     * 当 Forge 触发 RegisterCommandsEvent 时调用，用于向游戏注册 /elementalcraft blacklist 相关子命令。
     * Called when Forge fires RegisterCommandsEvent, used to register all /elementalcraft blacklist sub-commands.
     */
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("elementalcraft")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("blacklist")
                .then(Commands.literal("add")
                    .executes(ctx -> addToBlacklist(ctx.getSource()))
                )
                .then(Commands.literal("remove")
                    .executes(ctx -> removeFromBlacklist(ctx.getSource()))
                )
                .then(Commands.literal("list")
                    .executes(BlacklistCommand::listBlacklist)
                )
            )
        );
    }

    /**
     * 将当前手持刷怪蛋对应的实体加入黑名单
     * Add the entity corresponding to the held spawn egg into the blacklist.
     * 
     * 仅允许玩家执行，控制台或命令方块将被拒绝。
     * Only executable by players; console or command blocks are rejected.
     */
    private static int addToBlacklist(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.elementalcraft.player_only"));
            return 0;
        }

        String entityId = getHeldEntityId(player);
        if (entityId == null) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.blacklist.no_egg"));
            return 0;
        }

        List<String> list = new java.util.ArrayList<>(ElementalConfig.BLACKLISTED_ENTITIES.get());
        if (list.contains(entityId)) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.blacklist.already_exists", entityId));
            return 0;
        }

        list.add(entityId);
        ElementalConfig.BLACKLISTED_ENTITIES.set(list);
        ElementalConfig.SPEC.save();

        player.sendSystemMessage(Component.translatable("command.elementalcraft.blacklist.added", entityId));
        player.sendSystemMessage(Component.translatable("command.elementalcraft.blacklist.saved"));
        return 1;
    }

    /**
     * 将当前手持刷怪蛋对应的实体从黑名单中移除
     * Remove the entity corresponding to the held spawn egg from the blacklist.
     * 
     * 若黑名单中不存在该实体，则不会修改配置。
     * If the entity is not found in the blacklist, no config changes are made.
     */
    private static int removeFromBlacklist(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.elementalcraft.player_only"));
            return 0;
        }

        String entityId = getHeldEntityId(player);
        if (entityId == null) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.blacklist.no_egg"));
            return 0;
        }

        List<String> list = new java.util.ArrayList<>(ElementalConfig.BLACKLISTED_ENTITIES.get());
        if (!list.remove(entityId)) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.blacklist.not_found", entityId));
            return 0;
        }

        ElementalConfig.BLACKLISTED_ENTITIES.set(list);
        ElementalConfig.SPEC.save();

        player.sendSystemMessage(Component.translatable("command.elementalcraft.blacklist.removed", entityId));
        player.sendSystemMessage(Component.translatable("command.elementalcraft.blacklist.saved"));
        return 1;
    }

    /**
     * 列出当前配置文件中的所有黑名单实体
     * List all blacklisted entities stored in the config.
     * 
     * 该指令不会修改任何数据，仅用于信息展示。
     * This command does not modify any data; it is for display only.
     */
    private static int listBlacklist(CommandContext<CommandSourceStack> ctx) {
        List<? extends String> list = ElementalConfig.BLACKLISTED_ENTITIES.get();
        CommandSourceStack source = ctx.getSource();

        if (list.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.elementalcraft.blacklist.empty"), false);
        } else {
            source.sendSuccess(() -> Component.translatable("command.elementalcraft.blacklist.header", list.size()), false);
            for (String id : list) {
                source.sendSuccess(() -> Component.literal(" §7- §f" + id), false);
            }
        }
        return list.size();
    }

    /**
     * 从玩家主手物品中解析实体 ID
     * Resolve entity registry ID from the item in the player's main hand.
     * 
     * 仅当主手物品是刷怪蛋时才会返回有效值，否则返回 null。
     * Returns a valid ID only if the held item is a spawn egg; otherwise returns null.
     */
    private static String getHeldEntityId(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof SpawnEggItem egg)) {
            return null;
        }
        var type = egg.getType(stack.getTag());
        return player.getServer()
                .registryAccess()
                .registryOrThrow(Registries.ENTITY_TYPE)
                .getKey(type)
                .toString();
    }
}
