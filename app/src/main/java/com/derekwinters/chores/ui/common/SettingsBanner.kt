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
            backgroundColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
            borderColor = MaterialTheme.colorScheme.error,
            textColor = MaterialTheme.colorScheme.error
        )
        BannerType.SUCCESS -> BannerColors(
            backgroundColor = Color(0xFF4CAF50).copy(alpha = 0.15f),  // Success green
            borderColor = Color(0xFF4CAF50),
            textColor = Color(0xFF2E7D32)  // Darker green for better contrast
        )
    }

    Box(
        modifier = modifier
            .testTag(if (type == BannerType.ERROR) "ErrorBanner" else "SuccessBanner")
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(
                width = 1.dp,
                color = colors.borderColor,
                shape = MaterialTheme.shapes.small
            )
            .background(
                color = colors.backgroundColor,
                shape = MaterialTheme.shapes.small
            )
            .padding(12.dp)
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
