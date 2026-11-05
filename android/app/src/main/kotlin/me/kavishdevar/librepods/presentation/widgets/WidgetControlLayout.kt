package me.kavishdevar.librepods.presentation.widgets

/** Buds 4 Manager 9.0.00.1604 home layout: xm.b0, xm.m, xm.q0, xm.r0 and xm.j. */
internal data class WidgetControlLayout(
    val scale: Float,
    val buttonX: Float, val buttonY: Float, val diameter: Float, val gap: Float,
    val headerX: Float, val headerY: Float, val headerWidth: Float,
    val titleSize: Float, val titleHeight: Float,
    val batteryY: Float, val batteryRowHeight: Float, val batterySize: Float,
    val compact: Boolean, val centered: Boolean,
) {
    companion object {
        fun create(size: WidgetGeometry, count: Int): WidgetControlLayout {
            val w = size.width
            val h = size.height
            val s = size.controlScale ?: minOf(w / (if (size.narrow) 136f else 302f),
                h / (if (size.short) 62f else 156f)).coerceIn(.6f, 1.3f)
            val padding = (if (size.short) 8f else 12f) * s
            val innerWidth = (w - padding * 2).coerceAtLeast(1f)
            // The native linked row reserves a 1 dp seam per link, independent of size ratio.
            // A configuration preview magnifies the entire host layout, including that seam.
            // Its wrap-content width, rather than the allocation budget, precedes the battery.
            fun gap(width: Float, diameter: Float) = if (count > 1)
                ((width - diameter * count) / (count - 1) - size.controlSeamScale).coerceAtLeast(0f) else 0f
            if (size.short) {
                val contentHeight = h * .75f
                val controlWidth = if (size.narrow) contentHeight else innerWidth * .747f
                val diameter = minOf(46f * s, controlWidth / count, contentHeight)
                val gap = gap(controlWidth, diameter)
                val headerX = padding + diameter * count + gap * (count - 1) +
                    if (size.narrow) 6f * s else innerWidth * .022f
                val headerWidth = (w - 9f * s - headerX).coerceAtLeast(1f)
                val rowHeight = minOf(16f * s, contentHeight / 2)
                return WidgetControlLayout(s, padding, (h - diameter) / 2, diameter,
                    gap,
                    headerX, (h - rowHeight * 2) / 2, headerWidth, 0f, 0f, (h - rowHeight * 2) / 2, rowHeight,
                    minOf(12f * s, rowHeight * .75f), true, false)
            }
            val top = if (size.narrow) h * .052f else padding
            val bottom = if (size.narrow) h * .067f else 0f
            val diameter = minOf(52f * s, innerWidth / count, h - top - bottom)
            val spacing = (if (size.narrow) 1f else 2f) * s
            val free = (h - top - bottom - diameter - spacing).coerceAtLeast(0f)
            val titleBudget = free * (if (size.narrow) .388f else .538f)
            val batteryBudget = free * (if (size.narrow) .592f else .41f)
            val titleSize = minOf(16f * s, titleBudget * 16f / 21f)
            val rowHeight = minOf(16f * s, batteryBudget / (if (size.narrow) 2 else 1))
            val titleHeight = titleSize * 21f / 16f
            val headerHeight = titleHeight + spacing + rowHeight * (if (size.narrow) 2 else 1)
            // The native tall layout places a wrap-content header above a centered button area.
            val headerY = if (size.narrow) top + (h - top - bottom - diameter - headerHeight) / 2 else top
            val buttonsTop = if (size.narrow) h - bottom - diameter
                else top + headerHeight + (h - top - headerHeight - diameter) / 2
            return WidgetControlLayout(s, if (size.narrow) (w - diameter) / 2 else padding,
                buttonsTop, diameter, gap(innerWidth, diameter),
                padding, headerY, innerWidth, titleSize, titleHeight,
                headerY + titleHeight + spacing, rowHeight, minOf(12f * s, rowHeight * .75f), false, size.narrow)
        }
    }
}
