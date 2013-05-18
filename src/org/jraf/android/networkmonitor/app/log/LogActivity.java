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
package org.jraf.android.networkmonitor.app.log;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.R;
import org.jraf.android.networkmonitor.provider.NetMonColumns;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class LogActivity extends Activity {

	private static final String TAG = Constants.TAG
			+ LogActivity.class.getSimpleName();
	private static final String CSV_FILE = "networkmonitor.csv";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.log);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.log, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_send) {
			AsyncTask<Void, Void, File> asyncTask = new AsyncTask<Void, Void, File>() {

				@Override
				protected File doInBackground(Void... params) {
					if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
						return null;

					File file = new File(getExternalFilesDir(null), CSV_FILE);
					Cursor c = getContentResolver().query(
							NetMonColumns.CONTENT_URI, null, null, null,
							NetMonColumns.TIMESTAMP);
					if (c != null) {
						try {
							PrintWriter pw = new PrintWriter(file);
							String[] columnNames = c.getColumnNames();
							pw.println(TextUtils.join(",", columnNames));
							if (c.moveToFirst()) {
								while (c.moveToNext()) {
									StringBuilder sb = new StringBuilder();

									for (int i = 0; i < columnNames.length; i++) {
										String cellValue = c
												.getString(c
														.getColumnIndex(columnNames[i]));
										if (cellValue.contains(",")) {
											cellValue.replaceAll("\"", "\"\"");
											cellValue = "\"" + cellValue + "\"";
										}
										sb.append(cellValue);
										if (i < columnNames.length - 1)
											sb.append(",");
									}
									pw.println(sb);
									pw.flush();
								}
							}
							pw.close();
						} catch (IOException e) {
							Log.e(TAG,
									"Error creating CSV file: "
											+ e.getMessage(), e);
							return null;
						} finally {
							c.close();
						}
					}
					Intent sendIntent = new Intent();
					sendIntent.setAction(Intent.ACTION_SEND);
					sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.subject_send_log));
					sendIntent.putExtra(Intent.EXTRA_STREAM,
							Uri.parse("file://" + file.getAbsolutePath()));
					sendIntent.setType("message/rfc822");
					startActivity(Intent.createChooser(sendIntent,
							getResources().getText(R.string.action_send)));

					return file;
				}

				@Override
				protected void onPostExecute(File result) {
					super.onPostExecute(result);
					if (result == null)
						Toast.makeText(LogActivity.this,
								R.string.error_sdcard_unmounted,
								Toast.LENGTH_LONG).show();
				}

			};
			asyncTask.execute();
		}
		return super.onOptionsItemSelected(item);
	}

}
