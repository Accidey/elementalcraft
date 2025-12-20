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
 *
 * 中文说明：
 * 负责注册和管理 /elementalcraft entity 指令（子命令：add/remove）。
 * 该命令允许 OP 玩家通过手持刷怪蛋，为特定实体类型强制绑定或移除元素属性配置。
 * 调整了命令结构为 /elementalcraft entity add/remove，使其更符合直觉。
 *
 * English description:
 * Responsible for registering and handling the /elementalcraft entity command (subcommands: add/remove).
 * Allows OP players to force-bind or remove elemental attribute configurations for specific entity types by holding spawn eggs.
 * Adjusted command structure to /elementalcraft entity add/remove for better intuitiveness.
 */
@Mod.EventBusSubscriber(modid = "elementalcraft")
public class ForcedEntityCommand {

    // ================================================================================================================
    // Constants & Suggestions / 常量与补全建议
    // ================================================================================================================

    /**
     * 可用于命令补全的元素字符串列表
     * - 包含空字符串与 none，用于表示“不指定”
     * - 顺序即为补全优先级
     *
     * List of element strings for command suggestion
     * - Includes empty and "none" for optional arguments
     */
    private static final String[] ELEMENTS = {"", "none", "fire", "frost", "thunder", "nature"};

    /**
     * 常用的固定点数补全
     * 用于快速选择常见数值
     *
     * Fixed common values for quick selection
     */
    private static final String[] FIXED = {"0", "50", "100"};

    /**
     * 范围形式的点数补全
     * 表示区间概率 / 权重
     *
     * Range-style values representing probability/weight intervals
     */
    private static final String[] RANGE = {"0-100", "20-80", "50-100"};

    /**
     * 元素参数的命令补全提供器
     *
     * Command suggestion provider for element arguments
     */
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_ELEMENT =
            (ctx, builder) -> SharedSuggestionProvider.suggest(ELEMENTS, builder);

    /**
     * 点数参数的命令补全提供器
     * - 若输入中包含 "-"，或为空，优先提示区间
     * - 否则提示固定数值
     *
     * Command suggestion provider for point arguments
     * - Suggest ranges if dash is detected
     * - Otherwise suggest fixed values
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
     * 注册命令到 Brigadier 系统
     * 结构变更为：/elementalcraft entity [add|remove] ...
     *
     * Registers commands to the Brigadier system.
     * Structure changed to: /elementalcraft entity [add|remove] ...
     */
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("elementalcraft")
                .requires(source -> source.hasPermission(2)) // 需要 OP 权限 / Requires OP permission

                // 新的 entity 主节点 / New 'entity' main node
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
                                            .executes(ctx -> executeAdd(
                                                ctx,
                                                getStr(ctx, "attack_element"),
                                                getStr(ctx, "enhance_element"),
                                                getStr(ctx, "enhance_points"),
                                                getStr(ctx, "resist_element"),
                                                getStr(ctx, "resist_points")
                                            ))
                                        )
                                        .executes(ctx -> executeAdd(
                                            ctx,
                                            getStr(ctx, "attack_element"),
                                            getStr(ctx, "enhance_element"),
                                            getStr(ctx, "enhance_points"),
                                            getStr(ctx, "resist_element"),
                                            "0"
                                        ))
                                    )
                                    .executes(ctx -> executeAdd(
                                        ctx,
                                        getStr(ctx, "attack_element"),
                                        getStr(ctx, "enhance_element"),
                                        getStr(ctx, "enhance_points"),
                                        "",
                                        "0"
                                    ))
                                )
                                .executes(ctx -> executeAdd(
                                    ctx,
                                    getStr(ctx, "attack_element"),
                                    getStr(ctx, "enhance_element"),
                                    "0",
                                    "",
                                    "0"
                                ))
                            )
                            .executes(ctx -> executeAdd(
                                ctx,
                                getStr(ctx, "attack_element"),
                                "",
                                "0",
                                "",
                                "0"
                            ))
                        )
                        // 零参数情况 / Zero argument case
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
     * 执行 entity add 命令
     * 解析参数，构建配置字符串，写入配置并刷新缓存
     *
     * Executes entity add command
     * Parses arguments, builds config string, writes to config and refreshes cache
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

        // 检查手持物品是否为刷怪蛋
        // Check if held item is a spawn egg
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof SpawnEggItem egg)) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forcedadd.no_egg"));
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

        // 解析参数
        // Parse parameters
        ElementType attack = parse(attackRaw);
        ElementType enhance = parse(enhanceRaw);
        ElementType resist = parse(resistRaw);

        String enhanceStr = enhanceInput.isBlank() ? "0" : enhanceInput.trim();
        String resistStr = resistInput.isBlank() ? "0" : resistInput.trim();

        // 构建配置行，格式：minecraft:zombie,fire,frost,100,nature,50
        // Build config line
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
        // Use ArrayList to ensure list is mutable for add/remove operations
        List<String> list = new ArrayList<>(ElementalConfig.FORCED_ENTITIES.get());
        
        // 移除旧配置（防止重复）
        // Remove old config to prevent duplicates
        list.removeIf(s -> s.replace("\"", "").trim().startsWith(entityKey + ","));
        
        // 添加新配置
        // Add new config
        list.add(line);

        // 保存到内存和文件
        // Save to memory and file
        ElementalConfig.FORCED_ENTITIES.set(list);
        ElementalConfig.SPEC.save();

        // 立即清理缓存，确保本次修改对当前游戏进程立即生效
        // Immediately clear cache to ensure changes take effect instantly
        ForcedAttributeHelper.clearCache();

        player.sendSystemMessage(Component.translatable("command.elementalcraft.forcedadd.success", entityKey));
        player.sendSystemMessage(Component.translatable("command.elementalcraft.forcedadd.saved"));

        return 1;
    }

    // ================================================================================================================
    // Execution Logic - Remove / 执行逻辑 - 移除
    // ================================================================================================================

    /**
     * 执行 entity remove 命令
     * 移除对应实体的配置，保存并刷新缓存
     *
     * Executes entity remove command
     * Removes config for the entity, saves and refreshes cache
     */
    private static int executeRemove(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.elementalcraft.player_only"));
            return 0;
        }

        // 检查手持物品是否为刷怪蛋
        // Check if held item is a spawn egg
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

        // 发送反馈
        // Send feedback
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
     * 安全获取字符串参数
     * 避免参数不存在导致命令执行失败
     *
     * Safely retrieve optional string arguments
     * Prevents command failure if argument is missing
     */
    private static String getStr(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            return ctx.getArgument(name, String.class).trim();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 将输入字符串解析为 ElementType
     * - 空字符串 / none → null
     * - 非法值 → null
     *
     * Parses input string to ElementType
     * - Empty or "none" returns null
     */
    private static ElementType parse(String input) {
        if (input == null || input.isBlank() || "none".equalsIgnoreCase(input)) {
            return null;
        }
        return ElementType.fromId(input.toLowerCase());
    }
}