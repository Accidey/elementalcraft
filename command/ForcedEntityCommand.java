package com.xulai.elementalcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ForcedAttributeHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
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

@Mod.EventBusSubscriber(modid = "elementalcraft")
public class ForcedEntityCommand {

    private static final String[] ELEMENTS = {"none", "fire", "frost", "thunder", "nature"};
    private static final String[] FIXED = {"50", "100", "0-100", "20-80", "50-100"};
    private static final String[] RANGE = {"50", "100", "0-100", "20-80", "50-100"};

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_ELEMENT =
            (ctx, builder) -> SharedSuggestionProvider.suggest(ELEMENTS, builder);

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_POINTS =
            (ctx, builder) -> {
                try {
                    String input = ctx.getInput();
                    String last = input.substring(input.lastIndexOf(' ') + 1);
                    if (last.contains("-") || last.isEmpty()) {
                        return SharedSuggestionProvider.suggest(RANGE, builder);
                    } else {
                        return SharedSuggestionProvider.suggest(FIXED, builder);
                    }
                } catch (Exception e) {
                    return SharedSuggestionProvider.suggest(FIXED, builder);
                }
            };

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("elementalcraft")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("entity")
                        .then(Commands.literal("add")
                                .then(Commands.argument("attack_element", StringArgumentType.string())
                                        .suggests(SUGGEST_ELEMENT)
                                        .then(Commands.argument("enhance_element", StringArgumentType.string())
                                                .suggests(SUGGEST_ELEMENT)
                                                .then(Commands.argument("enhance_points", StringArgumentType.string())
                                                        .suggests(SUGGEST_POINTS)
                                                        .then(Commands.argument("resist_element", StringArgumentType.string())
                                                                .suggests(SUGGEST_ELEMENT)
                                                                .then(Commands.argument("resist_points", StringArgumentType.word())
                                                                        .suggests(SUGGEST_POINTS)
                                                                        .executes(ctx -> executeEntityAdd(
                                                                                ctx,
                                                                                getStr(ctx, "attack_element"),
                                                                                getStr(ctx, "enhance_element"),
                                                                                getStr(ctx, "enhance_points"),
                                                                                getStr(ctx, "resist_element"),
                                                                                getStr(ctx, "resist_points")
                                                                        ))
                                                                )
                                                                .executes(ctx -> executeEntityAdd(
                                                                        ctx,
                                                                        getStr(ctx, "attack_element"),
                                                                        getStr(ctx, "enhance_element"),
                                                                        getStr(ctx, "enhance_points"),
                                                                        getStr(ctx, "resist_element"),
                                                                        "0"
                                                                ))
                                                        )
                                                        .executes(ctx -> executeEntityAdd(
                                                                ctx,
                                                                getStr(ctx, "attack_element"),
                                                                getStr(ctx, "enhance_element"),
                                                                getStr(ctx, "enhance_points"),
                                                                "",
                                                                "0"
                                                        ))
                                                )
                                                .executes(ctx -> executeEntityAdd(
                                                        ctx,
                                                        getStr(ctx, "attack_element"),
                                                        getStr(ctx, "enhance_element"),
                                                        "0",
                                                        "",
                                                        "0"
                                                ))
                                        )
                                        .executes(ctx -> executeEntityAdd(
                                                ctx,
                                                getStr(ctx, "attack_element"),
                                                "",
                                                "0",
                                                "",
                                                "0"
                                        ))
                                )
                                .executes(ctx -> executeEntityAdd(ctx, "", "", "0", "", "0"))
                        )
                        .then(Commands.literal("remove")
                                .executes(ctx -> executeEntityRemove(ctx.getSource()))
                        )
                        .then(Commands.literal("blacklist")
                                .then(Commands.literal("add")
                                        .then(Commands.argument("element", StringArgumentType.word())
                                                .suggests((ctx, builder) -> {
                                                    builder.suggest("fire").suggest("frost").suggest("thunder").suggest("nature").suggest("all");
                                                    return builder.buildFuture();
                                                })
                                                .executes(ctx -> executeBlacklistAdd(ctx, StringArgumentType.getString(ctx, "element")))
                                        )
                                )
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("element", StringArgumentType.word())
                                                .suggests((ctx, builder) -> {
                                                    ServerPlayer player = ctx.getSource().getPlayer();
                                                    if (player == null) return builder.buildFuture();
                                                    String entityId = getHeldEntityId(player);
                                                    if (entityId == null) return builder.buildFuture();
                                                    for (String entry : ElementalConfig.BLACKLISTED_ENTITIES.get()) {
                                                        if (entry.startsWith(entityId + ":")) {
                                                            String elem = entry.substring(entityId.length() + 1);
                                                            builder.suggest(elem);
                                                        }
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .executes(ctx -> executeBlacklistRemove(ctx, StringArgumentType.getString(ctx, "element")))
                                        )
                                )
                                .then(Commands.literal("list")
                                        .executes(ForcedEntityCommand::executeBlacklistList)
                                )
                        )
                )
        );
    }

    private static int executeEntityAdd(
            CommandContext<CommandSourceStack> ctx,
            String attackRaw,
            String enhanceRaw,
            String enhanceInput,
            String resistRaw,
            String resistInput
    ) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.translatable("command.elementalcraft.player_only"));
            return 0;
        }

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof SpawnEggItem)) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forcedadd.no_egg"));
            return 0;
        }

        String entityKey = getHeldEntityId(player);
        if (entityKey == null) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forcedadd.no_egg"));
            return 0;
        }

        ElementType attack = parseElement(attackRaw);
        ElementType enhance = parseElement(enhanceRaw);
        ElementType resist = parseElement(resistRaw);

        String enhanceStr = enhanceInput.isBlank() ? "0" : enhanceInput.trim();
        String resistStr = resistInput.isBlank() ? "0" : resistInput.trim();

        String newLine = String.format(
                "%s,%s,%s,%s,%s,%s",
                entityKey,
                attack != ElementType.NONE ? attack.getId() : "",
                enhance != ElementType.NONE ? enhance.getId() : "",
                enhanceStr,
                resist != ElementType.NONE ? resist.getId() : "",
                resistStr
        );

        List<String> list = new ArrayList<>(ElementalConfig.FORCED_ENTITIES.get());

        boolean duplicate = list.stream()
                .map(s -> s.replace("\"", "").trim())
                .anyMatch(existing -> existing.equals(newLine));
        if (duplicate) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forcedadd.duplicate"));
            return 0;
        }

        list.add(newLine);
        try {
            ElementalConfig.FORCED_ENTITIES.set(list);
            ElementalConfig.SPEC.save();
        } catch (Exception e) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.save_failed"));
            return 0;
        }

        ForcedAttributeHelper.clearCache();
        player.sendSystemMessage(Component.translatable("command.elementalcraft.forcedadd.success", entityKey));
        player.sendSystemMessage(Component.translatable("command.elementalcraft.forcedadd.saved"));
        return 1;
    }

    private static int executeEntityRemove(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.elementalcraft.player_only"));
            return 0;
        }

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof SpawnEggItem)) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forcedremove.no_egg"));
            return 0;
        }

        String entityKey = getHeldEntityId(player);
        if (entityKey == null) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forcedremove.no_egg"));
            return 0;
        }

        List<String> list = new ArrayList<>(ElementalConfig.FORCED_ENTITIES.get());
        int oldSize = list.size();
        list.removeIf(s -> s.replace("\"", "").trim().startsWith(entityKey + ","));
        int removed = oldSize - list.size();

        try {
            ElementalConfig.FORCED_ENTITIES.set(list);
            ElementalConfig.SPEC.save();
        } catch (Exception e) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.save_failed"));
            return 0;
        }

        ForcedAttributeHelper.clearCache();

        if (removed > 0) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forcedremove.success", entityKey));
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forcedremove.saved"));
        } else {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forcedremove.not_found", entityKey));
        }
        return 1;
    }

    private static int executeBlacklistAdd(CommandContext<CommandSourceStack> ctx, String elementStr) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.elementalcraft.player_only"));
            return 0;
        }

        String entityId = getHeldEntityId(player);
        if (entityId == null) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.blacklist.no_egg"));
            return 0;
        }

        boolean isAll = elementStr.equalsIgnoreCase("all");
        ElementType type = isAll ? null : ElementType.fromId(elementStr.toLowerCase());

        if (!isAll && (type == null || type == ElementType.NONE)) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.blacklist.invalid_attribute", elementStr));
            return 0;
        }

        String targetEntry = isAll ? entityId + ":all" : entityId + ":" + type.getId();

        List<String> list = new ArrayList<>(ElementalConfig.BLACKLISTED_ENTITIES.get());

        boolean alreadyExists = list.stream().anyMatch(e -> {
            if (e.equals(targetEntry)) return true;
            if (e.equals(entityId + ":all")) return true;
            if (targetEntry.endsWith(":all") && e.startsWith(entityId + ":")) return true;
            return false;
        });

        if (alreadyExists) {
            player.sendSystemMessage(Component.translatable(
                    "command.elementalcraft.blacklist.already_exists",
                    entityId,
                    isAll ? "ALL" : type.getDisplayName().getString()
            ));
            player.sendSystemMessage(Component.translatable("command.elementalcraft.blacklist.use_remove_first"));
            return 0;
        }

        list.add(targetEntry);
        try {
            ElementalConfig.BLACKLISTED_ENTITIES.set(list);
            ElementalConfig.SPEC.save();
        } catch (Exception e) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.save_failed"));
            return 0;
        }

        if (isAll) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.blacklist.added_all", entityId));
        } else {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.blacklist.added_single", type.getDisplayName(), entityId));
        }
        player.sendSystemMessage(Component.translatable("command.elementalcraft.blacklist.saved"));
        return 1;
    }

    private static int executeBlacklistRemove(CommandContext<CommandSourceStack> ctx, String elementStr) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.elementalcraft.player_only"));
            return 0;
        }

        String entityId = getHeldEntityId(player);
        if (entityId == null) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.blacklist.no_egg"));
            return 0;
        }

        boolean isAll = elementStr.equalsIgnoreCase("all");
        ElementType type = isAll ? null : ElementType.fromId(elementStr.toLowerCase());

        if (!isAll && (type == null || type == ElementType.NONE)) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.blacklist.invalid_attribute", elementStr));
            return 0;
        }

        List<String> list = new ArrayList<>(ElementalConfig.BLACKLISTED_ENTITIES.get());
        boolean removed;

        if (isAll) {
            removed = list.removeIf(e -> e.startsWith(entityId + ":"));
        } else {
            String exactEntry = entityId + ":" + type.getId();
            removed = list.remove(exactEntry);
        }

        if (!removed) {
            if (isAll) {
                player.sendSystemMessage(Component.translatable("command.elementalcraft.blacklist.not_found_any", entityId));
            } else {
                player.sendSystemMessage(Component.translatable("command.elementalcraft.blacklist.not_found_single", entityId, elementStr));
            }
            return 0;
        }

        try {
            ElementalConfig.BLACKLISTED_ENTITIES.set(list);
            ElementalConfig.SPEC.save();
        } catch (Exception e) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.save_failed"));
            return 0;
        }

        if (isAll) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.blacklist.removed_all", entityId));
        } else {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.blacklist.removed_single", type.getDisplayName(), entityId));
        }
        player.sendSystemMessage(Component.translatable("command.elementalcraft.blacklist.saved"));
        return 1;
    }

    private static int executeBlacklistList(CommandContext<CommandSourceStack> ctx) {
        List<? extends String> list = ElementalConfig.BLACKLISTED_ENTITIES.get();
        CommandSourceStack source = ctx.getSource();

        if (list.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.elementalcraft.blacklist.empty"), false);
        } else {
            source.sendSuccess(() -> Component.translatable("command.elementalcraft.blacklist.header", list.size()), false);
            for (String entry : list) {
                source.sendSuccess(() -> Component.literal(" §7- §f" + entry), false);
            }
        }
        return list.size();
    }

    private static String getStr(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            return ctx.getArgument(name, String.class).trim();
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    private static ElementType parseElement(String input) {
        if (input == null || input.isBlank() || "none".equalsIgnoreCase(input)) {
            return ElementType.NONE;
        }
        ElementType type = ElementType.fromId(input.toLowerCase());
        return type != null ? type : ElementType.NONE;
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