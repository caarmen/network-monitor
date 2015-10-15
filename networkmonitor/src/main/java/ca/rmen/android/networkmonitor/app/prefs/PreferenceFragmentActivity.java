/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2013-2015 Carmen Alvarez (c@rmen.ca)
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
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;

import java.util.Arrays;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dbops.ui.Clear;
import ca.rmen.android.networkmonitor.app.dbops.ui.Compress;
import ca.rmen.android.networkmonitor.app.dbops.ui.Import;
import ca.rmen.android.networkmonitor.app.dbops.ui.Share;
import ca.rmen.android.networkmonitor.app.dialog.ChoiceDialogFragment.DialogItemListener;
import ca.rmen.android.networkmonitor.app.dialog.ConfirmDialogFragment.DialogButtonListener;
import ca.rmen.android.networkmonitor.app.dialog.DialogFragmentFactory;
import ca.rmen.android.networkmonitor.app.dialog.InfoDialogFragment.InfoDialogListener;
import ca.rmen.android.networkmonitor.util.Log;

/**
 * Since AdvancedPreferencesActivity is a PreferenceActivity, which extends Activity instead of FragmentActivity, we need this "helper" activity for preference
 * functions which require a FragmentActivity.
 * This activity has a transparent theme. The only thing the user will see will be alert dialogs that this activity creates.
 *
 * TODO: We no longer use PreferenceActivities, so we should be able to remove this class.
 */
