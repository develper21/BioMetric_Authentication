# Voice + Eye Authentication App

A biometric authentication system for mobile applications that introduces multi-factor security using voice recognition and eye scanning. The device unlocks only after successful verification of both biometric methods, enhancing privacy and protection.

## 🚀 Features

### 🔐 Multi-Factor Authentication
- **Voice Recognition**: Wake word detection + voice pattern matching
- **Eye Scanning**: Real-time face detection with liveness detection
- **Iris Authentication**: Advanced iris pattern recognition
- **Secure Storage**: Encrypted storage of biometric data

### 🛡️ Security Features
- **Liveness Detection**: Prevents spoofing attacks
- **Secure Encryption**: Military-grade encryption for biometric data
- **Fallback Authentication**: Multiple fallback options (biometric, PIN, pattern)
- **Error Handling**: Comprehensive error management and recovery

### 📱 User Experience
- **Hinglish Interface**: User-friendly Hindi/English mixed interface
- **Real-time Feedback**: Live status updates during authentication
- **Setup Wizard**: Easy step-by-step setup process
- **Modern UI**: Material Design 3 with dark theme

## 🏗️ Architecture

### Core Components

1. **Voice Module**
   - `VoiceRecognitionManager`: Speech recognition and wake word detection
   - `VoicePatternMatcher`: Voice biometric authentication

2. **Eye Module**
   - `EyeScannerManager`: Camera integration and face detection
   - `IrisPatternMatcher`: Iris pattern recognition

3. **Security Module**
   - `SecureStorage`: Encrypted data storage
   - `PhoneUnlockManager`: Device unlock integration
   - `FallbackManager`: Backup authentication methods

4. **UI Module**
   - `MainActivity`: Main authentication interface
   - `VoiceSetupActivity`: Voice biometric setup
   - `EyeSetupActivity`: Eye biometric setup

## 📋 Requirements

### Minimum Requirements
- **Android Version**: 7.0 (API Level 24) or higher
- **RAM**: Minimum 2GB
- **Storage**: 100MB free space
- **Hardware**: Front camera, Microphone

### Permissions Required
- `RECORD_AUDIO`: Voice recognition
- `CAMERA`: Eye and iris scanning
- `USE_BIOMETRIC`: Biometric authentication

## 🛠️ Installation

### Build from Source

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd BioMetric
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the BioMetric directory

3. **Build the APK**
   - Click Build > Build Bundle(s) / APK(s) > Build APK(s)
   - The APK will be generated in `app/build/outputs/apk/debug/`

4. **Install on Device**
   ```bash
   adb install app-debug.apk
   ```

## 🚀 Quick Start

### First Time Setup

1. **Launch the App**
   - Open VoiceEye Auth from your app drawer

2. **Grant Permissions**
   - Allow camera and microphone permissions
   - Enable biometric permissions

3. **Setup Voice Authentication**
   - Tap "Setup Voice"
   - Follow the voice recording instructions
   - Say the sample phrase clearly

4. **Setup Eye Authentication**
   - Tap "Setup Eye"
   - Position your face in the camera frame
   - Follow the eye scanning instructions

5. **Start Authentication**
   - Tap "Start Authentication"
   - Say wake word: "open phone"
   - Complete eye scan when prompted

### Authentication Flow

```
Voice Wake Word → Voice Authentication → Eye Scan → Iris Authentication → Phone Unlock
```

## 🔧 Configuration

### Custom Wake Word

Change the wake word in `VoiceRecognitionManager.kt`:

```kotlin
companion object {
    const val WAKE_WORD = "your_custom_wake_word"
}
```

### Security Settings

Adjust security thresholds in respective files:

- Voice similarity: `VoicePatternMatcher.SIMILARITY_THRESHOLD`
- Iris similarity: `IrisPatternMatcher.IRIS_SIMILARITY_THRESHOLD`

## 🧪 Testing

### Run Tests

1. **Unit Tests**
   ```bash
   ./gradlew test
   ```

2. **Integration Tests**
   ```bash
   ./gradlew connectedAndroidTest
   ```

3. **Manual Testing**
   - Use the built-in test runner
   - Go to Settings > Developer Options > Test Authentication

### Test Coverage

- Voice recognition accuracy
- Eye scanning reliability
- Iris pattern matching
- Security vulnerability testing
- Performance benchmarking

## 🔒 Security Considerations

### Data Protection
- All biometric data is encrypted using AES-256
- No data is transmitted to external servers
- Local storage only with Android Keystore

### Anti-Spoofing
- Liveness detection for eye scanning
- Voice pattern analysis (not just text matching)
- Multiple verification layers

### Privacy
- Biometric data never leaves the device
- No analytics or telemetry collection
- User has full control over stored data

## 🐛 Troubleshooting

### Common Issues

1. **Voice Recognition Not Working**
   - Check microphone permissions
   - Ensure quiet environment
   - Speak clearly and slowly

2. **Eye Scan Failing**
   - Check camera permissions
   - Ensure proper lighting
   - Clean front camera lens

3. **Authentication Failed**
   - Complete both voice and eye setup
   - Check internet connection (for some features)
   - Restart the app

### Debug Mode

Enable debug logging in `build.gradle`:

```gradle
buildTypes {
    debug {
        debuggable true
        minifyEnabled false
    }
}
```

## 📱 Supported Devices

### Tested Devices
- Samsung Galaxy S21+
- Google Pixel 6
- OnePlus 9 Pro
- Xiaomi Mi 11

### Minimum Specifications
- Android 7.0+ (API 24+)
- 2GB RAM
- Front-facing camera
- Microphone

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Support

For issues and support:
- Create an issue on GitHub
- Email: support@voiceeye.auth
- Documentation: [Wiki](wiki-url)

---

**Made with ❤️ for secure biometric authentication**
# BioMetric_Authentication
