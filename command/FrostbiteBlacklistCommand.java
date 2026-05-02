package com.xulai.elementalcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class FrostbiteBlacklistCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("elementalcraft")
         .then(Commands.literal("blacklist")
                .then(Commands.literal("frostbite")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.literal("add")
                                        .executes(FrostbiteBlacklistCommand::addEntity))
                                .then(Commands.literal("remove")
                                        .executes(FrostbiteBlacklistCommand::removeEntity))
                                .then(Commands.literal("list")
                                        .executes(FrostbiteBlacklistCommand::listEntities))
                        )
                )
        );
    }

    private static int addEntity(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        String entityId = getHeldEntityId(player);
        if (entityId == null) {
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.frostbite.blacklist.no_egg"));
            return 0;
        }

        List<String> currentList = new ArrayList<>(ElementalThunderFrostReactionsConfig.FROSTBITE_IMMUNITY_BLACKLIST.get());

        if (currentList.contains(entityId)) {
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.frostbite.blacklist.already_exists", entityId));
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.blacklist.use_remove_first"));
            return 0;
        }

        currentList.add(entityId);
        ElementalThunderFrostReactionsConfig.FROSTBITE_IMMUNITY_BLACKLIST.set(currentList);
        ElementalThunderFrostReactionsConfig.SPEC.save();
        ElementalThunderFrostReactionsConfig.refreshCache();

        context.getSource().sendSuccess(() -> Component.translatable("command.elementalcraft.frostbite.blacklist.added", entityId)
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int removeEntity(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        String entityId = getHeldEntityId(player);
        if (entityId == null) {
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.frostbite.blacklist.no_egg"));
            return 0;
        }

        List<String> currentList = new ArrayList<>(ElementalThunderFrostReactionsConfig.FROSTBITE_IMMUNITY_BLACKLIST.get());

        if (!currentList.contains(entityId)) {
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.frostbite.blacklist.not_found", entityId));
            return 0;
        }

        currentList.remove(entityId);
        ElementalThunderFrostReactionsConfig.FROSTBITE_IMMUNITY_BLACKLIST.set(currentList);
        ElementalThunderFrostReactionsConfig.SPEC.save();
        ElementalThunderFrostReactionsConfig.refreshCache();

        context.getSource().sendSuccess(() -> Component.translatable("command.elementalcraft.frostbite.blacklist.removed", entityId)
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int listEntities(CommandContext<CommandSourceStack> context) {
        List<? extends String> list = ElementalThunderFrostReactionsConfig.cachedFrostbiteImmunityBlacklist;
        CommandSourceStack source = context.getSource();

        if (list == null || list.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.elementalcraft.frostbite.blacklist.empty")
                    .withStyle(ChatFormatting.GRAY), false);
        } else {
            source.sendSuccess(() -> Component.translatable("command.elementalcraft.frostbite.blacklist.header", list.size())
                    .withStyle(ChatFormatting.GOLD), false);
            for (String entry : list) {
                source.sendSuccess(() -> Component.literal(" - " + entry).withStyle(ChatFormatting.WHITE), false);
            }
        }
        return list == null ? 0 : list.size();
    }

    private static String getHeldEntityId(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof SpawnEggItem egg)) {
            return null;
        }
        var type = egg.getType(stack.getTag());
        return ForgeRegistries.ENTITY_TYPES.getKey(type).toString();
    }
}