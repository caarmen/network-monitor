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
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.widget.TextView;

import ca.rmen.android.networkmonitor.Constants;

/**
 * Shows a dialog with a title, message, and a single button to dismiss the dialog.
 */
public class InfoDialogFragment extends DialogFragment { // NO_UCD (use default)

    private static final String TAG = Constants.TAG + InfoDialogFragment.class.getSimpleName();

    public InfoDialogFragment() {
        super();
    }

    /**
     * @return a Dialog with a title, message, and single button to dismiss the dialog.
     */
    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.v(TAG, "onCreateDialog: savedInstanceState = " + savedInstanceState);
        Context context = getActivity();
        Bundle arguments = getArguments();
        if (context == null || arguments == null) return super.onCreateDialog(savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final int iconId = arguments.getInt(DialogFragmentFactory.EXTRA_ICON_ID);
        if (iconId > 0) builder.setIcon(iconId);
        CharSequence message = arguments.getString(DialogFragmentFactory.EXTRA_MESSAGE);
        SpannableString s = new SpannableString(message);
        Linkify.addLinks(s, Linkify.WEB_URLS);
        builder.setTitle(arguments.getString(DialogFragmentFactory.EXTRA_TITLE)).setMessage(s);
        builder.setPositiveButton(android.R.string.ok, null);
        if (getActivity() instanceof OnCancelListener) builder.setOnCancelListener((OnCancelListener) getActivity());
        final Dialog dialog = builder.create();
        if (getActivity() instanceof OnDismissListener) dialog.setOnDismissListener((OnDismissListener) getActivity());
        dialog.setOnShowListener(dialogInterface -> {
            TextView messageTextView = ((Dialog) dialogInterface).findViewById(android.R.id.message);
            if (messageTextView != null)
                messageTextView.setMovementMethod(LinkMovementMethod.getInstance());
        });
        return dialog;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        Log.v(TAG, "onDismiss");
        super.onDismiss(dialog);
        if (getActivity() instanceof OnDismissListener) ((OnDismissListener) getActivity()).onDismiss(dialog);
    }
}
