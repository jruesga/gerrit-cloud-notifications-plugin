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
package com.ruesga.gerrit.plugins.fcm.handlers;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.events.ChangeMergedListener;
import com.google.gerrit.reviewdb.client.AccountProjectWatch.NotifyType;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.IdentifiedUser.GenericFactory;
import com.google.gerrit.server.config.AllProjectsName;
import com.google.gerrit.server.query.change.ChangeQueryBuilder;
import com.google.gerrit.server.query.change.ChangeQueryProcessor;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.ruesga.gerrit.plugins.fcm.messaging.Notification;
import com.ruesga.gerrit.plugins.fcm.rest.CloudNotificationEvents;
import com.ruesga.gerrit.plugins.fcm.workers.FcmUploaderWorker;

public class ChangeMergedEventHandler extends EventHandler
        implements ChangeMergedListener {

    @Inject
    public ChangeMergedEventHandler(
            @PluginName String pluginName,
            FcmUploaderWorker uploader,
            AllProjectsName allProjectsName,
            ChangeQueryBuilder cqb,
            ChangeQueryProcessor cqp,
            Provider<ReviewDb> reviewdb,
            GenericFactory identifiedUserFactory) {
        super(pluginName,
                uploader,
                allProjectsName,
                cqb, cqp,
                reviewdb,
                identifiedUserFactory);
    }

    protected int getEventType() {
        return CloudNotificationEvents.CHANGE_MERGED_EVENT;
    }

    protected NotifyType getNotifyType() {
        return NotifyType.SUBMITTED_CHANGES;
    }

    @Override
    public void onChangeMerged(Event event) {
        Notification notification = createNotification(event);
        notification.body = formatAccount(event.getWho())
                + " merged this change";

        notify(notification, event);
    }

}
