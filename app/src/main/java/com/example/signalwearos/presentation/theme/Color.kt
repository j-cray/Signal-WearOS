package com.example.signalwearos.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors

val SignalBlue = Color(0xFF3A76F0)
val SignalBlueVariant = Color(0xFF2C5AC0)
val SignalOrange = Color(0xFFFF9400)

internal val SignalColorPalette = Colors(
    primary = SignalBlue,
    primaryVariant = SignalBlueVariant,
    secondary = SignalOrange,
    error = Color.Red,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onError = Color.White
)
