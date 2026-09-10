#!/usr/bin/env python3
"""Reject Android release configuration that Google Play will not accept."""

from pathlib import Path
import re
import sys


REPO_ROOT = Path(__file__).resolve().parents[1]
CONFIG_FILE = (
    REPO_ROOT
    / "build-logic/convention/src/main/kotlin/com/jjswigut/eventide/ConfigConstants.kt"
)
VERSIONS_FILE = REPO_ROOT / "gradle/libs.versions.toml"
MINIMUM_PLAY_TARGET_SDK = 36
MINIMUM_AGP = (8, 10, 0)


def read_integer_constant(source: str, name: str) -> int:
    match = re.search(rf"const val {name}\s*=\s*(\d+)", source)
    if not match:
        raise ValueError(f"Missing integer constant: {name}")
    return int(match.group(1))


def read_agp_version(source: str) -> tuple[int, int, int]:
    match = re.search(r'^androidGradle\s*=\s*"(\d+)\.(\d+)\.(\d+)"', source, re.MULTILINE)
    if not match:
        raise ValueError("Missing stable androidGradle version")
    return tuple(int(part) for part in match.groups())


def main() -> int:
    try:
        config = CONFIG_FILE.read_text(encoding="utf-8")
        versions = VERSIONS_FILE.read_text(encoding="utf-8")
        compile_sdk = read_integer_constant(config, "COMPILE_SDK")
        target_sdk = read_integer_constant(config, "TARGET_SDK")
        agp_version = read_agp_version(versions)
    except (OSError, ValueError) as error:
        print(f"Android release configuration check failed: {error}", file=sys.stderr)
        return 1

    errors = []
    if target_sdk < MINIMUM_PLAY_TARGET_SDK:
        errors.append(
            f"TARGET_SDK is {target_sdk}; Google Play updates require at least "
            f"{MINIMUM_PLAY_TARGET_SDK}."
        )
    if compile_sdk < target_sdk:
        errors.append(f"COMPILE_SDK {compile_sdk} is lower than TARGET_SDK {target_sdk}.")
    if agp_version < MINIMUM_AGP:
        errors.append(
            "Android Gradle Plugin "
            f"{'.'.join(map(str, agp_version))} does not support this project's "
            "API 36 and Kotlin 2.2 combination; use at least 8.10.0."
        )

    if errors:
        print("Android release configuration check failed:", file=sys.stderr)
        for error in errors:
            print(f"- {error}", file=sys.stderr)
        return 1

    print(
        "Android release configuration verified: "
        f"compileSdk={compile_sdk}, targetSdk={target_sdk}, "
        f"AGP={'.'.join(map(str, agp_version))}."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
