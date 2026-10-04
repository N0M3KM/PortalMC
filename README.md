# PortalMC

A Fabric mod that brings original Portal-inspired mechanics into Minecraft Java
Edition. Minecraft hosts the entire game. Portal 2 and the Source engine are not
launched, embedded or required.

## Current scope: Phase 0

- Minecraft Java **26.3**, Java **25**.
- Fabric Loader **0.19.5**, Fabric API **0.161.0+26.3**.
- Official Fabric template build layout, unobfuscated Minecraft names, separated
  common and client source sets.
- `portalmod:hello_world`, displayed as **PortalMC Test Token**, in the Tools &
  Utilities creative tab and searchable in the creative inventory.
- An original placeholder texture, English translation and item model.
- No movement replacement, character replacement or functional portals yet.

The template is pinned to revision
`44465cb0eb83932c72ece5934d32ddfc758802ed`. Its Gradle wrapper is **9.7.1** and
Loom setting is **1.18-SNAPSHOT**. The snapshot can change upstream; see
`docs/build-provenance.md` for the resolved version and validation record.

## Build and run

Install a JDK 25 and ensure `java -version` reports 25. A separate Gradle install
is unnecessary. In PowerShell:

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
```

On Linux/macOS use `bash ./gradlew build` and `bash ./gradlew runClient`.
The mod JAR is `build/libs/portalmc-0.1.0.jar`; the `-sources.jar` is for developers.
Install the mod and the matching Fabric API on **both client and server**.
Do not put the sources JAR in `mods`.

The development client uses `run/`. Create a creative test world with commands
enabled and run:

```text
/give @s portalmod:hello_world
```

The development dedicated server uses the separate, ignored `run-server/`:

```powershell
.\gradlew.bat runServer
```

Read the Minecraft EULA linked by the server. If you accept it, set `eula=true`
in `run-server/eula.txt`, then rerun. Do not commit test worlds, account data,
server properties, logs, or local EULA acceptance.

## Phases

0. Scaffold and verify item registration, client startup and dedicated startup.
1. Source-style movement, shared prediction/server simulation, documented
   constants, configuration and conditional fall protection.
2. Original gun view model and orange-jumpsuit/long-fall-boots character.
3. Server-validated gun placement, custom portal rendering, collision/traversal
   and remote chunk synchronization.
4. Faith plates, bridges, redstone puzzles and cubes, only after Phase 3 is stable.

Each phase must pass its checks before starting the next. See
`docs/testing/phase-0.md`, `docs/architecture.md` and `docs/asset-provenance.md`.
