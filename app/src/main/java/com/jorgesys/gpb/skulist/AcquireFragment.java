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

package com.jorgesys.gpb.skulist;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.jorgesys.gpb.R;
import com.jorgesys.gpb.billing.BillingProvider;
import com.jorgesys.gpb.skulist.row.SkuRowData;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays a screen with various in-app purchase and subscription options
 */
public class AcquireFragment extends DialogFragment {
    private static final String TAG = "AcquireFragment";

    private RecyclerView mRecyclerView;
    private SkusAdapter mAdapter;
    private View mLoadingView;
    private TextView mErrorTextView;
    private BillingProvider mBillingProvider;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.acquire_fragment, container, false);
        mErrorTextView = (TextView) root.findViewById(R.id.error_textview);
        mRecyclerView = (RecyclerView) root.findViewById(R.id.list);
        mLoadingView = root.findViewById(R.id.screen_wait);
        // Setup a toolbar for this fragment
        Toolbar toolbar = (Toolbar) root.findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_up);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        toolbar.setTitle(R.string.button_purchase);
        setWaitScreen(true);
        onManagerReady((BillingProvider) getActivity());
        return root;
    }

    /**
     * Refreshes this fragment's UI
     */
    public void refreshUI() {
        Log.d(TAG, "Looks like purchases list might have been updated - refreshing the UI");
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Notifies the fragment that billing manager is ready and provides a BillingProvider
     * instance to access it
     */
    public void onManagerReady(BillingProvider billingProvider) {
        mBillingProvider = billingProvider;
        if (mRecyclerView != null) {
            mAdapter = new SkusAdapter(mBillingProvider);
            if (mRecyclerView.getAdapter() == null) {
                mRecyclerView.setAdapter(mAdapter);
                mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            }
            handleManagerAndUiReady();
        }
    }

    /**
     * Enables or disables "please wait" screen.
     */
    private void setWaitScreen(boolean set) {
        mRecyclerView.setVisibility(set ? View.GONE : View.VISIBLE);
        mLoadingView.setVisibility(set ? View.VISIBLE : View.GONE);
    }

    /**
     * Executes query for SKU details at the background thread
     */
    private void handleManagerAndUiReady() {
        // TODO: If Billing Manager was successfully initialized - start querying for SKUs
        final List<SkuRowData> inList = new ArrayList<>();
        SkuDetailsResponseListener responseListener = new SkuDetailsResponseListener() {
            @Override
            public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> skuDetailsList) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                    for (SkuDetails details : skuDetailsList) {
                        Log.w(TAG, "Found sku: " + details);
                        //inList.add(new SkuRowData(details.getSku(), details.getTitle(), details.getPrice(), details.getDescription(), details.getType()));
                        inList.add(new SkuRowData(details.getSku(), details.getTitle(), details.getPrice(), details.getDescription(), details.getType(), details));
                    }

                    if (inList.size() == 0) {
                        displayAnErrorIfNeeded();
                    } else {
                        mAdapter.updateData(inList);
                        setWaitScreen(false);
                    }
                }
            }
        };
        //Start querying for SKUs
        List<String> skus = mBillingProvider.getBillingManager().getSkus(BillingClient.SkuType.INAPP);
        mBillingProvider.getBillingManager().querySkuDetailsAsync(BillingClient.SkuType.INAPP, skus, responseListener);

        //Start querying for subscriptions SKUs
        skus = mBillingProvider.getBillingManager().getSkus(BillingClient.SkuType.SUBS);
        mBillingProvider.getBillingManager().querySkuDetailsAsync(BillingClient.SkuType.SUBS, skus, responseListener);

        // and only otherwise display an error
        //displayAnErrorIfNeeded();
    }

    private void displayAnErrorIfNeeded() {
        if (getActivity() == null || getActivity().isFinishing()) {
            Log.i(TAG, "No need to show an error - activity is finishing already");
            return;
        }

        mLoadingView.setVisibility(View.GONE);
        mErrorTextView.setVisibility(View.VISIBLE);
        mErrorTextView.setText(getText(R.string.error_codelab_not_finished));

        // TODO: Here you will need to handle various respond codes from BillingManager
    }
}

