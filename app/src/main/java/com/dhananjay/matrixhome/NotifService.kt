package com.dhananjay.matrixhome

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object NotifStore {
    var titles by mutableStateOf(listOf<String>())
}

/** Needs one-time access: Settings > Notification access, or the ADB command in README. */
class NotifService : NotificationListenerService() {
    override fun onListenerConnected() = refresh()
    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    private fun refresh() {
        NotifStore.titles = try {
            activeNotifications.filter { it.isClearable }.map {
                it.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: it.packageName
            }
        } catch (_: Exception) { emptyList() }
    }
}
