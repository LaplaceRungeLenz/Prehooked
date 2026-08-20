# Changelog

## 1.1.0-gtnh - 2026-08-21

- Made hook lookup locations and each hook tier's range, projectile speed, player pull speed, retract speed, and
  maximum anchor count configurable in `config/hooked.cfg` while preserving all existing defaults.
- Added an immutable, validated gameplay-settings model with explicit units and safe limits; separated retract speed
  from player pull speed and corrected remote retract prediction to match the authoritative server.
- Added server-to-client gameplay configuration synchronization for dedicated, LAN, and integrated servers. Clients
  now use server values for prediction and automatically restore their local configuration after disconnecting.
- Hardened configurable motion by range-clamping the initial and interpolated projectile steps, keeping Red Hook
  geometry at its supported four-anchor limit, and expanding the observer synchronization radius for longer ropes.
- Corrected Red Hook bounded-flight collision coordinates so its synchronized center advances smoothly without an
  occasional prediction snap while the player is still catching up to the previous center.
- Added configuration generation, bounds, override/restore, and network round-trip regression coverage.

## 1.0.3-gtnh - 2026-08-17

- Recolored both Diamond Hook chain ribbons from the Iron Hook's gray metal palette to the cyan-blue palette used by
  the Diamond Hook item texture.
- Lowered the stationary jump-retract apex from approximately 2.5 blocks to approximately 2 blocks while retaining
  momentum-scaled lift.
- Clarified the BSD-2-Clause/MIT multi-license boundary, documented exact upstream asset provenance, and embedded the
  verbatim Hooked MIT notice in release artifacts.

## 1.0.2-gtnh - 2026-08-16

- Made chest-fired hooks converge on the eye ray (and exact crosshair block hit), while clipping the owning player's
  first-person chain near the camera so its wide crossed ribbons no longer cover the view.
- Added immediate first-flight collision detection and owning-client fire prediction, removing the extra tick before a
  hook visibly plants against a wall touching the player.
- Added owning-client jump-retract prediction and a release grace window so stale anchor snapshots cannot fight the
  server velocity update; a stationary release now reaches approximately 2.5 blocks high.

## 1.0.1-gtnh - 2026-08-13

- Renamed the user-facing mod, Gradle project, and release artifacts to Prehooked while retaining the internal
  `hooked` identity for existing-world compatibility.
- Made jump-retract preserve horizontal momentum and add bounded, speed-scaled lift; the server now explicitly
  synchronizes the release velocity to the owning client.
- Moved newly fired hook anchors from eye height to chest height.
- Added local prediction for Red Hook bounded movement while retaining server-side collision and region validation;
  a newly planted single anchor now initializes at the player to avoid a first-input position snap.
- Raised Iron hook projectile speed to 3.6 blocks/tick and Diamond to 5.0 blocks/tick, straddling Minecraft's
  approximately 3.92 blocks/tick terminal free-fall speed.
- Replaced the Rope recipe with a shapeless two-String recipe and retired Plant Fiber from recipes/creative tabs.
- Replaced the fired hook-head billboard with a direction-oriented four-pronged 3D model and five new world-model
  material atlases. Inventory item textures are unchanged.

## 1.0.0-gtnh - 2026-08-13

- Ported Hooked from Minecraft 1.12.2 to Forge 1.7.10.
- Restored Wooden, Iron, Diamond, Red, and Ender hooks with upstream statistics and recipes.
- Added server-authoritative anchors, pulling, boosted retraction, red bounded movement, Ender effects, persistence,
  multiplayer synchronization, and client rendering.
- Integrated hooks as Baubles Expanded universal accessories and enforced a single equipped hook.
- Added configurable hook lookup locations and state synchronization interval.
- Added English and Simplified Chinese localization.
- Added 12 automated tests and an opt-in full-environment client self-test.
- Validated one release binary with GTNH `2.9.0-beta-2` and `2.8.4`.
