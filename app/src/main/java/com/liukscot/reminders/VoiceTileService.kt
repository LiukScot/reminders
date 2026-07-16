package com.liukscot.reminders

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class VoiceTileService : TileService() {
    // The tile is an action, not a toggle — keep it inactive so it never renders as "on".
    override fun onStartListening() {
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            // Hand the launch to System UI so a tap starts the activity directly. Going through
            // onClick() instead makes the tap wait for our process to start and the tile service to
            // bind, measured at ~3.4s once the process had been killed.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                setActivityLaunchForClick(voicePendingIntent())
            }
            updateTile()
        }
    }

    // Only reached below API 34, where System UI cannot launch the activity itself.
    override fun onClick() {
        @Suppress("DEPRECATION")
        startActivityAndCollapse(voiceIntent())
    }

    private fun voiceIntent() = Intent(this, VoiceCaptureActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun voicePendingIntent(): PendingIntent =
        PendingIntent.getActivity(this, 0, voiceIntent(), PendingIntent.FLAG_IMMUTABLE)
}
