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

The client then saved and closed with process exit code 0. Strict Prehooked failure count: 0.

## Dedicated-server discovery

The exact candidate was launched in both official full server fixtures. With each fixture's untouched `eula=false`,
Forge completed mod discovery, construction, and PreInitialization before Minecraft stopped at its normal EULA gate.
This validates dedicated-side class loading without accepting the Mojang EULA on the operator's behalf.

| GTNH release | Full mod count | Baubles Expanded | Prehooked result |
| --- | ---: | --- | --- |
| `2.9.0-beta-2` | 295 | `2.2.21-GTNH` | 1.1.0 construction and PreInit completed; 0 strict Prehooked errors |
| `2.8.4` | 288 | `2.1.19-GTNH` | 1.1.0 construction and PreInit completed; 0 strict Prehooked errors |

The recorded Prehooked PreInitialization time was 0.025 seconds in both fixtures. A full world start, remote custom
configuration exchange, and disconnect restore check require the fixture operator to accept the Mojang EULA and are
reported separately when authorized.

## Scope

This is compatibility evidence for the named pack releases and exact artifact, not a promise about future GTNH
snapshots or every third-party add-on combination. Re-run the build, integrated/LAN test, dedicated discovery, and
remote synchronization test after runtime-code or dependency changes; compare checksums before treating different
files as the same candidate.
