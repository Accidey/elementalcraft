// src/main/java/com/xulai/elementalcraft/command/ForcedItemCommand.java
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
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * ForcedItemCommand
 *
 * 中文说明：
 * 负责注册和管理 /elementalcraft forceditem 命令。
 * 该命令允许 OP 玩家通过手持物品为特定物品强制绑定元素属性（武器攻击属性或装备强化/抗性属性），并写入配置文件。
 * 支持热重载：配置保存后会自动清理 ForcedItemHelper 缓存，使变更立即生效。
 *
 * English description:
 * Responsible for registering and handling the /elementalcraft forceditem command.
 * This command allows OP players to force-bind elemental attributes (weapon attack element or armor enhancement/resistance) to specific items by holding them,
 * and writes the configuration to file. Supports hot-reloading: ForcedItemHelper cache is automatically cleared after saving config.
 */
@Mod.EventBusSubscriber(modid = "elementalcraft")
@SuppressWarnings("null")
public class ForcedItemCommand {

    /**
     * 在服务器命令注册事件中注册 /elementalcraft forceditem 命令。
     * 需要 OP 权限（level 2）。
     *
     * Registers the /elementalcraft forceditem command during the server command registration event.
     * Requires OP permission (level 2).
     *
     * @param event 命令注册事件 / Command registration event
     */
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("elementalcraft")
            .requires(source -> source.hasPermission(2)) // 需要 OP 权限 / Requires OP permission
            .then(Commands.literal("forceditem")
                // ===== weapon 子命令：管理武器强制攻击属性 =====
                // ===== weapon subcommand: manage forced attack element for weapons =====
                .then(Commands.literal("weapon")
                    .then(Commands.literal("add")
                        .then(Commands.argument("element", StringArgumentType.word())
                            // 自动补全有效元素属性 ID（fire, frost, nature, thunder）
                            // Auto-complete valid elemental attribute IDs
                            .suggests((ctx, builder) -> {
                                for (ElementType type : ElementType.values()) {
                                    if (type != ElementType.NONE) builder.suggest(type.getId());
                                }
                                return builder.buildFuture();
                            })
                            // 执行添加武器强制属性 / Execute add weapon forced attribute
                            .executes(ctx -> addWeaponForced(ctx, StringArgumentType.getString(ctx, "element")))
                        )
                    )
                    // 移除武器强制属性 / Remove weapon forced attribute
                    .then(Commands.literal("remove")
                        .executes(ForcedItemCommand::removeWeaponForced)
                    )
                )

                // ===== armor 子命令：管理装备强制强化/抗性属性 =====
                // ===== armor subcommand: manage forced enhancement/resistance for armor =====
                .then(Commands.literal("armor")
                    .then(Commands.literal("add")
                        .then(Commands.argument("enhance_element", StringArgumentType.word())
                            // 强化属性补全（允许留空）
                            // Enhancement attribute suggestion (allow empty)
                            .suggests((ctx, builder) -> {
                                builder.suggest("");
                                for (ElementType type : ElementType.values()) {
                                    if (type != ElementType.NONE) builder.suggest(type.getId());
                                }
                                return builder.buildFuture();
                            })
                            .then(Commands.argument("enhance_points", StringArgumentType.string())
                                .then(Commands.argument("resist_element", StringArgumentType.word())
                                    // 抗性属性补全（允许留空）
                                    // Resistance attribute suggestion (allow empty)
                                    .suggests((ctx, builder) -> {
                                        builder.suggest("");
                                        for (ElementType type : ElementType.values()) {
                                            if (type != ElementType.NONE) builder.suggest(type.getId());
                                        }
                                        return builder.buildFuture();
                                    })
                                    .then(Commands.argument("resist_points", StringArgumentType.string())
                                        // 完整参数执行 / Execute with full parameters
                                        .executes(ctx -> addArmorForced(ctx,
                                            getStr(ctx, "enhance_element"),
                                            getStr(ctx, "enhance_points"),
                                            getStr(ctx, "resist_element"),
                                            getStr(ctx, "resist_points")))
                                    )
                                    // 可选：省略抗性点数，默认0 / Optional: omit resistance points, default 0
                                    .executes(ctx -> addArmorForced(ctx,
                                        getStr(ctx, "enhance_element"),
                                        getStr(ctx, "enhance_points"),
                                        getStr(ctx, "resist_element"),
                                        "0"))
                                )
                                // 可选：省略抗性属性和点数 / Optional: omit resistance attribute and points
                                .executes(ctx -> addArmorForced(ctx,
                                    getStr(ctx, "enhance_element"),
                                    getStr(ctx, "enhance_points"),
                                    "", "0"))
                            )
                            // 可选：只指定强化属性，点数默认0 / Optional: only specify enhancement attribute, points default 0
                            .executes(ctx -> addArmorForced(ctx,
                                getStr(ctx, "enhance_element"),
                                "0", "", "0"))
                        )
                    )
                    // 移除装备强制属性 / Remove armor forced attributes
                    .then(Commands.literal("remove")
                        .executes(ForcedItemCommand::removeArmorForced)
                    )
                )
            )
        );
    }

    /**
     * 安全获取命令参数字符串，防止异常返回空字符串。
     *
     * Safely retrieve command argument string, return empty string on exception.
     *
     * @param ctx 命令上下文 / Command context
     * @param name 参数名 / Argument name
     * @return 参数值（已 trim） / Argument value (trimmed)
     */
    private static String getStr(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            return ctx.getArgument(name, String.class).trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 根据字符串 ID 解析元素属性类型。
     *
     * Parse ElementType from string ID.
     *
     * @param input 输入字符串 / Input string
     * @return 对应的 ElementType 或 null / Corresponding ElementType or null
     */
    private static ElementType parse(String input) {
        if (input == null || input.isBlank()) return null;
        return ElementType.fromId(input.toLowerCase());
    }

    /**
     * 标准化配置行：去除首尾引号并 trim，用于兼容不同来源的配置格式。
     *
     * Normalize configuration line: remove surrounding quotes and trim, for compatibility with different config sources.
     *
     * @param line 配置行 / Configuration line
     * @return 标准化后的字符串 / Normalized string
     */
    private static String normalizeLine(String line) {
        String trimmed = line.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    /**
     * 为手持武器添加强制攻击属性配置。
     * 如果该物品已在配置文件中注册强制属性，则拒绝添加并提示先移除。
     *
     * Add forced attack element configuration for the held weapon.
     * If the item already has forced attributes registered in the config, refuse to add and prompt to remove first.
     *
     * @param ctx 命令上下文 / Command context
     * @param elementRaw 元素属性 ID / Elemental attribute ID
     * @return 1 表示成功 / 1 for success
     */
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

        ElementType type = parse(elementRaw);
        if (type == null || type == ElementType.NONE) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forceditem.invalid_element", elementRaw));
            return 0;
        }

        String itemId = stack.getItem().builtInRegistryHolder().key().location().toString();

        // 检查是否已存在该物品的强制武器配置 / Check if the item already has a forced weapon entry
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

        // 添加配置并保存
        // Add config and save
        List<String> list = new ArrayList<>(ForcedItemConfig.FORCED_WEAPONS.get());
        list.add(newLine);
        ForcedItemConfig.FORCED_WEAPONS.set(list);
        ForcedItemConfig.SPEC.save();

        // 立即清理缓存，确保本次修改对当前游戏进程立即生效
        // Immediately clear cache to ensure changes take effect instantly
        ForcedItemHelper.clearCache();

        player.sendSystemMessage(Component.translatable("command.elementalcraft.forceditem.weapon.add_success", stack.getHoverName(), type.getDisplayName()));
        player.sendSystemMessage(Component.literal("§7" + newLine));
        player.sendSystemMessage(Component.translatable("command.elementalcraft.forceditem.saved"));

        return 1;
    }

    /**
     * 移除手持武器的强制攻击属性配置。
     *
     * Remove forced attack element configuration for the held weapon.
     *
     * @param ctx 命令上下文 / Command context
     * @return 1 表示成功 / 1 for success
     */
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

        // 保存配置并清理缓存
        // Save config and clear cache
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

    /**
     * 为手持装备添加强制强化/抗性属性配置。
     * 如果该物品已在配置文件中注册强制属性，则拒绝添加并提示先移除。
     *
     * Add forced enhancement/resistance attribute configuration for the held armor.
     * If the item already has forced attributes registered in the config, refuse to add and prompt to remove first.
     *
     * @param ctx 命令上下文 / Command context
     * @param enhanceRaw 强化属性 ID / Enhancement attribute ID
     * @param enhanceInput 强化点数 / Enhancement points
     * @param resistRaw 抗性属性 ID / Resistance attribute ID
     * @param resistInput 抗性点数 / Resistance points
     * @return 1 表示成功 / 1 for success
     */
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

        ElementType enhance = parse(enhanceRaw);
        ElementType resist = parse(resistRaw);

        String enhanceStr = enhanceInput.isBlank() ? "0" : enhanceInput.trim();
        String resistStr = resistInput.isBlank() ? "0" : resistInput.trim();

        String itemId = stack.getItem().builtInRegistryHolder().key().location().toString();

        // 检查是否已存在该物品的强制装备配置 / Check if the item already has a forced armor entry
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

        // 添加配置并保存
        // Add config and save
        List<String> list = new ArrayList<>(ForcedItemConfig.FORCED_ARMOR.get());
        list.add(newLine);
        ForcedItemConfig.FORCED_ARMOR.set(list);
        ForcedItemConfig.SPEC.save();

        // 立即清理缓存，确保本次修改对当前游戏进程立即生效
        // Immediately clear cache to ensure changes take effect instantly
        ForcedItemHelper.clearCache();

        player.sendSystemMessage(Component.translatable("command.elementalcraft.forceditem.armor.add_success", stack.getHoverName()));
        player.sendSystemMessage(Component.literal("§7" + newLine));
        player.sendSystemMessage(Component.translatable("command.elementalcraft.forceditem.saved"));

        return 1;
    }

    /**
     * 移除手持装备的强制强化/抗性属性配置。
     *
     * Remove forced enhancement/resistance attribute configuration for the held armor.
     *
     * @param ctx 命令上下文 / Command context
     * @return 1 表示成功 / 1 for success
     */
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

        // 保存配置并清理缓存
        // Save config and clear cache
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