package com.os11.app.service.notification

/**
 * Service-level DTO representing a notification item, preserving Konsist UI boundaries.
 */
data class NotificationItem(
    val id: String,
    val type: String, // 'due' | 'reminder' | 'pomodoro' | 'collaboration' | 'agenda' | 'goal' | 'streak' | 'system'
    val taskId: String? = null,
    val title: String,
    val body: String,
    val createdAt: String,
    val readAt: String? = null,
) {
    val isRead: Boolean get() = readAt != null
}
