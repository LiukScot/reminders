package com.liukscot.reminders.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liukscot.reminders.R
import com.liukscot.reminders.data.RecurrenceFrequency
import com.liukscot.reminders.data.RecurrenceRule
import com.liukscot.reminders.data.Task
import com.liukscot.reminders.data.TaskList
import com.liukscot.reminders.data.parseReminderText
import com.liukscot.reminders.ui.components.GradientButton
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

private val PRIORITY_LABELS = listOf("None", "!", "!!", "!!!")
private val SIMPLE_RECURRENCES = listOf(
    null to "Never",
    RecurrenceFrequency.DAILY to "Daily",
    RecurrenceFrequency.WEEKLY to "Weekly",
    RecurrenceFrequency.MONTHLY to "Monthly",
)
private val CUSTOM_FREQUENCIES = RecurrenceFrequency.entries
private val WEEKDAY_ORDER = listOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY,
)

private fun recurrenceUnitLabel(freq: RecurrenceFrequency, interval: Int): String {
    val unit = when (freq) {
        RecurrenceFrequency.DAILY -> "day"
        RecurrenceFrequency.WEEKLY -> "week"
        RecurrenceFrequency.MONTHLY -> "month"
        RecurrenceFrequency.YEARLY -> "year"
    }
    return if (interval == 1) unit else "${unit}s"
}

private fun isCustomRule(rule: RecurrenceRule): Boolean =
    rule.interval != 1 || rule.byDay.isNotEmpty() || rule.frequency == RecurrenceFrequency.YEARLY

private fun recurrenceSummary(freq: RecurrenceFrequency, interval: Int, byDay: Set<DayOfWeek>): String {
    val cadence = "Every $interval ${recurrenceUnitLabel(freq, interval)}"
    if (freq != RecurrenceFrequency.WEEKLY || byDay.isEmpty()) return cadence
    val days = WEEKDAY_ORDER.filter { it in byDay }.joinToString(", ") { it.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH) }
    return "$cadence · $days"
}

