"""Resume and verify the official GTNH 2.8.4 server compatibility fixture."""

from __future__ import annotations

import concurrent.futures
import hashlib
import os
import pathlib
import random
import sys
import time
import zipfile

import requests


URL = (
    "https://downloads.gtnewhorizons.com/ServerPacks/"
    "GT_New_Horizons_2.8.4_Server_Java_17-25.zip"
)
EXPECTED_SIZE = 441_093_161
EXPECTED_ETAG = '"7b07c62de9a04483efc3877d8480fba3-85"'
CHUNK_SIZE = 8 * 1024 * 1024
MAX_ATTEMPTS = 40


def part_count() -> int:
    return (EXPECTED_SIZE + CHUNK_SIZE - 1) // CHUNK_SIZE


def fetch_part(index: int, part_directory: pathlib.Path) -> pathlib.Path:
    start = index * CHUNK_SIZE
    end = min(EXPECTED_SIZE, start + CHUNK_SIZE) - 1
    expected = end - start + 1
    destination = part_directory / f"{index:03d}.part"
    temporary = part_directory / f"{index:03d}.tmp"
    if destination.exists() and destination.stat().st_size == expected:
        return destination

    for attempt in range(1, MAX_ATTEMPTS + 1):
        try:
            with requests.get(
                URL,
                headers={
                    "Range": f"bytes={start}-{end}",
                    "If-Match": EXPECTED_ETAG,
                    "User-Agent": "Hooked-GTNH-port-validation",
                },
                stream=True,
                timeout=(30, 90),
            ) as response:
                response.raise_for_status()
                if response.status_code != 206:
                    raise RuntimeError(f"expected HTTP 206, got {response.status_code}")
                content_range = response.headers.get("Content-Range", "")
                if not content_range.startswith(f"bytes {start}-{end}/"):
                    raise RuntimeError(f"unexpected Content-Range: {content_range!r}")
                if response.headers.get("ETag") != EXPECTED_ETAG:
                    raise RuntimeError(f"unexpected ETag: {response.headers.get('ETag')!r}")
                with temporary.open("wb") as stream:
                    for block in response.iter_content(1024 * 1024):
                        if block:
                            stream.write(block)
            if temporary.stat().st_size != expected:
                raise RuntimeError(f"expected {expected} bytes, got {temporary.stat().st_size}")
            os.replace(temporary, destination)
            print(f"part {index + 1}/{part_count()} complete", flush=True)
            return destination
        except (OSError, requests.RequestException, RuntimeError) as error:
            temporary.unlink(missing_ok=True)
            if attempt == MAX_ATTEMPTS:
                raise
            delay = min(30.0, 2.0 + attempt * 1.5) + random.random()
            print(
                f"part {index + 1}/{part_count()} attempt {attempt} failed: {error}; retrying",
                file=sys.stderr,
                flush=True,
            )
            time.sleep(delay)
    raise AssertionError("unreachable")


def main() -> None:
    root = pathlib.Path(__file__).resolve().parents[1]
    download_directory = root / ".compat" / "downloads"
    part_directory = download_directory / "gtnh-2.8.4-server.parts"
    destination = download_directory / "GT_New_Horizons_2.8.4_Server_Java_17-25.zip"
    part_directory.mkdir(parents=True, exist_ok=True)

    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
        futures = [pool.submit(fetch_part, index, part_directory) for index in range(part_count())]
        for future in concurrent.futures.as_completed(futures):
            future.result()

    digest = hashlib.sha256()
    temporary_archive = destination.with_suffix(".zip.assembling")
    with temporary_archive.open("wb") as output:
        for index in range(part_count()):
            part = part_directory / f"{index:03d}.part"
            with part.open("rb") as source:
                while block := source.read(1024 * 1024):
                    digest.update(block)
                    output.write(block)
    if temporary_archive.stat().st_size != EXPECTED_SIZE:
        raise RuntimeError(f"assembled archive has size {temporary_archive.stat().st_size}")
    os.replace(temporary_archive, destination)
    with zipfile.ZipFile(destination) as archive:
        bad_entry = archive.testzip()
        if bad_entry is not None:
            raise RuntimeError(f"CRC failure in {bad_entry}")
    print(f"verified ZIP CRCs; SHA-256 {digest.hexdigest()}; source ETag {EXPECTED_ETAG}", flush=True)


if __name__ == "__main__":
    main()
