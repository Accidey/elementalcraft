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
 *
 * 中文说明：
 * 用于注册和处理 /elementalcraft biomebias 指令。
 * 支持为当前所在群系单独设置某个元素的生成偏向概率，或使用 all 一键设置所有元素的相同概率。
 * 同时提供 remove 和 list 子命令，便于管理与查看。
 * 修改说明：指令执行并保存配置后，会立即清理 CustomBiomeBias 缓存，实现热重载。
 *
 * English description:
 * Registers and handles the /elementalcraft biomebias command.
 * Allows setting bias probability for a single element or using "all" to set the same probability for all elements in the current biome.
 * Includes remove and list subcommands for management and viewing.
 * Modification Note: Clears CustomBiomeBias cache immediately after saving config to enable hot-reloading.
 */
public class BiomeBiasCommand {

    /**
     * 注册整个 biomebias 指令及其所有子命令到 Brigadier 系统
     *
     * Registers the entire biomebias command and all its subcommands into the Brigadier dispatcher.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("elementalcraft")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("biomebias")
                                // add <element|all> <probability>
                                // 为群系添加元素偏向概率 / Add element bias probability for the biome
                                .then(Commands.literal("add")
                                        .then(Commands.argument("element", StringArgumentType.word())
                                                // 自动补全建议：四个元素 + all / Suggests the four elements + "all"
                                                .suggests((ctx, builder) -> {
                                                    builder.suggest("fire").suggest("frost").suggest("thunder").suggest("nature").suggest("all");
                                                    return builder.buildFuture();
                                                })
                                                // 概率参数 0.0~100.0 / Probability argument 0.0~100.0
                                                .then(Commands.argument("probability", DoubleArgumentType.doubleArg(0.0, 100.0))
                                                        .executes(ctx -> addBiomeBias(
                                                                ctx,
                                                                StringArgumentType.getString(ctx, "element"),
                                                                DoubleArgumentType.getDouble(ctx, "probability")
                                                        ))
                                                )
                                        )
                                )
                                // remove <element|all>
                                // 从群系移除元素偏向概率 / Remove element bias probability from the biome
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("element", StringArgumentType.word())
                                                // 根据当前群系已配置的偏向提供补全 / Suggests based on existing biases in the current biome
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
                                // list
                                // 列出当前群系的所有元素偏向配置 / List all element biases for the current biome
                                .then(Commands.literal("list")
                                        .executes(BiomeBiasCommand::listBiomeBias)
                                )
                        )
        );
    }

    /**
     * 获取玩家当前所在群系的注册 ID（如 minecraft:plains）
     *
     * Gets the registry ID of the biome the player is currently standing in (e.g., minecraft:plains).
     *
     * @param player 执行命令的玩家 / The player executing the command
     * @return 群系 ID 字符串 / Biome ID string
     */
    private static String getCurrentBiomeId(ServerPlayer player) {
        Holder<Biome> biomeHolder = player.serverLevel().getBiome(player.blockPosition());
        ResourceLocation loc = biomeHolder.unwrapKey().orElseThrow().location();
        return loc.toString();
    }

