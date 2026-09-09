package com.xaniihub.app.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.xaniihub.app.MainActivity
import com.xaniihub.app.R
import com.xaniihub.app.tracking.TrackingConstants
import java.time.LocalDate

class StepsTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        refresh()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        super.onClick()
        refresh()
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pending = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun formatInt(value: Int): String = java.text.NumberFormat.getIntegerInstance().format(value)

    private fun refresh() {
        val tile = qsTile ?: return
        val prefs = getSharedPreferences(TrackingConstants.PREFS_NAME, MODE_PRIVATE)
        val today = LocalDate.now().toEpochDay()
        val steps = if (prefs.getLong(TrackingConstants.PREF_LATEST_STEPS_DATE, Long.MIN_VALUE) == today) {
            prefs.getInt(TrackingConstants.PREF_LATEST_STEPS, 0)
        } else {
            0
        }
        val goal = prefs.getInt(TrackingConstants.PREF_DAILY_GOAL, 8_000)
        val percent = if (goal > 0) (steps * 100 / goal).coerceIn(0, 999) else 0

        tile.state = Tile.STATE_ACTIVE
        tile.label = formatInt(steps)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = percent.toString() + "% / " + formatInt(goal)
        }
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_steps)
        tile.updateTile()
    }
}
