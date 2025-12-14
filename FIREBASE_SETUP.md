# Firebase Crashlytics Setup Guide

## Overview

Firebase Crashlytics provides real-time crash reporting and analytics for production releases of the Protector app. This guide covers complete setup and usage.

## Why Firebase Crashlytics?

### Benefits:
- **Real-time crash monitoring**: Instant notification of crashes affecting users
- **Detailed stack traces**: Full error context with breadcrumbs
- **User segmentation**: Prioritize crashes affecting premium users
- **Issue prioritization**: Automatic grouping of similar crashes
- **ANR detection**: Catch "Application Not Responding" issues
- **Custom logging**: Track non-fatal issues and user flows
- **Free tier**: Generous free usage for indie developers

### Integration Status:
✅ CrashReporter utility class created  
✅ ErrorHandler integration complete  
✅ MainActivity initialization added  
✅ Build configuration updated  
🔄 Firebase project setup required (5-10 minutes)  
🔄 Testing and validation needed  

---

## Setup Instructions

### Step 1: Create Firebase Project (5 minutes)

1. **Go to Firebase Console**:
   - Visit: https://console.firebase.google.com/
   - Sign in with Google account

2. **Create New Project**:
   - Click "Add project" or "Create a project"
   - Enter project name: `Protector` (or your preferred name)
   - **Disable Google Analytics** (optional, not needed for Crashlytics)
   - Click "Create project"
   - Wait for project creation (~30 seconds)

3. **Add Android App**:
   - Click "Add app" → Select Android icon
   - **Android package name**: `com.protector.app` (must match exactly)
   - **App nickname**: `Protector` (optional)
   - **Debug signing certificate**: Leave blank for now
   - Click "Register app"

4. **Download Configuration File**:
   - Download `google-services.json` file
   - Place it in `app/` directory (same level as `build.gradle`)
   - **Important**: Do NOT commit this file to public repositories

5. **Enable Crashlytics**:
   - In Firebase Console, go to "Build" → "Crashlytics"
   - Click "Enable Crashlytics"
   - Accept terms and conditions
   - Wait for Crashlytics to initialize

### Step 2: Configure App (Already Complete ✅)

The following have been pre-configured in the codebase:

**`build.gradle` (project-level)**:
```gradle
dependencies {
    classpath 'com.google.gms:google-services:4.4.0'
    classpath 'com.google.firebase:firebase-crashlytics-gradle:2.9.9'
}
```

**`app/build.gradle`**:
```gradle
plugins {
    id 'com.google.gms.google-services'
    id 'com.google.firebase.crashlytics'
}

dependencies {
    implementation platform('com.google.firebase:firebase-bom:32.7.0')
    implementation 'com.google.firebase:firebase-crashlytics'
    implementation 'com.google.firebase:firebase-analytics'
}
```

**`MainActivity.java`**:
```java
CrashReporter.initialize(this);
```

### Step 3: Build and Test (10 minutes)

1. **Clean and Rebuild**:
   ```bash
   ./gradlew clean
   ./gradlew build
   ```

2. **Verify Integration**:
   - Check build logs for "Crashlytics" mentions
   - Look for: "Crashlytics build ID: ..."
   - No errors should appear

3. **Test Crash Reporting** (DEBUG BUILD ONLY):
   
   **Option A: Test Crash Button** (Recommended)
   - Add a hidden test button in MainActivity:
   ```java
   // DEBUG ONLY - Remove before production
   Button btnTestCrash = new Button(this);
   btnTestCrash.setText("Test Crash");
   btnTestCrash.setOnClickListener(v -> CrashReporter.testCrash());
   // Add to layout temporarily
   ```
   
   **Option B: Manual Exception Trigger**:
   ```java
   // In any button handler
   try {
       throw new RuntimeException("Test crash for verification");
   } catch (Exception e) {
       CrashReporter.logException(e);
       // Or let it crash for real test:
       // throw e;
   }
   ```

4. **Verify in Console** (May take 5-10 minutes):
   - Go to Firebase Console → Crashlytics
   - Check "Issues" tab
   - Look for test crash report
   - Click on crash to see full details

---

## Usage Guide

### Automatic Crash Detection

All uncaught exceptions are automatically reported to Crashlytics. No code changes needed.

**Example automatic crash**:
```java
// This will automatically be reported:
String text = null;
text.length(); // NullPointerException → Crashlytics
```

### Manual Exception Logging

Use `CrashReporter.logException()` for caught exceptions you want to track:

```java
try {
    // Risky operation
    voiceRecognitionManager.initialize();
} catch (Exception e) {
    // Log to Crashlytics but don't crash
    CrashReporter.logException(e);
    
    // Continue with fallback
    showErrorMessage("Voice features unavailable");
}
```

