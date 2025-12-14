// src/main/java/com/xulai/elementalcraft/command/ForcedRemoveCommand.java
package com.xulai.elementalcraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.xulai.elementalcraft.config.ElementalConfig;
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
 * ForcedRemoveCommand 类负责注册和管理 /elementalcraft forcedremove 命令。
 * 该命令允许 OP 玩家通过手持刷怪蛋移除指定实体类型的强制元素属性配置。
 * 执行后会从配置文件中删除对应实体的强制属性条目，并保存配置。
 *
 * ForcedRemoveCommand class is responsible for registering and handling the /elementalcraft forcedremove command.
 * This command allows OP players to remove forced elemental attribute configuration for a specific entity type by holding its spawn egg.
 * Upon execution, it removes the corresponding entry from the configuration file and saves the changes.
 */
@Mod.EventBusSubscriber(modid = "elementalcraft")
public class ForcedRemoveCommand {

    /**
     * 在服务器命令注册事件中注册 /elementalcraft forcedremove 命令。
     * 需要 OP 权限（level 2）。
     *
     * Registers the /elementalcraft forcedremove command during the server command registration event.
     * Requires OP permission (level 2).
     *
     * @param event 命令注册事件 / Command registration event
     */
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("elementalcraft")
            .requires(source -> source.hasPermission(2)) // 需要 OP 权限 / Requires OP permission
            .then(Commands.literal("forcedremove")
                // 执行移除逻辑 / Execute removal logic
                .executes(ctx -> execute(ctx.getSource()))
            )
        );
    }

    /**
     * 执行 /elementalcraft forcedremove 命令的主要逻辑。
     * 检查玩家手持物品是否为刷怪蛋，若是则移除对应实体类型的强制属性配置。
     *
     * Main logic for executing the /elementalcraft forcedremove command.
     * Checks if the player is holding a spawn egg, and if so, removes the forced attribute configuration for the corresponding entity type.
     *
     * @param source 命令来源（通常为玩家） / Command source (typically a player)
     * @return 命令执行结果（1 表示成功） / Command execution result (1 = success)
     */
    private static int execute(CommandSourceStack source) {
        // 必须由玩家执行 / Must be executed by a player
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("command.elementalcraft.player_only"));
            return 0;
        }

        // 获取玩家主手物品 / Get the item in the player's main hand
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof SpawnEggItem egg)) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forcedremove.no_egg"));
            return 0;
        }

        // 从刷怪蛋获取对应实体类型，并转换为注册键字符串（如 "minecraft:zombie"）
        // Retrieve the entity type from the spawn egg and convert it to its registry key string (e.g., "minecraft:zombie")
        var entityType = egg.getType(stack.getTag());
        String entityKey = player.getServer()
                .registryAccess()
                .registryOrThrow(Registries.ENTITY_TYPE)
                .getKey(entityType)
                .toString();

        // 读取当前强制实体配置列表 / Read current forced entities configuration list
        List<String> list = new ArrayList<>(ElementalConfig.FORCED_ENTITIES.get());

        // 计算移除前的数量，用于判断是否实际移除了条目
        // Calculate the size before removal to determine if any entries were actually removed
        int oldSize = list.size();
        list.removeIf(s -> s.replace("\"", "").trim().startsWith(entityKey + ","));
        int removed = oldSize - list.size();

        // 保存更新后的配置 / Save the updated configuration
        ElementalConfig.FORCED_ENTITIES.set(list);
        ElementalConfig.SPEC.save();

        // 根据是否移除成功发送对应反馈消息
        // Send appropriate feedback message based on whether removal was successful
        if (removed > 0) {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forcedremove.success", entityKey));
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forcedremove.saved"));
        } else {
            player.sendSystemMessage(Component.translatable("command.elementalcraft.forcedremove.not_found", entityKey));
        }

        return 1;
    }
}