package com.xulai.elementalcraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalThunderFrostReactionsConfig;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID, value = Dist.CLIENT)
public class FrostbiteSnowLayer {

    @SuppressWarnings("removal")
    private static final ResourceLocation SNOW_TEXTURE = new ResourceLocation("minecraft:textures/block/powder_snow.png");

    private static final Map<UUID, Integer> frostbiteCache = new HashMap<>();

    @SubscribeEvent
    public static void onMobEffectAdded(net.minecraftforge.event.entity.living.MobEffectEvent.Added event) {
        MobEffectInstance effectInstance = event.getEffectInstance();
        if (effectInstance != null && effectInstance.getEffect() == ModMobEffects.FROSTBITE.get()) {
            frostbiteCache.put(event.getEntity().getUUID(), effectInstance.getAmplifier() + 1);
        }
    }

    @SubscribeEvent
    public static void onMobEffectRemoved(net.minecraftforge.event.entity.living.MobEffectEvent.Remove event) {
        if (event.getEffect() == ModMobEffects.FROSTBITE.get()) {
            frostbiteCache.remove(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void onMobEffectExpired(net.minecraftforge.event.entity.living.MobEffectEvent.Expired event) {
        if (event.getEffectInstance() != null && event.getEffectInstance().getEffect() == ModMobEffects.FROSTBITE.get()) {
            frostbiteCache.remove(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();

        if (entity.hasEffect(ModMobEffects.FREEZE.get())) return;

        Integer cached = frostbiteCache.get(entity.getUUID());
        if (cached == null || cached <= 0) return;
        int stacks = cached;
        int maxStacks = ElementalThunderFrostReactionsConfig.frostbiteMaxTotalStacks;
        if (maxStacks <= 0) maxStacks = 5;

        float coverage = Math.min(1.0f, (float) stacks / maxStacks);
        float hw = entity.getBbWidth() / 2.0f + 0.2f;
        float hd = hw;
        float top = entity.getBbHeight() * coverage;
        float bot = -0.1f;
        float alpha = 1.0f;
        float uvTop = coverage;

        VertexConsumer consumer = event.getMultiBufferSource().getBuffer(RenderType.entityTranslucent(SNOW_TEXTURE));
        var pose = event.getPoseStack().last().pose();
        var normal = event.getPoseStack().last().normal();
        int light = event.getPackedLight();

        consumer.vertex(pose, -hw, bot, hd).color(1, 1, 1, alpha).uv(0, uvTop).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 0, 1).endVertex();
        consumer.vertex(pose, -hw, top, hd).color(1, 1, 1, alpha).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 0, 1).endVertex();
        consumer.vertex(pose,  hw, top, hd).color(1, 1, 1, alpha).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 0, 1).endVertex();
        consumer.vertex(pose,  hw, bot, hd).color(1, 1, 1, alpha).uv(1, uvTop).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 0, 1).endVertex();

        consumer.vertex(pose,  hw, bot, -hd).color(1, 1, 1, alpha).uv(0, uvTop).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 0, -1).endVertex();
        consumer.vertex(pose,  hw, top, -hd).color(1, 1, 1, alpha).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 0, -1).endVertex();
        consumer.vertex(pose, -hw, top, -hd).color(1, 1, 1, alpha).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 0, -1).endVertex();
        consumer.vertex(pose, -hw, bot, -hd).color(1, 1, 1, alpha).uv(1, uvTop).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 0, -1).endVertex();

        consumer.vertex(pose, -hw, bot, -hd).color(1, 1, 1, alpha).uv(0, uvTop).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, -1, 0, 0).endVertex();
        consumer.vertex(pose, -hw, top, -hd).color(1, 1, 1, alpha).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, -1, 0, 0).endVertex();
        consumer.vertex(pose, -hw, top,  hd).color(1, 1, 1, alpha).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, -1, 0, 0).endVertex();
        consumer.vertex(pose, -hw, bot,  hd).color(1, 1, 1, alpha).uv(1, uvTop).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, -1, 0, 0).endVertex();

        consumer.vertex(pose, hw, bot,  hd).color(1, 1, 1, alpha).uv(0, uvTop).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 1, 0, 0).endVertex();
        consumer.vertex(pose, hw, top,  hd).color(1, 1, 1, alpha).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 1, 0, 0).endVertex();
        consumer.vertex(pose, hw, top, -hd).color(1, 1, 1, alpha).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 1, 0, 0).endVertex();
        consumer.vertex(pose, hw, bot, -hd).color(1, 1, 1, alpha).uv(1, uvTop).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 1, 0, 0).endVertex();

        consumer.vertex(pose, -hw, top,  hd).color(1, 1, 1, alpha).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 1, 0).endVertex();
        consumer.vertex(pose, -hw, top, -hd).color(1, 1, 1, alpha).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 1, 0).endVertex();
        consumer.vertex(pose,  hw, top, -hd).color(1, 1, 1, alpha).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 1, 0).endVertex();
        consumer.vertex(pose,  hw, top,  hd).color(1, 1, 1, alpha).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, 1, 0).endVertex();

        consumer.vertex(pose, -hw, bot, -hd).color(1, 1, 1, alpha).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, -1, 0).endVertex();
        consumer.vertex(pose,  hw, bot, -hd).color(1, 1, 1, alpha).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, -1, 0).endVertex();
        consumer.vertex(pose,  hw, bot,  hd).color(1, 1, 1, alpha).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, -1, 0).endVertex();
        consumer.vertex(pose, -hw, bot,  hd).color(1, 1, 1, alpha).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, 0, -1, 0).endVertex();
    }
}
