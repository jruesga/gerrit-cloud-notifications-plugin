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

import static com.google.gerrit.server.account.AccountResource.ACCOUNT_KIND;
import static com.ruesga.gerrit.plugins.fcm.server.DeviceResource.DEVICE_KIND;
import static com.ruesga.gerrit.plugins.fcm.server.TokenResource.TOKEN_KIND;

import com.google.gerrit.extensions.events.ChangeAbandonedListener;
import com.google.gerrit.extensions.events.ChangeMergedListener;
import com.google.gerrit.extensions.events.ChangeRestoredListener;
import com.google.gerrit.extensions.events.ChangeRevertedListener;
import com.google.gerrit.extensions.events.CommentAddedListener;
import com.google.gerrit.extensions.events.DraftPublishedListener;
import com.google.gerrit.extensions.events.HashtagsEditedListener;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.extensions.events.ReviewerAddedListener;
import com.google.gerrit.extensions.events.ReviewerDeletedListener;
import com.google.gerrit.extensions.events.RevisionCreatedListener;
import com.google.gerrit.extensions.events.TopicEditedListener;
import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.restapi.RestApiModule;
import com.google.inject.Scopes;
import com.ruesga.gerrit.plugins.fcm.DatabaseManager;
import com.ruesga.gerrit.plugins.fcm.handlers.ChangeAbandonedEventHandler;
import com.ruesga.gerrit.plugins.fcm.handlers.ChangeMergedEventHandler;
import com.ruesga.gerrit.plugins.fcm.handlers.ChangeRestoredEventHandler;
import com.ruesga.gerrit.plugins.fcm.handlers.ChangeRevertedEventHandler;
import com.ruesga.gerrit.plugins.fcm.handlers.CommentAddedEventHandler;
import com.ruesga.gerrit.plugins.fcm.handlers.DraftPublishedEventHandler;
import com.ruesga.gerrit.plugins.fcm.handlers.HashtagsEditedEventHandler;
import com.ruesga.gerrit.plugins.fcm.handlers.LifeCycleHandler;
import com.ruesga.gerrit.plugins.fcm.handlers.ReviewerAddedEventHandler;
import com.ruesga.gerrit.plugins.fcm.handlers.ReviewerDeletedEventHandler;
import com.ruesga.gerrit.plugins.fcm.handlers.RevisionCreatedEventHandler;
import com.ruesga.gerrit.plugins.fcm.handlers.TopicEditedEventHandler;
import com.ruesga.gerrit.plugins.fcm.server.DeleteToken;
import com.ruesga.gerrit.plugins.fcm.server.Devices;
import com.ruesga.gerrit.plugins.fcm.server.GetToken;
import com.ruesga.gerrit.plugins.fcm.server.PostToken;
import com.ruesga.gerrit.plugins.fcm.server.Tokens;
import com.ruesga.gerrit.plugins.fcm.workers.FcmUploaderWorker;


public class ApiModule extends RestApiModule {

    public static final String DEVICES_ENTRY_POINT = "devices";
    public static final String TOKEN_ENTRY_POINT = "tokens";

    @Override
    protected void configure() {
        bind(DatabaseManager.class).in(Scopes.SINGLETON);
        bind(Configuration.class).in(Scopes.SINGLETON);
        bind(FcmUploaderWorker.class).in(Scopes.SINGLETON);

        // Configure listener handlers
        DynamicSet.bind(binder(), LifecycleListener.class)
                .to(LifeCycleHandler.class);
        DynamicSet.bind(binder(), ChangeAbandonedListener.class)
                .to(ChangeAbandonedEventHandler.class);
        DynamicSet.bind(binder(), ChangeMergedListener.class)
                .to(ChangeMergedEventHandler.class);
        DynamicSet.bind(binder(), ChangeRestoredListener.class)
                .to(ChangeRestoredEventHandler.class);
        DynamicSet.bind(binder(), ChangeRevertedListener.class)
                .to(ChangeRevertedEventHandler.class);
        DynamicSet.bind(binder(), CommentAddedListener.class)
                .to(CommentAddedEventHandler.class);
        DynamicSet.bind(binder(), DraftPublishedListener.class)
                .to(DraftPublishedEventHandler.class);
        DynamicSet.bind(binder(), HashtagsEditedListener.class)
                .to(HashtagsEditedEventHandler.class);
        DynamicSet.bind(binder(), ReviewerDeletedListener.class)
                .to(ReviewerDeletedEventHandler.class);
        DynamicSet.bind(binder(), ReviewerAddedListener.class)
                .to(ReviewerAddedEventHandler.class);
        DynamicSet.bind(binder(), RevisionCreatedListener.class)
                .to(RevisionCreatedEventHandler.class);
        DynamicSet.bind(binder(), TopicEditedListener.class)
                .to(TopicEditedEventHandler.class);

        // Configure the Rest API
        DynamicMap.mapOf(binder(), DEVICE_KIND);
        DynamicMap.mapOf(binder(), TOKEN_KIND);
        child(ACCOUNT_KIND, DEVICES_ENTRY_POINT).to(Devices.class);
        child(DEVICE_KIND, TOKEN_ENTRY_POINT).to(Tokens.class);
        get(TOKEN_KIND).to(GetToken.class);
        post(DEVICE_KIND, TOKEN_ENTRY_POINT).to(PostToken.class);
        delete(TOKEN_KIND).to(DeleteToken.class);
    }
}