# Release validation: Prehooked 1.1.0-gtnh

Validation date: 2026-08-21 (Asia/Shanghai)

## Artifact identity

```text
File:    prehooked-1.1.0-gtnh.jar
Size:    113,690 bytes
SHA-256: 403397B775F6B180470212679CABB4F2ECDCAF226AB5BE5F20481DAB986571E0
Target:  Minecraft 1.7.10 / Forge 10.13.4.1614 / Java 8 bytecode
```

This exact local artifact and checksum were copied into every runtime fixture described below. The tagged GitHub
workflow independently rebuilds the release, attaches both runtime and sources JARs, and generates
`SHA256SUMS.txt` from the JARs actually attached to that release.

## Build gate

`gradlew spotlessApply build` completed successfully with Spotless and Checkstyle enabled. JUnit reported 25 tests,
0 failures, 0 errors, and 0 skipped across ten suites. Coverage includes:

- generated configuration categories, all five per-tier statistics, input clamping, and immutable defaults;
- authoritative override and local restore behavior, plus configuration-packet round trips, malformed protocols, and
  empty payloads;
- hook statistics, chest-to-crosshair launch geometry, close-wall planting, anchor NBT and packet caps;
- release momentum, rope recipes, and single- and multi-anchor Red Hook geometry.

The runtime JAR embeds the project BSD 2-Clause license, provenance notice, and verbatim Hooked MIT license.

## GTNH 2.9.0-beta-2 integrated and LAN runtime

- Official full client fixture with Forge `10.13.4.1614`, Baubles Expanded `2.2.21-GTNH`, and OpenJDK `21.0.4`
  through GTNH's modern-Java bootstrap.
- The client joined an integrated server with 323 mod containers, applied the server's authoritative Prehooked
  settings, and opened that server to LAN on port 25565.
- Assertions covered universal Baubles lookup and second-hook rejection; close-wall and ordinary Wooden Hook fire;
  chest origin and eye-ray landing; player pulling; momentum-scaled server release and client velocity receipt; Red
  Hook equip, landing, client prediction, server input receipt, collision/range bounds, and smooth center movement.
- The corrected Red Hook prediction produced a maximum nonzero center step of exactly 0.25 blocks, below the 0.251
  regression threshold that catches the earlier intermittent approximately 0.42-block snap.
- Final marker:

```text
HOOKED_SELF_TEST_PASS baubleSlot=0 woodPlanted=true pullDistance=0.0
pullVelocity=0.15915788356999913 clientReleaseY=0.5985136930449182
redVerticalDelta=0.25 maxRedCenterStep=0.25 lanPort=25565
```

The client then saved and closed with process exit code 0. The complete integrated/LAN run was repeated twice
independently with the same marker and exit status. Strict Prehooked failure count: 0.

## Dedicated servers and remote synchronization

The exact candidate was launched to a complete `Done` state in both official full server fixtures. The fixture operator
explicitly authorized temporary EULA acceptance for this isolated local test. The two `eula.txt` files were restored
to `eula=false` afterward; GTNH 2.9's temporarily disabled online mode and whitelist were also restored to
`online-mode=true` and `white-list=true`.

| GTNH release | Full mod count | Baubles Expanded | Full-start result |
| --- | ---: | --- | --- |
| `2.9.0-beta-2` | 295 | `2.2.21-GTNH` | `Done (6.612s)`; clean save and exit code 0 |
| `2.8.4` | 288 | `2.1.19-GTNH` | `Done (8.608s)`; clean save and exit code 0 |

GTNH 2.9 then accepted the offline `HookedQA` client and supplied a deliberately different server configuration:
`searchLocations=15`, Red Hook flight disabled, and Wooden Hook values of 3 anchors, 12-block range, 0.75 projectile
speed, 0.35 player pull speed, and 0.65 retract speed. The client asserted every value, disconnected itself, asserted
that its local `searchLocations=1`, Red Hook flight, and default Wooden Hook values had been restored, and exited 0.

```text
HOOKED_CONFIG_SYNC_TEST_PASS serverSearch=15 localSearch=1 disconnectRestore=true
```

Both servers were stopped through their consoles and saved normally. A strict scan of both server logs and the
successful client log found no Prehooked `ERROR`, `FATAL`, failed assertion, or exception.

## Scope

This is compatibility evidence for the named pack releases and exact artifact, not a promise about future GTNH
snapshots or every third-party add-on combination. Re-run the build, integrated/LAN test, dedicated full start, and
remote synchronization test after runtime-code or dependency changes; compare checksums before treating different
files as the same candidate.
