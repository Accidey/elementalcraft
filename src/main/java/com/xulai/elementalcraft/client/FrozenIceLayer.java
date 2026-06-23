package com.xulai.elementalcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.potion.ModMobEffects;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID, value = Dist.CLIENT)
public class FrozenIceLayer {

    @SuppressWarnings("removal")
    private static final ResourceLocation ICE_TEXTURE = new ResourceLocation("minecraft:textures/block/packed_ice.png");

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();

        boolean hasFreezeEffect = entity.hasEffect(ModMobEffects.FREEZE.get());
        boolean hasFrozenTicks = entity.getTicksFrozen() > 5 && entity.hasEffect(ModMobEffects.FROSTBITE.get());

        if (!hasFreezeEffect && hasFrozenTicks && (entity.getTicksFrozen() >= 300 || entity.isInPowderSnow)) {
            hasFrozenTicks = false;
        }

        if (!hasFreezeEffect && !hasFrozenTicks) return;

        float alpha = calcAlpha(entity, hasFreezeEffect, hasFrozenTicks);
        if (alpha <= 0.01f) return;

        renderIceBox(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), entity, alpha);
    }

    private static float calcAlpha(LivingEntity entity, boolean hasFreezeEffect, boolean hasFrozenTicks) {
        float fullAlpha = 1.0f;

        if (hasFreezeEffect) {
            MobEffectInstance effect = entity.getEffect(ModMobEffects.FREEZE.get());
            if (effect != null) {
                int dur = effect.getDuration();
                if (dur < 20) return fullAlpha * (dur / 20.0f);
            }
            return fullAlpha;
        }

        if (hasFrozenTicks) {
            int ticks = entity.getTicksFrozen();
            if (ticks < 20) return fullAlpha * (ticks / 20.0f);
            return fullAlpha;
        }

        return 0;
    }

    private static void renderIceBox(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                     LivingEntity entity, float alpha) {
        float hw = entity.getBbWidth() / 2.0f + 0.15f;
        float hd = hw;
        float top = entity.getBbHeight() + 0.15f;
        float bot = -0.15f;

        RenderType renderType = RenderType.entityTranslucent(ICE_TEXTURE);
        VertexConsumer consumer = buffer.getBuffer(renderType);

        var pose = poseStack.last().pose();
        var normal = poseStack.last().normal();

        // Front (+Z)
        consumer.vertex(pose, -hw, bot, hd).color(1, 1, 1, alpha).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 0, 1).endVertex();
        consumer.vertex(pose, -hw, top, hd).color(1, 1, 1, alpha).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 0, 1).endVertex();
        consumer.vertex(pose,  hw, top, hd).color(1, 1, 1, alpha).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 0, 1).endVertex();
        consumer.vertex(pose,  hw, bot, hd).color(1, 1, 1, alpha).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 0, 1).endVertex();

        // Back (-Z)
        consumer.vertex(pose,  hw, bot, -hd).color(1, 1, 1, alpha).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 0, -1).endVertex();
        consumer.vertex(pose,  hw, top, -hd).color(1, 1, 1, alpha).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 0, -1).endVertex();
        consumer.vertex(pose, -hw, top, -hd).color(1, 1, 1, alpha).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 0, -1).endVertex();
        consumer.vertex(pose, -hw, bot, -hd).color(1, 1, 1, alpha).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 0, -1).endVertex();

        // Left (-X)
        consumer.vertex(pose, -hw, bot, -hd).color(1, 1, 1, alpha).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, -1, 0, 0).endVertex();
        consumer.vertex(pose, -hw, top, -hd).color(1, 1, 1, alpha).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, -1, 0, 0).endVertex();
        consumer.vertex(pose, -hw, top,  hd).color(1, 1, 1, alpha).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, -1, 0, 0).endVertex();
        consumer.vertex(pose, -hw, bot,  hd).color(1, 1, 1, alpha).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, -1, 0, 0).endVertex();

        // Right (+X)
        consumer.vertex(pose, hw, bot,  hd).color(1, 1, 1, alpha).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 1, 0, 0).endVertex();
        consumer.vertex(pose, hw, top,  hd).color(1, 1, 1, alpha).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 1, 0, 0).endVertex();
        consumer.vertex(pose, hw, top, -hd).color(1, 1, 1, alpha).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 1, 0, 0).endVertex();
        consumer.vertex(pose, hw, bot, -hd).color(1, 1, 1, alpha).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 1, 0, 0).endVertex();

        // Top (+Y)
        consumer.vertex(pose, -hw, top,  hd).color(1, 1, 1, alpha).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 1, 0).endVertex();
        consumer.vertex(pose, -hw, top, -hd).color(1, 1, 1, alpha).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 1, 0).endVertex();
        consumer.vertex(pose,  hw, top, -hd).color(1, 1, 1, alpha).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 1, 0).endVertex();
        consumer.vertex(pose,  hw, top,  hd).color(1, 1, 1, alpha).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 1, 0).endVertex();

        // Bottom (-Y)
        consumer.vertex(pose, -hw, bot, -hd).color(1, 1, 1, alpha).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, -1, 0).endVertex();
        consumer.vertex(pose,  hw, bot, -hd).color(1, 1, 1, alpha).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, -1, 0).endVertex();
        consumer.vertex(pose,  hw, bot,  hd).color(1, 1, 1, alpha).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, -1, 0).endVertex();
        consumer.vertex(pose, -hw, bot,  hd).color(1, 1, 1, alpha).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, -1, 0).endVertex();
    }
}
