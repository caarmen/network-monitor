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
package ca.rmen.android.networkmonitor.app.dbops.ui;

import java.util.Arrays;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;

import ca.rmen.android.networkmonitor.Constants;
import ca.rmen.android.networkmonitor.R;
import ca.rmen.android.networkmonitor.app.dbops.ProgressListener;
import ca.rmen.android.networkmonitor.app.dbops.Task;
import ca.rmen.android.networkmonitor.app.dialog.DialogFragmentFactory;
import ca.rmen.android.networkmonitor.app.dialog.ProgressDialogFragment;
import ca.rmen.android.networkmonitor.util.Log;

/**
 * Executes a task in the background, displaying a progress dialog on the given activity during the task's execution.
 * Currently this is only used for operations on the db (import, export, clean...). If at some point we find
 * that we need some async task for some other types of operations, this class can be moved to a different package.
 */
public abstract class DBOpAsyncTask<T> extends AsyncTask<Void, Integer, T> {
    private static final String TAG = Constants.TAG + DBOpAsyncTask.class.getSimpleName();

    public static final String EXTRA_DIALOG_STYLE = "extra_dialog_style";
    public static final String EXTRA_DIALOG_MESSAGE = "extra_dialog_message";
    private static final String PROGRESS_DIALOG_FRAGMENT_TAG = "progress_dialog_fragment_tag";

    private final FragmentActivity mActivity;
    private final Task<T> mTask;
    private final int mDialogStyle;
    private final String mDialogMessage;

    public DBOpAsyncTask(FragmentActivity activity, Task<T> task, Bundle args) {
        Log.v(TAG, "Constructor: activity = " + activity + ", task = " + task);
        mActivity = activity;
        mTask = task;
        if (args == null) args = new Bundle();
        mDialogStyle = args.getInt(EXTRA_DIALOG_STYLE, ProgressDialog.STYLE_HORIZONTAL);
        String dialogMessage = args.getString(EXTRA_DIALOG_MESSAGE);
        mDialogMessage = TextUtils.isEmpty(dialogMessage) ? activity.getString(R.string.progress_dialog_message) : dialogMessage;
    }

    @Override
    protected void onPreExecute() {
        Log.v(TAG, "onPreExecute");
        DialogFragmentFactory.showProgressDialog(mActivity, mDialogMessage, mDialogStyle, PROGRESS_DIALOG_FRAGMENT_TAG);
    }

    @Override
    protected T doInBackground(Void... params) {
        Log.v(TAG, "doInBackground");
        return mTask.execute(mProgressListener);
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        Log.v(TAG, "onProgressUpdate: task  " + mTask + ", values = " + Arrays.toString(values));
        int progress = values[0];
        int max = values[1];
        ProgressDialogFragment fragment = (ProgressDialogFragment) mActivity.getSupportFragmentManager().findFragmentByTag(PROGRESS_DIALOG_FRAGMENT_TAG);
        if (mActivity.isFinishing()) {
            Log.v(TAG, "Activity " + mActivity + " finished when updating the dialog progress.  Monkey?");
            return;
        }
        if (fragment != null) {
            fragment.setProgress(progress, max);
        }
    }

    /**
     * Since this finishes the activity, superclasses should call to super at the end of their onPostExecute implementation.
     */
    @Override
    protected void onPostExecute(T result) {
        Log.v(TAG, "onPostExecute");
        ProgressDialogFragment dialogFragment = (ProgressDialogFragment) mActivity.getSupportFragmentManager().findFragmentByTag(PROGRESS_DIALOG_FRAGMENT_TAG);
        if (dialogFragment != null) dialogFragment.dismissAllowingStateLoss();
        if (mActivity.isFinishing()) {
            Log.v(TAG, "Activity " + mActivity + " finished before the task finished.  Monkey?");
        } else {
            mActivity.finish();
        }
    }

    private final ProgressListener mProgressListener = new ProgressListener() {

        @Override
        public void onProgress(final int progress, final int max) {
            Log.v(TAG, "onProgress: " + progress + "/" + max);
            publishProgress(progress, max);
        }
    };

}
