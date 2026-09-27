package com.netmuzzle.firewall.service.dns

import com.netmuzzle.firewall.model.CapturedDomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object TrafficInspectorManager {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var timerJob: Job? = null

    private val _isSniffing = MutableStateFlow(false)
    val isSniffing: StateFlow<Boolean> = _isSniffing.asStateFlow()

    private val _targetPackageName = MutableStateFlow<String?>(null)
    val targetPackageName: StateFlow<String?> = _targetPackageName.asStateFlow()

    private val _capturedDomains = MutableStateFlow<List<CapturedDomain>>(emptyList())
    val capturedDomains: StateFlow<List<CapturedDomain>> = _capturedDomains.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    // Słowa kluczowe wskazujące z dużym prawdopodobieństwem na sieć reklamową lub tracker
    private val SUSPICIOUS_KEYWORDS = setOf(
        "ad", "ads", "adservice", "adserver", "advert", "advertising",
        "banner", "popup", "popunder", "interstitial", "rewarded",
        "track", "tracker", "tracking", "telemetry", "analytics", "metrics",
        "pixel", "beacon", "stat", "stats", "monetization", "monetize",
        "bid", "bidding", "campaign", "adcolony", "applovin", "unity3d",
        "ironsrc", "vungle", "inmobi", "chartboost", "pangle", "mintegral",
        "adjust", "appsflyer", "singular", "kochava", "doubleclick"
    )

    fun startSniffing(packageName: String, durationMinutes: Int = 30) {
        _targetPackageName.value = packageName
        _isSniffing.value = true
        _remainingSeconds.value = durationMinutes * 60

        timerJob?.cancel()
        timerJob = scope.launch {
            while (_remainingSeconds.value > 0 && _isSniffing.value) {
                delay(1000)
                _remainingSeconds.value -= 1
            }
            if (_isSniffing.value) {
                stopSniffing()
            }
        }
    }

    fun stopSniffing() {
        _isSniffing.value = false
        timerJob?.cancel()
        timerJob = null
    }

    fun clearDomains() {
        _capturedDomains.value = emptyList()
    }

    fun onDomainQueried(domain: String, senderPackage: String?, isBlocked: Boolean) {
        if (!_isSniffing.value) return

        val target = _targetPackageName.value ?: return
        // Jeśli system potrafił ustalić pakiet i różni się on od badanego celu, ignorujemy
        if (senderPackage != null && senderPackage != target) return

        val cleanDomain = domain.trim().lowercase()
        if (cleanDomain.isBlank()) return

        val isSuspicious = checkIsSuspicious(cleanDomain)

        val currentList = _capturedDomains.value
        val existingIndex = currentList.indexOfFirst { it.domain.equals(cleanDomain, ignoreCase = true) }

        if (existingIndex != -1) {
            // Przenosimy na samą górę z nowym timestampem (Punkt A: chronologia / najnowsze u góry)
            val existing = currentList[existingIndex]
            val updated = existing.copy(
                timestamp = System.currentTimeMillis(),
                queryCount = existing.queryCount + 1,
                isBlocked = isBlocked,
                isSuspicious = isSuspicious || existing.isSuspicious
            )
            val newList = currentList.toMutableList()
            newList.removeAt(existingIndex)
            newList.add(0, updated)
            _capturedDomains.value = newList
        } else {
            val newEntry = CapturedDomain(
                domain = cleanDomain,
                packageName = target,
                timestamp = System.currentTimeMillis(),
                isBlocked = isBlocked,
                isSuspicious = isSuspicious,
                queryCount = 1
            )
            _capturedDomains.value = listOf(newEntry) + currentList
        }
    }

    private fun checkIsSuspicious(domain: String): Boolean {
        val parts = domain.split('.', '-', '_')
        return parts.any { part -> SUSPICIOUS_KEYWORDS.contains(part) }
    }
}
