package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.util.UpdateChecker;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID, value = Dist.CLIENT)
public class UpdateCheckHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!UpdateChecker.hasUpdate()) return;

        Component header = Component.translatable("update.elementalcraft.header")
                .append(Component.translatable("update.elementalcraft.available")
                        .withStyle(ChatFormatting.GREEN));

        Component modrinthLink = Component.translatable("update.elementalcraft.modrinth")
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.BLUE)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, UpdateChecker.MODRINTH_URL))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("update.elementalcraft.modrinth.hover"))));

        Component curseforgeLink = Component.translatable("update.elementalcraft.curseforge")
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.BLUE)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, UpdateChecker.CURSEFORGE_URL))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("update.elementalcraft.curseforge.hover"))));

        event.getEntity().sendSystemMessage(header);
        event.getEntity().sendSystemMessage(modrinthLink);
        event.getEntity().sendSystemMessage(curseforgeLink);
    }
}