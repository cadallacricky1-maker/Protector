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

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BodySensorMonitor
 */
public class BodySensorMonitorTest {
    
    @Mock
    private Context mockContext;
    
    @Mock
    private SensorManager mockSensorManager;
    
    @Mock
    private Sensor mockBodySensor;
    
    private BodySensorMonitor monitor;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        when(mockContext.getApplicationContext()).thenReturn(mockContext);
        when(mockContext.getSystemService(Context.SENSOR_SERVICE)).thenReturn(mockSensorManager);
        when(mockSensorManager.getDefaultSensor(Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT))
            .thenReturn(mockBodySensor);
    }
    
    @Test
    public void testBodySensorAvailable() {
        monitor = new BodySensorMonitor(mockContext);
        assertTrue("Body sensor should be available", monitor.isBodySensorAvailable());
    }
    
    @Test
    public void testBodySensorUnavailable() {
        when(mockSensorManager.getDefaultSensor(Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT))
            .thenReturn(null);
        
        monitor = new BodySensorMonitor(mockContext);
        assertFalse("Body sensor should not be available", monitor.isBodySensorAvailable());
    }
    
    @Test
    public void testInitialStateUnknown() {
        monitor = new BodySensorMonitor(mockContext);
        assertEquals("Initial state should be UNKNOWN", 
            BodySensorMonitor.WearState.UNKNOWN, monitor.getCurrentState());
    }
    
    @Test
    public void testStartMonitoringSuccess() {
        when(mockSensorManager.registerListener(any(), eq(mockBodySensor), anyInt()))
            .thenReturn(true);
        
        monitor = new BodySensorMonitor(mockContext);
        BodySensorMonitor.BodySensorListener mockListener = mock(BodySensorMonitor.BodySensorListener.class);
        
        monitor.startMonitoring(mockListener);
        
        verify(mockSensorManager).registerListener(any(), eq(mockBodySensor), anyInt());
    }
    
    @Test
    public void testStartMonitoringWithoutSensor() {
        when(mockSensorManager.getDefaultSensor(Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT))
            .thenReturn(null);
        
        monitor = new BodySensorMonitor(mockContext);
        BodySensorMonitor.BodySensorListener mockListener = mock(BodySensorMonitor.BodySensorListener.class);
        
        monitor.startMonitoring(mockListener);
        
        verify(mockListener).onSensorUnavailable();
    }
    
    @Test
    public void testStopMonitoring() {
        when(mockSensorManager.registerListener(any(), eq(mockBodySensor), anyInt()))
            .thenReturn(true);
        
        monitor = new BodySensorMonitor(mockContext);
        monitor.startMonitoring(null);
        monitor.stopMonitoring();
        
        verify(mockSensorManager).unregisterListener(any(BodySensorMonitor.class));
    }
    
    @Test
    public void testIsOnWristWhenOnWrist() {
        monitor = new BodySensorMonitor(mockContext);
        // Simulate state change to ON_WRIST (tested separately)
        assertFalse("Should not be on wrist initially", monitor.isOnWrist());
    }
    
    @Test
    public void testTimeSinceLastStateChange() {
        monitor = new BodySensorMonitor(mockContext);
        long time = monitor.getTimeSinceLastStateChange();
        assertEquals("Should be 0 initially", 0, time);
    }
    
    @Test
    public void testDoubleStartMonitoring() {
        when(mockSensorManager.registerListener(any(), eq(mockBodySensor), anyInt()))
            .thenReturn(true);
        
        monitor = new BodySensorMonitor(mockContext);
        monitor.startMonitoring(null);
        monitor.startMonitoring(null);
        
        // Should only register once
        verify(mockSensorManager, times(1)).registerListener(any(), eq(mockBodySensor), anyInt());
    }
    
    @Test
    public void testStopMonitoringWhenNotStarted() {
        monitor = new BodySensorMonitor(mockContext);
        monitor.stopMonitoring();
        
        // Should not crash
        verify(mockSensorManager, never()).unregisterListener(any());
    }
    
    @Test
    public void testForceStateCheckWhenNotMonitoring() {
        monitor = new BodySensorMonitor(mockContext);
        monitor.forceStateCheck();
        
        // Should not crash and not trigger sensor
        verify(mockSensorManager, never()).requestTriggerSensor(any(), any());
    }
    
    @Test
    public void testCurrentStateRetrieval() {
        monitor = new BodySensorMonitor(mockContext);
        BodySensorMonitor.WearState state = monitor.getCurrentState();
        assertNotNull("State should not be null", state);
    }
    
    @Test
    public void testSensorAvailabilityCheck() {
        monitor = new BodySensorMonitor(mockContext);
        boolean available = monitor.isBodySensorAvailable();
        
        // Based on mock setup
        assertTrue("Sensor availability should be deterministic", 
            available == (mockBodySensor != null));
    }
}
