# Wear OS Integration

## Overview
Protector includes a companion Wear OS app that displays alerts from your phone directly on your smartwatch. Both apps are built from the same project and work together seamlessly.

## Architecture: Phone as Brain, Watch as Hand

### Phone App (Brain - All Processing)
- Runs all sensors and monitoring
- Performs all theft detection logic
- Manages location tracking
- Handles voice recognition
- Makes all decisions
- Sends alert commands to watch

### Wear OS App (Hand - Display Only)
- Receives alerts from phone
- Displays notifications on watch
- Vibrates for different alert types
- Shows protection status
- **Zero processing** - minimal battery impact

## Features

### Real-Time Alerts on Your Wrist
The Wear OS app displays all security alerts instantly:
- 🚨 **Theft Alert**: Device being moved away
- 📍 **Proximity Warning**: Don't forget your device
- 🗺️ **Geofence Alert**: Device left safe zone
- 🎤 **Unauthorized Voice**: Unknown voice detected

### Smart Vibration Patterns
Different vibration patterns for each alert type:
- **Theft**: Strong, repeated vibration (urgent)
- **Unauthorized Voice**: Quick, urgent vibration
- **Proximity/Geofence**: Moderate vibration
- **Status Update**: Gentle single vibration

### Minimal Battery Impact on Watch
- No sensors running on watch
- Receives messages only (ultra-efficient)
- Uses Wearable Data Layer (hardware-optimized)
- Display updates only when alerts occur
- **Estimated battery impact: <2% per day**

## Technical Implementation

### Communication Protocol
Uses Google's Wearable Data Layer API:
```
Phone → Wearable MessageClient → Watch
```

**Message Format:**
```
ALERT_TYPE|message
```

**Alert Types:**
- `THEFT_DETECTED`
- `PROXIMITY_BREACH`
- `GEOFENCE_EXIT`
- `UNAUTHORIZED_VOICE`
- `STATUS_UPDATE`

### Components

#### Phone App Components
1. **WearCommunicator** (`app/src/main/java/com/protector/app/wear/WearCommunicator.java`)
   - Sends messages to connected watches
   - Checks watch connection status
   - Handles asynchronous messaging

2. **ProtectionService** (Updated)
   - Integrated WearCommunicator
   - Sends alerts to watch automatically
   - Status updates on protection enable/disable

#### Wear OS App Components
1. **MainActivity** (`wear/src/main/java/com/protector/wear/MainActivity.java`)
   - Displays current protection status
   - Shows last alert received
   - Minimal UI optimized for small screens

2. **WearMessageListenerService** (`wear/src/main/java/com/protector/wear/WearMessageListenerService.java`)
   - Listens for messages from phone
   - Creates notifications
   - Triggers vibrations
   - Broadcasts to MainActivity if running

## Building and Deployment

### Project Structure
```
Protector/
├── app/              # Phone app module
│   ├── src/main/
│   │   ├── java/com/protector/app/
│   │   │   ├── MainActivity.java
│   │   │   ├── SubscriptionActivity.java
│   │   │   ├── service/
│   │   │   │   └── ProtectionService.java
│   │   │   └── wear/
│   │   │       └── WearCommunicator.java
│   │   └── AndroidManifest.xml
│   └── build.gradle
│
└── wear/             # Wear OS app module
    ├── src/main/
    │   ├── java/com/protector/wear/
    │   │   ├── MainActivity.java
    │   │   └── WearMessageListenerService.java
    │   └── AndroidManifest.xml
    └── build.gradle
```

### Build Instructions

#### Build Both Apps:
```bash
./gradlew build
```

#### Build Phone App Only:
```bash
./gradlew :app:build
```

#### Build Wear OS App Only:
```bash
./gradlew :wear:build
```

#### Install on Devices:
```bash
# Install phone app
./gradlew :app:installDebug

# Install watch app (watch must be connected via ADB or paired)
./gradlew :wear:installDebug
```

### Deployment Methods

#### Method 1: Via Google Play Store
- Upload both APKs as part of the same release
- Google Play automatically installs the watch app when the phone app is installed
- Users with paired watches get both apps

#### Method 2: Direct Installation
1. Enable ADB debugging on phone and watch
2. Connect phone via USB
3. Pair watch with phone
4. Enable ADB debugging over Bluetooth on watch
5. Run `adb devices` to see both devices
6. Install both APKs:
   ```bash
   adb -s <phone-device-id> install app-debug.apk
   adb -s <watch-device-id> install wear-debug.apk
   ```

#### Method 3: Side-by-Side Development
- Phone app runs on physical phone or emulator
- Wear OS app runs on Wear OS emulator or physical watch
- Apps communicate if on same network/paired

## Usage

### Setup
1. Install phone app on your phone
2. Install Wear OS app on your smartwatch
3. Ensure watch is paired with phone via Wear OS companion app
4. Open phone app and start protection

