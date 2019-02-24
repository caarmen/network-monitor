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
package ca.rmen.android.networkmonitor.app.main;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences;

/**
 * Warn the user about battery and data consumption.
 */
public class WarningDialogFragment extends DialogFragment { // NO_UCD (use default)

    private static final String TAG = Constants.TAG + WarningDialogFragment.class.getSimpleName();

    /**
     * An activity which contains a confirmation dialog fragment should implement this interface to be notified if the user clicks ok on the dialog.
     */
    public interface DialogButtonListener {
        void onAppWarningOkClicked();

        void onAppWarningCancelClicked();
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.v(TAG, "onCreateDialog: savedInstanceState = " + savedInstanceState);

        Activity activity = getActivity();
        if (activity == null) return super.onCreateDialog(savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.app_warning_title);
        final View view = View.inflate(activity, R.layout.warning_dialog, null);
        builder.setView(view);
        OnClickListener positiveListener = null;
        OnClickListener negativeListener = null;
        if (activity instanceof DialogButtonListener) {
            positiveListener = (dialog, which) -> {
                Log.v(TAG, "onClick (positive button");
                if (activity.isFinishing()) {
                    Log.w(TAG, "User clicked on dialog after it was detached from activity. Monkey?");
                } else {
                    CheckBox showWarningDialog = view.findViewById(R.id.app_warning_cb_stfu);
                    NetMonPreferences.getInstance(activity).setShowAppWarning(!showWarningDialog.isChecked());
                    ((DialogButtonListener) activity).onAppWarningOkClicked();
                }
            };
            negativeListener = (dialog, which) -> {
                Log.v(TAG, "onClick (negative button");
                if (activity.isFinishing())
                    Log.w(TAG, "User clicked on dialog after it was detached from activity. Monkey?");
                else
                    ((DialogButtonListener) activity).onAppWarningCancelClicked();
            };
        }
        builder.setNegativeButton(R.string.app_warning_cancel, negativeListener);
        builder.setPositiveButton(R.string.app_warning_ok, positiveListener);
        builder.setCancelable(false);
        final Dialog dialog = builder.create();
        dialog.setCancelable(false);
        setCancelable(false);
        return dialog;
    }

    public static void show(FragmentActivity activity) {
        WarningDialogFragment fragment = new WarningDialogFragment();
        fragment.show(activity.getSupportFragmentManager(), TAG);
    }
}
