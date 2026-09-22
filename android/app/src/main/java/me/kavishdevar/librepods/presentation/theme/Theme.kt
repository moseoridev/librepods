/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package me.kavishdevar.librepods.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

val ColorScheme.sectionHeader: Color
    get() = onSurfaceVariant

// Separate SESL roles: changing a Material container role would recolor unrelated screens.
val ColorScheme.toolbarSubtitle: Color
    get() = if (onSurface == Color(0xFF010102)) Color(0xFF636368) else Color(0xFFB7B7BB)
val ColorScheme.switchThumb: Color
    get() = Color(0xFFFCFCFF)

// ASC: xu.f -> nu.j -> ju.a case 12; resolved SESL resources in Buds 4 Manager.
private val BudsLightColors = lightColorScheme(
    background = Color(0xFFF1F1F3), surfaceContainer = Color(0xFFF1F1F3),
    surface = Color(0xFFFCFCFF), onSurface = Color(0xFF010102),
    onBackground = Color(0xFF010102), onSurfaceVariant = Color(0xFF848487),
    primary = Color(0xFF387AFF), onPrimary = Color.White,
    secondary = Color(0xFF2E65D4), outline = Color(0xFFA3A3A7),
    inverseSurface = Color(0xFF252528), inverseOnSurface = Color(0xFFFCFCFF),
    secondaryContainer = Color(0xFFE4E4E7), onSecondaryContainer = Color(0xFF387AFF),
    outlineVariant = Color(0x1A000000),
)
private val BudsDarkColors = darkColorScheme(
    background = Color(0xFF010102), surfaceContainer = Color(0xFF010102),
    surface = Color(0xFF17171A), onSurface = Color(0xFFFCFCFF),
    onBackground = Color(0xFFFCFCFF), onSurfaceVariant = Color(0xFFA3A3A7),
    primary = Color(0xFF387AFF), onPrimary = Color.White,
    secondary = Color(0xFF578FFF), outline = Color(0xFF636368),
    inverseSurface = Color(0xFFFCFCFF), inverseOnSurface = Color(0xFF252528),
    secondaryContainer = Color(0xFF3A3A3D), onSecondaryContainer = Color(0xFF387AFF),
    outlineVariant = Color(0x33FFFFFF),
)

@Suppress("UNUSED_PARAMETER") // Retain the upstream call signature during the screen migration.
@Composable
fun LibrePodsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    m3eEnabled: Boolean = false,
    content: @Composable () -> Unit
) {
    // Unmigrated specialized controls still use their standard Compose branch.
    CompositionLocalProvider(LocalDesignSystem provides DesignSystem.Material) {
        MaterialExpressiveTheme(
            colorScheme = if (darkTheme) BudsDarkColors else BudsLightColors,
            motionScheme = MotionScheme.standard(),
            typography = BudsTypography,
            content = content
        )
    }
}
