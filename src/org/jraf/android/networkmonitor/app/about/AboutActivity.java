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
package org.jraf.android.networkmonitor.app.about;

import java.io.File;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.jraf.android.networkmonitor.R;
import org.jraf.android.networkmonitor.util.Log;

public class AboutActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);

        String versionName = null;
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = packageInfo.versionName;
        } catch (NameNotFoundException e) {
            // Should never happen
            throw new AssertionError(e);
        }

        ((TextView) findViewById(R.id.txtVersion)).setText(getString(R.string.app_name) + " v" + versionName);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) setDisplayHomeAsUpEnabled(true);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setDisplayHomeAsUpEnabled(boolean enabled) {
        getActionBar().setDisplayHomeAsUpEnabled(enabled);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.about, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.action_send_logs:
                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... params) {
                        Log.prepareLogFile();
                        // Bring up the chooser to share the file.
                        Intent sendIntent = new Intent();
                        sendIntent.setAction(Intent.ACTION_SEND);
                        //sendIntent.setData(Uri.fromParts("mailto", getString(R.string.send_logs_to), null));
                        sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.support_send_debug_logs_subject));
                        String messageBody = getString(R.string.support_send_debug_logs_body);
                        File f = new File(getExternalFilesDir(null), Log.FILE);
                        sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + f.getAbsolutePath()));
                        sendIntent.setType("message/rfc822");
                        sendIntent.putExtra(Intent.EXTRA_TEXT, messageBody);
                        sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { getString(R.string.support_send_debug_logs_to) });
                        startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.action_share)));
                        return null;
                    }

                }.execute();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
