package com.netmuzzle.firewall.model

data class BuiltInAdNetwork(
    val id: String,
    val name: String,
    val description: String,
    val domains: List<String>
)

data class CustomAdDomain(
    val domain: String,
    val isEnabled: Boolean = true
)

object DefaultAdNetworks {
    val NETWORKS = listOf(
        BuiltInAdNetwork(
            id = "unity",
            name = "Unity Ads",
            description = "Główny dostawca reklam i wideo z nagrodami w grach 3D",
            domains = listOf(
                "unityads.unity3d.com",
                "auction.unityads.unity3d.com",
                "webview.unityads.unity3d.com",
                "config.unityads.unity3d.com"
            )
        ),
        BuiltInAdNetwork(
            id = "admob",
            name = "Google AdMob / DoubleClick",
            description = "Banery i reklamy pełnoekranowe Google",
            domains = listOf(
                "googleads.g.doubleclick.net",
                "pagead2.googlesyndication.com",
                "adservice.google.com"
            )
        ),
        BuiltInAdNetwork(
            id = "applovin",
            name = "AppLovin / MAX",
            description = "Platforma monetyzacji i wideo dla gier mobilnych",
            domains = listOf(
                "applvn.com",
                "ms.applovin.com",
                "a.applovin.com",
                "res.applovin.com"
            )
        ),
        BuiltInAdNetwork(
            id = "ironsource",
            name = "IronSource (LevelPlay)",
            description = "Popularne reklamy wideo i gry casualowe",
            domains = listOf(
                "ironsrc.mobi",
                "supersonicads-a.akamaihd.net",
                "init.supersonicads.com"
            )
        ),
        BuiltInAdNetwork(
            id = "vungle",
            name = "Vungle (Liftoff)",
            description = "Reklamy wideo HD w grach mobilnych",
            domains = listOf(
                "vungle.com",
                "ads.api.vungle.com",
                "v.vungle.com"
            )
        ),
        BuiltInAdNetwork(
            id = "mintegral",
            name = "Mintegral",
            description = "Globalna sieć reklamowa w grach",
            domains = listOf(
                "mintegral.net",
                "adx.mintegral.com",
                "setting.mintegral.net"
            )
        ),
        BuiltInAdNetwork(
            id = "inmobi",
            name = "InMobi",
            description = "Globalna platforma reklamowa w aplikacjach",
            domains = listOf(
                "inmobi.com",
                "config.inmobi.com",
                "ads.inmobi.com"
            )
        ),
        BuiltInAdNetwork(
            id = "chartboost",
            name = "Chartboost",
            description = "Promocje krzyżowe i reklamy w grach",
            domains = listOf(
                "chartboost.com",
                "live.chartboost.com",
                "da.chartboost.com"
            )
        ),
        BuiltInAdNetwork(
            id = "pangle",
            name = "Pangle",
            description = "Sieć reklamowa wideo dla gier",
            domains = listOf(
                "pangle-ads.com",
                "pangolin-sdk-toutiao.com"
            )
        )
    )

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
