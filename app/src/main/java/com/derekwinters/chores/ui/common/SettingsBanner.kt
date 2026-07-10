package com.derekwinters.chores.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.derekwinters.chores.tokens.DesignTokens
import com.derekwinters.chores.ui.theme.Space
import com.derekwinters.chores.ui.theme.TokenAlpha

/**
 * A reusable banner component for displaying error and success messages in Settings screens.
 * Provides Material3-styled bordered and tinted backgrounds for visual prominence.
 *
 * Issue #116: Replaces plain text error/success feedback with styled banners.
 */
@Composable
fun SettingsBanner(
    message: String,
    type: BannerType = BannerType.ERROR,
    modifier: Modifier = Modifier
) {
    val colors = when (type) {
        BannerType.ERROR -> BannerColors(
            backgroundColor = MaterialTheme.colorScheme.error.copy(alpha = TokenAlpha.tint),
            borderColor = MaterialTheme.colorScheme.error,
            textColor = MaterialTheme.colorScheme.error
        )
        // Issue #23: success has no first-class Material3 ColorScheme slot (see ChoresTheme's
        // doc), so the hardcoded greens are retired in favor of the design-token success role,
        // tinted for the fill like web's banner treatment.
        BannerType.SUCCESS -> BannerColors(
            backgroundColor = Color(DesignTokens.ColorDark.SUCCESS).copy(alpha = TokenAlpha.tint),
            borderColor = Color(DesignTokens.ColorDark.SUCCESS),
            textColor = Color(DesignTokens.ColorDark.SUCCESS)
        )
    }

    Box(
        modifier = modifier
            .testTag(if (type == BannerType.ERROR) "ErrorBanner" else "SuccessBanner")
            .fillMaxWidth()
            .padding(vertical = Space.sm)
            .border(
                width = 1.dp,
                color = colors.borderColor,
                shape = MaterialTheme.shapes.small
            )
            .background(
                color = colors.backgroundColor,
                shape = MaterialTheme.shapes.small
            )
            .padding(Space.md)
    ) {
        Text(
            text = message,
            color = colors.textColor,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Enum for banner types: ERROR or SUCCESS
 */
enum class BannerType {
    ERROR,
    SUCCESS
}

/**
 * Data class holding the color configuration for a banner type
 */
private data class BannerColors(
    val backgroundColor: Color,
    val borderColor: Color,
    val textColor: Color
)
