package com.os11.app.platform.widgets

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.os11.app.data.sync.TrackedWriteListener
import com.os11.app.data.sync.TrackedWriter
import com.os11.domain.logic.sync.Operation
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages home-screen widget update throttling per PERFORMANCE.md item 21:
 * "Widget updates are throttled and read a slim read-only query. A burst of writes causes one widget refresh."
 */
@OptIn(FlowPreview::class)
@Singleton
class WidgetUpdateManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val trackedWriter: TrackedWriter,
) : TrackedWriteListener {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val updateRequests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    init {
        trackedWriter.registerListener(this)
        scope.launch {
            updateRequests
                .debounce(DEBOUNCE_MS)
                .collect {
                    performUpdate()
                }
        }
    }

    override fun onWriteCommitted(entity: String, entityId: String, operation: Operation) {
        if (entity in RELEVANT_ENTITIES) {
            requestUpdate()
        }
    }

    fun requestUpdate() {
        updateRequests.tryEmit(Unit)
    }

    suspend fun performUpdate() {
        try {
            MyDayWidget().updateAll(context)
            QuickAddWidget().updateAll(context)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Glance updates may fail silently if no widget instances are placed or in unit test sandbox
        }
    }

    companion object {
        private const val DEBOUNCE_MS = 500L
        private val RELEVANT_ENTITIES = setOf("tasks", "my_day", "settings", "workspaces", "projects")
    }
}
