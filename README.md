# ElementalCraft: Reactions Mod Introduction

Welcome to **ElementalCraft**! This is a Minecraft mod centered around elemental combat, adding a new attribute system, elemental reactions, special enchantments, and stunning visual effects to vanilla survival and combat.

**The following is for version V1.6.0. Frost attribute elemental reactions will be updated in V1.7.0.**

## 🌟 Core Elements

The game features 4 elemental types + None:

| Element | Identifier | Color |
|---------|------------|-------|
| **Fire** | fire | Red |
| **Nature** | nature | Green |
| **Frost** | frost | Blue |
| **Thunder** | thunder | Purple |
| **None** | none | White |

Each element has attack enchantments, enhancement enchantments, and resistance enchantments.

## ⚔️ Enchantment System

The mod adds 12 exclusive enchantments to weapons and armor (3 per element):

### Weapon Enchantments (Attack Attributes)
- **Fire Attribute / Nature Attribute / Frost Attribute / Thunder Attribute**: Grants the weapon corresponding elemental attack, dealing additional elemental damage.
  - Can only be enchanted on swords, axes, tridents, bows, and crossbows.
  - Mutually exclusive with Flame, Fire Aspect, and Channeling.
  - Different elemental attack enchantments are mutually exclusive.

### Armor Enchantments (Enhancement + Resistance)
- **Fire Enhancement / Nature Enhancement / Frost Enhancement / Thunder Enhancement**: Increases damage bonus for corresponding elemental attacks (fixed percentage per level).
- **Fire Resistance / Nature Resistance / Frost Resistance / Thunder Resistance**: Reduces damage taken from corresponding elemental attacks.
  - Can only be enchanted on armor (helmet, chestplate, leggings, boots).
  - Enhancement and resistance for the same element can coexist.
  - Different elemental enhancement enchantments are mutually exclusive, same with resistance enchantments.
  - Maximum enchantment level is dynamically calculated from configuration file (maxStatCap / points per level).

All enchantments can be obtained normally through enchanting tables, villager trades, loot chests, etc., or forcibly bound with commands.

## 🔁 Elemental Weaknesses

Elements have weakness relationships (customizable in configuration):
- **Default Weakness Chain**: 🔥 Fire → 🌿 Nature → ⚡ Thunder → ❄️ Frost → 🔥 Fire
  - Example: Fire attack against Nature → 1.5× damage; Fire attack against Frost → 0.5× damage.
  - Elemental relationships (weak/strong/none) can be viewed on entities using Jade info panel.

## 🧪 Status Effects

The mod introduces 4 new status effects:

| Effect | Description |
|--------|-------------|
| **Wetness** | Triggers elemental reactions when hit by elemental attacks. Slowly decreases when away from water, maintained/increased in rain or water. |
| **Flammable Spores** | Parasitized by spores, movement slowed and afraid of fire, but physical resistance increased. Higher levels = stronger effects. |
| **Static** | Takes random damage periodically and conducts to nearby entities. |
| **Paralysis** | Unable to move (attack, use items, switch gear). |

Parameters like duration and damage can be adjusted in configuration files.

## 💥 Elemental Reactions

Elemental interactions require the target to have Wetness status:

### 🔥 Fire-Related Reactions

| Reaction Name | Trigger Condition | Effect |
|---------------|-------------------|--------|
| **Self-Drying** | Wetness + Fire attack (self) | Reduces own Wetness levels, temporarily lowers fire damage. |
| **Scorching** | Fire attack (target not Wet or Frost) | Strong sustained fire damage (scorching), immune to normal fire, triggers "Heat Shock" when contacting water. |
| **Toxic Blast** | Fire attack + target has Flammable Spores | Explodes spores, dealing area explosion damage and applying Scorching. Higher spore levels = larger damage and area. |
| **High-Temperature Steam** | Fire attack + target Wet | Generates steam cloud, continuously scalding nearby entities and clearing Wetness. |

### 🌿 Nature-Related Reactions

| Reaction Name | Trigger Condition | Effect |
|---------------|-------------------|--------|
| **Nature Parasitism** | Nature attack (chance) | Applies Flammable Spores layers, chance increases with Nature Enhancement points. |
| **Wildfire Jet** | Nature entity under Scorching | Knocks back nearby enemies and applies spores. |
| **Spore Transmission** | Spore count reaches threshold | Transmits to nearby entities periodically, Wet targets convert additional spores. |

