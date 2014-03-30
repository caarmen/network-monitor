/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014 Carmen Alvarez (c@rmen.ca)
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
package ca.rmen.android.networkmonitor.app.service.datasources;

import android.content.ContentValues;
import android.content.Context;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import ca.rmen.android.networkmonitor.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

/**
 * Retrieves the device's location, using Google Play Services if it is available, or one of the location providers otherwise.
 */
class DeviceLocationDataSource implements NetMonDataSource {
    private static final String TAG = Constants.TAG + DeviceLocationDataSource.class.getSimpleName();
    private NetMonDataSource mDeviceLocationDataSourceImpl;

    public DeviceLocationDataSource() {}

    @Override
    public void onCreate(Context context) {
        Log.v(TAG, "onCreate");
        int playServicesAvailable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        if (playServicesAvailable == ConnectionResult.SUCCESS) mDeviceLocationDataSourceImpl = new GmsDeviceLocationDataSource();
        else
            mDeviceLocationDataSourceImpl = new StandardDeviceLocationDataSource();
        mDeviceLocationDataSourceImpl.onCreate(context);
    }

    /**
     * @return the last location the device recorded in a ContentValues with keys {@link NetMonColumns#DEVICE_LATITUDE} and
     *         {@link NetMonColumns#DEVICE_LONGITUDE}. Tries to use Google Play Services if available. Otherwise falls back
     *         to the most recently retrieved location among all the providers.
     */
    @Override
    public ContentValues getContentValues() {
        Log.v(TAG, "getContentValues");
        return mDeviceLocationDataSourceImpl.getContentValues();
    }


    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        mDeviceLocationDataSourceImpl.onDestroy();
    }

}
