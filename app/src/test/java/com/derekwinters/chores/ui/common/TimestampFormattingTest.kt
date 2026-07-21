package com.derekwinters.chores.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

/**
 * Issue #70: [formatRelativeTimestamp] must be deterministic when given an explicit reference
 * instant, so clock-based snapshot/preview rendering no longer drifts over calendar time. These are
 * plain JVM tests (no Robolectric) — the logic is pure `java.time`.
 */
class TimestampFormattingTest {

    private val now: Instant = Instant.parse("2026-07-16T08:00:00Z")

    @Test
    fun daysAgo_isMeasuredAgainstInjectedNow_notWallClock() {
        assertEquals(
            "2d ago",
            formatRelativeTimestamp(now.minus(Duration.ofDays(2)).toString(), now)
        )
        assertEquals(
            "3d ago",
            formatRelativeTimestamp(now.minus(Duration.ofDays(3)).toString(), now)
        )
    }

    @Test
    fun subMinute_rendersJustNow() {
        assertEquals("just now", formatRelativeTimestamp(now.minus(Duration.ofSeconds(30)).toString(), now))
    }

    @Test
    fun minutesAndHours_useTheirThresholds() {
        assertEquals("5m ago", formatRelativeTimestamp(now.minus(Duration.ofMinutes(5)).toString(), now))
        assertEquals("3h ago", formatRelativeTimestamp(now.minus(Duration.ofHours(3)).toString(), now))
    }

    @Test
    fun unparseableInput_fallsBackToRawString() {
        assertEquals("not-a-timestamp", formatRelativeTimestamp("not-a-timestamp", now))
    }
}
