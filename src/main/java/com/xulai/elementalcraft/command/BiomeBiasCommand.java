package com.xulai.elementalcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.util.CustomBiomeBias;
import com.xulai.elementalcraft.util.ElementType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BiomeBiasCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("elementalcraft")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("biome")
                                .then(Commands.literal("add")
                                        .then(Commands.argument("element", StringArgumentType.word())
                                                .suggests((ctx, builder) -> {
                                                    builder.suggest("fire").suggest("frost").suggest("thunder").suggest("nature").suggest("all");
                                                    return builder.buildFuture();
                                                })
                                                .then(Commands.argument("probability", DoubleArgumentType.doubleArg(0.0, 100.0))
                                                        .executes(ctx -> addBiomeBias(
                                                                ctx,
                                                                StringArgumentType.getString(ctx, "element"),
                                                                DoubleArgumentType.getDouble(ctx, "probability")
                                                        ))
                                                )
                                        )
                                )
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("element", StringArgumentType.word())
                                                .suggests((ctx, builder) -> {
                                                    ServerPlayer player = ctx.getSource().getPlayer();
                                                    if (player == null) return builder.buildFuture();

                                                    String biomeId = getCurrentBiomeId(player);
                                                    if (biomeId == null) return builder.buildFuture();

                                                    List<? extends String> lines = ElementalConfig.CUSTOM_BIOME_ATTRIBUTE_BIAS.get();
                                                    for (String line : lines) {
                                                        String trimmed = line.trim();
                                                        if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                                                        if (trimmed.startsWith(biomeId + ":")) {
                                                            String elem = trimmed.substring(biomeId.length() + 1).split(",")[0];
                                                            builder.suggest(elem);
                                                        }
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .executes(ctx -> removeBiomeBias(
                                                        ctx,
                                                        StringArgumentType.getString(ctx, "element")
                                                ))
                                        )
                                )
                                .then(Commands.literal("list")
                                        .executes(BiomeBiasCommand::listBiomeBias)
                                )
                        )
        );
    }

    private static String getCurrentBiomeId(ServerPlayer player) {
        Holder<Biome> biomeHolder = player.serverLevel().getBiome(player.blockPosition());
        ResourceLocation loc = biomeHolder.unwrapKey().orElseThrow().location();
        return loc.toString();
    }

    private static int addBiomeBias(CommandContext<CommandSourceStack> ctx, String elementStr, double probability) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.elementalcraft.player_only"));
            return 0;
        }

        String biomeId = getCurrentBiomeId(player);
        boolean isAll = elementStr.equalsIgnoreCase("all");
        @Nullable ElementType type = isAll ? null : ElementType.fromId(elementStr.toLowerCase());

        if (!isAll && (type == null || type == ElementType.NONE)) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.invalid_attribute", elementStr));
            return 0;
        }

        List<String> currentList = new ArrayList<>(ElementalConfig.CUSTOM_BIOME_ATTRIBUTE_BIAS.get());
        String prefix = biomeId + ":";

        boolean hasAll = currentList.stream().anyMatch(l -> l.trim().startsWith(prefix + "all,"));
        boolean hasTarget = currentList.stream().anyMatch(l -> l.trim().startsWith(prefix + (isAll ? "all," : type.getId() + ",")));

        if (hasAll || hasTarget) {
            if (hasAll && !isAll) {
                player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.conflict_all", biomeId));
            } else {
                player.sendSystemMessage(Component.translatable(
                        "command.elementalcraft.biomebias.already_exists",
                        biomeId,
                        isAll ? "ALL" : type.getDisplayName().getString()
                ));
            }
            player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.use_remove_first"));
            return 0;
        }

        String newEntry = biomeId + ":" + (isAll ? "all" : type.getId()) + "," + String.format("%.1f", probability);
        currentList.add(newEntry);

        ElementalConfig.CUSTOM_BIOME_ATTRIBUTE_BIAS.set(currentList);
        ElementalConfig.SPEC.save();

        CustomBiomeBias.clearCache();

        if (isAll) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.added_all", probability, biomeId));
        } else {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.added_single", type.getDisplayName(), probability, biomeId));
        }
        player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.saved"));

        return 1;
    }

    private static int removeBiomeBias(CommandContext<CommandSourceStack> ctx, String elementStr) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.elementalcraft.player_only"));
            return 0;
        }

        String biomeId = getCurrentBiomeId(player);
        boolean isAll = elementStr.equalsIgnoreCase("all");
        @Nullable ElementType type = isAll ? null : ElementType.fromId(elementStr.toLowerCase());

        if (!isAll && (type == null || type == ElementType.NONE)) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.invalid_attribute", elementStr));
            return 0;
        }

        List<String> currentList = new ArrayList<>(ElementalConfig.CUSTOM_BIOME_ATTRIBUTE_BIAS.get());
        String prefix = biomeId + ":";

        boolean removed = false;

        if (isAll) {
            removed = currentList.removeIf(l -> l.trim().startsWith(prefix));
        } else {
            String target = biomeId + ":" + type.getId() + ",";
            removed = currentList.removeIf(l -> l.trim().startsWith(target));
        }

        if (!removed) {
            if (isAll) {
                player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.not_found_any", biomeId));
            } else {
                player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.not_found_single", biomeId, elementStr));
            }
            return 0;
        }

        ElementalConfig.CUSTOM_BIOME_ATTRIBUTE_BIAS.set(currentList);
        ElementalConfig.SPEC.save();
        CustomBiomeBias.clearCache();

        if (isAll) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.removed_all", biomeId));
        } else {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.removed_single", type.getDisplayName(), biomeId));
        }
        player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.saved"));

        return 1;
    }

    private static int listBiomeBias(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();

        if (player == null) {
            source.sendFailure(Component.translatable("command.elementalcraft.player_only"));
            return 0;
        }

        String biomeId = getCurrentBiomeId(player);
        List<? extends String> allLines = ElementalConfig.CUSTOM_BIOME_ATTRIBUTE_BIAS.get();

        List<String> relevant = new ArrayList<>();
        for (String line : allLines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            if (trimmed.startsWith(biomeId + ":")) {
                relevant.add(trimmed.substring(biomeId.length() + 1)); 
            }
        }

        if (relevant.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.elementalcraft.biomebias.not_found_any", biomeId), false);
        } else {
            source.sendSuccess(() -> Component.translatable("command.elementalcraft.biomebias.header", biomeId, relevant.size()), false);
            for (String entry : relevant) {
                source.sendSuccess(() -> Component.literal(" §7- §f" + entry), false);
            }
        }
        return relevant.size();
    }
}