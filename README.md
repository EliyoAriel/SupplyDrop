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
- Trap crates (chance-based, spawns configurable mobs + explosion on open)
- Auto-drop scheduler with fixed/random intervals and wave drops
- Full GUI loot table editor (`/supplydrop templates`) with pagination
- Interactive config GUI (`/supplydrop config`) with number input, world selector, template editor, trap mob editor
- Per-template settings (display-name, fall-duration, lock-duration) via GUI
- Preview GUI (`/supplydrop preview`) showing all items with rarity/weight info
- Landing zone marker — particle ring showing where crate will land
- Anti-grief — block place/break protection + piston protection around crates
- Zone protection — anti-camping radius around LOCK/READY crates with merged particle border
- Async SQLite persistence across server restarts
- Event history log with pagination and filters (`/supplydrop history`)
- Notification system with per-player subscriptions
- Crate UUID system with short IDs for identification
- Active crate list with clickable coordinate teleport (including falling crates)
- Delete crates by short UUID, list number, or bulk `all`
- Toggle commands for hologram, announcements, and notifications
- Max active supplydrops limit

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
| `/supplydrop active` | List all active crates (landed + falling) with clickable coords |
| `/supplydrop preview <template>` | Preview all items in a template (paginated) |
| `/supplydrop templates` | Open the GUI loot table editor |
| `/supplydrop config` | Open the interactive config GUI |
| `/supplydrop delete <id\|number\|all>` | Delete active crates by short UUID, list number, or all |
| `/supplydrop db` | Show database status |
| `/supplydrop history [page] [event:<type>] [player:<name>]` | View event history with filters |
| `/supplydrop reload` | Reload configuration files |
| `/supplydrop version` | Show plugin version |
| `/supplydrop pause` | Pause the auto-drop scheduler |
| `/supplydrop resume` | Resume the auto-drop scheduler |

### Toggle Commands

| Command | Description |
|---|---|
| `/supplydrop toggle hologram` | Toggle hologram visibility |
| `/supplydrop toggle announce` | Toggle announcements |
| `/supplydrop toggle notify` | Toggle personal notifications |
| `/supplydrop toggle zone` | Toggle zone protection |

### Notification Commands

| Command | Description |
|---|---|
| `/supplydrop subscribe` | Subscribe to drop notifications |
| `/supplydrop unsubscribe` | Unsubscribe from notifications |

### Spawn Conditions

The `/supplydrop spawn` command accepts optional conditions:

```
/supplydrop spawn <template> [team] [trap] [wave:<count>] [players:<count>] [expiry:<ticks>] [lock:<ticks>]
```

| Condition | Description |
|---|---|
| `team` | Force this crate to be a team crate |
| `trap` | Force this crate to be a trap |
| `wave:<count>` | Spawn multiple crates simultaneously |
| `players:<count>` | Required players for team crate (default: 2) |
| `expiry:<ticks>` | Custom expiry time (0 = no expiry) |
| `lock:<ticks>` | Custom lock duration (0 = skip lock phase) |

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
| `/supplydrop auto team-crate <percent>` | Team crate chance |
| `/supplydrop auto team-players <count>` | Required players |
| `/supplydrop auto trap-chance <percent>` | Trap chance |
| `/supplydrop auto trap-mobs <type1> <type2> ...` | Set trap mob types |
| `/supplydrop auto loot-scaling <true\|false>` | Toggle loot scaling |

## Configuration

### config.yml

