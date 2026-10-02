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

package me.kavishdevar.librepods.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ColorScheme.sectionHeader: Color
    get() = onSurfaceVariant

// Separate SESL roles: changing a Material container role would recolor unrelated screens.
val ColorScheme.toolbarSubtitle: Color
    get() = if (onSurface == Color(0xFF010102)) Color(0xFF636368) else Color(0xFFB7B7BB)
// indicator_background_color is distinct from screen_background in the current source.
internal val ColorScheme.contentFade: Color
    get() = if (onSurface == Color(0xFF010102)) Color(0xFFF1F1F3) else Color(0xFF010101)
// action_bar_floating_button_bg_color; its surface applies .9 alpha separately.
internal val ColorScheme.floatingToolbarBackground: Color
    get() = if (onSurface == Color(0xFF010102)) Color(0xFFFCFCFF) else Color(0xFF252528)
val ColorScheme.switchThumb: Color
    get() = Color(0xFFFCFCFF)

// SESL seekbar roles: sesl_seekbar_control_color_default(_dark) and
// sesl_thumb_control_fill_color_activated -> sesl_gray_L1/D1.
val ColorScheme.sliderTrack: Color
    get() = if (onSurface == Color(0xFF010102)) Color(0x1A17171A) else Color(0x26FCFCFF)
val ColorScheme.sliderThumbCore: Color
    get() = if (onSurface == Color(0xFF010102)) Color(0xFFFCFCFF) else Color(0xFF010102)

// sesl_ripple_color → basic_token_state_pressed_on_surface_{light,dark}.
internal val ColorScheme.sliderFeedback: Color
    get() = if (onSurface == Color(0xFF010102)) Color(0x1A000000) else Color(0x33FFFFFF)

// ASC: xu.f -> nu.j -> ju.a case 12; resolved SESL resources in the reference manager.
private val SettingsLightColors = lightColorScheme(
    background = Color(0xFFF3F3F5), surfaceContainer = Color(0xFFF3F3F5),
    surface = Color(0xFFFCFCFF), onSurface = Color(0xFF010102),
    onBackground = Color(0xFF010102), onSurfaceVariant = Color(0xFF848487),
    primary = Color(0xFF387AFF), onPrimary = Color.White,
    secondary = Color(0xFF2E65D4), outline = Color(0xFFA3A3A7),
    inverseSurface = Color(0xFF252528), inverseOnSurface = Color(0xFFFCFCFF),
    secondaryContainer = Color(0xFFE4E4E7), onSecondaryContainer = Color(0xFF387AFF),
    outlineVariant = Color(0x1A000000),
)
private val SettingsDarkColors = darkColorScheme(
    background = Color(0xFF010102), surfaceContainer = Color(0xFF010102),
    surface = Color(0xFF17171A), onSurface = Color(0xFFFCFCFF),
    onBackground = Color(0xFFFCFCFF), onSurfaceVariant = Color(0xFFA3A3A7),
    primary = Color(0xFF387AFF), onPrimary = Color.White,
    secondary = Color(0xFF598FFF), outline = Color(0xFF636368),
    inverseSurface = Color(0xFFFCFCFF), inverseOnSurface = Color(0xFF252528),
    secondaryContainer = Color(0xFF3A3A3D), onSecondaryContainer = Color(0xFF387AFF),
    outlineVariant = Color(0x33FFFFFF),
)

@Composable
fun SettingsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialExpressiveTheme(
        colorScheme = if (darkTheme) SettingsDarkColors else SettingsLightColors,
        motionScheme = MotionScheme.standard(),
        typography = SettingsTypography,
        content = content
    )
}
