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
package org.jraf.android.networkmonitor.app.prefs;

import android.app.ProgressDialog;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.R;
import org.jraf.android.networkmonitor.app.dialog.ConfirmDialogFragment.DialogButtonListener;
import org.jraf.android.networkmonitor.app.dialog.DialogFragmentFactory;
import org.jraf.android.networkmonitor.app.dialog.ProgressDialogFragment;
import org.jraf.android.networkmonitor.app.importdb.DBImport;
import org.jraf.android.networkmonitor.util.Log;

/**
 * Since AdvancedPreferencesActivity is a PreferenceActivity, which extends Activity instead of FragmentActivity, we need this "helper" activity for preference
 * functions which require a FragmentActivity.
 * This activity has a transparent theme. The only thing the user will see will be alert dialogs that this activity creates.
 */
public class PreferenceFragmentActivity extends FragmentActivity implements DialogButtonListener { // NO_UCD (use default)
    public static final String ACTION_IMPORT = PreferenceFragmentActivity.class.getPackage().getName() + "_import";
    public static final String EXTRA_IMPORT_URI = PreferenceFragmentActivity.class.getPackage().getName() + "_db_url";

    private static final String TAG = Constants.TAG + PreferenceFragmentActivity.class.getSimpleName();
    private static final String PROGRESS_DIALOG_FRAGMENT_TAG = "progress_dialog_fragment_tag";
    private static final int ID_ACTION_IMPORT = 1;

    @Override
    protected void onCreate(Bundle bundle) {
        Log.v(TAG, "onCreate, bundle = " + bundle);
        super.onCreate(bundle);
        String action = getIntent().getAction();
        if (ACTION_IMPORT.equals(action)) {
            Uri importFile = getIntent().getExtras().getParcelable(EXTRA_IMPORT_URI);
            DialogFragmentFactory.showConfirmDialog(this, getString(R.string.import_confirm_title),
                    getString(R.string.import_confirm_message, importFile.getPath()), ID_ACTION_IMPORT, getIntent().getExtras());
        } else {
            Log.w(TAG, "Activity created without a known action.  Action=" + action);
            finish();
        }
    }

    @Override
    public void onOkClicked(int actionId, Bundle extras) {
        Log.v(TAG, "onClicked, actionId=" + actionId + ", extras = " + extras);
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
                    if (dialogFragment != null) dialogFragment.dismiss();
                    String toastText = result ? getString(R.string.import_successful, uri.getPath()) : getString(R.string.import_failed, uri.getPath());
                    Toast.makeText(PreferenceFragmentActivity.this, toastText, Toast.LENGTH_SHORT).show();
                    finish();
                }
            };
            task.execute();
        }
    }

    @Override
    public void onCancelClicked(int actionId, Bundle extras) {
        Log.v(TAG, "onClicked, actionId=" + actionId + ", extras = " + extras);
        finish();
    }
}
