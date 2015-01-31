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
package ca.rmen.android.networkmonitor.app.service.scheduler;

import java.util.List;

import android.content.Context;
import android.telephony.CellInfo;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;

import ca.rmen.android.networkmonitor.util.Log;

/**
 * Execute the runnable when the network changes.
 */
public class NetworkChangeScheduler implements Scheduler {

    private static final String TAG = NetworkChangeScheduler.class.getSimpleName();
    private TelephonyManager mTelephonyManager;
    private Runnable mRunnableImpl;

    @Override
    public void onCreate(Context context) {
        Log.v(TAG, "onCreate");
        // Register the broadcast receiver in a background thread
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CELL_INFO | PhoneStateListener.LISTEN_CELL_LOCATION
                | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE | PhoneStateListener.LISTEN_SERVICE_STATE);
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

    @Override
    public void schedule(Runnable runnable, int interval) {
        Log.v(TAG, "schedule at interval " + interval);
        mRunnableImpl = runnable;
    }

    @Override
    public void setInterval(int interval) {}

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {


        @Override
        public void onCellInfoChanged(List<CellInfo> cellInfo) {
            Log.v(TAG, "onCellInfoChanged: cellInfo " + cellInfo);
            run();
        }

        @Override
        public void onCellLocationChanged(CellLocation location) {
            Log.v(TAG, "onCellLocationChanged: location " + location);
            run();
        }

        @Override
        public void onDataConnectionStateChanged(int state) {
            Log.v(TAG, "onDataConnectionStateChanged: state = %d" + state);
            run();
        }

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            Log.v(TAG, "onServiceStateChanged: serviceState = " + serviceState);
            run();
        }

        private void run() {
            try {
                Log.v(TAG, "Executing task");
                mRunnableImpl.run();
            } catch (Throwable t) {
                Log.v(TAG, "Error executing task: " + t.getMessage(), t);
            }
        }
    };

}
