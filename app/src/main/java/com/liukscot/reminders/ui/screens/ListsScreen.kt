package com.liukscot.reminders.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.liukscot.reminders.ui.ScreenFab
import com.liukscot.reminders.data.ListIcons
import com.liukscot.reminders.data.SmartList
import com.liukscot.reminders.data.TaskList
import com.liukscot.reminders.ui.theme.MonoFontFamily

// Ref: Reminders App Mockup "Home" screen — header, smart-list card grid, "My
// lists" section, and groupRadius-style rows.
@Composable
fun ListsScreen(
    onOpenList: (Long) -> Unit,
    onOpenSmartList: (SmartList) -> Unit,
    onOpenSearch: () -> Unit,
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
            // Ref: mockup's Home search pill — not a field, it opens the Search screen.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp)
                    .height(46.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .clickable(onClick = onOpenSearch)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Search reminders",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            SmartListGrid(
                countFor = state::countFor,
                onOpen = onOpenSmartList,
                modifier = Modifier.padding(bottom = 18.dp),
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

        ScreenFab(onAdd = { editing = EditTarget.NewTask })
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
            onSave = { title, notes, listId, tags, flagged, priority, dueAt, hasDueTime, recFreq, recInterval, recByDay, recAnchor ->
                viewModel.addTask(
                    title, notes, listId, tags, flagged, priority, dueAt, hasDueTime,
                    recFreq, recInterval, recByDay, recAnchor,
                )
                editing = EditTarget.None
            },
        )
        EditTarget.None -> Unit
    }
}

// Ref: mockup's 2x2 card grid between the search pill and "MY LISTS". Enum order is card order.
@Composable
private fun SmartListGrid(
    countFor: (SmartList) -> Int,
    onOpen: (SmartList) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SmartList.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { smartList ->
                    SmartListCard(
                        smartList = smartList,
                        count = countFor(smartList),
                        onClick = { onOpen(smartList) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SmartListCard(
    smartList: SmartList,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(smartList.icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = count.toString(),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = smartList.label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )
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
            .heightIn(min = 72.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(ListIcons.resolve(entry.list.icon)),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
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
