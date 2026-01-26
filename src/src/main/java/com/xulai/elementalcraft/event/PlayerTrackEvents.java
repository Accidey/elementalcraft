// src/main/java/com/xulai/elementalcraft/event/PlayerTrackEvents.java
package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.logic.MobAttributeLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * PlayerTrackEvents
 * <p>
 * 中文说明：
 * 玩家追踪事件处理类。
 * 负责监听服务端玩家的 Tick 事件，周期性地（每秒）扫描玩家周围的生物。
 * 如果发现尚未被赋予属性的生物，则调用 {@link MobAttributeLogic} 进行处理。
 * <p>
 * English Description:
 * Player tracking event handler class.
 * Responsible for listening to server-side player Tick events and periodically (every second) scanning mobs around the player.
 * If mobs without assigned attributes are found, {@link MobAttributeLogic} is called to process them.
 */
@Mod.EventBusSubscriber(modid = "elementalcraft")
public class PlayerTrackEvents {

    /**
     * 监听玩家 Tick 事件。
     * 每秒（20 ticks）扫描一次玩家周围的生物并尝试应用元素属性。
     * 仅在服务端执行，且只处理尚未设置属性的存活生物。
     * <p>
     * Listens to the Player Tick event.
     * Scans mobs around the player every second (20 ticks) and attempts to apply elemental attributes.
     * Executes only on the server side and processes only living mobs that haven't had attributes set yet.
     *
     * @param event 玩家 Tick 事件 / Player Tick event
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 仅在服务端运行
        // Run only on server side
        if (event.side.isClient()) return;
        
        // 仅在 Tick 开始阶段运行，避免在一帧内重复计算
        // Run only at the start of the tick to avoid duplicate calculations in one frame
        if (event.phase != TickEvent.Phase.START) return;
        
        if (!(event.player instanceof ServerPlayer player)) return;

        // 性能优化：每 20 tick (1秒) 执行一次，避免每 tick 扫描造成卡顿
        // Performance optimization: Execute every 20 ticks (1 second) to prevent lag from scanning every tick
        if (player.tickCount % 20 != 0) return;

        ServerLevel level = player.serverLevel();

        // 扫描玩家周围 20x5x20 范围内的生物
        // Scan mobs within a 20x5x20 range around the player
        level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(20, 5, 20),
                mob -> mob.isAlive() && !mob.getPersistentData().getBoolean("ElementalCraft_AttributesSet")
        ).forEach(MobAttributeLogic::processMob);
    }
}