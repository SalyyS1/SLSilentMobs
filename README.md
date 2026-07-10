# SLSilentMobs

**Private/Silent Mobs & Instanced Loot for RPG Minecraft Servers**

> Version: 2.3.0 | Author: SalyVn | API: Paper / Spigot 1.21.5 | Java: 17

A packet-level entity visibility plugin inspired by **Wynncraft**. Mobs, item drops, and regions can be made visible only to specific players — enabling per-player quests, private farm zones, instanced loot, and more.

**[Documentation (Wiki)](https://salyys1.github.io/SLSilentMobs/)** | **[Releases](https://github.com/SalyyS1/SLSilentMobs/releases)**

---

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Commands](#commands)
- [Permissions](#permissions)
- [Configuration](#configuration)
- [RPG Use Cases](#rpg-use-cases)
- [Project Structure](#project-structure)
- [Building](#building)
- [License](#license)

---

## Features

### Silent Mobs (Manual)

Spawn mobs that are **only visible to a designated player**. All other players on the server cannot see or interact with the entity.

- Supports both **Vanilla** and **MythicMobs** entity types
- Hides **ModelEngine 4** client-side models together with their base entity
- Mobs only target their assigned owner
- Permission-based visibility — share mobs with players who have a specific permission node
- Automatic despawn on owner disconnect, timeout expiry, or plugin disable

### Global Silent Mode

When enabled, **every mob on the server** automatically becomes private to the nearest player within a configurable radius.

- Natural spawns, spawners, and MythicMobs are all affected
- **Whitelist** system: bosses and special mobs remain visible to everyone
- Ownership transfers to the nearest player when the current owner disconnects

### Silent Regions

Define area-based visibility zones using an in-game wand tool.

- Mobs inside a region are only visible to allowed players or permission holders
- Per-region mob type filters and exemption lists
- Spawn private mobs when players enter a region, with per-player cooldowns
- Managed entirely via commands — region data persists in `regions.yml`

### Instanced Loot

Item drops from mob kills are **only visible to and collectible by the killer**.

- Other players cannot see or pick up the items
- EXP is awarded directly to the killer
- Configurable timeout before uncollected items despawn
- Can be scoped to silent mobs only or applied server-wide

### Additional Systems

- **Anti-Target** — Silent mobs ignore all players except their owner
- **Auto Timeout & Cleanup** — Background task removes expired mobs every 5 seconds
- **Per-player mob limits** — Configurable cap on active silent mobs per player
- **PlaceholderAPI** integration
- **Multi-language messages** — English and Vietnamese supported out of the box

---

## Requirements

| Dependency | Version | Required |
|------------|---------|----------|
| Paper / Spigot | 1.21.5+ | Yes |
| ProtocolLib | 5.4.0+ | Yes |
| MythicMobs | 5.x | No (enables custom mob support) |
| ModelEngine | 4.0.7 - 4.1.x | No (enables per-player model visibility) |
| PlaceholderAPI | 2.11+ | No (enables placeholders) |

---

## Installation

1. Download `SLSilentMobs-2.3.0.jar` from the [Releases](https://github.com/SalyyS1/SLSilentMobs/releases) page.
2. Place the JAR into your server's `plugins/` directory.
3. Ensure **ProtocolLib** is installed. Install **ModelEngine 4** only when using modeled mobs.
4. Restart the server.
5. Configuration files are generated automatically in `plugins/SLSilentMobs/`.

---

## Commands

Base command: `/silentmob` (aliases: `/sm`, `/slmob`)

### Core

| Command | Description |
|---------|-------------|
| `/sm help` | Display the help menu |
| `/sm spawn <player> <mob> [amount] [level] [world] [x] [y] [z] [-p <perm>]` | Spawn a silent mob for a player |
| `/sm despawn <player> [mob]` | Remove silent mobs belonging to a player |
| `/sm despawnall` | Remove all silent mobs server-wide |
| `/sm list [player]` | List active silent mobs |
| `/sm reload` | Reload all configuration files |

### Global Silent

| Command | Description |
|---------|-------------|
| `/sm global on` | Enable global silent mode |
| `/sm global off` | Disable global silent mode |
| `/sm global status` | Show current status and mob count |
| `/sm global whitelist add <mob>` | Add a mob to the visibility whitelist |
| `/sm global whitelist remove <mob>` | Remove a mob from the whitelist |

### Regions

| Command | Description |
|---------|-------------|
| `/sm wand` | Receive the region selection wand |
| `/sm region create <name>` | Create a region from the current selection |
| `/sm region delete <name>` | Delete a region |
| `/sm region list` | List all defined regions |
| `/sm region info <name>` | Show details of a region |
| `/sm region addmob <region> <mob>` | Add a mob type to a region's silent list |
| `/sm region addplayer <region> <player>` | Allow a player to see mobs in a region |
| `/sm region addperm <region> <perm>` | Allow a permission node to see mobs in a region |
| `/sm region addspawn <region> <mob> [amount] [level] [cooldown] [spread]` | Spawn private mobs when players enter a region |
| `/sm region removespawn <region> <mob>` | Remove an enter-triggered region spawn |
| `/sm region listspawns <region>` | List region enter-triggered spawns |

### Examples

```bash
# Spawn a silent zombie for Steve
/sm spawn Steve ZOMBIE

# Spawn 5 level-10 skeletons at specific coordinates
/sm spawn Steve SKELETON 5 10 world 100 65 200

# Spawn a MythicMob visible only to players with vip.premium
/sm spawn Steve DungeonGoblin 1 1 -p vip.premium

# Enable global silent mode with boss whitelist
/sm global on
/sm global whitelist add ENDER_DRAGON
/sm global whitelist add WorldBoss_Dragon

# Spawn 3 private wolves when players enter road_01, then wait 60s before spawning again
/sm region addspawn road_01 WOLF 3 1 60 6
```

---

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `silentmob.use` | Use the `/silentmob` command | OP |
| `silentmob.admin` | Admin commands: `global`, `reload`, `despawnall`, `region`, `wand` | OP |

---

## Configuration

Version 2.3.0 splits configuration into four files:

```
plugins/SLSilentMobs/
  config.yml     — General settings and global silent mode
  drops.yml      — Instanced loot settings
  regions.yml    — Region data (managed by commands)
  messages.yml   — All messages with language toggle (en / vi)
```

Refer to the **[Wiki](https://salyys1.github.io/SLSilentMobs/)** for detailed configuration documentation.

### Quick Reference

```yaml
# config.yml
settings:
  max-amount: 50
  default-timeout: 300        # seconds, 0 = infinite
  despawn-on-quit: true
  mob-target-owner-only: true
  death-notification: true
  max-mobs-per-player: 10

integrations:
  model-engine: true            # auto-detect and hide client-side models

global-silent:
  enabled: false
  assign-radius: 32
  whitelist:
    vanilla: [ENDER_DRAGON, WITHER, WARDEN]
    mythicmobs: []
  reassign-on-owner-quit: true
  despawn-if-no-player: true
```

```yaml
# drops.yml
silent-drops:
  enabled: true
  apply-to-silent-mobs-only: true
  apply-globally: false
  drop-timeout: 60
```

```yaml
# messages.yml
language: vi   # Switch to "en" for English
```

---

## RPG Use Cases

**Personal Quest Mobs** — NPCs trigger `/sm spawn` to create private quest encounters per player. No interference from others.

**Road Encounters** — Regions trigger private mob packs when players enter, with cooldowns to prevent spam.

**Private Farm Zones** — Enable global silent mode so every player farms their own mob instances. No kill-stealing.

**Dungeon Bosses** — Whitelist boss mobs so the entire party can see and fight them together.

**Instanced Loot** — Each player sees only their own drops. No loot competition.

**PvE Events** — Spawn waves of mobs visible only to the event participant.

---

## Project Structure

```
src/main/java/vn/saly/silentmobs/
  SLSilentMobs.java                 Main plugin class
  config/
    ConfigManager.java              Multi-file config handler
  command/
    SilentMobCommand.java           Command handler
    SilentMobTab.java               Tab completer
  global/
    GlobalSilentManager.java        Auto-silent all mobs
    GlobalSilentConfig.java         Whitelist configuration
  listener/
    SilentMobDeathListener.java     Death cleanup & notification
    SilentMobTargetListener.java    Owner-only targeting
    PlayerConnectionListener.java   Join/quit handling
  loot/
    SilentDropManager.java          Instanced loot engine
    SilentDropListener.java         Drop interception
  manager/
    SilentMobManager.java           Central mob registry
  model/
    SilentMob.java                  Mob data model
  region/
    RegionManager.java              Region CRUD & persistence
    SilentRegion.java               Region data model
    RegionSilentListener.java       Region-based visibility
    RegionSpawnListener.java        Enter-triggered private region spawns
    RegionSpawnEntry.java           Region spawn rule model
    WandManager.java                Selection wand tool
    WandListener.java               Wand interaction handler
  task/
    MobTimeoutTask.java             Scheduled cleanup
  visibility/
    EntityHider.java                ProtocolLib packet hiding
    ModelEngineVisibilityBridge.java Optional ModelEngine per-viewer pairing
    ModelEngineViewerMethods.java   ModelEngine 4.0/4.1 compatibility resolver
  placeholder/
    SilentMobsPlaceholder.java      PlaceholderAPI hook
  util/
    MobSpawner.java                 Shared vanilla/MythicMobs spawn helper
```

---

## Building

```bash
mvn clean package
```

The compiled JAR is output to `target/SLSilentMobs-2.3.0.jar`.

---

## License

Developed by **SalyVn** for RPG server use.
