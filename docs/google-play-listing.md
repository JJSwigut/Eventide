# Google Play Store Listing

Eventide's English (United States) listing is versioned under
`fastlane/metadata/android/en-US` and uploaded separately from app releases.

## Asset Set

The canonical phone screenshot order is:

1. `01-station-map.png` - NOAA tide stations on the coastal map.
2. `02-tides.png` - Daily high and low tide predictions.
3. `03-weather.png` - National Weather Service forecast in station details.
4. `04-marine.png` - Nearby buoy wind, wave, and water observations.
5. `05-map-overlays.png` - Radar, weather, and cloud map controls.
6. `06-favorites-alerts.png` - Saved-station, home-station, and tide-alert controls.

Google Play requires the feature graphic to be a 1024x500 PNG or JPEG without
alpha. Its editable source is `docs/assets/google-play-feature-graphic.svg`.
Eventide phone screenshots use 1080x1920 PNGs without alpha. The existing Play
icon remains unchanged; a future replacement can be supplied as a 512x512
`images/icon.png` aligned with the launcher icon.

## Screenshot Alt Text

Fastlane Supply does not currently manage Google Play screenshot alt text. Add
these descriptions in Play Console after uploading the images:

- Coastal map centered on Long Island Sound with nearby NOAA tide stations.
- Station details showing high and low tide times and predicted heights.
- Station weather card showing forecast conditions, temperatures, wind, and NWS source.
- Marine conditions card showing nearby buoy wind, waves, water temperature, and freshness.
- Coastal map with the overlay menu open for radar, weather, and cloud layers.
- Saved station controls for favorites, home station, and high or low tide alerts.

## Validation And Publishing

Run the local validation before review:

```bash
python3 tools/eventide_validate_store_listing.py
```

The `Google Play Store Listing` GitHub Actions workflow is manual and defaults
to `validate`. Its `publish` mode replaces the live English listing images and
copy, so publishing requires an explicit owner decision. Normal app releases
continue uploading only the AAB and version-specific changelog.

Google Play asset guidance:
https://support.google.com/googleplay/android-developer/answer/9866151?hl=en
