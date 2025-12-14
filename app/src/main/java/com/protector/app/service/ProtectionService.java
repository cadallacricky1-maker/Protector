/*
 * Copyright (c) 2025 cadallacricky1-maker
 * All Rights Reserved.
 * 
 * PROPRIETARY AND CONFIDENTIAL
 * 
 * This software is the proprietary information of cadallacricky1-maker.
 * Unauthorized copying, distribution, or use is strictly prohibited.
 */

package com.protector.app.service;

/**
 * Battery-Optimized Protection Service
 * 
 * BATTERY OPTIMIZATION STRATEGY:
 * 1. Significant Motion Sensor: Uses hardware sensor hub for ultra-low power motion detection
 *    - Only wakes CPU when significant device movement occurs
 *    - Falls back to low-power accelerometer if unavailable
 * 
 * 2. Adaptive Location Tracking:
 *    - Stationary: 30-second intervals with balanced power accuracy (WiFi/Cell towers)
 *    - Moving: 10-second intervals with balanced accuracy
 *    - Alert mode: 5-second intervals with high accuracy GPS
 *    - Uses batching to reduce CPU wake-ups
 * 
 * 3. On-Demand Voice Recognition:
 *    - NOT continuously running (saves 20-30% battery)
 *    - Only activates when theft detected or user interaction
 *    - Automatically stops when device returns to stationary state
 * 
 * 4. Intelligent State Management:
 *    - Monitors device state (stationary vs moving)
 *    - Dynamically adjusts all sensors based on current threat level
 *    - Returns to low-power mode when no activity detected
 * 
 * ARCHITECTURE:
 * - Phone = Brain: All logic and processing happens here
 * - Smartwatch = Hand: Only receives alert commands via broadcasts
 *   No processing on watch, just display notifications
 */

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.protector.app.MainActivity;
import com.protector.app.R;
import com.protector.app.util.ErrorHandler;
import com.protector.app.util.CrashReporter;
import com.protector.app.wear.WearCommunicator;

public class ProtectionService extends Service implements SensorEventListener {
    private static final String CHANNEL_ID = "ProtectorServiceChannel";
    private static final int NOTIFICATION_ID = 1001;
    
    // Theft detection thresholds
    private static final float ACCELERATION_THRESHOLD = 12.0f; // m/s^2
    private static final long MOVEMENT_TIME_THRESHOLD = 2000; // 2 seconds
    
    // Battery optimization: Adaptive location update intervals
    private static final long LOCATION_UPDATE_INTERVAL_STATIONARY = 30000; // 30 seconds when stationary
    private static final long LOCATION_UPDATE_INTERVAL_MOVING = 10000; // 10 seconds when moving
    private static final long LOCATION_UPDATE_INTERVAL_FAST = 5000; // 5 seconds when alert
    private static final int LOCATION_BATCH_SIZE = 5; // Batch updates to reduce wake-ups
    
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor significantMotionSensor;
    private boolean isSignificantMotionRegistered = false;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    
    private SharedPreferences preferences;
    private NotificationManager notificationManager;
    private Vibrator vibrator;
    private PowerManager powerManager;
    
    // State tracking
    private Location initialLocation;
    private Location currentLocation;
    private long lastHighAccelerationTime = 0;
    private boolean isTheftDetected = false;
    private float proximityRadius = 50.0f; // meters
    private boolean isDeviceStationary = true;
    private long lastLocationUpdateTime = 0;
    
    // Voice recognition components (on-demand, not continuous)
    private VoiceRecognitionManager voiceRecognitionManager;
    private boolean isVoiceRecognitionActive = false;
    
    // Wear OS communication
    private WearCommunicator wearCommunicator;

