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
 * Copyright (C) 2014 Carmen Alvarez (c@rmen.ca)
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
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.afollestad.materialdialogs.MaterialDialog;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.util.Log;


/**
 * A ProgressDialog with a message.
 */
public class ProgressDialogFragment extends DialogFragment { // NO_UCD (use private)

    private static final String TAG = Constants.TAG + ProgressDialogFragment.class.getSimpleName();


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.v(TAG, "onCreateDialog");
        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
        builder.content(getArguments().getString(DialogFragmentFactory.EXTRA_MESSAGE));
        builder.progress(false, 0, true);
        return builder.build();
    }

    public void setProgress(int progress, int max) {
        Log.v(TAG, "setProgress " + progress + "/" + max);
        MaterialDialog dialog = (MaterialDialog) getDialog();
        if (progress >= max) {
            dialog.setMaxProgress(100);
            dialog.setProgress(0);
            dialog.setContent(getActivity().getString(R.string.export_progress_finalizing_export));
        } else {
            dialog.setMaxProgress(max);
            dialog.setProgress(progress);
            dialog.setContent(getActivity().getString(R.string.export_progress_processing_data));
        }
    }

}
