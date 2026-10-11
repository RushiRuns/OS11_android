package com.os11.app.service.pomodoro

enum class PomodoroPhase(val raw: String, val label: String) {
    WORK("work", "Focus"),
    SHORT_BREAK("short_break", "Short Break"),
    LONG_BREAK("long_break", "Long Break"),
    ;

    companion object {
        fun fromRaw(raw: String): PomodoroPhase = entries.firstOrNull { it.raw == raw } ?: WORK
    }
}

enum class PomodoroStatus {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED,
}

private const val DEFAULT_WORK_MINUTES = 25
private const val SECONDS_PER_MINUTE = 60
const val DEFAULT_WORK_SECONDS: Int = DEFAULT_WORK_MINUTES * SECONDS_PER_MINUTE
const val MILLIS_PER_SECOND: Long = 1000L

data class PomodoroState(
    val phase: PomodoroPhase = PomodoroPhase.WORK,
    val status: PomodoroStatus = PomodoroStatus.IDLE,
    val taskId: String? = null,
    val taskTitle: String? = null,
    val startTimestamp: Long = 0L,
    val plannedDurationSeconds: Int = DEFAULT_WORK_SECONDS,
    val elapsedBeforePauseSeconds: Int = 0,
    val endAlarmTime: Long = 0L,
    val completedSessionsInCycle: Int = 0,
    val sessionsBeforeLongBreak: Int = 4,
) {
    fun currentElapsedSeconds(nowMillis: Long = System.currentTimeMillis()): Int = when (status) {
        PomodoroStatus.RUNNING -> {
            val segment = maxOf(0L, (nowMillis - startTimestamp) / MILLIS_PER_SECOND).toInt()
            minOf(plannedDurationSeconds, elapsedBeforePauseSeconds + segment)
        }
        PomodoroStatus.PAUSED -> elapsedBeforePauseSeconds
        PomodoroStatus.COMPLETED -> plannedDurationSeconds
        PomodoroStatus.IDLE -> 0
    }

    fun remainingSeconds(nowMillis: Long = System.currentTimeMillis()): Int {
        val elapsed = currentElapsedSeconds(nowMillis)
        return maxOf(0, plannedDurationSeconds - elapsed)
    }

    fun progress(nowMillis: Long = System.currentTimeMillis()): Float {
        if (plannedDurationSeconds <= 0) return 0f
        return (currentElapsedSeconds(nowMillis).toFloat() / plannedDurationSeconds.toFloat()).coerceIn(0f, 1f)
    }

    val isActive: Boolean
        get() = status == PomodoroStatus.RUNNING || status == PomodoroStatus.PAUSED
}

data class PomodoroSessionItem(
    val id: String,
    val taskId: String?,
    val type: String,
    val durationSeconds: Int,
    val startedAt: String,
    val endedAt: String?,
    val wasCompleted: Boolean,
)
