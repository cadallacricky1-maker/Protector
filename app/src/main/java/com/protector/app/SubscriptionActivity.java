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

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.protector.app.billing.SubscriptionManager;

public class SubscriptionActivity extends AppCompatActivity {
    
    private SubscriptionManager subscriptionManager;
    private Button btnSubscribe;
    private Button btnRestorePurchases;
    private TextView tvSubscriptionStatus;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subscription);
        
        initViews();
        setupSubscriptionManager();
        setupListeners();
    }
    
    private void initViews() {
        btnSubscribe = findViewById(R.id.btnSubscribe);
        btnRestorePurchases = findViewById(R.id.btnRestorePurchases);
        tvSubscriptionStatus = findViewById(R.id.tvSubscriptionStatus);
    }
    
    private void setupSubscriptionManager() {
        subscriptionManager = new SubscriptionManager(this);
        subscriptionManager.setStatusListener(new SubscriptionManager.SubscriptionStatusListener() {
            @Override
            public void onSubscriptionStatusChanged(boolean isPremium) {
                runOnUiThread(() -> updateUI(isPremium));
            }
            
            @Override
            public void onSubscriptionError(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(SubscriptionActivity.this, message, Toast.LENGTH_LONG).show();
                });
            }
        });
        
        updateUI(subscriptionManager.isPremium());
    }
    
    private void setupListeners() {
        btnSubscribe.setOnClickListener(v -> {
            if (subscriptionManager.isPremium()) {
                Toast.makeText(this, "You're already a Premium subscriber!", Toast.LENGTH_SHORT).show();
            } else {
                subscriptionManager.launchSubscriptionFlow(this);
            }
        });
        
        btnRestorePurchases.setOnClickListener(v -> {
            Toast.makeText(this, "Restoring purchases...", Toast.LENGTH_SHORT).show();
            subscriptionManager.refreshPurchases();
        });
    }
    
    private void updateUI(boolean isPremium) {
        if (isPremium) {
            tvSubscriptionStatus.setText("✓ Premium Active");
            tvSubscriptionStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            btnSubscribe.setText("MANAGE SUBSCRIPTION");
            btnSubscribe.setEnabled(false);
        } else {
            tvSubscriptionStatus.setText("Not subscribed");
            tvSubscriptionStatus.setTextColor(getResources().getColor(android.R.color.darker_gray));
            btnSubscribe.setText("START 7-DAY FREE TRIAL");
            btnSubscribe.setEnabled(true);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (subscriptionManager != null) {
            subscriptionManager.destroy();
        }
    }
}
