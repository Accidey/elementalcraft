// src/main/java/com/xulai/elementalcraft/command/ForcedEntityCommand.java
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

/**
 * ForcedEntityCommand
 * <p>
 * 中文说明：
 * 负责注册和管理 /elementalcraft entity 指令（包含子命令 add 和 remove）。
 * 该命令允许拥有 OP 权限的玩家通过手持刷怪蛋，为特定的实体类型强制绑定或移除元素属性配置。
 * 配置直接写入文件并支持热重载。
 * <p>
 * English Description:
 * Responsible for registering and managing the /elementalcraft entity command (includes subcommands add and remove).
 * This command allows players with OP permissions to force-bind or remove elemental attribute configurations for specific entity types by holding their spawn eggs.
 * Configurations are written directly to the file and support hot-reloading.
 */
@Mod.EventBusSubscriber(modid = "elementalcraft")
public class ForcedEntityCommand {

    // ================================================================================================================
    // Constants & Suggestions / 常量与补全建议
    // ================================================================================================================

    /**
     * 可用于命令补全的元素字符串列表。
     * - 包含空字符串与 "none"，用于表示“不指定”该位置的参数。
     * - 列表顺序决定了补全显示的优先级。
     * <p>
     * List of element strings for command suggestion.
     * - Includes empty string and "none" to indicate "unspecified" for the argument.
     * - The order of the list determines the priority of suggestion display.
     */
    private static final String[] ELEMENTS = {"", "none", "fire", "frost", "thunder", "nature"};

    /**
     * 常用的固定点数补全建议。
     * 用于快速选择常见的属性数值。
     * <p>
     * Common fixed point suggestion values.
     * Used for quickly selecting common attribute values.
     */
    private static final String[] FIXED = {"50", "100", "0-100", "20-80", "50-100"};

    /**
     * 范围形式的点数补全建议。
     * 表示区间概率或权重。
     * <p>
     * Range-style point suggestion values.
     * Represents interval probability or weight.
     */
    private static final String[] RANGE = {"50", "100", "0-100", "20-80", "50-100"};

    /**
     * 元素参数的命令补全提供器。
     * <p>
     * Command suggestion provider for element arguments.
     */
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_ELEMENT =
            (ctx, builder) -> SharedSuggestionProvider.suggest(ELEMENTS, builder);

    /**
     * 点数参数的命令补全提供器。
     * - 若当前输入中包含 "-" 或为空，优先提示区间格式。
     * - 否则提示固定数值格式。
     * <p>
     * Command suggestion provider for point arguments.
     * - If the current input contains "-" or is empty, prioritizes range format suggestions.
     * - Otherwise, suggests fixed value format.
     */
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

    // ================================================================================================================
    // Registration / 注册逻辑
    // ================================================================================================================

    /**
     * 注册命令到 Brigadier 系统。
     * 命令结构：/elementalcraft entity [add|remove] ...
     * <p>
     * Registers commands to the Brigadier system.
     * Command structure: /elementalcraft entity [add|remove] ...
     *
     * @param event 命令注册事件 / Command registration event
     */
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("elementalcraft")
                        // 需要 2 级权限（OP）
                        // Requires permission level 2 (OP)
                        .requires(source -> source.hasPermission(2))

