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

import com.google.gerrit.extensions.restapi.RestView;
import com.google.inject.TypeLiteral;

public class TokenResource extends DeviceResource {
    public static final TypeLiteral<RestView<TokenResource>> TOKEN_KIND =
            new TypeLiteral<RestView<TokenResource>>() {};

    private final String token;

    public TokenResource(DeviceResource rsrc, String token) {
      super(rsrc.getUser(), rsrc.getDevice());
      this.token = token;
    }

    public String getToken() {
        return token;
    }
}
