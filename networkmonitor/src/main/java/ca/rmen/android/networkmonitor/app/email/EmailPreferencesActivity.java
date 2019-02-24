/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2013 Benoit 'BoD' Lubek (BoD@JRAF.org)
 * Copyright (C) 2013-2017 Carmen Alvarez (c@rmen.ca)
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
package ca.rmen.android.networkmonitor.app.email;

import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import androidx.preference.MultiSelectListPreference;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dialog.DialogFragmentFactory;
import ca.rmen.android.networkmonitor.app.email.EmailPreferences.EmailConfig;
import ca.rmen.android.networkmonitor.app.prefs.NetMonPreferenceFragmentCompat;

public class EmailPreferencesActivity extends AppCompatActivity {
    private static final String TAG = Constants.TAG + EmailPreferencesActivity.class.getSimpleName();

    private NetMonPreferenceFragmentCompat mPreferenceFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mPreferenceFragment = NetMonPreferenceFragmentCompat.newInstance(R.xml.email_preferences);
        getSupportFragmentManager().
                beginTransaction().
                replace(android.R.id.content, mPreferenceFragment).
                commit();
        getSupportFragmentManager().executePendingTransactions();
        updatePreferenceSummary(EmailPreferences.PREF_EMAIL_REPORT_FORMATS, R.string.pref_summary_email_report_formats);
        updatePreferenceSummary(EmailPreferences.PREF_EMAIL_RECIPIENTS, R.string.pref_summary_email_recipients);
        updatePreferenceSummary(EmailPreferences.PREF_EMAIL_SERVER, R.string.pref_summary_email_server);
        updatePreferenceSummary(EmailPreferences.PREF_EMAIL_PORT, R.string.pref_summary_email_port);
        updatePreferenceSummary(EmailPreferences.PREF_EMAIL_USER, R.string.pref_summary_email_user);
        updatePreferenceSummary(EmailPreferences.PREF_EMAIL_LAST_EMAIL_SENT, R.string.pref_summary_email_last_email_sent);
        mPreferenceFragment.findPreference(EmailPreferences.PREF_EMAIL_REPORT_FORMATS).setOnPreferenceChangeListener(mOnPreferenceChangeListener);
    }

    @Override
    protected void onStart() {
        Log.v(TAG, "onStart");
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
    }

    @Override
    public void onBackPressed() {
        Log.v(TAG, "onBackPressed");
        int emailInterval = EmailPreferences.getInstance(this).getEmailReportInterval();
        // If the user enabled sending e-mails, make sure we have enough info.
        if (emailInterval > 0) {
            EmailConfig emailConfig = EmailPreferences.getInstance(this).getEmailConfig();
            if (!emailConfig.isValid()) {
                ListPreference preference = (ListPreference) mPreferenceFragment.findPreference(EmailPreferences.PREF_EMAIL_INTERVAL);
                preference.setValue("0");
                DialogFragmentFactory.showInfoDialog(this, getString(R.string.missing_email_settings_info_dialog_title),
                        getString(R.string.missing_email_settings_info_dialog_message));
                updatePreferenceSummary(EmailPreferences.PREF_EMAIL_INTERVAL, R.string.pref_summary_email_report_interval);
                return;
            }
        }
        super.onBackPressed();
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop");
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(mOnSharedPreferenceChangeListener);
        super.onStop();
    }

    private final OnSharedPreferenceChangeListener mOnSharedPreferenceChangeListener = (sharedPreferences, key) -> {
        Log.v(TAG, "onSharedPreferenceChanged: key = " + key);
        if (EmailPreferences.PREF_EMAIL_RECIPIENTS.equals(key)) {
            updatePreferenceSummary(EmailPreferences.PREF_EMAIL_RECIPIENTS, R.string.pref_summary_email_recipients);
        } else if (EmailPreferences.PREF_EMAIL_REPORT_FORMATS.equals(key)) {
            updatePreferenceSummary(EmailPreferences.PREF_EMAIL_REPORT_FORMATS, R.string.pref_summary_email_report_formats);
        } else if (EmailPreferences.PREF_EMAIL_SERVER.equals(key)) {
            updatePreferenceSummary(EmailPreferences.PREF_EMAIL_SERVER, R.string.pref_summary_email_server);
        } else if (EmailPreferences.PREF_EMAIL_PORT.equals(key)) {
            updatePreferenceSummary(EmailPreferences.PREF_EMAIL_PORT, R.string.pref_summary_email_port);
        } else if (EmailPreferences.PREF_EMAIL_USER.equals(key)) {
            updatePreferenceSummary(EmailPreferences.PREF_EMAIL_USER, R.string.pref_summary_email_user);
        } else if (EmailPreferences.PREF_EMAIL_LAST_EMAIL_SENT.equals(key)) {
            updatePreferenceSummary(EmailPreferences.PREF_EMAIL_LAST_EMAIL_SENT, R.string.pref_summary_email_last_email_sent);
        }
    };

    /**
     * @return a String containing the user-friendly names of the values selected by the user.
     */
    private String getSummary(MultiSelectListPreference preference, Set<String> values) {
        List<CharSequence> result = new ArrayList<>();
        CharSequence[] entries = preference.getEntries();
        for (String value : values) {
            int index = preference.findIndexOfValue(value);
            result.add(entries[index]);
        }
        return TextUtils.join(", ", result);
    }

    private void updatePreferenceSummary(CharSequence key, int summaryResId) {
        Preference pref = mPreferenceFragment.findPreference(key);
        CharSequence value;
        if (key.equals(EmailPreferences.PREF_EMAIL_LAST_EMAIL_SENT)) {
            long lastEmailSent = EmailPreferences.getInstance(this).getLastEmailSent();
            if (lastEmailSent > 0)
                value = DateUtils.formatDateTime(this, lastEmailSent, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME
                        | DateUtils.FORMAT_SHOW_YEAR);
            else
                value = getString(R.string.pref_value_email_report_interval_never);
        } else if (pref instanceof EditTextPreference)
            value = ((EditTextPreference) pref).getText();
        else if (pref instanceof MultiSelectListPreference)
            value = getSummary((MultiSelectListPreference) pref, ((MultiSelectListPreference) pref).getValues());
        else if (pref instanceof ListPreference)
            value = ((ListPreference)pref).getEntry();
        else
            return;
        if (value == null) {
            value = "";
        }
        String summary = getString(summaryResId, value);
        pref.setSummary(summary);
    }

    /**
     * The OnSharedPreferenceChangeListener is not always called for the MultiSelectListPreference.
     * Because of this, we set a listener directly on the MultiSelectListPreference.
     * http://stackoverflow.com/questions/22388683/multiselectlistpreference-onsharedpreferencechanged-not-called-after-first-time
     */
    private final Preference.OnPreferenceChangeListener mOnPreferenceChangeListener = (preference, newValue) -> {
        Log.v(TAG, "onPreferenceChange: preference = " + preference + ", newValue = " + newValue);
        if (EmailPreferences.PREF_EMAIL_REPORT_FORMATS.equals(preference.getKey())) {
            @SuppressWarnings("unchecked")
            String valueStr = getSummary((MultiSelectListPreference) preference, (Set<String>) newValue);
            String summary = getString(R.string.pref_summary_email_report_formats, valueStr);
            preference.setSummary(summary);
        }
        return true;
    };

}
