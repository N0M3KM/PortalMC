# Architecture and phase boundaries

Minecraft is the host. All simulation and rendering run in Java/Fabric. SkyCraft
is conceptual inspiration, not a dependency or a shared-memory engine bridge.

## Environment separation

`src/main` contains registration, configuration, input payload definitions,
deterministic movement math and authoritative movement. Portal state is future work.
`src/client` contains client initialization, prediction, input and future
character, camera and portal rendering. Item visual assets also live in
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

## Phase 2 design

Original gun model with idle/fire/portal-open states and an original orange
jumpsuit character with long-fall boots. Synchronize appearance to modded peers.
No Valve assets, engine integration or external Portal 2 process.

## Phase 3 design

Custom same-dimension backend, one blue/orange pair per owner. Server validates
fire requests, range, item, surface tag, full-block geometry and aperture
clearance. Rectangles are 1x2 in their surface plane, snapped to wall/floor/ceiling
block grids. Removing a supporting block removes the portal. Reject overlap and
corners; report invalid shots with an original fizzle effect.

Use portal-frame rigid transforms for position, view and velocity with no speed
scaling. Sweep movement segments, continue unused travel at the destination,
and bound crossings per step. Players, fitting mobs, items and projectiles use
the same geometric contract. Large mobs cannot fit a fixed 1x2 aperture.

Entities halfway through need source/destination collision constraints and
clipped rendering on both sides. Do not delete the supporting wall blocks.
Camera roll recovers upright without changing Minecraft gravity.

## Rendering decision

The original Immersive Portals repository is archived and its visible branch is
1.21. No supported 26.3 build was verified. Making it mandatory would require an
upstream port. Use it as a design reference, not a dependency:
https://github.com/iPortalTeam/ImmersivePortalsMod

Its implementation describes stencil recursion, clipping, remote chunk/entity
tracking and split collision:
https://qouteall.fun/immptl/wiki/Implementation-Details.html

Proposed custom implementation renders destination views into pooled targets,
deepest recursion first, then composites with aperture/depth masks. Transform
the camera using traversal geometry; clip against the destination plane and
restore all nested render state. Exact 26.3 renderer hooks and clipping APIs
remain unverified until Phase 3; no compatibility claim is made now.

Server chunk tickets alone are insufficient. Portal viewers need remote chunk
and entity subscriptions and a client cache/render strategy outside the local
view distance. Bound subscription radius and release tickets when portals close.
Cross-dimension portals are outside v1 scope.

## Known risks

- Sodium replaces terrain rendering: remote views/culling need a version-tested
  adapter. Start with the vanilla renderer.
- Iris shader depth, shadows and temporal effects may fail in nested views.
  Unsupported combinations need a diagnostic/fallback, not a claim of support.
- Cap recursion (proposed default 2), total view passes, target resolution,
  subscription radius and memory. Cull invisible views.
- Reject noncoplanar surfaces, corner intersections and overlapping apertures.
- Handle floor support, entity halves, blocked exits and high-speed projectiles
  with collision geometry, not cooldown-only teleportation.

Phase 4 starts only after Phase 3 stability checks pass.
