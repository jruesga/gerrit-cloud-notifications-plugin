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

import org.eclipse.jgit.lib.Config;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.pgm.init.api.AllProjectsConfig;
import com.google.gerrit.pgm.init.api.ConsoleUI;
import com.google.inject.Inject;

public class InitStep implements com.google.gerrit.pgm.init.api.InitStep {
    private final String pluginName;
    private final ConsoleUI ui;
    private final AllProjectsConfig allProjectsConfig;

    @Inject
    public InitStep(@PluginName String pluginName, ConsoleUI ui,
        AllProjectsConfig allProjectsConfig) {
      this.pluginName = pluginName;
      this.ui = ui;
      this.allProjectsConfig = allProjectsConfig;
    }

    @Override
    public void run() throws Exception {
    }

    @Override
    public void postRun() throws Exception {
        ui.message("\n");
        ui.header(pluginName + " Settings");
        Config cfg = allProjectsConfig.load().getConfig();

        String serverUrl = ui.readString(
                Configuration.DEFAULT_SERVER_URL, "Firebase Server Url");
        String serverToken = ui.readString(
                "", "Firebase Server Token");
        String databasePath = ui.readString(
                "", "Database path (leave empty for default)");

        if (serverUrl != null && serverUrl.isEmpty()) {
            serverUrl = Configuration.DEFAULT_SERVER_URL;
        }
        cfg.setString("plugin", pluginName,
                Configuration.PROP_SERVER_TOKEN, serverUrl);
        if (serverToken != null && serverToken.isEmpty()) {
            cfg.unset("plugin", pluginName, Configuration.PROP_SERVER_TOKEN);
        } else {
            cfg.setString("plugin", pluginName,
                    Configuration.PROP_SERVER_TOKEN, serverToken);
        }
        if (databasePath != null && databasePath.isEmpty()) {
            cfg.unset("plugin", pluginName, Configuration.PROP_DATABASE_PATH);
        } else {
            cfg.setString("plugin", pluginName,
                    Configuration.PROP_DATABASE_PATH, databasePath);
        }

        allProjectsConfig.save(pluginName, pluginName + " initialized");
    }

}
