package io.github.rachmanzz.rarelauncer.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.rachmanzz.rarelauncer.data.model.AtmospherePreset
import io.github.rachmanzz.rarelauncer.data.model.WallpaperConfig
import io.github.rachmanzz.rarelauncer.ui.components.AtmosphericBackground
import io.github.rachmanzz.rarelauncer.ui.theme.AetherGlassBorder
import io.github.rachmanzz.rarelauncer.ui.theme.AetherOnSurfaceVariant
import io.github.rachmanzz.rarelauncer.ui.theme.AetherPrimary
import io.github.rachmanzz.rarelauncer.ui.theme.AetherSurface
import io.github.rachmanzz.rarelauncer.ui.theme.RareLauncerTheme
import io.github.rachmanzz.rarelauncer.ui.viewmodel.ClockUiState

private val LockClockColor = Color(0xFFDBFCFF)
private val LockPillBg = Color(0xB3272A31)

/**
 * 1:1 implementation of temp/.../aetheros_minimal_lock_screen:
 * Secured pill, big clock + seconds, date, battery pill,
 * tagline and the bottom hub (torch | swipe up to unlock | camera).
 */
@Composable
fun LockScreen(
    clockState: ClockUiState,
    batteryLevel: Int,
    isCharging: Boolean,
    wallpaperConfig: WallpaperConfig,
    isTorchOn: Boolean,
    onTorchClick: () -> Unit,
    onCameraClick: () -> Unit,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AetherSurface)
            .pointerInput(Unit) {
                var accumulatedY = 0f
                var handled = false
                detectVerticalDragGestures(
                    onDragStart = {
                        accumulatedY = 0f
                        handled = false
                    },
                    onDragEnd = { accumulatedY = 0f },
                    onDragCancel = { accumulatedY = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedY += dragAmount
                        // Swipe UP only
                        if (!handled && accumulatedY < -140f) {
                            handled = true
                            onUnlock()
                        }
                    },
                )
            },
    ) {
        AtmosphericBackground(config = wallpaperConfig)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Header lock state indicator
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(LockPillBg)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = LockClockColor,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "SECURED",
                    color = AetherOnSurfaceVariant,
                    fontSize = 9.sp,
                    letterSpacing = 1.5.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Clock + trailing seconds
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${clockState.hours}:${clockState.minutes}",
                    color = LockClockColor,
                    fontSize = 68.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-2).sp,
                )
                Text(
                    text = clockState.seconds,
                    color = AetherOnSurfaceVariant.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
                )
            }

            // Date
            Text(
                text = clockState.dateText.uppercase(),
                color = LockClockColor.copy(alpha = 0.8f),
                fontSize = 11.sp,
                letterSpacing = 1.5.sp,
                fontFamily = FontFamily.Monospace,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Battery telemetry pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(LockPillBg)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = if (isCharging) AetherPrimary else LockClockColor,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = "$batteryLevel%",
                    color = AetherPrimary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                )
                if (isCharging) {
                    Text(
                        text = "CHARGING",
                        color = AetherOnSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Aspirational status prompt
            Text(
                text = "PRESENCE • INTENT • EQUILIBRIUM",
                color = AetherOnSurfaceVariant.copy(alpha = 0.5f),
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom interaction hub
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ShortcutButton(
                    icon = if (isTorchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                    description = "Flashlight",
                    active = isTorchOn,
                    onClick = onTorchClick,
                )

                SwipeUpHint(onClick = onUnlock)

                ShortcutButton(
                    icon = Icons.Default.PhotoCamera,
                    description = "Camera",
                    active = false,
                    onClick = onCameraClick,
                )
            }
        }
    }
}

@Composable
private fun ShortcutButton(
    icon: ImageVector,
    description: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (active) AetherPrimary else LockPillBg)
            .border(1.dp, AetherGlassBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (active) Color(0xFF006970) else AetherOnSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SwipeUpHint(onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "unlock-bounce")
    val bounce = transition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bounce",
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardDoubleArrowUp,
            contentDescription = null,
            tint = AetherPrimary.copy(alpha = 0.8f),
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer { translationY = bounce.value.dp.toPx() },
        )
        Text(
            text = "SWIPE UP TO UNLOCK",
            color = AetherOnSurfaceVariant.copy(alpha = 0.8f),
            fontSize = 9.sp,
            letterSpacing = 1.5.sp,
            fontFamily = FontFamily.Monospace,
        )
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(9999.dp))
                .background(AetherPrimary.copy(alpha = 0.8f)),
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun LockScreenPreview() {
    RareLauncerTheme {
        LockScreen(
            clockState = ClockUiState(
                hours = "09",
                minutes = "41",
                seconds = "28",
                dateText = "Thursday, Oct 24",
            ),
            batteryLevel = 88,
            isCharging = true,
            wallpaperConfig = WallpaperConfig(preset = AtmospherePreset.SPACE),
            isTorchOn = false,
            onTorchClick = {},
            onCameraClick = {},
            onUnlock = {},
        )
    }
}
