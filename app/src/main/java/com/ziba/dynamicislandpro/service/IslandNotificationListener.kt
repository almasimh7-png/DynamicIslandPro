package com.ziba.dynamicislandpro.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.ziba.dynamicislandpro.core.AppBus

/**
 * Listens to all notifications and pushes call-style ones (and others) to AppBus,
 * which the overlay observes. Documented limitations:
 *  - Cannot automatically answer/reject calls (requires InCallService + default dialer role).
 *  - Some OEM dialers do not use CallStyle; we detect EXTRA_CALL_* extras as fallback.
 */
class IslandNotificationListener : NotificationListenerService() {

    private fun isCallStyle(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        val categoryCall = n.category == Notification.CATEGORY_CALL
        val extras = n.extras
        val hasCallExtras = extras.containsKey("android.extra.CALL_STATE") ||
            extras.containsKey("android.callStyle")
        return categoryCall || hasCallExtras
    }

    private fun callStateOf(sbn: StatusBarNotification): AppBus.CallState {
        val state = sbn.notification.extras.getInt("android.extra.CALL_STATE", -1)
        return when (state) {
            1 -> AppBus.CallState.INCOMING
            2 -> AppBus.CallState.ACTIVE
            else -> AppBus.CallState.INCOMING
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val call = isCallStyle(sbn)
        AppBus.pushNotification(
            AppBus.NotificationEvent(sbn.packageName, title, text, call)
        )
        if (call) AppBus.pushCallState(callStateOf(sbn))
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (isCallStyle(sbn)) AppBus.pushCallState(AppBus.CallState.ENDED)
    }
}
