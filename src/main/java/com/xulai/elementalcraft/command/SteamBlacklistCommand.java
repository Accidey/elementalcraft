package com.xulai.elementalcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
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

public class SteamBlacklistCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("elementalcraft")
        .then(Commands.literal("blacklist")
                .then(Commands.literal("steam")
                        .requires(source -> source.hasPermission(2))

                        .then(Commands.literal("add")
                                .executes(SteamBlacklistCommand::addFromHand)
                        )

                        .then(Commands.literal("remove")
                                .executes(SteamBlacklistCommand::removeFromHand)
                        )

                        .then(Commands.literal("list")
                                .executes(SteamBlacklistCommand::listEntities)
                        )
                    )
                )
        );
    }

    private static int addFromHand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack heldItem = player.getMainHandItem();

        if (!(heldItem.getItem() instanceof SpawnEggItem egg)) {
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.steam_blacklist.not_egg"));
            return 0;
        }

        EntityType<?> type = egg.getType(heldItem.getTag());
        ResourceLocation idLoc = ForgeRegistries.ENTITY_TYPES.getKey(type);

        if (idLoc == null) return 0;
        String entityId = idLoc.toString();

        List<String> currentList = new ArrayList<>(ElementalFireNatureReactionsConfig.STEAM_IMMUNITY_BLACKLIST.get());

        if (currentList.contains(entityId)) {
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.steam_blacklist.exists", entityId));
            return 0;
        }

        currentList.add(entityId);
        ElementalFireNatureReactionsConfig.STEAM_IMMUNITY_BLACKLIST.set(currentList);
        ElementalFireNatureReactionsConfig.SPEC.save();
        ElementalFireNatureReactionsConfig.refreshCache();

        context.getSource().sendSuccess(() -> Component.translatable("command.elementalcraft.steam_blacklist.add.success", entityId), true);
        return 1;
    }

    private static int removeFromHand(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ItemStack heldItem = player.getMainHandItem();

        if (!(heldItem.getItem() instanceof SpawnEggItem egg)) {
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.steam_blacklist.not_egg"));
            return 0;
        }

        EntityType<?> type = egg.getType(heldItem.getTag());
        ResourceLocation idLoc = ForgeRegistries.ENTITY_TYPES.getKey(type);

        if (idLoc == null) return 0;
        String entityId = idLoc.toString();

        List<String> currentList = new ArrayList<>(ElementalFireNatureReactionsConfig.STEAM_IMMUNITY_BLACKLIST.get());

        if (!currentList.contains(entityId)) {
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.steam_blacklist.not_found", entityId));
            return 0;
        }

        currentList.remove(entityId);
        ElementalFireNatureReactionsConfig.STEAM_IMMUNITY_BLACKLIST.set(currentList);
        ElementalFireNatureReactionsConfig.SPEC.save();
        ElementalFireNatureReactionsConfig.refreshCache();

        context.getSource().sendSuccess(() -> Component.translatable("command.elementalcraft.steam_blacklist.remove.success", entityId), true);
        return 1;
    }

    private static int listEntities(CommandContext<CommandSourceStack> context) {
        List<? extends String> list = ElementalFireNatureReactionsConfig.cachedSteamBlacklist;

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