# Installation

## Requirements

Eternal Parkour requires:

| Paper version | Required Java |
| --- | --- |
| 26.3 | Java 25+ |

The plugin does not require `vilib`, PaperLib, VoidGen, or a separate void-world generator.

## New installation

1. Stop the server.
2. Download the jar from [GitHub Releases](https://github.com/jaymingxyz/EternalParkour/releases).
3. Place the jar in the server's `plugins/` directory.
4. Start the server and wait for the plugin to generate its files.
5. Run `/parkour`, `/ipp`, and `/iep` in game to verify each menu.
6. Stop the server before editing YAML files, then start it again.

## First-start files

The main files are generated below `plugins/EternalParkour/`:

```text
plugins/EternalParkour/
├─ config.yml
├─ generation.yml
├─ rewards-v2.yml
├─ locales/
├─ schematics/
├─ plus/
│  ├─ config.yml
│  └─ locales/
└─ elytra/
   ├─ config.yml
   ├─ rewards.yml
   ├─ locales/
   └─ schematics/
```

## Optional plugins

PlaceholderAPI, Vault, Multiverse-Core, and floodgate are detected when installed. They are optional; see [Integrations](./integrations).

::: tip
Keep a copy of the generated defaults before making large changes. YAML indentation matters, and material, particle, and sound names must exist in the Paper version you run.
:::
