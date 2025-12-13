package com.protector.app.service;

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
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
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

public class ProtectionService extends Service implements SensorEventListener {
    private static final String CHANNEL_ID = "ProtectorServiceChannel";
    private static final int NOTIFICATION_ID = 1001;
    
    // Theft detection thresholds
    private static final float ACCELERATION_THRESHOLD = 12.0f; // m/s^2
    private static final long MOVEMENT_TIME_THRESHOLD = 2000; // 2 seconds
    
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    
    private SharedPreferences preferences;
    private NotificationManager notificationManager;
    private Vibrator vibrator;
    
    // State tracking
    private Location initialLocation;
    private Location currentLocation;
    private long lastHighAccelerationTime = 0;
    private boolean isTheftDetected = false;
    private float proximityRadius = 50.0f; // meters
    
    // Voice recognition components
    private VoiceRecognitionManager voiceRecognitionManager;

    @Override
    public void onCreate() {
        super.onCreate();
        
        preferences = getSharedPreferences("ProtectorPrefs", MODE_PRIVATE);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        
        proximityRadius = preferences.getFloat("proximity_radius", 50.0f);
        
        initializeSensors();
        initializeLocationTracking();
        initializeVoiceRecognition();
        
        startForeground(NOTIFICATION_ID, createNotification("Monitoring your device"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    private void initializeSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }
    
    private void initializeLocationTracking() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        LocationRequest locationRequest = new LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build();
        
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                
                for (Location location : locationResult.getLocations()) {
                    handleLocationUpdate(location);
                }
            }
        };
        
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    initialLocation = location;
                }
            });
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
    
    private void initializeVoiceRecognition() {
        boolean voiceAuthEnabled = preferences.getBoolean("voice_auth_enabled", false);
        if (voiceAuthEnabled) {
            voiceRecognitionManager = new VoiceRecognitionManager(this);
            voiceRecognitionManager.startListening(new VoiceRecognitionManager.VoiceCallback() {
                @Override
                public void onVoiceDetected(String text) {
                    handleVoiceCommand(text);
                }
                
                @Override
                public void onUnauthorizedVoice() {
                    sendAlert("Unauthorized Voice", "Voice not recognized. Device alert engaging.");
                }
            });
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
                } else if (currentTime - lastHighAccelerationTime > MOVEMENT_TIME_THRESHOLD) {
                    // Sustained high acceleration detected
                    if (!isTheftDetected) {
                        isTheftDetected = true;
                        handleTheftDetection();
                    }
                }
            } else {
                // Reset if acceleration drops
                lastHighAccelerationTime = 0;
                isTheftDetected = false;
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
        // Send message to smartwatch via Wearable API
        // This would use Google Wearable MessageClient
        // For now, just log the intent
        Intent intent = new Intent("com.protector.app.SMARTWATCH_ALERT");
        intent.putExtra("alert_type", alertType);
        sendBroadcast(intent);
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
        
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        
        if (voiceRecognitionManager != null) {
            voiceRecognitionManager.stopListening();
        }
    }
}
