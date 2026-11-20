@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package me.kavishdevar.librepods.presentation.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.Icon
import android.os.Bundle
import android.util.LruCache
import android.util.SizeF
import android.util.TypedValue
import android.view.View
import android.view.Gravity
import android.widget.RemoteViews
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import me.kavishdevar.librepods.presentation.activities.MainActivity
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.core.bluetooth.MacAddress

/** Complete, event-driven views. Geometry/appearance belong to each widget, not the service. */
object WidgetPublisher {
    private val published = mutableMapOf<Int, Publication>()
    private val controlGlyphs = LruCache<Pair<Int, Int>, Bitmap>(4)
    private data class ControlMask(val diameter: Float, val link: Float, val seam: Float,
                                   val join: Int, val active: Boolean, val color: Int)
    private val controlMasks = LruCache<ControlMask, Bitmap>(12)
    // The source Glance TextViews enable linear/subpixel text; weight and family alone differ in width.
    private val controlTextFlags = Paint().flags or Paint.ANTI_ALIAS_FLAG or Paint.LINEAR_TEXT_FLAG or Paint.SUBPIXEL_TEXT_FLAG
    private val buttons = intArrayOf(R.id.widget_off_button, R.id.widget_transparency_button, R.id.widget_adaptive_button, R.id.widget_anc_button)
    private val backgrounds = intArrayOf(R.id.widget_off_background, R.id.widget_transparency_background, R.id.widget_adaptive_background, R.id.widget_anc_background)
    private val glyphs = intArrayOf(R.id.widget_off_icon, R.id.widget_transparency_icon, R.id.widget_adaptive_icon, R.id.widget_anc_icon)
    private val links = intArrayOf(R.id.widget_link_0, R.id.widget_link_1, R.id.widget_link_2)
    private val modes = listOf(1, 3, 4, 2)
    private data class Palette(val foreground: Int, val surface: Int, val idle: Int)
    private data class Appearance(val day: Palette, val night: Palette)
    private data class Publication(val state: WidgetState, val prefs: WidgetPreferences, val appearance: Appearance,
                                   val sizes: List<WidgetGeometry>, val locale: String, val hostOwnsOutline: Boolean)

    fun forget(ids: IntArray) {
        ids.forEach { published.remove(it) }
    }

    fun update(context: Context, forceIds: Set<Int> = emptySet()) {
        val manager = AppWidgetManager.getInstance(context)
        val installed = manager.getAppWidgetIds(ComponentName(context, NoiseControlWidget::class.java)).toSet()
        if (installed.isEmpty()) return
        published.keys.retainAll(installed)
        val states = mutableMapOf<MacAddress?, WidgetState>()
        val locale = context.resources.configuration.locales.toLanguageTags()
        installed.forEach { id ->
            val state = states.getOrPut(WidgetDataSource.address(context, id)) {
                WidgetDataSource.state(context, id)
            }
            val prefs = WidgetPreferences.read(context, id)
            val options = manager.getAppWidgetOptions(id)
            val sizes = sizes(options)
            val colors = appearance(prefs)
            val hostOwnsOutline = WidgetSurface.hostOwnsOutline(options)
            val key = Publication(state, prefs, colors, sizes, locale, hostOwnsOutline)
            if (id in forceIds || published[id] != key) {
                val variants = sizes.associate { size -> SizeF(size.width, size.height) to
                    render(context, state, prefs, colors, size, hostOwnsOutline, id) }
                manager.updateAppWidget(id, if (variants.size == 1) variants.values.first() else RemoteViews(variants))
                published[id] = key
            }
        }
    }

