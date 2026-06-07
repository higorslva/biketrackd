# BikeTrackd

A cycling speedometer and GPS tracker for Android. Built with Jetpack Compose + Material 3, osmdroid maps, and Room database.

## Features

- **Speedometer** — real-time speed with animated counter and color gradient (green → yellow → red)
- **GPS Map** — osmdroid map with trail tracking, follow mode, rotation (GPS + gestures), and offline tile download
- **Session Recording** — start/stop sessions with distance, max/avg speed, duration; data persisted with Room
- **GPX Export** — export any recorded session via share sheet
- **Weather** — current temperature display via Open-Meteo API
- **Offline Maps** — download city tiles (zoom 10–14, 40km radius) stored as .mbtiles
- **Battery Status** — built-in battery level indicator

## Tech Stack

| Layer | Library |
|-------|---------|
| UI | Jetpack Compose + Material 3 |
| Map | osmdroid (OpenTopoMap tiles) |
| Persistence | Room (SQLite) |
| Weather | Open-Meteo API |
| Location | Android Fused Location Provider |
| Icons | Material Icons Extended |

## Screens

- **GPS** — interactive map with trail overlay, follow/center button, rotation, offline download
- **PAINEL** — speedometer with GPS status, weather, battery, clock; session stats (MAX, TEMPO, DISTÂNCIA)
- **OPÇÕES** — save/reset session, session history with GPX export, offline map management (list, delete, download by city)

## Architecture

```
com.biketrackd.app
├── data/           Room entities, DAOs, DB, GPX exporter, tile downloader
├── location/       LocationService, LocationRepository (singleton state + trail)
├── ui/
│   ├── components/ Sidebar, StatusBar, dialogs
│   ├── screens/    GpsScreen, SpeedometerScreen, SettingsScreen
│   └── theme/      Color, Type, Theme (Material 3 dark scheme, green #4CAF50)
└── weather/        Open-Meteo client, weather data
```

The data, location, and weather layers are fully decoupled from the UI — the same architecture can be reused with a different frontend.

## Build

```
./gradlew assembleDebug
```

Requires Android SDK 34, Kotlin 1.9.22, Compose BOM 2024.06.00.

## License

MIT
