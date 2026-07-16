package com.xulai.elementalcraft.event.iss;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.event.ScorchedHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class PoisonCloudReactionHandler {

    private static final String ISS_NAMESPACE = "irons_spellbooks";
    private static EntityType<?> cachedPoisonCloudType;
    private static boolean cacheInitialized;

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.level.isClientSide()) return;
        if (event.phase != TickEvent.Phase.END) return;
        if (ElementalFireNatureReactionsConfig.scorchedTriggerThreshold <= 0) return;

        if (!cacheInitialized) {
            cachedPoisonCloudType = ForgeRegistries.ENTITY_TYPES.getValue(
                    new ResourceLocation("irons_spellbooks:poison_cloud"));
            cacheInitialized = true;
        }
        if (cachedPoisonCloudType == null) return;

        ServerLevel level = (ServerLevel) event.level;

        for (Entity entity : level.getEntities().getAll()) {
            if (entity.getType() != cachedPoisonCloudType) continue;
            if (!entity.isAlive()) continue;

            AABB cloudBox = entity.getBoundingBox().inflate(0.5);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, cloudBox,
                    e -> e.isAlive() && ScorchedHandler.isScorched(e));

            if (targets.isEmpty()) continue;

            for (LivingEntity target : targets) {
                CompoundTag data = target.getPersistentData();

                int remaining = data.getInt(ScorchedHandler.NBT_SCORCHED_TICKS);
                if (remaining <= 0) continue;

                double durMult = ElementalFireNatureReactionsConfig.poisonScorchDurationMultiplier;
                int extended = Math.max(remaining, (int) (remaining * durMult));
                data.putInt(ScorchedHandler.NBT_SCORCHED_TICKS, extended);

                float curDmg = data.getFloat(ScorchedHandler.NBT_SCORCHED_DAMAGE_MULT);
                if (curDmg <= 0) curDmg = 1.0f;
                double dmgMult = ElementalFireNatureReactionsConfig.poisonScorchDamageMultiplier;
                data.putFloat(ScorchedHandler.NBT_SCORCHED_DAMAGE_MULT, curDmg * (float) dmgMult);

                target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), extended));

                level.sendParticles(ParticleTypes.LAVA,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                        12, 0.3, 0.3, 0.3, 0.1);
            }

            level.sendParticles(ParticleTypes.POOF,
                    entity.getX(), entity.getY() + 0.5, entity.getZ(),
                    15, 0.5, 0.5, 0.5, 0.05);

            entity.discard();
            break;
        }
    }
}
