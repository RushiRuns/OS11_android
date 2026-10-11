package com.os11.domain.logic.scheduling

import com.os11.domain.logic.validation.TaskValidation
import com.os11.domain.logic.validation.TaskValidationError
import com.os11.domain.logic.validation.TaskValidationInput

/**
 * Valid bucket types matching SCHEMA.md.
 */
enum class BucketType(val value: String) {
    ANYTIME("anytime"),
    SOMEDAY("someday"),
    ;

    companion object {
        fun fromValue(value: String?): BucketType? = entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
    }
}

/**
 * Valid recurrence basis types matching SCHEMA.md.
 */
enum class RecurrenceBasis(val value: String) {
    FIXED("fixed"),
    AFTER_COMPLETION("after_completion"),
    ;

    companion object {
        fun fromValue(value: String?): RecurrenceBasis? =
            entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
    }
}

/**
 * Complete immutable representation of a task's scheduling state.
 */
data class ScheduleState(
    val bucket: String? = null,
    val dueDate: String? = null,
    val dueTime: String? = null,
    val allDay: Boolean = true,
    val recurrenceRule: String? = null,
    val recurrenceBasis: String? = null,
    val waitingOn: String? = null,
    val waitingSince: String? = null,
    val followUpDate: String? = null,
    val followUpNotifiedOn: String? = null,
    val reviewedAt: String? = null,
)

/**
 * Scheduling operations that can be performed on a task.
 */
sealed interface SchedulingAction {
    data class ScheduleDueDate(
        val dueDate: String,
        val dueTime: String? = null,
        val allDay: Boolean = dueTime == null,
        val recurrenceRule: String? = null,
        val recurrenceBasis: String? = RecurrenceBasis.FIXED.value,
    ) : SchedulingAction

    data class MoveToBucket(val bucket: BucketType) : SchedulingAction

    data object ClearTemporalSchedule : SchedulingAction

    data class SetWaiting(val waitingOn: String, val followUpDate: String? = null) : SchedulingAction

    data object ClearWaiting : SchedulingAction

    data class SetRecurrence(val recurrenceRule: String, val recurrenceBasis: String = RecurrenceBasis.FIXED.value) :
        SchedulingAction

    data object ClearRecurrence : SchedulingAction

    data class AdvanceRecurrence(val nextDueDate: String) : SchedulingAction

    data object MarkReviewed : SchedulingAction

    data class AcknowledgeFollowUpNotification(val notifiedOnDate: String) : SchedulingAction
}

/**
 * Result of applying a scheduling transition.
 */
data class TransitionResult(val state: ScheduleState, val errors: List<TaskValidationError> = emptyList()) {
    val isSuccess: Boolean get() = errors.isEmpty()
}

/**
 * The 27-row GTD scheduling transition engine (ADR-0010).
 *
 * Implements strict state transitions governing:
 * 1. Mutual exclusivity of `bucket` and `due_date` (Rules R1-R3 in SCHEMA.md).
 * 2. Guarded fields (`bucket`, `waiting_*`, `follow_up_*`, `reviewed_at`, `due_*`, `recurrence_*`).
 * 3. Atomic field reset when transitioning between scheduled, bucketed, and inbox states.
 */
@Suppress("TooManyFunctions")
object SchedulingTransitions {

    fun transition(
        current: ScheduleState,
        action: SchedulingAction,
        referenceDate: String,
        referenceTimestamp: String,
    ): TransitionResult = when (action) {
        is SchedulingAction.ScheduleDueDate -> applyScheduleDueDate(current, action)
        is SchedulingAction.MoveToBucket -> applyMoveToBucket(current, action)
        is SchedulingAction.ClearTemporalSchedule -> applyClearTemporalSchedule(current)
        is SchedulingAction.SetWaiting -> applySetWaiting(current, action, referenceDate)
        is SchedulingAction.ClearWaiting -> applyClearWaiting(current)
        is SchedulingAction.SetRecurrence -> applySetRecurrence(current, action)
        is SchedulingAction.ClearRecurrence -> applyClearRecurrence(current)
        is SchedulingAction.AdvanceRecurrence -> applyAdvanceRecurrence(current, action)
        is SchedulingAction.MarkReviewed -> applyMarkReviewed(current, referenceTimestamp)
        is SchedulingAction.AcknowledgeFollowUpNotification ->
            applyAcknowledgeFollowUp(current, action.notifiedOnDate)
    }

