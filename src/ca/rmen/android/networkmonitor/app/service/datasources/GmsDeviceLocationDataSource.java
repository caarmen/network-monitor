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
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.preference.PreferenceManager;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences.LocationFetchingStrategy;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import ca.rmen.android.networkmonitor.util.Log;

import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

/**
 * Retrieves the device's location, using Google Play Services.
 */
class GmsDeviceLocationDataSource implements NetMonDataSource {
    private static final String TAG = Constants.TAG + GmsDeviceLocationDataSource.class.getSimpleName();
    private LocationClient mLocationClient;
    private Location mMostRecentLocation;
    private Context mContext;

    public GmsDeviceLocationDataSource(LocationClient locationClient) {
        mLocationClient = locationClient;
    }

    @Override
    public void onCreate(Context context) {
        Log.v(TAG, "onCreate");
        mContext = context;
        PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(mPreferenceListener);
    }


    /**
     * @return the last location the device recorded in a ContentValues with keys {@link NetMonColumns#DEVICE_LATITUDE} and
     *         {@link NetMonColumns#DEVICE_LONGITUDE}. Uses Google Play Services
     */
    @Override
    public ContentValues getContentValues() {
        Log.v(TAG, "getContentValues");
        ContentValues values = new ContentValues(2);
        // Try getting the location from the LocationClient
        mMostRecentLocation = mLocationClient.getLastLocation();
        Log.v(TAG, "Got location from LocationClient: " + mMostRecentLocation);
        Log.v(TAG, "Most recent location: " + mMostRecentLocation);
        if (mMostRecentLocation != null) {
            values.put(NetMonColumns.DEVICE_LATITUDE, mMostRecentLocation.getLatitude());
            values.put(NetMonColumns.DEVICE_LONGITUDE, mMostRecentLocation.getLongitude());
            values.put(NetMonColumns.DEVICE_POSITION_ACCURACY, mMostRecentLocation.getAccuracy());
        }
        return values;
    }


    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        PreferenceManager.getDefaultSharedPreferences(mContext).unregisterOnSharedPreferenceChangeListener(mPreferenceListener);
        if (mLocationClient != null && mLocationClient.isConnected()) {
            mLocationClient.removeLocationUpdates(mGmsLocationListener);
            mLocationClient.disconnect();
        }
    }

    private void registerLocationListener() {
        LocationFetchingStrategy locationFetchingStrategy = NetMonPreferences.getInstance(mContext).getLocationFetchingStrategy();
        Log.v(TAG, "registerLocationListener: strategy = " + locationFetchingStrategy);
        mLocationClient.removeLocationUpdates(mGmsLocationListener);
        int pollingInterval = NetMonPreferences.getInstance(mContext).getUpdateInterval();
        if (locationFetchingStrategy == LocationFetchingStrategy.HIGH_ACCURACY) {
            LocationRequest request = new LocationRequest();
            request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            request.setInterval(pollingInterval);
            mLocationClient.requestLocationUpdates(request, mGmsLocationListener);
            Log.v(TAG, "registered location listener");
        }
    }


    LocationListener mGmsLocationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            Log.v(TAG, "onLocationChanged, location = " + location);
            mMostRecentLocation = location;
        }
    };

    OnSharedPreferenceChangeListener mPreferenceListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Log.v(TAG, "onSharedPreferenceChanged: key = " + key);
            if (NetMonPreferences.PREF_LOCATION_FETCHING_STRATEGY.equals(key)) {
                registerLocationListener();
            }
        }
    };
}
