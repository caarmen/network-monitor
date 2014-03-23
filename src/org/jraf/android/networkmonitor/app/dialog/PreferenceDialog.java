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
package org.jraf.android.networkmonitor.app.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.view.ContextThemeWrapper;

import org.jraf.android.networkmonitor.R;
import org.jraf.android.networkmonitor.app.prefs.NetMonPreferences;
import org.jraf.android.networkmonitor.provider.NetMonColumns;
import org.jraf.android.networkmonitor.util.Log;

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

    private static final String TAG = PreferenceDialog.class.getSimpleName();

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
    public static void showFilterRecordCountChoiceDialog(Context context, PreferenceDialog.PreferenceChoiceDialogListener listener) {
        showPreferenceChoiceDialog(context, NetMonPreferences.PREF_FILTER_RECORD_COUNT, NetMonPreferences.PREF_FILTER_RECORD_COUNT_DEFAULT,
                R.array.preferences_filter_record_count_values, R.array.preferences_filter_record_count_labels, R.string.pref_title_filter_record_count,
                listener);
    }

    /**
     * Show the user a dialog to choose the format for the cell ids.
     */
    public static void showCellIdFormatChoiceDialog(Context context, PreferenceDialog.PreferenceChoiceDialogListener listener) {
        showPreferenceChoiceDialog(context, NetMonPreferences.PREF_CELL_ID_FORMAT, NetMonPreferences.PREF_CELL_ID_FORMAT_DEFAULT,
                R.array.preferences_cell_id_format_values, R.array.preferences_cell_id_format_labels, R.string.pref_title_cell_id_format, listener);
    }

    /**
     * Show the user a preference choice dialog.
     */
    private static void showPreferenceChoiceDialog(Context context, final String preferenceName, String defaultValue, int valuesArrayId, int labelsArrayId,
            int titleId, final PreferenceDialog.PreferenceChoiceDialogListener listener) {
        showPreferenceChoiceDialog(context, preferenceName, defaultValue, valuesArrayId, context.getResources().getStringArray(labelsArrayId), titleId,
                listener);
    }

    /**
     * Show the user a preference choice dialog.
     */
    private static void showPreferenceChoiceDialog(final Context context, final String preferenceName, String defaultValue, int valuesArrayId, String[] labels,
            int titleId, final PreferenceDialog.PreferenceChoiceDialogListener listener) {
        Log.v(TAG, "showPreferenceChoic@eDialog");
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
        // Build a chooser dialog for the preference
        Context contextWrapper = new ContextThemeWrapper(context, R.style.dialogStyle);
        AlertDialog.Builder builder = new AlertDialog.Builder(contextWrapper).setSingleChoiceItems(labels, currentPrefPosition, null).setPositiveButton(
                android.R.string.ok, new OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Save the preference for the record count.
                        int selectedItemPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                        final String selectedItemValue = values[selectedItemPosition];
                        new AsyncTask<Void, Void, Void>() {

                            // Save our preference
                            @Override
                            protected Void doInBackground(Void... params) {
                                sharedPrefs.edit().putString(preferenceName, selectedItemValue).commit();
                                return null;
                            }

                            // Notify the listener
                            @Override
                            protected void onPostExecute(Void result) {
                                listener.onPreferenceValueSelected(selectedItemValue);
                            }
                        }.execute();
                    }
                });
        DialogStyle.setCustomTitle(context, builder, context.getString(titleId));
        // Manage canceling: the user can either click on the cancel button or can tap back to dismiss the dialog
        builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                listener.onCancel();
            }
        });
        final AlertDialog dialog = builder.create();
        new NetMonDialogStyleHacks(context).styleDialog(dialog);
        dialog.setOnCancelListener(new OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                listener.onCancel();
            }
        });
        dialog.show();
    }
}
