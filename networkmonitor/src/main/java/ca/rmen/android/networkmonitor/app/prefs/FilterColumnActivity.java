/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014-2015 Carmen Alvarez (c@rmen.ca)
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
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.fragment.app.ListFragment;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.prefs.FilterColumnListFragment.FilterListItem;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;

/**
 * Activity which lets the user choose which values for a particular column will appear in the report.
 */
public class FilterColumnActivity extends AppCompatActivity { // NO_UCD (use default)
    private static final String TAG = Constants.TAG + FilterColumnActivity.class.getSimpleName();
    public static final String EXTRA_COLUMN_NAME = "column_name";
    private ListView mListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filter_columns);
        ListFragment lvf = (ListFragment) getSupportFragmentManager().findFragmentById(R.id.listFragment);
        mListView = lvf.getListView();

        // Show a hint to the user, explaining what filtering this column does.
        TextView tvHint = findViewById(R.id.filter_columns_hint);
        String columnName = getIntent().getStringExtra(EXTRA_COLUMN_NAME);
        String columnLabel = NetMonColumns.getColumnLabel(this, columnName);
        String hintText = getString(R.string.filter_columns_hint, columnLabel);
        tvHint.setText(hintText);
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

    @SuppressWarnings("UnusedParameters")
    public void onCancel(View v) {
        Log.v(TAG, "onCancel");
        finish();
    }

    @SuppressWarnings("UnusedParameters")
    public void onOk(View v) {
        Log.v(TAG, "onOk");
        // Update the preference for values to filter, for this particular column.

        // Build a list of all the values the user selected.
        SparseBooleanArray checkedPositions = mListView.getCheckedItemPositions();
        final List<String> selectedValues = new ArrayList<>(mListView.getCount());
        for (int i = 0; i < mListView.getCount(); i++) {
            if (checkedPositions.get(i)) selectedValues.add(((FilterListItem) mListView.getAdapter().getItem(i)).value);
        }
        AsyncTask.execute(() -> {
            // Update the filter preference for this column.
            String columnName = getIntent().getStringExtra(EXTRA_COLUMN_NAME);
            NetMonPreferences.getInstance(FilterColumnActivity.this).setColumnFilterValues(columnName, selectedValues);
            runOnUiThread(() -> {
                setResult(Activity.RESULT_OK);
                finish();
            });
        });
    }
}
