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

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;

import org.jraf.android.networkmonitor.R;
import org.jraf.android.networkmonitor.provider.NetMonColumns;

public class SelectFieldsActivity extends FragmentActivity {
    private static final String TAG = SelectFieldsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_fields);
    }

    public void onCancel(View v) {
        Log.v(TAG, "onCancel");
        NavUtils.navigateUpFromSameTask(this);
    }

    public void onOk(View v) {
        Log.v(TAG, "onOk");
        ListFragment lv = (ListFragment) getSupportFragmentManager().findFragmentById(R.id.listFragment);
        SparseBooleanArray checkedPositions = lv.getListView().getCheckedItemPositions();
        String[] dbColumns = NetMonColumns.getColumnNames(this);
        final List<String> selectedColumns = new ArrayList<String>(dbColumns.length);
        for (int i = 0; i < dbColumns.length; i++) {
            if (checkedPositions.get(i)) selectedColumns.add(dbColumns[i]);
        }
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                NetMonPreferences.getInstance(SelectFieldsActivity.this).setSelectedColumns(selectedColumns);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                NavUtils.navigateUpFromSameTask(SelectFieldsActivity.this);
            }
        }.execute();
    }
}
