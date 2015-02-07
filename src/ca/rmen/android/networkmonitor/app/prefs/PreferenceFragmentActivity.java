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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.db.DBCompress;
import ca.rmen.android.networkmonitor.app.db.DBImport;
import ca.rmen.android.networkmonitor.app.dialog.ConfirmDialogFragment.DialogButtonListener;
import ca.rmen.android.networkmonitor.app.dialog.DialogFragmentFactory;
import ca.rmen.android.networkmonitor.app.dialog.InfoDialogFragment.InfoDialogListener;
import ca.rmen.android.networkmonitor.app.dialog.ProgressDialogFragment;
import ca.rmen.android.networkmonitor.util.Log;

/**
 * Since AdvancedPreferencesActivity is a PreferenceActivity, which extends Activity instead of FragmentActivity, we need this "helper" activity for preference
 * functions which require a FragmentActivity.
 * This activity has a transparent theme. The only thing the user will see will be alert dialogs that this activity creates.
 */
public class PreferenceFragmentActivity extends FragmentActivity implements DialogButtonListener, OnDismissListener, OnCancelListener, InfoDialogListener { // NO_UCD (use default)
    public static final String ACTION_IMPORT = PreferenceFragmentActivity.class.getPackage().getName() + "_import";
    public static final String ACTION_COMPRESS = PreferenceFragmentActivity.class.getPackage().getName() + "_compress";
    public static final String ACTION_CHECK_LOCATION_SETTINGS = PreferenceFragmentActivity.class.getPackage().getName() + "_check_location_settings";
    public static final String ACTION_SHOW_INFO_DIALOG = PreferenceFragmentActivity.class.getPackage().getName() + "_show_info_dialog";
    public static final String ACTION_SHOW_WARNING_DIALOG = PreferenceFragmentActivity.class.getPackage().getName() + "_show_warning_dialog";
    public static final String EXTRA_IMPORT_URI = PreferenceFragmentActivity.class.getPackage().getName() + "_db_url";
    public static final String EXTRA_DIALOG_TITLE = PreferenceFragmentActivity.class.getPackage().getName() + "_dialog_title";
    public static final String EXTRA_DIALOG_MESSAGE = PreferenceFragmentActivity.class.getPackage().getName() + "_dialog_message";

    private static final String TAG = Constants.TAG + PreferenceFragmentActivity.class.getSimpleName();
    private static final String PROGRESS_DIALOG_FRAGMENT_TAG = "progress_dialog_fragment_tag";
    private static final int ID_ACTION_IMPORT = 1;
    private static final int ID_ACTION_LOCATION_SETTINGS = 2;
    private static final int ID_ACTION_COMPRESS = 3;

    // True if the user interacted with a dialog other than to dismiss it.
    // IE: they clicked "ok" or selected an item from the list.
    private boolean mUserInput = false;

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
        } else if (ACTION_SHOW_INFO_DIALOG.equals(action)) {
            DialogFragmentFactory.showInfoDialog(this, getIntent().getExtras().getString(EXTRA_DIALOG_TITLE),
                    getIntent().getExtras().getString(EXTRA_DIALOG_MESSAGE));
        } else if (ACTION_SHOW_WARNING_DIALOG.equals(action)) {
            DialogFragmentFactory.showWarningDialog(this, getIntent().getExtras().getString(EXTRA_DIALOG_TITLE),
                    getIntent().getExtras().getString(EXTRA_DIALOG_MESSAGE));
        } else {
            Log.w(TAG, "Activity created without a known action.  Action=" + action);
            finish();
        }
    }

    @Override
    public void onOkClicked(int actionId, Bundle extras) {
        Log.v(TAG, "onClicked, actionId=" + actionId + ", extras = " + extras);
        mUserInput = true;
        // Import the database in a background thread.
        if (actionId == ID_ACTION_IMPORT) {
            final Uri uri = extras.getParcelable(EXTRA_IMPORT_URI);
            AsyncTask<Void, Void, Boolean> task = new AsyncTask<Void, Void, Boolean>() {

                @Override
                protected void onPreExecute() {
                    DialogFragmentFactory.showProgressDialog(PreferenceFragmentActivity.this, getString(R.string.progress_dialog_message),
                            ProgressDialog.STYLE_SPINNER, PROGRESS_DIALOG_FRAGMENT_TAG);
                }

                @Override
                protected Boolean doInBackground(Void... params) {
                    try {
                        Log.v(TAG, "Importing db from " + uri);
                        DBImport.importDB(PreferenceFragmentActivity.this, uri);
                        return true;
                    } catch (Exception e) {
                        Log.e(TAG, "Error importing db: " + e.getMessage(), e);
                        return false;
                    }
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    ProgressDialogFragment dialogFragment = (ProgressDialogFragment) getSupportFragmentManager()
                            .findFragmentByTag(PROGRESS_DIALOG_FRAGMENT_TAG);
                    if (dialogFragment != null) dialogFragment.dismissAllowingStateLoss();
                    String toastText = result ? getString(R.string.import_successful, uri.getPath()) : getString(R.string.import_failed, uri.getPath());
                    Toast.makeText(PreferenceFragmentActivity.this, toastText, Toast.LENGTH_SHORT).show();
                    finish();
                }
            };
            task.execute();
        }
        // Compress the database in a background thread
        else if (actionId == ID_ACTION_COMPRESS) {
            AsyncTask<Void, Void, Integer> task = new AsyncTask<Void, Void, Integer>() {

                @Override
                protected void onPreExecute() {
                    DialogFragmentFactory.showProgressDialog(PreferenceFragmentActivity.this, getString(R.string.progress_dialog_message),
                            ProgressDialog.STYLE_SPINNER, PROGRESS_DIALOG_FRAGMENT_TAG);
                }

                @Override
                protected Integer doInBackground(Void... params) {
                    try {
                        Log.v(TAG, "Compressing db");
                        return DBCompress.compressDB(PreferenceFragmentActivity.this);
                    } catch (Exception e) {
                        Log.e(TAG, "Error compressing db: " + e.getMessage(), e);
                        return -1;
                    }
                }

                @Override
                protected void onPostExecute(Integer result) {
                    ProgressDialogFragment dialogFragment = (ProgressDialogFragment) getSupportFragmentManager()
                            .findFragmentByTag(PROGRESS_DIALOG_FRAGMENT_TAG);
                    if (dialogFragment != null) dialogFragment.dismissAllowingStateLoss();
                    String toastText = result >= 0 ? getString(R.string.compress_successful, result) : getString(R.string.compress_failed);
                    Toast.makeText(PreferenceFragmentActivity.this, toastText, Toast.LENGTH_SHORT).show();
                    finish();
                }
            };
            task.execute();
        } else if (actionId == ID_ACTION_LOCATION_SETTINGS) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onCancelClicked(int actionId, Bundle extras) {
        Log.v(TAG, "onClicked, actionId=" + actionId + ", extras = " + extras);
        // If the user dismissed the dialog, let's close this transparent activity.
        dismiss();
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

    @Override
    public void onDismiss(DialogInterface dialog) {
        Log.v(TAG, "onDismiss");
        if (mUserInput) {
            // Ignore, the dialog was dismissed because the user tapped ok on the dialog or selected an item from the list in the dialog.
        } else {
            dismiss();
        }
    }

    private void dismiss() {
        Log.v(TAG, "dismiss");
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        Log.v(TAG, "onCancel");
        dismiss();
    }

    @Override
    public void onNeutralClicked(int actionId, Bundle extras) {
        Log.v(TAG, "onNeutralClicked, actionId = " + actionId + ", extras = " + extras);
        dismiss();
    }
}
