# Readwise Android Widget

A fully customizable Android home screen widget that displays random highlights from your [Readwise](https://readwise.io) library.

## Features

- **Random highlight rotation** — tap the widget to load a new quote
- **Filter by book or tag** — show only highlights from a specific source
- **Filter by length** — exclude highlights that are too long for the widget
- **Include/exclude individual highlights** — manage your highlight pool via the in-app list
- **Fully customizable appearance** — font size, font family, colors, corner radius, border, padding
- **Material You support** — optional dynamic color theming (Android 12+)
- **Configurable refresh interval** — from 15 minutes up to 24 hours
- **Background sync** — powered by WorkManager

## Requirements

- Android 12 (API 31) or higher
- A [Readwise account](https://readwise.io) with an API token

## Getting Started

1. Install the app
2. Open it and paste your [Readwise API token](https://readwise.io/access_token)
3. Tap **Sync** to download your highlights
4. Long-press your home screen and add the **Readwise Highlight** widget
5. Customize appearance and filters in the app settings

## Building from Source

### Prerequisites

- Android Studio (latest stable)
- JDK 17+
- Android SDK API 36

### Clone & Build

```bash
git clone https://github.com/benstockhecke/readwise-android-widget.git
cd readwise-android-widget
./gradlew assembleDebug
```

### Release Build

Create a `local.properties` file in the project root (this file is **not** committed):

```properties
sdk.dir=/path/to/your/android/sdk

RELEASE_STORE_FILE=../keystore.jks
RELEASE_STORE_PASSWORD=your_keystore_password
RELEASE_KEY_ALIAS=your_key_alias
RELEASE_KEY_PASSWORD=your_key_password
```

Then build:

```bash
./gradlew assembleRelease
```

## Project Structure

```
app/src/main/java/com/readwise/widget/
├── api/                    # Retrofit API client & data models
│   ├── Models.kt
│   └── ReadwiseApi.kt
├── data/                   # Room database, DataStore, Repository
│   ├── AppDatabase.kt
│   ├── HighlightRepository.kt
│   └── SettingsDataStore.kt
├── ui/
│   ├── highlights/         # Highlight management screen
│   └── settings/           # Main settings screen
├── widget/                 # Glance widget & WorkManager sync
│   ├── HighlightWidget.kt
│   ├── HighlightWidgetReceiver.kt
│   └── RefreshWorker.kt
├── MainActivity.kt
└── ReadwiseApp.kt
```

## Tech Stack

| Layer | Library |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Widget | Jetpack Glance |
| Networking | Retrofit + OkHttp + kotlinx.serialization |
| Database | Room |
| Preferences | DataStore |
| Background sync | WorkManager |
| Navigation | Navigation Compose |

## Privacy

Your Readwise API token is stored locally on your device using DataStore and is never transmitted to any server other than `readwise.io`.

## License

MIT