```yaml
# Drop Settings
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
  fall-duration: 0
  height: 20
  hologram:
    enabled: true
    lines:
      - "&b&l{template}"
      - "{team}"
      - "{team-progress}"
      - "{state}"
      - "{lock-progress}"
      - "{lock-time}"
      - "{time}"
      - "&7Right-click to open"

# Crate settings
crate:
  expiry: 1200
  protection-radius: 2
  max-active: 30
  team-open-chance: 0
  team-open-range: 2
  trap-chance: 0
  trap-mobs:
    - ZOMBIE
    - SKELETON
    - CREEPER
  lock:
    enabled: true
    duration: 200
    random: false
    duration-min: 100
    duration-max: 400
    sound-lock: "entity.iron_door.close"
    sound-ready: "entity.player.levelup"
    ready-notification: true
    ready-notification-radius: 20
    break-behavior: "destroy"
    particle:
      enabled: true
      type: ENCHANTMENT_TABLE
      radius: 1.5
  zone:
    enabled: true
    radius: 25
    particle: FLAME
    deny-message: "&c&lSupply Drop &7- &fYou cannot enter the drop zone!"

# Loot rarity tiers (weights are relative, higher = more common)
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

# How many items to roll per drop (min-max)
rolls:
  min: 3
  max: 6

# /supplydrop call settings
call:
  fall-duration: 0

# /supplydrop spawn settings
spawn:
  fall-duration: 0

# Announcement settings
announce:
  enabled: true
  actionbar: false
  coord-reveal-delay: 0

# Auto-drop settings
auto-drop:
  enabled: false
  paused: false
  random-interval: true
  interval: 72000
  interval-min: 36000
  interval-max: 72000
  world: "world"
  random-radius: 500
  fall-duration: 0
  templates:
    weapons:
      weight: 50
    armor:
      weight: 30
    supplies:
      weight: 20
  wave-count: 1
  announce: true
  announce-actionbar: false
  announce-delay: 100
  coord-reveal-delay: 0
  expiry: 0
  team-crate-chance: 0
  team-crate-range: 2
  trap-chance: 0
  trap-mobs:
    - ZOMBIE
    - SKELETON
    - CREEPER
  loot-scaling: false
  loot-scaling-max: 20

# Logging settings
logging:
  debug: false

# UI theme settings
ui:
  chat:
    colors:
      primary: BLUE
      text: WHITE
      accent: AQUA
      success: GREEN
      warning: YELLOW
      error: RED
      error-detail: DARK_RED

# Notification settings
notification:
  default-subscribe: true
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
| `{lock-progress}` | Lock phase visual progress bar |
| `{lock-time}` | Lock phase remaining time |
| `{state}` | Crate state label (Falling, LOCK, READY, etc.) |

### packages.yml

Define loot tables with weighted items:

```yaml
weapons:
  display-name: "&c&lWeapons Crate"
  fall-duration: 3
  lock-duration: 0
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

## Crate Types

### Normal Crate
Standard loot crate with random items. Items drop on ground when barrel is broken normally.

### Team Crate
- Chance-based (configurable via `team-open-chance` / `team-crate-chance`)
- Requires N players to right-click simultaneously (random between 2 and `team-open-range`)
- Hologram shows progress bar with contributor names
- All players must be in range when opening
- Broadcasts contributor names on open
- **Punishment**: breaking a team crate manually destroys all items (no drop)

### Trap Crate
- Chance-based (configurable via `trap-chance`)
- Spawns random mobs from configurable list (each spawn picks independently)
- Small TNT explosion for visual effect
- Hologram does not show trap warning (hidden until opened)
- **Breaking manually**: trap fires + items destroyed

### Crate Lifecycle
- **Falling**: parachute system active, cannot be opened
- **Landing**: barrel created, hologram spawned
- **Lock Phase**: crate locked, cannot be opened, hologram shows countdown
- **Ready**: crate unlocked, can be opened
- **Opened**: hologram removed, after 3 seconds remaining loot drops + barrel destroyed
- **Expired**: destroyed with NO item drop (server takes the loot)
- **Normal barrel break**: items drop on ground
- **Explosion/burn**: items always drop on ground
- **Anti-grief**: block place/break protection around crates (configurable radius)

### Crate Lock Phase

A configurable lock phase delays access to landed crates:
- **Config**: `crate.lock.enabled`, `crate.lock.duration`, `crate.lock.random`, `crate.lock.duration-min/max`
- **Per-template**: Set `lock-duration` in `packages.yml` or via GUI
- **Bypass**: Use `lock:0` in spawn command to skip lock phase
- **Break behavior**: Configurable (`destroy` = items lost, `drop` = items drop)
- **Effects**: Lock particles and sound effects during lock phase
- **Ready notification**: Nearby players notified when crate unlocks

### Crate UUID System
Every crate gets a unique UUID on creation. Short 8-character IDs are displayed in the active list for easy identification. UUIDs are persisted in the database across server restarts.

### Configuration Precedence

Settings follow a clear override chain. Each level overrides the one below it:

```
Template (per-template) > Context (auto-drop/call/spawn) > Global (crate.*)
```

| Setting | Template Override | Auto-Drop Override | Call/Spawn Override | Global Default |
|---|---|---|---|---|
| **Fall duration** | `packages.yml` `fall-duration` | `auto-drop.fall-duration` | `call.fall-duration` / `spawn.fall-duration` | `drop.fall-duration` |
| **Expiry** | — | `auto-drop.expiry` (0 = use `crate.expiry`) | — | `crate.expiry` |
| **Trap chance** | — | `auto-drop.trap-chance` (0 = use `crate.trap-chance`) | — | `crate.trap-chance` |
| **Trap mobs** | — | `auto-drop.trap-mobs` | — | `crate.trap-mobs` |
| **Team crate chance** | — | `auto-drop.team-crate-chance` (0 = use `crate.team-open-chance`) | — | `crate.team-open-chance` |
| **Team crate range** | — | `auto-drop.team-crate-range` | — | `crate.team-open-range` |
| **Lock duration** | `packages.yml` `lock-duration` | — | `lock:<ticks>` in spawn command | `crate.lock.duration` |

