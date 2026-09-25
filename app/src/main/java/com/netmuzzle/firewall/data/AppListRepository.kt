package com.netmuzzle.firewall.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.netmuzzle.firewall.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppListRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private var cachedApps: List<AppInfo>? = null

    suspend fun getInstalledApps(forceRefresh: Boolean = false): List<AppInfo> = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedApps != null) {
            return@withContext cachedApps!!
        }

        val myPackageName = context.packageName
        val packages: List<PackageInfo> = try {
            packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        } catch (e: Exception) {
            emptyList()
        }

        val appList = mutableListOf<AppInfo>()

        for (pkgInfo in packages) {
            val appInfo = pkgInfo.applicationInfo ?: continue
            val pkgName = pkgInfo.packageName

            // Nie pokazujemy samej aplikacji NetMuzzle na liście
            if (pkgName == myPackageName) continue

            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val hasInternet = pkgInfo.requestedPermissions?.contains(android.Manifest.permission.INTERNET) == true

            // Pomijamy aplikacje systemowe, które w ogóle nie mają uprawnień do internetu
            if (isSystem && !hasInternet) continue

            val label = try {
                appInfo.loadLabel(packageManager).toString()
            } catch (e: Exception) {
                pkgName
            }

            val icon = try {
                appInfo.loadIcon(packageManager)
            } catch (e: Exception) {
                null
            }

            appList.add(
                AppInfo(
                    name = label,
                    packageName = pkgName,
                    icon = icon,
                    isBlocked = false, // Stan zostanie uzupełniony przez ViewModel
                    isSystemApp = isSystem,
                    hasInternetPermission = hasInternet
                )
            )
        }

        // Sortowanie alfabetyczne po nazwie
        val sortedList = appList.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        cachedApps = sortedList
        sortedList
    }

    fun clearCache() {
        cachedApps = null
    }
}
