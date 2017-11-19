/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2013-2015 Carmen Alvarez (c@rmen.ca)
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

import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ca.rmen.android.networkmonitor.Constants;

/**
 * Uses a {@link ScheduledExecutorService} to schedule a single task to be run periodically.
 * 
 * To ensure accurate scheduling, this class obtains a wake lock as soon as the runnable is first scheduled, which it releases in onDestroy().
 * 
 * The wake lock may result in battery drain. For less battery drain, but less accurate scheduling, use {@link AlarmManagerScheduler}.
 */
public class ExecutorServiceScheduler implements Scheduler {
    private static final String TAG = Constants.TAG + ExecutorServiceScheduler.class.getSimpleName();

    private ScheduledExecutorService mExecutorService;
    private Future<?> mFuture;
    private WakeLock mWakeLock = null;
    private Runnable mRunnableImpl;

    @Override
    public void onCreate(Context context) {
        Log.v(TAG, "onCreate");
        mExecutorService = Executors.newSingleThreadScheduledExecutor();
        // Prevent the system from closing the connection after 30 minutes of screen off.
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        }
    }

    @Override
    public void schedule(Runnable runnable, int interval) {
        Log.v(TAG, "schedule at interval " + interval);
        if (mWakeLock != null) {
        mRunnableImpl = runnable;
            mWakeLock.acquire();
            setInterval(interval);
        }
    }

    @Override
    public void setInterval(int interval) {
        Log.v(TAG, "setInterval " + interval);
        if (mFuture != null) mFuture.cancel(true);
        // Issue #20: We should respect the testing interval.  We shouldn't wait for more than this interval for
        // the connection tests to timeout.  
        mFuture = mExecutorService.scheduleAtFixedRate(mRunnable, 0, interval, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        if (mWakeLock != null) mWakeLock.release();
        if (mFuture != null) mFuture.cancel(true);
        if (mExecutorService != null) mExecutorService.shutdownNow();
    }

    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                Log.v(TAG, "Executing task");
                mRunnableImpl.run();
            } catch (Throwable t) {
                Log.v(TAG, "Error executing task: " + t.getMessage(), t);
            }
        }
    };
}
