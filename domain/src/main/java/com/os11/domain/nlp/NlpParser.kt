package com.os11.domain.nlp

import kotlinx.serialization.Serializable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Serializable
enum class NlpTokenType {
    DATE,
    TIME,
    PRIORITY,
    TAG,
    AREA,
    PROJECT,
    RECURRENCE,
    POMODORO,
}

@Serializable
data class NlpTokenMatch(val type: NlpTokenType, val rawText: String, val startIndex: Int, val endIndex: Int)

@Serializable
data class NlpParseResult(
    val title: String,
    val dueDate: String? = null,
    val dueTime: String? = null,
    val priority: Int = 0,
    val areaName: String? = null,
    val projectName: String? = null,
    val tags: List<String> = emptyList(),
    val recurrenceRule: String? = null,
    val estimatedPomodoros: Int? = null,
    val matchedTokens: List<NlpTokenMatch> = emptyList(),
)

/**
 * Deterministic, offline NLP parser for Quick Add input text.
 * Strictly adheres to clean architecture rules: pure Kotlin in :domain, no Android dependencies.
 */
@Suppress(
    "TooManyFunctions",
    "ReturnCount",
    "MagicNumber",
    "MaxLineLength",
    "UnusedParameter",
    "CyclomaticComplexMethod",
    "LongMethod",
)
object NlpParser {

    private val ISO_DATE_REGEX = Regex("""\b(\d{4})-(\d{2})-(\d{2})\b""")
    private val RELATIVE_DAYS_REGEX = Regex("""\bin\s+(\d+)\s+days?\b""", RegexOption.IGNORE_CASE)
    private val RELATIVE_WEEKS_REGEX = Regex("""\bin\s+(\d+)\s+weeks?\b""", RegexOption.IGNORE_CASE)
    private val RELATIVE_MONTHS_REGEX = Regex("""\bin\s+(\d+)\s+months?\b""", RegexOption.IGNORE_CASE)

    private val MONTH_NAMES = listOf(
        "jan", "feb", "mar", "apr", "may", "jun",
        "jul", "aug", "sep", "oct", "nov", "dec",
    )

    private val MONTH_DAY_REGEX = Regex(
        """\b(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|""" +
            """aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\s+""" +
            """(\d{1,2})(?:st|nd|rd|th)?(?:,?\s+(\d{4}))?\b""",
        RegexOption.IGNORE_CASE,
    )

    private val DAY_MONTH_REGEX = Regex(
        """\b(\d{1,2})(?:st|nd|rd|th)?\s+(?:of\s+)?(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|""" +
            """may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)""" +
            """(?:,?\s+(\d{4}))?\b""",
        RegexOption.IGNORE_CASE,
    )

    private val TIME_12H_REGEX = Regex("""\b(?:at\s+)?(\d{1,2})(?::(\d{2}))?\s*(am|pm)\b""", RegexOption.IGNORE_CASE)
    private val TIME_24H_REGEX = Regex("""\b(?:at\s+)?([01]?\d|2[0-3]):([0-5]\d)\b""", RegexOption.IGNORE_CASE)

    private val PRIORITY_REGEX = Regex(
        """(?:^|\s)!(critical|crit|high|medium|med|low|1|2|3|4)(?=\s|$)""",
        RegexOption.IGNORE_CASE,
    )
    private val AREA_REGEX = Regex("""(?:^|\s)@([a-zA-Z0-9_\-]+)""")
    private val PROJECT_REGEX = Regex("""(?<!https?:)(?:^|\s)/([a-zA-Z0-9_\-]+)""")
    private val TAG_REGEX = Regex("""(?:^|\s)#([a-zA-Z0-9_\-]+)""")
    private val POMO_COUNT_REGEX = Regex(
        """\b(\d+)\s*(?:🍅|tomato(?:es)?|pomodoro(?:s)?)\b|\b(?:🍅|tomato(?:es)?|pomodoro(?:s)?)\s*(\d+)\b""",
        RegexOption.IGNORE_CASE,
    )

