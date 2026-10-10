package com.netmuzzle.firewall.model

data class AppUpdateInfo(
    val minVersionCode: Int,
    val latestVersionCode: Int,
    val latestVersionName: String,
    val updateUrl: String,
    val releaseNotes: String,
    val releaseNotesI18n: Map<String, String> = emptyMap(),
    val forceUpdate: Boolean
) {
    fun isUpdateRequired(currentVersionCode: Int): Boolean {
        return forceUpdate && (currentVersionCode < minVersionCode)
    }

    /**
     * Zwraca opis zmian dostosowany do języka użytkownika (np. "pl", "en").
     * Jeśli dany język nie istnieje w releaseNotesI18n, szuka wersji angielskiej ("en"),
     * a na końcu sięga po ogólne pole releaseNotes.
     */
    fun getLocalizedReleaseNotes(languageCode: String): String {
        val lang = languageCode.lowercase()
        return releaseNotesI18n[lang]
            ?: releaseNotesI18n["en"]
            ?: releaseNotes
    }
}
