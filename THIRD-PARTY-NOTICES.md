# Third-party notices

The Gradle wrapper scripts and JAR are from FabricMC's official example project,
revision `44465cb0eb83932c72ece5934d32ddfc758802ed`, branch `26.3`:
https://github.com/FabricMC/fabric-example-mod/tree/44465cb0eb83932c72ece5934d32ddfc758802ed

The wrapper is Gradle software under Apache License 2.0. Its source headers are
preserved. The license is at https://www.apache.org/licenses/LICENSE-2.0 .
The Fabric example project's surrounding template is CC0-1.0:
https://github.com/FabricMC/fabric-example-mod/blob/44465cb0eb83932c72ece5934d32ddfc758802ed/LICENSE

Minecraft, Fabric Loader, Fabric API, Loom and Gradle distributions are downloaded
as development dependencies. Their licenses remain applicable; they are not
copied into the mod JAR.

Portal and Portal 2 are Valve trademarks. Minecraft is a Mojang/Microsoft
trademark. This project is unofficial and is not endorsed by those companies.
No Valve models, textures, sounds, voice lines, music or engine binaries are
included. The test-token texture is original and reproducible using
`tools/generate-placeholder-texture.ps1`.

An optional runtime loader reads models and textures from the user's own Portal 2
installation. Those assets retain their owners' rights and are not covered by
this project's MIT license. They are decoded in memory and never included in
source control or the distributable. The format readers are original Java code;
no Source engine or third-party model-loader implementation is bundled.
