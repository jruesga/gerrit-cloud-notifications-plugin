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

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.h2.mvstore.MVStore;
import org.h2.mvstore.db.TransactionStore;
import org.h2.mvstore.db.TransactionStore.Transaction;
import org.h2.mvstore.db.TransactionStore.TransactionMap;
import org.h2.mvstore.type.StringDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.extensions.annotations.PluginData;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ruesga.gerrit.plugins.fcm.adapters.UtcDateAdapter;
import com.ruesga.gerrit.plugins.fcm.rest.CloudNotificationInfo;

@Singleton
public class DatabaseManager {

    private static final Logger log =
            LoggerFactory.getLogger(Configuration.class);

    private static final String DATABASE_NAME = "cloud-notifications.h2.db";

    private final File dbFile;
    private final String pluginName;
    private final Gson gson;

    @Inject
    public DatabaseManager(
            @PluginName String pluginName,
            @PluginData java.nio.file.Path path,
            Configuration cfg) {
        this.pluginName = pluginName;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new UtcDateAdapter())
                .create();
        if (cfg.databasePath != null && !cfg.databasePath.isEmpty()) {
            this.dbFile = new File(cfg.databasePath);
        } else {
            this.dbFile = new File(path.toFile(), DATABASE_NAME);
        }

        if (!this.dbFile.canWrite()) {
            log.warn(String.format(
                    "[%s] Database is not writeable.", pluginName));
            throw new IllegalArgumentException("Database is not writeable: "
                    + this.dbFile.getAbsolutePath());
        }

        initializeDatabase();
    }

    private TransactionStore openTransactionalStore() {
        return new TransactionStore(
                MVStore.open(this.dbFile.getAbsolutePath()));
    }

    private void initializeDatabase() {
        log.info(String.format("[%s] Initialize database ...", pluginName));

        MVStore store = MVStore.open(this.dbFile.getAbsolutePath());
        store.compactMoveChunks();
        store.close();
    }

    public CloudNotificationInfo getCloudNotification(
            String userName, String deviceId, String token) {
        CloudNotificationInfo notification = null;
        TransactionStore ts = openTransactionalStore();
        Transaction tx  = ts.begin();

        final String key = token + "|" + deviceId;
        TransactionMap<String, String> map = tx.openMap(userName,
                StringDataType.INSTANCE, StringDataType.INSTANCE);
        if (map.containsKey(key)) {
            notification = gson.fromJson(
                    map.get(key), CloudNotificationInfo.class);
            tx.commit();
        } else {
            tx.rollback();
        }

        ts.close();

        return notification;
    }

    public List<CloudNotificationInfo> getCloudNotifications(String userName) {
        List<CloudNotificationInfo> notifications = new ArrayList<>();
        TransactionStore ts = openTransactionalStore();
        Transaction tx  = ts.begin();

        TransactionMap<String, String> map = tx.openMap(userName,
                StringDataType.INSTANCE, StringDataType.INSTANCE);
        Iterator<String> it = map.keyIterator(null);
        while (it.hasNext()) {
            notifications.add(
                    gson.fromJson(
                            map.get(it.next()), CloudNotificationInfo.class));
        }

        if (!notifications.isEmpty()) {
            tx.commit();
        } else {
            tx.rollback();
        }

        ts.close();

        return notifications;
    }

    public void registerCloudNotification(
            String userName, CloudNotificationInfo notification) {
        TransactionStore ts = openTransactionalStore();
        Transaction tx  = ts.begin();

        TransactionMap<String, String> map = tx.openMap(userName,
                StringDataType.INSTANCE, StringDataType.INSTANCE);
        map.put(notification.deviceId, gson.toJson(notification));

        ts.close();
    }

    public void unregisterCloudNotification(
            String userName, String deviceId, String token) {
        TransactionStore ts = openTransactionalStore();
        Transaction tx  = ts.begin();

        final String key = token + "|" + deviceId;
        TransactionMap<String, String> map = tx.openMap(userName,
                StringDataType.INSTANCE, StringDataType.INSTANCE);
        if (map.containsKey(key)) {
            map.remove(key);
            tx.commit();
        } else {
            tx.rollback();
        }

        ts.close();
    }
}
