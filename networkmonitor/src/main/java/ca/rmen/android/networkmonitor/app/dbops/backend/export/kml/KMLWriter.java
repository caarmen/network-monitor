/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014-2015 Carmen Alvarez (c@rmen.ca)
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
package ca.rmen.android.networkmonitor.app.dbops.backend.export.kml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import android.text.TextUtils;

/**
 * Writes a KML document with styles and placemarks.
 */
class KMLWriter extends PrintWriter {
    private static final String STYLEMAP_RED = "#stylemap_red";
    private static final String STYLEMAP_GREEN = "#stylemap_green";
    private static final String STYLEMAP_YELLOW = "#stylemap_yellow";
    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
    private final KMLStyle mKmlStyle;
    private final String mEmptyLabel;
    private final String mTitle;
    private final Map<String, String> mFieldDisplayNames;

    /**
     * @param file the kml file to create
     * @param emptyLabel when the placemark name has no value, use this label instead of a blank value
     * @param fieldDisplayNames mapping between the names of the placemark fields and the user-friendly names to display for these fields
     */
    public KMLWriter(File file, String title, KMLStyle kmlStyle, String emptyLabel, Map<String, String> fieldDisplayNames) throws FileNotFoundException {
        super(file);
        mKmlStyle = kmlStyle;
        mEmptyLabel = emptyLabel;
        mFieldDisplayNames = fieldDisplayNames;
        mTitle = title;
        TIMESTAMP_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public void writeHeader() {
        println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        println("<kml xmlns=\"http://earth.google.com/kml/2.1\">");
        println("  <Document>");
        print("    <name>");
        print(mTitle);
        println("</name>");
        writeStyles();
    }

    /**
     * Write a single Placemark
     * 
     * @param values map of field name to value
     */
    public void writePlacemark(String name, Map<String, String> values, String latitude, String longitude, long timestamp) {
        println("    <Placemark>");
        writePlacemarkName(name);
        writePlacemarkCoordinates(longitude, latitude);
        if (timestamp > 0) writePlacemarkTimestamp(timestamp);
        writePlacemarkStyleUrl(values);
        writePlacemarkExtendedData(values);
        println("    </Placemark>");
        flush();
    }

    public void writeFooter() {
        println("  </Document>");
        println("</kml>");
        flush();
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
        println("    <StyleMap id=\"stylemap_" + colorName + "\">");
        // Write the style map
        String[] keys = new String[] { "normal", "highlight" };
        for (String key : keys) {
            println("      <Pair>");
            println("        <key>" + key + "</key>");
            println("        <styleUrl>#style_" + colorName + "</styleUrl>");
            println("      </Pair>");
        }
        println("    </StyleMap>");

        // Write the style
        println("    <Style id=\"style_" + colorName + "\">");

        // The icon style
        println("      <IconStyle>");
        print("        <color>");
        print(colorCode);
        println("</color>");
        println("        <Icon>");
        print("          <href>");
        String iconUrl = "http://maps.google.com/mapfiles/kml/shapes/placemark_circle.png";
        print(iconUrl);
        println("</href>");
        println("        </Icon>");
        println("      </IconStyle>");

        // The label style
        println("      <LabelStyle>");
        println("        <scale>1.0</scale>");
        println("      </LabelStyle>");
        println("    </Style>");
    }

    /**
     * Write the label of this placemark. This is the value for the column the user chose to export.
     */
    private void writePlacemarkName(String label) {
        print("      <name>");
        if (TextUtils.isEmpty(label)) label = mEmptyLabel;
        print(label);
        println("</name>");
    }

    /**
     * Refer to the correct style, depending on the value of the data point for this record.
     */
    private void writePlacemarkStyleUrl(Map<String, String> values) {
        final String styleUrl;
        KMLStyle.IconColor iconColor = mKmlStyle.getColor(values);
        if (iconColor == KMLStyle.IconColor.GREEN) styleUrl = STYLEMAP_GREEN;
        else if (iconColor == KMLStyle.IconColor.RED) styleUrl = STYLEMAP_RED;
        else
            styleUrl = STYLEMAP_YELLOW;
        print("      <styleUrl>");
        print(styleUrl);
        println("</styleUrl>");
    }

    /**
     * Write the device coordinates for this record.
     */
    private void writePlacemarkCoordinates(String longitude, String latitude) {
        // Write the device coordinates.
        println("      <Point>");
        print("        <coordinates>");
        print(longitude + "," + latitude);
        println("</coordinates>");
        println("      </Point>");
    }

    /**
     * Write the timestamp.
     */
    private void writePlacemarkTimestamp(long timestamp) {
        Date date = new Date(timestamp);
        String timestampString = TIMESTAMP_FORMAT.format(date);
        print("      <when>");
        print(timestampString);
        println("</when>>");
    }

    /**
     * Write all the attributes we were able to retrieve for this record.
     */
    private void writePlacemarkExtendedData(Map<String, String> values) {
        println("      <ExtendedData>");
        for (String columnName : values.keySet()) {
            String displayName = mFieldDisplayNames.get(columnName);
            String value = values.get(columnName);
            if (!TextUtils.isEmpty(value)) {
                println("        <Data name=\"" + displayName + "\">");
                print("          <value>");
                print(value);
                println("</value>");
                println("        </Data>");
            }
        }
        println("      </ExtendedData>");
    }
}