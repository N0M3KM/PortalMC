# Visual defaults and provenance (0.5.0)

Numeric defaults for the new visual layers and performance policy are listed below. Unless noted otherwise, values are **estimated/original design**, not verified Valve values. Every effect has a boolean toggle in its corresponding JSON. Model topology, authored palette and fixed aperture geometry are art/compatibility contracts; they are not movement cvars.

| Config / constant | Default | Source |
| --- | --- | --- |
| `visuals.gripX` | `-9` | Estimated/original design |
| `visuals.gripY` | `55` | Estimated/original design |
| `visuals.gripZ` | `17` | Estimated/original design |
| `visuals.supportGripX` | `-2` | Estimated/original design |
| `visuals.supportGripY` | `55` | Estimated/original design |
| `visuals.supportGripZ` | `20` | Estimated/original design |
| `visuals.armPoleOut` | `1` | Estimated/original design |
| `visuals.armPoleDown` | `-1` | Estimated/original design |
| `visuals.armPoleForward` | `0` | Estimated/original design |
| `visuals.aimPitchLimit` | `65` | Estimated/original design |
| `visuals.headPitchLimit` | `60` | Estimated/original design |
| `visuals.headYawLimit` | `65` | Estimated/original design |
| `visuals.handRadius` | `0.045` | Estimated/original design |
| `visuals.forearmRadius` | `0.052` | Estimated/original design |
| `visuals.viewArmX` | `0.52` | Estimated/original design |
| `visuals.viewArmY` | `-0.65` | Estimated/original design |
| `visuals.viewArmZ` | `0.08` | Estimated/original design |
| `visuals.originalGunLength` | `0.65` | Estimated/original design |
| `visuals.originalGunRadius` | `0.12` | Estimated/original design |
| `visuals.poseResponse` | `12` | Estimated/original design |
| `visuals.aimResponse` | `16` | Estimated/original design |
| `visuals.strideRadiansPerBlock` | `3.5` | Estimated/original design |
| `visuals.runSpeed` | `8` | Estimated/original design |
| `visuals.idleShiftDegrees` | `1.5` | Estimated/original design |
| `visuals.idleFrequency` | `1.1` | Estimated/original design |
| `visuals.strideDegrees` | `32` | Estimated/original design |
| `visuals.kneeDegrees` | `38` | Estimated/original design |
| `visuals.strafeLeanDegrees` | `7` | Estimated/original design |
| `visuals.accelerationTiltDegrees` | `6` | Estimated/original design |
| `visuals.accelerationScale` | `20` | Estimated/original design |
| `visuals.crouchDropUnits` | `8` | Estimated/original design |
| `visuals.crouchBendDegrees` | `25` | Estimated/original design |
| `visuals.jumpBendDegrees` | `20` | Estimated/original design |
| `visuals.airborneBendDegrees` | `12` | Estimated/original design |
| `visuals.landingMaxDropUnits` | `5` | Estimated/original design |
| `visuals.landingBendDegrees` | `22` | Estimated/original design |
| `visuals.landingReferenceSpeed` | `15` | Estimated/original design |
| `visuals.landingRecovery` | `.3` | Estimated/original design |
| `visuals.takeoffRecovery` | `.18` | Estimated/original design |
| `visuals.characterRecoilDegrees` | `5` | Estimated/original design |
| `visuals.characterRecoilRecovery` | `.18` | Estimated/original design |
| `visuals.fireAttack` | `.025` | Estimated/original design |
| `visuals.fireRecovery` | `.16` | Estimated/original design |
| `visuals.fireKickDegrees` | `6` | Estimated/original design |
| `visuals.prongOpenDegrees` | `12` | Estimated/original design |
| `visuals.emitterTravel` | `.025` | Estimated/original design |
| `visuals.glowPulse` | `.25` | Estimated/original design |
| `visuals.bobAmplitude` | `.012` | Estimated/original design |
| `visuals.bobFrequency` | `9` | Estimated/original design |
| `visuals.swayFrequency` | `1.6` | Estimated/original design |
| `visuals.fizzleDuration` | `.3` | Estimated/original design |
| `visuals.fizzleShakeDegrees` | `3` | Estimated/original design |
| `visuals.fizzleFrequency` | `35` | Estimated/original design |
| `visuals.emitterRadius` | `.055` | Estimated/original design |
| `visuals.emitterTipOffset` | `.025` | Estimated/original design |
| `visuals.motionIntensity` | `1` | Estimated/original design |
| `visuals.landingPunchDegrees` | `5` | Estimated/original design |
| `visuals.landingPunchSeconds` | `.32` | Estimated/original design |
| `visuals.firePunchDegrees` | `1.2` | Estimated/original design |
| `visuals.firePunchSeconds` | `.16` | Estimated/original design |
| `visuals.exitPunchDegrees` | `.7` | Estimated/original design |
| `visuals.exitRollDegrees` | `.35` | Estimated/original design |
| `visuals.exitPunchSeconds` | `.22` | Estimated/original design |
| `visuals.exitReferenceSpeed` | `25` | Estimated/original design |
| `visuals.hudMargin` | `6` | Estimated/original design |
| `visuals.hudLineHeight` | `10` | Estimated/original design |
| `visuals.hudColor` | `-1` | Estimated/original design |
| `portal views.recursionDepth` | `1` | Estimated/original design |
| `portal views.maxRenderPasses` | `2` | Estimated/original design |
| `portal views.resolutionScale` | `0.35` | Estimated/original design |
| `portal views.maxResolution` | `768` | Estimated/original design |
| `portal views.sceneRadius` | `12` | Estimated/original design |
| `portal views.maxSceneBlocks` | `4096` | Estimated/original design |
| `portal views.maxSceneEntities` | `32` | Estimated/original design |
| `portal views.maxSceneBlockEntities` | `64` | Estimated/original design |
| `portal views.maxSceneFluidBlocks` | `256` | Estimated/original design |
| `portal views.sceneRefreshTicks` | `10` | Estimated/original design |
| `portal views.maxViewDistance` | `128` | Estimated/original design |
| `portal views.surfaceOffset` | `0.002` | Estimated/original design |
| `portal views.rimWidth` | `0.035` | Estimated/original design |
| `portal views.referenceWidthUnits` | `64` | Estimated request/reference proportion; unverified primary source |
| `portal views.referenceHeightUnits` | `112` | Estimated request/reference proportion; unverified primary source |
| `portal views.ovalSegments` | `48` | Estimated/original design |
| `portal views.particleCap` | `128` | Estimated/original design |
| `portal views.particleBurst` | `16` | Estimated/original design |
| `portal views.openSeconds` | `.22` | Estimated/original design |
| `portal views.closeSeconds` | `.18` | Estimated/original design |
| `portal views.rimPulseFrequency` | `6` | Estimated/original design |
| `portal views.rimPulseStrength` | `.2` | Estimated/original design |
| `portal views.particleDensity` | `.7` | Estimated/original design |
| `portal views.particleLifeSeconds` | `.45` | Estimated/original design |
| `portal views.particleSize` | `.035` | Estimated/original design |
| `portal views.particleSpeed` | `.3` | Estimated/original design |
| `portal views.frameBudgetMs` | `20` | Estimated/original design |
| `portal views.portalBudgetMs` | `4` | Estimated/original design |
| `portal views.overloadFrames` | `3` | Estimated/original design |
| `portal views.recoveryFrames` | `120` | Estimated/original design |
| `portal views.sceneVoxelBudget` | `2048` | Estimated/original design |
| `server.chunkRadius` | `1` | Original working-set policy: 3x3 endpoint chunks |
| `server.entityCaptureRadius` | `2` blocks | Estimated capture policy, backed by projectile crossing-ray capture |
| Source conversion | `40` units/block | Existing documented 72-unit / 1.8-block ratio; unchanged |

