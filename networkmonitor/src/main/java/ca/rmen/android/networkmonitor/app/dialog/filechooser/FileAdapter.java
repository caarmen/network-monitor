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
package ca.rmen.android.networkmonitor.app.dialog.filechooser;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
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
 * An adapter to display a list of files at a given folder.
 */
class FileAdapter extends ArrayAdapter<File> {
    private static final String TAG = Constants.TAG + FileAdapter.class.getSimpleName();

    private final FileFilter mFileFilter;
    private final LayoutInflater mInflater;
    private final FileComparator mFileComparator = new FileComparator();
    private File mSelectedFolder;

    FileAdapter(Context context, File initialFolder, boolean foldersOnly) {
        // Actually, the layout we provide here is ignored: we override
        // getView() and specify the layouts there.
        super(context, ca.rmen.android.networkmonitor.R.layout.select_dialog_singlechoice_material);
        mFileFilter = new MyFileFilter(foldersOnly);
        mInflater = LayoutInflater.from(getContext());
        load(initialFolder);
    }

    void load(File selectedFolder) {
        Log.v(TAG, "load " + selectedFolder);
        mSelectedFolder = selectedFolder;
        clear();
        File[] files = selectedFolder.listFiles(mFileFilter);
        if (selectedFolder.getParentFile() != null) {
            add(selectedFolder.getParentFile());
        }
        if(files != null) { // will be null if we don't have permission to read this folder
            Arrays.sort(files, mFileComparator);
            for (File file : files) {
                add(file);
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final TextView result;
        File file = getItem(position);
        if(convertView == null) {
            if(file.isDirectory()) {
                result = (TextView) mInflater.inflate(R.layout.select_dialog_item_material, parent, false);
            } else {
                result = (TextView) mInflater.inflate(ca.rmen.android.networkmonitor.R.layout.select_dialog_singlechoice_material, parent, false);
            }
        } else {
            result = (TextView) convertView;
        }
        // The first item is the parent directory (if there is one).
        if (position == 0 && mSelectedFolder.getParentFile() != null) {
            updateViewBackFolder(result, file);
        } else if(file.isDirectory()){
            updateViewFolder(result, file);
        } else {
            updateViewFile(result, file);
        }
        return result;
    }

    private void updateViewFile(TextView view, File file) {
        view.setText(FileChooser.getShortDisplayName(getContext(), file));
        view.setTypeface(null, Typeface.NORMAL);
        view.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_file, 0, 0, 0);
    }

    private void updateViewFolder(TextView view, File folder) {
        view.setText(FileChooser.getShortDisplayName(getContext(), folder));
        view.setTypeface(null, Typeface.NORMAL);
        view.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_folder, 0, 0, 0);
    }

    private void updateViewBackFolder(TextView view, File backFolder) {
        view.setText("(" + FileChooser.getShortDisplayName(getContext(), backFolder) + ")");
        view.setTypeface(null, Typeface.ITALIC);
        view.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_navigation_arrow_back, 0, 0, 0);
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        File file = getItem(position);
        if(file.isDirectory()) return 0;
        return 1;
    }

    private static class MyFileFilter implements FileFilter {
        private final boolean mFoldersOnly;

        private MyFileFilter(boolean foldersOnly) {
            mFoldersOnly = foldersOnly;
        }

        @Override
        public boolean accept(File file) {
            return file.isDirectory() || (file.isFile() && !mFoldersOnly);
        }
    }

    /**
     * Folders are first, then files.  Each are sorted alphabetically.
     */
    private static class FileComparator implements Comparator<File> {

        @Override
        public int compare(File file1, File file2) {
            if (file1.isDirectory() && !file2.isDirectory())
                return -1;
            if (!file1.isDirectory() && file2.isDirectory())
                return 1;
            return file1.getName().compareTo(file2.getName());
        }
    }
}
