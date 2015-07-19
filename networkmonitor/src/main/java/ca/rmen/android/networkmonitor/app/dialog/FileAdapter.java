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

import android.content.Context;
import android.graphics.Typeface;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Comparator;

import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.util.Log;


class FileAdapter extends ArrayAdapter<File> {
    private static final String TAG = FileAdapter.class.getSimpleName();

    private final FileFilter mFileFilter;
    private final FileComparator mFileComparator = new FileComparator();
    private File mSelectedFolder;

    FileAdapter(Context context, File initialFolder, boolean foldersOnly) {
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
            if (file.isDirectory()) {
                iconId = R.drawable.ic_folder;
            } else {
                iconId = 0;
            }
        }
        label.setCompoundDrawablesWithIntrinsicBounds(iconId, 0, 0, 0);
        return result;
    }

    static final String getDisplayName(Context context, File file) {
        if (file.getAbsolutePath().equals(Environment.getExternalStorageDirectory().getAbsolutePath()))
            return context.getString(R.string.file_chooser_sdcard);
        else if (TextUtils.isEmpty(file.getName()))
            return context.getString(R.string.file_chooser_root);
        else
            return file.getName();
    }

    private static class MyFileFilter implements FileFilter {
        private final boolean mFoldersOnly;

        private MyFileFilter(boolean foldersOnly) {
            mFoldersOnly = foldersOnly;
        }

        @Override
        public boolean accept(File file) {
            if (file.isDirectory()) return true;
            if (file.isFile() && !mFoldersOnly) return true;
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
