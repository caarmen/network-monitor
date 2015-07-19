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
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Comparator;

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

    /**
     * @return a Dialog to browse files and folders
     */
    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.v(TAG, "onCreateDialog: savedInstanceState = " + savedInstanceState);

        Bundle arguments = getArguments();
        final int actionId = arguments.getInt(DialogFragmentFactory.EXTRA_ACTION_ID);
        File initialFolder = (File) arguments.getSerializable(FileChooserDialogFragment.EXTRA_FILE_CHOOSER_INITIAL_FOLDER);
        boolean foldersOnly = arguments.getBoolean(FileChooserDialogFragment.EXTRA_FILE_CHOOSER_FOLDERS_ONLY);

        if (initialFolder == null || !initialFolder.isDirectory()) {
            initialFolder = Environment.getExternalStorageDirectory();
        }

        final Context context = getActivity();
        final FileAdapter adapter = new FileAdapter(context, initialFolder, foldersOnly);

         // Save the file the user selected.
        OnClickListener fileSelectionListener = new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mSelectedFile = adapter.getItem(i);
                if (mSelectedFile.isDirectory()) {
                    adapter.load(mSelectedFile);
                    AlertDialog dialog = (AlertDialog) dialogInterface;
                    dialog.setTitle(getDisplayName(context, mSelectedFile));
                    dialog.getListView().clearChoices();
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
                .setTitle(getDisplayName(context, initialFolder))
                .setSingleChoiceItems(adapter, -1, fileSelectionListener)
                .setPositiveButton(android.R.string.ok, positiveListener)
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
        if(listener != null) listener.onDismiss(actionId);
    }

    private static final String getDisplayName(Context context, File file) {
        if(file.getAbsolutePath().equals(Environment.getExternalStorageDirectory().getAbsolutePath()))
            return context.getString(R.string.file_chooser_sdcard);
        else if(TextUtils.isEmpty(file.getName()))
            return context.getString(R.string.file_chooser_root);
        else
            return file.getName();
    }

    private static class FileAdapter extends ArrayAdapter<File> {
        private final FileFilter mFileFilter;
        private final FileComparator mFileComparator = new FileComparator();
        private File mSelectedFolder;

        public FileAdapter(Context context, File initialFolder, boolean foldersOnly) {
            super(context, R.layout.select_dialog_item_material);
            mFileFilter = new MyFileFilter(foldersOnly);
            load(initialFolder);
        }

        void load(File selectedFolder) {
            Log.v(TAG, "load " + selectedFolder);
            mSelectedFolder = selectedFolder;
            clear();
            File[] files = selectedFolder.listFiles(mFileFilter);
            Arrays.sort(files, mFileComparator);
            if (selectedFolder.getParentFile() != null) {
                add(selectedFolder.getParentFile());
            }
            for (File file : files) {
                add(file);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View result = super.getView(position, convertView, parent);
            TextView label = (TextView) result.findViewById(android.R.id.text1);
            File file = getItem(position);
            final int iconId;
            if (position == 0 && mSelectedFolder.getParentFile() != null) {
                label.setText("(" + getDisplayName(getContext(), file) + ")");
                iconId = R.drawable.ic_action_navigation_arrow_back;
                label.setTypeface(null, Typeface.ITALIC);
            } else {
                label.setText(getDisplayName(getContext(), file));
                label.setTypeface(null, Typeface.NORMAL);
                if(file.isDirectory()) {
                    iconId = R.drawable.ic_folder;
                } else {
                    iconId = 0;
                }
            }
            label.setCompoundDrawablesWithIntrinsicBounds(iconId, 0, 0, 0);
            return result;
        }
    }

    private static class MyFileFilter implements FileFilter {
        private final boolean mFoldersOnly;

        private MyFileFilter(boolean foldersOnly) {
            mFoldersOnly = foldersOnly;
        }

        @Override
        public boolean accept(File file) {
            if(file.isDirectory()) return true;
            if(file.isFile() && !mFoldersOnly) return true;
            return false;
        }
    }

    private static class FileComparator implements Comparator<File> {

        @Override
        public int compare(File file1, File file2) {
            if (file1.getParent() == null && file2.getParent() != null)
                return 1;
            if (file1.getParent() != null && file2.getParent() == null)
                return -1;
            if (file1.isDirectory() && !file2.isDirectory())
                return 1;
            if (!file1.isDirectory() && file2.isDirectory())
                return -1;
            return file1.getName().compareTo(file2.getName());
        }
    }
}
