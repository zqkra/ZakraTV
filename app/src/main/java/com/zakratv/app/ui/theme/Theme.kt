package com.zakratv.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

val ZakraBg = Color(0xFF0B0B0F)
val ZakraSurface = Color(0xFF14141C)
val ZakraSurface2 = Color(0xFF1C1C28)
val ZakraAccent = Color(0xFFE50914)
val ZakraAccentSoft = Color(0xFFB20710)
val ZakraText = Color(0xFFF5F5F7)
val ZakraMuted = Color(0xFFB3B3C0)
val ZakraFocus = Color(0xFFFFFFFF)
val ZakraSuccess = Color(0xFF2ECC71)
val ZakraWarning = Color(0xFFF1C40F)

private val DarkScheme = darkColorScheme(
    primary = ZakraAccent,
    onPrimary = Color.White,
    secondary = ZakraSurface2,
    onSecondary = ZakraText,
    background = ZakraBg,
    onBackground = ZakraText,
    surface = ZakraSurface,
    onSurface = ZakraText,
    border = ZakraMuted.copy(alpha = 0.35f),
)

@Suppress("DEPRECATION")
private val NoFontPadding = PlatformTextStyle(includeFontPadding = false)

private val CenterLine = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)

/** Body ≥20sp — centered line metrics so TV buttons don't look top-heavy. */
object ZakraType {
    val title = TextStyle(
        color = ZakraText,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 34.sp,
        platformStyle = NoFontPadding,
        lineHeightStyle = CenterLine,
    )
    val section = TextStyle(
        color = ZakraText,
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 30.sp,
        platformStyle = NoFontPadding,
        lineHeightStyle = CenterLine,
    )
    val body = TextStyle(
        color = ZakraText,
        fontSize = 20.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 26.sp,
        platformStyle = NoFontPadding,
        lineHeightStyle = CenterLine,
    )
    val bodyMuted = TextStyle(
        color = ZakraMuted,
        fontSize = 20.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 26.sp,
        platformStyle = NoFontPadding,
        lineHeightStyle = CenterLine,
    )
    val caption = TextStyle(
        color = ZakraMuted,
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 22.sp,
        platformStyle = NoFontPadding,
        lineHeightStyle = CenterLine,
    )
    val button = TextStyle(
        color = ZakraText,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp,
        textAlign = TextAlign.Center,
        platformStyle = NoFontPadding,
        lineHeightStyle = CenterLine,
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ZakraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        content = content,
    )
}
