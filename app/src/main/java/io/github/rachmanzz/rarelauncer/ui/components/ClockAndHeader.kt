package io.github.rachmanzz.rarelauncer.ui.components

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import io.github.rachmanzz.rarelauncer.data.model.AtmospherePreset
import io.github.rachmanzz.rarelauncer.data.model.WallpaperConfig
import io.github.rachmanzz.rarelauncer.ui.theme.AetherGlassBg
import io.github.rachmanzz.rarelauncer.ui.theme.AetherGlassBorder
import io.github.rachmanzz.rarelauncer.ui.theme.AetherGlassBorderSubtle
import io.github.rachmanzz.rarelauncer.ui.theme.AetherOnSurface
import io.github.rachmanzz.rarelauncer.ui.theme.AetherOnSurfaceVariant
import io.github.rachmanzz.rarelauncer.ui.theme.AetherPrimary
import io.github.rachmanzz.rarelauncer.ui.viewmodel.ClockUiState

@Composable
fun ClockAndHeader(
    clockState: ClockUiState,
    batteryLevel: Int,
    isCharging: Boolean,
    wallpaperConfig: WallpaperConfig,
    recentBubbleApps: List<AppItem> = emptyList(),
    isRecentBubbleVisible: Boolean = false,
    onRecentBubbleClicked: () -> Unit = {},
    onDismissRecentBubble: () -> Unit = {},
    onPresetSelected: (AtmospherePreset) -> Unit,
    onPickCustomWallpaper: () -> Unit,
    onClockClicked: () -> Unit,
    onDateClicked: () -> Unit,
    onOpenCommClicked: () -> Unit,
    onOpenMusicClicked: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Minimal Horizon Bar: Date / Recent Bubble & Battery Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Side: Recent App Bubble Cluster OR Live Date Pill
            Box(contentAlignment = Alignment.CenterStart) {
                // Default Live Date Pill (always underlying)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onDateClicked() }
                        .padding(vertical = 4.dp, horizontal = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(AetherPrimary, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = clockState.dateText.uppercase(),
                        color = AetherOnSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Recent Apps Organic Sticky Bubble Cluster (Overlapping Round Bubbles)
                androidx.compose.animation.AnimatedVisibility(
                    visible = isRecentBubbleVisible && recentBubbleApps.isNotEmpty(),
                    enter = fadeIn() + slideInHorizontally { -it / 2 },
                    exit = fadeOut() + slideOutHorizontally { -it / 2 }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(Color.Black.copy(alpha = 0.94f))
                            .border(1.dp, AetherPrimary.copy(alpha = 0.7f), RoundedCornerShape(9999.dp))
                            .clickable { onRecentBubbleClicked() }
                            .padding(start = 4.dp, top = 3.dp, bottom = 3.dp, end = 8.dp)
                    ) {
                        // Sticky overlapping bubble circles
                        Box(contentAlignment = Alignment.CenterStart) {
                            recentBubbleApps.take(3).forEachIndexed { index, app ->
                                Box(
                                    modifier = Modifier
                                        .padding(start = (index * 16).dp)
                                        .size(24.dp)
                                        .border(1.5.dp, Color.Black, CircleShape)
                                        .border(1.dp, AetherPrimary, CircleShape)
                                        .background(Color(0xFF1E262B), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (app.icon != null) {
                                        Image(
                                            bitmap = app.icon,
                                            contentDescription = app.label,
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                        )
                                    } else {
                                        Text(
                                            text = app.label.take(1).uppercase(),
                                            color = AetherPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        val bubbleOffset = (minOf(recentBubbleApps.size, 3) * 16 + 10).dp
                        Spacer(modifier = Modifier.width(bubbleOffset))

                        Text(
                            text = recentBubbleApps.firstOrNull()?.label ?: "Recent",
                            color = AetherPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(end = 6.dp)
                        )

                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = AetherOnSurfaceVariant,
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .clickable { onDismissRecentBubble() }
                        )
                    }
                }
            }

            // Live Battery Capsule

            // Live Battery Capsule
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(AetherGlassBg)
                    .border(1.dp, AetherGlassBorderSubtle, RoundedCornerShape(9999.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                    contentDescription = "Battery",
                    tint = if (isCharging) AetherPrimary else AetherOnSurfaceVariant,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$batteryLevel%",
                    color = AetherOnSurface,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Large Architectural Digital Clock & COMM shortcut button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clock (Hours : Minutes : Seconds)
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onClockClicked() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = clockState.hours,
                    color = AetherOnSurface,
                    fontSize = 62.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-2).sp
                )
                Text(
                    text = ":",
                    color = AetherPrimary,
                    fontSize = 58.sp,
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                Text(
                    text = clockState.minutes,
                    color = AetherOnSurface,
                    fontSize = 62.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-2).sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = clockState.seconds,
                    color = AetherPrimary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Quick Left Slide Trigger Button (< MUSIC)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(AetherGlassBg)
                        .border(1.dp, AetherGlassBorderSubtle, RoundedCornerShape(9999.dp))
                        .clickable { onOpenMusicClicked() }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Music Player",
                        tint = AetherPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "MUSIC",
                        color = AetherPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Quick Right Slide Trigger Button (COMM >)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(AetherGlassBg)
                        .border(1.dp, AetherGlassBorder, RoundedCornerShape(9999.dp))
                        .clickable { onOpenCommClicked() }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "COMM",
                        color = AetherOnSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Communication Hub",
                        tint = AetherPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Wallpaper Atmosphere Selector Glass Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(9999.dp))
                .background(AetherGlassBg)
                .border(1.dp, AetherGlassBorderSubtle, RoundedCornerShape(9999.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 6.dp, end = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Theme",
                    tint = AetherPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "AURA",
                    color = AetherOnSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AtmospherePreset.values().forEach { preset ->
                    val isSelected = wallpaperConfig.customImageUri == null &&
                            wallpaperConfig.preset == preset

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(
                                if (isSelected) Color(0xFF363940) else Color.Transparent
                            )
                            .clickable { onPresetSelected(preset) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = preset.title,
                            color = if (isSelected) AetherPrimary else AetherOnSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }

                // Pick Custom Wallpaper from Gallery
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(
                            if (wallpaperConfig.customImageUri != null) Color(0xFF363940) else Color.Transparent
                        )
                        .clickable { onPickCustomWallpaper() }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Custom Photo",
                            tint = if (wallpaperConfig.customImageUri != null) AetherPrimary else AetherOnSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Photo",
                            color = if (wallpaperConfig.customImageUri != null) AetherPrimary else AetherOnSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = if (wallpaperConfig.customImageUri != null) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
