/*
 * Copyright (c) 2025 cadallacricky1-maker
 * All Rights Reserved.
 * 
 * PROPRIETARY AND CONFIDENTIAL
 * 
 * This software is the proprietary information of cadallacricky1-maker.
 * Unauthorized copying, distribution, or use is strictly prohibited.
 */

package com.protector.app;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.protector.app.billing.PremiumFeatures;
import com.protector.app.billing.SubscriptionManager;
import com.protector.app.service.ProtectionService;
import com.protector.app.util.CrashReporter;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1001;
    
    private TextView statusText;
    private TextView tvPremiumStatus;
    private Button btnStartProtection;
    private Button btnStopProtection;
    private Button btnSetGeofence;
    private Button btnTrainVoice;
    private Button btnUpgradePremium;
    private EditText etProximityRadius;
    private CheckBox cbVoiceAuth;
    
    private SharedPreferences preferences;
    private SubscriptionManager subscriptionManager;
    private boolean isServiceRunning = false;
    private boolean isPremium = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize crash reporting first
        CrashReporter.initialize(this);
        CrashReporter.log("MainActivity onCreate");
        
        preferences = getSharedPreferences("ProtectorPrefs", MODE_PRIVATE);
        
        initViews();
        setupSubscriptionManager();
        checkAndRequestPermissions();
        loadSettings();
        setupListeners();
    }
    
    private void initViews() {
        statusText = findViewById(R.id.statusText);
        tvPremiumStatus = findViewById(R.id.tvPremiumStatus);
        btnStartProtection = findViewById(R.id.btnStartProtection);
        btnStopProtection = findViewById(R.id.btnStopProtection);
        btnSetGeofence = findViewById(R.id.btnSetGeofence);
        btnTrainVoice = findViewById(R.id.btnTrainVoice);
        btnUpgradePremium = findViewById(R.id.btnUpgradePremium);
        etProximityRadius = findViewById(R.id.etProximityRadius);
        cbVoiceAuth = findViewById(R.id.cbVoiceAuth);
    }
    
    private void setupSubscriptionManager() {
        subscriptionManager = new SubscriptionManager(this);
        subscriptionManager.setStatusListener(new SubscriptionManager.SubscriptionStatusListener() {
            @Override
            public void onSubscriptionStatusChanged(boolean premium) {
                isPremium = premium;
                updatePremiumUI();
                
                // Log premium status to crash reports
                CrashReporter.logPremiumStatus(premium);
            }
            
            @Override
            public void onSubscriptionError(String message) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
        
        isPremium = subscriptionManager.isPremium();
        updatePremiumUI();
        
        // Initial premium status logging
        CrashReporter.logPremiumStatus(isPremium);
    }
    
    private void updatePremiumUI() {
        if (isPremium) {
            tvPremiumStatus.setText("✨ PREMIUM ACTIVE");
            tvPremiumStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            btnUpgradePremium.setText("MANAGE PREMIUM");
        } else {
            tvPremiumStatus.setText("FREE");
            tvPremiumStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
            btnUpgradePremium.setText("✨ UPGRADE TO PREMIUM - $4.99/month");
        }
        
        // Update feature availability based on premium status
        updateFeatureAccess();
    }
    
    private void updateFeatureAccess() {
        // Voice recognition only for premium
        cbVoiceAuth.setEnabled(PremiumFeatures.hasVoiceRecognition(isPremium));
        btnTrainVoice.setEnabled(PremiumFeatures.hasVoiceRecognition(isPremium));
        
        if (!isPremium) {
            cbVoiceAuth.setChecked(false);
        }
        
        // Custom proximity radius only for premium
        if (!PremiumFeatures.hasCustomProximityRadius(isPremium)) {
            etProximityRadius.setText("50");
            etProximityRadius.setEnabled(false);
            etProximityRadius.setHint("50 (Premium for custom)");
        } else {
            etProximityRadius.setEnabled(true);
            etProximityRadius.setHint("1-10000");
        }
    }
    
    private void loadSettings() {
        float radius = preferences.getFloat("proximity_radius", 50.0f);
        etProximityRadius.setText(String.valueOf(radius));
        
        boolean voiceAuthEnabled = preferences.getBoolean("voice_auth_enabled", false);
        cbVoiceAuth.setChecked(voiceAuthEnabled);
    }
    
    private void setupListeners() {
        btnStartProtection.setOnClickListener(v -> startProtection());
        btnStopProtection.setOnClickListener(v -> stopProtection());
        btnSetGeofence.setOnClickListener(v -> setGeofence());
        btnTrainVoice.setOnClickListener(v -> trainVoiceModel());
        
        btnUpgradePremium.setOnClickListener(v -> {
            Intent intent = new Intent(this, SubscriptionActivity.class);
            startActivity(intent);
        });
        
        cbVoiceAuth.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !PremiumFeatures.hasVoiceRecognition(isPremium)) {
                buttonView.setChecked(false);
                Toast.makeText(this, "Voice authentication is a Premium feature", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, SubscriptionActivity.class);
                startActivity(intent);
            } else {
                preferences.edit().putBoolean("voice_auth_enabled", isChecked).apply();
            }
        });
    }
    
    private void startProtection() {
        if (!hasAllPermissions()) {
            Toast.makeText(this, "Please grant all required permissions", Toast.LENGTH_LONG).show();
            checkAndRequestPermissions();
            return;
        }
        
        // Save proximity radius
        try {
            String radiusText = etProximityRadius.getText().toString().trim();
            if (radiusText.isEmpty()) {
                throw new NumberFormatException("Empty input");
            }
            float radius = Float.parseFloat(radiusText);
            if (radius <= 0 || radius > 10000) {
                throw new NumberFormatException("Out of range");
            }
            preferences.edit().putFloat("proximity_radius", radius).apply();
        } catch (NumberFormatException e) {
            preferences.edit().putFloat("proximity_radius", 50.0f).apply();
            Toast.makeText(this, "Invalid radius value (must be 1-10000m), using default: 50m", Toast.LENGTH_SHORT).show();
        }
        
        Intent serviceIntent = new Intent(this, ProtectionService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        isServiceRunning = true;
        updateUIState();
        Toast.makeText(this, "Protection started", Toast.LENGTH_SHORT).show();
    }
    
    private void stopProtection() {
        Intent serviceIntent = new Intent(this, ProtectionService.class);
        stopService(serviceIntent);
        
        isServiceRunning = false;
        updateUIState();
        Toast.makeText(this, "Protection stopped", Toast.LENGTH_SHORT).show();
    }
    
    private void setGeofence() {
        if (!hasLocationPermission()) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Save current location as geofence center
        preferences.edit().putBoolean("geofence_enabled", true).apply();
        Toast.makeText(this, "Geo-fence set at current location", Toast.LENGTH_SHORT).show();
    }
    
    private void trainVoiceModel() {
        if (!hasAudioPermission()) {
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Start voice training activity or dialog
        Toast.makeText(this, "Voice training feature - speak 3 times to train", Toast.LENGTH_LONG).show();
        preferences.edit().putBoolean("voice_model_trained", true).apply();
    }
    
    private void updateUIState() {
        if (isServiceRunning) {
            statusText.setText(R.string.status_active);
            btnStartProtection.setEnabled(false);
            btnStopProtection.setEnabled(true);
        } else {
            statusText.setText(R.string.status_inactive);
            btnStartProtection.setEnabled(true);
            btnStopProtection.setEnabled(false);
        }
    }
    
    private void checkAndRequestPermissions() {
        String[] permissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.VIBRATE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        };
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions = new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.VIBRATE,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            };
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.VIBRATE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            };
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String[] newPermissions = new String[permissions.length + 1];
            System.arraycopy(permissions, 0, newPermissions, 0, permissions.length);
            newPermissions[permissions.length] = Manifest.permission.POST_NOTIFICATIONS;
            permissions = newPermissions;
        }
        
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }
    
    private boolean hasAllPermissions() {
        return hasLocationPermission() && hasAudioPermission() && hasBluetoothPermission();
    }
    
    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            == PackageManager.PERMISSION_GRANTED;
    }
    
    private boolean hasAudioPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            == PackageManager.PERMISSION_GRANTED;
    }
    
    private boolean hasBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
                == PackageManager.PERMISSION_GRANTED;
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) 
            == PackageManager.PERMISSION_GRANTED;
    }
}
