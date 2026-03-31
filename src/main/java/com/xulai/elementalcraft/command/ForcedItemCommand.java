package com.xulai.elementalcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.xulai.elementalcraft.config.ForcedItemConfig;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ForcedItemHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = "elementalcraft")
@SuppressWarnings("null")
public class ForcedItemCommand {

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("elementalcraft")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("item")
                        .then(Commands.literal("weapon")
                                .then(Commands.literal("add")
                                        .then(Commands.argument("element", StringArgumentType.word())
                                                .suggests((ctx, builder) -> {
                                                    for (ElementType type : ElementType.values()) {
                                                        if (type != ElementType.NONE) builder.suggest(type.getId());
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .executes(ctx -> addWeaponForced(ctx, StringArgumentType.getString(ctx, "element")))
                                        )
                                )
                                .then(Commands.literal("remove")
                                        .executes(ForcedItemCommand::removeWeaponForced)
                                )
                        )

                        .then(Commands.literal("armor")
                                .then(Commands.literal("add")
                                        .then(Commands.argument("enhance_element", StringArgumentType.word())
                                                .suggests((ctx, builder) -> {
                                                    builder.suggest("");
                                                    for (ElementType type : ElementType.values()) {
                                                        if (type != ElementType.NONE) builder.suggest(type.getId());
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .then(Commands.argument("enhance_points", StringArgumentType.string())
                                                        .then(Commands.argument("resist_element", StringArgumentType.word())
                                                                .suggests((ctx, builder) -> {
                                                                    builder.suggest("");
                                                                    for (ElementType type : ElementType.values()) {
                                                                        if (type != ElementType.NONE) builder.suggest(type.getId());
                                                                    }
                                                                    return builder.buildFuture();
                                                                })
                                                                .then(Commands.argument("resist_points", StringArgumentType.string())
                                                                        .executes(ctx -> addArmorForced(ctx,
                                                                                getStr(ctx, "enhance_element"),
                                                                                getStr(ctx, "enhance_points"),
                                                                                getStr(ctx, "resist_element"),
                                                                                getStr(ctx, "resist_points")))
                                                                )
                                                                .executes(ctx -> addArmorForced(ctx,
                                                                        getStr(ctx, "enhance_element"),
                                                                        getStr(ctx, "enhance_points"),
                                                                        getStr(ctx, "resist_element"),
                                                                        "0"))
                                                        )
                                                        .executes(ctx -> addArmorForced(ctx,
                                                                getStr(ctx, "enhance_element"),
                                                                getStr(ctx, "enhance_points"),
                                                                "", "0"))
                                                )
                                                .executes(ctx -> addArmorForced(ctx,
                                                        getStr(ctx, "enhance_element"),
                                                        "0", "", "0"))
                                        )
                                )
                                .then(Commands.literal("remove")
                                        .executes(ForcedItemCommand::removeArmorForced)
                                )
                        )
                )
        );
    }

    private static String getStr(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            return ctx.getArgument(name, String.class).trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static ElementType parse(String input) {
        if (input == null || input.isBlank()) return null;
        return ElementType.fromId(input.toLowerCase());
    }

    private static String normalizeLine(String line) {
        String trimmed = line.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private static int addWeaponForced(CommandContext<CommandSourceStack> ctx, String elementRaw) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.translatable("command.elementalcraft.player_only"));
            return 0;
        }

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forceditem.no_item"));
            return 0;
        }

        var modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
        boolean hasDamage = modifiers.containsKey(Attributes.ATTACK_DAMAGE);
        boolean hasSpeed = modifiers.containsKey(Attributes.ATTACK_SPEED);
        if (!hasDamage || !hasSpeed) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forceditem.weapon.no_attack_attributes"));
            return 0;
        }

        ElementType type = parse(elementRaw);
        if (type == null || type == ElementType.NONE) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forceditem.invalid_element", elementRaw));
            return 0;
        }

        String itemId = stack.getItem().builtInRegistryHolder().key().location().toString();

        List<String> currentWeapons = new ArrayList<>(ForcedItemConfig.FORCED_WEAPONS.get());
        boolean alreadyExists = currentWeapons.stream()
                .anyMatch(s -> normalizeLine(s).startsWith(itemId + ","));

        if (alreadyExists) {
            MutableComponent msg = Component.translatable("command.elementalcraft.forceditem.weapon.already_exists", stack.getHoverName());
            player.sendSystemMessage(msg);
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forceditem.use_remove_first"));
            return 0;
        }

        String newLine = itemId + "," + type.getId();

