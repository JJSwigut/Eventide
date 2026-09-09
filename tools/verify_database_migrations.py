#!/usr/bin/env python3
"""Verify that an existing Eventide database upgrades to the current schema."""

from pathlib import Path
import sqlite3
import tempfile


REPO_ROOT = Path(__file__).resolve().parent.parent
MIGRATION = (
    REPO_ROOT
    / "app/src/main/sqldelight/com/jjswigut/eventide/db/1.sqm"
)


def create_legacy_database(connection: sqlite3.Connection) -> None:
    connection.executescript(
        """
        CREATE TABLE StationEntity (
            id TEXT PRIMARY KEY,
            latitude REAL NOT NULL,
            longitude REAL NOT NULL,
            name TEXT NOT NULL,
            state TEXT NOT NULL
        );
        CREATE INDEX index_lat_lng ON StationEntity(latitude, longitude);
        CREATE TABLE LastUpdate (
            id INTEGER PRIMARY KEY,
            lastUpdated INTEGER
        );
        PRAGMA user_version = 1;
        """
    )


def apply_migration(connection: sqlite3.Connection) -> None:
    if not MIGRATION.is_file():
        raise AssertionError(f"Missing SQLDelight migration: {MIGRATION.relative_to(REPO_ROOT)}")

    connection.executescript(MIGRATION.read_text(encoding="utf-8"))


def assert_current_tables_are_usable(connection: sqlite3.Connection) -> None:
    tables = {
        row[0]
        for row in connection.execute(
            "SELECT name FROM sqlite_master WHERE type = 'table'"
        )
    }
    expected = {
        "StationEntity",
        "LastUpdate",
        "FavoriteStationEntity",
        "TideAlertPreferenceEntity",
    }
    if not expected.issubset(tables):
        raise AssertionError(f"Migration missing tables: {sorted(expected - tables)}")

    connection.execute(
        """
        INSERT INTO FavoriteStationEntity
            (id, latitude, longitude, name, state, created_at)
        VALUES ('8461490', 41.3614, -72.09, 'New London', 'CT', 1)
        """
    )
    connection.execute(
        """
        INSERT INTO TideAlertPreferenceEntity
            (station_id, lead_time_minutes, tide_filter, enabled, updated_at)
        VALUES ('8461490', 60, 'both', 1, 1)
        """
    )
    assert connection.execute(
        "SELECT COUNT(*) FROM FavoriteStationEntity"
    ).fetchone() == (1,)
    assert connection.execute(
        "SELECT COUNT(*) FROM TideAlertPreferenceEntity"
    ).fetchone() == (1,)


def verify_upgrade() -> None:

    with tempfile.TemporaryDirectory() as temp_dir:
        database_path = Path(temp_dir) / "Stations.db"
        with sqlite3.connect(database_path) as connection:
            create_legacy_database(connection)
            apply_migration(connection)
            assert_current_tables_are_usable(connection)


def verify_already_current_schema() -> None:
    """Builds 23-27 created all tables but still recorded schema version 1."""
    with sqlite3.connect(":memory:") as connection:
        create_legacy_database(connection)
        apply_migration(connection)
        apply_migration(connection)
        assert_current_tables_are_usable(connection)


if __name__ == "__main__":
    verify_upgrade()
    verify_already_current_schema()
    print("Database migration verification passed")
