# Protector
Keep me aware device of my items

## Overview
Protector is an Android application that provides advanced device theft protection and proximity monitoring features. It alerts your smartwatch when someone moves your phone from a table, monitors geo-fencing boundaries, and uses voice recognition for intelligent security control.

## Features

### 1. Theft Detection
- **Accelerometer Monitoring**: Detects when your phone is being moved or accelerated away from its resting position
- **Real-time Alerts**: Sends immediate notifications to your smartwatch when suspicious movement is detected
- **Vibration Feedback**: Device vibrates to alert potential thieves they've been detected

### 2. Geo-Fencing
- **Location-based Protection**: Set a proximity boundary around your device's location
- **Automatic Alerts**: Get notified when your device moves outside the designated area
- **Customizable Radius**: Configure the geo-fence radius to suit your needs (default: 50 meters)

### 3. Proximity Warning
- **Distance Monitoring**: Tracks the distance between you and your device
- **"Don't Forget" Reminders**: Warns you if you move away from your device (e.g., leaving your phone on a table)
- **Smartwatch Integration**: Alerts appear on your paired smartwatch for immediate awareness

### 4. Voice Recognition Control
- **Vosk Speech Recognition**: Integrated offline voice recognition using Vosk
- **Voice Commands**: Control warnings and alerts using voice commands
  - "disable warnings" - Temporarily pause alerts
  - "enable warnings" - Resume alert monitoring
  - "turn off" / "turn on" - Toggle protection
- **Google Speech Recognition Fallback**: Optional online recognition support

### 5. Voice Authentication
- **AI-based Voice Matching**: Learns and recognizes your voice pattern
- **Unauthorized Access Detection**: Alerts if an unfamiliar voice is detected using your device
- **Training Mode**: Train the system with your voice for better recognition
- **Configurable Sensitivity**: Adjust voice matching threshold

## Installation

### Prerequisites
- Android device running Android 8.0 (API 26) or higher
- Android Studio (for building from source)
- Smartwatch with Wear OS (optional, for smartwatch features)

### Building the App
1. Clone this repository:
   ```bash
   git clone https://github.com/cadallacricky1-maker/Protector.git
   cd Protector
   ```

2. Open the project in Android Studio

3. Sync Gradle dependencies

4. Build and run on your Android device

## Usage

### Initial Setup
1. **Grant Permissions**: On first launch, grant the following permissions:
   - Location (Fine and Background)
   - Microphone (for voice recognition)
   - Bluetooth (for smartwatch communication)
   - Notifications

2. **Configure Settings**:
   - Set your preferred proximity radius
   - Enable voice authentication (optional)
   - Train your voice model for better recognition

### Starting Protection
1. Launch the app
2. Click **"Start Protection"** to begin monitoring
3. The app will run in the background with a persistent notification
4. Your device is now protected

### Setting Geo-Fence
1. Navigate to your desired location
2. Click **"Set Geo-fence"** to mark the current location as the protected area
3. The app will alert you if your device leaves this area

### Voice Training
1. Enable **"Voice Authentication"** in settings
2. Click **"Train Voice Model"**
3. Speak clearly 3 times to train the system
4. The app will now recognize your voice

### Voice Commands
While protection is active, you can use these voice commands:
- "disable warnings" - Pause alerts temporarily
- "enable warnings" - Resume alerts
- "turn off" - Stop protection
- "turn on" - Start protection

## Technical Architecture

### Components
- **MainActivity**: Main UI for configuration and control
- **ProtectionService**: Background service that monitors device state
- **VoiceRecognitionManager**: Handles Vosk speech recognition and voice authentication
- **GeofenceBroadcastReceiver**: Processes geo-fence transition events

### Key Technologies
- **Vosk**: Offline speech recognition
- **Google Play Services Location**: Location tracking and geo-fencing
- **Android Sensors**: Accelerometer for theft detection
- **Wear OS**: Smartwatch integration

## Permissions Required
- `ACCESS_FINE_LOCATION`: Track device location
- `ACCESS_BACKGROUND_LOCATION`: Monitor location when app is in background
- `RECORD_AUDIO`: Voice recognition
- `BLUETOOTH_CONNECT`: Smartwatch communication
- `POST_NOTIFICATIONS`: Display alerts
- `FOREGROUND_SERVICE`: Run protection service continuously
- `VIBRATE`: Device vibration alerts

## Privacy & Security
- All voice recognition processing happens on-device using Vosk
- Voice patterns are stored locally, never transmitted
- Location data is used only for proximity monitoring
- No data is collected or sent to external servers

## Future Enhancements
- Machine learning-based theft pattern detection
- Enhanced voice biometrics using neural networks
- Multi-device synchronization
- Remote device locking via smartwatch
- Integration with smart home systems

## Contributing
Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.

## License
See LICENSE file for details.

## Support
For issues and questions, please open an issue on GitHub.
