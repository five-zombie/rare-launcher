package io.github.rachmanzz.rarelauncer.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.rachmanzz.rarelauncer.data.model.ContactItem
import io.github.rachmanzz.rarelauncer.ui.theme.AetherGlassBorder
import io.github.rachmanzz.rarelauncer.ui.theme.AetherGlassBorderSubtle
import io.github.rachmanzz.rarelauncer.ui.theme.AetherOnPrimary
import io.github.rachmanzz.rarelauncer.ui.theme.AetherOnSurface
import io.github.rachmanzz.rarelauncer.ui.theme.AetherOnSurfaceVariant
import io.github.rachmanzz.rarelauncer.ui.theme.AetherPrimary
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

enum class CommunicationActionType {
    CALL,
    MESSAGE
}

@Composable
fun SlideToCallOverlay(
    contact: ContactItem,
    actionType: CommunicationActionType,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
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
        val actionKey = remember(contact.id, actionType) { "${contact.id}_${actionType.name}" }
        val offsetX = remember(actionKey) { Animatable(0f) }
        var confirmed by remember(actionKey) { mutableStateOf(false) }

        val progress = (offsetX.value / maxOffsetPx).coerceIn(0f, 1f)
        val isCall = actionType == CommunicationActionType.CALL
        val actionColor = if (isCall) AetherPrimary else Color(0xFF7000FF)
        val actionLabel = if (isCall) "PANGGIL KONTAK" else "KIRIM PESAN"

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
                // Contact Avatar Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(actionColor.copy(alpha = 0.2f))
                        .border(1.dp, actionColor.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = contact.initials,
                        color = actionColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Contact Name
                Text(
                    text = contact.name,
                    color = AetherOnSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Phone number
                Text(
                    text = if (contact.phoneNumber.isNotEmpty()) contact.phoneNumber else contact.subtitle,
                    color = AetherOnSurfaceVariant,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Slider Track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(Color(0xFF0D1017))
                        .border(1.dp, AetherGlassBorderSubtle, RoundedCornerShape(9999.dp))
                        .onSizeChanged { trackWidthPx = it.width },
                    contentAlignment = Alignment.CenterStart,
                ) {
                    // Filled progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(
                                with(density) {
                                    (trackPaddingPx + offsetX.value + thumbSizePx / 2f).toDp()
                                },
                            )
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        actionColor.copy(alpha = 0.15f),
                                        actionColor.copy(alpha = 0.45f),
                                    ),
                                ),
                            ),
                    )

                    // Track Label
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = thumbSizeDp + 8.dp, end = 16.dp)
                            .alpha((1f - progress * 1.4f).coerceIn(0f, 1f)),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Geser untuk $actionLabel",
                            color = AetherOnSurfaceVariant.copy(alpha = 0.75f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = actionColor.copy(alpha = 0.75f),
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    // Draggable Thumb Knob
                    Box(
                        modifier = Modifier
                            .padding(start = trackPaddingDp)
                            .offset { IntOffset(x = offsetX.value.roundToInt(), y = 0) }
                            .size(thumbSizeDp)
                            .clip(CircleShape)
                            .background(actionColor)
                            .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            .pointerInput(actionKey, maxOffsetPx) {
                                detectHorizontalDragGestures(
                                    onDragStart = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragEnd = {
                                        if (progress >= 0.85f && !confirmed) {
                                            confirmed = true
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            scope.launch {
                                                offsetX.animateTo(maxOffsetPx, tween(100))
                                                onConfirm()
                                            }
                                        } else if (!confirmed) {
                                            scope.launch {
                                                offsetX.animateTo(
                                                    0f,
                                                    spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessLow,
                                                    ),
                                                )
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        if (!confirmed) {
                                            scope.launch {
                                                offsetX.animateTo(0f, spring())
                                            }
                                        }
                                    },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        if (confirmed) return@detectHorizontalDragGestures
                                        val newTarget = (offsetX.value + dragAmount).coerceIn(0f, maxOffsetPx)
                                        scope.launch { offsetX.snapTo(newTarget) }

                                        if (newTarget / maxOffsetPx >= 0.96f && !confirmed) {
                                            confirmed = true
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            scope.launch {
                                                offsetX.snapTo(maxOffsetPx)
                                                onConfirm()
                                            }
                                        }
                                    },
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isCall) Icons.Default.Call else Icons.AutoMirrored.Filled.Message,
                            contentDescription = actionLabel,
                            tint = AetherOnPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Batal Button
                Text(
                    text = "Batal",
                    color = AetherOnSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }
    }
}
