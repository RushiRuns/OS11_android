package com.os11.app.ui.features.notification

import com.os11.app.service.notification.NotificationItem

data class NotificationCenterUiState(
    val notifications: List<NotificationItem> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
)
