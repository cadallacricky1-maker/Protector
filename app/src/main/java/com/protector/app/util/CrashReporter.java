/*
 * Copyright (c) 2025 cadallacricky1-maker
 * All rights reserved. This software is proprietary and confidential.
 * Unauthorized copying, modification, or distribution is strictly prohibited.
 */

package com.protector.app.util;

import android.content.Context;
import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

/**
 * Centralized crash reporting and analytics utility using Firebase Crashlytics.
 * 
 * Features:
 * - Automatic crash detection and reporting
 * - Custom exception logging
 * - User identification for premium users
 * - Breadcrumb logging for debugging
 * - Key-value logging for context
 * 
 * Usage:
 * - CrashReporter.initialize(context) in Application.onCreate()
 * - CrashReporter.logException(e) for caught exceptions
 * - CrashReporter.log(message) for breadcrumbs
 * - CrashReporter.setUserId(id) for user tracking
 */
public class CrashReporter {
    private static final String TAG = "CrashReporter";
    private static FirebaseCrashlytics crashlytics;
    private static boolean initialized = false;
    
    /**
     * Initialize Firebase Crashlytics.
     * Call this in Application.onCreate() or MainActivity.onCreate().
     */
    public static void initialize(Context context) {
        try {
            crashlytics = FirebaseCrashlytics.getInstance();
            
            // Enable collection in production builds
            crashlytics.setCrashlyticsCollectionEnabled(true);
            
            initialized = true;
            Log.i(TAG, "Firebase Crashlytics initialized successfully");
            
            // Log initialization as breadcrumb
            log("CrashReporter initialized");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Firebase Crashlytics", e);
            initialized = false;
        }
    }
    
    /**
     * Check if Crashlytics is initialized and available.
     */
    public static boolean isInitialized() {
        return initialized && crashlytics != null;
    }
    
    /**
     * Log a caught exception to Crashlytics.
     * This won't crash the app but will record the exception for analysis.
     * 
     * @param throwable The exception to log
     */
    public static void logException(Throwable throwable) {
        if (!isInitialized()) {
            Log.w(TAG, "Crashlytics not initialized, logging locally", throwable);
            return;
        }
        
        try {
            crashlytics.recordException(throwable);
            Log.d(TAG, "Exception logged to Crashlytics: " + throwable.getClass().getSimpleName());
        } catch (Exception e) {
            Log.e(TAG, "Failed to log exception to Crashlytics", e);
        }
    }
    
    /**
     * Log a custom message as a breadcrumb.
     * Breadcrumbs help reconstruct the events leading to a crash.
     * 
     * @param message The breadcrumb message
     */
    public static void log(String message) {
        if (!isInitialized()) {
            Log.d(TAG, "Breadcrumb (local): " + message);
            return;
        }
        
        try {
            crashlytics.log(message);
        } catch (Exception e) {
            Log.e(TAG, "Failed to log breadcrumb", e);
        }
    }
    
    /**
     * Set a custom key-value pair for additional context.
     * These values are included in crash reports.
     * 
     * @param key The key name
     * @param value The value
     */
    public static void setCustomKey(String key, String value) {
        if (!isInitialized()) {
            return;
        }
        
        try {
            crashlytics.setCustomKey(key, value);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set custom key", e);
        }
    }
    
    /**
     * Set a custom key-value pair with boolean value.
     */
    public static void setCustomKey(String key, boolean value) {
        if (!isInitialized()) {
            return;
        }
        
        try {
            crashlytics.setCustomKey(key, value);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set custom key", e);
        }
    }
    
    /**
     * Set a custom key-value pair with integer value.
     */
    public static void setCustomKey(String key, int value) {
        if (!isInitialized()) {
            return;
        }
        
        try {
            crashlytics.setCustomKey(key, value);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set custom key", e);
        }
    }
    
    /**
     * Set user identifier for tracking crashes per user.
     * Use anonymized ID, not PII (Personal Identifiable Information).
     * 
     * @param userId Anonymous user identifier
     */
    public static void setUserId(String userId) {
        if (!isInitialized()) {
            return;
        }
        
        try {
            crashlytics.setUserId(userId);
            Log.d(TAG, "User ID set for crash reporting");
        } catch (Exception e) {
            Log.e(TAG, "Failed to set user ID", e);
        }
    }
    
    /**
     * Force send any unsent crash reports.
     * Useful for testing crash reporting functionality.
     */
    public static void sendUnsentReports() {
        if (!isInitialized()) {
            return;
        }
        
        try {
            crashlytics.sendUnsentReports();
            Log.d(TAG, "Sending unsent crash reports");
        } catch (Exception e) {
            Log.e(TAG, "Failed to send unsent reports", e);
        }
    }
    
    /**
     * Test crash reporting by throwing a test exception.
     * ONLY use this in debug builds for testing.
     */
    public static void testCrash() {
        if (!isInitialized()) {
            Log.w(TAG, "Cannot test crash - Crashlytics not initialized");
            return;
        }
        
        throw new RuntimeException("Test crash for Firebase Crashlytics verification");
    }
    
    /**
     * Log component-specific context for better crash analysis.
     * 
     * @param component Component name (e.g., "ProtectionService", "VoiceRecognition")
     * @param state Current state (e.g., "initializing", "running", "error")
     */
    public static void logComponentState(String component, String state) {
        if (!isInitialized()) {
            return;
        }
        
        try {
            crashlytics.setCustomKey("component_" + component, state);
            crashlytics.log(component + " state: " + state);
        } catch (Exception e) {
            Log.e(TAG, "Failed to log component state", e);
        }
    }
    
    /**
     * Log feature usage for crash context.
     * Helps understand which features were active during a crash.
     * 
     * @param featureName Feature name (e.g., "voice_auth", "geo_fence")
     * @param enabled Whether the feature is enabled
     */
    public static void logFeatureUsage(String featureName, boolean enabled) {
        if (!isInitialized()) {
            return;
        }
        
        try {
            crashlytics.setCustomKey("feature_" + featureName, enabled);
        } catch (Exception e) {
            Log.e(TAG, "Failed to log feature usage", e);
        }
    }
    
    /**
     * Log premium subscription status.
     * Helps prioritize issues affecting paying users.
     * 
     * @param isPremium Whether user has premium subscription
     */
    public static void logPremiumStatus(boolean isPremium) {
        if (!isInitialized()) {
            return;
        }
        
        try {
            crashlytics.setCustomKey("is_premium", isPremium);
            crashlytics.setCustomKey("user_tier", isPremium ? "premium" : "free");
        } catch (Exception e) {
            Log.e(TAG, "Failed to log premium status", e);
        }
    }
}