### During Protection
- Alerts appear on watch instantly
- Vibration patterns indicate alert type
- Tap watch notification to open Wear OS app
- See last alert and protection status

### Checking Status
- Open Protector on watch to see:
  - Current protection status (🛡️ PROTECTED or ⚠️ INACTIVE)
  - Last alert received
  - Time of last alert

## Battery Optimization

### Phone App
As documented in BATTERY_OPTIMIZATION.md:
- 8.2x longer battery life with optimizations
- Significant Motion Sensor for ultra-low power
- Adaptive location tracking
- On-demand voice recognition

### Wear OS App
**Additional optimizations for watch:**
- No sensors active on watch
- Receives messages only (push-based)
- Minimal UI updates
- Efficient Wearable Data Layer
- **Result: <2% battery impact per day**

### Combined System
- Phone does all processing (brain)
- Watch only displays (hand)
- Total battery optimization maintained
- Seamless user experience

## Troubleshooting

### Watch Not Receiving Alerts

**Check 1: Connection**
- Ensure watch is paired with phone
- Open Wear OS companion app on phone
- Verify watch shows as connected

**Check 2: Bluetooth**
- Bluetooth must be enabled on both devices
- Watch and phone should be within range

**Check 3: App Installation**
- Verify Protector is installed on both phone and watch
- Check both apps are the same version

**Check 4: Permissions**
- Ensure phone app has all required permissions
- Check watch app has notification permissions

### Alerts Not Vibrating on Watch

**Check 1: Do Not Disturb**
- Disable DND mode on watch
- Check theater mode is off

**Check 2: Notification Settings**
- Open watch Settings → Apps & notifications
- Find Protector app
- Ensure notifications are enabled
- Check vibration is enabled

**Check 3: Battery Saver**
- Disable battery saver on watch
- May restrict background processes

### Watch App Not Opening

**Check 1: Installation**
- Reinstall watch app
- Clear app cache
- Restart watch

**Check 2: Compatibility**
- Wear OS 3.0+ recommended
- Minimum: Wear OS 2.0 (SDK 28)

## Testing Communication

### Send Test Alert from Phone
Add this code to test in phone app:
```java
WearCommunicator wearComm = new WearCommunicator(context);
wearComm.sendAlert("THEFT_DETECTED", "Test alert");
```

### Check Watch Connection
```java
WearCommunicator wearComm = new WearCommunicator(context);
boolean connected = wearComm.isWatchConnected();
Log.d("Protector", "Watch connected: " + connected);
```

### Monitor Communication Logs
```bash
# Phone logs
adb -s <phone-id> logcat | grep WearCommunicator

# Watch logs
adb -s <watch-id> logcat | grep WearMessageListener
```

## Premium Features on Watch

The Wear OS app automatically reflects your Premium subscription status:
- All alerts available for Premium users
- Free users see limited features as per phone app
- Subscription managed entirely on phone
- Watch displays full functionality when Premium active

## Future Enhancements

### Planned Features:
1. **Interactive Actions**: Snooze alerts from watch
2. **Quick Controls**: Enable/disable protection from watch
3. **Complications**: Show status on watch face
4. **Tiles**: Quick access protection toggle
5. **Voice Commands**: Control via watch voice assistant
6. **Gesture Controls**: Wave to snooze, double-tap to view

### Community Requests:
- Multiple device monitoring from one watch
- Location history view on watch
- Battery status of phone on watch
- Custom vibration patterns

## Developer Notes

### Adding New Alert Types
1. Add alert type constant in both apps
2. Update `getAlertMessage()` in ProtectionService
3. Update `getAlertDisplayText()` in Wear MainActivity
4. Add vibration pattern in WearMessageListenerService
5. Test on both phone and watch

### Debugging
- Use Android Studio's paired devices feature
- Run both modules simultaneously
- Set breakpoints in message sending/receiving code
- Monitor logcat for both devices

### Performance Monitoring
- Watch battery usage in Settings → Battery
- Monitor message frequency in logs
- Check connection stability over time
- Verify alert delivery latency (<1 second target)

## Support

For issues specific to Wear OS integration:
1. Verify both apps are installed and updated
2. Check watch is properly paired
3. Review troubleshooting section above
4. Check device compatibility (Wear OS 2.0+)
5. Report issues with detailed logs from both devices

## Summary

The Wear OS integration provides:
✅ Seamless dual-app experience  
✅ Real-time alerts on your wrist  
✅ Smart vibration patterns  
✅ Minimal battery impact (<2%/day)  
✅ Zero processing on watch  
✅ Easy deployment as single project  
✅ Maintains phone-centric architecture  
✅ Premium features supported  

Both apps work together to provide comprehensive device protection with alerts delivered instantly to your smartwatch, all while maintaining exceptional battery efficiency.
