package com.netmuzzle.firewall.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.netmuzzle.firewall.data.AppListRepository
import com.netmuzzle.firewall.data.FirewallPreferences
import com.netmuzzle.firewall.model.AppInfo
import com.netmuzzle.firewall.model.BlockMode
import com.netmuzzle.firewall.model.FirewallUiState
import com.netmuzzle.firewall.model.VpnStatus
import com.netmuzzle.firewall.service.FirewallService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FirewallViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppListRepository(application)
    private val preferences = FirewallPreferences(application)

    private val _searchQuery = MutableStateFlow("")
    private val _filterBlockedOnly = MutableStateFlow(false)
    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _isLoading = MutableStateFlow(true)

    init {
        loadInstalledApps()
    }

    private data class AppRules(
        val fullBlocked: Set<String>,
        val adBlocked: Set<String>,
        val disabledAdNets: Set<String>,
        val customDomains: Set<String>,
        val disabledCustom: Set<String>
    )

    private val appRulesFlow = combine(
        preferences.blockedPackages,
        preferences.adBlockPackages,
        preferences.disabledAdNetworks,
        preferences.customAdDomains,
        preferences.disabledCustomDomains
    ) { fullBlocked, adBlocked, disabledNets, customDoms, disabledCustom ->
        AppRules(fullBlocked, adBlocked, disabledNets, customDoms, disabledCustom)
    }

    val uiState: StateFlow<FirewallUiState> = combine(
        FirewallService.vpnStatus,
        preferences.isFirewallEnabled,
        preferences.startOnBoot,
        preferences.showSystemApps,
        appRulesFlow,
        _searchQuery,
        _filterBlockedOnly,
        _installedApps,
        _isLoading
    ) { args: Array<Any> ->
        val vpnStatus = args[0] as VpnStatus
        val isMasterEnabled = args[1] as Boolean
        val startOnBoot = args[2] as Boolean
        val showSystemApps = args[3] as Boolean
        val rules = args[4] as AppRules
        val searchQuery = args[5] as String
        val filterBlockedOnly = args[6] as Boolean
        val rawApps = args[7] as List<AppInfo>
        val isLoading = args[8] as Boolean

        // Aktualizacja stanu zablokowania dla poszczególnych aplikacji
        val updatedApps = rawApps.map { app ->
            val mode = when {
                rules.fullBlocked.contains(app.packageName) -> BlockMode.FULL_BLOCK
                rules.adBlocked.contains(app.packageName) -> BlockMode.AD_BLOCK
                else -> BlockMode.ALLOW
            }
            app.copy(blockMode = mode)
        }

        // Filtrowanie według wyszukiwania, typu aplikacji i zablokowania
        val filteredApps = updatedApps.filter { app ->
            val systemCondition = showSystemApps || !app.isSystemApp
            val blockedCondition = !filterBlockedOnly || app.blockMode != BlockMode.ALLOW
            val searchCondition = searchQuery.isBlank() ||
                    app.name.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)

            systemCondition && blockedCondition && searchCondition
        }

        val effectiveStatus = if (!isMasterEnabled) {
            VpnStatus.DISABLED
        } else {
            vpnStatus
        }

        FirewallUiState(
            status = effectiveStatus,
            isMasterEnabled = isMasterEnabled,
            startOnBoot = startOnBoot,
            showSystemApps = showSystemApps,
            searchQuery = searchQuery,
            filterBlockedOnly = filterBlockedOnly,
            apps = filteredApps,
            isLoading = isLoading,
            fullBlockedCount = rules.fullBlocked.size,
            adBlockedCount = rules.adBlocked.size,
            disabledAdNetworks = rules.disabledAdNets,
            customAdDomains = rules.customDomains,
            disabledCustomDomains = rules.disabledCustom
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FirewallUiState()
    )

    fun loadInstalledApps(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _installedApps.value = repository.getInstalledApps(forceRefresh)
            _isLoading.value = false
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterBlockedToggled(filterBlocked: Boolean) {
        _filterBlockedOnly.value = filterBlocked
    }

    fun onShowSystemAppsToggled(show: Boolean) {
        viewModelScope.launch {
            preferences.setShowSystemApps(show)
        }
    }

    fun onStartOnBootToggled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setStartOnBoot(enabled)
        }
    }

    fun onAppBlockModeChanged(packageName: String, mode: BlockMode, context: Context) {
        viewModelScope.launch {
            preferences.setPackageBlockMode(packageName, mode)
            if (uiState.value.isMasterEnabled) {
                FirewallService.reloadRules(context)
            }
        }
    }

    fun onAdNetworkToggled(networkId: String, enabled: Boolean, context: Context) {
        viewModelScope.launch {
            preferences.setAdNetworkEnabled(networkId, enabled)
            if (uiState.value.isMasterEnabled) {
                FirewallService.reloadRules(context)
            }
        }
    }

    fun onAddCustomDomain(domain: String, context: Context) {
        viewModelScope.launch {
            if (preferences.addCustomAdDomain(domain)) {
                if (uiState.value.isMasterEnabled) {
                    FirewallService.reloadRules(context)
                }
            }
        }
    }

    fun onRemoveCustomDomain(domain: String, context: Context) {
        viewModelScope.launch {
            preferences.removeCustomAdDomain(domain)
            if (uiState.value.isMasterEnabled) {
                FirewallService.reloadRules(context)
            }
        }
    }

    fun onToggleCustomDomain(domain: String, enabled: Boolean, context: Context) {
        viewModelScope.launch {
            preferences.setCustomAdDomainEnabled(domain, enabled)
            if (uiState.value.isMasterEnabled) {
                FirewallService.reloadRules(context)
            }
        }
    }

    fun onMasterSwitchToggled(
        enabled: Boolean,
        context: Context,
        onRequireVpnPermission: (Intent) -> Unit
    ) {
        viewModelScope.launch {
            if (enabled) {
                val prepareIntent = VpnService.prepare(context)
                if (prepareIntent != null) {
                    onRequireVpnPermission(prepareIntent)
                } else {
                    preferences.setFirewallEnabled(true)
                    FirewallService.startService(context)
                }
            } else {
                preferences.setFirewallEnabled(false)
                FirewallService.stopService(context)
            }
        }
    }

    fun onVpnPermissionGranted(context: Context) {
        viewModelScope.launch {
            preferences.setFirewallEnabled(true)
            FirewallService.startService(context)
        }
    }
}
