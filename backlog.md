<!--
This is where future tasks for Eventide are kept.
When a task is finished, delete it from this backlog.
-->

# Eventide Backlog

## 2. Tide notifications / alerts

**Goal:** "Notify me N minutes before the next low/high tide at a chosen station."

**Why:** The core retention hook for the real audience (fishing, surfing, clamming, boating, tidepooling). Tide data is already timestamped and structured (`Tide.time`, `TideValue.Low/High`).

**Depends on:** Task 1 (favorites) for choosing which stations to alert on.

**Implementation notes:**
- Add `POST_NOTIFICATIONS` permission to `AndroidManifest.xml` and request it at runtime (Android 13+). Add a notifications permission rationale flow alongside the existing location permission handling in `MainActivity`.
- Use **WorkManager** to schedule alerts. Store alert preferences (station id, lead time, high/low/both) in settings (task 6) or a dedicated table.
- A periodic/one-shot worker fetches upcoming tides for subscribed stations (reuse `NoaaRepository.getTidesForStation`), computes the next matching extreme, and schedules a notification at `tideTime - leadTime`.
- Build a notification channel in `EventideApplication`.
- UI: per-favorite "Alert" toggle + lead-time picker (e.g. 30/60/120 min) and high/low selection.
- Handle reschedule on reboot (`BOOT_COMPLETED` receiver) and after each fire.

**Acceptance:**
- User can enable an alert on a favorite station with a chosen lead time.
- A notification fires at the correct time before the next high/low.
- Alerts survive app kill and device reboot.

---

## 3. Tide curve graph

**Goal:** Replace the discrete high/low list with a smooth tide curve and a "you are here" marker for the current water level.

**Why:** Biggest visual upgrade; it's what every competing tide app leads with. Currently `TideCard` only lists discrete extreme points.

**Implementation notes:**
- Interpolate between consecutive high/low extremes using a cosine segment: between an extreme at `t0` (height `h0`) and the next at `t1` (height `h1`), `h(t) = h0 + (h1 - h0) * (1 - cos(pi * (t - t0)/(t1 - t0))) / 2`. This approximates real tide shape well enough for display.
- Draw with Compose `Canvas` in a new `map/components/TideGraph.kt`: axis labels (time across, height up), the curve path, dots at extremes with time + height labels, and a vertical "now" line with a dot at the interpolated current height (only on today's card).
- Parse `Tide.time` / `Tide.height` into numeric values for plotting (may need a richer model than the current `String` fields — consider adding parsed `LocalDateTime` and `Double` height to `Tide`, or a derived plotting model).
- Make it adaptive like the existing `AdaptiveSize` scaling in `TideCard`.
- Keep the textual high/low list available (small, below the graph) for accessibility.

**Acceptance:**
- Each day card shows a continuous tide curve with labeled high/low points.
- Today's card shows a live "now" marker at the correct interpolated height.
- Graph scales cleanly across phone sizes.

---

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
