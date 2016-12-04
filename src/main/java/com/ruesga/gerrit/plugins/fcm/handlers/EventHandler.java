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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.common.EventListener;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.server.account.AccountState;
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
import com.google.gerrit.server.query.QueryParseException;
import com.google.gerrit.server.query.QueryResult;
import com.google.gerrit.server.query.account.AccountQueryBuilder;
import com.google.gerrit.server.query.account.AccountQueryProcessor;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.gerrit.server.query.change.ChangeQueryBuilder;
import com.google.gerrit.server.query.change.ChangeQueryProcessor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.ruesga.gerrit.plugins.fcm.messaging.Notification;
import com.ruesga.gerrit.plugins.fcm.rest.CloudNotificationEvents;
import com.ruesga.gerrit.plugins.fcm.workers.FcmUploaderWorker;

public class EventHandler implements EventListener {

    private static final Logger log =
            LoggerFactory.getLogger(EventHandler.class);

    private final String pluginName;
    private final FcmUploaderWorker uploader;
    private final AccountQueryBuilder aqb;
    private final AccountQueryProcessor aqp;
    private final ChangeQueryBuilder cqb;
    private final ChangeQueryProcessor cqp;
    private final Gson gson;

    public static class AccountInfo {
        @SerializedName("username") public String username;
        @SerializedName("name") public String name;
        @SerializedName("email") public String email;
    }

    public static class TopicInfo {
        @SerializedName("oldTopic") public String oldTopic;
        @SerializedName("newTopic") public String newTopic;
    }

    public static class HashtagsInfo {
        @SerializedName("oldHashtags") public String[] oldHashtags;
        @SerializedName("newHashtags") public String[] newHashtags;
    }

    @Inject
    public EventHandler(
            @PluginName String pluginName,
            FcmUploaderWorker uploader,
            AccountQueryBuilder aqb,
            AccountQueryProcessor aqp,
            ChangeQueryBuilder cqb,
            ChangeQueryProcessor cqp) {
        super();
        this.pluginName = pluginName;
        this.uploader = uploader;
        this.aqb = aqb;
        this.aqp = aqp;
        this.cqb = cqb;
        this.cqp = cqp;
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

            // Obtain information about the accounts that need to be
            // notified related to this event
            List<Integer> notifiedUsers =
                    obtainNotifiedAccounts(change, author);
            if (notifiedUsers.isEmpty()) {
                // Nobody to notify about this event
                return;
            }

            // Build the notification
            Notification notification = new Notification();
            notification.when = event.eventCreatedOn;
            notification.event = eventId;
            notification.change = change.number;
            notification.revision = patchset == null ? null : patchset.revision;
            notification.project = change.project;
            notification.branch = change.branch;
            notification.topic = change.topic;
            notification.author = toSerializedAccount(author);
            notification.subject = StringUtils.abbreviate(change.subject, 100);
            notification.extra = extra;

            // Perform notification
            this.uploader.notifyTo(notifiedUsers, notification);

        } catch (Exception ex) {
            log.error(String.format(
                    "[%s] Failed to process event", pluginName), ex);
        }
    }

    private List<Integer> obtainNotifiedAccounts(ChangeAttribute change,
            AccountAttribute author) throws QueryParseException, OrmException {

        // Obtain the information about the change and the author
        QueryResult<ChangeData> changeQuery =
                cqp.query(cqb.parse("change:" + change.number));
        List<ChangeData> changeQueryResults = changeQuery.entities();
        if (changeQueryResults == null || changeQueryResults.isEmpty()) {
            log.warn(String.format(
                    "[%s] No change found for %s", pluginName, change.number));
            return new ArrayList<>();
        }
        ChangeData changeData = changeQueryResults.get(0);

        // Obtain the information about the originator of this event
        AccountState authorData = null;
        if (author != null) {
            QueryResult<AccountState> accountQuery =
                    aqp.query(aqb.username(author.username));
            List<AccountState> authorQueryResults = accountQuery.entities();
            if (authorQueryResults != null && !authorQueryResults.isEmpty()) {
                authorData = authorQueryResults.get(0);
            }
        }


        Set<Integer> notifiedUsers = new HashSet<>();

        // 1.- Owner of the change
        notifiedUsers.add(changeData.change().getOwner().get());

        // 2.- Reviewers
        if (changeData.reviewers() != null &&
                changeData.reviewers().all() != null) {
            for (Account.Id account : changeData.reviewers().all().asList()) {
                notifiedUsers.add(account.get());
            }
        }

        // 3.- Watchers
        // TODO

        // 4.- Remove the author of this event (he doesn't need to get
        // the notification)
        if (authorData != null) {
            notifiedUsers.remove(authorData.getAccount().getId().get());
        }
        return new ArrayList<>(notifiedUsers);
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
