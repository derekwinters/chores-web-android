package com.derekwinters.chores.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.draw.shadow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.derekwinters.chores.data.model.ThemeOption
import com.derekwinters.chores.ui.UiState

/**
 * Issue #25: personal theme preference — a grid of "Default (household theme)"
 * plus every available theme; tapping applies immediately (no separate save step).
 *
 * Thin Hilt-wired wrapper around [ThemePreferenceContent].
 */
@Composable
fun ThemePreferenceScreen(modifier: Modifier = Modifier, viewModel: ThemePreferenceViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    ThemePreferenceContent(modifier = modifier, uiState = uiState, onSelectTheme = viewModel::selectTheme)
}

@Composable
fun ThemePreferenceContent(
    uiState: UiState<ThemePreferenceData>,
    onSelectTheme: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is UiState.Idle, is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is UiState.Error -> Text(
                modifier = Modifier.align(Alignment.Center).padding(Space.xl),
                text = uiState.message,
                color = MaterialTheme.colorScheme.error
            )
            is UiState.Success -> {
                val data = uiState.data
                // `defaultInfo` (from the non-admin-gated /v1/theme/default-info) is the household
                // default's true id/name regardless of override state; `current.theme` alone can't
                // be used for this label since it reflects the *resolved* (possibly overridden)
                // theme, not necessarily the default. Its colors are looked up from the full
                // catalog just to draw the swatch.
                val householdDefault = data.themes.firstOrNull { it.id == data.defaultInfo.id } ?: data.current.theme

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().padding(Space.md),
                    horizontalArrangement = Arrangement.spacedBy(Space.md),
                    verticalArrangement = Arrangement.spacedBy(Space.md)
                ) {
                    item {
                        ThemeOptionCard(
                            name = "Default (${data.defaultInfo.name})",
                            theme = householdDefault,
                            selected = !data.current.isPersonalOverride,
                            onClick = { onSelectTheme(null) }
                        )
                    }
                    items(data.themes, key = { it.id }) { theme ->
                        ThemeOptionCard(
                            name = theme.name,
                            theme = theme,
                            selected = data.current.isPersonalOverride && data.current.theme.id == theme.id,
                            onClick = { onSelectTheme(theme.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionCard(name: String, theme: ThemeOption, selected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (selected) {
                    Modifier.shadow(
                        elevation = Elevations.level4,
                        shape = RoundedCornerShape(Corner.md),
                        clip = false
                    )
                } else {
                    Modifier
                }
            ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) Elevations.level3 else Elevations.level1
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Space.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            Text(
                name,
                modifier = Modifier.padding(horizontal = Space.xs),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
            // Display all 4 preview colors (matching web: primary, secondary, accent, background)
            Row(
                modifier = Modifier.fillMaxWidth(),
                // Off-scale 6dp gap snapped to the nearest spacing token (4dp) per issue #23.
                horizontalArrangement = Arrangement.spacedBy(Space.xs, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ColorSwatch(color = theme.primary, contentDescription = "Primary color")
                ColorSwatch(color = theme.secondary, contentDescription = "Secondary color")
                ColorSwatch(color = theme.accent, contentDescription = "Accent color")
                ColorSwatch(color = theme.background, contentDescription = "Background color")
            }
        }
    }
}

@Composable
private fun ColorSwatch(color: String, contentDescription: String) {
    Box(
        modifier = Modifier
            // Off-scale 30dp swatch snapped to the 32dp spacing token per issue #23 (swatch
            // sizes ride the Space scale, like ThemeAdminScreen's 40dp/Space.xxxl swatches).
            .size(Space.xxl)
            .background(parseHexColor(color), RoundedCornerShape(Corner.xs))
    ) {
        // Empty box for color display
    }
}
