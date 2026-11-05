package me.kavishdevar.librepods.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Decorative device illustration; the caller owns its artwork. */
@Composable
fun SettingsDeviceIllustration(
    painter: Painter,
    modifier: Modifier = Modifier,
    width: Dp = 120.dp,
    height: Dp = 80.dp,
    topPadding: Dp = 12.dp,
) {
    // rl.r.i / zn.a: padding is inside the measured box. Paint, unlike Image,
    // allows the illustration and its shadow to extend below that box.
    Box(modifier.requiredSize(width, height).padding(top = topPadding)
        .paint(painter, sizeToIntrinsics = false, alignment = Alignment.TopStart,
            contentScale = ContentScale.FillWidth))
}
