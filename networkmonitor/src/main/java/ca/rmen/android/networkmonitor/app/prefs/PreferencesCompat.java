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
package ca.rmen.android.networkmonitor.app.prefs;

import android.annotation.TargetApi;
import ca.rmen.android.networkmonitor.R;
import android.support.v7.widget.Toolbar;
import android.app.ActionBar;
import android.os.Build;
import android.preference.PreferenceActivity;
import android.view.View;

import ca.rmen.android.networkmonitor.Constants;

public class PreferencesCompat extends PreferenceActivity { // NO_UCD (use default)
    private static final String TAG = Constants.TAG + PreferencesCompat.class.getSimpleName();

    public static void setupActionBar(final PreferenceActivity activity) {
        activity.setContentView(R.layout.preference_layout);
        Toolbar actionbar = (Toolbar) activity.findViewById(R.id.actionbar);
        actionbar.setTitle(activity.getTitle());
        actionbar.setNavigationIcon(activity.getResources().getDrawable(activity.getApplicationInfo().icon));
        actionbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.finish();
            }
        });
    }
}