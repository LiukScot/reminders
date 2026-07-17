package com.liukscot.reminders.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.liukscot.reminders.R
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Past this much of the row's width, letting go fires the action.
private const val ActionThresholdFraction = 0.35f

// Swipe right completes, swipe left deletes — wraps any task row so every list behaves the same.
//
// Written by hand rather than with SwipeToDismissBox: that component exists to throw a row out of
// the list, and these rows stay. Every way of asking it to dismiss-then-come-back fought its own
// model — the action fired twice per gesture, and the row never came back. Here the row is only
// ever offset and sprung back, so there is no dismissal to undo. It also matches DayTaskRow, whose
// drag&drop is built from the same pieces.
//
// `shape` and `modifier` must be the row's own: the background sits directly behind it, so it only
// lines up if it is cut to the same corners and the row's outer spacing is applied out here, to the
// box, rather than inside the row — otherwise the colour bleeds into the gap between rows.
@Composable
internal fun SwipeableTaskRow(
    onComplete: () -> Unit,
    onDeleteRequest: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var width by remember { mutableIntStateOf(0) }

    Box(modifier = modifier.onSizeChanged { width = it.width }) {
        // Only while the row is off its mark — at rest there is nothing behind it to show.
        if (offsetX.value != 0f) {
            SwipeBackground(
                swipingRight = offsetX.value > 0f,
                shape = shape,
                modifier = Modifier.matchParentSize(),
            )
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch { offsetX.snapTo(offsetX.value + delta) }
                    },
                    onDragStopped = {
                        // Read the offset once, before the spring starts moving it back.
                        val released = offsetX.value
                        val threshold = width * ActionThresholdFraction
                        if (threshold > 0f) {
                            if (released > threshold) onComplete()
                            if (released < -threshold) onDeleteRequest()
                        }
                        offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                    },
                ),
        ) {
            content()
        }
    }
}

@Composable
private fun SwipeBackground(swipingRight: Boolean, shape: Shape, modifier: Modifier = Modifier) {
    // Colour alone can't say which action is armed — the icon does, and it sits on the side the
    // finger came from.
    val color = if (swipingRight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Box(
        modifier = modifier.background(color, shape),
        contentAlignment = if (swipingRight) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Icon(
            painter = painterResource(if (swipingRight) R.drawable.ic_check else R.drawable.ic_trash_2),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 20.dp).size(22.dp),
        )
    }
}

// Deleting a reminder can't be undone, and a swipe is easy to do by accident — so it asks first.
@Composable
internal fun DeleteReminderDialog(title: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete reminder?") },
        text = { Text("“$title” will be deleted. This cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