    /**
     * 为当前群系添加元素偏向概率（支持单个元素或 all）
     *
     * Adds element bias probability for the current biome (supports single element or "all").
     *
     * @param ctx 命令上下文 / Command context
     * @param elementStr 元素 ID 或 "all" / Element ID or "all"
     * @param probability 偏向概率 / Bias probability
     * @return 命令执行结果（1 表示成功） / Command execution result (1 for success)
     */
    private static int addBiomeBias(CommandContext<CommandSourceStack> ctx, String elementStr, double probability) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.elementalcraft.player_only"));
            return 0;
        }

        String biomeId = getCurrentBiomeId(player);

        boolean isAll = elementStr.equalsIgnoreCase("all");
        @Nullable ElementType type = isAll ? null : ElementType.fromId(elementStr.toLowerCase());

        // 验证元素 ID 是否有效 / Validate element ID
        if (!isAll && (type == null || type == ElementType.NONE)) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.invalid_attribute", elementStr));
            return 0;
        }

        // 从配置获取当前列表并创建可变副本 / Get current list from config and create mutable copy
        List<String> currentList = new ArrayList<>(ElementalConfig.CUSTOM_BIOME_ATTRIBUTE_BIAS.get());
        String prefix = biomeId + ":";

        // 检查冲突：已存在 all 或目标单个元素 / Check for conflicts: existing "all" or the target single element
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

        // 只有在无冲突时才真正添加并保存 / Only add and save when no conflict
        String newEntry = biomeId + ":" + (isAll ? "all" : type.getId()) + "," + String.format("%.1f", probability);
        currentList.add(newEntry);

        ElementalConfig.CUSTOM_BIOME_ATTRIBUTE_BIAS.set(currentList);
        ElementalConfig.SPEC.save();

        // 关键修复：保存后立即清理缓存，实现热重载
        // Critical Fix: Clear cache immediately after saving to enable hot-reloading
        CustomBiomeBias.clearCache();

        // 发送成功反馈 / Send success feedback
        if (isAll) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.added_all", probability, biomeId));
        } else {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.added_single", type.getDisplayName(), probability, biomeId));
        }
        player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.saved"));

        return 1;
    }

    /**
     * 从当前群系移除元素偏向概率（支持单个元素或 all）
     *
     * Removes element bias probability from the current biome (supports single element or "all").
     *
     * @param ctx 命令上下文 / Command context
     * @param elementStr 元素 ID 或 "all" / Element ID or "all"
     * @return 命令执行结果（1 表示成功） / Command execution result (1 for success)
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

        // 验证元素 ID 是否有效 / Validate element ID
        if (!isAll && (type == null || type == ElementType.NONE)) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.invalid_attribute", elementStr));
            return 0;
        }

        // 创建可变副本并执行移除 / Create mutable copy and perform removal
        List<String> currentList = new ArrayList<>(ElementalConfig.CUSTOM_BIOME_ATTRIBUTE_BIAS.get());
        String prefix = biomeId + ":";

        boolean removed = false;

        if (isAll) {
            removed = currentList.removeIf(l -> l.trim().startsWith(prefix));
        } else {
            String target = biomeId + ":" + type.getId() + ",";
            removed = currentList.removeIf(l -> l.trim().startsWith(target));
        }

        // 若未找到对应配置，发送提示 / If no matching entry found, send feedback
        if (!removed) {
            if (isAll) {
                player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.not_found_any", biomeId));
            } else {
                player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.not_found_single", biomeId, elementStr));
            }
            return 0;
        }

        // 保存配置并发送成功反馈 / Save config and send success feedback
        ElementalConfig.CUSTOM_BIOME_ATTRIBUTE_BIAS.set(currentList);
        ElementalConfig.SPEC.save();

        // 关键修复：保存后立即清理缓存，实现热重载
        // Critical Fix: Clear cache immediately after saving to enable hot-reloading
        CustomBiomeBias.clearCache();

        if (isAll) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.removed_all", biomeId));
        } else {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.removed_single", type.getDisplayName(), biomeId));
        }
        player.sendSystemMessage(Component.translatable("command.elementalcraft.biomebias.saved"));

        return 1;
    }

    /**
     * 列出玩家当前所在群系的所有元素偏向配置
     *
     * Lists all element bias configurations for the biome the player is currently in.
     *
     * @param ctx 命令上下文 / Command context
     * @return 配置条目数量 / Number of configuration entries
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

        // 筛选出当前群系的相关配置 / Filter entries related to the current biome
        List<String> relevant = new ArrayList<>();
        for (String line : allLines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            if (trimmed.startsWith(biomeId + ":")) {
                relevant.add(trimmed.substring(biomeId.length() + 1)); // 只显示 element,prob
            }
        }

        // 发送列表或空提示 / Send list or empty message
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