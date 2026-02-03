# Saaya - Your Digital Shadow 🌑

![Build Status](https://github.com/YOUR_USERNAME/Saaya/workflows/Build%20Saaya%20APK/badge.svg)

## Overview

**Saaya** is a native Android automation app that runs locally as an "Invisible Operator" or "Shadow" to learn from user interactions and automate tasks intelligently. Built with Java, it uses Android's Accessibility Services to observe screen context while maintaining strict privacy standards.

### Key Features

- 🤖 **Intelligent Pattern Learning**: Learns from your app usage patterns using local SQLite database
- 🔒 **Privacy-First**: Runs 100% offline, password fields are never recorded
- ⚡ **Task Automation**: Automate repetitive tasks using gesture automation
- 🧠 **Context-Aware**: Uses TensorFlow Lite for intelligent decision making
- 🎯 **Non-Root**: Works on non-rooted devices with Accessibility Services

## Technical Stack

- **Language**: Java (100%)
- **Min SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 14 (API 34)
- **Dependencies**:
  - TensorFlow Lite 2.14.0
  - AndroidX AppCompat
  - Material Design Components
  - SQLite (Built-in)

## Project Structure

```
Saaya/
├── app/
│   ├── src/main/
│   │   ├── java/com/saaya/automator/
│   │   │   ├── MainActivity.java              # Control Center UI
│   │   │   ├── core/
│   │   │   │   └── SaayaService.java         # Accessibility Service
│   │   │   └── data/
│   │   │       └── SaayaMemoryDB.java        # SQLite Database
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   └── activity_main.xml         # Main UI Layout
│   │   │   ├── values/
│   │   │   │   ├── strings.xml
│   │   │   │   ├── colors.xml
│   │   │   │   └── themes.xml
│   │   │   └── xml/
│   │   │       └── saaya_config.xml          # Accessibility Config
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── .github/workflows/
    └── build.yml                              # GitHub Actions CI/CD
```

## Installation

### Option 1: Build from Source

1. **Clone the repository**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/Saaya.git
   cd Saaya
   ```

2. **Build using Gradle**:
   ```bash
   ./gradlew assembleDebug
   ```

3. **Install APK**:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

### Option 2: Download from Releases

Download the latest APK from the [Releases](https://github.com/YOUR_USERNAME/Saaya/releases) page.

### Option 3: Build with GitHub Actions

1. Fork this repository
2. Go to Actions tab
3. Run "Build Saaya APK" workflow
4. Download artifacts from the workflow run

## Required Permissions

The app requires the following permissions:

1. **Accessibility Service** - To observe screen content and learn patterns
2. **Display Over Other Apps** - For overlay features
3. **Usage Access** - To track app usage patterns

### How to Grant Permissions:

1. Open the Saaya app
2. Tap "Grant Access" on each permission card
3. Enable Saaya in the system settings
4. Return to the app and verify "Shadow is Active" status

## How It Works

### Architecture

```
┌─────────────────────────────────────────────┐
│           User Interaction                  │
│     (Clicks, Text Input, App Usage)         │
└────────────────┬────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────┐
│        SaayaService (Accessibility)         │
│  ┌──────────────────────────────────────┐   │
│  │ • Capture Screen Events              │   │
│  │ • Filter Password Fields (SECURITY)  │   │
│  │ • Extract Context                    │   │
│  └──────────────────────────────────────┘   │
└────────────────┬────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────┐
│         SaayaMemoryDB (SQLite)              │
│  ┌──────────────────────────────────────┐   │
│  │ • Store Patterns                     │   │
│  │ • Learn Behaviors                    │   │
│  │ • Recall Suggestions                 │   │
│  └──────────────────────────────────────┘   │
└────────────────┬────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────┐
│       Automation Engine (Future)            │
│  • Gesture Automation                       │
│  • Task Scheduling                          │
│  • ML-Based Predictions                     │
└─────────────────────────────────────────────┘
```

### Data Flow

1. **Event Capture**: `SaayaService` monitors accessibility events (text changes, clicks, window state changes)
2. **Security Filter**: Password fields are detected using `node.isPassword()` and immediately ignored
3. **Pattern Learning**: Non-sensitive data is stored in local SQLite database
4. **Pattern Recall**: When similar context is detected, app suggests actions based on past behavior
5. **Automation**: App can perform gestures (tap, swipe) to automate repetitive tasks

### Security & Privacy

- ✅ **Password Protection**: `isPassword()` check ensures sensitive fields are never logged
- ✅ **Local Storage**: All data stored locally in SQLite, no cloud sync
- ✅ **Package Filtering**: System UI and keyboards are ignored
- ✅ **Minimal Data**: Only necessary context is stored
- ✅ **User Control**: Clear patterns button to reset all learned data

## Usage Example

### Learning Phase:
1. User opens WhatsApp
2. Types "On my way!" frequently
3. Saaya learns this pattern and stores it

### Automation Phase:
1. User opens WhatsApp again
2. Saaya detects similar context
3. Suggests "On my way!" as quick action
4. User can automate with one tap

## Building APK on GitHub

### Automated Build Setup:

1. **Push your code to GitHub**:
   ```bash
   git add .
   git commit -m "Initial Saaya commit"
   git push origin main
   ```

2. **GitHub Actions will automatically**:
   - Set up Java 17 environment
   - Build debug APK
   - Build release APK
   - Upload artifacts
   - Create releases with version tags

3. **Download Built APKs**:
   - Go to Actions tab
   - Click on latest workflow run
   - Download artifacts (saaya-debug or saaya-release)

### Manual Build Commands:

```bash
# Debug build
./gradlew assembleDebug

# Release build (unsigned)
./gradlew assembleRelease

# Clean build
./gradlew clean assembleDebug

# Build and install
./gradlew installDebug
```

## Development

### Prerequisites:
- Android Studio Arctic Fox or later
- JDK 17
- Android SDK 26-34
- Gradle 8.1+

### Setup Development Environment:

1. Open project in Android Studio
2. Sync Gradle files
3. Connect Android device or start emulator
4. Run app (Shift + F10)

### Key Files to Modify:

- **SaayaService.java**: Core automation logic
- **SaayaMemoryDB.java**: Database schema and queries
- **MainActivity.java**: UI and permission handling
- **saaya_config.xml**: Accessibility service configuration

## Roadmap

- [x] Basic accessibility service
- [x] Pattern learning and storage
- [x] Permission management UI
- [x] Gesture automation
- [ ] TensorFlow Lite integration
- [ ] Smart suggestions UI
- [ ] Task scheduler
- [ ] Export/import patterns
- [ ] Multi-language support

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Disclaimer

This app is for educational and personal automation purposes. Users are responsible for ensuring their use complies with app terms of service and local regulations. The developers are not liable for misuse.

## Support

- **Issues**: [GitHub Issues](https://github.com/YOUR_USERNAME/Saaya/issues)
- **Discussions**: [GitHub Discussions](https://github.com/YOUR_USERNAME/Saaya/discussions)

---

**Made with ❤️ for automation enthusiasts**

*"Your Digital Shadow, Always Learning"* 🌑
#   s a a y a  
 