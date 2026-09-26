package com.netmuzzle.firewall.model

enum class VpnStatus {
    DISABLED, // Firewall jest wyłączony
    STANDBY,  // Włączony, ale 0 aplikacji na liście (brak tunelu TUN, czuwanie)
    ACTIVE    // Włączony i chroni/blokuje przynajmniej 1 aplikację
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
    val fullBlockedCount: Int = 0,
    val adBlockedCount: Int = 0,
    val disabledAdNetworks: Set<String> = emptySet(),
    val customAdDomains: Set<String> = emptySet(),
    val disabledCustomDomains: Set<String> = emptySet()
) {
    val totalProtectedCount: Int get() = fullBlockedCount + adBlockedCount
    val blockedCount: Int get() = totalProtectedCount
}
