package com.xulai.elementalcraft;

import com.mojang.logging.LogUtils;
import com.xulai.elementalcraft.client.ModParticles;
import com.xulai.elementalcraft.command.ModCommands;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
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
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Mod(ElementalCraft.MODID)
public class ElementalCraft {
    public static final String MODID = "elementalcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ElementalCraft() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        migrateConfigFiles();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ElementalConfig.SPEC, "ElementalCraft/elementalcraft-common.toml");
        ForcedItemConfig.register("ElementalCraft/elementalcraft-forced-items.toml");
        ElementalFireNatureReactionsConfig.register("ElementalCraft/elementalcraft-fire-nature-reactions.toml");
        ElementalVisualConfig.register("ElementalCraft/elementalcraft-visuals.toml");
        ElementalThunderFrostReactionsConfig.register("ElementalCraft/elementalcraft-thunder-frost-reactions.toml");

        ModEnchantments.register(modEventBus);
        ModMobEffects.register(modEventBus);
        ModSounds.register(modEventBus);
        ModParticles.PARTICLE_TYPES.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onConfigReload);
        modEventBus.addListener(this::onConfigLoad);

        MinecraftForge.EVENT_BUS.register(TooltipEvents.class);
        MinecraftForge.EVENT_BUS.register(ModCommands.class);
        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListeners);

        LOGGER.info("§a[ElementalCraft] Mod Constructed!");
    }

    private void migrateConfigFiles() {
        try {
            Path configRoot = FMLPaths.CONFIGDIR.get();
            Path oldDir = configRoot;
            Path newDir = configRoot.resolve("ElementalCraft");

            Files.createDirectories(newDir);

            String[] filesToMigrate = {
                "elementalcraft-common.toml",
                "elementalcraft-forced-items.toml",
                "elementalcraft-reactions.toml",
                "elementalcraft-visuals.toml",
                "elementalcraft-thunderfrost-reactions.toml"
            };

            // 第一步：将根目录的文件移动到 ElementalCraft 文件夹
            for (String file : filesToMigrate) {
                Path oldPath = oldDir.resolve(file);
                Path newPath = newDir.resolve(file);
                if (Files.exists(oldPath) && !Files.exists(newPath)) {
                    Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.info("[ElementalCraft] 迁移配置文件: {}", file);
                }
            }

            // 第二步：重命名 火与自然反应配置文件
            Path oldReactionPath = newDir.resolve("elementalcraft-reactions.toml");
            Path newReactionPath = newDir.resolve("elementalcraft-fire-nature-reactions.toml");
            if (Files.exists(oldReactionPath) && !Files.exists(newReactionPath)) {
                Files.move(oldReactionPath, newReactionPath, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("[ElementalCraft] 重命名反应配置文件: elementalcraft-reactions.toml -> elementalcraft-fire-nature-reactions.toml");
            }

            // 第三步：重命名 雷霜反应配置文件
            Path oldThunderFrostPath = newDir.resolve("elementalcraft-thunderfrost-reactions.toml");
            Path newThunderFrostPath = newDir.resolve("elementalcraft-thunder-frost-reactions.toml");
            if (Files.exists(oldThunderFrostPath) && !Files.exists(newThunderFrostPath)) {
                Files.move(oldThunderFrostPath, newThunderFrostPath, StandardCopyOption.REPLACE_EXISTING);
                LOGGER.info("[ElementalCraft] 重命名反应配置文件: elementalcraft-thunderfrost-reactions.toml -> elementalcraft-thunder-frost-reactions.toml");
            }

        } catch (Exception e) {
            LOGGER.warn("[ElementalCraft] 配置文件迁移失败: {}", e.getMessage());
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        ElementalConfig.refreshCache();
        ElementalFireNatureReactionsConfig.refreshCache();
        ElementalVisualConfig.refreshCache();
        ElementalThunderFrostReactionsConfig.refreshCache();
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
    }

    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ResourceManagerReloadListener() {
            @Override
            public void onResourceManagerReload(ResourceManager resourceManager) {
                ElementalConfig.refreshCache();
                ElementalFireNatureReactionsConfig.refreshCache();
                ElementalVisualConfig.refreshCache();
                ElementalThunderFrostReactionsConfig.refreshCache();
                CustomBiomeBias.clearCache();
                ForcedAttributeHelper.clearCache();
                ForcedItemHelper.clearCache();
            }
        });
    }
}