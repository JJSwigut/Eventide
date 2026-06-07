<!--
This is where future tasks for Eventide are kept.
When a task is finished, delete it from this backlog.
-->

# Eventide Backlog

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
