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

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.R;
import org.jraf.android.networkmonitor.app.dialog.ConfirmDialogFragment.DialogButtonListener;
import org.jraf.android.networkmonitor.app.dialog.DialogFragmentFactory;
import org.jraf.android.networkmonitor.util.Log;

/**
 * Since AdvancedPreferencesActivity is a PreferenceActivity, which extends Activity instead of FragmentActivity, we need this "helper" activity for preference
 * functions which require a FragmentActivity.
 * This activity has a transparent theme. The only thing the user will see will be alert dialogs that this activity creates.
 */
public class PreferenceFragmentActivity extends FragmentActivity implements DialogButtonListener { // NO_UCD (use default)
    public static final String ACTION_IMPORT = PreferenceFragmentActivity.class.getPackage().getName() + "_import";
    public static final String EXTRA_DB_URL = PreferenceFragmentActivity.class.getPackage().getName() + "_db_url";

    private static final String TAG = Constants.TAG + PreferenceFragmentActivity.class.getSimpleName();
    private static final int ID_ACTION_IMPORT = 1;

    @Override
    protected void onCreate(Bundle bundle) {
        Log.v(TAG, "onCreate, bundle = " + bundle);
        super.onCreate(bundle);
        String action = getIntent().getAction();
        if (ACTION_IMPORT.equals(action)) {
            getIntent().getExtras().isEmpty();
            DialogFragmentFactory.showConfirmDialog(this, getString(R.string.pref_title_import), getString(R.string.pref_summary_import), ID_ACTION_IMPORT,
                    getIntent().getExtras());
        } else {
            Log.w(TAG, "Activity created without a known action.  Action=" + action);
            finish();
        }
    }

    @Override
    public void onOkClicked(int actionId, Bundle extras) {
        Log.v(TAG, "onClicked, actionId=" + actionId + ", extras = " + extras);
        if (actionId == ID_ACTION_IMPORT) {
            Toast.makeText(this, "Will  " + extras.getParcelable(EXTRA_DB_URL), Toast.LENGTH_LONG).show();
        }
        finish();
    }

    @Override
    public void onCancelClicked(int actionId, Bundle extras) {
        Log.v(TAG, "onClicked, actionId=" + actionId + ", extras = " + extras);
        finish();
    }
}
