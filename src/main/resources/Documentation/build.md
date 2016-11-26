Build
=====

This plugin is built with Buck.

Build in Gerrit tree
--------------------

Issue this commands to build the plugin inside the Gerrit's source tree:

```
  git clone https://gerrit.googlesource.com/gerrit
  cd gerrit
  git submodule init
  git submodule update
  git clone https://github.com/jruesga/gerrit-cloud-notifications-plugin.git plugins/cloud-notifications
  buck build plugins/cloud-notifications
```

The output is created in

```
  buck-out/gen/plugins/cloud-notifications/cloud-notifications.jar
```

Check out the Gerrit Plugin API [documentation](https://gerrit-review.googlesource.com/Documentation/dev-buck.html#_extension_and_plugin_api_jar_files)
