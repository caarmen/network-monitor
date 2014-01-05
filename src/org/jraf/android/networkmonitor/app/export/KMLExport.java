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
 * Export the Network Monitor data to a KML file. For now, the KML file placemark icon label and color depend on the connection test result.
 */
public class KMLExport extends TableFileExport {
    private static final String KML_FILE = "networkmonitor.kml";
    private static final String STYLEMAP_RED = "#stylemap_red";
    private static final String STYLEMAP_GREEN = "#stylemap_green";
    private static final String STYLEMAP_YELLOW = "#stylemap_yellow";

    private PrintWriter mPrintWriter;
    private String[] mColumnNames;
    private int mHttpConnectionTestColumnIndex;
    private int mSocketConnectionTestColumnIndex;
    private int mLatitudeColumnIndex;
    private int mLongitudeColumnIndex;



    /**
     */
    public KMLExport(Context context, FileExport.ExportProgressListener listener) throws FileNotFoundException {
        super(context, new File(context.getExternalFilesDir(null), KML_FILE), listener);
        mPrintWriter = new PrintWriter(mFile);
    }

    @Override
    void writeHeader(String[] columnNames) {
        mColumnNames = columnNames;
        // Save the indices of the columns which require specific processing:
        // * the connection test result columns determine the label and color of the icons.
        // * the latitude and longitude are exported in a specific attribute
        Map<String, Integer> columnNamePositions = new HashMap<String, Integer>();
        for (int i = 0; i < columnNames.length; i++)
            columnNamePositions.put(columnNames[i], i);
        mHttpConnectionTestColumnIndex = columnNamePositions.get(mContext.getString(R.string.http_connection_test));
        mSocketConnectionTestColumnIndex = columnNamePositions.get(mContext.getString(R.string.google_connection_test));
        mLatitudeColumnIndex = columnNamePositions.get(mContext.getString(R.string.device_latitude));
        mLongitudeColumnIndex = columnNamePositions.get(mContext.getString(R.string.device_longitude));

        mPrintWriter.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        mPrintWriter.println("<kml xmlns=\"http://earth.google.com/kml/2.1\">");
        mPrintWriter.println("  <Document>");
        writeStyles();
    }

    /**
     * To make the output file a bit smaller, we use KML styles: one style for pass (green), one for slow (yellow) and one for fail (red).
     */
    private void writeStyles() {
        // KML colors are of the format aabbggrr: https://developers.google.com/kml/documentation/kmlreference#color
        writeStyle("red", "ff0000ff");
        writeStyle("green", "ff00ff00");
        writeStyle("yellow", "ff00ffff");
    }

    /**
     * Write the style xml for the given color.
     * 
     * @param colorName the name of the color: used for the name of the kml style and stylemap
     * @param colorCode the aabbggrr color code for this color.
     */
    private void writeStyle(String colorName, String colorCode) {
        mPrintWriter.println("    <StyleMap id=\"stylemap_" + colorName + "\">");
        // Write the style map
        String[] keys = new String[] { "normal", "highlight" };
        for (String key : keys) {
            mPrintWriter.println("      <Pair>");
            mPrintWriter.println("        <key>" + key + "</key>");
            mPrintWriter.println("        <styleUrl>#style_" + colorName + "</styleUrl>");
            mPrintWriter.println("      </Pair>");
        }
        mPrintWriter.println("    </StyleMap>");

        // Write the style
        mPrintWriter.println("    <Style id=\"style_" + colorName + "\">");

        // The icon style
        mPrintWriter.println("      <IconStyle>");
        mPrintWriter.print("        <color>");
        mPrintWriter.print(colorCode);
        mPrintWriter.println("</color>");
        mPrintWriter.println("        <Icon>");
        mPrintWriter.print("          <href>");
        String iconUrl = "http://maps.google.com/mapfiles/kml/shapes/placemark_circle.png";
        mPrintWriter.print(iconUrl);
        mPrintWriter.println("</href>");
        mPrintWriter.println("        </Icon>");
        mPrintWriter.println("      </IconStyle>");

        // The label style
        mPrintWriter.println("      <LabelStyle>");
        mPrintWriter.println("        <scale>1.0</scale>");
        mPrintWriter.println("      </LabelStyle>");
        mPrintWriter.println("    </Style>");
    }

    @Override
    void writeRow(int rowNumber, String[] cellValues) {
        mPrintWriter.println("    <Placemark>");

        // The label/name of this placemark will be the connection test result.
        mPrintWriter.print("      <name>");
        String label = getLabel(cellValues);
        mPrintWriter.print(label);
        mPrintWriter.println("</name>");

        // Write the device coordinates.
        mPrintWriter.println("      <Point>");
        mPrintWriter.print("        <coordinates>");
        String latitude = cellValues[mLatitudeColumnIndex];
        String longitude = cellValues[mLongitudeColumnIndex];
        mPrintWriter.print(longitude + "," + latitude);
        mPrintWriter.println("</coordinates>");
        mPrintWriter.println("      </Point>");

        // Write all the attributes we were able to retrieve for this connection test.
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

        // Refer to the correct style, depending on the test result.
        mPrintWriter.print("      <styleUrl>");
        String styleUrl = getStyleUrl(label);
        mPrintWriter.print(styleUrl);
        mPrintWriter.println("</styleUrl>");
        mPrintWriter.println("    </Placemark>");
        mPrintWriter.flush();
    }

    private String getStyleUrl(String label) {
        if (Constants.CONNECTION_TEST_FAIL.equals(label)) return STYLEMAP_RED;
        if (Constants.CONNECTION_TEST_PASS.equals(label)) return STYLEMAP_GREEN;
        return STYLEMAP_YELLOW;
    }

    /**
     * @param cellValues all the recorded values for this connection test.
     * @return the label to display for this record.
     */
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
        mPrintWriter.println("  </Document>");
        mPrintWriter.println("</kml>");
        mPrintWriter.flush();
        mPrintWriter.close();
    }
}
