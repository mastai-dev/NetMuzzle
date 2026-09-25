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

    val uiState: StateFlow<FirewallUiState> = combine(
        FirewallService.vpnStatus,
        preferences.isFirewallEnabled,
        preferences.startOnBoot,
        preferences.showSystemApps,
        preferences.blockedPackages,
        _searchQuery,
        _filterBlockedOnly,
        _installedApps,
        _isLoading
    ) { params ->
        val vpnStatus = params[0] as VpnStatus
        val isMasterEnabled = params[1] as Boolean
        val startOnBoot = params[2] as Boolean
        val showSystemApps = params[3] as Boolean
        val blockedPackages = params[4] as Set<String>
        val searchQuery = params[5] as String
        val filterBlockedOnly = params[6] as Boolean
        val rawApps = params[7] as List<AppInfo>
        val isLoading = params[8] as Boolean

        // Aktualizacja stanu zablokowania dla poszczególnych aplikacji
        val updatedApps = rawApps.map { app ->
            app.copy(isBlocked = blockedPackages.contains(app.packageName))
        }

        // Filtrowanie według wyszukiwania, typu aplikacji i zablokowania
        val filteredApps = updatedApps.filter { app ->
            // Filtr aplikacji systemowych
            val systemCondition = showSystemApps || !app.isSystemApp
            // Filtr tylko zablokowanych
            val blockedCondition = !filterBlockedOnly || app.isBlocked
            // Filtr wyszukiwania
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
            blockedCount = blockedPackages.size
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

    fun onAppBlockToggled(packageName: String, isBlocked: Boolean, context: Context) {
        viewModelScope.launch {
            preferences.setPackageBlocked(packageName, isBlocked)
            // Jeśli firewall jest aktualnie uruchomiony, odświeżamy reguły bezszwowo
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
