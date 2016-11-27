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
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.ruesga.gerrit.plugins.fcm.DatabaseManager;
import com.ruesga.gerrit.plugins.fcm.rest.CloudNotificationEvents;
import com.ruesga.gerrit.plugins.fcm.rest.CloudNotificationInfo;
import com.ruesga.gerrit.plugins.fcm.rest.CloudNotificationResponseMode;
import com.ruesga.gerrit.plugins.fcm.server.PostCloudNotification.Input;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.account.AccountResource;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class PostCloudNotification
        implements RestModifyView<AccountResource, Input> {

    public static class Input {
        /**
         * A Firebase Cloud Messaging registered device identification.
         */
        @SerializedName("deviceId") public String deviceId;

        /**
         * A device token that unique identifies the server/account in the device.
         */
        @SerializedName("token") public String token;

        /**
         * A bitwise flag to indicate which events to notify.
         * @see CloudNotificationEvents
         */
        @SerializedName("events") public int events;

        /**
         * Firebase response mode.
         * @see CloudNotificationResponseMode
         */
        @SerializedName("responseMode")
        public CloudNotificationResponseMode responseMode =
                CloudNotificationResponseMode.BOTH;
    }

    private final Provider<CurrentUser> self;
    private final DatabaseManager db;

    @Inject
    public PostCloudNotification(
            Provider<CurrentUser> self,
            DatabaseManager db) {
        super();
        this.self = self;
        this.db = db;
    }

    @Override
    public CloudNotificationInfo apply(AccountResource acct, Input input)
            throws BadRequestException {
        // Request are only valid from the current authenticated user
        if (self.get() != acct.getUser()) {
            throw new BadRequestException("invalid account!");
        }

        // Check request parameters
        if (input.deviceId == null || input.deviceId.isEmpty()) {
            throw new BadRequestException("deviceId is empty!");
        }
        if (input.token == null || input.token.isEmpty()) {
            throw new BadRequestException("token is empty!");
        }

        // Create or update the notification
        CloudNotificationInfo notification = db.getCloudNotification(
                self.get().getUserName(), input.deviceId, input.token);
        if (notification == null) {
            notification = new CloudNotificationInfo();
            notification.deviceId = input.deviceId;
            notification.token = input.token;
        }
        notification.registeredOn = System.currentTimeMillis();
        notification.events = input.events;
        notification.responseMode = input.responseMode;

        // Persist the notification
        db.registerCloudNotification(
                self.get().getUserName(), notification);

        return notification;
    }
}
