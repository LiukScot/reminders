package com.liukscot.reminders.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liukscot.reminders.R
import com.liukscot.reminders.data.AiProvider
import com.liukscot.reminders.ui.navigation.Destination
import com.liukscot.reminders.ui.theme.MonoFontFamily
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Ref: Reminders App Mockup "Settings" screen (data-screen-label="Settings")
// for section/row styling. The mockup's "Auto backup" row is a scheduled
// backup, its own not-yet-built issue — this section is the manual export and
// restore, and reuses that row's icon and subtitle treatment.
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = rememberSettingsViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var pickerOpen by remember { mutableStateOf(false) }
    var providerPickerOpen by remember { mutableStateOf(false) }
    var keyDialogOpen by remember { mutableStateOf(false) }
    var snoozePickerOpen by remember { mutableStateOf(false) }
    var startPagePickerOpen by remember { mutableStateOf(false) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }

    val message by viewModel.message.collectAsState()
    val context = LocalContext.current
    message?.let {
        LaunchedEffect(it) {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessage()
        }
    }

    // The system pickers own the file: no storage permission, and the user picks where a backup
    // lands and which one to restore. A null uri means they backed out of the picker.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE),
    ) { uri -> uri?.let(viewModel::exportTo) }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> restoreUri = uri }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Text(
            text = "Settings",
            style = TextStyle(
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
        Text(
            text = "ORGANIZATION",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.3.sp,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        SettingsRow(
            icon = R.drawable.ic_list_checks,
            title = "Default list",
            value = state.defaultListName ?: "None",
            shape = groupedRowShape(0, 2, bigRadius = 12.dp, smallRadius = 4.dp),
            onClick = { pickerOpen = true },
        )
        Spacer(modifier = Modifier.height(2.dp))
        SettingsRow(
            icon = R.drawable.ic_calendar,
            title = "Starting page",
            value = startPageDestination(state.startPageRoute).label,
            shape = groupedRowShape(1, 2, bigRadius = 12.dp, smallRadius = 4.dp),
            onClick = { startPagePickerOpen = true },
        )

        Spacer(modifier = Modifier.height(22.dp))
        Text(
            text = "BACKUP & EXPORT",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.3.sp,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        SettingsRow(
            icon = R.drawable.ic_database,
            title = "Export to file",
            subtitle = "All lists, tasks and tags",
            shape = groupedRowShape(0, 2, bigRadius = 12.dp, smallRadius = 4.dp),
            onClick = { exportLauncher.launch(defaultBackupFileName()) },
        )
        Spacer(modifier = Modifier.height(2.dp))
        SettingsRow(
            icon = R.drawable.ic_download,
            title = "Restore from file",
            subtitle = "Replaces everything on this device",
            shape = groupedRowShape(1, 2, bigRadius = 12.dp, smallRadius = 4.dp),
            onClick = { restoreLauncher.launch(BACKUP_PICKER_MIME_TYPES) },
        )

        Spacer(modifier = Modifier.height(22.dp))
        Text(
            text = "VOICE & AI",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.3.sp,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        SettingsRow(
            icon = R.drawable.ic_sparkles,
            title = "AI model",
            value = state.aiProvider.displayName,
            shape = groupedRowShape(0, 2, bigRadius = 12.dp, smallRadius = 4.dp),
            onClick = { providerPickerOpen = true },
        )
        // 2dp hairline between grouped rows, matching the "My Lists" rows so the group reads as one.
        Spacer(modifier = Modifier.height(2.dp))
        SettingsRow(
            icon = R.drawable.ic_key,
            title = "${state.aiProvider.displayName} API key",
            value = state.apiKeyHint ?: "Not set",
            shape = groupedRowShape(1, 2, bigRadius = 12.dp, smallRadius = 4.dp),
            onClick = { keyDialogOpen = true },
        )

        Spacer(modifier = Modifier.height(22.dp))
        Text(
            text = "NOTIFICATIONS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.3.sp,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        SettingsRow(
            icon = R.drawable.ic_clock,
            title = "Quick snooze",
            subtitle = "The notification's one-tap snooze button",
            value = quickSnoozeLabel(state.quickSnoozeMinutes),
            shape = groupedRowShape(0, 1, bigRadius = 12.dp, smallRadius = 4.dp),
            onClick = { snoozePickerOpen = true },
        )
    }

    if (pickerOpen) {
        ListPickerDialog(
            lists = state.lists,
            selectedId = state.defaultListId,
            onDismiss = { pickerOpen = false },
            onSelect = { id -> viewModel.setDefaultList(id); pickerOpen = false },
        )
    }
    if (providerPickerOpen) {
        AiProviderPickerDialog(
            selected = state.aiProvider,
            onDismiss = { providerPickerOpen = false },
            onSelect = { provider -> viewModel.setAiProvider(provider); providerPickerOpen = false },
        )
    }
    if (keyDialogOpen) {
        ApiKeyDialog(
            providerName = state.aiProvider.displayName,
            onDismiss = { keyDialogOpen = false },
            onSave = { key -> viewModel.setApiKey(key); keyDialogOpen = false },
        )
    }
    if (snoozePickerOpen) {
        QuickSnoozePickerDialog(
            selected = state.quickSnoozeMinutes,
            onDismiss = { snoozePickerOpen = false },
            onSelect = { minutes -> viewModel.setQuickSnoozeMinutes(minutes); snoozePickerOpen = false },
        )
    }
    if (startPagePickerOpen) {
        StartPagePickerDialog(
            selectedRoute = state.startPageRoute,
            onDismiss = { startPagePickerOpen = false },
            onSelect = { route -> viewModel.setStartPage(route); startPagePickerOpen = false },
        )
    }
    // Restoring drops every reminder currently on the device, and there is no undo — so it asks.
    restoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { restoreUri = null },
            title = { Text("Restore backup?") },
            text = { Text("Every list, reminder and tag on this device is replaced by the ones in the file. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.restoreFrom(uri); restoreUri = null }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { restoreUri = null }) { Text("Cancel") }
            },
        )
    }
}

private const val BACKUP_MIME_TYPE = "application/json"

// Backups that have been through a share sheet or a cloud drive often come back typed as a generic
// binary, and the picker greys out anything it isn't told to accept.
private val BACKUP_PICKER_MIME_TYPES = arrayOf(BACKUP_MIME_TYPE, "application/octet-stream")

private val BackupFileNameFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

private fun defaultBackupFileName(): String =
    "reminders-${LocalDate.now().format(BackupFileNameFormatter)}.json"

@Composable
private fun AiProviderPickerDialog(
    selected: AiProvider,
    onDismiss: () -> Unit,
    onSelect: (AiProvider) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI model") },
        text = {
            Column {
                AiProvider.entries.forEach { provider ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(provider) }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (provider == selected) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        } else {
                            Spacer(modifier = Modifier.size(18.dp))
                        }
                        Text(text = provider.displayName, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

// Settings itself makes no sense as a landing page, so it is not offered.
private val START_PAGE_CHOICES = Destination.entries.filter { it != Destination.Settings }

// The stored route is validated against the offered pages; anything else falls back to Lists so
// the row never shows a blank.
private fun startPageDestination(route: String): Destination =
    START_PAGE_CHOICES.firstOrNull { it.route == route } ?: Destination.Lists

@Composable
private fun StartPagePickerDialog(
    selectedRoute: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Starting page") },
        text = {
            Column {
                START_PAGE_CHOICES.forEach { destination ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(destination.route) }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (destination.route == selectedRoute) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        } else {
                            Spacer(modifier = Modifier.size(18.dp))
                        }
                        Text(text = destination.label, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun QuickSnoozePickerDialog(
    selected: Long,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick snooze") },
        text = {
            Column {
                QUICK_SNOOZE_CHOICES.forEach { minutes ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(minutes) }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (minutes == selected) {
                            Icon(
                                painter = painterResource(R.drawable.ic_check),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        } else {
                            Spacer(modifier = Modifier.size(18.dp))
                        }
                        Text(text = quickSnoozeLabel(minutes), color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun ApiKeyDialog(
    providerName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var key by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$providerName API key") },
        text = {
            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                singleLine = true,
                placeholder = { Text("Paste your key") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(key) }, enabled = key.isNotBlank()) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

// Ref: mockup rows carry either a trailing value ("••••2f4a") or a subtitle under the title
// ("Daily, over Wi-Fi · last run ~2 h ago"), never both.
@Composable
private fun SettingsRow(
    icon: Int,
    title: String,
    shape: Shape,
    onClick: () -> Unit,
    value: String? = null,
    subtitle: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, shape)
            .clickable(onClick = onClick)
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
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            subtitle?.let {
                Text(
                    text = it,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        value?.let {
            Text(
                text = it,
                fontFamily = MonoFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
