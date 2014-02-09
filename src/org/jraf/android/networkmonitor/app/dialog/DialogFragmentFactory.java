/**
 * Copyright 2013 Carmen Alvarez
 *
 * This file is part of Scrum Chatter.
 *
 * Scrum Chatter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Scrum Chatter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Scrum Chatter. If not, see <http://www.gnu.org/licenses/>.
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


}
