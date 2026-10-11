package com.os11.app.platform.shortcuts

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import com.os11.app.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooGenericExceptionCaught")
class AppShortcutManager @Inject constructor(@param:ApplicationContext private val context: Context) {
    fun updateShortcuts(enabledModules: Set<String>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return

        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return

        try {
            shortcutManager.dynamicShortcuts = buildShortcutList(enabledModules)
        } catch (_: Exception) {
            // Ignore system shortcut manager exceptions
        }
    }

    internal fun buildShortcutList(enabledModules: Set<String>): List<ShortcutInfo> {
        val shortcuts = mutableListOf<ShortcutInfo>()

        // 1. Quick Add (always available)
        val quickAddIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_QUICK_ADD
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        shortcuts.add(
            ShortcutInfo.Builder(context, "shortcut_quick_add")
                .setShortLabel("Quick Add")
                .setLongLabel("Capture task")
                .setIcon(Icon.createWithResource(context, android.R.drawable.ic_input_add))
                .setIntent(quickAddIntent)
                .build(),
        )

        // 2. My Day (if my_day module enabled)
        if (enabledModules.contains("my_day")) {
            val myDayIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_MY_DAY
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            shortcuts.add(
                ShortcutInfo.Builder(context, "shortcut_my_day")
                    .setShortLabel("My Day")
                    .setLongLabel("Open My Day")
                    .setIcon(Icon.createWithResource(context, android.R.drawable.ic_menu_today))
                    .setIntent(myDayIntent)
                    .build(),
            )
        }

        // 3. Pomodoro (if pomodoro module enabled)
        if (enabledModules.contains("pomodoro")) {
            val pomodoroIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_POMODORO
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            shortcuts.add(
                ShortcutInfo.Builder(context, "shortcut_pomodoro")
                    .setShortLabel("Pomodoro")
                    .setLongLabel("Focus Timer")
                    .setIcon(Icon.createWithResource(context, android.R.drawable.ic_lock_idle_alarm))
                    .setIntent(pomodoroIntent)
                    .build(),
            )
        }

        return shortcuts
    }

    internal fun computeShortcutIds(enabledModules: Set<String>): List<String> {
        val list = mutableListOf(ID_QUICK_ADD)
        if (enabledModules.contains("my_day")) list.add(ID_MY_DAY)
        if (enabledModules.contains("pomodoro")) list.add(ID_POMODORO)
        return list
    }

    companion object {
        const val ID_QUICK_ADD = "shortcut_quick_add"
        const val ID_MY_DAY = "shortcut_my_day"
        const val ID_POMODORO = "shortcut_pomodoro"

        const val ACTION_QUICK_ADD = "com.os11.app.action.QUICK_ADD"
        const val ACTION_MY_DAY = "com.os11.app.action.MY_DAY"
        const val ACTION_POMODORO = "com.os11.app.action.POMODORO"
    }
}
