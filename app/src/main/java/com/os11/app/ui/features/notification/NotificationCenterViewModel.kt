package com.os11.app.ui.features.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.os11.app.service.notification.NotificationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationCenterViewModel @Inject constructor(private val notificationService: NotificationService) :
    ViewModel() {

    val uiState: StateFlow<NotificationCenterUiState> = combine(
        notificationService.observeNotifications(),
        notificationService.observeUnreadCount(),
    ) { notifications, unreadCount ->
        NotificationCenterUiState(
            notifications = notifications,
            unreadCount = unreadCount,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotificationCenterUiState(isLoading = true),
    )

    fun markAsRead(id: String) {
        viewModelScope.launch {
            notificationService.markAsRead(id)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            notificationService.markAllAsRead()
        }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            notificationService.deleteNotification(id)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            notificationService.clearAll()
        }
    }
}
