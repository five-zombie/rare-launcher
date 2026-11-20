package io.github.rachmanzz.rarelauncer.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.rachmanzz.rarelauncer.data.model.WallpaperConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AtmosphericBackground(
    config: WallpaperConfig,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var customBitmap by remember(config.customImageUri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(config.customImageUri) {
        val uriStr = config.customImageUri
        if (!uriStr.isNullOrEmpty()) {
            customBitmap = withContext(Dispatchers.IO) {
                try {
                    val uri = Uri.parse(uriStr)
                    val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, boundsOpts)
                    }
                    val maxDim = 2048
                    var sampleSize = 1
                    while (
                        boundsOpts.outWidth / sampleSize > maxDim ||
                        boundsOpts.outHeight / sampleSize > maxDim
                    ) {
                        sampleSize *= 2
                    }
                    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, decodeOpts)?.asImageBitmap()
                    }
                } catch (_: Throwable) {
                    null
                }
            }
        } else {
            customBitmap = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (customBitmap != null) {
            Image(
                bitmap = customBitmap!!,
                contentDescription = "Custom Wallpaper",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // System-level dimming filter for optical clarity
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xB310131A))
            )
        } else {
            // Atmospheric multi-stop gradient backdrop
            val gradientBrush = Brush.verticalGradient(
                colors = config.preset.gradientColors
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(gradientBrush)
            )

            // Top ambient diffuse glow
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-80).dp)
                    .size(340.dp)
                    .blur(90.dp)
                    .background(config.preset.glowColor1, CircleShape)
            )

            // Center-right secondary radiant glow
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 60.dp, y = 50.dp)
                    .size(280.dp)
                    .blur(80.dp)
                    .background(config.preset.glowColor2, CircleShape)
            )

            // Subtle base canvas noise/vignette overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0x6605070A)),
                        radius = size.maxDimension * 0.85f
                    )
                )
            }
        }
    }
}
