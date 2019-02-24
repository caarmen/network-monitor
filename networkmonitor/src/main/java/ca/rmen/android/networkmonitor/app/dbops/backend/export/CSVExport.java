/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2013-2019 Carmen Alvarez (c@rmen.ca)
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
package ca.rmen.android.networkmonitor.app.dbops.backend.export;

import android.content.Context;
import android.text.TextUtils;

import java.io.IOException;
import java.io.PrintWriter;

import ca.rmen.android.networkmonitor.app.dbops.backend.export.FormatterFactory.FormatterStyle;
import ca.rmen.android.networkmonitor.app.dbops.ui.Share;

/**
 * Export the Network Monitor data to a CSV file.
 */
public class CSVExport extends TableFileExport {
    private static final String CSV_FILE = "networkmonitor.csv";
    private PrintWriter mPrintWriter;

    public CSVExport(Context context) {
        super(context, Share.getExportFile(context, CSV_FILE), FormatterStyle.DEFAULT);
    }

    @Override
    void writeHeader(String[] columnNames) throws IOException {
        mPrintWriter = new PrintWriter(mFile, "utf-8");
        mPrintWriter.println(TextUtils.join(",", columnNames));
    }

    @Override
    void writeRow(int rowNumber, String[] cellValues) {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cellValues.length; i++) {
            // Escape commas and quotes.
            if (cellValues[i].contains(",") || cellValues[i].contains("\"")) {
                cellValues[i] = cellValues[i].replaceAll("\"", "\"\"");
                cellValues[i] = "\"" + cellValues[i] + "\"";
            }
            sb.append(cellValues[i]);
            if (i < cellValues.length - 1) sb.append(",");
        }
        mPrintWriter.println(sb);
        mPrintWriter.flush();
    }

    @Override
    void writeFooter() {
        mPrintWriter.flush();
        mPrintWriter.close();
    }
}
