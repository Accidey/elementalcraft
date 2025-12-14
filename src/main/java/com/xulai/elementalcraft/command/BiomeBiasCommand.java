// src/main/java/com/xulai/elementalcraft/command/BiomeBiasCommand.java
package com.xulai.elementalcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.util.ElementType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * BiomeBiasCommand
 *
 * 用于注册和处理 /elementalcraft biomebias 指令。
 * 该指令允许管理员为“当前所在群系”配置元素属性偏向，
 * 例如：某个群系更容易生成带有火焰/冰霜属性的生物或效果。
 *
 * Registers and handles the /elementalcraft biomebias command.
 * This command allows administrators to define element bias
 * probabilities for the biome the player is currently in.
 */
public class BiomeBiasCommand {

    /**
     * 注册指令到 Brigadier 命令系统。
     *
     * Registers this command and all its subcommands
     * into the Brigadier command dispatcher.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                Commands.literal("elementalcraft")

                        // 限制只有 OP（权限等级 ≥ 2）才能使用该指令
                        // Restricts this command to operators only
                        .requires(source -> source.hasPermission(2))

                        .then(Commands.literal("biomebias")

                                /**
                                 * /elementalcraft biomebias add <attribute> <probability>
                                 *
                                 * 为玩家当前所在的群系添加一个元素属性偏向。
                                 * Adds an element bias entry for the biome
                                 * the player is currently standing in.
                                 */
                                .then(Commands.literal("add")
                                        .then(Commands.argument("attribute", StringArgumentType.word())

                                                // 提供元素 ID 的自动补全（不包含 NONE）
                                                // Suggests all valid element IDs except NONE
                                                .suggests((ctx, builder) -> {
                                                    for (ElementType type : ElementType.values()) {
                                                        if (type != ElementType.NONE) {
                                                            builder.suggest(type.getId());
                                                        }
                                                    }
                                                    return builder.buildFuture();
                                                })

                                                // 概率参数，范围为 0.0 ~ 100.0
                                                // Probability argument (percentage range)
                                                .then(Commands.argument(
                                                        "probability",
                                                        DoubleArgumentType.doubleArg(0.0, 100.0)
                                                )
                                                        .executes(ctx ->
                                                                addBiomeBias(
                                                                        ctx,
                                                                        StringArgumentType.getString(ctx, "attribute"),
                                                                        DoubleArgumentType.getDouble(ctx, "probability")
                                                                )
                                                        )
                                                )
                                        )
                                )

