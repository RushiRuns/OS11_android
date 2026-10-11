package com.os11.app.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation destinations for OS11 Android.
 */
sealed interface OS11Route {
    @Serializable
    data object MyDay : OS11Route

    @Serializable
    data object DayPlan : OS11Route

    @Serializable
    data object Inbox : OS11Route

    @Serializable
    data object Planned : OS11Route

    @Serializable
    data object Browse : OS11Route

    @Serializable
    data object Dashboard : OS11Route

    @Serializable
    data object Goals : OS11Route

    @Serializable
    data object Pomodoro : OS11Route

    @Serializable
    data object Settings : OS11Route

    @Serializable
    data object SettingsAppearance : OS11Route

    @Serializable
    data object SettingsNotifications : OS11Route

    @Serializable
    data object SettingsModules : OS11Route

    @Serializable
    data object SettingsData : OS11Route

    @Serializable
    data object SettingsPrivacy : OS11Route

    @Serializable
    data object SettingsAbout : OS11Route

    @Serializable
    data object Gallery : OS11Route

    @Serializable
    data object Trash : OS11Route

    @Serializable
    data class AreaOverview(val areaId: String) : OS11Route

    @Serializable
    data class ProjectDetail(val projectId: String) : OS11Route

    @Serializable
    data object Important : OS11Route

    @Serializable
    data object AllTasks : OS11Route

    @Serializable
    data object Completed : OS11Route

    @Serializable
    data object Search : OS11Route

    @Serializable
    data class TagDetail(val tagId: String, val tagName: String) : OS11Route

    @Serializable
    data object TagManager : OS11Route

    @Serializable
    data object NotificationCenter : OS11Route

    @Serializable
    data object BatteryGuidance : OS11Route

    @Serializable
    data class BucketList(val bucketType: String) : OS11Route
}
