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
 * 4. 自我干燥惩罚机制（如果攻击者触发了干燥，属性伤害根据配置减少）。
 * 元素激活机制：生物元素属性 = 手持物品属性 + 对应防具强化。若不匹配则视为无属性。
 *
 * English description:
 * Handles elemental damage calculation when an entity takes damage.
 * Implements custom damage formula.
 * Includes:
 * 1. Elemental Restraint and Floor Damage.
 * 2. Wetness modification (Thunder/Frost bonus, Fire reduction, Nature bonus).
 * 3. Nature Element "Siphon" mechanism (Absorb wetness, Heal, FX).
 * 4. Self-Drying Penalty (Reduces elemental damage based on config if attacker triggered drying).
 * Elemental Activation: Creature Element = Hand Item Attribute + Armor Enhancement. Otherwise Non-Elemental.
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class CombatEvents {

    private static final String NBT_WETNESS = "EC_WetnessLevel";
    // 定义最大抗性基准值，用于计算克制状态下的减伤百分比
    // Define max resistance benchmark for calculating reduction percentage under restraint
    private static final float MAX_RESISTANCE_BENCHMARK = 100.0f;

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
        // Return if no elemental damage
        if (rawElementalDamage <= 0.0f) {
            return;
        }

        // ==================== 4. 潮湿、蒸汽与自然反应修正 ====================
        // ==================== 4. Wetness, Steam & Nature Modifiers ====================
        int wetnessLevel = target.getPersistentData().getInt(NBT_WETNESS);
        
        // 如果 NBT 为 0，检查是否有快照标签（说明刚才触发了蒸汽反应，消耗了潮湿）
        // If NBT is 0, check for snapshot tag (means steam reaction just consumed wetness)
        if (wetnessLevel <= 0) {
            for (String tag : target.getTags()) {
                if (tag.startsWith("EC_WetnessSnapshot_")) {
                    try {
                        wetnessLevel = Integer.parseInt(tag.substring(19)); // "EC_WetnessSnapshot_".length()
                        target.removeTag(tag); // 用完即焚 / Consume tag
                    } catch (NumberFormatException ignored) {}
                    break;
                }
            }
        }

        float wetnessMultiplier = 1.0f;

        // 读取配置：减伤最大修正幅度上限
        // Read Config: Max reduction cap
        float maxCap = (float) ElementalReactionConfig.steamMaxReduction;

        if (wetnessLevel > 0) {
            if (attackElement == ElementType.FIRE) {
                // 赤焰属性：减伤 (受上限限制)
                // Fire Element: Damage Reduction (Capped)
                float reductionPerLevel = (float) ElementalReactionConfig.wetnessFireReduction;
                float finalReduction = Math.min(wetnessLevel * reductionPerLevel, maxCap);
                wetnessMultiplier = 1.0f - finalReduction;
                
            } else if (attackElement == ElementType.THUNDER || attackElement == ElementType.FROST) {
                // 雷霆/冰霜属性：增伤 (无上限)
                // Thunder/Frost Element: Damage Increase (Uncapped)
                float increasePerLevel = (float) ElementalReactionConfig.wetnessResistModifier;
                float totalIncrease = wetnessLevel * increasePerLevel;
                wetnessMultiplier = 1.0f + totalIncrease;

            } else if (attackElement == ElementType.NATURE) {
                // 自然属性：增伤 (无上限) + 汲取逻辑
                // Nature Element: Damage Bonus (Uncapped) + Siphon Logic
                
                // 1. 计算增伤
                // 1. Calculate Damage Bonus
                float bonusPerLevel = (float) ElementalReactionConfig.natureWetnessBonus;
                float totalBonus = wetnessLevel * bonusPerLevel;
                wetnessMultiplier = 1.0f + totalBonus;

                // 2. 执行汲取 (吸水、回血、特效) - 仅在服务端执行
                // 2. Execute Siphon (Absorb, Heal, FX) - Server side only
                if (!target.level().isClientSide) {
                    handleNatureSiphon(attacker, target, wetnessLevel, enhancementPoints);
                }
            }
        }

        // ==================== 5. 自我干燥惩罚检查 ====================
        // ==================== 5. Self-Drying Penalty Check ====================
        // 检查攻击者是否有“自我干燥惩罚”标签
        // Check if attacker has "Self-Drying Penalty" tag
        if (attacker.getTags().contains(com.xulai.elementalcraft.event.SteamReactionHandler.TAG_SELF_DRYING_PENALTY) && attackElement == ElementType.FIRE) {
            // 惩罚倍率 (读取配置)
            // Penalty multiplier (Read from config)
            float penalty = 1.0f - (float) ElementalReactionConfig.wetnessSelfDryingDamagePenalty;
            wetnessMultiplier *= penalty;
            
            // 移除标签
            // Remove tag
            attacker.removeTag(com.xulai.elementalcraft.event.SteamReactionHandler.TAG_SELF_DRYING_PENALTY);
        }

        // ==================== 6. 克制关系计算 ====================
        // ==================== 6. Restraint Calculation ====================
        ElementType targetDominant = ElementType.NONE;
        
        // 激活逻辑检查：受击者的元素属性取决于【手持物品属性】+【身上对应强化】
        // Activation Logic Check
        ItemStack targetWeapon = target.getMainHandItem();
        ElementType weaponElement = ElementUtils.getAttackElement(targetWeapon);
        
        if (weaponElement == ElementType.NONE) {
             targetWeapon = target.getOffhandItem();
             weaponElement = ElementUtils.getAttackElement(targetWeapon);
        }

        if (weaponElement != ElementType.NONE) {
            int targetEnhancement = ElementUtils.getDisplayEnhancement(target, weaponElement);
            if (targetEnhancement > 0) {
                targetDominant = weaponElement;
            }
        }

        float restraintMultiplier = ElementalConfig.getRestraintMultiplier(attackElement, targetDominant);

        // ==================== 7. 应用最终伤害公式 ====================
        // ==================== 7. Apply Final Formula ====================
        
        // 计算攻击部分 (强化 * 全局 * 克制 * 潮湿)
        // Calculate Attack Part
        float attackPart = rawElementalDamage
                * (float) ElementalConfig.elementalDamageMultiplier
                * wetnessMultiplier
                * restraintMultiplier;

        float finalElementalDmg;
        boolean isFloored = false;
        double minPercent = ElementalConfig.restraintMinDamagePercent;

        // 分支逻辑：
        // 如果触发了克制 (Restraint > 1.0)，则采用【线性百分比减免】逻辑，而非【减法】逻辑。
        // 这确保了即使抗性很高（但未满），伤害也不会被硬保底锁死，而是平滑过渡到最大抗性时的保底值。
        // Branch Logic:
        // If Restraint triggered (> 1.0), use [Linear Percentage Reduction] instead of [Subtraction].
        // This ensures damage smoothly transitions to the floor value at max resistance, instead of snapping.
        
        if (restraintMultiplier > 1.0f) {
            // 计算抗性占比 (0.0 ~ 1.0)
            // Calculate Resistance Ratio
            float resistRatio = Math.min(resistancePoints / MAX_RESISTANCE_BENCHMARK, 1.0f);
            
            // 计算最大允许的减免比例 (1.0 - 保底比例)
            // Calculate Max Allowed Reduction (1.0 - Floor Ratio)
            // 例如保底是 50% (0.5)，则最大减免也是 50% (0.5)
            double maxReductionAllowed = 1.0 - minPercent;
            
            // 计算实际减免比例 (线性插值)
            // Calculate Actual Reduction (Lerp)
            double actualReduction = resistRatio * maxReductionAllowed;
            
            // 应用减免
            // Apply Reduction
            finalElementalDmg = attackPart * (float) (1.0 - actualReduction);
            
            // 标记为已触发修正（用于 Debug 显示）
            // Mark as floored/modified (For Debug)
            isFloored = true; 
            
        } else {
            // 正常逻辑：减法公式
            // Normal Logic: Subtraction Formula
            float defensePart = rawResistReduction * (float) ElementalConfig.elementalResistanceMultiplier;
            finalElementalDmg = Math.max(0.0f, attackPart - defensePart);
        }

        // ==================== 8. 应用总伤害并输出调试日志 ====================
        // ==================== 8. Apply Total Damage & Debug ====================
        float totalDamage = physicalDamage + finalElementalDmg;
        event.setAmount(totalDamage);

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
        // 1. Calculate Siphon Amount
        int threshold = ElementalReactionConfig.natureSiphonThreshold;
        if (threshold <= 0) threshold = 20;
        
        int siphonAmount = 1 + (natureEnhancement / threshold);
        int actualSiphon = Math.min(siphonAmount, targetWetness);

        if (actualSiphon > 0) {
            // 2. 修改双方 NBT
            // 2. Update NBT for both
            
            // 减少目标的潮湿
            // Reduce target's wetness
            int newTargetLevel = targetWetness - actualSiphon;
            if (newTargetLevel <= 0) {
                target.getPersistentData().remove(NBT_WETNESS);
                target.getPersistentData().remove(com.xulai.elementalcraft.event.WetnessHandler.NBT_RAIN_TIMER);
                target.getPersistentData().remove(com.xulai.elementalcraft.event.WetnessHandler.NBT_DECAY_TIMER);
                target.removeEffect(com.xulai.elementalcraft.potion.ModMobEffects.WETNESS.get());
            } else {
                target.getPersistentData().putInt(NBT_WETNESS, newTargetLevel);
            }

            // 增加自身的潮湿
            // Increase attacker's wetness
            int attackerWetness = attacker.getPersistentData().getInt(NBT_WETNESS);
            int maxLevel = ElementalReactionConfig.wetnessMaxLevel;
            int newAttackerLevel = Math.min(attackerWetness + actualSiphon, maxLevel);
            
            attacker.getPersistentData().putInt(NBT_WETNESS, newAttackerLevel);

            // 3. 回血 (Heal)
            // 3. Heal
            float healAmount = (float) ElementalReactionConfig.natureSiphonHeal;
            if (healAmount > 0) {
                attacker.heal(healAmount);
            }

            // 4. 视觉与听觉特效 (FX)
            if (target.level() instanceof ServerLevel serverLevel) {
                float pitch = 1.2F + serverLevel.random.nextFloat() * 0.4F; 
                serverLevel.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), 
                    SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 2.0F, pitch);

                Vec3 start = target.position().add(0, target.getBbHeight() * 0.6, 0); 
                Vec3 end = attacker.position().add(0, attacker.getBbHeight() * 0.6, 0); 
                Vec3 vector = end.subtract(start);
                double distance = vector.length();
                
                int particleCount = (int) (distance * 10); 

                for (int i = 0; i <= particleCount; i++) {
                    double progress = (double) i / particleCount;
                    double arcHeight = Math.sin(progress * Math.PI) * 0.5; 
                    Vec3 pos = start.add(vector.scale(progress)).add(0, arcHeight, 0);

                    serverLevel.sendParticles(ParticleTypes.BUBBLE, 
                        pos.x, pos.y, pos.z, 
                        1, 0, 0, 0, 0.0);
                        
                    if (i % 5 == 0) {
                        serverLevel.sendParticles(ParticleTypes.ENTITY_EFFECT, 
                            pos.x, pos.y, pos.z, 
                            0, 0.2, 0.4, 1.0, 1.0);
                    }
                }
                
                serverLevel.sendParticles(ParticleTypes.SPLASH,
                    attacker.getX(), attacker.getY() + attacker.getBbHeight() * 0.5, attacker.getZ(),
                    20, 0.5, 0.5, 0.5, 0.1);

                DebugCommand.sendDebugMessage(attacker, 
                    Component.translatable("debug.elementalcraft.nature.siphon", actualSiphon, healAmount));
            }
        }
    }
}