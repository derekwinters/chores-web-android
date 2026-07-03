package com.derekwinters.chores.ui.common

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Issue #36: shared home for the `Instant`-based timestamp-formatting logic independently
 * arrived at by issues #33/#34/#35/#36/#37 in this milestone, rather than five inline copies.
 * Both functions parse the wire value with [Instant.parse] (confirmed by #33 to be the
 * FastAPI/Pydantic v2 ISO-8601-with-`Z` serialization used across every timestamp field in this
 * API) and fall back to the raw string unchanged if parsing fails, rather than crashing or
 * hiding the field.
 */

/**
 * Date+time, locale-aware — matches web's `toLocaleString()` (used by #33's expanded detail,
 * #34's Auth Log, #35's Points Log, and #37's Settings/About screen).
 */
fun formatDateTime(iso: String): String {
    return try {
        val instant = Instant.parse(iso)
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault())
            .format(instant)
    } catch (e: DateTimeParseException) {
        iso
    }
}

/**
 * Date only, locale-aware — matches web's `toLocaleDateString()` (used by #36's User Detail
 * screen).
 */
fun formatDate(iso: String): String {
    return try {
        val instant = Instant.parse(iso)
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault())
            .format(instant)
    } catch (e: DateTimeParseException) {
        iso
    }
}