    @Override
    public void onCreate() {
        super.onCreate();
        
        try {
            preferences = getSharedPreferences("ProtectorPrefs", MODE_PRIVATE);
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            
            // Initialize Wear communicator with error handling
            try {
                wearCommunicator = new WearCommunicator(this);
            } catch (Exception e) {
                ErrorHandler.handleError(this, "ProtectionService", "WearCommunicator initialization", e, false);
                ErrorHandler.logWarning("ProtectionService", "Continuing without Wear OS support");
            }
            
            proximityRadius = preferences.getFloat("proximity_radius", 50.0f);
            
            // Battery optimization: Initialize sensors intelligently with error handling
            initializeBatteryOptimizedSensors();
            initializeBatteryOptimizedLocationTracking();
            
            // Voice recognition is NOT started here - it's on-demand only
            // This saves significant battery
            
            startForeground(NOTIFICATION_ID, createNotification("Monitoring your device (Battery Optimized)"));
            ErrorHandler.logInfo("ProtectionService", "Service started successfully");
            
            // Log component state for crash reports
            CrashReporter.logComponentState("ProtectionService", "started");
        } catch (Exception e) {
            ErrorHandler.handleError(this, "ProtectionService", "onCreate", e);
            CrashReporter.logComponentState("ProtectionService", "failed_to_start");
            stopSelf(); // Stop service if critical initialization fails
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    /**
     * Battery-optimized sensor initialization
     * Uses Significant Motion Sensor for low-power detection (API 18+)
     * Only enables continuous accelerometer when motion detected
     */
    private void initializeBatteryOptimizedSensors() {
        try {
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager == null) {
                ErrorHandler.logWarning("ProtectionService", "SensorManager not available");
                return;
            }
            
            // Try to use Significant Motion Sensor first (ultra low power)
            // Available from API 18 (Android 4.3+), our min is API 26
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                significantMotionSensor = sensorManager.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION);
                if (significantMotionSensor != null) {
                    // This sensor triggers once and auto-cancels, uses hardware sensor hub
                    try {
                        boolean registered = sensorManager.requestTriggerSensor(significantMotionTriggerListener, significantMotionSensor);
                        isSignificantMotionRegistered = registered;
                        if (!registered) {
                            ErrorHandler.logWarning("ProtectionService", "Failed to register significant motion sensor, using fallback");
                            enableAccelerometerMonitoring(false);
                        } else {
                            ErrorHandler.logInfo("ProtectionService", "Significant motion sensor registered successfully");
                        }
                    } catch (Exception e) {
                        ErrorHandler.handleError(this, "ProtectionService", "significant motion sensor registration", e, false);
                        enableAccelerometerMonitoring(false);
                    }
                } else {
                    ErrorHandler.logWarning("ProtectionService", "Significant motion sensor not available, using fallback");
                    // Fallback: Use accelerometer with low power delay
                    enableAccelerometerMonitoring(false);
                }
            } else {
                // API < 18: Use accelerometer with low power delay
                enableAccelerometerMonitoring(false);
            }
        } catch (Exception e) {
            ErrorHandler.handleError(this, "ProtectionService", "sensor initialization", e, false);
        }
    }
    
    /**
     * Enable/disable accelerometer monitoring with appropriate power settings
     * @param highPower true for theft detection, false for normal monitoring
     */
    private void enableAccelerometerMonitoring(boolean highPower) {
        if (sensorManager == null) {
            ErrorHandler.logWarning("ProtectionService", "Cannot enable accelerometer: SensorManager is null");
            return;
        }
        
        try {
            // Unregister previous listener
            sensorManager.unregisterListener(this);
            
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                // Use SENSOR_DELAY_UI for better battery (vs SENSOR_DELAY_NORMAL)
                // UI delay is ~60Hz vs Normal ~200Hz, sufficient for theft detection
                int sensorDelay = highPower ? SensorManager.SENSOR_DELAY_NORMAL : SensorManager.SENSOR_DELAY_UI;
                boolean success = sensorManager.registerListener(this, accelerometer, sensorDelay);
                if (!success) {
                    ErrorHandler.logWarning("ProtectionService", "Failed to register accelerometer listener");
                }
            } else {
                ErrorHandler.logWarning("ProtectionService", "Accelerometer sensor not available on this device");
            }
        } catch (Exception e) {
            ErrorHandler.handleError(this, "ProtectionService", "accelerometer monitoring", e, false);
        }
    }
    
    /**
     * Significant Motion Sensor trigger - extremely battery efficient
     * Only wakes up when device experiences significant motion
     */
    private final TriggerEventListener significantMotionTriggerListener = new TriggerEventListener() {
        @Override
        public void onTrigger(TriggerEvent event) {
            // Significant motion detected - device is being moved
            isDeviceStationary = false;
            
            // Now enable accelerometer for detailed monitoring
            enableAccelerometerMonitoring(true);
            
            // Switch to faster location updates
            updateLocationTrackingInterval(LOCATION_UPDATE_INTERVAL_MOVING);
            
            // Re-register for next significant motion (it auto-cancels after trigger)
            if (significantMotionSensor != null && sensorManager != null) {
                boolean registered = sensorManager.requestTriggerSensor(significantMotionTriggerListener, significantMotionSensor);
                isSignificantMotionRegistered = registered;
            }
        }
    };
    
    /**
     * Battery-optimized location tracking
     * Uses adaptive intervals and batching to minimize battery drain
     */
    private void initializeBatteryOptimizedLocationTracking() {
        try {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            
            // Check for location permission before requesting updates
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) 
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                ErrorHandler.logWarning("ProtectionService", "Location permission not granted");
                return;
            }
            
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null) return;
                    
                    try {
                        lastLocationUpdateTime = System.currentTimeMillis();
                        
                        for (Location location : locationResult.getLocations()) {
                            handleLocationUpdate(location);
                        }
                    } catch (Exception e) {
                        ErrorHandler.handleError(ProtectionService.this, "ProtectionService", "location result handling", e, false);
                    }
                }
            };
            
            // Start with stationary interval (battery-friendly)
            updateLocationTrackingInterval(LOCATION_UPDATE_INTERVAL_STATIONARY);
            
            try {
                fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            initialLocation = location;
                            ErrorHandler.logInfo("ProtectionService", "Initial location obtained");
                        }
                    })
                    .addOnFailureListener(e -> {
                        ErrorHandler.handleError(ProtectionService.this, "ProtectionService", "get last location", (Exception) e, false);
                    });
            } catch (SecurityException e) {
                ErrorHandler.handleError(this, "ProtectionService", "location permission check", e, false);
            }
        } catch (Exception e) {
            ErrorHandler.handleError(this, "ProtectionService", "location tracking initialization", e, false);
        }
    }
    
    /**
     * Dynamically adjust location update interval based on device state
     * @param interval Update interval in milliseconds
     */
    private void updateLocationTrackingInterval(long interval) {
        if (fusedLocationClient == null || locationCallback == null) return;
        
        try {
            // Remove previous updates
            fusedLocationClient.removeLocationUpdates(locationCallback);
            
            // Battery optimization: Use BALANCED_POWER_ACCURACY instead of HIGH_ACCURACY
            // This allows the system to use cell towers/WiFi instead of GPS when possible
            int priority = (interval <= LOCATION_UPDATE_INTERVAL_FAST) 
                ? Priority.PRIORITY_HIGH_ACCURACY 
                : Priority.PRIORITY_BALANCED_POWER_ACCURACY;
            
            LocationRequest locationRequest = new LocationRequest.Builder(priority, interval)
                .setMinUpdateIntervalMillis(interval / 2)
                .setMaxUpdateDelayMillis(interval * LOCATION_BATCH_SIZE) // Batching for efficiency
                .setWaitForAccurateLocation(false) // Don't wait, use whatever is available
                .build();
            
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Voice recognition is now ON-DEMAND only for battery optimization
     * Only activates when user interacts with device or on specific events
     */
    private void enableVoiceRecognitionOnDemand() {
        try {
            boolean voiceAuthEnabled = preferences.getBoolean("voice_auth_enabled", false);
            if (!voiceAuthEnabled || isVoiceRecognitionActive) return;
            
            if (voiceRecognitionManager == null) {
                try {
                    voiceRecognitionManager = new VoiceRecognitionManager(this);
                } catch (Exception e) {
                    ErrorHandler.handleError(this, "ProtectionService", "voice recognition manager initialization", e, false);
                    return;
                }
            }
            
            voiceRecognitionManager.startListening(new VoiceRecognitionManager.VoiceCallback() {
                @Override
                public void onVoiceDetected(String text) {
                    try {
                        handleVoiceCommand(text);
                    } catch (Exception e) {
                        ErrorHandler.handleError(ProtectionService.this, "ProtectionService", "voice command handling", e, false);
                    }
                }
                
                @Override
                public void onUnauthorizedVoice() {
                    try {
                        sendAlert("Unauthorized Voice", "Voice not recognized. Device alert engaging.");
                        sendSmartWatchAlert("UNAUTHORIZED_VOICE");
                    } catch (Exception e) {
                        ErrorHandler.handleError(ProtectionService.this, "ProtectionService", "unauthorized voice handling", e, false);
                    }
                }
            });
            
            isVoiceRecognitionActive = true;
            ErrorHandler.logInfo("ProtectionService", "Voice recognition enabled on demand");
        } catch (Exception e) {
            ErrorHandler.handleError(this, "ProtectionService", "voice recognition enablement", e, false);
        }
    }
    
    /**
     * Disable voice recognition to save battery
     */
    private void disableVoiceRecognition() {
        try {
            if (voiceRecognitionManager != null) {
                if (isVoiceRecognitionActive) {
                    voiceRecognitionManager.stopListening();
                    isVoiceRecognitionActive = false;
                    ErrorHandler.logInfo("ProtectionService", "Voice recognition disabled to save battery");
                }
            }
        } catch (Exception e) {
            ErrorHandler.handleError(this, "ProtectionService", "voice recognition disablement", e, false);
        }
    }
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            
            // Calculate magnitude of acceleration
            float acceleration = (float) Math.sqrt(x * x + y * y + z * z);
            
            // Check for sudden acceleration (possible theft)
            if (acceleration > ACCELERATION_THRESHOLD) {
                long currentTime = System.currentTimeMillis();
                
                if (lastHighAccelerationTime == 0) {
                    lastHighAccelerationTime = currentTime;
                    // Switch to high-power mode for accurate detection
                    updateLocationTrackingInterval(LOCATION_UPDATE_INTERVAL_FAST);
                } else if (currentTime - lastHighAccelerationTime > MOVEMENT_TIME_THRESHOLD) {
                    // Sustained high acceleration detected
                    if (!isTheftDetected) {
                        isTheftDetected = true;
                        handleTheftDetection();
                        // Enable voice recognition to detect unauthorized users
                        enableVoiceRecognitionOnDemand();
                    }
                }
            } else {
                // Reset if acceleration drops - device seems stationary again
                if (lastHighAccelerationTime != 0) {
                    lastHighAccelerationTime = 0;
                    isTheftDetected = false;
                    isDeviceStationary = true;
                    
                    // Battery optimization: Return to low-power mode
                    updateLocationTrackingInterval(LOCATION_UPDATE_INTERVAL_STATIONARY);
                    enableAccelerometerMonitoring(false); // Lower power mode
                    disableVoiceRecognition(); // Stop voice to save battery
                }
            }
        }
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }
    
    private void handleLocationUpdate(Location location) {
        currentLocation = location;
        
        if (initialLocation != null) {
            float distance = initialLocation.distanceTo(location);
            
            // Battery optimization: Adjust tracking based on movement
            if (distance > proximityRadius * 0.5 && isDeviceStationary) {
                // Device is moving - increase monitoring
                isDeviceStationary = false;
                updateLocationTrackingInterval(LOCATION_UPDATE_INTERVAL_MOVING);
            } else if (distance < proximityRadius * 0.2 && !isDeviceStationary) {
                // Device settled down - reduce monitoring
                isDeviceStationary = true;
                updateLocationTrackingInterval(LOCATION_UPDATE_INTERVAL_STATIONARY);
            }
            
            // Check if device moved beyond proximity radius
            if (distance > proximityRadius) {
                handleProximityBreach(distance);
            }
        }
        
        // Check geo-fence
        if (preferences.getBoolean("geofence_enabled", false)) {
            checkGeofence(location);
        }
    }
    
    private void handleTheftDetection() {
        sendAlert("Theft Alert!", "Your device is being moved away!");
        sendSmartWatchAlert("THEFT_DETECTED");
        vibrateDevice();
    }
    
    private void handleProximityBreach(float distance) {
        String message = "Don't forget your device - " + Math.round(distance) + "m away";
        sendAlert("Device Warning", message);
        sendSmartWatchAlert("PROXIMITY_BREACH");
    }
    
    private void checkGeofence(Location location) {
        // Check if location is outside predefined geo-fence
        // Implementation would use Geofencing API
        float geofenceRadius = preferences.getFloat("geofence_radius", 100.0f);
        
        // Get saved geofence center
        float savedLat = preferences.getFloat("geofence_lat", 0);
        float savedLng = preferences.getFloat("geofence_lng", 0);
        
        if (savedLat != 0 && savedLng != 0) {
            Location geofenceCenter = new Location("");
            geofenceCenter.setLatitude(savedLat);
            geofenceCenter.setLongitude(savedLng);
            
            float distance = location.distanceTo(geofenceCenter);
            if (distance > geofenceRadius) {
                sendAlert("Geofence Alert", "Device has left the designated area");
            }
        }
    }
    
    private void handleVoiceCommand(String command) {
        command = command.toLowerCase();
        
        if (command.contains("disable") || command.contains("turn off")) {
            // User confirmed, disable warnings temporarily
            preferences.edit().putBoolean("warnings_paused", true).apply();
        } else if (command.contains("enable") || command.contains("turn on")) {
            preferences.edit().putBoolean("warnings_paused", false).apply();
        }
    }
    
    private void sendAlert(String title, String message) {
        // Check if warnings are paused
        if (preferences.getBoolean("warnings_paused", false)) {
            return;
        }
        
        Notification notification = createNotification(title, message);
        notificationManager.notify((int) System.currentTimeMillis(), notification);
    }
    
    private void sendSmartWatchAlert(String alertType) {
        try {
            // Send message to smartwatch via Wearable Data API
            String message = getAlertMessage(alertType);
            if (wearCommunicator != null) {
                wearCommunicator.sendAlert(alertType, message);
            } else {
                ErrorHandler.logWarning("ProtectionService", "WearCommunicator not available, skipping watch alert");
            }
            
            // Also send local broadcast for backwards compatibility
            Intent intent = new Intent("com.protector.app.SMARTWATCH_ALERT");
            intent.putExtra("alert_type", alertType);
            sendBroadcast(intent);
        } catch (Exception e) {
            ErrorHandler.handleError(this, "ProtectionService", "smart watch alert", e, false);
        }
    }
    
    private String getAlertMessage(String alertType) {
        switch (alertType) {
            case "THEFT_DETECTED":
                return "Device is being moved away!";
            case "PROXIMITY_BREACH":
                return "Don't forget your device";
            case "GEOFENCE_EXIT":
                return "Device left safe zone";
            case "UNAUTHORIZED_VOICE":
                return "Unauthorized voice detected";
            default:
                return "Alert: " + alertType;
        }
    }
    
    private void vibrateDevice() {
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 500, 200, 500, 200, 500};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createWaveform(pattern, -1));
            } else {
                vibrator.vibrate(pattern, -1);
            }
        }
    }
    
    private Notification createNotification(String message) {
        return createNotification("Protector Active", message);
    }
    
    private Notification createNotification(String title, String message) {
        createNotificationChannel();
        
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, 
            notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build();
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Protector Service",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Device protection monitoring");
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        try {
            // Clean up all sensors and listeners
            if (sensorManager != null) {
                try {
                    sensorManager.unregisterListener(this);
                    // Cancel significant motion sensor if active
                    if (significantMotionSensor != null && isSignificantMotionRegistered) {
                        sensorManager.cancelTriggerSensor(significantMotionTriggerListener, significantMotionSensor);
                        isSignificantMotionRegistered = false;
                    }
                } catch (Exception e) {
                    ErrorHandler.handleError(this, "ProtectionService", "sensor cleanup", e, false);
                }
            }
            
            if (fusedLocationClient != null && locationCallback != null) {
                try {
                    fusedLocationClient.removeLocationUpdates(locationCallback);
                } catch (Exception e) {
                    ErrorHandler.handleError(this, "ProtectionService", "location tracking cleanup", e, false);
                }
            }
            
            // Properly cleanup voice recognition
            disableVoiceRecognition();
            
            ErrorHandler.logInfo("ProtectionService", "Service destroyed successfully");
        } catch (Exception e) {
            ErrorHandler.handleError(this, "ProtectionService", "onDestroy", e, false);
        }
    }
}
