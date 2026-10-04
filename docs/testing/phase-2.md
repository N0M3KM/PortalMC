# Phase 2: local Portal 2 models

**Historical baseline:** gameplay/visual validation below is for 0.3/0.4.
Version 0.5 adds the [fidelity work packages](fidelity-0.5.md), with builds/unit
checks only and no new in-game validation. Use that report's current controls,
performance defaults, assets and manual checklist.

The user requested the actual gun and Chell models, superseding the earlier
placeholder-only preference for their own installation. The mod reads those
assets from a local Portal 2 installation at runtime. It never launches Portal
2, extracts asset files, sends them to a server, or includes them in Git/JARs.

Configure `config/portalmod-client.json`:

```json
{
  "enabled": true,
  "portal2Directory": "D:\\SteamLibrary\\steamapps\\common\\Portal 2",
  "chellCharacter": true,
  "portalGunModel": true
}
```

Other generated fields tune texture resolution, units, gun position/scale,
idle sway, fire recoil/duration, gun full-bright lighting and procedural walk/arm angles. Restart the
client after changing these settings. Missing installations or unsupported
assets fall back to the original item/Minecraft character visuals with a log
message; a dedicated server never reads these files.

## Results (2026-10-04)

- Build and all 14 existing movement/queue tests pass.
- The actual local MDL 49/VVD 4/VTX 7 files load: view gun 7,290 triangles,
  Chell 21,606 triangles. Companion checksums match.
- Native VTF 7.5 BC1/BC3 base-color textures decode and upload to GPU memory.
- Client integrated game test passes with both asset models loaded, alongside
  all Phase 1 gameplay regressions.
- First-person screenshot visually checked: actual textured portal gun.
- Third-person front screenshot visually checked: upright Chell, correct facing,
  lowered arms, orange trousers and long-fall boots.
- Test assets/screenshots and local install data remain outside the distributable.

Format research: [SourceIO's supported formats](https://github.com/REDxEYE/SourceIO)
and its MDL/VVD/VTX definitions. Readers are independently implemented Java;
SourceIO, Blender and the Source engine are not runtime dependencies.

## Manual checklist and limitations

1. Launch `gradlew.bat runClient`, give yourself `portalmod:portal_gun` and hold it.
   Confirm its model, idle sway and texture; inspect F5 front/back views of Chell.
2. Walk, crouch, jump and turn. Check procedural leg/arm/head animation and model
   scale. The collision hull remains Minecraft's.
3. Disable `chellCharacter` or `portalGunModel`, restart, and confirm fallback.
4. Point `portal2Directory` at a missing folder, restart, and confirm the client
   still runs with a clear log message. Restore the path afterward.
5. Test the intended rendering-mod combination and multiplayer separately.

Animation is original procedural Minecraft animation, not playback of Valve's
animation sequences. Source material shaders, facial flexes, jiggle bones and
glass are not reproduced. The player replacement currently suppresses vanilla
cosmetic/equipment layers and name tags. Gun inventory/dropped/third-person item
visuals remain the original sprite; the imported model replaces first-person
holding. Recoil is triggered by the portal shot handler. All players
seen by this client use the Chell appearance when enabled; clients choose their
own local visuals and must supply their own installation.

Portal placement, rendering and traversal are covered in `phase-3.md`.
