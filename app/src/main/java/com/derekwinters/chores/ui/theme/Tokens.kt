package com.derekwinters.chores.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.derekwinters.chores.tokens.DesignTokens

/**
 * Iteration 3 of the design-token rollout (derekwinters/chores-web-docs#11, this repo's #23):
 * thin Compose-typed accessors over the generated [DesignTokens] artifact so call sites read
 * `Space.sm` instead of `DesignTokens.Space.SM.dp`. No logic and no values of its own — every
 * constant here is a straight re-export of the artifact. Component-level tokens (pill badge,
 * accent-bar inset, ...) arrive in Iteration 4 (#24); their few call sites keep commented
 * literals until then.
 */

/** Spacing scale (padding, gaps, offsets). */
object Space {
    val none: Dp = DesignTokens.Space.NONE.dp
    val xxs: Dp = DesignTokens.Space.XXS.dp
    val xs: Dp = DesignTokens.Space.XS.dp
    val sm: Dp = DesignTokens.Space.SM.dp
    val md: Dp = DesignTokens.Space.MD.dp
    val lg: Dp = DesignTokens.Space.LG.dp
    val xl: Dp = DesignTokens.Space.XL.dp
    val xxl: Dp = DesignTokens.Space.XXL.dp
    val xxxl: Dp = DesignTokens.Space.XXXL.dp
}

/** Corner radii ("Corner" rather than "Radius" to avoid reading as a circle radius). */
object Corner {
    val xs: Dp = DesignTokens.Radius.XS.dp
    val sm: Dp = DesignTokens.Radius.SM.dp
    val md: Dp = DesignTokens.Radius.MD.dp
}

/** Icon (and icon-sized indicator) sizes. Named IconSize: `Icon` is the M3 composable. */
object IconSize {
    val sm: Dp = DesignTokens.Icon.SM.dp
    val md: Dp = DesignTokens.Icon.MD.dp
    val lg: Dp = DesignTokens.Icon.LG.dp
}

/** Avatar circle sizes. */
object AvatarSize {
    val md: Dp = DesignTokens.Avatar.MD.dp
    val lg: Dp = DesignTokens.Avatar.LG.dp
}

/** Elevation steps for cards/shadows. */
object Elevations {
    val level0: Dp = DesignTokens.Elevation.ELEVATION_0.dp
    val level1: Dp = DesignTokens.Elevation.ELEVATION_1.dp
    val level2: Dp = DesignTokens.Elevation.ELEVATION_2.dp
    val level3: Dp = DesignTokens.Elevation.ELEVATION_3.dp
    val level4: Dp = DesignTokens.Elevation.ELEVATION_4.dp
}

/** Decorative stroke widths (borders, underline bars, accent bars). */
object StrokeWidth {
    val emphasis: Dp = DesignTokens.Stroke.EMPHASIS.dp
    val accentBar: Dp = DesignTokens.Stroke.ACCENT_BAR.dp
}

/** Layout max/fixed sizes. Named Sizes: `Size` is androidx.compose.ui.geometry.Size. */
object Sizes {
    val formMax: Dp = DesignTokens.Size.FORM_MAX.dp
}

/** Design alphas (tints, muting) — not for unrelated math. */
object TokenAlpha {
    val tint: Float = DesignTokens.Alpha.TINT
    val muted: Float = DesignTokens.Alpha.MUTED
}
