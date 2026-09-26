package com.netmuzzle.firewall.model

import android.graphics.drawable.Drawable

enum class BlockMode {
    ALLOW,       // Otwarty / Bypass - bez VPN, pełen internet
    AD_BLOCK,    // Tylko Ads - filtrowanie domen reklamowych, ruch gry bezpośredni
    FULL_BLOCK   // Kaganiec - całkowite odcięcie od internetu (Blackhole)
}

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable? = null,
    val blockMode: BlockMode = BlockMode.ALLOW,
    val isSystemApp: Boolean = false,
    val hasInternetPermission: Boolean = true,
    val isGame: Boolean = false
) {
    val isBlocked: Boolean get() = blockMode == BlockMode.FULL_BLOCK
    val isAdBlocked: Boolean get() = blockMode == BlockMode.AD_BLOCK
}
