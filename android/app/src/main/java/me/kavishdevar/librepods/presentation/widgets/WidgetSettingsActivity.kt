package me.kavishdevar.librepods.presentation.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.services.ServiceManager
import kotlin.math.roundToInt

/** A finite configuration screen: draft changes are previewed locally and only Save persists them. */
class WidgetSettingsActivity : Activity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var draft = WidgetPreferences()
    private var battery = false
    private lateinit var preview: FrameLayout
    private lateinit var deviceRows: LinearLayout
    private val backgroundControls = mutableListOf<View>()
    private val textColor: Int get() = if (dark) Color.WHITE else 0xFF010102.toInt()
    private val dark: Boolean get() = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    private fun dp(value: Float) = (value * resources.displayMetrics.density).roundToInt()
    private fun dp(value: Int) = dp(value.toFloat())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val info = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)
        if (info?.provider?.packageName != packageName || info.provider.className !in listOf(BatteryWidget::class.java.name, NoiseControlWidget::class.java.name)) {
            finish(); return
        }
        battery = info.provider.className == BatteryWidget::class.java.name
        draft = WidgetPreferences.read(this, widgetId, controls = !battery)
        savedInstanceState?.let {
            draft = draft.copy(background = it.getBoolean("background"), color = it.getInt("color"), opacity = it.getInt("opacity"),
                shape = it.getInt("shape"), automatic = it.getBoolean("automatic"), items = it.getIntArray("items")?.toList() ?: draft.items)
        }
        actionBar?.hide()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(if (dark) 0xFF010102.toInt() else 0xFFF5F5F5.toInt())
        }
        root.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(android.view.WindowInsets.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        val header = TextView(this).apply {
            text = getString(if (battery) R.string.widget_battery_name else R.string.widget_controls_name)
            textSize = 26f; setTextColor(textColor); typeface = Typeface.create("sec", Typeface.BOLD)
            setPadding(dp(24), dp(24), dp(24), dp(20))
            isAccessibilityHeading = true
        }
        root.addView(header)
        val scroll = ScrollView(this).apply { isFillViewport = false }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), 0, dp(16), dp(16)) }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        preview = FrameLayout(this).apply { setPadding(dp(8), dp(16), dp(8), dp(16)) }
        content.addView(preview, LinearLayout.LayoutParams(-1, dp(if (battery) 106 else 180)))
        preview.addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
            if (right - left != oldRight - oldLeft) refreshPreview()
        }
        val appearance = section(content, R.string.widget_background)
        toggle(appearance, R.string.widget_background, draft.background) {
            draft = draft.copy(background = it); updateEnabled(); refreshPreview()
        }
        label(appearance, R.string.widget_colors)
        val colors = RadioGroup(this).apply { orientation = RadioGroup.VERTICAL }
        listOf(R.string.widget_follow_phone, R.string.widget_light, R.string.widget_dark).forEachIndexed { index, text ->
            colors.addView(RadioButton(this).apply {
                id = View.generateViewId(); setText(text); setTextColor(textColor); textSize = 16f
                isChecked = draft.color == index; setPadding(dp(8), dp(4), dp(8), dp(4))
                setOnClickListener { draft = draft.copy(color = index); refreshPreview() }
                backgroundControls.add(this)
            })
        }
        appearance.addView(colors)
        label(appearance, R.string.widget_opacity)
        val opacity = SeekBar(this).apply {
            max = 2; progress = draft.opacity; contentDescription = getString(R.string.widget_opacity)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seek: SeekBar?, value: Int, fromUser: Boolean) {
                    if (fromUser) { draft = draft.copy(opacity = value); refreshPreview() }
                }
                override fun onStartTrackingTouch(seek: SeekBar?) = Unit
                override fun onStopTrackingTouch(seek: SeekBar?) = Unit
            })
        }
        appearance.addView(opacity, LinearLayout.LayoutParams(-1, dp(48)))
        backgroundControls.add(opacity)
        val levels = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (percent in if (battery) listOf("30%", "65%", "100%") else listOf("50%", "75%", "100%")) levels.addView(TextView(this).apply {
            text = percent; gravity = Gravity.CENTER; setTextColor(textColor); textSize = 12f
        }, LinearLayout.LayoutParams(0, -2, 1f))
        appearance.addView(levels)
        val geometry = WidgetPublisher.previewGeometry(this, widgetId)
        if (geometry.type == WidgetGeometry.MEDIUM) {
            val shapeCard = section(content, R.string.widget_shape)
            val shapes = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val labels = listOf(R.string.widget_shape_round, R.string.widget_shape_leaf_left, R.string.widget_shape_leaf_right,
                R.string.widget_shape_corner_right, R.string.widget_shape_corner_left)
            val drawables = listOf(R.drawable.widget_shape_0, R.drawable.widget_shape_1, R.drawable.widget_shape_2,
                R.drawable.widget_shape_3, R.drawable.widget_shape_4)
            val choices = mutableListOf<TextView>()
            fun selectShape() {
                choices.forEachIndexed { index, view ->
                    view.text = if (draft.shape == index) "✓" else ""
                    view.isSelected = draft.shape == index
                    view.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        if (draft.shape == index) 0xFF505052.toInt() else 0xFFDEDEE1.toInt())
                }
            }
            labels.forEachIndexed { index, label ->
                val choice = TextView(this).apply {
                    contentDescription = getString(label); textSize = 24f; gravity = Gravity.CENTER
                    setTextColor(Color.WHITE); setBackgroundResource(drawables[index]); isFocusable = true
                    setOnClickListener { draft = draft.copy(shape = index); selectShape(); refreshPreview() }
                }
                choices.add(choice)
                shapes.addView(choice, LinearLayout.LayoutParams(0, dp(48), 1f).apply { setMargins(dp(3), dp(8), dp(3), dp(8)) })
            }
            selectShape()
            shapeCard.addView(shapes)
        }
        if (battery) {
            val devices = section(content, R.string.widget_devices)
            toggle(devices, R.string.widget_automatic_devices, draft.automatic) {
                draft = draft.copy(automatic = it); refreshPreview()
            }
            deviceRows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            devices.addView(deviceRows)
            rebuildDeviceRows()
        }
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(16), dp(8), dp(16), dp(12)) }
        actions.addView(Button(this).apply { setText(android.R.string.cancel); setOnClickListener { finish() } }, LinearLayout.LayoutParams(0, dp(52), 1f))
        actions.addView(Button(this).apply {
            setText(R.string.widget_save)
            setOnClickListener {
                WidgetPreferences.write(this@WidgetSettingsActivity, widgetId, draft, controls = !battery)
                ServiceManager.getService()?.synchronizeWidgetBatteryReceiver()
                WidgetPublisher.update(this@WidgetSettingsActivity, forceIds = setOf(widgetId))
                setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                finish()
            }
        }, LinearLayout.LayoutParams(0, dp(52), 1f))
        root.addView(actions)
        setContentView(root)
        updateEnabled()
    }

    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        out.putBoolean("background", draft.background); out.putInt("color", draft.color)
        out.putInt("opacity", draft.opacity); out.putInt("shape", draft.shape)
        out.putBoolean("automatic", draft.automatic); out.putIntArray("items", draft.items.toIntArray())
    }

    private fun section(parent: LinearLayout, title: Int): LinearLayout {
        label(parent, title)
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(10), dp(16), dp(14))
            background = GradientDrawable().apply { setColor(if (dark) 0xFF171719.toInt() else Color.WHITE); cornerRadius = dp(24).toFloat() }
            parent.addView(this, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        }
    }
    private fun label(parent: LinearLayout, title: Int) {
        parent.addView(TextView(this).apply { setText(title); setTextColor(textColor); textSize = 14f; setPadding(dp(8), dp(12), dp(8), dp(8)) })
    }
    private fun toggle(parent: LinearLayout, label: Int, checked: Boolean, changed: (Boolean) -> Unit) {
        parent.addView(Switch(this).apply {
            setText(label); setTextColor(textColor); textSize = 16f; isChecked = checked; minHeight = dp(52)
            setOnCheckedChangeListener { _, value -> changed(value) }
        }, LinearLayout.LayoutParams(-1, -2))
    }
    private fun updateEnabled() { backgroundControls.forEach { it.isEnabled = draft.background; it.alpha = if (draft.background) 1f else .4f } }

    private fun rebuildDeviceRows() {
        deviceRows.removeAllViews()
        val labels = listOf(R.string.widget_phone, R.string.widget_left, R.string.widget_right, R.string.widget_case)
        val order = draft.items + (0..3).filter { it !in draft.items }
        order.forEach { item ->
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            row.addView(CheckBox(this).apply {
                setText(labels[item]); setTextColor(textColor); isChecked = item in draft.items; minHeight = dp(48)
                setOnCheckedChangeListener { _, enabled ->
                    draft = draft.copy(items = if (enabled) draft.items + item else draft.items - item)
                    rebuildDeviceRows(); refreshPreview()
                }
            }, LinearLayout.LayoutParams(0, -2, 1f))
            for (delta in listOf(-1, 1)) row.addView(Button(this).apply {
                text = if (delta < 0) "↑" else "↓"
                contentDescription = getString(if (delta < 0) R.string.widget_move_up else R.string.widget_move_down, getString(labels[item]))
                val index = draft.items.indexOf(item)
                isEnabled = index >= 0 && index + delta in draft.items.indices
                setOnClickListener {
                    val moved = draft.items.toMutableList()
                    java.util.Collections.swap(moved, moved.indexOf(item), moved.indexOf(item) + delta)
                    draft = draft.copy(items = moved); rebuildDeviceRows(); refreshPreview()
                }
            }, LinearLayout.LayoutParams(dp(48), dp(48)))
            deviceRows.addView(row)
        }
    }

    private fun refreshPreview() {
        val width = (preview.width - preview.paddingLeft - preview.paddingRight) / resources.displayMetrics.density
        if (width <= 0) return
        val source = WidgetPublisher.previewGeometry(this, widgetId)
        val height = width * source.height / source.width
        preview.layoutParams = preview.layoutParams.apply { this.height = dp(height) + preview.paddingTop + preview.paddingBottom }
        val views = WidgetPublisher.preview(this, widgetId, draft, width, height)
        preview.removeAllViews()
        val view = views.apply(this, preview)
        WidgetSurface.applyPreviewOutline(view, AppWidgetManager.getInstance(this).getAppWidgetOptions(widgetId),
            source, draft.shape, minOf(width / source.width, height / source.height))
        // A preview must never send a real mode command or open the application.
        fun disableActions(node: View) {
            node.setOnClickListener(null); node.isClickable = false; node.isFocusable = false
            if (node is ViewGroup) for (i in 0 until node.childCount) disableActions(node.getChildAt(i))
        }
        disableActions(view)
        preview.addView(view, FrameLayout.LayoutParams(-1, -1))
    }
}
