# SupplyDrop

A Minecraft Paper plugin that drops randomized care packages from the sky as world events. Features weighted loot tables, team/trap crates, async SQLite persistence, and a full GUI editor.

## Requirements

- **Paper** 1.21+
- **Java** 21+

## Features

- Randomized loot tables with weighted rarity system (dynamic tiers)
- Parachute system (invisible slimes + chickens as falling block passengers)
- Barrel crate container (auto-destroys when empty)
- Floating hologram with live countdown, team progress, and customizable placeholders
- Team crates (chance-based, requires multiple players to open simultaneously)
- Trap crates (chance-based, spawns mobs + small TNT explosion on open)
- Auto-drop scheduler with fixed/random intervals and wave drops
- Full GUI editor (`/supplydrop templates`) with pagination
- Preview GUI (`/supplydrop preview`) showing all items with rarity/weight info
- Landing zone marker — particle ring showing where crate will land
- Anti-grief — block place/break protection + piston protection around crates
- Async SQLite persistence across server restarts
- Customizable display names per template
- Kill credit — team crate broadcasts contributor names on open
- All features configurable in-game via commands

## Permissions

| Permission | Description |
|---|---|
| `supplydrop.admin` | Access to all admin commands |
| `supplydrop.<package>` | Access to open a specific package |

## Commands

### Main Commands

| Command | Description |
|---|---|
| `/supplydrop call` | Call a random supply drop at your location |
| `/supplydrop <template>` | Call a specific loot table template |
| `/supplydrop spawn <template> [conditions...]` | Spawn a crate with forced conditions |
| `/supplydrop active` | List all active (unopened) crates |
| `/supplydrop preview <template>` | Preview all items in a template (paginated) |
| `/supplydrop db` | Show database status |
| `/supplydrop templates` | Open the GUI loot table editor |
| `/supplydrop reload` | Reload configuration files |
| `/supplydrop version` | Show plugin version |
| `/supplydrop pause` | Pause the auto-drop scheduler |
| `/supplydrop resume` | Resume the auto-drop scheduler |

### Spawn Conditions

The `/supplydrop spawn` command accepts optional conditions:

```
/supplydrop spawn <template> [team] [trap] [wave:<count>] [players:<count>] [expiry:<ticks>]
```

| Condition | Description |
|---|---|
| `team` | Force this crate to be a team crate |
| `trap` | Force this crate to be a trap |
| `wave:<count>` | Spawn multiple crates simultaneously |
| `players:<count>` | Required players for team crate (default: 2) |
| `expiry:<ticks>` | Custom expiry time (0 = no expiry) |

**Examples:**
```bash
/supplydrop spawn weapons
/supplydrop spawn armor team players:3
/supplydrop spawn supplies trap expiry:6000
/supplydrop spawn legendary wave:3 team players:4
```

### Package Commands

| Command | Description |
|---|---|
| `/supplydrop package create <name>` | Create a new loot table template |
| `/supplydrop package delete <name>` | Delete a template |
| `/supplydrop package setdisplay <name> <display name>` | Set barrel display name |
| `/supplydrop package setdisplay <name> clear` | Reset to default name |

### Auto-Drop Commands

| Command | Description |
|---|---|
| `/supplydrop auto status` | Show current settings |
| `/supplydrop auto enable` | Enable auto-drop |
| `/supplydrop auto disable` | Disable auto-drop |
| `/supplydrop auto pause` | Pause scheduler |
| `/supplydrop auto resume` | Resume scheduler |
| `/supplydrop auto interval <ticks>` | Set fixed interval |
| `/supplydrop auto random-interval <true\|false>` | Toggle random interval |
| `/supplydrop auto interval-min <ticks>` | Set minimum interval |
| `/supplydrop auto interval-max <ticks>` | Set maximum interval |
| `/supplydrop auto world <name>` | Set target world |
| `/supplydrop auto radius <blocks>` | Set random radius |
| `/supplydrop auto templates` | List templates |
| `/supplydrop auto templates add\|remove\|set` | Manage templates |
| `/supplydrop auto announce <true\|false>` | Toggle announcements |
| `/supplydrop auto announce-delay <ticks>` | Set announce delay |
| `/supplydrop auto announce-actionbar <true\|false>` | Use actionbar |
| `/supplydrop auto coord-reveal-delay <ticks>` | Coord reveal delay |
| `/supplydrop auto wave-count <count>` | Crates per drop |
| `/supplydrop auto expiry <ticks>` | Crate expiry time |
| `/supplydrop auto team-crate <true\|false>` | Toggle team crates |
| `/supplydrop auto team-players <count>` | Required players |
| `/supplydrop auto trap-chance <percent>` | Trap chance |
| `/supplydrop auto trap-mobs <add\|remove> <type>` | Manage trap mobs |
| `/supplydrop auto loot-scaling <true\|false>` | Toggle loot scaling |
| `/supplydrop auto escalating <true\|false>` | Toggle escalating rarity |

## Configuration

### config.yml

