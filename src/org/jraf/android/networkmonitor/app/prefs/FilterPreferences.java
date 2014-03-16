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
package org.jraf.android.networkmonitor.app.prefs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;

import org.jraf.android.networkmonitor.provider.NetMonColumns;
import org.jraf.android.networkmonitor.util.Log;


/**
 * TODO add javadoc and logging
 */
public class FilterPreferences {

    private static final String TAG = FilterPreferences.class.getSimpleName();

    public static class Selection {
        public final String selection;
        public final String[] selectionArgs;

        public Selection(String selection, String[] selectionArgs) {
            this.selection = selection;
            this.selectionArgs = selectionArgs;
        }

        @Override
        public String toString() {
            return Selection.class.getSimpleName() + ": " + selection + ", " + Arrays.toString(selectionArgs);
        }
    }

    /**
     * @return a string which can be used as an selection clause in a query.
     */
    public static Selection getSelectionClause(Context context) {
        Log.v(TAG, "getSelectionClause");
        String[] columnNames = NetMonColumns.getColumnNames(context);
        // TODO only filterable columns
        StringBuilder selection = new StringBuilder();
        List<String> selectionArgs = new ArrayList<String>();
        for (String columnName : columnNames)
            addFilterSelection(context, columnName, selection, selectionArgs);
        String[] selectionArgsArr = new String[selectionArgs.size()];
        selectionArgs.toArray(selectionArgsArr);
        Selection result = new Selection(selection.toString(), selectionArgsArr);
        Log.v(TAG, "returning " + result);
        return result;

    }

    private static void addFilterSelection(Context context, String columnName, StringBuilder outSelection, List<String> outSelectionArgs) {
        Log.v(TAG, "addFilterSelection for " + columnName);
        List<String> values = NetMonPreferences.getInstance(context).getColumnFilterValues(columnName);
        if (values != null && values.size() > 0) {
            if (outSelection.length() > 0) outSelection.append(" AND ");
            outSelection.append(" " + columnName + " in (");
            for (int i = 0; i < values.size(); i++) {
                outSelection.append("?");
                outSelectionArgs.add(values.get(i));
                if (i < values.size() - 1) outSelection.append(",");
            }
            outSelection.append(")\n");
        }
    }

}