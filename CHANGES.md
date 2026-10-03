# Changelog

## Eternal Parkour 1.0.0 (unreleased)

### Identity

- Renamed the plugin to Eternal Parkour (`plugins/EternalParkour`). Data from
  `plugins/IP`, `plugins/IPPlus`, and `plugins/IEP` is imported once, without
  modifying the originals.
- The main command is `/eternalparkour` with `/ep`, `/parkour`, and `/witp` as
  aliases. `/ip` was removed. `/ipp` and `/iep` are unchanged.
- Added the `%eternalparkour_...%` PlaceholderAPI identifier. `%witp_...%` keeps working.
- Permission nodes are unchanged and are now declared in `plugin.yml` (all default to op,
  matching previous behaviour).
- Java packages moved from `dev.efnilite.*` to `io.github.jaymingxyz.eternalparkour.*`.
- Removed the update checker, which pointed at the Infinite Parkour Reborn Spigot page and
  reported any version difference as an update.
- Everything players see says Eternal Parkour: the sidebar in block and elytra parkour (it said
  "Infinite Parkour" or "IEP"), the main menu, chat prefixes (multiplayer used "Eternal
  Parkour+") and the three help messages, which now share one layout. Existing locale files that
  still have the old default sidebar title get the new one; titles a server changed are kept.
- The example leaderboard holograms and config file headers are in English and use the new name.

### Menus

- Every menu shares one look: a background without tooltips, a dark bar along the bottom with
  Back or Close in the middle, and page buttons in its corners that show "Page 1 of 2". Titles are
  dark and readable (they were white on the light grey inventory).
- Lists (modes, leaderboards, styles, languages, lobbies, players) show up to 21 entries per page
  inside a border, with a short row centred, and "Nothing here yet" when empty.
- Items describe what they do and end with a hint such as "» Click to play". Descriptions are
  light grey instead of dark grey. The current style and language are highlighted.
- Buttons that go to the previous menu are labelled Back; Close only closes. The main menu got a
  Close button, and the parkour settings menu its own title.
- Switches are lime and grey dyes, and schematic difficulty uses a dye per level.
- Leaderboards show the mode in the title, gold, silver and bronze names for the top three, and
  your own rank next to Back.
- The lobby browser showed hard-coded Chinese text on every server. It is translated now, shows
  the mode's name, and says when a full lobby can only be spectated.
- The elytra style menu placed one style under the Random button (so it couldn't be chosen) and
  mixed the rest with the navigation. It is a paged list now. The five spiral styles showed as
  empty slots, because their icon could be an air block.
- Multiplayer menus are available in Chinese (the translation was never extracted).

### Paper 26.3

- Supports Paper 26.3 on Java 25 only (`api-version: '26.3'`). Built with no deprecated API use.
- Worlds: since Paper 26.1, extra worlds live in `world/dimensions/minecraft/<name>`, so
  `delete-on-reload` (and the elytra world cleanup) silently did nothing. Worlds are now deleted
  through Paper's own world path, with a remembered path for cleanup after a crash. Deletion
  refuses any folder that is not that world's own folder.
- Game rules use the new `GameRules` keys.
- New `modules.elytra` option (default `true`) to turn off elytra parkour and its world. The
  elytra entry in the play menu now appears; it used to look for a separate IEP plugin.
- The scoreboard uses Paper's scoreboard API instead of FastBoard, so it no longer depends on
  server internals. While playing, players get their own scoreboard; their previous one is put
  back when they leave.
- Removed the HolographicDisplays hook (that plugin was archived in 2024). The built-in
  holograms still work.
- Removed the Chunky integration. It never pre-generated anything: with Chunky installed, each
  elytra run only looked up Chunky's API, and could fail doing so.
- Removed the Apache Commons Math dependency (only one spline class was used). A small built-in
  spline replaces it, giving bit-identical results, so seeded elytra courses don't change. The
  jar shrank from about 5.0 MB to 2.8 MB.
- Elytra: clearing a course section no longer loads chunks on the server.
- Sounds are looked up by key (`block.note_block.guitar`); old names still work. Sound pitch and
  volume can be decimals.
- Item names, lore, titles and menu titles use Adventure components. Items glow with the glint
  override instead of a hidden enchantment, and custom model data is only set when configured.

### Fixes

- Security: parkour chat modes parsed players' messages as MiniMessage, so players could add
  click actions, hover text or fake formatting. Messages are now inserted as plain text.
- Chat uses Paper's `AsyncChatEvent`, and player/session lookups are safe to use from the chat
  thread (they used to iterate unsynchronized maps).
- Session members are kept in join order, so the session owner is the first player that joined
  (it used to be whichever player the hash map returned first).
- Menus: items could be shift-clicked into menus (and deleted) or pulled out with double-click.
  Menus now block every item movement. Closed menus no longer leak event listeners.
- `/ep recoverinventory` never actually gave items back. It now restores them after a
  `confirm` step, refuses while the player is playing, and deletes the backup afterwards.
- Inventory backups are written in Paper's NBT item format and deleted once the inventory is
  given back, so an old backup can't be restored over items the player moved elsewhere. A
  backup that was never restored (e.g. after a crash) is kept instead of overwritten. Old
  Infinite Parkour backups can still be read.
- Schematic and legacy backup files are read with a class allow-list, so a crafted file can't
  run code through Java deserialization.
- Plugin shutdown: one failure no longer skips the rest. Players always get their inventory
  and location back (synchronously), then data is saved and storage closed.
- `/ep reload`, `/ep reset` and `/ep recoverinventory` no longer throw from RCON or command blocks.
- `/ep leaderboard` without arguments opens the leaderboards menu directly.
- Setting toggles in the parkour settings menu now show in green/red as intended.
- `Strings.getClosestMatching` always returned the last candidate.
- Pinned ConfigUpdater to a fixed build and updated PlaceholderAPI to 2.12.3.

### Survival (SMP) safety

- Crash-safe backups for both modes: when a player joins block or elytra parkour, their inventory,
  location, gamemode, hunger, flight and effects are written to `backups/<uuid>.json`, and deleted
  once they have everything back. Elytra parkour used to keep the inventory only in memory, so a
  crash during an elytra run lost it for good.
- A backup left behind by a crash is restored automatically when the player next joins (they get
  a message, the console logs it). Before, players came back at spawn in adventure mode holding
  the parkour items, and an admin had to recover their inventory by hand.
- Items handed out during parkour (hotbar menu items, the unbreakable elytra, the duels start
  item) are marked and removed from anyone holding one while not playing: on join, every 5
  seconds, and when they try to pick one up or place one. The barrier "quit" item and the elytra
  can no longer end up in survival.
- The offhand-boat block only applies to parkour players; it used to stop every player on the
  server from placing boats from their off hand.
- When another plugin teleports a player out of parkour (`/home`, `/spawn`, `/tpa`), they get
  their things back and stay where that plugin sent them, instead of being pulled back to where
  they were before joining.
- Joining block parkour (or spectating) during an elytra run ends the elytra run first, so the
  elytra loadout is never saved as the player's "previous" inventory.
- Elytra: the scoreboard came back after switching modes (it was deleted and never recreated).
- A player who left parkour, or was teleported out, within 5 ticks of joining lost their
  inventory: it was given back, and then the delayed parkour setup cleared it and handed out
  parkour items anyway. Delayed setup now does nothing once the player has left. The same check
  stops Speed mode's Speed III effect (which leaving within half a second let players keep for
  about 80 minutes), the duels start item and spectator mode from being applied after leaving.
- Players who had played Super Jump could log in with an empty air bar that didn't refill on land,
  and start drowning as soon as they were in water. Super Jump raised their maximum air to
  100,000,000 and never lowered it; on land air then climbed past what the save format holds and
  came back negative on the next login. Super Jump no longer changes it, and players affected by
  an earlier version are repaired when they join.
- `/ep recoverinventory` restores the new backups (inventory, gamemode and more) and still reads
  Infinite Parkour's old `inventories/<uuid>` files.
- Focus mode: whitelist entries are now matched as command names. They used to match anywhere in
  the command, so the default `r` entry let almost every command through. The plugin's own
  commands are always allowed (so `/ep leave` works), and focus mode now also applies to elytra players.

### Storage and leaderboards

- MySQL: every query now uses statement parameters (values used to be pasted into the SQL),
  connections come from a small HikariCP pool instead of one connection shared between
  threads, statements are always closed, and scores are written as one batched transaction.
  The SQL prefix and mode names are validated before they are used as table names.
- Resetting a score (`/ep reset`) now also deletes it from MySQL. Before, the row stayed and the
  score came back after a restart.
- Player settings are loaded while the player logs in and cached until they quit, so joining
  parkour no longer waits on the database on the main thread.
- Leaderboards: scores are only changed on the main thread, and lookups use a published sorted
  copy, so PlaceholderAPI and the periodic save can't corrupt or lose scores. The initial load
  is synchronous, so scores set right after startup are no longer wiped by a late async read.
- Run times of an hour or more are stored correctly (they used to wrap back to 00 minutes and
  sort wrongly), and scores with unknown time or difficulty no longer break sorting.
- JSON files (player settings, leaderboards) are written atomically, and a corrupt player file
  gives default settings instead of stopping the player from joining.
- New installs get neutral MySQL defaults (`minecraft` / `change-me` / `eternalparkour`).
- Spectators: finding the closest player runs on the main thread, and spectating an empty
  session is rejected instead of throwing.
- Joining in the parkour world with no other world to send the player to logs an error
  instead of throwing.

### Gameplay and command fixes

- The special-jump and schematic-jump chances in `generation.yml` were swapped (with the
  defaults, special jumps appeared 5% of the time instead of 10%, and schematics 10% instead of 5%).
- `/ep menu` only opened for players *without* permission.
- `/parkour create`, `/parkour lobbies` and `/parkour invite` never worked (the command was
  rewritten without its leading slash). They are now real subcommands of `/ep` and tab-complete.
- Security: `/ipp lobbygm` (an admin command) was usable by everyone while
  `permissions.enabled` was false, the default. It now always requires `ip.admin`.
- Duels: when the lobby owner left before the start, the owner was put into default parkour once
  per other player, instead of those players. Moving them now also keeps the inventory they had
  before joining.
- Duels: the start item could start several countdowns (duplicate courses) when its material
  was changed in the locale files.
- `/ep reload` added a duplicate of every style to the style menu; reloaded styles now replace
  the old ones.
- `/ep reload` reloads everything: the core files, the multiplayer files (which needed a separate
  `/ipp reload`) and elytra parkour's `config.yml`, `rewards.yml` and locales. Elytra had no reload,
  so every change needed a restart; turning elytra modes on or off and the MySQL settings still do.
  `/ipp reload` still works and runs `/ep reload`. If one part fails to reload, the others still do
  and the command says so.
- Blocks the player jumped past without landing on could stay floating in the section forever.
- Landing on a bottom slab now counts as progress immediately.
- The scoreboard (and PlaceholderAPI parsing) updates every 5 ticks instead of every tick. The
  sidebar is only created while it's shown, so players who turned it off keep their normal
  scoreboard and nametag teams.
- Block particles are centred on the jump instead of drawn at its far corner.
- `/ep reset <player>` no longer blocks the server looking up unknown names, and rejects
  invalid UUIDs instead of throwing.
- `/ep forcejoin nearest` no longer picks the player who ran it. Subcommands are
  case-insensitive, and tab completion matches from the start of words.
- `/ipp` tab completion was off by one and suggested nothing.
- Schematics are read on the main thread and written in the background (they were read from
  the world on another thread). Saving an all-air selection is refused.
- Elytra: course blocks are sent without loading chunks on the server, and scores with equal
  points are now ranked fastest first.
- Elytra: the course no longer vanishes partway into the first climbing section (around 1300–1600
  blocks in). Entering a climb sent the whole section at once, including chunks the client hadn't
  loaded yet, and the client throws those blocks away. Course blocks now wait until the player has
  their chunk. This also fixes gaps at the start of the course right after joining, gaps when
  flying fast or with a low view distance, and obstacle mode showing no obstacles at all.
- Elytra: the next section and the climb boost are triggered by position, so a fast player flying
  off-centre can no longer skip them.
- Elytra: climbs pushed the player a little harder every tick for the whole climb, so the speed
  kept rising to about 7 blocks a tick (four times a firework rocket's), and pushed along the
  straight line between the climb's ends, which the curving pipe strays 20 to 50 blocks from, so
  players had to fight it. A climb now works like a rocket held for the whole climb: it pushes
  where the player looks, and settles at the speed they came in with, or a rocket's (34 blocks a
  second) if that was slower. Speed Demon scores no longer jump on every climb, and Min Speed
  doesn't count a climb as too slow.
- Elytra: resetting a score also deletes it from MySQL, leaderboards can be saved while players
  play without errors, and corrupt settings files no longer stop a player from playing.
- Hologram cleanup reacts to entities loading, which Paper does separately from chunks.
- `holograms.yml` came with two holograms enabled at a world from the upstream author's server,
  so every new install logged two warnings. They are now disabled examples.
- Clicking a multiplayer invite ran `/ip:ip join`, which no longer exists after the rename. It
  now runs `/eternalparkour:eternalparkour join`.
- Block and elytra parkour loaded their schematics into the same unsynchronised map from two
  threads at startup, so one could overwrite the other's and leave block parkour without
  schematic jumps.
- Text that mixed MiniMessage with legacy colour codes threw an error on Paper 26.3; for example
  `/iep seed` failed. Legacy codes are converted to MiniMessage first.

## Infinite Parkour Reborn 6.0.0 (upstream, unreleased)

- Merge Infinite Parkour, IPPlus, and Infinite Elytra Parkour into one plugin jar.
- Support only Paper 1.21.11, Paper 26.1.2, and Paper 26.2.
- Emit Java 21 bytecode so the same jar can run on Paper 1.21.11 and newer targets.
- Remove the external `vilib`, PaperLib, and VoidGen requirements. The required
  foundation code and native void generator now live inside the plugin.
- Preserve `/ipp` and `/iep` as compatibility commands.
- Migrate missing legacy data into `plugins/IP/plus` and `plugins/IP/elytra`
  without deleting the original folders.
- Give every Bukkit custom event its own `HandlerList`.
- Unregister integrations and PlaceholderAPI expansions during shutdown/reload.

## 5.4.0-paper26

- Initial community port to the Paper 26.1.2 API.
- Replace removed Paper and Adventure API calls.
- Update build tooling for Java 25 class files.

This section is historical. Version 6 supersedes the standalone IPPlus and IEP
builds and no longer requires an independently installed or built `vilib` artifact.
