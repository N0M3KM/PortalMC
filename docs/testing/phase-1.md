# Phase 1 verification

Target: Minecraft 26.3, Fabric Loader 0.19.5, Fabric API 0.161.0+26.3,
Oracle Java 25.0.2. Observed locally on 2026-10-04.

## Automated results

| Check | Result |
| --- | --- |
| `gradlew.bat build` | PASS: common/client compilation, tests and distributable JAR |
| Pure movement unit tests | PASS: 11 tests covering converted speed/jump arc, diagonal normalization, friction, air projection/strafe gain, crouch/ceiling, wall sliding, manual/automatic bhop, validation, and replay with four-tick delayed acknowledgements |
| Input queue unit tests | PASS: 3 tests covering server-time budget, duplicate handling, bounded catch-up, sequence gaps and overflow |
| `gradlew.bat runClientGameTest` | PASS: actual client and integrated server loaded all mixins and custom payloads |
| In-game movement | PASS: normal walking distance, crouched distance, full-block wall collision, jump and repeated hold-to-jump bunny hops |
| In-game authority | PASS: client/server positions converge after walking; forged vanilla position packet (+200 blocks) rejected |
| In-game fall damage | PASS: bare survival fall damages player; equipped boots and held gun protect; disabling gun protection restores damage |
| Mode and teleport resets | PASS: repeated `/tp` commands and Portal/vanilla switching recover active prediction |
| Original placeholder gun rendering | PASS: screenshot displays original gun sprite in hand without missing texture |
| Package contents | PASS: common/client classes, mixin config and original assets present; game-test mod excluded |
| Dedicated initialization | PASS: Phase 1 common initializer and mixin loading reached EULA gate; client initializer absent |
| Full dedicated server and multiplayer gameplay | PENDING: local EULA remains `false` at user's explicit request |
| Real multiplayer latency/jitter/load and two-player observation | PENDING: pure delayed replay is not an end-to-end network impairment test |
| Sodium/Iris, other movement mods, complex terrain/external forces | PENDING |

Commands:

```powershell
.\gradlew.bat build
.\gradlew.bat runClientGameTest
```

The client game-test creates an isolated flat integrated world under
`build/run/clientGameTest`, prepares a stone platform, simulates actual key input,
then saves a screenshot at
`build/run/clientGameTest/screenshots/0000_phase-1-movement.png` and shuts down.
The harness mod in `src/gametest` is client-only and excluded from release JARs.
The test configuration has `enableGameTests=false` and `eula=false`.

Host Windows performance-counter and development-account/Realms warnings are
present, but did not prevent the successful client test. A successful Gradle
`runServer` exit at the EULA gate is **not** a full dedicated startup pass.

## Manual checklist

1. Install `build/libs/portalmc-0.2.0.jar` and matching Fabric API on Minecraft
   26.3, or run `gradlew.bat runClient`. Use a survival world with commands.
2. Walk forward on level ground, stop, then compare diagonal travel. Expect
   acceleration and friction, about 4.375 blocks/s at steady walking speed,
   and no diagonal speed bonus. Sprint does not change configured wish speed.
3. Jump, release forward in the air, and observe retained horizontal momentum.
   Strafe while turning the mouse to gain speed. Hold Space for repeated bunny
   hops; set `autoBunnyHop=false`, reload, and confirm release is required.
4. Crouch with Shift. Expect reduced speed and a lowered eye/pose. Test a low
   ceiling, a wall corner, stairs and slabs. Default 0.45-block step height
   means half slabs/stairs require jumping; raising `stepHeight` is optional.
5. Run `/give @s portalmod:portal_gun` and `/give @s portalmod:long_fall_boots`.
   Fall from a safe test tower while bare, holding the gun in each hand, and
   wearing boots in the feet slot. Items in inventory alone must not protect.
6. Independently disable each fall-protection boolean and reload; repeat the
   falls. Re-enable them afterward. Gun/boots have placeholder item sprites;
   gun firing and visible custom character/boot models are later phases.
7. Run `/portalmod movement vanilla`, then `portal`; confirm each mode responds
   correctly and persists across restart. Edit gravity/acceleration, reload,
   and observe the change. Invalid JSON/ranges must report failure and preserve
   the file; restore valid settings before restarting.
8. Enter/leave water, climb a ladder, ride/dismount, use creative flight/elytra,
   and test slow falling/levitation. Confirm vanilla handoff and recovery.
9. Teleport, die/respawn, change dimensions and reconnect. Confirm no retained
   input stream, stuck crouch, hover, or accidental extra jump.
10. If a dedicated server is separately EULA-accepted by its owner, install the
    mod/API on both sides. Join with two players; verify peer motion, equipment,
    permissions and config synchronization. Repeat movement under latency,
    jitter, packet stalls and server load; check convergence and camera stability.
11. Test pistons, moving entities, knockback, ice/slime/soul sand, damage blocks
    and chunk boundaries. Repeat with the intended Sodium/Iris versions and
    remove other movement mods when investigating conflicts.

## Limitations and gate

This is a tested Phase 1 implementation with dedicated/latency validation still
pending. No unconditional zero-rubber-banding claim is made: differing terrain,
external forces, unsupported combinations and long input stalls can require
correction. The 0.3-second input grace period is configurable. Source hull,
duck-jump transitions, surfing, per-surface friction and every vanilla special
movement behavior are not reproduced. See `../movement.md` for details and
unverified Portal 2-specific constants.

Crafting, custom character/view-model animation, gun firing and portal rendering
are outside this phase. Phase 2 has not been started. Complete the remaining
manual acceptance checks before advancing phases.
