package com.netmuzzle.firewall.model

data class CapturedDomain(
    val domain: String,
    val packageName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isBlocked: Boolean = false,
    val isSuspicious: Boolean = false,
    val queryCount: Int = 1
)
