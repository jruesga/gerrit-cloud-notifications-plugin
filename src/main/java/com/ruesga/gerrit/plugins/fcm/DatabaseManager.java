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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.extensions.annotations.PluginData;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ruesga.gerrit.plugins.fcm.rest.CloudNotificationInfo;

@Singleton
public class DatabaseManager {

    private static final Logger log =
            LoggerFactory.getLogger(Configuration.class);

    private static final String DATABASE_NAME = "cloud-notifications.h2.db";

    private final File dbFile;
    private final String pluginName;
    private final Gson gson;
    private JdbcConnectionPool connectionPool;

    @Inject
    public DatabaseManager(
            @PluginName String pluginName,
            @PluginData java.nio.file.Path path,
            Configuration cfg) {
        this.pluginName = pluginName;
        this.gson = new GsonBuilder().create();
        if (cfg.databasePath != null && !cfg.databasePath.isEmpty()) {
            this.dbFile = new File(cfg.databasePath);
        } else {
            this.dbFile = new File(path.toFile(), DATABASE_NAME);
        }

        boolean canWrite = (this.dbFile.exists() && this.dbFile.canWrite())
                || (this.dbFile.getParentFile() != null
                && this.dbFile.getParentFile().canWrite());
        if (!canWrite) {
            log.warn(String.format(
                    "[%s] Database is not writeable.", pluginName));
            throw new IllegalArgumentException("Database is not writeable: "
                    + this.dbFile.getAbsolutePath());
        }
    }

    public void initialize() {
        log.info(String.format("[%s] Initialize database [%s]...",
                pluginName, this.dbFile.getAbsolutePath()));

        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:" + this.dbFile.getAbsolutePath());
        this.connectionPool = JdbcConnectionPool.create(ds);
        createDatabaseIfNeeded();
    }

    public void shutdown() {
        this.connectionPool.dispose();
    }

    public CloudNotificationInfo getCloudNotification(
            String userName, String deviceId, String token) {
        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            conn = this.connectionPool.getConnection();
            st = conn.prepareStatement("select * from notifications where " +
                    "user = ? and device = ? and token = ?");
            st.setString(1, userName);
            st.setString(2, deviceId);
            st.setString(3, token);
            rs = st.executeQuery();
            if (rs.next()) {
                return gson.fromJson(
                        rs.getString("data"), CloudNotificationInfo.class);
            }
        } catch (SQLException ex) {
            log.warn(String.format(
                    "[%s] Failed to access notifications database",
                    this.pluginName), ex);
        } finally {
            safelyCloseResources(conn, st, rs);
        }

        return null;
    }

    public List<CloudNotificationInfo> getCloudNotifications(String userName) {
        List<CloudNotificationInfo> notifications = new ArrayList<>();
        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            conn = this.connectionPool.getConnection();
            st = conn.prepareStatement("select * from notifications where " +
                    "user = ?");
            st.setString(1, userName);
            rs = st.executeQuery();
            while (rs.next()) {
                notifications.add(gson.fromJson(
                        rs.getString("data"), CloudNotificationInfo.class));
            }
        } catch (SQLException ex) {
            log.warn(String.format(
                    "[%s] Failed to access notifications database",
                    this.pluginName), ex);
        } finally {
            safelyCloseResources(conn, st, rs);
        }

        return notifications;
    }

    public void registerCloudNotification(
            String userName, CloudNotificationInfo notification) {
        Connection conn = null;
        PreparedStatement st = null;
        try {
            conn = this.connectionPool.getConnection();
            st = conn.prepareStatement("merge into notifications (user, " +
                    "device, token, data) KEY(user, device, token) " +
                    "VALUES (?, ?, ?, ?)");
            st.setString(1, userName);
            st.setString(2, notification.deviceId);
            st.setString(3, notification.token);
            st.setString(4, gson.toJson(notification));
            st.execute();
        } catch (SQLException ex) {
            log.warn(String.format(
                    "[%s] Failed to update device: %s",
                    this.pluginName, notification.deviceId), ex);
        } finally {
            safelyCloseResources(conn, st, null);
        }
    }

    public void unregisterCloudNotification(
            String userName, String deviceId, String token) {
        Connection conn = null;
        PreparedStatement st = null;
        try {
            conn = this.connectionPool.getConnection();
            st = conn.prepareStatement("delete from notifications where " +
                    "user = ? and device = ? and token = ?");
            st.setString(1, userName);
            st.setString(2, deviceId);
            st.setString(3, token);
            st.execute();
        } catch (SQLException ex) {
            log.warn(String.format(
                    "[%s] Failed to delete device: %s",
                    this.pluginName, deviceId), ex);
        } finally {
            safelyCloseResources(conn, st, null);
        }
    }

    private void createDatabaseIfNeeded() {
        Connection conn = null;
        Statement st = null;
        try {
            conn = this.connectionPool.getConnection();
            st = conn.createStatement();
            st.execute(
                    "create table if not exists notifications (" +
                    "user varchar(50) NOT NULL, " +
                    "device varchar(250) NOT NULL, " +
                    "token varchar(50) NOT NULL, " +
                    "data varchar(4000) NOT NULL," +
                    "primary key (user, device, token))");
        } catch (SQLException ex) {
            // The table exists. Ignore
        } finally {
            safelyCloseResources(conn, st, null);
        }
    }

    private void safelyCloseResources(
            Connection conn, Statement st, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (Exception ex) {
                // Ignore
            }
        }
        if (st != null) {
            try {
                st.close();
            } catch (Exception ex) {
                // Ignore
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (Exception ex) {
                // Ignore
            }
        }
    }
}
