/*
 * Copyright (C) 2016 Jorge Ruesga
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
package com.ruesga.gerrit.plugins.fcm.handlers;

import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.inject.Inject;
import com.ruesga.gerrit.plugins.fcm.Configuration;
import com.ruesga.gerrit.plugins.fcm.DatabaseManager;
import com.ruesga.gerrit.plugins.fcm.workers.FcmUploaderWorker;

public class LifeCycleHandler implements LifecycleListener {

    private final FcmUploaderWorker uploader;

    @Inject
    public LifeCycleHandler(
            Configuration config,
            DatabaseManager db,
            FcmUploaderWorker uploader) {
        super();
        this.uploader = uploader;
    }

    @Override
    public void start() {
        this.uploader.create();
    }

    @Override
    public void stop() {
        this.uploader.shutdown();
    }

}
