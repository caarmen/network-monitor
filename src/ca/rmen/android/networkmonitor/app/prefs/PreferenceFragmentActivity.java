/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2013 Carmen Alvarez (c@rmen.ca)
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
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.db.DBCompress;
import ca.rmen.android.networkmonitor.app.db.DBImport;
import ca.rmen.android.networkmonitor.app.dialog.DialogFragmentFactory;
import ca.rmen.android.networkmonitor.app.dialog.TransparentDialogActivity;
import ca.rmen.android.networkmonitor.app.main.NetMonAsyncTask;
import ca.rmen.android.networkmonitor.util.Log;

/**
 * Since AdvancedPreferencesActivity is a PreferenceActivity, which extends Activity instead of FragmentActivity, we need this "helper" activity for preference
 * functions which require a FragmentActivity.
 * This activity has a transparent theme. The only thing the user will see will be alert dialogs that this activity creates.
 */
public class PreferenceFragmentActivity extends TransparentDialogActivity { // NO_UCD (use default)
    public static final String ACTION_IMPORT = PreferenceFragmentActivity.class.getPackage().getName() + "_import";
    public static final String ACTION_COMPRESS = PreferenceFragmentActivity.class.getPackage().getName() + "_compress";
    public static final String ACTION_CHECK_LOCATION_SETTINGS = PreferenceFragmentActivity.class.getPackage().getName() + "_check_location_settings";
    public static final String EXTRA_IMPORT_URI = PreferenceFragmentActivity.class.getPackage().getName() + "_db_url";

    private static final String TAG = Constants.TAG + PreferenceFragmentActivity.class.getSimpleName();
    private static final int ID_ACTION_IMPORT = 1;
    private static final int ID_ACTION_LOCATION_SETTINGS = 2;
    private static final int ID_ACTION_COMPRESS = 3;

    @Override
    protected void onCreate(Bundle bundle) {
        Log.v(TAG, "onCreate, bundle = " + bundle);
        super.onCreate(bundle);
        String action = getIntent().getAction();
        if (ACTION_IMPORT.equals(action)) {
            // Get the file the user selected, and show a dialog asking for confirmation to import the file.
            Uri importFile = getIntent().getExtras().getParcelable(EXTRA_IMPORT_URI);
            DialogFragmentFactory.showConfirmDialog(this, getString(R.string.import_confirm_title),
                    getString(R.string.import_confirm_message, importFile.getPath()), ID_ACTION_IMPORT, getIntent().getExtras());
        } else if (ACTION_CHECK_LOCATION_SETTINGS.equals(action)) {
            checkLocationSettings();
        } else if (ACTION_COMPRESS.equals(action)) {
            DialogFragmentFactory.showConfirmDialog(this, getString(R.string.compress_confirm_title), getString(R.string.compress_confirm_message),
                    ID_ACTION_COMPRESS, getIntent().getExtras());
        } else {
            Log.w(TAG, "Activity created without a known action.  Action=" + action);
            finish();
        }
    }

    @Override
    public void onOkClicked(int actionId, Bundle extras) {
        Log.v(TAG, "onClicked, actionId=" + actionId + ", extras = " + extras);
        super.onOkClicked(actionId, extras);
        // Import the database in a background thread.
        if (actionId == ID_ACTION_IMPORT) {
            final Uri uri = extras.getParcelable(EXTRA_IMPORT_URI);
            DBImport dbImport = new DBImport(this, uri);
            NetMonAsyncTask<Boolean> task = new NetMonAsyncTask<Boolean>(this, dbImport, null) {

                @Override
                protected void onPostExecute(Boolean result) {
                    String toastText = result ? getString(R.string.import_successful, uri.getPath()) : getString(R.string.import_failed, uri.getPath());
                    Toast.makeText(PreferenceFragmentActivity.this, toastText, Toast.LENGTH_SHORT).show();
                    super.onPostExecute(result);
                }
            };
            task.execute();
        }
        // Compress the database in a background thread
        else if (actionId == ID_ACTION_COMPRESS) {
            DBCompress dbCompress = new DBCompress(this);
            NetMonAsyncTask<Integer> task = new NetMonAsyncTask<Integer>(this, dbCompress, null) {

                @Override
                protected void onPostExecute(Integer result) {
                    String toastText = result >= 0 ? getString(R.string.compress_successful, result) : getString(R.string.compress_failed);
                    Toast.makeText(PreferenceFragmentActivity.this, toastText, Toast.LENGTH_SHORT).show();
                    super.onPostExecute(result);
                }
            };
            task.execute();
        } else if (actionId == ID_ACTION_LOCATION_SETTINGS) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
            finish();
        }
    }

    /**
     * Checks if we have either the GPS or Network location provider enabled. If not, shows a popup dialog telling the user they should go to the system
     * settings to enable location tracking.
     */
    private void checkLocationSettings() {
        // If the user chose high accuracy, make sure we have at least one location provider.
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))) {
            DialogFragmentFactory.showConfirmDialog(this, getString(R.string.no_location_confirm_dialog_title),
                    getString(R.string.no_location_confirm_dialog_message), ID_ACTION_LOCATION_SETTINGS, null);
        } else {
            finish();
        }
    }

}
