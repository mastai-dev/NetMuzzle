package com.lightvpn.firewall.service

import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.lightvpn.firewall.data.FirewallPreferences
import com.lightvpn.firewall.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.N)
class FirewallTileService : TileService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var preferences: FirewallPreferences

    override fun onCreate() {
        super.onCreate()
        preferences = FirewallPreferences(this)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    private fun updateTileState() {
        serviceScope.launch {
            val isEnabled = preferences.isFirewallEnabledSync()
            val tile = qsTile ?: return@launch
            tile.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        serviceScope.launch {
            val isCurrentlyEnabled = preferences.isFirewallEnabledSync()
            val newEnabled = !isCurrentlyEnabled

            if (newEnabled) {
                // Sprawdzamy czy mamy uprawnienie systemowe VPN
                val prepareIntent = VpnService.prepare(this@FirewallTileService)
                if (prepareIntent != null) {
                    // Wymagana interakcja użytkownika - otwieramy aplikację
                    val intent = Intent(this@FirewallTileService, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivityAndCollapse(intent)
                    return@launch
                }
                preferences.setFirewallEnabled(true)
                FirewallService.startService(this@FirewallTileService)
            } else {
                preferences.setFirewallEnabled(false)
                FirewallService.stopService(this@FirewallTileService)
            }

            val tile = qsTile ?: return@launch
            tile.state = if (newEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.updateTile()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
