package me.kavishdevar.librepods.presentation.widgets

import android.content.Context
import android.appwidget.AppWidgetManager

data class WidgetPreferences(
    val background: Boolean = true,
    val color: Int = 0, // Follow phone, light, dark.
    val opacity: Int = 1,
    val shape: Int = 0,
    val automatic: Boolean = true,
    val items: List<Int> = listOf(0, 1, 2, 3), // Phone, left, right, case, in display order.
) {
    companion object {
        private fun isControls(context: Context, id: Int) = AppWidgetManager.getInstance(context)
            .getAppWidgetInfo(id)?.provider?.className == NoiseControlWidget::class.java.name
        private fun store(context: Context, controls: Boolean) = context.getSharedPreferences(
            if (controls) "widgets_controls" else "widgets_battery", Context.MODE_PRIVATE)

        fun read(context: Context, id: Int, controls: Boolean = isControls(context, id)): WidgetPreferences {
            val prefs = store(context, controls)
            val phone = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .getBoolean("show_phone_battery_in_widget", true)
            return WidgetPreferences(
                prefs.getBoolean("${id}_background", true),
                prefs.getInt("${id}_color", 0).coerceIn(0, 2),
                prefs.getInt("${id}_opacity", 1).coerceIn(0, 2),
                prefs.getInt("${id}_shape", 0).coerceIn(0, 4),
                prefs.getBoolean("${id}_automatic", true),
                prefs.getString("${id}_items", if (phone) "0,1,2,3" else "1,2,3").orEmpty()
                    .split(',').mapNotNull { it.toIntOrNull()?.takeIf { value -> value in 0..3 } }.distinct()
            )
        }

        fun write(context: Context, id: Int, value: WidgetPreferences, controls: Boolean = isControls(context, id)) {
            store(context, controls).edit().putBoolean("${id}_background", value.background)
                .putInt("${id}_color", value.color).putInt("${id}_opacity", value.opacity)
                .putInt("${id}_shape", value.shape).putBoolean("${id}_automatic", value.automatic)
                .putString("${id}_items", value.items.joinToString(",")).apply()
        }

        fun delete(context: Context, ids: IntArray, controls: Boolean) {
            val prefs = store(context, controls)
            val edit = prefs.edit()
            prefs.all.keys.filter { key -> ids.any { key.startsWith("${it}_") } }.forEach(edit::remove)
            edit.apply()
        }

        fun restore(context: Context, oldIds: IntArray, newIds: IntArray, controls: Boolean) {
            val values = oldIds.map { read(context, it, controls) }
            delete(context, oldIds, controls)
            newIds.zip(values).forEach { (id, value) -> write(context, id, value, controls) }
        }
    }
}

/** The four native home-widget shapes; dimensions come from the host, never a phone model. */
data class WidgetGeometry(val width: Float, val height: Float, val type: Int, val batteryMetrics: WidgetBatteryMetrics? = null,
                          val controlScale: Float? = null, val controlSeamScale: Float = 1f) {
    val short: Boolean get() = type == SMALL || type == WIDE
    val narrow: Boolean get() = type == SMALL || type == MEDIUM
    val columns: Int get() = if (narrow) 2 else 4
    val rows: Int get() = if (short) 1 else 2
    fun resized(width: Float, height: Float) = from(width, height, type).let {
        val factor = minOf(it.width / this.width, it.height / this.height)
        it.copy(batteryMetrics = (batteryMetrics ?: WidgetBatteryMetrics.fallback(this)).scaled(factor),
            controlScale = WidgetControlLayout.create(this, if (narrow) 1 else 4).scale * factor,
            controlSeamScale = controlSeamScale * factor)
    }

    companion object {
        const val SMALL = 2
        const val WIDE = 4
        const val MEDIUM = 8
        const val LARGE = 16
        fun fromBounds(minWidth: Float, minHeight: Float, maxWidth: Float, maxHeight: Float, controls: Boolean = false): List<WidgetGeometry> =
            listOf(from(minWidth, maxHeight, controls = controls), from(maxWidth, minHeight, controls = controls)).distinct()
        fun from(width: Float, height: Float, samsungSize: Int = 0, controls: Boolean = false): WidgetGeometry {
            val w = width.coerceIn(60f, 1000f)
            val h = height.coerceIn(if (controls) 30f else 40f, 800f)
            val short = h < if (controls) 90f else 110f
            val narrow = w < if (controls) 270f else 230f
            val type = samsungSize.takeIf { it in listOf(SMALL, WIDE, MEDIUM, LARGE) }
                ?: if (short) { if (narrow) SMALL else WIDE }
                else { if (narrow) MEDIUM else LARGE }
            return WidgetGeometry(w, h, type)
        }
    }
}
