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
package com.ruesga.gerrit.plugins.fcm.rest;

import com.google.gson.annotations.SerializedName;

public class CloudNotificationInfo {
    /**
     * A Firebase Cloud Messaging registered device identification.
     */
    @SerializedName("deviceId") public String deviceId;

    /**
     * When the device was registered.
     */
    @SerializedName("registeredOn") public long registeredOn;

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
    @SerializedName("responseMode") public CloudNotificationResponseMode responseMode = CloudNotificationResponseMode.BOTH;
}
