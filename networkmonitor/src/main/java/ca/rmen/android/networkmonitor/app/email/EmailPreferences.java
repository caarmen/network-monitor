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
package ca.rmen.android.networkmonitor.app.email;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.util.HashSet;
import java.util.Set;

import androidx.annotation.NonNull;

/**
 * Convenience methods for getting/setting shared preferences.
 */
public class EmailPreferences {


    enum EmailSecurity {
        NONE, SSL, TLS
    }

    static class EmailConfig {
        final Set<String> reportFormats;
        final String server;
        final int port;
        final String user;
        final String password;
        final String recipients;
        final EmailSecurity security;

        private EmailConfig(Set<String> reportFormats, String server, int port, String user, String password, String recipients, EmailSecurity security) {
            this.reportFormats = reportFormats;
            this.server = server;
            this.port = port;
            this.user = user;
            this.password = password;
            this.recipients = recipients;
            this.security = security;
        }

        /**
         * @return true if we have enough info to attempt to send a mail.
         */
        boolean isValid() {
            return !TextUtils.isEmpty(server) && port > 0 && !TextUtils.isEmpty(user) && !TextUtils.isEmpty(password) && !TextUtils.isEmpty(recipients);
        }

        @Override
        @NonNull
        public String toString() {
            return EmailPreferences.class.getSimpleName() + " [reportFormats=" + reportFormats + ", server=" + server + ", port=" + port + ", user=" + user
                    + ", password=******, recipients=" + recipients + ", security=" + security + "]";
        }

    }

    static final String PREF_EMAIL_INTERVAL = "PREF_EMAIL_INTERVAL";
    static final String PREF_EMAIL_REPORT_FORMATS = "PREF_EMAIL_REPORT_FORMATS";
    static final String PREF_EMAIL_SERVER = "PREF_EMAIL_SERVER";
    static final String PREF_EMAIL_PORT = "PREF_EMAIL_PORT";
    static final String PREF_EMAIL_USER = "PREF_EMAIL_USER";
    static final String PREF_EMAIL_RECIPIENTS = "PREF_EMAIL_RECIPIENTS";
    static final String PREF_EMAIL_LAST_EMAIL_SENT = "PREF_EMAIL_LAST_EMAIL_SENT";
    private static final String PREF_EMAIL_PASSWORD = "PREF_EMAIL_PASSWORD";

    private static final String PREF_EMAIL_SECURITY = "PREF_EMAIL_SECURITY";
    private static final String PREF_EMAIL_PORT_DEFAULT = "587";

    private static EmailPreferences INSTANCE = null;
    private final SharedPreferences mSharedPrefs;

    static synchronized EmailPreferences getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new EmailPreferences(context);
        }
        return INSTANCE;
    }

    private EmailPreferences(Context context) {
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * @return the interval, in milliseconds, between e-mailing reports.
     */
    int getEmailReportInterval() {
        return getIntPreference(EmailPreferences.PREF_EMAIL_INTERVAL, "0") * 60 * 1000;
    }

    void setLastEmailSent(long when) {
        Editor editor = mSharedPrefs.edit();
        editor.putLong(EmailPreferences.PREF_EMAIL_LAST_EMAIL_SENT, when);
        editor.apply();
    }

    long getLastEmailSent() {
        return mSharedPrefs.getLong(PREF_EMAIL_LAST_EMAIL_SENT, 0);
    }

    EmailConfig getEmailConfig() {
        Set<String> reportFormats = mSharedPrefs.getStringSet(PREF_EMAIL_REPORT_FORMATS, new HashSet<>());
        String server = mSharedPrefs.getString(PREF_EMAIL_SERVER, "").trim();
        int port = getIntPreference(PREF_EMAIL_PORT, PREF_EMAIL_PORT_DEFAULT);
        String user = mSharedPrefs.getString(PREF_EMAIL_USER, "").trim();
        String password = mSharedPrefs.getString(PREF_EMAIL_PASSWORD, "").trim();
        String recipients = mSharedPrefs.getString(PREF_EMAIL_RECIPIENTS, "");
        EmailSecurity security = EmailSecurity.valueOf(mSharedPrefs.getString(PREF_EMAIL_SECURITY, EmailSecurity.NONE.name()));
        return new EmailConfig(reportFormats, server, port, user, password, recipients, security);
    }

    private int getIntPreference(String key, String defaultValue) {
        String valueStr = mSharedPrefs.getString(key, defaultValue);
        if (TextUtils.isEmpty(valueStr)) return 0;
        return Integer.valueOf(valueStr);
    }

}