### ⚡ Thunder-Related Reactions

| Reaction Name | Trigger Condition | Effect |
|---------------|-------------------|--------|
| **Static Application** | Thunder attack (chance) | Applies Static layers, chance increases with Thunder Enhancement points, higher chance against Wet targets. |
| **Static Conduction** | Static target takes periodic damage | Deals splash damage to nearby entities (damage percentage configurable). |
| **Paralysis** | Static target encounters Wetness | Clears Static and Wetness, converts to Paralysis, dealing instant damage based on Static damage. |
| **Paralysis Transmission** | Paralysis reaches threshold | Automatically transmits to nearby entities. |

### ❄️ Frost-Related Reactions

| Reaction Name | Trigger Condition | Effect |
|---------------|-------------------|--------|
| **Low-Temperature Steam** | Frost attack + target Fire | Generates low-temperature steam cloud, applying Wetness and promoting spore reproduction. |
| **Condensation** | Standing in low-temperature steam | Slowly gains Wetness layers. |

### Universal Reactions

| Reaction Name | Trigger Condition | Effect |
|---------------|-------------------|--------|
| **Heat Shock** | Scorching target contacts water | Instantly settles remaining Scorching damage and clears Scorching. |

All reaction parameters can be adjusted in configuration files.

## 🌍 Biome Elemental Bias

Different biomes generate creatures with specific elemental tendencies:
- 🔥 **Hot biomes** (desert, badlands, nether, etc.) → Fire bias
- ❄️ **Cold biomes** (snow plains, ice spikes, etc.) → Frost bias
- 🌲 **Forest/jungle biomes** → Nature bias
- ⛈️ **Thunderstorm weather** (global) → Thunder bias

## 🛠️ Command System (Admin/OP)

All commands start with `/elementalcraft` and support Tab completion.

### Debug Mode
```
/elementalcraft debug  # Toggle debug mode, displays elemental damage calculations and reaction info
```

### Biome Bias Management
```
/elementalcraft biome add <element> <probability>
/elementalcraft biome remove <element>
/elementalcraft biome list
```

### Forced Entity Attributes
```
/elementalcraft entity add <attack element> <enhancement element> <enhancement points> <resistance element> <resistance points>
Example: /elementalcraft entity add fire fire 50 frost 20
Points support fixed values (e.g., 50) or ranges (e.g., 20-80)
/elementalcraft entity remove              # Clear forced attributes on held spawn egg
```

### Entity Attribute Blacklist
```
/elementalcraft entity blacklist add <element>      # Hold spawn egg, prevent entity from carrying specific element (or "all" for all)
/elementalcraft entity blacklist remove <element>
/elementalcraft entity blacklist list
```

### Forced Item Attributes
- **Weapons**:
```
/elementalcraft item weapon add <element>       # Bind attack attribute
/elementalcraft item weapon remove
```

- **Armor**:
```
/elementalcraft item armor add <enhancement element> <points> <resistance element> <points>
/elementalcraft item armor remove
```

### Effect Immunity Blacklist
```
# Command format (using Scorching as example):
/elementalcraft blacklist scorched add/remove/list
# Other effects: spore, static, paralysis, steam, wetness
```

### Configuration File Operations
- Changes made via commands are automatically saved and hot-loaded, no restart needed.
- Directly edit TOML files in `config/ElementalCraft/`, mod automatically detects and refreshes every 100 ticks.

## ✨ Visual Effects (Graded by Enhancement Points)

When equipment enhancement points reach thresholds, visual effects are triggered (threshold = maxStatCap configuration value):

### Melee Swing Effects
- **🔥 Fire**: Flame arcs (higher levels add soul flames, lava, smoke)
- **🌿 Nature**: Compost particles, spore flowers (higher levels add cherry blossoms, waxed particles)
- **⚡ Thunder**: Glowing particle arcs (higher levels add reverse portal, lightning arcs)

