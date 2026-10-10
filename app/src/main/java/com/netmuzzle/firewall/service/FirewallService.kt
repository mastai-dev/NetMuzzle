package com.netmuzzle.firewall.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.netmuzzle.firewall.NetMuzzleApp
import com.netmuzzle.firewall.R
import com.netmuzzle.firewall.data.FirewallPreferences
import com.netmuzzle.firewall.model.VpnStatus
import com.netmuzzle.firewall.service.dns.DnsPacketHandler
import com.netmuzzle.firewall.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FirewallService : VpnService() {

    companion object {
        private const val TAG = "FirewallService"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.netmuzzle.firewall.ACTION_START"
        const val ACTION_STOP = "com.netmuzzle.firewall.ACTION_STOP"
        const val ACTION_RELOAD = "com.netmuzzle.firewall.ACTION_RELOAD"

        private val _vpnStatus = MutableStateFlow(VpnStatus.DISABLED)
        val vpnStatus: StateFlow<VpnStatus> = _vpnStatus.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, FirewallService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FirewallService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun reloadRules(context: Context) {
            val intent = Intent(context, FirewallService::class.java).apply {
                action = ACTION_RELOAD
            }
            context.startService(intent)
        }
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var dnsHandler: DnsPacketHandler? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val rulesMutex = Mutex()
    private lateinit var preferences: FirewallPreferences

    override fun onCreate() {
        super.onCreate()
        preferences = FirewallPreferences(this)
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        if (networkCallback == null) {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    dnsHandler?.clearCache()
                }
                override fun onLost(network: Network) {
                    dnsHandler?.clearCache()
                }
            }
            networkCallback = callback
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    connectivityManager.registerDefaultNetworkCallback(callback)
                } else {
                    val request = NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build()
                    connectivityManager.registerNetworkCallback(request, callback)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Nie udało się zarejestrować NetworkCallback", e)
            }
        }
    }

    private fun unregisterNetworkCallback() {
        networkCallback?.let {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            try {
                connectivityManager?.unregisterNetworkCallback(it)
            } catch (_: Exception) {}
            networkCallback = null
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_STOP -> {
                shutdownFirewall()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, ACTION_RELOAD -> {
                val isCurrentlyActive = _vpnStatus.value == VpnStatus.ACTIVE
                if (!isCurrentlyActive) {
                    startForeground(NOTIFICATION_ID, buildNotification(0, isStandby = true))
                }

                serviceScope.launch {
                    rulesMutex.withLock {
                        val snapshot = preferences.getRulesSnapshotSync()
                        if (!snapshot.isMasterEnabled) {
                            shutdownFirewall()
                            stopSelf()
                            return@withLock
                        }
                        applyFirewallRules(
                            snapshot.fullBlockedPackages,
                            snapshot.adBlockedPackages,
                            snapshot.activeBlockedDomains
                        )
                    }
                }
            }
        }

        return START_STICKY
    }

    private suspend fun applyFirewallRules(
        fullBlockedPackages: Set<String>,
        adBlockedPackages: Set<String>,
        activeBlockedDomains: Set<String>
    ) {
        val totalPackages = fullBlockedPackages + adBlockedPackages
        if (totalPackages.isEmpty()) {
            closeTunnel()
            _vpnStatus.value = VpnStatus.STANDBY
            updateNotification(blockedCount = 0, isStandby = true)
            Log.d(TAG, "Firewall w trybie czuwania - brak aplikacji na liście.")
            return
        }

        try {
            val builder = Builder()
                .setSession(getString(R.string.app_name))
                .setMtu(1500)

            val isAdBlockActive = adBlockedPackages.isNotEmpty()

            if (!isAdBlockActive) {
                // TRYB 1: Czysty Blackhole (0% CPU, 0% baterii, brak pętli odczytu)
                builder.addAddress("10.0.0.2", 32)
                builder.addRoute("0.0.0.0", 0)
                builder.addDnsServer("10.0.0.1")

                builder.addAddress("fd00::1", 128)
                builder.addRoute("::", 0)
                builder.addDnsServer("fd00::2")
            } else {
                // TRYB 2: DNS-Shield (Tylko ruch DNS w tunelu, ruch gier leci bezpośrednio)
                builder.addAddress("10.0.0.2", 32)
                builder.addDnsServer("10.0.0.1")
                builder.addRoute("10.0.0.1", 32)

                // Trasy dla popularnych serwerów DNS (zapobiega omijaniu DNS przez biblioteki reklamowe i aplikacje z kagańcem)
                val dnsIpv4Routes = listOf(
                    "8.8.8.8", "8.8.4.4",                 // Google DNS
                    "1.1.1.1", "1.0.0.1",                 // Cloudflare DNS
                    "1.1.1.2", "1.0.0.2",                 // Cloudflare Malware
                    "1.1.1.3", "1.0.0.3",                 // Cloudflare Family
                    "9.9.9.9", "149.112.112.112",         // Quad9
                    "9.9.9.10", "149.112.112.10",         // Quad9 Unsecured
                    "208.67.222.222", "208.67.220.220",   // OpenDNS
                    "208.67.222.123", "208.67.220.123",   // OpenDNS FamilyShield
                    "94.140.14.14", "94.140.15.15",       // AdGuard DNS
                    "94.140.14.140", "94.140.14.141",     // AdGuard Family
                    "76.76.2.0", "76.76.10.0",             // ControlD
                    "185.228.168.9", "185.228.169.9",     // CleanBrowsing
                    "8.26.56.26", "8.20.247.20"           // Comodo Secure
                )
                for (ip in dnsIpv4Routes) {
                    builder.addRoute(ip, 32)
                }

                builder.addAddress("fd00::2", 128)
                builder.addDnsServer("fd00::1")
                builder.addRoute("fd00::1", 128)

                val dnsIpv6Routes = listOf(
                    "2001:4860:4860::8888", "2001:4860:4860::8844", // Google DNS
                    "2606:4700:4700::1111", "2606:4700:4700::1001", // Cloudflare DNS
                    "2620:fe::fe", "2620:fe::9",                     // Quad9
                    "2620:119:35::35", "2620:119:53::53",           // OpenDNS
                    "2a10:50c0::ad1:ff", "2a10:50c0::ad2:ff"         // AdGuard DNS
                )
                for (ip6 in dnsIpv6Routes) {
                    builder.addRoute(ip6, 128)
                }
            }

            var validAppCount = 0
            for (pkg in totalPackages) {
                try {
                    builder.addAllowedApplication(pkg)
                    validAppCount++
                } catch (e: PackageManager.NameNotFoundException) {
                    Log.w(TAG, "Pominięto odinstalowaną aplikację: $pkg")
                } catch (e: Exception) {
                    Log.e(TAG, "Błąd dodawania aplikacji $pkg", e)
                }
            }

            if (validAppCount == 0) {
                closeTunnel()
                _vpnStatus.value = VpnStatus.STANDBY
                updateNotification(blockedCount = 0, isStandby = true)
                return
            }

            val oldInterface = vpnInterface
            val newInterface = builder.establish()

            if (newInterface != null) {
                // Zatrzymujemy poprzedni wątek DNS
                dnsHandler?.stop()
                dnsHandler = null

                vpnInterface = newInterface
                oldInterface?.close()
                _vpnStatus.value = VpnStatus.ACTIVE
                updateNotification(blockedCount = validAppCount, isStandby = false)

                if (isAdBlockActive) {
                    val handler = DnsPacketHandler(
                        vpnService = this,
                        context = this,
                        vpnInterfaceFd = newInterface.fileDescriptor,
                        blockedDomains = activeBlockedDomains,
                        fullBlockedPackages = fullBlockedPackages
                    )
                    dnsHandler = handler
                    serviceScope.launch(Dispatchers.IO) {
                        handler.runLoop()
                    }
                    Log.d(TAG, "Uruchomiono lekki filtr DNS (Game Shield) dla $validAppCount aplikacji.")
                } else {
                    Log.d(TAG, "Uruchomiono czarną dziurę (Blackhole) dla $validAppCount aplikacji.")
                }

                if (preferences.isFloatingWidgetEnabledSync()) {
                    FloatingWidgetService.start(this@FirewallService)
                }
            } else {
                Log.e(TAG, "Nie udało się utworzyć interfejsu VPN (establish zwrócił null)")
                closeTunnel()
                _vpnStatus.value = VpnStatus.STANDBY
                updateNotification(blockedCount = 0, isStandby = true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Krytyczny błąd podczas konfiguracji VPN", e)
            closeTunnel()
            _vpnStatus.value = VpnStatus.STANDBY
            updateNotification(blockedCount = 0, isStandby = true)
        }
    }

    private fun closeTunnel() {
        try {
            dnsHandler?.stop()
            dnsHandler = null
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Błąd zamykania tunelu", e)
        } finally {
            vpnInterface = null
        }
    }

    private fun shutdownFirewall() {
        closeTunnel()
        FloatingWidgetService.stop(this)
        com.netmuzzle.firewall.service.dns.FloatingWidgetManager.resumeAdBlock()
        _vpnStatus.value = VpnStatus.DISABLED
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun updateNotification(blockedCount: Int, isStandby: Boolean) {
        val notification = buildNotification(blockedCount, isStandby)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(blockedCount: Int, isStandby: Boolean): Notification {
        val mainActivityIntent = Intent(this, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FirewallService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isStandby) {
            getString(R.string.notification_standby_title)
        } else {
            getString(R.string.notification_active_title)
        }

        val text = if (isStandby) {
            getString(R.string.notification_standby_text)
        } else {
            getString(R.string.notification_active_text, blockedCount)
        }

        return NotificationCompat.Builder(this, NetMuzzleApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.notification_action_stop),
                stopPendingIntent
            )
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterNetworkCallback()
        serviceScope.cancel()
        shutdownFirewall()
    }

    override fun onRevoke() {
        super.onRevoke()
        shutdownFirewall()
        stopSelf()
    }
}
