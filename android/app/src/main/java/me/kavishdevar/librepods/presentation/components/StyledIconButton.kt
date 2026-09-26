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

package me.kavishdevar.librepods.presentation.components

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import me.kavishdevar.librepods.R

@Composable
fun StyledIconButton(
    modifier: Modifier = Modifier,
    icon: String,
    iconTint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    backdrop: LayerBackdrop = rememberLayerBackdrop(),
    onClick: () -> Unit,
    enabled: Boolean = true,
    materialButtonStyle: MaterialButtonStyle = MaterialButtonStyle.Normal
) {
    when (materialButtonStyle) {
        MaterialButtonStyle.Tonal -> {
            FilledTonalIconButton(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.size(52.dp)
            ) {
                Text(
                    text = icon,
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontFamily = FontFamily(Font(R.font.sf_pro))
                    )
                )
            }
        }
        MaterialButtonStyle.Filled -> {
            FilledIconButton(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.size(52.dp)
            ) {
                Text(
                    text = icon,
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontFamily = FontFamily(Font(R.font.sf_pro))
                    )
                )
            }
        }
        MaterialButtonStyle.Outlined -> {
            OutlinedIconButton(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.size(52.dp)
            ) {
                Text(
                    text = icon,
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontFamily = FontFamily(Font(R.font.sf_pro))
                    )
                )
            }
        }
        MaterialButtonStyle.Normal -> {
            IconButton(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.size(52.dp)
            ) {
                Text(
                    text = icon,
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontFamily = FontFamily(Font(R.font.sf_pro))
                    )
                )
            }
        }
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO, name = "Light")
@Preview(uiMode = UI_MODE_NIGHT_YES, name = "Dark")
@Composable
fun StyledIconButtonPreview() {
    Box(modifier = Modifier
        .height(120.dp)
        .width(200.dp)
        .background(
            if (isSystemInDarkTheme()) Color(0xFF000000) else Color(0xFFF2F2F7),
            RoundedCornerShape(28.dp)
        ), contentAlignment = Alignment.Center) {
        StyledIconButton(
            icon = "􀍟",
            onClick = { }
        )
    }
}
