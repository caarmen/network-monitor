/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014-2015 Carmen Alvarez (c@rmen.ca)
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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import ca.rmen.android.networkmonitor.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;

/**
 * Retrieves the device's location, using Google Play Services if it is available, or one of the location providers otherwise.
 */
public class DeviceLocationDataSource implements NetMonDataSource {
    private static final String TAG = Constants.TAG + DeviceLocationDataSource.class.getSimpleName();
    private NetMonDataSource mDeviceLocationDataSourceImpl;
    private GoogleApiClient mGoogleApiClient;
    private Context mContext;

    public DeviceLocationDataSource() {}

    @Override
    public void onCreate(Context context) {
        Log.v(TAG, "onCreate");
        mContext = context;
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addConnectionCallbacks(mConnectionCallbacks)
                .addOnConnectionFailedListener(mConnectionFailedListener)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
        PreferenceManager.getDefaultSharedPreferences(mContext).registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);

    }

    /**
     * @return the last location the device recorded in a ContentValues with keys {@link NetMonColumns#DEVICE_LATITUDE} and
     *         {@link NetMonColumns#DEVICE_LONGITUDE}. Tries to use Google Play Services if available. Otherwise falls back
     *         to the most recently retrieved location among all the providers.
     */
    @Override
    public ContentValues getContentValues() {
        Log.v(TAG, "getContentValues");
        if (mDeviceLocationDataSourceImpl != null) return mDeviceLocationDataSourceImpl.getContentValues();
        else {
            Log.w(TAG, "No data source available to get location");
            return new ContentValues();
        }
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        mGoogleApiClient.disconnect();
        if (mDeviceLocationDataSourceImpl != null) mDeviceLocationDataSourceImpl.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(mContext).unregisterOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener);
    }

    /**
     * Choose the {@link GmsDeviceLocationDataSource} if Google Play Services is available. Otherwise choose {@link StandardDeviceLocationDataSource}.
     */
    private void selectLocationDataSource() {
        Log.v(TAG, "selectLocationDataSource");
        if (mDeviceLocationDataSourceImpl != null) mDeviceLocationDataSourceImpl.onDestroy();
        mDeviceLocationDataSourceImpl = null;
        NetMonPreferences.LocationFetchingStrategy strategy = NetMonPreferences.getInstance(mContext).getLocationFetchingStrategy();
        if (strategy == NetMonPreferences.LocationFetchingStrategy.HIGH_ACCURACY_GMS
                || strategy == NetMonPreferences.LocationFetchingStrategy.SAVE_POWER_GMS) {
            GoogleApiAvailability api = GoogleApiAvailability.getInstance();
            int playServicesAvailable = api.isGooglePlayServicesAvailable(mContext);

            if (playServicesAvailable == ConnectionResult.SUCCESS) {
                mDeviceLocationDataSourceImpl = new GmsDeviceLocationDataSource(mGoogleApiClient);
            } else {
                NetMonPreferences.getInstance(mContext).forceFossLocationFetchingStrategy();
            }
        }
        if (mDeviceLocationDataSourceImpl == null) {
            mDeviceLocationDataSourceImpl = new StandardDeviceLocationDataSource();
        }
        Log.v(TAG, "selectLocationDataSource: using " + mDeviceLocationDataSourceImpl);
        mDeviceLocationDataSourceImpl.onCreate(mContext);
    }

    private final ConnectionCallbacks mConnectionCallbacks = new ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {
            Log.v(TAG, "onConnected: " + bundle);
            selectLocationDataSource();
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.v(TAG, "onConnectionSuspended: " + i);
            mGoogleApiClient.connect();
        }
    };

    private final OnConnectionFailedListener mConnectionFailedListener = result -> {
        Log.v(TAG, "onConnectionFailed: " + result);
        selectLocationDataSource();
    };

    private final SharedPreferences.OnSharedPreferenceChangeListener mSharedPreferenceChangeListener
            = (sharedPreferences, key) -> {
                if(NetMonPreferences.PREF_LOCATION_FETCHING_STRATEGY.equals(key)) {
                    selectLocationDataSource();
                }
            };
}
