# Respawn and authored Portal 2 animation repair: 0.5.2

## What changed

- `ShotCooldown`, `ClientPortals`: firing used the old player's `tickCount` as its
  cooldown origin. Respawn resets that age to zero, blocking shots until the new
  player reaches the old age. Cooldown now uses the world clock and resets when
  the player incarnation/world changes. Ordinary cooldown and server validation
  remain in force. `PortalServer` clears the old shot/capture state and resends
  the portal snapshot/destination chunks after the pinned Fabric respawn event.
- `SourceAnimations`: a bounded, independently implemented reader for MDL49
  frame animations, external ANI blocks, mixed constant/animated half-vector and
  Quaternion48 channels, sequence blend grids and bone weights. No Source engine
  is linked or executed; imported data stays in memory.
- `LocalPortalAssets`, `AuthoredCharacterAnimation`: the installed Chell mesh
  references `models/player_animations.mdl` and `.ani`; previous versions loaded
  only mesh/bind-pose data and substituted procedural arm/leg motion. The new
  default imports the authored clips and bypasses that arm IK/leg replacement
  for local Chell. Idle, gun/no-gun directional running, crouch idle/walk, jump,
  airborne, aim, fire and landing use those clips. Both arms and twist-bone
  channels remain part of the full skeleton. The previous grip adjustment did
  not resolve the user's report and is not claimed as a verified fix.
- `CharacterAnimation`: supplies tracked velocity, accumulated travel and
  jump/landing/fire timing. Walking cadence comes from encoded travel per cycle,
  rather than the earlier estimated sinusoidal stride. Blending/state selection
  is implemented by this mod; it is not the complete original animation graph.
- Convert Source's X-forward/Y-left/Z-up animation coordinates to the existing
  mesh's Y-up/Z-forward path, then retain its existing render Z reflection.
  Offline checks caught and corrected an initially sideways pose before release.

## Scope and language choice

The user wants Portal 2's actual mechanics/presentation inside Minecraft. Imported
animations move this closer to that target; a procedural approximation is retained
only when the local library is unavailable/disabled or the original fallback is used.
This is still an incomplete adaptation. Source's full animation graph, native IK,
flexes, jiggle/twist controllers, materials and game logic are not running here.
Gun mechanism playback still uses the existing procedural layer; RLE animation
and Quaternion48S formats are explicitly unsupported by the new reader. The local
player animation library inspected for this release uses the supported frame format.

[SkyCraft](https://github.com/chasmlol/SkyCraft) runs Minecraft and Skyrim together
and connects them through shared memory. That architecture would require a running
Portal 2/Source engine for equivalent pass-through behavior, which conflicts with
this project's host-only requirement. [Rust's FFI](https://doc.rust-lang.org/nomicon/ffi.html)
can implement a native boundary, but does not provide missing engine behavior or
animation decoding. Keep Java/Fabric for this repair; consider native code only for
a measured need and a defined integration contract, not as a fidelity shortcut.

Format references: [Valve's public studio layout](https://github.com/ValveSoftware/source-sdk-2013/blob/master/src/public/studio.h),
[packed vector layouts](https://github.com/ValveSoftware/source-sdk-2013/blob/master/src/public/mathlib/compressed_vector.h),
and the SourceIO author's [MDL49 frame-animation reader](https://github.com/REDxEYE/SourceIO/blob/master/library/models/mdl/structs/local_animation.py).
References are not bundled; implementation code is independently written.

## Values and controls

| Value | Setting/data | Provenance |
| --- | --- | --- |
| Animation FPS | 30 | Decoded local Portal 2 player animation descriptors |
| Forward run frames | 37 | Decoded `portalgun_runN` clip |
| Forward run travel | 223.362 units/cycle, about 5.584 blocks | Decoded root-movement data; existing 40 units/block conversion |
| Crouch frames | 118 for idle; 61 for directional walk | Decoded local descriptors |
| Moving-pose blend speed | 0.5 blocks/s | Estimated adaptation policy |
| Animation memory cap | 64 MiB | Original resource policy |

`portalmod-visuals.json`: `authoredCharacterAnimations=true`,
`maxDecodedAnimationMegabytes=64`, `authoredMovingBlendSpeed=0.5`,
`authoredUnitsPerBlock=40`. Existing character/stride/crouch/jump/air/head/recoil
toggles select their corresponding playback layers. Procedural stride/lean/drop
constants govern the fallback; authored clips contain their own poses. Local model
options/install path remain in `portalmod-client.json`. Restart to load changes.

## Verification

Build and **37 unit tests passed**, including respawn/clock/cooldown regression,
mixed frame channels, interpolation, retargeting, malformed-format/memory limits,
packed values, and up/facing-axis conversion. Gameplay-test sources compile only.

A read-only standalone check decoded the actual local library (28 sequences),
sampled 112 finite joint poses, and skinned idle/crouch/run meshes without non-finite
vertices. Up/facing and standing/crouch height assertions passed. Both arm rigs and
substantial vertex-weight influences exist (406 left / 441 right influences).
At the sampled standing/crouch phases, mesh heights were approximately 70.6 / 48.7
Source units. These are offline numerical checks, not proof of on-screen visibility.

**No Minecraft launch, gameplay test or dedicated-server test was performed.**
The server EULA remains unaccepted. Missing-arm visibility, grip, motion fidelity,
shader compatibility and multiplayer must still be inspected in-game.

## Manual checklist

1. Hold the gun, fire both colours, die with a high player age, respawn, reacquire
   the gun and fire immediately. Repeat with/without keep-inventory and in multiplayer.
2. Check the startup log reports 28 authored sequences loaded; inspect both arms
   with/without the gun, including idle, low/high aim and crouch.
3. Walk forward/back/sideways/diagonally; compare the directional run/crouch walk,
   foot contact and cadence. Inspect jumping, falling, firing and hard landings.
4. Observe another player, respawn repeatedly and change dimension; confirm
   remote poses, shot feedback, portal views and both colours remain functional.
5. Recheck movement momentum/traversal and first-person hands/crosshair; disable
   authored animation to check fallback. Test Sodium/Iris independently.
