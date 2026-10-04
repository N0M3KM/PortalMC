# PortalMC

A Fabric mod that brings original Portal-inspired mechanics into Minecraft Java
Edition. Minecraft hosts the entire game. Portal 2 and the Source engine are not
launched, embedded or required.

## Current scope: Phase 1 (0.2.0)

- Minecraft Java **26.3**, Java **25**.
- Fabric Loader **0.19.5**, Fabric API **0.161.0+26.3**.
- Official Fabric template build layout, unobfuscated Minecraft names, separated
  common and client source sets.
- `portalmod:hello_world`, displayed as **PortalMC Test Token**, in the Tools &
  Utilities creative tab and searchable in the creative inventory.
- An original placeholder texture, English translation and item model.
- Source-style ground acceleration/friction, air strafing, crouching, jumping
  and optional hold-to-bhop. Shared simulation uses server-authoritative inputs
  with local prediction and acknowledgement/replay.
- Server-synchronized `config/portalmod.json`, operator mode/reload commands,
  conditional fall protection and original placeholder gun/boots items.
- Character models and functional portals remain later phases.

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
The mod JAR is `build/libs/portalmc-0.2.0.jar`; the `-sources.jar` is for developers.
Install the mod and the matching Fabric API on **both client and server**.
Do not put the sources JAR in `mods`.

The development client uses `run/`. Create a creative test world with commands
enabled and run:

```text
/give @s portalmod:hello_world
/give @s portalmod:portal_gun
/give @s portalmod:long_fall_boots
/portalmod status
/portalmod movement vanilla
/portalmod movement portal
/portalmod reload
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

WASD and mouse steer; Space jumps; Shift crouches. Hold Space for automatic
bunny hopping with the default config. Strafing while turning in the air can
increase speed. Sprint does not add a separate speed boost. Gun fall protection
requires holding it in either hand; boots protection requires the feet slot.
The gun does not fire portals yet. These items are available through creative
inventory or commands; crafting and durability are future work.

The server owns movement settings. Edit its JSON and run `/portalmod reload`.
Mode commands persist `enabled` to that file. Invalid values are rejected and
the file is preserved. Movement temporarily returns to vanilla during swimming,
climbing, riding, elytra/creative flight, sleep, powder snow, and levitation or
slow falling. Fall-protection toggles also apply in vanilla mode.

Run `gradlew.bat test` for movement/queue unit tests and
`gradlew.bat runClientGameTest` for a real client/integrated-server test world.
Game-test code is excluded from the distributable JAR. No dedicated-server EULA
is accepted by these tasks.

See `docs/movement.md` for every tuning value and provenance, and
`docs/testing/phase-1.md` for results, manual checks and limitations. Each phase
must pass its checks before starting the next. See also `docs/testing/phase-0.md`,
`docs/architecture.md` and `docs/asset-provenance.md`.
