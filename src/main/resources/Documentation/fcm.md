Firebase Cloud Notifications Plugin
===================================

App implementations should register the device token identifier received from FCM, using the *Register Cloud Notification* method to send the device token identifier and configure how it wants to be notified. When registering the device, it also should provide a token, which is returned on every notification, that allows to identify
the Gerrit instance how send the notification. In addition, an app can configure which events wants to listen to and how the message from FCM is received (as notification, as custom data or received both). Read the FCM documentation to know which is more appropriated for your device operation system. See *Custom Data* in the FCM Notification section to see the information returned as custom data.

An app can unregistered a device (and stop receiving notifications) from the Gerrit instance using the *Unregister Cloud Notification* method.


REST API
--------

This plugin addes new methods to the /accounts Gerrit REST Api entry point to allow a device to register/unregister to receive event notification in this gerrit instance.

***

**Get Cloud Notification**

`'GET /accounts/{account-id}/cloud-notifications/{device-id}/{token}'`

Retrieves a registered device information hold by the Gerrit server instance.

*Request*
This request requires an authenticated call  and only returns information if account-id is the authenticated account. This method returns a *CloudNotificationInfo* entity (see below).

    GET /accounts/self/cloud-notifications/bk3RNwTe3H0:CI2k_HHwgIpoDKCIZvvDMExUdFQ3P1/f986567456f107d0eb2d84c85ac5aed2

*Response*

    HTTP1.1 200 OK
    Content-Disposition: attachment
    Content-Type: application/json; charset=UTF-8
    
    )]}'
    {
      "deviceId": "bk3RNwTe3H0:CI2k_HHwgIpoDKCIZvvDMExUdFQ3P1",
      "registeredOn": "2016-11-25 14:45:03.123",
      "token": "f986567456f107d0eb2d84c85ac5aed2",
      "events": 1023,
      "responseMode": "DATA"
    }

***

**Register Cloud Notification**

`'POST /accounts/{account-id}/cloud-notifications'`

Register or update a registered device information to be hold by the Gerrit server instance.

*Request*
This request requires an authenticated call  and is only valid if account-id is the authenticated account. This method accepts a *CloudNotificationInput* entity (see below).

    POST /accounts/self/cloud-notifications
    Content-Type: application/json
    
    {
      "deviceId": "bk3RNwTe3H0:CI2k_HHwgIpoDKCIZvvDMExUdFQ3P1",
      "token": "f986567456f107d0eb2d84c85ac5aed2",
      "events": 8,
      "responseMode": "NOTIFICATION"
    }

As a response, this method returs the registered *CloudNotificationInfo* entity (see below).

*Response*

    HTTP1.1 200 OK
    Content-Disposition: attachment
    Content-Type: application/json; charset=UTF-8
    
    )]}'
    {
      "deviceId": "bk3RNwTe3H0:CI2k_HHwgIpoDKCIZvvDMExUdFQ3P1",
      "registeredOn": "2016-11-25 14:45:03.123",
      "token": "f986567456f107d0eb2d84c85ac5aed2",
      "events": 8,
      "responseMode": "NOTIFICATION"
    }

***

**Unregister Cloud Notification**

`'DELETE /accounts/{account-id}/cloud-notifications/{device-id}/{token}'`

Unregister a registered device information hold by the Gerrit server instance.

*Request*
This request requires an authenticated call  and is only valid if account-id is the authenticated account.

    DELETE /accounts/self/cloud-notifications/bk3RNwTe3H0:CI2k_HHwgIpoDKCIZvvDMExUdFQ3P1/f986567456f107d0eb2d84c85ac5aed2

*Response*

    HTTP/1.1 204 No Content

***

**CloudNotificationInfo**

Entity with information about a registered device.

`deviceId: A Firebase Cloud Messaging registered device identification.`

`registeredOn: When the device was registered.`

`token: A device token that unique identifies the server/account in the device.`

`events : A bitwise flag to indicate which events to notify. See CloudNotificationEvents below.`

`responseMode: Firebase response mode. See CloudNotificationResponseMode below.`

***

**CloudNotificationInput**

Entity with information about a device to be register.

`deviceId: A Firebase Cloud Messaging registered device identification.`

`token: A device token that unique identifies the server/account in the device.`

`events : A bitwise flag to indicate which events to notify. See CloudNotificationEvents below.`

`responseMode: Firebase response mode. See CloudNotificationResponseMode below.`

***

**CloudNotificationEvents**

Enumeration of available events to notify to the client device.

`CHANGE_ABANDONED_EVENT = 0x01`

`CHANGE_MERGED_EVENT = 0x02`

`CHANGE_RESTORED_EVENT = 0x04`

`COMMENT_ADDED_EVENT = 0x08`

`DRAFT_PUBLISHED_EVENT = 0x10`

`HASHTAG_CHANGED_EVENT = 0x20`

`REVIEWER_ADDED_EVENT = 0x40`

`REVIEWER_DELETED_EVENT = 0x80`

`PATCHSET_CREATED_EVENT = 0x100`

`TOPIC_CHANGED_EVENT = 0x200`

***

**CloudNotificationResponseMode**

Enumeration of available Firebase notification modes.

`NOTIFICATION: Notification in the device is handled by Firebase`

`DATA: Notification in the device is handled by the app which receives a custom object data`

`BOTH: Notification includes both: notification data and custom object data`



FCM NOTIFICATION
----------------

**Custom Data**

This is the information sent as a custom data inside the FCM notification. Depends on the event, some of this fields could be empty.

`token: The token used to registered the device`

`event: The event type (see CloudNotificationEvents above)`

`change: The legacy change identifier`

`revision: The revision/patchset identifier`

`project: The project identifier`

`branch: The branch identifier`

`topic: The topic identifier`

`author: A json object that contains the username, name and email of the account that originated the notification`

`subject: The subject of the change`

`extra: A json with extended information depending on event type, like comment, old and new topics, ...`
