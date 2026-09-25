package com.lightvpn.firewall.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lightvpn_prefs")

class FirewallPreferences(private val context: Context) {

    companion object {
        val KEY_BLOCKED_PACKAGES = stringSetPreferencesKey("blocked_packages")
        val KEY_FIREWALL_ENABLED = booleanPreferencesKey("firewall_enabled")
        val KEY_START_ON_BOOT = booleanPreferencesKey("start_on_boot")
        val KEY_SHOW_SYSTEM_APPS = booleanPreferencesKey("show_system_apps")
    }

    val blockedPackages: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_BLOCKED_PACKAGES] ?: emptySet()
    }

    val isFirewallEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_FIREWALL_ENABLED] ?: false
    }

    val startOnBoot: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_START_ON_BOOT] ?: true
    }

    val showSystemApps: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_SHOW_SYSTEM_APPS] ?: false
    }

    suspend fun getBlockedPackagesSync(): Set<String> {
        return blockedPackages.first()
    }

    suspend fun isFirewallEnabledSync(): Boolean {
        return isFirewallEnabled.first()
    }

    suspend fun isStartOnBootSync(): Boolean {
        return startOnBoot.first()
    }

    suspend fun setFirewallEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_FIREWALL_ENABLED] = enabled
        }
    }

    suspend fun setStartOnBoot(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_START_ON_BOOT] = enabled
        }
    }

    suspend fun setShowSystemApps(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SHOW_SYSTEM_APPS] = show
        }
    }

    suspend fun setPackageBlocked(packageName: String, blocked: Boolean) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_BLOCKED_PACKAGES]?.toMutableSet() ?: mutableSetOf()
            if (blocked) {
                current.add(packageName)
            } else {
                current.remove(packageName)
            }
            preferences[KEY_BLOCKED_PACKAGES] = current
        }
    }

    suspend fun removePackage(packageName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_BLOCKED_PACKAGES]?.toMutableSet() ?: return@edit
            if (current.remove(packageName)) {
                preferences[KEY_BLOCKED_PACKAGES] = current
            }
        }
    }
}
