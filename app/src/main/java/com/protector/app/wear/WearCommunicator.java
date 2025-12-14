/*
 * Copyright (c) 2025 cadallacricky1-maker
 * All Rights Reserved.
 * 
 * PROPRIETARY AND CONFIDENTIAL
 * 
 * This software is the proprietary information of cadallacricky1-maker.
 * Unauthorized copying, distribution, or use is strictly prohibited.
 */

package com.protector.app.wear;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Handles communication with Wear OS watch
 * Sends alerts and status updates using Wearable Data API
 * 
 * BATTERY OPTIMIZATION:
 * - Uses efficient Wearable Data Layer
 * - Asynchronous messaging
 * - Minimal data transfer
 */
public class WearCommunicator {
    
    private static final String TAG = "WearCommunicator";
    private static final String MESSAGE_PATH = "/protector/alert";
    
    private final Context context;
    
    public WearCommunicator(Context context) {
        this.context = context.getApplicationContext();
    }
    
    /**
     * Send alert to connected Wear OS device
     * @param alertType Type of alert (THEFT_DETECTED, PROXIMITY_BREACH, etc.)
     * @param message Alert message
     */
    public void sendAlert(String alertType, String message) {
        new Thread(() -> {
            try {
                // Get connected nodes (watches)
                Task<List<Node>> nodeListTask = Wearable.getNodeClient(context).getConnectedNodes();
                List<Node> nodes = Tasks.await(nodeListTask);
                
                if (nodes.isEmpty()) {
                    Log.d(TAG, "No connected Wear OS devices");
                    return;
                }
                
                // Prepare message data
                String alertData = alertType + "|" + message;
                byte[] messageBytes = alertData.getBytes();
                
                // Send to all connected watches
                for (Node node : nodes) {
                    Wearable.getMessageClient(context)
                        .sendMessage(node.getId(), MESSAGE_PATH, messageBytes)
                        .addOnSuccessListener(integer -> 
                            Log.d(TAG, "Alert sent to watch: " + alertType))
                        .addOnFailureListener(e -> 
                            Log.e(TAG, "Failed to send alert to watch", e));
                }
                
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error sending message to watch", e);
            }
        }).start();
    }
    
    /**
     * Send status update to watch
     * @param isActive Whether protection is currently active
     */
    public void sendStatusUpdate(boolean isActive) {
        String message = isActive ? "Protection enabled" : "Protection disabled";
        sendAlert("STATUS_UPDATE", message);
    }
    
    /**
     * Check if any Wear OS devices are connected
     * @return true if at least one watch is connected
     */
    public boolean isWatchConnected() {
        try {
            Task<List<Node>> nodeListTask = Wearable.getNodeClient(context).getConnectedNodes();
            List<Node> nodes = Tasks.await(nodeListTask);
            return !nodes.isEmpty();
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Error checking watch connection", e);
            return false;
        }
    }
}