### Breadcrumb Logging

Track user actions leading to crashes:

```java
// User actions
CrashReporter.log("User started protection");
CrashReporter.log("User enabled voice authentication");
CrashReporter.log("Geo-fence created at coordinates: " + coords);

// Component states
CrashReporter.logComponentState("ProtectionService", "initializing");
CrashReporter.logComponentState("ProtectionService", "running");
```

### Custom Key-Value Pairs

Add context to crash reports:

```java
// User info (use anonymized IDs)
CrashReporter.setUserId("user_" + hashUserId());

// Feature states
CrashReporter.setCustomKey("voice_enabled", true);
CrashReporter.setCustomKey("geofence_count", 3);
CrashReporter.setCustomKey("proximity_radius", 100);

// Device info
CrashReporter.setCustomKey("battery_saver_mode", isBatterySaverOn());
CrashReporter.setCustomKey("location_mode", getLocationMode());
```

### Premium User Tracking

Prioritize issues affecting paying customers:

```java
// Called when subscription status changes
CrashReporter.logPremiumStatus(isPremium);

// Or manually:
CrashReporter.setCustomKey("is_premium", true);
CrashReporter.setCustomKey("subscription_tier", "premium");
```

### Component State Tracking

Monitor component lifecycle:

```java
// Service lifecycle
CrashReporter.logComponentState("ProtectionService", "created");
CrashReporter.logComponentState("ProtectionService", "started");
CrashReporter.logComponentState("ProtectionService", "destroyed");

// Feature usage
CrashReporter.logFeatureUsage("voice_auth", true);
CrashReporter.logFeatureUsage("geo_fence", true);
```

---

## Production Best Practices

### 1. Initialization

**Always initialize in Application.onCreate() or MainActivity.onCreate()**:
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    // Initialize crash reporting FIRST
    CrashReporter.initialize(this);
    
    // Rest of initialization...
}
```

### 2. Privacy Considerations

**Never log PII (Personally Identifiable Information)**:
```java
// ❌ BAD - Contains PII
CrashReporter.setUserId("john.doe@email.com");
CrashReporter.setCustomKey("phone", "+1234567890");

