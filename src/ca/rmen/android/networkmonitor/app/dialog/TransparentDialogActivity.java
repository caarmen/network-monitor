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
package ca.rmen.android.networkmonitor.app.dialog;

import java.util.Arrays;

import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.app.dialog.ChoiceDialogFragment.DialogItemListener;
import ca.rmen.android.networkmonitor.app.dialog.ConfirmDialogFragment.DialogButtonListener;
import ca.rmen.android.networkmonitor.app.dialog.InfoDialogFragment.InfoDialogListener;
import ca.rmen.android.networkmonitor.util.Log;

/**
 * This activity has a transparent theme. The only thing the user will see will be alert dialogs that this activity creates.
 * This is useful if we need to show dialogs from a PreferenceActivity,
 * which does not extend FragmentActivity, and cannot manage fragments.
 * */
public class TransparentDialogActivity extends FragmentActivity implements DialogButtonListener, OnDismissListener, DialogItemListener, OnCancelListener,
        InfoDialogListener { // NO_UCD (use default){
    private static final String TAG = Constants.TAG + TransparentDialogActivity.class.getSimpleName();

    public static final String ACTION_SHOW_INFO_DIALOG = TransparentDialogActivity.class.getPackage().getName() + "_show_info_dialog";
    public static final String ACTION_SHOW_WARNING_DIALOG = TransparentDialogActivity.class.getPackage().getName() + "_show_warning_dialog";
    public static final String EXTRA_DIALOG_TITLE = TransparentDialogActivity.class.getPackage().getName() + "_dialog_title";
    public static final String EXTRA_DIALOG_MESSAGE = TransparentDialogActivity.class.getPackage().getName() + "_dialog_message";

    // True if the user interacted with a dialog other than to dismiss it.
    // IE: they clicked "ok" or selected an item from the list.
    private boolean mUserInput = false;

    @Override
    protected void onCreate(Bundle bundle) {
        Log.v(TAG, "onCreate, bundle = " + bundle);
        super.onCreate(bundle);
        String action = getIntent().getAction();
        if (ACTION_SHOW_INFO_DIALOG.equals(action)) {
            DialogFragmentFactory.showInfoDialog(this, getIntent().getExtras().getString(EXTRA_DIALOG_TITLE),
                    getIntent().getExtras().getString(EXTRA_DIALOG_MESSAGE));
        } else if (ACTION_SHOW_WARNING_DIALOG.equals(action)) {
            DialogFragmentFactory.showWarningDialog(this, getIntent().getExtras().getString(EXTRA_DIALOG_TITLE),
                    getIntent().getExtras().getString(EXTRA_DIALOG_MESSAGE));
        }
    }

    @Override
    public void onItemSelected(int actionId, CharSequence[] choices, int which) {
        Log.v(TAG, "onItemSelected: actionId =  " + actionId + ", choices = " + Arrays.toString(choices) + ", which = " + which);
        mUserInput = true;
    }

    @Override
    public void onOkClicked(int actionId, Bundle extras) {
        Log.v(TAG, "onClicked, actionId=" + actionId + ", extras = " + extras);
        mUserInput = true;
    }

    @Override
    public void onCancelClicked(int actionId, Bundle extras) {
        Log.v(TAG, "onClicked, actionId=" + actionId + ", extras = " + extras);
        // If the user dismissed the dialog, let's close this transparent activity.
        dismiss();
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

    @Override
    public void onNeutralClicked(int actionId, Bundle extras) {
        Log.v(TAG, "onNeutralClicked, actionId = " + actionId + ", extras = " + extras);
        dismiss();
    }

    private void dismiss() {
        Log.v(TAG, "dismiss");
        setResult(RESULT_CANCELED);
        finish();
    }

}