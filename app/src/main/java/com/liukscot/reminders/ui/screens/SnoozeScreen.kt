package com.liukscot.reminders.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liukscot.reminders.data.SnoozeChoice
import com.liukscot.reminders.data.SnoozeKind
import com.liukscot.reminders.ui.theme.MonoFontFamily
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// The in-app snooze picker. No mockup screen exists for it, so it borrows the grouped-row look of
// the settings/list rows. `now` is passed in rather than read here so the labels stay testable and
// match the choices the caller computed.
@Composable
fun SnoozeScreen(
    title: String,
    now: LocalDateTime,
    choices: List<SnoozeChoice>,
    onPick: (SnoozeChoice) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 20.dp)) {
        Text(
            text = "Snooze",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = title,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp),
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            itemsIndexed(choices, key = { _, c -> c.kind.name }) { index, choice ->
                SnoozeRow(
                    label = choice.label(),
                    time = choice.timeLabel(now),
                    shape = groupedRowShape(index, choices.size, bigRadius = 14.dp, smallRadius = 4.dp),
                    onClick = { onPick(choice) },
                )
            }
        }
    }
}

@Composable
private fun SnoozeRow(
    label: String,
    time: String?,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (time != null) {
            Text(
                text = time,
                fontFamily = MonoFontFamily,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun SnoozeChoice.label(): String = when (kind) {
    SnoozeKind.Plus15m -> "15 minutes"
    SnoozeKind.Plus1h -> "1 hour"
    SnoozeKind.Later -> "Later"
    SnoozeKind.Tomorrow -> "Tomorrow"
    SnoozeKind.Weekend -> "This weekend"
    SnoozeKind.Quick -> "Quick snooze"
    SnoozeKind.PickDateTime -> "Pick date & time…"
}

private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

// The trailing time is the "when will it come back" hint. A same-day snooze shows just the clock;
// anything landing on another date leads with the weekday so it doesn't read as today.
private fun SnoozeChoice.timeLabel(now: LocalDateTime): String? {
    val target = at ?: return null
    val clock = target.toLocalTime().format(TimeFormatter)
    if (target.toLocalDate() == now.toLocalDate()) return clock
    val day = target.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    val prefix = if (target.toLocalDate() == now.toLocalDate().plusDays(1)) "Tomorrow" else day
    return "$prefix $clock"
}