public class PreferenceFragmentActivity extends AppCompatActivity implements DialogItemListener, DialogButtonListener, OnDismissListener, OnCancelListener,
        InfoDialogListener { // NO_UCD (use default)
    public static final String ACTION_SHARE = PreferenceFragmentActivity.class.getPackage().getName() + "_share";
    public static final String ACTION_CLEAR = PreferenceFragmentActivity.class.getPackage().getName() + "_clear";
    public static final String ACTION_IMPORT = PreferenceFragmentActivity.class.getPackage().getName() + "_import";
    public static final String ACTION_COMPRESS = PreferenceFragmentActivity.class.getPackage().getName() + "_compress";
    public static final String ACTION_CLEAR_OLD = PreferenceFragmentActivity.class.getPackage().getName() + "_clear_old";
    public static final String ACTION_CHECK_LOCATION_SETTINGS = PreferenceFragmentActivity.class.getPackage().getName() + "_check_location_settings";
    public static final String ACTION_SHOW_INFO_DIALOG = PreferenceFragmentActivity.class.getPackage().getName() + "_show_info_dialog";
    public static final String ACTION_SHOW_WARNING_DIALOG = PreferenceFragmentActivity.class.getPackage().getName() + "_show_warning_dialog";
    public static final String EXTRA_IMPORT_URI = PreferenceFragmentActivity.class.getPackage().getName() + "_db_url";
    public static final String EXTRA_DIALOG_TITLE = PreferenceFragmentActivity.class.getPackage().getName() + "_dialog_title";
    public static final String EXTRA_DIALOG_MESSAGE = PreferenceFragmentActivity.class.getPackage().getName() + "_dialog_message";

    private static final String TAG = Constants.TAG + PreferenceFragmentActivity.class.getSimpleName();
    private static final int ID_ACTION_SHARE = 1;
    private static final int ID_ACTION_CLEAR = 2;
    private static final int ID_ACTION_IMPORT = 3;
    private static final int ID_ACTION_LOCATION_SETTINGS = 4;
    private static final int ID_ACTION_COMPRESS = 5;

    // True if the user interacted with a dialog other than to dismiss it.
    // IE: they clicked "ok" or selected an item from the list.
    private boolean mUserInput = false;

    @Override
    protected void onCreate(Bundle bundle) {
        Log.v(TAG, "onCreate, bundle = " + bundle);
        super.onCreate(bundle);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.v(TAG, "onNewIntent: intent = " + intent);
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        Log.v(TAG, "handleIntent: intent =  " + intent);
        String action = intent.getAction();
        if (ACTION_SHARE.equals(action)) {
            DialogFragmentFactory.showChoiceDialog(this, getString(R.string.export_choice_title), getResources().getStringArray(R.array.export_choices), -1,
                    ID_ACTION_SHARE);
        } else if (ACTION_CLEAR.equals(action)) {
            DialogFragmentFactory.showConfirmDialog(this, getString(R.string.action_clear), getString(R.string.confirm_logs_clear), ID_ACTION_CLEAR, null);
        } else if (ACTION_IMPORT.equals(action)) {
            // Get the file the user selected, and show a dialog asking for confirmation to import the file.
            Uri importFile = intent.getExtras().getParcelable(EXTRA_IMPORT_URI);
            DialogFragmentFactory.showConfirmDialog(this, getString(R.string.import_confirm_title),
                    getString(R.string.import_confirm_message, importFile.getPath()), ID_ACTION_IMPORT, getIntent().getExtras());
        } else if (ACTION_CHECK_LOCATION_SETTINGS.equals(action)) {
            checkLocationSettings();
        } else if (ACTION_COMPRESS.equals(action)) {
            DialogFragmentFactory.showConfirmDialog(this, getString(R.string.compress_confirm_title), getString(R.string.compress_confirm_message),
                    ID_ACTION_COMPRESS, intent.getExtras());
        } else if (ACTION_CLEAR_OLD.equals(action)) {
            Clear.clear(this, NetMonPreferences.getInstance(this).getDBRecordCount());
        } else if (ACTION_SHOW_INFO_DIALOG.equals(action)) {
            DialogFragmentFactory.showInfoDialog(this, intent.getExtras().getString(EXTRA_DIALOG_TITLE), intent.getExtras().getString(EXTRA_DIALOG_MESSAGE));
        } else if (ACTION_SHOW_WARNING_DIALOG.equals(action)) {
            DialogFragmentFactory.showWarningDialog(this, intent.getExtras().getString(EXTRA_DIALOG_TITLE), intent.getExtras().getString(EXTRA_DIALOG_MESSAGE));
        } else {
            Log.w(TAG, "Activity created without a known action.  Action=" + action);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        Log.v(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop");
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        Log.v(TAG, "onBackPressed");
        super.onBackPressed();
    }

    @Override
    public void onItemSelected(int actionId, CharSequence[] choices, int which) {
        Log.v(TAG, "onItemSelected: actionId =  " + actionId + ", choices = " + Arrays.toString(choices) + ", which = " + which);
        mUserInput = true;
        // The user picked a file format to export.
        if (actionId == ID_ACTION_SHARE) {
            String[] exportChoices = getResources().getStringArray(R.array.export_choices);
            String selectedShareFormat = exportChoices[which];
            Share.share(this, selectedShareFormat);
        }
    }

    @Override
    public void onOkClicked(int actionId, Bundle extras) {
        Log.v(TAG, "onOkClicked, actionId=" + actionId + ", extras = " + extras);
        mUserInput = true;
        if (isFinishing()) return;
        // The user confirmed to clear the logs.
        if (actionId == ID_ACTION_CLEAR) {
            Log.v(TAG, "Clicked ok to clear log");
            Clear.clear(this, 0);
        }
        // Import the database in a background thread.
        else if (actionId == ID_ACTION_IMPORT) {
            final Uri uri = extras.getParcelable(EXTRA_IMPORT_URI);
            Import.importDb(this, uri);
        }
        // Compress the database in a background thread
        else if (actionId == ID_ACTION_COMPRESS) {
            Compress.compress(this);
        } else if (actionId == ID_ACTION_LOCATION_SETTINGS) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onCancelClicked(int actionId, Bundle extras) {
        Log.v(TAG, "onCancelClicked, actionId=" + actionId + ", extras = " + extras);
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
        if (!isFinishing()) finish();
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
