package com.liukscot.reminders.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.liukscot.reminders.R
import com.liukscot.reminders.data.Task
import com.liukscot.reminders.ui.theme.CheckboxIdleBorder
import com.liukscot.reminders.ui.theme.Dimens
import com.liukscot.reminders.ui.theme.MonoFontFamily
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

internal enum class DaySlot(val label: String, val icon: Int, val scheduledTime: LocalTime) {
    Morning("Morning", R.drawable.ic_sunrise, LocalTime.of(9, 0)),
    Afternoon("Afternoon", R.drawable.ic_sun, LocalTime.of(15, 0)),
    Evening("Evening", R.drawable.ic_moon, LocalTime.of(18, 0)),
}

internal sealed interface ReminderSheetTarget {
    data object None : ReminderSheetTarget
    data class New(val date: LocalDate) : ReminderSheetTarget
    data class Edit(val task: Task, val tags: List<String>) : ReminderSheetTarget
}

@Composable
fun DayScreen(viewModel: DayViewModel = rememberDayViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var sheetTarget by remember { mutableStateOf<ReminderSheetTarget>(ReminderSheetTarget.None) }
    var draggedTask by remember { mutableStateOf<Task?>(null) }
    var dragPosition by remember { mutableStateOf<Offset?>(null) }
    // ponytail: drop target = last section header above the finger, not a precise per-row
    // hit box — cheaper to track and more forgiving (dropping past the last row still counts).
    val slotHeaderTop = remember { mutableStateMapOf<DaySlot, Float>() }
    val slotSectionBottom = remember { mutableStateMapOf<DaySlot, Float>() }

    fun hoveredSlot(position: Offset?) = position?.let { pos ->
        DaySlot.entries
            .filter { (slotHeaderTop[it] ?: Float.MAX_VALUE) <= pos.y }
            .maxByOrNull { slotHeaderTop.getValue(it) }
    }

    val dropTargetSlot = if (draggedTask != null) hoveredSlot(dragPosition) else null

    // The header/tasks of a section are separate LazyColumn items (needed for animateItem() to
    // animate a task across sections), so there's no single node to paint one backdrop behind —
    // instead draw it ourselves at the tracked top/bottom of the current drop target section.
    var containerTop by remember { mutableStateOf(0f) }
    var highlightTopTarget by remember { mutableStateOf(0f) }
    var highlightBottomTarget by remember { mutableStateOf(0f) }
    dropTargetSlot?.let { s ->
        slotHeaderTop[s]?.let { highlightTopTarget = it }
        slotSectionBottom[s]?.let { highlightBottomTarget = it }
    }
    val highlightTop by animateFloatAsState(highlightTopTarget, label = "dropHighlightTop")
    val highlightBottom by animateFloatAsState(highlightBottomTarget, label = "dropHighlightBottom")
    val highlightAlpha by animateFloatAsState(
        if (dropTargetSlot != null) 0.04f else 0f,
        // ponytail: snap the fade-out so a one-frame flicker from finger jitter at long-press
        // start can't linger as a slow-fading false highlight.
        animationSpec = if (dropTargetSlot != null) spring() else snap(),
        label = "dropHighlightAlpha",
    )
    val highlightColor = MaterialTheme.colorScheme.onSurface

    fun finishTaskDrag() {
        val task = draggedTask
        val destination = hoveredSlot(dragPosition)
        if (task != null && destination != null) {
            viewModel.moveTaskToSlot(task, state.selectedDate, destination)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { containerTop = it.boundsInRoot().top }
            .drawBehind {
                if (highlightAlpha <= 0f) return@drawBehind
                val marginX = (Dimens.screenEdge + Dimens.sp3).toPx()
                val top = highlightTop - containerTop
                val bottom = highlightBottom - containerTop
                if (bottom <= top) return@drawBehind
                drawRoundRect(
                    color = highlightColor.copy(alpha = highlightAlpha),
                    topLeft = Offset(marginX, top),
                    size = Size(size.width - marginX * 2, bottom - top),
                    cornerRadius = CornerRadius(12.dp.toPx()),
                )
            },
    ) {
        CompositionLocalProvider(LocalViewConfiguration provides fastDragViewConfiguration) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = Dimens.screenEdge, top = 10.dp, end = Dimens.screenEdge, bottom = 165.dp),
        ) {
            item(key = "day-header") { DayHeader(state.selectedDate, viewModel::selectDate) }
            if (state.selectedDate == LocalDate.now() && state.overdueTasks.isNotEmpty()) {
                item(key = "overdue-header") {
                    Column(
                        Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.sp3, vertical = Dimens.sp3),
                    ) {
                        OverdueHeaderRow()
                    }
                }
                itemsIndexed(state.overdueTasks, key = { _, task -> "overdue-${task.id}" }) { index, task ->
                    DayTaskRow(
                        task = task,
                        trailingText = task.overdueDateLabel(),
                        trailingColor = MaterialTheme.colorScheme.error,
                        onToggleComplete = viewModel::toggleComplete,
                        isDragged = draggedTask?.id == task.id,
                        onDragStart = { t, position -> draggedTask = t; dragPosition = position },
                        onDrag = { dragPosition = it },
                        onDragEnd = ::finishTaskDrag,
                        onClick = { sheetTarget = ReminderSheetTarget.Edit(task, state.tagsByTaskId[task.id].orEmpty()) },
                        modifier = Modifier
                            .animateItem()
                            .padding(horizontal = Dimens.sp3)
                            .padding(top = if (index == 0) 0.dp else 6.dp),
                    )
                }
                item(key = "overdue-spacer") { Spacer(Modifier.animateItem().height(Dimens.sp4)) }
            }
            DaySlot.entries.forEach { slot ->
                val tasks = state.tasks
                    .filter { it.slot() == slot }
                    .sortedWith(compareByDescending<Task> { it.hasDueTime }.thenBy { it.dueAt }.thenByDescending { it.priority })
                item(key = "slot-header-${slot.name}") {
                    Column(
                        Modifier
                            .animateItem()
                            .padding(horizontal = Dimens.sp3)
                            .fillMaxWidth()
                            .onGloballyPositioned {
                                val bounds = it.boundsInRoot()
                                slotHeaderTop[slot] = bounds.top
                                if (tasks.isEmpty()) slotSectionBottom[slot] = bounds.bottom
                            }
                            .padding(vertical = Dimens.sp3),
                    ) {
                        SlotHeaderRow(slot, tasks.size)
                        if (tasks.isEmpty()) {
                            Text(
                                "Nothing yet — drag a task here",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(horizontal = Dimens.sp1, vertical = 10.dp),
                            )
                        }
                    }
                }
                itemsIndexed(tasks, key = { _, task -> "slot-task-${task.id}" }) { index, task ->
                    DayTaskRow(
                        task = task,
                        trailingText = task.timeLabel(),
                        trailingColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        onToggleComplete = viewModel::toggleComplete,
                        isDragged = draggedTask?.id == task.id,
                        onDragStart = { t, position -> draggedTask = t; dragPosition = position },
                        onDrag = { dragPosition = it },
                        onDragEnd = ::finishTaskDrag,
                        onClick = { sheetTarget = ReminderSheetTarget.Edit(task, state.tagsByTaskId[task.id].orEmpty()) },
                        modifier = Modifier
                            .animateItem()
                            .padding(horizontal = Dimens.sp3)
                            .padding(top = if (index == 0) 0.dp else 6.dp)
                            .then(
                                if (index == tasks.lastIndex) {
                                    Modifier.onGloballyPositioned { slotSectionBottom[slot] = it.boundsInRoot().bottom }
                                } else {
                                    Modifier
                                },
                            ),
                    )
                }
                item(key = "slot-spacer-${slot.name}") { Spacer(Modifier.animateItem().height(Dimens.sp4)) }
            }
        }
        }

        FloatingActionButton(
            onClick = { sheetTarget = ReminderSheetTarget.New(state.selectedDate) },
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

@Composable
private fun DayHeader(selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    val today = LocalDate.now()
    val firstWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(520)
    val pagerState = rememberPagerState(initialPage = 520, pageCount = { 1_041 })
    val title = when (selectedDate) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> selectedDate.format(DateTimeFormatter.ofPattern("EEE d", Locale.ENGLISH))
    }

    Column {
        Text(
            text = title,
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = Dimens.sp2, bottom = 2.dp),
        )
        Text(
            text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH)),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = Dimens.radiusMd),
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
        ) { page ->
            val dates = (0L..6L).map(firstWeek.plusWeeks(page.toLong())::plusDays)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                dates.forEach { date ->
                    val selected = date == selectedDate
                    val isToday = date == today
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(12.dp),
                            )
                            .clickable(onClick = { onDateSelected(date) })
                            .padding(vertical = Dimens.sp2),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = date.format(DateTimeFormatter.ofPattern("EEEEE", Locale.ENGLISH)).uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline,
                        )
                        Text(
                            text = date.dayOfMonth.toString(),
                            fontFamily = MonoFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                selected -> MaterialTheme.colorScheme.onPrimary
                                isToday -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverdueHeaderRow() {
    Row(
        modifier = Modifier.padding(start = Dimens.sp1, end = Dimens.sp1, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.sp2),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_clock),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
        )
        Text("Overdue", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        Text(
            "drag into a slot to reschedule",
            fontFamily = MonoFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun SlotHeaderRow(slot: DaySlot, taskCount: Int) {
    Row(
        modifier = Modifier.padding(start = Dimens.sp1, end = Dimens.sp1, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.sp2),
    ) {
        Icon(
            painter = painterResource(slot.icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(slot.label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(
            taskCount.toString(),
            fontFamily = MonoFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
internal fun DayTaskRow(
    task: Task,
    trailingText: String?,
    trailingColor: androidx.compose.ui.graphics.Color,
    onToggleComplete: (Task) -> Unit,
    isDragged: Boolean,
    onDragStart: (Task, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    containerModifier: Modifier = Modifier
        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
        .padding(horizontal = Dimens.sp3, vertical = 11.dp),
) {
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val dragOffset = remember(task.id) { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scope = rememberCoroutineScope()
    // pointerInput is keyed by task.id only, so the gesture coroutine survives a slot move
    // (id doesn't change) instead of restarting — read task/onClick via rememberUpdatedState so
    // callbacks always report the latest values, not the ones captured when the drag started.
    val latestTask = rememberUpdatedState(task)
    val latestOnClick = rememberUpdatedState(onClick)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates = it }
            .pointerInput(task.id) {
                // ponytail: a separate Modifier.clickable next to this pointerInput silently ate
                // taps (its own down/up detector raced the drag detector's). Running both gesture
                // detectors as sibling coroutines inside ONE pointerInput scope is the documented
                // way to combine tap + long-press-drag on the same node.
                coroutineScope {
                    launch { detectTapGestures(onTap = { latestOnClick.value() }) }
                    launch {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { position ->
                                scope.launch { dragOffset.snapTo(Offset.Zero) }
                                coordinates?.localToRoot(position)?.let { onDragStart(latestTask.value, it) }
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                scope.launch { dragOffset.snapTo(dragOffset.value + amount) }
                                coordinates?.localToRoot(change.position)?.let(onDrag)
                            },
                            onDragEnd = {
                                scope.launch { dragOffset.animateTo(Offset.Zero, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                                onDragEnd()
                            },
                            onDragCancel = {
                                scope.launch { dragOffset.animateTo(Offset.Zero) }
                                onDragEnd()
                            },
                        )
                    }
                }
            }
            .graphicsLayer {
                translationX = dragOffset.value.x
                translationY = dragOffset.value.y
                scaleX = if (isDragged) 1.025f else 1f
                scaleY = if (isDragged) 1.025f else 1f
                shadowElevation = if (isDragged) 8.dp.toPx() else 0f
            }
            .then(containerModifier)
            .alpha(if (task.completed) 0.65f else 1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        DayTaskCheckbox(task.completed, onClick = { onToggleComplete(task) })
        Text(
            text = task.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (task.completed) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (task.completed) TextDecoration.LineThrough else null,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        if (trailingText != null || task.recurrenceFreq != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (task.recurrenceFreq != null) {
                    Icon(
                        painter = painterResource(R.drawable.ic_repeat),
                        contentDescription = "Repeats",
                        tint = trailingColor,
                        modifier = Modifier.size(11.dp),
                    )
                }
                if (trailingText != null) {
                    Text(
                        trailingText,
                        fontFamily = MonoFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = trailingColor,
                    )
                }
            }
        }
    }
}

@Composable
internal fun DayTaskCheckbox(checked: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .then(
                if (checked) Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                else Modifier.border(2.dp, CheckboxIdleBorder, CircleShape),
            )
            .toggleable(value = checked, onValueChange = { onClick() }, role = Role.Checkbox)
            .semantics { contentDescription = if (checked) "Completed" else "Mark complete" },
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

private fun Task.slot(): DaySlot {
    if (!hasDueTime) return DaySlot.Morning
    return when (Instant.ofEpochMilli(requireNotNull(dueAt)).atZone(ZoneId.systemDefault()).toLocalTime()) {
        in LocalTime.MIDNIGHT..<LocalTime.NOON -> DaySlot.Morning
        in LocalTime.NOON..<LocalTime.of(18, 0) -> DaySlot.Afternoon
        else -> DaySlot.Evening
    }
}

internal fun Task.timeLabel(): String? = dueAt
    ?.takeIf { hasDueTime }
    ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("H:mm")) }

private fun Task.overdueDateLabel(): String? = dueAt
    ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEE d", Locale.ENGLISH)) }
