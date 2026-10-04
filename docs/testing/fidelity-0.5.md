# Portal 2 fidelity work packages: 0.5.0

Minecraft remains the host. No Portal 2 process or Source engine is launched.
Existing optional imports from a locally owned install remain available; new
forearm/hand geometry, fallback character/gun meshes, motion curves, font and
wisp sprite are independently authored. No Valve assets are in the repository
or JAR. This is a mechanics/visual adaptation, not a complete content port.

## Implementation and root causes

| Package | Changes and principal files |
| --- | --- |
| WP1 | `assets/ArmaturePose.java`: model-space joint transforms propagate to descendants, two-bone IK preserves lengths, head limits. `client/assets/LocalPortalAssets.java`: attach gun at authored wrist, shared grip and view transform. `client/visual/OriginalGeometry.java`: original gripping hand/forearm. `PortalHandMixin`: original gun fallback also renders in first person. |
| WP2 | `client/visual/CharacterAnimation.java`: tick-driven smoothed idle, stride, lean, acceleration tilt, crouch, takeoff, air, impact-scaled landing, head tracking and recoil. `OriginalCharacter.java`: original human fallback with orange trousers, white top and boot braces. Existing tracked player positions/velocity/ground/crouch drive remote poses; `PortalPayloads.ShotFeedback` carries authoritative last-fire events. |
| WP3 | `GunAnimation.java` and `LocalPortalAssets.java`: kick/recovery, emitter/prong mechanisms, active-color glow, idle sway, movement bob and rejection shake. Remote gun color/mechanisms use that player's authoritative shot event rather than the local player's trigger. |
| WP4 | `portal/PortalEffects.java`, `PortalRenderer.java`, `PortalViewConfig.java`: oval surface, emissive pulsing rim, opening/closing, ambient wisps and opening/fizzle/closing bursts. Original radial particle sprite is generated in memory; counts/density/lifetime are bounded. |
| WP5 | `visual/CameraEffects.java`, `CameraEffectsMixin.java`: one camera-only landing/fire/exit layer. Player aim, inputs and velocity are unchanged. Intensity and reduce-motion controls; reduce-motion also removes first-person sway/bob/recoil/fizzle shake. |
| WP6 | `visual/PhysicsHud.java`: F8 and `/portalpos`; positions, angles, horizontal/vertical/total speed, ground/crouch, peak speed, air time, impact, authoritative traversal count, FPS and per-pass portal CPU timing. `tools/generate-hud-font.py`, `font/telemetry.json`, `textures/font/telemetry.png`: original fixed-advance font with optional shadow. |
| WP7 | `PortalRenderer.java`, `assets/SceneOffsets.java`, `PortalRenderBudget.java`: frustum culling, bounded incremental scene builds, pose/vertex reuse, conservative defaults, adaptive recursion, smaller destination working sets. `PortalFrame.intersectsSweep`: cheap swept bounding-box rejection. `PortalServer.java`: stable read-only frame view, smaller paired endpoint captures; actual projectile rays also capture crossings. |

**Arm root cause:** The old upper-arm rotations used global axes and a pivot-sign
heuristic without a grip target. Cancelling vanilla rendering removed its held-item
pose and hands; the imported first-person gun also stayed in its bind pose.
Joint-aware IK and wrist attachment replace those offsets. Mesh proportions,
palm orientation and extreme pitch still require the manual checks below.

**Lag causes identified from code, not a profiler:** Visibility used a portal-center
test rather than a frustum. The scene builder repeatedly visited interior cube
coordinates while enumerating shells. Every view allocated and re-skinned the
large imported meshes. Recursion defaulted to two levels/six passes, and endpoint
capture regions were much larger than the aperture. These costs have been reduced;
an actual performance improvement has not been measured in-game.

## Performance measures

| Requested measure | Status |
| --- | --- |
| Render only visible/facing/in-range portals | Aperture AABB against actual camera/projection frustum, positive facing-plane test, configurable range. Optional culling toggles aid diagnosis. No occlusion-query support yet. |
| Reuse targets | Already present; retained. Pooled targets resize only for window/scale/cap changes and close on disconnect. Per-frame reallocation was not applicable. |
| Reduced resolution | Default scale 0.35, maximum side 768; configurable. |
| Lowest useful recursion / adaptive reduction | Default one destination level and two passes. Sustained frame/portal CPU budget excess removes nested levels; 120 quiet frames restore them. Keeps the base destination view. |
| Reduced destination distance / culling | Default scene radius 12 blocks, entity cap 32; blocks, entities, fluids and block entities have per-view frustum checks. A shared 2048-voxel build budget spreads scene refresh over frames. |
| Reduce allocations | Reuse skinned vertex arrays and poses per entity/model/frame, cached oval unit-circle coordinates and block bounds. Reuse the scene across views and refresh incrementally. Native submission objects and small temporary transforms still allocate. |
| Bounding-box traversal checks | Swept AABB rejection precedes exact plane crossing/full-hull fit. No portal transform or movement constant changed. |
| Smallest practical chunk radius | New server default 1 (3x3 chunks per endpoint; one extra ticket level is the native loading-distance convention). Scene radius 12 fits inside the loaded neighborhood even near chunk edges. Existing server config values remain intact. |
| Per-tick entity scans | Native section queries are restricted to paired endpoints, default capture radius 2, and fallback player/projectile candidates. Mobs/items use their existing movement hook. Projectile crossing rays register their own capture, including starts outside that radius. Per-tick capture remains necessary to know positions before native motion; global remote tracking remains on its existing configured cadence. |
| Visible render cost | HUD gives CPU scene preparation/submission/compositing and each offscreen pass, excluding child-pass time from its parent. It is **not GPU execution time**. FPS is the native counter. |

