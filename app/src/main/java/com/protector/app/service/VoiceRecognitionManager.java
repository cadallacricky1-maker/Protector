/*
 * Copyright (c) 2025 cadallacricky1-maker
 * All Rights Reserved.
 * 
 * PROPRIETARY AND CONFIDENTIAL
 * 
 * This software is the proprietary information of cadallacricky1-maker.
 * Unauthorized copying, distribution, or use is strictly prohibited.
 */

package com.protector.app.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;

import java.io.IOException;

/**
 * Manages voice recognition using Vosk speech recognition
 * Handles voice authentication and command processing
 */
public class VoiceRecognitionManager {
    private static final String TAG = "VoiceRecognitionManager";
    private static final float VOICE_MATCH_THRESHOLD = 0.7f; // Similarity threshold for voice matching
    
    // Voice command grammar
    private static final String[] VOICE_COMMANDS = {
        "disable warnings", "enable warnings", "turn off", "turn on", "stop alert"
    };
    
    private Context context;
    private SharedPreferences preferences;
    private SpeechService speechService;
    private Model model;
    private VoiceCallback callback;
    private boolean isListening = false;
    
    // Voice authentication
    private String[] voiceSamples; // Stored voice patterns for authentication
    private int consecutiveUnauthorizedAttempts = 0;
    private static final int MAX_UNAUTHORIZED_ATTEMPTS = 3;

    public interface VoiceCallback {
        void onVoiceDetected(String text);
        void onUnauthorizedVoice();
    }

    public VoiceRecognitionManager(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences("ProtectorPrefs", Context.MODE_PRIVATE);
        loadVoiceModel();
    }
    
    private void loadVoiceModel() {
        // Load Vosk model asynchronously
        StorageService.unpack(context, "model-en-us", "model",
            (model) -> {
                this.model = model;
                initializeRecognizer();
            },
            (exception) -> {
                Log.e(TAG, "Failed to unpack Vosk model", exception);
                // Fallback to simple text matching without full voice auth
            });
    }
    
    private void initializeRecognizer() {
        try {
            Recognizer recognizer = new Recognizer(model, 16000.0f);
            
            // Set up grammar for specific commands
            StringBuilder grammar = new StringBuilder("[");
            for (int i = 0; i < VOICE_COMMANDS.length; i++) {
                grammar.append("\"").append(VOICE_COMMANDS[i]).append("\"");
                if (i < VOICE_COMMANDS.length - 1) {
                    grammar.append(", ");
                }
            }
            grammar.append("]");
            recognizer.setGrammar(grammar.toString());
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize recognizer", e);
        }
    }
    
    public void startListening(VoiceCallback callback) {
        this.callback = callback;
        
        if (model == null) {
            Log.w(TAG, "Model not loaded yet, attempting to initialize");
            loadVoiceModel();
            return;
        }
        
        try {
            speechService = new SpeechService(new Recognizer(model, 16000.0f), 16000.0f);
            speechService.startListening(recognitionListener);
            isListening = true;
            Log.d(TAG, "Voice recognition started");
        } catch (IOException e) {
            Log.e(TAG, "Failed to start speech service", e);
        }
    }
    
