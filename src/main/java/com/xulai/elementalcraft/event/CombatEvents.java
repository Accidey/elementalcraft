// src/main/java/com/xulai/elementalcraft/event/CombatEvents.java
package com.xulai.elementalcraft.event;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.command.DebugCommand;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ElementalReactionConfig;
import com.xulai.elementalcraft.util.ElementType;
import com.xulai.elementalcraft.util.ElementUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * CombatEvents
 *
 * 中文说明：
 * 负责处理实体受到伤害时的元素属性伤害计算逻辑。
 * 实现了自定义的伤害公式：(属性伤害 * 全局倍率 * 克制倍率 * 潮湿/蒸汽修正) - (抗性抵消 * 抗性倍率)。
 * 包含：
 * 1. 元素克制与保底伤害。
 * 2. 潮湿对各属性的修正（雷/冰增伤，火减伤，自然增伤）。
 * 3. 自然属性的"汲取"机制（吸水、回血、特效）。
 * 元素激活机制：生物元素属性 = 手持物品属性 + 对应防具强化。若不匹配则视为无属性。
 *
 * English description:
 * Handles elemental damage calculation when an entity takes damage.
 * Implements custom damage formula.
 * Includes:
 * 1. Elemental Restraint and Floor Damage.
 * 2. Wetness modification (Thunder/Frost bonus, Fire reduction, Nature bonus).
 * 3. Nature Element "Siphon" mechanism (Absorb wetness, Heal, FX).
 * Elemental Activation: Creature Element = Hand Item Attribute + Armor Enhancement. Otherwise Non-Elemental.
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class CombatEvents {

    private static final String NBT_WETNESS = "EC_WetnessLevel";

    /**
     * 监听 LivingDamageEvent，在物理伤害基础上计算并添加元素属性伤害。
     *
     * Listens to LivingDamageEvent to calculate and add elemental damage on top of physical damage.
     *
     * @param event 实体伤害事件 / Entity damage event
     */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();

        // 仅处理由生物造成的伤害
        // Only handle damage caused by living entities
        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            return;
        }

        // ==================== 1. 确定攻击使用的物品 ====================
        // ==================== 1. Determine the weapon used ====================
        ItemStack weaponStack = ItemStack.EMPTY;
        Entity directEntity = source.getDirectEntity();

        // 处理投掷的三叉戟，从 NBT 恢复物品信息以获取附魔
        // Handle thrown tridents, restore item info from NBT to get enchantments
        if (directEntity instanceof ThrownTrident trident) {
            try {
                CompoundTag tag = new CompoundTag();
                trident.addAdditionalSaveData(tag);
                if (tag.contains("Trident", 10)) {
                    weaponStack = ItemStack.of(tag.getCompound("Trident"));
                }
            } catch (Exception ignored) {}
        }

        // 若未获取到三叉戟信息，检查主手与副手
        // If trident info not found, check main hand and offhand
        if (weaponStack.isEmpty()) {
            ItemStack mainHand = attacker.getMainHandItem();
            ItemStack offHand = attacker.getOffhandItem();

            if (ElementUtils.getAttackElement(mainHand) != ElementType.NONE) {
                weaponStack = mainHand;
            } else if (ElementUtils.getAttackElement(offHand) != ElementType.NONE) {
                weaponStack = offHand;
            }
        }

        // ==================== 2. 获取攻击元素属性 ====================
        // ==================== 2. Get Attack Element ====================
        ElementType attackElement = ElementUtils.getAttackElement(weaponStack);

        if (attackElement == ElementType.NONE) {
            return;
        }

        // ==================== 3. 获取基础数值 ====================
        // ==================== 3. Get Base Stats ====================
        float physicalDamage = event.getAmount();

        // 获取攻击者对应元素的强化总点数（所有护甲累加）
        // Get attacker's total enhancement points for the element
        int enhancementPoints = ElementUtils.getDisplayEnhancement(attacker, attackElement);
        // 获取目标对应元素的抗性总点数（所有护甲累加）
        // Get target's total resistance points for the element
        int resistancePoints = ElementUtils.getDisplayResistance(target, attackElement);

        // 配置值：每多少点强化对应增加 0.5 点属性伤害 / 每多少点抗性对应减少 0.5 点伤害
        // Config values: Points per half damage/reduction
        int strengthPerHalfDamage = ElementalConfig.getStrengthPerHalfDamage();
        int resistPerHalfReduction = ElementalConfig.getResistPerHalfReduction();

        // 计算原始属性伤害与原始抗性减免
        // Calculate raw elemental damage and raw resistance reduction
        float rawElementalDamage = enhancementPoints / (float) strengthPerHalfDamage * 0.5f;
        float rawResistReduction = resistancePoints / (float) resistPerHalfReduction * 0.5f;

        // 若无属性伤害则直接返回
        // 这也符合"激活机制"：如果攻击者身上没有对应的强化点数，rawElementalDamage 为 0，直接返回，视为无属性攻击
        // Return if no elemental damage
        // This also fits the "Activation Mechanism": if attacker has no corresponding enhancement, rawElementalDamage is 0, return, treated as non-elemental attack
        if (rawElementalDamage <= 0.0f) {
            return;
        }

        // ==================== 4. 潮湿、蒸汽与自然反应修正 ====================
        // ==================== 4. Wetness, Steam & Nature Modifiers ====================
        int wetnessLevel = target.getPersistentData().getInt(NBT_WETNESS);
        float wetnessMultiplier = 1.0f;

        if (wetnessLevel > 0) {
            // 读取配置：最大修正幅度上限 (使用静态缓存并强转float)
            // Read Config: Max modification cap (Use static cache and cast to float)
            float maxCap = (float) ElementalReactionConfig.steamMaxReduction;

            if (attackElement == ElementType.FIRE) {
                // 赤焰属性：减伤
                // Fire Element: Damage Reduction
                float reductionPerLevel = (float) ElementalReactionConfig.wetnessFireReduction;
                float finalReduction = Math.min(wetnessLevel * reductionPerLevel, maxCap);
                wetnessMultiplier = 1.0f - finalReduction;
                
            } else if (attackElement == ElementType.THUNDER || attackElement == ElementType.FROST) {
                // 雷霆/冰霜属性：增伤 (模拟抗性降低)
                // Thunder/Frost Element: Damage Increase (Simulate resistance drop)
                float increasePerLevel = (float) ElementalReactionConfig.wetnessResistModifier;
                float totalIncrease = Math.min(wetnessLevel * increasePerLevel, maxCap);
                wetnessMultiplier = 1.0f + totalIncrease;

            } else if (attackElement == ElementType.NATURE) {
                // 【新增】自然属性：增伤 + 汲取逻辑
                // [NEW] Nature Element: Damage Bonus + Siphon Logic
                
                // 1. 计算增伤 (使用静态缓存并强转)
                // 1. Calculate Damage Bonus (Use static cache and cast)
                float bonusPerLevel = (float) ElementalReactionConfig.natureWetnessBonus;
                float totalBonus = Math.min(wetnessLevel * bonusPerLevel, maxCap); // 受通用上限限制 / Capped
                wetnessMultiplier = 1.0f + totalBonus;

                // 2. 执行汲取 (吸水、回血、特效) - 仅在服务端执行
                // 2. Execute Siphon (Absorb, Heal, FX) - Server side only
                if (!target.level().isClientSide) {
                    handleNatureSiphon(attacker, target, wetnessLevel, enhancementPoints);
                }
            }
        }

        // ==================== 5. 克制关系计算 (已修改) ====================
        // ==================== 5. Restraint Calculation (Modified) ====================
        ElementType targetDominant = ElementType.NONE;
        
        // 激活逻辑检查：受击者的元素属性取决于【手持物品属性】+【身上对应强化】
        // Activation Logic Check: Target's element depends on [Hand Item Attribute] + [Corresponding Body Enhancement]
        
        // 1. 获取目标手持物品的攻击属性 (优先检查主手)
        // 1. Get target's held item attack attribute (Check main hand first)
        ItemStack targetWeapon = target.getMainHandItem();
        ElementType weaponElement = ElementUtils.getAttackElement(targetWeapon);
        
        if (weaponElement == ElementType.NONE) {
             targetWeapon = target.getOffhandItem();
             weaponElement = ElementUtils.getAttackElement(targetWeapon);
        }

        // 2. 只有当手持属性物品，且身上有对应的属性强化时，才被判定为该属性生物
        // 2. Only considered as an elemental creature if holding an elemental item AND having corresponding attribute enhancement
        if (weaponElement != ElementType.NONE) {
            int targetEnhancement = ElementUtils.getDisplayEnhancement(target, weaponElement);
            // 如果有对应的强化点数，则确认激活该属性
            // If corresponding enhancement points exist, confirm activation of this attribute
            if (targetEnhancement > 0) {
                targetDominant = weaponElement;
            }
            // 否则视为无属性生物 (targetDominant 保持 NONE)
            // Otherwise treated as non-elemental creature (targetDominant remains NONE)
        }

        float restraintMultiplier = ElementalConfig.getRestraintMultiplier(attackElement, targetDominant);

        // ==================== 6. 应用最终伤害公式 ====================
        // ==================== 6. Apply Final Formula ====================
        float attackPart = rawElementalDamage
                * (float) ElementalConfig.elementalDamageMultiplier
                * wetnessMultiplier // 包含了潮湿和蒸汽反应的所有系数
                * restraintMultiplier;

        float defensePart = rawResistReduction * (float) ElementalConfig.elementalResistanceMultiplier;

        float finalElementalDmg = Math.max(0.0f, attackPart - defensePart);

        // ==================== 7. 克制保底机制 ====================
        // ==================== 7. Restraint Floor Mechanism ====================
        boolean isFloored = false;
        double minPercent = ElementalConfig.restraintMinDamagePercent;

        if (restraintMultiplier > 1.0f) {
            float floorValue = (float) (attackPart * minPercent);
            if (finalElementalDmg < floorValue) {
                finalElementalDmg = floorValue;
                isFloored = true;
            }
        }

        // ==================== 8. 应用总伤害并输出调试日志 ====================
        // ==================== 8. Apply Total Damage & Debug ====================
        float totalDamage = physicalDamage + finalElementalDmg;
        event.setAmount(totalDamage);

        ElementalCraft.LOGGER.debug("[EC Combat] Nature Bonus Check: Element={}, Wetness={}, Mult={}", 
            attackElement, wetnessLevel, wetnessMultiplier);

        DebugCommand.sendCombatLog(
                attacker, target, directEntity,
                physicalDamage,
                rawElementalDamage,
                rawResistReduction,
                ElementalConfig.elementalDamageMultiplier,
                ElementalConfig.elementalResistanceMultiplier,
                restraintMultiplier,
                wetnessMultiplier,
                finalElementalDmg,
                totalDamage,
                isFloored,
                minPercent,
                wetnessLevel
        );
    }

    /**
     * 处理自然属性的汲取逻辑 (吸水 + 回血 + 特效)。
     *
     * Handles Nature element siphon logic (Absorb water + Heal + FX).
     *
     * @param attacker 攻击者 (自然属性) / Attacker (Nature)
     * @param target 受击者 (潮湿) / Target (Wet)
     * @param targetWetness 目标当前的潮湿层数 / Target's current wetness level
     * @param natureEnhancement 攻击者的自然强化点数 / Attacker's nature enhancement points
     */
    private static void handleNatureSiphon(LivingEntity attacker, LivingEntity target, int targetWetness, int natureEnhancement) {
        // 1. 计算吸取层数
        // 公式：1 + (强化点数 / 阈值)
        // 1. Calculate Siphon Amount
        // Formula: 1 + (Enhancement / Threshold)
        int threshold = ElementalReactionConfig.natureSiphonThreshold;
        if (threshold <= 0) threshold = 20; // 防止除以0 / Prevent div by zero
        
        int siphonAmount = 1 + (natureEnhancement / threshold);

        // 实际能吸取的数量不能超过目标拥有的数量
        // Actual siphon amount cannot exceed target's current level
        int actualSiphon = Math.min(siphonAmount, targetWetness);

        if (actualSiphon > 0) {
            // 2. 修改双方 NBT
            // 2. Update NBT for both
            
            // 减少目标的潮湿
            // Reduce target's wetness
            int newTargetLevel = targetWetness - actualSiphon;
            if (newTargetLevel <= 0) {
                target.getPersistentData().remove(NBT_WETNESS);
                target.getPersistentData().remove(WetnessHandler.NBT_RAIN_TIMER);
                target.getPersistentData().remove(WetnessHandler.NBT_DECAY_TIMER);
                target.removeEffect(com.xulai.elementalcraft.potion.ModMobEffects.WETNESS.get());
            } else {
                target.getPersistentData().putInt(NBT_WETNESS, newTargetLevel);
            }

            // 增加自身的潮湿 (上限控制在配置的最大值)
            // Increase attacker's wetness (Capped at max level)
            int attackerWetness = attacker.getPersistentData().getInt(NBT_WETNESS);
            int maxLevel = ElementalReactionConfig.wetnessMaxLevel;
            int newAttackerLevel = Math.min(attackerWetness + actualSiphon, maxLevel);
            
            attacker.getPersistentData().putInt(NBT_WETNESS, newAttackerLevel);

            // 3. 回血 (Heal)
            // 3. Heal
            // 使用静态缓存并强转float
            // Use static cache and cast to float
            float healAmount = (float) ElementalReactionConfig.natureSiphonHeal;
            if (healAmount > 0) {
                attacker.heal(healAmount);
            }

            // 4. 视觉与听觉特效 (FX) - Optimized
            if (target.level() instanceof ServerLevel serverLevel) {
                // 音效：位置改为攻击者（你在吸水），音量调大到 2.0 (增加传播距离)，音调降低 (增加厚度)
                // Sound: Play at Attacker's pos, increase Volume to 2.0, lower pitch
                float pitch = 1.2F + serverLevel.random.nextFloat() * 0.4F; 
                serverLevel.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), 
                    SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 2.0F, pitch);

                // 粒子连线动画 (从目标 -> 攻击者)
                // Particle line animation (Target -> Attacker)
                Vec3 start = target.position().add(0, target.getBbHeight() * 0.6, 0); // 从胸口发出
                Vec3 end = attacker.position().add(0, attacker.getBbHeight() * 0.6, 0); // 到胸口吸收
                Vec3 vector = end.subtract(start);
                double distance = vector.length();
                
                // 密度增加：每格 10 个粒子，形成连续的线
                int particleCount = (int) (distance * 10); 

                for (int i = 0; i <= particleCount; i++) {
                    double progress = (double) i / particleCount;
                    // 正弦波弧度：模拟水流被吸取时的自然弯曲
                    // Add slight sine wave offset for arc effect
                    double arcHeight = Math.sin(progress * Math.PI) * 0.5; 
                    Vec3 pos = start.add(vector.scale(progress)).add(0, arcHeight, 0);

                    // 使用 BUBBLE (气泡) 粒子：有向上的浮力，看起来像悬浮的水柱
                    serverLevel.sendParticles(ParticleTypes.BUBBLE, 
                        pos.x, pos.y, pos.z, 
                        1, 0, 0, 0, 0.0);
                        
                    // 偶尔混入一点蓝色魔法粒子 (ENTITY_EFFECT)，增加"法术"感
                    if (i % 5 == 0) {
                        // Count=0 模式设置颜色 (R=0.2, G=0.4, B=1.0) = 亮蓝色
                        serverLevel.sendParticles(ParticleTypes.ENTITY_EFFECT, 
                            pos.x, pos.y, pos.z, 
                            0, 0.2, 0.4, 1.0, 1.0);
                    }
                }
                
                // 冲击反馈：在攻击者身上生成一圈水波，表现"吸收"成功的瞬间
                // Impact FX: Splash around attacker to show successful absorption
                serverLevel.sendParticles(ParticleTypes.SPLASH,
                    attacker.getX(), attacker.getY() + attacker.getBbHeight() * 0.5, attacker.getZ(),
                    20, 0.5, 0.5, 0.5, 0.1);

                // 调试信息
                // Debug message
                DebugCommand.sendDebugMessage(attacker, 
                    Component.translatable("debug.elementalcraft.nature.siphon", actualSiphon, healAmount));
            }
        }
    }
}