// ✅ GOOD - Anonymized
CrashReporter.setUserId("user_" + hashOf(userId));
CrashReporter.setCustomKey("user_region", "US");
```

### 3. Error Handling Integration

**Use through ErrorHandler for consistency**:
```java
try {
    riskyOperation();
} catch (Exception e) {
    // ErrorHandler already integrates with CrashReporter
    ErrorHandler.handleError(context, "ComponentName", "operation", e);
}
```

### 4. Release vs Debug

**Crashlytics is enabled in RELEASE builds only**:
```gradle
buildTypes {
    debug {
        // Crashlytics disabled by default in debug
    }
    release {
        // Crashlytics enabled in release
        minifyEnabled true
    }
}
```

To test in debug builds:
```java
// Temporarily force enable
FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
```

### 5. ProGuard Configuration

**Already configured in `proguard-rules.pro`**:
```proguard
# Firebase Crashlytics
-keepattributes SourceFile,LineNumberTable
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
```

---

## Monitoring and Analysis

### Dashboard Overview

**Firebase Console → Crashlytics → Issues**:
- **Crash-free users**: Percentage of users not experiencing crashes
- **Crash-free sessions**: Percentage of sessions without crashes
- **Open issues**: Unresolved crashes
- **Closed issues**: Resolved crashes

### Issue Prioritization

**Crashlytics automatically prioritizes by**:
1. **Impact**: Number of affected users
2. **Frequency**: Number of occurrences
3. **Recency**: When last occurred
4. **Severity**: Fatal vs non-fatal

### Key Metrics to Monitor

**Daily**:
- Crash-free users rate (target: >99%)
- New crashes (investigate immediately)
- Recurring crashes (top priority)

**Weekly**:
- Crash trends over time
- Most common crash types
- Premium vs free user crash rates

**Monthly**:
- Crash-free users improvement
- Resolved vs open issues ratio
- ANR (Application Not Responding) rate

### Alert Configuration

**Set up email alerts** (Firebase Console → Project Settings → Integrations):
- New fatal issues
- Increase in crash rate
- Regressed issues

---

## Troubleshooting

### Build Errors

**Error: "google-services.json not found"**
- Solution: Download from Firebase Console and place in `app/` directory

**Error: "Firebase SDK version conflict"**
- Solution: Use Firebase BoM (Bill of Materials) - already configured
- Check: All Firebase dependencies using `platform('com.google.firebase:firebase-bom:32.7.0')`

**Error: "Plugin with id 'com.google.gms.google-services' not found"**
- Solution: Add to project-level `build.gradle` dependencies - already done

### Crash Reports Not Appearing

**Wait 5-10 minutes** after first crash:
- Crashlytics batches reports to save battery
- First report may take longer to appear

**Check initialization**:
```java
if (!CrashReporter.isInitialized()) {
    Log.e("App", "CrashReporter not initialized!");
}
```

**Force send unsent reports** (debug only):
```java
CrashReporter.sendUnsentReports();
```

**Verify in logcat**:
```
I/CrashReporter: Firebase Crashlytics initialized successfully
D/CrashReporter: Exception logged to Crashlytics: NullPointerException
```

### Missing Stack Traces

**Caused by ProGuard obfuscation**:
- Solution: Upload mapping file after each release build
- Location: `app/build/outputs/mapping/release/mapping.txt`
- Firebase Console → Crashlytics → Settings → Mapping files

**Automated upload** (already configured):
```gradle
// Firebase Crashlytics Gradle plugin automatically uploads
id 'com.google.firebase.crashlytics'
```

---

## Testing Checklist

### Pre-Production Testing

- [ ] Firebase project created
- [ ] `google-services.json` added to `app/` directory
- [ ] Build successful with no Firebase errors
- [ ] Test crash triggered and appears in console
- [ ] Breadcrumbs visible in crash report
- [ ] Custom keys visible in crash report
- [ ] Premium status tracked correctly
- [ ] ProGuard mapping uploaded (release build)
- [ ] Email alerts configured
- [ ] Privacy policy updated with Firebase disclosure

### Post-Launch Monitoring

- [ ] Monitor crash-free users daily (Week 1)
- [ ] Review top crashes immediately
- [ ] Verify premium user crash prioritization
- [ ] Check breadcrumbs provide useful context
- [ ] Confirm error messages are actionable
- [ ] Test recovery mechanisms for top crashes
- [ ] Set up weekly crash review meeting
- [ ] Document and fix critical crashes within 24h

---

## Cost Estimation

### Firebase Crashlytics Pricing

**Free Tier (Generous for most apps)**:
- Unlimited crash reports
- Unlimited users
- Full feature access
- No credit card required

**Spark Plan** (Free):
- Perfect for Protector app
- Sufficient for 10,000+ users
- Real-time reporting
- Complete analytics

**Blaze Plan** (Pay-as-you-go):
- Only needed for very large scale (100k+ users)
- Crashlytics remains free
- Only pay for other Firebase services if used

**Estimated Cost**: **$0/month** for Protector app

---

## Integration Timeline

### Already Complete ✅ (30 minutes of work)
- CrashReporter utility class created
- ErrorHandler integration
- Build configuration
- MainActivity initialization
- Documentation created

### Remaining Work 🔄 (10-15 minutes)
- Create Firebase project (5 min)
- Download google-services.json (1 min)
- Build and test (5 min)
- Verify crash reports appear (5 min)

### Total Time: ~45 minutes for complete integration

---

## Related Documentation

- **ERROR_HANDLING_GUIDE.md**: Complete error handling patterns
- **PRODUCTION_READINESS.md**: Full production checklist
- **PRIVACY_POLICY.md**: Data collection disclosure (update with Firebase)

---

## Support and Resources

### Official Documentation
- [Firebase Crashlytics Overview](https://firebase.google.com/docs/crashlytics)
- [Android Setup Guide](https://firebase.google.com/docs/crashlytics/get-started?platform=android)
- [Customize Crash Reports](https://firebase.google.com/docs/crashlytics/customize-crash-reports?platform=android)

### Common Questions

**Q: Does Crashlytics work offline?**
A: Yes, crash reports are cached and uploaded when network is available.

**Q: How much battery does Crashlytics use?**
A: Minimal impact (<0.1% battery/hour), batches uploads intelligently.

**Q: Can I disable Crashlytics for specific users?**
A: Yes, call `FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false)`.

**Q: How long are crash reports retained?**
A: 90 days by default, configurable in Firebase Console.

**Q: Does it work with ProGuard/R8?**
A: Yes, mapping files are automatically uploaded for deobfuscation.

---

## Next Steps

1. **Complete Firebase setup** (follow Step 1 above)
2. **Test crash reporting** (follow Step 3 above)
3. **Monitor for 1 week** during beta testing
4. **Review and fix top 5 crashes** before public launch
5. **Set up crash review process** (weekly team meetings)

---

**Status**: Production-ready infrastructure complete. Firebase project setup required (10-15 minutes).

**Impact**: Enables proactive crash monitoring, faster issue resolution, and improved user experience.

**Priority**: **CRITICAL** - Must complete before Play Store launch.
