// src/main/java/com/xulai/elementalcraft/util/ConfigAutoSync.java
package com.xulai.elementalcraft.util;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ForcedItemConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * ConfigAutoSync
 *
 * 中文说明：
 * 自动配置同步器。
 * 解决 /reload 有时无法更新 Config 或手动修改文件不生效的问题。
 * 该类通过监听 ServerTickEvent，每隔一段时间检查配置文件的“最后修改时间戳”。
 * 一旦检测到文件变更，立即强制重新加载内存中的配置并清理缓存。
 *
 * English description:
 * Automatic Configuration Synchronizer.
 * Solves the issue where /reload sometimes fails to update Config or manual file edits do not take effect.
 * This class listens to ServerTickEvent and checks the "last modified timestamp" of config files periodically.
 * Upon detecting a file change, it immediately forces a reload of the configuration in memory and clears caches.
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class ConfigAutoSync {

    /**
     * 存储文件路径和最后修改时间的映射。
     * 用于比对文件是否发生变更。
     *
     * Map storing file paths and their last modified timestamps.
     * Used to compare whether the file has changed.
     */
    private static final Map<String, Long> FILE_TIMESTAMPS = new HashMap<>();

    /**
     * 服务器 Tick 计数器，用于控制检查频率。
     *
     * Server tick counter, used to control check frequency.
     */
    private static int tickCounter = 0;

    /**
     * 检查间隔（Ticks）。40 ticks ≈ 2秒。
     * 设置为 40 可以平衡性能与响应速度。
     *
     * Check interval (Ticks). 40 ticks ≈ 2 seconds.
     * Setting to 40 balances performance and response speed.
     */
    private static final int CHECK_INTERVAL = 100;

    /**
     * 服务器 Tick 监听器。
     * 周期性检查配置文件状态。
     *
     * Server Tick Listener.
     * Periodically checks the status of configuration files.
     *
     * @param event 服务器 Tick 事件 / Server Tick Event
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        // 仅在 Phase.END 执行，确保逻辑在每帧结束时运行
        // Execute only in Phase.END to ensure logic runs at the end of each frame
        if (event.phase != TickEvent.Phase.END) return;

        // 使用计数器控制频率，每 40 ticks 执行一次
        // Use counter to control frequency, execute once every 40 ticks
        if (tickCounter++ % CHECK_INTERVAL != 0) return;

        // 检查通用配置文件（包含生物群系偏好、强制实体属性等）
        // Check common configuration file (includes biome bias, forced entity attributes, etc.)
        checkConfig(ElementalConfig.SPEC, "elementalcraft-common.toml", () -> {
            // 刷新静态配置缓存
            // Refresh static configuration cache
            ElementalConfig.refreshCache();

            // 清理群系偏好缓存
            // Clear biome bias cache
            CustomBiomeBias.clearCache();
            // 清理强制实体属性缓存
            // Clear forced entity attribute cache
            ForcedAttributeHelper.clearCache();
            
            ElementalCraft.LOGGER.info("[ElementalCraft] Detected change in elementalcraft-common.toml, caches refreshed automatically.");
        });

        // 检查强制物品配置文件（包含强制武器、强制护甲配置）
        // Check forced items configuration file (includes forced weapons, forced armor configs)
        checkConfig(ForcedItemConfig.SPEC, "elementalcraft-forced-items.toml", () -> {
            // 清理强制物品缓存
            // Clear forced item cache
            ForcedItemHelper.clearCache();

            ElementalCraft.LOGGER.info("[ElementalCraft] Detected change in elementalcraft-forced-items.toml, caches refreshed automatically.");
        });
    }

    /**
     * 检查单个配置文件的逻辑。
     * 如果文件修改时间发生变化，则重新加载配置并执行回调。
     *
     * Logic for checking a single configuration file.
     * If the file modification time changes, reloads the configuration and executes the callback.
     *
     * @param spec 配置规范 / Config specification
     * @param fileName 文件名（用于日志和键值） / File name (for logging and keys)
     * @param onReload 发生变更时的回调操作 / Callback action when change occurs
     */
    private static void checkConfig(ForgeConfigSpec spec, String fileName, Runnable onReload) {
        // 获取底层的 FileConfig 对象，如果类型不匹配则跳过
        // Get the underlying FileConfig object, skip if type mismatch
        if (!(spec.getValues() instanceof CommentedFileConfig fileConfig)) return;

        // 获取实际的文件对象
        // Get the actual File object
        File file = fileConfig.getFile();
        if (file == null || !file.exists()) return;

        // 获取当前文件的最后修改时间戳
        // Get the current last modified timestamp of the file
        long currentModified = file.lastModified();
        Long lastModified = FILE_TIMESTAMPS.get(fileName);

        // 如果是第一次运行，初始化时间戳并记录
        // If running for the first time, initialize and record timestamp
        if (lastModified == null) {
            FILE_TIMESTAMPS.put(fileName, currentModified);
            // 首次运行时也刷新一次缓存，防止 Config 加载延迟导致静态变量为 0
            // Refresh cache on first run to prevent static variables from being 0 due to delayed Config loading
            if (fileName.contains("common")) ElementalConfig.refreshCache();
            return;
        }

        // 如果当前文件修改时间大于记录的时间，说明文件已被外部修改
        // If current file modification time is greater than recorded time, the file has been modified externally
        if (currentModified > lastModified) {
            // 更新内存中的时间戳
            // Update timestamp in memory
            FILE_TIMESTAMPS.put(fileName, currentModified);

            // 核心步骤：强制从磁盘加载最新数据到内存配置对象中
            // Core step: Force load the latest data from disk into the memory config object
            fileConfig.load();

            // 执行回调（通常用于清理相关 Helper 类的缓存）
            // Execute callback (usually used to clear caches of related Helper classes)
            onReload.run();
        }
    }
}