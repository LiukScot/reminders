package com.liukscot.reminders.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.LineHeightStyle.Alignment as LineAlignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liukscot.reminders.R
import com.liukscot.reminders.data.SmartList
import com.liukscot.reminders.data.Task
import com.liukscot.reminders.ui.theme.MonoFontFamily

// The mockup has no smart-list screen of its own, so this mirrors "List detail" minus what only a
// real list has: no rename/delete menu, and no "New reminder" row — a filter has no list to add to.
@Composable
fun SmartListScreen(
    smartList: SmartList,
    onBack: () -> Unit,
    viewModel: SmartListViewModel = rememberSmartListViewModel(smartList),
) {
    val state by viewModel.uiState.collectAsState()
    var editing by remember { mutableStateOf<TaskGroup?>(null) }
    var deleting by remember { mutableStateOf<Task?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderIconButton(icon = R.drawable.ic_chevron_left, contentDescription = "Back", onClick = onBack)
        }
        Text(
            text = smartList.label,
            style = TextStyle(
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(
                    alignment = LineAlignment.Center,
                    trim = LineHeightStyle.Trim.Both,
                ),
            ),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "${state.tasks.size} reminders",
            fontFamily = MonoFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
        )

        if (state.tasks.isEmpty()) {
            Text(
                text = smartList.emptyMessage,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
            )
            return@Column
        }

        LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
            itemsIndexed(state.tasks, key = { _, group -> group.task.id }) { index, group ->
                val shape = groupedRowShape(index, state.tasks.size, bigRadius = 14.dp, smallRadius = 4.dp)
                SwipeableTaskRow(
                    onComplete = { viewModel.toggleComplete(group.task) },
                    onDeleteRequest = { deleting = group.task },
                    shape = shape,
                    modifier = Modifier.padding(bottom = 2.dp),
                ) {
                    TaskGroupCard(
                        group = group,
                        shape = shape,
                        onToggle = viewModel::toggleComplete,
                        onEdit = { editing = group },
                    )
                }
            }
        }
    }

    editing?.let { group ->
        ReminderSheet(
            lists = state.lists,
            existingTask = group.task,
            existingTags = group.tags,
            preselectedListId = group.task.listId,
            onDismiss = { editing = null },
            onDelete = { viewModel.deleteTask(group.task); editing = null },
            onSave = { title, notes, listId, tags, flagged, priority, dueAt, hasDueTime, recFreq, recInterval, recByDay, recAnchor ->
                viewModel.saveTask(
                    group.task, title, notes, listId, tags, flagged, priority, dueAt, hasDueTime,
                    recFreq, recInterval, recByDay, recAnchor,
                )
                editing = null
            },
        )
    }

    deleting?.let { task ->
        DeleteReminderDialog(
            title = task.title,
            onDismiss = { deleting = null },
            onConfirm = { viewModel.deleteTask(task); deleting = null },
        )
    }
}

private val SmartList.emptyMessage: String
    get() = when (this) {
        SmartList.Flagged -> "Nothing flagged"
        SmartList.All -> "No open reminders"
        SmartList.Completed -> "Nothing completed yet"
        SmartList.Overdue -> "Nothing overdue — you're on top of it"
    }
