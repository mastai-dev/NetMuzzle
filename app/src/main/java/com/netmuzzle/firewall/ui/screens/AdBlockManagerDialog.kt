package com.netmuzzle.firewall.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netmuzzle.firewall.R
import com.netmuzzle.firewall.model.DefaultAdNetworks
import com.netmuzzle.firewall.model.FirewallUiState
import com.netmuzzle.firewall.ui.theme.DarkBorder
import com.netmuzzle.firewall.ui.theme.DarkCard
import com.netmuzzle.firewall.ui.theme.DarkSurface
import com.netmuzzle.firewall.ui.theme.NeonCyan
import com.netmuzzle.firewall.ui.theme.TextMuted
import com.netmuzzle.firewall.ui.theme.TextPrimary
import com.netmuzzle.firewall.ui.theme.TextSecondary

@Composable
fun AdBlockManagerDialog(
    uiState: FirewallUiState,
    onDismiss: () -> Unit,
    onToggleAdNetwork: (String, Boolean) -> Unit,
    onAddCustomDomain: (String) -> Unit,
    onRemoveCustomDomain: (String) -> Unit,
    onToggleCustomDomain: (String, Boolean) -> Unit
) {
    var showAddDomainDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_shield_adblock),
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.adblock_manager_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
            ) {
                Text(
                    text = stringResource(R.string.adblock_manager_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Sekcja: Wbudowane sieci
                    item {
                        Text(
                            text = stringResource(R.string.builtin_networks_header),
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    items(DefaultAdNetworks.NETWORKS, key = { it.id }) { network ->
                        val isEnabled = !uiState.disabledAdNetworks.contains(network.id)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, if (isEnabled) NeonCyan.copy(alpha = 0.3f) else DarkBorder, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = if (isEnabled) Color(0xFF0F1E28) else DarkCard),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = network.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = network.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = network.domains.firstOrNull() ?: "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = isEnabled,
                                    onCheckedChange = { onToggleAdNetwork(network.id, it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = NeonCyan,
                                        checkedTrackColor = NeonCyan.copy(alpha = 0.3f),
                                        uncheckedThumbColor = TextMuted,
                                        uncheckedTrackColor = DarkSurface
                                    )
                                )
                            }
                        }
                    }

                    // Sekcja: Własne domeny
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.custom_domains_header),
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (uiState.customAdDomains.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.no_custom_domains),
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        items(uiState.customAdDomains.toList(), key = { it }) { domain ->
                            val isEnabled = !uiState.disabledCustomDomains.contains(domain)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, if (isEnabled) NeonCyan.copy(alpha = 0.3f) else DarkBorder, RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = if (isEnabled) Color(0xFF0F1E28) else DarkCard),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = domain,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Switch(
                                        checked = isEnabled,
                                        onCheckedChange = { onToggleCustomDomain(domain, it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = NeonCyan,
                                            checkedTrackColor = NeonCyan.copy(alpha = 0.3f),
                                            uncheckedThumbColor = TextMuted,
                                            uncheckedTrackColor = DarkSurface
                                        )
                                    )
                                    IconButton(onClick = { onRemoveCustomDomain(domain) }) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_delete),
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showAddDomainDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkCard,
                                contentColor = NeonCyan
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_add),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.add_domain_button), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_ok), color = NeonCyan)
            }
        },
        containerColor = DarkSurface
    )

    if (showAddDomainDialog) {
        var inputDomain by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDomainDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.add_domain_dialog_title),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.add_domain_dialog_desc),
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputDomain,
                        onValueChange = {
                            inputDomain = it
                            isError = false
                        },
                        placeholder = { Text(stringResource(R.string.add_domain_hint), color = TextMuted) },
                        singleLine = true,
                        isError = isError,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clean = inputDomain.trim().lowercase()
                            .removePrefix("http://")
                            .removePrefix("https://")
                            .trimEnd('/')
                        if (clean.isNotBlank() && clean.contains('.')) {
                            onAddCustomDomain(clean)
                            showAddDomainDialog = false
                        } else {
                            isError = true
                        }
                    }
                ) {
                    Text(stringResource(R.string.add_domain_confirm), color = NeonCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDomainDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel), color = TextMuted)
                }
            },
            containerColor = DarkSurface
        )
    }
}
