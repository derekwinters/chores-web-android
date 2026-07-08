package com.derekwinters.chores.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Parses a `#RRGGBB` or `#AARRGGBB` hex string (chores-web's theme color format, issue #24) into
 * a Compose [Color]. Falls back to [fallback] for malformed input rather than crashing, since
 * these strings come from the network.
 */
fun parseHexColor(hex: String, fallback: Color = Color.Gray): Color {
    val cleaned = hex.removePrefix("#")
    val argb = when (cleaned.length) {
        6 -> "FF$cleaned"
        8 -> cleaned
        else -> return fallback
    }
    return runCatching { Color(argb.toLong(16).toInt()) }.getOrDefault(fallback)
}

/**
 * Whether [hex] is a color [parseHexColor] can parse without falling back — i.e. `#RRGGBB` or
 * `#AARRGGBB` (leading `#` optional, mirroring [parseHexColor]). Issue #130 uses this to gate the
 * theme color editor's Save button on all 9 fields being valid.
 */
fun isValidHexColor(hex: String): Boolean {
    val cleaned = hex.removePrefix("#")
    return (cleaned.length == 6 || cleaned.length == 8) &&
        cleaned.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }
}
