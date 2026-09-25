package com.lightvpn.firewall.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import com.lightvpn.firewall.data.FirewallPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d(TAG, "Odebrano zdarzenie startu urządzenia: $action")

            val preferences = FirewallPreferences(context)
            val scope = CoroutineScope(Dispatchers.IO)

            scope.launch {
                val shouldStartOnBoot = preferences.isStartOnBootSync()
                val isFirewallEnabled = preferences.isFirewallEnabledSync()

                if (shouldStartOnBoot && isFirewallEnabled) {
                    // Sprawdzamy czy mamy już uprawnienie VPN (czy VpnService.prepare nie wymaga ponownego dialogu)
                    val prepareIntent = VpnService.prepare(context)
                    if (prepareIntent == null) {
                        Log.i(TAG, "Automatyczne uruchamianie FirewallService po restarcie...")
                        FirewallService.startService(context)
                    } else {
                        Log.w(TAG, "Wymagana ponowna autoryzacja użytkownika dla VpnService, pomijam autostart.")
                    }
                }
            }
        }
    }
}
