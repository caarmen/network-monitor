/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014 Carmen Alvarez (c@rmen.ca)
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

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import org.jraf.android.networkmonitor.R;
import org.jraf.android.networkmonitor.util.Log;

public class FilterColumnActivity extends FragmentActivity { // NO_UCD (use default)
    private static final String TAG = FilterColumnActivity.class.getSimpleName();
    public static final String EXTRA_COLUMN_NAME = "column_name";
    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filter_columns);
        ListFragment lvf = (ListFragment) getSupportFragmentManager().findFragmentById(R.id.listFragment);
        mListView = lvf.getListView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.filter_columns, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_select_all:
                for (int i = 0; i < mListView.getCount(); i++)
                    mListView.setItemChecked(i, true);
                break;
            case R.id.action_select_none:
                for (int i = 0; i < mListView.getCount(); i++)
                    mListView.setItemChecked(i, false);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public void onCancel(View v) { // NO_UCD (unused code)
        Log.v(TAG, "onCancel");
        finish();
    }

    public void onOk(View v) { // NO_UCD (unused code)
        Log.v(TAG, "onOk");
        SparseBooleanArray checkedPositions = mListView.getCheckedItemPositions();
        final List<String> selectedValues = new ArrayList<String>(mListView.getCount());
        for (int i = 0; i < mListView.getCount(); i++) {
            if (checkedPositions.get(i)) selectedValues.add((String) mListView.getAdapter().getItem(i));
        }
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                String columnName = getIntent().getExtras().getString(EXTRA_COLUMN_NAME);
                NetMonPreferences.getInstance(FilterColumnActivity.this).setColumnFilterValues(columnName, selectedValues);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                setResult(Activity.RESULT_OK);
                finish();
            }
        }.execute();
    }
}
