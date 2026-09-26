package com.netmuzzle.firewall.ui.screens

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netmuzzle.firewall.R
import com.netmuzzle.firewall.model.AppFilter
import com.netmuzzle.firewall.model.AppInfo
import com.netmuzzle.firewall.model.BlockMode
import com.netmuzzle.firewall.model.VpnStatus
import com.netmuzzle.firewall.ui.theme.DarkBorder
import com.netmuzzle.firewall.ui.theme.DarkCard
import com.netmuzzle.firewall.ui.theme.DarkSurface
import com.netmuzzle.firewall.ui.theme.ModeAdBlockAccent
import com.netmuzzle.firewall.ui.theme.ModeAdBlockBg
import com.netmuzzle.firewall.ui.theme.ModeAllowAccent
import com.netmuzzle.firewall.ui.theme.ModeAllowBg
import com.netmuzzle.firewall.ui.theme.ModeFullBlockAccent
import com.netmuzzle.firewall.ui.theme.ModeFullBlockBg
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
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showBatteryDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showAdBlockManagerDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

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
                    // Przycisk Pomocy i Samouczka
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_help),
                            contentDescription = stringResource(R.string.cd_help),
                            tint = NeonCyan
                        )
                    }

                    // Przycisk ustawień filtrów AdBlock w grach
                    IconButton(onClick = { showAdBlockManagerDialog = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_tune),
                            contentDescription = stringResource(R.string.cd_filters),
                            tint = NeonCyan
                        )
                    }

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
                                Text(
                                    stringResource(R.string.menu_help),
                                    color = TextPrimary,
                                    fontSize = 14.sp
                                )
                            },
                            onClick = {
                                showMenu = false
                                showHelpDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.menu_adblock_filters),
                                    color = TextPrimary,
                                    fontSize = 14.sp
                                )
                            },
                            onClick = {
                                showMenu = false
                                showAdBlockManagerDialog = true
                            }
                        )
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
                                    stringResource(R.string.setting_language),
                                    color = TextPrimary,
                                    fontSize = 14.sp
                                )
                            },
                            onClick = {
                                showMenu = false
                                showLanguageDialog = true
                            }
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
            // Główny Włącznik Ochrony (Hero Master Switch Card)
            MasterProtectionHeroCard(
                isEnabled = uiState.isMasterEnabled,
                status = uiState.status,
                fullBlockedCount = uiState.fullBlockedCount,
                adBlockedCount = uiState.adBlockedCount,
                onToggle = { isChecked ->
                    viewModel.onMasterSwitchToggled(isChecked, context, onRequireVpnPermission)
                }
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

            // Pasek filtrów z poziomym przewijaniem i licznikami (Chips)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Wszystkie (All)
                FilterChip(
                    selected = uiState.selectedFilter == AppFilter.ALL,
                    onClick = { viewModel.onFilterSelected(AppFilter.ALL) },
                    label = { Text(stringResource(R.string.filter_all, uiState.totalAppsCount)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                        selectedLabelColor = NeonCyan,
                        containerColor = DarkSurface,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (uiState.selectedFilter == AppFilter.ALL) NeonCyan else DarkBorder,
                        enabled = true,
                        selected = uiState.selectedFilter == AppFilter.ALL
                    )
                )

                // 2. Gry (Games)
                FilterChip(
                    selected = uiState.selectedFilter == AppFilter.GAMES,
                    onClick = { viewModel.onFilterSelected(AppFilter.GAMES) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_gamepad),
                            contentDescription = null,
                            tint = if (uiState.selectedFilter == AppFilter.GAMES) NeonCyan else TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    label = { Text(stringResource(R.string.filter_games, uiState.gamesCount)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                        selectedLabelColor = NeonCyan,
                        containerColor = DarkSurface,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (uiState.selectedFilter == AppFilter.GAMES) NeonCyan else DarkBorder,
                        enabled = true,
                        selected = uiState.selectedFilter == AppFilter.GAMES
                    )
                )

                // 3. Blokada Ads (AdBlock)
                FilterChip(
                    selected = uiState.selectedFilter == AppFilter.AD_BLOCK,
                    onClick = { viewModel.onFilterSelected(AppFilter.AD_BLOCK) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_shield_adblock),
                            contentDescription = null,
                            tint = if (uiState.selectedFilter == AppFilter.AD_BLOCK) ModeAdBlockAccent else TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    label = { Text(stringResource(R.string.filter_adblock, uiState.adBlockedCount)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ModeAdBlockAccent.copy(alpha = 0.2f),
                        selectedLabelColor = ModeAdBlockAccent,
                        containerColor = DarkSurface,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (uiState.selectedFilter == AppFilter.AD_BLOCK) ModeAdBlockAccent else DarkBorder,
                        enabled = true,
                        selected = uiState.selectedFilter == AppFilter.AD_BLOCK
                    )
                )

                // 4. Kaganiec (Full Block)
                FilterChip(
                    selected = uiState.selectedFilter == AppFilter.FULL_BLOCK,
                    onClick = { viewModel.onFilterSelected(AppFilter.FULL_BLOCK) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_block),
                            contentDescription = null,
                            tint = if (uiState.selectedFilter == AppFilter.FULL_BLOCK) ModeFullBlockAccent else TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    label = { Text(stringResource(R.string.filter_fullblock, uiState.fullBlockedCount)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ModeFullBlockAccent.copy(alpha = 0.2f),
                        selectedLabelColor = ModeFullBlockAccent,
                        containerColor = DarkSurface,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (uiState.selectedFilter == AppFilter.FULL_BLOCK) ModeFullBlockAccent else DarkBorder,
                        enabled = true,
                        selected = uiState.selectedFilter == AppFilter.FULL_BLOCK
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
                            onModeChanged = { newMode ->
                                viewModel.onAppBlockModeChanged(app.packageName, newMode, context)
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialog Wyboru Języka (Language Switcher)
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = {
                Text(
                    stringResource(R.string.dialog_language_title),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Opcja 1: English
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurface)
                            .clickable {
                                showLanguageDialog = false
                                changeAppLanguage(context, "en")
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🇺🇸", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.lang_english),
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Opcja 2: Polski
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurface)
                            .clickable {
                                showLanguageDialog = false
                                changeAppLanguage(context, "pl")
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🇵🇱", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.lang_polish),
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel), color = NeonCyan)
                }
            },
            containerColor = DarkCard
        )
    }

    // Dialog Samouczka i Pomocy
    if (showHelpDialog) {
        HelpGuideDialog(onDismiss = { showHelpDialog = false })
    }

    // Dialog Zarządzania Filtrami AdBlock
    if (showAdBlockManagerDialog) {
        AdBlockManagerDialog(
            uiState = uiState,
            onDismiss = { showAdBlockManagerDialog = false },
            onToggleAdNetwork = { netId, enabled ->
                viewModel.onAdNetworkToggled(netId, enabled, context)
            },
            onAddCustomDomain = { domain ->
                viewModel.onAddCustomDomain(domain, context)
            },
            onRemoveCustomDomain = { domain ->
                viewModel.onRemoveCustomDomain(domain, context)
            },
            onToggleCustomDomain = { domain, enabled ->
                viewModel.onToggleCustomDomain(domain, enabled, context)
            }
        )
    }

    // Dialog Optymalizacji Baterii
    if (showBatteryDialog) {
        AlertDialog(
            onDismissRequest = { showBatteryDialog = false },
            title = {
                Text(
                    stringResource(R.string.battery_opt_dialog_title),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    stringResource(R.string.battery_opt_dialog_desc),
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showBatteryDialog = false
                    requestIgnoreBatteryOptimization(context)
                }) {
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

/**
 * Wyrazisty, nowoczesny Główny Włącznik Ochrony (Hero Master Switch)
 */
@Composable
fun MasterProtectionHeroCard(
    isEnabled: Boolean,
    status: VpnStatus,
    fullBlockedCount: Int,
    adBlockedCount: Int,
    onToggle: (Boolean) -> Unit
) {
    val borderColorAnim by animateColorAsState(
        targetValue = if (isEnabled) {
            if (status == VpnStatus.STANDBY) StatusStandbyAmber else NeonCyan
        } else {
            DarkBorder
        },
        label = "heroBorderAnim"
    )

    val containerColorAnim by animateColorAsState(
        targetValue = if (isEnabled) {
            if (status == VpnStatus.STANDBY) Color(0xFF1E1912) else Color(0xFF0D1E28)
        } else {
            DarkCard
        },
        label = "heroContainerAnim"
    )

    val iconColorAnim by animateColorAsState(
        targetValue = if (isEnabled) {
            if (status == VpnStatus.STANDBY) StatusStandbyAmber else NeonCyan
        } else {
            TextMuted
        },
        label = "heroIconColorAnim"
    )

    val statusHeadline = if (isEnabled) {
        if (status == VpnStatus.STANDBY) {
            stringResource(R.string.firewall_status_standby)
        } else {
            stringResource(R.string.firewall_status_active)
        }
    } else {
        stringResource(R.string.firewall_status_disabled)
    }

    val subtitle = if (isEnabled) {
        when {
            fullBlockedCount > 0 && adBlockedCount > 0 -> stringResource(
                R.string.firewall_status_both_active,
                fullBlockedCount,
                adBlockedCount
            )
            adBlockedCount > 0 -> stringResource(
                R.string.firewall_status_ads_active,
                adBlockedCount
            )
            fullBlockedCount > 0 -> stringResource(
                R.string.firewall_status_muzzle_active,
                fullBlockedCount
            )
            else -> stringResource(R.string.notification_standby_text)
        }
    } else {
        stringResource(R.string.firewall_status_disabled_desc)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColorAnim, RoundedCornerShape(16.dp))
            .clickable { onToggle(!isEnabled) },
        colors = CardDefaults.cardColors(containerColor = containerColorAnim),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ikona Tarczy
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconColorAnim.copy(alpha = 0.15f))
                    .border(1.dp, iconColorAnim.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_shield),
                    contentDescription = null,
                    tint = iconColorAnim,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.master_switch_label).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = iconColorAnim,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 11.sp
                )
                Text(
                    text = statusHeadline,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Główny Włącznik (Duży, wyrazisty)
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = NeonCyan,
                    checkedTrackColor = NeonCyan.copy(alpha = 0.35f),
                    checkedBorderColor = NeonCyan,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = DarkSurface,
                    uncheckedBorderColor = DarkBorder
                )
            )
        }
    }
}

@Composable
fun AppListItem(
    app: AppInfo,
    onModeChanged: (BlockMode) -> Unit
) {
    val borderColorAnim by animateColorAsState(
        targetValue = when (app.blockMode) {
            BlockMode.FULL_BLOCK -> ModeFullBlockAccent.copy(alpha = 0.6f)
            BlockMode.AD_BLOCK -> ModeAdBlockAccent.copy(alpha = 0.6f)
            BlockMode.ALLOW -> DarkBorder
        },
        label = "borderColorAnim"
    )

    val containerColorAnim by animateColorAsState(
        targetValue = when (app.blockMode) {
            BlockMode.FULL_BLOCK -> Color(0xFF1E1318)
            BlockMode.AD_BLOCK -> Color(0xFF0C1D2A)
            BlockMode.ALLOW -> DarkCard
        },
        label = "containerColorAnim"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.2.dp, borderColorAnim, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = containerColorAnim),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Nagłówek kafelka: Ikona z plakietką gry + Nazwa + PackageName + Badge systemu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(
                    drawable = app.icon,
                    isGame = app.isGame,
                    modifier = Modifier.size(46.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = app.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (app.isSystemApp) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(DarkBorder)
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.badge_system),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Nowoczesna 3-stanowa kapsuła tekstowa z wyrazistym zaznaczeniem i podświetleniem
            SegmentedModePill(
                currentMode = app.blockMode,
                onModeChanged = onModeChanged,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SegmentedModePill(
    currentMode: BlockMode,
    onModeChanged: (BlockMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF090E17))
            .border(1.dp, DarkBorder, RoundedCornerShape(24.dp))
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Zezwalaj (Allow / Bypass)
        TextSegmentItem(
            text = stringResource(R.string.mode_allow),
            isSelected = currentMode == BlockMode.ALLOW,
            selectedBg = Color(0xFF1E293B),
            selectedBorder = Color(0xFF334155),
            selectedText = Color(0xFFF8FAFC),
            unselectedText = TextMuted,
            onClick = { onModeChanged(BlockMode.ALLOW) },
            modifier = Modifier.weight(1f)
        )

        // 2. Blokada Ads w grach (Block Ads / Game Shield)
        TextSegmentItem(
            text = stringResource(R.string.mode_adblock),
            isSelected = currentMode == BlockMode.AD_BLOCK,
            selectedBg = Color(0xFF0C2433),
            selectedBorder = NeonCyan,
            selectedText = NeonCyan,
            unselectedText = TextMuted,
            onClick = { onModeChanged(BlockMode.AD_BLOCK) },
            modifier = Modifier.weight(1f)
        )

        // 3. Kaganiec (Muzzle / Full Block)
        TextSegmentItem(
            text = stringResource(R.string.mode_fullblock),
            isSelected = currentMode == BlockMode.FULL_BLOCK,
            selectedBg = Color(0xFF281116),
            selectedBorder = Color(0xFFEF4444),
            selectedText = Color(0xFFFCA5A5),
            unselectedText = TextMuted,
            onClick = { onModeChanged(BlockMode.FULL_BLOCK) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TextSegmentItem(
    text: String,
    isSelected: Boolean,
    selectedBg: Color,
    selectedBorder: Color,
    selectedText: Color,
    unselectedText: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgAnim by animateColorAsState(
        targetValue = if (isSelected) selectedBg else Color.Transparent,
        label = "bgAnim"
    )
    val borderAnim by animateColorAsState(
        targetValue = if (isSelected) selectedBorder else Color.Transparent,
        label = "borderAnim"
    )
    val textTintAnim by animateColorAsState(
        targetValue = if (isSelected) selectedText else unselectedText,
        label = "textTintAnim"
    )

    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bgAnim)
            .border(if (isSelected) 1.2.dp else 0.dp, borderAnim, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textTintAnim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AppIcon(
    drawable: Drawable?,
    isGame: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        if (drawable != null) {
            val bitmap = remember(drawable) {
                drawableToBitmap(drawable)
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                FallbackAppIcon()
            }
        } else {
            FallbackAppIcon()
        }

        // Mini odznaka GAME w prawym dolnym rogu ikony
        if (isGame) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 3.dp, y = 3.dp)
                    .size(17.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF090D16))
                    .border(1.dp, NeonCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_gamepad),
                    contentDescription = stringResource(R.string.badge_game),
                    tint = NeonCyan,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}

@Composable
private fun FallbackAppIcon() {
    Box(
        modifier = Modifier
            .fillMaxSize()
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

fun changeAppLanguage(context: Context, langCode: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val localeManager = context.getSystemService(LocaleManager::class.java)
        localeManager?.applicationLocales = LocaleList.forLanguageTags(langCode)
    } else {
        val locale = java.util.Locale(langCode)
        java.util.Locale.setDefault(locale)
        val resources = context.resources
        val config = resources.configuration
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
        (context as? Activity)?.recreate()
    }
}
