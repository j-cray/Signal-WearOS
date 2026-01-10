package com.example.signalwearos.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme

@Composable
fun SignalWearOSTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = SignalColorPalette,
        content = content
    )
}
