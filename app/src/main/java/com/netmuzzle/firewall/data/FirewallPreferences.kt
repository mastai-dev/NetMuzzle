package com.netmuzzle.firewall.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.netmuzzle.firewall.model.BlockMode
import com.netmuzzle.firewall.model.DefaultAdNetworks
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "netmuzzle_prefs")

class FirewallPreferences(private val context: Context) {

    companion object {
        val KEY_BLOCKED_PACKAGES = stringSetPreferencesKey("blocked_packages") // Full blackhole
        val KEY_ADBLOCK_PACKAGES = stringSetPreferencesKey("adblock_packages") // Game Shield (DNS filter)
        val KEY_FIREWALL_ENABLED = booleanPreferencesKey("firewall_enabled")
        val KEY_START_ON_BOOT = booleanPreferencesKey("start_on_boot")
        val KEY_SHOW_SYSTEM_APPS = booleanPreferencesKey("show_system_apps")

        val KEY_DISABLED_AD_NETWORKS = stringSetPreferencesKey("disabled_ad_networks")
        val KEY_CUSTOM_AD_DOMAINS = stringSetPreferencesKey("custom_ad_domains")
        val KEY_DISABLED_CUSTOM_DOMAINS = stringSetPreferencesKey("disabled_custom_domains")
    }

    val blockedPackages: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_BLOCKED_PACKAGES] ?: emptySet()
    }

    val adBlockPackages: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_ADBLOCK_PACKAGES] ?: emptySet()
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

    val disabledAdNetworks: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_DISABLED_AD_NETWORKS] ?: emptySet()
    }

    val customAdDomains: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_CUSTOM_AD_DOMAINS] ?: emptySet()
    }

    val disabledCustomDomains: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[KEY_DISABLED_CUSTOM_DOMAINS] ?: emptySet()
    }

    suspend fun getBlockedPackagesSync(): Set<String> = blockedPackages.first()
    suspend fun getAdBlockPackagesSync(): Set<String> = adBlockPackages.first()
    suspend fun isFirewallEnabledSync(): Boolean = isFirewallEnabled.first()
    suspend fun isStartOnBootSync(): Boolean = startOnBoot.first()

    suspend fun getActiveBlockedDomainsSync(): Set<String> {
        val disabledNets = disabledAdNetworks.first()
        val customDoms = customAdDomains.first()
        val disabledCustom = disabledCustomDomains.first()
        return DefaultAdNetworks.computeActiveBlockedDomains(disabledNets, customDoms, disabledCustom)
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

    suspend fun setPackageBlockMode(packageName: String, mode: BlockMode) {
        context.dataStore.edit { preferences ->
            val blocked = preferences[KEY_BLOCKED_PACKAGES]?.toMutableSet() ?: mutableSetOf()
            val adBlock = preferences[KEY_ADBLOCK_PACKAGES]?.toMutableSet() ?: mutableSetOf()

            when (mode) {
                BlockMode.ALLOW -> {
                    blocked.remove(packageName)
                    adBlock.remove(packageName)
                }
                BlockMode.AD_BLOCK -> {
                    blocked.remove(packageName)
                    adBlock.add(packageName)
                }
                BlockMode.FULL_BLOCK -> {
                    blocked.add(packageName)
                    adBlock.remove(packageName)
                }
            }

            preferences[KEY_BLOCKED_PACKAGES] = blocked
            preferences[KEY_ADBLOCK_PACKAGES] = adBlock
        }
    }

    suspend fun removePackage(packageName: String) {
        context.dataStore.edit { preferences ->
            val blocked = preferences[KEY_BLOCKED_PACKAGES]?.toMutableSet()
            if (blocked != null && blocked.remove(packageName)) {
                preferences[KEY_BLOCKED_PACKAGES] = blocked
            }
            val adBlock = preferences[KEY_ADBLOCK_PACKAGES]?.toMutableSet()
            if (adBlock != null && adBlock.remove(packageName)) {
                preferences[KEY_ADBLOCK_PACKAGES] = adBlock
            }
        }
    }

    suspend fun setAdNetworkEnabled(networkId: String, enabled: Boolean) {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_DISABLED_AD_NETWORKS]?.toMutableSet() ?: mutableSetOf()
            if (enabled) {
                current.remove(networkId)
            } else {
                current.add(networkId)
            }
            preferences[KEY_DISABLED_AD_NETWORKS] = current
        }
    }

    suspend fun addCustomAdDomain(rawDomain: String): Boolean {
        val cleanDomain = rawDomain.trim().lowercase()
            .removePrefix("http://")
            .removePrefix("https://")
            .trimEnd('/')
        if (cleanDomain.isBlank() || !cleanDomain.contains('.')) return false

        context.dataStore.edit { preferences ->
            val current = preferences[KEY_CUSTOM_AD_DOMAINS]?.toMutableSet() ?: mutableSetOf()
            current.add(cleanDomain)
            preferences[KEY_CUSTOM_AD_DOMAINS] = current

            // Ensure not disabled by default
            val disabled = preferences[KEY_DISABLED_CUSTOM_DOMAINS]?.toMutableSet() ?: mutableSetOf()
            disabled.remove(cleanDomain)
            preferences[KEY_DISABLED_CUSTOM_DOMAINS] = disabled
        }
        return true
    }

    suspend fun removeCustomAdDomain(domain: String) {
        val clean = domain.trim().lowercase()
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_CUSTOM_AD_DOMAINS]?.toMutableSet() ?: return@edit
            current.remove(clean)
            preferences[KEY_CUSTOM_AD_DOMAINS] = current

            val disabled = preferences[KEY_DISABLED_CUSTOM_DOMAINS]?.toMutableSet() ?: return@edit
            disabled.remove(clean)
            preferences[KEY_DISABLED_CUSTOM_DOMAINS] = disabled
        }
    }

    suspend fun setCustomAdDomainEnabled(domain: String, enabled: Boolean) {
        val clean = domain.trim().lowercase()
        context.dataStore.edit { preferences ->
            val disabled = preferences[KEY_DISABLED_CUSTOM_DOMAINS]?.toMutableSet() ?: mutableSetOf()
            if (enabled) {
                disabled.remove(clean)
            } else {
                disabled.add(clean)
            }
            preferences[KEY_DISABLED_CUSTOM_DOMAINS] = disabled
        }
    }
}
