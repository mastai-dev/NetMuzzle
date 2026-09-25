package com.lightvpn.firewall.model

enum class VpnStatus {
    DISABLED, // Firewall jest wyłączony
    STANDBY,  // Włączony, ale 0 aplikacji na liście (brak tunelu TUN, czuwanie)
    ACTIVE    // Włączony i blokuje przynajmniej 1 aplikację
}

data class FirewallUiState(
    val status: VpnStatus = VpnStatus.DISABLED,
    val isMasterEnabled: Boolean = false,
    val startOnBoot: Boolean = false,
    val showSystemApps: Boolean = false,
    val searchQuery: String = "",
    val filterBlockedOnly: Boolean = false,
    val apps: List<AppInfo> = emptyList(),
    val isLoading: Boolean = true,
    val blockedCount: Int = 0
)
