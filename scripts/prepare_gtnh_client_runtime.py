"""Prepare a verified Windows x64 runtime from a GTNH Prism component set."""

from __future__ import annotations

import concurrent.futures
import hashlib
import json
import os
import pathlib
import shutil
import time
import zipfile

import requests


ROOT = pathlib.Path(__file__).resolve().parents[1]
INSTANCE = ROOT / ".compat" / "prism-290" / "instances" / "hooked-gtnh-290"
PATCHES = INSTANCE / "patches"
RUNTIME = ROOT / ".compat" / "client-runtime-290"
LIBRARIES = RUNTIME / "libraries"
ASSETS = RUNTIME / "assets"
NATIVES = RUNTIME / "natives"
OLD_CACHE = pathlib.Path(r"D:\MultiMC")
MAX_ATTEMPTS = 12


def sha1(path: pathlib.Path) -> str:
    digest = hashlib.sha1()
    with path.open("rb") as stream:
        while block := stream.read(1024 * 1024):
            digest.update(block)
    return digest.hexdigest()


def allowed(rules: list[dict] | None) -> bool:
    if not rules:
        return True
    result = False
    for rule in rules:
        os_rule = rule.get("os")
        matches = os_rule is None or os_rule.get("name") == "windows"
        if matches:
            result = rule.get("action") == "allow"
    return result


def correct_architecture(name: str) -> bool:
    return "natives-windows-arm64" not in name and "natives-windows-x86" not in name


def maven_path(name: str) -> pathlib.Path:
    parts = name.split(":")
    group, artifact, version = parts[:3]
    classifier = parts[3] if len(parts) >= 4 else None
    filename = f"{artifact}-{version}{'-' + classifier if classifier else ''}.jar"
    return pathlib.Path(*group.split(".")) / artifact / version / filename


def materialize(url: str, expected_sha1: str, expected_size: int, destination: pathlib.Path) -> pathlib.Path:
    destination.parent.mkdir(parents=True, exist_ok=True)
    if destination.exists() and valid_file(destination, expected_sha1, expected_size):
        return destination

    relative = destination.relative_to(LIBRARIES) if destination.is_relative_to(LIBRARIES) else None
    cached = OLD_CACHE / "libraries" / relative if relative is not None else None
    if destination.is_relative_to(ASSETS):
        cached = OLD_CACHE / "assets" / destination.relative_to(ASSETS)
    if cached is not None:
        if cached.exists() and valid_file(cached, expected_sha1, expected_size):
            shutil.copy2(cached, destination)
            return destination

    temporary = destination.with_suffix(destination.suffix + ".part")
    for attempt in range(1, MAX_ATTEMPTS + 1):
        try:
            with requests.get(url, stream=True, timeout=(30, 90), headers={"User-Agent": "Hooked-GTNH-QA"}) as response:
                response.raise_for_status()
                with temporary.open("wb") as stream:
                    for block in response.iter_content(1024 * 1024):
                        if block:
                            stream.write(block)
            if not valid_file(temporary, expected_sha1, expected_size):
                raise RuntimeError(f"checksum mismatch for {url}")
            os.replace(temporary, destination)
            return destination
        except (OSError, requests.RequestException, RuntimeError):
            temporary.unlink(missing_ok=True)
            if attempt == MAX_ATTEMPTS:
                raise
            time.sleep(min(20, attempt * 2))
    raise AssertionError("unreachable")


def valid_file(path: pathlib.Path, expected_sha1: str, expected_size: int) -> bool:
    if expected_size and path.stat().st_size != expected_size:
        return False
    if expected_sha1 and sha1(path) != expected_sha1:
        return False
    if not expected_sha1:
        try:
            with zipfile.ZipFile(path) as archive:
                return archive.testzip() is None
        except zipfile.BadZipFile:
            return False
    return True


