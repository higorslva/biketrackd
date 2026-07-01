package com.biketrackd.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun screenScale(): Float {
    val config = LocalConfiguration.current
    return (config.screenWidthDp / 360f).coerceIn(0.8f, 1.5f)
}

@Composable
fun scaledDp(dp: Dp): Dp = dp * screenScale()
