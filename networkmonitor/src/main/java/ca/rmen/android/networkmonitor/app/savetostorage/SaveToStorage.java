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
 * Copyright (C) 2015-2017 Carmen Alvarez (c@rmen.ca)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ca.rmen.android.networkmonitor.app.savetostorage;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.WorkerThread;
import android.widget.Toast;

import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dbops.ui.Share;

class SaveToStorage {

    private SaveToStorage() {
        // utility class
    }

    @WorkerThread
    static void displaySuccessToast(final Context context, final Uri dest) {
        Handler handler = new Handler(Looper.getMainLooper());
        final String displayName = Share.readDisplayName(context, dest);
        handler.post(() -> Toast.makeText(context, context.getString(R.string.export_save_to_external_storage_success, displayName), Toast.LENGTH_LONG).show());
    }

    static void displayErrorToast(final Context context) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(context, R.string.export_notif_error_content, Toast.LENGTH_LONG).show());
    }
}
