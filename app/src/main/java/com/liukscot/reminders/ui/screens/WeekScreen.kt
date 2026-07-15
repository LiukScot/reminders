package com.liukscot.reminders.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liukscot.reminders.R
import com.liukscot.reminders.data.Task
import com.liukscot.reminders.ui.theme.Dimens
import com.liukscot.reminders.ui.theme.MonoFontFamily
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Composable
fun WeekScreen(viewModel: WeekViewModel = rememberWeekViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val today = LocalDate.now()
    val zone = remember { ZoneId.systemDefault() }

    var sheetTarget by remember { mutableStateOf<ReminderSheetTarget>(ReminderSheetTarget.None) }
    var draggedTask by remember { mutableStateOf<Task?>(null) }
    var dragPosition by remember { mutableStateOf<Offset?>(null) }
    val dayTop = remember { mutableStateMapOf<LocalDate, Float>() }
    val dayBottom = remember { mutableStateMapOf<LocalDate, Float>() }

    fun hoveredDay(position: Offset?) = position?.let { pos ->
        state.days
            .map { it.date }
            .firstOrNull { date ->
                pos.y in (dayTop[date] ?: Float.MAX_VALUE)..(dayBottom[date] ?: Float.MIN_VALUE)
            }
    }

    val dropTargetDay = draggedTask?.let { task -> hoveredDay(dragPosition)?.takeIf { it != task.dueDate(zone) } }

    fun finishTaskDrag() {
        val task = draggedTask
        val destination = hoveredDay(dragPosition)
        if (task != null && destination != null) {
            viewModel.moveTaskToDay(task, destination)
        }
        draggedTask = null
        dragPosition = null
    }

    // ponytail: shorten only the long-press timeout so a drag starts sooner; touch slop and
    // everything else stays the platform default, so normal taps/scrolls are unaffected.
    val baseViewConfiguration = LocalViewConfiguration.current
    val fastDragViewConfiguration = remember(baseViewConfiguration) {
        object : ViewConfiguration by baseViewConfiguration {
            override val longPressTimeoutMillis: Long get() = 200L
        }
    }

    val entries = remember(state.days) { buildWeekEntries(state.days) }
    val todayIndex = remember(entries) { entries.indexOfFirst { it is WeekEntry.DayRow && it.day.date == today } }
    val listState = rememberLazyListState()
    var hasCenteredOnToday by remember { mutableStateOf(false) }
    LaunchedEffect(todayIndex) {
        if (!hasCenteredOnToday && todayIndex >= 0) {
            listState.scrollToItem(todayIndex)
            hasCenteredOnToday = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalViewConfiguration provides fastDragViewConfiguration) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = Dimens.screenEdge, top = 10.dp, end = Dimens.screenEdge, bottom = 165.dp),
            ) {
                items(entries, key = { it.key }) { entry ->
                    when (entry) {
                        is WeekEntry.MonthHeader -> MonthHeaderText(entry.date)
                        is WeekEntry.WeekLabel -> WeekLabelText(entry.mondayDate, today)
                        is WeekEntry.DayRow -> WeekDayRow(
                            day = entry.day,
                            isToday = entry.day.date == today,
                            isDropTarget = entry.day.date == dropTargetDay,
                            onPositioned = { bounds ->
                                dayTop[entry.day.date] = bounds.top
                                dayBottom[entry.day.date] = bounds.bottom
                            },
                            onToggleComplete = viewModel::toggleComplete,
                            isDragged = { task -> draggedTask?.id == task.id },
                            onDragStart = { task, position -> draggedTask = task; dragPosition = position },
                            onDrag = { dragPosition = it },
                            onDragEnd = ::finishTaskDrag,
                            onAddReminder = { sheetTarget = ReminderSheetTarget.New(entry.day.date) },
                            onEditTask = { task -> sheetTarget = ReminderSheetTarget.Edit(task, state.tagsByTaskId[task.id].orEmpty()) },
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { sheetTarget = ReminderSheetTarget.New(today) },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = Dimens.screenEdge, bottom = 88.dp),
        ) {
            Icon(painterResource(R.drawable.ic_plus), contentDescription = "Add reminder")
        }
    }

    when (val target = sheetTarget) {
        is ReminderSheetTarget.None -> Unit
        is ReminderSheetTarget.New -> ReminderSheet(
            lists = state.lists,
            preselectedListId = state.lists.firstOrNull()?.id,
            initialDate = target.date,
            onDismiss = { sheetTarget = ReminderSheetTarget.None },
            onSave = { title, notes, listId, tags, flagged, priority, dueAt, hasDueTime, recFreq, recInterval, recByDay, recAnchor ->
                viewModel.saveTask(
                    null, title, notes, listId, tags, flagged, priority, dueAt, hasDueTime,
                    recFreq, recInterval, recByDay, recAnchor,
                )
                sheetTarget = ReminderSheetTarget.None
            },
        )
        is ReminderSheetTarget.Edit -> ReminderSheet(
            lists = state.lists,
            existingTask = target.task,
            existingTags = target.tags,
            preselectedListId = target.task.listId,
            onDismiss = { sheetTarget = ReminderSheetTarget.None },
            onSave = { title, notes, listId, tags, flagged, priority, dueAt, hasDueTime, recFreq, recInterval, recByDay, recAnchor ->
                viewModel.saveTask(
                    target.task, title, notes, listId, tags, flagged, priority, dueAt, hasDueTime,
                    recFreq, recInterval, recByDay, recAnchor,
                )
                sheetTarget = ReminderSheetTarget.None
            },
        )
    }
}

