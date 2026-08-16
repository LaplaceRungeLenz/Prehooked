"""Launch the isolated GTNH 2.9.0-beta-2 client fixture without a GUI launcher."""

from __future__ import annotations

import json
import pathlib
import subprocess
import sys
import uuid


ROOT = pathlib.Path(__file__).resolve().parents[1]
INSTANCE = ROOT / ".compat" / "prism-290" / "instances" / "hooked-gtnh-290"
GAME = INSTANCE / ".minecraft"
RUNTIME = ROOT / ".compat" / "client-runtime-290"
JAVA = pathlib.Path(r"D:\Eclipse Temurin JDK21\bin\java.exe")


def main() -> None:
    manifest = json.loads((RUNTIME / "launch-manifest.json").read_text())
    jvm_args = [
        "-Xms1G",
        "-Xmx8G",
        "-Dfile.encoding=UTF-8",
        "-Djava.system.class.loader=com.gtnewhorizons.retrofuturabootstrap.RfbSystemClassLoader",
        f"-Djava.library.path={manifest['natives']}",
        "--enable-native-access=ALL-UNNAMED",
    ]
    opened = [
        "java.base/java.io",
        "java.base/java.lang.invoke",
        "java.base/java.lang.ref",
        "java.base/java.lang.reflect",
        "java.base/java.lang",
        "java.base/java.net.spi",
        "java.base/java.net",
        "java.base/java.nio.channels",
        "java.base/java.nio.charset",
        "java.base/java.nio.file",
        "java.base/java.nio",
        "java.base/java.text",
        "java.base/java.time.chrono",
        "java.base/java.time.format",
        "java.base/java.time.temporal",
        "java.base/java.time.zone",
        "java.base/java.time",
        "java.base/java.util.concurrent.atomic",
        "java.base/java.util.concurrent.locks",
        "java.base/java.util.jar",
        "java.base/java.util.zip",
        "java.base/java.util",
        "java.base/jdk.internal.loader",
        "java.base/jdk.internal.misc",
        "java.base/jdk.internal.ref",
        "java.base/jdk.internal.reflect",
        "java.base/sun.nio.ch",
        "java.desktop/com.sun.imageio.plugins.png",
        "java.desktop/sun.awt.image",
        "java.desktop/sun.awt",
        "java.sql.rowset/javax.sql.rowset.serial",
        "jdk.dynalink/jdk.dynalink.beans",
        "jdk.naming.dns/com.sun.jndi.dns=ALL-UNNAMED,java.naming",
    ]
    for package in opened:
        target = package if "=" in package else f"{package}=ALL-UNNAMED"
        jvm_args.extend(["--add-opens", target])
    self_test = "--self-test" in sys.argv[1:]
    if self_test:
        jvm_args.append("-Dhooked.selfTest=true")
        qa_worlds = sorted(
            (GAME / "saves").glob("Hooked-QA-*"),
            key=lambda path: path.stat().st_mtime,
            reverse=True,
        )
        if qa_worlds:
            jvm_args.append(f"-Dhooked.selfTest.world={qa_worlds[0].name}")
            print(f"Reusing QA world {qa_worlds[0].name}", flush=True)

    classpath = ";".join(manifest["classpath"])
    offline_uuid = uuid.uuid3(uuid.NAMESPACE_DNS, "OfflinePlayer:HookedQA").hex
    game_args = [
        "--username",
        "HookedQA",
        "--version",
        "GTNH-2.9.0-beta-2-Hooked-QA",
        "--gameDir",
        str(GAME.resolve()),
        "--assetsDir",
        manifest["assets"],
        "--assetIndex",
        manifest["assetIndex"],
        "--uuid",
        offline_uuid,
        "--accessToken",
        "0",
        "--userProperties",
        "{}",
        "--userType",
        "legacy",
        "--tweakClass",
        "cpw.mods.fml.common.launcher.FMLTweaker",
    ]
    command = [str(JAVA), *jvm_args, "-cp", classpath, "com.gtnewhorizons.retrofuturabootstrap.MainStartOnFirstThread", *game_args]
    (RUNTIME / "last-command.json").write_text(json.dumps(command, indent=2))
    print("Launching verified GTNH client runtime", flush=True)
    fml_log = GAME / "logs" / "fml-client-latest.log"
    result = subprocess.run(command, cwd=GAME)
    if self_test:
        # FML truncates fml-client-latest.log during startup, so reading from
        # the pre-launch byte offset can skip the new run's pass marker.
        run_log = fml_log.read_text(encoding="utf-8", errors="replace")
        passed = "HOOKED_SELF_TEST_PASS" in run_log
        failed = "HOOKED_SELF_TEST_FAIL" in run_log
        if not passed or failed:
            print("GTNH Hooked self-test did not pass; inspect fml-client-latest.log", flush=True)
            sys.exit(2)
    sys.exit(result.returncode)


if __name__ == "__main__":
    main()
