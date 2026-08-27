package com.xulai.elementalcraft.event.cataclysm;

import com.xulai.elementalcraft.ElementalCraft;
import com.xulai.elementalcraft.config.ElementalConfig;
import com.xulai.elementalcraft.config.ElementalFireNatureReactionsConfig;
import com.xulai.elementalcraft.event.FrostbiteHandler;
import com.xulai.elementalcraft.event.ScorchedHandler;
import com.xulai.elementalcraft.event.StaticShockHandler;
import com.xulai.elementalcraft.event.WetnessHandler;
import com.xulai.elementalcraft.potion.ModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 灾变（L_Ender's Cataclysm）潮湿效果 × ElementalCraft 潮湿系统联动桥。
 *
 * 设计目标（三原则）：
 * 1. 灾变未安装时完全不影响 EC 正常游玩：无编译期依赖（cataclysm:wetness 通过注册表字符串惰性查询），
 *    CATA_LOADED 短路保证灾变缺席时所有处理器空转、连配置都不加载。
 * 2. 灾变潮湿 1:1 替换为 EC 潮湿：拦截 cataclysm:wetness 施加（DENY，效果永不出现，
 *    其 -5% 移速 / 对水敏生物伤害随之消失），按 层数 = 灾变 amp + 1、时长 = 原 tick 原样换算；
 *    灾变"现有效果 amp + 1"的叠层算法因效果被拦截读不到自身效果，由 NBT 计数器（EC_CataclysmWetnessLevel）
 *    复刻其语义：连续命中 1→2→3→4→5 层，干透后从 1 重新开始。
 * 3. 灾变自身针对潮湿的内部处理不受影响、且灾变生物可触发 EC 元素反应：
 *    - 灾变闪电增伤（ServerEventHandler：闪电伤害 ×(1+(amp+1)×0.2)，封顶 amp4=+100%）在 EC 侧同公式复刻；
 *    - 水波 extinguishFire、Deepling MOISTNESS 等独立机制原样未动（经全项目核对，灾变无任何生物 AI 读取潮湿效果）；
 *    - 转换后的 EC 潮湿走标准管线（NBT + 效果 + 衰减），全部 EC 反应链自动生效；
 *    - 灾变 Boss 的 canBeAffected 只接受 cataclysm:effective_for_bosses 标签内效果，
 *      由资源数据包 data/cataclysm/tags/mob_effect/effective_for_bosses.json（replace:false）追加 EC 效果解决。
 *
 * 注意：本类为自包含实现，只使用 EC 各 Handler 的 public API，不改动任何既有 Java 文件。
 */
@Mod.EventBusSubscriber(modid = ElementalCraft.MODID)
public class CataclysmWetnessBridge {

    /** 灾变是否加载（静态快照，与 ISSCore.ISS_LOADED 同一模式）。 */
    public static final boolean CATA_LOADED;

    static {
        boolean loaded = false;
        try {
            loaded = ModList.get() != null && ModList.get().isLoaded("cataclysm");
        } catch (Exception e) {
            loaded = false;
        }
        CATA_LOADED = loaded;
    }

    /** 灾变叠加计数器：模拟"灾变现有效果的 amplifier 对应层数"，效果被拦截后用于复刻其叠层算法。 */
    static final String NBT_CATA_WET_COUNTER = "EC_CataclysmWetnessLevel";

    /** 复刻灾变闪电增伤公式的常量（灾变 ServerEventHandler：i = (amp+1)×0.2，amp 0~4）。 */
    private static final float LIGHTNING_BOOST_PER_LEVEL = 0.2F;
    private static final int LIGHTNING_BOOST_MAX_LEVEL = 5;

    private static MobEffect cachedCataclysmWetness;
    private static boolean cacheAttempted;

    private CataclysmWetnessBridge() {
    }

