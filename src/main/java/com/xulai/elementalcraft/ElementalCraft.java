// src/main/java/com/xulai/elementalcraft/ElementalCraft.java
package com.xulai.elementalcraft;

import com.mojang.logging.LogUtils;
import com.xulai.elementalcraft.command.ModCommands;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ElementalReactionConfig;
import com.xulai.elementalcraft.config.ForcedItemConfig;
import com.xulai.elementalcraft.enchantment.ModEnchantments;
import com.xulai.elementalcraft.event.CombatEvents;
import com.xulai.elementalcraft.event.InventoryAutoForceEvents;
import com.xulai.elementalcraft.event.PlayerTrackEvents;
import com.xulai.elementalcraft.event.TooltipEvents;
import com.xulai.elementalcraft.potion.ModMobEffects;
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

/**
 * ElementalCraft
 *
 * 中文说明：
 * ElementalCraft 的主入口类。
 * 负责注册配置文件、附魔、药水效果、事件监听器以及处理配置文件的热重载逻辑。
 *
 * English description:
 * The main entry class for ElementalCraft.
 * Responsible for registering configuration files, enchantments, potion effects, event listeners, and handling config hot-reloading logic.
 */
@Mod(ElementalCraft.MODID)
public class ElementalCraft {
    public static final String MODID = "elementalcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 构造函数，初始化模组组件。
     *
     * Constructor, initializes mod components.
     */
    public ElementalCraft() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册所有配置文件
        // Register all configuration files
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ElementalConfig.SPEC, "elementalcraft-common.toml");
        ForcedItemConfig.register();
        ElementalReactionConfig.register(); // 注册新的潮湿反应配置 / Register new wetness reaction config

        // 注册延迟注册器（附魔、药水效果）
        // Register deferred registers (Enchantments, Potion Effects)
        ModEnchantments.register(modEventBus);
        ModMobEffects.register(modEventBus);

        // 注册模组生命周期事件监听器
        // Register mod lifecycle event listeners
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onConfigReload);
        modEventBus.addListener(this::onConfigLoad);

        // 注册 Forge 事件总线监听器（游戏逻辑事件）
        // Register Forge event bus listeners (Game logic events)
        
        // 注意：带有 @Mod.EventBusSubscriber 注解的类（如 WetnessHandler, ConfigAutoSync, SteamReactionHandler）不需要在此处手动注册。
        // Note: Classes with @Mod.EventBusSubscriber (like WetnessHandler, ConfigAutoSync, SteamReactionHandler) do not need manual registration here.
        
        MinecraftForge.EVENT_BUS.register(CombatEvents.class);
        MinecraftForge.EVENT_BUS.register(PlayerTrackEvents.class);
        MinecraftForge.EVENT_BUS.register(InventoryAutoForceEvents.class);
        MinecraftForge.EVENT_BUS.register(TooltipEvents.class);
        MinecraftForge.EVENT_BUS.register(ModCommands.class);
        
        // 注册资源重载监听器 (/reload)
        // Register resource reload listener (/reload)
        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListeners);

        LOGGER.info("§a[ElementalCraft] Mod Constructed!");
    }

    /**
     * 通用设置阶段，初始化静态缓存。
     *
     * Common setup phase, initializes static caches.
     *
     * @param event FML通用设置事件 / FML common setup event
     */
    private void commonSetup(final FMLCommonSetupEvent event) {
        ElementalConfig.refreshCache();
        ElementalReactionConfig.refreshCache();
        LOGGER.info("[ElementalCraft] Common Setup: Config cache initialized.");
    }

    /**
     * 配置文件首次加载时的处理逻辑。
     *
     * Handling logic when the configuration file is loaded for the first time.
     *
     * @param event 模组配置加载事件 / Mod config loading event
     */
    public void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == ElementalConfig.SPEC) {
            ElementalConfig.refreshCache();
            LOGGER.info("[ElementalCraft] Config Loaded: elementalcraft-common.toml");
        }
        if (event.getConfig().getSpec() == ElementalReactionConfig.SPEC) {
            ElementalReactionConfig.refreshCache();
            LOGGER.info("[ElementalCraft] Config Loaded: elementalcraft-reactions.toml");
        }
        // ForcedItemConfig 通常在需要时懒加载或通过 Reload 触发
    }

    /**
     * 配置文件重载时的处理逻辑（通过 GUI 或文件修改触发）。
     *
     * Handling logic when the configuration file is reloaded (triggered via GUI or file modification).
     *
     * @param event 模组配置重载事件 / Mod config reloading event
     */
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
    }

    /**
     * 注册资源重载监听器（响应 /reload 指令）。
     * 这确保了即使是数据包重载，缓存也会被刷新，保证数据一致性。
     *
     * Registers a resource reload listener (responds to /reload command).
     * This ensures caches are refreshed even on datapack reloads, guaranteeing data consistency.
     *
     * @param event 添加重载监听器事件 / Add reload listener event
     */
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ResourceManagerReloadListener() {
            @Override
            public void onResourceManagerReload(ResourceManager resourceManager) {
                // 强制刷新所有缓存
                // Force refresh all caches
                ElementalConfig.refreshCache();
                ElementalReactionConfig.refreshCache();
                CustomBiomeBias.clearCache();
                ForcedAttributeHelper.clearCache();
                ForcedItemHelper.clearCache();
            }
        });
    }
}