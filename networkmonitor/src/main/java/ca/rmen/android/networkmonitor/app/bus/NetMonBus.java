/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2015-2019 Carmen Alvarez (c@rmen.ca)
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
package ca.rmen.android.networkmonitor.app.bus;

import android.os.Handler;
import android.os.Looper;

import org.greenrobot.eventbus.EventBus;

import androidx.annotation.NonNull;


public class NetMonBus {
    private static final EventBus BUS = EventBus.getDefault();

    private NetMonBus() {
        // prevent instantiation
    }

    public static synchronized EventBus getBus() {
        return BUS;
    }

    /**
     * Utility method to post an event to the bus from any thread.
     */
    public static void post(final Object event) {
        new Handler(Looper.getMainLooper()).post(() -> BUS.post(event));
    }

    // region bus events
    public static class DBOperationStarted {
        public final String name;

        public DBOperationStarted(String name) {
            this.name = name;
        }

        @Override
        @NonNull
        public String toString() {
            return "DBOperationStarted{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }

    public static class DBOperationEnded {
        public final boolean isDataChanged;

        public DBOperationEnded(boolean isDataChanged) {
            this.isDataChanged = isDataChanged;
        }

        @Override
        @NonNull
        public String toString() {
            return "DBOperationEnded{" +
                    "isDataChanged=" + isDataChanged +
                    '}';
        }
    }
    // endregion

}
