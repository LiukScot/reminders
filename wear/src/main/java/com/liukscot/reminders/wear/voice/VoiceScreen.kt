package com.liukscot.reminders.wear.voice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.liukscot.reminders.ui.theme.EmberFlowA
import com.liukscot.reminders.ui.theme.EmberFlowB
import com.liukscot.reminders.ui.theme.EmberTextOnAccent
import com.liukscot.reminders.wear.R

// The five states the phone's VoiceCaptureState collapses into on a watch: its MissingKey and
// Error both land on Blocked, since the watch's answer to either is the same one sentence and one
// button.
sealed interface VoiceUiState {
    data object Idle : VoiceUiState
    data class Listening(val partial: String) : VoiceUiState
    data class Processing(val transcript: String) : VoiceUiState
    data class Result(val title: String, val due: String, val list: String) : VoiceUiState
    data class Blocked(val headline: String, val detail: String, val action: String) : VoiceUiState
}

// The signature gradient. Only ever on a large element — a small detail takes the solid accent
// instead (design system rule).
internal val EmberGradient = Brush.linearGradient(listOf(EmberFlowA, EmberFlowB))

@Composable
fun VoiceScreen(
    state: VoiceUiState,
    onMicTap: () -> Unit,
    onStopListening: () -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            is VoiceUiState.Idle -> IdleState(onMicTap)
            is VoiceUiState.Listening -> ListeningState(state.partial, onStopListening)
            is VoiceUiState.Processing -> ProcessingState(state.transcript)
            is VoiceUiState.Result -> ResultState(state, onSave, onRetry, onDiscard)
            is VoiceUiState.Blocked -> BlockedState(state, onMicTap)
        }
    }
}

@Composable
private fun IdleState(onMicTap: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        GradientCircle(size = 89.dp, onClick = onMicTap) {
            Icon(
                painter = painterResource(R.drawable.ic_mic),
                contentDescription = "Speak",
                tint = EmberTextOnAccent,
                modifier = Modifier.size(38.dp),
            )
        }
        Text(
            text = "Tap to speak",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ListeningState(partial: String, onStop: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Box(Modifier.size(5.dp).clip(CircleShape).background(EmberFlowB))
            Text("Listening", style = MaterialTheme.typography.labelMedium)
        }
        Text(
            text = partial.ifEmpty { "…" },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        GradientCircle(size = 32.dp, onClick = onStop) {
            Box(
                Modifier
                    .size(13.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(EmberTextOnAccent),
            )
        }
    }
}

@Composable
private fun ProcessingState(transcript: String) {
    // Wear puts indeterminate progress on the bezel rather than in a centred spinner: on a round
    // screen the edge is the only place with room to spare.
    CircularProgressIndicator(modifier = Modifier.fillMaxSize().padding(3.dp))
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(horizontal = 34.dp),
    ) {
        Text(
            text = "“$transcript”",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Reading that back…",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ResultState(
    state: VoiceUiState.Result,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onDiscard: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterVertically),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
    ) {
        Text(
            text = "NEW REMINDER",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = state.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        MetaRow(R.drawable.ic_clock, state.due)
        MetaRow(R.drawable.ic_list, state.list)
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 5.dp),
        ) {
            PlainCircle(R.drawable.ic_retry, "Try again", onRetry)
            GradientPill(text = "Save", onClick = onSave)
            PlainCircle(R.drawable.ic_close, "Discard", onDiscard)
        }
    }
}

@Composable
private fun BlockedState(state: VoiceUiState.Blocked, onAction: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterVertically),
        modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_mic_off),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(21.dp),
            )
        }
        Text(
            text = state.headline,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = state.detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        GradientPill(text = state.action, onClick = onAction, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun MetaRow(icon: Int, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
