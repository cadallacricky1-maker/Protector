# Error Handling & Resilience Guide

## Overview

The Protector app implements comprehensive error handling to ensure stable operation even when individual components fail. This document outlines the error handling strategy and implementation details.

## Architecture

### Centralized Error Handler

All errors flow through `ErrorHandler.java` utility class:

```java
ErrorHandler.handleError(context, "ComponentName", "operation", exception);
```

**Features:**
- Consistent error logging with component tags
- User-friendly error messages
- Automatic error recovery detection
- Graceful degradation support

## Error Handling by Component

### 1. ProtectionService

**Critical Operations with Error Handling:**

#### Sensor Initialization
```java
try {
    initializeBatteryOptimizedSensors();
} catch (Exception e) {
    ErrorHandler.handleError(this, "ProtectionService", "sensor initialization", e);
    // Falls back to alternative sensor or continues without it
}
```

**Graceful Degradation:**
- Significant Motion Sensor unavailable → Falls back to accelerometer
- Accelerometer unavailable → Logs warning, continues with location-only monitoring
- Service continues operating with reduced functionality rather than crashing

#### Location Tracking
```java
try {
    initializeBatteryOptimizedLocationTracking();
} catch (SecurityException e) {
    ErrorHandler.handleError(this, "ProtectionService", "location permission", e);
    // Notifies user about permission requirement
}
```

**Permission Handling:**
- Runtime permission checks before accessing location
- User-friendly messages when permissions denied
- Service degrades gracefully without location features

#### Voice Recognition
```java
try {
    voiceRecognitionManager = new VoiceRecognitionManager(this);
} catch (Exception e) {
    ErrorHandler.handleError(this, "ProtectionService", "voice init", e);
    // Continues without voice features
    return; // Early return prevents further voice operations
}
```

**Fallback Strategy:**
- Voice model load failure → Continues without voice authentication
- Microphone permission denied → Disables voice features
- Speech recognition errors → Logs and continues monitoring

#### Wear OS Communication
```java
try {
    wearCommunicator = new WearCommunicator(this);
} catch (Exception e) {
    ErrorHandler.handleError(this, "ProtectionService", "WearCommunicator", e);
    // Continues without watch support
}
```

**Graceful Degradation:**
- Watch not connected → Logs warning, phone-only mode
- Bluetooth off → Continues with local notifications
- Message send fails → Retries or logs error, doesn't crash

### 2. SubscriptionManager

**Billing Client Error Handling:**

```java
@Override
public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
    if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
        ErrorHandler.handleError(context, "SubscriptionManager", "billing setup", 
            new Exception(billingResult.getDebugMessage()));
        // Falls back to free tier
    }
}
```

**Purchase Flow Protection:**
- Network errors → Retry mechanism with exponential backoff
- User cancellation → Graceful return to previous screen
- Invalid product → Falls back to free tier with upgrade prompt

### 3. VoiceRecognitionManager

**Model Loading:**

```java
StorageService.unpack(context, "model-en-us", "model",
    (model) -> {
        this.model = model;
        initializeRecognizer();
    },
    (exception) -> {
        ErrorHandler.handleError(context, "VoiceRecognitionManager", "model load", exception);
        // Falls back to simple text matching
    });
```

**Recognition Errors:**
- Model unavailable → Disables voice features
- Audio input errors → Restarts recognition or disables temporarily
- Pattern matching failures → Counts attempts, triggers unauthorized alert

## User-Friendly Error Messages

### Translation Strategy

Technical exceptions → User-friendly messages:

| Technical Error | User Message |
|----------------|--------------|
| SecurityException (location) | "Location permission required. Please enable in Settings." |
| SecurityException (audio) | "Microphone permission required for voice features." |
| IOException | "Network error. Please check your connection." |
| Billing errors | "Subscription service unavailable. Try again later." |
| Sensor errors | "Sensor access required for theft detection." |

### Message Display

- **Toast notifications** for user-actionable errors
- **Silent logging** for internal errors that don't affect UX
- **Status indicators** in UI for degraded functionality

## Recovery Mechanisms

### Automatic Recovery

1. **Retry with Exponential Backoff:**
   ```java
   private void retryOperation(int attempt) {
       if (attempt < MAX_RETRIES && ErrorHandler.isRecoverable(lastException)) {
           long delay = (long) Math.pow(2, attempt) * 1000; // 1s, 2s, 4s, 8s
           handler.postDelayed(() -> executeOperation(), delay);
       }
   }
   ```

