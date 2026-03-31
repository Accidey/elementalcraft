package com.xulai.elementalcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
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

public class WetnessBlacklistCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("elementalcraft")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("blacklist")
                                .then(Commands.literal("wetness")
                                

                                        .then(Commands.literal("add")
                                                .executes(WetnessBlacklistCommand::executeAdd)
                                        )

                                        .then(Commands.literal("remove")
                                                .executes(WetnessBlacklistCommand::executeRemove)
                                        )

                                        .then(Commands.literal("list")
                                                .executes(WetnessBlacklistCommand::executeList)
                                        )
                                )
                        )
        );
    }

    private static int executeAdd(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        String entityId = getHeldEntityId(player);
        if (entityId == null) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.wetness.blacklist.no_egg")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        List<String> list = new ArrayList<>(ElementalFireNatureReactionsConfig.WETNESS_ENTITY_BLACKLIST.get());

        if (list.contains(entityId)) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.wetness.blacklist.already_exists", entityId)
                    .withStyle(ChatFormatting.YELLOW));
            return 0;
        }

        list.add(entityId);
        ElementalFireNatureReactionsConfig.WETNESS_ENTITY_BLACKLIST.set(list);
        ElementalFireNatureReactionsConfig.SPEC.save();
        ElementalFireNatureReactionsConfig.refreshCache();

        player.sendSystemMessage(Component.translatable("command.elementalcraft.wetness.blacklist.added", entityId)
                .withStyle(ChatFormatting.GREEN));
        player.sendSystemMessage(Component.translatable("command.elementalcraft.wetness.blacklist.saved")
                .withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static int executeRemove(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        String entityId = getHeldEntityId(player);
        if (entityId == null) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.wetness.blacklist.no_egg")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        List<String> list = new ArrayList<>(ElementalFireNatureReactionsConfig.WETNESS_ENTITY_BLACKLIST.get());

        if (list.remove(entityId)) {
            ElementalFireNatureReactionsConfig.WETNESS_ENTITY_BLACKLIST.set(list);
            ElementalFireNatureReactionsConfig.SPEC.save();
            ElementalFireNatureReactionsConfig.refreshCache();

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

    private static int executeList(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        List<? extends String> list = ElementalFireNatureReactionsConfig.WETNESS_ENTITY_BLACKLIST.get();

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