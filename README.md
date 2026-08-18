# Prehooked 1.7.10 / GTNH Port

中文说明见 [docs/Introduction.zh-CN.md](docs/Introduction.zh-CN.md)。

Prehooked is a Minecraft 1.7.10 port of the original
[Hooked](https://github.com/thecodewarrior/Hooked) grappling-hook mod. It keeps the five original hook tiers and
integrates directly with the Baubles Expanded inventory used by GT New Horizons.

## Compatibility

- Primary target: GT New Horizons `2.9.0-beta-2`, Baubles Expanded `2.2.21-GTNH`.
- Stable regression target: GT New Horizons `2.8.4`, Baubles Expanded `2.1.19-GTNH`.
- Minecraft `1.7.10`, Forge `10.13.4.1614`, Java 8-compatible bytecode.
- Client and dedicated server must install the same Prehooked JAR. Baubles Expanded is required.

## Features and controls

- Wooden, Iron, Diamond, Red, and Ender hooks with their original ranges and anchor counts, plus faster Iron and
  Diamond projectiles tuned around Minecraft's terminal free-fall speed.
- Server-authoritative firing, block ray tracing, anchor state, movement, and bounded network input.
- Client prediction plus synchronized rope rendering and direction-oriented 3D fired-hook models for other players.
- Universal Baubles slot support; only one hook can be equipped at a time.
- Persistent planted anchors across ordinary save/logout, with safe cleanup for death, dimension changes, broken
  anchor blocks, and removed hook items.
- English and Simplified Chinese localization; inventory item artwork remains separate from the new fired-hook model
  materials.

Default controls:

- `C`: fire the equipped hook.
- Sneak + `C`: retract the hook currently under the crosshair.
- Jump: retract all hooks, preserve horizontal momentum, and jump higher when released with more current velocity.
- Red Hook: WASD, jump, and sneak move inside the region bounded by planted anchors. Double-tap jump to retract.
- Sneak + right-click a multi-anchor hook item: toggle its one-anchor limit.

The fire key can be changed under Minecraft Controls. See the Chinese installation guide for configuration details.

## Build and verification

Use the Java version selected by `.java-version`, then run:

```powershell
.\gradlew.bat build
```

The build runs formatting, Checkstyle, and 18 unit tests and creates the production and sources JARs under
`build/libs`. The release candidate was also exercised in complete official GTNH client/server fixtures; see
[docs/VALIDATION.md](docs/VALIDATION.md) for the exact evidence and checksum.

The in-game release self-test is opt-in and inert during normal play. Maintainers may start a QA client with
`-Dhooked.selfTest=true`; it must never be enabled in a normal pack launch.

## Credits and licensing

This port is based on upstream commit `2035946b08b15a14224b36d1c46b19cf8391ffd2`. Hooked was created by
thecodewarrior; upstream credits Daniel Astral for the item textures and Terraria for the hook design inspiration.

This is a multi-license repository: Prehooked-specific contributions use the [BSD 2-Clause License](LICENSE), while
code and art copied or adapted from the pinned Hooked revision retain its verbatim
[MIT License](LICENSES/Hooked-MIT.txt). Both notices are embedded in release JARs. The exact code and asset provenance
is documented in [NOTICE.md](NOTICE.md).
