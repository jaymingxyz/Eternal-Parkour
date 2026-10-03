# Commands

Arguments in `<angle brackets>` are required. Arguments in `[square brackets]` are optional.

## Block parkour

`/ep`, `/eternalparkour`, `/ep`, and `/witp` call the same command.

| Command | Description | Permission |
| --- | --- | --- |
| `/ep` | Open the main menu; show help when used outside the game | `ip.main` |
| `/ep help` | Show available commands | — |
| `/ep join [mode/player]` | Join the default mode, a named mode, or another player's session | `ip.join` |
| `/ep leave` | Leave the current session | `ip.quit` |
| `/ep menu` | Open the main menu | `ip.main` |
| `/ep play` | Open mode selection | `ip.play` |
| `/ep leaderboard [mode]` | Open leaderboards | `ip.community.leaderboards` |
| `/ep reload` | Reload every config and locale file: core, multiplayer, and elytra | `ip.admin` |
| `/ep reset <everyone/player/uuid>` | Permanently reset block-parkour scores | `ip.admin` |
| `/ep forcejoin <everyone/nearest/player>` | Force players into the default mode | `ip.admin` |
| `/ep forceleave <everyone/player>` | Force players out of parkour | `ip.admin` |
| `/ep recoverinventory <player>` | Restore an online player's saved inventory | `ip.admin` |

### Block-parkour schematics

These commands require `ip.admin` and must be run by a player.

| Command | Description |
| --- | --- |
| `/ep schematic wand` | Receive the selection wand |
| `/ep schematic pos1` | Set position 1 at your location |
| `/ep schematic pos2` | Set position 2 at your location |
| `/ep schematic save` | Save the selected area with a generated code |
| `/ep schematic paste <file>` | Paste a loaded schematic at your location |

## Multiplayer

| Command | Description | Permission |
| --- | --- | --- |
| `/ipp create` | Open multiplayer mode selection | `ip.multiplayer` |
| `/ipp lobbies` | View active multiplayer lobbies | `ip.active` |
| `/ipp invite` | Open the invitation menu | `ip.invite` |
| `/ipp lobbygm pos1` | Set lobby-selection position 1 | `ip.admin` |
| `/ipp lobbygm pos2` | Set lobby-selection position 2 | `ip.admin` |
| `/ipp lobbygm save` | Save the selected lobby area | `ip.admin` |

`/ipp multiplayer` aliases `/ipp create`; `/ipp lobby` aliases `/ipp lobbies`. The old `/ipp reload` runs `/ep reload`.

## Elytra parkour

| Command | Description | Permission |
| --- | --- | --- |
| `/iep play` | Open the play menu | `iep.play` |
| `/iep leaderboards` | Open elytra leaderboards | `iep.leaderboard` |
| `/iep settings` | Open elytra settings | `iep.setting` |
| `/iep leave` | Leave elytra parkour | `iep.leave` |
| `/iep seed <seed>` | Set a non-negative seed for the current run | `iep.setting.seed` |
| `/iep schematic <x,y,z> <x,y,z>` | Save the selected cuboid as an elytra schematic | Operator |
| `/iep reset <player/uuid> [mode]` | Reset all or one mode's scores | Operator |
