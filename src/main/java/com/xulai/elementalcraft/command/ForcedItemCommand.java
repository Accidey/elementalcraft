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
 * <p>
 * 中文说明：
 * 负责注册和管理 /elementalcraft forceditem 命令。
 * 该命令允许拥有 OP 权限的玩家通过手持物品，将特定的元素属性（武器攻击属性或防具的增强/抗性属性）绑定到该物品类型上。
 * 配置更改会直接写入文件，并触发 ForcedItemHelper 的缓存清理以实现热重载。
 * <p>
 * English Description:
 * Responsible for registering and managing the /elementalcraft forceditem command.
 * This command allows players with OP permissions to bind specific elemental attributes (weapon attack attributes or armor enhancement/resistance attributes) to the held item type.
 * Configuration changes are written directly to the file and trigger a cache clear in ForcedItemHelper for hot-reloading.
 */
@Mod.EventBusSubscriber(modid = "elementalcraft")
@SuppressWarnings("null")
public class ForcedItemCommand {

    /**
     * 注册 /elementalcraft forceditem 命令及其子命令。
     * <p>
     * Registers the /elementalcraft forceditem command and its subcommands.
     *
     * @param event 命令注册事件 / Command registration event
     */
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("elementalcraft")
                // 权限检查：需要 2 级权限（管理员）
                // Permission check: Requires permission level 2 (Admin)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("forceditem")
                        // ===== weapon 子命令：管理武器强制攻击属性 =====
                        // ===== weapon subcommand: manage forced attack element for weapons =====
                        .then(Commands.literal("weapon")
                                .then(Commands.literal("add")
                                        .then(Commands.argument("element", StringArgumentType.word())
                                                // 自动补全有效元素属性 ID
                                                // Auto-complete valid elemental attribute IDs
                                                .suggests((ctx, builder) -> {
                                                    for (ElementType type : ElementType.values()) {
                                                        if (type != ElementType.NONE) builder.suggest(type.getId());
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                // 执行添加武器强制属性逻辑
                                                // Execute logic to add weapon forced attribute
                                                .executes(ctx -> addWeaponForced(ctx, StringArgumentType.getString(ctx, "element")))
                                        )
                                )
                                // 移除武器强制属性
                                // Remove weapon forced attribute
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
                                                                        // 完整参数执行
                                                                        // Execute with full parameters
                                                                        .executes(ctx -> addArmorForced(ctx,
                                                                                getStr(ctx, "enhance_element"),
                                                                                getStr(ctx, "enhance_points"),
                                                                                getStr(ctx, "resist_element"),
                                                                                getStr(ctx, "resist_points")))
                                                                )
                                                                // 可选：省略抗性点数，默认 0
                                                                // Optional: omit resistance points, default 0
                                                                .executes(ctx -> addArmorForced(ctx,
                                                                        getStr(ctx, "enhance_element"),
                                                                        getStr(ctx, "enhance_points"),
                                                                        getStr(ctx, "resist_element"),
                                                                        "0"))
                                                        )
                                                        // 可选：省略抗性属性和点数
                                                        // Optional: omit resistance attribute and points
                                                        .executes(ctx -> addArmorForced(ctx,
                                                                getStr(ctx, "enhance_element"),
                                                                getStr(ctx, "enhance_points"),
                                                                "", "0"))
                                                )
                                                // 可选：只指定强化属性，点数默认 0
                                                // Optional: only specify enhancement attribute, points default 0
                                                .executes(ctx -> addArmorForced(ctx,
                                                        getStr(ctx, "enhance_element"),
                                                        "0", "", "0"))
                                        )
                                )
                                // 移除装备强制属性
                                // Remove armor forced attributes
                                .then(Commands.literal("remove")
                                        .executes(ForcedItemCommand::removeArmorForced)
                                )
                        )
                )
        );
    }

    /**
     * 安全获取命令参数字符串。
     * 捕获可能出现的异常并返回空字符串。
     * <p>
     * Safely retrieve command argument string.
     * Catches potential exceptions and returns an empty string.
     *
     * @param ctx  命令上下文 / Command context
     * @param name 参数名 / Argument name
     * @return 处理后的参数字符串 / Processed argument string
     */
    private static String getStr(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            return ctx.getArgument(name, String.class).trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 解析字符串为 ElementType 枚举。
     * <p>
     * Parses a string into an ElementType enum.
     *
     * @param input 输入字符串 / Input string
     * @return 对应的 ElementType，若无效则返回 null / Corresponding ElementType, or null if invalid
     */
    private static ElementType parse(String input) {
        if (input == null || input.isBlank()) return null;
        return ElementType.fromId(input.toLowerCase());
    }

    /**
     * 标准化配置行字符串。
     * 去除首尾的引号并去除空白字符，确保与不同格式的配置兼容。
     * <p>
     * Normalizes the configuration line string.
     * Removes surrounding quotes and trims whitespace to ensure compatibility with different config formats.
     *
     * @param line 原始配置行 / Original configuration line
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
     * 为玩家主手持有的武器添加强制攻击属性配置。
     * <p>
     * Adds forced attack attribute configuration for the weapon held in the player's main hand.
     *
     * @param ctx        命令上下文 / Command context
     * @param elementRaw 元素属性 ID 字符串 / Elemental attribute ID string
     * @return 1 表示成功，0 表示失败 / 1 for success, 0 for failure
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

        // 检查该物品是否已存在强制武器配置
        // Check if a forced weapon configuration already exists for this item
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

        // 添加新配置并保存
        // Add new configuration and save
        List<String> list = new ArrayList<>(ForcedItemConfig.FORCED_WEAPONS.get());
        list.add(newLine);
        ForcedItemConfig.FORCED_WEAPONS.set(list);
        ForcedItemConfig.SPEC.save();

        // 立即清理缓存以应用更改
        // Immediately clear cache to apply changes
        ForcedItemHelper.clearCache();

        player.sendSystemMessage(Component.translatable("command.elementalcraft.forceditem.weapon.add_success", stack.getHoverName(), type.getDisplayName()));
        player.sendSystemMessage(Component.literal("§7" + newLine));
        player.sendSystemMessage(Component.translatable("command.elementalcraft.forceditem.saved"));

        return 1;
    }

    /**
     * 移除玩家主手持有武器的强制攻击属性配置。
     * <p>
     * Removes the forced attack attribute configuration for the weapon held in the player's main hand.
     *
     * @param ctx 命令上下文 / Command context
     * @return 1 表示成功，0 表示失败 / 1 for success, 0 for failure
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

        // 从配置中查找并移除对应条目
        // Find and remove the corresponding entry from the configuration
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
     * 为玩家主手持有的防具添加强制强化/抗性属性配置。
     * <p>
     * Adds forced enhancement/resistance attribute configuration for the armor held in the player's main hand.
     *
     * @param ctx          命令上下文 / Command context
     * @param enhanceRaw   强化属性 ID 字符串 / Enhancement attribute ID string
     * @param enhanceInput 强化点数 / Enhancement points
     * @param resistRaw    抗性属性 ID 字符串 / Resistance attribute ID string
     * @param resistInput  抗性点数 / Resistance points
     * @return 1 表示成功，0 表示失败 / 1 for success, 0 for failure
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

        // 检查该物品是否已存在强制防具配置
        // Check if a forced armor configuration already exists for this item
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

        // 添加新配置并保存
        // Add new configuration and save
        List<String> list = new ArrayList<>(ForcedItemConfig.FORCED_ARMOR.get());
        list.add(newLine);
        ForcedItemConfig.FORCED_ARMOR.set(list);
        ForcedItemConfig.SPEC.save();

        // 立即清理缓存
        // Immediately clear cache
        ForcedItemHelper.clearCache();

        player.sendSystemMessage(Component.translatable("command.elementalcraft.forceditem.armor.add_success", stack.getHoverName()));
        player.sendSystemMessage(Component.literal("§7" + newLine));
        player.sendSystemMessage(Component.translatable("command.elementalcraft.forceditem.saved"));

        return 1;
    }

    /**
     * 移除玩家主手持有防具的强制强化/抗性属性配置。
     * <p>
     * Removes the forced enhancement/resistance attribute configuration for the armor held in the player's main hand.
     *
     * @param ctx 命令上下文 / Command context
     * @return 1 表示成功，0 表示失败 / 1 for success, 0 for failure
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

        // 从配置中查找并移除对应条目
        // Find and remove the corresponding entry from the configuration
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