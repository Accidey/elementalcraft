// src/main/java/com/xulai/elementalcraft/ElementalCraft.java
package com.xulai.elementalcraft;

import com.mojang.logging.LogUtils;
import com.xulai.elementalcraft.command.ModCommands;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ForcedItemConfig;
import com.xulai.elementalcraft.enchantment.ModEnchantments;
import com.xulai.elementalcraft.event.CombatEvents;
import com.xulai.elementalcraft.event.PlayerTrackEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * ElementalCraft 主模组类，是模组的入口点。
 * 负责在模组加载阶段注册配置、附魔、事件监听器和命令系统。
 *
 * ElementalCraft main mod class, serving as the entry point for the mod.
 * Responsible for registering configurations, enchantments, event listeners, and command system during mod loading.
 */
@Mod(ElementalCraft.MODID)
public class ElementalCraft {
    /**
     * 模组唯一 ID，用于资源定位、事件注册等。
     *
     * Unique mod ID used for resource location, event registration, etc.
     */
    public static final String MODID = "elementalcraft";

    /**
     * 模组专用日志器，用于输出调试和运行信息。
     *
     * Dedicated mod logger for outputting debug and runtime information.
     */
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 模组构造函数，在模组加载时执行。
     * 注册配置、附魔、事件总线监听器，并输出加载完成日志。
     *
     * Mod constructor, executed during mod loading.
     * Registers configurations, enchantments, event bus listeners, and outputs loading completion log.
     */
    public ElementalCraft() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册主配置文件 elementalcraft-common.toml / Register main config file elementalcraft-common.toml
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ElementalConfig.SPEC, "elementalcraft-common.toml");

        // 注册独立强制物品属性配置文件 / Register independent forced item attribute config file
        ForcedItemConfig.register();

        // 注册所有自定义附魔 / Register all custom enchantments
        ModEnchantments.register(modEventBus);

        // 注册 Forge 事件监听器（使用类注册，内部 @SubscribeEvent 方法会自动生效）
        // Register Forge event listeners (registering classes enables internal @SubscribeEvent methods)
        MinecraftForge.EVENT_BUS.register(CombatEvents.class);
        MinecraftForge.EVENT_BUS.register(PlayerTrackEvents.class);
        MinecraftForge.EVENT_BUS.register(ModCommands.class); // 统一命令注册入口 / Unified command registration entry

        // 输出模组加载完成日志 / Output mod loading completion log
        LOGGER.info("§a[ElementalCraft] 模组加载完成！属性系统、命令、事件全部就绪！");
        LOGGER.info("[ElementalCraft] Mod loaded successfully! Elemental system, commands, and events are all ready!");
    }
}