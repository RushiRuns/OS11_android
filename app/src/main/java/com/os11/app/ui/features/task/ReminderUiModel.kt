package com.os11.app.ui.features.task

import androidx.compose.runtime.Immutable

/**
 * UI presentation model for task reminders.
 * Keeps UI layer completely decoupled from Room entities per Konsist architecture rules.
 */
@Immutable
data class ReminderUiModel(val id: String, val remindAt: String, val formattedRemindAt: String = remindAt)