def component_files() -> tuple[list[dict], dict]:
    downloads: dict[pathlib.Path, dict] = {}
    classpath: list[pathlib.Path] = []
    minecraft = json.loads((PATCHES / "net.minecraft.json").read_text())
    for filename in ["net.minecraft.json", "net.minecraftforge.json", "org.lwjgl3.json"]:
        component = json.loads((PATCHES / filename).read_text())
        for library in component.get("libraries", []):
            if not allowed(library.get("rules")) or not correct_architecture(library["name"]):
                continue
            artifact = library.get("downloads", {}).get("artifact")
            if artifact:
                destination = LIBRARIES / maven_path(library["name"])
                downloads[destination] = artifact
                classpath.append(destination)

    main = minecraft["mainJar"]["downloads"]["artifact"]
    main_destination = LIBRARIES / maven_path(minecraft["mainJar"]["name"])
    downloads[main_destination] = main
    classpath.append(main_destination)
    return [{"path": path, **data} for path, data in downloads.items()], {
        "classpath": classpath,
        "assetIndex": minecraft["assetIndex"],
    }


def prepare_assets(index: dict) -> None:
    index_destination = ASSETS / "indexes" / f"{index['id']}.json"
    materialize(index["url"], index["sha1"], index["size"], index_destination)
    data = json.loads(index_destination.read_text())
    jobs = []
    for entry in data["objects"].values():
        digest = entry["hash"]
        jobs.append(
            (
                f"https://resources.download.minecraft.net/{digest[:2]}/{digest}",
                digest,
                entry["size"],
                ASSETS / "objects" / digest[:2] / digest,
            )
        )
    with concurrent.futures.ThreadPoolExecutor(max_workers=16) as pool:
        for _ in pool.map(lambda job: materialize(*job), jobs):
            pass


def extract_natives(classpath: list[pathlib.Path]) -> None:
    if NATIVES.exists():
        shutil.rmtree(NATIVES)
    NATIVES.mkdir(parents=True)
    for library in classpath:
        if "natives-windows" not in library.name:
            continue
        with zipfile.ZipFile(library) as archive:
            for info in archive.infolist():
                if info.is_dir() or info.filename.upper().startswith("META-INF/"):
                    continue
                target = (NATIVES / info.filename).resolve()
                if not target.is_relative_to(NATIVES.resolve()):
                    raise RuntimeError(f"unsafe native path: {info.filename}")
                target.parent.mkdir(parents=True, exist_ok=True)
                with archive.open(info) as source, target.open("wb") as output:
                    shutil.copyfileobj(source, output)


def main() -> None:
    entries, metadata = component_files()
    with concurrent.futures.ThreadPoolExecutor(max_workers=16) as pool:
        for path in pool.map(
            lambda entry: materialize(entry["url"], entry["sha1"], entry["size"], entry["path"]), entries
        ):
            print(f"verified {path.relative_to(RUNTIME)}", flush=True)

    local_patch = INSTANCE / "libraries" / "lwjgl3ify-3.0.26-forgePatches.jar"
    if not local_patch.exists():
        raise RuntimeError(f"missing local early-classpath library: {local_patch}")
    # Higher-order Forge libraries replace older Minecraft versions with the same
    # Maven group/artifact (notably Guava 15 -> 17).
    deduplicated: dict[tuple[str, str], pathlib.Path] = {}
    for path in metadata["classpath"]:
        relative = path.relative_to(LIBRARIES)
        parts = relative.parts
        key = (".".join(parts[:-3]), parts[-3]) if len(parts) >= 4 else (str(path), "")
        deduplicated[key] = path
    metadata["classpath"] = [local_patch, *deduplicated.values()]
    prepare_assets(metadata["assetIndex"])
    extract_natives(metadata["classpath"])
    manifest = {
        "classpath": [str(path.resolve()) for path in metadata["classpath"]],
        "assets": str(ASSETS.resolve()),
        "natives": str(NATIVES.resolve()),
        "assetIndex": metadata["assetIndex"]["id"],
    }
    (RUNTIME / "launch-manifest.json").write_text(json.dumps(manifest, indent=2))
    print(
        f"prepared {len(entries)} libraries, {len(json.loads((ASSETS / 'indexes' / '1.7.10.json').read_text())['objects'])} assets",
        flush=True,
    )


if __name__ == "__main__":
    main()
