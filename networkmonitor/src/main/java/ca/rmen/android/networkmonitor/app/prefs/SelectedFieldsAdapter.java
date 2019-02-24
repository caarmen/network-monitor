/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2016-2019 Carmen Alvarez (c@rmen.ca)
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

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dialog.DialogFragmentFactory;
import ca.rmen.android.networkmonitor.databinding.FieldItemBinding;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;

class SelectedFieldsAdapter extends RecyclerView.Adapter<SelectedFieldsAdapter.SelectedFieldHolder> {

    private static class SelectedField {
        @NonNull
        final String dbName;
        @NonNull
        final String label;
        @Nullable
        final String tip;

        // Build the list of choices for the user.  Look up the friendly label of each column name, and pre-select the one the user chose last time.
        SelectedField(@NonNull String dbName, @NonNull String label, @Nullable String tip) {
            this.dbName = dbName;
            this.label = label;
            this.tip = tip;
        }

        @Override
        @NonNull
        public String toString() {
            return label;
        }
    }

    private final FragmentActivity mActivity;
    private final SelectedField[] mSelectedFields;
    private final Set<String> mCheckedItems = new HashSet<>();

    SelectedFieldsAdapter(FragmentActivity activity) {
        mActivity = activity;
        String[] dbColumns = NetMonColumns.getColumnNames(activity);
        String[] columnLabels = NetMonColumns.getColumnLabels(activity);
        mSelectedFields = new SelectedField[dbColumns.length];
        for (int i = 0; i < dbColumns.length; i++) {
            int tipId = activity.getResources().getIdentifier(dbColumns[i] + "_help", "string", activity.getPackageName());
            String tip = tipId > 0 ? activity.getString(tipId) : null;
            mSelectedFields[i] = new SelectedField(dbColumns[i], columnLabels[i], tip);
        }
        // Preselect the columns from the preferences
        List<String> selectedColumns = NetMonPreferences.getInstance(activity).getSelectedColumns();
        mCheckedItems.addAll(selectedColumns);
    }

    void selectAll() {
        for (SelectedField field : mSelectedFields) mCheckedItems.add(field.dbName);
        notifyDataSetChanged();
    }

    void selectNone() {
        mCheckedItems.clear();
        notifyDataSetChanged();
    }

    void selectColumns(String[] dbColumns) {
        mCheckedItems.clear();
        Collections.addAll(mCheckedItems, dbColumns);
        notifyDataSetChanged();
    }

    List<String> getSelectedColumns() {
        List<String> result = new ArrayList<>();
        for (SelectedField field : mSelectedFields) {
            if (mCheckedItems.contains(field.dbName)) {
                result.add(field.dbName);
            }
        }
        return result;
    }

    @Override
    public void onBindViewHolder(@NonNull SelectedFieldHolder holder, int position) {
        final SelectedField selectedField = mSelectedFields[position];
        holder.binding.fieldName.setText(selectedField.label);
        holder.binding.fieldHelp.setVisibility(TextUtils.isEmpty(selectedField.tip) ? View.GONE : View.VISIBLE);
        holder.binding.checkbox.setOnCheckedChangeListener(null);
        holder.binding.checkbox.setChecked(mCheckedItems.contains(selectedField.dbName));
        holder.binding.checkbox.setOnCheckedChangeListener((compoundButton, checked) -> {
            if (checked) mCheckedItems.add(selectedField.dbName);
            else mCheckedItems.remove(selectedField.dbName);
            notifyDataSetChanged();
        });
        holder.binding.fieldHelp.setOnClickListener(view -> DialogFragmentFactory.showInfoDialog(mActivity, selectedField.label, selectedField.tip));
    }

    @Override
    @NonNull
    public SelectedFieldHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SelectedFieldHolder(DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.field_item, parent, false));
    }

    @Override
    public int getItemCount() {
        return mSelectedFields.length;
    }

    static class SelectedFieldHolder extends RecyclerView.ViewHolder {

        final FieldItemBinding binding;

        SelectedFieldHolder(FieldItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            binding.getRoot().setOnClickListener(v -> binding.checkbox.performClick());
        }
    }
}
