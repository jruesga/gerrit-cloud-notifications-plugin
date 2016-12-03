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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.ruesga.gerrit.plugins.fcm.DatabaseManager;
import com.ruesga.gerrit.plugins.fcm.rest.CloudNotificationInfo;
import com.ruesga.gerrit.plugins.fcm.rest.CloudNotificationInput;
import com.google.gerrit.server.CurrentUser;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class PostToken
        implements RestModifyView<DeviceResource, CloudNotificationInput> {

    private final Provider<CurrentUser> self;
    private final DatabaseManager db;
    private final SimpleDateFormat formatter;

    @Inject
    public PostToken(
            Provider<CurrentUser> self,
            DatabaseManager db) {
        super();
        this.self = self;
        this.db = db;

        formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public CloudNotificationInfo apply(
            DeviceResource rsrc, CloudNotificationInput input)
            throws BadRequestException {
        // Request are only valid from the current authenticated user
        if (self.get() == null || self.get() != rsrc.getUser()) {
            throw new BadRequestException("invalid account!");
        }

        // Check request parameters
        if (input.token == null || input.token.isEmpty()) {
            throw new BadRequestException("token is empty!");
        }

        final String registeredOn;
        synchronized (formatter) {
            registeredOn = formatter.format(new Date());
        }

        // Create or update the notification
        CloudNotificationInfo notification = db.getCloudNotification(
                self.get().getAccountId().get(),
                rsrc.getDevice(), input.token);
        if (notification == null) {
            notification = new CloudNotificationInfo();
            notification.device = rsrc.getDevice();
            notification.token = input.token;
        }
        notification.registeredOn = registeredOn;
        notification.events = input.events;
        notification.responseMode = input.responseMode;

        // Persist the notification
        db.registerCloudNotification(
                self.get().getAccountId().get(), notification);

        return notification;
    }
}
