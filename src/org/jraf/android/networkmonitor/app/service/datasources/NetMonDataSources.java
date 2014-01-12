/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014 Carmen Alvarez
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

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import org.jraf.android.networkmonitor.Constants;

public class NetMonDataSources {

    private static final String TAG = Constants.TAG + NetMonDataSources.class.getSimpleName();
    private List<NetMonDataSource> mSources = new ArrayList<NetMonDataSource>();
    // @formatter:off
    private static final Class<?>[] DATA_SOURCE_CLASSES = new Class<?>[] { 
        ActiveNetworkInfoDataSource.class,
        CellLocationDataSource.class,
        CellSignalStrengthDataSource.class,
        ConnectionTesterDataSource.class,
        DeviceLocationDataSource.class,
        MobileDataConnectionDataSource.class,
        SIMDataSource.class,
        WiFiDataSource.class,
    };
    // @formatter:on

    public NetMonDataSources(Context context) {
        Log.v(TAG, "NetMonDataSources");
        for (Class<?> clazz : DATA_SOURCE_CLASSES) {
            NetMonDataSource dataSource;
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

    public ContentValues getContentValues() {
        Log.v(TAG, "getContentValues");
        ContentValues result = new ContentValues();
        for (NetMonDataSource source : mSources)
            result.putAll(source.getContentValues());
        return result;
    }

    public void onDestroy() {
        for (NetMonDataSource source : mSources)
            source.onDestroy();
    }
}
