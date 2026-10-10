package com.os11.domain.validation

import com.os11.domain.logic.validation.TaskValidation
import com.os11.domain.logic.validation.TaskValidationError
import com.os11.domain.logic.validation.TaskValidationInput
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskValidationTest {

    @Test
    fun valid_task_has_no_errors() {
        val input = TaskValidationInput(
            title = "Implement Phase 3",
            dueDate = "2026-10-15",
            scheduledStartMin = 540,
            scheduledDurationMin = 60,
            priority = 2,
        )
        val errors = TaskValidation.validate(input)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun blank_title_fails() {
        val input = TaskValidationInput(title = "   ")
        val errors = TaskValidation.validate(input)
        assertTrue(errors.contains(TaskValidationError.BlankTitle))
    }

    @Test
    fun bucket_and_due_date_conflict_fails() {
        val input = TaskValidationInput(
            title = "Task",
            dueDate = "2026-10-15",
            bucket = "someday",
        )
        val errors = TaskValidation.validate(input)
        assertTrue(errors.contains(TaskValidationError.BucketDueDateConflict))
    }

    @Test
    fun invalid_bucket_fails() {
        val input = TaskValidationInput(
            title = "Task",
            bucket = "invalid_bucket",
        )
        val errors = TaskValidation.validate(input)
        assertTrue(errors.contains(TaskValidationError.InvalidBucket))
    }

    @Test
    fun partial_or_invalid_schedule_fails() {
        val partial = TaskValidationInput(
            title = "Task",
            scheduledStartMin = 100,
            scheduledDurationMin = null,
        )
        assertTrue(TaskValidation.validate(partial).contains(TaskValidationError.PartialSchedule))

        val invalidStart = TaskValidationInput(
            title = "Task",
            scheduledStartMin = 1500,
            scheduledDurationMin = 30,
        )
        assertTrue(TaskValidation.validate(invalidStart).contains(TaskValidationError.InvalidScheduledStart))

        val invalidDuration = TaskValidationInput(
            title = "Task",
            scheduledStartMin = 100,
            scheduledDurationMin = 10, // < 15
        )
        assertTrue(TaskValidation.validate(invalidDuration).contains(TaskValidationError.InvalidScheduledDuration))
    }

    @Test
    fun waiting_on_too_long_fails() {
        val input = TaskValidationInput(
            title = "Task",
            waitingOn = "A".repeat(121),
        )
        val errors = TaskValidation.validate(input)
        assertTrue(errors.contains(TaskValidationError.WaitingOnTooLong))
    }

    @Test
    fun invalid_priority_fails() {
        val input = TaskValidationInput(
            title = "Task",
            priority = 5,
        )
        val errors = TaskValidation.validate(input)
        assertTrue(errors.contains(TaskValidationError.InvalidPriority))
    }
}
