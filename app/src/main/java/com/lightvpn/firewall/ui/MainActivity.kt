package com.lightvpn.firewall.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.lightvpn.firewall.ui.screens.FirewallScreen
import com.lightvpn.firewall.ui.theme.NetMuzzleTheme
import com.lightvpn.firewall.ui.viewmodel.FirewallViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FirewallViewModel by viewModels()

    // Launcher do obsługi systemowego dialogu zgody na VPN (VpnService.prepare)
    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onVpnPermissionGranted(this)
        }
    }

    // Launcher uprawnienia powiadomień (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Uprawnienie przyznane lub odrzucone */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestNotificationPermission()

        setContent {
            NetMuzzleTheme {
                FirewallScreen(
                    viewModel = viewModel,
                    onRequireVpnPermission = { prepareIntent ->
                        vpnPermissionLauncher.launch(prepareIntent)
                    }
                )
            }
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
