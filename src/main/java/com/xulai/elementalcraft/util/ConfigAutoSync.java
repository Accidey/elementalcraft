package com.xulai.elementalcraft.util;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.config.ElementalISSIntegrationConfig;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.config.ElementalVisualConfig;
import com.xulai.elementalcraft.config.ForcedItemConfig;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.IConfigEvent;
import net.minecraftforge.fml.config.ModConfig;

import java.io.File;
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
    private static final String ISS_INTEGRATION = "ElementalCraft/elementalcraft-iss-integration.toml";

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

        if (ModList.get().isLoaded("irons_spellbooks")) {
            checkConfig(ISS_INTEGRATION, () -> {
                ElementalISSIntegrationConfig.refreshCache();

                ElementalCraft.LOGGER.info("[ElementalCraft] Detected change in elementalcraft-iss-integration.toml, caches refreshed automatically.");
            });
        }
    }

    private static ModConfig getModConfig(String fileName) {
        return ConfigTracker.INSTANCE.fileMap().get(fileName);
    }

    private static void fireEvent(ModConfig modConfig) {
        try {
            var method = ModConfig.class.getDeclaredMethod("fireEvent", IConfigEvent.class);
            method.setAccessible(true);
            method.invoke(modConfig, IConfigEvent.reloading(modConfig));
        } catch (Exception ignored) {}
    }

    private static void checkConfig(String fileName, Runnable onReload) {
        ModConfig modConfig = getModConfig(fileName);
        if (modConfig == null || modConfig.getConfigData() == null) return;

        File file = modConfig.getFullPath().toFile();
        if (!file.exists()) return;

        long currentModified = file.lastModified();
        Long lastModified = FILE_TIMESTAMPS.get(fileName);

        if (lastModified == null) {
            FILE_TIMESTAMPS.put(fileName, currentModified);

            if (fileName.equals(COMMON)) ElementalConfig.refreshCache();
            if (fileName.equals(FORCED_ITEMS)) ForcedItemHelper.clearCache();
            if (fileName.equals(FIRE_NATURE)) ElementalFireNatureReactionsConfig.refreshCache();
            if (fileName.equals(VISUALS)) ElementalVisualConfig.refreshCache();
            if (fileName.equals(THUNDER_FROST)) ElementalThunderFrostReactionsConfig.refreshCache();
            if (fileName.equals(ISS_INTEGRATION)) ElementalISSIntegrationConfig.refreshCache();

            return;
        }

        if (currentModified > lastModified) {
            FILE_TIMESTAMPS.put(fileName, currentModified);

            try {
                ((CommentedFileConfig) modConfig.getConfigData()).load();
                modConfig.getSpec().afterReload();
                fireEvent(modConfig);
                onReload.run();
            } catch (Exception e) {
                ElementalCraft.LOGGER.error("[ElementalCraft] Failed to auto-reload config: {}", fileName, e);
            }
        }
    }

    public static void reloadAll() {
        String[] paths = { COMMON, FORCED_ITEMS, FIRE_NATURE, VISUALS, THUNDER_FROST, ISS_INTEGRATION };
        for (String path : paths) {
            ModConfig modConfig = getModConfig(path);
            if (modConfig == null || modConfig.getConfigData() == null) continue;
            try {
                ((CommentedFileConfig) modConfig.getConfigData()).load();
                modConfig.getSpec().afterReload();
                fireEvent(modConfig);
            } catch (Exception e) {
                ElementalCraft.LOGGER.error("[ElementalCraft] Failed to reload config: {}", path, e);
            }
        }
        ElementalConfig.refreshCache();
        CustomBiomeBias.clearCache();
        ForcedAttributeHelper.clearCache();
        ForcedItemHelper.clearCache();
        ElementalFireNatureReactionsConfig.refreshCache();
        ElementalVisualConfig.refreshCache();
        ElementalThunderFrostReactionsConfig.refreshCache();
        if (ModList.get().isLoaded("irons_spellbooks")) {
            ElementalISSIntegrationConfig.refreshCache();
        }
        FILE_TIMESTAMPS.clear();
    }
}
