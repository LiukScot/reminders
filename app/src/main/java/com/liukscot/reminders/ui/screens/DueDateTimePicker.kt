package com.liukscot.reminders.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liukscot.reminders.R
import com.liukscot.reminders.ui.theme.MonoFontFamily
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

// Ref: "7 - new reminder 2.png". The mockup's date/time controls are a
// scroll-snapping "wheel" for time; here time is a scrollable row of tap-to-
// select chips instead (same visual language as this sheet's other chip rows
// — Repeat/Priority — rather than hand-rolled scroll-snap-to-center physics).
// Section header + on/off Switch is shared visual shape for Date and Time.
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
                        text = "${displayedMonth.month.getDisplayName(JavaTextStyle.FULL, Locale.getDefault())} ${displayedMonth.year}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    CalNavButton(R.drawable.ic_chevron_right) { onMonthChange(displayedMonth.plusMonths(1)) }
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    DayOfWeek.entries.forEach { day ->
                        Text(
                            text = day.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()).take(2).uppercase(),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
                calendarCells(displayedMonth).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { day ->
                            Box(
                                modifier = Modifier.weight(1f).padding(1.dp).size(34.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (day != null) {
                                    val date = displayedMonth.atDay(day)
                                    val selected = date == selectedDate
                                    Text(
                                        text = day.toString(),
                                        fontFamily = MonoFontFamily,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                RoundedCornerShape(9.dp),
                                            )
                                            .clickable { onDateSelected(date) }
                                            .padding(vertical = 8.dp),
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

private val FIVE_MINUTE_TIMES = (0 until 288).map { LocalTime.MIDNIGHT.plusMinutes(it * 5L) }

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
            val listState = rememberLazyListState()
            val scope = rememberCoroutineScope()
            val selectedIndex = FIVE_MINUTE_TIMES.indexOf(selectedTime).coerceAtLeast(0)
            LaunchedEffect(Unit) { listState.scrollToItem((selectedIndex - 2).coerceAtLeast(0)) }
            LazyRow(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .padding(vertical = 12.dp, horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(FIVE_MINUTE_TIMES.size) { index ->
                    val time = FIVE_MINUTE_TIMES[index]
                    val selected = time == selectedTime
                    Text(
                        text = "%02d:%02d".format(time.hour, time.minute),
                        fontFamily = MonoFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                RoundedCornerShape(10.dp),
                            )
                            .clickable {
                                onTimeSelected(time)
                                scope.launch { listState.animateScrollToItem((index - 2).coerceAtLeast(0)) }
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}
