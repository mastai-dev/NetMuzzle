package com.netmuzzle.firewall.model

import com.netmuzzle.firewall.R

object DefaultNetworksConfig {
    val NETWORKS = listOf(
        BuiltInAdNetwork(
            id = "unity",
            name = "Unity Telemetry & Analytics",
            descriptionRes = R.string.ad_net_unity_desc,
            domains = listOf(
                "unityads.unity3d.com",
                "auction.unityads.unity3d.com",
                "webview.unityads.unity3d.com",
                "config.unityads.unity3d.com"
            )
        ),
        BuiltInAdNetwork(
            id = "applovin",
            name = "AppLovin Telemetry & MAX",
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
            name = "IronSource Telemetry",
            descriptionRes = R.string.ad_net_ironsource_desc,
            domains = listOf(
                "ironsrc.mobi",
                "supersonicads-a.akamaihd.net",
                "init.supersonicads.com"
            )
        ),
        BuiltInAdNetwork(
            id = "vungle",
            name = "Vungle Telemetry",
            descriptionRes = R.string.ad_net_vungle_desc,
            domains = listOf(
                "vungle.com",
                "ads.api.vungle.com",
                "v.vungle.com"
            )
        ),
        BuiltInAdNetwork(
            id = "mintegral",
            name = "Mintegral SDK Telemetry",
            descriptionRes = R.string.ad_net_mintegral_desc,
            domains = listOf(
                "mintegral.net",
                "adx.mintegral.com",
                "setting.mintegral.net"
            )
        ),
        BuiltInAdNetwork(
            id = "inmobi",
            name = "InMobi Tracking",
            descriptionRes = R.string.ad_net_inmobi_desc,
            domains = listOf(
                "inmobi.com",
                "config.inmobi.com",
                "ads.inmobi.com"
            )
        ),
        BuiltInAdNetwork(
            id = "chartboost",
            name = "Chartboost Telemetry",
            descriptionRes = R.string.ad_net_chartboost_desc,
            domains = listOf(
                "chartboost.com",
                "live.chartboost.com",
                "da.chartboost.com"
            )
        ),
        BuiltInAdNetwork(
            id = "pangle",
            name = "Pangle Media Analytics",
            descriptionRes = R.string.ad_net_pangle_desc,
            domains = listOf(
                "pangle-ads.com",
                "pangolin-sdk-toutiao.com"
            )
        )
    )
}
