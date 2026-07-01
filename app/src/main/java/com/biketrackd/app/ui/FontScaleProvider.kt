package com.biketrackd.app.ui

import androidx.compose.runtime.staticCompositionLocalOf

val LocalFontScale = staticCompositionLocalOf { 1f }
val LocalSetFontScale = staticCompositionLocalOf<((Float) -> Unit)> { {} }