// How far down, and how fast, a drag has to go before letting go closes the sheet.
//
// The two are an OR: exceeding either one closes it. That is what makes the speed value the one
// that actually decides. A deliberate downward drag runs well past 1000.dp per second, so anything
// near Material's stock 125.dp per second fires on every gesture and the distance never gets a
// vote — the sheet then shuts on the smallest flick no matter how far the distance is raised.
//
// So the speed is set above a normal drag and only a sharp flick clears it, which leaves distance
// as the usual decider: about a quarter of the screen. This is a form, and closing it by accident
// throws away everything typed, so it is deliberately stiffer than a sheet you just glance at.
//
// Not stiffer still: an earlier 320.dp / 10_000.dp per second felt broken rather than firm. No
// finger reaches 10_000.dp per second, so velocity could never close it at all, and a third of the
// screen of travel was the only way out — every normal flick just rebounded.
private val SheetDismissDistance = 160.dp
private val SheetDismissFlingSpeed = 1800.dp

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.3.sp,
        color = MaterialTheme.colorScheme.outline,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSheet(
    lists: List<TaskList>,
    existingTask: Task? = null,
    existingTags: List<String> = emptyList(),
    preselectedListId: Long?,
    initialDate: LocalDate? = null,
    onDismiss: () -> Unit,
    // Null while creating: there is nothing to delete yet.
    onDelete: (() -> Unit)? = null,
    onSave: (
        title: String,
        notes: String?,
        listId: Long,
        tags: List<String>,
        flagged: Boolean,
        priority: Int,
        dueAt: Long?,
        hasDueTime: Boolean,
        recurrenceFreq: String?,
        recurrenceInterval: Int,
        recurrenceByDay: String?,
        recurrenceAnchor: Long?,
    ) -> Unit,
) {
    var title by remember { mutableStateOf(existingTask?.title ?: "") }
    var notes by remember { mutableStateOf(existingTask?.notes ?: "") }
    var tagsText by remember { mutableStateOf(existingTags.joinToString(" ")) }
    var flagged by remember { mutableStateOf(existingTask?.flagged ?: false) }
    var priority by remember { mutableStateOf(existingTask?.priority ?: 0) }
    var selectedListId by remember {
        mutableStateOf(existingTask?.listId ?: preselectedListId ?: lists.firstOrNull()?.id)
    }
    val existingDateTime = remember(existingTask) {
        existingTask?.dueAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime() }
    }
    var dateEnabled by remember { mutableStateOf(existingTask?.dueAt != null || initialDate != null) }
    var selectedDate by remember { mutableStateOf(existingDateTime?.toLocalDate() ?: initialDate ?: LocalDate.now()) }
    var displayedMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    var timeEnabled by remember { mutableStateOf(existingTask?.hasDueTime ?: false) }
    var selectedTime by remember { mutableStateOf(existingDateTime?.toLocalTime() ?: LocalTime.of(9, 0)) }
    val existingRule = remember(existingTask) { existingTask?.let(RecurrenceRule::fromTask) }
    var recurrenceFreq by remember { mutableStateOf(existingRule?.frequency) }
    var recurrenceInterval by remember { mutableStateOf(existingRule?.interval ?: 1) }
    var recurrenceByDay by remember { mutableStateOf(existingRule?.byDay ?: emptySet<DayOfWeek>()) }
    var usingCustomRecurrence by remember { mutableStateOf(existingRule?.let(::isCustomRule) ?: false) }
    var showCustomRecurrenceSheet by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    // Issue #18: recognize date/time/recurrence phrases in the title (Italian first, English
    // second — see ReminderTextParser). While typing, the recognized phrase is only highlighted
    // (visual transformation below); pressing Enter commits — it lifts the phrase into the sheet's
    // own controls and strips it from the title. Nothing changes until Enter, so a half-typed word
    // doesn't flip the pickers, and spaces stay editable because the text isn't rewritten mid-typing.
    fun commitTitleParsing() {
        val parsed = parseReminderText(title)
        if (parsed.matchedRanges.isEmpty()) return
        title = parsed.cleanTitle
        parsed.date?.let {
            selectedDate = it
            dateEnabled = true
            displayedMonth = YearMonth.from(it)
        }
        parsed.time?.let {
            selectedTime = it
            timeEnabled = true
            // A time needs a date to hang on, and the TIME section only shows when dateEnabled —
            // so enabling a time implies today unless a date was also parsed.
            dateEnabled = true
        }
        parsed.recurrence?.let { rule ->
            recurrenceFreq = rule.frequency
            recurrenceInterval = rule.interval
            recurrenceByDay = rule.byDay
            usingCustomRecurrence = isCustomRule(rule)
        }
    }
    val highlightBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    val highlightFg = MaterialTheme.colorScheme.primary
    val nlpHighlight = remember(highlightBg, highlightFg) {
        VisualTransformation { text ->
            val ranges = parseReminderText(text.text).matchedRanges
            if (ranges.isEmpty()) {
                TransformedText(text, OffsetMapping.Identity)
            } else {
                val annotated = buildAnnotatedString {
                    append(text.text)
                    ranges.forEach { r -> addStyle(SpanStyle(background = highlightBg, color = highlightFg), r.first, r.last + 1) }
                }
                TransformedText(annotated, OffsetMapping.Identity)
            }
        }
    }
    val density = LocalDensity.current
    val sheetState = remember(density) {
        SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { with(density) { SheetDismissDistance.toPx() } },
            velocityThreshold = { with(density) { SheetDismissFlingSpeed.toPx() } },
            initialValue = SheetValue.Hidden,
        )
    }
    val contentScroll = rememberScrollState()
    // derivedStateOf, not a raw `contentScroll.value == 0`: the raw read would recompose this whole
    // sheet on every scrolled pixel. This only wakes it when the answer actually flips.
    val contentAtTop by remember { derivedStateOf { contentScroll.value == 0 } }
    val fieldColors = TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        // The list field is `enabled = false` (see below) so it doesn't
        // steal taps from the overlay that opens the dropdown — pin the
        // disabled look to match enabled so it doesn't visually dim.
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        disabledIndicatorColor = Color.Transparent,
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        // The sheet grows to the full window, so by default its surface runs behind the status bar
        // and only the *content* is inset — the card looks like it has no top edge and the scroll
        // viewport reads as a second window underneath. Cap the surface at the status bar instead,
        // and leave the content only the bottom (gesture bar) inset.
        modifier = Modifier.statusBarsPadding(),
        contentWindowInsets = { WindowInsets.navigationBars },
        // Drag-to-dismiss only once the form is scrolled back to the top. Mid-form a downward drag
        // is someone reading their way back up, not asking to close — handing that gesture to the
        // sheet closed the form and lost what they had typed.
        sheetGesturesEnabled = contentAtTop,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(contentScroll)
                .padding(horizontal = 18.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (existingTask != null) "Edit reminder" else "New reminder",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_x),
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("What do you need to do?", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                singleLine = true,
                visualTransformation = nlpHighlight,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commitTitleParsing() }),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
            TextField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = { Text("Add notes", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground) },
                textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
            DueDateSection(
                enabled = dateEnabled,
                onEnabledChange = { dateEnabled = it },
                selectedDate = selectedDate,
                onDateSelected = { selectedDate = it },
                displayedMonth = displayedMonth,
                onMonthChange = { displayedMonth = it },
            )
            if (dateEnabled) {
                DueTimeSection(
                    enabled = timeEnabled,
                    onEnabledChange = { timeEnabled = it },
                    selectedTime = selectedTime,
                    onTimeSelected = { selectedTime = it },
                )
            }
            SectionLabel("REPEAT")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SIMPLE_RECURRENCES.forEach { (freq, label) ->
                    val selected = !usingCustomRecurrence && recurrenceFreq == freq
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(50),
                            )
                            .clickable {
                                recurrenceFreq = freq
                                recurrenceInterval = 1
                                recurrenceByDay = emptySet()
                                usingCustomRecurrence = false
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .background(
                        if (usingCustomRecurrence) MaterialTheme.colorScheme.primary else Color.Transparent,
                        RoundedCornerShape(50),
                    )
                    .clickable { showCustomRecurrenceSheet = true }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_repeat),
                    contentDescription = null,
                    tint = if (usingCustomRecurrence) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = if (usingCustomRecurrence && recurrenceFreq != null) {
                        recurrenceSummary(recurrenceFreq!!, recurrenceInterval, recurrenceByDay)
                    } else {
                        "Custom…"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (usingCustomRecurrence) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                )
            }
            SectionLabel("PRIORITY")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PRIORITY_LABELS.forEachIndexed { value, label ->
                    val selected = priority == value
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(50),
                            )
                            .clickable { priority = value }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            SectionLabel("LIST")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                lists.forEach { list ->
                    val selected = list.id == selectedListId
                    Text(
                        text = list.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(50),
                            )
                            .clickable { selectedListId = list.id }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            SectionLabel("OTHER")
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                GroupedTextFieldRow(
                    value = tagsText,
                    onValueChange = { tagsText = it },
                    placeholder = "Add tags, separated by spaces",
                    icon = R.drawable.ic_tag,
                    shape = groupedRowShape(0, 2, bigRadius = 12.dp, smallRadius = 4.dp),
                )
                GroupedFlagRow(
                    flagged = flagged,
                    onFlaggedChange = { flagged = it },
                    shape = groupedRowShape(1, 2, bigRadius = 12.dp, smallRadius = 4.dp),
                )
            }
            GradientButton(
                text = if (existingTask != null) "Save changes" else "Add reminder",
                onClick = {
                    // Apply any still-typed phrase ("... alle 18") even if the user taps Save without
                    // pressing Enter first; it's idempotent once the title is already clean.
                    commitTitleParsing()
                    val listId = selectedListId
                    if (title.isNotBlank() && listId != null) {
                        val tags = tagsText.split(" ").map { it.trim() }.filter { it.isNotEmpty() }
                        val dueAt = if (dateEnabled) {
                            val time = if (timeEnabled) selectedTime else LocalTime.MIDNIGHT
                            LocalDateTime.of(selectedDate, time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        } else {
                            null
                        }
                        val freq = recurrenceFreq
                        // A plain reschedule (same rule, just a different dueAt) keeps the
                        // existing anchor so the cadence doesn't shift. Setting or changing the
                        // rule itself starts a fresh cadence anchored on the sheet's own date/time.
                        val ruleChanged = freq != existingRule?.frequency ||
                            recurrenceInterval != (existingRule?.interval ?: 1) ||
                            recurrenceByDay != (existingRule?.byDay ?: emptySet<DayOfWeek>())
                        val anchor = when {
                            freq == null -> null
                            ruleChanged || existingTask?.recurrenceAnchor == null -> dueAt
                            else -> existingTask.recurrenceAnchor
                        }
                        onSave(
                            title,
                            notes.ifBlank { null },
                            listId,
                            tags,
                            flagged,
                            priority,
                            dueAt,
                            dateEnabled && timeEnabled,
                            freq?.name,
                            recurrenceInterval,
                            freq?.let { RecurrenceRule.encodeByDay(recurrenceByDay) },
                            anchor,
                        )
                    }
                },
                enabled = title.isNotBlank() && selectedListId != null,
            )
            if (existingTask != null && onDelete != null) {
                TextButton(
                    onClick = { confirmingDelete = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_trash_2),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Delete reminder", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (confirmingDelete && onDelete != null) {
        DeleteReminderDialog(
            title = existingTask?.title.orEmpty(),
            onDismiss = { confirmingDelete = false },
            onConfirm = { confirmingDelete = false; onDelete() },
        )
    }

    if (showCustomRecurrenceSheet) {
        CustomRecurrenceSheet(
            initialFrequency = recurrenceFreq ?: RecurrenceFrequency.WEEKLY,
            initialInterval = recurrenceInterval,
            initialByDay = recurrenceByDay,
            onDone = { freq, every, days ->
                recurrenceFreq = freq
                recurrenceInterval = every
                recurrenceByDay = days
                usingCustomRecurrence = true
                showCustomRecurrenceSheet = false
            },
            onDismiss = { showCustomRecurrenceSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomRecurrenceSheet(
    initialFrequency: RecurrenceFrequency,
    initialInterval: Int,
    initialByDay: Set<DayOfWeek>,
    onDone: (RecurrenceFrequency, Int, Set<DayOfWeek>) -> Unit,
    onDismiss: () -> Unit,
) {
    // Draft locally so backing out (dismiss) discards edits — only Done commits them upward. Before,
    // the edit callbacks mutated the parent live, so a cancelled sheet still left its changes behind.
    var frequency by remember { mutableStateOf(initialFrequency) }
    var interval by remember { mutableStateOf(initialInterval) }
    var byDay by remember { mutableStateOf(initialByDay) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Custom recurrence",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_x),
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_repeat),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = recurrenceSummary(frequency, interval, byDay),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            SectionLabel("FREQUENCY")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CUSTOM_FREQUENCIES.forEach { freq ->
                    val selected = freq == frequency
                    Text(
                        text = freq.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(50),
                            )
                            .clickable { frequency = freq }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            SectionLabel("EVERY")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RecurrenceStepButton(icon = R.drawable.ic_minus, contentDescription = "Decrease interval", onClick = { interval = (interval - 1).coerceAtLeast(1) })
                Text(
                    text = "$interval ${recurrenceUnitLabel(frequency, interval)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                RecurrenceStepButton(icon = R.drawable.ic_plus, contentDescription = "Increase interval", onClick = { interval = interval + 1 })
            }
            if (frequency == RecurrenceFrequency.WEEKLY) {
                SectionLabel("ON THESE DAYS")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    WEEKDAY_ORDER.forEach { day ->
                        val selected = day in byDay
                        Text(
                            text = day.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.ENGLISH),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(10.dp),
                                )
                                .clickable { byDay = if (selected) byDay - day else byDay + day }
                                .padding(vertical = 10.dp),
                        )
                    }
                }
            }
            GradientButton(text = "Done", onClick = { onDone(frequency, interval, byDay) }, enabled = true)
        }
    }
}

@Composable
private fun RecurrenceStepButton(icon: Int, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun GroupedTextFieldRow(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: Int,
    shape: Shape,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, shape)
            .height(44.dp)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(text = placeholder, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun GroupedFlagRow(flagged: Boolean, onFlaggedChange: (Boolean) -> Unit, shape: Shape) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, shape)
            .height(44.dp)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_flag),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Text("Flag", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
        Switch(
            checked = flagged,
            onCheckedChange = onFlaggedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
        )
    }
}
