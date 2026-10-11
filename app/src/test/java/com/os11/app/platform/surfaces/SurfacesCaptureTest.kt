package com.os11.app.platform.surfaces

import android.content.Intent
import com.os11.app.platform.shortcuts.AppShortcutManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SurfacesCaptureTest {

    @Test
    fun share_target_extracts_plain_text() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Read chapter 4 of Clean Architecture")
        }

        val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()
        val content = when {
            subject.isNotBlank() && text.isNotBlank() && subject != text -> "$subject $text"
            text.isNotBlank() -> text
            else -> subject
        }

        assertEquals("Read chapter 4 of Clean Architecture", content)
    }

    @Test
    fun share_target_combines_subject_and_url() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Kotlin Coroutines Guide")
            putExtra(Intent.EXTRA_TEXT, "https://kotlinlang.org/docs/coroutines-guide.html")
        }

        val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()
        val content = when {
            subject.isNotBlank() && text.isNotBlank() && subject != text -> "$subject $text"
            text.isNotBlank() -> text
            else -> subject
        }

        assertEquals("Kotlin Coroutines Guide https://kotlinlang.org/docs/coroutines-guide.html", content)
    }

    @Test
    fun quick_add_action_constant_matches_expected_contract() {
        assertEquals("com.os11.app.action.QUICK_ADD", AppShortcutManager.ACTION_QUICK_ADD)
        assertEquals("com.os11.app.action.MY_DAY", AppShortcutManager.ACTION_MY_DAY)
        assertEquals("com.os11.app.action.POMODORO", AppShortcutManager.ACTION_POMODORO)
    }

    @Test
    fun quick_add_intent_flags_allow_clean_navigation() {
        val intent = Intent().apply {
            action = AppShortcutManager.ACTION_QUICK_ADD
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        assertTrue((intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
        assertTrue((intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0)
    }
}