2. **Service Self-Healing:**
   - Service monitors its own health
   - Restarts failed components automatically
   - Falls back to low-power mode if errors persist

3. **Permission Re-Request:**
   - Detects permission revocation at runtime
   - Prompts user to re-grant if needed
   - Continues with reduced functionality if denied

### Manual Recovery

1. **User Actions:**
   - Settings screen to manually restart components
   - Clear data/cache option for corrupt states
   - Force stop and restart service

2. **Support Integration:**
   - Error logs collected for support requests
   - Diagnostic information available in settings
   - One-tap error reporting to developer

## Testing Error Conditions

### Unit Tests

```java
@Test
public void testGracefulDegradation_NoLocationPermission() {
    // Simulate permission denial
    when(context.checkSelfPermission(any())).thenReturn(PERMISSION_DENIED);
    
    service.initializeBatteryOptimizedLocationTracking();
    
    // Verify service continues without location
    assertNull(service.fusedLocationClient);
    assertTrue(service.isRunning());
}
```

### Integration Tests

1. **Revoke permissions during operation**
2. **Disconnect smartwatch mid-operation**
3. **Disable network during billing operations**
4. **Corrupt voice model files**
5. **Simulate sensor failures**

### Manual Testing Checklist

- [ ] Revoke location permission while service running
- [ ] Revoke microphone permission during voice recognition
- [ ] Disable Bluetooth while watch connected
- [ ] Turn off network during subscription check
- [ ] Force close app during critical operations
- [ ] Test on devices without specific sensors
- [ ] Test with corrupted SharedPreferences
- [ ] Test with full storage

## Logging Strategy

### Log Levels

- **ERROR**: Component failures requiring attention
- **WARNING**: Degraded functionality, but app continues
- **INFO**: Normal operational messages
- **DEBUG**: Detailed diagnostic information (removed in production)

### Production Logging

```java
if (BuildConfig.DEBUG) {
    Log.d(TAG, "Detailed diagnostic info");
} else {
    // Only ERROR and WARNING in production
    ErrorHandler.logWarning(component, message);
}
```

### Crash Reporting Integration Points

Ready for Firebase Crashlytics or similar:

```java
// In ErrorHandler.handleError()
if (CRASHLYTICS_ENABLED) {
    FirebaseCrashlytics.getInstance().recordException(e);
    FirebaseCrashlytics.getInstance().setCustomKey("component", component);
    FirebaseCrashlytics.getInstance().setCustomKey("operation", operation);
}
```

## Performance Impact

### Error Handling Overhead

- **Minimal**: Try-catch blocks have negligible performance impact in normal operation
- **Battery**: Error logging uses < 0.1% battery
- **Memory**: Error handler is stateless, no memory leaks

### Production Optimizations

- Verbose logging removed via ProGuard
- Debug assertions stripped in release builds
- Error message generation lazy-loaded

## Best Practices

### Do's

✅ Always wrap potentially failing operations in try-catch
✅ Provide user-friendly error messages
✅ Implement graceful degradation
✅ Log errors with sufficient context
✅ Test error conditions thoroughly

### Don'ts

❌ Don't show technical stack traces to users
❌ Don't crash the app for non-critical errors
❌ Don't retry forever without exponential backoff
❌ Don't log sensitive user data in errors
❌ Don't ignore errors silently without logging

## Future Enhancements

### Phase 2 Improvements

1. **Automatic Error Reporting:**
   - Firebase Crashlytics integration
   - Anonymous error telemetry
   - Trend analysis and alerts

2. **Advanced Recovery:**
   - Machine learning to predict failures
   - Proactive component restarts
   - Self-tuning retry strategies

3. **User Communication:**
   - In-app error history
   - Diagnostic mode for troubleshooting
   - Automated support ticket creation

## Summary

The Protector app's error handling strategy ensures:
- **Stability**: App never crashes due to component failures
- **User Experience**: Clear, actionable error messages
- **Reliability**: Graceful degradation maintains core functionality
- **Maintainability**: Centralized error handling for consistency
- **Debuggability**: Comprehensive logging for issue diagnosis

All critical paths are protected, and the app continues operating even when individual features fail.
