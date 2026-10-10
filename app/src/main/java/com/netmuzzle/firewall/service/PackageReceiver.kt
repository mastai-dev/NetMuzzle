package com.netmuzzle.firewall.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.netmuzzle.firewall.data.FirewallPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PackageReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PackageReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PACKAGE_FULLY_REMOVED) {
            val packageName = intent.data?.schemeSpecificPart ?: return
            Log.d(TAG, "Wykryto usunięcie pakietu z urządzenia: $packageName")

            val pendingResult = goAsync()
            val preferences = FirewallPreferences(context)
            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

            scope.launch {
                try {
                    val blocked = preferences.getBlockedPackagesSync()
                    val adBlocked = preferences.getAdBlockPackagesSync()
                    if (blocked.contains(packageName) || adBlocked.contains(packageName)) {
                        Log.i(TAG, "Usuwanie pakietu $packageName z konfiguracji...")
                        preferences.removePackage(packageName)
                        FirewallService.reloadRules(context)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Błąd podczas usuwania pakietu z konfiguracji", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
