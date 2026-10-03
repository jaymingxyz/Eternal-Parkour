# Eternal Parkour

Eternal Parkour is infinite parkour for Paper: randomly generated block parkour,
multiplayer modes (duels, team survival, time trial, and more), and infinite
elytra courses in one plugin.

It is a fork of [Infinite Parkour Reborn](https://github.com/LostUmbrella58/IP-Reborn),
which merged Efnilite's Infinite Parkour, IPPlus, and Infinite Elytra Parkour.
See [NOTICE.md](NOTICE.md) for attribution.

## Supported servers

- Paper 26.3 on Java 25

Spigot, Folia, older Minecraft releases, and unofficial forks are not supported.

## Installation

1. Stop the server.
2. Remove any old IP, Infinite Parkour Reborn, IPPlus, or IEP jars.
3. Put the Eternal Parkour jar in `plugins/`.
4. Start the server.

On first start, Eternal Parkour copies missing files from `plugins/IP`,
`plugins/IPPlus`, and `plugins/IEP` into `plugins/EternalParkour`. The old
folders are never modified or deleted. See [MIGRATION.md](MIGRATION.md).

## Commands

The main command is `/eternalparkour`, with `/ep`, `/parkour`, and `/witp` as
aliases. The multiplayer and elytra commands are still `/ipp` and `/iep`.
Permission nodes are unchanged (`ip.*` and `iep.*`).

PlaceholderAPI placeholders are available as `%eternalparkour_...%`. The old
`%witp_...%` and `%iep_...%` placeholders keep working.

## Survival servers

Parkour runs in its own void worlds, and everything a player has is backed up
while they play and restored automatically after a crash. See
`docs/guide/survival-servers.md` for recommended settings.

## Documentation

- Server owners: the documentation site in `docs/` (`npm ci && npm run docs:build`).
- Developers: [DEVELOPMENT.md](DEVELOPMENT.md) covers the architecture, threading
  rules, storage, build, tests and current status.
- Changes since Infinite Parkour Reborn: [CHANGES.md](CHANGES.md).

## Building

```powershell
.\mvnw.cmd -B clean verify
```

Requires JDK 25. The shaded plugin jar is written to `target/EternalParkour-<version>.jar`.

This project is licensed under GPL-3.0. See [NOTICE.md](NOTICE.md) for upstream
attribution.