    private static MobEffect getCataclysmWetness() {
        if (!CATA_LOADED || cacheAttempted) return cachedCataclysmWetness;
        cacheAttempted = true;
        cachedCataclysmWetness = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("cataclysm", "wetness"));
        return cachedCataclysmWetness;
    }

    /** 联动总开关：灾变在场 && 替换开关（常量，仿灾变原生无可配置项）&& EC 潮湿系统启用。 */
    private static boolean active() {
        return CATA_LOADED && ElementalFireNatureReactionsConfig.wetnessMaxLevel > 0;
    }

    // ==================== 1. 拦截 cataclysm:wetness 并 1:1 转换为 EC 潮湿 ====================

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (!active()) return;
        MobEffectInstance instance = event.getEffectInstance();
        MobEffect catWet = getCataclysmWetness();
        if (catWet == null || instance == null || instance.getEffect() != catWet) return;
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) return; // 服务端已 DENY，客户端收不到该效果，无需重复处理

        // 无条件 DENY：cataclysm:wetness 永远不进入效果表（-5% 移速修饰符、水敏伤害随之消失）
        event.setResult(Event.Result.DENY);
        applyAsExternal(target, instance.getAmplifier(), instance.getDuration());
    }

    /**
     * 1:1 换算：灾变 amp A → EC 层数 A+1（风暴蛇硬编码 amp4 → 层 5）；时长 tick 原样保留。
     * 灾变叠层算法（现有效果 amp + 1，clamp 0~4）复刻：计数器 +1，封顶层 5；
     * 不降级实体已有的 EC 潮湿（水/雨等来源）。
     */
    private static void applyAsExternal(LivingEntity target, int amplifier, int durationTicks) {
        CompoundTag data = target.getPersistentData();
        int counter = data.getInt(NBT_CATA_WET_COUNTER);
        int cap = Math.min(5, ElementalFireNatureReactionsConfig.wetnessMaxLevel);
        int fromEvent = Math.min(amplifier + 1, cap);
        int stacked = Math.min(counter + 1, cap);
        int newLevel = Math.max(fromEvent, stacked);
        newLevel = Math.max(newLevel, WetnessHandler.getWetnessLevel(target));
        newLevel = Math.max(1, Math.min(newLevel, ElementalFireNatureReactionsConfig.wetnessMaxLevel));
        data.putInt(NBT_CATA_WET_COUNTER, newLevel);
        tryApplyWetness(target, newLevel, durationTicks);
    }

    /**
     * 施加 EC 潮湿（与 WetnessHandler 的同步逻辑对偶）：
     * - 守卫与 WetnessHandler.isImmune / blockWetnessIfParalyzed / blockWetnessIfFrozen / 灼烧 一致；
     * - 时长换算与 WetnessHandler.syncEffect 对偶：remainingTicks = (threshold - progress) × 20，
     *   故 progress = level×baseTime − durationTicks/20 使首层剩余时长 ≈ durationTicks；
     * - 孢子同化（有孢子且无静电/冻结 → 转孢子）与 syncEffect 内置行为一致；
     * - 随后 WetnessHandler 每 20 tick 的 handleWetnessLogic/syncEffect 会以相同参数续期，无冲突。
     */
    private static boolean tryApplyWetness(LivingEntity entity, int level, int durationTicks) {
        if (ElementalFireNatureReactionsConfig.wetnessMaxLevel <= 0) return false;
        if (entity.level().isClientSide) return false;
        if (ScorchedHandler.isScorched(entity)) return false;
        if (isImmune(entity)) return false;
        if (entity.hasEffect(ModMobEffects.PARALYSIS.get()) || FrostbiteHandler.isFrozen(entity)) return false;

        int clampedLevel = Math.min(level, ElementalFireNatureReactionsConfig.wetnessMaxLevel);
        if (clampedLevel <= 0) return false;

        CompoundTag data = entity.getPersistentData();

        // 孢子同化：与 syncEffect 完全一致（转换后不施加潮湿效果本身）
        if (ModMobEffects.SPORES.isPresent() && entity.hasEffect(ModMobEffects.SPORES.get())) {
            boolean hasStatic = data.getInt(StaticShockHandler.NBT_STATIC_STACKS) > 0;
            boolean hasFrostbite = FrostbiteHandler.isFrozen(entity) || FrostbiteHandler.isTempFrostbite(entity);
            if (!hasStatic && !hasFrostbite) {
                WetnessHandler.convertWetnessToSpores(entity);
                return false;
            }
        }

        int baseTime = Math.max(1, ElementalFireNatureReactionsConfig.wetnessDecayBaseTime);
        int threshold = clampedLevel * baseTime;                       // 单位：20 tick
        float progress = Math.max(0, threshold - Math.max(20, durationTicks) / 20.0F);
        data.putFloat(WetnessHandler.NBT_DECAY_PROGRESS, progress);
        data.putInt(WetnessHandler.NBT_RAIN_TIMER, 0);
        WetnessHandler.updateWetnessLevel(entity, clampedLevel);       // 写 NBT 层数 + 清 NBT_REACTION_RESOLVED

        // 立即施加效果（与 syncEffect 参数一致：ambient=true, 隐藏粒子, 显示图标；暂停态 24000 tick）
        int effectDuration = isPaused(entity) ? 24000 : Math.max(5, durationTicks);
        entity.addEffect(new MobEffectInstance(
                ModMobEffects.WETNESS.get(), effectDuration, clampedLevel - 1, true, false, true));
        return true;
    }

    /** 与 syncEffect 的 isPaused 判定一致：水中 / 此处下雨 / 下雪。 */
    private static boolean isPaused(LivingEntity entity) {
        if (entity.isInWater()) return true;
        BlockPos pos = entity.blockPosition();
        Level level = entity.level();
        if (level.isRainingAt(pos)) return true;
        if (level.isRaining() && level.canSeeSky(pos)) {
            var biome = level.getBiome(pos).value();
            if (biome != null && biome.getPrecipitationAt(pos) == Biome.Precipitation.SNOW) return true;
        }
        return false;
    }

    /** 与 WetnessHandler.isImmune 一致：水生生物 / 下界维度 / 实体黑名单。 */
    private static boolean isImmune(LivingEntity entity) {
        if (ElementalFireNatureReactionsConfig.wetnessWaterAnimalImmune && entity instanceof WaterAnimal) {
            return true;
        }
        if (ElementalFireNatureReactionsConfig.wetnessNetherDimensionImmune
                && entity.level().dimension() == Level.NETHER) {
            return true;
        }
        if (!ElementalFireNatureReactionsConfig.cachedWetnessBlacklist.isEmpty()) {
            var key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            if (key != null && ElementalConfig.matchesBlacklist(
                    ElementalFireNatureReactionsConfig.cachedWetnessBlacklist, key.toString())) {
                return true;
            }
        }
        return false;
    }

    // ==================== 2. 复刻灾变"潮湿 + 闪电 = 增伤"（原处理器检查 cataclysm:wetness，替换后永远不触发） ====================

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDamage(LivingDamageEvent event) {
        if (!active()) return;
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) return;
        if (event.isCanceled()) return;
        if (!event.getSource().is(DamageTypeTags.IS_LIGHTNING)) return;

        // 防御：目标若仍带残留 cataclysm:wetness（清扫窗口内），灾变自己的处理器会按原公式增伤，跳过避免双算
        MobEffect catWet = getCataclysmWetness();
        if (catWet != null && target.hasEffect(catWet)) return;

        int level = WetnessHandler.getWetnessLevel(target);
        if (level <= 0) return;
        int effective = Math.min(level, LIGHTNING_BOOST_MAX_LEVEL);
        if (effective <= 0 || LIGHTNING_BOOST_PER_LEVEL <= 0) return;
        // 与灾变公式逐项一致：damage × (1 + 层数 × 0.2)，层 5 = +100%
        event.setAmount(Math.min(Float.MAX_VALUE,
                event.getAmount() * (1.0F + effective * LIGHTNING_BOOST_PER_LEVEL)));
    }

    // ==================== 3. 残留清扫 + 计数器生命周期 ====================

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!active()) return;
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        if (entity.tickCount % 20 != 0) return;

        CompoundTag data = entity.getPersistentData();
        MobEffect catWet = getCataclysmWetness();
        if (catWet != null) {
            MobEffectInstance residual = entity.getEffect(catWet);
            if (residual != null) {
                // 联动前已存在 / 其他模组施加的残留：移除（顺带清 -5% 移速修饰符）并转换
                entity.removeEffect(catWet);
                applyAsExternal(entity, residual.getAmplifier(), residual.getDuration());
            }
        }
        // EC 潮湿归零 → 计数器复位（对应灾变"效果不在则叠层从 0 重新开始"）
        if (WetnessHandler.getWetnessLevel(entity) <= 0) {
            data.remove(NBT_CATA_WET_COUNTER);
        }
    }
}
