/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014 Benoit 'BoD' Lubek (BoD@JRAF.org) //TODO <- replace with *your* name/email
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
package org.jraf.android.networkmonitor.app.prefs;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.jraf.android.networkmonitor.R;
import org.jraf.android.networkmonitor.app.prefs.FilterPreferences.Selection;
import org.jraf.android.networkmonitor.provider.NetMonColumns;
import org.jraf.android.networkmonitor.util.Log;

public class FilterColumnListFragment extends ListFragment {
    private static final String TAG = FilterColumnListFragment.class.getSimpleName();
    private static final int URL_LOADER = 0;

    private static String mColumnName;

    public static class FilterListItem {
        final String value;
        final String label;

        public FilterListItem(String value, String label) {
            this.value = value;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        Log.v(TAG, "onAttach");
        super.onAttach(activity);
        mColumnName = activity.getIntent().getExtras().getString(FilterColumnActivity.EXTRA_COLUMN_NAME);
        getLoaderManager().initLoader(URL_LOADER, null, mLoaderCallbacks);

    }

    private LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
            Log.v(TAG, "onCreateLoader, loaderId = " + loaderId + ", bundle = " + bundle);
            String[] projection = new String[] { NetMonColumns.UNIQUE_VALUES_VALUE, NetMonColumns.UNIQUE_VALUES_COUNT };
            Selection selection = FilterPreferences.getSelectionClause(getActivity(), mColumnName);
            CursorLoader loader = new CursorLoader(getActivity(), Uri.withAppendedPath(NetMonColumns.UNIQUE_VALUES_URI, mColumnName), projection,
                    selection.selection, selection.selectionArgs, null);
            return loader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            Log.v(TAG, "onLoadFinished, loader = " + loader + ", cursor = " + cursor);
            Context context = getActivity();
            if (context == null) return;

            FilterListItem[] values = new FilterListItem[cursor.getCount()];
            int i = 0;
            while (cursor.moveToNext()) {
                String value = cursor.getString(0);
                long count = cursor.getLong(1);
                String displayValue = value;
                if (TextUtils.isEmpty(value)) {
                    value = FilterPreferences.EMPTY;
                    displayValue = getString(R.string.filter_columns_empty_value);
                }
                String label = displayValue + " (" + count + ")";
                values[i++] = new FilterListItem(value, label);
            }

            ArrayAdapter<FilterListItem> adapter = new ArrayAdapter<FilterListItem>(context, android.R.layout.simple_list_item_multiple_choice, values);
            setListAdapter(adapter);
            ListView lv = getListView();
            lv.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
            // Preselect the columns from the preferences
            List<String> selectedColumns = NetMonPreferences.getInstance(context).getColumnFilterValues(mColumnName);
            for (i = 0; i < lv.getCount(); i++) {
                FilterListItem item = adapter.getItem(i);
                if (selectedColumns.contains(item.value)) lv.setItemChecked(i, true);
            }

            setListShown(true);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            Log.v(TAG, "onLoaderReset " + loader);
        }
    };



}
