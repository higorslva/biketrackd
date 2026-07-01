> [!IMPORTANT]
> Support an open Android ecosystem: [keepandroidopen.org](https://keepandroidopen.org/) ·
> [Change.org — Stop Google from limiting APK file usage](https://www.change.org/p/stop-google-from-limiting-apk-file-usage)

# BikeTrackd

[![Made in Brazil](https://selo.feitonobrasil.dev.br/en/custom/1x.svg?feito=%23232324&b=%23009440&r=%23ffcb00&a=%23302681&s=%23ffcb00&i=%23009440&l=%23302681)](https://feitonobrasil.dev.br)

A cycling speedometer and GPS tracker for Android. Built with Jetpack Compose + Material 3, MapLibre GL maps, and Room database.

[<img alt="Get it on GitHub" src="https://raw.githubusercontent.com/Kunzisoft/Github-badge/main/get-it-on-github.png" width="240">](https://github.com/higorslva/biketrackd/releases/latest/download/app-universal-release.apk)

## Features

- **Speedometer** — real-time speed with animated counter and color gradient (green → yellow → red)
- **GPS Map** — MapLibre GL map with trail tracking, follow mode, rotation (GPS + gestures), and offline tile download
- **Session Recording** — start/stop sessions with distance, max/avg speed, duration; data persisted with Room
- **GPX Export** — export any recorded session via share sheet
- **Weather** — current temperature display via Open-Meteo API
- **Offline Maps** — download city tiles (zoom 10–14, 40km radius) via MapLibre OfflineManager
- **Battery Status** — built-in battery level indicator
- **Unit System** — metric/imperial toggle with live conversion (km/h ↔ mph, m ↔ ft, °C ↔ °F)
- **Orientation** — landscape/portrait toggle with optimized layouts

## Tech Stack

| Layer | Library |
|-------|---------|
| UI | Jetpack Compose + Material 3 |
| Map | MapLibre GL (OpenFreeMap vector tiles) |
| Persistence | Room (SQLite) |
| Weather | Open-Meteo API |
| Location | Android Fused Location Provider |
| Icons | Material Icons Extended |

## Screens

- **GPS** — interactive map with trail overlay, follow/center button, rotation, offline download
- **PAINEL** — speedometer with GPS status, weather, battery, clock; session stats (MAX, TEMPO, DISTÂNCIA)
- **OPÇÕES** — save/reset session, session history with GPX export, offline map management (list, delete, download by city)

## Screenshots

### Map & Navigation

| Route map (landscape) | Route + Speedometer (landscape) |
|---|---|
| ![Route map landscape](images/rota-gps-only-paisagem.png) | ![Route + speedometer landscape](images/rota-gps-velo-paisagem.png) |

| Map + Mini speedometer (portrait) | Map + Mini speedometer (landscape) |
|---|---|
| ![Map and speedometer portrait](images/mapa-e-velocimetro-retrato.png) | ![Map and speedometer landscape](images/mapa-e-velocimetro-paisagem.png) |

### Speedometer

| Portrait | Landscape | MR Warning |
|---|---|---|
| ![Speedometer portrait](images/velocimetro-retrato.png) | ![Speedometer landscape](images/velocimetro-paisagem.png) | ![MR warning](images/velocimetro-alerta-mr-retrato.png) |

### Bikes & Parts

| Edit bike | Parts list | Add part |
|---|---|---|
| ![Edit bike](images/editar-bike-retrato.png) | ![Parts list](images/tela-peças-retrato.png) | ![Add part](images/adicionar-peças-retrato.png) |

| Edit part | Wear alert |
|---|---|
| ![Edit part](images/editar-peça-retrato.png) | ![Wear alert](images/alerta-peças-retrato.png) |

### Settings

| Settings |
|---|
| ![Settings](images/settings-retrato.png) |

## Dashboard Warnings

| Indicator | Color | Meaning |
|-----------|-------|---------|
| **BAT** | Green / Amber / Red | Battery ≥40% / 16–40% / ≤15% (blinking + warning icon) |
| **ENG** | Green / Amber | Session active / Moving without an active session |
| **GPS** | Green / Yellow / Red | Position fix acquired / Stationary (<3 km/h) / No fix (blinking) |
| **TMP** | Green / Amber / Red | Normal / Warm / Hot or critical (blinking + warning icon) |
| **MR** | Red blinking | Maintenance Required — any part has reached ≥90% wear |
| **⚠️ BIKE LANE** | Red banner | Speed exceeds the configured bike lane limit |

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