    fun parse(
        input: String,
        referenceDate: LocalDate = LocalDate.now(),
        referenceTime: LocalTime = LocalTime.now(),
    ): NlpParseResult {
        if (input.isBlank()) {
            return NlpParseResult(title = input.trim())
        }

        val matches = mutableListOf<NlpTokenMatch>()

        val priority = extractPriority(input, matches)
        val area = extractArea(input, matches)
        val project = extractProject(input, matches)
        val tags = extractTags(input, matches)
        val pomodoros = extractPomodoro(input, matches)
        val recurrence = extractRecurrence(input, matches)
        val dateTime = extractDateTime(input, referenceDate, referenceTime, matches)

        val cleanTitle = computeCleanTitle(input, matches)

        return NlpParseResult(
            title = cleanTitle.ifBlank { input.trim() },
            dueDate = dateTime.first,
            dueTime = dateTime.second,
            priority = priority,
            areaName = area,
            projectName = project,
            tags = tags,
            recurrenceRule = recurrence,
            estimatedPomodoros = pomodoros,
            matchedTokens = matches.sortedBy { it.startIndex },
        )
    }

    private fun extractPriority(text: String, matches: MutableList<NlpTokenMatch>): Int {
        val match = PRIORITY_REGEX.find(text) ?: return 0
        val value = match.groupValues[1].lowercase(Locale.US)
        val prio = when (value) {
            "critical", "crit", "4" -> 4
            "high", "3" -> 3
            "medium", "med", "2" -> 2
            "low", "1" -> 1
            else -> 0
        }
        val matchStart = match.range.first + (if (match.value.startsWith(" ")) 1 else 0)
        matches.add(NlpTokenMatch(NlpTokenType.PRIORITY, match.value.trim(), matchStart, match.range.last + 1))
        return prio
    }

    private fun extractArea(text: String, matches: MutableList<NlpTokenMatch>): String? {
        val match = AREA_REGEX.find(text) ?: return null
        val matchStart = match.range.first + (if (match.value.startsWith(" ")) 1 else 0)
        matches.add(NlpTokenMatch(NlpTokenType.AREA, match.value.trim(), matchStart, match.range.last + 1))
        return match.groupValues[1]
    }

    private fun extractProject(text: String, matches: MutableList<NlpTokenMatch>): String? {
        val match = PROJECT_REGEX.find(text) ?: return null
        val matchStart = match.range.first + (if (match.value.startsWith(" ")) 1 else 0)
        matches.add(NlpTokenMatch(NlpTokenType.PROJECT, match.value.trim(), matchStart, match.range.last + 1))
        return match.groupValues[1]
    }

    private fun extractTags(text: String, matches: MutableList<NlpTokenMatch>): List<String> {
        val tags = mutableListOf<String>()
        TAG_REGEX.findAll(text).forEach { match ->
            val matchStart = match.range.first + (if (match.value.startsWith(" ")) 1 else 0)
            matches.add(NlpTokenMatch(NlpTokenType.TAG, match.value.trim(), matchStart, match.range.last + 1))
            tags.add(match.groupValues[1])
        }
        return tags
    }

    private fun extractPomodoro(text: String, matches: MutableList<NlpTokenMatch>): Int? {
        val countMatch = POMO_COUNT_REGEX.find(text)
        if (countMatch != null) {
            val numStr = countMatch.groupValues[1].ifBlank { countMatch.groupValues[2] }
            val count = numStr.toIntOrNull() ?: 1
            matches.add(
                NlpTokenMatch(
                    NlpTokenType.POMODORO,
                    countMatch.value,
                    countMatch.range.first,
                    countMatch.range.last + 1,
                ),
            )
            return count
        }

        // Count individual tomato emojis
        val emojiRegex = Regex("""(🍅+)""")
        val emojiMatch = emojiRegex.find(text)
        if (emojiMatch != null) {
            val count = emojiMatch.value.codePointCount(0, emojiMatch.value.length)
            matches.add(
                NlpTokenMatch(
                    NlpTokenType.POMODORO,
                    emojiMatch.value,
                    emojiMatch.range.first,
                    emojiMatch.range.last + 1,
                ),
            )
            return count
        }

        return null
    }

