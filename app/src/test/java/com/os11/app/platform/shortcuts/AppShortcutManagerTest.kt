package com.os11.app.platform.shortcuts

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppShortcutManagerTest {

    private lateinit var context: Context
    private lateinit var shortcutManager: AppShortcutManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shortcutManager = AppShortcutManager(context)
    }

    @Test
    fun computeShortcutIds_always_includes_quick_add() {
        val ids = shortcutManager.computeShortcutIds(emptySet())
        assertEquals(listOf(AppShortcutManager.ID_QUICK_ADD), ids)
    }

    @Test
    fun computeShortcutIds_includes_my_day_when_enabled() {
        val ids = shortcutManager.computeShortcutIds(setOf("my_day"))
        assertTrue(ids.contains(AppShortcutManager.ID_QUICK_ADD))
        assertTrue(ids.contains(AppShortcutManager.ID_MY_DAY))
        assertEquals(2, ids.size)
    }

    @Test
    fun computeShortcutIds_includes_pomodoro_when_enabled() {
        val ids = shortcutManager.computeShortcutIds(setOf("pomodoro"))
        assertTrue(ids.contains(AppShortcutManager.ID_QUICK_ADD))
        assertTrue(ids.contains(AppShortcutManager.ID_POMODORO))
        assertEquals(2, ids.size)
    }

    @Test
    fun computeShortcutIds_includes_all_when_all_enabled() {
        val ids = shortcutManager.computeShortcutIds(setOf("my_day", "pomodoro"))
        assertEquals(
            listOf(
                AppShortcutManager.ID_QUICK_ADD,
                AppShortcutManager.ID_MY_DAY,
                AppShortcutManager.ID_POMODORO,
            ),
            ids,
        )
    }

    @Test
    fun buildShortcutList_creates_intents_with_correct_actions() {
        val shortcuts = shortcutManager.buildShortcutList(setOf("my_day", "pomodoro"))
        assertEquals(3, shortcuts.size)

        val quickAdd = shortcuts.first { it.id == AppShortcutManager.ID_QUICK_ADD }
        assertEquals(AppShortcutManager.ACTION_QUICK_ADD, quickAdd.intent?.action)

        val myDay = shortcuts.first { it.id == AppShortcutManager.ID_MY_DAY }
        assertEquals(AppShortcutManager.ACTION_MY_DAY, myDay.intent?.action)

        val pomodoro = shortcuts.first { it.id == AppShortcutManager.ID_POMODORO }
        assertEquals(AppShortcutManager.ACTION_POMODORO, pomodoro.intent?.action)
    }
}
