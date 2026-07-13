package com.liukscot.reminders.ui.screens

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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liukscot.reminders.R
import com.liukscot.reminders.data.Task
import com.liukscot.reminders.ui.theme.CheckboxIdleBorder
import com.liukscot.reminders.ui.theme.Line1
import com.liukscot.reminders.ui.theme.MonoFontFamily

// Ref: Reminders App Mockup "List detail" screen (data-screen-label="List
// detail"). Sub-tasks nest inside their parent's own card (no separate
// background per sub-task) — matches the mockup's single `t.subtasks`
// sc-for living inside the same flex column as the parent title/meta.
@Composable
fun TaskListDetailScreen(
    listId: Long,
    onBack: () -> Unit,
    viewModel: TaskListDetailViewModel = rememberTaskListDetailViewModel(listId),
) {
    val state by viewModel.uiState.collectAsState()
    var editing by remember { mutableStateOf<DetailEditTarget>(DetailEditTarget.None) }
    var completedExpanded by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            ListDetailHeader(
                onBack = onBack,
                menuExpanded = menuExpanded,
                onMenuExpandedChange = { menuExpanded = it },
                canDelete = state.lists.size > 1,
                onRename = { editing = DetailEditTarget.RenameList },
                onDelete = { viewModel.deleteList(onDeleted = onBack) },
            )
            Text(
                text = state.list?.name.orEmpty(),
                style = TextStyle(
                    fontSize = 30.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.Both,
                    ),
                ),
                color = MaterialTheme.colorScheme.primary,
            )
            val total = state.open.size + state.completed.size
            Text(
                text = "$total reminders",
                fontFamily = MonoFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp),
            )

            LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                itemsIndexed(state.open, key = { _, group -> group.task.id }) { index, group ->
                    TaskGroupCard(
                        group = group,
                        shape = groupedRowShape(index, state.open.size, bigRadius = 14.dp, smallRadius = 4.dp),
                        onToggle = viewModel::toggleComplete,
                        onEdit = { editing = DetailEditTarget.EditTask(group.task) },
                    )
                }
                item {
                    NewReminderRow(onClick = { editing = DetailEditTarget.NewTask })
                }
                if (state.completed.isNotEmpty()) {
                    item {
                        CompletedSectionHeader(
                            count = state.completed.size,
                            expanded = completedExpanded,
                            onClick = { completedExpanded = !completedExpanded },
                        )
                    }
                    if (completedExpanded) {
                        itemsIndexed(state.completed, key = { _, group -> "completed-${group.task.id}" }) { index, group ->
                            CompletedTaskRow(
                                task = group.task,
                                shape = groupedRowShape(index, state.completed.size, bigRadius = 14.dp, smallRadius = 4.dp),
                                onToggle = viewModel::toggleComplete,
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { editing = DetailEditTarget.NewTask },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 18.dp, bottom = 18.dp),
        ) {
            Icon(painterResource(R.drawable.ic_plus), contentDescription = "Add reminder")
        }
    }

    when (val target = editing) {
        is DetailEditTarget.NewTask -> ReminderSheet(
            lists = state.lists,
            preselectedListId = listId,
            onDismiss = { editing = DetailEditTarget.None },
            onSave = { title, notes, taskListId ->
                viewModel.saveTask(null, title, notes, taskListId)
                editing = DetailEditTarget.None
            },
        )
        is DetailEditTarget.EditTask -> ReminderSheet(
            lists = state.lists,
            existingTask = target.task,
            preselectedListId = listId,
            onDismiss = { editing = DetailEditTarget.None },
            onSave = { title, notes, taskListId ->
                viewModel.saveTask(target.task, title, notes, taskListId)
                editing = DetailEditTarget.None
            },
        )
        DetailEditTarget.RenameList -> SimpleTextDialog(
            title = "Rename list",
            label = "Name",
            initialValue = state.list?.name.orEmpty(),
            onDismiss = { editing = DetailEditTarget.None },
            onConfirm = { name -> viewModel.renameList(name); editing = DetailEditTarget.None },
        )
        DetailEditTarget.None -> Unit
    }
}