                        // 新的 entity 主节点
                        // New 'entity' main node
                        .then(Commands.literal("entity")

                                // ===== add: 添加/修改强制属性 =====
                                // ===== add: Add/Modify forced attributes =====
                                .then(Commands.literal("add")
                                        .then(Commands.argument("attack_element", StringArgumentType.string())
                                                .suggests(SUGGEST_ELEMENT)
                                                .then(Commands.argument("enhance_element", StringArgumentType.string())
                                                        .suggests(SUGGEST_ELEMENT)
                                                        .then(Commands.argument("enhance_points", StringArgumentType.string())
                                                                .suggests(SUGGEST_POINTS)
                                                                .then(Commands.argument("resist_element", StringArgumentType.string())
                                                                        .suggests(SUGGEST_ELEMENT)
                                                                        .then(Commands.argument("resist_points", StringArgumentType.greedyString())
                                                                                .suggests(SUGGEST_POINTS)
                                                                                // 完整参数执行
                                                                                // Execute with full arguments
                                                                                .executes(ctx -> executeAdd(
                                                                                        ctx,
                                                                                        getStr(ctx, "attack_element"),
                                                                                        getStr(ctx, "enhance_element"),
                                                                                        getStr(ctx, "enhance_points"),
                                                                                        getStr(ctx, "resist_element"),
                                                                                        getStr(ctx, "resist_points")
                                                                                ))
                                                                        )
                                                                        // 缺少抗性点数时默认 0
                                                                        // Default resist points to 0 if missing
                                                                        .executes(ctx -> executeAdd(
                                                                                ctx,
                                                                                getStr(ctx, "attack_element"),
                                                                                getStr(ctx, "enhance_element"),
                                                                                getStr(ctx, "enhance_points"),
                                                                                getStr(ctx, "resist_element"),
                                                                                "0"
                                                                        ))
                                                                )
                                                                // 缺少抗性元素及点数时默认空/0
                                                                // Default resist element to empty and points to 0 if missing
                                                                .executes(ctx -> executeAdd(
                                                                        ctx,
                                                                        getStr(ctx, "attack_element"),
                                                                        getStr(ctx, "enhance_element"),
                                                                        getStr(ctx, "enhance_points"),
                                                                        "",
                                                                        "0"
                                                                ))
                                                        )
                                                        // 缺少增强点数及后续参数时默认
                                                        // Default enhance points and subsequent args if missing
                                                        .executes(ctx -> executeAdd(
                                                                ctx,
                                                                getStr(ctx, "attack_element"),
                                                                getStr(ctx, "enhance_element"),
                                                                "0",
                                                                "",
                                                                "0"
                                                        ))
                                                )
                                                // 仅有攻击元素参数时默认
                                                // Default if only attack element arg is present
                                                .executes(ctx -> executeAdd(
                                                        ctx,
                                                        getStr(ctx, "attack_element"),
                                                        "",
                                                        "0",
                                                        "",
                                                        "0"
                                                ))
                                        )
                                        // 无参数情况（作为回退或提示）
                                        // Zero argument case (as fallback or hint)
                                        .executes(ctx -> executeAdd(ctx, "", "", "0", "", "0"))
                                )

                                // ===== remove: 移除强制属性 =====
                                // ===== remove: Remove forced attributes =====
                                .then(Commands.literal("remove")
                                        .executes(ctx -> executeRemove(ctx.getSource()))
                                )
                        )
        );
    }

    // ================================================================================================================
    // Execution Logic - Add / 执行逻辑 - 添加
    // ================================================================================================================

    /**
     * 执行 entity add 命令。
     * 解析参数，构建配置字符串，写入配置并刷新缓存。
     * <p>
     * Executes the entity add command.
     * Parses arguments, builds the configuration string, writes to config, and refreshes the cache.
     *
     * @param ctx          命令上下文 / Command context
     * @param attackRaw    攻击元素原始字符串 / Raw attack element string
     * @param enhanceRaw   增强元素原始字符串 / Raw enhance element string
     * @param enhanceInput 增强点数输入 / Enhance points input
     * @param resistRaw    抗性元素原始字符串 / Raw resist element string
     * @param resistInput  抗性点数输入 / Resist points input
     * @return 命令执行结果（1 为成功） / Command execution result (1 for success)
     */
    private static int executeAdd(
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

        // 检查玩家主手是否持有刷怪蛋
        // Check if the player is holding a spawn egg in the main hand
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof SpawnEggItem egg)) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forcedadd.no_egg"));
            return 0;
        }

        // 获取刷怪蛋对应的实体 ID
        // Get Entity ID corresponding to the spawn egg
        var entityType = egg.getType(stack.getTag());
        String entityKey = player.getServer()
                .registryAccess()
                .registryOrThrow(Registries.ENTITY_TYPE)
                .getKey(entityType)
                .toString();

        // 解析参数
        // Parse parameters
        ElementType attack = parse(attackRaw);
        ElementType enhance = parse(enhanceRaw);
        ElementType resist = parse(resistRaw);

        String enhanceStr = enhanceInput.isBlank() ? "0" : enhanceInput.trim();
        String resistStr = resistInput.isBlank() ? "0" : resistInput.trim();

        // 构建配置行，格式例如：minecraft:zombie,fire,frost,100,nature,50
        // Build config line, format example: minecraft:zombie,fire,frost,100,nature,50
        String line = String.format(
                "%s,%s,%s,%s,%s,%s",
                entityKey,
                attack != null ? attack.getId() : "",
                enhance != null ? enhance.getId() : "",
                enhanceStr,
                resist != null ? resist.getId() : "",
                resistStr
        );

        // 使用 ArrayList 确保列表可变，支持增删操作
        // Use ArrayList to ensure the list is mutable for add/remove operations
        List<String> list = new ArrayList<>(ElementalConfig.FORCED_ENTITIES.get());

        // 移除该实体的旧配置（防止重复）
        // Remove old config for this entity (to prevent duplicates)
        list.removeIf(s -> s.replace("\"", "").trim().startsWith(entityKey + ","));

        // 添加新配置
        // Add new config
        list.add(line);

        // 保存配置到内存和文件
        // Save config to memory and file
        ElementalConfig.FORCED_ENTITIES.set(list);
        ElementalConfig.SPEC.save();

        // 立即清理缓存，确保本次修改对当前游戏进程立即生效
        // Immediately clear cache to ensure changes take effect instantly in the current game session
        ForcedAttributeHelper.clearCache();

        // 发送成功反馈
        // Send success feedback
        player.sendSystemMessage(Component.translatable("command.elementalcraft.forcedadd.success", entityKey));
        player.sendSystemMessage(Component.translatable("command.elementalcraft.forcedadd.saved"));

        return 1;
    }

    // ================================================================================================================
    // Execution Logic - Remove / 执行逻辑 - 移除
    // ================================================================================================================

    /**
     * 执行 entity remove 命令。
     * 移除对应实体的配置，保存并刷新缓存。
     * <p>
     * Executes the entity remove command.
     * Removes config for the corresponding entity, saves, and refreshes the cache.
     *
     * @param source 命令源 / Command source
     * @return 命令执行结果 / Command execution result
     */
    private static int executeRemove(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.elementalcraft.player_only"));
            return 0;
        }

        // 检查玩家主手是否持有刷怪蛋
        // Check if the player is holding a spawn egg in the main hand
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof SpawnEggItem egg)) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forcedremove.no_egg"));
            return 0;
        }

        // 获取实体 ID
        // Get Entity ID
        var entityType = egg.getType(stack.getTag());
        String entityKey = player.getServer()
                .registryAccess()
                .registryOrThrow(Registries.ENTITY_TYPE)
                .getKey(entityType)
                .toString();

        // 读取当前配置并执行移除
        // Read current config and perform removal
        List<String> list = new ArrayList<>(ElementalConfig.FORCED_ENTITIES.get());

        int oldSize = list.size();
        // 移除匹配该实体 ID 的条目
        // Remove entry matching this entity ID
        list.removeIf(s -> s.replace("\"", "").trim().startsWith(entityKey + ","));
        int removed = oldSize - list.size();

        // 保存更新后的配置
        // Save the updated configuration
        ElementalConfig.FORCED_ENTITIES.set(list);
        ElementalConfig.SPEC.save();

        // 立即清理缓存
        // Immediately clear cache
        ForcedAttributeHelper.clearCache();

        // 发送反馈信息
        // Send feedback message
        if (removed > 0) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forcedremove.success", entityKey));
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forcedremove.saved"));
        } else {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forcedremove.not_found", entityKey));
        }

        return 1;
    }

    // ================================================================================================================
    // Utilities / 工具方法
    // ================================================================================================================

    /**
     * 安全获取字符串参数。
     * 避免参数不存在导致命令执行失败，提供默认空字符串回退。
     * <p>
     * Safely retrieve optional string arguments.
     * Prevents command failure if argument is missing by providing a default empty string fallback.
     *
     * @param ctx  命令上下文 / Command context
     * @param name 参数名称 / Argument name
     * @return 参数值或空字符串 / Argument value or empty string
     */
    private static String getStr(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            return ctx.getArgument(name, String.class).trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 将输入字符串解析为 ElementType 枚举。
     * - 空字符串、"none" 或非法值将返回 null。
     * <p>
     * Parses the input string into an ElementType enum.
     * - Empty string, "none", or invalid values return null.
     *
     * @param input 输入字符串 / Input string
     * @return 对应的元素类型或 null / Corresponding ElementType or null
     */
    private static ElementType parse(String input) {
        if (input == null || input.isBlank() || "none".equalsIgnoreCase(input)) {
            return null;
        }
        return ElementType.fromId(input.toLowerCase());
    }
}