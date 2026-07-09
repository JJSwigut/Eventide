# Marine Weather Sources

Eventide uses no-key/free marine and weather data sources for selected tide stations.

- NOAA CO-OPS Data API: latest station observations for supported products such as wind, air and water temperature, pressure, visibility, humidity, and salinity.
- National Weather Service API: active alerts for the selected station point. Requests must keep an identifying `User-Agent`.
- National Data Buoy Center: active station metadata and realtime HTTPS flat files for the nearest buoy within the app's configured search radius.
- NOAA nowCOAST/NWS map services: radar, weather, and GOES satellite/cloud imagery layers.
- Open-Meteo Marine: no-key non-commercial fallback for modeled wave height, wave period, and sea-surface temperature. Open-Meteo and its upstream model providers require attribution in user-visible output.

Follow-up items:

- CO-OPS currents require station-specific bin handling; do not request them until the app can resolve or present supported bins.
- Additional nowCOAST layers such as marine warnings, watches, surface wind analysis, and sea-surface temperature analysis should be added only after validating stable service/layer names and mobile map readability.
- NDBC realtime parsing currently uses standard meteorological flat files. Spectral wave summary files can provide richer wave data where available.
