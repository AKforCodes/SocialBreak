package com.socialbreak.android.services

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.socialbreak.android.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.N)
class FocusSprintTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private lateinit var prefs: PreferencesManager

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager(applicationContext)
    }

    override fun onStartListening() {
        super.onStartListening()
        serviceScope.launch {
            val focusUntil = prefs.focusBlockUntil.first()
            val now = System.currentTimeMillis()
            val isActive = now < focusUntil

            qsTile?.apply {
                state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                label = if (isActive) {
                    val remainingMins = ((focusUntil - now) / 60000).coerceAtLeast(1)
                    "Focusing (${remainingMins}m)"
                } else {
                    "Focus Sprint"
                }
                updateTile()
            }
        }
    }

    override fun onClick() {
        super.onClick()
        serviceScope.launch {
            val focusUntil = prefs.focusBlockUntil.first()
            val now = System.currentTimeMillis()

            if (now < focusUntil) {
                // Already focusing, cancel
                prefs.setFocusBlockUntil(0L)
                qsTile?.apply {
                    state = Tile.STATE_INACTIVE
                    label = "Focus Sprint"
                    updateTile()
                }
            } else {
                // Start 25 minute sprint
                val sprintUntil = now + (25 * 60 * 1000L)
                prefs.setFocusBlockUntil(sprintUntil)
                qsTile?.apply {
                    state = Tile.STATE_ACTIVE
                    label = "Focusing (25m)"
                    updateTile()
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.N)
class SleepModeTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private lateinit var prefs: PreferencesManager

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager(applicationContext)
    }

    override fun onStartListening() {
        super.onStartListening()
        serviceScope.launch {
            val isSleep = prefs.sleepModeEnabled.first()
            qsTile?.apply {
                state = if (isSleep) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                label = if (isSleep) "Sleep Mode: On" else "Sleep Mode"
                updateTile()
            }
        }
    }

    override fun onClick() {
        super.onClick()
        serviceScope.launch {
            val isSleep = prefs.sleepModeEnabled.first()
            val newState = !isSleep
            prefs.setSleepMode(newState)

            qsTile?.apply {
                state = if (newState) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                label = if (newState) "Sleep Mode: On" else "Sleep Mode"
                updateTile()
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.N)
class HardModeTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private lateinit var prefs: PreferencesManager

    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager(applicationContext)
    }

    override fun onStartListening() {
        super.onStartListening()
        serviceScope.launch {
            val isHard = prefs.hardModeEnabled.first()
            qsTile?.apply {
                state = if (isHard) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                label = if (isHard) "Hard Mode: Locked" else "Hard Mode: Off"
                updateTile()
            }
        }
    }

    override fun onClick() {
        super.onClick()
        // Hard mode cannot be toggled off directly from tile without gauntlet
        serviceScope.launch {
            val isHard = prefs.hardModeEnabled.first()
            if (!isHard) {
                prefs.setHardMode(true)
                qsTile?.apply {
                    state = Tile.STATE_ACTIVE
                    label = "Hard Mode: Locked"
                    updateTile()
                }
            }
        }
    }
}