                                /**
                                 * /elementalcraft biomebias remove <attribute>
                                 *
                                 * 从玩家当前所在的群系中移除指定的元素属性偏向。
                                 * Removes an element bias entry from the
                                 * biome the player is currently in.
                                 */
                                .then(Commands.literal("remove")
                                        .then(Commands.argument("attribute", StringArgumentType.word())

                                                // 根据当前群系中已配置的属性偏向提供补全建议
                                                // Suggests only attributes that already exist
                                                // for the player's current biome
                                                .suggests((ctx, builder) -> {
                                                    ServerPlayer player = ctx.getSource().getPlayer();
                                                    if (player == null) return builder.buildFuture();

                                                    Holder<Biome> biomeHolder =
                                                            player.serverLevel().getBiome(player.blockPosition());

                                                    ResourceLocation biomeId = biomeHolder.unwrapKey()
                                                            .orElseThrow()
                                                            .location();

                                                    for (String line : ElementalConfig.CUSTOM_BIOME_ATTRIBUTE_BIAS.get()) {
                                                        String trimmed = line.trim();
                                                        if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                                                        String[] parts = trimmed.split(",");
                                                        if (parts.length != 3) continue;

                                                        if (parts[0].trim().equals(biomeId.toString())) {
                                                            builder.suggest(parts[1].trim());
                                                        }
                                                    }
                                                    return builder.buildFuture();
                                                })

                                                .executes(ctx ->
                                                        removeBiomeBias(
                                                                ctx,
                                                                StringArgumentType.getString(ctx, "attribute")
                                                        )
                                                )
                                        )
                                )
                        )
        );
    }

    /**
     * 为当前群系添加元素属性偏向。
     *
     * Adds an element bias entry for the player's current biome.
     * The entry format stored in config:
     * biome_id,element_id,probability
     */
    private static int addBiomeBias(
            CommandContext<CommandSourceStack> ctx,
            String attributeId,
            double probability
    ) {

        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(
                    Component.translatable("command.elementalcraft.player_only")
            );
            return 0;
        }

        Holder<Biome> biomeHolder =
                player.serverLevel().getBiome(player.blockPosition());

        ResourceLocation biomeLocation = biomeHolder.unwrapKey()
                .orElseThrow(() -> new IllegalStateException("Biome has no registry key"))
                .location();

        String biomeId = biomeLocation.toString();

        ElementType type = ElementType.fromId(attributeId.toLowerCase());
        if (type == null || type == ElementType.NONE) {
            player.sendSystemMessage(
                    Component.translatable(
                            "command.elementalcraft.biomebias.invalid_attribute",
                            attributeId
                    )
            );
            return 0;
        }

        List<String> currentList =
                new ArrayList<>(ElementalConfig.CUSTOM_BIOME_ATTRIBUTE_BIAS.get());

        String newEntry =
                biomeId + "," + type.getId() + "," + String.format("%.1f", probability);

        boolean exists = currentList.stream()
                .anyMatch(line ->
                        line.trim().startsWith(
                                biomeId + "," + type.getId() + ","
                        )
                );

        if (exists) {
            player.sendSystemMessage(
                    Component.translatable(
                            "command.elementalcraft.biomebias.already_exists",
                            biomeId,
                            type.getId()
                    )
            );
            return 0;
        }

        currentList.add(newEntry);

        ElementalConfig.CUSTOM_BIOME_ATTRIBUTE_BIAS.set(currentList);
        ElementalConfig.SPEC.save();

        player.sendSystemMessage(
                Component.translatable(
                        "command.elementalcraft.biomebias.add_success",
                        biomeId,
                        type.getDisplayName(),
                        probability
                )
        );

        player.sendSystemMessage(
                Component.translatable("command.elementalcraft.biomebias.saved")
        );

        return 1;
    }

    /**
     * 从当前群系移除指定的元素属性偏向。
     *
     * Removes an element bias entry from the player's current biome.
     */
    private static int removeBiomeBias(
            CommandContext<CommandSourceStack> ctx,
            String attributeId
    ) {

        if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
            ctx.getSource().sendFailure(
                    Component.translatable("command.elementalcraft.player_only")
            );
            return 0;
        }

        Holder<Biome> biomeHolder =
                player.serverLevel().getBiome(player.blockPosition());

        ResourceLocation biomeLocation = biomeHolder.unwrapKey()
                .orElseThrow(() -> new IllegalStateException("Biome has no registry key"))
                .location();

        String biomeId = biomeLocation.toString();

        ElementType type = ElementType.fromId(attributeId.toLowerCase());
        if (type == null || type == ElementType.NONE) {
            player.sendSystemMessage(
                    Component.translatable(
                            "command.elementalcraft.biomebias.invalid_attribute",
                            attributeId
                    )
            );
            return 0;
        }

        List<String> currentList =
                new ArrayList<>(ElementalConfig.CUSTOM_BIOME_ATTRIBUTE_BIAS.get());

        boolean removed = false;

        for (int i = currentList.size() - 1; i >= 0; i--) {
            String line = currentList.get(i).trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] parts = line.split(",");
            if (parts.length != 3) continue;

            if (parts[0].trim().equals(biomeId)
                    && parts[1].trim().equals(type.getId())) {
                currentList.remove(i);
                removed = true;
            }
        }

        if (!removed) {
            player.sendSystemMessage(
                    Component.translatable(
                            "command.elementalcraft.biomebias.not_found",
                            biomeId,
                            type.getId()
                    )
            );
            return 0;
        }

        ElementalConfig.CUSTOM_BIOME_ATTRIBUTE_BIAS.set(currentList);
        ElementalConfig.SPEC.save();

        player.sendSystemMessage(
                Component.translatable(
                        "command.elementalcraft.biomebias.remove_success",
                        biomeId,
                        type.getDisplayName()
                )
        );

        player.sendSystemMessage(
                Component.translatable("command.elementalcraft.biomebias.saved")
        );

        return 1;
    }
}
