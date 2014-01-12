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
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.R;
import org.jraf.android.networkmonitor.app.log.LogActivity;
import org.jraf.android.networkmonitor.app.main.MainActivity;
import org.jraf.android.networkmonitor.app.service.datasources.NetMonDataSources;
import org.jraf.android.networkmonitor.provider.NetMonColumns;

public class NetMonService extends Service {
    private static final String TAG = Constants.TAG + NetMonService.class.getSimpleName();
    private static final String PREFIX = NetMonService.class.getName() + ".";

    public static final String ACTION_PREF_CHANGED = PREFIX + "ACTION_PREF_CHANGED";
    private static final String ACTION_DISABLE = PREFIX + "ACTION_DISABLE";

    private static final int NOTIFICATION_ID = 1;

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
        if (!isServiceEnabled()) {
            Log.d(TAG, "onCreate Service is disabled: stopping now");
            stopSelf();
            return;
        }
        Log.d(TAG, "onCreate Service is enabled: starting monitor loop");

        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, new IntentFilter(ACTION_PREF_CHANGED));
        Notification notification = createNotification();
        startForeground(NOTIFICATION_ID, notification);

        mExecutorService = Executors.newSingleThreadScheduledExecutor();

        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mDataSources = new NetMonDataSources();
        mDataSources.onCreate(this);
        // Prevent the system from closing the connection after 30 minutes of screen off.
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        mWakeLock.acquire();
        reScheduleMonitorLoop();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    private void reScheduleMonitorLoop() {
        int updateInterval = getUpdateInterval();
        Log.d(TAG, "monitorLoop Sleeping " + updateInterval / 1000 + " seconds...");
        if (mMonitorLoopFuture != null) mMonitorLoopFuture.cancel(true);
        mMonitorLoopFuture = mExecutorService.scheduleAtFixedRate(mMonitorLoop, 0, updateInterval, TimeUnit.MILLISECONDS);
    }

    /*
     * Broadcast.
     */
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            reScheduleMonitorLoop();
        }
    };

    /*
     * Notification.
     */
    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setOngoing(true);
        builder.setSmallIcon(R.drawable.ic_stat_service_running);
        builder.setTicker(getString(R.string.service_notification_ticker));
        builder.setContentTitle(getString(R.string.app_name));
        builder.setContentText(getString(R.string.service_notification_text));
        builder.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));
        builder.addAction(R.drawable.ic_action_stop, getString(R.string.service_notification_action_stop),
                PendingIntent.getBroadcast(this, 0, new Intent(ACTION_DISABLE), PendingIntent.FLAG_CANCEL_CURRENT));
        builder.addAction(R.drawable.ic_action_logs, getString(R.string.service_notification_action_logs),
                PendingIntent.getActivity(this, 0, new Intent(this, LogActivity.class), PendingIntent.FLAG_UPDATE_CURRENT));
        Notification notification = builder.build();
        return notification;
    }

    private void dismissNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private boolean isServiceEnabled() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_SERVICE_ENABLED, Constants.PREF_SERVICE_ENABLED_DEFAULT);
    }

    private Runnable mMonitorLoop = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "monitorLoop iteration: destroyed = " + mDestroyed);
            if (mDestroyed) {
                Log.d(TAG, "mDestroyed is true, exiting");
                return;
            }
            WakeLock wakeLock = null;
            try {
                long wakeInterval = getWakeInterval();
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

            // Loop if service is still enabled, otherwise stop
            if (!isServiceEnabled()) {
                Log.d(TAG, "onCreate Service is disabled: stopping now");
                stopSelf();
                return;
            }
        }
    };

    private int getIntPreference(String key, String defaultValue) {
        String valueStr = PreferenceManager.getDefaultSharedPreferences(this).getString(key, defaultValue);
        int valueInt = Integer.valueOf(valueStr);
        return valueInt;
    }

    private int getUpdateInterval() {
        return getIntPreference(Constants.PREF_UPDATE_INTERVAL, Constants.PREF_UPDATE_INTERVAL_DEFAULT);
    }

    private int getWakeInterval() {
        return getIntPreference(Constants.PREF_WAKE_INTERVAL, Constants.PREF_WAKE_INTERVAL_DEFAULT);
    }

    @Override
    public void onDestroy() {
        mDestroyed = true;
        if (mDataSources != null) mDataSources.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        dismissNotification();
        if (mWakeLock != null && mWakeLock.isHeld()) mWakeLock.release();
        super.onDestroy();
    }
}
