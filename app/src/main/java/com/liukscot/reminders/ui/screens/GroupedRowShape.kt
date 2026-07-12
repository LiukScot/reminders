package com.liukscot.reminders.ui.screens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

// Mirrors the mockup's `groupRadius()`: adjacent rows in a list join with a
// small radius, the first/last row of the group keep the full radius.
fun groupedRowShape(index: Int, count: Int, bigRadius: Dp, smallRadius: Dp): Shape {
    val top = if (index == 0) bigRadius else smallRadius
    val bottom = if (index == count - 1) bigRadius else smallRadius
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}
