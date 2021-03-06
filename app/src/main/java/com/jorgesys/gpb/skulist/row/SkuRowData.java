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
package com.jorgesys.gpb.skulist.row;

import com.android.billingclient.api.SkuDetails;

/**
 * A model for SkusAdapter's row which holds all the data to render UI
 */
public class SkuRowData {
    private final String sku, title, price, description, billingType;
    private final SkuDetails skuDetails;

    public SkuRowData(String sku, String title, String price, String description, String type, SkuDetails skuDetails) {
        this.sku = sku;
        this.title = title;
        this.price = price;
        this.description = description;
        this.billingType = type;
        this.skuDetails = skuDetails;
    }

    public String getSku() {
        return sku;
    }

    public String getTitle() {
        return title;
    }

    public String getPrice() {
        return price;
    }

    public String getDescription() {
        return description;
    }

    public String getBillingType() {
        return billingType;
    }

    public SkuDetails getSkuDetails() {
        return skuDetails;
    }
}