### Ranged Projectile Effects
- **🔥 Fire**: Double spiral flame trail (outer flame, inner soul flame, lava particles at tail)
- **🌿 Nature**: Cherry blossom spiral trail (tail with happy villager particles)
- **⚡ Thunder**: Lightning spark spiral trail (higher levels add ender candles, reverse portals, dragon breath)

### Impact Explosion Effects
- Bursts of corresponding elemental particles (flame/lava, cherry blossoms/spore flowers, glowing/ender candles, etc.)
- Effect intensity increases linearly with level, max level is extremely impressive. Can be adjusted in `elementalcraft-visuals.toml`.

## ⚙️ Configuration Files (Server/Client)

Configuration files are located in `config/ElementalCraft/`:

| File | Purpose |
|------|---------|
| **elementalcraft-common.toml** | Elemental weaknesses, damage multipliers, biome biases, enchantment bonuses, forced entities/blacklists, dimension attributes, etc. |
| **elementalcraft-forced-items.toml** | Forced item attribute configuration (can be added via commands). |
| **elementalcraft-fire-nature-reactions.toml** | Fire and Nature reaction parameters. |
| **elementalcraft-thunder-frost-reactions.toml** | Thunder and Frost reaction parameters. |
| **elementalcraft-visuals.toml** | Particle effect toggles, density, angles, speeds, etc. |

Supports hot-loading: modifications are automatically refreshed after saving.

## 🧙 Player Tips
1. **View Information**: Equip elemental weapons to see attack attributes, enhancement/resistance values on Jade (or TOP) panel; item tooltips show elemental prefixes and values.
2. **Elemental Weaknesses**: Prioritize using elements that are strong against enemies to increase damage.
3. **Wetness Double-Edged Sword**: Triggers reactions but easily evaporated by Fire or converted to Paralysis.
4. **Spore Parasitism**: Fear fire but increase physical defense, pay attention to positioning.
5. **Paralysis is Fatal**: Avoid being continuously hit by Thunder to prevent immobilization.
6. **Debug Mode**: Use `/elementalcraft debug` to display combat information.
7. **Dimension Features**: Nether creatures default to Fire, End creatures default to Thunder (can be disabled).

✨ **Recommended Mods**:
- **Improve Mobs**: It's recommended to disable "Mobs Can Get Enchantments" to avoid conflicts.
- **Enchanting Infuser**: Change weapon/equipment attribute enchantments, flexibly adjust elemental configurations.


# 属性锻造：元素反应 模组介绍

欢迎来到《属性锻造：元素反应》！这是一款围绕元素战斗打造的Minecraft模组，为原版生存与战斗玩法新增了全新的属性系统、元素反应机制、专属附魔以及炫酷的视觉特效。

**以下内容适用于V1.6.0版本（当前正在测试并修复BUG，更新即将发布）。冰霜属性的元素反应将在V1.7.0版本中更新。**

## 🌟 核心元素

游戏内包含4种基础元素属性 + 无属性：

| 元素 | 标识符 | 颜色 |
|---------|------------|-------|
| **赤焰** | fire | 红色 |
| **自然** | nature | 绿色 |
| **冰霜** | frost | 蓝色 |
| **雷霆** | thunder | 紫色 |
| **无属性** | none | 白色 |

每种元素均配有攻击附魔、强化附魔和抗性附魔。

## ⚔️ 附魔系统

模组为武器和防具新增了12种专属附魔（每种元素对应3种）：

### 武器附魔（攻击属性）
- **赤焰属性 / 自然属性 / 冰霜属性 / 雷霆属性**：为武器赋予对应元素的攻击效果，对敌人造成额外的元素伤害。
  - 仅可附魔在剑、斧、三叉戟、弓和弩上。
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
  - 示例：赤焰攻击自然属性目标 → 1.5倍伤害；赤焰攻击冰霜属性目标 → 0.5倍伤害。
  - 可通过Jade信息面板查看实体的元素关系（克制/被克制/无关系）。

## 🧪 状态效果

模组新增了4种全新的状态效果：