    public void stopListening() {
        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
            isListening = false;
            Log.d(TAG, "Voice recognition stopped");
        }
    }
    
    private final RecognitionListener recognitionListener = new RecognitionListener() {
        @Override
        public void onPartialResult(String hypothesis) {
            // Partial results - not used for now
        }
        
        @Override
        public void onResult(String hypothesis) {
            // Full result received
            Log.d(TAG, "Recognition result: " + hypothesis);
            
            // Parse the hypothesis (JSON format from Vosk)
            String text = parseHypothesis(hypothesis);
            
            if (!text.isEmpty()) {
                // Check voice authentication
                if (preferences.getBoolean("voice_auth_enabled", false)) {
                    if (isVoiceAuthorized(hypothesis)) {
                        consecutiveUnauthorizedAttempts = 0;
                        if (callback != null) {
                            callback.onVoiceDetected(text);
                        }
                    } else {
                        consecutiveUnauthorizedAttempts++;
                        if (consecutiveUnauthorizedAttempts >= MAX_UNAUTHORIZED_ATTEMPTS) {
                            if (callback != null) {
                                callback.onUnauthorizedVoice();
                            }
                        }
                    }
                } else {
                    // Voice auth disabled, just process command
                    if (callback != null) {
                        callback.onVoiceDetected(text);
                    }
                }
            }
        }
        
        @Override
        public void onFinalResult(String hypothesis) {
            Log.d(TAG, "Final result: " + hypothesis);
            // Restart listening for continuous recognition
            if (isListening && speechService != null) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (speechService != null) {
                        speechService.stop();
                        try {
                            speechService.startListening(recognitionListener);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to restart listening", e);
                        }
                    }
                }, 100);
            }
        }
        
        @Override
        public void onError(Exception e) {
            Log.e(TAG, "Recognition error", e);
            // Attempt to restart
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (isListening) {
                    stopListening();
                    startListening(callback);
                }
            }, 1000);
        }
        
        @Override
        public void onTimeout() {
            Log.d(TAG, "Recognition timeout");
            // Restart listening
            if (isListening && speechService != null) {
                try {
                    speechService.startListening(recognitionListener);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to restart after timeout", e);
                }
            }
        }
    };
    
    private String parseHypothesis(String hypothesis) {
        // Vosk returns JSON like: {"text": "recognized text"}
        try {
            int startIndex = hypothesis.indexOf("\"text\"");
            if (startIndex != -1) {
                startIndex = hypothesis.indexOf(":", startIndex) + 1;
                int endIndex = hypothesis.indexOf("\"", startIndex + 1);
                if (endIndex != -1) {
                    startIndex = hypothesis.indexOf("\"", startIndex) + 1;
                    return hypothesis.substring(startIndex, endIndex).trim();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse hypothesis", e);
        }
        return "";
    }
    
    private boolean isVoiceAuthorized(String hypothesis) {
        // Simple voice authentication based on stored patterns
        // In a real implementation, this would use voice biometrics
        
        if (!preferences.getBoolean("voice_model_trained", false)) {
            // No trained model, allow by default
            return true;
        }
        
        // Check if the voice pattern matches stored samples
        // This is a simplified version - real implementation would use
        // voice feature extraction and comparison algorithms
        
        String storedVoicePattern = preferences.getString("voice_pattern", "");
        if (storedVoicePattern.isEmpty()) {
            return true; // No pattern stored yet
        }
        
        // Calculate similarity (simplified)
        float similarity = calculateVoiceSimilarity(hypothesis, storedVoicePattern);
        
        return similarity >= VOICE_MATCH_THRESHOLD;
    }
    
    private float calculateVoiceSimilarity(String current, String stored) {
        // Simplified similarity calculation
        // Real implementation would use acoustic features and ML models
        
        // For now, use a simple text-based similarity
        int matches = 0;
        String[] currentWords = current.toLowerCase().split("\\s+");
        String[] storedWords = stored.toLowerCase().split("\\s+");
        
        // Protect against empty arrays
        if (currentWords.length == 0 || storedWords.length == 0) {
            return 0;
        }
        
        for (String word : currentWords) {
            for (String storedWord : storedWords) {
                if (word.equals(storedWord)) {
                    matches++;
                    break;
                }
            }
        }
        
        return (float) matches / currentWords.length;
    }
    
    public void trainVoiceModel(String[] samples) {
        // Store voice samples for future authentication
        // In a real implementation, this would extract acoustic features
        
        if (samples != null && samples.length > 0) {
            // Combine samples into a pattern string
            StringBuilder pattern = new StringBuilder();
            for (String sample : samples) {
                pattern.append(sample).append(" ");
            }
            
            preferences.edit()
                .putString("voice_pattern", pattern.toString().trim())
                .putBoolean("voice_model_trained", true)
                .apply();
            
            Log.d(TAG, "Voice model trained with " + samples.length + " samples");
        }
    }
    
    public boolean isListening() {
        return isListening;
    }
}
