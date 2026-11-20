package io.github.rachmanzz.rarelauncer.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.rachmanzz.rarelauncer.data.model.AppItem
import io.github.rachmanzz.rarelauncer.ui.theme.AetherGlassBorder
import io.github.rachmanzz.rarelauncer.ui.theme.AetherGlassBorderSubtle
import io.github.rachmanzz.rarelauncer.ui.theme.AetherOnPrimary
import io.github.rachmanzz.rarelauncer.ui.theme.AetherOnSurface
import io.github.rachmanzz.rarelauncer.ui.theme.AetherOnSurfaceVariant
import io.github.rachmanzz.rarelauncer.ui.theme.AetherPrimary
import io.github.rachmanzz.rarelauncer.ui.theme.RareLauncerTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun SlideToOpenOverlay(
    app: AppItem,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isLocked: Boolean = false,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val density = LocalDensity.current
        val haptics = LocalHapticFeedback.current
        val scope = rememberCoroutineScope()

        val thumbSizeDp = 48.dp
        val trackPaddingDp = 4.dp
        val thumbSizePx = with(density) { thumbSizeDp.toPx() }
        val trackPaddingPx = with(density) { trackPaddingDp.toPx() }

        var trackWidthPx by remember { mutableIntStateOf(0) }
        val maxOffsetPx = (trackWidthPx - thumbSizePx - trackPaddingPx * 2f).coerceAtLeast(1f)
        val offsetX = remember(app.packageName) { Animatable(0f) }
        var confirmed by remember(app.packageName) { mutableStateOf(false) }

        val progress = (offsetX.value / maxOffsetPx).coerceIn(0f, 1f)

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0x9905070A))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                )
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xF5151923))
                    .border(1.dp, AetherGlassBorder, RoundedCornerShape(24.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF222836))
                            .border(1.dp, AetherGlassBorderSubtle, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (app.icon != null) {
                            Image(
                                bitmap = app.icon,
                                contentDescription = app.label,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                            )
                        } else {
                            Text(
                                text = app.label.take(1).uppercase(),
                                color = AetherPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = app.label,
                                color = AetherOnSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (isLocked) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Protected",
                                    tint = Color(0xFFFFD54F),
                                    modifier = Modifier.size(15.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isLocked) {
                                "Geser ke kanan lalu verifikasi sidik jari / PIN"
                            } else {
                                "Geser kiri ke kanan untuk membuka"
                            },
                            color = AetherOnSurfaceVariant,
                            fontSize = 12.sp,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Progress-bar style Slide-to-Open track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(Color(0xFF1C212E))
                        .border(1.dp, AetherGlassBorderSubtle, RoundedCornerShape(9999.dp))
                        .onSizeChanged { trackWidthPx = it.width }
                        .padding(trackPaddingDp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    // Progress fill
                    val fillWidthDp = with(density) {
                        (thumbSizePx + offsetX.value).coerceAtLeast(thumbSizePx).toDp()
                    }
                    Box(
                        modifier = Modifier
                            .width(fillWidthDp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(9999.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        AetherPrimary.copy(alpha = 0.22f),
                                        AetherPrimary.copy(alpha = 0.65f),
                                    ),
                                ),
                            ),
                    )

                    // Centered hint label
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha((1f - progress * 1.4f).coerceIn(0f, 1f)),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(modifier = Modifier.width(24.dp))
                        Text(
                            text = "SLIDE TO OPEN",
                            color = AetherOnSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.6.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = AetherPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    // Draggable thumb
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                            .size(thumbSizeDp)
                            .clip(CircleShape)
                            .background(AetherPrimary)
                            .pointerInput(app.packageName, maxOffsetPx) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        if (confirmed) return@detectHorizontalDragGestures
                                        scope.launch {
                                            if (offsetX.value / maxOffsetPx >= 0.85f) {
                                                confirmed = true
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                offsetX.animateTo(maxOffsetPx, tween(90))
                                                onConfirm()
                                            } else {
                                                offsetX.animateTo(
                                                    0f,
                                                    spring(stiffness = Spring.StiffnessMedium),
                                                )
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        if (confirmed) return@detectHorizontalDragGestures
                                        scope.launch {
                                            offsetX.animateTo(
                                                0f,
                                                spring(stiffness = Spring.StiffnessMedium),
                                            )
                                        }
                                    },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        if (confirmed) return@detectHorizontalDragGestures
                                        scope.launch {
                                            val next = (offsetX.value + dragAmount).coerceIn(0f, maxOffsetPx)
                                            offsetX.snapTo(next)
                                            if (next >= maxOffsetPx * 0.96f && !confirmed) {
                                                confirmed = true
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onConfirm()
                                            }
                                        }
                                    },
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Slide to open",
                            tint = AetherOnPrimary,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Batal",
                    color = AetherOnSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0D14)
@Composable
private fun SlideToOpenOverlayPreview() {
    RareLauncerTheme {
        SlideToOpenOverlay(
            app = AppItem("WhatsApp", "com.whatsapp", ""),
            onConfirm = {},
            onDismiss = {},
        )
    }
}
