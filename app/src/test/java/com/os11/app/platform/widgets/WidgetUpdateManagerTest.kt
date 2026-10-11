package com.os11.app.platform.widgets

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.os11.app.data.sync.TrackedWriter
import com.os11.domain.logic.sync.Operation
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WidgetUpdateManagerTest {

    private lateinit var context: Context
    private val trackedWriter: TrackedWriter = mockk(relaxed = true)
    private lateinit var widgetUpdateManager: WidgetUpdateManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        widgetUpdateManager = WidgetUpdateManager(context, trackedWriter)
    }

    @Test
    fun registers_as_listener_on_init() {
        verify(atLeast = 1) { trackedWriter.registerListener(widgetUpdateManager) }
    }

    @Test
    fun relevant_entity_write_requests_update() = runTest {
        // Writes to tasks should trigger update request without throwing
        widgetUpdateManager.onWriteCommitted("tasks", "task-101", Operation.UPSERT)
        widgetUpdateManager.onWriteCommitted("my_day", "task-101", Operation.UPSERT)
    }

    @Test
    fun non_relevant_entity_write_does_not_throw() = runTest {
        widgetUpdateManager.onWriteCommitted("pomodoro_sessions", "session-1", Operation.UPSERT)
        widgetUpdateManager.onWriteCommitted("sync_queue", "queue-1", Operation.DELETE)
    }
}
