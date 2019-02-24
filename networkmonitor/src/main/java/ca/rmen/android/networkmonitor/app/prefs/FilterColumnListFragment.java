/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014-2019 Carmen Alvarez (c@rmen.ca)
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
package ca.rmen.android.networkmonitor.app.prefs;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.ListFragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.app.LoaderManager.LoaderCallbacks;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.prefs.FilterPreferences.Selection;
import ca.rmen.android.networkmonitor.provider.UniqueValuesColumns;

/**
 * A list of the unique values for a particular column.
 */
public class FilterColumnListFragment extends ListFragment {
    private static final String TAG = Constants.TAG + FilterColumnListFragment.class.getSimpleName();
    private static final int URL_LOADER = 0;

    private String mColumnName;

    /**
     * The list will contain FilterListItems.
     */
    static class FilterListItem {
        // The value to use for the query selection.
        @NonNull
        final String value;

        // The string to display for this item in the list.
        @NonNull
        private final String label;

        private FilterListItem(@NonNull String value, @NonNull String label) {
            this.value = value;
            this.label = label;
        }

        @NonNull
        @Override
        public String toString() {
            return label;
        }
    }

    @Override
    public void onAttach(Context context) {
        Log.v(TAG, "onAttach");
        super.onAttach(context);
        Activity activity = getActivity();
        if (activity != null) {
            mColumnName = activity.getIntent().getStringExtra(FilterColumnActivity.EXTRA_COLUMN_NAME);
            LoaderManager.getInstance(this).initLoader(URL_LOADER, null, mLoaderCallbacks);
        }

    }

    private final LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderCallbacks<Cursor>() {
        @Override
        @NonNull
        public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {
            Log.v(TAG, "onCreateLoader, loaderId = " + loaderId + ", bundle = " + bundle);
            Activity activity = requireActivity();
            String[] projection = new String[] { UniqueValuesColumns.VALUE, UniqueValuesColumns.COUNT };
            // We only want to show values for this column that appear with all the filters for the other columns being used at the same time.
            // So we build a query with a selection applying the filters on all the other columns.
            Selection selection = FilterPreferences.getSelectionClause(activity, mColumnName);
            return new CursorLoader(activity, Uri.withAppendedPath(UniqueValuesColumns.CONTENT_URI, mColumnName), projection,
                    selection.selectionString, selection.selectionArgs, mColumnName + " ASC");
        }

        @Override
        public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
            Log.v(TAG, "onLoadFinished, loader = " + loader + ", cursor = " + cursor);
            Context context = getActivity();
            if (context == null) return;

            FilterListItem[] values = new FilterListItem[cursor.getCount()];
            int i = 0;
            while (cursor.moveToNext()) {
                String value = cursor.getString(0);
                long count = cursor.getLong(1);

                // The string we display in the list is the value, plus
                // the number of occurrences of this value (given all the other column filters).
                // For null or empty values, we display a special label.
                String displayValue = value;
                if (TextUtils.isEmpty(value)) {
                    value = FilterPreferences.EMPTY;
                    displayValue = getString(R.string.filter_columns_empty_value);
                }
                String label = displayValue + " (" + count + ")";
                values[i++] = new FilterListItem(value, label);
            }

            ArrayAdapter<FilterListItem> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_multiple_choice, values);
            setListAdapter(adapter);
            ListView lv = getListView();
            lv.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);

            // Preselect the filtered values from the preferences
            List<String> selectedColumns = NetMonPreferences.getInstance(context).getColumnFilterValues(mColumnName);
            for (i = 0; i < lv.getCount(); i++) {
                FilterListItem item = adapter.getItem(i);
                if (item != null && selectedColumns.contains(item.value)) lv.setItemChecked(i, true);
            }

            setListShown(true);
        }

        @Override
        public void onLoaderReset(@NonNull Loader<Cursor> loader) {
            Log.v(TAG, "onLoaderReset " + loader);
        }
    };
}
