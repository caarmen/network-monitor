/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014-2016 Carmen Alvarez (c@rmen.ca)
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
package ca.rmen.android.networkmonitor.app.speedtest;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;
import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;

/**
 * Activity which shows an HTML explanation of the speed test limitations
 */
public class SpeedTestAboutActivity extends AppCompatActivity {
    private static final String TAG = Constants.TAG + SpeedTestAboutActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speed_test_about);

        WebView wv = findViewById(R.id.webview);
        wv.setBackgroundColor(0);
        wv.getSettings().setDefaultFontSize(14);
        wv.loadUrl(getString(R.string.speed_test_about_file));
    }

    @SuppressWarnings("UnusedParameters")
    public void okClicked(View view) {
        Log.v(TAG, "okClicked");
        finish();
    }

}
