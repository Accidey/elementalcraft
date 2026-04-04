


# ElementalCraft Mod Introduction

Welcome to **ElementalCraft**! This is a Minecraft mod centered around elemental combat, adding a new attribute system, elemental reactions, special enchantments, and stunning visual effects to vanilla survival and combat.

**The following is for version V1.6.0 (upcoming update). Frost attribute elemental reactions will be updated in V1.7.0.**

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
  - Different elemental attack enchantments are mutually exclusive, but same-element enchantments can coexist (only level 1).

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
| **High-Temperature Steam** | Fire attack + target Wet or Frost | Generates steam cloud, continuously scalding nearby entities and clearing Wetness. |

### 🌿 Nature-Related Reactions

| Reaction Name | Trigger Condition | Effect |
|---------------|-------------------|--------|
| **Nature Parasitism** | Nature attack (chance) | Applies Flammable Spores layers, chance increases with Nature Enhancement points. |
| **Nature Siphon** | Nature attack + target Wet | Siphons Wetness layers, restoring health and increasing spore count. |
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

### Entity Attribute Blacklist
```
/elementalcraft blacklist add <element>      # Hold spawn egg, prevent entity from carrying specific element (or "all" for all)
/elementalcraft blacklist remove <element>
/elementalcraft blacklist list
```

### Forced Entity Attributes
```
/elementalcraft entity add <attack element> <enhancement element> <enhancement points> <resistance element> <resistance points>
Example: /elementalcraft entity add fire fire 50 frost 20
Points support fixed values (e.g., 50) or ranges (e.g., 20-80)
/elementalcraft entity remove              # Clear forced attributes on held spawn egg
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
| **...-fire-nature-reactions.toml** | Fire and Nature reaction parameters. |
| **...-thunder-frost-reactions.toml** | Thunder and Frost reaction parameters. |
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

Embrace the power of elements and forge your legendary gear!
