# ElementalCraft: Reactions（中文描述在下方）

Welcome to **ElementalCraft: Reactions**! This Minecraft mod introduces a new elemental attribute system, reaction mechanics, dedicated enchantments, and visual effects on top of vanilla survival and combat.

**If you have good ideas or encounter problems, please submit an issue on github（github.com/Accidey/elementalcraft）, and I will review and resolve it as soon as possible.**

## 🌟 Core Elements

The mod includes 4 base elements + None:

| Element | ID | Color |
|---------|------------|-------|
| **Fire** | fire | Red |
| **Nature** | nature | Green |
| **Thunder** | thunder | Purple |
| **Frost** | frost | Blue |
| **None** | none | White |

Each element has a Strike enchantment (weapon), Enhancement enchantment (armor), and Resistance enchantment (armor).

## ⚔️ Enchantment System

The mod adds 12 dedicated enchantments (3 per element) for weapons and armor:

### Weapon Enchantments
- **Fire Aspect / Nature Aspect / Frost Aspect / Thunder Aspect**: Imbues the weapon with the corresponding element, dealing additional elemental damage to enemies.
  - Can be applied to all weapons that deal damage . (Previously limited to swords, axes, tridents, bows, and crossbows)
  - Mutually exclusive with Fire Aspect (vanilla), Flame, and Channeling.
  - Different elemental Aspect enchantments are also mutually exclusive.

### Armor Enchantments (Enhancement + Resistance)
- **Fire / Nature / Frost / Thunder Enhancement**: Increases the wearer's enhancement stat for the corresponding element (fixed percentage per level).
- **Fire / Nature / Frost / Thunder Resistance**: Redcribes incoming elemental damage of the corresponding type.
  - Can only be applied to armor (helmet, chestplate, leggings, boots).
  - Enhancement and Resistance of the same element can coexist on the same piece.
  - Different Enhancement enchantments are mutually exclusive; same for Resistance.
  - Max level is dynamically calculated from config (max stat cap / points per level).

All enchantments are obtainable through enchanting tables, villager trading, loot chests, and commands.

## 🔁 Element Restraint (Counter/Weakness)

Elements have a restraint relationship (configurable):
- **Default chain**: Fire → Nature → Thunder → Frost → Fire
  - Example: Fire attacking a Nature target → 1.5x elemental damage; Fire attacking a Frost target → 0.5x elemental damage.
  - The Jade info panel shows element relationships (Restrains / Weak / None) for entities.

## 🧪 Status Effects

The mod adds 6 new status effects:

| Effect | Description |
|--------|-------------|
| **Wetness** | Triggers elemental reactions when hit by elemental attacks. Slowly fades away from water; maintained or increased in rain/water. Increases hunger exhaustion. |
| **Flammable Spores** | Parasitic spores deal poison damage over time and corrode equipment durability. Fire and Static Shock detonate them; Frostbite suppresses them. |
| **Static Shock** | Takes random damage periodically. At high stacks, forms a Static Aura that damages nearby entities, detonates Spores and Creepers, and causes them to flee. Conducts electricity when wet; can also break Freeze. |
| **Paralysis** | Unable to act; mob AI is disabled. Continuously sinks and takes drowning damage in water. |
| **Frostbite**  | Takes ice damage every 5 seconds; movement speed and attack speed are reduced. At high stacks, forms a Frostbite Aura that applies frostbite to entities in range and forces them to flee. Contact with Wetness causes freezing! |
| **Freeze**  | Completely immobilized! Unable to move or attack; takes periodic ice damage; mob AI disabled. Physical melee/projectile attacks are blocked by the ice shell, but elemental attacks penetrate and deal attribute damage. Fire attacks (strong enough) instantly break Freeze and convert it to Wetness. |

Duration, damage, and other parameters are configurable.

## 💥 Elemental Reactions

### Fire Reactions

| Reaction | Trigger | Effect |
|---------------|-------------------|--------|
| **Self-Drying** | Self has Wetness + Fire attack | Reduces own Wetness stacks; fire damage reduced. If Fire power ≥ Low-Heat Steam threshold and not on cooldown, spawns a Low-Heat Steam Cloud. |
| **Scorched** | Fire attack (target not wet, not in Low-Heat Steam Cloud) | Applies high-duration fire damage over time (Scorched). Immune to vanilla fire. Contact with water triggers Thermal Shock. High enough Fire power applies a Scorched Aura that damages entities in range. |
| **Scorched Aura**  | Scorched entity has high enough Fire power | Periodically damages entities in range, applies temporary Scorched, clears Frostbite from nearby entities, detonates Creepers, forces nearby mobs to flee. |
| **Poison Boost**  | Scorched target has Poison effect | Consumes the Poison effect to boost Scorched: extends duration and increases damage multiplier. |
| **High-Heat Steam Cloud** | Fire attack + target is Wet | Spawns a High-Heat Steam Cloud. Periodically scalds entities inside (fire damage, scales with level). Clears Wetness and Frostbite from entities inside. Applies Blindness. Clears aggro. Fire-immune entities take reduced damage. |
| **Low-Heat Steam Cloud** | Generated from Self-Drying, Thermal Shock, or Freeze→Steam | Slowly applies Wetness (condensation) and Blindness to entities inside.  Can be electrified by Static entities (→ damage + Paralysis) or frosted by Frostbite entities (→ freeze entities inside). Electrified/Frosted clouds prevent further condensation. |

### Nature Reactions

