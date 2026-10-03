# Eternal Parkour

Eternal Parkour is infinite parkour for Paper: randomly generated block parkour,
multiplayer modes (duels, team survival, time trial, and more), and infinite
elytra courses in one plugin. The focus is safety to be used on a SMP server.

It is a fork of [Infinite Parkour Reborn](https://github.com/LostUmbrella58/IP-Reborn),
which merged Efnilite's Infinite Parkour, IPPlus, and Infinite Elytra Parkour.
See [NOTICE.md](NOTICE.md) for attribution.

## Supported servers

- Paper 26.3 on Java 25

Spigot, Folia, older Minecraft releases, and unofficial forks are not supported.

## Installation

1. Stop the server.
2. Put the Eternal Parkour jar in `plugins/`.
3. Start the server.


## Commands

The main command is `/eternalparkour`, with `/ep`, `/parkour`, and `/witp` as
aliases. 

PlaceholderAPI placeholders are available as `%eternalparkour_...%`. The old
`%witp_...%` and `%iep_...%` placeholders keep working.

## Survival servers

Parkour runs in its own void worlds, and everything a player has is backed up
while they play and restored automatically after a crash. See
`docs/guide/survival-servers.md` for recommended settings.

## Documentation

- Server owners: the documentation site in `docs/`.
- Changes since Infinite Parkour Reborn: [CHANGES.md](CHANGES.md).


This project is licensed under GPL-3.0. See [NOTICE.md](NOTICE.md) for upstream
attribution.
