<!--
This is where future tasks for Eventide are kept.
When a task is finished, delete it from this backlog.
-->

# Eventide Backlog

## 6. Units & settings

**Goal:** Give `SettingsRepository` real responsibilities and a settings screen.

**Why:** `SettingsRepository` is currently an empty interface. Users want feet/meters, °F/°C, 12/24h time, and a default home station.

**Implementation notes:**
- Back `SettingsRepository` with Jetpack **DataStore (Preferences)**. Define:
  - `tideUnit`: feet | meters
  - `tempUnit`: fahrenheit | celsius
  - `timeFormat`: 12h | 24h
  - `homeStationId`: String?
- Expose settings as `Flow`s and inject into `MapViewModel` so tide heights, temps, and times render in the chosen units/format. Add conversion helpers (ft↔m, °F↔°C).
- On launch, if `homeStationId` is set, center camera there and optionally open its tides instead of the hardcoded `StartLocation` in `MapViewModel`.
- Add a Settings screen (Compose) reachable from the floating menu; wire a `MapAction.OpenSettings` (or navigate to a separate screen).
- "Set as home station" action on a station ties into task 1.

**Acceptance:**
- Changing units/time format updates all displayed tide/weather/time values app-wide.
- Settings persist across restarts.
- A set home station is used as the launch location.

---

## 7. Sun & moon data

**Goal:** Show sunrise/sunset and moon phase on each day card.

**Why:** Pairs naturally with tides (solunar/fishing tables, golden-hour planning) and is computable on-device — no extra API or key needed.

**Implementation notes:**
- Compute sunrise/sunset from the station's lat/lng (`Station.latLng`) and the card's date using a standard NOAA solar position algorithm (or a small library). Compute moon phase from the date (synodic month math).
- Add fields to `TideDay` or a sibling model: `sunrise`, `sunset`, `moonPhase` (enum + illumination %).
- Render a compact row in `TideCard` (sun emoji + sunrise/sunset times, moon-phase emoji). Respect the time-format setting from task 6.
- Keep it adaptive with the existing `AdaptiveSize` scaling.

**Acceptance:**
- Each day card shows correct sunrise/sunset for that station and date.
- Moon phase displays with the right icon for the date.
- Times honor the user's 12/24h setting.
