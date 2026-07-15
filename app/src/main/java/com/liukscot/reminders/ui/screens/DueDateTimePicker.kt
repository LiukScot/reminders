package com.liukscot.reminders.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liukscot.reminders.R
import com.liukscot.reminders.ui.theme.MonoFontFamily
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

// Android's default font padding adds asymmetric vertical space around
// glyphs, which throws off centering in tight pill/wheel containers —
// disabling it (same fix already used for the list-screen header) is what
// makes a single centered digit actually sit in the middle of its box.
private val CenteredNumberStyle = TextStyle(
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.Both),
)

// Ref: "7 - new reminder 2.png". Section header + on/off Switch is shared
// visual shape for Date and Time.
@Composable
private fun SectionHeader(label: String, enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.3.sp,
            color = MaterialTheme.colorScheme.outline,
        )
        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange,
            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
fun DueDateSection(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    displayedMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
) {
    Column {
        SectionHeader("DATE", enabled, onEnabledChange)
        if (enabled) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .padding(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CalNavButton(R.drawable.ic_chevron_left) { onMonthChange(displayedMonth.minusMonths(1)) }
                    Text(
                        text = "${displayedMonth.month.getDisplayName(JavaTextStyle.FULL, Locale.ENGLISH)} ${displayedMonth.year}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    CalNavButton(R.drawable.ic_chevron_right) { onMonthChange(displayedMonth.plusMonths(1)) }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    DayOfWeek.entries.forEach { day ->
                        Text(
                            text = day.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH).take(2).uppercase(),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
                val today = remember { LocalDate.now() }
                calendarCells(displayedMonth).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { day ->
                            Box(
                                modifier = Modifier.weight(1f).padding(2.dp).aspectRatio(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (day != null) {
                                    val date = displayedMonth.atDay(day)
                                    val selected = date == selectedDate
                                    val isToday = date == today
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                when {
                                                    selected -> MaterialTheme.colorScheme.primary
                                                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                                    else -> Color.Transparent
                                                },
                                                RoundedCornerShape(10.dp),
                                            )
                                            .clickable { onDateSelected(date) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = day.toString(),
                                            style = CenteredNumberStyle,
                                            fontFamily = MonoFontFamily,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = when {
                                                selected -> MaterialTheme.colorScheme.onPrimary
                                                isToday -> MaterialTheme.colorScheme.primary
                                                else -> MaterialTheme.colorScheme.onSurface
                                            },
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalNavButton(icon: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(16.dp),
        )
    }
}

private fun calendarCells(month: YearMonth): List<List<Int?>> {
    val leadingBlanks = month.atDay(1).dayOfWeek.value - 1
    val cells = List(leadingBlanks) { null } + (1..month.lengthOfMonth()).toList()
    val trailingBlanks = (7 - cells.size % 7) % 7
    return (cells + List(trailingBlanks) { null }).chunked(7)
}

private val QUARTER_HOUR_TIMES = (0 until 96).map { LocalTime.MIDNIGHT.plusMinutes(it * 15L) }

// Steepens the wheel's velocity→distance curve so a hard flick travels much further while gentle
// swipes stay precise for single-step nudges. Below rampStart the fling is untouched; between
// rampStart and rampEnd the boost ramps 1x→maxBoost. The ramp uses sqrt(t) (concave/front-loaded)
// so medium-power swings already get most of the boost instead of sitting at the linear midpoint.
// Thresholds are dp/s (converted to px/s at the call site) to feel the same across screen densities.
private const val WheelFlingRampStartDpPerSec = 2000f
private const val WheelFlingRampEndDpPerSec = 9000f
private const val WheelFlingMaxBoost = 3f

private class SteepFlingBehavior(
    private val base: FlingBehavior,
    private val rampStartPx: Float,
    private val rampEndPx: Float,
    private val maxBoost: Float,
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        val t = ((abs(initialVelocity) - rampStartPx) / (rampEndPx - rampStartPx)).coerceIn(0f, 1f)
        val boosted = initialVelocity * (1f + (maxBoost - 1f) * sqrt(t))
        return with(base) { performFling(boosted) }
    }
}

private fun nearestQuarterHour(time: LocalTime): LocalTime {
    val roundedMinutes = ((time.toSecondOfDay() / 60 + 7) / 15 * 15) % (24 * 60)
    return LocalTime.of(roundedMinutes / 60, roundedMinutes % 60)
}

// True scroll-snap "wheel": a highlight box fixed at the center of the strip,
// content scrolls under it, and whichever item settles at center becomes the
// selection — matching the mockup's wheelTimes behavior exactly (not a
// tap-to-select chip list).
@Composable
fun DueTimeSection(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    selectedTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
) {
    Column {
        SectionHeader("TIME", enabled, onEnabledChange)
        if (enabled) {
            val wheelHeight = 70.dp
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .height(wheelHeight)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
            ) {
                val itemWidth = 64.dp
                val sidePadding = (maxWidth - itemWidth) / 2
                val listState = rememberLazyListState()
                val density = LocalDensity.current
                val snapFling = rememberSnapFlingBehavior(listState)
                val wheelFling = remember(snapFling, density) {
                    SteepFlingBehavior(
                        base = snapFling,
                        rampStartPx = with(density) { WheelFlingRampStartDpPerSec.dp.toPx() },
                        rampEndPx = with(density) { WheelFlingRampEndDpPerSec.dp.toPx() },
                        maxBoost = WheelFlingMaxBoost,
                    )
                }
                val selectedIndex = remember(selectedTime) {
                    QUARTER_HOUR_TIMES.indexOf(nearestQuarterHour(selectedTime))
                }
                LaunchedEffect(Unit) { listState.scrollToItem(selectedIndex) }

                val centeredIndex by remember {
                    derivedStateOf {
                        val info = listState.layoutInfo
                        val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
                        info.visibleItemsInfo.minByOrNull { abs((it.offset + it.size / 2) - center) }?.index
                    }
                }
                LaunchedEffect(listState.isScrollInProgress) {
                    if (!listState.isScrollInProgress) {
                        centeredIndex?.let { onTimeSelected(QUARTER_HOUR_TIMES[it]) }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(itemWidth)
                        .height(52.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                )
                LazyRow(
                    state = listState,
                    flingBehavior = wheelFling,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(wheelHeight),
                    contentPadding = PaddingValues(horizontal = sidePadding),
                ) {
                    items(QUARTER_HOUR_TIMES.size) { index ->
                        val time = QUARTER_HOUR_TIMES[index]
                        val isCentered = index == centeredIndex
                        Box(
                            modifier = Modifier
                                .width(itemWidth)
                                .height(wheelHeight),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "%02d:%02d".format(time.hour, time.minute),
                                style = CenteredNumberStyle,
                                fontFamily = MonoFontFamily,
                                fontSize = 16.sp,
                                fontWeight = if (isCentered) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isCentered) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                0f to MaterialTheme.colorScheme.surfaceVariant,
                                0.14f to Color.Transparent,
                                0.86f to Color.Transparent,
                                1f to MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ),
                )
            }
        }
    }
}
