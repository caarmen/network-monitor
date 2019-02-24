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
package ca.rmen.android.networkmonitor.app.dbops.ui;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import android.view.View;
import android.widget.TextView;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dbops.backend.DBOpIntentService;
import ca.rmen.android.networkmonitor.app.prefs.FilterColumnActivity;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferenceFragmentCompat;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferences;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;
import android.util.Log;

public class GnuplotSettingsActivity extends AppCompatActivity {
    private static final String TAG = Constants.TAG + GnuplotSettingsActivity.class.getSimpleName();

    private static final String PREF_EXPORT_GNUPLOT_SERIES_FILTER = "PREF_EXPORT_GNUPLOT_SERIES_FILTER";
    private Preference mFilterPreference;
    private ListPreference mSeriesPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate");
        setContentView(R.layout.gnuplot_settings);
        NetMonPreferenceFragmentCompat prefFragment = NetMonPreferenceFragmentCompat.newInstance(R.xml.export_gnuplot_preferences);
        getSupportFragmentManager().
                beginTransaction().
                replace(R.id.preference_fragment, prefFragment).
                commit();
        getSupportFragmentManager().executePendingTransactions();
        ((TextView) findViewById(R.id.ok)).setText(R.string.export);
        mSeriesPref = (ListPreference) prefFragment.findPreference(NetMonPreferences.PREF_EXPORT_GNUPLOT_SERIES);
        ListPreference yAxisPref = (ListPreference) prefFragment.findPreference(NetMonPreferences.PREF_EXPORT_GNUPLOT_Y_AXIS);
        mFilterPreference = prefFragment.findPreference(PREF_EXPORT_GNUPLOT_SERIES_FILTER);
        mFilterPreference.setOnPreferenceClickListener(mOnPreferenceClickListener);
        mSeriesPref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);
        updateColumnLabels(mSeriesPref);
        updateColumnLabels(yAxisPref);
        updateFilterPreferenceTitle(mSeriesPref.getValue());
    }

    private void updateColumnLabels(ListPreference listPreference) {
        CharSequence[] entryValues = listPreference.getEntryValues();
        CharSequence[] entries = NetMonColumns.getColumnLabels(this, entryValues);
        listPreference.setEntries(entries);
    }

    private void updateFilterPreferenceTitle(String seriesColumnName) {
        String seriesColumnLabel = NetMonColumns.getColumnLabel(this, seriesColumnName);
        mFilterPreference.setTitle(getString(R.string.export_gnuplot_series_filter_title, seriesColumnLabel));
    }

    public void onOk(@SuppressWarnings("UnusedParameters") View view) {
        DBOpIntentService.startActionExport(this, DBOpIntentService.ExportFormat.GNUPLOT);
        finish();
    }

    public void onCancel(@SuppressWarnings("UnusedParameters") View view) {
        finish();
    }
    private final Preference.OnPreferenceClickListener mOnPreferenceClickListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            Intent intent = new Intent(GnuplotSettingsActivity.this, FilterColumnActivity.class);
            intent.putExtra(FilterColumnActivity.EXTRA_COLUMN_NAME, mSeriesPref.getValue());
            startActivity(intent);
            return true;
        }
    };

    private final Preference.OnPreferenceChangeListener mOnPreferenceChangeListener = (preference, newValue) -> {
        updateFilterPreferenceTitle((String) newValue);
        return true;
    };
}
