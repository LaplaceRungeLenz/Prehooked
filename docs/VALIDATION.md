# Release validation: Prehooked 1.0.3-gtnh

Validation date: 2026-08-17 (Asia/Shanghai)

## Artifact identity

```text
File:    prehooked-1.0.3-gtnh.jar
Size:    96,765 bytes
SHA-256: 61FE3D5D055E6D08294EBE54669B3CB1A7510CBFCBF133E7CE3628B8DC9011C5
Target:  Minecraft 1.7.10 / Forge 10.13.4.1614 / Java 8 bytecode
```

The GTNH 2.9.0-beta-2 client run below used this exact file and checksum.

## Build gate

The build completed successfully with formatting and Checkstyle checks enabled. JUnit reported 18 tests, 0 failures,
0 errors, and 0 skipped across seven suites. Coverage includes chest-to-crosshair launch geometry, the close-wall
window, a simulated approximately 2.03-block stationary release apex, hook speeds, anchor NBT and malformed-input
handling, the two-String Rope recipe, Red Hook geometric constraints, and packet round trips and caps.

Both Diamond Hook chain textures were verified as 16x16 images with their original alpha masks and pixel geometry
unchanged; all visible gray chain pixels were remapped to the cyan-blue palette used by the existing diamond item art.

## GTNH 2.9.0-beta-2 client

- Official full client fixture, Forge `10.13.4.1614`, OpenJDK `21.0.4` through GTNH's modern-Java bootstrap.
- Forge successfully loaded 323 mod containers with Prehooked and Baubles Expanded `2.2.21-GTNH` present.
- An integrated world was opened and the opt-in release self-test completed on the logical server thread.
- Assertions passed: universal Baubles slot 0; equipped-hook lookup; rejection of a second hook; immediate close-wall
  landing in the fire action; Wooden Hook fire from chest height with its tip at the eye-ray crosshair height; pulling,
  momentum-scaled boosted retract and client velocity receipt; Red Hook equip, fire, landing, client-predicted
  movement, server receipt, collision/range bounds, and per-tick smoothness.
- Final marker:

```text
HOOKED_SELF_TEST_PASS baubleSlot=0 woodPlanted=true pullDistance=0.0
pullVelocity=0.11023393543189161 clientReleaseY=0.5842061485025023
redVerticalDelta=1.784025900603595 maxRedCenterStep=0.23052736891295922
```

The client then saved and closed normally with process exit code 0. Strict Prehooked failure count: 0.

## Prior dedicated-server compatibility baseline

| GTNH release | Full mod count | Baubles Expanded | Prehooked 1.0.1-gtnh result |
| --- | ---: | --- | --- |
| `2.9.0-beta-2` | 295 | `2.2.21-GTNH` | Construction and PreInit completed; 0 strict Hooked errors |
| `2.8.4` | 288 | `2.1.19-GTNH` | Construction and PreInit completed; 0 strict Hooked errors |

These earlier dedicated-server discovery tests used Prehooked 1.0.1-gtnh and OpenJDK `21.0.4`. Version 1.0.3-gtnh
does not change its dependencies, mod metadata, or dedicated-server entry points. Its current artifact received the
stronger functional check above: a complete 2.9 integrated-client run with both logical sides active.

## Scope

This is strong compatibility evidence for the named pack releases, not a promise about future GTNH snapshots or every
third-party add-on combination. Re-run the build gate, client self-test, and dedicated-server discovery test after any
runtime-code or dependency change; the artifact checksum must remain identical across all fixtures.
