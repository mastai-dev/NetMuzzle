package com.netmuzzle.firewall.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.netmuzzle.firewall.model.AppInfo
import com.netmuzzle.firewall.model.CapturedDomain
import com.netmuzzle.firewall.service.dns.TrafficInspectorManager
import com.netmuzzle.firewall.ui.theme.DarkBorder
import com.netmuzzle.firewall.ui.theme.DarkCard
import com.netmuzzle.firewall.ui.theme.DarkSurface
import com.netmuzzle.firewall.ui.theme.ModeAdBlockAccent
import com.netmuzzle.firewall.ui.theme.NeonCyan
import com.netmuzzle.firewall.ui.theme.StatusActiveGreen
import com.netmuzzle.firewall.ui.theme.StatusActiveGreenContainer
import com.netmuzzle.firewall.ui.theme.StatusStandbyAmber
import com.netmuzzle.firewall.ui.theme.TextMuted
import com.netmuzzle.firewall.ui.theme.TextPrimary
import com.netmuzzle.firewall.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun TrafficInspectorDialog(
    app: AppInfo,
    onDismiss: () -> Unit,
    onAddCustomDomain: (String) -> Unit,
    onRemoveCustomDomain: (String) -> Unit
) {
    val isSniffing by TrafficInspectorManager.isSniffing.collectAsState()
    val remainingSeconds by TrafficInspectorManager.remainingSeconds.collectAsState()
    val capturedDomains by TrafficInspectorManager.capturedDomains.collectAsState()
    val activeTarget by TrafficInspectorManager.targetPackageName.collectAsState()

    val isThisAppTarget = activeTarget == app.packageName
    val isCurrentlyActiveForApp = isSniffing && isThisAppTarget

    // Pulsowanie ikony radaru podczas aktywnego nasłuchu
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .border(1.2.dp, if (isCurrentlyActiveForApp) NeonCyan else DarkBorder, RoundedCornerShape(20.dp)),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AppIcon(
                    drawable = app.icon,
                    isGame = app.isGame,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.traffic_inspector_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Wskaźnik stanu aktywności
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCurrentlyActiveForApp) NeonCyan.copy(alpha = 0.15f * pulseAlpha)
                            else DarkSurface
                        )
                        .border(
                            1.dp,
                            if (isCurrentlyActiveForApp) NeonCyan.copy(alpha = pulseAlpha) else DarkBorder,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_radar),
                        contentDescription = null,
                        tint = if (isCurrentlyActiveForApp) NeonCyan else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
            ) {
                // Pasek kontrolny: Start / Stop + Licznik czasu
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrentlyActiveForApp) StatusActiveGreenContainer.copy(alpha = 0.35f) else DarkSurface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isCurrentlyActiveForApp) StatusActiveGreen.copy(alpha = 0.5f) else DarkBorder
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isCurrentlyActiveForApp) {
                                        val minutes = remainingSeconds / 60
                                        val seconds = remainingSeconds % 60
                                        val timeStr = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                                        stringResource(R.string.traffic_inspector_active_status, timeStr)
                                    } else {
                                        stringResource(R.string.traffic_inspector_idle_status)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isCurrentlyActiveForApp) StatusActiveGreen else TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            if (isCurrentlyActiveForApp) {
                                OutlinedButton(
                                    onClick = { TrafficInspectorManager.stopSniffing() },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = StatusStandbyAmber
                                    ),
                                    border = BorderStroke(1.dp, StatusStandbyAmber),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                        horizontal = 12.dp,
                                        vertical = 6.dp
                                    )
                                ) {
                                    Text(
                                        text = stringResource(R.string.traffic_inspector_stop),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        TrafficInspectorManager.startSniffing(app.packageName, durationMinutes = 30)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NeonCyan,
                                        contentColor = Color(0xFF090D16)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                        horizontal = 12.dp,
                                        vertical = 6.dp
                                    )
                                ) {
                                    Text(
                                        text = stringResource(R.string.traffic_inspector_start),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Wskazówka jak znaleźć serwer reklamy
                        if (!isCurrentlyActiveForApp && capturedDomains.isEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.traffic_inspector_tip),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Lista przechwyconych domen na żywo
                if (capturedDomains.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.traffic_inspector_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = capturedDomains,
                            key = { it.domain }
                        ) { item ->
                            CapturedDomainItem(
                                item = item,
                                onBlock = { onAddCustomDomain(item.domain) },
                                onUnblock = { onRemoveCustomDomain(item.domain) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (capturedDomains.isNotEmpty()) {
                    TextButton(onClick = { TrafficInspectorManager.clearDomains() }) {
                        Text(
                            text = stringResource(R.string.traffic_inspector_btn_clear),
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.dialog_ok),
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    )
}

@Composable
private fun CapturedDomainItem(
    item: CapturedDomain,
    onBlock: () -> Unit,
    onUnblock: () -> Unit
) {
    val borderColor = when {
        item.isBlocked -> StatusActiveGreen.copy(alpha = 0.4f)
        item.isSuspicious -> StatusStandbyAmber.copy(alpha = 0.5f)
        else -> DarkBorder
    }

    val cardBg = when {
        item.isBlocked -> Color(0xFF0D211C)
        item.isSuspicious -> Color(0xFF211B0E)
        else -> DarkSurface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Nazwa domeny
                Text(
                    text = item.domain,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Plakietki: Status (Zablokowana / Podejrzana / Zezwolona) + Czas + Licznik
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (item.isBlocked) {
                        BadgePill(
                            text = stringResource(R.string.traffic_inspector_badge_blocked),
                            bgColor = StatusActiveGreen.copy(alpha = 0.2f),
                            textColor = StatusActiveGreen
                        )
                    } else if (item.isSuspicious) {
                        BadgePill(
                            text = stringResource(R.string.traffic_inspector_badge_suspicious),
                            bgColor = StatusStandbyAmber.copy(alpha = 0.2f),
                            textColor = StatusStandbyAmber
                        )
                    } else {
                        BadgePill(
                            text = stringResource(R.string.traffic_inspector_badge_allowed),
                            bgColor = DarkBorder,
                            textColor = TextMuted
                        )
                    }

                    if (item.queryCount > 1) {
                        BadgePill(
                            text = "${item.queryCount}x",
                            bgColor = DarkCard,
                            textColor = TextSecondary
                        )
                    }

                    Text(
                        text = formatTimeAgo(item.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Przycisk Zablokuj / Odblokuj
            if (item.isBlocked) {
                TextButton(
                    onClick = onUnblock,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.traffic_inspector_btn_unblock),
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            } else {
                Button(
                    onClick = onBlock,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (item.isSuspicious) StatusStandbyAmber else NeonCyan,
                        contentColor = Color(0xFF090D16)
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.traffic_inspector_btn_block),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgePill(text: String, bgColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    val diffSeconds = ((System.currentTimeMillis() - timestamp) / 1000).coerceAtLeast(0)
    return when {
        diffSeconds < 5 -> "przed chwilą"
        diffSeconds < 60 -> "${diffSeconds}s temu"
        diffSeconds < 3600 -> "${diffSeconds / 60}m temu"
        else -> "${diffSeconds / 3600}h temu"
    }
}
