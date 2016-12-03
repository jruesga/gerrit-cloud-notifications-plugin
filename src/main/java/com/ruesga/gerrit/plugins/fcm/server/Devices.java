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
package com.ruesga.gerrit.plugins.fcm.server;

import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.ChildCollection;
import com.google.gerrit.extensions.restapi.IdString;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.RestView;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.account.AccountResource;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class Devices
        implements ChildCollection<AccountResource, DeviceResource> {

    private final DynamicMap<RestView<DeviceResource>> views;
    private final ListDevices list;
    private final Provider<CurrentUser> self;

    @Inject
    public Devices(
            DynamicMap<RestView<DeviceResource>> views,
            ListDevices list,
            Provider<CurrentUser> self) {
        super();
        this.views = views;
        this.list = list;
        this.self = self;
    }

    @Override
    public RestView<AccountResource> list()
            throws ResourceNotFoundException, AuthException {
        return this.list;
    }

    @Override
    public DeviceResource parse(AccountResource rsrc, IdString id)
            throws ResourceNotFoundException, Exception {
        if (self.get() == null || self.get() != rsrc.getUser()) {
            throw new ResourceNotFoundException();
        }

        return new DeviceResource(rsrc.getUser(), id.get());
    }

    @Override
    public DynamicMap<RestView<DeviceResource>> views() {
        return this.views;
    }

}
