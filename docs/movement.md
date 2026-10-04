# Phase 1 movement and configuration

The mod implements independently written Source-style equations in Minecraft;
it does not execute the Source engine. Physics is shared between the local
predictor and the logical server. Minecraft runs at 20 ticks/second; each
movement command covers 0.05 seconds, divided into three substeps by default.

## Units and equations

Using the requested reference height, 72 Source units / 1.8 blocks = **40 units
per block**. Distances and speeds divide by 40; acceleration in units/second²
also divides by 40. Internal velocity uses blocks/second; Minecraft's delta
movement uses blocks/tick, so the adapter divides by 20. Source's vertical Z axis
maps to Minecraft Y. Camera yaw determines the horizontal wish direction.

Ground friction removes `max(horizontalSpeed, stopSpeed) * friction * dt`.
Ground acceleration adds velocity along the requested direction, limited by
the remaining projected wish speed. Air acceleration limits the projection to
`airWishSpeedCap`, while using the full wish speed in the acceleration amount.
This distinction permits air strafing and speed gain without a global walking
speed clamp. Gravity uses half steps around collision; jump impulse is
`sqrt(2 * gravity * jumpHeight)`. Jumping happens before ground friction.
Diagonal input is normalized; the server validates finite axes and view angles.

## Provenance

- **P2:** [Saul's console-variable extraction, Portal 2 build 8151, 2021-03-02](https://github.com/saul/cvar-unhide/blob/master/cvarlist-portal2.md).
  These are the recorded defaults of that build, not a claim about every map or
  later game build. `sv_speed_normal` is the actual normal movement speed;
  `sv_maxspeed` is a separate ceiling on requested speed.
- **SDK:** Valve's published [movement equations](https://github.com/ValveSoftware/source-sdk-2013/blob/master/src/game/shared/gamemovement.cpp)
  and [air-speed cap](https://github.com/ValveSoftware/source-sdk-2013/blob/master/src/game/shared/gamemovement.h).
  SDK behavior is a reference for the implementation, not proof of Portal 2's
  private implementation. No Valve code is bundled.
- **Adaptation:** an explicit PortalMC choice, configurable rather than presented
  as a verified Portal 2 console variable.

## Every configuration field

The generated server file is `config/portalmod.json`. It contains the following
defaults. Settings are synchronized to connected clients; client edits cannot
change server physics. Operator commands `/portalmod reload` and
`/portalmod movement portal|vanilla` reset prediction epochs.

| JSON field | Default | Units / converted meaning | Source |
| --- | ---: | --- | --- |
| `enabled` | true | Replace eligible player movement | Adaptation |
| `sourceUnitsPerBlock` | 40 | Source units/block | Requested 72 / 1.8 ratio |
| `gravity` | 600 | units/s² → 15 blocks/s² | P2 `sv_gravity` |
| `groundAcceleration` | 10 | acceleration coefficient | P2 `sv_accelerate` |
| `airAcceleration` | 10 | acceleration coefficient | P2 `sv_airaccelerate` |
| `friction` | 4 | ground friction coefficient | P2 `sv_friction` |
| `stopSpeed` | 100 | units/s → 2.5 blocks/s | P2 `sv_stopspeed` |
| `maxSpeed` | 320 | wish-speed ceiling → 8 blocks/s | P2 `sv_maxspeed` |
| `normalSpeed` | 175 | units/s → 4.375 blocks/s | P2 `sv_speed_normal`, also `cl_forwardspeed` |
| `airWishSpeedCap` | 30 | units/s → 0.75 blocks/s projection | SDK `GetAirSpeedCap`; not independently verified in P2 |
| `maxVelocity` | 3500 | per-axis units/s → 87.5 blocks/s | P2 `sv_maxvelocity` |
| `jumpHeight` | 45 | units → 1.125 blocks; impulse ≈ 5.81 blocks/s | Adaptation to allow a Minecraft block jump; P2 height not independently verified |
| `stepHeight` | 18 | units → 0.45 blocks | P2 `sv_stepsize` |
| `crouchSpeedMultiplier` | 1/3 | normal wish-speed multiplier | SDK duck scaling; P2-specific value unverified |
| `surfaceFriction` | 1 | friction/acceleration multiplier | Adaptation: uniform surfaces in v1 |
| `useItemSpeedMultiplier` | 0.2 | wish speed while using an item | Minecraft-style adaptation |
| `substeps` | 3 | physics steps/command → nominal 60 Hz | Adaptation to 20 Hz Minecraft |
| `autoBunnyHop` | true | Hold Space to jump on landing; false requires release | Adaptation, not stock P2 autojump |
| `preventFallDamageWithGun` | true | Gun in main hand or offhand | Requested conditional protection |
| `preventFallDamageWithBoots` | true | Boots equipped in feet slot | Requested conditional protection |
| `maxQueuedInputs` | 128 | maximum pending commands per player | Adaptation: bounded memory and replay |
| `maxCatchUpSteps` | 2 | maximum banked simulation commands | Adaptation: earns one credit/server tick |
| `inputGraceTicks` | 6 | ticks without input before stream reset/neutral gravity | Adaptation: default 0.3 s |
| `reconciliationTolerance` | 0.002 | blocks of position mismatch before replay | Adaptation |
| `velocityTolerance` | 0.01 | blocks/s of velocity mismatch before replay | Adaptation |
| `groundProbeDistance` | 0.001 | blocks down to test continued support | Adaptation |

Fixed protocol/algorithm constants are 20 commands/s (Minecraft's tick rate),
`1e-7` collision/standing-query numerical tolerance, and a 16,384-character
settings payload limit. These are timing/precision/security boundaries, not
gameplay tuning. There is no portal-recursion setting until Phase 3 implements
the renderer.

## Scope and differences

The solver uses Minecraft's block/entity collision shapes and its player poses,
not the Source player hull. Default step height is smaller than a half slab:
jump to climb slabs/stairs, or deliberately raise `stepHeight` (e.g. 24 units
for 0.6 blocks). Crouch uses Minecraft's crouched height and view transition;
Source duck-jump hull transitions and ramp surfing are not reproduced exactly.
Surface friction is uniform, so ice, slime and soul-sand behavior differs from
vanilla while custom movement is active. Movement speed/jump status effects and
enchantments do not replace configured movement constants in this phase.

Fluid, ladder, riding, flying, powder-snow and selected status-effect movement
temporarily uses vanilla. The server sends an inactive state and new epoch on
these transitions. Teleports/respawns clear pending movement; external forces
are reconciled from server state. Terrain changes, slow clients, long network
stalls and external forces can require visible correction. The code cannot
promise zero rubber-banding under every condition; dedicated latency/load
testing remains a release gate. Other movement mods can conflict with these
mixins. Phase 1 does not modify the renderer; Sodium/Iris combinations have
not yet been tested.
