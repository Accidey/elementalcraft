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
 *
 * 中文说明：
 * 负责监听服务端玩家的 Tick 事件。
 * 作为入口类，周期性扫描玩家周围的生物，并调用逻辑类处理属性赋予。
 *
 * English description:
 * Responsible for listening to server-side player Tick events.
 * Acts as the entry point, periodically scanning mobs around players and calling the logic class to handle attribute assignment.
 */
@Mod.EventBusSubscriber(modid = "elementalcraft")
public class PlayerTrackEvents {

    /**
     * 每秒扫描玩家周围的生物并应用元素属性（服务端）。
     *
     * Scans mobs around players every second and applies elemental attributes (server-side).
     *
     * @param event 玩家 Tick 事件 / Player tick event
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side.isClient()) return;
        if (event.phase != TickEvent.Phase.START) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        
        // 每 20 tick (1秒) 执行一次，节省性能
        // Execute every 20 ticks (1 second) to save performance
        if (player.tickCount % 20 != 0) return;

        ServerLevel level = player.serverLevel();
        
        // 扫描玩家周围 20x10x20 范围内的生物
        // Scan mobs within 20x10x20 range around the player
        level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(20, 5, 20),
                mob -> mob.isAlive() && !mob.getPersistentData().getBoolean("ElementalCraft_AttributesSet")
        ).forEach(MobAttributeLogic::processMob);
    }
}