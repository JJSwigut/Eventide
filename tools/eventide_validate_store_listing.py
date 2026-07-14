#!/usr/bin/env python3
from __future__ import annotations

import struct
import sys
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
LOCALE_DIR = REPO_ROOT / "fastlane" / "metadata" / "android" / "en-US"
IMAGES_DIR = LOCALE_DIR / "images"
SCREENSHOTS_DIR = IMAGES_DIR / "phoneScreenshots"


def fail(message: str) -> None:
    print(f"Store listing validation failed: {message}", file=sys.stderr)
    raise SystemExit(1)


def read_text(name: str, limit: int) -> str:
    path = LOCALE_DIR / name
    if not path.is_file():
        fail(f"missing {path.relative_to(REPO_ROOT)}")
    value = path.read_text(encoding="utf-8").strip()
    if not value:
        fail(f"{name} is empty")
    if len(value) > limit:
        fail(f"{name} is {len(value)} characters; limit is {limit}")
    return value


def image_files(directory: Path) -> list[Path]:
    if not directory.is_dir():
        return []
    return sorted(
        path
        for path in directory.iterdir()
        if path.is_file() and path.suffix.lower() in {".png", ".jpg", ".jpeg"}
    )


def read_png_header(path: Path) -> tuple[int, int, int]:
    with path.open("rb") as image_file:
        signature = image_file.read(8)
        length = struct.unpack(">I", image_file.read(4))[0]
        chunk_type = image_file.read(4)
        header = image_file.read(length)

    if signature != b"\x89PNG\r\n\x1a\n" or chunk_type != b"IHDR" or length != 13:
        fail(f"{path.name} is not a valid PNG")
    width, height, bit_depth, color_type = struct.unpack(">IIBB", header[:10])
    if bit_depth != 8 or color_type not in {0, 2, 3, 4, 6}:
        fail(f"{path.name} uses unsupported PNG format: bit_depth={bit_depth}, color_type={color_type}")
    return width, height, color_type


def validate_raster(path: Path, expected_size: tuple[int, int], allow_alpha: bool) -> None:
    if not path.is_file():
        fail(f"missing {path.relative_to(REPO_ROOT)}")
    if path.suffix.lower() != ".png":
        fail(f"{path.name} must be a PNG")
    if path.stat().st_size > 8 * 1024 * 1024:
        fail(f"{path.name} exceeds Google Play's 8 MB image limit")

    width, height, color_type = read_png_header(path)
    if (width, height) != expected_size:
        fail(f"{path.name} is {width}x{height}; expected {expected_size[0]}x{expected_size[1]}")
    if not allow_alpha and color_type in {4, 6}:
        fail(f"{path.name} contains an alpha channel")


def main() -> None:
    title = read_text("title.txt", 30)
    short_description = read_text("short_description.txt", 80)
    full_description = read_text("full_description.txt", 4000)

    validate_raster(IMAGES_DIR / "featureGraphic.png", (1024, 500), allow_alpha=False)
    icon = IMAGES_DIR / "icon.png"
    if icon.exists():
        validate_raster(icon, (512, 512), allow_alpha=True)

    screenshots = image_files(SCREENSHOTS_DIR)
    if not 4 <= len(screenshots) <= 8:
        fail(f"expected 4-8 phone screenshots; found {len(screenshots)}")
    for screenshot in screenshots:
        validate_raster(screenshot, (1080, 1920), allow_alpha=False)

    print(
        "Store listing validation passed: "
        f"title={len(title)}/30, short={len(short_description)}/80, "
        f"full={len(full_description)}/4000, screenshots={len(screenshots)}"
    )


if __name__ == "__main__":
    main()