| 效果 | 描述 |
|--------|-------------|
| **潮湿** | 受到元素攻击时会触发元素反应。远离水源时缓慢消退，在雨中或水中会维持/提升层数。 |
| **易燃孢子** | 被孢子寄生，移动速度变慢且极度畏火，但物理抗性会提升。层数越高，效果越强。 |
| **静电** | 每隔一段时间受到随机伤害，并会传导至附近实体。 |
| **麻痹** | 无法进行任何动作（攻击、使用物品、切换装备）。 |

持续时间、伤害等参数均可在配置文件中调整。

## 💥 元素反应

元素之间的交互需要目标处于“潮湿”状态：

### 🔥 赤焰相关反应

| 反应名称 | 触发条件 | 效果 |
|---------------|-------------------|--------|
| **自我干燥** | 自身处于潮湿状态 + 受到赤焰攻击 | 降低自身潮湿层数，暂时降低所受赤焰伤害。 |
| **灼烧** | 赤焰攻击（目标无潮湿/冰霜属性） | 造成高强度持续火焰伤害（灼烧），免疫普通火焰伤害，接触水时触发“热冲击”。 |
| **毒火爆燃** | 赤焰攻击 + 目标带有易燃孢子 | 引爆孢子，造成范围爆炸伤害并附加灼烧效果。孢子层数越高，伤害和范围越大。 |
| **高温蒸汽** | 赤焰攻击 + 目标处于潮湿状态 | 生成蒸汽云，持续烫伤附近实体并清除潮湿状态。 |

### 🌿 自然相关反应

| 反应名称 | 触发条件 | 效果 |
|---------------|-------------------|--------|
| **自然寄生** | 自然属性攻击（概率触发） | 附加易燃孢子层数，触发概率随自然强化点数提升。 |
| **野火喷射** | 处于灼烧状态的自然属性实体 | 击退附近敌人并附加孢子效果。 |
| **孢子传播** | 孢子层数达到阈值 | 定期向附近实体传播，潮湿目标会额外转化孢子层数。 |

### ⚡ 雷霆相关反应

| 反应名称 | 触发条件 | 效果 |
|---------------|-------------------|--------|
| **静电附着** | 雷霆属性攻击（概率触发） | 附加静电层数，触发概率随雷霆强化点数提升，对潮湿目标概率更高。 |
| **静电传导** | 带有静电的目标受到周期性伤害 | 对附近实体造成溅射伤害（伤害百分比可配置）。 |
| **麻痹** | 带有静电的目标接触潮湿状态 | 清除静电和潮湿状态，转化为麻痹效果，根据静电伤害造成瞬间伤害。 |
| **麻痹传播** | 麻痹层数达到阈值 | 自动向附近实体传播。 |

### ❄️ 冰霜相关反应

| 反应名称 | 触发条件 | 效果 |
|---------------|-------------------|--------|
| **低温蒸汽** | 冰霜攻击 + 目标带有赤焰属性 | 生成低温蒸汽云，附加潮湿状态并促进孢子繁殖。 |
| **冷凝** | 处于低温蒸汽云中 | 缓慢叠加潮湿层数。 |

### 通用反应

| 反应名称 | 触发条件 | 效果 |
|---------------|-------------------|--------|
| **热冲击** | 处于灼烧状态的目标接触水 | 瞬间结算剩余灼烧伤害并清除灼烧状态。 |

所有反应参数均可在配置文件中调整。

## 🌍 群系元素偏向

不同群系生成的生物会带有特定的元素倾向：
- 🔥 **炎热群系**（沙漠、恶地、下界等）→ 赤焰偏向
- ❄️ **寒冷群系**（雪原、冰刺平原等）→ 冰霜偏向
- 🌲 **森林/丛林群系** → 自然偏向
- ⛈️ **雷暴天气**（全局）→ 雷霆偏向

## 🛠️ 指令系统（管理员/OP专用）

所有指令均以 `/elementalcraft` 开头，支持Tab补全。

### 调试模式
/elementalcraft debug  # 切换调试模式，显示元素伤害计算过程和反应相关信息

### 群系偏向管理
/elementalcraft biome add <元素> <概率>
/elementalcraft biome remove <元素>
/elementalcraft biome list

### 强制实体属性
/elementalcraft entity add <攻击元素> <强化元素> <强化点数> <抗性元素> <抗性点数>
示例：/elementalcraft entity add fire fire 50 frost 20
点数支持固定值（如50）或范围值（如20-80）
/elementalcraft entity remove              # 清除手持刷怪蛋对应的实体强制属性

