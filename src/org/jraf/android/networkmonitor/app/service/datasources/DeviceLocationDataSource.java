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
package org.jraf.android.networkmonitor.app.service.datasources;

import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import org.jraf.android.networkmonitor.util.Log;

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.provider.NetMonColumns;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;

/**
 * Retrieves the device's location, using either Google Play Services or one of the location providers.
 */
class DeviceLocationDataSource implements NetMonDataSource {
    private static final String TAG = Constants.TAG + DeviceLocationDataSource.class.getSimpleName();
    private LocationManager mLocationManager;
    private LocationClient mLocationClient;

    public DeviceLocationDataSource() {}

    @Override
    public void onCreate(Context context) {
        Log.v(TAG, "onCreate");
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mLocationClient = new LocationClient(context, mConnectionCallbacks, mConnectionFailedListener);
        mLocationClient.connect();
    }


    /**
     * @return the last location the device recorded in a ContentValues with keys {@link NetMonColumns#DEVICE_LATITUDE} and
     *         {@link NetMonColumns#DEVICE_LONGITUDE}. Tries to use Google Play Services if available. Otherwise falls back
     *         to the most recently retrieved location among all the providers.
     */
    @Override
    public ContentValues getContentValues() {
        Log.v(TAG, "getContentValues");
        ContentValues values = new ContentValues(2);
        Location mostRecentLocation = null;
        // Try getting the location from the LocationClient
        if (mLocationClient.isConnected()) {
            mostRecentLocation = mLocationClient.getLastLocation();
            Log.v(TAG, "Got location from LocationClient: " + mostRecentLocation);
        }
        // Fall back to the old way.
        if (mostRecentLocation == null) {
            List<String> providers = mLocationManager.getProviders(true);
            long mostRecentFix = 0;
            for (String provider : providers) {
                Location location = mLocationManager.getLastKnownLocation(provider);
                Log.v(TAG, "Location for provider " + provider + ": " + location);
                if (location == null) continue;
                long time = location.getTime();
                if (time > mostRecentFix) {
                    time = mostRecentFix;
                    mostRecentLocation = location;
                }
            }
        }
        Log.v(TAG, "Most recent location: " + mostRecentLocation);
        if (mostRecentLocation != null) {
            values.put(NetMonColumns.DEVICE_LATITUDE, mostRecentLocation.getLatitude());
            values.put(NetMonColumns.DEVICE_LONGITUDE, mostRecentLocation.getLongitude());
            values.put(NetMonColumns.DEVICE_POSITION_ACCURACY, mostRecentLocation.getAccuracy());
        }
        return values;
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        if (mLocationClient != null) mLocationClient.disconnect();
    }

    private ConnectionCallbacks mConnectionCallbacks = new ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {
            Log.v(TAG, "onConnected: " + bundle);
        }

        @Override
        public void onDisconnected() {
            Log.v(TAG, "onDisconnected");
        }
    };

    private OnConnectionFailedListener mConnectionFailedListener = new OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(ConnectionResult result) {
            Log.v(TAG, "onConnectionFailed: " + result);
        }
    };
}
