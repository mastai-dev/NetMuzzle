package com.netmuzzle.firewall.data

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import com.netmuzzle.firewall.model.AppUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class UpdateCheckerRepository(private val context: Context) {

    companion object {
        const val DEFAULT_VERSION_URL =
            "https://raw.githubusercontent.com/mastai-dev/NetMuzzle/main/version.json"
        private const val CONNECT_TIMEOUT_MS = 6000
        private const val READ_TIMEOUT_MS = 6000
    }

    val currentVersionCode: Int by lazy {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            PackageInfoCompat.getLongVersionCode(packageInfo).toInt()
        } catch (e: Exception) {
            1
        }
    }

    val currentVersionName: String by lazy {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    suspend fun checkUpdate(url: String = DEFAULT_VERSION_URL): Result<AppUpdateInfo?> =
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val requestUrl = URL(url)
                connection = (requestUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    useCaches = false
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "NetMuzzle-App/$currentVersionName")
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val jsonStr = reader.use { it.readText() }
                    val json = JSONObject(jsonStr)

                    val notesI18n = mutableMapOf<String, String>()
                    val i18nObj = json.optJSONObject("release_notes_i18n")
                    if (i18nObj != null) {
                        val keys = i18nObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            notesI18n[key.lowercase()] = i18nObj.optString(key, "")
                        }
                    }

                    val rawUpdateUrl = json.optString("update_url", "").trim()
                    val safeUpdateUrl = if (rawUpdateUrl.startsWith("https://", ignoreCase = true)) {
                        rawUpdateUrl
                    } else {
                        "https://github.com/mastai-dev/NetMuzzle/releases/latest"
                    }

                    val info = AppUpdateInfo(
                        minVersionCode = json.optInt("min_version_code", 1),
                        latestVersionCode = json.optInt("latest_version_code", 1),
                        latestVersionName = json.optString("latest_version_name", ""),
                        updateUrl = safeUpdateUrl,
                        releaseNotes = json.optString("release_notes", ""),
                        releaseNotesI18n = notesI18n,
                        forceUpdate = json.optBoolean("force_update", false)
                    )
                    Result.success(info)
                } else {
                    Result.failure(Exception("HTTP error code: $responseCode"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            } finally {
                connection?.disconnect()
            }
        }
}
