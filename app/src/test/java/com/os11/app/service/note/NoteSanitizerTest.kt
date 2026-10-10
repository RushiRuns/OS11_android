package com.os11.app.service.note

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteSanitizerTest {

    @Test
    fun sanitize_emptyOrNull_returnsEmpty() {
        assertEquals("", NoteSanitizer.sanitize(null))
        assertEquals("", NoteSanitizer.sanitize(""))
        assertEquals("", NoteSanitizer.sanitize("   "))
    }

    @Test
    fun sanitize_preservesStandardFormattingTags() {
        val input = "<h1>Heading</h1><p>This is <strong>bold</strong>, <em>italic</em>, and <s>strikethrough</s>.</p>"
        val result = NoteSanitizer.sanitize(input)
        assertEquals(input, result)
    }

    @Test
    fun sanitize_preservesCodeAndBlockquotes() {
        val input = "<blockquote>A quote</blockquote><pre><code class=\"language-kotlin\">val x = 1</code></pre>"
        val result = NoteSanitizer.sanitize(input)
        assertEquals(input, result)
    }

    @Test
    fun sanitize_preservesTipTapTaskListAttributes() {
        val input = "<ul data-type=\"taskList\">" +
            "<li data-type=\"taskItem\" data-checked=\"true\">Done task</li>" +
            "<li data-type=\"taskItem\" data-checked=\"false\">Todo task</li></ul>"
        val result = NoteSanitizer.sanitize(input)
        assertTrue(result.contains("data-type=\"taskList\""))
        assertTrue(result.contains("data-type=\"taskItem\""))
        assertTrue(result.contains("data-checked=\"true\""))
        assertTrue(result.contains("data-checked=\"false\""))
    }

    @Test
    fun sanitize_stripsMaliciousScriptsAndEventHandlers() {
        val malicious = "<p>Hello</p><script>alert('xss');</script>" +
            "<img src=\"x\" onerror=\"alert(1)\"><a href=\"javascript:evil()\">Link</a>"
        val result = NoteSanitizer.sanitize(malicious)
        assertFalse(result.contains("script"))
        assertFalse(result.contains("alert"))
        assertFalse(result.contains("onerror"))
        assertFalse(result.contains("javascript:"))
        assertTrue(result.contains("<p>Hello</p>"))
    }

    @Test
    fun toPlainText_extractsReadableText() {
        val html = "<h2>Meeting Notes</h2><p>Discussed <strong>Phase 5</strong> deliverables.</p>"
        val plain = NoteSanitizer.toPlainText(html)
        assertEquals("Meeting Notes Discussed Phase 5 deliverables.", plain)
    }
}
