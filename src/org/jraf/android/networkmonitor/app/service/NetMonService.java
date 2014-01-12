/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014 Benoit 'BoD' Lubek (BoD@JRAF.org)
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
package org.jraf.android.networkmonitor.app.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.app.prefs.NetMonPreferences;
import org.jraf.android.networkmonitor.app.service.datasources.NetMonDataSources;
import org.jraf.android.networkmonitor.provider.NetMonColumns;

/**
 * This service periodically retrieves network state information and writes it to the database.
 */
public class NetMonService extends Service {
    private static final String TAG = Constants.TAG + NetMonService.class.getSimpleName();
    private static final String PREFIX = NetMonService.class.getName() + ".";
    private static final String ACTION_FETCH_DATA = PREFIX + "ACTION_FETCH_DATA";
    private static final int REQUEST_CODE_FETCH_DATA = 1;

    private PowerManager mPowerManager;
    private AlarmManager mAlarmManager;
    private long mLastWakeUp = 0;
    private NetMonDataSources mDataSources;
    private PendingIntent mPendingIntent;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate Service is enabled: starting monitor loop");

        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // Show our ongoing notification
        Notification notification = NetMonNotification.createNotification(this);
        startForeground(NetMonNotification.NOTIFICATION_ID, notification);

        // Register the broadcast receiver in a background thread
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        Handler handler = new Handler(thread.getLooper());
        Intent intent = new Intent(ACTION_FETCH_DATA);
        registerReceiver(mBroadcastReceiver, new IntentFilter(ACTION_FETCH_DATA), null, handler);

        // Prepare our data sources
        mDataSources = new NetMonDataSources();
        mDataSources.onCreate(this);

        // Prepare the pending intent which will be executed periodically.
        mPendingIntent = PendingIntent.getBroadcast(this, REQUEST_CODE_FETCH_DATA, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(mSharedPreferenceListener);
        reScheduleMonitorLoop();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(mSharedPreferenceListener);
        mAlarmManager.cancel(mPendingIntent);
        unregisterReceiver(mBroadcastReceiver);
        mDataSources.onDestroy();
        NetMonNotification.dismissNotification(this);
        super.onDestroy();
    }

    /**
     * Schedule our task to fetch data based on the interval chosen by the user.
     */
    private void reScheduleMonitorLoop() {
        int updateInterval = NetMonPreferences.getInstance(this).getUpdateInterval();
        Log.d(TAG, "reScheduleMonitorLoop: will execute every " + updateInterval / 1000 + " seconds...");
        mAlarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), updateInterval, mPendingIntent);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "onReceive: " + intent);
            // The AlarmManager called us.  Let's fetch data.
            if (ACTION_FETCH_DATA.equals(intent.getAction())) {
                WakeLock wakeLock = null;
                try {
                    // Periodically wake up the device to prevent the data connection from being cut.
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
        }
    };

    private OnSharedPreferenceChangeListener mSharedPreferenceListener = new OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Log.v(TAG, "onSharedPreferenceChanged: " + key);
            // Listen for the user disabling the service
            if (Constants.PREF_SERVICE_ENABLED.equals(key)) {
                if (!sharedPreferences.getBoolean(key, Constants.PREF_SERVICE_ENABLED_DEFAULT)) {
                    Log.v(TAG, "Preference to enable service was turned off");
                    stopSelf();
                }
            }
            // Reschedule our task if the user changed the interval
            else if (Constants.PREF_UPDATE_INTERVAL.equals(key)) {
                reScheduleMonitorLoop();
            }
        }
    };

}
