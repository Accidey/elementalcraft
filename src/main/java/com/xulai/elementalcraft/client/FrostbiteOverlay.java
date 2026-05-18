package com.xulai.elementalcraft.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
import com.xulai.elementalcraft.potion.ModMobEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID, value = Dist.CLIENT)
public class FrostbiteOverlay {

    private static final ResourceLocation POWDER_SNOW_OUTLINE = new ResourceLocation("textures/misc/powder_snow_outline.png");
    private static float currentDisplayAlpha = 0.0f;

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.FROSTBITE.type()) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || player.isSpectator()) return;

        boolean isFrozen = player.hasEffect(ModMobEffects.FREEZE.get());
        MobEffectInstance frostbiteEffect = player.getEffect(ModMobEffects.FROSTBITE.get());

        float targetAlpha;
        if (isFrozen) {
            targetAlpha = 1.0f;
        } else if (frostbiteEffect != null) {
            int stacks = frostbiteEffect.getAmplifier() + 1;
            int maxStacks = ElementalThunderFrostReactionsConfig.frostbiteMaxTotalStacks;
            if (maxStacks <= 0) maxStacks = 1;
            targetAlpha = (float) stacks / (float) maxStacks;
        } else {
            targetAlpha = 0.0f;
        }

        float lerpSpeed = 0.05f;
        if (targetAlpha > currentDisplayAlpha) {
            currentDisplayAlpha = Math.min(targetAlpha, currentDisplayAlpha + lerpSpeed);
        } else if (targetAlpha < currentDisplayAlpha) {
            currentDisplayAlpha = Math.max(targetAlpha, currentDisplayAlpha - lerpSpeed);
        }

        if (currentDisplayAlpha <= 0.01f) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, currentDisplayAlpha);
        RenderSystem.setShaderTexture(0, POWDER_SNOW_OUTLINE);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.vertex(0.0, screenHeight, -90.0).uv(0.0f, 1.0f).endVertex();
        bufferbuilder.vertex(screenWidth, screenHeight, -90.0).uv(1.0f, 1.0f).endVertex();
        bufferbuilder.vertex(screenWidth, 0.0, -90.0).uv(1.0f, 0.0f).endVertex();
        bufferbuilder.vertex(0.0, 0.0, -90.0).uv(0.0f, 0.0f).endVertex();
        tesselator.end();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
