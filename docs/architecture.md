# Architecture and phase boundaries

Minecraft is the host. All simulation and rendering run in Java/Fabric. SkyCraft
is conceptual inspiration, not a dependency or a shared-memory engine bridge.

## Environment separation

`src/main` contains registration and, in later phases, configuration, input
payload definitions, deterministic movement math and authoritative portal state.
`src/client` contains client initialization and future prediction, input,
character, camera and portal rendering. Item visual assets also live in
`src/client/resources`, and Loom packages both source sets in the mod JAR.
Common code must not import client
classes. Dedicated startup is a required check for each phase.

## Phase 1 design

Use one solver on client and server. Send sequenced inputs; return authoritative
states with acknowledgements; reconcile and replay pending inputs. Adapt vanilla
movement handling only for players using Portal movement, avoiding duplicate
simulation and invalid vanilla speed checks. Do not globally disable validation.
Network loss or changed collision may still require visible correction.

Ground friction/acceleration, air strafing, jumping, crouching, sliding and steps
belong to the solver. Initially swimming, riding, elytra and flight remain vanilla.
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
