# Changelog

All notable changes to SLSilentMobs will be documented in this file.

## [Unreleased]

### Bug Fixes

- Apply ModelEngine's native per-viewer renderer predicate before display entities are created, preventing model bones recreated by animated skills from reaching unauthorized clients.
- Keep the native audience policy synchronized across viewer changes, disconnects, integration reloads, and entity cleanup while retaining packet-level cleanup as a fallback.
- Track ModelEngine VFX pivot and model display IDs created by skills for a private mob, so their packets follow the same viewer policy as the mob.
- Keep each ModelEngine tracked-entity wrapper for the private mob's lifetime so wrapper-local audience and forced-hidden state cannot be discarded between syncs.
- Learn client-only model descendants from `MOUNT` packets and release their mappings when a mount changes or the private base model is removed.

### Diagnostics

- Added `/sm debug` to report ModelEngine integration state, mapped client-side IDs, and base/model packet-cancellation counters from the live server.

### Compatibility

- Verified the native audience integration against ModelEngine R4.0.7, R4.0.9, and R4.1.0 APIs.
- Documented Paper 26.1+ requirements: Java 25 and ProtocolLib 5.5 development build or newer.

## [2.3.0] - 2026-07-10

### Features

- Added optional ModelEngine 4 client-side model visibility integration.
- Added runtime compatibility for ModelEngine 4.0 `Player` and 4.1 `UUID` viewer APIs.
- Added late-model synchronization through ModelEngine's `AddModelEvent`.
- Added focused JUnit regression tests for ModelEngine compatibility and region rules.

### Improvements

- Added diff-based viewer updates with immediate entity respawn for newly authorized players.
- Refresh region and permission viewers for already spawned mobs.
- Preserve visibility metadata and age when global mobs change owner.
- Reassign orphaned global mobs during maintenance when a player becomes available.
- Centralized entity untracking and cleanup paths.
- Prune region spawn cooldown state when players disconnect.
- Made mob and region key normalization locale-independent.

## [2.2.0] - 2026-07-08

### Features

- Added region enter-triggered private mob spawns.
- Added per-player cooldowns for region spawns.
- Added `/sm region addspawn`, `/sm region removespawn`, and `/sm region listspawns`.
- Added shared vanilla/MythicMobs spawn helper for command and region spawns.

## [2.1.2] - 2026-07-08

### Bug Fixes

- Fixed silent mob instanced loot not applying when death cleanup ran before drop handling.
- Fixed restricted silent regions revealing mobs to unauthorized nearest players.
- Fixed region UUID/permission viewers not receiving visibility when joining after spawn.
- Fixed permission viewers being able to see mobs but not damage or be targeted by them.
- Fixed silent item drops remaining in-world after plugin disable.
- Fixed `%slsilentmobs_player_can_see_<region>%` placeholder routing.
- Aligned plugin version/API/Java metadata across Maven, plugin.yml, README, and configs.

## [2.1.1] - 2026-02-15

### 🐛 Bug Fixes

- **Fix AOE damage affecting other players' silent mobs** — Added `SilentMobDamageListener` that intercepts `EntityDamageByEntityEvent` and cancels damage from non-owner players
  - Handles: direct melee, projectiles (Arrow/Fireball/Trident), AreaEffectCloud, EvokerFangs, TNT, Firework
  - Previously, silent mobs were only hidden visually (packet-level) but server-side entities still received damage from AOE skills

## [2.1.0] - 2026-02-11

### ✨ Features

- Split config into 4 files: `config.yml`, `drops.yml`, `regions.yml`, `messages.yml`
- Enhanced `/sm help` command with multi-language support (EN/VI)
- Improved `/sm reload` to reload all config files

## [2.0.0] - 2026-02-11

### ✨ Features

- Manual Silent Mobs: spawn mobs visible only to a specific player or permission
- Global Silent Mode: ALL mobs become private (Wynncraft-style)
- Silent Item Drops: per-player instanced loot
- Silent Regions: area-based mob visibility with wand tool
- PlaceholderAPI integration
- Multi-language support (EN/VI)
- ProtocolLib-based entity visibility (packet-level hiding)
- MythicMobs integration for whitelisting boss mobs
