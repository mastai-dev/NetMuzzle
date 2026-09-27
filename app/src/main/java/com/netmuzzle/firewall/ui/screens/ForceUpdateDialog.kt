package com.netmuzzle.firewall.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.netmuzzle.firewall.R
import com.netmuzzle.firewall.model.AppUpdateInfo
import com.netmuzzle.firewall.ui.theme.DarkBorder
import com.netmuzzle.firewall.ui.theme.DarkCard
import com.netmuzzle.firewall.ui.theme.DarkSurface
import com.netmuzzle.firewall.ui.theme.NeonCyan
import com.netmuzzle.firewall.ui.theme.StatusActiveGreen
import com.netmuzzle.firewall.ui.theme.StatusActiveGreenContainer
import com.netmuzzle.firewall.ui.theme.StatusStandbyAmber
import com.netmuzzle.firewall.ui.theme.TextMuted
import com.netmuzzle.firewall.ui.theme.TextPrimary
import com.netmuzzle.firewall.ui.theme.TextSecondary

@Composable
fun ForceUpdateDialog(
    updateInfo: AppUpdateInfo?,
    currentVersionName: String,
    onUpdateClicked: () -> Unit,
    onDismissToBackground: () -> Unit
) {
    val fallbackLatest = stringResource(R.string.latest_version_label)
    val latestName = updateInfo?.latestVersionName?.ifBlank { null } ?: fallbackLatest
    val currentLanguage = java.util.Locale.getDefault().language
    val localizedReleaseNotes = updateInfo?.getLocalizedReleaseNotes(currentLanguage).orEmpty()

    AlertDialog(
        onDismissRequest = { /* Modalny - brak możliwości zamknięcia kliknięciem obok */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        containerColor = DarkCard,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .border(1.dp, StatusStandbyAmber.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(StatusStandbyAmber.copy(alpha = 0.15f))
                        .border(1.dp, StatusStandbyAmber, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_shield),
                        contentDescription = null,
                        tint = StatusStandbyAmber,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column {
                    Text(
                        text = stringResource(R.string.update_required_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = stringResource(R.string.update_required_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusStandbyAmber
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(
                        R.string.update_required_desc,
                        currentVersionName,
                        latestName
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )

                // Karta informująca o ciągłym działaniu zapory w tle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = StatusActiveGreenContainer.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(1.dp, StatusActiveGreen.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(StatusActiveGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_shield),
                                contentDescription = null,
                                tint = StatusActiveGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = stringResource(R.string.update_service_running_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusActiveGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Sekcja z opisem zmian (Changelog) jeśli dostępna
                if (localizedReleaseNotes.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = DarkSurface
                        ),
                        border = BorderStroke(1.dp, DarkBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.update_changelog_header),
                                style = MaterialTheme.typography.labelMedium,
                                color = NeonCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = localizedReleaseNotes,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onUpdateClicked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = Color(0xFF090D16)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.update_btn_download),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissToBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.update_btn_run_background),
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
        }
    )
}
