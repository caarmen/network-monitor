/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
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
package org.jraf.android.networkmonitor.app.dialog;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.app.dialog.ConfirmDialogFragment.DialogButtonListener;

/**
 * Create different types of dialog fragments (edit text input, information, choice, confirmation, progress).
 * The dialogs created by this class are not only created but also shown in the activity given to the creation methods.
 * 
 * This is a subset of the same DialogFragmentFactory class from the scrum chatter project.
 */
public class DialogFragmentFactory extends DialogFragment {

    private static final String TAG = Constants.TAG + "/" + DialogFragmentFactory.class.getSimpleName();
    static final String EXTRA_TITLE = "title";
    static final String EXTRA_MESSAGE = "message";
    static final String EXTRA_ACTION_ID = "action_id";
    static final String EXTRA_EXTRAS = "extras";
    static final String EXTRA_PROGRESS_DIALOG_STYLE = "progress_dialog_style";


    /**
     * @return a visible dialog fragment with the given title and message, and an ok and cancel button. If the given activity implements
     *         {@link DialogButtonListener}, the actionId and extras parameter will be provided in
     *         the {@link DialogButtonListener#onOkClicked(int, Bundle)} callback on the activity, when the user clicks on the ok button.
     */
    public static ConfirmDialogFragment showConfirmDialog(FragmentActivity activity, String title, String message, int actionId, Bundle extras) {
        Log.v(TAG, "showConfirmDialog: title = " + title + ", message = " + message + ", actionId = " + actionId + ", extras = " + extras);
        ConfirmDialogFragment result = new ConfirmDialogFragment();
        Bundle arguments = new Bundle(4);
        arguments.putString(EXTRA_TITLE, title);
        arguments.putString(EXTRA_MESSAGE, message);
        arguments.putInt(EXTRA_ACTION_ID, actionId);
        if (extras != null) arguments.putBundle(EXTRA_EXTRAS, extras);
        result.setArguments(arguments);
        result.show(activity.getSupportFragmentManager(), ConfirmDialogFragment.class.getSimpleName());
        return result;
    }

    /**
     * @return a visible dialog fragment with the given message.
     * @param tag should be used by the calling activity, when the background task is complete, to find the fragment and dismiss it.
     */
    public static ProgressDialogFragment showProgressDialog(FragmentActivity activity, String message, int progressDialogStyle, String tag) {
        Log.v(TAG, "showProgressDialog: message = " + message);
        Bundle arguments = new Bundle(2);
        arguments.putString(EXTRA_MESSAGE, message);
        arguments.putInt(EXTRA_PROGRESS_DIALOG_STYLE, progressDialogStyle);
        ProgressDialogFragment result = new ProgressDialogFragment();
        result.setArguments(arguments);
        result.setCancelable(false);
        result.show(activity.getSupportFragmentManager(), tag);
        return result;
    }

}
