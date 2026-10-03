# Troubleshooting

## The plugin does not load

1. Confirm the server is Paper 26.3.
2. Confirm the server runs on Java 25 or newer.
3. Remove old IP, Infinite Parkour Reborn, IPPlus and IEP jars; only the Eternal Parkour jar should be installed.
4. Read the first exception in the console, not only the final “disabled” message.

## A config change has no effect

- Validate YAML indentation and quoting.
- Check that materials, particles, and sounds exist in your Paper version.
- Run `/ep reload` after editing. It reloads the core, multiplayer, and elytra files.
- Restart the server for world, mode registration, or storage changes.
- Compare the file with a newly generated default after making a backup.

## Players cannot open a menu or join

Check `permissions.enabled` in `plugins/EternalParkour/config.yml`, the matching `default-values` menu entry, and the player's permission nodes. Elytra uses its own `permissions` switch in `plugins/EternalParkour/elytra/config.yml`.

Also confirm `joining: true` in the core config when players should be able to start block parkour.

## Inventory was not restored

When a player joins block or elytra parkour, their inventory, location, gamemode, hunger, flight and effects are backed up to `plugins/EternalParkour/backups/<uuid>.json`. The backup is deleted once they have everything back. If the server stops without shutting down cleanly, the backup is restored automatically the next time the player joins, and they get a message saying so.

To restore a backup by hand, keep the player online, make sure they are not playing, and run:

```text
/ep recoverinventory <player>
```

This shows how many stacks the backup holds. Restoring replaces the player's current inventory and gamemode, so run `/ep recoverinventory <player> confirm` to restore it. The backup is deleted afterwards. The command requires `ip.admin`. It also reads inventory backups left by Infinite Parkour (`inventories/<uuid>`).

If a backup still exists when the player joins parkour again (for example because it couldn't be read), it is kept as `backups/<uuid>.json.unrecovered-<time>` instead of being overwritten, and the console says so. To restore such a file, stop the server and rename it back to `<uuid>.json`; it is restored when the player next joins.

Items handed out during parkour (menu items, the elytra, the duels start item) are marked, and removed from anyone who has one while not playing.

## A migrated file is missing

The migration only copies a file when the destination does not already exist. Compare:

- `plugins/IP/` with `plugins/EternalParkour/`
- `plugins/IPPlus/` with `plugins/EternalParkour/plus/`
- `plugins/IEP/` with `plugins/EternalParkour/elytra/`

Each old folder is imported only once. To import one again, remove its line from `plugins/EternalParkour/.legacy-imports`.

Stop the server before manually copying anything and keep both backups.

## Reporting a bug

Open a [GitHub issue](https://github.com/jaymingxyz/EternalParkour/issues). Include:

- exact Paper and Java versions;
- plugin version;
- startup log and complete exception;
- relevant configuration with passwords removed;
- steps that reproduce the problem.

Do not report bugs in a review, because reviews do not provide enough space to diagnose them.
