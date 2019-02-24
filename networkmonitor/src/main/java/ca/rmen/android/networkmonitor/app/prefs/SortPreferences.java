/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2014-2019 Carmen Alvarez (c@rmen.ca)
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
package ca.rmen.android.networkmonitor.app.prefs;

import androidx.annotation.NonNull;
import ca.rmen.android.networkmonitor.provider.NetMonColumns;

/**
 * Settings for how rows of data are sorted (for export or display).
 */
public class SortPreferences {
    public enum SortOrder {
        ASC, DESC
    }

    public final String sortColumnName;
    public final SortPreferences.SortOrder sortOrder;

    public SortPreferences(String sortColumnName, SortPreferences.SortOrder sortOrder) {
        this.sortColumnName = sortColumnName;
        this.sortOrder = sortOrder;
    }

    /**
     * @return a string which can be used as an order by clause in a query.
     */
    public String getOrderByClause() {
        // Order criteria, based on the selected order column :
        // 1) Rows with null for this column will appear at the end
        // 2) Among rows with a non-null value for this column, sort by this column in the given order
        // 3) If two rows have the same value for this column, order by timestamp.
        // @formatter:off
        // Special case if the sort field is timestamp: don't need to check for null 
        // and don't need to add timestamp as a second sort column.d
        final String orderBy;
        if(sortColumnName.equals(NetMonColumns.TIMESTAMP))
            orderBy = sortColumnName + " " + sortOrder;
        else
            orderBy = sortColumnName + " IS NULL, " 
            + sortColumnName + " " + sortOrder.name() +", "
            + NetMonColumns.TIMESTAMP + " " + SortOrder.DESC.name();
        // @formatter:on
        return orderBy;
    }

    @Override
    @NonNull
    public String toString() {
        return SortPreferences.class.getSimpleName() + " " + sortColumnName + " " + sortOrder;
    }
}