Existing JSON settings are preserved. For an older checkout's client, use
`/portalpos performance` once to apply and save the conservative view profile.
For the smallest server working set, set `chunkRadius` to `1` and
`entityCaptureRadius` to `2` in `config/portalmod-portals.json`, then restart.

## Tuning and provenance

New client effects: `config/portalmod-visuals.json`; each motion layer/effect has
its own boolean. `reduceMotion=true` removes camera effects and first-person
sway, bob, recoil and fizzle shake. Portal appearance/particles/budgets:
`config/portalmod-portals-client.json`. Restart after file edits. F8 binding is
editable in Minecraft Controls. `/portalpos on`, `off`, `reset`, `debug` are
client commands; reset clears peak, air/impact measurements and the displayed
traversal-count baseline. Runtime HUD toggles do not rewrite the JSON.

| Constant | Default | Source |
| --- | --- | --- |
| Source units/block | 40 | Existing documented 72-unit / 1.8-block conversion; unchanged. See `../movement.md` for movement cvar sources. |
| Portal aspect reference | 64x112 units | **Estimated** request/reference ratio; accessible primary documentation did not verify it. Code explicitly labels it estimated. |
| Fitted oval | 1x1.75 blocks plus rim | Original adaptation: uniform scale 0.625 of 1.6x2.8 blocks at 40 units/block. Existing placement/collision remains 1x2. |
| Fire attack / recovery | 0.025 / 0.16 s | Estimated original curves; not Valve animation timing. |
| Landing recovery / camera punch | 0.30 / 0.32 s; 5 degrees maximum before intensity | Estimated. |
| Opening / closing | 0.22 / 0.18 s | Estimated. |
| Recursion / passes / scale | 1 / 2 / 0.35 | Original performance policy. |
| Frame / portal CPU budget | 20 / 4 ms | Estimated policy, not measured hardware performance. |

The exhaustive default-value table is in `../visual-constants.md`. All new art
dimensions, poses, timings, colors and effect strengths are estimates/original
design; none is claimed to come from an unverified Valve cvar. Existing movement
constants retain their original provenance. Imported model proportions are read
from the user's own model, never written to source or exported as assets.

## Validation and manual checklist

Each package's `gradlew.bat build` finished successfully after correcting API
differences against pinned 26.3 sources. The final build also compiles the existing
integration-test source without running it. All **25 unit tests** passed. The unit suite covers movement/queues,
36 canonical transform pairs, swept rejection, IK lengths/descendants/bind skinning,
effect bounds, adaptive hysteresis and unique near-to-far voxel enumeration.

**Not verified in-game for 0.5.0:** No client, dedicated server or gameplay test
was launched, as requested. The 0.4.0 gameplay results are historical and do not
validate these new effects. Dedicated EULA acceptance remains false.

1. Use `Launch-PortalMC.cmd`, hold the gun, inspect first-person grip/framing and
   F5 front/back poses at low/high pitch. Check hand/gun alignment and head/torso clipping.
2. Inspect idle, walk/run, strafe, acceleration/deceleration and crouch transitions;
   repeat with the original fallback and with optional local models.
3. Jump and land at several speeds; check airborne/takeoff pose, impact-scaled
   squash/recovery, bhopping and absence of animation pops.
4. Fire both colors and an invalid shot: verify mechanism movement, glow, recoil
   and distinct fizzle animation/particles. Observe a second player remotely.
5. Inspect oval/rim/open/close on wall, floor and ceiling; replace colors and break
   supports. Verify particle density/cap and disabled-effect switches.
6. Recheck player/mob/item/projectile momentum, thick-wall travel, blocked exits,
   floor flings, ceiling exits and projectiles starting outside capture radius.
7. Toggle reduce-motion/intensity; confirm steady aim/projectile direction while
   camera effects change, and that third-person posing remains independent.
8. Test F8 and all `/portalpos` commands, units at known speeds, font/shadow,
   landing/air measurements, traversal counts and reset.
9. Compare paired/unpaired, visible/offscreen and recursive views with HUD timing;
   change window size and resolution, apply performance preset and test adaptive recovery.
10. Test distant endpoints and multiple owners in dedicated multiplayer, with latency,
    then test Sodium/Iris separately. Check entity tracking and cache/target cleanup.

## Limits and follow-ups

The oval is a visual mask over the unchanged rectangular traversal contract:
near its corners, an entity can cross outside the visible oval. Upright players
can overlap the smaller visual rim; exact ellipse collision would change existing
traversal and is intentionally deferred. No duplicated entity halves or smooth
portal camera roll were added. The scene is still a bounded native-model renderer,
not a full nested world with sky/weather/shader packs.

Landing impact uses the last downward velocity sampled at 20 Hz; it is an
approximation rather than the exact velocity at a collision substep.

The original fallback meshes are stylized procedural geometry and need an artist
pass for close-up anatomical/detail fidelity. Optional imported rig palm/cloth/twist
deformation, third-person offhand selection, special swimming/flying poses and
every projectile subclass remain unverified. Cosmetic events use reliable custom
payloads; joining after a shot starts with neutral recoil/default gun color until
another event. No new audio was added. Client GPU timing, actual hardware profiling,
occlusion queries, incremental streaming of block updates and multiplayer visual
verification are useful follow-ups. Client-only camera mixin injection/resource
loading still needs a real launch check; compilation does not prove runtime hooks.
