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
package ca.rmen.android.networkmonitor.app.service.datasources;

import android.content.ContentValues;
import android.content.Context;

import ca.rmen.android.networkmonitor.app.service.NetMonService;

/**
 * A {@link NetMonDataSource} implementation is called periodically to retrieve values for one or several related fields, which will be stored in the database.
 * An implementing class must have a no-args constructor: either public, or with package-visibility if it is in this package.
 */
interface NetMonDataSource {
    /**
     * Perform any initialization which will be needed to retrieve data. For example, register any listeners, retrieve any needed system services. This will be
     * called when {@link NetMonService} starts.
     */
    void onCreate(Context context);

    /**
     * Perform any cleanup. For example, unregister any listeners. This will be called when {@link NetMonService} stops.
     */
    void onDestroy();

    /**
     * This will be called periodically, on a background thread, according to the interval the user selected in the preferences.
     * 
     * @return the attributes of the particular data source, at the current time.
     */
    ContentValues getContentValues();
}