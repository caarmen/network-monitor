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
package org.jraf.android.networkmonitor.app.service.scheduler;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import org.jraf.android.networkmonitor.util.Log;

public class AlarmManagerScheduler implements Scheduler {

    private static final String TAG = AlarmManagerScheduler.class.getSimpleName();
    private static final String ACTION = TAG + "_action";
    private PendingIntent mPendingIntent;
    private HandlerThread mHandlerThread;
    private AlarmManager mAlarmManager;
    private Context mContext;
    private int mInterval;
    private Runnable mRunnableImpl;

    @Override
    public void onCreate(Context context) {
        Log.v(TAG, "onCreate");
        mContext = context;
        mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // Register the broadcast receiver in a background thread
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        Handler handler = new Handler(mHandlerThread.getLooper());
        mContext.registerReceiver(mBroadcastReceiver, new IntentFilter(ACTION), null, handler);
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        mContext.unregisterReceiver(mBroadcastReceiver);
        mAlarmManager.cancel(mPendingIntent);
        mHandlerThread.quit();
    }

    @Override
    public void schedule(Runnable runnable, int interval) {
        Log.v(TAG, "schedule at interval " + interval);
        mRunnableImpl = runnable;
        Intent intent = new Intent(ACTION);
        mPendingIntent = PendingIntent.getBroadcast(mContext, TAG.hashCode(), intent, PendingIntent.FLAG_CANCEL_CURRENT);
        setInterval(interval);
    }

    @Override
    public void setInterval(int interval) {
        Log.v(TAG, "Set interval " + interval);
        mInterval = interval;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) scheduleAlarmKitKat(0);
        else
            mAlarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), interval, mPendingIntent);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void scheduleAlarmKitKat(int delay) {
        Log.v(TAG, "scheduleAlarmKitKat: delay=" + delay);
        mAlarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delay, mPendingIntent);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "onReceive: " + intent);
            // The AlarmManager called us.  Let's fetch data.
            if (ACTION.equals(intent.getAction())) {
                // Schedule the next one
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    Log.v(TAG, "rescheduling for kitkat");
                    scheduleAlarmKitKat(mInterval);
                }
            }
            try {
                Log.v(TAG, "Executing task");
                mRunnableImpl.run();
            } catch (Throwable t) {
                Log.v(TAG, "Error executing task: " + t.getMessage(), t);
            }
        }
    };
}
