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

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for PremiumFeatures class
 * Tests feature gating logic for free vs premium users
 */
public class PremiumFeaturesTest {
    
    @Test
    public void testVoiceRecognition_Premium_ReturnsTrue() {
        assertTrue("Premium users should have voice recognition", 
            PremiumFeatures.hasVoiceRecognition(true));
    }
    
    @Test
    public void testVoiceRecognition_Free_ReturnsFalse() {
        assertFalse("Free users should not have voice recognition", 
            PremiumFeatures.hasVoiceRecognition(false));
    }
    
    @Test
    public void testCustomProximityRadius_Premium_ReturnsTrue() {
        assertTrue("Premium users should have custom proximity", 
            PremiumFeatures.hasCustomProximityRadius(true));
    }
    
    @Test
    public void testCustomProximityRadius_Free_ReturnsFalse() {
        assertFalse("Free users should not have custom proximity", 
            PremiumFeatures.hasCustomProximityRadius(false));
    }
    
    @Test
    public void testMaxGeofenceLocations_Premium_ReturnsUnlimited() {
        assertEquals("Premium users should have unlimited geofences", 
            999, PremiumFeatures.getMaxGeofenceLocations(true));
    }
    
    @Test
    public void testMaxGeofenceLocations_Free_ReturnsOne() {
        assertEquals("Free users should have 1 geofence only", 
            1, PremiumFeatures.getMaxGeofenceLocations(false));
    }
    
    @Test
    public void testDailyAlertLimit_Premium_ReturnsUnlimited() {
        assertEquals("Premium users should have unlimited alerts", 
            Integer.MAX_VALUE, PremiumFeatures.getDailyAlertLimit(true));
    }
    
    @Test
    public void testDailyAlertLimit_Free_ReturnsTen() {
        assertEquals("Free users should have 10 alerts per day", 
            10, PremiumFeatures.getDailyAlertLimit(false));
    }
    
    @Test
    public void testCloudBackup_Premium_ReturnsTrue() {
        assertTrue("Premium users should have cloud backup", 
            PremiumFeatures.hasCloudBackup(true));
    }
    
    @Test
    public void testCloudBackup_Free_ReturnsFalse() {
        assertFalse("Free users should not have cloud backup", 
            PremiumFeatures.hasCloudBackup(false));
    }
    
    @Test
    public void testMultiDeviceSync_Premium_ReturnsTrue() {
        assertTrue("Premium users should have multi-device sync", 
            PremiumFeatures.hasMultiDeviceSync(true));
    }
    
    @Test
    public void testMultiDeviceSync_Free_ReturnsFalse() {
        assertFalse("Free users should not have multi-device sync", 
            PremiumFeatures.hasMultiDeviceSync(false));
    }
    
    @Test
    public void testHistoryDays_Premium_Returns365() {
        assertEquals("Premium users should have 365 days history", 
            365, PremiumFeatures.getHistoryDays(true));
    }
    
    @Test
    public void testHistoryDays_Free_ReturnsSeven() {
        assertEquals("Free users should have 7 days history", 
            7, PremiumFeatures.getHistoryDays(false));
    }
    
    @Test
    public void testIsAdFree_Premium_ReturnsTrue() {
        assertTrue("Premium users should have no ads", 
            PremiumFeatures.isAdFree(true));
    }
    
    @Test
    public void testIsAdFree_Free_ReturnsFalse() {
        assertFalse("Free users should see ads", 
            PremiumFeatures.isAdFree(false));
    }
    
    @Test
    public void testFeatureDescription_TheftDetection_Premium() {
        String description = PremiumFeatures.getFeatureDescription("theft_detection", true);
        assertTrue("Premium theft detection description should mention AI", 
            description.contains("AI"));
    }
    
    @Test
    public void testFeatureDescription_TheftDetection_Free() {
        String description = PremiumFeatures.getFeatureDescription("theft_detection", false);
        assertTrue("Free theft detection description should mention upgrade", 
            description.contains("upgrade"));
    }
    
    @Test
    public void testFeatureDescription_VoiceRecognition_Free() {
        String description = PremiumFeatures.getFeatureDescription("voice_recognition", false);
        assertTrue("Free voice recognition should show as locked", 
            description.contains("🔒") || description.contains("Premium Only"));
    }
}