```yaml
drop:
  parachute:
    chicken-count: 5
  particles:
    landing-effects: true
    continuous-effects: true
    flare-effects: true
    smoke:
      enabled: false
      height: 20
    landing-zone:
      enabled: true
      radius: 3
      particle: FLAME
  falling-speed: 0.3
  height: 20
  hologram:
    enabled: true
    lines:
      - "&b&l{template}"
      - "{team}"
      - "{team-progress}"
      - "{time}"
      - "&7Right-click to open"

crate:
  expiry: 0
  protection-radius: 2
  team-open-chance: 0
  team-open-players: 2
  trap-chance: 0
  trap-mobs:
    - ZOMBIE
    - SKELETON
    - CREEPER

rarity:
  common:
    weight: 60
    color: GRAY
    prefix: "&7"
  uncommon:
    weight: 25
    color: GREEN
    prefix: "&a"
  rare:
    weight: 10
    color: BLUE
    prefix: "&9"
  legendary:
    weight: 5
    color: GOLD
    prefix: "&6"

rolls:
  min: 3
  max: 6

announce:
  actionbar: false
  coord-reveal-delay: 0

auto-drop:
  enabled: false
  paused: false
  interval: 72000
  random-interval: false
  interval-min: 36000
  interval-max: 72000
  world: "world"
  random-radius: 500
  templates:
    weapons:
      weight: 50
    armor:
      weight: 30
    supplies:
      weight: 20
  announce: true
  announce-actionbar: false
  announce-delay: 100
  coord-reveal-delay: 0
  wave-count: 1
  expiry: 0
  team-crate-chance: 0
  team-crate-players: 2
  trap-chance: 0
  trap-mobs:
    - ZOMBIE
    - SKELETON
    - CREEPER
  loot-scaling: false
  loot-scaling-max: 20
  escalating: false
  escalating-factor: 1.5
```

### Hologram Placeholders

| Placeholder | Description |
|---|---|
| `{template}` | Template display name |
| `{name}` | Alias for `{template}` |
| `{x}` | X coordinate |
| `{z}` | Z coordinate |
| `{time}` | Remaining time (auto-formatted) |
| `{team}` | "TEAM CRATE" label (empty if not team) |
| `{team-required}` | Players needed |
| `{team-remaining}` | Players still needed |
| `{team-progress}` | Visual progress bar |

### packages.yml

Define loot tables with weighted items:

```yaml
weapons:
  display-name: "&c&lWeapons Crate"
  common:
    items:
      wooden_sword:
        weight: 50
      stone_sword:
        weight: 30
  uncommon:
    items:
      iron_sword:
        weight: 20
      bow:
        weight: 15
  rare:
    items:
      diamond_sword:
        weight: 5
      crossbow:
        weight: 8
  legendary:
    items:
      netherite_sword:
        weight: 2
```

## Loot System

### Rarity Tiers

- **Weight-based**: Higher weight = more common
- **Dynamic**: Add custom tiers (e.g., `mythic`, `divine`) in config
- **Color-coded**: Each tier has configurable chat color and prefix

### Item Rolls

Each drop rolls `min` to `max` items from the weighted pool. Effective chance = `itemWeight × rarityWeight`.

### Loot Scaling

When enabled, bonus rolls scale based on time since last drop:
- More time = more items
- Configurable maximum bonus

### Escalating Rarity

When enabled, rarity improves the longer without a drop:
- Multiplier increases each drop without loot
- Configurable escalation factor

## Crate Types

### Normal Crate
Standard loot crate with random items. Items drop on ground when barrel is broken normally.

### Team Crate
- Chance-based (configurable)
- Requires N players to right-click simultaneously
- Hologram shows progress bar `■■■`
- All players must be in range when opening
- Broadcasts contributor names on open
- **Punishment**: breaking a team crate manually destroys all items (no drop)

### Trap Crate
- Chance-based (configurable)
- Spawns random mobs on open
- Small TNT explosion for visual effect
- Hologram does not show trap warning (hidden until opened)
- **Breaking manually**: trap fires + items destroyed

### Crate Lifecycle
- **Opened**: hologram removed, after 3 seconds remaining loot drops + barrel destroyed
- **Expired**: destroyed with NO item drop (server takes the loot)
- **Normal barrel break**: items drop on ground
- **Explosion/burn**: items always drop on ground
- **Anti-grief**: block place/break protection around crates (configurable radius)

## Features in Detail

### Landing Zone Marker
When a crate starts falling, a particle ring appears on the ground showing where it will land. Configurable particle type and radius. A vertical column marks the center.

### Anti-Grief
Protects the area around supply crates from griefing:
- Prevents block placement near crates
- Prevents block breaking near crates (except the crate barrel itself)
- Prevents pistons from pushing/pulling crate barrels
- Configurable protection radius in blocks

### Preview GUI
`/supplydrop preview <template>` opens a virtual chest showing all items in a loot table. Each item displays its rarity, weight, and effective weight. Supports pagination for large loot tables.

### Database Persistence

Crate data saves to SQLite database (`plugins/SupplyDrop/crates.db`). On server restart:
- Unopened crates are restored
- Barrel blocks are verified
- Invalid entries are auto-cleaned
- Async write queue — zero main-thread blocking

### Announcements

- Chat or actionbar messages
- Optional coordinate reveal delay
- Only announces to players in the same world

### Wave Drops

Multiple crates can drop simultaneously:
- Configurable wave count
- Each crate gets independent loot
- Random spread around center location

### Auto-Drop Scheduler

- Fixed or random intervals
- Pause/resume without disabling
- Template weight system for random selection
- World-specific drops with configurable radius

## Building

```bash
mvn clean package -Dmaven.compiler.forceJavacCompilerUse=true
```

Output: `target/SupplyDrop-1.0.0-SNAPSHOT.jar`

## Installation

1. Build or download the JAR
2. Place in `plugins/` folder
3. Restart server
4. Edit `plugins/SupplyDrop/config.yml` as needed
5. Use `/supplydrop reload` to apply changes

## License

MIT
