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
package ca.rmen.android.networkmonitor.app.prefs;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import ca.rmen.android.networkmonitor.provider.NetMonColumns;

public class SelectFieldsFragment extends ListFragment {

    interface SelectFieldsFragmentListener {
        void onListItemClick(ListView l, int position);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Activity activity = getActivity();
        FieldsAdapter adapter = FieldsAdapter.newInstance(activity);
        setListAdapter(adapter);
        ListView lv = getListView();
        lv.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        // Preselect the columns from the preferences
        List<String> selectedColumns = NetMonPreferences.getInstance(activity).getSelectedColumns();
        for (String selectedColumn : selectedColumns) {
            int position = adapter.getPositionForColumnName(selectedColumn);
            lv.setItemChecked(position, true);
        }
    }

    static class SelectedField {
        final String dbName;
        final String label;

        // Build the list of choices for the user.  Look up the friendly label of each column name, and pre-select the one the user chose last time.
        SelectedField(String dbName, String label) {
            this.dbName = dbName;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    static class FieldsAdapter extends ArrayAdapter<SelectedField> {

        static FieldsAdapter newInstance(Context context) {
            String[] dbColumns = NetMonColumns.getColumnNames(context);
            String[] columnLabels = NetMonColumns.getColumnLabels(context);
            SelectedField[] fields = new SelectedField[dbColumns.length];
            for (int i=0; i < dbColumns.length; i++) {
                fields[i] = new SelectedField(dbColumns[i], columnLabels[i]);
            }
            return new FieldsAdapter(context, fields);
        }

        FieldsAdapter(Context context, SelectedField[] selectedFields) {
            super(context, android.R.layout.simple_list_item_multiple_choice, selectedFields);
        }

        int getPositionForColumnName(String columnName) {
            for (int i = 0; i < getCount(); i++) {
                SelectedField selectedField = getItem(i);
                if (selectedField != null && selectedField.dbName.equals(columnName)) {
                    return i;
                }
            }
            return -1;
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Activity activity = getActivity();
        if (activity instanceof SelectFieldsFragmentListener) ((SelectFieldsFragmentListener) activity).onListItemClick(l, position);
    }

}
