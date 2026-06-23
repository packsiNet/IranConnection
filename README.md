# SafeTunnels

Android (Kotlin + Jetpack Compose) implementation of the **SafeTunnels** VPN
app, built from the Claude Design handoff bundle (`SafeTunnels Home.dc.html`
and siblings).

The HTML prototypes wrapped every screen in an iPhone mockup (Dynamic Island,
side buttons, home indicator). That chrome is preview-only — the native app
renders full-screen and uses the real system status / navigation bars.

## Screens

| Screen | Source prototype | Highlights |
|--------|------------------|-----------|
| **Home** (primary) | `SafeTunnels Home.dc.html` | Live session timer, power toggle (teal ⇄ red), pulsing rings, dotted world map, download/upload stats, Iran server card |
| **Servers** | `SafeTunnels All Server.dc.html` | Automatic / Free / Premium sections, live search, connect / disconnect |
| **Apps** | `SafeTunnels Apps.dc.html` | Iranian bank apps (2 free, 8 premium scrollable), slide toggles, active count, search |

All three share the bottom navigation; tabs switch screens and the connection /
selection state stays in sync.

## Architecture

- **UI:** Jetpack Compose, Material 3, single `MainActivity`. Custom `Canvas`
  drawing recreates the SVG glyphs, flags, world map and power button — no raster
  assets.
- **State:** one `VpnViewModel` (`AndroidViewModel`) holds connection status, the
  per-second timer, selected server and enabled apps. Mirrors the prototypes'
  `DCLogic` state, unified across screens.
- **Persistence:** Jetpack DataStore (`VpnPreferences`) replaces the prototype's
  `localStorage` (`iranconn_secs` / `iranconn_connected`). Timer and toggles
  survive process death.

### Source layout

```
app/src/main/java/com/safetunnels/app/
├── MainActivity.kt              # entry point, tab host, edge-to-edge insets
├── data/
│   ├── VpnPreferences.kt        # DataStore wrapper
│   └── VpnViewModel.kt          # shared state + timer loop
└── ui/
    ├── theme/                   # AppColors, Theme
    ├── components/              # BottomNav, NavIcons, Flags, SignalBars, WorldMap
    └── screens/                 # HomeScreen, ServersScreen, AppsScreen
```

## Build

Open in Android Studio and Sync, or from the CLI:

```bash
./gradlew :app:assembleDebug      # -> app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug            # to a connected device / emulator
```

### Toolchain

- Android Gradle Plugin **8.5.2**, Gradle **8.7**, Kotlin **2.0.0**
  (Compose Compiler plugin), Compose BOM **2024.06.00**
- `compileSdk` / `targetSdk` 34, `minSdk` 24

> The exact plugin/dependency versions above were chosen to match what is already
> present in the local Gradle cache, because this build host cannot reach Google's
> Android Maven repository. On a machine with normal network access you can bump
> AGP / Kotlin / Compose to current releases freely.
# SafeTunnels
