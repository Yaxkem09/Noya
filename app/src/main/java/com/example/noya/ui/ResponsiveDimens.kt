package com.example.noya.ui

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Clase de datos para almacenar todas las dimensiones pre-calculadas
data class ResponsiveDimensValues(
    val scaleFactor: Float,
    val screenPadding: Dp,
    val smallPadding: Dp,
    val mediumPadding: Dp,
    val largePadding: Dp,
    val timeTextSize: Float,
    val dateTextSize: Float,
    val titleTextSize: Float,
    val largeTextSize: Float,
    val mediumTextSize: Float,
    val normalTextSize: Float,
    val smallTextSize: Float,
    val tinyTextSize: Float,
    val largeButtonHeight: Dp,
    val mediumButtonHeight: Dp,
    val smallButtonHeight: Dp,
    val extraSmallButtonHeight: Dp,
    val largeIconSize: Dp,
    val mediumIconSize: Dp,
    val smallIconSize: Dp,
    val contactPhotoSize: Dp,
    val callPhotoSize: Dp,
    val gridImageSize: Dp,
    val cornerRadius: Dp,
    val largeCornerRadius: Dp,
    val smallCornerRadius: Dp,
    val settingsButtonSize: Dp
)

// CompositionLocal para las dimensiones responsivas
private val LocalResponsiveDimens = staticCompositionLocalOf<ResponsiveDimensValues> {
    error("ResponsiveDimens not provided")
}

// Calcular dimensiones una sola vez
@Composable
private fun calculateResponsiveDimens(): ResponsiveDimensValues {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.toFloat()
    val scale = (screenWidth / 360f).coerceIn(0.8f, 1.5f)

    return ResponsiveDimensValues(
        scaleFactor = scale,
        screenPadding = (24 * scale).dp,
        smallPadding = (8 * scale).dp,
        mediumPadding = (16 * scale).dp,
        largePadding = (24 * scale).dp,
        timeTextSize = 56 * scale,
        dateTextSize = 24 * scale,
        titleTextSize = 32 * scale,
        largeTextSize = 28 * scale,
        mediumTextSize = 24 * scale,
        normalTextSize = 22 * scale,
        smallTextSize = 18 * scale,
        tinyTextSize = 16 * scale,
        largeButtonHeight = (100 * scale).dp,
        mediumButtonHeight = (80 * scale).dp,
        smallButtonHeight = (60 * scale).dp,
        extraSmallButtonHeight = (48 * scale).dp,
        largeIconSize = (48 * scale).dp,
        mediumIconSize = (32 * scale).dp,
        smallIconSize = (28 * scale).dp,
        contactPhotoSize = (150 * scale).dp,
        callPhotoSize = (150 * scale).dp,
        gridImageSize = (110 * scale).dp,
        cornerRadius = (16 * scale).dp,
        largeCornerRadius = (20 * scale).dp,
        smallCornerRadius = (12 * scale).dp,
        settingsButtonSize = (60 * scale).dp
    )
}

// Provider para las dimensiones
@Composable
fun ProvideResponsiveDimens(content: @Composable () -> Unit) {
    val dimens = calculateResponsiveDimens()
    CompositionLocalProvider(LocalResponsiveDimens provides dimens) {
        content()
    }
}

// Dimensiones responsivas: ResponsiveDimens.current.screenPadding, etc.
object ResponsiveDimens {
    val current: ResponsiveDimensValues
        @Composable get() = LocalResponsiveDimens.current
}
