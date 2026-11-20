package io.github.rachmanzz.rarelauncer.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

data class AppItem(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: ImageBitmap? = null
)

data class AppDirectory(
    val id: String = "primary_dir",
    val name: String = "Apps",
    val packageNames: List<String> = emptyList()
)

data class ContactItem(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val initials: String,
    val subtitle: String = "Mobile"
)

enum class AtmospherePreset(
    val id: String,
    val title: String,
    val gradientColors: List<Color>,
    val glowColor1: Color,
    val glowColor2: Color
) {
    SPACE(
        id = "space",
        title = "Deep Space",
        gradientColors = listOf(Color(0xFF0B101D), Color(0xFF10131A), Color(0xFF05070A)),
        glowColor1 = Color(0x3300F0FF),
        glowColor2 = Color(0x287000FF)
    ),
    NEON(
        id = "neon",
        title = "Cyber Neon",
        gradientColors = listOf(Color(0xFF0E0024), Color(0xFF10131A), Color(0xFF01141C)),
        glowColor1 = Color(0x3D7000FF),
        glowColor2 = Color(0x3300F0FF)
    ),
    SUNSET(
        id = "sunset",
        title = "Sunset Glow",
        gradientColors = listOf(Color(0xFF240C14), Color(0xFF10131A), Color(0xFF0A0714)),
        glowColor1 = Color(0x33FF3366),
        glowColor2 = Color(0x28FFA726)
    ),
    MINIMAL(
        id = "minimal",
        title = "Minimal Slate",
        gradientColors = listOf(Color(0xFF13171E), Color(0xFF10131A), Color(0xFF0C0E12)),
        glowColor1 = Color(0x28363940),
        glowColor2 = Color(0x20272A31)
    )
}

data class WallpaperConfig(
    val preset: AtmospherePreset = AtmospherePreset.SPACE,
    val customImageUri: String? = null
)
