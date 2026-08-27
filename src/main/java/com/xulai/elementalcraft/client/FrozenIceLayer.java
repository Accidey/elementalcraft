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
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID, value = Dist.CLIENT)
public class FrozenIceLayer {

    @SuppressWarnings("removal")
    private static final ResourceLocation ICE_TEXTURE = new ResourceLocation("minecraft:textures/block/packed_ice.png");

    private static final Map<UUID, Boolean> freezeCache = new HashMap<>();

    @SubscribeEvent
    public static void onMobEffectAdded(MobEffectEvent.Added event) {
        if (event.getEffectInstance() != null && event.getEffectInstance().getEffect() == ModMobEffects.FREEZE.get()) {
            freezeCache.put(event.getEntity().getUUID(), true);
        }
    }

    @SubscribeEvent
    public static void onMobEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getEffect() == ModMobEffects.FREEZE.get()) {
            freezeCache.remove(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void onMobEffectExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null && event.getEffectInstance().getEffect() == ModMobEffects.FREEZE.get()) {
            freezeCache.remove(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();

        boolean hasFreezeEffect = freezeCache.containsKey(entity.getUUID());
        boolean hasFrozenTicks = false;

        if (!hasFreezeEffect) return;

        float alpha = calcAlpha(entity);
        if (alpha <= 0.01f) return;

        renderIceBox(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), entity, alpha);
    }

    private static float calcAlpha(LivingEntity entity) {
        if (!freezeCache.containsKey(entity.getUUID())) return 0;
        MobEffectInstance effect = entity.getEffect(ModMobEffects.FREEZE.get());
        if (effect != null) {
            int dur = effect.getDuration();
            if (dur < 20) return 1.0f * (dur / 20.0f);
        }
        return 1.0f;
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
