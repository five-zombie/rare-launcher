package io.github.rachmanzz.rarelauncer.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.rachmanzz.rarelauncer.data.model.AppDirectory
import io.github.rachmanzz.rarelauncer.data.model.AppItem
import io.github.rachmanzz.rarelauncer.ui.theme.AetherGlassBgCard
import io.github.rachmanzz.rarelauncer.ui.theme.AetherGlassBorder
import io.github.rachmanzz.rarelauncer.ui.theme.AetherOnSurface
import io.github.rachmanzz.rarelauncer.ui.theme.AetherOnSurfaceVariant
import io.github.rachmanzz.rarelauncer.ui.theme.AetherPrimary
import io.github.rachmanzz.rarelauncer.ui.theme.AetherSurfaceBright

@Composable
fun AppDirectorySection(
    directory: AppDirectory,
    allApps: List<AppItem>,
    onDirectoryClicked: () -> Unit,
    onAppClicked: ((AppItem) -> Unit)? = null,
    modifier: Modifier = Modifier,
    lockedApps: Set<String> = emptySet()
) {
    val appMap = remember(allApps) { allApps.associateBy { it.packageName } }
    val dirApps = directory.packageNames.mapNotNull { appMap[it] }.take(4)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // Section Header (1:1 from code.html: Focus Workspaces | 1 HUB)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Focus Workspaces",
                color = AetherOnSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "1 HUB",
                color = AetherPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 1 Folder Card (NOT expanded full width; strictly half-width matching 2-column grid in code.html)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Half-width App Directory Folder Card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AetherGlassBgCard)
                    .border(1.dp, AetherGlassBorder, RoundedCornerShape(16.dp))
                    .clickable { onDirectoryClicked() }
                    .padding(10.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header inside card: Title + Pill Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = directory.name,
                            color = AetherOnSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9999.dp))
                                .background(AetherPrimary.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${dirApps.size}/4 Apps",
                                color = AetherPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4 App Mini Squircle Grid (bg-surface-container-lowest/60)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x990B0E14))
                            .padding(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Row 1: 2 mini apps (slots 0 and 1)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                val app0 = dirApps.getOrNull(0)
                                val app1 = dirApps.getOrNull(1)
                                MiniAppCell(
                                    app = app0,
                                    isLocked = app0 != null && app0.packageName in lockedApps,
                                    onAppClicked = onAppClicked ?: { onDirectoryClicked() },
                                    onEmptySlotClicked = onDirectoryClicked
                                )
                                MiniAppCell(
                                    app = app1,
                                    isLocked = app1 != null && app1.packageName in lockedApps,
                                    onAppClicked = onAppClicked ?: { onDirectoryClicked() },
                                    onEmptySlotClicked = onDirectoryClicked
                                )
                            }

                            // Row 2: 2 mini apps (slots 2 and 3)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                val app2 = dirApps.getOrNull(2)
                                val app3 = dirApps.getOrNull(3)
                                MiniAppCell(
                                    app = app2,
                                    isLocked = app2 != null && app2.packageName in lockedApps,
                                    onAppClicked = onAppClicked ?: { onDirectoryClicked() },
                                    onEmptySlotClicked = onDirectoryClicked
                                )
                                MiniAppCell(
                                    app = app3,
                                    isLocked = app3 != null && app3.packageName in lockedApps,
                                    onAppClicked = onAppClicked ?: { onDirectoryClicked() },
                                    onEmptySlotClicked = onDirectoryClicked
                                )
                            }
                        }
                    }
                }
            }

            // Right half left empty to preserve the 1:1 half-width 2-column grid proportion
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MiniAppCell(
    app: AppItem?,
    isLocked: Boolean = false,
    onAppClicked: (AppItem) -> Unit,
    onEmptySlotClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(54.dp)
            .clickable {
                if (app != null) {
                    onAppClicked(app)
                } else {
                    onEmptySlotClicked()
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(40.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AetherSurfaceBright),
                contentAlignment = Alignment.Center
            ) {
                if (app?.icon != null) {
                    Image(
                        bitmap = app.icon,
                        contentDescription = app.label,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (app != null) {
                    Text(
                        text = app.label.take(1).uppercase(),
                        color = AetherPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add app",
                        tint = AetherOnSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (isLocked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF151923))
                        .border(1.dp, Color(0x99FFD54F), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(9.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app?.label ?: "Add",
            color = if (app != null) AetherOnSurfaceVariant else AetherOnSurfaceVariant.copy(alpha = 0.5f),
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
