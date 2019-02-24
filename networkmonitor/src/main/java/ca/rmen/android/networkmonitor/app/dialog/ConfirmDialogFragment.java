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
package ca.rmen.android.networkmonitor.app.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;

import ca.rmen.android.networkmonitor.Constants;

/**
 * A dialog fragment with a title, message, ok and cancel buttons. This is based on ConfirmDialogFragment from the scrum chatter project.
 */
public class ConfirmDialogFragment extends DialogFragment { // NO_UCD (use default)

    private static final String TAG = Constants.TAG + ConfirmDialogFragment.class.getSimpleName();

    /**
     * An activity which contains a confirmation dialog fragment should implement this interface to be notified if the user clicks ok on the dialog.
     */
    public interface DialogButtonListener {
        void onOkClicked(int actionId, Bundle extras);

        void onCancelClicked(int actionId, Bundle extras);
    }

    public ConfirmDialogFragment() {
        super();
    }

    /**
     * @return a Dialog with a title, message, ok, and cancel buttons.
     */
    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.v(TAG, "onCreateDialog: savedInstanceState = " + savedInstanceState);

        Activity activity = getActivity();
        Bundle arguments = getArguments();
        if (activity == null || arguments == null) return super.onCreateDialog(savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(arguments.getCharSequence(DialogFragmentFactory.EXTRA_TITLE));
        builder.setMessage(arguments.getCharSequence(DialogFragmentFactory.EXTRA_MESSAGE));
        final int actionId = arguments.getInt(DialogFragmentFactory.EXTRA_ACTION_ID);
        final Bundle extras = arguments.getBundle(DialogFragmentFactory.EXTRA_EXTRAS);
        OnClickListener positiveListener = null;
        OnClickListener negativeListener = null;
        if (activity instanceof DialogButtonListener) {
            positiveListener = (dialog, which) -> {
                Log.v(TAG, "onClick (positive button");
                if (activity.isFinishing()) Log.w(TAG, "User clicked on dialog after it was detached from activity. Monkey?");
                else
                    ((DialogButtonListener) activity).onOkClicked(actionId, extras);
            };
            negativeListener = (dialog, which) -> {
                Log.v(TAG, "onClick (negative button");
                if (activity.isFinishing()) Log.w(TAG, "User clicked on dialog after it was detached from activity. Monkey?");
                else
                    ((DialogButtonListener) activity).onCancelClicked(actionId, extras);
            };
        }
        builder.setNegativeButton(android.R.string.cancel, negativeListener);
        builder.setPositiveButton(android.R.string.ok, positiveListener);
        if (activity instanceof OnCancelListener) builder.setOnCancelListener((OnCancelListener) activity);
        final Dialog dialog = builder.create();
        if (activity instanceof OnDismissListener) dialog.setOnDismissListener((OnDismissListener) activity);
        return dialog;

    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        Log.v(TAG, "onDismiss");
        super.onDismiss(dialog);
        if (getActivity() instanceof OnDismissListener) ((OnDismissListener) getActivity()).onDismiss(dialog);
    }
}
