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

    private final PrintWriter mPrintWriter;
    private String[] mColumnNames;

    private KMLStyle mKmlStyle;

    // The column which determines the label and icon color of each KML data point.
    private final String mDataColumnName;

    // The positions of the columns which need special handling.
    // * The value of the field the user chose to export is exported as a Placemark label.
    // * the latitude and longitude are exported in a specific attribute
    private final Map<String, Integer> mColumnNamePositions = new HashMap<String, Integer>();
    private int mDataColumnIndex;
    private int mLatitudeColumnIndex;
    private int mLongitudeColumnIndex;



    /**
     */
    public KMLExport(Context context, FileExport.ExportProgressListener listener, String dataColumnName) throws FileNotFoundException {
        super(context, new File(context.getExternalFilesDir(null), KML_FILE), listener);
        mDataColumnName = dataColumnName;
        mPrintWriter = new PrintWriter(mFile);
    }

    @Override
    void writeHeader(String[] columnNames) {
        mColumnNames = columnNames;
        // Save the indices of the columns which require specific processing:
        for (int i = 0; i < columnNames.length; i++)
            mColumnNamePositions.put(columnNames[i], i);
        mDataColumnIndex = mColumnNamePositions.get(mDataColumnName);
        mLatitudeColumnIndex = mColumnNamePositions.get(mContext.getString(R.string.device_latitude));
        mLongitudeColumnIndex = mColumnNamePositions.get(mContext.getString(R.string.device_longitude));

        mKmlStyle = KMLStyle.getKMLStyle(mContext, mDataColumnName, mColumnNamePositions);

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

        // Write the label of this placemark. This is the value for the column the user chose to export.
        mPrintWriter.print("      <name>");
        final String styleUrl;
        String label = cellValues[mDataColumnIndex];
        if (TextUtils.isEmpty(label)) {
            label = mContext.getString(R.string.unknown);
            styleUrl = STYLEMAP_YELLOW;
        } else {
            KMLStyle.IconColor iconColor = mKmlStyle.getColor(cellValues);
            if (iconColor == org.jraf.android.networkmonitor.app.export.KMLExport.KMLStyle.IconColor.GREEN) styleUrl = STYLEMAP_GREEN;
            else if (iconColor == org.jraf.android.networkmonitor.app.export.KMLExport.KMLStyle.IconColor.RED) styleUrl = STYLEMAP_RED;
            else
                styleUrl = STYLEMAP_YELLOW;
        }
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
        mPrintWriter.print(styleUrl);
        mPrintWriter.println("</styleUrl>");
        mPrintWriter.println("    </Placemark>");
        mPrintWriter.flush();
    }

    @Override
    void writeFooter() {
        mPrintWriter.println("  </Document>");
        mPrintWriter.println("</kml>");
        mPrintWriter.flush();
        mPrintWriter.close();
    }

    private static class KMLStyle {
        private int mColumnIndex;

        enum IconColor {
            RED, YELLOW, GREEN
        };

        protected void setColumnIndex(int columnIndex) {
            mColumnIndex = columnIndex;
        }

        IconColor getColor(String[] values) {
            return getColor(values[mColumnIndex]);
        }

        IconColor getColor(String value) {
            return IconColor.YELLOW;
        }

        static KMLStyle getKMLStyle(Context context, String columnName, Map<String, Integer> columnPositions) {
            int columnIndex = columnPositions.get(columnName);
            KMLStyle result = null;
            if (context.getString(R.string.google_connection_test).equals(columnName) || context.getString(R.string.http_connection_test).equals(columnName)) result = new KMLStyleConnectionTest();
            else if (context.getString(R.string.is_connected).equals(columnName) || context.getString(R.string.is_roaming).equals(columnName)
                    || context.getString(R.string.is_available).equals(columnName) || context.getString(R.string.is_failover).equals(columnName)
                    || context.getString(R.string.is_network_metered).equals(columnName)) result = new KMLStyleBoolean();
            else if (context.getString(R.string.wifi_signal_strength).equals(columnName) || context.getString(R.string.wifi_rssi).equals(columnName)
                    || context.getString(R.string.cell_signal_strength).equals(columnName)
                    || context.getString(R.string.cell_signal_strength_dbm).equals(columnName) || context.getString(R.string.cell_asu_level).equals(columnName)) return new KMLStyleSignalStrength(
                    context, columnName, columnPositions);
            else if (context.getString(R.string.detailed_state).equals(columnName)) result = new KMLStyleDetailedState();
            else if (context.getString(R.string.sim_state).equals(columnName)) result = new KMLStyleSIMState();
            else if (context.getString(R.string.data_activity).equals(columnName)) result = new KMLStyleDataActivity();
            else if (context.getString(R.string.data_state).equals(columnName)) result = new KMLStyleDataState();
            else
                result = new KMLStyle();
            result.setColumnIndex(columnIndex);
            return result;
        }
    }

    private static class KMLStyleConnectionTest extends KMLStyle {
        @Override
        IconColor getColor(String value) {
            if (Constants.CONNECTION_TEST_FAIL.equals(value)) return IconColor.RED;
            if (Constants.CONNECTION_TEST_PASS.equals(value)) return IconColor.GREEN;
            return IconColor.YELLOW;
        }
    }

    private static class KMLStyleBoolean extends KMLStyle {
        @Override
        IconColor getColor(String value) {
            if ("0".equals(value)) return IconColor.RED;
            if ("1".equals(value)) return IconColor.GREEN;
            return IconColor.YELLOW;
        }
    }

    private static class KMLStyleSignalStrength extends KMLStyle {

        KMLStyleSignalStrength(Context context, String columnName, Map<String, Integer> columnPositions) {
            final int columnIndex;
            // If we are reporting on the wifi signal strength or rssi, the icon color will be determined by the wifi signal strength
            if (context.getString(R.string.wifi_rssi).equals(columnName) || context.getString(R.string.wifi_signal_strength).equals(columnName)) columnIndex = columnPositions
                    .get(context.getString(R.string.wifi_signal_strength));
            // If we are reporting on the cell signal strength (0-4), cell signal strength (dBm), or asu level, the icon color will be determined by the cell signal strength (0-4)
            else
                columnIndex = columnPositions.get(context.getString(R.string.cell_signal_strength));
            setColumnIndex(columnIndex);
        }

        @Override
        IconColor getColor(String value) {
            if (TextUtils.isEmpty(value)) return IconColor.YELLOW;
            Integer signalStrength = Integer.valueOf(value);
            if (signalStrength >= 4) return IconColor.GREEN;
            if (signalStrength <= 1) return IconColor.RED;
            return IconColor.YELLOW;
        }
    }

    private static class KMLStyleDetailedState extends KMLStyle {
        @Override
        IconColor getColor(String value) {
            if ("CONNECTED".equals(value)) return IconColor.GREEN;
            if ("CONNECTING".equals(value) || "AUTHENTICATING".equals(value) || "OBTAINING_IPADDR".equals(value) || "IDLE".equals(value))
                return IconColor.YELLOW;
            return IconColor.RED;
        }
    }

    private static class KMLStyleSIMState extends KMLStyle {
        @Override
        IconColor getColor(String value) {
            if ("READY".equals(value)) return IconColor.GREEN;
            if ("PIN_REQUIRED".equals(value)) return IconColor.YELLOW;
            return IconColor.RED;
        }
    }

    private static class KMLStyleDataActivity extends KMLStyle {
        @Override
        IconColor getColor(String value) {
            if ("DORMANT".equals(value)) return IconColor.RED;
            if ("NONE".equals(value)) return IconColor.YELLOW;
            return IconColor.GREEN;
        }
    }

    private static class KMLStyleDataState extends KMLStyle {
        @Override
        IconColor getColor(String value) {
            if ("CONNECTED".equals(value)) return IconColor.GREEN;
            if ("CONNECTING".equals(value)) return IconColor.YELLOW;
            return IconColor.RED;
        }
    }
}
