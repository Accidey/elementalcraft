// src/main/java/com/xulai/elementalcraft/command/BlacklistCommand.java
package com.xulai.elementalcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.util.ElementType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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

/**
 * BlacklistCommand
 * <p>
 * 中文说明：
 * 负责注册和处理 /elementalcraft blacklist 命令。
 * 该命令用于管理实体元素属性生成的黑名单。
 * 管理员可以通过手持刷怪蛋来指定目标实体，将其添加到配置文件中，防止该实体生成特定的或所有的元素属性。
 * 支持热重载：直接修改配置文件并保存。
 * <p>
 * English Description:
 * Responsible for registering and handling the /elementalcraft blacklist command.
 * This command is used to manage the blacklist for entity elemental attribute generation.
 * Admins can target specific entities by holding their spawn eggs and add them to the configuration file,
 * preventing that entity from generating specific or all elemental attributes.
 * Supports hot-reloading: directly modifies and saves the configuration file.
 */
@Mod.EventBusSubscriber(modid = "elementalcraft")
public class BlacklistCommand {

    /**
     * 注册指令入口。
     * 监听 Forge 的 RegisterCommandsEvent 事件进行指令注册。
     * <p>
     * Register command entry point.
     * Listens to Forge's RegisterCommandsEvent for command registration.
     *
     * @param event 命令注册事件 / Command registration event
     */
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("elementalcraft")
                // 权限检查：需要 2 级权限（通常是管理员/OP）
                // Permission check: Requires permission level 2 (usually Admin/OP)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("blacklist")
                        // 子命令：添加黑名单
                        // Subcommand: Add to blacklist
                        .then(Commands.literal("add")
                                .then(Commands.argument("element", StringArgumentType.word())
                                        // 建议提供器：提供四种元素类型和 "all" 选项
                                        // Suggestion provider: Provides the four element types and the "all" option
                                        .suggests((ctx, builder) -> {
                                            builder.suggest("fire").suggest("frost").suggest("thunder").suggest("nature").suggest("all");
                                            return builder.buildFuture();
                                        })
                                        // 执行添加逻辑
                                        // Execute add logic
                                        .executes(ctx -> addToBlacklist(ctx, StringArgumentType.getString(ctx, "element")))
                                )
                        )
                        // 子命令：移除黑名单
                        // Subcommand: Remove from blacklist
                        .then(Commands.literal("remove")
                                .then(Commands.argument("element", StringArgumentType.word())
                                        // 动态建议提供器：根据玩家手持刷怪蛋对应的已存在配置提供建议
                                        // Dynamic suggestion provider: Suggests based on existing configurations for the held spawn egg
                                        .suggests((ctx, builder) -> {
                                            ServerPlayer player = ctx.getSource().getPlayer();
                                            if (player == null) return builder.buildFuture();
                                            
                                            // 获取手持刷怪蛋对应的实体 ID
                                            // Get the Entity ID corresponding to the held spawn egg
                                            String entityId = getHeldEntityId(player);
                                            if (entityId == null) return builder.buildFuture();

                                            // 遍历配置查找匹配项并提供给命令补全
                                            // Iterate through config to find matches and provide for command completion
                                            for (String entry : ElementalConfig.BLACKLISTED_ENTITIES.get()) {
                                                if (entry.startsWith(entityId + ":")) {
                                                    String elem = entry.substring(entityId.length() + 1);
                                                    builder.suggest(elem);
                                                }
                                            }
                                            return builder.buildFuture();
                                        })
                                        // 执行移除逻辑
                                        // Execute remove logic
                                        .executes(ctx -> removeFromBlacklist(ctx, StringArgumentType.getString(ctx, "element")))
                                )
                        )
                        // 子命令：列出黑名单
                        // Subcommand: List blacklist
                        .then(Commands.literal("list")
                                .executes(BlacklistCommand::listBlacklist)
                        )
                )
        );
    }

    /**
     * 将手持刷怪蛋对应实体的指定属性加入黑名单。
     * <p>
     * Adds the specified attribute of the entity corresponding to the held spawn egg to the blacklist.
     *
     * @param ctx        命令上下文 / Command context
     * @param elementStr 元素字符串（fire, frost, ..., all） / Element string
     * @return 1 表示成功，0 表示失败 / 1 for success, 0 for failure
     */
    private static int addToBlacklist(CommandContext<CommandSourceStack> ctx, String elementStr) {
        CommandSourceStack source = ctx.getSource();
        // 确保执行者是玩家
        // Ensure the executor is a player
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.elementalcraft.player_only"));
            return 0;
        }

        // 获取手持刷怪蛋对应的实体 ID
        // Get Entity ID from held spawn egg
        String entityId = getHeldEntityId(player);
        if (entityId == null) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.blacklist.no_egg"));
            return 0;
        }

        boolean isAll = elementStr.equalsIgnoreCase("all");
        ElementType type = isAll ? null : ElementType.fromId(elementStr.toLowerCase());

        // 验证输入的元素类型是否有效
        // Validate if the input element type is valid
        if (!isAll && (type == null || type == ElementType.NONE)) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.blacklist.invalid_attribute", elementStr));
            return 0;
        }

        // 构建目标配置条目字符串
        // Build the target config entry string
        String targetEntry = isAll ? entityId + ":all" : entityId + ":" + (isAll ? "all" : type.getId());

        // 获取当前配置列表的可变副本
        // Get a mutable copy of the current configuration list
        List<String> list = new ArrayList<>(ElementalConfig.BLACKLISTED_ENTITIES.get());

        // 检查重复或冲突条目
        // Check for duplicate or conflicting entries
        boolean alreadyExists = list.stream().anyMatch(e ->
                e.equals(targetEntry) || // 完全重复 / Exact duplicate
                        e.equals(entityId + ":all") || // 如果已禁用 "all"，则不能添加单个 / If "all" is disabled, cannot add single
                        (targetEntry.endsWith(":all") && e.startsWith(entityId + ":")) // 如果试图添加 "all"，但已存在单个配置 / If trying to add "all", but single config exists
        );

        if (alreadyExists) {
            player.sendSystemMessage(Component.translatable(
                    "command.elementalcraft.blacklist.already_exists",
                    entityId,
                    isAll ? "ALL" : type.getDisplayName().getString()
            ));
            player.sendSystemMessage(Component.translatable("command.elementalcraft.blacklist.use_remove_first"));
            return 0;
        }

        // 添加新条目并保存配置
        // Add new entry and save config
        list.add(targetEntry);
        ElementalConfig.BLACKLISTED_ENTITIES.set(list);
        ElementalConfig.SPEC.save();

        // 黑名单逻辑通常直接读取配置，无需像 BiomeBias 那样手动清理缓存
        // Blacklist logic usually reads directly from config, no need to manually clear cache like BiomeBias

        if (isAll) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.blacklist.added_all", entityId));
        } else {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.blacklist.added_single", type.getDisplayName(), entityId));
        }
        player.sendSystemMessage(Component.translatable("command.elementalcraft.blacklist.saved"));

        return 1;
    }

    /**
     * 从手持刷怪蛋对应实体的黑名单中移除指定属性或全部。
     * <p>
     * Removes the specified attribute or all attributes from the blacklist for the entity corresponding to the held spawn egg.
     *
     * @param ctx        命令上下文 / Command context
     * @param elementStr 元素字符串 / Element string
     * @return 1 表示成功，0 表示失败 / 1 for success, 0 for failure
     */
    private static int removeFromBlacklist(CommandContext<CommandSourceStack> ctx, String elementStr) {
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
        String prefix = entityId + ":";

        boolean removed = false;

        if (isAll) {
            // 移除该实体下的所有相关条目（包括 ":all" 和单个属性）
            // Remove all related entries for this entity (including ":all" and single attributes)
            removed = list.removeIf(e -> e.startsWith(prefix));
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

        // 保存更新后的配置
        // Save the updated configuration
        ElementalConfig.BLACKLISTED_ENTITIES.set(list);
        ElementalConfig.SPEC.save();

        if (isAll) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.blacklist.removed_all", entityId));
        } else {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.blacklist.removed_single", type.getDisplayName(), entityId));
        }
        player.sendSystemMessage(Component.translatable("command.elementalcraft.blacklist.saved"));

        return 1;
    }

    /**
     * 列出当前所有元素属性黑名单条目。
     * <p>
     * Lists all current elemental attribute blacklist entries.
     *
     * @param ctx 命令上下文 / Command context
     * @return 条目数量 / Number of entries
     */
    private static int listBlacklist(CommandContext<CommandSourceStack> ctx) {
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

    /**
     * 从玩家主手持有的刷怪蛋中解析出实体 ID。
     * <p>
     * Parses the entity ID from the spawn egg held in the player's main hand.
     *
     * @param player 玩家实体 / Player entity
     * @return 实体资源 ID 字符串（例如 "minecraft:zombie"），如果未持有刷怪蛋则返回 null / Entity resource ID string (e.g., "minecraft:zombie"), or null if no spawn egg is held
     */
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