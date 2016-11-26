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
package com.ruesga.gerrit.plugins.fcm.server;

import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.ruesga.gerrit.plugins.fcm.DatabaseManager;
import com.ruesga.gerrit.plugins.fcm.rest.CloudNotificationInfo;
import com.google.gerrit.server.CurrentUser;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class GetCloudNotification implements RestReadView<CloudNotification> {

    private final Provider<CurrentUser> self;
    private final DatabaseManager db;

    @Inject
    public GetCloudNotification(
            Provider<CurrentUser> self,
            DatabaseManager db) {
        super();
        this.self = self;
        this.db = db;
    }

    @Override
    public CloudNotificationInfo apply(CloudNotification notification)
            throws BadRequestException, ResourceNotFoundException {
        // Request are only valid from the current authenticated user
        if (self.get() != notification.getUser()) {
            throw new BadRequestException("invalid account!");
        }

        // Obtain from database
        CloudNotificationInfo notif = db.getCloudNotification(
                self.get().getUserName(), notification.getDeviceId());
        if (notif == null) {
            throw new ResourceNotFoundException();
        }
        return notif;
    }
  }
