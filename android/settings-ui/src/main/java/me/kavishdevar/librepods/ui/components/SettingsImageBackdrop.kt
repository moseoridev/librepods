package me.kavishdevar.librepods.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/** Source Home's image placement. Artwork is supplied and owned by the caller. */
@Composable
fun SettingsImageBackdrop(painter: Painter, modifier: Modifier = Modifier) {
    // gm.a.b -> a1.o3 case 12 / a1.t case 25. Keep padding outside the layer.
    Image(painter, contentDescription = null,
        modifier = modifier.fillMaxWidth().padding(top = 110.dp).graphicsLayer {
            scaleX = 4f
            scaleY = 3f
            // s2.n0.v sets the translation-X bit (8), not translation-Y (16).
            translationX = 50.dp.toPx()
        }, contentScale = ContentScale.Fit)
}
