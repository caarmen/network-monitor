/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2013 Benoit 'BoD' Lubek (BoD@JRAF.org)
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
package ca.rmen.android.networkmonitor.app.log;

import java.util.Arrays;

import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dialog.ChoiceDialogFragment.DialogItemListener;
import ca.rmen.android.networkmonitor.app.dialog.ConfirmDialogFragment.DialogButtonListener;
import ca.rmen.android.networkmonitor.app.dialog.DialogFragmentFactory;
import ca.rmen.android.networkmonitor.app.useractions.Clear;
import ca.rmen.android.networkmonitor.app.useractions.Share;
import ca.rmen.android.networkmonitor.util.Log;

/**
 * Provides actions on the network monitor log: sharing and clearing the log file.
 * This activity has a transparent theme. The only thing the user will see will be alert dialogs that this activity creates.
 */
public class LogActionsActivity extends FragmentActivity implements DialogButtonListener, DialogItemListener, OnCancelListener, OnDismissListener { // NO_UCD (use default)
    static final String ACTION_SHARE = LogActionsActivity.class.getPackage().getName() + "_share";
    static final String ACTION_CLEAR = LogActionsActivity.class.getPackage().getName() + "_clear";

    private static final String TAG = Constants.TAG + LogActionsActivity.class.getSimpleName();
    // True if the user interacted with a dialog other than to dismiss it.
    // IE: they clicked "ok" or selected an item from the list.
    private boolean mUserInput = false;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        String action = getIntent().getAction();
        if (ACTION_SHARE.equals(action)) {
            DialogFragmentFactory.showChoiceDialog(this, getString(R.string.export_choice_title), getResources().getStringArray(R.array.export_choices), -1,
                    R.id.action_share);
        } else if (ACTION_CLEAR.equals(action)) {
            DialogFragmentFactory.showConfirmDialog(this, getString(R.string.action_clear), getString(R.string.confirm_logs_clear), R.id.action_clear, null);
        } else {
            Log.w(TAG, "Activity created without a known action.  Action=" + action);
            finish();
        }
    }


    @Override
    public void onOkClicked(int actionId, Bundle extras) {
        Log.v(TAG, "onOkClicked, actionId = " + actionId);
        mUserInput = true;
        // The user confirmed to clear the logs.
        if (actionId == R.id.action_clear) {
            Log.v(TAG, "Clicked ok to clear log");
            Clear.clear(this, 0);
        }
    }

    @Override
    public void onItemSelected(int actionId, CharSequence[] choices, int which) {
        Log.v(TAG, "onItemSelected: actionId =  " + actionId + ", choices = " + Arrays.toString(choices) + ", which = " + which);
        mUserInput = true;
        // The user picked a file format to export.
        if (actionId == R.id.action_share) {
            String[] exportChoices = getResources().getStringArray(R.array.export_choices);
            String selectedShareFormat = exportChoices[which];
            Share.share(this, selectedShareFormat);
        }
    }

    @Override
    public void onCancelClicked(int actionId, Bundle extras) {
        Log.v(TAG, "onCancelClicked, actionId = " + actionId);
        if (actionId == R.id.action_clear || actionId == R.id.action_share) dismiss();
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

    /**
     * Listener to finish this activity with a canceled result when the user presses back on a dialog.
     */
    @Override
    public void onCancel(DialogInterface dialog) {
        Log.v(TAG, "onCancel");
        dismiss();
    }

    private void dismiss() {
        Log.v(TAG, "dismiss");
        setResult(RESULT_CANCELED);
        finish();
    }
}
