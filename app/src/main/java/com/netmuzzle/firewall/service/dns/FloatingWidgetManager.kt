package com.netmuzzle.firewall.service.dns

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Zarządza stanem tymczasowego odblokowywania reklam (np. dla reklam z nagrodami w grach)
 * oraz odliczaniem timera z automatycznym ponownym zbrojeniem blokady.
 */
object FloatingWidgetManager {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var timerJob: Job? = null
    private val lock = Any()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _pausedPackage = MutableStateFlow<String?>(null)
    val pausedPackage: StateFlow<String?> = _pausedPackage.asStateFlow()

    private val _lastActiveGamePackage = MutableStateFlow<String?>(null)
    val lastActiveGamePackage: StateFlow<String?> = _lastActiveGamePackage.asStateFlow()

    /**
     * Wstrzymuje blokadę reklam dla danej gry (lub ostatnio aktywnej) na określony czas.
     */
    fun pauseAdBlock(durationSeconds: Int = 60, targetPackage: String? = null) {
        synchronized(lock) {
            val pkg = targetPackage ?: _lastActiveGamePackage.value
            _pausedPackage.value = pkg
            _isPaused.value = true
            _remainingSeconds.value = durationSeconds

            timerJob?.cancel()
            if (durationSeconds > 0) {
                timerJob = scope.launch {
                    while (_remainingSeconds.value > 0 && _isPaused.value) {
                        delay(1000)
                        _remainingSeconds.value -= 1
                    }
                    if (_isPaused.value) {
                        resumeAdBlock()
                    }
                }
            }
        }
    }

    /**
     * Natychmiast przywraca pełne blokowanie reklam.
     */
    fun resumeAdBlock() {
        synchronized(lock) {
            _isPaused.value = false
            _remainingSeconds.value = 0
            _pausedPackage.value = null
            timerJob?.cancel()
            timerJob = null
        }
    }

    /**
     * Przełącza stan pauzy (z blokowania na odblokowanie lub odwrotnie).
     */
    fun toggle(durationSeconds: Int = 60, targetPackage: String? = null) {
        if (_isPaused.value) {
            resumeAdBlock()
        } else {
            pauseAdBlock(durationSeconds, targetPackage)
        }
    }

    /**
     * Sprawdza, czy zapytanie reklamowe z danego pakietu powinno zostać tymczasowo przepuszczone.
     */
    fun isAdBlockPausedFor(packageName: String?): Boolean {
        if (!_isPaused.value) return false
        val pausedPkg = _pausedPackage.value
        // Jeśli wskazano konkretną grę, odblokowujemy tylko dla niej; jeśli brak przypisania, odblokowujemy dla aktywnej
        return pausedPkg == null || packageName == null || pausedPkg.equals(packageName, ignoreCase = true)
    }

    /**
     * Notuje pakiet ostatnio aktywnej gry wysyłającej zapytania przez filtr.
     */
    fun registerGameQuery(packageName: String?) {
        if (!packageName.isNullOrBlank()) {
            _lastActiveGamePackage.value = packageName
        }
    }
}
