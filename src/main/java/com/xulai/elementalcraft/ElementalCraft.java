// src/main/java/com/xulai/elementalcraft/ElementalCraft.java
package com.xulai.elementalcraft;

import com.mojang.logging.LogUtils;
import com.xulai.elementalcraft.command.ModCommands;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ForcedItemConfig;
import com.xulai.elementalcraft.enchantment.ModEnchantments;
import com.xulai.elementalcraft.event.CombatEvents;
import com.xulai.elementalcraft.event.InventoryAutoForceEvents;
import com.xulai.elementalcraft.event.PlayerTrackEvents;
import com.xulai.elementalcraft.event.TooltipEvents;
import com.xulai.elementalcraft.util.ConfigAutoSync;
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
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * ElementalCraft 主模组类。
 * 实现了配置文件的热重载监听（手动修改文件）以及 /reload 指令的响应。
 *
 * ElementalCraft main mod class.
 * Implements hot-reload listeners for config files (manual edits) and response to the /reload command.
 */
@Mod(ElementalCraft.MODID)
public class ElementalCraft {
    public static final String MODID = "elementalcraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ElementalCraft() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册配置文件 / Register configs
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ElementalConfig.SPEC, "elementalcraft-common.toml");
        ForcedItemConfig.register();

        // 注册附魔 / Register enchantments
        ModEnchantments.register(modEventBus);

        // 监听配置重载事件
        // Listen for config reload events
        modEventBus.addListener(this::onConfigReload);

        // 注册 Forge 事件总线 / Register Forge event bus
        MinecraftForge.EVENT_BUS.register(CombatEvents.class);
        MinecraftForge.EVENT_BUS.register(PlayerTrackEvents.class);
        MinecraftForge.EVENT_BUS.register(InventoryAutoForceEvents.class);
        MinecraftForge.EVENT_BUS.register(TooltipEvents.class);
        MinecraftForge.EVENT_BUS.register(ModCommands.class);
        
        // 注册自动同步器，确保文件修改立即生效
        // Register auto-synchronizer to ensure file modifications take effect immediately
        MinecraftForge.EVENT_BUS.register(ConfigAutoSync.class);

        // 监听 /reload 指令事件
        // Listen for /reload command event
        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListeners);

        LOGGER.info("§a[ElementalCraft] Mod Loaded Successfully!");
    }

    /**
     * 当配置文件被修改（手动编辑或指令保存）时触发。
     * 清理对应缓存，使新配置立即生效。
     *
     * Triggered when config files are modified (manual edit or command save).
     * Clears corresponding caches to apply new configs immediately.
     */
    public void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == ElementalConfig.SPEC) {
            CustomBiomeBias.clearCache();
            ForcedAttributeHelper.clearCache();
            LOGGER.info("[ElementalCraft] Config reloaded from file: elementalcraft-common.toml");
        }
        if (event.getConfig().getSpec() == ForcedItemConfig.SPEC) {
            ForcedItemHelper.clearCache();
            LOGGER.info("[ElementalCraft] Config reloaded from file: elementalcraft-forced-items.toml");
        }
    }

    /**
     * 当玩家执行 /reload 指令时触发。
     * 强制清理所有缓存，作为双重保险。
     *
     * Triggered when player executes /reload command.
     * Forcefully clears all caches as a failsafe.
     */
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ResourceManagerReloadListener() {
            @Override
            public void onResourceManagerReload(ResourceManager resourceManager) {
                CustomBiomeBias.clearCache();
                ForcedAttributeHelper.clearCache();
                ForcedItemHelper.clearCache();
                LOGGER.info("[ElementalCraft] /reload detected, forced cache refresh.");
            }
        });
    }
}