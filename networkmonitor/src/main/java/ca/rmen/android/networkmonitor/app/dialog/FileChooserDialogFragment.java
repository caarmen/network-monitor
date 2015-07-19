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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import java.io.File;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.util.Log;

/**
 * Dialog to pick a file (or folder)
 */
public class FileChooserDialogFragment extends DialogFragment {

    private static final String TAG = Constants.TAG + FileChooserDialogFragment.class.getSimpleName();

    /**
     * Optional.  Must be a folder. If provided, the file browser will open at this folder.
     */
    public static final String EXTRA_FILE_CHOOSER_INITIAL_FOLDER = "initial_folder";

    /**
     * Optional. If true, only folders will appear in the chooser.
     */
    public static final String EXTRA_FILE_CHOOSER_FOLDERS_ONLY = "folders_only";

    private File mSelectedFile = null;

    /**
     * The calling activity must implement the {@link ca.rmen.android.networkmonitor.app.dialog.FileChooserDialogFragment.FileChooserDialogListener} interface.
     */
    public interface FileChooserDialogListener {
        void onFileSelected(int actionId, File file);

        void onDismiss(int actionId);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(EXTRA_FILE_CHOOSER_INITIAL_FOLDER, mSelectedFile);
        super.onSaveInstanceState(outState);
        Log.v(TAG, "onSavedInstanceState, outState=" + outState);
    }

    /**
     * Returns the initial folder to open in the file chooser dialog.
     * First we look in the savedInstanceState, if any, to see if the user selected a folder before
     * rotating the screen.
     * Then we look in the arguments given when creating this dialog.
     * Then we fall back to the SD card folder.
     */
    private File getInitialFolder(Bundle savedInstanceState) {
        File initialFolder = null;

        if (savedInstanceState != null) {
            initialFolder = (File) savedInstanceState.getSerializable(EXTRA_FILE_CHOOSER_INITIAL_FOLDER);
        }

        if (initialFolder == null) {
            initialFolder = (File) getArguments().getSerializable(EXTRA_FILE_CHOOSER_INITIAL_FOLDER);
        }

        if (initialFolder == null) {
            initialFolder = Environment.getExternalStorageDirectory();
        }

        // We need a folder to start with.
        if (!initialFolder.isDirectory()) {
            initialFolder = initialFolder.getParentFile();
        }
        return initialFolder;
    }

    /**
     * @return a Dialog to browse files and folders
     */
    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.v(TAG, "onCreateDialog: savedInstanceState = " + savedInstanceState);

        Bundle arguments = getArguments();
        final int actionId = arguments.getInt(DialogFragmentFactory.EXTRA_ACTION_ID);
        boolean foldersOnly = arguments.getBoolean(FileChooserDialogFragment.EXTRA_FILE_CHOOSER_FOLDERS_ONLY);

        mSelectedFile = getInitialFolder(savedInstanceState);

        final Context context = getActivity();
        final FileAdapter adapter = new FileAdapter(context, mSelectedFile, foldersOnly);

        // Save the file the user selected.
        OnClickListener fileSelectionListener = new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mSelectedFile = adapter.getItem(i);
                if (mSelectedFile.isDirectory()) {
                    adapter.load(mSelectedFile);
                    AlertDialog dialog = (AlertDialog) dialogInterface;
                    dialog.setTitle(FileAdapter.getFullDisplayName(context, mSelectedFile));
                    dialog.getListView().clearChoices();
                    dialog.getListView().setSelectionAfterHeaderView();
                }
            }
        };

        // When the user taps the positive button, notify the listener.
        OnClickListener positiveListener = new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                FileChooserDialogListener activity = (FileChooserDialogListener) getActivity();
                if (activity == null)
                    Log.w(TAG, "User clicked on dialog after it was detached from activity. Monkey?");
                else
                    activity.onFileSelected(actionId, mSelectedFile);
            }
        };

        // Dismiss/cancel callbacks.
        OnClickListener negativeListener = new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ((FileChooserDialogListener) getActivity()).onDismiss(actionId);
            }
        };
        OnCancelListener cancelListener = new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                ((FileChooserDialogListener) getActivity()).onDismiss(actionId);
            }
        };
        OnDismissListener dismissListener = new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                ((FileChooserDialogListener) getActivity()).onDismiss(actionId);
            }
        };
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(FileAdapter.getFullDisplayName(context, mSelectedFile))
                .setSingleChoiceItems(adapter, -1, fileSelectionListener)
                .setPositiveButton(R.string.file_chooser_choose, positiveListener)
                .setNegativeButton(android.R.string.cancel, negativeListener)
                .setOnCancelListener(cancelListener)
                .create();
        dialog.setOnDismissListener(dismissListener);
        return dialog;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        Log.v(TAG, "onDismiss");
        super.onDismiss(dialog);
        Bundle arguments = getArguments();
        int actionId = arguments.getInt(DialogFragmentFactory.EXTRA_ACTION_ID);
        FileChooserDialogListener listener = (FileChooserDialogListener) getActivity();
        if (listener != null) listener.onDismiss(actionId);
    }
}
