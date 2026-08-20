# Prehooked configuration

Prehooked creates `config/hooked.cfg` on first launch. Configuration is loaded during startup, so restart the client or
server after changing it.

## General settings

| Property | Default | Range | Purpose |
| --- | ---: | ---: | --- |
| `searchLocations` | `1` | `1`–`15` | Bit mask selecting where equipped hooks are discovered |
| `stateSyncInterval` | `2` | `1`–`20` ticks | Interval between active server hook-state updates |
| `enableRedHookFlight` | `true` | Boolean | Enables movement inside the region bounded by Red Hook anchors |

`searchLocations` uses `1=Baubles`, `2=held item`, `4=hotbar`, and `8=main inventory`. Add values to enable multiple
locations. Search order is always Baubles, held item, hotbar, then main inventory.

## Per-tier hook settings

Each `hooks.<tier>` category (`wood`, `iron`, `diamond`, `red`, and `ender`) contains:

| Property | Range | Purpose |
| --- | ---: | --- |
| `maxAnchors` | `1`–`16` | Maximum simultaneous anchors; Red Hook is limited to `1`–`4` |
| `rangeBlocks` | `1`–`256` | Maximum firing distance and rope length, in blocks |
| `projectileSpeedBlocksPerTick` | `0.01`–`128` | Hook-head extension speed |
| `pullSpeedBlocksPerTick` | `0.01`–`128` | Maximum player speed toward planted anchors |
| `retractSpeedBlocksPerTick` | `0.01`–`128` | Retracting hook-head speed |

Default values:

| Tier | Anchors | Range | Projectile speed | Pull speed | Retract speed |
| --- | ---: | ---: | ---: | ---: | ---: |
| Wood | 1 | 8 | 0.4 | 0.2 | 0.5 |
| Iron | 2 | 16 | 3.6 | 0.4 | 0.5 |
| Diamond | 4 | 24 | 5.0 | 1.0 | 1.0 |
| Red | 4 | 24 | 1.2 | 1.0 | 1.0 |
| Ender | 1 | 64 | 64.0 | 2.25 | 2.25 |

## Multiplayer authority

The server automatically sends `searchLocations`, `enableRedHookFlight`, and every per-tier statistic to a client on
login and respawn. Client prediction and rendering therefore use the authoritative server values even when the
client's local file differs. The local configuration is restored automatically on disconnect for later single-player
worlds or another server. `stateSyncInterval` remains server-only because it controls packet frequency.

Client and server must still install the same Prehooked JAR version.
