package com.netmuzzle.firewall.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable? = null,
    val isBlocked: Boolean = false,
    val isSystemApp: Boolean = false,
    val hasInternetPermission: Boolean = true
)
