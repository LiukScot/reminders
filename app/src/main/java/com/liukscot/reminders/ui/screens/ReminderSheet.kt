package com.liukscot.reminders.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liukscot.reminders.R
import com.liukscot.reminders.data.Task
import com.liukscot.reminders.data.TaskList
import com.liukscot.reminders.ui.components.GradientButton
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

private val PRIORITY_LABELS = listOf("None", "!", "!!", "!!!")

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

// Ref: Reminders App Mockup "New reminder" sheet (data-screen-label="New
// reminder sheet"). One deliberate deviation: the mockup has no list-picker
// UI (list inferred from screen context, falling back to a hardcoded list) —
// per explicit user decision this sheet instead shows a list dropdown,
// pre-selected from the active list or the configurable default-list setting.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSheet(
    lists: List<TaskList>,
    existingTask: Task? = null,
    existingTags: List<String> = emptyList(),
    preselectedListId: Long?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        notes: String?,
        listId: Long,
        tags: List<String>,
        flagged: Boolean,
        priority: Int,
        dueAt: Long?,
        hasDueTime: Boolean,
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
    var dateEnabled by remember { mutableStateOf(existingTask?.dueAt != null) }
    var selectedDate by remember { mutableStateOf(existingDateTime?.toLocalDate() ?: LocalDate.now()) }
    var displayedMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    var timeEnabled by remember { mutableStateOf(existingTask?.hasDueTime ?: false) }
    var selectedTime by remember { mutableStateOf(existingDateTime?.toLocalTime() ?: LocalTime.of(9, 0)) }
    var listMenuExpanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
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
            Box {
                // A TextField consumes taps for its own focus handling, so a
                // plain `.clickable` on it never fires (verified on-device:
                // the menu didn't open). `enabled = false` stops it from
                // taking touch input at all; a transparent clickable Box on
                // top opens the menu instead. Disabled colors are pinned to
                // match the enabled look so it doesn't visually dim.
                TextField(
                    value = lists.firstOrNull { it.id == selectedListId }?.name.orEmpty(),
                    onValueChange = {},
                    enabled = false,
                    trailingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_chevron_right),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { listMenuExpanded = true },
                )
                DropdownMenu(expanded = listMenuExpanded, onDismissRequest = { listMenuExpanded = false }) {
                    lists.forEach { list ->
                        DropdownMenuItem(
                            text = { Text(list.name) },
                            onClick = { selectedListId = list.id; listMenuExpanded = false },
                        )
                    }
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
                    val listId = selectedListId
                    if (title.isNotBlank() && listId != null) {
                        val tags = tagsText.split(" ").map { it.trim() }.filter { it.isNotEmpty() }
                        val dueAt = if (dateEnabled) {
                            val time = if (timeEnabled) selectedTime else LocalTime.MIDNIGHT
                            LocalDateTime.of(selectedDate, time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        } else {
                            null
                        }
                        onSave(title, notes.ifBlank { null }, listId, tags, flagged, priority, dueAt, dateEnabled && timeEnabled)
                    }
                },
                enabled = title.isNotBlank() && selectedListId != null,
            )
        }
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
