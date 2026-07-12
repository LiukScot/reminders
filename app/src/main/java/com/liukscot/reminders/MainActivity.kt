package com.liukscot.reminders

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.liukscot.reminders.data.RemindersDatabase
import com.liukscot.reminders.ui.RemindersApp
import com.liukscot.reminders.ui.theme.RemindersTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RemindersDatabase.getInstance(applicationContext)
        enableEdgeToEdge()
        setContent {
            RemindersTheme {
                RemindersApp()
            }
        }
    }
}
