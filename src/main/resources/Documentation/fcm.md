Firebase Cloud Notifications Plugin
===================================

This document explain how this plugins works.


REST API
--------

This plugin addes new methods to the /accounts Gerrit REST Api entry point to allow a device to register/unregister to receive event notification in this gerrit instance.

----------
**Get Cloud Notification**
`'GET /accounts/{account-id}/cloud-notifications/{device-id}'`

Retrieves a registered device information hold by the Gerrit server instance.

*Request*
This request requires an authenticated call  and only returns information if account-id is the authenticated account. This method returns a *CloudNotificationInfo* entity (see below).

    GET /accounts/self/cloud-notifications/bk3RNwTe3H0:CI2k_HHwgIpoDKCIZvvDMExUdFQ3P1

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

----------
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

 ----------
**Unregister Cloud Notification**
`'DELETE /accounts/{account-id}/cloud-notifications/{device-id}'`

Unregister a registered device information hold by the Gerrit server instance.

*Request*
This request requires an authenticated call  and is only valid if account-id is the authenticated account.

    DELETE /accounts/self/cloud-notifications/bk3RNwTe3H0:CI2k_HHwgIpoDKCIZvvDMExUdFQ3P1

*Response*

    HTTP/1.1 204 No Content

----------
**CloudNotificationInfo**
Entity with information about a registered device.

`deviceId: A Firebase Cloud Messaging registered device identification.`

`registeredOn: When the device was registered.`

`token: A device token that unique identifies the server/account in the device.`

`events : A bitwise flag to indicate which events to notify. See CloudNotificationEvents below.`

`responseMode: Firebase response mode. See CloudNotificationResponseMode below.`

----------
**CloudNotificationInput**
Entity with information about a device to be register.

`deviceId: A Firebase Cloud Messaging registered device identification.`

`token: A device token that unique identifies the server/account in the device.`

`events : A bitwise flag to indicate which events to notify. See CloudNotificationEvents below.`

`responseMode: Firebase response mode. See CloudNotificationResponseMode below.`

 ----------
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

 ----------
**CloudNotificationResponseMode**
Enumeration of available Firebase notification modes.

`NOTIFICATION: Notification in the device is handled by Firebase`

`DATA: Notification in the device is handled by the app which receives a custom object data`

`BOTH: Notification includes both: notification data and custom object data`