| Reaction | Trigger | Effect |
|---------------|-------------------|--------|
| **Nature Parasite** | Nature attack (chance scales with Nature Enhancement; Wetness bonus, stacking bonus) | Applies Flammable Spores stacks. Immune if Nature Resistance ≥ threshold. Frozen targets are immune. |
| **Nature Counter** | Nature-aligned entity enters Scorched state (victim's Nature Enhancement ≥ threshold) | Knocks back nearby hostile enemies and applies Spores to them. Can optionally clear the victim's Scorched state. |
| **Spore Contagion** | Spore stacks reach threshold | Periodically spreads Spores to nearby entities. Wetness on targets converts Wetness stacks to additional Spore stacks. Only spreads to hostile mobs, not players or tamed animals. |
| **Toxic Blast** | Scorched target has Flammable Spores | **Low stacks (< threshold):** Weak blast — applies reduced Scorched without explosion. **High stacks (≥ threshold):** Full explosion — area damage with radius scaling by Spore stacks, applies enhanced Scorched to all nearby entities. Can chain-detonate nearby targets with Spores ≥ threshold. |

### Thunder Reactions

| Reaction | Trigger | Effect |
|---------------|-------------------|--------|
| **Static Attachment** | Thunder attack (chance scales with Thunder Enhancement; Wetness bonus, stacking bonus) | Applies Static Shock stacks. Higher chance on wet targets. Immune if Thunder Resistance ≥ threshold. |
| **Static Aura**  | Static stacks ≥ aura threshold | Periodically damages entities in range; paralyzes wet targets; detonates Spores; breaks Freeze; detonates Creepers; forces nearby mobs to flee. |
| **Water Electrification**  | Static entity enters/stands in water | Electrifies a zone of water, dealing settlement damage and Paralysis to all entities in the water. Clears the source entity's Static and Wetness. |
| **Static Spore Blast** | Entity with Static (or in Static Aura range) has Spores | Chance to detonate Spores based on Static and Spore stacks. |
| **Paralysis** | Static target gains Wetness (or Static Aura hits wet target) | Clears Static and Wetness, converts to Paralysis, deals remaining Static damage. |
| **Thunder Counter** | Nature attack hits a Thunder-aligned entity with Spores | Thunder entity counter-attacks the attacker with lightning, applying Paralysis (if wet) or Static (if not wet). |
| **Static Steam Cloud**  | Static entity enters Low-Heat Steam Cloud | Electrifies the steam cloud, dealing settlement damage and Paralysis to all entities inside. |

### Frost Reactions 

| Reaction | Trigger | Effect |
|---------------|-------------------|--------|
| **Frostbite** | Frost attack (chance scales with Frost Enhancement; Wetness bonus, stacking bonus) | Applies Frostbite stacks; periodic ice damage and reduced movement/attack speed. High enough stacks → Frostbite Aura. |
| **Frostbite Aura** | Frostbite stacks ≥ aura threshold | Applies temporary Frostbite to entities in range (slows and deals periodic ice damage). Freezes wet targets. If target is Scorched: clears Scorched and forces fleeing (no frostbite applied). |
| **Freeze** | Frostbite + target has Wetness | Completely immobilizes target (AI disabled, periodic ice damage). Physical attacks blocked by ice shell; elemental attacks penetrate and deal attribute damage. Fire attacks (strong enough) break Freeze and convert to Wetness. Generates a cold mist cloud on freeze. |
| **Fire Freeze Melt** | Fire attack + target is Frozen (Fire power ≥ threshold) | Instantly breaks Freeze; clears Scorched; converts to Wetness (stacks = freeze stacks). |
| **Freeze→Steam** | Frozen entity gains Scorched (via aura or Fire attack) | Scorched melts the freeze, generating a Low-Heat Steam Cloud. |
| **Frostbite→Wetness** | Scorched + target has Frostbite | Converts Frostbite stacks to Wetness stacks; clears both Scorched and Frostbite. |
| **Frosted Steam Cloud** | Frostbite Aura entity enters Low-Heat Steam Cloud | Steam cloud becomes frosted, freezing entities inside. |
| **Frostbite Suppresses Spores** | Entity has both Frostbite and Spores | Spores are suppressed during Frostbite; Spore duration decays faster. Spores resume when Frostbite ends. |
| **Scorched Aura Clears Frostbite** | Scorched Aura hits Frostbite/temporary Frostbite target | Clears Frostbite state, forces fleeing. |
| **Condensation** | Standing in Low-Heat Steam Cloud | Slowly accumulates Wetness stacks. Electrified/Frosted steam clouds prevent condensation. |

### Universal Reactions

| Reaction | Trigger | Effect |
|---------------|-------------------|--------|
| **Thermal Shock** | Scorched target contacts water | Instantly deals remaining Scorched damage and clears Scorched. Generates Low-Heat Steam Cloud. Vaporizes water at the contact point. |

All reaction parameters are configurable.

## 🌍 Biome Element Bias

Different biomes grant element tendencies to spawned mobs:
- 🔥 **Hot biomes** (Desert, Badlands, Nether, etc.) → Fire bias
- ❄️ **Cold biomes** (Snowy Plains, Ice Spikes, etc.) → Frost bias
- 🌲 **Forest/Jungle biomes** → Nature bias
- ⛈️ **Thunderstorm weather** (global) → Thunder bias

## 🧟 Mob Attribute System

Mobs can randomly gain elemental attributes at spawn based on biome bias and configuration:

### Attribute Assignment
- **Hostile mobs** (Monster) have a configurable chance to gain elemental attributes at spawn.
- **Neutral mobs** (NeutralMob, Piglins) have a separate, lower chance.
- Blacklisted mobs will not gain attributes.
- The assigned element is determined by **biome element bias** (e.g. Nether → Fire, End → Thunder).
- Each mob can gain: **Attack element** (chance-based), **Enhancement points** (distributed across 4 armor slots), **Resistance points** (distributed across 4 armor slots).
- The resistance type may be the attack element's **counter element** (configurable chance).

### Equipment & Durability
- Mobs with an attack element receive a **random weapon** (sword, axe, pickaxe, shovel, etc.) with the corresponding **elemental Aspect enchantment** and **Unbreaking III**.
- If the mob already has a held weapon, the enchantments are applied directly without replacement.
- Armor is automatically generated (if missing) with **Enhancement** and/or **Resistance enchantments** + **Unbreaking III**, with points distributed proportionally across the 4 armor slots.
- All equipment drop chances default to 0% (equipment does not drop).

### Enchanted Book Drops
- Element-aligned mobs have a chance to drop **enchanted books** on death (affected by Looting).
- Books may contain the mob's **Aspect enchantment**, **Enhancement enchantment** (random level based on enhancement points), or **Resistance enchantment** (random level based on resistance points).
- Book level weighting favors the mob's actual attribute quality.

### Dimension Defaults
- **Nether**: All mobs default to Fire attribute (configurable).
- **End**: All mobs default to Thunder attribute (configurable).

### Mob Flee Behavior
- Mobs near entities with **Scorched Aura**, **Frostbite Aura**, or **Static Aura** effects will attempt to **flee** (path away from the aura source; can be disabled in config).
- If stuck while fleeing, mobs will try to jump over 1-block-high obstacles.
- Fleeing lasts up to 200 ticks (10 seconds), stopping when the target point is reached or the aura source is far enough.

## 🛠️ Command System (Admin/OP)

All commands start with `/elementalcraft` and support Tab completion.

### Debug Mode
```
/elementalcraft debug  # Toggle debug mode, shows elemental damage calculation and reaction info
```

### Reload Config 
```
/elementalcraft reload  # Reload all config caches from disk
```

### Biome Bias Management
```
/elementalcraft biome add <element> <probability>
/elementalcraft biome remove <element>
/elementalcraft biome list
```

### Forced Entity Attributes
```
/elementalcraft entity add <attack element> [enhancement element] [enhancement points] [resistance element] [resistance points]
Example: /elementalcraft entity add fire fire 50 frost 20
Points support fixed values (e.g. 50) or ranges (e.g. 20-80)
All parameters after attack element are optional (default: none, 0, none, 0)
/elementalcraft entity remove              # Clears forced attributes for the held spawn egg
```

### Entity Attribute Blacklist
```
/elementalcraft entity blacklist add <element>     # Held spawn egg: prevent this entity from gaining the specified element (or "all" for all elements)
/elementalcraft entity blacklist remove <element>
/elementalcraft entity blacklist list
```

### Forced Item Attributes
- **Weapons**:
```
/elementalcraft item weapon add <element>       # Bind attack attribute (held weapon)
/elementalcraft item weapon remove           # Remove forced weapon attribute (held weapon)
```

- **Armor**:
```
/elementalcraft item armor add <enhancement element> [enhancement points] [resistance element] [resistance points]
# Bind enhancement + resistance attributes (held armor)
# Enhancement element is required; points, resistance element, and resistance points are optional (default: 0, none, 0)
/elementalcraft item armor remove            # Remove forced armor attributes (held armor)
```

### Effect Immunity Blacklist
```
# Command format (using Scorched as example):
/elementalcraft blacklist scorched add/remove/list
# All effects: scorched, spore, static, paralysis, frostbite, freeze, steam, wetness
```

### Config File Operations
- Changes made via commands are automatically saved and hot-loaded; no server restart needed.
- Directly edit TOML files in `config/ElementalCraft/`; the mod auto-detects and refreshes configuration every 100 ticks.

## ✨ Visual Effects (Graded by Enhancement Points)

When enhancement points reach the threshold (maxStatCap from config), visual effects trigger:

### Melee Swing Effects
- **Fire**: Flame arc (soul fire, lava, smoke particles at higher levels)
- **Nature**: Compost particles, spore blossoms (cherry blossoms, wax particles at higher levels)
- **Frost**: Ice rune particles, snowflake particles (frost overlay, ice layer, snow layer at higher levels)
- **Thunder**: Glow particle arc (reverse portal, lightning arc at higher levels)

### Ranged Projectile Effects
- **Fire**: Double helix flame trail (outer flame, inner soul fire, lava tail particles)
- **Nature**: Cherry blossom spiral trail (villager happy particles at tail)
- **Frost**: Snowflake spiral trail (frost overlay, ice rune particles at higher levels)
- **Thunder**: Lightning spark spiral trail (end candle, reverse portal, dragon breath at higher levels)

### Impact Explosion Effects
- Corresponding elemental particle burst (fire/lava, cherry/spore blossom, ice rune/snowflake, glow particles/end candles, etc.)
- Effect intensity scales linearly with level; max level effects are visually impactful. Configurable in `elementalcraft-visuals.toml`.

### Frost Exclusive Visuals 
- **Frostbite Overlay**: Screen overlay when entity has Frostbite
- **Frost Snow Layer**: Snow rendering on frozen entities
- **Freeze Ice Layer**: Ice shell visual effect on frozen entities

## ⚙️ Configuration Files (Server/Client)

Config files are located in `config/ElementalCraft/`:

| File | Purpose |
|------|---------|
| **elementalcraft-common.toml** | Element restraints, damage multipliers, biome bias, enchantment stats, forced entities/blacklists, dimension attributes, etc. |
| **elementalcraft-forced-items.toml** | Forced item attributes (can be added via commands). |
| **elementalcraft-fire-nature-reactions.toml** | Fire and Nature reaction parameters (Scorched, Spores, Steam, Toxic Blast, etc.). |
| **elementalcraft-thunder-frost-reactions.toml** | Thunder and Frost reaction parameters (Static, Paralysis, Frostbite, Freeze, Water Electrification, etc.). Frost parameters added in V1.7.0. |
| **elementalcraft-visuals.toml** | Particle effect toggles, density, angles, speed, etc. |

Hot-reload supported: configurations refresh automatically after save.

## 🔗 Iron's Spellbooks Integration

When **Iron's Spellbooks** is installed alongside ElementalCraft, additional integration features are enabled. All spell reaction chances and intensities are calculated based on the caster's corresponding element enhancement points (spell rarity → stacks, best spell level matched to enhancement points).

### Caster Mob Spawning
- Mobs with an attack element have a chance to become **ISS spell caster mobs** at spawn.
- Caster spawn chance is controlled by a unified config value `casterMobChance` (shared across all elements).
- Caster type is tied to the attack element (Thunder → Thunder caster, Nature → Nature caster, etc.).
- A caster blacklist can prevent specific entities from becoming casters.
- Nature casters with the Acid Orb spell additionally receive a random weapon (main hand), with the scroll in the off hand.

### Fire Spell Reactions
- Fire spells (Firebolt, Fireball, Burning Dash, Magma Bomb, Flaming Barrage, Flaming Strike, Scorch, Heat Surge, Blaze Storm, Fire Breath, Fire Arrow) have a chance to apply **Scorched** on hit, replacing ISS's built-in fire effect.
- **Blaze Storm** (each small fireball independently rolls for Scorched with a cooldown between triggers) and **Flaming Barrage** each roll independently.
- **Flaming Strike** and **Raise Hell** have dedicated handlers that trigger Scorched through the main spell reaction path.
- **Fire Field / Wall of Fire** continuously applies Scorched over time to entities in range; first checks for **Fire Freeze Melt** on frozen targets, then checks **Wetness** for High-Heat Steam Cloud, then applies Scorched.
- If the target has **Poison** or **Flammable Spores**, the Scorched trigger chance is boosted to **100%** and damage is amplified.
- A Nature-aligned target with both Spores and Scorched can trigger **Nature Counter** (knocks back enemies and clears Scorched).

### Nature Spell Reactions
- Nature spells (Acid Orb, Poison Arrow, Earthquake, Firefly Swarm, Poison Spray, Poison Splash, Root, Stomp) trigger **Nature Parasite** (apply Flammable Spores) on hit.
- **Poison-type spells** (Poison Arrow, Poison Spray, Poison Splash) do NOT apply Spores — they are explicitly excluded from spore application.
- **Acid Orb** applies Spores in an AoE (3.5 block radius) on impact, affecting all nearby entities.
- **Root** applies initial Spores on hit, then continuously applies additional Spores every second while the target remains Rooted.

### Thunder Spell Reactions
- Thunder spells (Lightning Lance, Chain Lightning, Ball Lightning, Electrocute, Lightning Bolt, Shockwave, Thunderstorm, Ascension, Volt Strike) apply **Static Shock** stacks based on the caster's Thunder Enhancement.
- If target has **Wetness**, triggers **Paralysis** (clears Wetness, converts to Paralysis).
- If target is **in water**, triggers **Water Electrification** — creates a persistent electrified water zone per dimension that periodically damages and paralyzes aquatic entities.
- If target has **Flammable Spores**, triggers **Spore Blast**.
- If target is in a **Low-Heat Steam Cloud**, electrifies the cloud, dealing damage and Paralysis to entities inside.
- **Electrocute** can refresh **Paralysis** duration on paralyzed targets. After electrocuting ends, the caster enters a Paralysis cooldown.
- Vanilla **Lightning Bolt** hitting a wet target: if Water Electrification is available, triggers it; otherwise applies max Static Shock stacks and immediately resolves Wetness conflict.

### Frost Spell Reactions
- Frost spells (Cone of Cold, Icicle, Ray of Frost, Frostwave, Ice Spikes, Snowball, Frostbite, Blizzard, Ice Tomb, Summon Polar Bear) have a chance to apply **Frostbite** based on the caster's Frost Enhancement.
- **Blizzard** AoE continuously applies Frostbite every second to all entities inside the blizzard area.
- **Ice Tomb** is specially handled for caster mobs — directly traps the target with a 5-second duration.
- If the target has **Wetness**, triggers **Freeze** (Frostbite + Wetness → Freeze).
- Frost spells hitting an already-frozen target **refresh** the Freeze duration, keeping the target immobilized longer.
- ISS's **Chilled** effect combined with Wetness converts to **Freeze** automatically.
- **Polar Bear** casters' summoned bears are automatically removed on caster death.

### Poison Cloud + Fire Reaction
- When a **Scorched target** enters an ISS **Poison Cloud**, the cloud enhances the Scorched duration and damage multiplier (poison boost), then dissipates with a poof effect.
- Scorched Aura entities near a Poison Cloud can also trigger this reaction.

### ROOT Spell Integration
- **ROOT** spell applies **Flammable Spores** stacks on hit based on the caster's Nature Enhancement.
- While ROOTed, additional Spores are applied every second based on the caster's Nature Enhancement.
- If a ROOTed target is hit by **Scorched** (Fire attack triggers), ROOT is removed, and the Scorched duration and damage multiplier are enhanced (same mechanic as the Poison boost).
- ROOT immobilization suppresses mob flee behavior — affected mobs stop fleeing attempts.

### Caster Mob AI

All 4 element caster mobs share the following behaviors:
- Automatically equipped with the corresponding element's **spell scroll** (main/off hand) and **elemental Aspect enchantment** at spawn.
- Spell level is selected based on the mob's enhancement points via a rarity-matching algorithm to pick the most appropriate level.
- Enters **aggressive mode** (more frequent casting) below **50%** health.
- Scroll drop chance is configurable (default: 100%).
- **Drop enchantment cleansing**: Items with elemental Aspect enchantments dropped by ISS caster mobs are automatically de-enchanted (prevents infinite farming of elemental enchantments from mob kills).

**Fire caster mobs**: Equipped with splash poison potions. Throws poison at the target before casting fire spells — if target is wet, triggers High-Heat Steam Cloud first (consumes Wetness); once Wetness is cleared, Poison guarantees 100% Scorched trigger chance and enhances Scorched duration.

**Nature caster mobs**:
- With Acid Orb: Special AI loop (cast → wait for hit → check **REND** effect → if applied, melee while REND lasts; if missed, retry; 2 consecutive misses triggers cooldown).
- Other spells: Standard casting, checks target paralysis immunity.

**Thunder caster mobs**:
- Throws **splash water bottles** to apply Wetness before casting to trigger Paralysis.
- Tracks target's Wetness status; only throws bottle if target is not already wet.
- If `wetnessNetherDimensionImmune` is enabled, does not equip water bottles in the Nether (Wetness is disabled).

**Frost caster mobs**:
- Throws **splash water bottles** to apply Wetness before casting to trigger Freeze.
- Can cast **Summon Polar Bear** — after casting, periodically checks if the bear is alive; if dead, re-summons (up to 3 total summons, 48-block search range).
- Caster's polar bears are automatically removed when the caster dies.
- If `wetnessNetherDimensionImmune` is enabled, does not equip water bottles in the Nether (Wetness is disabled).

### Other Integration Details
- **Non-aggressive spell exclusion**: Heat Surge, Acid Orb, Oakskin, Fire Breath, Cone of Cold, and Electrocute do not trigger aggressive casting.
- **Scroll Rarity Matching**: Mobs calculate the best spell level from their enhancement points by matching closest rarity value.
- **Player Spell Tracking**: When a player casts a spell, the last spell ID, level, and cast source are tracked on the player's NBT data, used for reaction calculations on subsequent hits.
- **Thunder Spell Enchantment Handling**: Thunder caster mob spells temporarily save and clear enchantments on the target's items during damage calculation, then restore them after, ensuring damage tracking is accurate.

<br>

# 属性锻造：元素反应 模组介绍

欢迎来到《属性锻造：元素反应》！这是一款围绕元素战斗打造的Minecraft模组，为原版生存与战斗玩法新增了全新的属性系统、元素反应机制、专属附魔以及炫酷的视觉特效。

**如果你有好的想法或遇到问题,请在GitHub上提交issue（github.com/Accidey/elementalcraft）

## 🌟 核心元素

游戏内包含4种基础元素属性 + 无属性：

| 元素 | 标识符 | 颜色 |
|---------|------------|-------|
| **赤焰** | fire | 红色 |
| **自然** | nature | 绿色 |
| **雷霆** | thunder | 紫色 |
| **冰霜** | frost | 蓝色 |
| **无属性** | none | 白色 |

每种元素均配有攻击附魔、强化附魔和抗性附魔。

## ⚔️ 附魔系统

模组为武器和防具新增了12种专属附魔（每种元素对应3种）：

### 武器附魔（攻击属性）
- **赤焰属性 / 自然属性 / 冰霜属性 / 雷霆属性**：为武器赋予对应元素的攻击效果，对敌人造成额外的元素伤害。
  - 可附魔在所有有伤害的武器上 。（此前仅限剑、斧、三叉戟、弓和弩）
  - 与火焰附加、火矢、引雷附魔互斥。
  - 不同元素的攻击附魔之间也相互排斥。

### 防具附魔（强化 + 抗性）
- **赤焰强化 / 自然强化 / 冰霜强化 / 雷霆强化**：提升对应元素攻击的伤害加成（每级为固定百分比）。
- **赤焰抗性 / 自然抗性 / 冰霜抗性 / 雷霆抗性**：降低受到的对应元素攻击伤害。
  - 仅可附魔在防具（头盔、胸甲、护腿、靴子）上。
  - 同一种元素的强化附魔和抗性附魔可以共存。
  - 不同元素的强化附魔之间相互排斥，抗性附魔同理。
  - 附魔最高等级由配置文件动态计算（最大属性上限 / 每级所需点数）。

所有附魔均可通过附魔台、村民交易、战利品宝箱等常规方式获取，也可通过指令强制绑定。

## 🔁 元素克制关系

元素之间存在克制关系（可在配置文件中自定义）：
- **默认克制链**：🔥 赤焰 → 🌿 自然 → ⚡ 雷霆 → ❄️ 冰霜 → 🔥 赤焰
  - 示例：赤焰攻击自然属性目标 → 1.5倍属性伤害；赤焰攻击冰霜属性目标 → 0.5倍属性伤害。
  - 可通过Jade信息面板查看实体的元素关系（克制/被克制/无关系）。

## 🧪 状态效果

模组新增了6种全新的状态效果：

| 效果 | 描述 |
|--------|-------------|
| **潮湿** | 受到元素攻击时会触发元素反应。远离水源时缓慢消退，在雨中或水中会维持/提升层数。会增加饱食度消耗。 |
| **易燃孢子** | 被孢子寄生，持续受到毒伤，装备耐久会被腐蚀。赤焰和静电会引爆它们，霜冻期间会抑制它们。 |
| **静电** | 每隔一段时间受到随机伤害。层数足够时会形成静电光环伤害光环范围内实体，可引爆孢子和苦力怕，并迫使它们逃跑。潮湿时会导电，但也能破除冰冻。 |
| **麻痹** | 无法做出任何动作，生物AI被禁用。在水中会持续下沉并受到溺水伤害。 |
| **霜冻**  | 被冰霜侵蚀，每5秒受到一次冰冻伤害，移动速度和攻击速度降低。层数足够时形成霜冻光环，对光环范围内的实体施加霜冻并迫使它们逃跑。与潮湿效果接触时会被冻结！ |
| **冻结**  | 完全被冻住！无法移动和攻击，周期性受到冰冻伤害，生物AI被禁用。物理近战/投射物攻击被冰壳格挡，但元素攻击可以穿透并造成属性伤害。赤焰属性攻击（强度足够时）会立即解除冻结并转化为潮湿。 |

持续时间、伤害等参数均可在配置文件中调整。

## 💥 元素反应

### 🔥 赤焰相关反应

| 反应名称 | 触发条件 | 效果 |
|---------------|-------------------|--------|
| **自我干燥** | 自身处于潮湿状态 + 赤焰攻击 | 降低自身潮湿层数，本次攻击的赤焰伤害降低。若赤焰强度 ≥ 低温蒸汽阈值且不在冷却中，则生成低温蒸汽云。 |
| **灼烧** | 赤焰攻击（目标无潮湿，不在低温蒸汽云中） | 造成高强度持续火焰伤害（灼烧），免疫普通火焰伤害，接触水时触发"热冲击"。赤焰强度足够时施加灼烧光环，伤害光环范围内实体。 |
| **灼烧光环**  | 灼烧实体赤焰强度足够高 | 周期性伤害光环范围内实体，施加临时灼烧，清除附近实体的霜冻效果，引爆苦力怕，迫使附近生物逃跑。 |
| **中毒增幅**  | 灼烧目标有效果 | 中毒效果被消耗以增幅灼烧：延长持续时间并提高伤害倍率。 |
| **高温蒸汽云** | 赤焰攻击 + 目标处于潮湿状态 | 生成高温蒸汽云。周期性烫伤云内实体（火焰伤害，随等级提升）。清除云内实体的潮湿和霜冻。施加致盲。清除生物仇恨。火焰免疫实体受到减伤。 |
| **低温蒸汽云** | 由自我干燥、热冲击或冻结→蒸汽生成 | 缓慢对云内实体施加潮湿（冷凝）和致盲效果。可被静电实体感电化（→造成伤害和麻痹）或被霜冻实体霜寒化（→冻结云内实体）。感电/霜寒化的云阻止进一步冷凝。 |

### 🌿 自然相关反应

| 反应名称 | 触发条件 | 效果 |
|---------------|-------------------|--------|
| **自然寄生** | 自然属性攻击（概率随自然强化点数提升，潮湿加成，叠加加成） | 附加易燃孢子层数。自然抗性 ≥ 阈值则免疫。冻结目标免疫。 |
| **自然反制** | 自然属性实体进入灼烧状态（受害者的自然强化 ≥ 阈值） | 击退附近敌对敌人并对其施加孢子。根据配置可清除受害者的灼烧状态。 |
| **孢子传播** | 孢子层数达到阈值 | 定期向附近实体传播。潮湿目标的潮湿层数会转化为额外孢子层数。仅传播至敌对生物，不会传播给玩家或已驯服动物。 |
| **毒火爆燃** | 灼烧目标带有易燃孢子 | **低层数（< 阈值）：** 弱效爆燃——仅施加伤害降低的灼烧，无爆炸。**高层数（≥ 阈值）：** 完整爆炸——造成范围爆炸伤害，范围随孢子层数增长，对附近所有实体施加增强灼烧。可连锁引爆附近孢子 ≥ 阈值的目标。 |

### ⚡ 雷霆相关反应

| 反应名称 | 触发条件 | 效果 |
|---------------|-------------------|--------|
| **静电附着** | 雷霆属性攻击（概率随雷霆强化点数提升，潮湿加成，叠加加成） | 附加静电层数，对潮湿目标概率更高。雷霆抗性 ≥ 阈值则免疫。 |
| **静电光环** | 静电层数 ≥ 光环阈值 | 周期性伤害光环范围内实体，可麻痹潮湿目标、引爆孢子、破除冰冻、引爆苦力怕，迫使附近生物逃跑。 |
| **感电水域** | 静电实体进入/站在水中 | 使范围内的水域感电，对水中所有实体造成结算伤害和麻痹效果。清除源实体的静电和潮湿。 |
| **静电孢子引爆** | 带有静电效果或处于静电光环范围内的实体拥有孢子 | 基于静电和孢子层数的概率引爆孢子。 |
| **麻痹** | 静电目标获得潮湿状态（或静电光环 + 潮湿目标） | 清除静电和潮湿状态，转化为麻痹效果，结算剩余静电伤害。 |
| **雷霆反制** | 自然攻击带有孢子的雷霆属性实体 | 雷霆实体用闪电反击攻击者，施加麻痹（若潮湿）或静电（若不潮湿）。 |
| **感电蒸汽云** | 静电实体进入低温蒸汽云 | 使蒸汽云感电，对云中所有实体造成结算伤害和麻痹效果。 |

### ❄️ 冰霜相关反应 

| 反应名称 | 触发条件 | 效果 |
|---------------|-------------------|--------|
| **霜冻** | 冰霜攻击（概率随冰霜强化点数提升，潮湿加成，叠加加成） | 施加霜冻层数，周期性造成冰冻伤害并减速移动/攻击速度。层数足够时 → 霜冻光环。 |
| **霜冻光环** | 霜冻层数 ≥ 光环阈值 | 对光环范围内的实体施加临时霜冻（减速并造成周期冰伤）。可冻结潮湿目标。若目标处于灼烧状态：清除灼烧并迫使其逃跑（不施加霜冻）。 |
| **冻结** | 霜冻 + 目标有潮湿效果 | 完全禁锢目标（生物AI禁用，周期性冰伤），物理攻击被冰壳格挡，元素攻击可穿透并造成属性伤害。赤焰属性攻击（强度足够时）可解除冻结并转化为潮湿。冻结时生成寒冷云雾。 |
| **赤焰融冰** | 赤焰攻击 + 目标被冻结（赤焰强度 ≥ 阈值） | 立即解除冻结，清除灼烧，转化为潮湿状态（层数 = 冻结层数）。 |
| **冻结→蒸汽** | 被冻结的实体获得灼烧状态（通过光环或赤焰攻击） | 灼烧效果融化冻结，生成低温蒸汽云。 |
| **霜冻→潮湿** | 灼烧 + 目标带有霜冻 | 霜冻层数转化为潮湿层数，同时清除灼烧和霜冻。 |
| **霜寒蒸汽云** | 霜冻光环实体进入低温蒸汽云 | 蒸汽云变为霜寒状态，冻结云中的实体。 |
| **霜冻抑制孢子** | 实体同时拥有霜冻和孢子效果 | 霜冻期间孢子效果失效，并加速孢子持续时间衰减。霜冻消失后，孢子恢复生效。 |
| **灼烧光环清除霜冻** | 灼烧光环范围内有霜冻/临时霜冻目标 | 清除目标的霜冻状态，迫使逃跑。 |
| **冷凝** | 处于低温蒸汽云中 | 缓慢叠加潮湿层数。感电/霜寒的蒸汽云阻止冷凝。 |

### 通用反应

| 反应名称 | 触发条件 | 效果 |
|---------------|-------------------|--------|
| **热冲击** | 处于灼烧状态的目标接触水 | 瞬间结算剩余灼烧伤害并清除灼烧状态。生成低温蒸汽云。蒸发接触点的水。 |

所有反应参数均可在配置文件中调整。

## 🌍 群系元素偏向

不同群系生成的生物会带有特定的元素倾向：
- 🔥 **炎热群系**（沙漠、恶地、下界等）→ 赤焰偏向
- ❄️ **寒冷群系**（雪原、冰刺平原等）→ 冰霜偏向
- 🌲 **森林/丛林群系** → 自然偏向
- ⛈️ **雷暴天气**（全局）→ 雷霆偏向

## 🧟 生物属性系统

生物在生成时可以根据群系偏向和配置随机获得元素属性：

### 属性分配 
- **敌对生物**（Monster）有可配置的概率在生成时获得元素属性。
- **中立生物**（NeutralMob、猪灵）有独立的、较低的概率。
- 黑名单中的生物不会获得属性。
- 分配的元素由**群系元素偏向**决定（如下界→赤焰，末地→雷霆）。
- 每个生物可获得：**攻击元素**（概率触发）、**强化点数**（分配到4个防具槽位）、**抗性点数**（分配到4个防具槽位）。
- 抗性类型可能是攻击元素的**克制元素**（可配置概率）。

### 装备与耐久 
- 拥有攻击属性的生物会获得一把**随机武器**（剑、斧、镐、锹等），附带对应的**元素攻击附魔**和**耐久III**。
- 如果生物已有手持武器，则直接在其武器上附加元素攻击附魔和耐久III，不替换。
- 防具会自动生成（若缺失），附带**强化**和/或**抗性附魔** + **耐久III**，强化/抗性点数按比例分配到4个防具槽位。
- 所有装备掉落概率默认设为0%（不掉落装备本身）。

### 附魔书掉落 
- 拥有元素属性的生物死亡时，有概率掉落**附魔书**（受抢夺等级影响）。
- 附魔书可包含：生物的**攻击附魔**、**强化附魔**（随机等级，基于强化点数）、或**抗性附魔**（随机等级，基于抗性点数）。
- 附魔书等级加权偏向生物的实际属性质量。

### 维度默认值
- **下界**：所有生物默认为赤焰属性（可配置）。
- **末地**：所有生物默认为雷霆属性（可配置）。

### 生物逃跑行为 
- 靠近拥有**蒸汽云*、*灼烧光环**、**霜冻光环**或**静电光环**效果的实体时，生物会尝试**逃跑**（远离光环来源寻路，可通过配置选项关闭）。
- 逃跑过程中卡住时，尝试跳跃越过1格高的障碍物继续逃跑。
- 逃跑持续最多200 tick（10秒），到达目标点或远离光环源后停止。

## 🛠️ 指令系统（管理员/OP专用）

所有指令均以 `/elementalcraft` 开头，支持Tab补全。

### 调试模式
```
/elementalcraft debug  # 切换调试模式，显示元素伤害计算过程和反应相关信息
```

### 重载配置 
```
/elementalcraft reload  # 从磁盘重新加载所有配置缓存
```

### 群系偏向管理
```
/elementalcraft biome add <元素> <概率>
/elementalcraft biome remove <元素>
/elementalcraft biome list
```

### 强制实体属性
```
/elementalcraft entity add <攻击元素> [强化元素] [强化点数] [抗性元素] [抗性点数]
示例：/elementalcraft entity add fire fire 50 frost 20
点数支持固定值（如50）或范围值（如20-80）
攻击元素之后的所有参数均为可选（默认：无、0、无、0）
/elementalcraft entity remove              # 清除手持刷怪蛋对应的实体强制属性
```

### 实体属性黑名单
```
/elementalcraft entity blacklist add <元素>      # 手持刷怪蛋，禁止该实体携带指定元素（或填写"all"表示所有元素）
/elementalcraft entity blacklist remove <元素>
/elementalcraft entity blacklist list
```

### 强制物品属性
- **武器**：
```
/elementalcraft item weapon add <元素>       # 绑定攻击属性（手持武器）
/elementalcraft item weapon remove           # 移除强制武器属性（手持武器）
```

- **防具**：
```
/elementalcraft item armor add <强化元素> [强化点数] [抗性元素] [抗性点数]
# 绑定强化+抗性属性（手持防具）
# 强化元素为必填；点数、抗性元素、抗性点数为可选（默认：0、无、0）
/elementalcraft item armor remove            # 移除强制防具属性（手持防具）
```

### 效果免疫黑名单
```
# 指令格式（以灼烧为例）：
/elementalcraft blacklist scorched add/remove/list
# 全部效果：scorched（灼烧）、spore（孢子）、static（静电）、paralysis（麻痹）、frostbite（霜冻）、freeze（冻结）、steam（蒸汽）、wetness（潮湿）
```

### 配置文件操作
- 通过指令做出的修改会自动保存并热加载，无需重启服务器。
- 直接编辑 `config/ElementalCraft/` 目录下的TOML文件，模组每100刻会自动检测并刷新配置。

## ✨ 视觉特效（按强化点数分级）

当装备的强化点数达到阈值时，会触发对应的视觉特效（阈值 = 配置文件中的maxStatCap值）：

### 近战挥砍特效
- **🔥 赤焰**：火焰弧光（高等级追加灵魂火、熔岩、烟雾效果）
- **🌿 自然**：堆肥粒子、孢子花（高等级追加樱花、蜡质粒子效果）
- **❄️ 冰霜**：冰符文粒子、雪花粒子（高等级追加霜冻覆盖层、冻结冰层、积雪层效果）
- **⚡ 雷霆**：发光粒子弧光（高等级追加反向传送门、闪电弧光效果）

### 远程投射物特效
- **🔥 赤焰**：双螺旋火焰轨迹（外层火焰、内层灵魂火，尾部附带熔岩粒子）
- **🌿 自然**：樱花螺旋轨迹（尾部附带村民喜悦粒子）
- **❄️ 冰霜**：雪花粒子螺旋轨迹（高等级追加霜冻覆盖层、冰符文粒子）
- **⚡ 雷霆**：闪电火花螺旋轨迹（高等级追加末地蜡烛、反向传送门、龙息效果）

### 撞击爆炸特效
- 对应元素粒子爆发（火焰/熔岩、樱花/孢子花、冰符文/雪花、发光粒子/末地蜡烛等）
- 特效强度随等级线性提升，最高等级效果极具视觉冲击力。可在 `elementalcraft-visuals.toml` 中调整。

### 冰霜专属视觉效果 
- **霜冻覆盖层**：实体带有霜冻效果时的屏幕覆盖层
- **霜冻积雪层**：冻结实体上的积雪渲染
- **冻结冰层**：冻结实体的冰壳视觉效果

## ⚙️ 配置文件（服务端/客户端）

配置文件位于 `config/ElementalCraft/` 目录下：

| 文件 | 用途 |
|------|---------|
| **elementalcraft-common.toml** | 元素克制关系、伤害倍率、群系偏向、附魔加成、强制实体/黑名单、维度属性等。 |
| **elementalcraft-forced-items.toml** | 强制物品属性配置（可通过指令添加）。 |
| **elementalcraft-fire-nature-reactions.toml** | 赤焰与自然元素反应参数（灼烧、孢子、蒸汽、毒火爆燃等）。 |
| **elementalcraft-thunder-frost-reactions.toml** | 雷霆与冰霜元素反应参数（静电、麻痹、霜冻、冻结、感电水域等）。冰霜参数于V1.7.0版本添加。 |
| **elementalcraft-visuals.toml** | 粒子特效开关、密度、角度、速度等。 |

支持热加载：保存修改后配置会自动刷新。

## 🔗 Iron's Spellbooks 联动

当 **Iron's Spellbooks**（铁的法术书）与 ElementalCraft 同时安装时，将启用额外的联动功能。所有法术反应的触发概率和强度基于施法者的对应元素强化点数计算（稀有度等级 → stacks，与强化点数匹配最佳法术等级）。

### 施法者生物生成
- 拥有攻击元素的生物在生成时，有概率成为**ISS法术施法者**。
- 施法者生成概率由统一配置项 `casterMobChance` 控制（各元素共用此值）。
- 施法者类型与攻击元素绑定（雷霆→雷霆施法者，自然→自然施法者，等）。
- 施法者黑名单可阻止特定实体成为施法者。
- 自然施法者装备酸液球法术时，额外获得一把随机武器（主手），卷轴放在副手。

### 赤焰法术反应
- 赤焰法术（火矢、火球术、烈焰冲刺、岩浆炸弹、烈焰弹幕、烈焰打击、灼烧、热浪、烈焰风暴、龙息术、火焰箭矢）命中时，根据施法者的**赤焰强化点数**概率触发**灼烧**效果，替换ISS自带的着火效果。
- **烈焰风暴**（Blaze Storm）的每一发小型火球均可独立触发灼烧判定，但触发之间有冷却间隔；**炽焰追踪弹幕**（Flaming Barrage）同样可独立触发。
- **烈焰打击**（Flaming Strike）和**地狱浮现**（Raise Hell）有专属处理器，通过法术反应路径触发灼烧。
- **火墙/岩浆场**（Fire Field / Wall of Fire）持续对范围内实体施加灼烧；先检查**赤焰融冰**（若目标被冻结），再检查**潮湿**（触发高温蒸汽云），最后施加灼烧。
- 若目标同时有**中毒**或**易燃孢子**，灼烧触发概率提升至**100%**，并增幅灼烧伤害。
- 目标为自然属性且同时有孢子和灼烧时，可触发**自然反制**（击退敌人并清除灼烧）。

### 自然法术反应
- 自然法术（酸液球、毒箭、地震、萤火虫群、毒雾喷射、毒液溅射、根须缠绕、践踏等）命中时触发**自然寄生**（施加易燃孢子）。
- **毒属性法术**（毒箭、毒雾喷射、毒液溅射）**不施加孢子**——它们被明确排除在孢子施加之外。
- **酸液球**（Acid Orb）命中时触发AoE孢子施加（半径3.5格内所有实体）。
- **根须缠绕**（Root）命中时施加初始孢子层数，然后在缠绕持续期间每隔1秒根据施法者自然强化点数追加孢子层数。

### 雷霆法术反应
- 闪电法术（闪电长矛、连锁闪电、球状闪电、电击、闪电束、冲击波、雷暴、升天、伏特打击）根据施法者的雷霆强化点数对目标施加**静电**层数。
- 若目标有**潮湿**效果，闪电法术触发**麻痹**（清除潮湿，转化为麻痹）。
- 若目标**在水中**，触发**感电水域**——按维度创建持久的感电区域，周期性伤害和麻痹水中的实体。
- 若目标有**易燃孢子**，触发**孢子引爆**。
- 若目标在**低温蒸汽云**中，使蒸汽云**感电**，对云中实体施加麻痹伤害。
- **电击**（Electrocute）法术可**刷新麻痹**目标的麻痹持续时间。电击结束后，施法者进入麻痹冷却。
- 原版**闪电束**击中潮湿目标时：若感电水域可用则触发感电；否则施加最大静电层数并立即解决潮湿冲突。

### 冰霜法术反应
- 冰霜法术（寒冰锥、冰锥术、霜冻射线、霜浪、冰刺、雪球、霜咬、暴风雪、冰墓、召唤北极熊）根据施法者的冰霜强化点数概率触发**霜冻**效果。
- **暴风雪**（Blizzard）AoE**每秒**对范围内所有实体施加霜冻判定。
- **冰墓**（Ice Tomb）被生物施法者特殊处理——直接封锁目标5秒。
- 若目标已有**潮湿**效果，冰霜法术触发**冻结**（霜冻+潮湿→冻结）。
- 冰霜法术命中已冻结的目标会**刷新冻结持续时间**，延长目标被禁锢的时间。
- ISS的**Chilled**效果与潮湿叠加时自动转化为**冻结**。
- **北极熊**施法者死亡时，其召唤的北极熊自动移除。

### 毒雾云 + 赤焰反应
- **灼烧目标**进入ISS的**毒雾云**范围时，毒雾云会增强灼烧的持续时间和伤害倍率（中毒增幅），然后消散（伴随 buff 粒子效果）。
- 灼烧光环实体靠近毒雾云时也可触发此反应。

### ROOT法术联动
- **ROOT**法术命中目标时，根据施法者的自然强化点数施加**易燃孢子**层数。
- ROOT持续期间，每隔1秒根据施法者自然强化点数追加孢子层数。
- 若被ROOT的目标受到**灼烧**（赤焰攻击触发），ROOT被移除，同时灼烧持续时间和伤害倍率增强（与中毒增幅机制相同）。
- ROOT禁锢期间抑制目标逃跑行为——受影响的生物停止逃跑尝试。

### 生物施法AI

所有4种元素的施法生物共享以下行为：
- 出生时自动装备对应元素的**法术卷轴**（主手/副手）和**元素攻击附魔**。
- 法术等级基于生物的元素强化点数，通过稀有度匹配算法选取最合适的等级。
- 生命值低于 **50%** 时进入**激进模式**（施法更频繁）。
- 法术卷轴掉落概率可配置（默认：100%）。
- **掉落附魔净化**：ISS施法者掉落的物品若带有元素攻击附魔，会被自动清除（防止通过击杀生物无限获取元素附魔书）。

**赤焰施法生物**：配备喷溅剧毒水瓶，向目标投毒后施放赤焰法术——目标潮湿时优先触发高温蒸汽云（消耗潮湿），潮湿清除后中毒确保灼烧100%触发并增强持续时间。

**自然施法生物**：
- 装备酸液球时：特殊AI循环（施法→等待命中判定→检测**REND**效果→命中则近战等待REND消失，未命中则重试，连续2次未命中进入冷却）。
- 其他法术：常规施法，检测目标麻痹免疫状态。

**雷霆施法生物**：
- 向目标投掷**喷溅水瓶**施加潮湿后再施法以触发麻痹。
- 追踪目标潮湿状态，仅当目标未潮湿时才投掷水瓶。
- 若 `wetnessNetherDimensionImmune` 开启且在下界维度，不给喷溅水瓶（潮湿效果已禁用）。

**冰霜施法生物**：
- 向目标投掷**喷溅水瓶**施加潮湿后再施法以触发冻结。
- 可施放**召唤北极熊**——施法后每隔1秒检测北极熊是否存活（48格搜索范围），若死亡则重新召唤（最多3次）。
- 施法者死亡时自动移除其召唤的北极熊。
- 若 `wetnessNetherDimensionImmune` 开启且在下界维度，不给喷溅水瓶（潮湿效果已禁用）。

### 其他联动细节
- **非攻击法术排除**：热浪、酸液球、橡木皮肤、龙息术、寒冰锥、电击等非攻击或持续施法法术不会触发激进施法。
- **卷轴稀有度匹配**：生物根据自身强化点数计算法术最优等级（通过稀有度值匹配最接近的等级）。
- **玩家法术追踪**：玩家施法时，最后一次施法的法术ID、等级和施法来源会被记录在NBT数据中，用于后续命中的反应计算。
- **雷霆法术附魔处理**：雷霆施法生物在伤害计算期间临时保存并清除目标物品上的附魔，计算完毕后恢复，确保伤害追踪准确。

