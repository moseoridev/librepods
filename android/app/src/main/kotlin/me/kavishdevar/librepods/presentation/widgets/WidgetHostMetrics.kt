package me.kavishdevar.librepods.presentation.widgets

import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.util.LruCache
import android.util.SizeF
import kotlin.math.abs

/** Optional One UI sizing contract, also used by Samsung's Glance widgets. No polling. */
internal object WidgetHostMetrics {
    private val tables = LruCache<Int, List<Bundle>>(12)
    private val query by lazy {
        runCatching { AppWidgetManager::class.java.getMethod("hidden_semGetAppWidgetSizeInfo", Int::class.javaPrimitiveType) }.getOrNull()
    }

    fun forget(ids: IntArray) { ids.forEach(tables::remove) }

    fun apply(manager: AppWidgetManager, id: Int, options: Bundle, sizes: List<WidgetGeometry>, refresh: Boolean): List<WidgetGeometry> {
        if (!options.containsKey("semWidgetSize")) return sizes
        if (refresh) tables.remove(id)
        val entries = tables.get(id) ?: run {
            val result = runCatching { (query?.invoke(manager, id) as? List<*>)?.filterIsInstance<Bundle>()?.take(64) }.getOrNull().orEmpty()
            tables.put(id, result)
            result
        }
        if (entries.isEmpty()) return sizes
        // A host table can split font/padding/row sizes into separate bundles.
        var host = 0
        val groups = entries.groupBy { entry ->
            host = entry.getInt("hostKey", host)
            host to entry.getBoolean("isPortrait", true)
        }.values
        return sizes.map { size ->
            val group = groups.minByOrNull { group ->
                group.filter { it.getInt("semAppWidgetRowSpan") == size.rows }
                    .mapNotNull { it.getParcelable("appWidgetSizes", SizeF::class.java)?.height }
                    .minOfOrNull { abs(it - size.height) } ?: Float.MAX_VALUE
            } ?: return@map size
            fun array(key: String, count: Int) = group.firstNotNullOfOrNull { it.getFloatArray(key) }
                ?.takeIf { it.size == count && it.all { value -> value.isFinite() && value > 0f && value < 400f } }
            val pads = array("semPaddingSizes", 7) ?: return@map size
            val fonts = array("semFontSizes", 20) ?: return@map size
            size.copy(batteryMetrics = WidgetBatteryMetrics(pads[2], if (size.short) pads[5] else pads[2], pads[6], fonts.toList()))
        }
    }
}

data class WidgetBatteryMetrics(val horizontal: Float, val vertical: Float, val gap: Float, val fonts: List<Float>) {
    fun scaled(factor: Float) = WidgetBatteryMetrics(horizontal * factor, vertical * factor, gap * factor, fonts.map { it * factor })

    companion object {
        fun fallback(size: WidgetGeometry): WidgetBatteryMetrics {
            // One UI's documented-in-code fallback ratios use one row's height.
            val unit = if (size.short) size.height else size.height / 2.53f
            val ratios = listOf(.67f, .627f, .582f, .4775f, .448f, .403f, .39f, .358f, .3285f, .3f,
                .27f, .24f, .225f, .21f, .195f, .18f, .165f, .15f, .135f, .12f)
            return WidgetBatteryMetrics(unit * .12f, unit * (if (size.short) .06f else .12f), unit * .03f, ratios.map { it * unit })
        }
    }
}