private sealed interface WeekEntry {
    val key: String

    data class MonthHeader(val date: LocalDate) : WeekEntry {
        override val key get() = "month-${date.year}-${date.monthValue}"
    }

    data class WeekLabel(val mondayDate: LocalDate) : WeekEntry {
        override val key get() = "week-$mondayDate"
    }

    data class DayRow(val day: WeekDay) : WeekEntry {
        override val key get() = "day-${day.date}"
    }
}

private fun buildWeekEntries(days: List<WeekDay>): List<WeekEntry> {
    val entries = mutableListOf<WeekEntry>()
    days.forEachIndexed { index, day ->
        val previousDate = days.getOrNull(index - 1)?.date
        val isNewMonth = previousDate == null || previousDate.month != day.date.month || previousDate.year != day.date.year
        val isNewWeek = !isNewMonth && day.date.dayOfWeek == DayOfWeek.MONDAY
        when {
            isNewMonth -> entries += WeekEntry.MonthHeader(day.date)
            isNewWeek -> entries += WeekEntry.WeekLabel(day.date)
        }
        entries += WeekEntry.DayRow(day)
    }
    return entries
}

@Composable
private fun WeekDayRow(
    day: WeekDay,
    isToday: Boolean,
    isDropTarget: Boolean,
    onPositioned: (androidx.compose.ui.geometry.Rect) -> Unit,
    onToggleComplete: (Task) -> Unit,
    isDragged: (Task) -> Boolean,
    onDragStart: (Task, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onAddReminder: () -> Unit,
    onEditTask: (Task) -> Unit,
) {
    val shape = RoundedCornerShape(Dimens.radiusMd)
    val dropTargetTint by animateColorAsState(
        if (isDropTarget) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f) else Color.Transparent,
        // ponytail: snap the fade-out so a one-frame flicker from finger jitter at long-press
        // start can't linger as a slow-fading false highlight.
        animationSpec = if (isDropTarget) spring() else snap(),
        label = "dropTargetTint",
    )
    val background = when {
        isToday -> MaterialTheme.colorScheme.surface
        else -> dropTargetTint
    }

    Row(
        modifier = Modifier
            .padding(bottom = Dimens.sp1)
            .fillMaxWidth()
            .background(background, shape)
            .onGloballyPositioned { onPositioned(it.boundsInRoot()) }
            .padding(horizontal = Dimens.sp2, vertical = Dimens.sp3),
        horizontalArrangement = Arrangement.spacedBy(Dimens.sp3),
    ) {
        // ponytail: clickable lives on the label/dash, not the whole row — a row-wide clickable
        // would sit behind each task's own tap-to-edit and race it for the same gesture.
        DayLabelColumn(day.date, isToday, modifier = Modifier.clickable(onClick = onAddReminder))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (day.tasks.isEmpty()) {
                Text(
                    "—",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onAddReminder)
                        .padding(vertical = Dimens.sp2),
                )
            } else {
                day.tasks.forEach { task ->
                    DayTaskRow(
                        task = task,
                        trailingText = task.timeLabel(),
                        trailingColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        onToggleComplete = onToggleComplete,
                        isDragged = isDragged(task),
                        onDragStart = onDragStart,
                        onDrag = onDrag,
                        onDragEnd = onDragEnd,
                        onClick = { onEditTask(task) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthHeaderText(date: LocalDate) {
    Text(
        date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)),
        fontSize = 22.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.3).sp,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = Dimens.sp4, bottom = Dimens.sp2),
    )
}

@Composable
private fun WeekLabelText(mondayDate: LocalDate, today: LocalDate) {
    Text(
        weekLabel(mondayDate, today),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.4.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Dimens.sp3, bottom = Dimens.sp1),
    )
}

@Composable
private fun DayLabelColumn(date: LocalDate, isToday: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.width(44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            date.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)).uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        )
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(28.dp)
                .then(
                    if (isToday) Modifier.background(MaterialTheme.colorScheme.primary, CircleShape) else Modifier,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                date.dayOfMonth.toString(),
                fontFamily = MonoFontFamily,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

private fun weekLabel(mondayDate: LocalDate, today: LocalDate): String {
    val todayMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return when (ChronoUnit.WEEKS.between(todayMonday, mondayDate)) {
        0L -> "This week"
        1L -> "Next week"
        -1L -> "Last week"
        else -> "Week of ${mondayDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))}"
    }
}
