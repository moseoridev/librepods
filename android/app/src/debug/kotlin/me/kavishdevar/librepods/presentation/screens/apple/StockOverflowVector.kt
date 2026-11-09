package me.kavishdevar.librepods.presentation.screens.apple

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Manager 9.0.10.1724 draws its masthead overflow from a programmatic Filled.Overflow
// vector, not from a drawable resource: `base_menu_more_ellipsis` measures 8px wide per
// dot where the native is 9px, and the whole cluster sits one pixel left. Reproducing the
// vector is what closes that band. Debug fixture only; nothing here ships.
private val overflowVector = ImageVector.Builder(
    name = "Filled.Overflow", defaultWidth = 24.dp, defaultHeight = 24.dp,
    viewportWidth = 24f, viewportHeight = 24f,
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(11.6f, 16.75f)
        curveTo(12.4684f, 16.75f, 13.1f, 17.3816f, 13.1f, 18.25f)
        curveTo(13.1f, 19.1184f, 12.4684f, 19.75f, 11.6f, 19.75f)
        curveTo(10.8105f, 19.75f, 10.1f, 19.1184f, 10.1f, 18.25f)
        curveTo(10.1f, 17.3816f, 10.7316f, 16.75f, 11.6f, 16.75f)
        close()
        moveTo(11.6f, 10.5f)
        curveTo(12.4684f, 10.5f, 13.1f, 11.1316f, 13.1f, 12f)
        curveTo(13.1f, 12.8684f, 12.4684f, 13.5f, 11.6f, 13.5f)
        curveTo(10.8105f, 13.5f, 10.1f, 12.8684f, 10.1f, 12f)
        curveTo(10.1f, 11.1316f, 10.7316f, 10.5f, 11.6f, 10.5f)
        close()
        moveTo(11.6f, 4.25f)
        curveTo(12.3895f, 4.25f, 13.1f, 4.8816f, 13.1f, 5.75f)
        curveTo(13.1f, 6.6184f, 12.4684f, 7.25f, 11.6f, 7.25f)
        curveTo(10.8105f, 7.25f, 10.1f, 6.6184f, 10.1f, 5.75f)
        curveTo(10.1f, 4.8816f, 10.7316f, 4.25f, 11.6f, 4.25f)
        close()
    }
}.build()

@Composable
internal fun StockOverflowIcon(tint: Color) {
    Icon(overflowVector, null, Modifier.size(24.dp), tint = tint)
}
