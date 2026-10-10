package com.os11.domain.result

/**
 * Standard typed application result.
 *
 * Architecture Rule 4 (ARCHITECTURE.md):
 * Services return typed results (AppResult). Exceptions do not cross a layer boundary into UI.
 * ViewModels map failures to UI state or effects.
 */
sealed interface AppResult<out T> {
    data class Ok<T>(val value: T) : AppResult<T>
    data class Err(val error: AppError) : AppResult<Nothing>

    val isOk: Boolean get() = this is Ok
    val isErr: Boolean get() = this is Err

    fun getOrNull(): T? = when (this) {
        is Ok -> value
        is Err -> null
    }

    fun getOrThrow(): T = when (this) {
        is Ok -> value
        is Err -> throw error.toThrowable()
    }

    companion object {
        fun <T> ok(value: T): AppResult<T> = Ok(value)
        fun err(error: AppError): AppResult<Nothing> = Err(error)
    }
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Ok -> AppResult.Ok(transform(value))
    is AppResult.Err -> this
}

inline fun <T, R> AppResult<T>.flatMap(transform: (T) -> AppResult<R>): AppResult<R> = when (this) {
    is AppResult.Ok -> transform(value)
    is AppResult.Err -> this
}

inline fun <T> AppResult<T>.getOrElse(onFailure: (AppError) -> T): T = when (this) {
    is AppResult.Ok -> value
    is AppResult.Err -> onFailure(error)
}

inline fun <T> AppResult<T>.onFailure(action: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Err) action(error)
    return this
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Ok) action(value)
    return this
}

/**
 * Standard application error categories per SYSTEM.md section 3.
 */
sealed interface AppError {
    data class Validation(val field: String, val reason: String) : AppError
    data object NotFound : AppError
    data class RuleViolation(val rule: String) : AppError
    data class Unexpected(val cause: Throwable) : AppError

    fun userMessage(): String = when (this) {
        is Validation -> if (field.isNotBlank()) "$field: $reason" else reason
        is NotFound -> "Item not found"
        is RuleViolation -> rule
        is Unexpected -> cause.message ?: "An unexpected error occurred"
    }

    fun toMessage(): String = userMessage()

    fun toThrowable(): Throwable = when (this) {
        is Unexpected -> cause
        else -> IllegalStateException(userMessage())
    }
}