    private fun applyScheduleDueDate(
        current: ScheduleState,
        action: SchedulingAction.ScheduleDueDate,
    ): TransitionResult {
        val newState = current.copy(
            bucket = null, // Mutual exclusivity rule: setting due_date clears bucket
            dueDate = action.dueDate,
            dueTime = action.dueTime,
            allDay = action.allDay,
            recurrenceRule = action.recurrenceRule ?: current.recurrenceRule,
            recurrenceBasis = action.recurrenceBasis ?: current.recurrenceBasis,
        )
        return validateAndBuild(newState)
    }

    private fun applyMoveToBucket(current: ScheduleState, action: SchedulingAction.MoveToBucket): TransitionResult {
        val newState = current.copy(
            bucket = action.bucket.value,
            // Mutual exclusivity rule: moving to anytime/someday clears all dates & recurrence
            dueDate = null,
            dueTime = null,
            allDay = true,
            recurrenceRule = null,
            recurrenceBasis = null,
        )
        return validateAndBuild(newState)
    }

    private fun applyClearTemporalSchedule(current: ScheduleState): TransitionResult {
        val newState = current.copy(
            bucket = null,
            dueDate = null,
            dueTime = null,
            allDay = true,
            recurrenceRule = null,
            recurrenceBasis = null,
        )
        return validateAndBuild(newState)
    }

    private fun applySetWaiting(
        current: ScheduleState,
        action: SchedulingAction.SetWaiting,
        referenceDate: String,
    ): TransitionResult {
        val trimmedWaiting = action.waitingOn.trim()
        val newState = current.copy(
            waitingOn = trimmedWaiting,
            waitingSince = current.waitingSince ?: referenceDate,
            followUpDate = action.followUpDate,
            followUpNotifiedOn = null,
        )
        return validateAndBuild(newState)
    }

    private fun applyClearWaiting(current: ScheduleState): TransitionResult {
        val newState = current.copy(
            waitingOn = null,
            waitingSince = null,
            followUpDate = null,
            followUpNotifiedOn = null,
        )
        return validateAndBuild(newState)
    }

    private fun applySetRecurrence(current: ScheduleState, action: SchedulingAction.SetRecurrence): TransitionResult {
        // Recurrence requires a due date and cannot exist in a bucket
        val errors = mutableListOf<TaskValidationError>()
        if (current.dueDate == null) {
            errors.add(TaskValidationError.BlankTitle) // We will treat missing due date as invalid
        }
        if (current.bucket != null) {
            errors.add(TaskValidationError.BucketDueDateConflict)
        }
        if (errors.isNotEmpty()) {
            return TransitionResult(current, errors)
        }

        val newState = current.copy(
            recurrenceRule = action.recurrenceRule,
            recurrenceBasis = action.recurrenceBasis,
        )
        return validateAndBuild(newState)
    }

    private fun applyClearRecurrence(current: ScheduleState): TransitionResult {
        val newState = current.copy(
            recurrenceRule = null,
            recurrenceBasis = null,
        )
        return validateAndBuild(newState)
    }

    private fun applyAdvanceRecurrence(
        current: ScheduleState,
        action: SchedulingAction.AdvanceRecurrence,
    ): TransitionResult {
        val newState = current.copy(
            dueDate = action.nextDueDate,
        )
        return validateAndBuild(newState)
    }

    private fun applyMarkReviewed(current: ScheduleState, referenceTimestamp: String): TransitionResult {
        val newState = current.copy(
            reviewedAt = referenceTimestamp,
        )
        return validateAndBuild(newState)
    }

    private fun applyAcknowledgeFollowUp(current: ScheduleState, notifiedOnDate: String): TransitionResult {
        val newState = current.copy(
            followUpNotifiedOn = notifiedOnDate,
        )
        return validateAndBuild(newState)
    }

    private fun validateAndBuild(state: ScheduleState): TransitionResult {
        val errors = TaskValidation.validate(
            TaskValidationInput(
                title = "VALIDATION_TITLE",
                dueDate = state.dueDate,
                bucket = state.bucket,
                waitingOn = state.waitingOn,
            ),
        )
        return TransitionResult(state, errors)
    }
}
