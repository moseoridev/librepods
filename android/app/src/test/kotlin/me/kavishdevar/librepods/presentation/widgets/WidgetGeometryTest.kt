package me.kavishdevar.librepods.presentation.widgets

import org.junit.Assert.*
import org.junit.Test

class WidgetGeometryTest {
    @Test fun legacyBoundsPreserveBothOrientationsInsteadOfCombiningTheirMinima() {
        val variants = WidgetGeometry.fromBounds(125f, 52f, 270f, 130f)
        assertEquals(listOf(
            WidgetGeometry(125f, 130f, WidgetGeometry.MEDIUM),
            WidgetGeometry(270f, 52f, WidgetGeometry.WIDE)
        ), variants)
    }

    @Test fun enlargedPreviewPreservesTheHostsSingleCycleButtonAndRowCount() {
        val source = WidgetGeometry.from(125f, 52f)
        val preview = source.resized(300f, 124.8f)
        assertEquals(WidgetGeometry.SMALL, preview.type)
        assertTrue(preview.narrow)
        assertTrue(preview.short)
        assertEquals(source.columns, preview.columns)
        assertEquals(source.rows, preview.rows)
        val originalControls = WidgetControlLayout.create(source, 1)
        val previewControls = WidgetControlLayout.create(preview, 1)
        val ratio = preview.width / source.width
        assertEquals(originalControls.diameter * ratio, previewControls.diameter, .001f)
        assertEquals(originalControls.batterySize * ratio, previewControls.batterySize, .001f)

        // A wide preview must also magnify the seams, or the battery drifts after four buttons.
        val wide = WidgetGeometry.from(302f, 62f, controls = true)
        val enlarged = wide.resized(604f, 124f)
        val hostRow = WidgetControlLayout.create(wide, 4)
        val previewRow = WidgetControlLayout.create(enlarged, 4)
        assertEquals(hostRow.gap * 2, previewRow.gap, .001f)
        assertEquals(hostRow.headerX * 2, previewRow.headerX, .001f)
    }

    @Test fun shortControlsKeepBatteryOutsideButtonsAtMinimumAndGalaxyHostSizes() {
        for ((width, height) in listOf(100f to 30f, 136f to 62f, 270f to 52f, 334.4f to 71.73333f)) {
            val size = WidgetGeometry.from(width, height, controls = true)
            for (count in if (size.narrow) listOf(1) else listOf(3, 4)) {
                val layout = WidgetControlLayout.create(size, count)
                val buttonsEnd = layout.buttonX + layout.diameter * count + layout.gap * (count - 1)
                assertTrue(layout.headerX >= buttonsEnd)
                assertTrue(layout.headerX + layout.headerWidth <= width)
                assertTrue(layout.buttonY >= 0f && layout.buttonY + layout.diameter <= height)
                assertTrue(layout.batteryY >= 0f && layout.batteryY + layout.batteryRowHeight * 2 <= height)
            }
        }
    }
}
