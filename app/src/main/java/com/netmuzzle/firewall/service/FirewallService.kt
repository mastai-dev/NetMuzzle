package com.netmuzzle.firewall.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.netmuzzle.firewall.NetMuzzleApp
import com.netmuzzle.firewall.R
import com.netmuzzle.firewall.data.FirewallPreferences
import com.netmuzzle.firewall.model.VpnStatus
import com.netmuzzle.firewall.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var preferences: FirewallPreferences

    override fun onCreate() {
        super.onCreate()
        preferences = FirewallPreferences(this)
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
                // Natychmiast startujemy Foreground Service z domyślnym powiadomieniem
                startForeground(NOTIFICATION_ID, buildNotification(0, isStandby = true))
                
                serviceScope.launch {
                    val isMasterEnabled = preferences.isFirewallEnabledSync()
                    if (!isMasterEnabled) {
                        shutdownFirewall()
                        stopSelf()
                        return@launch
                    }
                    val blockedPackages = preferences.getBlockedPackagesSync()
                    applyFirewallRules(blockedPackages)
                }
            }
        }

        return START_STICKY
    }

    private fun applyFirewallRules(blockedPackages: Set<String>) {
        if (blockedPackages.isEmpty()) {
            // STAN CZUWANIA (Standby):
            // Brak zablokowanych aplikacji -> nie tworzymy tunelu TUN,
            // aby nie odciąć ruchu całego telefonu.
            closeTunnel()
            _vpnStatus.value = VpnStatus.STANDBY
            updateNotification(blockedCount = 0, isStandby = true)
            Log.d(TAG, "Firewall w trybie czuwania - brak aplikacji na czarnej liście.")
            return
        }

        try {
            val builder = Builder()
                .setSession(getString(R.string.app_name))
                .setMtu(1500)
                // Konfiguracja IPv4:
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("10.0.0.1")
                // Konfiguracja IPv6 (wymagana dla szczelności i uniknięcia IllegalArgumentException):
                .addAddress("fd00::1", 128)
                .addRoute("::", 0)
                .addDnsServer("fd00::2")

            var validAppCount = 0
            for (pkg in blockedPackages) {
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

            // Bezszwowa podmiana tunelu (Seamless handover)
            val oldInterface = vpnInterface
            val newInterface = builder.establish()

            if (newInterface != null) {
                vpnInterface = newInterface
                oldInterface?.close()
                _vpnStatus.value = VpnStatus.ACTIVE
                updateNotification(blockedCount = validAppCount, isStandby = false)
                Log.d(TAG, "Tunel TUN ustanowiony pomyślnie dla $validAppCount aplikacji.")
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
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Błąd zamykania tunelu", e)
        } finally {
            vpnInterface = null
        }
    }

    private fun shutdownFirewall() {
        closeTunnel()
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
        serviceScope.cancel()
        shutdownFirewall()
    }

    override fun onRevoke() {
        // Wywoływane przez system, jeśli użytkownik lub inna aplikacja VPN rozłączyła sesję
        super.onRevoke()
        shutdownFirewall()
        stopSelf()
    }
}
