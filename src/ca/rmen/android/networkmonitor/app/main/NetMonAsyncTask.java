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

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.db.DBProcessProgressListener;
import ca.rmen.android.networkmonitor.app.db.DBTask;
import ca.rmen.android.networkmonitor.app.dialog.DialogFragmentFactory;
import ca.rmen.android.networkmonitor.app.dialog.ProgressDialogFragment;
import ca.rmen.android.networkmonitor.util.Log;

public abstract class NetMonAsyncTask<T> extends AsyncTask<Void, Void, T> {
    private static final String TAG = Constants.TAG + NetMonAsyncTask.class.getSimpleName();

    public static final String EXTRA_DIALOG_STYLE = "extra_dialog_style";
    public static final String EXTRA_DIALOG_MESSAGE = "extra_dialog_message";
    private static final String PROGRESS_DIALOG_FRAGMENT_TAG = "progress_dialog_fragment_tag";

    private final FragmentActivity mActivity;
    private final DBTask<T> mTask;
    private final int mDialogStyle;
    private final String mDialogMessage;


    public NetMonAsyncTask(FragmentActivity activity, DBTask<T> task, Bundle args) {
        mActivity = activity;
        mTask = task;
        if (args == null) args = new Bundle();
        mDialogStyle = args.getInt(EXTRA_DIALOG_STYLE, ProgressDialog.STYLE_HORIZONTAL);
        String dialogMessage = args.getString(EXTRA_DIALOG_MESSAGE);
        mDialogMessage = TextUtils.isEmpty(dialogMessage) ? activity.getString(R.string.progress_dialog_message) : dialogMessage;
    }

    @Override
    protected void onPreExecute() {
        DialogFragmentFactory.showProgressDialog(mActivity, mDialogMessage, mDialogStyle, PROGRESS_DIALOG_FRAGMENT_TAG);
    }

    @Override
    protected T doInBackground(Void... params) {
        return mTask.execute(mProgressListener);
    }

    @Override
    protected void onPostExecute(T result) {
        ProgressDialogFragment dialogFragment = (ProgressDialogFragment) mActivity.getSupportFragmentManager().findFragmentByTag(PROGRESS_DIALOG_FRAGMENT_TAG);
        if (dialogFragment != null) dialogFragment.dismissAllowingStateLoss();
        mActivity.finish();
    }

    private final DBProcessProgressListener mProgressListener = new DBProcessProgressListener() {

        @Override
        public void onProgress(final int progress, final int max) {
            Log.v(TAG, "onProgress: " + progress + "/" + max);
            mActivity.runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    ProgressDialogFragment fragment = (ProgressDialogFragment) mActivity.getSupportFragmentManager().findFragmentByTag(
                            PROGRESS_DIALOG_FRAGMENT_TAG);
                    if (fragment != null) {
                        fragment.setProgress(progress, max);
                    }
                }
            });
        }
    };

}
