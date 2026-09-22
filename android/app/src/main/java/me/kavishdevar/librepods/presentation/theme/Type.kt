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

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// tp.d / u3.a: optional on-device family, not a bundled or downloaded Samsung font.
// Declare every supported weight so Compose does not synthesize one from a single Typeface.
internal val BudsFontFamily = FontFamily(listOf(100, 300, 400, 500, 600, 700).map {
    Font(DeviceFontFamilyName("sec"), FontWeight(it), variationSettings = FontVariation.Settings())
})

internal val BudsTypography = Typography().run {
    val body = TextStyle(fontFamily = BudsFontFamily, fontSize = 17.sp)
    copy(
        bodyLarge = body, bodyMedium = body,
        bodySmall = body.copy(fontSize = 13.sp),
        titleLarge = body.copy(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
        titleSmall = body.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),
        labelMedium = body, labelMediumEmphasized = body,
        labelSmall = body.copy(fontSize = 13.sp),
        labelSmallEmphasized = body.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
    )
}
