/*
 * Copyright (c) 2025 cadallacricky1-maker
 * All Rights Reserved.
 * 
 * PROPRIETARY AND CONFIDENTIAL
 * 
 * This software is the proprietary information of cadallacricky1-maker.
 * Unauthorized copying, distribution, or use is strictly prohibited.
 */

package com.protector.app.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

/**
 * Receives geofence transition events and triggers alerts
 */
public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "GeofenceReceiver";
    private static final String CHANNEL_ID = "GeofenceAlertChannel";

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        
        if (geofencingEvent == null) {
            return;
        }
        
        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofencing error: " + geofencingEvent.getErrorCode());
            return;
        }

        // Get the transition type
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Check if the transition is of interest
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            // Device has left the geofence
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            
            if (triggeringGeofences != null && !triggeringGeofences.isEmpty()) {
                sendGeofenceAlert(context, "Device has left the protected area");
            }
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            // Device has entered the geofence (optional notification)
            Log.d(TAG, "Device entered geofence");
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
            // Device is dwelling in the geofence
            Log.d(TAG, "Device dwelling in geofence");
        }
    }
    
    private void sendGeofenceAlert(Context context, String message) {
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        createNotificationChannel(context, notificationManager);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Geofence Alert")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true);
        
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
    
    private void createNotificationChannel(Context context, NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Geofence Alerts",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Alerts when device leaves geofenced area");
            notificationManager.createNotificationChannel(channel);
        }
    }
}
