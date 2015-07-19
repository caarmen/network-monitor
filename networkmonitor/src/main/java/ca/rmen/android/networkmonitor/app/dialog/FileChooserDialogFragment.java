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

    public static final String EXTRA_FILE_CHOOSER_INITIAL_FOLDER = "initial_folder";
    public static final String EXTRA_FILE_CHOOSER_FOLDERS_ONLY = "folders_only";

    private File mSelectedFile = null;

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
        Context context = getActivity();
        Bundle arguments = getArguments();
        final int actionId = arguments.getInt(DialogFragmentFactory.EXTRA_ACTION_ID);
        File initialFolder = (File) arguments.getSerializable(FileChooserDialogFragment.EXTRA_FILE_CHOOSER_INITIAL_FOLDER);
        boolean foldersOnly = arguments.getBoolean(FileChooserDialogFragment.EXTRA_FILE_CHOOSER_FOLDERS_ONLY);

        if (initialFolder == null) {
            initialFolder = Environment.getExternalStorageDirectory();
        }

        final FileAdapter adapter = new FileAdapter(context, initialFolder, foldersOnly);
        final OnClickListener fileSelectionListener = new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mSelectedFile = adapter.getItem(i);
                if (mSelectedFile.isDirectory()) {
                    adapter.load(mSelectedFile);
                }
            }
        };
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
                .setTitle(arguments.getString(DialogFragmentFactory.EXTRA_TITLE))
                .setMessage(arguments.getString(DialogFragmentFactory.EXTRA_MESSAGE))
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
        if (getActivity() instanceof OnDismissListener)
            ((OnDismissListener) getActivity()).onDismiss(dialog);
    }

    private static class FileAdapter extends ArrayAdapter<File> {
        private final FileFilter mFileFilter;
        private final FileComparator mFileComparator = new FileComparator();

        public FileAdapter(Context context, File initialFolder, boolean foldersOnly) {
            super(context, R.layout.select_dialog_singlechoice_material);
            mFileFilter = new MyFileFilter(foldersOnly);
            load(initialFolder);
        }

        void load(File selectedFolder) {
            clear();
            File[] files = selectedFolder.listFiles(mFileFilter);
            Arrays.sort(files, mFileComparator);
            if (selectedFolder.getParent() != null) {
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
            if (position == 0 && file.getParentFile() != null) label.setText("..");
            else label.setText(file.getName());
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
            return !mFoldersOnly || file.isDirectory();
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
