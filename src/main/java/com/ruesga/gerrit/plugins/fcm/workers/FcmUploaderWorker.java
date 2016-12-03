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
package com.ruesga.gerrit.plugins.fcm.workers;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ruesga.gerrit.plugins.fcm.Configuration;
import com.ruesga.gerrit.plugins.fcm.DatabaseManager;
import com.ruesga.gerrit.plugins.fcm.messaging.Notification;
import com.ruesga.gerrit.plugins.fcm.rest.CloudNotificationInfo;
import com.ruesga.gerrit.plugins.fcm.rest.CloudNotificationResponseMode;

@Singleton
public class FcmUploaderWorker {

    private static final Logger log =
            LoggerFactory.getLogger(FcmUploaderWorker.class);

    private final String pluginName;
    private final Configuration config;
    private final DatabaseManager db;
    private final Gson gson;
    private ExecutorService executor;

    @Inject
    public FcmUploaderWorker(
            @PluginName String pluginName,
            Configuration config,
            DatabaseManager db) {
        super();
        this.pluginName = pluginName;
        this.config = config;
        this.db = db;
        this.gson = new GsonBuilder().create();
    }

    public void create() {
        this.executor = Executors.newCachedThreadPool();
    }

    public void shutdown() {
        this.executor.shutdown();
    }

    public void notify(final List<String> notifiedUsers,
            final Notification notification) {
        if (!config.isEnabled()) {
            return;
        }

        for (final String user : notifiedUsers) {
            this.executor.submit(new Runnable() {
                @Override
                public void run() {
                    asyncNotify(user, notification);
                }
            });
        }
    }

    private void asyncNotify(String user, Notification notification) {
        List<CloudNotificationInfo> notifications =
                db.getCloudNotifications(user);
        for (CloudNotificationInfo to : notifications) {
            if ((notification.event | to.events) == to.events) {
                Notification what = (Notification) notification.clone();
                what.token = to.token;

                sendNotification(createRequest(to, what));
            }
        }
    }

    private synchronized void sendNotification(FcmRequestInfo request) {
        try {
            String data = gson.toJson(request);
            if (log.isDebugEnabled()) {
                log.debug("[%] Sending fcm notification: %s", pluginName, data);
            }

            URL url = new URL(config.serverUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty(
                    "Content-Type", "application/json");
            conn.setRequestProperty(
                    "Authorization", "key=" + config.serverToken);
            conn.setRequestProperty(
                    "Content-Length", Integer.toString(data.length()));

            DataOutputStream os = new DataOutputStream(conn.getOutputStream());
            try {
                os.write(data.getBytes());
                os.flush();
            } finally {
                try {
                    os.close();
                } catch (IOException ex) {
                    // Ignore
                }
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                StringBuilder response = new StringBuilder();
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                try {
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                } finally {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        // Ignore
                    }
                }

                processResponse(gson.fromJson(
                        response.toString(), FcmResponseInfo.class));
            } else {
                log.warn(String.format(
                        "[%s] Failed to send notification to device %s. code: %d",
                            pluginName, request.to, responseCode));
            }

        } catch (IOException e) {
            log.warn(String.format(
                    "[%s] Failed to send notification to device %s",
                        pluginName, request.to), e);
        }
    }

    private FcmRequestInfo createRequest(
            CloudNotificationInfo to, Notification what) {
        FcmRequestInfo request = new FcmRequestInfo();
        request.to = to.device;
        if (to.responseMode.equals(CloudNotificationResponseMode.NOTIFICATION)
                || to.responseMode.equals(CloudNotificationResponseMode.BOTH)) {
            request.notification = new FcmRequestNotificationInfo();
            request.notification.title = "FIXME";
            request.notification.body = "FIXME";
        }
        if (to.responseMode.equals(CloudNotificationResponseMode.DATA)
                || to.responseMode.equals(CloudNotificationResponseMode.BOTH)) {
            request.data = what;
        }
        return request;
    }

    private void processResponse(FcmResponseInfo response) {
        // TODO Handle response
    }
}
