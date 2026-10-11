package com.os11.app.platform.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.os11.app.MainActivity
import com.os11.app.R
import com.os11.app.data.db.dao.SlimTask
import com.os11.app.platform.shortcuts.AppShortcutManager
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

/**
 * Home-screen widget for My Day tasks powered by Jetpack Glance.
 *
 * Implements:
 * 1. Read-only query over observeMyDaySlim.
 * 2. Privacy protection: hides task contents when app lock is active.
 * 3. Quick complete toggle via ActionCallback.
 * 4. Header with task count and Quick Add launcher.
 */
@Suppress("LongMethod", "MagicNumber")
class MyDayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java,
        )

        val isLocked = entryPoint.appLockManager().isLocked.value
        val today = entryPoint.clock().today()
        val tasks = if (!isLocked) {
            try {
                entryPoint.taskRepository().observeMyDaySlim(today).first()
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        provideContent {
            val bgSurface = ColorProvider(Color(0xFF1C1C1C))
            val cardSurface = ColorProvider(Color(0xFF262626))
            val textPrimary = ColorProvider(Color(0xFFE6E6E6))
            val textSecondary = ColorProvider(Color(0xFF8C8C8C))
            val accentColor = ColorProvider(Color(0xFF4DA3FF))

            val quickAddIntent = Intent(context, MainActivity::class.java).apply {
                action = AppShortcutManager.ACTION_QUICK_ADD
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(bgSurface)
                    .cornerRadius(16.dp)
                    .padding(12.dp),
            ) {
                // Header
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "My Day",
                            style = TextStyle(
                                color = textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        if (!isLocked) {
                            val activeCount = tasks.count { !it.isCompleted }
                            Text(
                                text = if (activeCount > 0) "$activeCount remaining" else "All done",
                                style = TextStyle(
                                    color = textSecondary,
                                    fontSize = 12.sp,
                                ),
                            )
                        }
                    }

                    // Quick Add button
                    Box(
                        modifier = GlanceModifier
                            .size(32.dp)
                            .background(cardSurface)
                            .cornerRadius(8.dp)
                            .clickable(actionStartActivity(quickAddIntent)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_quick_add),
                            contentDescription = "Quick Add",
                            modifier = GlanceModifier.size(18.dp),
                        )
                    }
                }

                // Content
                when {
                    isLocked -> {
                        Box(
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .background(cardSurface)
                                .cornerRadius(12.dp)
                                .clickable(actionStartActivity(openAppIntent))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "OS11 is Locked",
                                    style = TextStyle(
                                        color = textPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                )
                                Spacer(modifier = GlanceModifier.height(4.dp))
                                Text(
                                    text = "Tap to open and unlock",
                                    style = TextStyle(
                                        color = accentColor,
                                        fontSize = 12.sp,
                                    ),
                                )
                            }
                        }
                    }
                    tasks.isEmpty() -> {
                        Box(
                            modifier = GlanceModifier
                                .fillMaxSize()
                                .background(cardSurface)
                                .cornerRadius(12.dp)
                                .clickable(actionStartActivity(quickAddIntent))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No tasks for today",
                                    style = TextStyle(
                                        color = textSecondary,
                                        fontSize = 13.sp,
                                    ),
                                )
                                Spacer(modifier = GlanceModifier.height(4.dp))
                                Text(
                                    text = "Tap + to capture a task",
                                    style = TextStyle(
                                        color = accentColor,
                                        fontSize = 12.sp,
                                    ),
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                            items(tasks) { task ->
                                TaskWidgetRow(
                                    task = task,
                                    cardSurface = cardSurface,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    openAppIntent = openAppIntent,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun TaskWidgetRow(
        task: SlimTask,
        cardSurface: ColorProvider,
        textPrimary: ColorProvider,
        textSecondary: ColorProvider,
        openAppIntent: Intent,
    ) {
        val checkIcon = if (task.isCompleted) {
            R.drawable.ic_check_circle
        } else {
            R.drawable.ic_circle_outline
        }

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 3.dp)
                .background(cardSurface)
                .cornerRadius(8.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Checkbox
            Box(
                modifier = GlanceModifier
                    .size(24.dp)
                    .clickable(
                        actionRunCallback<ToggleTaskActionCallback>(
                            actionParametersOf(ToggleTaskActionCallback.TaskIdKey to task.id),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    provider = ImageProvider(checkIcon),
                    contentDescription = if (task.isCompleted) "Completed" else "Mark Complete",
                    modifier = GlanceModifier.size(18.dp),
                )
            }

            Spacer(modifier = GlanceModifier.width(8.dp))

            // Task Title
            Text(
                text = task.title,
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(actionStartActivity(openAppIntent)),
                maxLines = 1,
                style = TextStyle(
                    color = if (task.isCompleted) textSecondary else textPrimary,
                    fontSize = 13.sp,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                ),
            )
        }
    }
}