**How it works:**
- Each setting is carried in `DropOptions`, which Crate reads at creation time
- `null` field = fall back to global `crate.*` setting
- `0` for auto-drop overrides = fall back to global (except fall-duration where 0 = use `drop.fall-duration`)
- `/supplydrop call` uses global `crate.*` settings (no auto-drop overrides)
- `/supplydrop spawn` uses global `crate.*` settings, with per-command overrides via parameters
- Auto-drops and wave drops resolve auto-drop-specific overrides into `DropOptions`

## Features in Detail

### Interactive Config GUI

`/supplydrop config` opens a chest-inventory-based configuration system with multiple pages:

- **Main Menu**: Overview of all settings categories
- **Auto-Drop Page**: All auto-drop settings with number inputs, toggles, world selector, template editor, trap mob editor
- **Crate Page**: Crate behavior settings including lock duration, lock random, trap mob management
- **Toggles Page**: Quick access to all boolean settings
- **Hologram Page**: Hologram toggle and line editor

Number inputs feature ±1/±10/±100 buttons, a "Set to 0" button, and confirm/cancel/reset controls.

### Per-Template Settings

Each template has its own settings GUI accessible from the template list:
- **Display Name**: Custom barrel name (editable via chat input)
- **Fall Duration**: Per-template fall time override
- **Lock Duration**: Per-template lock phase override
- **Item Count**: View total items in the loot table

### Template Auto-Drop Editor

`/supplydrop config` → Templates opens a GUI showing all templates:
- **Emerald** = enabled, **Gray dye** = disabled, **Barrier** = no items
- **Left-click** to edit weight (opens number input)
- **Shift-click** to toggle enabled/disabled (weight 0 = disabled)

### Trap Mob Editor

Both crate and auto-drop config pages have clickable trap-mob items that open a GUI showing all mob types with themed materials. Click any mob to toggle it on/off.

### Active Crate Management

`/supplydrop active` lists all active crates (landed + falling):
- Falling crates show `[Falling]` tag with X/Z coordinates
- Locked crates show `[LOCK Xs]` tag with countdown
- Ready crates show `[READY]` tag
- Clickable coordinates teleport you to the crate
- Short UUID prefix on each entry for identification

`/supplydrop delete <id|number|all>` removes crates:
- **Short UUID**: first 8 characters of the crate UUID
- **Number**: the list number from `/supplydrop active`
- **`all`**: remove all active crates

### Event History

`/supplydrop history [page] [event:<type>] [player:<name>]` shows a paginated event log:
- Events: DROP, OPEN, BREAK, EXPIRE, TRAP
- Player filter uses fuzzy search (matches partial names)
- Stored in separate `history.db` database

### Notification System

Players can subscribe/unsubscribe from drop notifications:
- `/supplydrop subscribe` / `/supplydrop unsubscribe`
- Same-world drops notify all online players
- Cross-world drops only notify subscribed players
- Configurable default subscription status

### Landing Zone Marker
When a crate starts falling, a particle ring appears on the ground showing where it will land. Configurable particle type and radius. A vertical column marks the center.

### Anti-Grief
Protects the area around supply crates from griefing:
- Prevents block placement near crates
- Prevents block breaking near crates (except the crate barrel itself)
- Prevents pistons from pushing/pulling crate barrels
- Configurable protection radius in blocks

### Zone Protection
Prevents block interactions around LOCK/READY crates:
- Configurable radius (default 25 blocks)
- Block placement and breaking disabled inside the zone
- Merged particle border: when multiple crates overlap, only the outermost border is shown
- Visual flame particles show the protected area
- Toggle with `/supplydrop toggle zone` or config GUI
- Config: `crate.zone.enabled`, `crate.zone.radius`, `crate.zone.particle`

### Preview GUI
`/supplydrop preview <template>` opens a virtual chest showing all items in a loot table. Each item displays its rarity, weight, and effective weight. Supports pagination for large loot tables.

### Database Persistence

Crate data saves to SQLite database (`plugins/SupplyDrop/crates.db`). Event history and subscriptions saved to `history.db`. On server restart:
- Unopened crates are restored
- Barrel blocks are verified
- Invalid entries are auto-cleaned
- Async write queue — zero main-thread blocking

### Announcements

- Chat or actionbar messages
- Optional coordinate reveal delay
- Only announces to players in the same world
- Toggle with `/supplydrop toggle announce`

### Wave Drops

Multiple crates can drop simultaneously:
- Configurable wave count
- Each crate gets independent loot
- Random spread around center location

### Auto-Drop Scheduler

- Fixed or random intervals
- Pause/resume without disabling
- Weighted template system for random selection
- World-specific drops with configurable radius
- Separate team-crate and trap settings from global crate settings
- Per-template and per-command fall duration overrides

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
5. Use `/supplydrop reload` or `/supplydrop config` to apply changes

## License

MIT
