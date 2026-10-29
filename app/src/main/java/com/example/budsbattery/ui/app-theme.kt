package com.example.budsbattery.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.budsbattery.R

/**
 * Material You colors from the wallpaper, light or dark with the system. Shared by every screen.
 *
 * @param content - the screen to theme
 */
@Composable
fun BudsTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colors = if (isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    MaterialTheme(colorScheme = colors, content = content)
}

/**
 * Picks the icon that matches what is reachable: the left or right bud alone, both buds, or the case while a bud sits in it.
 *
 * @param shape - shape from the shared view
 * @returns a drawable resource id
 */
fun shapeIcon(shape: BudsShape): Int = when (shape) {
    BudsShape.LEFT_BUD -> R.drawable.ic_bud_left
    BudsShape.RIGHT_BUD -> R.drawable.ic_bud_right
    BudsShape.TWO_BUDS -> R.drawable.ic_buds
    BudsShape.WITH_CASE -> R.drawable.ic_case
}
