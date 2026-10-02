package me.kavishdevar.librepods.ui.theme

import android.graphics.PathMeasure
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsGroupShapeTest {
    @Test fun sharedShapeTracksResizeAndDensityChanges() {
        val shape = SettingsGroupShape(26.dp)
        val density = object : Density {
            override var density = 3f
            override val fontScale = 1f
        }
        fun check(size: Size, radiusPx: Float, direction: LayoutDirection = LayoutDirection.Ltr) {
            val outline = shape.createOutline(size, direction, density) as Outline.Generic
            val bounds = outline.path.getBounds()
            assertEquals(size.width, bounds.width, .001f)
            assertEquals(size.height, bounds.height, .001f)
            val start = FloatArray(2)
            assertTrue(PathMeasure(outline.path.asAndroidPath(), false).getPosTan(0f, start, null))
            assertEquals(0f, start[0], .001f)
            assertEquals(radiusPx, start[1], .001f)
        }
        check(Size(300f, 240f), 78f)
        check(Size(90f, 120f), 45f)
        check(Size(300f, 240f), 78f)
        // A caller can reuse a Density object while its configuration changes.
        density.density = 2f
        check(Size(300f, 240f), 52f)
        check(Size(300f, 240f), 52f, LayoutDirection.Rtl)
        assertTrue(shape.createOutline(Size.Zero, LayoutDirection.Ltr, density) is Outline.Rectangle)
        check(Size(300f, 240f), 52f)
    }
}