        List<String> list = new ArrayList<>(ForcedItemConfig.FORCED_WEAPONS.get());
        list.add(newLine);
        ForcedItemConfig.FORCED_WEAPONS.set(list);
        ForcedItemConfig.SPEC.save();

        ForcedItemHelper.clearCache();

        player.sendSystemMessage(Component.translatable("command.elementalcraft.forceditem.weapon.add_success", stack.getHoverName(), type.getDisplayName()));
        player.sendSystemMessage(Component.literal("§7" + newLine));
        player.sendSystemMessage(Component.translatable("command.elementalcraft.forceditem.saved"));

        return 1;
    }

    private static int removeWeaponForced(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.translatable("command.elementalcraft.player_only"));
            return 0;
        }

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forceditem.no_item"));
            return 0;
        }

        String itemId = stack.getItem().builtInRegistryHolder().key().location().toString();
        String targetPrefix = itemId + ",";

        List<String> list = new ArrayList<>(ForcedItemConfig.FORCED_WEAPONS.get());
        int oldSize = list.size();
        list.removeIf(s -> normalizeLine(s).startsWith(targetPrefix));
        int removed = oldSize - list.size();

        ForcedItemConfig.FORCED_WEAPONS.set(list);
        ForcedItemConfig.SPEC.save();
        ForcedItemHelper.clearCache();

        if (removed > 0) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forceditem.weapon.remove_success", stack.getHoverName()));
        } else {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forceditem.weapon.not_found", stack.getHoverName()));
        }
        player.sendSystemMessage(Component.translatable("command.elementalcraft.forceditem.saved"));
        return 1;
    }

    private static int addArmorForced(CommandContext<CommandSourceStack> ctx,
                                      String enhanceRaw, String enhanceInput,
                                      String resistRaw, String resistInput) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.translatable("command.elementalcraft.player_only"));
            return 0;
        }

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forceditem.no_item"));
            return 0;
        }

        if (!(stack.getItem() instanceof ArmorItem)) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forceditem.armor.no_armor_value"));
            return 0;
        }

        ElementType enhance = parse(enhanceRaw);
        ElementType resist = parse(resistRaw);

        String enhanceStr = enhanceInput.isBlank() ? "0" : enhanceInput.trim();
        String resistStr = resistInput.isBlank() ? "0" : resistInput.trim();

        String itemId = stack.getItem().builtInRegistryHolder().key().location().toString();

        List<String> currentArmor = new ArrayList<>(ForcedItemConfig.FORCED_ARMOR.get());
        boolean alreadyExists = currentArmor.stream()
                .anyMatch(s -> normalizeLine(s).startsWith(itemId + ","));

        if (alreadyExists) {
            MutableComponent msg = Component.translatable("command.elementalcraft.forceditem.armor.already_exists", stack.getHoverName());
            player.sendSystemMessage(msg);
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forceditem.use_remove_first"));
            return 0;
        }

        String newLine = itemId + "," +
                (enhance != null ? enhance.getId() : "") + "," +
                enhanceStr + "," +
                (resist != null ? resist.getId() : "") + "," +
                resistStr;

        List<String> list = new ArrayList<>(ForcedItemConfig.FORCED_ARMOR.get());
        list.add(newLine);
        ForcedItemConfig.FORCED_ARMOR.set(list);
        ForcedItemConfig.SPEC.save();

        ForcedItemHelper.clearCache();

        player.sendSystemMessage(Component.translatable("command.elementalcraft.forceditem.armor.add_success", stack.getHoverName()));
        player.sendSystemMessage(Component.literal("§7" + newLine));
        player.sendSystemMessage(Component.translatable("command.elementalcraft.forceditem.saved"));

        return 1;
    }

    private static int removeArmorForced(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(Component.translatable("command.elementalcraft.player_only"));
            return 0;
        }

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forceditem.no_item"));
            return 0;
        }

        String itemId = stack.getItem().builtInRegistryHolder().key().location().toString();
        String targetPrefix = itemId + ",";

        List<String> list = new ArrayList<>(ForcedItemConfig.FORCED_ARMOR.get());
        int oldSize = list.size();
        list.removeIf(s -> normalizeLine(s).startsWith(targetPrefix));
        int removed = oldSize - list.size();

        ForcedItemConfig.FORCED_ARMOR.set(list);
        ForcedItemConfig.SPEC.save();
        ForcedItemHelper.clearCache();

        if (removed > 0) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forceditem.armor.remove_success", stack.getHoverName()));
        } else {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forceditem.armor.not_found", stack.getHoverName()));
        }
        player.sendSystemMessage(Component.translatable("command.elementalcraft.forceditem.saved"));
        return 1;
    }
}