package com.derekwinters.chores.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.derekwinters.chores.tokens.DesignTokens

/**
 * Iterations 3 and 4 of the design-token rollout (derekwinters/chores-web-docs#11, this repo's
 * #23/#24): thin Compose-typed accessors over the generated [DesignTokens] artifact so call
 * sites read `Space.sm` instead of `DesignTokens.Space.SM.dp`. No logic and no values of its
 * own — every constant here is a straight re-export of the artifact. Iteration 4 (#24) adds the
 * component tier ([PillBadgeTokens], [ChoreRowTokens], [CardTokens], [TopBarTokens], [pillShape])
 * from `DesignTokens.Component`, replacing the commented literals Iteration 3 left behind.
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

// --- Component tier (Iteration 4, issue #24) ---------------------------------------------------

/**
 * Resolves a component elevation *level index* (e.g. [DesignTokens.Component.CARD_ELEVATION_STAT]
 * = 2) onto the foundation 0/4/8/12/16dp elevation scale ([DesignTokens.Elevation]). Component
 * elevation tokens are indices into that scale, not dp values themselves.
 */
fun elevationForLevel(levelIndex: Int): Dp = when (levelIndex) {
    0 -> Elevations.level0
    1 -> Elevations.level1
    2 -> Elevations.level2
    3 -> Elevations.level3
    4 -> Elevations.level4
    else -> error("No elevation token for level index $levelIndex")
}

/**
 * Fully-rounded "pill" shape ([DesignTokens.Component.PILL_BADGE_RADIUS] = 9999dp). Compose
 * clamps a corner radius to half the shape's smaller dimension, so this renders identically to
 * `RoundedCornerShape(percent = 50)` at any badge height.
 */
val pillShape: RoundedCornerShape = RoundedCornerShape(DesignTokens.Component.PILL_BADGE_RADIUS.dp)

/** Pill badge (Activity Log's action/target badges) component tokens. */
object PillBadgeTokens {
    val paddingX: Dp = DesignTokens.Component.PILL_BADGE_PADDING_X.dp
    val paddingY: Dp = DesignTokens.Component.PILL_BADGE_PADDING_Y.dp
    val fillAlpha: Float = DesignTokens.Component.PILL_BADGE_FILL_ALPHA
}

/** Chore row (ChoreListScreen's per-chore card) component tokens. */
object ChoreRowTokens {
    val paddingOuterX: Dp = DesignTokens.Component.CHORE_ROW_PADDING_OUTER_X.dp
    val paddingOuterY: Dp = DesignTokens.Component.CHORE_ROW_PADDING_OUTER_Y.dp
    val paddingInner: Dp = DesignTokens.Component.CHORE_ROW_PADDING_INNER.dp

    /** Inner start padding when the row carries a status accent bar (bar width + gap). */
    val paddingInnerWithBar: Dp = DesignTokens.Component.CHORE_ROW_PADDING_INNER_WITH_BAR.dp
    val accentBarWidth: Dp = DesignTokens.Component.CHORE_ROW_ACCENT_BAR_WIDTH.dp
    val actionGap: Dp = DesignTokens.Component.CHORE_ROW_ACTION_GAP.dp
}

/**
 * Card elevation component tokens, resolved from level indices via [elevationForLevel].
 * `resting`/`stat` currently have no elevated call site in this app (cards ride a
 * surfaceVariant container color instead — see DashboardScreen's per-person Card); they are
 * re-exported alongside `selected` so future call sites bind the same way.
 */
object CardTokens {
    val restingElevation: Dp = elevationForLevel(DesignTokens.Component.CARD_ELEVATION_RESTING)
    val statElevation: Dp = elevationForLevel(DesignTokens.Component.CARD_ELEVATION_STAT)
    val selectedElevation: Dp = elevationForLevel(DesignTokens.Component.CARD_ELEVATION_SELECTED)
}

/**
 * Top app bar component tokens. Height is deliberately not re-exported:
 * [DesignTokens.Component.TOP_BAR_HEIGHT] (64) equals M3 `TopAppBar`'s default container
 * height, so the bar needs no override (see ChoresApp's `TopAppBar`).
 */
object TopBarTokens {
    val avatarSize: Dp = DesignTokens.Component.TOP_BAR_AVATAR_SIZE.dp
}
