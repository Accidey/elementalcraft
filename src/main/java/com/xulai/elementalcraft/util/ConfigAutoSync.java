package com.xulai.elementalcraft.util;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.config.ElementalVisualConfig;
import com.xulai.elementalcraft.config.ForcedItemConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        tickCounter++;
        if (tickCounter < CHECK_INTERVAL) {
            return;
        }
        tickCounter = 0;

        checkConfig(ElementalConfig.SPEC, COMMON, () -> {
            ElementalConfig.refreshCache();
            CustomBiomeBias.clearCache();
            ForcedAttributeHelper.clearCache();

            ElementalCraft.LOGGER.info("[ElementalCraft] Detected change in elementalcraft-common.toml, caches refreshed automatically.");
        });

        checkConfig(ForcedItemConfig.SPEC, FORCED_ITEMS, () -> {
            ForcedItemHelper.clearCache();

            ElementalCraft.LOGGER.info("[ElementalCraft] Detected change in elementalcraft-forced-items.toml, caches refreshed automatically.");
        });

        checkConfig(ElementalFireNatureReactionsConfig.SPEC, FIRE_NATURE, () -> {
            ElementalFireNatureReactionsConfig.refreshCache();

            ElementalCraft.LOGGER.info("[ElementalCraft] Detected change in elementalcraft-fire-nature-reactions.toml, caches refreshed automatically.");
        });

        checkConfig(ElementalVisualConfig.SPEC, VISUALS, () -> {
            ElementalVisualConfig.refreshCache();

            ElementalCraft.LOGGER.info("[ElementalCraft] Detected change in elementalcraft-visuals.toml, caches refreshed automatically.");
        });

        checkConfig(ElementalThunderFrostReactionsConfig.SPEC, THUNDER_FROST, () -> {
            ElementalThunderFrostReactionsConfig.refreshCache();

            ElementalCraft.LOGGER.info("[ElementalCraft] Detected change in elementalcraft-thunder-frost-reactions.toml, caches refreshed automatically.");
        });
    }

    private static void checkConfig(ForgeConfigSpec spec, String fileName, Runnable onReload) {
        if (!(spec.getValues() instanceof CommentedFileConfig fileConfig)) return;

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
