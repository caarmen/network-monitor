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
package org.jraf.android.networkmonitor.app.export;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.provider.NetMonDatabase;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

/**
 * Export the Network Monitor DB file.
 */
public class DBExport extends FileExport {

	private static final String TAG = Constants.TAG
			+ DBExport.class.getSimpleName();

	public DBExport(Context context) throws FileNotFoundException {
		super(context, new File(context.getExternalFilesDir(null),
				NetMonDatabase.DATABASE_NAME));
	}

	@Override
	void writeHeader(String[] columnNames) throws IOException {
	}

	@Override
	void writeRow(int rowNumber, String[] cellValues) throws IOException {
	}

	@Override
	void writeFooter() throws IOException {
	}

	@Override
	public File export() {
		File db = mContext.getDatabasePath(NetMonDatabase.DATABASE_NAME);
		try {
			InputStream is = new FileInputStream(db);
			OutputStream os = new FileOutputStream(mFile);
			byte[] buffer = new byte[1024];
			int len;
			while ((len = is.read(buffer)) > 0) {
				os.write(buffer, 0, len);
			}
			is.close();
			os.close();
			return mFile;
		} catch (IOException e) {
			Log.v(TAG, "Could not copy DB file: " + e.getMessage(), e);
			return null;
		}
	}

}