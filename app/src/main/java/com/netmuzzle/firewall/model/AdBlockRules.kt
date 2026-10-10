package com.netmuzzle.firewall.model

import androidx.annotation.StringRes
import com.netmuzzle.firewall.R

data class BuiltInAdNetwork(
    val id: String,
    val name: String,
    @StringRes val descriptionRes: Int,
    val domains: List<String>
)

data class CustomAdDomain(
    val domain: String,
    val isEnabled: Boolean = true
)

object DefaultAdNetworks {
    val NETWORKS: List<BuiltInAdNetwork> get() = DefaultNetworksConfig.NETWORKS

    /**
     * Zwraca zbiór wszystkich aktywnych domen na podstawie wykluczonych sieci
     * oraz niestandardowych domen użytkownika.
     */
    fun computeActiveBlockedDomains(
        disabledNetworkIds: Set<String>,
        customDomains: Set<String>,
        disabledCustomDomains: Set<String>
    ): Set<String> {
        val result = mutableSetOf<String>()
        for (network in NETWORKS) {
            if (!disabledNetworkIds.contains(network.id)) {
                result.addAll(network.domains)
            }
        }
        for (custom in customDomains) {
            val normalized = custom.trim().lowercase()
            if (normalized.isNotBlank() && !disabledCustomDomains.contains(normalized)) {
                result.add(normalized)
            }
        }
        return result
    }
}
