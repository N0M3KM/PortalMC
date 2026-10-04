# Phase 3 v1: working portals

**Historical baseline:** gameplay/visual validation below is for 0.3/0.4.
Version 0.5 adds the [fidelity work packages](fidelity-0.5.md), with builds/unit
checks only and no new in-game validation. Use that report's current controls,
performance defaults, assets and manual checklist.

Minecraft 26.3, Loader 0.19.5, Fabric API 0.161.0+26.3, Java 25.
This is an integrated-server gameplay baseline with a bounded destination
renderer. It is not a claim that full visual polish, latency testing or rendering
mod compatibility is complete. Phase 4 remains deferred.

## Approach and implementation

The [Immersive Portals repository](https://github.com/iPortalTeam/ImmersivePortalsMod)
and its [implementation notes](https://qouteall.fun/immptl/wiki/Implementation-Details.html)
describe portal camera transforms, recursion, clipping, remote tracking and
collision concerns. A supported 26.3 dependency was not verified. The custom
backend uses Minecraft's actual 26.3 renderpearl GPU API and native feature
renderer; it does not bundle or depend on Immersive Portals.

The primary LevelRenderer has mutable extracted state. Instead of recursively
re-entering it, this implementation collects bounded native baked block models,
fluids, block entities and entity render states around the exit. It renders them
to pooled color/depth targets, deepest recursion first. A projective shader
samples each destination using the source viewport coordinates. The exit plane
replaces the projection's reverse-Z near plane; the camera position and full
orientation use the same rigid transform as traversal. The compositor draws
against world depth before first-person hands. Source and destination collision
shapes are queried separately for player travel, preserving remaining travel
through thick walls. Native mob/item moves split at the plane; projectile rays
ignore the hidden source segment and test remaining travel at the exit.

The server validates held gun, cooldown, range, tagged full collision faces,
two supporting blocks, clear front cells, pair limits and overlap. Clients send
shot color and aim, never a chosen placement. Invalid shots produce Minecraft
smoke. The gun's local model plays procedural recoil on firing. Supporting blocks
are not deleted. Chunk tickets load/simulate both endpoints; native chunk/light
packets feed an auxiliary client cache. Native entity pairing extends tracking
around viewed destinations, including equipment, metadata and movement updates.

## Automated validation

Run `gradlew.bat build runClientGameTest`. The distributable excludes test code,
test worlds, screenshots, caches and local Portal 2 assets.

- Sixteen unit tests, including all 36 canonical orientation pairs for speed preservation
  and reversible point/velocity transforms, plus hull-fit and crossing direction.
- The Phase 1 integrated movement, input authority, fall-protection and mode
  regression test, and the Phase 2 local-model checks.
- Actual blue/orange shot payloads; linked destination rendering; player traversal,
  exit heading and client/server convergence.
- A high-speed player through a wall with several opaque blocks behind it.
- Native item, moving chicken and arrow traversal through that thick wall,
  including the arrow's transformed momentum.
- Invalid untagged gold surface rejected without replacing a working portal.
- Floor-to-wall fling with falling momentum; ceiling-to-wall jump.
- Destination 482 blocks away, outside native cache range: chunk data, entity
  tracking, rendered view and authoritative player traversal.
- Removing one supporting block removes its portal and unlinks the other color.

The release test also exercises native fluids/chests, recursive facing portals,
a snowball's momentum and a projectile hitting an obstacle at its exit.
`build runClientGameTest` passed on 2026-10-04 for version 0.4.0, including all
sixteen unit tests and the integrated checks above. Destination and recursive
screenshots were also inspected. This does not replace the untested manual cases.

Dedicated gameplay is pending: the user chose to leave the local dedicated
server EULA unaccepted. The 0.4.0 dedicated startup check passed up to that gate
on 2026-10-04 without accepting it; this does not validate an actual multiplayer
session. Network latency/loss, multiple owners,
Vulkan and Sodium/Iris combinations require separate testing.

## Configuration

Server `config/portalmod-portals.json`, synchronized to clients:

| Setting | Default | Meaning |
| --- | ---: | --- |
| enabled | true | Allow shots, openings and traversal |
| shotRange | 64 | Blocks along the server ray |
| shotCooldownTicks | 4 | Minimum interval per owner |
| maxPairs | 64 | Owner pair limit |
| chunkRadius | 2 | Destination chunk radius |
| chunkRefreshTicks | 40 | Remote block/light refresh interval |
| maxRemoteChunks | 128 | Per-client stream/cache cap |
| remoteViewDistance | 128 | Source distance for remote subscriptions |
| entityCaptureRadius | 12 | Nearby native entity sweep capture in blocks |
| entityRefreshTicks | 10 | Additional native entity tracking refresh |
| exitEpsilon | 0.025 | Outward plane clearance in blocks |
| collisionMargin | 0.1 | Swept opening query margin in blocks |
| maxCrossingsPerTick | 4 | Shared player solver crossing cap |
| fizzleParticles | 8 | Smoke count |
| fizzleSpread | 0.08 | Smoke spread in blocks |
| fizzleSpeed | 0.02 | Smoke particle speed |
| fizzleMissDistance | 3 | Miss effect distance from eye |

Client `config/portalmod-portals-client.json`:

| Setting | Default | Meaning |
| --- | ---: | --- |
| enabled | true | Destination rendering/compositing |
| recursionDepth | 2 | Maximum nested destination views; 0 is solid color |
| maxRenderPasses | 6 | Total offscreen views per frame |
| resolutionScale | 0.5 | View target scale relative to the window |
| maxResolution | 1024 | Maximum target side in pixels |
| sceneRadius | 16 | Exit scene radius in blocks |
| maxSceneBlocks | 4096 | Cached model block cap per exit |
| maxSceneEntities | 128 | Entity submissions per view |
| maxSceneBlockEntities | 64 | Native block entity cap |
| maxSceneFluidBlocks | 256 | Native fluid block cap |
| sceneRefreshTicks | 10 | Baked scene refresh interval |
| maxViewDistance | 128 | Visible source portal distance in blocks |
| surfaceOffset | 0.002 | Aperture offset to avoid wall depth fighting |
| rimWidth | 0.035 | Original blue/orange border width in blocks |

Restart after editing these configs. Values have finite/range checks or bounded
client clamps. The fixed 1x2 aperture and axis-aligned snapping are v1's geometry
contract. Extend `portalmod:portalable` with a datapack to choose surfaces.

## Manual checklist

1. Run `Launch-PortalMC.cmd`, create a creative world with commands, and use
   `/give @s portalmod:portal_gun`. Confirm the local gun and F5 Chell model.
2. Make two white concrete walls at least two blocks high, with open space in
   front. Shoot blue with left click and orange with right click. Look from
   different angles: the view should follow your camera, with world occlusion.
3. Walk and bhop through. Confirm heading, momentum and continued control. Try
   crouching and a thick entrance wall; press F5 to inspect the character.
4. Make a two-block floor surface, shoot downward, then fall onto the aperture's
   center. Pair with a wall and check the fling. Repeat with a low ceiling.
5. Throw items and fire projectiles through; lead a fitting mob through. Place an
   obstacle immediately beyond the exit and check impacts/collision there.
6. Replace each color, shoot an invalid material, aim at slab/corner/blocked faces,
   and break each supporting block. Check that rejected shots keep the old pair.
7. Separate endpoints beyond view distance. Check blocks/entities on the other
   side, traverse, and revisit both endpoints. Disconnect/rejoin to check reset.
8. Face portals toward one another. Change recursion/pass/resolution limits and
   confirm finite rendering and acceptable frame time on the intended hardware.
9. Independently test a dedicated session, latency/loss and multiple owners before
   claiming multiplayer stability. Test Sodium/Iris separately.

## Current limits and risks

- Same dimension, one pair per owner; portal state is session-only and is removed
  on owner disconnect. No crafting recipes or stretch mechanics yet.
- The destination scene is bounded, not a full nested vanilla world render. Far
  terrain can end at the scene cap; sky uses fog background. Clouds, full weather,
  particles, terrain ambient occlusion and shader-pack lighting are not replicated.
  Block lighting uses neighboring maxima rather than per-face terrain lighting.
  Fluids and native block entities are included; unusual mod renderers remain
  untested. Remote block changes refresh periodically rather than instantly.
- Vanilla OpenGL is the validated renderer. Sodium changes terrain submission;
  this view path uses baked models, but its depth/hooks still need combination
  testing. Iris can replace render targets, depth and lighting; destination views
  will not reproduce its shader effects and could break. Disable client portal
  rendering if needed; disable server portals as well to avoid invisible openings.
- GPU/CPU cost grows with passes and scene complexity. Depth is clamped to 4,
  passes to 16 and target sides to 2048. Default targets cost roughly 25 MB at a
  1920x1080 window if all six are used. Lower scene radius, pass count and resolution
  on slower hardware. Reduce pair/chunk limits on a busy server.
- Entire upright hulls must fit. Large mobs and ridden/passenger entities are
  excluded. Corners and shared support blocks are rejected. The reverse side of
  a wall stays solid. A linked floor opening removes ground support for fitting
  entities, so standing over it starts a fall; an unlinked portal stays solid.
- Entities transition at their hull center; there are no duplicated, independently
  clipped halves across both surfaces yet. Player interpolation is reset on a
  predicted crossing, and the camera becomes upright immediately, without a
  smooth roll animation. Floor/ceiling camera transitions need further visual
  polish. Vanilla/special-movement players retain native teleport acknowledgement.
- Native mob/item/projectile handling currently permits one native crossing per
  move/sweep; fast multi-portal chains and every projectile subclass are not
  validated. Players have bounded multiple crossings in the shared solver.
- Late portal/chunk packets or different collision state can require prediction
  correction. The integrated convergence test does not prove zero rubber-banding
  on a dedicated network connection.
