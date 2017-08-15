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

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.app.service.NetMonService;
import android.util.Log;
import java8.util.stream.StreamSupport;

/**
 * Maintains the list of {@link NetMonDataSource}s. For now, the list of available data sources is hardcoded in this class. {@link NetMonService} has a
 * reference to this class, which delegates the data retrieval to the individual data sources.
 */
public class NetMonDataSources {

    private static final String TAG = Constants.TAG + NetMonDataSources.class.getSimpleName();
    private final List<NetMonDataSource> mSources = new ArrayList<>();
    // @formatter:off
    private static final Class<?>[] DATA_SOURCE_CLASSES = new Class<?>[] { 
        ActiveNetworkInfoDataSource.class,
        BatteryDataSource.class,
        CellLocationDataSource.class,
        CellSignalStrengthDataSource.class,
        ConnectionTesterDataSource.class,
        DeviceLocationDataSource.class,
        MobileDataConnectionDataSource.class,
        SIMDataSource.class,
        ServiceStateDataSource.class,
        WiFiDataSource.class,
        NetworkInterfaceDataSource.class,
        ConsumingAppDataSource.class,
        DownloadSpeedTestDataSource.class,
        UploadSpeedTestDataSource.class
    };
    // @formatter:on

    /**
     * Instantiate all the data sources and call {@link NetMonDataSource#onCreate(Context)} on them.
     */
    public void onCreate(Context context) {
        Log.v(TAG, "onCreate");
        for (Class<?> clazz : DATA_SOURCE_CLASSES) {
            NetMonDataSource dataSource;
            //noinspection TryWithIdenticalCatches
            try {
                dataSource = (NetMonDataSource) clazz.newInstance();
                Log.v(TAG, "Added data source " + dataSource);
                dataSource.onCreate(context);
                mSources.add(dataSource);
            } catch (InstantiationException e) {
                Log.e(TAG, "NetMonDataSources Could not create a " + clazz + ": " + e.getMessage(), e);
            } catch (IllegalAccessException e) {
                Log.e(TAG, "NetMonDataSources Could not create a " + clazz + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * @return the fetched data from all data sources.
     */
    public ContentValues getContentValues() {
        Log.v(TAG, "getContentValues");
        ContentValues result = new ContentValues();
        for (NetMonDataSource source : mSources)
            result.putAll(source.getContentValues());
        return result;
    }

    /**
     * Perform cleanup: call {@link NetMonDataSource#onDestroy()} on all data sources.
     */
    public void onDestroy() {
        StreamSupport.stream(mSources).forEach(NetMonDataSource::onDestroy);
    }
}
