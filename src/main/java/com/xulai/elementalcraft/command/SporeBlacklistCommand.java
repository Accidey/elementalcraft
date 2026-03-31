package com.xulai.elementalcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
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

public class SporeBlacklistCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("elementalcraft")
        .then(Commands.literal("blacklist")
                .then(Commands.literal("spore")                        
                                .requires(source -> source.hasPermission(2)) 
                                .then(Commands.literal("add")
                                        .executes(SporeBlacklistCommand::addEntity))
                                .then(Commands.literal("remove")
                                        .executes(SporeBlacklistCommand::removeEntity))
                                .then(Commands.literal("list")
                                        .executes(SporeBlacklistCommand::listEntities))
                        )
                )
        );
    }

    private static int addEntity(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        String entityId = getHeldEntityId(player);
        if (entityId == null) {
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.spore.blacklist.no_egg"));
            return 0;
        }

        List<String> currentList = new ArrayList<>(ElementalFireNatureReactionsConfig.SPORE_ENTITY_BLACKLIST.get());
        
        if (currentList.contains(entityId)) {
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.spore.blacklist.already_exists", entityId));
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.blacklist.use_remove_first"));
            return 0;
        }

        currentList.add(entityId);
        ElementalFireNatureReactionsConfig.SPORE_ENTITY_BLACKLIST.set(currentList);
        ElementalFireNatureReactionsConfig.SPEC.save();
        ElementalFireNatureReactionsConfig.refreshCache();

        context.getSource().sendSuccess(() -> Component.translatable("command.elementalcraft.spore.blacklist.added", entityId)
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int removeEntity(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        String entityId = getHeldEntityId(player);
        if (entityId == null) {
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.spore.blacklist.no_egg"));
            return 0;
        }

        List<String> currentList = new ArrayList<>(ElementalFireNatureReactionsConfig.SPORE_ENTITY_BLACKLIST.get());

        if (!currentList.contains(entityId)) {
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.spore.blacklist.not_found", entityId));
            return 0;
        }

        currentList.remove(entityId);
        
        ElementalFireNatureReactionsConfig.SPORE_ENTITY_BLACKLIST.set(currentList);
        ElementalFireNatureReactionsConfig.SPEC.save();
        ElementalFireNatureReactionsConfig.refreshCache();

        context.getSource().sendSuccess(() -> Component.translatable("command.elementalcraft.spore.blacklist.removed", entityId)
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int listEntities(CommandContext<CommandSourceStack> context) {
        List<? extends String> list = ElementalFireNatureReactionsConfig.cachedSporeBlacklist;
        CommandSourceStack source = context.getSource();

        if (list.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.elementalcraft.spore.blacklist.empty")
                    .withStyle(ChatFormatting.GRAY), false);
        } else {
            source.sendSuccess(() -> Component.translatable("command.elementalcraft.spore.blacklist.header", list.size())
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