// src/main/java/com/xulai/elementalcraft/event/SteamReactionHandler.java
package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalReactionConfig;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * SteamReactionHandler
 *
 * 中文说明：
 * 负责处理 "蒸汽云 (Steam Cloud)" 元素反应。
 * 触发条件：赤焰属性 (Fire) 攻击 -> 潮湿 (Wetness) 目标。
 * 效果：
 * 1. 伤害衰减：大幅降低本次火属性伤害（配置可调，默认 50%）。
 * 2. 生成云雾：在目标位置生成 AreaEffectCloud (白色粒子)。
 * 3. 视野丢失：云雾给予进入者失明效果。
 * 4. 仇恨清除：云雾内的生物会丢失攻击目标（发呆）。
 *
 * English description:
 * Handles the "Steam Cloud" elemental reaction.
 * Trigger: Fire Element Attack -> Wetness Target.
 * Effects:
 * 1. Damage Reduction: Significantly reduces fire damage (Configurable, default 50%).
 * 2. Spawn Cloud: Spawns an AreaEffectCloud (White particles) at target location.
 * 3. Loss of Vision: Cloud applies Blindness effect to entities inside.
 * 4. Aggro Clear: Mobs inside the cloud lose their attack target (Dazed).
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class SteamReactionHandler {

    // 用于标记蒸汽云实体的 Tag / Tag used to mark steam cloud entities
    public static final String TAG_STEAM_CLOUD = "EC_SteamCloud";

    /**
     * 监听生物受伤事件。
     * 使用 LOWEST 优先级，确保在 CombatEvents 计算完基础元素伤害后再进行修正。
     *
     * Listens to Living Hurt events.
     * Uses LOWEST priority to ensure modification happens after CombatEvents calculates base elemental damage.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!ElementalReactionConfig.steamReactionEnabled) return;

        LivingEntity target = event.getEntity();
        Entity attackerEntity = event.getSource().getEntity();

        // 1. 检查目标是否潮湿
        // 1. Check if target is wet
        if (!target.getPersistentData().contains(WetnessHandler.NBT_WETNESS)) return;
        int wetnessLevel = target.getPersistentData().getInt(WetnessHandler.NBT_WETNESS);
        if (wetnessLevel <= 0) return;

        // 2. 检查攻击是否为火属性
        // 2. Check if attack is Fire element
        boolean isFireDamage = event.getSource().is(DamageTypeTags.IS_FIRE);
        
        // 如果不是原生火伤，检查是否为赤焰附魔/属性攻击
        // If not native fire damage, check if it's Fire Enchantment/Attribute attack
        if (!isFireDamage && attackerEntity instanceof LivingEntity attacker) {
            ElementType attackElement = ElementUtils.getAttackElement(attacker);
            if (attackElement == ElementType.FIRE) {
                isFireDamage = true;
            }
        }

        if (isFireDamage) {
            triggerSteamReaction(target, event);
        }
    }

    /**
     * 触发蒸汽反应逻辑。
     *
     * Triggers the steam reaction logic.
     */
    private static void triggerSteamReaction(LivingEntity target, LivingHurtEvent event) {
        // A. 伤害衰减
        // A. Damage Reduction
        float originalDamage = event.getAmount();
        float reduction = (float) ElementalReactionConfig.steamDamageReduction;
        float newDamage = originalDamage * (1.0f - reduction);
        event.setAmount(newDamage);

        // B. 生成蒸汽云
        // B. Spawn Steam Cloud
        if (!target.level().isClientSide) {
            spawnSteamCloud(target);
            
            // 调试日志 (使用本地化 Key) / Debug Log (Using Localization Key)
            if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                DebugCommand.sendDebugMessage(attacker, 
                    Component.translatable("debug.elementalcraft.reaction.steam", (int)(reduction * 100))
                );
            }
        }
    }

    /**
     * 在目标位置生成蒸汽云实体。
     *
     * Spawns the steam cloud entity at the target's location.
     */
    private static void spawnSteamCloud(LivingEntity target) {
        AreaEffectCloud cloud = new AreaEffectCloud(target.level(), target.getX(), target.getY(), target.getZ());
        
        // 设置半径 / Set Radius
        cloud.setRadius((float) ElementalReactionConfig.steamCloudRadius);
        cloud.setRadiusOnUse(0F); // 使用后不减少半径 / Don't reduce radius on use
        cloud.setRadiusPerTick(-0.005F); // 随时间缓慢缩小 / Slowly shrink over time
        
        // 设置持续时间 / Set Duration
        cloud.setDuration(ElementalReactionConfig.steamCloudDuration);
        
        // 设置粒子效果 (营火烟雾 或 爆炸烟雾，模拟蒸汽)
        // Set Particle (Campfire smoke or Explosion emitter, strictly simulating steam)
        cloud.setParticle(ParticleTypes.CLOUD); 
        
        // 添加药水效果：失明
        // Add Potion Effect: Blindness
        int blindDuration = ElementalReactionConfig.steamBlindnessDuration;
        cloud.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, blindDuration, 0));
        
        // 添加自定义 Tag，用于后续 AI 识别
        // Add custom Tag for subsequent AI recognition
        cloud.addTag(TAG_STEAM_CLOUD);
        
        // 设置颜色 (白色/灰色)
        // Set Color (White/Grey)
        cloud.setFixedColor(0xFFFFFF);
        
        target.level().addFreshEntity(cloud);
    }

    /**
     * 监听生物 Tick 事件。
     * 处理位于蒸汽云内的怪物的仇恨逻辑。
     *
     * Listens to Living Tick events.
     * Handles aggro logic for mobs inside the steam cloud.
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!ElementalReactionConfig.steamReactionEnabled || !ElementalReactionConfig.steamClearAggro) return;
        
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        // 仅处理怪物 (Mob)，玩家不需要清除仇恨 / Only handle Mobs, players don't need aggro clear
        if (!(entity instanceof Mob mob)) return;

        // 为了性能，每 10 tick (0.5秒) 检查一次
        // For performance, check every 10 ticks (0.5s)
        if (entity.tickCount % 10 != 0) return;

        // 检查周围是否有蒸汽云
        // Check for surrounding steam clouds
        AABB box = entity.getBoundingBox();
        List<AreaEffectCloud> clouds = entity.level().getEntitiesOfClass(AreaEffectCloud.class, box, 
            c -> c.getTags().contains(TAG_STEAM_CLOUD));

        if (!clouds.isEmpty()) {
            // 如果在蒸汽云内，且当前有攻击目标
            // If inside steam cloud and has attack target
            if (mob.getTarget() != null) {
                // 强制清除仇恨
                // Force clear aggro
                mob.setTarget(null);
                
                // 可选：停止寻路，使其原地发呆
                // Optional: Stop navigation to make it stand still
                mob.getNavigation().stop();
            }
        }
    }
}