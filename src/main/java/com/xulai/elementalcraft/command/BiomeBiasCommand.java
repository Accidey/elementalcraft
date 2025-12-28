// src/main/java/com/xulai/elementalcraft/command/BiomeBiasCommand.java
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

/**
 * BiomeBiasCommand
 * <p>
 * 中文说明：
 * 负责注册和处理 /elementalcraft biomebias 命令。
 * 允许管理员在游戏中直接修改群系的元素属性生成偏向（Biome Bias）。
 * 支持添加、移除和列出当前群系的配置，修改后会自动保存配置并刷新缓存。
 * <p>
 * English Description:
 * Responsible for registering and handling the /elementalcraft biomebias command.
 * Allows admins to modify the elemental attribute generation bias for biomes directly in-game.
 * Supports adding, removing, and listing configurations for the current biome.
 * Automatically saves the configuration and refreshes the cache upon modification.
 */
public class BiomeBiasCommand {

    /**
     * 注册指令到命令调度器。
     * <p>
     * Registers the command to the command dispatcher.
     *
     * @param dispatcher 命令调度器 / The command dispatcher
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("elementalcraft")
                        // 权限检查：需要 2 级权限（通常是 OP）
                        // Permission check: Requires permission level 2 (usually OP)
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("biomebias")
                                // 子命令：添加配置
                                // Subcommand: Add configuration
                                .then(Commands.literal("add")
                                        .then(Commands.argument("element", StringArgumentType.word())
                                                // 建议提供器：列出所有元素类型以及 "all"
                                                // Suggestion provider: Lists all element types and "all"
                                                .suggests((ctx, builder) -> {
                                                    builder.suggest("fire").suggest("frost").suggest("thunder").suggest("nature").suggest("all");
                                                    return builder.buildFuture();
                                                })
                                                .then(Commands.argument("probability", DoubleArgumentType.doubleArg(0.0, 100.0))
                                                        // 执行添加逻辑
                                                        // Execute add logic
                                                        .executes(ctx -> addBiomeBias(
                                                                ctx,
                                                                StringArgumentType.getString(ctx, "element"),
                                                                DoubleArgumentType.getDouble(ctx, "probability")
                                                        ))
                                                )
                                        )
                                )
                                // 子命令：移除配置
                                // Subcommand: Remove configuration
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("element", StringArgumentType.word())
                                                // 建议提供器：根据当前群系已有的配置动态提供建议
                                                // Suggestion provider: Dynamically suggests based on existing configurations for the current biome
                                                .suggests((ctx, builder) -> {
                                                    ServerPlayer player = ctx.getSource().getPlayer();
                                                    if (player == null) return builder.buildFuture();

                                                    String biomeId = getCurrentBiomeId(player);
                                                    if (biomeId == null) return builder.buildFuture();

                                                    List<? extends String> lines = ElementalConfig.CUSTOM_BIOME_ATTRIBUTE_BIAS.get();
                                                    for (String line : lines) {
                                                        String trimmed = line.trim();
                                                        if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                                                        // 匹配当前群系的配置项
                                                        // Match configuration entries for the current biome
                                                        if (trimmed.startsWith(biomeId + ":")) {
                                                            String elem = trimmed.substring(biomeId.length() + 1).split(",")[0];
                                                            builder.suggest(elem);
                                                        }
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                // 执行移除逻辑
                                                // Execute remove logic
                                                .executes(ctx -> removeBiomeBias(
                                                        ctx,
                                                        StringArgumentType.getString(ctx, "element")
                                                ))
                                        )
                                )
                                // 子命令：列出配置
                                // Subcommand: List configurations
                                .then(Commands.literal("list")
                                        .executes(BiomeBiasCommand::listBiomeBias)
                                )
                        )
        );
    }

    /**
     * 获取玩家当前所处群系的注册 ID。
     * <p>
     * Retrieves the registry ID of the biome the player is currently standing in.
     *
     * @param player 服务器玩家实体 / The server player entity
     * @return 群系资源路径字符串 (例如 "minecraft:plains") / Biome resource location string (e.g., "minecraft:plains")
     */
    private static String getCurrentBiomeId(ServerPlayer player) {
        Holder<Biome> biomeHolder = player.serverLevel().getBiome(player.blockPosition());
        ResourceLocation loc = biomeHolder.unwrapKey().orElseThrow().location();
        return loc.toString();
    }

    /**
     * 执行添加偏向配置的逻辑。
     * 验证输入，检查冲突，写入配置并刷新缓存。
     * <p>
     * Executes the logic for adding a bias configuration.
     * Validates input, checks for conflicts, writes to config, and refreshes cache.
     *
     * @param ctx         命令上下文 / Command context
     * @param elementStr  元素名称或 "all" / Element name or "all"
     * @param probability 概率值 / Probability value
     * @return 1 表示成功，0 表示失败 / 1 for success, 0 for failure
     */
    private static int addBiomeBias(CommandContext<CommandSourceStack> ctx, String elementStr, double probability) {
        CommandSourceStack source = ctx.getSource();
        // 确保命令执行者是玩家
        // Ensure the command executor is a player
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.elementalcraft.player_only"));
            return 0;
        }

