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
package ca.rmen.android.networkmonitor.app.dbops.backend;

import androidx.annotation.Nullable;

import ca.rmen.android.networkmonitor.app.dbops.ProgressListener;

/**
 * Performs a long-running, cancelable task (currently reading/writing on the DB).
 */
public interface DBOperation {

    /**
     * Execute the long-running operation.  This should be called on a background thread.
     * @param listener if given, must be notified of the progress of the task.
     */
    void execute(@Nullable ProgressListener listener);

    /**
     * Cancel the long-running operation.  If it is in progress, it should finish as soon as possible.
     */
    void cancel();
}
