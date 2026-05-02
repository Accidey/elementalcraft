package com.xulai.elementalcraft.config;

import com.xulai.elementalcraft.util.ElementType;
import net.minecraftforge.common.ForgeConfigSpec;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class ElementalConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue MOB_ATTRIBUTE_CHANCE_HOSTILE;
    public static final ForgeConfigSpec.DoubleValue MOB_ATTRIBUTE_CHANCE_NEUTRAL;
    public static final ForgeConfigSpec.DoubleValue ATTACK_ATTRIBUTE_CHANCE;
    public static final ForgeConfigSpec.DoubleValue COUNTER_RESIST_CHANCE;

    public static final ForgeConfigSpec.DoubleValue CHANCE_0_20;
    public static final ForgeConfigSpec.DoubleValue CHANCE_20_50;
    public static final ForgeConfigSpec.DoubleValue CHANCE_50_80;
    public static final ForgeConfigSpec.DoubleValue CHANCE_80_100;

    public static final ForgeConfigSpec.IntValue STRENGTH_PER_LEVEL;
    public static final ForgeConfigSpec.IntValue RESIST_PER_LEVEL;

    public static final ForgeConfigSpec.IntValue STRENGTH_PER_HALF_DAMAGE;
    public static final ForgeConfigSpec.IntValue RESIST_PER_HALF_REDUCTION;
    public static final ForgeConfigSpec.DoubleValue ELEMENTAL_DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue ELEMENTAL_RESISTANCE_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue MAX_STAT_CAP;

    public static final ForgeConfigSpec.DoubleValue RESTRAINT_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue WEAK_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue RESTRAINT_MIN_DAMAGE_PERCENT;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ELEMENT_RESTRAINTS;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> FORCED_ENTITIES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_ENTITIES;

    public static final ForgeConfigSpec.BooleanValue NETHER_DIMENSION_FORCED_FIRE;
    public static final ForgeConfigSpec.IntValue NETHER_FIRE_POINTS;
    public static final ForgeConfigSpec.BooleanValue END_DIMENSION_FORCED_THUNDER;
    public static final ForgeConfigSpec.IntValue END_THUNDER_POINTS;

    public static final ForgeConfigSpec.DoubleValue HOT_FIRE_BIAS;
    public static final ForgeConfigSpec.DoubleValue COLD_FROST_BIAS;
    public static final ForgeConfigSpec.DoubleValue FOREST_NATURE_BIAS;
    public static final ForgeConfigSpec.DoubleValue THUNDERSTORM_THUNDER_BIAS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CUSTOM_BIOME_ATTRIBUTE_BIAS;

    static {
        ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

        BUILDER.comment("Mob Attribute Generation System", "怪物属性生成系统")
                .push("mob_attribute_generation");

        MOB_ATTRIBUTE_CHANCE_HOSTILE = BUILDER
                .comment("Chance for hostile mobs (e.g., Zombies, Skeletons, Creepers) to become elemental.",
                        "When a hostile mob spawns, this probability determines whether it gains elemental attributes.",
                        "Higher values = more elemental mobs in the world.",
                        "",
                        "敌对生物（如僵尸、骷髅、苦力怕）成为属性生物的概率。",
                        "当敌对生物生成时，该概率决定它是否获得元素属性。",
                        "数值越高，世界中属性生物越多。",
                        "",
                        "Default: 0.40 (40%) / 默认：0.40（40%）")
                .defineInRange("mob_attribute_chance_hostile", 0.40, 0.0, 1.0);

        MOB_ATTRIBUTE_CHANCE_NEUTRAL = BUILDER
                .comment("Chance for neutral mobs (e.g., Piglins, Endermen, Wolves) to become elemental.",
                        "Only applies if the mob is not blacklisted or in a forced-dimension (Nether/End).",
                        "Higher values = more neutral mobs gain elemental powers.",
                        "",
                        "中立生物（如猪灵、末影人、狼）成为属性生物的概率。",
                        "仅在生物未被列入黑名单且不在强制维度（下界/末地）时生效。",
                        "数值越高，更多中立生物获得元素能力。",
                        "",
                        "Default: 0.30 (30%) / 默认：0.30（30%）")
                .defineInRange("mob_attribute_chance_neutral", 0.30, 0.0, 1.0);

        ATTACK_ATTRIBUTE_CHANCE = BUILDER
                .comment("Chance for an elemental mob to also gain an attack element on its weapon.",
                        "If triggered, the mob's weapon (existing or auto-given iron sword) will be enchanted",
                        "with the attack element matching its biome-biased element.",
                        "If the mob has no weapon, an iron sword with the enchantment will be given (drop chance 0%).",
                        "",
                        "属性生物同时在武器上获得攻击属性的概率。",
                        "触发后，生物的武器（已有武器或自动给予的铁剑）将被附魔",
                        "攻击属性，与其群系偏好元素一致。",
                        "如果生物没有武器，会自动给予一把附魔铁剑（掉落概率 0%）。",
                        "",
                        "Default: 0.60 (60%) / 默认：0.60（60%）")
                .defineInRange("attack_attribute_chance", 0.60, 0.0, 1.0);

        COUNTER_RESIST_CHANCE = BUILDER
                .comment("Chance for an elemental mob to gain resistance against the element that counters its attack.",
                        "Example: A mob with Fire attack has a chance to gain Nature resistance (since Nature counters Fire).",
                        "This makes the mob harder to exploit via elemental advantages.",
                        "If not triggered, a random non-none element is chosen for resistance.",
                        "",
                        "属性生物获得针对自身克制属性的抗性的概率。",
                        "示例：拥有赤焰攻击的生物有概率获得自然抗性（因为自然克制赤焰）。",
                        "这使得生物更难被属性克制所针对。",
                        "如果未触发，则随机选择一个非空属性作为抗性。",
                        "",
                        "Default: 0.70 (70%) / 默认：0.70（70%）")
                .defineInRange("counter_resist_chance", 0.70, 0.0, 1.0);

        BUILDER.comment("Mob Strength & Resistance Value Distribution",
                "怪物强化值与抗性值的概率分段分布",
                "",
                "These four probabilities control how strong elemental mobs are.",
                "Each represents the chance to roll a value in that percentage range of the maximum.",
                "The four ranges are continuous (0-20%, 20-50%, 50-80%, 80-100%).",
                "",
                "这四个概率控制属性生物的强弱分布。",
                "每个概率代表在最大值的对应百分比区间内取值的几率。",
                "四个区间是连续的（0~20%、20~50%、50~80%、80~100%）。",
                "",
                "Example with max_stat_cap=25 (max total = 100):",
                "  0-20%  → 0~20 points  (very weak)",
                "  20-50% → 20~50 points (moderate)",
                "  50-80% → 50~80 points (strong)",
                "  80-100%→ 80~100 points (elite)",
                "",
                "示例：max_stat_cap=25（最大总值=100）时：",
                "  0~20%  → 0~20 点（很弱）",
                "  20~50% → 20~50 点（中等）",
                "  50~80% → 50~80 点（较强）",
                "  80~100%→ 80~100 点（精英）",
                "",
                "The sum of all four does NOT need to equal 1.0.",
                "If the sum is less than 1.0, remaining probability defaults to the 80-100% range.",
                "",
                "四个概率的总和不需要等于 1.0。",
                "如果总和小于 1.0，剩余概率默认归入 80~100% 区间。");

        CHANCE_0_20 = BUILDER
                .comment("Probability of rolling a value in the 0-20% range of max.",
                        "Higher = more weak mobs.",
                        "",
                        "在最大值的 0~20% 区间内取值的概率。",
                        "数值越高，弱小生物越多。",
                        "",
                        "Default: 0.40 / 默认：0.40")
                .defineInRange("chance_0_20", 0.40, 0.0, 1.0);

        CHANCE_20_50 = BUILDER
                .comment("Probability of rolling a value in the 20-50% range of max.",
                        "This is the 'average' tier.",
                        "",
                        "在最大值的 20~50% 区间内取值的概率。",
                        "这是普通档位。",
                        "",
                        "Default: 0.30 / 默认：0.30")
                .defineInRange("chance_20_50", 0.30, 0.0, 1.0);

        CHANCE_50_80 = BUILDER
                .comment("Probability of rolling a value in the 50-80% range of max.",
                        "These mobs are noticeably stronger.",
                        "",
                        "在最大值的 50~80% 区间内取值的概率。",
                        "这些生物明显更强。",
                        "",
                        "Default: 0.15 / 默认：0.15")
                .defineInRange("chance_50_80", 0.15, 0.0, 1.0);

        CHANCE_80_100 = BUILDER
                .comment("Probability of rolling a value in the 80-100% range of max.",
                        "These are elite-tier mobs with near-maximum attributes.",
                        "Also absorbs any leftover probability from the other three ranges.",
                        "",
                        "在最大值的 80~100% 区间内取值的概率。",
                        "这些是接近满属性的精英级生物。",
                        "同时吸收其他三个区间未覆盖的剩余概率。",
                        "",
                        "Default: 0.10 / 默认：0.10")
                .defineInRange("chance_80_100", 0.10, 0.0, 1.0);

        BUILDER.pop();

        BUILDER.comment("Attribute Value System", "属性数值系统")
                .push("attribute_values");

        STRENGTH_PER_LEVEL = BUILDER
                .comment("How many strength points each level of the Elemental Strength enchantment provides.",
                        "This also affects how forced-entity strength points translate to enchantment levels.",
                        "Formula: enchantment_level = total_points / strength_per_level",
                        "",
                        "每级属性强化附魔提供多少强化点数。",
                        "这也影响强制属性生物的强化点数如何转化为附魔等级。",
                        "公式：附魔等级 = 总点数 / strength_per_level",
                        "",
                        "Example: If strength_per_level = 5 and a mob has 25 strength points,",
                        "the armor piece gets Strength Lv.5.",
                        "",
                        "示例：若 strength_per_level = 5，生物有 25 点强化值，",
                        "则护甲获得强化 Lv.5。",
                        "",
                        "Default: 5 / 默认：5")
                .defineInRange("strength_per_level", 5, 1, 1000);

        RESIST_PER_LEVEL = BUILDER
                .comment("How many resistance points each level of the Elemental Resistance enchantment provides.",
                        "Same logic as strength_per_level but for resistance enchantments.",
                        "",
                        "每级属性抗性附魔提供多少抗性点数。",
                        "逻辑与 strength_per_level 相同，但用于抗性附魔。",
                        "",
                        "Default: 5 / 默认：5")
                .defineInRange("resist_per_level", 5, 1, 1000);

        MAX_STAT_CAP = BUILDER
                .comment("Maximum points for a SINGLE attribute on a SINGLE armor piece.",
                        "This is the hard cap per armor slot, NOT the total across all 4 pieces.",
                        "",
                        "单件护甲上单个属性（强化或抗性）的最大点数上限。",
                        "这是单个护甲槽位的硬性上限，不是 4 件护甲的总和。",
                        "",
                        "Total possible points per attribute per mob = max_stat_cap × 4 (4 armor pieces).",
                        "单个属性的总点数上限 = max_stat_cap × 4（4 件护甲）。",
                        "",
                        "Maximum enchantment level per piece = max_stat_cap / points_per_level.",
                        "单件护甲的最大附魔等级 = max_stat_cap / 每级点数。",
                        "",
                        "IMPORTANT: When you change strength_per_level or resist_per_level,",
                        "adjust this value accordingly to control the maximum enchantment level.",
                        "",
                        "重要：当你修改 strength_per_level 或 resist_per_level 时，",
                        "请同步调整此值以控制最大附魔等级。",
                        "",
                        "Example: resist_per_level = 10, want max Lv.5 per piece → set max_stat_cap = 50.",
                        "示例：resist_per_level = 10，希望单件最高 Lv.5 → 设置 max_stat_cap = 50。",
                        "",
                        "Default: 25 / 默认：25")
                .defineInRange("max_stat_cap", 25, 10, 100000);

        BUILDER.pop();

        BUILDER.comment("Elemental Damage System", "属性伤害系统")
                .push("elemental_damage");

        STRENGTH_PER_HALF_DAMAGE = BUILDER
                .comment("Controls how much elemental damage each strength point adds.",
                        "Formula: elemental_damage += (0.5 / this_value) × strength_points",
                        "",
                        "控制每点强化值增加多少元素伤害。",
                        "公式：元素伤害 += (0.5 / 该值) × 强化点数",
                        "",
                        "Higher value = each point contributes LESS damage (more granular scaling).",
                        "数值越高 = 每点强化增加的伤害越少（更精细的缩放）。",
                        "",
                        "Example with value 10: 1 point = +0.05 damage, 50 points = +2.5 damage.",
                        "示例：该值为 10 时，1 点 = +0.05 伤害，50 点 = +2.5 伤害。",
                        "",
                        "Example with value 20: 1 point = +0.025 damage, 50 points = +1.25 damage.",
                        "示例：该值为 20 时，1 点 = +0.025 伤害，50 点 = +1.25 伤害。",
                        "",
                        "Default: 10 / 默认：10")
                .defineInRange("strength_per_half_damage", 10, 1, 1000);

        RESIST_PER_HALF_REDUCTION = BUILDER
                .comment("Controls how much elemental damage each resistance point reduces.",
                        "Formula: damage_reduction += (0.5 / this_value) × resistance_points",
                        "",
                        "控制每点抗性值减免多少元素伤害。",
                        "公式：伤害减免 += (0.5 / 该值) × 抗性点数",
                        "",
                        "Higher value = each point reduces LESS damage (more granular scaling).",
                        "数值越高 = 每点抗性减免的伤害越少（更精细的缩放）。",
                        "",
                        "Example with value 10: 1 point = -0.05 damage, 50 points = -2.5 damage.",
                        "示例：该值为 10 时，1 点 = -0.05 伤害，50 点 = -2.5 伤害。",
                        "",
                        "Default: 10 / 默认：10")
                .defineInRange("resist_per_half_reduction", 10, 1, 1000);

        ELEMENTAL_DAMAGE_MULTIPLIER = BUILDER
                .comment("Global multiplier applied to ALL elemental damage dealt.",
                        "Final damage = calculated_elemental_damage × this_multiplier",
                        "",
                        "全局元素伤害倍率，作用于所有元素伤害。",
                        "最终伤害 = 计算出的元素伤害 × 此倍率",
                        "",
                        "1.0 = normal damage, 2.0 = double damage, 0.5 = half damage.",
                        "1.0 = 正常伤害，2.0 = 双倍伤害，0.5 = 一半伤害。",
                        "",
                        "Default: 1.0 / 默认：1.0")
                .defineInRange("elemental_damage_multiplier", 1.0, 0.0, 100.0);

        ELEMENTAL_RESISTANCE_MULTIPLIER = BUILDER
                .comment("Global multiplier applied to ALL elemental resistance reduction.",
                        "Final reduction = calculated_resistance × this_multiplier",
                        "",
                        "全局元素抗性减免倍率，作用于所有元素抗性减免。",
                        "最终减免 = 计算出的抗性减免 × 此倍率",
                        "",
                        "1.0 = normal reduction, 2.0 = double reduction, 0.5 = half reduction.",
                        "1.0 = 正常减免，2.0 = 双倍减免，0.5 = 一半减免。",
                        "",
                        "Default: 1.0 / 默认：1.0")
                .defineInRange("elemental_resistance_multiplier", 1.0, 0.0, 100.0);

        BUILDER.pop();

        BUILDER.comment("Elemental Restraint System", "属性克制系统")
                .push("elemental_restraints");

        RESTRAINT_MULTIPLIER = BUILDER
                .comment("Damage multiplier when the attacker's element restrains the target's element.",
                        "Example: Fire vs Nature (Fire restrains Nature) → damage × this_value",
                        "",
                        "当攻击方元素克制目标元素时的伤害倍率。",
                        "示例：赤焰 vs 自然（赤焰克制自然）→ 伤害 × 此值",
                        "",
                        "1.5 means 50% more damage on a restrained target.",
                        "1.5 表示对被克制目标造成 50% 额外伤害。",
                        "",
                        "Default: 1.5 / 默认：1.5")
                .defineInRange("restraint_multiplier", 1.5, 0.1, 10.0);

        WEAK_MULTIPLIER = BUILDER
                .comment("Damage multiplier when the attacker's element is WEAK against the target's element.",
                        "Example: Fire vs Frost (Frost restrains Fire) → damage × this_value",
                        "",
                        "当攻击方元素被目标元素克制时的伤害倍率。",
                        "示例：赤焰 vs 冰霜（冰霜克制赤焰）→ 伤害 × 此值",
                        "",
                        "0.5 means 50% less damage when attacking a counter-element.",
                        "0.5 表示攻击克制自己的属性时只造成 50% 伤害。",
                        "",
                        "Default: 0.5 / 默认：0.5")
                .defineInRange("weak_multiplier", 0.5, 0.1, 10.0);

        RESTRAINT_MIN_DAMAGE_PERCENT = BUILDER
                .comment("Minimum elemental damage percentage that ALWAYS gets through, even if resistance fully negates it.",
                        "Only applies when the attacker's element restrains the target AND resistance would reduce damage to 0.",
                        "Value is a percentage of the original elemental damage (before resistance).",
                        "",
                        "即使抗性完全抵消伤害，仍然保留的最低元素伤害百分比。",
                        "仅在攻击方克制目标 且 抗性将伤害减免至 0 时生效。",
                        "该值是原始元素伤害（抗性减免前）的百分比。",
                        "",
                        "Example: Original damage = 3.0, resistance reduces it to 0, min_percent = 0.1",
                        "→ Final damage = 3.0 × 0.1 = 0.3 (guaranteed minimum).",
                        "",
                        "示例：原始伤害 = 3.0，抗性将其减至 0，最低百分比 = 0.1",
                        "→ 最终伤害 = 3.0 × 0.1 = 0.3（保底伤害）。",
                        "",
                        "Default: 0.1 (10%) / 默认：0.1（10%）")
                .defineInRange("restraint_min_damage_percent", 0.1, 0.0, 1.0);

        ELEMENT_RESTRAINTS = BUILDER
                .comment("Define which element restrains which.",
                        "Format: \"attacker_element->victim_element\"",
                        "When the attacker's element matches the left side and the victim's matches the right,",
                        "the restraint_multiplier is applied.",
                        "",
                        "定义属性克制关系。",
                        "格式：\"攻击方元素->受害方元素\"",
                        "当攻击方元素匹配左侧、受害方元素匹配右侧时，",
                        "应用 restraint_multiplier 克制倍率。",
                        "",
                        "Supported element IDs: fire, nature, frost, thunder",
                        "支持的属性ID：fire（赤焰）、nature（自然）、frost（冰霜）、thunder（雷霆）",
                        "",
                        "Default cycle: fire→nature→thunder→frost→fire",
                        "默认克制链：赤焰→自然→雷霆→冰霜→赤焰",
                        "",
                        "You can add, remove, or rearrange entries to change the restraint cycle.",
                        "你可以添加、删除或重新排列条目来改变克制关系。",
                        "",
                        "Default:",
                        "  \"fire->nature\"    (Fire restrains Nature / 赤焰克制自然)",
                        "  \"nature->thunder\" (Nature restrains Thunder / 自然克制雷霆)",
                        "  \"thunder->frost\"  (Thunder restrains Frost / 雷霆克制冰霜)",
                        "  \"frost->fire\"     (Frost restrains Fire / 冰霜克制赤焰)")
                .defineList("element_restraints",
                        List.of("fire->nature", "nature->thunder", "thunder->frost", "frost->fire"),
                        obj -> obj instanceof String s && s.matches("^[a-z]+->[a-z]+$"));

        BUILDER.pop();

        BUILDER.comment("Forced Attribute System", "强制属性系统")
                .push("forced_attributes");

        FORCED_ENTITIES = BUILDER
                .comment("Custom forced elemental attributes for specific entity types.",
                        "These override the normal random generation system for listed entities.",
                        "",
                        "为指定实体类型自定义强制元素属性。",
                        "列表中的实体将跳过正常随机生成，直接使用这里定义的属性。",
                        "",
                        "Each entry must be enclosed in double quotes \"\".",
                        "Each line defines ONE attribute set for ONE entity type.",
                        "An entity type can have MULTIPLE entries (one is randomly chosen at spawn).",
                        "",
                        "每个条目必须用英文双引号 \"\" 包裹。",
                        "每行定义一个实体类型的一组属性配置。",
                        "同一实体类型可以有多行（生成时随机选择一行）。",
                        "",
                        "Format (6 fields, comma-separated):",
                        "格式（6 个字段，英文逗号分隔）：",
                        "  \"EntityID,AttackElement,StrengthElement,StrengthPoints,ResistElement,ResistPoints\"",
                        "",
                        "Field details / 字段说明：",
                        "  1. EntityID       - Full entity registry name / 实体注册名（如 minecraft:zombie）",
                        "  2. AttackElement   - Element on weapon (fire/frost/thunder/nature/none/empty=none) / 武器攻击属性",
                        "  3. StrengthElement - Armor strength element (fire/frost/thunder/nature/none/empty=none) / 护甲强化属性",
                        "  4. StrengthPoints  - Strength point value / 强化点数",
                        "  5. ResistElement   - Armor resistance element / 护甲抗性属性",
                        "  6. ResistPoints    - Resistance point value / 抗性点数",
                        "",
                        "Point value formats / 点数格式：",
                        "  • Fixed: 100          → always exactly 100 / 固定值：始终为 100",
                        "  • Range: 50-200       → random between 50 and 200, weighted by chance_0_20~chance_80_100 / 随机范围，受概率分段权重影响",
                        "  • Empty or 0          → no attribute assigned / 留空或0 = 不赋予该属性",
                        "",
                        "Examples / 示例：",
                        "  \"minecraft:zombie,fire,fire,100,fire,100\"",
                        "    → Zombie: Fire attack, Fire strength 100, Fire resistance 100",
                        "    → 赤焰僵尸：赤焰攻击，赤焰强化100，赤焰抗性100",
                        "",
                        "  \"minecraft:skeleton,,nature,50-100,nature,100\"",
                        "    → Skeleton: No attack, Nature strength 50-100 (random), Nature resistance 100",
                        "    → 骷髅：无攻击属性，自然强化50~100随机，自然抗性固定100",
                        "",
                        "  \"minecraft:piglin,thunder,thunder,200,frost,150-400\"",
                        "    → Piglin: Thunder attack, Thunder strength 200, Frost resistance 150-400",
                        "    → 猪灵：雷霆攻击，雷霆强化200，冰霜抗性150~400随机",
                        "",
                        "  \"minecraft:creeper,none,fire,0-150,fire,50-200\"",
                        "    → Creeper: No attack, Fire strength 0-150, Fire resistance 50-200",
                        "    → 苦力怕：无攻击属性，赤焰强化0~150随机，赤焰抗性50~200随机",
                        "",
                        "  \"minecraft:blaze,,,fire,300\"",
                        "    → Blaze: No attack, no strength, Fire resistance 300 only",
                        "    → 烈焰人：无攻击无强化，仅赤焰抗性300",
                        "",
                        "  \"minecraft:wither,thunder,,100-500,,200-800\"",
                        "    → Wither: Thunder attack, random strength 100-500, random resistance 200-800",
                        "    → 凋灵：雷霆攻击，强化100~500随机，抗性200~800随机")
                .defineListAllowEmpty("forced_entities", List.of(), o -> o instanceof String);

        BUILDER.pop();

        BUILDER.comment("Nether Dimension Forced Fire", "下界维度强制赤焰属性")
                .push("nether_forced_fire");

        NETHER_DIMENSION_FORCED_FIRE = BUILDER
                .comment("When enabled, ALL mobs spawned in the Nether dimension automatically gain:",
                        "  - Fire attack element (if they have a weapon)",
                        "  - Fire strength on all armor",
                        "  - Fire resistance on all armor",
                        "",
                        "This overrides normal random generation AND takes priority over forced_entities config.",
                        "Disable to fall back to normal random generation or forced_entities.",
                        "",
                        "开启后，下界维度中所有生成的生物自动获得：",
                        "  - 赤焰攻击属性（如果有武器）",
                        "  - 全套护甲的赤焰强化",
                        "  - 全套护甲的赤焰抗性",
                        "",
                        "此选项覆盖正常随机生成，且优先级高于 forced_entities 配置。",
                        "关闭后回退到正常随机生成或 forced_entities 配置。",
                        "",
                        "Default: true / 默认：true")
                .define("nether_dimension_forced_fire", true);

        NETHER_FIRE_POINTS = BUILDER
                .comment("The point value used for Nether forced Fire strength AND resistance.",
                        "This value is applied to both strength and resistance equally.",
                        "",
                        "下界强制赤焰属性使用的点数，同时应用于强化和抗性。",
                        "",
                        "Enchantment level = nether_fire_points / per_level (strength_per_level or resist_per_level).",
                        "附魔等级 = nether_fire_points / 每级点数（strength_per_level 或 resist_per_level）。",
                        "",
                        "Example: nether_fire_points=100, strength_per_level=5 → Strength Lv.20 on each piece.",
                        "示例：nether_fire_points=100, strength_per_level=5 → 每件护甲强化 Lv.20。",
                        "",
                        "Default: 100 / 默认：100")
                .defineInRange("nether_fire_points", 100, 100, 100000);

        BUILDER.pop();

        BUILDER.comment("End Dimension Forced Thunder", "末地维度强制雷霆属性")
                .push("end_forced_thunder");

        END_DIMENSION_FORCED_THUNDER = BUILDER
                .comment("When enabled, ALL mobs spawned in The End dimension automatically gain:",
                        "  - Thunder attack element (if they have a weapon)",
                        "  - Thunder strength on all armor",
                        "  - Thunder resistance on all armor",
                        "",
                        "This overrides normal random generation AND takes priority over forced_entities config.",
                        "Disable to fall back to normal random generation or forced_entities.",
                        "",
                        "开启后，末地维度中所有生成的生物自动获得：",
                        "  - 雷霆攻击属性（如果有武器）",
                        "  - 全套护甲的雷霆强化",
                        "  - 全套护甲的雷霆抗性",
                        "",
                        "此选项覆盖正常随机生成，且优先级高于 forced_entities 配置。",
                        "关闭后回退到正常随机生成或 forced_entities 配置。",
                        "",
                        "Default: true / 默认：true")
                .define("end_dimension_forced_thunder", true);

        END_THUNDER_POINTS = BUILDER
                .comment("The point value used for End forced Thunder strength AND resistance.",
                        "This value is applied to both strength and resistance equally.",
                        "",
                        "末地强制雷霆属性使用的点数，同时应用于强化和抗性。",
                        "",
                        "Enchantment level = end_thunder_points / per_level (strength_per_level or resist_per_level).",
                        "附魔等级 = end_thunder_points / 每级点数（strength_per_level 或 resist_per_level）。",
                        "",
                        "Recommended: set this >= max_stat_cap to ensure maximum enchantment levels.",
                        "建议：设为 >= max_stat_cap 以确保达到最大附魔等级。",
                        "",
                        "Default: 100 / 默认：100")
                .defineInRange("end_thunder_points", 100, 100, 100000);

        BUILDER.pop();

        BUILDER.comment("Entity Blacklist", "属性生物黑名单")
                .push("blacklist");

        BLACKLISTED_ENTITIES = BUILDER
                .comment("Entities listed here will NEVER receive elemental attributes.",
                        "This applies globally: random generation, Nether/End forced, and forced_entities are all blocked.",
                        "",
                        "列表中的实体永远不会获得元素属性。",
                        "全局生效：随机生成、下界/末地强制、以及 forced_entities 配置均被阻止。",
                        "",
                        "Format: full entity ID in double quotes.",
                        "格式：完整的实体 ID，用双引号包裹。",
                        "",
                        "Examples / 示例：",
                        "  \"minecraft:creeper\"      - Creeper never gains attributes / 苦力怕永不获得属性",
                        "  \"minecraft:ghast\"        - Ghast never gains attributes / 恶魂永不获得属性",
                        "  \"minecraft:ender_dragon\" - Ender Dragon never gains attributes / 末影龙永不获得属性",
                        "  \"minecraft:wither\"       - Wither never gains attributes / 凋灵永不获得属性")
                .defineListAllowEmpty("blacklisted_entities", List.of(), obj -> obj instanceof String);

        BUILDER.pop();

        BUILDER.comment("Biome & Weather Attribute Bias", "生物群系与天气属性偏好")
                .push("biome_weather_bias");

        HOT_FIRE_BIAS = BUILDER
                .comment("Weight for Fire element in hot biomes (Desert, Savanna, Badlands, Nether, etc.).",
                        "This value is combined with weights for other elements and normalized to determine probability.",
                        "",
                        "炎热生物群系（沙漠、热带草原、恶地、下界等）中赤焰属性的权重。",
                        "该值与其他属性的权重合并后归一化，得出实际概率。",
                        "",
                        "Example: hot_fire_bias=60, and other biases are 10, 10, 10 → Fire probability = 60/(60+10+10+10) = 66.7%.",
                        "示例：hot_fire_bias=60，其他偏好为 10、10、10 → 赤焰概率 = 60/(60+10+10+10) = 66.7%。",
                        "",
                        "Default: 60.0 / 默认：60.0")
                .defineInRange("hot_fire_bias", 60.0, 0.0, 100.0);

        COLD_FROST_BIAS = BUILDER
                .comment("Weight for Frost element in cold biomes (Snowy Plains, Ice Spikes, Frozen Ocean, etc.).",
                        "",
                        "寒冷生物群系（雪原、冰刺之地、冻洋等）中冰霜属性的权重。",
                        "",
                        "Default: 60.0 / 默认：60.0")
                .defineInRange("cold_frost_bias", 60.0, 0.0, 100.0);

        FOREST_NATURE_BIAS = BUILDER
                .comment("Weight for Nature element in forest biomes (Forest, Dark Forest, Flower Forest, Jungle, etc.).",
                        "",
                        "森林类生物群系（森林、黑森林、花林、丛林等）中自然属性的权重。",
                        "",
                        "Default: 60.0 / 默认：60.0")
                .defineInRange("forest_nature_bias", 60.0, 0.0, 100.0);

        THUNDERSTORM_THUNDER_BIAS = BUILDER
                .comment("Weight for Thunder element during thunderstorm weather (global, overrides biome bias).",
                        "When a thunderstorm is active, this weight replaces the biome-based weight for Thunder.",
                        "Biome Fire/Frost/Nature weights still compete normally.",
                        "",
                        "雷雨天气时雷霆属性的权重（全局生效，覆盖群系偏好）。",
                        "雷雨天气激活时，此权重替代群系对雷霆的权重。",
                        "群系的赤焰/冰霜/自然权重仍正常竞争。",
                        "",
                        "Example: thunderstorm_bias=80, hot_fire_bias=10, others=10 → Thunder=80%, Fire≈7%, others≈7%.",
                        "示例：thunderstorm_bias=80, hot_fire_bias=10, 其他=10 → 雷霆≈80%，赤焰≈7%，其他≈7%。",
                        "",
                        "Default: 80.0 / 默认：80.0")
                .defineInRange("thunderstorm_thunder_bias", 80.0, 0.0, 100.0);

        BUILDER.pop();

        BUILDER.comment("Custom Biome Attribute Probabilities", "自定义生物群系属性概率")
                .push("custom_biome_attribute_bias");

        CUSTOM_BIOME_ATTRIBUTE_BIAS = BUILDER
                .comment("Override or supplement the default biome attribute weights for specific biomes.",
                        "Entries here take priority over the default hot/cold/forest bias for matching biomes.",
                        "",
                        "为特定生物群系覆盖或补充默认的属性权重。",
                        "此处的条目优先于默认的炎热/寒冷/森林偏好（对匹配的群系生效）。",
                        "",
                        "Format: \"BiomeID,ElementID,Probability\"",
                        "格式：\"群系ID,属性ID,概率\"",
                        "",
                        "  BiomeID    - Full biome registry name (e.g., minecraft:desert, minecraft:snowy_plains)",
                        "  BiomeID    - 完整的群系注册名（如 minecraft:desert, minecraft:snowy_plains）",
                        "  ElementID  - fire / frost / thunder / nature",
                        "  Probability - 0-100 (%)，weight for this element in this biome",
                        "  Probability - 0~100（%），该群系中此属性的权重",
                        "",
                        "Multiple entries for the same biome accumulate (summed as weights, then normalized).",
                        "同一群系的多个条目会叠加权重（累加后归一化）。",
                        "",
                        "Examples / 示例：",
                        "  \"minecraft:desert,fire,70\"",
                        "    → Desert: Fire gets 70% weight / 沙漠：赤焰权重 70%",
                        "  \"minecraft:desert,frost,30\"",
                        "    → Desert: Frost also gets 30% weight (Fire 70% + Frost 30% → Fire 70%, Frost 30%)",
                        "    → 沙漠：冰霜也获得 30% 权重（赤焰 70% + 冰霜 30% → 赤焰 70%，冰霜 30%）",
                        "  \"minecraft:snowy_plains,frost,80\"",
                        "    → Snowy Plains: Frost gets 80% weight / 雪原：冰霜权重 80%",
                        "  \"minecraft:jungle,nature,60\"",
                        "    → Jungle: Nature gets 60% weight / 丛林：自然权重 60%")
                .defineListAllowEmpty("custom_biome_attribute_bias", List.of(),
                        obj -> obj instanceof String s && s.matches("^[a-z_]+:[a-z_]+,[a-z]+,\\d{1,3}(\\.\\d+)?$"));

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static double restraintMultiplier = 1.5;
    public static double weakMultiplier = 0.5;
    public static double restraintMinDamagePercent = 0.5;
    public static double elementalDamageMultiplier = 1.0;
    public static double elementalResistanceMultiplier = 1.0;
    public static int maxStatCap = 100;
    public static int strengthPerLevel = 5;
    public static int resistPerLevel = 5;
    public static int strengthPerHalfDamage = 10;
    public static int resistPerHalfReduction = 10;

    public static double chance0_20, chance20_50, chance50_80, chance80_100;
    public static double mobChanceHostile, mobChanceNeutral, attackChance, counterResistChance;

    public static boolean netherForcedFire = true;
    public static int netherFirePoints = 100;
    public static boolean endForcedThunder = true;
    public static int endThunderPoints = 100;

    public static double hotFireBias = 60.0;
    public static double coldFrostBias = 60.0;
    public static double forestNatureBias = 60.0;
    public static double thunderstormBias = 80.0;

    public static List<? extends String> cachedRestraints = List.of();
    public static List<? extends String> cachedBlacklist = List.of();

    public static void refreshCache() {
        restraintMultiplier = RESTRAINT_MULTIPLIER.get();
        weakMultiplier = WEAK_MULTIPLIER.get();
        restraintMinDamagePercent = RESTRAINT_MIN_DAMAGE_PERCENT.get();
        elementalDamageMultiplier = ELEMENTAL_DAMAGE_MULTIPLIER.get();
        elementalResistanceMultiplier = ELEMENTAL_RESISTANCE_MULTIPLIER.get();

        maxStatCap = MAX_STAT_CAP.get();
        strengthPerLevel = Math.max(1, STRENGTH_PER_LEVEL.get());
        resistPerLevel = Math.max(1, RESIST_PER_LEVEL.get());
        strengthPerHalfDamage = STRENGTH_PER_HALF_DAMAGE.get();
        resistPerHalfReduction = RESIST_PER_HALF_REDUCTION.get();

        chance0_20 = CHANCE_0_20.get();
        chance20_50 = CHANCE_20_50.get();
        chance50_80 = CHANCE_50_80.get();
        chance80_100 = CHANCE_80_100.get();

        mobChanceHostile = MOB_ATTRIBUTE_CHANCE_HOSTILE.get();
        mobChanceNeutral = MOB_ATTRIBUTE_CHANCE_NEUTRAL.get();
        attackChance = ATTACK_ATTRIBUTE_CHANCE.get();
        counterResistChance = COUNTER_RESIST_CHANCE.get();

        netherForcedFire = NETHER_DIMENSION_FORCED_FIRE.get();
        netherFirePoints = NETHER_FIRE_POINTS.get();
        endForcedThunder = END_DIMENSION_FORCED_THUNDER.get();
        endThunderPoints = END_THUNDER_POINTS.get();

        hotFireBias = HOT_FIRE_BIAS.get();
        coldFrostBias = COLD_FROST_BIAS.get();
        forestNatureBias = FOREST_NATURE_BIAS.get();
        thunderstormBias = THUNDERSTORM_THUNDER_BIAS.get();

        cachedRestraints = ELEMENT_RESTRAINTS.get();
        cachedBlacklist = BLACKLISTED_ENTITIES.get();
    }

    public static int getStrengthPerHalfDamage() {
        return strengthPerHalfDamage;
    }

    public static int getResistPerHalfReduction() {
        return resistPerHalfReduction;
    }

    public static int getMaxStatCap() {
        return maxStatCap;
    }

    public static int getStrengthPerLevel() {
        return strengthPerLevel;
    }

    public static int getResistPerLevel() {
        return resistPerLevel;
    }

    public static float getRestraintMultiplier(ElementType attackElement, ElementType targetElement) {
        if (attackElement == null || targetElement == null || targetElement == ElementType.NONE) {
            return 1.0f;
        }

        String relation = attackElement.getId() + "->" + targetElement.getId();

        if (cachedRestraints.contains(relation)) {
            return (float) restraintMultiplier;
        }

        String reverse = targetElement.getId() + "->" + attackElement.getId();
        if (cachedRestraints.contains(reverse)) {
            return (float) weakMultiplier;
        }

        return 1.0f;
    }

    private static int rollDynamicValue(double c1, double c2, double c3, int maxValue) {
        int cap = Math.max(1, maxValue);

        double roll = ThreadLocalRandom.current().nextDouble();
        double s1 = c1;
        double s2 = s1 + c2;
        double s3 = s2 + c3;

        int min, max;

        if (roll < s1) {
            min = 1;
            max = (int) (cap * 0.20);
        } else if (roll < s2) {
            min = (int) (cap * 0.20);
            max = (int) (cap * 0.50);
        } else if (roll < s3) {
            min = (int) (cap * 0.50);
            max = (int) (cap * 0.80);
        } else {
            min = (int) (cap * 0.80);
            max = cap;
        }

        if (max < min) max = min;
        if (min < 1) min = 1;

        if (max == min) return min;
        return min + ThreadLocalRandom.current().nextInt(max - min + 1);
    }

    public static int rollMonsterStrength() {
        return rollDynamicValue(chance0_20, chance20_50, chance50_80, maxStatCap * 4);
    }

    public static int rollMonsterResist() {
        return rollDynamicValue(chance0_20, chance20_50, chance50_80, maxStatCap * 4);
    }
}
