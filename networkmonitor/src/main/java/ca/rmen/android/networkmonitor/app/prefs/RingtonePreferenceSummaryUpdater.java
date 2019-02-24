/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2016 Carmen Alvarez (c@rmen.ca)
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

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.preference.Preference;

import ca.rmen.android.networkmonitor.R;

/**
 * RingtoneManager.getRingtone() actually does some disk reads.
 * Discovered this with StrictMode and monkey.
 * Ugly code (async task) to make it easier to find real StrictMode violations...
 */
class RingtonePreferenceSummaryUpdater extends AsyncTask<Preference, Void, RingtonePreferenceSummaryUpdater.Result> {

    static class Result {
        public final Preference preference;
        public final CharSequence summary;

        Result(Preference preference, CharSequence summary) {
            this.preference = preference;
            this.summary = summary;
        }
    }

    @Override
    protected Result doInBackground(Preference... preferences) {
        Preference preference = preferences[0];
        Context context = preference.getContext();
        Uri ringtoneUri = NetMonPreferences.getInstance(context).getNotificationSoundUri();
        if (ringtoneUri == null) {
            return new Result(preference, context.getString(R.string.pref_value_notification_ringtone_silent));
        } else {
            Ringtone ringtone = RingtoneManager.getRingtone(context, ringtoneUri);
            // In some cases the ringtone object is null if the ringtoneUri is the default ringtone uri.
            if (ringtone == null) {
                Uri defaultRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                if (ringtoneUri.equals(defaultRingtoneUri)) return new Result(preference, context.getString(R.string.pref_value_notification_ringtone_default));
                else return new Result(preference, "");
            } else {
                return new Result(preference, ringtone.getTitle(context));
            }
        }
    }

    @Override
    protected void onPostExecute(@NonNull Result result) {
        result.preference.setSummary(result.summary);
    }
}
