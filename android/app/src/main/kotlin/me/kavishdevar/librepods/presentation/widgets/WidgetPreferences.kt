package me.kavishdevar.librepods.presentation.widgets

import android.content.Context

data class WidgetPreferences(
    val background: Boolean = true,
    val color: Int = 0, // Follow phone, light, dark.
    val opacity: Int = 1,
    val shape: Int = 0,
) {
    companion object {
        private fun store(context: Context) = context.getSharedPreferences("widgets_controls", Context.MODE_PRIVATE)

        fun read(context: Context, id: Int): WidgetPreferences {
            val prefs = store(context)
            return WidgetPreferences(
                prefs.getBoolean("${id}_background", true),
                prefs.getInt("${id}_color", 0).coerceIn(0, 2),
                prefs.getInt("${id}_opacity", 1).coerceIn(0, 2),
                prefs.getInt("${id}_shape", 0).coerceIn(0, 4),
            )
        }

        fun write(context: Context, id: Int, value: WidgetPreferences) {
            store(context).edit().putBoolean("${id}_background", value.background)
                .putInt("${id}_color", value.color).putInt("${id}_opacity", value.opacity)
                .putInt("${id}_shape", value.shape).apply()
        }

        fun delete(context: Context, ids: IntArray) {
            val prefs = store(context)
            val edit = prefs.edit()
            prefs.all.keys.filter { key -> ids.any { key.startsWith("${it}_") } }.forEach(edit::remove)
            edit.apply()
        }

        fun restore(context: Context, oldIds: IntArray, newIds: IntArray) {
            val values = oldIds.map { read(context, it) }
            delete(context, oldIds)
            newIds.zip(values).forEach { (id, value) -> write(context, id, value) }
        }
    }
}

/** The four native home-widget shapes; dimensions come from the host, never a phone model. */
data class WidgetGeometry(val width: Float, val height: Float, val type: Int,
                          val controlScale: Float? = null, val controlSeamScale: Float = 1f) {
    val short: Boolean get() = type == SMALL || type == WIDE
    val narrow: Boolean get() = type == SMALL || type == MEDIUM
    fun resized(width: Float, height: Float) = from(width, height, type).let {
        val factor = minOf(it.width / this.width, it.height / this.height)
        it.copy(controlScale = WidgetControlLayout.create(this, if (narrow) 1 else 4).scale * factor,
            controlSeamScale = controlSeamScale * factor)
    }

    companion object {
        const val SMALL = 2
        const val WIDE = 4
        const val MEDIUM = 8
        const val LARGE = 16
        fun fromBounds(minWidth: Float, minHeight: Float, maxWidth: Float, maxHeight: Float): List<WidgetGeometry> =
            listOf(from(minWidth, maxHeight), from(maxWidth, minHeight)).distinct()
        fun from(width: Float, height: Float, samsungSize: Int = 0): WidgetGeometry {
            val w = width.coerceIn(60f, 1000f)
            val h = height.coerceIn(30f, 800f)
            val short = h < 90f
            val narrow = w < 270f
            val type = samsungSize.takeIf { it in listOf(SMALL, WIDE, MEDIUM, LARGE) }
                ?: if (short) { if (narrow) SMALL else WIDE }
                else { if (narrow) MEDIUM else LARGE }
            return WidgetGeometry(w, h, type)
        }
    }
}
