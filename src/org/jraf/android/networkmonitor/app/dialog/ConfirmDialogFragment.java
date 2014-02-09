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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import org.jraf.android.networkmonitor.Constants;

/**
 * A dialog fragment with a title, message, ok and cancel buttons.
 */
public class ConfirmDialogFragment extends DialogFragment { // NO_UCD (use default)

    private static final String TAG = Constants.TAG + "/" + ConfirmDialogFragment.class.getSimpleName();

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
     * @return an AlertDialog with a title, message, ok, and cancel buttons.
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.v(TAG, "onCreateDialog: savedInstanceState = " + savedInstanceState);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        Bundle arguments = getArguments();
        builder.setTitle(arguments.getString(DialogFragmentFactory.EXTRA_TITLE)).setMessage(arguments.getString(DialogFragmentFactory.EXTRA_MESSAGE));
        final int actionId = arguments.getInt(DialogFragmentFactory.EXTRA_ACTION_ID);
        final Bundle extras = arguments.getBundle(DialogFragmentFactory.EXTRA_EXTRAS);
        OnClickListener positiveListener = null;
        OnClickListener negativeListener = null;
        if (getActivity() instanceof DialogButtonListener) {
            positiveListener = new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    FragmentActivity activity = getActivity();
                    if (activity == null) Log.w(TAG, "User clicked on dialog after it was detached from activity. Monkey?");
                    else
                        ((DialogButtonListener) activity).onOkClicked(actionId, extras);
                }
            };
            negativeListener = new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    FragmentActivity activity = getActivity();
                    if (activity == null) Log.w(TAG, "User clicked on dialog after it was detached from activity. Monkey?");
                    else
                        ((DialogButtonListener) activity).onCancelClicked(actionId, extras);
                }
            };
        }
        builder.setNegativeButton(android.R.string.cancel, negativeListener);
        builder.setPositiveButton(android.R.string.ok, positiveListener);
        final AlertDialog dialog = builder.create();
        return dialog;

    }
}
