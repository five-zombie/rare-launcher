package io.github.rachmanzz.rarelauncer.ui.components

import android.os.Build
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.rachmanzz.rarelauncer.data.model.AppItem
import io.github.rachmanzz.rarelauncer.ui.theme.AetherGlassBorder
import io.github.rachmanzz.rarelauncer.ui.theme.AetherOnSurface
import io.github.rachmanzz.rarelauncer.ui.theme.AetherOnSurfaceVariant
import io.github.rachmanzz.rarelauncer.ui.theme.AetherPrimary

@Composable
fun RecentAppsBubbleDialog(
    recentApps: List<AppItem>,
    onDismissRequest: () -> Unit,
    onAppClicked: (AppItem) -> Unit,
    onAppInfoClicked: (String) -> Unit,
    onRemoveFromRecents: (String) -> Unit,
    onClearAllRecents: () -> Unit = {},
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF14191C),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AetherGlassBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(AetherPrimary, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RECENT RUNNING APPS",
                        color = AetherPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AetherOnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            if (recentApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recent apps found",
                        color = AetherOnSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recentApps, key = { it.packageName }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1B2226))
                                .border(1.dp, AetherGlassBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    onAppClicked(app)
                                    onDismissRequest()
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (app.icon != null) {
                                Image(
                                    bitmap = app.icon,
                                    contentDescription = app.label,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(AetherPrimary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = app.label.take(1).uppercase(),
                                        color = Color.Black,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.label,
                                    color = AetherOnSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = app.packageName,
                                    color = AetherOnSurfaceVariant,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Quick Open Button
                            IconButton(
                                onClick = {
                                    onAppClicked(app)
                                    onDismissRequest()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Switch",
                                    tint = AetherPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // System Force Stop / App Info Button
                            IconButton(
                                onClick = {
                                    onAppInfoClicked(app.packageName)
                                    onDismissRequest()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PowerSettingsNew,
                                    contentDescription = "Force Stop (App Info)",
                                    tint = Color(0xFFFFD54F),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Kill background process & remove from recent manager
                            IconButton(
                                onClick = { onRemoveFromRecents(app.packageName) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Kill & Remove",
                                    tint = Color(0xFFFF6B6B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        item {
                            Text(
                                text = "Android 14+ membatasi kill otomatis. Gunakan ikon daya (kuning) untuk Force Stop sistem.",
                                color = AetherOnSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
                            )
                        }
                    }
                }
            }
        },
        dismissButton = {
            if (recentApps.isNotEmpty()) {
                TextButton(onClick = onClearAllRecents) {
                    Text(
                        text = "CLEAR ALL",
                        color = Color(0xFFFF6B6B),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(
                    text = "CLOSE",
                    color = AetherOnSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }
    )
}
