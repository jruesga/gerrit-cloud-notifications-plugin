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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.api.changes.NotifyHandling;
import com.google.gerrit.extensions.client.ReviewerState;
import com.google.gerrit.extensions.common.AccountInfo;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.events.ChangeEvent;
import com.google.gerrit.extensions.events.RevisionEvent;
import com.google.gerrit.reviewdb.client.AccountProjectWatch;
import com.google.gerrit.reviewdb.client.AccountProjectWatch.NotifyType;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.IdentifiedUser.GenericFactory;
import com.google.gerrit.server.config.AllProjectsName;
import com.google.gerrit.server.query.Predicate;
import com.google.gerrit.server.query.QueryParseException;
import com.google.gerrit.server.query.QueryResult;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.gerrit.server.query.change.ChangeQueryBuilder;
import com.google.gerrit.server.query.change.ChangeQueryProcessor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Provider;
import com.ruesga.gerrit.plugins.fcm.messaging.Notification;
import com.ruesga.gerrit.plugins.fcm.workers.FcmUploaderWorker;

public abstract class EventHandler {

    static final Logger log =
            LoggerFactory.getLogger(EventHandler.class);

    private final String pluginName;
    private final FcmUploaderWorker uploader;
    private final AllProjectsName allProjectsName;
    private final ChangeQueryBuilder cqb;
    private final ChangeQueryProcessor cqp;
    private final Provider<ReviewDb> reviewdb;
    private final GenericFactory identifiedUserFactory;
    private final Gson gson;

    public EventHandler(
            @PluginName String pluginName,
            FcmUploaderWorker uploader,
            AllProjectsName allProjectsName,
            ChangeQueryBuilder cqb,
            ChangeQueryProcessor cqp,
            Provider<ReviewDb> reviewdb,
            GenericFactory identifiedUserFactory) {
        super();
        this.pluginName = pluginName;
        this.uploader = uploader;
        this.allProjectsName = allProjectsName;
        this.cqb = cqb;
        this.cqp = cqp;
        this.reviewdb = reviewdb;
        this.identifiedUserFactory = identifiedUserFactory;
        this.gson = new GsonBuilder().create();
    }

    protected abstract int getEventType();

    protected abstract NotifyType getNotifyType();

    protected Gson getSerializer() {
        return this.gson;
    }

    protected Notification createNotification(ChangeEvent event) {
        Notification notification = new Notification();
        notification.event = getEventType();
        notification.when = event.getWhen().getTime() / 1000L;
        notification.who = event.getWho();
        notification.change = event.getChange().changeId;
        notification.legacyChangeId = event.getChange()._number;
        notification.project = event.getChange().project;
        notification.branch = event.getChange().branch;
        notification.topic = event.getChange().topic;
        notification.subject = StringUtils.abbreviate(
                event.getChange().subject, 100);
        if (event instanceof RevisionEvent) {
            notification.revision =
                    ((RevisionEvent) event).getRevision().commit.commit;
        }
        return notification;
    }

    protected void notify(Notification notification, ChangeEvent event) {
        // Check if this event should be notified
        if (event.getNotify().equals(NotifyHandling.NONE)) {
            if (log.isDebugEnabled()) {log.debug(
                    String.format("[%s] Notify event %d is not enabled: %s",
                        pluginName, getEventType(), gson.toJson(notification)));
            }
            return;
        }

        // Obtain information about the accounts that need to be
        // notified related to this event
        List<Integer> notifiedUsers = obtainNotifiedAccounts(event);
        if (notifiedUsers.isEmpty()) {
            // Nobody to notify about this event
            return;
        }

        // Perform notification
        if (log.isDebugEnabled()) {
            log.debug(String.format("[%s] Sending notification %s to %s",
                    pluginName, gson.toJson(notification),
                    gson.toJson(notifiedUsers)));
        }
        this.uploader.notifyTo(notifiedUsers, notification);
    }

    private List<Integer> obtainNotifiedAccounts(ChangeEvent event) {
        Set<Integer> notifiedUsers = new HashSet<>();
        ChangeInfo change = event.getChange();
        NotifyHandling notifyTo = event.getNotify();

        // 1.- Owner of the change
        notifiedUsers.add(change.owner._accountId);

        // 2.- Reviewers
        if (notifyTo.equals(NotifyHandling.OWNER_REVIEWERS) ||
                notifyTo.equals(NotifyHandling.ALL)) {
            if (change.reviewers != null) {
                for (ReviewerState state : change.reviewers.keySet()) {
                    Collection<AccountInfo> accounts =
                            change.reviewers.get(state);
                    for (AccountInfo account : accounts) {
                        notifiedUsers.add(account._accountId);
                    }
                }
            }
        }

        // 3.- Watchers
        ChangeData changeData = obtainChangeData(change);
        notifiedUsers.addAll(getWatchers(getNotifyType(), changeData));

        // 4.- Remove the author of this event (he doesn't need to get
        // the notification)
        notifiedUsers.remove(event.getWho()._accountId);

        return new ArrayList<>(notifiedUsers);
    }

    private Set<Integer> getWatchers(NotifyType type, ChangeData change) {
        Set<Integer> watchers = new HashSet<>();
        try {
            for (AccountProjectWatch w : reviewdb.get().accountProjectWatches()
                    .byProject(change.project())) {
                add(watchers, w, type, change);
            }
            for (AccountProjectWatch w : reviewdb.get().accountProjectWatches()
                    .byProject(this.allProjectsName)) {
                add(watchers, w, type, change);
            }
        } catch (OrmException ex) {
            log.error(String.format(
                    "[%s] Failed to obtain watchers", pluginName), ex);
        }
        return watchers;
    }

    private boolean add(Set<Integer> watchers, AccountProjectWatch w,
            NotifyType type, ChangeData change) throws OrmException {
        IdentifiedUser user = identifiedUserFactory.create(w.getAccountId());

        try {
            if (filterMatch(user, w.getFilter(), change)) {
                // If we are set to notify on this type, add the user.
                // Otherwise, still return true to stop notifications for this user.
                if (w.isNotify(type)) {
                    watchers.add(w.getAccountId().get());
                }
                return true;
            }
        } catch (QueryParseException e) {
            // Ignore broken filter expressions.
        }
        return false;
    }

    private boolean filterMatch(
            CurrentUser user, String filter, ChangeData change)
            throws OrmException, QueryParseException {
        ChangeQueryBuilder qb = cqb.asUser(user);
        Predicate<ChangeData> p = qb.is_visible();

        if (filter != null) {
            Predicate<ChangeData> filterPredicate = qb.parse(filter);
            if (p == null) {
                p = filterPredicate;
            } else {
                p = Predicate.and(filterPredicate, p);
            }
        }
        return p == null || p.asMatchable().match(change);
    }

    private ChangeData obtainChangeData(ChangeInfo change) {
        try {
            QueryResult<ChangeData> changeQuery =
                    cqp.query(cqb.parse("change:" + change._number));
            List<ChangeData> changeQueryResults = changeQuery.entities();
            if (changeQueryResults == null || changeQueryResults.isEmpty()) {
                log.warn(String.format("[%s] No change found for %s",
                        pluginName, change._number));
                return null;
            }
            return changeQueryResults.get(0);

        } catch (Exception ex) {
            log.error(String.format("[%s] Failed to obtain change data: %d",
                    pluginName, change._number), ex);
        }
        return null;
    }

    protected String formatAccount(AccountInfo account) {
        if (account.name != null) {
            return account.name;
        }
        if (account.username != null) {
            return account.username;
        }
        return account.email;
    }
}
