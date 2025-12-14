/*
 * Copyright (c) 2025 cadallacricky1-maker
 * All Rights Reserved.
 * 
 * PROPRIETARY AND CONFIDENTIAL
 * 
 * This software is the proprietary information of cadallacricky1-maker.
 * Unauthorized copying, distribution, or use is strictly prohibited.
 */

package com.protector.app.util;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/**
 * Centralized error handling utility
 * Provides consistent error logging and user-friendly messages
 */
public class ErrorHandler {
    private static final String TAG = "Protector";
    
    /**
     * Handle an error with logging and user notification
     */
    public static void handleError(Context context, String component, String operation, Exception e) {
        handleError(context, component, operation, e, true);
    }
    
    /**
     * Handle an error with optional user notification
     */
    public static void handleError(Context context, String component, String operation, Exception e, boolean showToast) {
        // Log the error
        String errorMessage = String.format("[%s] Error in %s: %s", component, operation, e.getMessage());
        Log.e(TAG, errorMessage, e);
        
        if (showToast && context != null) {
            // Show user-friendly message
            String userMessage = getUserFriendlyMessage(component, operation, e);
            Toast.makeText(context, userMessage, Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Convert technical errors to user-friendly messages
     */
    private static String getUserFriendlyMessage(String component, String operation, Exception e) {
        String exceptionType = e.getClass().getSimpleName();
        
        // Security/Permission errors
        if (exceptionType.contains("SecurityException") || exceptionType.contains("PermissionException")) {
            if (operation.contains("location")) {
                return "Location permission required. Please enable location access in Settings.";
            } else if (operation.contains("sensor")) {
                return "Sensor access required for theft detection.";
            } else if (operation.contains("audio") || operation.contains("voice")) {
                return "Microphone permission required for voice features.";
            }
            return "Permission required. Please check app settings.";
        }
        
        // Network errors
        if (exceptionType.contains("IOException") || exceptionType.contains("NetworkException")) {
            return "Network error. Please check your connection and try again.";
        }
        
        // Billing errors
        if (component.equals("SubscriptionManager")) {
            return "Subscription service unavailable. Please try again later.";
        }
        
        // Voice recognition errors
        if (component.equals("VoiceRecognitionManager")) {
            return "Voice recognition temporarily unavailable.";
        }
        
        // Wear OS errors
        if (component.equals("WearCommunicator")) {
            return "Unable to connect to smartwatch. Please check Bluetooth.";
        }
        
        // Generic fallback
        return "An error occurred. Please try again or contact support.";
    }
    
    /**
     * Log warning without user notification
     */
    public static void logWarning(String component, String message) {
        Log.w(TAG, String.format("[%s] Warning: %s", component, message));
    }
    
    /**
     * Log info message
     */
    public static void logInfo(String component, String message) {
        Log.i(TAG, String.format("[%s] %s", component, message));
    }
    
    /**
     * Check if error is recoverable
     */
    public static boolean isRecoverable(Exception e) {
        String exceptionType = e.getClass().getSimpleName();
        
        // Network errors are typically recoverable
        if (exceptionType.contains("IOException") || exceptionType.contains("NetworkException")) {
            return true;
        }
        
        // Timeout errors are recoverable
        if (exceptionType.contains("TimeoutException")) {
            return true;
        }
        
        // Permission errors might be recoverable if user grants permission
        if (exceptionType.contains("SecurityException")) {
            return true;
        }
        
        // Most other errors are not automatically recoverable
        return false;
    }
}
