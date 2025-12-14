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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.widget.TextView;

/**
 * Wear OS Main Activity
 * Displays protection status and alerts from the phone
 * 
 * ARCHITECTURE:
 * - Phone (Brain): Sends all alerts and status updates
 * - Watch (Hand): Displays alerts, minimal processing
 * - Battery optimized: No sensors, just display
 */
public class MainActivity extends Activity {
    
    private TextView tvStatus;
    private TextView tvLastAlert;
    private TextView tvAlertTime;
    private SharedPreferences preferences;
    private Vibrator vibrator;
    
    private final BroadcastReceiver alertReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String alertType = intent.getStringExtra("alert_type");
            String message = intent.getStringExtra("message");
            handleAlert(alertType, message);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        preferences = getSharedPreferences("ProtectorWear", MODE_PRIVATE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        
        initViews();
        loadLastAlert();
        registerAlertReceiver();
    }
    
    private void initViews() {
        tvStatus = findViewById(R.id.tvStatus);
        tvLastAlert = findViewById(R.id.tvLastAlert);
        tvAlertTime = findViewById(R.id.tvAlertTime);
        
        // Show current protection status
        boolean isActive = preferences.getBoolean("protection_active", false);
        updateStatus(isActive);
    }
    
    private void loadLastAlert() {
        String lastAlert = preferences.getString("last_alert_type", "No alerts yet");
        String lastTime = preferences.getString("last_alert_time", "");
        
        tvLastAlert.setText(getAlertDisplayText(lastAlert));
        tvAlertTime.setText(lastTime);
    }
    
    private void registerAlertReceiver() {
        IntentFilter filter = new IntentFilter("com.protector.wear.ALERT");
        registerReceiver(alertReceiver, filter);
    }
    
    private void handleAlert(String alertType, String message) {
        // Save alert info
        preferences.edit()
            .putString("last_alert_type", alertType)
            .putString("last_alert_time", getCurrentTime())
            .apply();
        
        // Update UI
        tvLastAlert.setText(getAlertDisplayText(alertType));
        tvAlertTime.setText(getCurrentTime());
        
        // Vibrate for alert
        vibrateForAlert(alertType);
    }
    
    private void updateStatus(boolean isActive) {
        if (isActive) {
            tvStatus.setText("🛡️ PROTECTED");
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvStatus.setText("⚠️ INACTIVE");
            tvStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
    }
    
    private String getAlertDisplayText(String alertType) {
        if (alertType == null) return "No alerts";
        
        switch (alertType) {
            case "THEFT_DETECTED":
                return "🚨 THEFT ALERT!\nDevice being moved!";
            case "PROXIMITY_BREACH":
                return "📍 DON'T FORGET\nYour device!";
            case "GEOFENCE_EXIT":
                return "🗺️ GEOFENCE ALERT\nDevice left safe zone";
            case "UNAUTHORIZED_VOICE":
                return "🎤 UNAUTHORIZED\nVoice detected!";
            case "STATUS_UPDATE":
                return "ℹ️ Status Updated";
            default:
                return "⚡ Alert: " + alertType;
        }
    }
    
    private void vibrateForAlert(String alertType) {
        if (vibrator == null || !vibrator.hasVibrator()) return;
        
        long[] pattern;
        
        switch (alertType) {
            case "THEFT_DETECTED":
                // Strong vibration for theft
                pattern = new long[]{0, 500, 100, 500, 100, 500, 100, 500};
                break;
            case "UNAUTHORIZED_VOICE":
                // Urgent vibration
                pattern = new long[]{0, 300, 100, 300, 100, 300};
                break;
            case "PROXIMITY_BREACH":
            case "GEOFENCE_EXIT":
                // Moderate vibration
                pattern = new long[]{0, 400, 200, 400};
                break;
            default:
                // Gentle vibration
                pattern = new long[]{0, 300};
                break;
        }
        
        vibrator.vibrate(pattern, -1);
    }
    
    private String getCurrentTime() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(alertReceiver);
        } catch (IllegalArgumentException e) {
            // Already unregistered
        }
    }
}
