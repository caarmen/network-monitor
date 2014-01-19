package org.jraf.android.networkmonitor.app.dialog;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.R;
import org.jraf.android.networkmonitor.util.Log;

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

/**
 * A ProgressDialog with a message.
 */
public class ProgressDialogFragment extends DialogFragment { // NO_UCD (use private)

    private static final String TAG = Constants.TAG + ProgressDialogFragment.class.getSimpleName();

    public static final String EXTRA_PROGRESS_DIALOG_STYLE = "progress_dialog_style";
    public static final String EXTRA_PROGRESS_DIALOG_MESSAGE = "progress_dialog_message";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setMessage(getArguments().getString(EXTRA_PROGRESS_DIALOG_MESSAGE));
        dialog.setIndeterminate(true);
        dialog.setProgressStyle(getArguments().getInt(EXTRA_PROGRESS_DIALOG_STYLE));
        return dialog;
    }

    public void setProgress(int progress, int max) {
        Log.v(TAG, "setProgress " + progress + "/" + max);
        ProgressDialog dialog = (ProgressDialog) getDialog();
        // The dialog may have been dismissed: probably a monkey race condition
        if (dialog == null) return;
        if (progress >= max) {
            dialog.setMax(100);
            dialog.setProgress(0);
            dialog.setIndeterminate(true);
            dialog.setMessage(getActivity().getString(R.string.export_progress_finalizing_export));
        } else {
            dialog.setIndeterminate(false);
            dialog.setMax(max);
            dialog.setProgress(progress);
            dialog.setMessage(getActivity().getString(R.string.export_progress_processing_data));
        }
    }

}
