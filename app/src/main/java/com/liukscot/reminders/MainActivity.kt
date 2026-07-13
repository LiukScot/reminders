package com.liukscot.reminders

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.liukscot.reminders.data.RemindersDatabase
import com.liukscot.reminders.notifications.EXTRA_LIST_ID
import com.liukscot.reminders.ui.RemindersApp
import com.liukscot.reminders.ui.theme.RemindersTheme

class MainActivity : ComponentActivity() {
    private val requestNotificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RemindersDatabase.getInstance(applicationContext)
        requestNotificationPermissionIfNeeded()
        enableEdgeToEdge()
        val deepLinkListId = intent.getLongExtra(EXTRA_LIST_ID, -1).takeIf { it > 0 }
        setContent {
            RemindersTheme {
                RemindersApp(deepLinkListId = deepLinkListId)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
