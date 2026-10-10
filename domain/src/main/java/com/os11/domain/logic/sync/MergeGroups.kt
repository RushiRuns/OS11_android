package com.os11.domain.logic.sync

/**
 * Merge group mappings per SYNC.md Section 5.
 *
 * Dependent fields merge together as a single group under the same HLC.
 * Fields not defined in a group merge independently as 1-field groups.
 */
object MergeGroups {
    private val GROUPS: Map<String, Map<String, Set<String>>> = mapOf(
        "tasks" to mapOf(
            "when" to setOf(
                "bucket",
                "due_date",
                "due_time",
                "all_day",
                "recurrence_rule",
                "recurrence_basis",
            ),
            "waiting" to setOf(
                "waiting_on",
                "waiting_since",
                "follow_up_date",
            ),
            "completion" to setOf(
                "is_completed",
                "completed_at",
            ),
            "trash" to setOf(
                "is_trashed",
                "trashed_at",
            ),
            "placement" to setOf(
                "area_id",
                "project_id",
                "section_id",
                "parent_task_id",
            ),
            "myday" to setOf(
                "my_day_date",
                "scheduled_start_min",
                "scheduled_duration_min",
            ),
        ),
        "projects" to mapOf(
            "status" to setOf("status", "due_date"),
        ),
        "goals" to mapOf(
            "progress" to setOf("current_value", "last_progress_at"),
        ),
    )

    /**
     * Returns the group name for a given entity and field.
     * If the field is not part of a multi-field group, its group name is the field itself.
     */
    fun getGroupName(entity: String, field: String): String {
        val entityGroups = GROUPS[entity] ?: return field
        return entityGroups.entries.firstOrNull { field in it.value }?.key ?: field
    }

    fun getGroupFields(entity: String, field: String): Set<String> {
        val entityGroups = GROUPS[entity] ?: return setOf(field)
        return entityGroups.values.firstOrNull { field in it } ?: setOf(field)
    }

    /**
     * Checks if the field belongs to a multi-field group.
     */
    fun isGroupField(entity: String, field: String): Boolean {
        val entityGroups = GROUPS[entity] ?: return false
        return entityGroups.values.any { field in it }
    }
}
