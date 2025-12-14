/*
 * Copyright (c) 2025 cadallacricky1-maker
 * All Rights Reserved.
 * 
 * PROPRIETARY AND CONFIDENTIAL
 * 
 * This software is the proprietary information of cadallacricky1-maker.
 * Unauthorized copying, distribution, or use is strictly prohibited.
 */

package com.protector.wear;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Listens for messages from the phone app
 * Receives alerts and status updates via Wearable Data API
 * 
 * BATTERY OPTIMIZATION:
 * - No processing, just receive and display
 * - Uses system's efficient data layer
 * - Minimal wake-ups
 */
public class WearMessageListenerService extends WearableListenerService {
    
    private static final String TAG = "WearMessageListener";
    private static final String CHANNEL_ID = "ProtectorAlerts";
    private static final String MESSAGE_PATH = "/protector/alert";
    
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "Message received: " + messageEvent.getPath());
        
        if (messageEvent.getPath().equals(MESSAGE_PATH)) {
            String message = new String(messageEvent.getData());
            processAlert(message);
        }
    }
    
    private void processAlert(String alertData) {
        // Parse alert data (format: "ALERT_TYPE|message")
        String[] parts = alertData.split("\\|", 2);
        String alertType = parts.length > 0 ? parts[0] : "UNKNOWN";
        String message = parts.length > 1 ? parts[1] : "Alert received";
        
        Log.d(TAG, "Alert: " + alertType + " - " + message);
        
        // Save to preferences
        SharedPreferences prefs = getSharedPreferences("ProtectorWear", MODE_PRIVATE);
        prefs.edit()
            .putString("last_alert_type", alertType)
            .putString("last_alert_time", getCurrentTime())
            .putBoolean("protection_active", true)
            .apply();
        
        // Broadcast to MainActivity if it's running
        Intent broadcastIntent = new Intent("com.protector.wear.ALERT");
        broadcastIntent.putExtra("alert_type", alertType);
        broadcastIntent.putExtra("message", message);
        sendBroadcast(broadcastIntent);
        
        // Show notification
        showNotification(alertType, message);
    }
    
    private void showNotification(String alertType, String message) {
        NotificationManager notificationManager = 
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        createNotificationChannel(notificationManager);
        
        String title;
        int priority;
        
        switch (alertType) {
            case "THEFT_DETECTED":
                title = "🚨 THEFT ALERT!";
                priority = NotificationCompat.PRIORITY_MAX;
                break;
            case "UNAUTHORIZED_VOICE":
                title = "🎤 Unauthorized Voice";
                priority = NotificationCompat.PRIORITY_HIGH;
                break;
            case "PROXIMITY_BREACH":
                title = "📍 Don't Forget Device";
                priority = NotificationCompat.PRIORITY_HIGH;
                break;
            case "GEOFENCE_EXIT":
                title = "🗺️ Geofence Alert";
                priority = NotificationCompat.PRIORITY_HIGH;
                break;
            default:
                title = "⚡ Protector Alert";
                priority = NotificationCompat.PRIORITY_DEFAULT;
                break;
        }
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(priority)
            .setVibrate(getVibrationPattern(alertType))
            .setAutoCancel(true);
        
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
    
    private void createNotificationChannel(NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Protector Alerts",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Security alerts from your phone");
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    private long[] getVibrationPattern(String alertType) {
        switch (alertType) {
            case "THEFT_DETECTED":
                return new long[]{0, 500, 100, 500, 100, 500};
            case "UNAUTHORIZED_VOICE":
                return new long[]{0, 300, 100, 300, 100, 300};
            default:
                return new long[]{0, 400, 200, 400};
        }
    }
    
    private String getCurrentTime() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }
}
