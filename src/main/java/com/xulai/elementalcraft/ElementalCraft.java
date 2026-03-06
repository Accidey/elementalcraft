package com.xulai.elementalcraft;

import com.mojang.logging.LogUtils;
import com.xulai.elementalcraft.command.ModCommands;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ElementalReactionConfig;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.config.ElementalVisualConfig;
import com.xulai.elementalcraft.config.ForcedItemConfig;
import com.xulai.elementalcraft.enchantment.ModEnchantments;
import com.xulai.elementalcraft.event.TooltipEvents;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.sound.ModSounds;
import com.xulai.elementalcraft.util.CustomBiomeBias;
import com.xulai.elementalcraft.util.ForcedAttributeHelper;
import com.xulai.elementalcraft.util.ForcedItemHelper;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(ElementalCraft.MODID)
public class ElementalCraft {
    public static final String MODID = "elementalcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ElementalCraft() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ElementalConfig.SPEC, "elementalcraft-common.toml");
        ForcedItemConfig.register();
        ElementalReactionConfig.register();
        ElementalVisualConfig.register();
        ElementalThunderFrostReactionsConfig.register();

        ModEnchantments.register(modEventBus);
        ModMobEffects.register(modEventBus);
        ModSounds.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onConfigReload);
        modEventBus.addListener(this::onConfigLoad);

        MinecraftForge.EVENT_BUS.register(TooltipEvents.class);
        MinecraftForge.EVENT_BUS.register(ModCommands.class);

        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListeners);

        LOGGER.info("§a[ElementalCraft] Mod Constructed!");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        ElementalConfig.refreshCache();
        ElementalReactionConfig.refreshCache();
        ElementalVisualConfig.refreshCache();
        ElementalThunderFrostReactionsConfig.refreshCache();
        LOGGER.info("[ElementalCraft] Common Setup: Config cache initialized.");
    }

    public void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == ElementalConfig.SPEC) {
            ElementalConfig.refreshCache();
            LOGGER.info("[ElementalCraft] Config Loaded: elementalcraft-common.toml");
        }
        if (event.getConfig().getSpec() == ElementalReactionConfig.SPEC) {
            ElementalReactionConfig.refreshCache();
            LOGGER.info("[ElementalCraft] Config Loaded: elementalcraft-reactions.toml");
        }
        if (event.getConfig().getSpec() == ElementalVisualConfig.SPEC) {
            ElementalVisualConfig.refreshCache();
        }
        if (event.getConfig().getSpec() == ElementalThunderFrostReactionsConfig.SPEC) {
            ElementalThunderFrostReactionsConfig.refreshCache();
            LOGGER.info("[ElementalCraft] Config Loaded: elementalcraft-thunderfrost-reactions.toml");
        }
    }

    public void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == ElementalConfig.SPEC) {
            ElementalConfig.refreshCache();
            CustomBiomeBias.clearCache();
            ForcedAttributeHelper.clearCache();
            LOGGER.info("[ElementalCraft] Config reloaded from file: elementalcraft-common.toml");
        }
        if (event.getConfig().getSpec() == ForcedItemConfig.SPEC) {
            ForcedItemHelper.clearCache();
            LOGGER.info("[ElementalCraft] Config reloaded from file: elementalcraft-forced-items.toml");
        }
        if (event.getConfig().getSpec() == ElementalReactionConfig.SPEC) {
            ElementalReactionConfig.refreshCache();
            LOGGER.info("[ElementalCraft] Config reloaded from file: elementalcraft-reactions.toml");
        }
        if (event.getConfig().getSpec() == ElementalVisualConfig.SPEC) {
            ElementalVisualConfig.refreshCache();
        }
        if (event.getConfig().getSpec() == ElementalThunderFrostReactionsConfig.SPEC) {
            ElementalThunderFrostReactionsConfig.refreshCache();
            LOGGER.info("[ElementalCraft] Config reloaded from file: elementalcraft-thunderfrost-reactions.toml");
        }
    }

    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ResourceManagerReloadListener() {
            @Override
            public void onResourceManagerReload(ResourceManager resourceManager) {
                ElementalConfig.refreshCache();
                ElementalReactionConfig.refreshCache();
                ElementalVisualConfig.refreshCache();
                ElementalThunderFrostReactionsConfig.refreshCache();
                CustomBiomeBias.clearCache();
                ForcedAttributeHelper.clearCache();
                ForcedItemHelper.clearCache();
            }
        });
    }
}