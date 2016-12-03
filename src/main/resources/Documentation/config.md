Configuration
=============

This plugin provides a wizard configuration that will you ask
for plugin configuration parameters on install.

Manual configuration
--------------------

You can manual configure the plugin by adding the plugin properties
described below in this document in the gerrit.config file.

```
[plugin "cloud-notifications"]
        serverUrl = https://fcm.googleapis.com/fcm/send
        serverToken = <SERVER_API_KEY>
        databasePath = <DATABASE_LOCATION_PATH>
```

Plugin parameters
--------------------

* serverUrl: The Firebase Cloud Messaging backend url.
Default: https://fcm.googleapis.com/fcm/send

* serverToken: Your Firebase Cloud Messaging server token. You can
grab it from your Firebase project console, in Configuration > Cloud
Messaging > Server key

* databasePath: The path to where to store the plugin database. Leave
empty to use the default path ($gerrit/data/cloud-notifications/cloud-notifications.h2.db)
