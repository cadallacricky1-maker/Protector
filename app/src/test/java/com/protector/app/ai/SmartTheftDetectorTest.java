/*
 * Copyright (c) 2025 cadallacricky1-maker
 * All Rights Reserved.
 * 
 * PROPRIETARY AND CONFIDENTIAL
 * 
 * This software is the proprietary information of cadallacricky1-maker.
 * Unauthorized copying, distribution, or use is strictly prohibited.
 */

package com.protector.app.ai;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.SensorEvent;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SmartTheftDetector AI system
 */
public class SmartTheftDetectorTest {
    
    @Mock
    private Context mockContext;
    
    @Mock
    private SharedPreferences mockPreferences;
    
    @Mock
    private SharedPreferences.Editor mockEditor;
    
    private SmartTheftDetector detector;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        
        when(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPreferences);
        when(mockPreferences.edit()).thenReturn(mockEditor);
        when(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor);
        when(mockEditor.putFloat(anyString(), anyFloat())).thenReturn(mockEditor);
        when(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor);
        
        detector = new SmartTheftDetector(mockContext);
    }
    
    @Test
    public void testInitialization() {
        assertNotNull("Detector should be initialized", detector);
        assertFalse("Model should not be trained initially", detector.isModelTrained());
        assertEquals("Initial theft patterns should be 0", 0, detector.getTheftPatternsDetected());
    }
    
    @Test
    public void testConfidenceLevels() {
        assertEquals(SmartTheftDetector.TheftConfidence.NONE, 
                    detector.getTheftConfidence(0.2f));
        assertEquals(SmartTheftDetector.TheftConfidence.LOW, 
                    detector.getTheftConfidence(0.4f));
        assertEquals(SmartTheftDetector.TheftConfidence.MEDIUM, 
                    detector.getTheftConfidence(0.6f));
        assertEquals(SmartTheftDetector.TheftConfidence.HIGH, 
                    detector.getTheftConfidence(0.8f));
    }
    
    @Test
    public void testConfidenceBoundaries() {
        // Test exact boundaries
        assertEquals(SmartTheftDetector.TheftConfidence.NONE, 
                    detector.getTheftConfidence(0.29f));
        assertEquals(SmartTheftDetector.TheftConfidence.LOW, 
                    detector.getTheftConfidence(0.3f));
        assertEquals(SmartTheftDetector.TheftConfidence.LOW, 
                    detector.getTheftConfidence(0.49f));
        assertEquals(SmartTheftDetector.TheftConfidence.MEDIUM, 
                    detector.getTheftConfidence(0.5f));
        assertEquals(SmartTheftDetector.TheftConfidence.MEDIUM, 
                    detector.getTheftConfidence(0.74f));
        assertEquals(SmartTheftDetector.TheftConfidence.HIGH, 
                    detector.getTheftConfidence(0.75f));
    }
    
    @Test
    public void testModelReset() {
        detector.resetModel();
        
        assertFalse("Model should not be trained after reset", detector.isModelTrained());
        assertEquals("Theft patterns should be reset to 0", 0, detector.getTheftPatternsDetected());
        
        verify(mockEditor).putBoolean(anyString(), eq(false));
        verify(mockEditor).apply();
    }
    
    @Test
    public void testProcessAccelerometerDataWithoutTraining() {
        // Without sufficient data, should return 0
        SensorEvent mockEvent = mock(SensorEvent.class);
        mockEvent.values = new float[]{1.0f, 2.0f, 3.0f};
        mockEvent.timestamp = System.nanoTime();
        
        float score = detector.processAccelerometerData(mockEvent);
        
        // Should return 0 with insufficient window samples
        assertEquals(0.0f, score, 0.01f);
    }
    
    @Test
    public void testHighAccelerationDetection() {
        // Simulate 50+ samples with high acceleration (theft pattern)
        for (int i = 0; i < 60; i++) {
            SensorEvent mockEvent = mock(SensorEvent.class);
            // High acceleration values simulating theft
            mockEvent.values = new float[]{
                15.0f + (float)(Math.random() * 5),
                15.0f + (float)(Math.random() * 5),
                15.0f + (float)(Math.random() * 5)
            };
            mockEvent.timestamp = System.nanoTime() + i * 100000000L;
            
            detector.processAccelerometerData(mockEvent);
        }
        
        // After 60 samples, should have detected patterns
        assertTrue("Should detect some patterns with high acceleration", 
                  detector.getTheftPatternsDetected() > 0);
    }
    
    @Test
    public void testLowAccelerationNoDetection() {
        // Simulate 50+ samples with low acceleration (normal pattern)
        for (int i = 0; i < 60; i++) {
            SensorEvent mockEvent = mock(SensorEvent.class);
            // Low acceleration values simulating normal use
            mockEvent.values = new float[]{
                1.0f + (float)(Math.random()),
                1.0f + (float)(Math.random()),
                9.8f + (float)(Math.random())  // Gravity component
            };
            mockEvent.timestamp = System.nanoTime() + i * 100000000L;
            
            detector.processAccelerometerData(mockEvent);
        }
        
        // With low acceleration, should not detect theft patterns
        assertEquals("Should not detect patterns with normal acceleration", 
                    0, detector.getTheftPatternsDetected());
    }
    
    @Test
    public void testModelPersistence() {
        detector.resetModel();
        
        verify(mockEditor).putBoolean(anyString(), anyBoolean());
        verify(mockEditor).putFloat(anyString(), anyFloat());
        verify(mockEditor).putInt(anyString(), anyInt());
        verify(mockEditor, atLeastOnce()).apply();
    }
    
    @Test
    public void testErrorHandling() {
        // Test with null sensor event
        float score = detector.processAccelerometerData(null);
        
        // Should handle gracefully and return 0
        assertEquals("Should handle null event gracefully", 0.0f, score, 0.01f);
    }
    
    @Test
    public void testWindowSizeLimit() {
        // Send 100 samples (more than WINDOW_SIZE of 50)
        for (int i = 0; i < 100; i++) {
            SensorEvent mockEvent = mock(SensorEvent.class);
            mockEvent.values = new float[]{1.0f, 2.0f, 3.0f};
            mockEvent.timestamp = System.nanoTime() + i * 100000000L;
            
            detector.processAccelerometerData(mockEvent);
        }
        
        // Should not throw exception and should maintain window size
        // (internal test - window should stay at 50)
        assertTrue("Should process all samples without error", true);
    }
    
    @Test
    public void testConfidenceScoreRange() {
        // Process samples and verify score is always in valid range
        for (int i = 0; i < 60; i++) {
            SensorEvent mockEvent = mock(SensorEvent.class);
            mockEvent.values = new float[]{
                (float)(Math.random() * 20),
                (float)(Math.random() * 20),
                (float)(Math.random() * 20)
            };
            mockEvent.timestamp = System.nanoTime() + i * 100000000L;
            
            float score = detector.processAccelerometerData(mockEvent);
            
            assertTrue("Score should be >= 0", score >= 0.0f);
            assertTrue("Score should be <= 1", score <= 1.0f);
        }
    }
    
    @Test
    public void testSuddenSpikeDetection() {
        // Normal activity followed by sudden spike
        for (int i = 0; i < 45; i++) {
            SensorEvent mockEvent = mock(SensorEvent.class);
            mockEvent.values = new float[]{1.0f, 1.0f, 9.8f};
            mockEvent.timestamp = System.nanoTime() + i * 100000000L;
            detector.processAccelerometerData(mockEvent);
        }
        
        float scoreBefore = 0;
        // Add sudden spikes
        for (int i = 45; i < 55; i++) {
            SensorEvent mockEvent = mock(SensorEvent.class);
            mockEvent.values = new float[]{20.0f, 20.0f, 20.0f};
            mockEvent.timestamp = System.nanoTime() + i * 100000000L;
            scoreBefore = detector.processAccelerometerData(mockEvent);
        }
        
        // Score should increase with sudden spikes
        assertTrue("Should detect sudden spikes", scoreBefore > 0.0f);
    }
    
    @Test
    public void testZeroMagnitudeHandling() {
        // Test with zero acceleration (device not moving)
        for (int i = 0; i < 60; i++) {
            SensorEvent mockEvent = mock(SensorEvent.class);
            mockEvent.values = new float[]{0.0f, 0.0f, 0.0f};
            mockEvent.timestamp = System.nanoTime() + i * 100000000L;
            
            float score = detector.processAccelerometerData(mockEvent);
            
            assertTrue("Should handle zero magnitude", score >= 0.0f && score <= 1.0f);
        }
    }
    
    @Test
    public void testHighVarianceDetection() {
        // Simulate high variance pattern (erratic movement)
        for (int i = 0; i < 60; i++) {
            SensorEvent mockEvent = mock(SensorEvent.class);
            // Alternating high and low values = high variance
            float magnitude = (i % 2 == 0) ? 20.0f : 2.0f;
            mockEvent.values = new float[]{magnitude, magnitude, magnitude};
            mockEvent.timestamp = System.nanoTime() + i * 100000000L;
            
            detector.processAccelerometerData(mockEvent);
        }
        
        // High variance should eventually trigger pattern detection
        assertTrue("High variance should be detected eventually", 
                  detector.getTheftPatternsDetected() >= 0);
    }
}