### 实体属性黑名单
/elementalcraft entity blacklist add <元素>      # 手持刷怪蛋，禁止该实体携带指定元素（或填写"all"表示所有元素）
/elementalcraft entity blacklist remove <元素>
/elementalcraft entity blacklist list

### 强制物品属性
- **武器**：
/elementalcraft item weapon add <元素>       # 绑定攻击属性
/elementalcraft item weapon remove

- **防具**：
/elementalcraft item armor add <强化元素> <点数> <抗性元素> <点数>
/elementalcraft item armor remove

### 效果免疫黑名单
# 指令格式（以灼烧为例）：
/elementalcraft blacklist scorched add/remove/list
# 其他效果：spore（孢子）、static（静电）、paralysis（麻痹）、steam（蒸汽）、wetness（潮湿）

### 配置文件操作
- 通过指令做出的修改会自动保存并热加载，无需重启服务器。
- 直接编辑 `config/ElementalCraft/` 目录下的TOML文件，模组每100刻会自动检测并刷新配置。

## ✨ 视觉特效（按强化点数分级）

当装备的强化点数达到阈值时，会触发对应的视觉特效（阈值 = 配置文件中的maxStatCap值）：

### 近战挥砍特效
- **🔥 赤焰**：火焰弧光（高等级追加灵魂火、熔岩、烟雾效果）
- **🌿 自然**：堆肥粒子、孢子花（高等级追加樱花、蜡质粒子效果）
- **⚡ 雷霆**：发光粒子弧光（高等级追加反向传送门、闪电弧光效果）

### 远程投射物特效
- **🔥 赤焰**：双螺旋火焰轨迹（外层火焰、内层灵魂火，尾部附带熔岩粒子）
- **🌿 自然**：樱花螺旋轨迹（尾部附带村民喜悦粒子）
- **⚡ 雷霆**：闪电火花螺旋轨迹（高等级追加末地蜡烛、反向传送门、龙息效果）

### 撞击爆炸特效
- 对应元素粒子爆发（火焰/熔岩、樱花/孢子花、发光粒子/末地蜡烛等）
- 特效强度随等级线性提升，最高等级效果极具视觉冲击力。可在 `elementalcraft-visuals.toml` 中调整。

## ⚙️ 配置文件（服务端/客户端）

配置文件位于 `config/ElementalCraft/` 目录下：

| 文件 | 用途 |
|------|---------|
| **elementalcraft-common.toml** | 元素克制关系、伤害倍率、群系偏向、附魔加成、强制实体/黑名单、维度属性等。 |
| **elementalcraft-forced-items.toml** | 强制物品属性配置（可通过指令添加）。 |
| **elementalcraft-fire-nature-reactions.toml** | 赤焰与自然元素反应参数。 |
| **elementalcraft-thunder-frost-reactions.toml** | 雷霆与冰霜元素反应参数。 |
| **elementalcraft-visuals.toml** | 粒子特效开关、密度、角度、速度等。 |

支持热加载：保存修改后配置会自动刷新。

## 🧙 玩家小贴士
1. **信息查看**：装备元素武器后，可在Jade（或TOP）面板查看攻击属性、强化/抗性数值；物品提示文本会显示元素前缀及对应数值。
2. **元素克制**：优先使用克制敌人的元素攻击，提升伤害输出。
3. **潮湿双刃剑**：会触发元素反应，但易被赤焰蒸发或转化为麻痹效果。
4. **孢子寄生**：畏惧火焰但提升物理防御，注意走位规避火焰攻击。
5. **麻痹致命**：避免被雷霆属性攻击连续命中，防止陷入无法行动的状态。
6. **调试模式**：使用 `/elementalcraft debug` 指令可显示详细的战斗结算信息。
7. **维度特性**：下界生物默认带有赤焰属性，末地生物默认带有雷霆属性（可在配置中禁用）。

✨ **推荐搭配模组**：
- **Improve Mobs**：建议关闭“生物可获得附魔”功能，避免冲突。
- **Enchanting Infuser**：可修改武器/装备的属性附魔，灵活调整元素配置。
