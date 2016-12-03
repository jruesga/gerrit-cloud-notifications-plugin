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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.common.EventListener;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.data.AccountAttribute;
import com.google.gerrit.server.data.ChangeAttribute;
import com.google.gerrit.server.data.PatchSetAttribute;
import com.google.gerrit.server.events.ChangeAbandonedEvent;
import com.google.gerrit.server.events.ChangeMergedEvent;
import com.google.gerrit.server.events.ChangeRestoredEvent;
import com.google.gerrit.server.events.CommentAddedEvent;
import com.google.gerrit.server.events.DraftPublishedEvent;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.HashtagsChangedEvent;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.gerrit.server.events.ReviewerAddedEvent;
import com.google.gerrit.server.events.ReviewerDeletedEvent;
import com.google.gerrit.server.events.TopicChangedEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import com.ruesga.gerrit.plugins.fcm.messaging.Notification;
import com.ruesga.gerrit.plugins.fcm.rest.CloudNotificationEvents;
import com.ruesga.gerrit.plugins.fcm.workers.FcmUploaderWorker;

public class EventHandler implements EventListener {

    private static final Logger log =
            LoggerFactory.getLogger(EventHandler.class);

    private final String pluginName;
    private final FcmUploaderWorker uploader;
    private final Gson gson;

    private static class AccountInfo {
        @SerializedName("username") private String username;
        @SerializedName("name") private String name;
        @SerializedName("email") private String email;
    }

    private static class TopicInfo {
        @SerializedName("oldTopic") private String oldTopic;
        @SerializedName("newTopic") private String newTopic;
    }

    private static class HashtagsInfo {
        @SerializedName("oldHashtags") private String[] oldHashtags;
        @SerializedName("newHashtags") private String[] newHashtags;
    }

    @Inject
    public EventHandler(
            @PluginName String pluginName,
            FcmUploaderWorker uploader) {
        super();
        this.pluginName = pluginName;
        this.uploader = uploader;
        this.gson = new GsonBuilder().create();
    }

    @Override
    public void onEvent(Event event) {
        try {
            int eventId;
            ChangeAttribute change;
            AccountAttribute author = null;
            PatchSetAttribute patchset = null;
            String extra = null;
            if (event instanceof ChangeAbandonedEvent) {
                eventId = CloudNotificationEvents.CHANGE_ABANDONED_EVENT;
                change = ((ChangeAbandonedEvent) event).change.get();
                author = ((ChangeAbandonedEvent) event).abandoner.get();
                patchset = ((ChangeAbandonedEvent) event).patchSet.get();
            } else if (event instanceof ChangeMergedEvent) {
                eventId = CloudNotificationEvents.CHANGE_MERGED_EVENT;
                change = ((ChangeMergedEvent) event).change.get();
                author = ((ChangeMergedEvent) event).submitter.get();
                patchset = ((ChangeMergedEvent) event).patchSet.get();
            } else if (event instanceof ChangeRestoredEvent) {
                eventId = CloudNotificationEvents.CHANGE_RESTORED_EVENT;
                change = ((ChangeRestoredEvent) event).change.get();
                author = ((ChangeRestoredEvent) event).restorer.get();
                patchset = ((ChangeRestoredEvent) event).patchSet.get();
            } else if (event instanceof CommentAddedEvent) {
                eventId = CloudNotificationEvents.COMMENT_ADDED_EVENT;
                change = ((CommentAddedEvent) event).change.get();
                author = ((CommentAddedEvent) event).author.get();
                patchset = ((CommentAddedEvent) event).patchSet.get();
                extra = StringUtils.abbreviate(
                        ((CommentAddedEvent) event).comment, 250);
            } else if (event instanceof DraftPublishedEvent) {
                eventId = CloudNotificationEvents.DRAFT_PUBLISHED_EVENT;
                change = ((DraftPublishedEvent) event).change.get();
                author = ((DraftPublishedEvent) event).uploader.get();
                patchset = ((DraftPublishedEvent) event).patchSet.get();
            } else if (event instanceof HashtagsChangedEvent) {
                eventId = CloudNotificationEvents.HASHTAG_CHANGED_EVENT;
                change = ((HashtagsChangedEvent) event).change.get();
                author = ((HashtagsChangedEvent) event).editor.get();

                HashtagsInfo info = new HashtagsInfo();
                info.oldHashtags = ((HashtagsChangedEvent) event).removed;
                info.newHashtags = ((HashtagsChangedEvent) event).hashtags;
                extra = this.gson.toJson(info);
            } else if (event instanceof ReviewerAddedEvent) {
                eventId = CloudNotificationEvents.REVIEWER_ADDED_EVENT;
                change = ((ReviewerAddedEvent) event).change.get();
                extra = toSerializedAccount(
                        ((ReviewerAddedEvent) event).reviewer.get());
            } else if (event instanceof ReviewerDeletedEvent) {
                eventId = CloudNotificationEvents.REVIEWER_DELETED_EVENT;
                change = ((ReviewerDeletedEvent) event).change.get();
                extra = toSerializedAccount(
                        ((ReviewerDeletedEvent) event).reviewer.get());
            } else if (event instanceof PatchSetCreatedEvent) {
                eventId = CloudNotificationEvents.PATCHSET_CREATED_EVENT;
                change = ((PatchSetCreatedEvent) event).change.get();
                author = ((PatchSetCreatedEvent) event).uploader.get();
                patchset = ((PatchSetCreatedEvent) event).patchSet.get();
            } else if (event instanceof TopicChangedEvent) {
                eventId = CloudNotificationEvents.TOPIC_CHANGED_EVENT;
                change = ((TopicChangedEvent) event).change.get();
                author = ((TopicChangedEvent) event).changer.get();

                TopicInfo topicInfo = new TopicInfo();
                topicInfo.oldTopic = ((TopicChangedEvent) event).oldTopic;
                topicInfo.newTopic = change.topic;
                extra = this.gson.toJson(topicInfo);
            } else {
                // Unsupported event for FCM notifications
                return;
            }

            List<String> notifiedUsers = obtainNotifiedUsers(change, author);

            Notification notification = new Notification();
            notification.when = System.currentTimeMillis();
            notification.event = eventId;
            notification.change = change.id;
            notification.revision = patchset == null ? null : patchset.revision;
            notification.project = change.project;
            notification.branch = change.branch;
            notification.topic = change.topic;
            notification.author = toSerializedAccount(author);
            notification.subject = StringUtils.abbreviate(change.subject, 100);
            notification.extra = extra;

            this.uploader.notify(notifiedUsers, notification);

        } catch (Exception ex) {
            log.error(String.format(
                    "[%s] Failed to process event", pluginName), ex);
        }
    }

    private List<String> obtainNotifiedUsers(
            ChangeAttribute change, AccountAttribute author) {
        List<String> notifiedUsers = new ArrayList<>();
        if (change.allReviewers != null) {
            for (AccountAttribute account : change.allReviewers) {
                if (account.username != null && !account.username.isEmpty()) {
                    notifiedUsers.add(account.username);
                }
            }
        }
        if (author.username != null && !author.username.isEmpty()) {
            notifiedUsers.remove(author.username);
        }
        return notifiedUsers;
    }

    private String toSerializedAccount(AccountAttribute account) {
        if (account == null) {
            return null;
        }
        AccountInfo info = new AccountInfo();
        info.username = account.username;
        info.name = account.name;
        info.email = account.email;
        return this.gson.toJson(info);
    }
}
