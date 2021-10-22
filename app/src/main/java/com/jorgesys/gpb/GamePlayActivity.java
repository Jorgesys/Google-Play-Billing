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

package com.jorgesys.gpb;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;
import androidx.fragment.app.FragmentActivity;

import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.jorgesys.gpb.billing.BillingManager;
import com.jorgesys.gpb.billing.BillingProvider;
import com.jorgesys.gpb.skulist.AcquireFragment;

import java.util.List;

/**
 * Example game using Play Billing library.
 * <p>
 * Please follow steps inside the codelab to understand the best practices for this new library.
 */
public class GamePlayActivity extends FragmentActivity implements BillingProvider {
    // Debug tag, for logging
    private static final String TAG = "GamePlayActivity";

    // Tag for a dialog that allows us to find it when screen was rotated
    private static final String DIALOG_TAG = "dialog";

    private BillingManager mBillingManager;
    private AcquireFragment mAcquireFragment;
    private MainViewController mViewController;

    private View mScreenWait, mScreenMain;
    //private ImageView mCarImageView;
    private ImageView mGasImageView;

    private AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener;

    /***
     * Metodo que manda a reconocer la compra
     * @param purchase la compra
     * @param listener el listener
     */
    public void acknowledgePurchase(Purchase purchase, AcknowledgePurchaseResponseListener listener) {
        //acknowledge purchase
        if (!purchase.isAcknowledged()) {
            Log.i(TAG, "Acknowledging purchase");
            if (listener != null) {
                mBillingManager.acknowledgePurchaseFromMain(purchase, listener);
            } else {
                Log.i(TAG, "acknowledgePurchaseResponseListener is null");
            }
        } else {
            Log.w(TAG, "Purchase already acknowledged!.");
        }
    }

    /***
     * Metodo para inicializar los listeners
     */
    private void setUpListeners() {
        acknowledgePurchaseResponseListener = new AcknowledgePurchaseResponseListener() {
            @Override
            public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.i(TAG, "Purchase acknowledged ");
                } else {
                    Log.e(TAG, "onAcknowledgePurchaseResponse - responseCode: " + billingResult.getResponseCode());
                }
            }
        };
    }

    /***
     * Metodo que manda a reconocer la compra en dado caso que no se haya reconocido, esto tambien
     * funciona para las compras que se hacen fuera de la aplicación
     */
    private void acknowledgePurchaseIfNeeded() {
        Log.i(TAG, "acknowledgePurchaseIfNeeded");
        if (mBillingManager.isConnected()) {
            List<Purchase> purchaseList = mBillingManager.getPurchases();
            if (purchaseList != null && purchaseList.size() > 0) {
                Purchase purchase = purchaseList.get(0);
                if (!purchase.isAcknowledged()) {
                    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                        //acknowledge purchase
                        if (!purchase.isAcknowledged()) {
                            if (acknowledgePurchaseResponseListener != null) {
                                acknowledgePurchase(purchase, acknowledgePurchaseResponseListener);
                            } else {
                                Log.i(TAG, "acknowledgePurchaseResponseListener is null");
                            }
                        } else {
                            Log.i(TAG, "Purchase already acknowledged");
                        }
                    } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
                        Log.i(TAG, "Pending purchase");
                    } else if (purchase.getPurchaseState() == Purchase.PurchaseState.UNSPECIFIED_STATE) {
                        Log.i(TAG, "Unspecified state");
                    }
                } else {
                    Log.w(TAG, "Purchase already acknowledged!.");
                }
            } else {
                Log.w(TAG, "Purchase list is null");
            }
        } else {
            Log.e(TAG, "billingClient not connected!.");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_game_play);
        setUpListeners();
        // Start the controller and load game data
        mViewController = new MainViewController(this);

        // Try to restore dialog fragment if we were showing it prior to screen rotation
        if (savedInstanceState != null) {
            mAcquireFragment = (AcquireFragment) getSupportFragmentManager()
                    .findFragmentByTag(DIALOG_TAG);
        }

        // Create and initialize BillingManager which talks to BillingLibrary
        mBillingManager = new BillingManager(this);

        mScreenWait = findViewById(R.id.screen_wait);
        mScreenMain = findViewById(R.id.screen_main);
        mGasImageView = ((ImageView) findViewById(R.id.gas_gauge));

        // Specify purchase and drive buttons listeners
        // Note: This couldn't be done inside *.xml for Android TV since TV layout is inflated
        // via AppCompat
        findViewById(R.id.button_purchase).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPurchaseButtonClicked(view);
            }
        });
        findViewById(R.id.button_drive).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onDriveButtonClicked(view);
            }
        });

        showRefreshedUi();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //TODO se reconoce compra desde la actividad princial 14/10/2020
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                acknowledgePurchaseIfNeeded();
            }
        }, 2500);
    }

    @Override
    public BillingManager getBillingManager() {
        return mBillingManager;
    }

    /**
     * User clicked the "Buy Gas" button - show a purchase dialog with all available SKUs
     */
    public void onPurchaseButtonClicked(final View arg0) {
        Log.d(TAG, "Purchase button clicked.");

        if (mAcquireFragment == null) {
            mAcquireFragment = new AcquireFragment();
        }

        if (!isAcquireFragmentShown()) {
            mAcquireFragment.show(getSupportFragmentManager(), DIALOG_TAG);
        }
    }

    /**
     * Drive button clicked. Burn gas!
     */
    public void onDriveButtonClicked(View arg0) {
        Log.d(TAG, "Drive button clicked.");

        if (mViewController.isTankEmpty()) {
            alert(R.string.alert_no_gas);
        } else {
            mViewController.useGas();
            alert(R.string.alert_drove);
            updateUi();
        }
    }

    /**
     * Remove loading spinner and refresh the UI
     */
    public void showRefreshedUi() {
        setWaitScreen(false);
        updateUi();

        if (isAcquireFragmentShown()) {
            mAcquireFragment.refreshUI();
        }
    }

    /**
     * Show an alert dialog to the user
     *
     * @param messageId String id to display inside the alert dialog
     */
    @UiThread
    void alert(@StringRes int messageId) {
        alert(messageId, null);
    }

    /**
     * Show an alert dialog to the user
     *
     * @param messageId     String id to display inside the alert dialog
     * @param optionalParam Optional attribute for the string
     */
    @UiThread
    void alert(@StringRes int messageId, @Nullable Object optionalParam) {
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            throw new RuntimeException("Dialog could be shown only from the main thread");
        }

        AlertDialog.Builder bld = new AlertDialog.Builder(this);
        bld.setNeutralButton("OK", null);

        if (optionalParam == null) {
            bld.setMessage(messageId);
        } else {
            bld.setMessage(getResources().getString(messageId, optionalParam));
        }

        bld.create().show();
    }

    /**
     * Enables or disables the "please wait" screen.
     */
    private void setWaitScreen(boolean set) {
        mScreenMain.setVisibility(set ? View.GONE : View.VISIBLE);
        mScreenWait.setVisibility(set ? View.VISIBLE : View.GONE);
    }

    /**
     * Update UI to reflect model
     */
    @UiThread
    private void updateUi() {
        Log.d(TAG, "Updating the UI. Thread: " + Thread.currentThread().getName());

        // Update gas gauge to reflect tank status
        mGasImageView.setImageResource(mViewController.getTankResId());
    }

    public boolean isAcquireFragmentShown() {
        return mAcquireFragment != null && mAcquireFragment.isVisible();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBillingManager.destroy();
    }
}
