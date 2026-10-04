# Phase 0 verification

## Automated/runtime results

Observed on 2026-10-04 with Oracle Java 25.0.2:

| Check | Result |
| --- | --- |
| `gradlew.bat genSources` | PASS: target 26.3 sources generated |
| `gradlew.bat build` | PASS: common and client compile; mod and sources JARs generated |
| Target API inspection | PASS: Identifier, Item.Properties.setId and CreativeModeTabEvents checked in downloaded classes |
| Development client | PASS for startup: both entrypoints ran, SDL window created, item atlas/resource reload completed |
| Integrated server/world startup | PASS in runtime log: 26.3 integrated server started, spawn generated and local player joined |
| Packaged mod inspection | PASS: metadata version expanded to 0.1.0; common/client classes and all item assets present |
| Dedicated mod initialization | PASS: common entrypoint registered the item; client entrypoint did not run |
| Full dedicated server startup | PENDING: Minecraft exits at `eula=false`; user explicitly requested leaving it unaccepted |
| Visual inventory and item persistence | PENDING: not manually observed |
| Multiplayer connection and clean dedicated shutdown | PENDING: requires the full server startup check |

`build` has no unit tests in this phase; its success means compilation/package
validation, not gameplay verification. Client startup logs include expected
development-account authentication/Realms warnings; no real credentials are
provided by `runClient`. The server also reported host Windows performance-counter
warnings, then successfully reached mod initialization and the EULA gate.

The Phase 0 gate remains open because full dedicated and world/item checks are
pending. Phase 1 has not been started. The user authorized pushing this scaffold
with the dedicated-server test pending.

## Manual checklist

1. With Java 25, run `gradlew.bat build`. Confirm `build/libs/portalmc-0.1.0.jar`.
2. Run `gradlew.bat runClient`. Confirm the title screen appears without a crash.
3. Create a creative world with commands enabled. Confirm the integrated server
   starts and normal Minecraft controls still work.
4. Search for `PortalMC Test Token` in the creative inventory. Also find it in
   Tools & Utilities.
5. Run `/give @s portalmod:hello_world`. Confirm its name and blue/orange token
   texture display without a missing-texture pattern. Hold and drop it.
6. Save and reopen the world. Confirm the token persists.
7. Run `gradlew.bat runServer`, read/accept the EULA yourself if necessary, and
   rerun. Confirm `Done` and the common PortalMC initializer in its log; the
   client initializer must not run there.
8. Join that server using the development client and repeat the item checks.
9. Type `stop` in the server console and confirm a clean shutdown.

## Limitations

Phase 0 adds only a diagnostic item. Portal movement, long-fall boots, a portal
gun, character replacement, rendering and traversal are not implemented.
Sodium and Iris compatibility is not yet tested. All manual checks not directly
observed remain pending; Phase 1 must not be started before the Phase 0 gate.
