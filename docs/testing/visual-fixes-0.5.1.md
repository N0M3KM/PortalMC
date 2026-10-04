# Visual repairs: 0.5.1

Minecraft is not launched during this repair. Build and unit tests are the automated
checks; visual alignment, multiplayer and runtime mixin behaviour require manual validation.
Existing movement constants, portal placement, momentum and traversal are unchanged.

The build and all **31 unit tests passed**, including material-alpha/iris loading,
alternating gait phases and planted-foot crouch IK. Gameplay-test sources compile;
no gameplay test was run. A read-only check of the user's installed files confirmed
both arm rigs, both eye meshes, authored gun/glass materials and their alpha usage.

## Findings and changes

- **Eyes:** the local Chell model has two eye meshes and `$Iris` materials. The old
  loader searched only for `$basetexture`, replacing both with grey placeholders.
  `SourceMaterial` now resolves `$Iris` and makes opaque shader-mask alpha opaque.
  This displays the existing iris using authored mesh UVs; Source's EyeRefract
  cornea/refraction/parallax shader is not reproduced. No eye art was invented for
  the original fallback, which has no eye meshes.
- **Left arm:** the local model contains both complete arm rigs and both sides of
  the torso mesh. The old support grip crossed toward the right arm and exceeded
  the left rig's reach. The default support target moves to `(3,55,15)` Source
  units with a separate outward elbow pole. This removes that overlap/overreach;
  visual checks must confirm whether it resolves the reported disappearance.
- **Legs:** replace rigid opposing thigh swings with two-bone foot IK. Feet stay
  at floor height when the body lowers for crouch/landing; knees flex, ankles keep
  their authored upright orientation, crouch widens the stance, and alternating
  walk phases lift one foot while planting the other. Imported and original rigs
  share `LegMotion`. These are original procedural motions, not Valve sequences.
- **First person:** remove the added hand/forearm submissions entirely. The old
  `firstPersonHands` JSON field is obsolete and ignored, including existing files.
  Native hands/held items remain cancelled while holding the gun in either hand.
- **Gun:** the native gun's texture alpha is a shader mask, but the old cutout path
  treated it as visibility. Opaque VMTs now force opaque pixels; genuine translucent
  materials preserve alpha. Restore the authored glass mesh with a translucent
  emissive layer and load the native blue/orange material variants in memory.
  Remove the replacement muzzle tube that obscured the model. Mesh data is still
  read only from the user's installed Portal 2; no Valve files are exported/bundled.
- **Crosshair:** `PortalCrosshairMixin` replaces the vanilla crosshair and attack
  indicator only while holding the gun in first person. Original blue/orange oval
  crescents and state dots follow the owner's server-confirmed portals. Spectator,
  third-person, hidden HUD and the native debug-axis crosshair retain native rules.
  This is a Portal-style recreation, not a copied/pixel-identical Valve HUD asset.

## Settings

`portalmod-visuals.json`: `portalCrosshair=true`, `crosshairSize=23`,
`crosshairBlue=0xff28aaff`, `crosshairOrange=0xffff8a19`, `gunGlassOpacity=0.65`,
`footStrideUnits=7`, `footLiftUnits=4`, `crouchStanceUnits=2`,
`airFootLiftUnits=7`, `jumpFootForwardUnits=5`. All are original estimated defaults.
Existing stride/crouch/air/jump toggles still control the corresponding motion.
On load, old unversioned visual configs migrate only the previous default support
grip values; other custom settings remain intact. Restart after file edits.

## Manual checks (not performed)

1. With local models enabled, inspect both eyes and arms while idle and holding the gun;
   aim up/down and check support-hand alignment and arm/torso overlap.
2. Walk/run/strafe, then crouch while stationary and moving; inspect knee flexion,
   foot lift, floor contact and transitions. Repeat with the original fallback.
3. In first person and either hand, verify no hands/forearms appear; inspect the
   gun silhouette, all three prongs, glass and blue/orange colour changes.
4. Fire, replace and remove both portals; verify each crosshair state indicator.
   Switch items, toggle F1/F5/debug axes and disable `portalCrosshair`.
5. Check a second player, wall/floor/ceiling traversal, landing/air poses, and
   Sodium/Iris separately. Source material shaders are approximated by Minecraft's
   native pipelines; exact Valve lighting is outside this repair.
