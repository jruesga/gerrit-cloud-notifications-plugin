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
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    private static class SubmitNotification {
        int accountId;
        String device;
        String token;
        FcmRequestInfo request;
        int attempt;
    }

    private final String pluginName;
    private final Configuration config;
    private final DatabaseManager db;
    private final Gson gson;
    private ExecutorService executor;
    private ScheduledExecutorService delayedExecutor;

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
        this.delayedExecutor = Executors.newScheduledThreadPool(50);
    }

    public void shutdown() {
        this.executor.shutdown();
        this.delayedExecutor.shutdownNow();
    }

    public void notifyTo(final List<Integer> notifiedAccounts,
            final Notification notification) {
        if (!config.isEnabled()) {
            return;
        }

        for (final Integer accountId : notifiedAccounts) {
            this.executor.submit(new Runnable() {
                @Override
                public void run() {
                    asyncNotify(accountId, notification);
                }
            });
        }
    }

    private void asyncNotify(int accountId, Notification notification) {
        List<CloudNotificationInfo> notifications =
                db.getCloudNotifications(accountId);
        for (CloudNotificationInfo to : notifications) {
            if ((notification.event | to.events) == to.events) {
                Notification what = (Notification) notification.clone();
                what.token = to.token;

                sendNotification(createRequest(accountId, to, what));
            }
        }
    }

    private synchronized void sendNotification(SubmitNotification submit) {
        try {
            String data = gson.toJson(submit.request);
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "[%] Sending fcm notification: %s", pluginName, data));
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

                // Process the server response
                processResponse(conn, submit, gson.fromJson(
                        response.toString(), FcmResponseInfo.class));

            } else if (responseCode == 500) {
                // Retry
                retryAfter(conn, submit);

            } else {
                log.warn(String.format(
                        "[%s] Failed to send notification to device %s. code: %d",
                            pluginName, submit.request.to, responseCode));
            }

        } catch (Throwable e) {
            log.warn(String.format(
                    "[%s] Failed to send notification to device %s",
                        pluginName, submit.request.to), e);
        }
    }

    private SubmitNotification createRequest(
            int accountId, CloudNotificationInfo to, Notification what) {
        FcmRequestInfo request = new FcmRequestInfo();
        request.to = to.device;
        request.timeToLive = 28800; // 8 hours
        if (to.responseMode.equals(CloudNotificationResponseMode.NOTIFICATION)
                || to.responseMode.equals(CloudNotificationResponseMode.BOTH)) {
            request.notification = new FcmRequestNotificationInfo();
            request.notification.title = "Gerrit notification";
            request.notification.body = what.body;
        }
        if (to.responseMode.equals(CloudNotificationResponseMode.DATA)
                || to.responseMode.equals(CloudNotificationResponseMode.BOTH)) {
            request.data = what;
        }

        SubmitNotification submit = new SubmitNotification();
        submit.accountId = accountId;
        submit.device = to.device;
        submit.token = to.token;
        submit.request = request;
        return submit;
    }

    private void processResponse(HttpURLConnection conn,
            SubmitNotification submit, FcmResponseInfo response) {
        if (response.failure > 0 && !response.results.isEmpty()) {
            FcmResponseResultInfo result = response.results.get(0);
            if (result.error != null) {
                switch (result.error) {
                case "Unavailable":
                case "InternalServerError":
                    // Retry
                    retryAfter(conn, submit);
                    break;

                case "NotRegistered":
                    // Remove this client from the database
                    if (log.isDebugEnabled()) {
                        log.debug("[%] %d - %s - %s is not registered. " +
                                "Remove from db.",
                                pluginName, submit.accountId,
                                submit.device,
                                submit.token);
                    }
                    db.unregisterCloudNotification(
                            submit.accountId,
                            submit.device,
                            submit.token);
                    break;

                case "DeviceMessageRateExceeded":
                    // TODO we should stop sending messages to this device
                    // or we will get banned. This shouldn't happen
                    // normally. Need to thought how to handle this.
                    break;

                default:
                    break;
                }
            }
        }

        // The message was successfully sent
    }

    private void retryAfter(
            HttpURLConnection conn, final SubmitNotification submit) {
        submit.attempt++;

        // Is Retry-After header present?
        int retryAfter = 0;
        try {
            Map<String, List<String>> headers = conn.getHeaderFields();
            if (headers.containsKey("Retry-After")) {
                retryAfter = Integer.parseInt(
                        headers.get("Retry-After").get(0));
            }
        } catch (Exception ex) {
            // Ignore
        }
        if (retryAfter == 0) {
            // If Retry-After isn't present, then use our
            // own exponential back-off timeout (in seconds)
            retryAfter = submit.attempt * 30;
        }

        if (log.isDebugEnabled()) {
            log.debug("[%] Retry fcm notification to %s after %d seconds",
                    pluginName, submit.request.to, retryAfter);
        }
        this.delayedExecutor.schedule(new Runnable() {
            @Override
            public void run() {
                sendNotification(submit);
            }
        }, retryAfter, TimeUnit.SECONDS);
    }

}