    private fun extractRecurrence(text: String, matches: MutableList<NlpTokenMatch>): String? {
        val patterns = listOf(
            Regex("""\b(?:every\s+weekday|on\s+weekdays)\b""", RegexOption.IGNORE_CASE) to
                "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR",
            Regex("""\bevery\s+monday\b""", RegexOption.IGNORE_CASE) to "FREQ=WEEKLY;BYDAY=MO",
            Regex("""\bevery\s+tuesday\b""", RegexOption.IGNORE_CASE) to "FREQ=WEEKLY;BYDAY=TU",
            Regex("""\bevery\s+wednesday\b""", RegexOption.IGNORE_CASE) to "FREQ=WEEKLY;BYDAY=WE",
            Regex("""\bevery\s+thursday\b""", RegexOption.IGNORE_CASE) to "FREQ=WEEKLY;BYDAY=TH",
            Regex("""\bevery\s+friday\b""", RegexOption.IGNORE_CASE) to "FREQ=WEEKLY;BYDAY=FR",
            Regex("""\bevery\s+saturday\b""", RegexOption.IGNORE_CASE) to "FREQ=WEEKLY;BYDAY=SA",
            Regex("""\bevery\s+sunday\b""", RegexOption.IGNORE_CASE) to "FREQ=WEEKLY;BYDAY=SU",
            Regex("""\bevery\s+day\b""", RegexOption.IGNORE_CASE) to "FREQ=DAILY",
            Regex("""\bevery\s+week\b""", RegexOption.IGNORE_CASE) to "FREQ=WEEKLY",
            Regex("""\bevery\s+month\b""", RegexOption.IGNORE_CASE) to "FREQ=MONTHLY",
            Regex("""\bevery\s+year\b""", RegexOption.IGNORE_CASE) to "FREQ=YEARLY",
            Regex("""\bweekdays\b""", RegexOption.IGNORE_CASE) to "FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR",
            Regex("""\bdaily\b""", RegexOption.IGNORE_CASE) to "FREQ=DAILY",
            Regex("""\bweekly\b""", RegexOption.IGNORE_CASE) to "FREQ=WEEKLY",
            Regex("""\bmonthly\b""", RegexOption.IGNORE_CASE) to "FREQ=MONTHLY",
            Regex("""\b(?:yearly|annually)\b""", RegexOption.IGNORE_CASE) to "FREQ=YEARLY",
        )

        for ((regex, rule) in patterns) {
            val match = regex.find(text)
            if (match != null) {
                matches.add(
                    NlpTokenMatch(NlpTokenType.RECURRENCE, match.value, match.range.first, match.range.last + 1),
                )
                return rule
            }
        }
        return null
    }

    private fun extractDateTime(
        text: String,
        referenceDate: LocalDate,
        referenceTime: LocalTime,
        matches: MutableList<NlpTokenMatch>,
    ): Pair<String?, String?> {
        val dateResult = extractDate(text, referenceDate, matches)
        val timeResult = extractTime(text, matches)

        var finalDate = dateResult?.first
        var finalTime = timeResult

        if (dateResult?.second != null && finalTime == null) {
            finalTime = dateResult.second
        }

        return Pair(finalDate?.format(DateTimeFormatter.ISO_LOCAL_DATE), finalTime)
    }

