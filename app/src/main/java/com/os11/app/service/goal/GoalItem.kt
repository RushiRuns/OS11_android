package com.os11.app.service.goal

import com.os11.app.data.db.entities.GoalEntity

/**
 * Domain-aligned Goal model exposed to UI layers.
 * Decouples Compose UI from Room GoalEntity per Konsist architectural rules.
 */
data class GoalItem(
    val id: String,
    val title: String,
    val description: String? = null,
    val goalType: String = "target",
    val targetDate: String? = null,
    val targetValue: Double = DEFAULT_TARGET_VALUE,
    val currentValue: Double = 0.0,
    val streakCount: Int = 0,
    val lastProgressAt: String? = null,
    val linkedTaskIds: List<String> = emptyList(),
    val linkedProjectIds: List<String> = emptyList(),
    val linkedTasksCount: Int = 0,
    val completedTasksCount: Int = 0,
    val createdAt: String,
    val updatedAt: String,
) {
    val progressPercent: Float
        get() {
            if (targetValue <= 0.0) return 0f
            return ((currentValue / targetValue) * 100.0).coerceIn(0.0, 100.0).toFloat()
        }

    val isCompleted: Boolean
        get() = currentValue >= targetValue

    companion object {
        const val DEFAULT_TARGET_VALUE = 100.0
    }
}

fun GoalEntity.toGoalItem(
    linkedTaskIds: List<String> = emptyList(),
    linkedProjectIds: List<String> = emptyList(),
    completedTasksCount: Int = 0,
): GoalItem = GoalItem(
    id = id,
    title = title,
    description = description,
    goalType = goalType,
    targetDate = targetDate,
    targetValue = targetValue,
    currentValue = currentValue,
    streakCount = streakCount,
    lastProgressAt = lastProgressAt,
    linkedTaskIds = linkedTaskIds,
    linkedProjectIds = linkedProjectIds,
    linkedTasksCount = linkedTaskIds.size,
    completedTasksCount = completedTasksCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
