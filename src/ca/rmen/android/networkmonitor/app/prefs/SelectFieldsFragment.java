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
package ca.rmen.android.networkmonitor.app.prefs;

import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import ca.rmen.android.networkmonitor.provider.NetMonColumns;

public class SelectFieldsFragment extends ListFragment {

    interface SelectFieldsFragmentListener {
        void onListItemClick(ListView l, View v, int position, long id);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Activity activity = getActivity();
        // Build the list of choices for the user.  Look up the friendly label of each column name, and pre-select the one the user chose last time.
        final String[] columnLabels = NetMonColumns.getColumnLabels(activity);
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(activity, android.R.layout.simple_list_item_multiple_choice, columnLabels);
        setListAdapter(adapter);
        ListView lv = getListView();
        lv.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        // Preselect the columns from the preferences
        List<String> selectedColumns = NetMonPreferences.getInstance(activity).getSelectedColumns();
        for (String selectedColumn : selectedColumns) {
            String selectedColumnLabel = NetMonColumns.getColumnLabel(activity, selectedColumn);
            int position = adapter.getPosition(selectedColumnLabel);
            lv.setItemChecked(position, true);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Activity activity = getActivity();
        if (activity instanceof SelectFieldsFragmentListener) ((SelectFieldsFragmentListener) activity).onListItemClick(l, v, position, id);
    }


}
