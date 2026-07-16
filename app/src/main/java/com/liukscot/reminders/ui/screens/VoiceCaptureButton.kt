package com.liukscot.reminders.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.liukscot.reminders.R
import com.liukscot.reminders.data.RecurrenceRule
import com.liukscot.reminders.ui.components.GradientButton
import com.liukscot.reminders.ui.components.flowGradientBackground
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Stock-Android FAB stack: rounded-square buttons in a vertical column, "+" on top, voice mic at
// the bottom where the thumb lands.
private val FabShape = RoundedCornerShape(20.dp)
private val FabSize = 64.dp

@Composable
fun ReminderActionButtons(onAddReminder: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FloatingActionButton(
            onClick = onAddReminder,
            shape = FabShape,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(FabSize),
        ) {
            Icon(painterResource(R.drawable.ic_plus), contentDescription = "Add reminder")
        }
        VoiceCaptureButton()
    }
}

// Ref: Reminders App Mockup — the gradient mic on Home plus the full-screen voice overlay
// (Listening… with live transcript, then a NEW REMINDER confirmation card). Owns the mic FAB, the
// RECORD_AUDIO permission, and the overlay; hands the confirmed draft back to the caller to save.
@Composable
fun VoiceCaptureButton(
    modifier: Modifier = Modifier,
    viewModel: VoiceCaptureViewModel = rememberVoiceCaptureViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) viewModel.start() }

    Box(
        modifier = modifier
            .size(FabSize)
            .flowGradientBackground(FabShape)
            .clickable {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                if (granted) viewModel.start() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_mic),
            contentDescription = "Speak a reminder",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(24.dp),
        )
    }

    if (state != VoiceCaptureState.Idle) {
        Dialog(
            onDismissRequest = { viewModel.dismiss() },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            VoiceOverlay(
                state = state,
                onStop = { viewModel.stopListening() },
                onRetry = { viewModel.retry() },
                onDismiss = { viewModel.dismiss() },
                onAdd = { viewModel.addReminder() },
            )
        }
    }
}

// The overlay on its own, with no app screen behind it: the Quick Settings tile's entry point.
// Starts listening as soon as it appears and closes itself once the capture ends.
@Composable
fun VoiceCaptureScreen(
    onFinish: () -> Unit,
    viewModel: VoiceCaptureViewModel = rememberVoiceCaptureViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) viewModel.start() else onFinish() }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.start() else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
    // Both dismiss() and addReminder() land back on Idle. Waiting to see a non-Idle state first
    // matters: Idle is also the state before start() runs, and closing on that would race.
    var wasActive by remember { mutableStateOf(false) }
    LaunchedEffect(state) {
        if (state != VoiceCaptureState.Idle) wasActive = true else if (wasActive) onFinish()
    }

    if (state != VoiceCaptureState.Idle) {
        VoiceOverlay(
            state = state,
            onStop = { viewModel.stopListening() },
            onRetry = { viewModel.retry() },
            onDismiss = { viewModel.dismiss() },
            onAdd = { viewModel.addReminder() },
        )
    }
}

@Composable
private fun VoiceOverlay(
    state: VoiceCaptureState,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.86f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        // Close button, top-right.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(18.dp)
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

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (state) {
                is VoiceCaptureState.Listening -> ListeningContent(state.partial, onStop)
                VoiceCaptureState.Processing -> ProcessingContent()
                is VoiceCaptureState.Result -> ResultContent(state, onRetry, onAdd)
                VoiceCaptureState.MissingKey -> MessageContent(
                    "No API key",
                    "Add your AI provider's API key in Settings first.",
                    onDismiss,
                )
                is VoiceCaptureState.Error -> MessageContent("Couldn't create the reminder", state.message, onRetry)
                VoiceCaptureState.Idle -> Unit
            }
        }
    }
}

@Composable
private fun ListeningContent(partial: String, onStop: () -> Unit) {
    MicOrb(onClick = onStop)
    WaveformBars()
    Text("Listening…", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    if (partial.isNotBlank()) {
        Text(
            text = "“$partial”",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ProcessingContent() {
    MicOrb(onClick = {})
    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
    Text("Thinking…", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
}

@Composable
private fun ResultContent(
    result: VoiceCaptureState.Result,
    onRetry: () -> Unit,
    onAdd: () -> Unit,
) {
    val draft = result.draft
    Text(
        text = "“${result.transcript}”",
        fontSize = 15.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "NEW REMINDER",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.3.sp,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = draft.title.ifBlank { "Untitled reminder" },
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            draft.date?.let { VoiceChip(R.drawable.ic_calendar, dateLabel(it)) }
            draft.time?.let { VoiceChip(R.drawable.ic_clock, it.format(TIME_FORMAT)) }
            draft.recurrence?.let { VoiceChip(R.drawable.ic_repeat, recurrenceLabel(it)) }
            result.listName?.let { VoiceChip(R.drawable.ic_list_checks, it) }
        }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OverlayButton(
            text = "Try again",
            modifier = Modifier.weight(1f),
            background = MaterialTheme.colorScheme.surfaceVariant,
            textColor = MaterialTheme.colorScheme.onSurface,
            onClick = onRetry,
        )
        GradientButton(
            text = "Add reminder",
            onClick = onAdd,
            enabled = result.listId != null,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MessageContent(title: String, message: String, onAction: () -> Unit) {
    Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    Text(message, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    OverlayButton(
        text = "Try again",
        background = MaterialTheme.colorScheme.surfaceVariant,
        textColor = MaterialTheme.colorScheme.onSurface,
        onClick = onAction,
    )
}

@Composable
private fun MicOrb(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(112.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .flowGradientBackground(CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_mic),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(34.dp),
            )
        }
    }
}

@Composable
private fun WaveformBars() {
    val transition = rememberInfiniteTransition(label = "waveform")
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(6) { index ->
            val height by transition.animateFloat(
                initialValue = 8f,
                targetValue = 26f,
                animationSpec = infiniteRepeatable(
                    animation = tween(420, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 90),
                ),
                label = "bar$index",
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
            )
        }
    }
}

@Composable
private fun VoiceChip(icon: Int, label: String) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(13.dp),
        )
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun OverlayButton(
    text: String,
    background: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)
    }
}

private val TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm", Locale.ENGLISH)

private fun dateLabel(date: LocalDate): String = when (date) {
    LocalDate.now() -> "Today"
    LocalDate.now().plusDays(1) -> "Tomorrow"
    else -> date.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH))
}

private fun recurrenceLabel(rule: RecurrenceRule): String =
    rule.frequency.name.lowercase().replaceFirstChar { it.uppercase() }
