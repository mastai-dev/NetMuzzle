package com.netmuzzle.firewall.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netmuzzle.firewall.R
import com.netmuzzle.firewall.model.AppInfo
import com.netmuzzle.firewall.model.FirewallUiState
import com.netmuzzle.firewall.model.VpnStatus
import com.netmuzzle.firewall.ui.theme.DarkBorder
import com.netmuzzle.firewall.ui.theme.DarkCard
import com.netmuzzle.firewall.ui.theme.DarkSurface
import com.netmuzzle.firewall.ui.theme.NeonCyan
import com.netmuzzle.firewall.ui.theme.StatusActiveGreen
import com.netmuzzle.firewall.ui.theme.StatusActiveGreenContainer
import com.netmuzzle.firewall.ui.theme.StatusDisabledRed
import com.netmuzzle.firewall.ui.theme.StatusStandbyAmber
import com.netmuzzle.firewall.ui.theme.StatusStandbyAmberContainer
import com.netmuzzle.firewall.ui.theme.TextMuted
import com.netmuzzle.firewall.ui.theme.TextPrimary
import com.netmuzzle.firewall.ui.theme.TextSecondary
import com.netmuzzle.firewall.ui.viewmodel.FirewallViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirewallScreen(
    viewModel: FirewallViewModel,
    onRequireVpnPermission: (Intent) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }
    var showBatteryDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_shield),
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                    }
                },
                actions = {
                    // Master Switch na belce
                    Switch(
                        checked = uiState.isMasterEnabled,
                        onCheckedChange = { isChecked ->
                            viewModel.onMasterSwitchToggled(isChecked, context, onRequireVpnPermission)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyan,
                            checkedTrackColor = NeonCyan.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkCard
                        )
                    )
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.cd_menu),
                            tint = TextSecondary
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        stringResource(R.string.setting_start_on_boot),
                                        color = TextPrimary,
                                        fontSize = 14.sp
                                    )
                                    Switch(
                                        checked = uiState.startOnBoot,
                                        onCheckedChange = { viewModel.onStartOnBootToggled(it) }
                                    )
                                }
                            },
                            onClick = { viewModel.onStartOnBootToggled(!uiState.startOnBoot) }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        stringResource(R.string.filter_system_apps),
                                        color = TextPrimary,
                                        fontSize = 14.sp
                                    )
                                    Switch(
                                        checked = uiState.showSystemApps,
                                        onCheckedChange = { viewModel.onShowSystemAppsToggled(it) }
                                    )
                                }
                            },
                            onClick = { viewModel.onShowSystemAppsToggled(!uiState.showSystemApps) }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.setting_battery_opt),
                                    color = TextPrimary,
                                    fontSize = 14.sp
                                )
                            },
                            onClick = {
                                showMenu = false
                                showBatteryDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.menu_about),
                                    color = TextPrimary,
                                    fontSize = 14.sp
                                )
                            },
                            onClick = {
                                showMenu = false
                                showAboutDialog = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Status Card
            StatusBanner(
                status = uiState.status,
                blockedCount = uiState.blockedCount
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Wyszukiwarka
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        stringResource(R.string.search_placeholder),
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.cd_clear),
                                tint = TextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = DarkBorder,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filtry (Chips)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !uiState.filterBlockedOnly,
                    onClick = { viewModel.onFilterBlockedToggled(false) },
                    label = { Text(stringResource(R.string.filter_all)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                        selectedLabelColor = NeonCyan,
                        containerColor = DarkSurface,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (!uiState.filterBlockedOnly) NeonCyan else DarkBorder,
                        enabled = true,
                        selected = !uiState.filterBlockedOnly
                    )
                )

                FilterChip(
                    selected = uiState.filterBlockedOnly,
                    onClick = { viewModel.onFilterBlockedToggled(true) },
                    label = {
                        Text(stringResource(R.string.filter_blocked, uiState.blockedCount))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = StatusStandbyAmber.copy(alpha = 0.2f),
                        selectedLabelColor = StatusStandbyAmber,
                        containerColor = DarkSurface,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (uiState.filterBlockedOnly) StatusStandbyAmber else DarkBorder,
                        enabled = true,
                        selected = uiState.filterBlockedOnly
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lista aplikacji
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            } else if (uiState.apps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_apps_found),
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = uiState.apps,
                        key = { it.packageName }
                    ) { app ->
                        AppListItem(
                            app = app,
                            onToggleBlocked = { isBlocked ->
                                viewModel.onAppBlockToggled(app.packageName, isBlocked, context)
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialog optymalizacji baterii
    if (showBatteryDialog) {
        AlertDialog(
            onDismissRequest = { showBatteryDialog = false },
            title = {
                Text(
                    stringResource(R.string.battery_opt_dialog_title),
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    stringResource(R.string.battery_opt_dialog_desc),
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBatteryDialog = false
                        requestIgnoreBatteryOptimization(context)
                    }
                ) {
                    Text(stringResource(R.string.battery_opt_action), color = NeonCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatteryDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel), color = TextMuted)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Dialog O aplikacji (Autor & Open Source)
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Text(
                    stringResource(R.string.about_title),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    stringResource(R.string.about_desc),
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.dialog_ok), color = NeonCyan)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun StatusBanner(
    status: VpnStatus,
    blockedCount: Int
) {
    val (bgColor, borderColor, iconColor, statusTitle, statusSubtitle) = when (status) {
        VpnStatus.ACTIVE -> StatusDetails(
            bgColor = StatusActiveGreenContainer.copy(alpha = 0.4f),
            borderColor = StatusActiveGreen,
            iconColor = StatusActiveGreen,
            title = stringResource(R.string.firewall_status_active),
            subtitle = stringResource(R.string.notification_active_text, blockedCount)
        )
        VpnStatus.STANDBY -> StatusDetails(
            bgColor = StatusStandbyAmberContainer.copy(alpha = 0.4f),
            borderColor = StatusStandbyAmber,
            iconColor = StatusStandbyAmber,
            title = stringResource(R.string.firewall_status_standby),
            subtitle = stringResource(R.string.notification_standby_text)
        )
        VpnStatus.DISABLED -> StatusDetails(
            bgColor = DarkCard,
            borderColor = DarkBorder,
            iconColor = StatusDisabledRed,
            title = stringResource(R.string.firewall_status_disabled),
            subtitle = stringResource(R.string.firewall_status_disabled_desc)
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_shield),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = statusTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = iconColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = statusSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

private data class StatusDetails(
    val bgColor: Color,
    val borderColor: Color,
    val iconColor: Color,
    val title: String,
    val subtitle: String
)

@Composable
fun AppListItem(
    app: AppInfo,
    onToggleBlocked: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (app.isBlocked) Color(0xFFEF4444).copy(alpha = 0.5f) else DarkBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (app.isBlocked) Color(0xFF1F1618) else DarkCard
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ikona aplikacji
            AppIcon(drawable = app.icon, modifier = Modifier.size(42.dp))

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (app.isSystemApp) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkBorder)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.badge_system),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Przełącznik blokady
            Switch(
                checked = app.isBlocked,
                onCheckedChange = onToggleBlocked,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFEF4444),
                    checkedTrackColor = Color(0xFFEF4444).copy(alpha = 0.3f),
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = DarkSurface
                )
            )
        }
    }
}

@Composable
fun AppIcon(drawable: Drawable?, modifier: Modifier = Modifier) {
    if (drawable != null) {
        val bitmap = remember(drawable) {
            drawableToBitmap(drawable)
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = modifier.clip(RoundedCornerShape(8.dp))
            )
            return
        }
    }
    // Fallback ikona
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_shield),
            contentDescription = null,
            tint = NeonCyan,
            modifier = Modifier.size(24.dp)
        )
    }
}

private fun drawableToBitmap(drawable: Drawable): Bitmap? {
    return try {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
    } catch (e: Exception) {
        null
    }
}

private fun requestIgnoreBatteryOptimization(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            context.startActivity(fallbackIntent)
        }
    }
}
