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

public class StaticImmunityBlacklistCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("elementalcraft")
        .then(Commands.literal("blacklist")
                .then(Commands.literal("static")
                                .requires(source -> source.hasPermission(2)) 
                                .then(Commands.literal("add")
                                        .executes(StaticImmunityBlacklistCommand::addEntity))
                                .then(Commands.literal("remove")
                                        .executes(StaticImmunityBlacklistCommand::removeEntity))
                                .then(Commands.literal("list")
                                        .executes(StaticImmunityBlacklistCommand::listEntities))
                        )
                )
        );
    }

    private static int addEntity(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        String entityId = getHeldEntityId(player);
        if (entityId == null) {
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.static_immunity.blacklist.no_egg"));
            return 0;
        }

        List<String> currentList = new ArrayList<>(ElementalThunderFrostReactionsConfig.STATIC_IMMUNITY_BLACKLIST.get());

        if (currentList.contains(entityId)) {
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.static_immunity.blacklist.already_exists", entityId));
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.blacklist.use_remove_first"));
            return 0;
        }

        currentList.add(entityId);
        ElementalThunderFrostReactionsConfig.STATIC_IMMUNITY_BLACKLIST.set(currentList);
        ElementalThunderFrostReactionsConfig.SPEC.save();
        ElementalThunderFrostReactionsConfig.refreshCache();

        context.getSource().sendSuccess(() -> Component.translatable("command.elementalcraft.static_immunity.blacklist.added", entityId)
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int removeEntity(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        String entityId = getHeldEntityId(player);
        if (entityId == null) {
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.static_immunity.blacklist.no_egg"));
            return 0;
        }

        List<String> currentList = new ArrayList<>(ElementalThunderFrostReactionsConfig.STATIC_IMMUNITY_BLACKLIST.get());

        if (!currentList.contains(entityId)) {
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.static_immunity.blacklist.not_found", entityId));
            return 0;
        }

        currentList.remove(entityId);
        ElementalThunderFrostReactionsConfig.STATIC_IMMUNITY_BLACKLIST.set(currentList);
        ElementalThunderFrostReactionsConfig.SPEC.save();
        ElementalThunderFrostReactionsConfig.refreshCache();

        context.getSource().sendSuccess(() -> Component.translatable("command.elementalcraft.static_immunity.blacklist.removed", entityId)
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int listEntities(CommandContext<CommandSourceStack> context) {
        List<? extends String> list = ElementalThunderFrostReactionsConfig.cachedStaticImmunityBlacklist;
        CommandSourceStack source = context.getSource();

        if (list.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.elementalcraft.static_immunity.blacklist.empty")
                    .withStyle(ChatFormatting.GRAY), false);
        } else {
            source.sendSuccess(() -> Component.translatable("command.elementalcraft.static_immunity.blacklist.header", list.size())
                    .withStyle(ChatFormatting.GOLD), false);
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
        return ForgeRegistries.ENTITY_TYPES.getKey(type).toString();
    }
}