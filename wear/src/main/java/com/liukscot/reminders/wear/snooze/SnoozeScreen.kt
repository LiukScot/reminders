package com.liukscot.reminders.wear.snooze

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.liukscot.reminders.data.SnoozeChoice
import com.liukscot.reminders.data.SnoozeKind
import com.liukscot.reminders.ui.theme.EmberTextOnAccent
import com.liukscot.reminders.wear.R
import com.liukscot.reminders.wear.voice.EmberGradient
import com.liukscot.reminders.wear.voice.GradientCircle
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// ScalingLazyColumn is the whole reason this list is on Wear rather than a plain Column: it scales
// and fades rows toward the top and bottom of the circle, so a row is never wider than the glass
// available at its height. It also brings rotary scrolling for free.
@Composable
fun SnoozeScreen(
    now: LocalDateTime,
    choices: List<SnoozeChoice>,
    onPick: (SnoozeChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = listState, modifier = modifier) { contentPadding ->
        ScalingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            item {
                Text(
                    text = "SNOOZE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            items(choices) { choice ->
                SnoozeRow(
                    label = choice.label(),
                    time = choice.timeLabel(now),
                    onClick = { onPick(choice) },
                )
            }
        }
    }
}

@Composable
private fun SnoozeRow(label: String, time: String?, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (time != null) {
            Text(
                text = time,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Held for a moment after picking, then the activity finishes. It names the resulting time rather
// than saying "Done" — which time you got was the entire point of the interaction.
@Composable
fun SnoozeConfirmation(at: LocalDateTime, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(13.dp, Alignment.CenterVertically),
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            modifier = Modifier.size(59.dp).clip(CircleShape).background(EmberGradient),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = EmberTextOnAccent,
                modifier = Modifier.size(28.dp),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Snoozed to",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = at.format(HOUR_MINUTE),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// The watch's own reminder notification opens straight onto the picker, so it needs a header that
// says which reminder is being moved.
@Composable
fun SnoozeHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = modifier.padding(horizontal = 16.dp),
    )
}

private val HOUR_MINUTE: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

// Labels mirror the phone's SnoozeScreen exactly. They live here rather than in the shared file
// because they are presentation, and the watch renders shorter strings than the phone would.
internal fun SnoozeChoice.label(): String = when (kind) {
    SnoozeKind.Plus15m -> "+15 min"
    SnoozeKind.Plus1h -> "+1 hour"
    SnoozeKind.Later -> "Later"
    SnoozeKind.Tomorrow -> "Tomorrow"
    SnoozeKind.Weekend -> "This weekend"
    SnoozeKind.Quick -> "Quick snooze"
    SnoozeKind.PickDateTime -> "Pick date & time"
}

internal fun SnoozeChoice.timeLabel(now: LocalDateTime): String? {
    val at = at ?: return null
    val sameDay = at.toLocalDate() == now.toLocalDate()
    return if (sameDay) {
        at.format(HOUR_MINUTE)
    } else {
        at.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    }
}
