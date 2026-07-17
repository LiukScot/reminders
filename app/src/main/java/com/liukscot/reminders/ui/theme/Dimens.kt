package com.liukscot.reminders.ui.theme

import androidx.compose.ui.unit.dp

// Material Flow spacing, radii & component sizes — mirror of the design
// system's tokens/spacing.css. Do not introduce ad-hoc px values elsewhere;
// add the missing token here. See docs/design-system.md.
object Dimens {
    // 4px spacing scale
    val sp1 = 4.dp
    val sp2 = 8.dp
    val sp3 = 12.dp
    val sp4 = 16.dp
    val sp6 = 24.dp
    val sp8 = 32.dp

    val screenEdge = 18.dp

    // Radii. radiusMd is the card/row corner: 12dp, deliberately tighter than the design system's
    // radius-md (14px) to read closer to stock Material 3 — a chosen deviation, not drift.
    val radiusMd = 12.dp
    val radiusXl = 24.dp

    // Components
    val navHeight = 62.dp
}
