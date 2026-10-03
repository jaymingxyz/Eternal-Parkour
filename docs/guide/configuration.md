# Configuration

Stop the server and back up the affected file before a large edit. `/ep reload` reloads the configuration and locale files of every module: core, multiplayer, and elytra. Restarting the server is the safest way to apply changes across every subsystem.

## File map

| File | Purpose |
| --- | --- |
| `plugins/EternalParkour/config.yml` | Core behavior, world, storage, permissions, styles, particles, and defaults |
| `plugins/EternalParkour/generation.yml` | Jump distances, chances, and block generation |
| `plugins/EternalParkour/rewards-v2.yml` | Block-parkour and multiplayer rewards |
| `plugins/EternalParkour/schematics/schematics.yml` | Schematic difficulty registration |
| `plugins/EternalParkour/locales/<language>.yml` | Core menus and messages |
| `plugins/EternalParkour/plus/config.yml` | Multiplayer modes and incremental styles |
| `plugins/EternalParkour/plus/locales/<language>.yml` | Multiplayer menus and messages |
| `plugins/EternalParkour/elytra/config.yml` | Elytra modes, settings, storage, and styles |
| `plugins/EternalParkour/elytra/rewards.yml` | Elytra rewards |
| `plugins/EternalParkour/elytra/locales/<language>.yml` | Elytra menus and messages |

## Core configuration

Important sections in `plugins/EternalParkour/config.yml`:

- `joining`: allow players to enter parkour. Disable it on a leaderboard-only server.
- `modules.elytra`: enable elytra parkour (default `true`). It uses a second void world named `iep`. Requires a restart.
- `bungeecord`: automatic join, return server, and non-proxy return location.
- `sql`: enable MySQL and configure host, port, credentials, database, and table prefix.
- `world`: parkour world name, removal on reload, and fallback world.
- `options`: block lead choices, time formatting, and inventory handling.
- `permissions`: enable permission checks and optional per-style permissions.
- `focus-mode`: while playing (block or elytra), only allow this plugin's commands and the whitelisted command names. Recommended on survival servers with trade or auction plugins; see [Survival servers](./survival-servers).
- `styles.list`: named random block palettes.
- `scoring`: point and interval-reward behavior.
- `particles`: generation particle shape/type plus sound, pitch, and volume. Sounds use keys such as `block.note_block.guitar`; old names such as `BLOCK_NOTE_BLOCK_GUITAR` also work.
- `default-values`: menu visibility and default player settings.

::: warning Inventory safety
Keep `options.inventory-saving` enabled. It backs up everything parkour changes about a player to `backups/`, so a crash can't lose their items: the backup is restored automatically when they next join. See [Troubleshooting](./troubleshooting#inventory-was-not-restored).
:::

## Multiplayer configuration

`plugins/EternalParkour/plus/config.yml` contains:

- `send_back_after_multiplayer`: restore the previous location/data after a match, or move players into regular single-player parkour.
- `gamemodes.<name>.enabled`: show or hide each mode.
- Mode-specific values such as `time`, `goal`, `max`, and `island_distance`.
- `styles.incremental`: ordered material palettes used one block after another.

Mode keys are `hourglass`, `lobby`, `practice`, `speed`, `super_jump`, `time_trial`, `wave_trial`, `duels`, and `team_survival`.

## Elytra configuration

`plugins/EternalParkour/elytra/config.yml` contains:

- `join-on-join`, `permissions`, and `time-format`.
- `proxy` return-server settings.
- `mysql` connection settings.
- `mode-settings` for Close, Min Speed, Obstacle, Speed Demon, and Time Trial.
- `settings` entries for style, radius, time, seed, locale, fall, information, and metric units.
- `styles.random` and `styles.incremental` material palettes.

The configured radius must be 3–6, world time 0–24000, and fixed seeds 0–1,000,000. The default seed `-1` selects a random seed.

`/ep reload` applies changes to this file, `rewards.yml`, and the elytra locales, also to courses already in progress. Turning a mode on or off under `mode-settings` and the `mysql` settings need a restart.

## Custom locales

Copy an existing locale file, rename it to the language key you want to use, and translate values without changing their YAML paths. Preserve MiniMessage tags and replacement tokens. Custom elytra style names belong under `styles.names.<key>` in the corresponding elytra locale.
