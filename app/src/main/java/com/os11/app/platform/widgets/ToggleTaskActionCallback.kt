package com.os11.app.platform.widgets

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import dagger.hilt.android.EntryPointAccessors

/**
 * Action callback to toggle task completion directly from the My Day Glance widget.
 */
class ToggleTaskActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[TaskIdKey] ?: return
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java,
        )

        val task = entryPoint.taskRepository().getById(taskId)
        if (task != null) {
            entryPoint.taskService().setCompleted(taskId, !task.isCompleted)
        }
        MyDayWidget().update(context, glanceId)
    }

    companion object {
        val TaskIdKey = ActionParameters.Key<String>("task_id")
    }
}
