/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2013 Carmen Alvarez (c@rmen.ca)
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
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.text.TextUtils;

import org.jraf.android.networkmonitor.Constants;
import org.jraf.android.networkmonitor.R;

/**
 * Export the Network Monitor data to a KML file. For now, the KML file placemark icons depend on the connection test result.
 */
public class KMLExport extends TableFileExport {
    private static final String KML_FILE = "networkmonitor.kml";
    // KML colors are of the format aabbggrr: https://developers.google.com/kml/documentation/kmlreference#color
    private static final String ICON_COLOR_RED = "ff0000ff";
    private static final String ICON_COLOR_GREEN = "ff00ff00";
    private static final String ICON_COLOR_YELLOW = "ff00ffff";
    private PrintWriter mPrintWriter;
    private String[] mColumnNames;
    private int mHttpConnectionTestColumnIndex;
    private int mSocketConnectionTestColumnIndex;
    private int mLatitudeColumnIndex;
    private int mLongitudeColumnIndex;



    /**
     * @param external if true, the file will be exported to the sd card. Otherwise it will written to the app's internal storage.
     */
    public KMLExport(Context context, boolean external, FileExport.ExportProgressListener listener) throws FileNotFoundException {
        super(context, new File(external ? context.getExternalFilesDir(null) : context.getFilesDir(), KML_FILE), listener);
        mPrintWriter = new PrintWriter(mFile);
    }

    @Override
    void writeHeader(String[] columnNames) {
        mColumnNames = columnNames;
        Map<String, Integer> columnNamePositions = new HashMap<String, Integer>();
        for (int i = 0; i < columnNames.length; i++)
            columnNamePositions.put(columnNames[i], i);
        mHttpConnectionTestColumnIndex = columnNamePositions.get(mContext.getString(R.string.http_connection_test));
        mSocketConnectionTestColumnIndex = columnNamePositions.get(mContext.getString(R.string.google_connection_test));
        mLatitudeColumnIndex = columnNamePositions.get(mContext.getString(R.string.device_latitude));
        mLongitudeColumnIndex = columnNamePositions.get(mContext.getString(R.string.device_longitude));
        mPrintWriter.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        mPrintWriter.println("<kml xmlns=\"http://earth.google.com/kml/2.1\">");
        mPrintWriter.println("  <Folder>");
    }

    @Override
    void writeRow(int rowNumber, String[] cellValues) {
        mPrintWriter.println("    <Placemark>");
        mPrintWriter.print("      <name>");
        String label = getLabel(cellValues);
        mPrintWriter.print(label);
        mPrintWriter.println("</name>");
        mPrintWriter.println("      <Point>");
        mPrintWriter.print("        <coordinates>");
        String latitude = cellValues[mLatitudeColumnIndex];
        String longitude = cellValues[mLongitudeColumnIndex];
        mPrintWriter.print(longitude + "," + latitude);
        mPrintWriter.println("</coordinates>");
        mPrintWriter.println("      </Point>");
        mPrintWriter.println("      <ExtendedData>");
        for (int i = 0; i < mColumnNames.length; i++) {
            if (!TextUtils.isEmpty(cellValues[i])) {
                mPrintWriter.println("        <Data name=\"" + mColumnNames[i] + "\">");
                mPrintWriter.print("          <value>");
                mPrintWriter.print(cellValues[i]);
                mPrintWriter.println("</value>");
                mPrintWriter.println("        </Data>");
            }
        }
        mPrintWriter.println("      </ExtendedData>");
        mPrintWriter.println("      <Style>");
        mPrintWriter.println("        <LabelStyle>");
        mPrintWriter.println("          <scale>1.0</scale>");
        mPrintWriter.println("        </LabelStyle>");
        mPrintWriter.println("        <IconStyle>");
        mPrintWriter.print("          <color>");
        String iconColor = getIconColor(label);
        mPrintWriter.print(iconColor);
        mPrintWriter.println("</color>");
        mPrintWriter.println("          <scale>1.0</scale>");
        mPrintWriter.println("          <Icon>");
        mPrintWriter.print("            <href>");
        String iconUrl = "http://maps.google.com/mapfiles/kml/shapes/placemark_circle.png";
        mPrintWriter.print(iconUrl);
        mPrintWriter.println("</href>");
        mPrintWriter.println("          </Icon>");
        mPrintWriter.println("        </IconStyle>");
        mPrintWriter.println("      </Style>");
        mPrintWriter.println("    </Placemark>");
        mPrintWriter.flush();
    }

    private String getIconColor(String label) {
        if (Constants.CONNECTION_TEST_FAIL.equals(label)) return ICON_COLOR_RED;
        if (Constants.CONNECTION_TEST_PASS.equals(label)) return ICON_COLOR_GREEN;
        return ICON_COLOR_YELLOW;
    }

    private String getLabel(String[] cellValues) {
        String httpConnectionTest = cellValues[mHttpConnectionTestColumnIndex];
        String socketConnectionTest = cellValues[mSocketConnectionTestColumnIndex];
        // Both are PASS, both are SLOW, or both are FAIL. Return the string for both tests.
        if (httpConnectionTest.equals(socketConnectionTest)) return httpConnectionTest;
        // Return the worse of the two:
        if (Constants.CONNECTION_TEST_FAIL.equals(httpConnectionTest) || Constants.CONNECTION_TEST_FAIL.equals(socketConnectionTest))
            return Constants.CONNECTION_TEST_FAIL;
        return Constants.CONNECTION_TEST_SLOW;
    }


    @Override
    void writeFooter() {
        mPrintWriter.println("  </Folder>");
        mPrintWriter.println("</kml>");
        mPrintWriter.flush();
        mPrintWriter.close();
    }
}
