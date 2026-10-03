# Survival servers

Eternal Parkour can run on a survival (SMP) server next to the normal game. This page explains what it
changes, how players' belongings are protected, and which settings to review.

## What the plugin touches

- **Two extra worlds.** Block parkour runs in `witp` and elytra parkour in `iep`. Both are empty void
  worlds that the plugin deletes and recreates on every restart. Your survival worlds are never changed.
  The one exception is lobby mode, which builds parkour inside an area an admin has set up with
  `/ipp lobbygm`.
- **Nothing is forced on players** with the default settings: they only enter parkour through the menu
  or a command. Keep `bungeecord.enabled` (in `config.yml`) and `join-on-join` (in `elytra/config.yml`)
  set to `false`; either one puts every player into parkour when they join.
- **Chat is untouched** unless a player picks the lobby-only or players-only chat mode in the settings menu.

## Players' belongings

When a player enters block or elytra parkour, the plugin backs up their inventory, location, gamemode,
hunger, flight and potion effects to `plugins/EternalParkour/backups/`. It then gives them the parkour
items. When they leave, everything is given back and the backup is deleted.

- **Server crash.** If the server stops without shutting down cleanly, the backup stays on disk. It is
  restored automatically the next time the player joins, and they get a message saying so. No admin
  action is needed. See [Troubleshooting](./troubleshooting#inventory-was-not-restored) for manual recovery.
- **Teleport commands.** If a player uses `/home`, `/spawn`, `/tpa` or similar while playing, they
  leave parkour, get their items back, and stay where the command sent them.
- **Parkour items can't be kept.** Items handed out during parkour are marked, including the menu items
  (one of them is a barrier block) and the unbreakable elytra. They are removed from anyone who has one
  while not playing: on join, every few seconds, and when they try to pick one up or place one.

::: warning Keep inventory saving on
`options.inventory-saving` must stay `true` on a survival server. With it off there is no backup on
disk, and a crash loses the inventories of everyone who was playing.
:::

## Recommended settings

| Setting | File | Recommendation |
| --- | --- | --- |
| `options.inventory-handling` | `config.yml` | `true`. Players get a clean parkour hotbar, and their items are kept safe. |
| `options.inventory-saving` | `config.yml` | `true` (see above). |
| `focus-mode.enabled` | `config.yml` | `true` if you run auction house, trade, sell or similar plugins. It blocks other commands while playing, so parkour items can't be sold or traded. Add chat commands you want to allow to `focus-mode.whitelist`. |
| `permissions.enabled` | `config.yml` | `false` lets everyone use every player feature; set `true` to control access with `ip.*` nodes. Admin commands always need `ip.admin` (elytra admin commands need operator status). |
| `enabled` | `rewards-v2.yml`, `elytra/rewards.yml` | Rewards are off by default. Interval rewards repeat every few points, so keep money and item rewards small to avoid farming. |
| `modules.elytra` | `config.yml` | Set `false` if you don't want elytra parkour or its world. |

## Other plugins

- **Anti-cheat.** Elytra runs speed players up, and parkour moves them quickly between blocks.
  Exempt the `witp` and `iep` worlds if your anti-cheat flags players there.
- **Combat logging.** Joining parkour moves a player away instantly. If you run PvP, block `/ep`,
  `/parkour`, `/ipp` and `/iep` during combat in your combat plugin.
- **Scoreboards and tab lists.** While the parkour scoreboard is shown, other plugins' nametag and tab
  colours don't apply to that player. They come back when the player leaves or turns the scoreboard off
  in the parkour settings.
- **World management and maps.** Don't import `witp` or `iep` into Multiverse, map renderers or world
  backups; they are recreated on every restart.
