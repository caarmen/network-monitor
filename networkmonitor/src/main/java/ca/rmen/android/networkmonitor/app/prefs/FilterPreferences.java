/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014-2019 Carmen Alvarez (c@rmen.ca)
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
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;


/**
 * Based on the filtering preferences the user has chosen, build a selection clause which can be used in the DB query.
 */
public class FilterPreferences {

    private static final String TAG = Constants.TAG + FilterPreferences.class.getSimpleName();
    static final String EMPTY = "##EMPTY##";

    public static class Selection {
        public final String selectionString;
        public final String[] selectionArgs;

        public Selection(String selectionString, String[] selectionArgs) {
            this.selectionString = selectionString;
            this.selectionArgs = selectionArgs;
        }

        @Override
        @NonNull
        public String toString() {
            return Selection.class.getSimpleName() + ": " + selectionString + ", " + Arrays.toString(selectionArgs);
        }
    }

    /**
     * @return a selection clause based on all the filters the user chose for all columns, except the given excludeColumn.
     */
    static Selection getSelectionClause(Context context, String excludeColumn) {
        Log.v(TAG, "getSelectionClause: exclude column " + excludeColumn);
        String[] filterableColumnNames = NetMonColumns.getFilterableColumns(context);
        List<String> filterableColumnNamesList = new ArrayList<>();
        for (String filterableColumnName : filterableColumnNames)
            if (!filterableColumnName.equals(excludeColumn)) filterableColumnNamesList.add(filterableColumnName);
        return getSelectionClause(context, filterableColumnNamesList);
    }

    /**
     * @return a selection clause based on all the filters the user chose for all columns.
     */
    public static Selection getSelectionClause(Context context) {
        Log.v(TAG, "getSelectionClause");
        String[] filterableColumnNames = NetMonColumns.getFilterableColumns(context);
        return getSelectionClause(context, Arrays.asList(filterableColumnNames));
    }

    /**
     * @return a selection clause based on all the filters the user chose for the given column names.
     */
    private static Selection getSelectionClause(Context context, List<String> columnNames) {
        List<String> selectionStrings = new ArrayList<>();
        List<String> selectionArgs = new ArrayList<>();
        for (String columnName : columnNames) {
            Selection selection = createSelection(context, columnName);
            if (selection != null) {
                selectionStrings.add(selection.selectionString);
                selectionArgs.addAll(Arrays.asList(selection.selectionArgs));
            }
        }
        String selectionString = TextUtils.join(" AND ", selectionStrings);
        Selection result = new Selection(selectionString, selectionArgs.toArray(new String[0]));
        Log.v(TAG, "returning " + result);
        return result;
    }

    /**
     * Create a selection clause for the filters the user chose for the given column.
     */
    private static Selection createSelection(Context context, String columnName) {
        Log.v(TAG, "addFilterSelection for " + columnName);
        List<String> values = NetMonPreferences.getInstance(context).getColumnFilterValues(columnName);
        if (values != null && values.size() > 0) {

            StringBuilder selectionString = new StringBuilder();
            List<String> selectionArgs = new ArrayList<>();
            selectionString.append("(");
            // If we support empty value, add the special condition for that.
            if (values.contains(EMPTY)) {
                selectionString.append(columnName).append(" IS NULL OR ")
                        .append(columnName).append(" = ''");
                // If we only have the empty value, we're done.
                if (values.size() == 1) {
                    selectionString.append(")\n");
                    return new Selection(selectionString.toString(), new String[0]);
                } else
                    selectionString.append(" OR ");
            }

            // Add each value (other than the special empty value).
            selectionString.append(columnName).append(" in (");
            for (int i = 0; i < values.size(); i++) {
                if (values.get(i).equals(EMPTY)) continue;
                selectionString.append("?");
                selectionArgs.add(values.get(i));
                if (i < values.size() - 1) selectionString.append(",");
            }
            selectionString.append("))\n");
            return new Selection(selectionString.toString(), selectionArgs.toArray(new String[0]));
        }
        return null;
    }
}