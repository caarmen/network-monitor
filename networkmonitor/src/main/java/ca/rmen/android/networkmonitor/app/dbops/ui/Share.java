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
package ca.rmen.android.networkmonitor.app.dbops.ui;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dbops.backend.DBOpIntentService;
import ca.rmen.android.networkmonitor.app.dialog.PreferenceDialog;
import ca.rmen.android.networkmonitor.util.Log;

/**
 *
 */
public class Share {
    private static final String TAG = Constants.TAG + Share.class.getSimpleName();

    /**
     * Export the log file in the given format, and display the list of apps to share the file.
     *
     * @param activity The progress of the file export will be displayed in a progress dialog on this activity.
     * @param selectedShareFormat the label of the file format selected by the user.
     */
    public static void share(FragmentActivity activity, String selectedShareFormat) {
        Log.v(TAG, "share " + selectedShareFormat);
        DBOpIntentService.ExportFormat exportFormat;
        if (activity.getString(R.string.export_choice_csv).equals(selectedShareFormat)) {
            exportFormat = DBOpIntentService.ExportFormat.CSV;
        } else if (activity.getString(R.string.export_choice_html).equals(selectedShareFormat)) {
            exportFormat = DBOpIntentService.ExportFormat.HTML;
        } else if (activity.getString(R.string.export_choice_kml).equals(selectedShareFormat)) {
            // The KML export requires a second dialog before we can share, so we return here.
            shareKml(activity);
            return;
        } else if (activity.getString(R.string.export_choice_gnuplot).equals(selectedShareFormat)) {
            // The gnuplot export requires a second activity before we can share, so we return here.
            shareGnuplot(activity);
            return;
        } else if (activity.getString(R.string.export_choice_excel).equals(selectedShareFormat)) {
            exportFormat = DBOpIntentService.ExportFormat.EXCEL;
        } else if (activity.getString(R.string.export_choice_db).equals(selectedShareFormat)) {
            exportFormat = DBOpIntentService.ExportFormat.DB;
        } else {
            exportFormat = DBOpIntentService.ExportFormat.SUMMARY;
        }
        DBOpIntentService.startActionExport(activity, exportFormat);
        activity.finish();
    }

    /**
     * Prompt a user for the field they want to export to KML, then do the export.
     */
    private static void shareKml(final FragmentActivity activity) {
        Log.v(TAG, "shareKml");

        PreferenceDialog.showKMLExportColumnChoiceDialog(activity, new PreferenceDialog.PreferenceChoiceDialogListener() {

            @Override
            public void onPreferenceValueSelected(String value) {
                DBOpIntentService.startActionKMLExport(activity, value);
                activity.finish();
            }

            @Override
            public void onCancel() {
                activity.setResult(Activity.RESULT_CANCELED);
                activity.finish();
            }
        });
    }

    private static void shareGnuplot(final FragmentActivity activity) {
        Log.v(TAG, "shareGnuplot");
        Intent intent = new Intent(activity, GnuplotSettingsActivity.class);
        activity.startActivity(intent);
    }


}
