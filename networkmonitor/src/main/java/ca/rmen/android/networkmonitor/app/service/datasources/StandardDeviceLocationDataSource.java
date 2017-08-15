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
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;

import java.util.List;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences.LocationFetchingStrategy;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import android.util.Log;
import ca.rmen.android.networkmonitor.util.PermissionUtil;

/**
 * Retrieves the device's location, using the most recent Location retrieved among all of the available the location providers.
 */
public class StandardDeviceLocationDataSource implements NetMonDataSource {
    private static final String TAG = Constants.TAG + StandardDeviceLocationDataSource.class.getSimpleName();
    private LocationManager mLocationManager;
    private volatile Location mMostRecentLocation;
    private Context mContext;

    StandardDeviceLocationDataSource() {}

    @Override
    public void onCreate(Context context) {
        Log.v(TAG, "onCreate");
        mContext = context;
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(mPreferenceListener);
        registerLocationListener();
    }

    /**
     * @return the last location the device recorded in a ContentValues with keys {@link NetMonColumns#DEVICE_LATITUDE} and
     *         Uses the most recently retrieved location among all the providers.
     */
    @Override
    public ContentValues getContentValues() {
        Log.v(TAG, "getContentValues");
        ContentValues values = new ContentValues(3);
        if (!PermissionUtil.hasLocationPermission(mContext)) {
            Log.v(TAG, "No location permission");
            return values;
        }
        Location mostRecentLocation = null;
        List<String> providers = mLocationManager.getProviders(true);
        for (String provider : providers) {
            @SuppressWarnings("MissingPermission")
            Location location = mLocationManager.getLastKnownLocation(provider);
            Log.v(TAG, "Location for provider " + provider + ": " + location);
            if (isBetter(mostRecentLocation, location)) mostRecentLocation = location;
        }
        Log.v(TAG, "Most recent location: " + mostRecentLocation);
        if (mostRecentLocation != null) {
            values.put(NetMonColumns.DEVICE_LATITUDE, mostRecentLocation.getLatitude());
            values.put(NetMonColumns.DEVICE_LONGITUDE, mostRecentLocation.getLongitude());
            values.put(NetMonColumns.DEVICE_POSITION_ACCURACY, mostRecentLocation.getAccuracy());
            values.put(NetMonColumns.DEVICE_SPEED, mostRecentLocation.getSpeed());
        }
        mMostRecentLocation = mostRecentLocation;
        return values;
    }

    @Override
    @SuppressWarnings("MissingPermission")
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        PreferenceManager.getDefaultSharedPreferences(mContext).unregisterOnSharedPreferenceChangeListener(mPreferenceListener);

        if (PermissionUtil.hasLocationPermission(mContext)) {
            mLocationManager.removeUpdates(mLocationListener);
        }
    }

    @SuppressWarnings("MissingPermission")
    private void registerLocationListener() {
        LocationFetchingStrategy locationFetchingStrategy = NetMonPreferences.getInstance(mContext).getLocationFetchingStrategy();
        Log.v(TAG, "registerLocationListener: strategy = " + locationFetchingStrategy);
        if (!PermissionUtil.hasLocationPermission(mContext)) {
            Log.d(TAG, "No location permissions");
            return;
        }
        mLocationManager.removeUpdates(mLocationListener);
        if (locationFetchingStrategy == LocationFetchingStrategy.HIGH_ACCURACY) {
            int pollingFrequency = NetMonPreferences.getInstance(mContext).getUpdateInterval();
            if (pollingFrequency < NetMonPreferences.PREF_MIN_POLLING_INTERVAL) pollingFrequency = NetMonPreferences.PREF_MIN_POLLING_INTERVAL;
            List<String> providers = mLocationManager.getProviders(true);
            for (String provider : providers) {
                mLocationManager.requestLocationUpdates(provider, pollingFrequency, 0f, mLocationListener);
                Log.v(TAG, "registered location listener for provider " + provider);
            }
        }
    }

    /**
     * @return true if location1 is better than location2.
     *         For now, we just use the most recent location.
     */
    private boolean isBetter(Location location1, Location location2) {
        if (location1 == null && location2 == null) return false;
        if (location1 == null) return true;
        //noinspection SimplifiableIfStatement
        if (location2 == null) return false;
        return location2.getTime() > location1.getTime();
    }

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            extras.isEmpty();
            Log.v(TAG, "onStatusChanged: provider = " + provider + ", status = " + status + ", extras = " + extras);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.v(TAG, "onProviderEnabled: " + provider);
            registerLocationListener();
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.v(TAG, "onProviderDisabled: " + provider);
            registerLocationListener();

        }

        @Override
        public void onLocationChanged(Location location) {
            Log.v(TAG, "onLocationChanged, location = " + location);
            if (isBetter(mMostRecentLocation, location)) mMostRecentLocation = location;
        }
    };

    private final OnSharedPreferenceChangeListener mPreferenceListener = (sharedPreferences, key) -> {
        Log.v(TAG, "onSharedPreferenceChanged: key = " + key);
        if (NetMonPreferences.PREF_LOCATION_FETCHING_STRATEGY.equals(key) || NetMonPreferences.PREF_UPDATE_INTERVAL.equals(key)) {
            registerLocationListener();
        }
    };
}
