package com.os11.app.service.settings

import com.os11.app.ui.theme.OS11Density
import com.os11.app.ui.theme.OS11FontStep

/**
 * Service-level DTOs for Settings and Modules to isolate Room entities from UI composables.
 */
data class SettingItem(val key: String, val value: String)

data class ModuleItem(val name: String, val isEnabled: Boolean, val title: String, val description: String)

data class AppearanceSettings(
    val accentColorHex: String = "#4da3ff",
    val fontStep: OS11FontStep = OS11FontStep.Md,
    val density: OS11Density = OS11Density.Comfortable,
    val reduceMotion: Boolean = false,
    val hapticsEnabled: Boolean = true,
)

data class NotificationSettings(
    val followUpNotifyTime: String = "09:00",
    val waitingStaleDays: Int = 7,
    val dayStartsAt: String = "00:00",
)

data class SecuritySettings(
    val appLockEnabled: Boolean = false,
    val appLockTimeoutSeconds: Int = 0,
    val secureScreen: Boolean = false,
    val hapticsEnabled: Boolean = true,
)

data class BackupSettings(
    val autoBackupEnabled: Boolean = false,
    val autoBackupUri: String? = null,
    val lastBackupTimestamp: String? = null,
)

data class ProfilePreset(
    val id: String,
    val title: String,
    val description: String,
    val moduleMap: Map<String, Boolean>,
) {
    companion object {
        val Minimal = ProfilePreset(
            id = "minimal",
            title = "Minimal",
            description = "Daily focus only: My Day and NLP quick capture. All secondary modules disabled.",
            moduleMap = mapOf(
                "my_day" to true,
                "project_management" to false,
                "pomodoro" to false,
                "agenda" to false,
                "goals_habits" to false,
                "dashboard" to false,
                "file_attachments" to false,
                "nlp_parsing" to true,
                "anytime" to false,
                "someday" to false,
                "waiting_for" to false,
                "habit_tracker" to false,
            ),
        )

        val Standard = ProfilePreset(
            id = "standard",
            title = "Standard",
            description = "Balanced system: My Day, Projects, Pomodoro, Day Plan, Goals, Dashboard, and Attachments.",
            moduleMap = mapOf(
                "my_day" to true,
                "project_management" to true,
                "pomodoro" to true,
                "agenda" to true,
                "goals_habits" to true,
                "dashboard" to true,
                "file_attachments" to true,
                "nlp_parsing" to true,
                "anytime" to false,
                "someday" to false,
                "waiting_for" to false,
                "habit_tracker" to false,
            ),
        )

        val GTD = ProfilePreset(
            id = "gtd",
            title = "Full GTD",
            description = "Complete Getting Things Done workflow with Anytime, Someday, and Waiting For lists.",
            moduleMap = mapOf(
                "my_day" to true,
                "project_management" to true,
                "pomodoro" to true,
                "agenda" to true,
                "goals_habits" to true,
                "dashboard" to true,
                "file_attachments" to true,
                "nlp_parsing" to true,
                "anytime" to true,
                "someday" to true,
                "waiting_for" to true,
                "habit_tracker" to false,
            ),
        )

        val Power = ProfilePreset(
            id = "power",
            title = "Power User",
            description = "All productivity modules enabled including habits and smart lists.",
            moduleMap = mapOf(
                "my_day" to true,
                "project_management" to true,
                "pomodoro" to true,
                "agenda" to true,
                "goals_habits" to true,
                "dashboard" to true,
                "file_attachments" to true,
                "nlp_parsing" to true,
                "anytime" to true,
                "someday" to true,
                "waiting_for" to true,
                "habit_tracker" to true,
            ),
        )

        val AllPresets = listOf(Minimal, Standard, GTD, Power)
    }
}
