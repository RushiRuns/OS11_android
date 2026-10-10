package com.os11.domain.logic.validation

/**
 * Task validation error types.
 */
sealed class TaskValidationError(val message: String) {
    data object BlankTitle : TaskValidationError("Task title cannot be blank")
    data object BucketDueDateConflict : TaskValidationError("A task cannot have both a bucket and a due date")
    data object InvalidBucket : TaskValidationError("Bucket must be either 'anytime' or 'someday'")
    data object PartialSchedule :
        TaskValidationError("scheduled_start_min and scheduled_duration_min must both be set or both null")
    data object InvalidScheduledStart : TaskValidationError("scheduled_start_min must be between 0 and 1439")
    data object InvalidScheduledDuration :
        TaskValidationError("scheduled_duration_min must be between 15 and 480 minutes")
    data object WaitingOnTooLong : TaskValidationError("waiting_on description exceeds 120 characters")
    data object InvalidPriority : TaskValidationError("Priority must be between 0 (None) and 4 (Critical)")
}

data class TaskValidationInput(
    val title: String,
    val dueDate: String? = null,
    val bucket: String? = null,
    val scheduledStartMin: Int? = null,
    val scheduledDurationMin: Int? = null,
    val waitingOn: String? = null,
    val priority: Int = 0,
)

object TaskValidation {
    private const val MAX_WAITING_ON_LENGTH = 120
    private const val MAX_DAY_MINUTES = 1440
    private const val MIN_DURATION_MINUTES = 15
    private const val MAX_DURATION_MINUTES = 480
    private const val MIN_PRIORITY = 0
    private const val MAX_PRIORITY = 4

    fun validate(input: TaskValidationInput): List<TaskValidationError> {
        val errors = mutableListOf<TaskValidationError>()
        validateTitle(input.title, errors)
        validateBucket(input.bucket, input.dueDate, errors)
        validateSchedule(input.scheduledStartMin, input.scheduledDurationMin, errors)
        validateWaitingOn(input.waitingOn, errors)
        validatePriority(input.priority, errors)
        return errors
    }

    private fun validateTitle(title: String, errors: MutableList<TaskValidationError>) {
        if (title.trim().isEmpty()) {
            errors.add(TaskValidationError.BlankTitle)
        }
    }

    private fun validateBucket(bucket: String?, dueDate: String?, errors: MutableList<TaskValidationError>) {
        if (bucket != null) {
            if (bucket !in setOf("anytime", "someday")) {
                errors.add(TaskValidationError.InvalidBucket)
            }
            if (dueDate != null) {
                errors.add(TaskValidationError.BucketDueDateConflict)
            }
        }
    }

    private fun validateSchedule(startMin: Int?, durationMin: Int?, errors: MutableList<TaskValidationError>) {
        val hasStart = startMin != null
        val hasDuration = durationMin != null
        if (hasStart != hasDuration) {
            errors.add(TaskValidationError.PartialSchedule)
        } else if (hasStart && hasDuration) {
            val start = startMin ?: 0
            val duration = durationMin ?: 0
            if (start < 0 || start >= MAX_DAY_MINUTES) {
                errors.add(TaskValidationError.InvalidScheduledStart)
            }
            if (duration < MIN_DURATION_MINUTES || duration > MAX_DURATION_MINUTES) {
                errors.add(TaskValidationError.InvalidScheduledDuration)
            }
        }
    }

    private fun validateWaitingOn(waitingOn: String?, errors: MutableList<TaskValidationError>) {
        if (waitingOn != null && waitingOn.length > MAX_WAITING_ON_LENGTH) {
            errors.add(TaskValidationError.WaitingOnTooLong)
        }
    }

    private fun validatePriority(priority: Int, errors: MutableList<TaskValidationError>) {
        if (priority < MIN_PRIORITY || priority > MAX_PRIORITY) {
            errors.add(TaskValidationError.InvalidPriority)
        }
    }
}
