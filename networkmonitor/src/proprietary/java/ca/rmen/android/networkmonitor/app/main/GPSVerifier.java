/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2015 Carmen Alvarez (c@rmen.ca)
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
package ca.rmen.android.networkmonitor.app.main;

import android.app.Activity;
import android.app.Dialog;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

/**
 * Class which checks if Google Play Services is available on the device.
 */
class GPSVerifier {

    private Activity mActivity;
    private Dialog mGPSDialog;

    GPSVerifier(Activity activity) {
        mActivity = activity;
    }

    /**
     * Check if GPS is available, and show an error dialog if not.
     */
    void verifyGPS() {
        int playServicesAvailable = GooglePlayServicesUtil.isGooglePlayServicesAvailable(mActivity);
        if (playServicesAvailable != ConnectionResult.SUCCESS) {
            if (mGPSDialog != null) {
                mGPSDialog.dismiss();
                mGPSDialog = null;
            }

            if (GooglePlayServicesUtil.isUserRecoverableError(playServicesAvailable)) {
                mGPSDialog = GooglePlayServicesUtil.getErrorDialog(playServicesAvailable, mActivity, 1);
            }
            if (mGPSDialog != null) {
                mGPSDialog.show();
            } else {
                Toast.makeText(mActivity, "Google Play Services must be installed", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Dismiss the Google Play Services error dialog, if it is showing.
     */
    void dismissGPSDialog() {
        if (mGPSDialog != null) mGPSDialog.dismiss();
    }
}