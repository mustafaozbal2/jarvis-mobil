package com.mustafa.jarvis.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** JARVIS renkleri: Nebula yeşili çekirdek, koyu uzay zemini. */
val NebulaYesil = Color(0xFF00FF9E)
val NebulaMavi = Color(0xFF00CCFF)
val NebulaMor = Color(0xFF8B5CFF)
val UzayZemin = Color(0xFF02110D)
val UzayYuzey = Color(0xFF0A1F19)
val Soluk = Color(0xFFB8D8CE)

private val renkler = darkColorScheme(
    primary = NebulaYesil,
    onPrimary = Color(0xFF00241A),
    secondary = NebulaMavi,
    tertiary = NebulaMor,
    background = UzayZemin,
    onBackground = Color(0xFFE6FFF6),
    surface = UzayYuzey,
    onSurface = Color(0xFFE6FFF6),
    surfaceVariant = Color(0xFF123128),
    onSurfaceVariant = Soluk,
    error = Color(0xFFFF6B6B),
)

@Composable
fun JarvisTema(icerik: @Composable () -> Unit) {
    // JARVIS daima koyu tema kullanır (sistem açık temada olsa bile)
    MaterialTheme(colorScheme = renkler, typography = Typography(), content = icerik)
}
