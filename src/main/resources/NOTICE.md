# Third-party notices and asset provenance

## License structure

Prehooked-specific contributions are available under the BSD 2-Clause License in `LICENSE`.

This port also incorporates and adapts code and art from upstream Hooked commit
`2035946b08b15a14224b36d1c46b19cf8391ffd2`. That revision was published under the MIT License. Its license notice is
reproduced verbatim in `LICENSES/Hooked-MIT.txt` and embedded in release JARs at
`META-INF/licenses/Hooked-MIT.txt`. The upstream file contains unfilled copyright placeholders; they are retained
unchanged rather than silently replacing the upstream notice.

Both notices apply to files containing upstream material. The BSD license does not remove or replace the upstream MIT
terms. Newer Hooked branches may use a different license; the pinned 1.12.2 revision above is the source used by this
port.

## Upstream Hooked

- Project: https://github.com/thecodewarrior/Hooked
- Revision used: `2035946b08b15a14224b36d1c46b19cf8391ffd2`
- Original mod author: thecodewarrior (Pierce Corcoran)
- Upstream version metadata: `1.0.2` for Minecraft 1.12.2
- License at the pinned revision: MIT

The Minecraft 1.7.10 implementation is an API-level rewrite, but its gameplay types, hook statistics, recipes,
movement concepts, rendering concepts, registered identity, and portions of its structure are derived from Hooked.

## Art and resource provenance

The following release resources are byte-for-byte copies from the pinned upstream repository:

- `assets/hooked/textures/items/*.png` included by this port;
- `assets/hooked/textures/logo.png`;
- `assets/hooked/textures/hooks/base/{chain1,chain2,hook}.png`;
- both chain textures for the Wood, Iron, Red, and Ender hooks.

The Diamond Hook chain textures are color-modified derivatives of the upstream chain geometry. The five tier-specific
fired-hook `hook.png` material atlases were generated for this port, while retaining the upstream four-pronged hook
concept. `docs/art/world-hook-head-concept.png` is new concept art for this port and is not included in the release JAR.

Upstream metadata credits Daniel Astral for the item textures and Terraria for hook-design inspiration. Those credits
are preserved. No file was imported separately from Terraria; the included bitmaps came from the MIT-licensed Hooked
repository revision identified above.

Minecraft, Forge, GT New Horizons, Baubles Expanded, and Terraria are trademarks or projects of their respective
owners. They are not bundled into this mod artifact.
