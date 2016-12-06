#
# Copyright (C) 2016 Jorge Ruesga
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

include_defs('//bucklets/gerrit_plugin.bucklet')
include_defs('//bucklets/java_sources.bucklet')
include_defs('//bucklets/maven_jar.bucklet')

SOURCES = glob(['src/main/java/**/*.java'])
RESOURCES = glob(['src/main/resources/**/*'])

PROVIDED_DEPS = [
  '//lib:gson',
  ':h2'
]

DEPS = [
]

gerrit_plugin(
  name = 'cloud-notifications',
  srcs = SOURCES,
  resources = RESOURCES,
  manifest_entries = [
    'Gerrit-PluginName: cloud-notifications',
    'Gerrit-ApiType: plugin',
    'Gerrit-ApiVersion: 2.14-SNAPSHOT',
    'Gerrit-Module: com.ruesga.gerrit.plugins.fcm.ApiModule',
    'Gerrit-InitStep: com.ruesga.gerrit.plugins.fcm.InitStep',
    'Implementation-Title: Firebase Cloud Notifications Plugin',
    'Implementation-Vendor: Jorge Ruesga',
    'Implementation-URL: https://github.com/jruesga/gerrit-cloud-notifications-plugin',
    'Implementation-Version: 2.14-SNAPSHOT'
  ],
  deps = DEPS,
  provided_deps = PROVIDED_DEPS
)

java_sources(
  name = 'cloud-notifications-sources',
  srcs = SOURCES + RESOURCES
)

maven_jar(
  name = 'h2',
  id = 'com.h2database:h2:1.3.176',
  license = 'Apache2.0',
  exclude_java_sources = True,
  visibility = [],
)