private sealed interface DetailEditTarget {
    data object None : DetailEditTarget
    data object NewTask : DetailEditTarget
    data object RenameList : DetailEditTarget
    data class EditTask(val task: Task) : DetailEditTarget
}

@Composable
private fun HeaderIconButton(icon: Int, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ListDetailHeader(
    onBack: () -> Unit,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    canDelete: Boolean,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderIconButton(icon = R.drawable.ic_chevron_left, contentDescription = "Back", onClick = onBack)
        Spacer(modifier = Modifier.weight(1f))
        Box {
            HeaderIconButton(
                icon = R.drawable.ic_more_horizontal,
                contentDescription = "List options",
                onClick = { onMenuExpandedChange(true) },
            )
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { onMenuExpandedChange(false) }) {
                DropdownMenuItem(
                    text = { Text("Rename list") },
                    onClick = { onMenuExpandedChange(false); onRename() },
                )
                if (canDelete) {
                    DropdownMenuItem(
                        text = { Text("Delete list") },
                        onClick = { onMenuExpandedChange(false); onDelete() },
                    )
                }
            }
        }
    }
}

@Composable
private fun NewReminderRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_plus),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = "New reminder",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun CompletedSectionHeader(count: Int, expanded: Boolean, onClick: () -> Unit) {
    Column {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Line1))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Completed ($count)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(15.dp)
                    .rotate(if (expanded) 90f else 0f),
            )
        }
    }
}

// One card per task group: parent title/meta, then its sub-tasks stacked
// directly below — no separate background per sub-task (matches mockup).
@Composable
private fun TaskGroupCard(
    group: TaskGroup,
    shape: Shape,
    onToggle: (Task) -> Unit,
    onEdit: () -> Unit,
) {
    val task = group.task
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
            .background(MaterialTheme.colorScheme.surface, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TaskCheckbox(checked = task.completed, size = 22.dp, checkIconSize = 13.dp, onClick = { onToggle(task) })
            // Ref: mockup's `rowTap` opens this same task in the edit sheet.
            Column(modifier = Modifier.weight(1f).clickable(onClick = onEdit)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = task.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (task.completed) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                    )
                    if (task.flagged) {
                        Icon(
                            painter = painterResource(R.drawable.ic_flag),
                            contentDescription = "Flagged",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
                val metaText = task.notes.orEmpty()
                if (metaText.isNotEmpty()) {
                    Text(
                        text = metaText,
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
        group.subtasks.forEach { sub ->
            Row(
                modifier = Modifier.padding(top = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 22dp checkbox + 12dp gap: aligns sub-task rows with the
                // parent's title column, matching the mockup's single
                // flex:1 content column shared by title, meta and subtasks.
                Spacer(modifier = Modifier.width(34.dp))
                TaskCheckbox(checked = sub.completed, size = 17.dp, checkIconSize = 10.dp, onClick = { onToggle(sub) })
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = sub.title,
                    fontSize = 13.5.sp,
                    color = if (sub.completed) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onBackground,
                    textDecoration = if (sub.completed) TextDecoration.LineThrough else null,
                )
            }
        }
    }
}

@Composable
private fun CompletedTaskRow(task: Task, shape: Shape, onToggle: (Task) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
            .background(MaterialTheme.colorScheme.surface, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .alpha(0.7f),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TaskCheckbox(checked = task.completed, size = 22.dp, checkIconSize = 13.dp, onClick = { onToggle(task) })
        Text(
            text = task.title,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.outline,
            textDecoration = TextDecoration.LineThrough,
        )
    }
}

@Composable
private fun TaskCheckbox(checked: Boolean, size: Dp, checkIconSize: Dp, onClick: () -> Unit) {
    val label = if (checked) "Completed" else "Mark complete"
    Box(
        modifier = Modifier
            .size(size)
            .then(
                if (checked) {
                    Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                } else {
                    Modifier.border(2.dp, CheckboxIdleBorder, CircleShape)
                },
            )
            .toggleable(value = checked, onValueChange = { onClick() }, role = Role.Checkbox)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(checkIconSize),
            )
        }
    }
}
