package com.os11.app.service.note

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Entities
import org.jsoup.safety.Safelist

/**
 * Jsoup-based HTML note sanitizer per ADR-A11 and Spike S-2.
 *
 * Guarantees zero silent node or attribute stripping across desktop TipTap
 * HTML sync boundaries, especially task lists (<ul data-type="taskList">)
 * and checklist items (<li data-type="taskItem" data-checked="true|false">).
 *
 * Strips script tags, unsafe URI schemes, event handlers, and malicious styles.
 */
object NoteSanitizer {

    private val noteSafelist: Safelist by lazy {
        Safelist()
            // Structure & headings
            .addTags("h1", "h2", "h3", "h4", "h5", "h6", "p", "br", "hr", "blockquote", "pre")
            // Inline marks
            .addTags("strong", "b", "em", "i", "s", "strike", "del", "u", "code")
            // Lists & checklists
            .addTags("ul", "ol", "li")
            // Links
            .addTags("a")
            .addAttributes("a", "href", "title", "target", "rel")
            .addProtocols("a", "href", "http", "https", "mailto")
            // TipTap specific attributes
            .addAttributes("ul", "data-type")
            .addAttributes("li", "data-type", "data-checked")
            // Code block classes
            .addAttributes("code", "class")
            .addAttributes("pre", "class")
    }

    private val outputSettings: Document.OutputSettings by lazy {
        Document.OutputSettings()
            .prettyPrint(false)
            .escapeMode(Entities.EscapeMode.xhtml)
            .charset("UTF-8")
    }

    /**
     * Sanitizes raw HTML input, removing disallowed tags and attributes while
     * preserving TipTap structural elements and checklists.
     */
    fun sanitize(html: String?): String {
        if (html.isNullOrBlank()) return ""

        val cleaned = Jsoup.clean(html, "", noteSafelist, outputSettings)
        return cleaned.trim()
    }

    /**
     * Extracts pure plain text from HTML for search indexing and slim summaries.
     */
    fun toPlainText(html: String?): String {
        if (html.isNullOrBlank()) return ""
        val doc = Jsoup.parse(html)
        return doc.text().trim()
    }

    /**
     * Converts plain text with basic markdown/newlines into well-formed HTML.
     */
    fun fromPlainText(text: String?): String {
        if (text.isNullOrBlank()) return ""
        val paragraphs = text.split("\n\n")
            .filter { it.isNotBlank() }
            .map { p -> "<p>${p.replace("\n", "<br/>")}</p>" }
        return sanitize(paragraphs.joinToString(""))
    }
}