        String biomeId = getCurrentBiomeId(player);
        boolean isAll = elementStr.equalsIgnoreCase("all");
        @Nullable ElementType type = isAll ? null : ElementType.fromId(elementStr.toLowerCase());

        // 验证元素类型是否有效
        // Validate if the element type is valid
        if (!isAll && (type == null || type == ElementType.NONE)) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.invalid_attribute", elementStr));
            return 0;
        }

        // 获取当前配置列表的可变副本
        // Get a mutable copy of the current configuration list
        List<String> currentList = new ArrayList<>(ElementalConfig.CUSTOM_BIOME_ATTRIBUTE_BIAS.get());
        String prefix = biomeId + ":";

        // 检查是否存在冲突配置（例如已存在 "all" 或相同的特定元素）
        // Check for conflicting configurations (e.g., "all" already exists or the specific element exists)
        boolean hasAll = currentList.stream().anyMatch(l -> l.trim().startsWith(prefix + "all,"));
        boolean hasTarget = currentList.stream().anyMatch(l -> l.trim().startsWith(prefix + (isAll ? "all," : type.getId() + ",")));

        if (hasAll || hasTarget) {
            if (hasAll && !isAll) {
                // 如果已存在 "all" 配置，则无法添加单个元素配置
                // If an "all" configuration exists, cannot add a single element configuration
                player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.conflict_all", biomeId));
            } else {
                // 如果已存在相同的配置
                // If the same configuration already exists
                player.sendSystemMessage(Component.translatable(
                        "command.elementalcraft.biomebias.already_exists",
                        biomeId,
                        isAll ? "ALL" : type.getDisplayName().getString()
                ));
            }
            player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.use_remove_first"));
            return 0;
        }

        // 格式化新条目并添加到列表
        // Format the new entry and add it to the list
        String newEntry = biomeId + ":" + (isAll ? "all" : type.getId()) + "," + String.format("%.1f", probability);
        currentList.add(newEntry);

        // 保存配置到文件
        // Save configuration to file
        ElementalConfig.CUSTOM_BIOME_ATTRIBUTE_BIAS.set(currentList);
        ElementalConfig.SPEC.save();

        // 立即清理运行时缓存以应用更改
        // Immediately clear runtime cache to apply changes
        CustomBiomeBias.clearCache();

        // 发送成功消息
        // Send success message
        if (isAll) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.added_all", probability, biomeId));
        } else {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.added_single", type.getDisplayName(), probability, biomeId));
        }
        player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.saved"));

        return 1;
    }

    /**
     * 执行移除偏向配置的逻辑。
     * 查找匹配的配置项并移除，随后保存并刷新缓存。
     * <p>
     * Executes the logic for removing a bias configuration.
     * Finds and removes the matching configuration entry, then saves and refreshes cache.
     *
     * @param ctx        命令上下文 / Command context
     * @param elementStr 元素名称或 "all" / Element name or "all"
     * @return 1 表示成功，0 表示失败 / 1 for success, 0 for failure
     */
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

        // 根据输入移除对应的配置行
        // Remove the corresponding configuration line based on input
        if (isAll) {
            removed = currentList.removeIf(l -> l.trim().startsWith(prefix));
        } else {
            String target = biomeId + ":" + type.getId() + ",";
            removed = currentList.removeIf(l -> l.trim().startsWith(target));
        }

        // 如果未找到要移除的配置
        // If the configuration to remove was not found
        if (!removed) {
            if (isAll) {
                player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.not_found_any", biomeId));
            } else {
                player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.not_found_single", biomeId, elementStr));
            }
            return 0;
        }

        // 保存更改并刷新缓存
        // Save changes and refresh cache
        ElementalConfig.CUSTOM_BIOME_ATTRIBUTE_BIAS.set(currentList);
        ElementalConfig.SPEC.save();
        CustomBiomeBias.clearCache();

        // 发送成功消息
        // Send success message
        if (isAll) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.removed_all", biomeId));
        } else {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.removed_single", type.getDisplayName(), biomeId));
        }
        player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.saved"));

        return 1;
    }

    /**
     * 列出当前群系的所有偏向配置。
     * <p>
     * Lists all bias configurations for the current biome.
     *
     * @param ctx 命令上下文 / Command context
     * @return 找到的配置条目数量 / Number of configuration entries found
     */
    private static int listBiomeBias(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayer();

        if (player == null) {
            source.sendFailure(Component.translatable("command.elementalcraft.player_only"));
            return 0;
        }

        String biomeId = getCurrentBiomeId(player);
        List<? extends String> allLines = ElementalConfig.CUSTOM_BIOME_ATTRIBUTE_BIAS.get();

        // 筛选出属于当前群系的配置行
        // Filter configuration lines belonging to the current biome
        List<String> relevant = new ArrayList<>();
        for (String line : allLines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            if (trimmed.startsWith(biomeId + ":")) {
                relevant.add(trimmed.substring(biomeId.length() + 1)); // 仅保留 "element,prob" 部分 / Keep only "element,prob" part
            }
        }

        // 向玩家发送列表信息
        // Send list information to the player
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