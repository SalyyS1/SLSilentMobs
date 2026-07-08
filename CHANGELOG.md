# Changelog

All notable changes to SLSilentMobs will be documented in this file.

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
