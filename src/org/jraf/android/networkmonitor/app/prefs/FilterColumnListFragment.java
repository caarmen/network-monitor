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
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.jraf.android.networkmonitor.provider.NetMonColumns;
import org.jraf.android.networkmonitor.util.Log;

public class FilterColumnListFragment extends ListFragment {
    private static final String TAG = FilterColumnListFragment.class.getSimpleName();
    private static final int URL_LOADER = 0;

    private static String mColumnName;

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
            CursorLoader loader = new CursorLoader(getActivity(), Uri.withAppendedPath(NetMonColumns.UNIQUE_VALUES_URI, mColumnName), null, null, null, null);
            return loader;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            Log.v(TAG, "onLoadFinished, loader = " + loader + ", cursor = " + cursor);
            Context context = getActivity();
            if (context == null) return;

            String[] values = new String[cursor.getCount()];
            int i = 0;
            while (cursor.moveToNext())
                values[i++] = cursor.getString(0);

            ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(context, android.R.layout.simple_list_item_multiple_choice, values);
            setListAdapter(adapter);
            ListView lv = getListView();
            lv.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
            // Preselect the columns from the preferences
            List<String> selectedColumns = NetMonPreferences.getInstance(context).getColumnFilterValues(mColumnName);
            for (String selectedColumn : selectedColumns) {
                int position = adapter.getPosition(selectedColumn);
                lv.setItemChecked(position, true);
            }
            setListShown(true);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            Log.v(TAG, "onLoaderReset " + loader);
        }
    };



}
