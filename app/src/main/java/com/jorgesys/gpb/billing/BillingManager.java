/*
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jorgesys.gpb.billing;

import android.app.Activity;
import android.util.Log;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * TODO: Implement BillingManager that will handle all the interactions with Play Store
 * (via Billing library), maintain connection to it through BillingClient and cache
 * temporary states/data if needed.
 */
public class BillingManager implements PurchasesUpdatedListener {
    private static final String TAG = "BillingManager";
    private final BillingClient mBillingClient;
    private final Activity mActivity;
    //structure to retrieve the lists of all the SKU IDs for a particular SKU type from Google Play Developer Console
    private static final HashMap<String, List<String>> SKUS;
    private AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener;
    private boolean connected;

    static {
        SKUS = new HashMap<>();
        //SKUs defined in Google Play Console
        SKUS.put(BillingClient.SkuType.INAPP, Arrays.asList("gas", "premium"));
        SKUS.put(BillingClient.SkuType.SUBS, Arrays.asList("subcription_gold"));
    }

    public BillingManager(/*Context context*/ Activity activity) {
        mActivity = activity;
        mBillingClient = BillingClient.newBuilder(mActivity).enablePendingPurchases().setListener(this).build();
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                Log.i(TAG, "onBillingSetupFinished()");
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.i(TAG, "onBillingSetupFinished() response: " + billingResult.getResponseCode());
                    //The BillingClient is ready. You can query purchases here.
                    connected = true;
                    queryPurchases();
                } else {
                    connected = false;
                    Log.w(TAG, "onBillingSetupFinished() error: " + billingResult.getResponseCode());
                    //Try to restart the connection on the next request to
                    //Google Play by calling the startConnection() method
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.w(TAG, "onBillingServiceDisconnected");
            }
        });

        acknowledgePurchaseResponseListener = new AcknowledgePurchaseResponseListener() {
            @Override
            public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                int responseCode = billingResult.getResponseCode();
                String debugMessage = billingResult.getDebugMessage();
                Log.d(TAG, "onAcknowledgePurchaseResponse() responseCode: " + responseCode + " debugMessage: " + debugMessage);
            }
        };
    }

    private void queryPurchases(){
        Log.i(TAG, "queryPurchases()");
        if (!mBillingClient.isReady()){
            Log.e(TAG, "queryPurchases: mBillingClient is not Ready.");
        }
        Log.d(TAG, "queryPurchases: SUBS");
        Purchase.PurchasesResult result = mBillingClient.queryPurchases(BillingClient.SkuType.SUBS);
        if (result == null){
            Log.i(TAG, "queryPurchases: null purchase result.");
            processPurchases(null);
        } else {
            if (result.getPurchasesList() == null){
                Log.i(TAG, "queryPurchases: null purchase list.");
                processPurchases(null);
            } else {
                processPurchases(result.getPurchasesList());
            }
        }
    }

    private void processPurchases(List<Purchase> purchaseList){
        Log.i(TAG, "processPurchases().");
        if (purchaseList != null) {
            Log.d(TAG, "processPurchases: " + purchaseList.size() + " purchases(s)");
        } else {
            Log.d(TAG, "processPurchases is empty.");
        }

        if (isUnchangedPurchaseList(purchaseList)) {
            Log.d(TAG, "processPurchases: Purchase list has not changed.");
            return;
        }

        if (purchaseList != null) {
            logAcknowledgmentStatus(purchaseList);
        }
    }

    /**
     * Log the number of purchases that are acknowledge and not acknowledged.
     * <p>
     * https://developer.android.com/google/play/billing/billing_library_releases_notes#2_0_acknowledge
     * <p>
     * When the purchase is first received, it will not be acknowledge.
     * This application sends the purchase token to the server for registration. After the
     * purchase token is registered to an account, the Android app acknowledges the purchase token.
     * The next time the purchase list is updated, it will contain acknowledged purchases.
     */
    private void logAcknowledgmentStatus(List<Purchase> purchasesList){
        Log.i(TAG, "logAcknowledgmentStatus()");
        int ack_yes = 0;
        int ack_no = 0;
        for (Purchase purchase : purchasesList){
            if (purchase.isAcknowledged()){
                ack_yes++;
            } else {
                ack_no++;
            }
        }
        Log.d(TAG, "logAcknowledgementStatus: acknowledged=" + ack_yes +
                " unacknowledged=" + ack_no);
    }

    /**
     * Acknowledge a purchase.
     * <p>
     * https://developer.android.com/google/play/billing/billing_library_releases_notes#2_0_acknowledge
     * <p>
     * Apps should acknowledge the purchase after confirming that the purchase token
     * has been associated with a user. This app only acknowledges purchases after
     * successfully receiving the subscription data back from the server.
     * <p>
     * Developers can choose to acknowledge purchases from a server using the
     * Google Play Developer API. The server has direct access to the user database,
     * so using the Google Play Developer API for acknowledgement might be more reliable.
     * <p>
     * If the purchase token is not acknowledged within 3 days,
     * then Google Play will automatically refund and revoke the purchase.
     * This behavior helps ensure that users are not charged for subscriptions unless the
     * user has successfully received access to the content.
     * This eliminates a category of issues where users complain to developers
     * that they paid for something that the app is not giving to them.
     */
    private void acknowledgePurchase(String purchaseToken) {
        Log.d(TAG, "acknowledgePurchase()");
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchaseToken)
                .build();
        /*mBillingClient.acknowledgePurchase(params, new AcknowledgePurchaseResponseListener() {
            @Override
            public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                int responseCode = billingResult.getResponseCode();
                String debugMessage = billingResult.getDebugMessage();
                Log.d(TAG, "acknowledgePurchase: " + responseCode + " " + debugMessage);
            }
        });*/
        mBillingClient.acknowledgePurchase(params, acknowledgePurchaseResponseListener);
    }

    /**
     * Check whether the purchases have changed before posting changes.
     */
    private boolean isUnchangedPurchaseList(List<Purchase> purchasesList) {
        Log.i(TAG, "isUnchangedPurchaseList()");
        // TODO: Optimize to avoid updates with identical data.
        return false;
    }

    public void startPurchaseFlow(/*String skuId, String billingType*/final SkuDetails skuDetails) {
        Log.i(TAG, "startPurchaseFlow()");
        //Specify a runnable to start when connection to Billing client is established
        Runnable executeOnConnectedService = new Runnable() {
            @Override
            public void run() {
                BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder().setSkuDetails(skuDetails).build();
                mBillingClient.launchBillingFlow(mActivity, billingFlowParams);
            }
        };

        // If Billing client was disconnected, we retry 1 time
        // and if success, execute the query
        startServiceConnectionIfNeeded(executeOnConnectedService);
    }

    //PurchasesUpdatedListener This method will receive callbacks for all the updates on the future purchases
    @Override
    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
        Log.i(TAG, "onPurchasesUpdated()");
        if (purchases != null && purchases.size() > 0){
            acknowledgePurchase(purchases.get(0).getPurchaseToken());
        }
        Log.d(TAG, "onPurchasesUpdated() response: " + billingResult.getResponseCode());
    }

    public List<String> getSkus(@BillingClient.SkuType String type) {
        Log.i(TAG, "getSkus()");
        return SKUS.get(type);
    }

    public void querySkuDetailsAsync(@BillingClient.SkuType final String itemType, final List<String> skuList, final SkuDetailsResponseListener listener) {
        //Specify a runnable to start when the connection to Billing client is established
        Log.i(TAG, "querySkuDetailsAsync()");
        Runnable executeOnConnectedService = new Runnable() {
            @Override
            public void run() {
                SkuDetailsParams skuDetailsParams = SkuDetailsParams.newBuilder().setSkusList(skuList).setType(itemType).build();
                mBillingClient.querySkuDetailsAsync(skuDetailsParams, new SkuDetailsResponseListener() {
                    @Override
                    public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetailsList) {
                        listener.onSkuDetailsResponse(billingResult, skuDetailsList);
                    }
                });
            }
        };

        //If Billing client was disconnected, we retry 1 time
        //and if success, execute the query
        startServiceConnectionIfNeeded(executeOnConnectedService);

    }

    private void startServiceConnectionIfNeeded(final Runnable executeOnSuccess) {
        Log.i(TAG, "startServiceConnectionIfNeeded()");
        if (mBillingClient.isReady()) {
            if (executeOnSuccess != null) {
                executeOnSuccess.run();
            }
        } else {
            mBillingClient.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(BillingResult billingResult) {
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        Log.i(TAG, "onBillingSetupFinished() response: " + billingResult.getResponseCode());
                        if (executeOnSuccess != null) {
                            executeOnSuccess.run();
                        }
                    } else {
                        Log.w(TAG, "onBillingSetupFinished() error code: " + billingResult.getResponseCode());
                    }
                }

                @Override
                public void onBillingServiceDisconnected() {
                    Log.w(TAG, "onBillingServiceDisconnected()");
                }
            });
        }
    }

    public void destroy() {
        Log.i(TAG, "destroy()");
        mBillingClient.endConnection();
    }

    /**
     * Método que indica si el billingClient esta conectado
     *
     * @return variable boleana la cual indica si esta o no conectado el billingClient
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Método para consultar las compras
     *
     * @return lista de compras
     */
    public List<Purchase> getPurchases() {
        List<Purchase> purchases = new ArrayList<>();
        if (mBillingClient.isReady()) {
            purchases = mBillingClient.queryPurchases(BillingClient.SkuType.SUBS).getPurchasesList();
        }
        return purchases;
    }

    /**
     * Metodo para reconocer las compras desde Main
     * @param purchase la compra
     * @param listener el listener
     */
    public void acknowledgePurchaseFromMain(Purchase purchase, AcknowledgePurchaseResponseListener listener) {
        //acknowledge purchase
        if (!purchase.isAcknowledged()) {
            Log.i(TAG, "Acknowledging purchase");
            AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams
                    .newBuilder()
                    .setPurchaseToken(purchase.getPurchaseToken())
                    .build();
            if (listener != null) {
                mBillingClient.acknowledgePurchase(acknowledgePurchaseParams, listener);
            } else {
                Log.i(TAG, "acknowledgePurchaseResponseListener is null");
            }
        } else {
            Log.w(TAG, "Purchase already acknowledged!.");
        }
    }
}
