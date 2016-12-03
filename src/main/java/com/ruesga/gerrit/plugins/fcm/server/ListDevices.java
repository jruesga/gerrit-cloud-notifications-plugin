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

import java.util.List;

import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.account.AccountResource;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class ListDevices implements RestReadView<AccountResource> {
    private final Provider<CurrentUser> self;

    @Inject
    public ListDevices(Provider<CurrentUser> self) {
        super();
        this.self = self;
    }

    @Override
    public List<DeviceResource> apply(AccountResource rsrc)
            throws BadRequestException {
        if (self.get() == null || self.get() != rsrc.getUser()) {
            throw new BadRequestException("invalid account!");
        }

        // Since return devices information from account, can lead to
        // tokens leak to different apps, just avoid it. Just return
        // empty information.
        throw new BadRequestException("unsupported!");
    }
}
