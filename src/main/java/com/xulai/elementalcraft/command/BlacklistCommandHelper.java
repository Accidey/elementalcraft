package com.xulai.elementalcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class BlacklistCommandHelper {

    public static class BlacklistEntry {
        public final String commandName;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> configValue;
        public final ForgeConfigSpec spec;
        public final Supplier<List<? extends String>> cachedListSupplier;
        public final Runnable refreshCache;
        public final String translationPrefix;

        public BlacklistEntry(String commandName,
                              ForgeConfigSpec.ConfigValue<List<? extends String>> configValue,
                              ForgeConfigSpec spec,
                              Supplier<List<? extends String>> cachedListSupplier,
                              Runnable refreshCache,
                              String translationPrefix) {
            this.commandName = commandName;
            this.configValue = configValue;
            this.spec = spec;
            this.cachedListSupplier = cachedListSupplier;
            this.refreshCache = refreshCache;
            this.translationPrefix = translationPrefix;
        }
    }

    public static void registerAll(CommandDispatcher<CommandSourceStack> dispatcher) {
        List<BlacklistEntry> entries = List.of(
                new BlacklistEntry("frostbite",
                        ElementalThunderFrostReactionsConfig.FROSTBITE_IMMUNITY_BLACKLIST,
                        ElementalThunderFrostReactionsConfig.SPEC,
                        () -> ElementalThunderFrostReactionsConfig.cachedFrostbiteImmunityBlacklist,
                        ElementalThunderFrostReactionsConfig::refreshCache,
                        "command.elementalcraft.frostbite.blacklist"),
                new BlacklistEntry("paralysis",
                        ElementalThunderFrostReactionsConfig.PARALYSIS_IMMUNITY_BLACKLIST,
                        ElementalThunderFrostReactionsConfig.SPEC,
                        () -> ElementalThunderFrostReactionsConfig.cachedParalysisImmunityBlacklist,
                        ElementalThunderFrostReactionsConfig::refreshCache,
                        "command.elementalcraft.paralysis.blacklist"),
                new BlacklistEntry("static",
                        ElementalThunderFrostReactionsConfig.STATIC_IMMUNITY_BLACKLIST,
                        ElementalThunderFrostReactionsConfig.SPEC,
                        () -> ElementalThunderFrostReactionsConfig.cachedStaticImmunityBlacklist,
                        ElementalThunderFrostReactionsConfig::refreshCache,
                        "command.elementalcraft.static_immunity.blacklist"),
                new BlacklistEntry("scorched",
                        ElementalFireNatureReactionsConfig.SCORCHED_ENTITY_BLACKLIST,
                        ElementalFireNatureReactionsConfig.SPEC,
                        () -> ElementalFireNatureReactionsConfig.cachedScorchedBlacklist,
                        ElementalFireNatureReactionsConfig::refreshCache,
                        "command.elementalcraft.scorched.blacklist"),
                new BlacklistEntry("spore",
                        ElementalFireNatureReactionsConfig.SPORE_ENTITY_BLACKLIST,
                        ElementalFireNatureReactionsConfig.SPEC,
                        () -> ElementalFireNatureReactionsConfig.cachedSporeBlacklist,
                        ElementalFireNatureReactionsConfig::refreshCache,
                        "command.elementalcraft.spore.blacklist"),
                new BlacklistEntry("steam",
                        ElementalFireNatureReactionsConfig.STEAM_IMMUNITY_BLACKLIST,
                        ElementalFireNatureReactionsConfig.SPEC,
                        () -> ElementalFireNatureReactionsConfig.cachedSteamBlacklist,
                        ElementalFireNatureReactionsConfig::refreshCache,
                        "command.elementalcraft.steam.blacklist"),
                new BlacklistEntry("wetness",
                        ElementalFireNatureReactionsConfig.WETNESS_ENTITY_BLACKLIST,
                        ElementalFireNatureReactionsConfig.SPEC,
                        () -> ElementalFireNatureReactionsConfig.cachedWetnessBlacklist,
                        ElementalFireNatureReactionsConfig::refreshCache,
                        "command.elementalcraft.wetness.blacklist"),
                new BlacklistEntry("freeze",
                        ElementalThunderFrostReactionsConfig.FREEZE_IMMUNITY_BLACKLIST,
                        ElementalThunderFrostReactionsConfig.SPEC,
                        () -> ElementalThunderFrostReactionsConfig.cachedFreezeImmunityBlacklist,
                        ElementalThunderFrostReactionsConfig::refreshCache,
                        "command.elementalcraft.freeze.blacklist")
        );

        for (BlacklistEntry entry : entries) {
            registerBlacklist(dispatcher, entry);
        }
    }

    private static void registerBlacklist(CommandDispatcher<CommandSourceStack> dispatcher, BlacklistEntry entry) {
        dispatcher.register(Commands.literal("elementalcraft")
                .then(Commands.literal("blacklist")
                        .then(Commands.literal(entry.commandName)
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.literal("add")
                                        .executes(ctx -> addEntity(ctx, entry)))
                                .then(Commands.literal("remove")
                                        .executes(ctx -> removeEntity(ctx, entry)))
                                .then(Commands.literal("list")
                                        .executes(ctx -> listEntities(ctx, entry)))
                        )
                )
        );
    }

    private static int addEntity(CommandContext<CommandSourceStack> context, BlacklistEntry entry) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        String entityId = getHeldEntityId(player);
        if (entityId == null) {
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.common.no_egg"));
            return 0;
        }

        List<String> currentList = new ArrayList<>(entry.configValue.get());

        if (currentList.contains(entityId)) {
            context.getSource().sendFailure(Component.translatable(entry.translationPrefix + ".already_exists", entityId));
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.blacklist.use_remove_first"));
            return 0;
        }

        currentList.add(entityId);
        entry.configValue.set(currentList);
        entry.spec.save();
        entry.refreshCache.run();

        context.getSource().sendSuccess(() -> Component.translatable(entry.translationPrefix + ".added", entityId)
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int removeEntity(CommandContext<CommandSourceStack> context, BlacklistEntry entry) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) return 0;

        String entityId = getHeldEntityId(player);
        if (entityId == null) {
            context.getSource().sendFailure(Component.translatable("command.elementalcraft.common.no_egg"));
            return 0;
        }

        List<String> currentList = new ArrayList<>(entry.configValue.get());

        if (!currentList.contains(entityId)) {
            context.getSource().sendFailure(Component.translatable(entry.translationPrefix + ".not_found", entityId));
            return 0;
        }

        currentList.remove(entityId);
        entry.configValue.set(currentList);
        entry.spec.save();
        entry.refreshCache.run();

        context.getSource().sendSuccess(() -> Component.translatable(entry.translationPrefix + ".removed", entityId)
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int listEntities(CommandContext<CommandSourceStack> context, BlacklistEntry entry) {
        List<? extends String> list = entry.cachedListSupplier.get();
        CommandSourceStack source = context.getSource();

        if (list == null || list.isEmpty()) {
            source.sendSuccess(() -> Component.translatable(entry.translationPrefix + ".empty")
                    .withStyle(ChatFormatting.GRAY), false);
        } else {
            source.sendSuccess(() -> Component.translatable(entry.translationPrefix + ".header", list.size())
                    .withStyle(ChatFormatting.GOLD), false);
            for (String e : list) {
                source.sendSuccess(() -> Component.literal(" - " + e).withStyle(ChatFormatting.WHITE), false);
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