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
 * Copyright (C) 2013-2016 Carmen Alvarez (c@rmen.ca)
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
package ca.rmen.android.networkmonitor.app.about;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.io.File;

import ca.rmen.android.networkmonitor.BuildConfig;
import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.Theme;
import ca.rmen.android.networkmonitor.util.Log;
import de.psdev.licensesdialog.LicensesDialogFragment;

public class AboutActivity extends AppCompatActivity {

    private static final String TAG = Constants.TAG + AboutActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);

        String versionName;
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = packageInfo.versionName;
        } catch (NameNotFoundException e) {
            // Should never happen
            throw new AssertionError(e);
        }

        ((TextView) findViewById(R.id.txtVersion)).setText(getString(R.string.app_name) + " v" + versionName);
        TextView tvLibraries = (TextView) findViewById(R.id.tv_about_libraries);
        SpannableString content = new SpannableString(getString(R.string.about_libraries));
        content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
        tvLibraries.setText(content);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // In night mode, we need to make the icons lighter, or else they don't appear.
        // We could just have different icon resources for night and day themes, but
        // I didn't feel like regenerating the icons :)
        tintCompoundDrawables(R.id.tv_about_legal,
            R.id.tv_about_bug,
            R.id.tv_about_contributions,
            R.id.tv_about_libraries,
            R.id.tv_about_rate,
            R.id.tv_about_source_code);
    }

    private void tintCompoundDrawables(@IdRes int... textViewIds) {
        for (int textViewId : textViewIds) {
            Theme.tintCompoundDrawables(this, (TextView) findViewById(textViewId));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.about, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem itemSendLogs = menu.findItem(R.id.action_send_logs);
        itemSendLogs.setVisible(BuildConfig.DEBUG);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.action_send_logs:
                new AsyncTask<Void, Void, Boolean>() {

                    @Override
                    protected Boolean doInBackground(Void... params) {
                        if (!Log.prepareLogFile(getApplicationContext())) {
                            return false;
                        }
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
                        return true;
                    }

                    @Override
                    protected void onPostExecute(Boolean result) {
                        if (!result) Snackbar.make(getWindow().getDecorView().getRootView(), R.string.support_error, Snackbar.LENGTH_LONG).show();
                    }


                }.execute();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("UnusedParameters")
    public void onLibrariesClicked(View v) {
        try {
            final LicensesDialogFragment fragment = new LicensesDialogFragment.Builder(this)
                    .setNotices(R.raw.licenses)
                    .setThemeResourceId(R.style.AppCompatAlertDialogStyle)
                    .setShowFullLicenseText(false)
                    .setUseAppCompat(true)
                    .setIncludeOwnLicense(true)
                    .setDividerColorRes(R.color.netmon_color)
                    .build();

            fragment.show(getSupportFragmentManager(), null);
        } catch (Exception e) {
            Log.w(TAG, "Couldn't show license dialog", e);
        }
    }

    @SuppressWarnings("UnusedParameters")
    public void onLegalClicked(View v) {
        startActivity(new Intent(this, LicenseActivity.class));
    }
}
