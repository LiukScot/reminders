package com.liukscot.reminders.ui.screens

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liukscot.reminders.R
import com.liukscot.reminders.data.AiProvider
import com.liukscot.reminders.ui.theme.MonoFontFamily

// Ref: Reminders App Mockup "Settings" screen (data-screen-label="Settings")
// for section/row styling. Only the "Default list" and "AI model" rows are
// built here — the mockup's other sections (backup, API key, notifications)
// belong to their own not-yet-built issues.
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = rememberSettingsViewModel()) {
    val state by viewModel.uiState.collectAsState()
    var pickerOpen by remember { mutableStateOf(false) }
    var providerPickerOpen by remember { mutableStateOf(false) }
    var keyDialogOpen by remember { mutableStateOf(false) }

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
            shape = groupedRowShape(0, 1, bigRadius = 14.dp, smallRadius = 4.dp),
            onClick = { pickerOpen = true },
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
            shape = groupedRowShape(0, 2, bigRadius = 14.dp, smallRadius = 4.dp),
            onClick = { providerPickerOpen = true },
        )
        // 2dp hairline between grouped rows, matching the "My Lists" rows so the group reads as one.
        Spacer(modifier = Modifier.height(2.dp))
        SettingsRow(
            icon = R.drawable.ic_key,
            title = "${state.aiProvider.displayName} API key",
            value = state.apiKeyHint ?: "Not set",
            shape = groupedRowShape(1, 2, bigRadius = 14.dp, smallRadius = 4.dp),
            onClick = { keyDialogOpen = true },
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
}

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

@Composable
private fun SettingsRow(icon: Int, title: String, value: String, shape: Shape, onClick: () -> Unit) {
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
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            fontFamily = MonoFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
