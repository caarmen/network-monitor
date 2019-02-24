/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014-2015 Carmen Alvarez (c@rmen.ca)
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
package ca.rmen.android.networkmonitor.app.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AlertDialog;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import android.util.Log;

/**
 * Displays dialogs for the user to set preferences.
 */
public class PreferenceDialog {
    public interface PreferenceChoiceDialogListener {
        /**
         * The user selected a new value for the preference, and it has been persisted.
         */
        void onPreferenceValueSelected(String value);

        /**
         * The user dismissed the dialog, either by tapping back or by pressing on the Cancel button.
         */
        void onCancel();
    }

    private static final String TAG = Constants.TAG + PreferenceDialog.class.getSimpleName();

    /**
     * Show the user a dialog to select the primary data field for a KML export.
     */
    public static void showKMLExportColumnChoiceDialog(Context context, PreferenceDialog.PreferenceChoiceDialogListener listener) {
        showPreferenceChoiceDialog(context, NetMonPreferences.PREF_KML_EXPORT_COLUMN, NetMonColumns.SOCKET_CONNECTION_TEST, R.array.db_columns,
                NetMonColumns.getColumnLabels(context), R.string.export_kml_choice_title, listener);
    }

    /**
     * Show the user a dialog to select how many records to display in the log view.
     */
    public static Dialog showFilterRecordCountChoiceDialog(Context context, PreferenceDialog.PreferenceChoiceDialogListener listener) {
        return showPreferenceChoiceDialog(context, NetMonPreferences.PREF_FILTER_RECORD_COUNT, NetMonPreferences.PREF_FILTER_RECORD_COUNT_DEFAULT,
                R.array.preferences_filter_record_count_values, R.array.preferences_filter_record_count_labels, R.string.pref_title_filter_record_count,
                listener);
    }

    /**
     * Show the user a dialog to choose the format for the cell ids.
     */
    public static Dialog showCellIdFormatChoiceDialog(Context context, PreferenceDialog.PreferenceChoiceDialogListener listener) {
        return showPreferenceChoiceDialog(context, NetMonPreferences.PREF_CELL_ID_FORMAT, NetMonPreferences.PREF_CELL_ID_FORMAT_DEFAULT,
                R.array.preferences_cell_id_format_values, R.array.preferences_cell_id_format_labels, R.string.pref_title_cell_id_format, listener);
    }

    /**
     * Show the user a preference choice dialog.
     */
    private static Dialog showPreferenceChoiceDialog(Context context, final String preferenceName, String defaultValue, int valuesArrayId,
            int labelsArrayId, int titleId, final PreferenceDialog.PreferenceChoiceDialogListener listener) {
        return showPreferenceChoiceDialog(context, preferenceName, defaultValue, valuesArrayId, context.getResources().getStringArray(labelsArrayId), titleId,
                listener);
    }

    /**
     * Show the user a preference choice dialog.
     */
    private static Dialog showPreferenceChoiceDialog(final Context context, final String preferenceName, String defaultValue, int valuesArrayId,
            String[] labels, int titleId, final PreferenceDialog.PreferenceChoiceDialogListener listener) {
        Log.v(TAG, "showPreferenceChoiceDialog");
        final SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        // Find the position in the list of choices which corresponds to our current preference.
        final String[] values = context.getResources().getStringArray(valuesArrayId);
        String currentValue = sharedPrefs.getString(preferenceName, defaultValue);
        int currentPrefPosition = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(currentValue)) {
                currentPrefPosition = i;
                break;
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(titleId)
                .setSingleChoiceItems(labels, currentPrefPosition, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    // Save the preference for the record count.
                    int selectedItemPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                    final String selectedItemValue = values[selectedItemPosition];
                    sharedPrefs.edit().putString(preferenceName, selectedItemValue).apply();
                    listener.onPreferenceValueSelected(selectedItemValue);
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> listener.onCancel());
        final Dialog dialog = builder.create();
        dialog.setOnCancelListener(dialogInterface -> listener.onCancel());
        dialog.show();
        return dialog;
    }
}