Existing Source movement cvar values/sources remain in [movement.md](movement.md). Existing local-model options remain in `portalmod-client.json`: `gunX=0.45`, `gunY=-0.25`, `gunZ=-0.45`, `gunScale=0.025`, `idleSway=0.005`, `fireRecoil=0.06` (estimated framing/visual policy; unchanged). Wrist-based attachment replaces the legacy `gunModelOriginY` offset. `fireDurationSeconds` and `armRestDegrees` are legacy compatibility fields; new motion/IK settings control those behaviors.

Original fallback proportions use Source-scale mesh coordinates, with a 72-unit maximum standing height at 40 units/block. Joint locations, limb radii, finger placement, eight-sided surface topology and palette (`#efc5a4` skin, `#e77227`/`#ed812b` trousers, `#deddda` top, `#eff4f4` braces, `#202a31` boots, `#4c3024` hair) are estimated original art. They are not copied model vertices or claimed Valve constants. Rim/wisp color approximations are `#28aaff` blue and `#ff8a19` orange. The rim has three traveling brightness lobes; the original font is 5x7 artwork in fixed 8x10 slots.

The 64x112 aspect maps to 1.6x2.8 blocks at the existing conversion. Fitting width to 1 block uniformly scales it by 0.625, yielding a 1x1.75 visual oval. Placement and collision retain the existing 1x2 rectangle, as required to preserve traversal.
