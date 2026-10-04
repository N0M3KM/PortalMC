# Asset provenance

| Asset | Origin | Status |
| --- | --- | --- |
| `textures/item/hello_world.png` | Original procedural pixel art | Phase 0 diagnostic placeholder |
| `models/item/hello_world.json` | Original model declaration using Minecraft's standard generated-item parent | Phase 0 |
| `items/hello_world.json` | Original item model declaration | Phase 0 |
| `lang/en_us.json` | Original English text | Phase 0 |

Regenerate the texture with `tools/generate-placeholder-texture.ps1` on Windows.
No Valve files are inputs to that script or the build.

The user's Portal 2 install at `D:\SteamLibrary\steamapps\common\Portal 2` is not
read by the mod. Any optional future runtime asset loader requires a separate
feature and must never package assets from that installation.
