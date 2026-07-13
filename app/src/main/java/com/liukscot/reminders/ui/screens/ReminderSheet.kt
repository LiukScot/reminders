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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liukscot.reminders.R
import com.liukscot.reminders.data.Task
import com.liukscot.reminders.data.TaskList
import com.liukscot.reminders.ui.components.GradientButton

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
    onSave: (title: String, notes: String?, listId: Long, tags: List<String>) -> Unit,
) {
    var title by remember { mutableStateOf(existingTask?.title ?: "") }
    var notes by remember { mutableStateOf(existingTask?.notes ?: "") }
    var tagsText by remember { mutableStateOf(existingTags.joinToString(" ")) }
    var selectedListId by remember {
        mutableStateOf(existingTask?.listId ?: preselectedListId ?: lists.firstOrNull()?.id)
    }
    var listMenuExpanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
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
                placeholder = { Text("What do you need to do?") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
            TextField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = { Text("Add notes") },
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
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
            TextField(
                value = tagsText,
                onValueChange = { tagsText = it },
                placeholder = { Text("Add tags, separated by spaces") },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_tag),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
            GradientButton(
                text = if (existingTask != null) "Save changes" else "Add reminder",
                onClick = {
                    val listId = selectedListId
                    if (title.isNotBlank() && listId != null) {
                        val tags = tagsText.split(" ").map { it.trim() }.filter { it.isNotEmpty() }
                        onSave(title, notes.ifBlank { null }, listId, tags)
                    }
                },
                enabled = title.isNotBlank() && selectedListId != null,
            )
        }
    }
}
