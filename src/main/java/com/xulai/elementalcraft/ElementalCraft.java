package com.xulai.elementalcraft;

import com.mojang.logging.LogUtils;
import com.xulai.elementalcraft.client.ModParticles;
import com.xulai.elementalcraft.command.ModCommands;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.config.ElementalISSIntegrationConfig;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.config.ElementalVisualConfig;
import com.xulai.elementalcraft.config.ForcedItemConfig;
import com.xulai.elementalcraft.enchantment.ModEnchantments;
import com.xulai.elementalcraft.event.TooltipEvents;
import com.xulai.elementalcraft.potion.ModMobEffects;
import com.xulai.elementalcraft.sound.ModSounds;
import com.xulai.elementalcraft.util.CustomBiomeBias;
import com.xulai.elementalcraft.network.FireCounterLockPacket;
import com.xulai.elementalcraft.util.ForcedAttributeHelper;
import com.xulai.elementalcraft.util.ForcedItemHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

@Mod(ElementalCraft.MODID)
public class ElementalCraft {
    public static final String MODID = "elementalcraft";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "main"),
            () -> "1", s -> true, s -> true);

    public ElementalCraft() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        migrateConfigFiles();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ElementalConfig.SPEC, "ElementalCraft/elementalcraft-common.toml");
        ForcedItemConfig.register("ElementalCraft/elementalcraft-forced-items.toml");
        ElementalFireNatureReactionsConfig.register("ElementalCraft/elementalcraft-fire-nature-reactions.toml");
        ElementalVisualConfig.register("ElementalCraft/elementalcraft-visuals.toml");
        ElementalThunderFrostReactionsConfig.register("ElementalCraft/elementalcraft-thunder-frost-reactions.toml");

        if (ModList.get().isLoaded("irons_spellbooks")) {
            ElementalISSIntegrationConfig.register("ElementalCraft/elementalcraft-iss-integration.toml");
        }

        ModEnchantments.register(modEventBus);
        ModMobEffects.register(modEventBus);
        ModSounds.register(modEventBus);
        ModParticles.PARTICLE_TYPES.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onConfigReload);
        modEventBus.addListener(this::onConfigLoad);

        CHANNEL.registerMessage(0, FireCounterLockPacket.class,
                FireCounterLockPacket::encode, FireCounterLockPacket::decode,
                FireCounterLockPacket::handle);

        MinecraftForge.EVENT_BUS.register(TooltipEvents.class);
        MinecraftForge.EVENT_BUS.register(ModCommands.class);
        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListeners);

        LOGGER.info("§a[ElementalCraft] Mod Constructed!");
    }

    private void migrateConfigFiles() {
        try {
            Path configRoot = FMLPaths.CONFIGDIR.get();

            String[] filesToDelete = {
                "elementalcraft-common.toml",
                "elementalcraft-forced-items.toml",
                "elementalcraft-reactions.toml",
                "elementalcraft-fire-nature-reactions.toml",
                "elementalcraft-visuals.toml",
                "elementalcraft-thunderfrost-reactions.toml",
                "elementalcraft-thunder-frost-reactions.toml",
                "elementalcraft-iss-integration.toml"
            };

            for (String file : filesToDelete) {
                Path oldPath = configRoot.resolve(file);
                if (Files.exists(oldPath)) {
                    Files.delete(oldPath);
                    LOGGER.info("[ElementalCraft] 删除旧配置文件: {}", file);
                }
            }

        } catch (Exception e) {
            LOGGER.warn("[ElementalCraft] 清理旧配置文件失败: {}", e.getMessage());
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        ElementalConfig.refreshCache();
        ElementalFireNatureReactionsConfig.refreshCache();
        ElementalVisualConfig.refreshCache();
        ElementalThunderFrostReactionsConfig.refreshCache();
        if (ModList.get().isLoaded("irons_spellbooks")) {
            ElementalISSIntegrationConfig.refreshCache();
        }
        LOGGER.info("[ElementalCraft] Common Setup: Config cache initialized.");
    }

    public void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == ElementalConfig.SPEC) {
            ElementalConfig.refreshCache();
            LOGGER.info("[ElementalCraft] Config Loaded: elementalcraft-common.toml");
        }
        if (event.getConfig().getSpec() == ElementalFireNatureReactionsConfig.SPEC) {
            ElementalFireNatureReactionsConfig.refreshCache();
            LOGGER.info("[ElementalCraft] Config Loaded: elementalcraft-fire-nature-reactions.toml");
        }
        if (event.getConfig().getSpec() == ElementalVisualConfig.SPEC) {
            ElementalVisualConfig.refreshCache();
        }
        if (event.getConfig().getSpec() == ElementalThunderFrostReactionsConfig.SPEC) {
            ElementalThunderFrostReactionsConfig.refreshCache();
            LOGGER.info("[ElementalCraft] Config Loaded: elementalcraft-thunder-frost-reactions.toml");
        }
        if (event.getConfig().getSpec() == ElementalISSIntegrationConfig.SPEC) {
            ElementalISSIntegrationConfig.refreshCache();
            LOGGER.info("[ElementalCraft] Config Loaded: elementalcraft-iss-integration.toml");
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
        if (event.getConfig().getSpec() == ElementalFireNatureReactionsConfig.SPEC) {
            ElementalFireNatureReactionsConfig.refreshCache();
            LOGGER.info("[ElementalCraft] Config reloaded from file: elementalcraft-fire-nature-reactions.toml");
        }
        if (event.getConfig().getSpec() == ElementalVisualConfig.SPEC) {
            ElementalVisualConfig.refreshCache();
        }
        if (event.getConfig().getSpec() == ElementalThunderFrostReactionsConfig.SPEC) {
            ElementalThunderFrostReactionsConfig.refreshCache();
            LOGGER.info("[ElementalCraft] Config reloaded from file: elementalcraft-thunder-frost-reactions.toml");
        }
        if (event.getConfig().getSpec() == ElementalISSIntegrationConfig.SPEC) {
            ElementalISSIntegrationConfig.refreshCache();
            LOGGER.info("[ElementalCraft] Config reloaded from file: elementalcraft-iss-integration.toml");
        }
    }

    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ResourceManagerReloadListener() {
            @Override
            public void onResourceManagerReload(ResourceManager resourceManager) {
                ElementalConfig.refreshCache();
                ElementalFireNatureReactionsConfig.refreshCache();
                ElementalVisualConfig.refreshCache();
                ElementalThunderFrostReactionsConfig.refreshCache();
                if (ModList.get().isLoaded("irons_spellbooks")) {
                    ElementalISSIntegrationConfig.refreshCache();
                }
                CustomBiomeBias.clearCache();
                ForcedAttributeHelper.clearCache();
                ForcedItemHelper.clearCache();
            }
        });
    }
}