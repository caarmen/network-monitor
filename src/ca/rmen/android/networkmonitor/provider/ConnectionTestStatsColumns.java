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
package ca.rmen.android.networkmonitor.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class ConnectionTestStatsColumns implements BaseColumns {
    static final String VIEW_NAME = "connection_test_stats";
    public static final Uri CONTENT_URI = Uri.parse(NetMonProvider.CONTENT_URI_BASE + "/" + VIEW_NAME);
    public static final String TYPE = "type";
    public static final String ID1 = "id1";
    public static final String ID2 = "id2";
    public static final String ID3 = "id3";
    public static final String LABEL = "label";
    public static final String TEST_RESULT = "test_result";
    public static final String TEST_COUNT = "test_count";
}