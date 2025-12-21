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
 *
 * 中文说明：
 * 负责注册和处理 /elementalcraft blacklist 指令。
 * 用于将特定实体（或实体的特定元素属性）加入黑名单，防止其生成元素属性。
 * 支持通过手持刷怪蛋指定目标实体。
 * 数据直接写入配置文件，支持热重载（通过直接读写配置实现）。
 *
 * English description:
 * Responsible for registering and handling the /elementalcraft blacklist command.
 * Used to blacklist specific entities (or specific elemental attributes of entities) to prevent them from generating elemental attributes.
 * Supports specifying the target entity by holding a spawn egg.
 * Data is written directly to the config file, supporting hot-reloading (via direct config read/write).
 */
@Mod.EventBusSubscriber(modid = "elementalcraft")
public class BlacklistCommand {

    /**
     * 注册指令入口。
     * 监听 RegisterCommandsEvent 事件进行注册。
     *
     * Register command entry point.
     * Listens to RegisterCommandsEvent for registration.
     *
     * @param event 命令注册事件 / Command registration event
     */
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("elementalcraft")
            .requires(source -> source.hasPermission(2)) // 需要 OP 权限 / Requires OP permission
            .then(Commands.literal("blacklist")
                // add <element|all>
                // 添加黑名单条目 / Add blacklist entry
                .then(Commands.literal("add")
                    .then(Commands.argument("element", StringArgumentType.word())
                        // 建议：四种元素 + all / Suggests: four elements + all
                        .suggests((ctx, builder) -> {
                            builder.suggest("fire").suggest("frost").suggest("thunder").suggest("nature").suggest("all");
                            return builder.buildFuture();
                        })
                        .executes(ctx -> addToBlacklist(ctx, StringArgumentType.getString(ctx, "element")))
                    )
                )
                // remove <element|all>
                // 移除黑名单条目 / Remove blacklist entry
                .then(Commands.literal("remove")
                    .then(Commands.argument("element", StringArgumentType.word())
                        // 动态建议：根据手持刷怪蛋对应的已存在黑名单条目提供建议
                        // Dynamic suggestions: suggest based on existing blacklist entries for the held spawn egg
                        .suggests((ctx, builder) -> {
                            ServerPlayer player = ctx.getSource().getPlayer();
                            if (player == null) return builder.buildFuture();
                            String entityId = getHeldEntityId(player);
                            if (entityId == null) return builder.buildFuture();

                            // 遍历配置查找匹配项 / Iterate config to find matches
                            for (String entry : ElementalConfig.BLACKLISTED_ENTITIES.get()) {
                                if (entry.startsWith(entityId + ":")) {
                                    String elem = entry.substring(entityId.length() + 1);
                                    builder.suggest(elem);
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> removeFromBlacklist(ctx, StringArgumentType.getString(ctx, "element")))
                    )
                )
                // list
                // 列出所有黑名单条目 / List all blacklist entries
                .then(Commands.literal("list")
                    .executes(BlacklistCommand::listBlacklist)
                )
            )
        );
    }

    /**
     * 将手持刷怪蛋对应实体的指定属性加入黑名单。
     *
     * Add specific element (or all) to blacklist for the held spawn egg entity.
     *
     * @param ctx 命令上下文 / Command context
     * @param elementStr 元素字符串（fire, frost, ..., all） / Element string
     * @return 1 表示成功，0 表示失败 / 1 for success, 0 for failure
     */
    private static int addToBlacklist(CommandContext<CommandSourceStack> ctx, String elementStr) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.elementalcraft.player_only"));
            return 0;
        }

        // 获取手持刷怪蛋对应的实体 ID / Get Entity ID from held spawn egg
        String entityId = getHeldEntityId(player);
        if (entityId == null) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.blacklist.no_egg"));
            return 0;
        }

        boolean isAll = elementStr.equalsIgnoreCase("all");
        ElementType type = isAll ? null : ElementType.fromId(elementStr.toLowerCase());

        // 验证输入有效性 / Validate input
        if (!isAll && (type == null || type == ElementType.NONE)) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.blacklist.invalid_attribute", elementStr));
            return 0;
        }

        // 构建目标配置条目字符串 / Build target config entry string
        String targetEntry = isAll ? entityId + ":all" : entityId + ":" + (isAll ? "all" : type.getId());

        // 获取当前列表的可变副本 / Get mutable copy of current list
        List<String> list = new ArrayList<>(ElementalConfig.BLACKLISTED_ENTITIES.get());

        // 检查是否已存在相同条目或冲突条目（如已存在 all 则不能加单个）
        // Check for duplicate or conflicting entries (e.g., cannot add single if "all" exists)
        boolean alreadyExists = list.stream().anyMatch(e ->
                e.equals(targetEntry) ||
                e.equals(entityId + ":all") ||           // 如果已禁用 all，则不能再加单个 / If all is disabled, cannot add single
                (targetEntry.endsWith(":all") && e.startsWith(entityId + ":"))  // 如果要加 all，但已有单个，也阻止 / If adding all, but single exists, prevent it
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

        // 添加并保存 / Add and save
        list.add(targetEntry);
        ElementalConfig.BLACKLISTED_ENTITIES.set(list);
        ElementalConfig.SPEC.save();

        // 这里的黑名单通常是直接读取配置，无需清理缓存
        // Blacklist is usually read directly from config, no cache clearing needed here

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
     *
     * Remove specific element (or all) from blacklist for the held spawn egg entity.
     *
     * @param ctx 命令上下文 / Command context
     * @param elementStr 元素字符串 / Element string
     * @return 1 表示成功 / 1 for success
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
            // 移除该实体所有相关条目（包括 :all 和单个属性）
            // Remove all related entries for this entity (including :all and single elements)
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

        // 保存更新 / Save updates
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
     *
     * List all elemental blacklist entries.
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
     * 从玩家主手刷怪蛋解析实体ID。
     *
     * Get entity registry ID from held spawn egg.
     *
     * @param player 玩家 / Player
     * @return 实体资源 ID 字符串（例如 "minecraft:zombie"） / Entity resource ID string (e.g., "minecraft:zombie")
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