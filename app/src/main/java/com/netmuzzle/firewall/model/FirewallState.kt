package com.netmuzzle.firewall.model

enum class VpnStatus {
    DISABLED, // Firewall jest wyłączony
    STANDBY,  // Włączony, ale 0 aplikacji na liście (brak tunelu TUN, czuwanie)
    ACTIVE    // Włączony i chroni/blokuje przynajmniej 1 aplikację
}

enum class AppFilter {
    ALL,        // Wszystkie aplikacje
    GAMES,      // Wykryte gry
    AD_BLOCK,   // Aplikacje z blokadą reklam
    FULL_BLOCK  // Aplikacje z całkowitym kagańcem
}

data class FirewallUiState(
    val status: VpnStatus = VpnStatus.DISABLED,
    val isMasterEnabled: Boolean = false,
    val startOnBoot: Boolean = false,
    val showSystemApps: Boolean = false,
    val searchQuery: String = "",
    val selectedFilter: AppFilter = AppFilter.ALL,
    val apps: List<AppInfo> = emptyList(),
    val isLoading: Boolean = true,
    val totalAppsCount: Int = 0,
    val gamesCount: Int = 0,
    val fullBlockedCount: Int = 0,
    val adBlockedCount: Int = 0,
    val disabledAdNetworks: Set<String> = emptySet(),
    val customAdDomains: Set<String> = emptySet(),
    val disabledCustomDomains: Set<String> = emptySet()
) {
    val totalProtectedCount: Int get() = fullBlockedCount + adBlockedCount
    val blockedCount: Int get() = totalProtectedCount
    val filterBlockedOnly: Boolean get() = selectedFilter != AppFilter.ALL
}
