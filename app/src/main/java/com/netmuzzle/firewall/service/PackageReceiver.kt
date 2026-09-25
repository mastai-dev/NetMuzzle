package com.netmuzzle.firewall.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.netmuzzle.firewall.data.FirewallPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PackageReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PackageReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PACKAGE_FULLY_REMOVED) {
            val packageName = intent.data?.schemeSpecificPart ?: return
            Log.d(TAG, "Wykryto usunięcie pakietu z urządzenia: $packageName")

            val preferences = FirewallPreferences(context)
            val scope = CoroutineScope(Dispatchers.IO)

            scope.launch {
                val blocked = preferences.getBlockedPackagesSync()
                if (blocked.contains(packageName)) {
                    Log.i(TAG, "Usuwanie pakietu $packageName z listy zablokowanych...")
                    preferences.removePackage(packageName)
                    FirewallService.reloadRules(context)
                }
            }
        }
    }
}
