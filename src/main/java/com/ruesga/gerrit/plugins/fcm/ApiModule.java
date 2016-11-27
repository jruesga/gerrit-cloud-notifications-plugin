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
package com.ruesga.gerrit.plugins.fcm;

import static com.google.gerrit.server.account.AccountResource.ACCOUNT_KIND;
import static com.ruesga.gerrit.plugins.fcm.server.CloudNotification.CLOUD_NOTIFICATION_KIND;

import com.google.gerrit.common.EventListener;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.restapi.RestApiModule;
import com.google.inject.Scopes;
import com.ruesga.gerrit.plugins.fcm.DatabaseManager;
import com.ruesga.gerrit.plugins.fcm.handlers.EventHandler;
import com.ruesga.gerrit.plugins.fcm.handlers.LifeCycleHandler;
import com.ruesga.gerrit.plugins.fcm.server.DeleteCloudNotification;
import com.ruesga.gerrit.plugins.fcm.server.GetCloudNotification;
import com.ruesga.gerrit.plugins.fcm.server.PostCloudNotification;
import com.ruesga.gerrit.plugins.fcm.workers.FcmUploaderWorker;


public class ApiModule extends RestApiModule {

    public static final String ENTRY_POINT = "cloud-notifications";

    @Override
    protected void configure() {
        bind(DatabaseManager.class).in(Scopes.SINGLETON);
        bind(Configuration.class).in(Scopes.SINGLETON);
        bind(FcmUploaderWorker.class).in(Scopes.SINGLETON);

        // Configure listener handlers
        DynamicSet.bind(binder(), LifecycleListener.class)
                .to(LifeCycleHandler.class);
        DynamicSet.bind(binder(), EventListener.class)
                .to(EventHandler.class);

        // Configure the Rest API
        DynamicMap.mapOf(binder(), CLOUD_NOTIFICATION_KIND);
        get(CLOUD_NOTIFICATION_KIND).to(GetCloudNotification.class);
        post(ACCOUNT_KIND, ENTRY_POINT).to(PostCloudNotification.class);
        delete(CLOUD_NOTIFICATION_KIND).to(DeleteCloudNotification.class);
    }
}