    private fun extractDate(
        text: String,
        refDate: LocalDate,
        matches: MutableList<NlpTokenMatch>,
    ): Pair<LocalDate, String?>? {
        val isoMatch = ISO_DATE_REGEX.find(text)
        if (isoMatch != null) {
            val date = LocalDate.parse(isoMatch.value)
            matches.add(NlpTokenMatch(NlpTokenType.DATE, isoMatch.value, isoMatch.range.first, isoMatch.range.last + 1))
            return Pair(date, null)
        }

        val relativeKeywords = listOf(
            Regex("""\btoday\b""", RegexOption.IGNORE_CASE) to (refDate to null),
            Regex("""\b(?:tomorrow|tmrw)\b""", RegexOption.IGNORE_CASE) to (refDate.plusDays(1) to null),
            Regex("""\byesterday\b""", RegexOption.IGNORE_CASE) to (refDate.minusDays(1) to null),
            Regex("""\btonight\b""", RegexOption.IGNORE_CASE) to (refDate to "20:00"),
            Regex("""\bnext\s+week\b""", RegexOption.IGNORE_CASE) to (refDate.plusWeeks(1) to null),
            Regex("""\bend\s+of\s+month\b""", RegexOption.IGNORE_CASE) to
                (refDate.withDayOfMonth(refDate.lengthOfMonth()) to null),
        )

        for ((regex, datePair) in relativeKeywords) {
            val match = regex.find(text)
            if (match != null) {
                matches.add(NlpTokenMatch(NlpTokenType.DATE, match.value, match.range.first, match.range.last + 1))
                return datePair
            }
        }

        RELATIVE_DAYS_REGEX.find(text)?.let { m ->
            val days = m.groupValues[1].toLong()
            matches.add(NlpTokenMatch(NlpTokenType.DATE, m.value, m.range.first, m.range.last + 1))
            return Pair(refDate.plusDays(days), null)
        }

        RELATIVE_WEEKS_REGEX.find(text)?.let { m ->
            val weeks = m.groupValues[1].toLong()
            matches.add(NlpTokenMatch(NlpTokenType.DATE, m.value, m.range.first, m.range.last + 1))
            return Pair(refDate.plusWeeks(weeks), null)
        }

        RELATIVE_MONTHS_REGEX.find(text)?.let { m ->
            val months = m.groupValues[1].toLong()
            matches.add(NlpTokenMatch(NlpTokenType.DATE, m.value, m.range.first, m.range.last + 1))
            return Pair(refDate.plusMonths(months), null)
        }

        val monthDayMatch = parseMonthDay(text, refDate, matches)
        if (monthDayMatch != null) return monthDayMatch

        return parseDayOfWeek(text, refDate, matches)
    }

    private fun parseMonthDay(
        text: String,
        refDate: LocalDate,
        matches: MutableList<NlpTokenMatch>,
    ): Pair<LocalDate, String?>? {
        val m1 = MONTH_DAY_REGEX.find(text)
        if (m1 != null) {
            val monthIdx = MONTH_NAMES.indexOf(m1.groupValues[1].take(3).lowercase(Locale.US)) + 1
            val day = m1.groupValues[2].toInt()
            val year = m1.groupValues.getOrNull(3)?.toIntOrNull() ?: refDate.year
            matches.add(NlpTokenMatch(NlpTokenType.DATE, m1.value, m1.range.first, m1.range.last + 1))
            return Pair(LocalDate.of(year, monthIdx, day), null)
        }

        val m2 = DAY_MONTH_REGEX.find(text)
        if (m2 != null) {
            val day = m2.groupValues[1].toInt()
            val monthIdx = MONTH_NAMES.indexOf(m2.groupValues[2].take(3).lowercase(Locale.US)) + 1
            val year = m2.groupValues.getOrNull(3)?.toIntOrNull() ?: refDate.year
            matches.add(NlpTokenMatch(NlpTokenType.DATE, m2.value, m2.range.first, m2.range.last + 1))
            return Pair(LocalDate.of(year, monthIdx, day), null)
        }
        return null
    }

