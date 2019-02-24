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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

import ca.rmen.android.networkmonitor.Constants;

/**
 * A dialog fragment with a list of choices.
 */
public class ChoiceDialogFragment extends DialogFragment { // NO_UCD (use default)

    private static final String TAG = Constants.TAG + ChoiceDialogFragment.class.getSimpleName();

    /**
     * An activity which contains a choice dialog fragment should implement this interface.
     */
    public interface DialogItemListener {
        void onItemSelected(int actionId, int which);
    }

    public ChoiceDialogFragment() {
        super();
    }

    /**
     * @return a Dialog with a list of items, one of them possibly pre-selected.
     */
    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.v(TAG, "onCreateDialog: savedInstanceState = " + savedInstanceState);
        Context context = getActivity();
        Bundle arguments = getArguments();
        if (context == null || arguments == null) return super.onCreateDialog(savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(arguments.getString(DialogFragmentFactory.EXTRA_TITLE));
        final int actionId = arguments.getInt(DialogFragmentFactory.EXTRA_ACTION_ID);
        int selectedItem = arguments.getInt(DialogFragmentFactory.EXTRA_SELECTED_ITEM);
        final CharSequence[] choices = arguments.getCharSequenceArray(DialogFragmentFactory.EXTRA_CHOICES);
        OnClickListener listener = null;
        final AtomicBoolean hasClicked = new AtomicBoolean(false);
        if (getActivity() instanceof DialogItemListener) {
            listener = (dialog, which) -> {
                FragmentActivity activity = getActivity();
                if (activity == null) {
                    Log.w(TAG, "User clicked on dialog after it was detached from activity. Monkey?");
                } else if (hasClicked.get()) {
                    Log.w(TAG, "User already clicked once on this dialog! Monkey?");
                } else {
                    hasClicked.set(true);
                    ((DialogItemListener) activity).onItemSelected(actionId, which);
                }
            };
        }
        // If one item is to be pre-selected, use the single choice items layout.
        if (selectedItem >= 0) builder.setSingleChoiceItems(choices, selectedItem, listener);
        // If no particular item is to be pre-selected, use the default list item layout.
        else
            builder.setItems(choices, listener);

        if (getActivity() instanceof OnCancelListener) builder.setOnCancelListener((OnCancelListener) getActivity());
        final Dialog dialog = builder.create();
        if (getActivity() instanceof OnDismissListener) dialog.setOnDismissListener((OnDismissListener) getActivity());
        return dialog;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        Log.v(TAG, "onDismiss");
        super.onDismiss(dialog);
        AlertDialog alertDialog = (AlertDialog) dialog;
        int selectedItemPosition = alertDialog.getListView().getSelectedItemPosition();
        Log.v(TAG, "Dialog dismissed: item = " + selectedItemPosition);
        if (selectedItemPosition < 0 && getActivity() instanceof OnDismissListener) ((OnDismissListener) getActivity()).onDismiss(dialog);
    }

}
