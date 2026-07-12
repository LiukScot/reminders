package com.liukscot.reminders.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liukscot.reminders.R
import com.liukscot.reminders.data.ListIcons
import com.liukscot.reminders.data.TaskList
import com.liukscot.reminders.ui.theme.MonoFontFamily

// Ref: Reminders App Mockup "Home" screen — header, "My lists" section, and
// groupRadius-style rows. Smart-list cards (Flagged/No date/All/Completed)
// depend on priority/flag fields that don't exist yet — built separately.
@Composable
fun ListsScreen(
    onOpenList: (Long) -> Unit,
    viewModel: ListsViewModel = rememberListsViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var editing by remember { mutableStateOf<EditTarget>(EditTarget.None) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            Text(
                text = "Reminders",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 34.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.Both,
                    ),
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "MY LISTS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.3.sp,
                    ),
                    color = MaterialTheme.colorScheme.outline,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_plus),
                    contentDescription = "New list",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { editing = EditTarget.NewList },
                )
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                itemsIndexed(state.lists, key = { _, entry -> entry.list.id }) { index, entry ->
                    ListRow(
                        entry = entry,
                        shape = groupedRowShape(index, state.lists.size, bigRadius = 14.dp, smallRadius = 4.dp),
                        onClick = { onOpenList(entry.list.id) },
                        onLongClick = { editing = EditTarget.Rename(entry.list) },
                    )
                }
            }
        }

        // Ref: mockup's plain "+" FAB — ink-2 circle, accent-solid icon (the
        // gradient mic button next to it is voice capture, not built yet).
        // 88dp mirrors the mockup's FAB bottom:88 vs floating nav's
        // bottom:10/height:62 — clears the nav bar with the same gap.
        FloatingActionButton(
            onClick = { editing = EditTarget.NewTask },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 18.dp, bottom = 88.dp),
        ) {
            Icon(painterResource(R.drawable.ic_plus), contentDescription = "Add reminder")
        }
    }

    when (val target = editing) {
        is EditTarget.NewList -> ListEditDialog(
            existing = null,
            onDismiss = { editing = EditTarget.None },
            onSave = { name -> viewModel.addList(name); editing = EditTarget.None },
            onDelete = null,
        )
        is EditTarget.Rename -> ListEditDialog(
            existing = target.list,
            onDismiss = { editing = EditTarget.None },
            onSave = { name -> viewModel.renameList(target.list, name); editing = EditTarget.None },
            onDelete = if (state.allLists.size > 1) {
                { viewModel.deleteList(target.list); editing = EditTarget.None }
            } else {
                null
            },
        )
        is EditTarget.NewTask -> ReminderSheet(
            lists = state.allLists,
            preselectedListId = state.defaultListId,
            onDismiss = { editing = EditTarget.None },
            onSave = { title, notes, listId -> viewModel.addTask(title, notes, listId); editing = EditTarget.None },
        )
        EditTarget.None -> Unit
    }
}

private sealed interface EditTarget {
    data object None : EditTarget
    data object NewList : EditTarget
    data object NewTask : EditTarget
    data class Rename(val list: TaskList) : EditTarget
}

@Composable
private fun ListRow(
    entry: ListWithCount,
    shape: Shape,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp)
            .background(MaterialTheme.colorScheme.surface, shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(ListIcons.resolve(entry.list.icon)),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = entry.list.name,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = entry.openTaskCount.toString(),
            fontFamily = MonoFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(16.dp),
        )
    }
}
