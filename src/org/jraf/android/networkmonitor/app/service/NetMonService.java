/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2013 Benoit 'BoD' Lubek (BoD@JRAF.org)
 * Copyright (C) 2013 Carmen Alvarez (c@rmen.ca)
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
package org.jraf.android.networkmonitor.app.service;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import android.app.Notification;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.app.prefs.NetMonPreferences;
import org.jraf.android.networkmonitor.app.service.datasources.NetMonDataSources;
import org.jraf.android.networkmonitor.provider.NetMonColumns;

public class NetMonService extends Service {
    private static final String TAG = Constants.TAG + NetMonService.class.getSimpleName();

    private PowerManager mPowerManager;
    private long mLastWakeUp = 0;
    private volatile boolean mDestroyed;
    private ScheduledExecutorService mExecutorService;
    private Future<?> mMonitorLoopFuture;
    private NetMonDataSources mDataSources;
    private WakeLock mWakeLock = null;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate Service is enabled: starting monitor loop");

        Notification notification = NetMonNotification.createNotification(this);
        startForeground(NetMonNotification.NOTIFICATION_ID, notification);

        mExecutorService = Executors.newSingleThreadScheduledExecutor();

        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mDataSources = new NetMonDataSources();
        mDataSources.onCreate(this);
        // Prevent the system from closing the connection after 30 minutes of screen off.
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mWakeLock.acquire();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(mSharedPreferenceListener);
        reScheduleMonitorLoop();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    private void reScheduleMonitorLoop() {
        int updateInterval = NetMonPreferences.getInstance(this).getUpdateInterval();
        Log.d(TAG, "monitorLoop Sleeping " + updateInterval / 1000 + " seconds...");
        if (mMonitorLoopFuture != null) mMonitorLoopFuture.cancel(true);
        mMonitorLoopFuture = mExecutorService.scheduleAtFixedRate(mMonitorLoop, 0, updateInterval, TimeUnit.MILLISECONDS);
    }

    private Runnable mMonitorLoop = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "monitorLoop iteration: destroyed = " + mDestroyed);
            if (mDestroyed) {
                Log.d(TAG, "mDestroyed is true, exiting");
                return;
            }

            // Loop if service is still enabled, otherwise stop
            if (!NetMonPreferences.getInstance(NetMonService.this).isServiceEnabled()) {
                Log.d(TAG, "onCreate Service is disabled: stopping now");
                stopSelf();
                return;
            }

            WakeLock wakeLock = null;
            try {
                long wakeInterval = NetMonPreferences.getInstance(NetMonService.this).getWakeInterval();
                long now = System.currentTimeMillis();
                long timeSinceLastWake = now - mLastWakeUp;
                Log.d(TAG, "wakeInterval = " + wakeInterval + ", lastWakeUp = " + mLastWakeUp + ", timeSinceLastWake = " + timeSinceLastWake);
                if (wakeInterval > 0 && timeSinceLastWake > wakeInterval) {
                    Log.d(TAG, "acquiring lock");
                    wakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
                    wakeLock.acquire();
                    mLastWakeUp = now;
                }

                // Insert this ContentValues into the DB.
                Log.v(TAG, "Inserting data into DB");
                // Put all the data we want to log, into a ContentValues.
                ContentValues values = new ContentValues();
                values.put(NetMonColumns.TIMESTAMP, System.currentTimeMillis());
                values.putAll(mDataSources.getContentValues());
                getContentResolver().insert(NetMonColumns.CONTENT_URI, values);

            } catch (Throwable t) {
                Log.v(TAG, "Error in monitorLoop: " + t.getMessage(), t);
            } finally {
                if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
            }
        }
    };

    private OnSharedPreferenceChangeListener mSharedPreferenceListener = new OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Log.v(TAG, "onSharedPreferenceChanged: " + key);
            if (Constants.PREF_SERVICE_ENABLED.equals(key) || Constants.PREF_UPDATE_INTERVAL.equals(key)) {
                reScheduleMonitorLoop();
            }
        }
    };

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        mDestroyed = true;
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(mSharedPreferenceListener);
        mExecutorService.shutdownNow();
        mDataSources.onDestroy();
        NetMonNotification.dismissNotification(this);
        if (mWakeLock != null && mWakeLock.isHeld()) mWakeLock.release();
        super.onDestroy();
    }
}
