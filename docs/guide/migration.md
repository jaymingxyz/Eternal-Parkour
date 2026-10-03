# Migration

Eternal Parkour replaces Infinite Parkour (IP), Infinite Parkour Reborn, IPPlus, and Infinite Elytra Parkour (IEP). Do not run any of those jars beside it.

## Before upgrading

1. Stop the server.
2. Back up `plugins/IP/`, `plugins/IPPlus/`, and `plugins/IEP/` (whichever exist).
3. Remove the old plugin jars from `plugins/`.
4. Add the Eternal Parkour jar and start the server.

## Automatic data import

At startup, files that do not already exist are copied as follows:

| Previous location | New location |
| --- | --- |
| `plugins/IP/` | `plugins/EternalParkour/` |
| `plugins/IPPlus/` | `plugins/EternalParkour/plus/` |
| `plugins/IEP/` | `plugins/EternalParkour/elytra/` |

The import never modifies or deletes the previous folders and never overwrites files already present in `plugins/EternalParkour/`. Each previous folder is imported once; imported folders are listed in `plugins/EternalParkour/.legacy-imports`. Remove a line from that file to import that folder again.

## What stays the same

- Permission nodes (`ip.*`, `iep.*`) and their defaults.
- `/parkour`, `/witp`, `/ipp`, and `/iep` still work. `/ip` has been replaced by `/ep`.
- `%witp_...%` and `%iep_...%` placeholders still work. New setups should use `%eternalparkour_...%`.
- The parkour world name (`witp` by default) and the MySQL table names.
- Inventory backups written by Infinite Parkour can still be restored with `/ep recoverinventory`.

## Verification checklist

- Open `/ep`, `/ipp`, and `/iep`.
- Confirm player scores and leaderboards.
- Test custom styles and schematics.
- Check SQL connection settings if MySQL is enabled.
- Join and leave each parkour type and confirm inventory restoration.
- Check the console for configuration or material-name warnings.

Keep the backup until all data and gameplay have been verified. After that, the old data directories may be archived manually.
