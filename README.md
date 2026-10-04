# PortalMC

A Fabric mod that brings Portal-style mechanics into Minecraft Java
Edition. Minecraft hosts the entire game. Portal 2 and the Source engine are not
launched, embedded or required.

## Current scope: movement, models, portals and fidelity layers (0.5.2)

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
- Optional actual Portal 2 gun and Chell models read directly from your own
  local installation into memory. No Valve files are shipped or exported.
- Left click places blue, right click places orange. Server-validated 1x2 wall,
  floor and ceiling portals support players, fitting mobs, items and projectiles.
- Destination views, bounded recursion, transformed momentum, server chunk
  tickets, remote client chunks and native remote entity tracking.

Version 0.5.2 fixes portal-gun cooldown after respawn and loads the authored Chell
animation library (`player_animations.mdl` / `.ani`) from the local Portal 2 install.
Idle, directional movement, crouch, jump, aim, fire and landing use the original
clips when available. This replaces the procedural arm/leg poses for imported
Chell. See [the authored-animation and respawn report](docs/testing/respawn-authored-0.5.2.md).

Version 0.5.1 fixes opaque material alpha/eye iris loading, restores the gun glass
and authored colour skins, removes first-person hands, adds a dual-colour gun
crosshair and uses planted-foot leg IK for crouching and walking. See
[the repair report](docs/testing/visual-fixes-0.5.1.md). No game was launched for this repair.

Version 0.5.0 adds joint-aware gripping poses, smooth movement/body animation,
original fallback meshes, gun mechanisms/color feedback, animated oval
surfaces and capped original particles, reduce-motion camera effects, an F8
physics HUD and conservative/adaptive portal-render budgets. This revision was
built and unit-tested without launching Minecraft; visual checks are pending.

Use **F8** or `/portalpos` for measurements. `/portalpos reset` resets telemetry;
`/portalpos debug` toggles render timing. For existing client settings, run
`/portalpos performance` to apply and save the conservative view profile. New
client effects live in `config/portalmod-visuals.json`; each has a toggle.
See [the seven-package report and manual checklist](docs/testing/fidelity-0.5.md).

Portal rendering is a bounded v1 implementation. Full visual polish and
dedicated multiplayer/rendering-mod validation remain before the stretch phase.

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

For this checkout, double-click **Launch-PortalMC.cmd**, or run:

```powershell
cd 'D:\Portal with Minecraft'
.\Launch-PortalMC.cmd
```

The launcher uses the local Gradle cache and the installed `D:\Java` JDK when
available. It starts Minecraft with the current source changes every time.

On Linux/macOS use `bash ./gradlew build` and `bash ./gradlew runClient`.
The mod JAR is `build/libs/portalmc-0.5.2.jar`; the `-sources.jar` is for developers.
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
2. Gun view model and character; optional local Portal 2 imports as requested.
3. Server-validated gun placement, custom portal rendering, collision/traversal
   and remote chunk synchronization.
4. Faith plates, bridges, redstone puzzles and cubes, only after Phase 3 is stable.

WASD and mouse steer; Space jumps; Shift crouches. Hold Space for automatic
bunny hopping with the default config. Strafing while turning in the air can
increase speed. Sprint does not add a separate speed boost. Gun fall protection
requires holding it in either hand; boots protection requires the feet slot.
The gun now fires portals. Aim at two unobstructed full-block faces made from
white concrete, quartz, smooth quartz, calcite, stone, polished andesite or iron.
Place both colors, then walk/jump/fall into an aperture. A floor/ceiling portal's
long axis follows your shot heading, and extends from the hit block to the next
block in that direction. Supporting blocks remain intact; breaking one removes
the portal. Each owner has one pair, reset when they disconnect.

These items are available through creative
inventory or commands; crafting and durability are future work.

The server owns movement settings. Edit its JSON and run `/portalmod reload`.
Mode commands persist `enabled` to that file. Invalid values are rejected and
the file is preserved. Movement temporarily returns to vanilla during swimming,
climbing, riding, elytra/creative flight, sleep, powder snow, and levitation or
slow falling. Fall-protection toggles also apply in vanilla mode.

Additional generated configs are `config/portalmod-client.json` (local asset
directory and character/gun visuals), `config/portalmod-portals.json` (server
portal rules) and `config/portalmod-portals-client.json` (appearance, particles and view budgets),
and `config/portalmod-visuals.json` (poses, gun/camera effects and HUD).
Restart after changing those files. `/portalmod reload` reloads movement only.
The model path defaults to `D:\SteamLibrary\steamapps\common\Portal 2`.
Missing local assets use the original procedural gun and character models; movement
and portals still work. Both sides of multiplayer must use matching mod builds.

Run `gradlew.bat test` for movement/queue unit tests and
`gradlew.bat runClientGameTest` for a real client/integrated-server test world.
Game-test code is excluded from the distributable JAR. No dedicated-server EULA
is accepted by these tasks.

See `docs/movement.md` for movement tuning and provenance,
`docs/testing/phase-2.md` for local assets, and `docs/testing/phase-3.md` for
portal tests, controls, budgets and limitations. Each phase
must pass its checks before starting the next. See also `docs/testing/phase-0.md`,
`docs/architecture.md` and `docs/asset-provenance.md`.
