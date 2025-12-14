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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages subscription billing for Protector Premium
 * Handles $4.99/month subscription with premium features
 */
public class SubscriptionManager implements PurchasesUpdatedListener {
    private static final String TAG = "SubscriptionManager";
    private static final String PREMIUM_SUBSCRIPTION_ID = "protector_premium_monthly";
    private static final String PREFS_NAME = "ProtectorSubscription";
    private static final String KEY_IS_PREMIUM = "is_premium";
    
    private final Context context;
    private final SharedPreferences preferences;
    private BillingClient billingClient;
    private ProductDetails premiumProductDetails;
    private SubscriptionStatusListener statusListener;
    
    public interface SubscriptionStatusListener {
        void onSubscriptionStatusChanged(boolean isPremium);
        void onSubscriptionError(String message);
    }
    
    public SubscriptionManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        initializeBillingClient();
    }
    
    private void initializeBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build();
        
        connectToBillingService();
    }
    
    private void connectToBillingService() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing client connected");
                    queryProductDetails();
                    queryExistingPurchases();
                } else {
                    Log.e(TAG, "Billing setup failed: " + billingResult.getDebugMessage());
                }
            }
            
            @Override
            public void onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected, will retry");
                // Retry connection
                connectToBillingService();
            }
        });
    }
    
    private void queryProductDetails() {
        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        productList.add(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_SUBSCRIPTION_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        );
        
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build();
        
        billingClient.queryProductDetailsAsync(params, (billingResult, productDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && 
                !productDetailsList.isEmpty()) {
                premiumProductDetails = productDetailsList.get(0);
                Log.d(TAG, "Product details loaded: " + premiumProductDetails.getName());
            } else {
                Log.e(TAG, "Failed to load product details: " + billingResult.getDebugMessage());
            }
        });
    }
    
    private void queryExistingPurchases() {
        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build();
        
        billingClient.queryPurchasesAsync(params, (billingResult, purchases) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                handlePurchases(purchases);
            }
        });
    }
    
    public void launchSubscriptionFlow(Activity activity) {
        if (premiumProductDetails == null) {
            if (statusListener != null) {
                statusListener.onSubscriptionError("Subscription not available yet, please try again");
            }
            return;
        }
        
        List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = new ArrayList<>();
        
        // Get the subscription offer (monthly at $4.99)
        List<ProductDetails.SubscriptionOfferDetails> offers = 
            premiumProductDetails.getSubscriptionOfferDetails();
        
        if (offers != null && !offers.isEmpty()) {
            String offerToken = offers.get(0).getOfferToken();
            
            productDetailsParamsList.add(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(premiumProductDetails)
                    .setOfferToken(offerToken)
                    .build()
            );
            
            BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();
            
            BillingResult billingResult = billingClient.launchBillingFlow(activity, billingFlowParams);
            
            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                Log.e(TAG, "Failed to launch billing flow: " + billingResult.getDebugMessage());
                if (statusListener != null) {
                    statusListener.onSubscriptionError("Failed to start subscription process");
                }
            }
        }
    }
    
    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases);
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "User canceled purchase");
        } else {
            Log.e(TAG, "Purchase error: " + billingResult.getDebugMessage());
            if (statusListener != null) {
                statusListener.onSubscriptionError("Purchase failed: " + billingResult.getDebugMessage());
            }
        }
    }
    
    private void handlePurchases(List<Purchase> purchases) {
        boolean hasPremium = false;
        
        for (Purchase purchase : purchases) {
            if (purchase.getProducts().contains(PREMIUM_SUBSCRIPTION_ID) && 
                purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                
                hasPremium = true;
                
                // Acknowledge purchase if not already acknowledged
                if (!purchase.isAcknowledged()) {
                    acknowledgePurchase(purchase);
                }
            }
        }
        
        updatePremiumStatus(hasPremium);
    }
    
    private void acknowledgePurchase(Purchase purchase) {
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.getPurchaseToken())
            .build();
        
        billingClient.acknowledgePurchase(params, billingResult -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged");
            }
        });
    }
    
    private void updatePremiumStatus(boolean isPremium) {
        boolean currentStatus = preferences.getBoolean(KEY_IS_PREMIUM, false);
        
        if (currentStatus != isPremium) {
            preferences.edit().putBoolean(KEY_IS_PREMIUM, isPremium).apply();
            
            if (statusListener != null) {
                statusListener.onSubscriptionStatusChanged(isPremium);
            }
            
            Log.d(TAG, "Premium status updated: " + isPremium);
        }
    }
    
    public boolean isPremium() {
        return preferences.getBoolean(KEY_IS_PREMIUM, false);
    }
    
    public void setStatusListener(SubscriptionStatusListener listener) {
        this.statusListener = listener;
    }
    
    public void refreshPurchases() {
        if (billingClient.isReady()) {
            queryExistingPurchases();
        }
    }
    
    public void destroy() {
        if (billingClient != null) {
            billingClient.endConnection();
        }
    }
}
