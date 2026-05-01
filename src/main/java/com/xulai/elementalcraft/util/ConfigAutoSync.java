package com.xulai.elementalcraft.util;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.config.ElementalVisualConfig;
import com.xulai.elementalcraft.config.ForcedItemConfig;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class ConfigAutoSync {

    private static final Map<String, Long> FILE_TIMESTAMPS = new HashMap<>();

    private static int tickCounter = 0;

    private static final int CHECK_INTERVAL = 100;

    private static final String COMMON = "ElementalCraft/elementalcraft-common.toml";
    private static final String FORCED_ITEMS = "ElementalCraft/elementalcraft-forced-items.toml";
    private static final String FIRE_NATURE = "ElementalCraft/elementalcraft-fire-nature-reactions.toml";
    private static final String VISUALS = "ElementalCraft/elementalcraft-visuals.toml";
    private static final String THUNDER_FROST = "ElementalCraft/elementalcraft-thunder-frost-reactions.toml";

    private static final Map<String, CommentedFileConfig> FILE_CONFIG_CACHE = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        tickCounter++;
        if (tickCounter < CHECK_INTERVAL) {
            return;
        }
        tickCounter = 0;

        checkConfig(COMMON, () -> {
            ElementalConfig.refreshCache();
            CustomBiomeBias.clearCache();
            ForcedAttributeHelper.clearCache();

            ElementalCraft.LOGGER.info("[ElementalCraft] Detected change in elementalcraft-common.toml, caches refreshed automatically.");
        });

        checkConfig(FORCED_ITEMS, () -> {
            ForcedItemHelper.clearCache();

            ElementalCraft.LOGGER.info("[ElementalCraft] Detected change in elementalcraft-forced-items.toml, caches refreshed automatically.");
        });

        checkConfig(FIRE_NATURE, () -> {
            ElementalFireNatureReactionsConfig.refreshCache();

            ElementalCraft.LOGGER.info("[ElementalCraft] Detected change in elementalcraft-fire-nature-reactions.toml, caches refreshed automatically.");
        });

        checkConfig(VISUALS, () -> {
            ElementalVisualConfig.refreshCache();

            ElementalCraft.LOGGER.info("[ElementalCraft] Detected change in elementalcraft-visuals.toml, caches refreshed automatically.");
        });

        checkConfig(THUNDER_FROST, () -> {
            ElementalThunderFrostReactionsConfig.refreshCache();

            ElementalCraft.LOGGER.info("[ElementalCraft] Detected change in elementalcraft-thunder-frost-reactions.toml, caches refreshed automatically.");
        });
    }

    private static CommentedFileConfig getOrCreateFileConfig(String fileName) {
        CommentedFileConfig cached = FILE_CONFIG_CACHE.get(fileName);
        if (cached != null) {
            return cached;
        }

        Path configPath = FMLPaths.CONFIGDIR.get().resolve(fileName);
        File configFile = configPath.toFile();
        if (!configFile.exists()) {
            return null;
        }

        CommentedFileConfig fileConfig = CommentedFileConfig.builder(configPath)
                .sync()
                .preserveInsertionOrder()
                .build();
        fileConfig.load();
        FILE_CONFIG_CACHE.put(fileName, fileConfig);
        return fileConfig;
    }

    private static void checkConfig(String fileName, Runnable onReload) {
        CommentedFileConfig fileConfig = getOrCreateFileConfig(fileName);
        if (fileConfig == null) return;

        File file = fileConfig.getFile();
        if (file == null || !file.exists()) return;

        long currentModified = file.lastModified();
        Long lastModified = FILE_TIMESTAMPS.get(fileName);

        if (lastModified == null) {
            FILE_TIMESTAMPS.put(fileName, currentModified);

            if (fileName.equals(COMMON)) ElementalConfig.refreshCache();
            if (fileName.equals(FORCED_ITEMS)) ForcedItemHelper.clearCache();
            if (fileName.equals(FIRE_NATURE)) ElementalFireNatureReactionsConfig.refreshCache();
            if (fileName.equals(VISUALS)) ElementalVisualConfig.refreshCache();
            if (fileName.equals(THUNDER_FROST)) ElementalThunderFrostReactionsConfig.refreshCache();

            return;
        }

        if (currentModified > lastModified) {
            FILE_TIMESTAMPS.put(fileName, currentModified);

            try {
                fileConfig.load();
                onReload.run();
            } catch (Exception e) {
                ElementalCraft.LOGGER.error("[ElementalCraft] Failed to auto-reload config: {}", fileName, e);
            }
        }
    }
}
