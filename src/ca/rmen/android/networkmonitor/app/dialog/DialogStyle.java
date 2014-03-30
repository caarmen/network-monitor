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
package ca.rmen.android.networkmonitor.app.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import ca.rmen.android.networkmonitor.R;

/**
 * Customizations to dialogs which are non-hacky :) The hacky ones go into DialogStyleHacks.
 */
public class DialogStyle {

    /**
     * Set the text of the dialog's custom title view.
     */
    public static void setCustomTitle(Context context, AlertDialog.Builder builder, CharSequence title) {
        View customTitle = View.inflate(context, R.layout.dialog_title, null);
        TextView textView = (TextView) customTitle.findViewById(R.id.dialog_title);
        textView.setText(title);
        builder.setCustomTitle(customTitle);
    }
}
