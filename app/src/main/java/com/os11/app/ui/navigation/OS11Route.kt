package com.os11.app.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation destinations for OS11 Android.
 */
sealed interface OS11Route {
    @Serializable
    data object MyDay : OS11Route

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
    data object Gallery : OS11Route
}
