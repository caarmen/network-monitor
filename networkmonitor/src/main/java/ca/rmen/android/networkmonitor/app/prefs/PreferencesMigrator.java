/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2016-2020 Carmen Alvarez (c@rmen.ca)
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
package ca.rmen.android.networkmonitor.app.prefs;

import android.content.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ca.rmen.android.networkmonitor.provider.NetMonColumns;

/**
 * Migrates/fixes any settings from previous installations.
 */
public class PreferencesMigrator {

    private final NetMonPreferences mPrefs;
    private final List<String> validColumns;

    public PreferencesMigrator(Context context) {
        mPrefs = NetMonPreferences.getInstance(context);
        validColumns = Arrays.asList(NetMonColumns.getColumnNames(context));
    }

    public void migratePreferences() {
        migrateTestServer();
        removeInvalidSelectedColumnNames();
    }

    private void migrateTestServer() {
        // The old google ip address no longer works. If the app was using
        // the old IP, reset it to use the new one.
        if ("173.194.45.41".equals(mPrefs.getTestServer())) {
            mPrefs.resetTestServer();
        }
    }

    private void removeInvalidSelectedColumnNames() {
        List<String> selectedColumns = mPrefs.getSelectedColumns();
        List<String> invalidColumns = new ArrayList<>();
        for (String selectedColumn : selectedColumns) {
            if (!validColumns.contains(selectedColumn)) invalidColumns.add(selectedColumn);
        }
        selectedColumns.removeAll(invalidColumns);
        mPrefs.setSelectedColumns(selectedColumns);
    }

}
