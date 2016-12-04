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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class Configuration {

    private static final Logger log =
            LoggerFactory.getLogger(Configuration.class);

    public static final String DEFAULT_SERVER_URL =
            "https://fcm.googleapis.com/fcm/send";

    public static final String PROP_DATABASE_PATH = "databasePath";
    public static final String PROP_SERVER_URL = "serverUrl";
    public static final String PROP_SERVER_TOKEN = "serverToken";

    public final String databasePath;
    public final String serverToken;
    public final String serverUrl;

    @Inject
    public Configuration(
            @PluginName String pluginName,
            PluginConfigFactory cfgFactory) {
        PluginConfig cfg = cfgFactory.getFromGerritConfig(pluginName);
        this.databasePath = cfg.getString(PROP_DATABASE_PATH);
        this.serverToken = cfg.getString(PROP_SERVER_TOKEN);
        String serverUrl = cfg.getString(PROP_SERVER_URL);
        if (serverUrl == null || serverUrl.isEmpty()) {
            serverUrl = Configuration.DEFAULT_SERVER_URL;
        }
        this.serverUrl = serverUrl;

        if (!isEnabled()) {
            log.info(String.format("[%s] Plugin disabled.", pluginName));
        }
    }

    public boolean isEnabled() {
        return this.serverToken != null && !this.serverToken.isEmpty();
    }
}
