package com.liukscot.reminders.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liukscot.reminders.R
import com.liukscot.reminders.data.Task
import com.liukscot.reminders.ui.theme.Dimens
import com.liukscot.reminders.ui.theme.MonoFontFamily
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Ref: Reminders App Mockup "Search" screen (data-screen-label="Search") — back button + search
// pill, then hits grouped under their list's name.
@Composable
fun SearchScreen(onBack: () -> Unit, viewModel: SearchViewModel = rememberSearchViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    var editingTask by remember { mutableStateOf<Task?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = Dimens.screenEdge)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = Dimens.sp4),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_left),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
            SearchField(
                query = state.query,
                onQueryChange = viewModel::setQuery,
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
            )
        }

        if (state.groups.isEmpty()) {
            Text(
                text = if (state.query.isBlank()) {
                    "Search titles, notes and tags across all lists"
                } else {
                    "No results for “${state.query.trim()}”"
                },
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.sp8, horizontal = Dimens.sp2),
            )
            return@Column
        }

        LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
            state.groups.forEach { group ->
                item(key = "header-${group.listName}") { GroupHeader(group.listName) }
                itemsIndexed(group.tasks, key = { _, task -> task.id }) { index, task ->
                    SearchTaskRow(
                        task = task,
                        shape = groupedRowShape(index, group.tasks.size, bigRadius = Dimens.radiusMd, smallRadius = 4.dp),
                        onToggle = { viewModel.toggleComplete(task) },
                        onClick = { editingTask = task },
                    )
                }
            }
        }
    }

    editingTask?.let { task ->
        ReminderSheet(
            lists = state.lists,
            existingTask = task,
            existingTags = state.tagsByTaskId[task.id].orEmpty(),
            preselectedListId = task.listId,
            onDismiss = { editingTask = null },
            onSave = { title, notes, listId, tags, flagged, priority, dueAt, hasDueTime, recFreq, recInterval, recByDay, recAnchor ->
                viewModel.saveTask(
                    task, title, notes, listId, tags, flagged, priority, dueAt, hasDueTime,
                    recFreq, recInterval, recByDay, recAnchor,
                )
                editingTask = null
            },
        )
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(46.dp)
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .padding(horizontal = Dimens.sp4),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(18.dp),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(
                    text = "Search reminders",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun GroupHeader(name: String) {
    Text(
        text = name.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.3.sp,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(top = 14.dp, bottom = Dimens.sp2),
    )
}

@Composable
private fun SearchTaskRow(task: Task, shape: Shape, onToggle: () -> Unit, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
            .background(MaterialTheme.colorScheme.surface, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = Dimens.sp3),
        horizontalArrangement = Arrangement.spacedBy(Dimens.sp3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DayTaskCheckbox(checked = task.completed, onClick = onToggle)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = task.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            task.searchTimeLabel()?.let { label ->
                Text(
                    text = label,
                    fontFamily = MonoFontFamily,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val SearchTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun Task.searchTimeLabel(): String? {
    if (!hasDueTime) return null
    val due = dueAt ?: return null
    return Instant.ofEpochMilli(due).atZone(ZoneId.systemDefault()).toLocalTime().format(SearchTimeFormatter)
}
