/*
 * Copyright (c) 2025 cadallacricky1-maker
 * All Rights Reserved.
 * 
 * PROPRIETARY AND CONFIDENTIAL
 * 
 * This software is the proprietary information of cadallacricky1-maker.
 * Unauthorized copying, distribution, or use is strictly prohibited.
 */

package com.protector.app.billing;

/**
 * Defines premium features available with $4.99/month subscription
 * 
 * FREE FEATURES:
 * - Basic theft detection (standard accelerometer monitoring)
 * - Single geo-fence location
 * - Basic proximity warning (50m radius only)
 * - Limited to 10 alerts per day
 * 
 * PREMIUM FEATURES ($4.99/month):
 * - Advanced theft detection with AI pattern recognition
 * - Unlimited geo-fence locations
 * - Customizable proximity radius (1-10000m)
 * - Unlimited alerts
 * - Voice recognition and authentication
 * - Multi-device sync (protect multiple devices)
 * - Cloud backup of settings and voice patterns
 * - Priority customer support
 * - Advanced analytics and theft history
 * - Scheduled protection (auto-enable at specific times/locations)
 * - Family sharing (up to 5 devices)
 * - No ads
 */
public class PremiumFeatures {
    
    /**
     * Check if advanced theft detection is available
     * Premium: AI-powered pattern recognition, higher sensitivity
     * Free: Basic accelerometer only
     */
    public static boolean hasAdvancedTheftDetection(boolean isPremium) {
        return isPremium;
    }
    
    /**
     * Check if voice recognition features are available
     * Premium: Full Vosk voice recognition and authentication
     * Free: Not available
     */
    public static boolean hasVoiceRecognition(boolean isPremium) {
        return isPremium;
    }
    
    /**
     * Check if multiple geo-fence locations are supported
     * Premium: Unlimited locations
     * Free: 1 location only
     */
    public static boolean hasMultipleGeofences(boolean isPremium) {
        return isPremium;
    }
    
    /**
     * Check if custom proximity radius is available
     * Premium: 1-10000m customizable
     * Free: Fixed at 50m
     */
    public static boolean hasCustomProximityRadius(boolean isPremium) {
        return isPremium;
    }
    
    /**
     * Get maximum number of geo-fence locations
     * Premium: Unlimited (999 for practical purposes)
     * Free: 1
     */
    public static int getMaxGeofenceLocations(boolean isPremium) {
        return isPremium ? 999 : 1;
    }
    
    /**
     * Get daily alert limit
     * Premium: Unlimited
     * Free: 10 per day
     */
    public static int getDailyAlertLimit(boolean isPremium) {
        return isPremium ? Integer.MAX_VALUE : 10;
    }
    
    /**
     * Check if cloud backup is available
     * Premium: Full cloud backup and sync
     * Free: Local storage only
     */
    public static boolean hasCloudBackup(boolean isPremium) {
        return isPremium;
    }
    
    /**
     * Check if multi-device sync is available
     * Premium: Sync across all devices
     * Free: Single device only
     */
    public static boolean hasMultiDeviceSync(boolean isPremium) {
        return isPremium;
    }
    
    /**
     * Check if scheduled protection is available
     * Premium: Auto-enable/disable based on time and location
     * Free: Manual control only
     */
    public static boolean hasScheduledProtection(boolean isPremium) {
        return isPremium;
    }
    
    /**
     * Check if advanced analytics are available
     * Premium: Full theft history, analytics, and insights
     * Free: Basic history (last 7 days)
     */
    public static boolean hasAdvancedAnalytics(boolean isPremium) {
        return isPremium;
    }
    
    /**
     * Check if family sharing is available
     * Premium: Share with up to 5 family members
     * Free: Not available
     */
    public static boolean hasFamilySharing(boolean isPremium) {
        return isPremium;
    }
    
    /**
     * Get maximum history days
     * Premium: Unlimited (365 days for practical purposes)
     * Free: 7 days
     */
    public static int getHistoryDays(boolean isPremium) {
        return isPremium ? 365 : 7;
    }
    
    /**
     * Check if ads are removed
     * Premium: No ads
     * Free: Shows ads
     */
    public static boolean isAdFree(boolean isPremium) {
        return isPremium;
    }
    
    /**
     * Get feature description for UI display
     */
    public static String getFeatureDescription(String featureName, boolean isPremium) {
        switch (featureName) {
            case "theft_detection":
                return isPremium ? 
                    "AI-powered advanced theft detection with pattern recognition" :
                    "Basic theft detection (upgrade for AI features)";
            
            case "voice_recognition":
                return isPremium ?
                    "Full voice recognition and authentication" :
                    "🔒 Premium Only - Voice control and authentication";
            
            case "geofencing":
                return isPremium ?
                    "Unlimited geo-fence locations" :
                    "Single geo-fence location (upgrade for more)";
            
            case "proximity":
                return isPremium ?
                    "Customizable proximity radius (1-10000m)" :
                    "Fixed 50m proximity radius (upgrade to customize)";
            
            case "alerts":
                return isPremium ?
                    "Unlimited alerts" :
                    "Limited to 10 alerts per day (upgrade for unlimited)";
            
            case "multi_device":
                return isPremium ?
                    "Sync across all your devices" :
                    "🔒 Premium Only - Multi-device sync";
            
            case "cloud_backup":
                return isPremium ?
                    "Cloud backup and restore" :
                    "🔒 Premium Only - Cloud backup";
            
            case "scheduled":
                return isPremium ?
                    "Auto-enable protection on schedule" :
                    "🔒 Premium Only - Scheduled protection";
            
            case "analytics":
                return isPremium ?
                    "Advanced analytics and theft history" :
                    "Basic history (last 7 days)";
            
            case "family":
                return isPremium ?
                    "Family sharing (up to 5 devices)" :
                    "🔒 Premium Only - Family sharing";
            
            default:
                return "";
        }
    }
}