    private fun parseDayOfWeek(
        text: String,
        refDate: LocalDate,
        matches: MutableList<NlpTokenMatch>,
    ): Pair<LocalDate, String?>? {
        val dowRegex = Regex(
            """\b(?:(next|this|on)\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|wed|thu|fri|sat|sun)\b""",
            RegexOption.IGNORE_CASE,
        )
        val match = dowRegex.find(text) ?: return null

        val prefix = match.groupValues[1].lowercase(Locale.US)
        val dowStr = match.groupValues[2].lowercase(Locale.US)
        val targetDow = when (dowStr.take(3)) {
            "mon" -> DayOfWeek.MONDAY
            "tue" -> DayOfWeek.TUESDAY
            "wed" -> DayOfWeek.WEDNESDAY
            "thu" -> DayOfWeek.THURSDAY
            "fri" -> DayOfWeek.FRIDAY
            "sat" -> DayOfWeek.SATURDAY
            "sun" -> DayOfWeek.SUNDAY
            else -> return null
        }

        val resolvedDate = if (prefix == "next") {
            refDate.with(TemporalAdjusters.next(targetDow))
        } else {
            val candidate = refDate.with(TemporalAdjusters.nextOrSame(targetDow))
            if (candidate == refDate && prefix != "this" && prefix != "on") {
                refDate.with(TemporalAdjusters.next(targetDow))
            } else {
                candidate
            }
        }

        matches.add(NlpTokenMatch(NlpTokenType.DATE, match.value, match.range.first, match.range.last + 1))
        return Pair(resolvedDate, null)
    }

    private fun extractTime(text: String, matches: MutableList<NlpTokenMatch>): String? {
        val namedTimes = listOf(
            Regex("""\b(?:in the\s+)?morning\b""", RegexOption.IGNORE_CASE) to "09:00",
            Regex("""\b(?:in the\s+)?afternoon\b""", RegexOption.IGNORE_CASE) to "14:00",
            Regex("""\b(?:in the\s+)?evening\b""", RegexOption.IGNORE_CASE) to "18:00",
            Regex("""\b(?:at\s+)?night\b""", RegexOption.IGNORE_CASE) to "21:00",
            Regex("""\bnoon\b""", RegexOption.IGNORE_CASE) to "12:00",
            Regex("""\bmidnight\b""", RegexOption.IGNORE_CASE) to "00:00",
        )
        for ((regex, timeStr) in namedTimes) {
            val match = regex.find(text)
            if (match != null) {
                matches.add(NlpTokenMatch(NlpTokenType.TIME, match.value, match.range.first, match.range.last + 1))
                return timeStr
            }
        }

        val m12 = TIME_12H_REGEX.find(text)
        if (m12 != null) {
            var hour = m12.groupValues[1].toInt()
            val min = m12.groupValues[2].ifBlank { "0" }.toInt()
            val ampm = m12.groupValues[3].lowercase(Locale.US)
            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0
            val formatted = String.format(Locale.US, "%02d:%02d", hour, min)
            matches.add(NlpTokenMatch(NlpTokenType.TIME, m12.value, m12.range.first, m12.range.last + 1))
            return formatted
        }

        val m24 = TIME_24H_REGEX.find(text)
        if (m24 != null) {
            val hour = m24.groupValues[1].toInt()
            val min = m24.groupValues[2].toInt()
            val formatted = String.format(Locale.US, "%02d:%02d", hour, min)
            matches.add(NlpTokenMatch(NlpTokenType.TIME, m24.value, m24.range.first, m24.range.last + 1))
            return formatted
        }

        return null
    }

    private fun computeCleanTitle(input: String, matches: List<NlpTokenMatch>): String {
        val sb = StringBuilder(input)
        // Blank out matched ranges from end to start to keep indices valid
        for (m in matches.sortedByDescending { it.startIndex }) {
            val start = m.startIndex.coerceIn(0, sb.length)
            val end = m.endIndex.coerceIn(start, sb.length)
            sb.replace(start, end, " ")
        }
        return sb.toString()
            .replace(Regex("""\s+"""), " ")
            .trim()
            .trim(',', '.', ';', '-', ':', ' ')
    }
}
