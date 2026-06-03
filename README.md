# ProtectedArea

A Minecraft protected area system split into three components that work together:

| Repo | Type | Description |
|---|---|---|
| **ProtectedAreaClient** | Fabric client mod | Area detection, skybox rendering, debug overlay |
| **ProtectedAreaServer** | Fabric server mod | Area management, rules enforcement, packet sync |
| **ProtectedAreaPlugin** | Paper plugin | Same as server mod, for Paper/Spigot servers |

## Features

- **Cube areas** — 3D bounding box zones with rules (NoEnter, NoExit, NoMobSpawn, NoExplosion, NoItemDrop, NoBlockPlace), player limits, priority, exceptions, and custom commands on enter/exit
- **Flat areas** — 2D plane zones that detect when a player crosses from one side to the other
- **Custom skybox** — per-area sky texture with smooth fade in/out
- **Advanced rules** — fine-grained rule control per area
- **Developer API** — Fabric events (`PLAYER_ENTERED_AREA`, `PLAYER_LEFT_AREA`, `PLAYER_CROSSED_FLAT`, etc.) and Bukkit events (`PlayerEnteredAreaEvent`, `PlayerLeftAreaEvent`, `PlayerCrossedFlatEvent`, `AreaRuleBlockedEvent`, etc.)
- **Mod verification** — optional enforcement that players must have the client mod installed
- **YAML persistence** — areas saved per-file, supports cube and flat types

## Documentation

Full documentation and command reference at **[protectedarea.crewved.com](http://node1.crewved.com:25570/)**

## Requirements

- **Client:** Minecraft 1.21.x + Fabric Loader + Fabric API
- **Server (Fabric):** Fabric Loader + Fabric API
- **Server (Paper):** Paper 1.21.x
