/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2015 Carmen Alvarez (c@rmen.ca)
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
package ca.rmen.android.networkmonitor.app.service.scheduler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences;

/**
 * Execute the runnable when the network changes.
 */
public class NetworkChangeScheduler implements Scheduler {

    private static final String TAG = Constants.TAG + NetworkChangeScheduler.class.getSimpleName();
    private Context mContext;
    private Runnable mRunnableImpl;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private long mLastPollTime;
    private TelephonyManager mTelephonyManager;

    @Override
    public void onCreate(Context context) {
        Log.v(TAG, "onCreate");
        mContext = context;
        // Register the broadcast receiver in a background thread
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mContext.registerReceiver(mBroadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION), null, mHandler);
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (mTelephonyManager != null) {
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
        }
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        mContext.unregisterReceiver(mBroadcastReceiver);
        if (mTelephonyManager != null)
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        mHandlerThread.quit();
    }

    @Override
    public void schedule(Runnable runnable, int interval) {
        Log.v(TAG, "schedule at interval " + interval);
        mRunnableImpl = runnable;
    }

    @Override
    public void setInterval(int interval) {
        // We ignore the interval.
    }

    /**
     * Prevent running our task too many times successively.
     * If we've already run the task recently, schedule it
     * to run in a few seconds. Otherwise just run it right now.
     */
    private final Runnable mBufferedRunnable = new Runnable() {

        @Override
        public void run() {
            Log.v(TAG, "mBufferedRunnable.run()");
            // If we have bad luck, we might be created,
            // and receive a broadcast before schedule was called
            if (mRunnableImpl == null) return;

            long now = System.currentTimeMillis();
            if (now - mLastPollTime > NetMonPreferences.PREF_MIN_POLLING_INTERVAL) {
                Log.v(TAG, "Will run the task now");
                try {
                    mRunnableImpl.run();
                    mLastPollTime = System.currentTimeMillis();
                } catch (Throwable t) {
                    Log.v(TAG, "Error executing task: " + t.getMessage(), t);
                }
            } else {
                Log.v(TAG, "Ran the task too recently: will schedule it for later");
                mHandler.removeCallbacksAndMessages(null);
                mHandler.postDelayed(mBufferedRunnable, NetMonPreferences.PREF_MIN_POLLING_INTERVAL);
            }
        }

    };

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {


        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "onReceive: intent = " + intent);
            mHandler.post(mBufferedRunnable);
        }
    };

    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            Log.v(TAG, "onServiceStateChanged " + serviceState);
            mHandler.post(mBufferedRunnable);
        }
    };

}
