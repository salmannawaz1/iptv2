# ProIPTV - Professional IPTV Android App

A professional IPTV Android application with Xtream Codes API and M3U playlist support, featuring an Admin/Reseller control panel.

## Features

### Content Support
- **Xtream Codes API** - Full support for Xtream Codes panel integration
- **M3U Playlist** - Import and play M3U/M3U8 playlists
- **Live TV** - Stream live TV channels with categories
- **Movies (VOD)** - Browse and watch movies on demand
- **Series** - Watch TV series with season/episode organization
- **EPG Support** - Electronic Program Guide integration

### Player Features
- **ExoPlayer (Media3)** - Modern, high-performance media player
- **HLS/DASH Support** - Adaptive streaming support
- **Picture-in-Picture** - Continue watching while using other apps
- **Player Controls** - Full playback controls with seek

### User Features
- **Favorites** - Save favorite channels, movies, and series
- **Search** - Search across all content
- **Categories** - Browse content by categories
- **User Preferences** - Customize playback settings

### Admin Panel
- **User Management** - Add, edit, delete users
- **Reseller Management** - Manage reseller accounts
- **Subscription Control** - Set expiry dates and connections
- **Statistics** - View active users, expired accounts

## Tech Stack

- **Language**: Kotlin
- **Architecture**: MVVM with Clean Architecture
- **DI**: Hilt (Dagger)
- **Network**: Retrofit + OkHttp
- **Database**: Room
- **Preferences**: DataStore
- **Media**: Media3 ExoPlayer
- **UI**: Material Design 3
- **Async**: Kotlin Coroutines + Flow

## Requirements

- Android 7.0 (API 24) or higher
- Internet connection
- Xtream Codes credentials OR M3U playlist URL

## Installation

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Build and run on device/emulator

```bash
git clone <repository-url>
cd ProIPTV
./gradlew assembleDebug
```

## Project Structure

```
app/src/main/java/com/proiptv/app/
├── data/
│   ├── api/           # API service interfaces
│   ├── local/         # Room database, DAOs, DataStore
│   ├── model/         # Data models (Xtream, M3U)
│   └── repository/    # Data repositories
├── di/                # Hilt dependency injection modules
├── ui/
│   ├── admin/         # Admin panel screens
│   ├── favorites/     # Favorites fragment
│   ├── livetv/        # Live TV fragment
│   ├── login/         # Login activity
│   ├── main/          # Main activity
│   ├── movies/        # Movies fragment
│   ├── player/        # Video player
│   ├── series/        # Series fragment
│   └── settings/      # Settings fragment
└── util/              # Utility classes
```

## Xtream Codes API Endpoints

The app uses standard Xtream Codes API endpoints:

- `player_api.php` - Authentication and content
- `/live/{user}/{pass}/{stream_id}.ts` - Live streams
- `/movie/{user}/{pass}/{stream_id}.mp4` - VOD streams
- `/series/{user}/{pass}/{stream_id}.mp4` - Series streams

## Screenshots

Coming soon...

## License

This project is for educational purposes only.

## Disclaimer

- This app is a streaming player only
- Users are responsible for the content they access
- Ensure you have proper rights to stream content
- The developer assumes no legal responsibility

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Open a Pull Request

## Support

For issues and feature requests, please open an issue on GitHub.