    private fun sizes(options: Bundle): List<WidgetGeometry> {
        val sizes = options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, SizeF::class.java)
            ?.filter { it.width.isFinite() && it.height.isFinite() && it.width > 0 && it.height > 0 }
            ?.distinct()?.take(16).orEmpty()
        if (sizes.isNotEmpty()) return sizes.map {
            WidgetGeometry.from(it.width, it.height, if (sizes.size == 1) options.getInt("semWidgetSize", 0) else 0)
        }
        // The two minima can belong to different orientations. Never combine them into one size.
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 270).coerceAtLeast(60).toFloat()
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 130).coerceAtLeast(30).toFloat()
        return WidgetGeometry.fromBounds(minWidth, minHeight,
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minWidth.toInt()).toFloat().coerceAtLeast(minWidth),
            options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minHeight.toInt()).toFloat().coerceAtLeast(minHeight))
    }

    fun previewGeometry(context: Context, id: Int): WidgetGeometry {
        val manager = AppWidgetManager.getInstance(context)
        val options = manager.getAppWidgetOptions(id)
        val variants = sizes(options)
        return if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
            variants.maxBy { it.width } else variants.minBy { it.width }
    }

    /** Configuration preview uses the same renderer, without writing preferences or posting widgets. */
    fun preview(context: Context, id: Int, prefs: WidgetPreferences, width: Float, height: Float, address: MacAddress? = null): RemoteViews {
        val geometry = previewGeometry(context, id).resized(width, height)
        return render(context, WidgetDataSource.state(context, id, address), prefs, appearance(prefs), geometry, widgetId = id)
    }

    private fun appearance(prefs: WidgetPreferences): Appearance {
        fun palette(dark: Boolean): Palette {
            val levels = intArrayOf(128, 191, 255)
            val alpha = if (prefs.background) levels[prefs.opacity] else 0
            return Palette(if (dark) Color.WHITE else Color.BLACK,
                (alpha shl 24) or if (dark) 0x010102 else 0xFCFCFF,
                if (dark) 0x26FCFCFF else 0x26000000)
        }
        // RemoteViews resolves these in the host's configuration, even with our process absent.
        return Appearance(palette(prefs.color == 2), palette(prefs.color != 1))
    }

    private fun render(context: Context, state: WidgetState, prefs: WidgetPreferences, colors: Appearance,
                       size: WidgetGeometry, hostOwnsOutline: Boolean = false, widgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID): RemoteViews {
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return RemoteViews(context.packageName, if (size.short) R.layout.noise_control_widget_compact
            else if (size.narrow) R.layout.noise_control_widget_medium else R.layout.noise_control_widget).apply {
            WidgetSurface.apply(this, size, prefs.shape, hostOwnsOutline)
            setColorStateList(R.id.widget_frame, "setBackgroundTintList", ColorStateList.valueOf(Color.TRANSPARENT))
            // Give One UI the actual background alpha, not a tint multiplied by a shape's alpha.
            // On supported One UI hosts the transparent outer shape conveys corners without clipping.
            setColorInt(android.R.id.background, "setBackgroundColor", colors.day.surface, colors.night.surface)
            setOnClickPendingIntent(android.R.id.background, open)
            controlViews(context, state, colors, size, open, widgetId)
        }
    }

    private fun RemoteViews.box(id: Int, width: Float, height: Float, x: Float? = null, y: Float? = null) {
        setViewLayoutWidth(id, width, if (width < 0) TypedValue.COMPLEX_UNIT_PX else TypedValue.COMPLEX_UNIT_DIP)
        setViewLayoutHeight(id, height, if (height < 0) TypedValue.COMPLEX_UNIT_PX else TypedValue.COMPLEX_UNIT_DIP)
        x?.let { setViewLayoutMargin(id, RemoteViews.MARGIN_START, it, TypedValue.COMPLEX_UNIT_DIP) }
        y?.let { setViewLayoutMargin(id, RemoteViews.MARGIN_TOP, it, TypedValue.COMPLEX_UNIT_DIP) }
    }
    private fun RemoteViews.tint(id: Int, day: Int, night: Int = day) =
        setColorStateList(id, "setImageTintList", ColorStateList.valueOf(day), ColorStateList.valueOf(night))

    private fun RemoteViews.controlPadding(context: Context, id: Int, start: Float, top: Float,
                                           end: Float, bottom: Float) {
        val density = context.resources.displayMetrics.density
        val rtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
        // The source Glance translator truncates dp padding independently before measuring children.
        setViewPadding(id, ((if (rtl) end else start) * density).toInt(), (top * density).toInt(),
            ((if (rtl) start else end) * density).toInt(), (bottom * density).toInt())
    }

    private data class ControlBatteryText(val text: String, val charging: Boolean = false, val low: Boolean = false,
                                          val divider: Boolean = false)

    private fun RemoteViews.controlBatteries(context: Context, state: WidgetState, colors: Appearance,
                                            layout: WidgetControlLayout) {
        fun entry(label: String, battery: WidgetBattery) = battery.level?.let {
            ControlBatteryText("$label $it%", battery.charging, it < 10)
        }
        val left = entry("L", state.left)
        val right = entry("R", state.right)
        val case = entry(context.getString(R.string.widget_case), state.case)
        val ears = listOfNotNull(left, right)
        val separator = ControlBatteryText(" • ")
        val combined = left != null && right != null && state.left == state.right
        val tallEars = if (combined) listOf(ControlBatteryText("L • ", left.charging), right)
            else ears.flatMapIndexed { i, item -> if (i == 0) listOf(item) else listOf(separator, item) }
        val rows = when {
            layout.compact && combined -> listOf(
                listOf(ControlBatteryText("L • ", left.charging), ControlBatteryText("R", right.charging)),
                listOf(ControlBatteryText("${state.left.level}%", low = left.low)))
            layout.compact -> ears.map { listOf(it) }
            layout.centered -> listOfNotNull(tallEars.takeIf { it.isNotEmpty() }, case?.let { listOf(it) })
            else -> listOf(buildList {
                addAll(tallEars)
                case?.let { if (isNotEmpty()) add(ControlBatteryText("", divider = true)); add(it) }
            }).filter { it.isNotEmpty() }
        }.ifEmpty { listOf(listOf(ControlBatteryText(context.getString(
            if (state.connected) R.string.widget_receiving else R.string.widget_disconnected)))) }
        val rowHeight = layout.batteryRowHeight
        // One known earbud is vertically centered in the same compact two-row slot.
        if (!layout.compact) box(R.id.widget_control_batteries, MATCH_PARENT.toFloat(), WRAP_CONTENT.toFloat())
        removeAllViews(R.id.widget_control_batteries)
        val paint = Paint().apply {
            flags = controlTextFlags
            typeface = Typeface.create(Typeface.create("sec", Typeface.NORMAL), if (layout.compact) 600 else 400, false)
            textSize = layout.batterySize
        }
        rows.forEach { entries ->
            val naturalWidth = entries.sumOf { item ->
                (if (item.divider) 9f * layout.scale else paint.measureText(item.text) +
                    if (item.charging) 11f * layout.scale else 0f).toDouble()
            }.toFloat()
            val fit = minOf(1f, layout.headerWidth / naturalWidth.coerceAtLeast(1f))
            val row = RemoteViews(context.packageName, R.layout.widget_control_battery_row).apply {
                box(R.id.widget_control_battery_row, if (layout.compact) layout.headerWidth else MATCH_PARENT.toFloat(), WRAP_CONTENT.toFloat())
                setInt(R.id.widget_control_battery_row, "setGravity", Gravity.CENTER_VERTICAL or
                    if (layout.centered) Gravity.CENTER_HORIZONTAL else Gravity.START)
                entries.forEach { item ->
                    val child = if (item.divider) RemoteViews(context.packageName, R.layout.widget_control_battery_divider).apply {
                        box(R.id.widget_control_divider, layout.scale * fit, minOf(8f * layout.scale * fit, rowHeight))
                        setViewLayoutMargin(R.id.widget_control_divider, RemoteViews.MARGIN_START, 4f * layout.scale * fit, TypedValue.COMPLEX_UNIT_DIP)
                        setViewLayoutMargin(R.id.widget_control_divider, RemoteViews.MARGIN_END, 4f * layout.scale * fit, TypedValue.COMPLEX_UNIT_DIP)
                        tint(R.id.widget_control_divider, colors.day.foreground, colors.night.foreground)
                    } else RemoteViews(context.packageName, if (layout.compact) R.layout.widget_control_battery_compact_item
                        else R.layout.widget_control_battery_item).apply {
                        val low = 0xFFFF5941.toInt()
                        setTextViewText(R.id.widget_control_value, item.text)
                        setInt(R.id.widget_control_value, "setPaintFlags", controlTextFlags)
                        setTextViewTextSize(R.id.widget_control_value, TypedValue.COMPLEX_UNIT_DIP, layout.batterySize * fit)
                        setColorInt(R.id.widget_control_value, "setTextColor", if (item.low) low else colors.day.foreground,
                            if (item.low) low else colors.night.foreground)
                        setViewVisibility(R.id.widget_control_charge, if (item.charging) View.VISIBLE else View.GONE)
                        box(R.id.widget_control_charge, 11f * layout.scale * fit, minOf(16f * layout.scale * fit, rowHeight))
                        tint(R.id.widget_control_charge, if (item.low) low else colors.day.foreground,
                            if (item.low) low else colors.night.foreground)
                        if (item.charging) setContentDescription(R.id.widget_control_value,
                            context.getString(R.string.widget_charging_description, item.text))
                    }
                    addView(R.id.widget_control_battery_row, child)
                }
            }
            addView(R.id.widget_control_batteries, row)
        }
    }

    private fun RemoteViews.controlViews(context: Context, state: WidgetState, colors: Appearance,
                                         size: WidgetGeometry, open: PendingIntent, widgetId: Int) {
        setOnClickPendingIntent(R.id.noise_control_widget, open)
        setOnClickPendingIntent(R.id.widget_device_name, open)
        setOnClickPendingIntent(R.id.widget_control_batteries, open)
        val single = size.narrow
        val available = state.availableModes
        val displayed = if (single) listOf(state.mode?.takeIf { it in available } ?: available.first()) else available
        val layout = WidgetControlLayout.create(size, displayed.size)
        val scale = layout.scale
        val diameter = layout.diameter
        val gap = layout.gap
        if (size.short) {
            controlPadding(context, R.id.noise_control_widget, 8f * scale, size.height * .125f,
                9f * scale, size.height * .125f)
            // Small uses a measured spacer; wide uses padding. Dimensions round, padding truncates.
            box(R.id.widget_control_gap, if (single) 6f * scale else 0f, 0f)
            val batteryPadding = if (single) 0f else (size.width - 16f * scale) * .022f
            controlPadding(context, R.id.widget_control_batteries, batteryPadding, 0f, 0f, 0f)
            box(R.id.widget_control_buttons, WRAP_CONTENT.toFloat(), diameter)
        } else {
            // Glance measures padded columns and text before distributing their remaining space.
            controlPadding(context, R.id.noise_control_widget, 12f * scale,
                if (single) size.height * .052f else 12f * scale, 12f * scale,
                if (single) size.height * .067f else 0f)
            setViewVisibility(R.id.widget_control_top_space, if (single) View.VISIBLE else View.GONE)
            setViewVisibility(R.id.widget_control_bottom_space, if (single) View.VISIBLE else View.GONE)
            box(R.id.widget_control_title_gap, 0f, (if (single) 1f else 2f) * scale)
            if (single) box(R.id.widget_control_button_area, MATCH_PARENT.toFloat(), diameter)
            box(R.id.widget_control_buttons, if (single) WRAP_CONTENT.toFloat() else MATCH_PARENT.toFloat(), diameter)
        }
        setViewVisibility(R.id.widget_device_name, if (size.short) View.GONE else View.VISIBLE)
        setTextViewText(R.id.widget_device_name, state.name.ifBlank { "AirPods" })
        setInt(R.id.widget_device_name, "setPaintFlags", controlTextFlags)
        setTextViewTextSize(R.id.widget_device_name, TypedValue.COMPLEX_UNIT_DIP, layout.titleSize)
        if (size.short) box(R.id.widget_device_name, layout.headerWidth, layout.titleHeight, layout.headerX, layout.headerY)
        else box(R.id.widget_device_name, if (single) WRAP_CONTENT.toFloat() else MATCH_PARENT.toFloat(), WRAP_CONTENT.toFloat())
        setInt(R.id.widget_device_name, "setGravity", Gravity.CENTER_VERTICAL or if (layout.centered) Gravity.CENTER_HORIZONTAL else Gravity.START)
        setColorInt(R.id.widget_device_name, "setTextColor", colors.day.foreground, colors.night.foreground)
        controlBatteries(context, state, colors, layout)
        val labels = listOf(R.string.off, R.string.transparency, R.string.adaptive, R.string.noise_cancellation)
        modes.forEachIndexed { index, mode ->
            val position = displayed.indexOf(mode)
            setViewVisibility(buttons[index], if (position >= 0) View.VISIBLE else View.GONE)
            if (position < 0) return@forEachIndexed
            box(buttons[index], diameter, diameter)
            val active = state.connected && state.mode == mode
            val selected = context.getColor(R.color.widget_control_selected)
            val logicalJoin = if (active || single || gap <= 0f) 0 else when (position) {
                0 -> 2
                displayed.lastIndex -> 3
                else -> 1
            }
            val join = if (context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL)
                when (logicalJoin) { 2 -> 3; 3 -> 2; else -> logicalJoin } else logicalJoin
            setInt(backgrounds[index], "setBackgroundResource", 0)
            // Bake color before rasterization: tinting a white mask changes premultiplied edges.
            // Day/night Icons keep appearance updates in the host, without waking our process.
            setColorStateList(backgrounds[index], "setImageTintList", null)
            setIcon(backgrounds[index], "setImageIcon",
                Icon.createWithBitmap(controlMask(context, diameter, 6f * scale, size.controlSeamScale,
                    join, active, if (active) selected else colors.day.idle or Color.BLACK)),
                Icon.createWithBitmap(controlMask(context, diameter, 6f * scale, size.controlSeamScale,
                    join, active, if (active) selected else colors.night.idle or Color.BLACK)))
            tint(glyphs[index], Color.WHITE)
            setImageViewBitmap(glyphs[index], controlGlyph(context, index))
            // Older publications dimmed the entire button; reset it when the host reapplies views.
            setFloat(buttons[index], "setAlpha", 1f)
            setFloat(glyphs[index], "setAlpha", if (state.connected) 1f else .4f)
            setBoolean(buttons[index], "setEnabled", state.connected)
            val label = context.getString(labels[index])
            setContentDescription(buttons[index], if (single && state.connected) context.getString(R.string.widget_cycle_description, label)
                else context.getString(if (!state.connected) R.string.widget_control_unavailable else if (active) R.string.widget_control_selected else R.string.widget_control_action, label))
            val action = Intent(context, NoiseControlWidget::class.java)
                .setAction(if (single) NoiseControlWidget.ACTION_CYCLE else NoiseControlWidget.ACTION_MODE)
                .putExtra(NoiseControlWidget.EXTRA_MODE, mode)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                .setData(android.net.Uri.parse("librepods://widget/$widgetId/$mode"))
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            setOnClickPendingIntent(buttons[index], if (state.connected) PendingIntent.getBroadcast(context, if (single) 10 else mode, action,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) else open)
        }
        links.forEachIndexed { i, id ->
            val position = displayed.indexOf(modes[i])
            val showing = !single && position >= 0 && position < displayed.lastIndex && gap > 0f
            setViewVisibility(id, if (showing) View.VISIBLE else View.GONE)
            if (showing) {
                box(id, if (size.short) gap else 0f, 6f * scale)
                tint(id, colors.day.idle, colors.night.idle)
            }
        }
    }

    /** Rasterize inset upstream artwork; the shared drawables also size static previews. */
    private fun controlGlyph(context: Context, index: Int): Bitmap {
        val resource = intArrayOf(R.drawable.widget_glyph_noise_cancellation, R.drawable.widget_glyph_transparency,
            R.drawable.widget_glyph_adaptive, R.drawable.widget_glyph_noise_cancellation)[index]
        val key = resource to context.resources.displayMetrics.densityDpi
        return controlGlyphs.get(key) ?: requireNotNull(context.getDrawable(resource)).let { drawable ->
            val edge = (80f * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
            Bitmap.createBitmap(edge, edge, Bitmap.Config.ARGB_8888).also { bitmap ->
                val ratio = minOf(edge.toFloat() / drawable.intrinsicWidth.coerceAtLeast(1),
                    edge.toFloat() / drawable.intrinsicHeight.coerceAtLeast(1))
                val width = (drawable.intrinsicWidth.coerceAtLeast(1) * ratio).toInt().coerceAtLeast(1)
                val height = (drawable.intrinsicHeight.coerceAtLeast(1) * ratio).toInt().coerceAtLeast(1)
                val left = (edge - width) / 2
                val top = (edge - height) / 2
                drawable.setBounds(left, top, left + width, top + height)
                drawable.draw(Canvas(bitmap))
                controlGlyphs.put(key, bitmap)
            }
        }
    }

    /** Joined circles reserve half/full seams at row ends/middle; avoid double-alpha intersections. */
    private fun controlMask(context: Context, diameter: Float, link: Float, seam: Float,
                            join: Int, active: Boolean, color: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val key = ControlMask(diameter * density, link * density, seam * density, join, active, color)
        return controlMasks.get(key) ?: run {
            val extension = when (join) { 0 -> 0f; 1 -> key.seam; else -> key.seam / 2 }
            Bitmap.createBitmap((key.diameter + extension).toInt().coerceAtLeast(1),
                key.diameter.toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888).also { bitmap ->
                val canvas = Canvas(bitmap)
                val paint = Paint().apply { this.color = color; alpha = if (active) 255 else 38 }
                if (join == 0) {
                    paint.isAntiAlias = true
                    canvas.drawRoundRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(),
                        key.diameter / 2, key.diameter / 2, paint)
                } else {
                    val radius = key.diameter / 2
                    val centerX = when (join) { 2 -> radius; 3 -> bitmap.width - radius; else -> bitmap.width / 2f }
                    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OVER)
                    canvas.drawRect(if (join == 2) radius else 0f, (bitmap.height - key.link) / 2,
                        if (join == 3) bitmap.width - radius else bitmap.width.toFloat(),
                        (bitmap.height + key.link) / 2, paint)
                    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                    canvas.drawCircle(centerX, bitmap.height / 2f, radius, paint)
                    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OVER)
                    canvas.drawCircle(centerX, bitmap.height / 2f, radius, paint)
                }
                controlMasks.put(key, bitmap)
            }
        }
    }
}
