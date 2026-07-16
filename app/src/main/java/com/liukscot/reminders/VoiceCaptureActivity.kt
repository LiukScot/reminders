package com.liukscot.reminders

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.liukscot.reminders.ui.screens.VoiceCaptureScreen
import com.liukscot.reminders.ui.theme.RemindersTheme

// Voice capture with no app behind it, launched by the Quick Settings tile. Separate from
// MainActivity so the tile can show it over the lock screen (see android:showWhenLocked) without
// exposing the reminder lists too.
class VoiceCaptureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RemindersTheme {
                VoiceCaptureScreen(onFinish = { finish() })
            }
        }
    }
}
