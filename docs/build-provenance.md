# Build provenance

Verified target on 2026-10-04: Fabric's official game metadata lists 26.3 as the
latest stable version. SkyCraft also targets 26.3.

- https://meta.fabricmc.net/v2/versions/game
- https://github.com/chasmlol/SkyCraft/blob/main/fabric/gradle.properties
- Template revision: `44465cb0eb83932c72ece5934d32ddfc758802ed`
- https://github.com/FabricMC/fabric-example-mod/tree/44465cb0eb83932c72ece5934d32ddfc758802ed
- Java 25, Gradle 9.7.1, Loader 0.19.5, API 0.161.0+26.3.
- Loom uses the template's `1.18-SNAPSHOT`. Initial resolution reports Loom
  1.18.2; plugin marker timestamp `1.18-20260916.103615-2`.
- No Yarn or separate Mojang mappings dependency: use unobfuscated game names
  as the official template does.

Snapshot resolution is not immutable. The upstream template revision is pinned,
but an identical dependency resolution on a future date is not guaranteed.

Minecraft/Fabric APIs are checked against downloaded 26.3 classes and by
compilation. The Fabric item tutorial currently describes 26.2 and is used only
as a starting reference, not as proof of 26.3 compatibility:
https://docs.fabricmc.net/develop/items/first-item

Validation results are recorded in `testing/phase-0.md`.
