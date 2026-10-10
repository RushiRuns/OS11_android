package com.os11.app.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Permitted Animation Specs per ARCHITECTURE.md (Animation Rules).
 *
 * Spring/overshoot motion exists at exactly the 4 permitted sites:
 * 1. Checkbox completion (spring overshoot + strikethrough)
 * 2. Task detail sheet open/close (spring slide up)
 * 3. List reorder / nesting drag (item displacement and drop settle)
 * 4. Quick-add sheet appear/dismiss (scale + fade with the sheet)
 *
 * Everything else uses tween with OS11Motion durations and easings.
 */
object OS11AnimationSpecs {
    // 1. Checkbox completion spring
    fun <T> checkboxSpring(reduceMotion: Boolean = false): AnimationSpec<T> = if (reduceMotion) {
        snap()
    } else {
        spring(
            dampingRatio = OS11Motion.SpringDamping,
            stiffness = OS11Motion.SpringStiffness,
        )
    }

    // 2. Task detail sheet slide spring
    fun <T> sheetSpring(reduceMotion: Boolean = false): AnimationSpec<T> = if (reduceMotion) {
        snap()
    } else {
        spring(
            dampingRatio = OS11Motion.SpringDamping,
            stiffness = OS11Motion.SpringStiffness,
        )
    }

    // 3. List reorder displacement spring
    fun <T> reorderSpring(reduceMotion: Boolean = false): AnimationSpec<T> = if (reduceMotion) {
        snap()
    } else {
        spring(
            dampingRatio = OS11Motion.SpringDamping,
            stiffness = OS11Motion.SpringStiffness,
        )
    }

    // 4. Quick-add scale + fade spring
    fun <T> quickAddSpring(reduceMotion: Boolean = false): AnimationSpec<T> = if (reduceMotion) {
        snap()
    } else {
        spring(
            dampingRatio = OS11Motion.SpringDamping,
            stiffness = OS11Motion.SpringStiffness,
        )
    }

    // Standard tween transitions
    fun <T> fastTween(reduceMotion: Boolean = false): TweenSpec<T> = if (reduceMotion) {
        tween(0)
    } else {
        tween(
            durationMillis = OS11Motion.Fast,
            easing = OS11Motion.EaseOut,
        )
    }

    fun <T> defaultTween(reduceMotion: Boolean = false): TweenSpec<T> = if (reduceMotion) {
        tween(0)
    } else {
        tween(
            durationMillis = OS11Motion.Default,
            easing = OS11Motion.EaseOut,
        )
    }

    fun <T> slowTween(reduceMotion: Boolean = false): TweenSpec<T> = if (reduceMotion) {
        tween(0)
    } else {
        tween(
            durationMillis = OS11Motion.Slow,
            easing = OS11Motion.EaseOut,
        )
    }
}
