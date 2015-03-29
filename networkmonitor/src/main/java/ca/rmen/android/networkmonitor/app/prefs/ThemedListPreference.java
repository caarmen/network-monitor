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

import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.afollestad.materialdialogs.prefs.MaterialListPreference;

import ca.rmen.android.networkmonitor.app.dialog.DialogStyle;

public class ThemedListPreference extends MaterialListPreference {

    public ThemedListPreference(Context context) {
        super(context);
    }

    public ThemedListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
    }

    // http://stackoverflow.com/questions/10119852/listpreferences-summary-text-is-not-updated-automatically-whenever-there-is-cha
    // NOTE:
    // The framework forgot to call notifyChanged() in setValue() on previous versions of android.
    // This bug has been fixed in android-4.4_r0.7.
    // Commit: platform/frameworks/base/+/94c02a1a1a6d7e6900e5a459e9cc699b9510e5a2
    // Time: Tue Jul 23 14:43:37 2013 -0700
    //
    // However on previous versions, we have to workaround it by ourselves.
    @Override
    public void setValue(String value) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            super.setValue(value);
        } else {
            String oldValue = getValue();
            super.setValue(value);
            if (!TextUtils.equals(value, oldValue)) {
                notifyChanged();
            }
        }
    }

    @Override
    public CharSequence getSummary() {
        CharSequence summary = super.getSummary();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) return summary;
        CharSequence entry = getEntry();
        if (!TextUtils.isEmpty(summary) && !TextUtils.isEmpty(entry)) {
            return String.valueOf(summary).replace("%s", entry);
        }
        return summary;
    }

}
