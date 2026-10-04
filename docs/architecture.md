# Architecture and phase boundaries

Minecraft is the host. All simulation and rendering run in Java/Fabric. SkyCraft
is conceptual inspiration, not a dependency or a shared-memory engine bridge.

## Environment separation

`src/main` contains registration, configuration, input payload definitions,
deterministic movement math, authoritative movement, portal placement, chunk
tickets and entity traversal/tracking. `src/client` contains initialization,
prediction, input, local model loading, camera and portal rendering. Item visual assets also live in
`src/client/resources`, and Loom packages both source sets in the mod JAR.
Common code must not import client
classes. Dedicated startup is a required check for each phase.

## Phase 1 implementation

`SourceMovement` is the shared pure solver. Clients send sequenced axes, view
angles and jump/crouch buttons; they never submit authoritative position or
velocity. The server consumes a bounded queue with a server-time budget, then
sends position/velocity/ground/pose and the acknowledged sequence. The client
compares its prediction and replays unacknowledged commands if needed. Epochs
discard stale inputs across teleports, respawns, dimension and mode changes.

26.3 restores a server player's position during the connection tick, so the
custom server solver runs after that restoration. Player travel and native jump
are suppressed only while custom movement is active; active movement ignores
vanilla position packets on the logical server thread. Native teleport
acknowledgement remains intact. An input timeout resets the stream and applies
neutral gravity every tick. The queue cannot purchase extra simulation time by
sending more packets. Small vanilla velocity cutoffs are suppressed to preserve
the shared equations. Native block collision queries are reused for each substep;
the server applies fall damage, movement statistics and swept block effects.

Reconciliation is necessary when server collision or external forces differ.
Loss, long stalls or newly changed terrain can still cause visible correction;
latency testing is required before claiming smooth dedicated multiplayer.

Ground friction/acceleration, air strafing, jumping, crouching, sliding and steps
belong to the solver. Swimming, riding, climbing, elytra and flight remain vanilla.
Use 40 Source units per block from the requested 72-unit/1.8-block height ratio.
Document every constant with its authoritative source, original units and
conversion; do not assume remembered Source defaults equal Portal 2 defaults.

Server gameplay settings go in `config/portalmod.json` and are synchronized.
Visual settings go in `config/portalmod-client.json`. No unused future config
keys are implemented in Phase 0.

## Phase 2 implementation

At the user's request, the optional client loader reads the actual gun and Chell
models directly from a local Portal 2 installation. Original Java VPK, MDL/VVD/VTX
and VTF readers decode these into memory; no Valve assets are exported or bundled.
Procedural idle, recoil and character poses replace Valve animation sequences.
Missing local assets fall back to original item sprites and the Minecraft player.
See `testing/phase-2.md` for supported formats and visual limitations.

## Phase 3 implementation

Custom same-dimension backend, one blue/orange pair per owner. Server validates
fire requests, range, item, surface tag, full-block geometry and aperture
clearance. Rectangles are 1x2 in their surface plane, snapped to wall/floor/ceiling
block grids. Removing a supporting block removes the portal. Reject overlap and
corners; report invalid shots with an original fizzle effect.

Use portal-frame rigid transforms for position, view and velocity with no speed
scaling. Sweep movement segments, continue unused travel at the destination,
and bound crossings per step. Players, fitting mobs, items and projectiles use
the same geometric contract. Large mobs cannot fit a fixed 1x2 aperture.

The player solver continues remaining travel against destination collision.
Native entity moves split at the plane, and projectile sweeps continue at the
exit using native hit handling. Supporting wall blocks remain intact. Camera
orientation becomes upright immediately without changing Minecraft gravity.
Duplicated clipped entity halves and smooth camera roll remain visual limitations.

## Rendering decision

The original Immersive Portals repository is archived and its visible branch is
1.21. No supported 26.3 build was verified. Making it mandatory would require an
upstream port. Use it as a design reference, not a dependency:
https://github.com/iPortalTeam/ImmersivePortalsMod

Its implementation describes stencil recursion, clipping, remote chunk/entity
tracking and split collision:
https://qouteall.fun/immptl/wiki/Implementation-Details.html

The custom implementation renders bounded destination scenes into pooled targets,
deepest recursion first, then composites against world depth. Camera transforms
reuse traversal geometry; an oblique reverse-Z near plane clips the exit. Native
26.3 baked models, fluids, block entities and entity feature renderers supply the
scene. It does not recursively re-enter the mutable primary LevelRenderer.
Vanilla OpenGL has passed integrated tests; no Sodium/Iris compatibility claim
is made. See `testing/phase-3.md` for budgets and omitted world effects.

Server chunk tickets are paired with remote chunk/light packets, native entity
tracking and a bounded auxiliary client chunk cache outside local view distance.
Subscriptions have configurable radius/caps; tickets are released when portals close.
Cross-dimension portals are outside v1 scope.

## Known risks

- Sodium replaces terrain rendering: remote views/culling need a version-tested
  adapter. Start with the vanilla renderer.
- Iris shader depth, shadows and temporal effects may fail in nested views.
  Unsupported combinations need a diagnostic/fallback, not a claim of support.
- Cap recursion (default 2), total view passes, target resolution,
  subscription radius and memory. Cull invisible views.
- Reject noncoplanar surfaces, corner intersections and overlapping apertures.
- Handle floor support, entity halves, blocked exits and high-speed projectiles
  with collision geometry, not cooldown-only teleportation.

Phase 4 starts only after Phase 3 stability checks pass.

## Fidelity layers (0.5)

Client-only procedural pose, gun, particle, camera and HUD layers extend the
existing renderer. Cosmetic shot results and traversal totals come from small
server payloads; they do not grant placement or change movement authority.
Portal scene refresh is incremental and globally budgeted, with frustum culling,
pooled targets and cached model vertices/poses. See `testing/fidelity-0.5.md`
for the seven packages, budgets, complete tuning provenance and manual checks.
The 1x2 collision contract remains unchanged beneath the visual oval.
# Authored animation extension (0.5.2)

The client can now read the local Chell animation include library and its external
ANI blocks. A bounded common parser decodes frames/sequence grids; client-only
playback selects/blends those poses from tracked movement and authoritative fire
events. Imported Chell bypasses procedural limb IK when this path is enabled.
Root-motion metadata controls visual cadence; it never changes gameplay position.
Respawn firing cooldown is scoped to player incarnation and world clock, with
server-side snapshot/chunk resynchronization on the Fabric respawn event.
