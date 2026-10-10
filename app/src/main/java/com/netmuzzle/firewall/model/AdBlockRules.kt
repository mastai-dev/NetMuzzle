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
    val NETWORKS = listOf(
        BuiltInAdNetwork(
            id = "unity",
            name = "Unity Ads",
            descriptionRes = R.string.ad_net_unity_desc,
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
            descriptionRes = R.string.ad_net_admob_desc,
            domains = listOf(
                "googleads.g.doubleclick.net",
                "pagead2.googlesyndication.com",
                "adservice.google.com"
            )
        ),
        BuiltInAdNetwork(
            id = "applovin",
            name = "AppLovin / MAX",
            descriptionRes = R.string.ad_net_applovin_desc,
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
            descriptionRes = R.string.ad_net_ironsource_desc,
            domains = listOf(
                "ironsrc.mobi",
                "supersonicads-a.akamaihd.net",
                "init.supersonicads.com"
            )
        ),
        BuiltInAdNetwork(
            id = "vungle",
            name = "Vungle (Liftoff)",
            descriptionRes = R.string.ad_net_vungle_desc,
            domains = listOf(
                "vungle.com",
                "ads.api.vungle.com",
                "v.vungle.com"
            )
        ),
        BuiltInAdNetwork(
            id = "mintegral",
            name = "Mintegral",
            descriptionRes = R.string.ad_net_mintegral_desc,
            domains = listOf(
                "mintegral.net",
                "adx.mintegral.com",
                "setting.mintegral.net"
            )
        ),
        BuiltInAdNetwork(
            id = "inmobi",
            name = "InMobi",
            descriptionRes = R.string.ad_net_inmobi_desc,
            domains = listOf(
                "inmobi.com",
                "config.inmobi.com",
                "ads.inmobi.com"
            )
        ),
        BuiltInAdNetwork(
            id = "chartboost",
            name = "Chartboost",
            descriptionRes = R.string.ad_net_chartboost_desc,
            domains = listOf(
                "chartboost.com",
                "live.chartboost.com",
                "da.chartboost.com"
            )
        ),
        BuiltInAdNetwork(
            id = "pangle",
            name = "Pangle",
            descriptionRes = R.string.ad_net_pangle_desc,
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
