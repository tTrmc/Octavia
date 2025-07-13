# Octavia Hi-Fi Android Music Player

A modern, offline Android music player built for audiophiles who demand the highest quality audio playback experience.

## Features

### 🎵 Hi-Res Audio Support
- **Lossless Formats**: FLAC, ALAC, WAV with bit-perfect playback
- **High Sample Rates**: Up to 192kHz/32-bit support
- **Gapless Playback**: Seamless transitions between tracks
- **Hardware Offloading**: Direct audio path to device DAC when available

### 🎛️ Advanced Audio Processing
- **ReplayGain**: Automatic volume normalization
- **Parametric EQ**: Customizable frequency response
- **Crossfade**: Smooth track transitions
- **Multiple Output Modes**: Optimized for different audio hardware

### 📚 Smart Library Management
- **Fast Scanning**: Efficient MediaStore integration
- **Rich Metadata**: Embedded artwork, ReplayGain, technical info
- **Smart Organization**: Albums, artists, genres, playlists
- **Search**: Fast full-text search across your library

### 🎨 Modern Material Design
- **Material 3**: Dynamic theming with light/dark modes
- **Responsive UI**: Optimized for phones and tablets
- **Smooth Animations**: Polished user experience
- **Accessibility**: Full screen reader and keyboard support

## Technical Architecture

### Built With Modern Android Stack
- **Language**: Kotlin 2.x with Coroutines & Flow
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM + Clean Architecture
- **DI**: Hilt for dependency injection
- **Database**: Room with TypeConverters
- **Media**: Media3 ExoPlayer with MediaSessionService

### Audio Engine
- **Core**: Media3 ExoPlayer with custom AudioSink
- **Decoding**: Native MediaCodec for API 27+, fallback libFLAC for older devices
- **Output**: AudioTrack with AAudio/Oboe optimizations
- **Processing**: Custom audio processor chain for EQ and ReplayGain

### Key Dependencies
- **Media3**: Latest ExoPlayer with hi-res audio support
- **Room**: Local database with full-text search
- **Coil**: Efficient image loading and caching
- **JAudioTagger**: Metadata reading and editing
- **Hilt**: Dependency injection
- **WorkManager**: Background library scanning

## Project Structure

```
app/src/main/java/com/octavia/player/
├── data/
│   ├── database/          # Room database and DAOs
│   ├── model/             # Data models and entities
│   └── repository/        # Repository pattern implementations
├── di/                    # Hilt dependency injection modules
├── presentation/
│   ├── components/        # Reusable UI components
│   ├── navigation/        # Compose navigation setup
│   ├── screens/           # Feature screens (Home, Library, Player, etc.)
│   ├── service/           # Media3 background service
│   └── theme/             # Material 3 theming
└── OctaviaApplication.kt  # Application class
```

## Building

### Prerequisites
- Android Studio Hedgehog 2023.1.1+
- Android SDK 35
- Gradle 8.9+
- Java 17

### Build Steps
```bash
# Clone the repository
git clone https://github.com/your-username/octavia.git
cd octavia

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run tests
./gradlew test
./gradlew connectedAndroidTest
```

## Supported Formats

### Lossless
- **FLAC**: Up to 192kHz/32-bit
- **ALAC**: Apple Lossless (M4A container)
- **WAV**: Uncompressed PCM
- **DSD**: Direct Stream Digital (where supported)

### Lossy
- **MP3**: All bitrates and sample rates
- **AAC/M4A**: Including Apple Music purchases
- **OGG Vorbis**: Open source alternative
- **Opus**: Modern efficient codec

---

**Octavia** - *Pure Sound, Pure Experience*