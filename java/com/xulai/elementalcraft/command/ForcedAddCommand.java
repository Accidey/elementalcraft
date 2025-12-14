// src/main/java/com/xulai/elementalcraft/command/ForcedAddCommand.java
package com.xulai.elementalcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.util.ElementType;
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

import java.util.List;

/**
 * ForcedAddCommand
 *
 * 中文：
 * 用于为指定实体（通过刷怪蛋）强制绑定元素属性配置。
 * 该命令允许直接写入完整的“攻击 / 强化 / 抗性”规则，
 * 并保存到配置文件中，用于覆盖或指定实体的元素行为。
 *
 * English:
 * This command forces elemental attributes onto an entity via spawn egg.
 * It writes a full elemental configuration line directly into config.
 */
@Mod.EventBusSubscriber(modid = "elementalcraft")
public class ForcedAddCommand {

    /**
     * 可用于命令补全的元素字符串列表
     *
     * 中文：
     * - 包含空字符串与 none，用于表示“不指定”
     * - 顺序即为补全优先级
     *
     * English:
     * - Includes empty and "none" for optional arguments
     */
    private static final String[] ELEMENTS = {"", "none", "fire", "frost", "thunder", "nature"};

    /**
     * 常用的固定点数补全
     *
     * 中文：用于快速选择常见数值
     * English: Fixed common values
     */
    private static final String[] FIXED = {"0", "50", "100"};

    /**
     * 范围形式的点数补全
     *
     * 中文：表示区间概率 / 权重
     * English: Range-style values
     */
    private static final String[] RANGE = {"0-100", "20-80", "50-100"};

    /**
     * 元素参数的命令补全提供器
     */
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_ELEMENT =
            (ctx, builder) -> SharedSuggestionProvider.suggest(ELEMENTS, builder);

    /**
     * 点数参数的命令补全提供器
     *
     * 中文：
     * - 若输入中包含 "-"，或为空，优先提示区间
     * - 否则提示固定数值
     *
     * English:
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

    /**
     * 注册 /elementalcraft forcedadd 命令
     *
     * 中文：
     * - 权限等级：2（OP）
     * - 支持从“全参数”到“零参数”的多种写法
     *
     * English:
     * - Permission level: 2 (OP)
     * - Supports fully optional argument chains
     */
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("elementalcraft")
                .requires(source -> source.hasPermission(2))
                .then(
                    Commands.literal("forcedadd")
                        .then(
                            Commands.argument("attack_element", StringArgumentType.string())
                                .suggests(SUGGEST_ELEMENT)
                                .then(
                                    Commands.argument("enhance_element", StringArgumentType.string())
                                        .suggests(SUGGEST_ELEMENT)
                                        .then(
                                            Commands.argument("enhance_points", StringArgumentType.string())
                                                .suggests(SUGGEST_POINTS)
                                                .then(
                                                    Commands.argument("resist_element", StringArgumentType.string())
                                                        .suggests(SUGGEST_ELEMENT)
                                                        .then(
                                                            Commands.argument("resist_points", StringArgumentType.greedyString())
                                                                .suggests(SUGGEST_POINTS)
                                                                .executes(ctx -> execute(
                                                                    ctx,
                                                                    getStr(ctx, "attack_element"),
                                                                    getStr(ctx, "enhance_element"),
                                                                    getStr(ctx, "enhance_points"),
                                                                    getStr(ctx, "resist_element"),
                                                                    getStr(ctx, "resist_points")
                                                                ))
                                                        )
                                                        .executes(ctx -> execute(
                                                            ctx,
                                                            getStr(ctx, "attack_element"),
                                                            getStr(ctx, "enhance_element"),
                                                            getStr(ctx, "enhance_points"),
                                                            getStr(ctx, "resist_element"),
                                                            "0"
                                                        ))
                                                )
                                                .executes(ctx -> execute(
                                                    ctx,
                                                    getStr(ctx, "attack_element"),
                                                    getStr(ctx, "enhance_element"),
                                                    getStr(ctx, "enhance_points"),
                                                    "",
                                                    "0"
                                                ))
                                        )
                                        .executes(ctx -> execute(
                                            ctx,
                                            getStr(ctx, "attack_element"),
                                            getStr(ctx, "enhance_element"),
                                            "0",
                                            "",
                                            "0"
                                        ))
                                )
                                .executes(ctx -> execute(
                                    ctx,
                                    getStr(ctx, "attack_element"),
                                    "",
                                    "0",
                                    "",
                                    "0"
                                ))
                        )
                        .executes(ctx -> execute(ctx, "", "", "0", "", "0"))
                )
        );
    }

    /**
     * 安全获取字符串参数
     *
     * 中文：避免参数不存在导致命令执行失败
     * English: Safely retrieve optional string arguments
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
     *
     * 中文：
     * - 空字符串 / none → null
     * - 非法值 → null
     *
     * English:
     * - Empty or "none" returns null
     */
    private static ElementType parse(String input) {
        if (input == null || input.isBlank() || "none".equalsIgnoreCase(input)) {
            return null;
        }
        return ElementType.fromId(input.toLowerCase());
    }

    /**
     * 命令的核心执行逻辑
     *
     * 中文：
     * - 读取玩家手持刷怪蛋
     * - 解析所有元素参数
     * - 写入 6 字段强制配置
     * - 保存到配置文件
     *
     * English:
     * - Reads spawn egg
     * - Parses all elemental parameters
     * - Writes a 6-field forced entry
     */
    private static int execute(
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

        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof SpawnEggItem egg)) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forcedadd.no_egg"));
            return 0;
        }

        var entityType = egg.getType(stack.getTag());
        String entityKey = player.getServer()
                .registryAccess()
                .registryOrThrow(Registries.ENTITY_TYPE)
                .getKey(entityType)
                .toString();

        ElementType attack = parse(attackRaw);
        ElementType enhance = parse(enhanceRaw);
        ElementType resist = parse(resistRaw);

        String enhanceStr = enhanceInput.isBlank() ? "0" : enhanceInput.trim();
        String resistStr = resistInput.isBlank() ? "0" : resistInput.trim();

        String line = String.format(
                "%s,%s,%s,%s,%s,%s",
                entityKey,
                attack != null ? attack.getId() : "",
                enhance != null ? enhance.getId() : "",
                enhanceStr,
                resist != null ? resist.getId() : "",
                resistStr
        );

        List<String> list = new java.util.ArrayList<>(ElementalConfig.FORCED_ENTITIES.get());
        list.removeIf(s -> s.replace("\"", "").trim().startsWith(entityKey + ","));
        list.add(line);

        ElementalConfig.FORCED_ENTITIES.set(list);
        ElementalConfig.SPEC.save();

        player.sendSystemMessage(Component.translatable("command.elementalcraft.forcedadd.success", entityKey));

        // 注意：该输出更偏向调试用途，当前设计下仍然保留
        player.sendSystemMessage(Component.literal("§7" + line));

        player.sendSystemMessage(Component.translatable("command.elementalcraft.forcedadd.saved"));

        return 1;
    }
}
