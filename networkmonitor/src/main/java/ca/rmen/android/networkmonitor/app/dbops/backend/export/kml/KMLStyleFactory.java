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

import java.util.Map;

import android.text.TextUtils;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;

/**
 * Returns the proper styling info (for now, just the icon color) for a KML placemark.
 */
class KMLStyleFactory {

    /**
     * @return an implementation of KMLStyle for the given placemark extended data field name.
     */
    static KMLStyle getKMLStyle(String fieldName) {
        KMLStyle result;
        if (NetMonColumns.SOCKET_CONNECTION_TEST.equals(fieldName) || NetMonColumns.HTTP_CONNECTION_TEST.equals(fieldName)) result = new KMLStyleConnectionTest(
                fieldName);
        else if (NetMonColumns.IS_CONNECTED.equals(fieldName) || NetMonColumns.IS_ROAMING.equals(fieldName) || NetMonColumns.IS_AVAILABLE.equals(fieldName)
                || NetMonColumns.IS_FAILOVER.equals(fieldName) || NetMonColumns.IS_NETWORK_METERED.equals(fieldName)) result = new KMLStyleBoolean(fieldName);
        else if (NetMonColumns.WIFI_SIGNAL_STRENGTH.equals(fieldName) || NetMonColumns.WIFI_RSSI.equals(fieldName)
                || NetMonColumns.CELL_SIGNAL_STRENGTH.equals(fieldName) || NetMonColumns.CELL_SIGNAL_STRENGTH_DBM.equals(fieldName)
                || NetMonColumns.CELL_ASU_LEVEL.equals(fieldName)) result = new KMLStyleSignalStrength(fieldName);
        else if (NetMonColumns.DETAILED_STATE.equals(fieldName)) result = new KMLStyleDetailedState(fieldName);
        else if (NetMonColumns.SIM_STATE.equals(fieldName)) result = new KMLStyleSIMState(fieldName);
        else if (NetMonColumns.DATA_ACTIVITY.equals(fieldName)) result = new KMLStyleDataActivity(fieldName);
        else if (NetMonColumns.DATA_STATE.equals(fieldName)) result = new KMLStyleDataState(fieldName);
        else if (NetMonColumns.BATTERY_LEVEL.equals(fieldName)) result = new KMLStyleBatteryLevel(fieldName);
        else
            result = new KMLStyleDefault(fieldName);
        return result;
    }

    private static class KMLStyleDefault implements KMLStyle {
        // The name of the field which is used for the label/name of a placemark.
        final String mPlacemarkNameField;

        /**
         * @param placemarkNameField the name of the field which determines the name/label of the placemark. In most cases the value of this field will also
         *            determine the color of the icon.
         */
        private KMLStyleDefault(String placemarkNameField) {
            mPlacemarkNameField = placemarkNameField;
        }

        /**
         * @return the icon color to use given the attributes for a given placemark
         */
        @Override
        public IconColor getColor(Map<String, String> values) {
            return getColor(values.get(mPlacemarkNameField));
        }


        /**
         * @return the icon color to use given the value for the relevant attribute of a given placemark.
         */
        IconColor getColor(String value) {
            if (TextUtils.isEmpty(value)) return IconColor.YELLOW;
            return IconColor.GREEN;
        }

    }

    private static class KMLStyleConnectionTest extends KMLStyleDefault {
        private KMLStyleConnectionTest(String columnName) {
            super(columnName);
        }

        @Override
        protected IconColor getColor(String value) {
            if (Constants.CONNECTION_TEST_FAIL.equals(value)) return IconColor.RED;
            if (Constants.CONNECTION_TEST_PASS.equals(value)) return IconColor.GREEN;
            return IconColor.YELLOW;
        }
    }

    private static class KMLStyleBoolean extends KMLStyleDefault {
        private KMLStyleBoolean(String columnName) {
            super(columnName);
        }

        @Override
        protected IconColor getColor(String value) {
            if ("0".equals(value)) return IconColor.RED;
            if ("1".equals(value)) return IconColor.GREEN;
            return IconColor.YELLOW;
        }
    }

    private static class KMLStyleSignalStrength extends KMLStyleDefault {

        private KMLStyleSignalStrength(String columnName) {
            super(columnName);
        }

        @Override
        public IconColor getColor(Map<String, String> values) {
            // If we are reporting on the wifi signal strength or rssi, the icon color will be determined by the wifi signal strength
            if (NetMonColumns.WIFI_RSSI.equals(mPlacemarkNameField) || NetMonColumns.WIFI_SIGNAL_STRENGTH.equals(mPlacemarkNameField)) return getColor(values
                    .get(NetMonColumns.WIFI_SIGNAL_STRENGTH));
            // If we are reporting on the cell signal strength (0-4), cell signal strength (dBm), or asu level, the icon color will be determined by the cell signal strength (0-4)
            else
                return getColor(values.get(NetMonColumns.CELL_SIGNAL_STRENGTH));
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

    private static class KMLStyleDetailedState extends KMLStyleDefault {
        private KMLStyleDetailedState(String columnName) {
            super(columnName);
        }

        @Override
        protected IconColor getColor(String value) {
            if ("CONNECTED".equals(value)) return IconColor.GREEN;
            if ("CONNECTING".equals(value) || "AUTHENTICATING".equals(value) || "OBTAINING_IPADDR".equals(value) || "IDLE".equals(value))
                return IconColor.YELLOW;
            return IconColor.RED;
        }
    }

    private static class KMLStyleSIMState extends KMLStyleDefault {
        private KMLStyleSIMState(String columnName) {
            super(columnName);
        }

        @Override
        protected IconColor getColor(String value) {
            if ("READY".equals(value)) return IconColor.GREEN;
            if ("PIN_REQUIRED".equals(value)) return IconColor.YELLOW;
            return IconColor.RED;
        }
    }

    private static class KMLStyleDataActivity extends KMLStyleDefault {
        private KMLStyleDataActivity(String columnName) {
            super(columnName);
        }

        @Override
        protected IconColor getColor(String value) {
            if ("DORMANT".equals(value)) return IconColor.RED;
            if ("NONE".equals(value)) return IconColor.YELLOW;
            return IconColor.GREEN;
        }
    }

    private static class KMLStyleDataState extends KMLStyleDefault {
        private KMLStyleDataState(String columnName) {
            super(columnName);
        }

        @Override
        protected IconColor getColor(String value) {
            if ("CONNECTED".equals(value)) return IconColor.GREEN;
            if ("CONNECTING".equals(value)) return IconColor.YELLOW;
            return IconColor.RED;
        }
    }

    private static class KMLStyleBatteryLevel extends KMLStyleDefault {
        private KMLStyleBatteryLevel(String columnName) {
            super(columnName);
        }

        @Override
        protected IconColor getColor(String value) {
            if (TextUtils.isEmpty(value)) return IconColor.YELLOW;
            try {
                Integer batteryLevel = Integer.valueOf(value);
                if (batteryLevel > 67) return IconColor.GREEN;
                if (batteryLevel > 33) return IconColor.YELLOW;
                return IconColor.RED;
            } catch (NumberFormatException e) {
                return IconColor.YELLOW;
            }
        }
    }
}