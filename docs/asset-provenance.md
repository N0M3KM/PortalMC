# Asset provenance

| Asset | Origin | Status |
| --- | --- | --- |
| `textures/item/hello_world.png` | Original procedural pixel art | Phase 0 diagnostic placeholder |
| `models/item/hello_world.json` | Original model declaration using Minecraft's standard generated-item parent | Phase 0 |
| `items/hello_world.json` | Original item model declaration | Phase 0 |
| `lang/en_us.json` | Original English text | Phase 0 |
| `textures/item/portal_gun.png` | Original procedural pixel-art silhouette | Phase 1 placeholder; no firing/model animations |
| `textures/item/long_fall_boots.png` | Original procedural pixel-art silhouette | Phase 1 placeholder; feet equipment for fall protection |
| Gun/boots item and generated model JSON | Original declarations using Minecraft's generated-item parent | Phase 1 |

Regenerate the textures with `tools/generate-placeholder-texture.ps1` on Windows.
No Valve files are inputs to that script or the build.

At the user's explicit Phase 2 request, a client-only runtime reader loads the
gun and Chell mesh/base-color textures from their Portal 2 install at
`D:\SteamLibrary\steamapps\common\Portal 2`. These remain in memory, are not
extracted, are not transmitted, and are never packaged or committed. The JAR
contains only the independently written reader and original placeholders.
See `testing/phase-2.md` for settings, formats, validation and limitations.
