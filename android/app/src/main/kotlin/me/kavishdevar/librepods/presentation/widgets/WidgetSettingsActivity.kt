package me.kavishdevar.librepods.presentation.widgets

import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import me.kavishdevar.librepods.LibrePodsApplication
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.core.bluetooth.MacAddress
import me.kavishdevar.librepods.database.widget.WidgetConfigEntity
import me.kavishdevar.librepods.ui.components.SettingsSeekBar
import me.kavishdevar.librepods.ui.components.SettingsSwitch
import me.kavishdevar.librepods.ui.theme.SettingsStyle
import me.kavishdevar.librepods.ui.theme.SettingsTheme
import kotlin.math.roundToInt

/**
 * Buds Manager 9.0.10.1724's WidgetSettingActivityUI85 for one controls widget. Changes are a
 * draft previewed locally; only Save persists them.
 */
class WidgetSettingsActivity : ComponentActivity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var draft by mutableStateOf(WidgetPreferences())
    private var deviceAddress: MacAddress? = null
    private lateinit var initialPreferences: WidgetPreferences
    private var initialAddress: MacAddress? = null

    private lateinit var previewHole: FrameLayout
    private lateinit var fadeoutLayout: View
    private var shapeSection: View? = null
    private lateinit var colorSummary: TextView
    private var deviceSummary: TextView? = null
    private val shapeViews = mutableListOf<WidgetShapeView>()
    private var popup: PopupWindow? = null

    private val dark get() = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    private val known by lazy { (application as LibrePodsApplication).appleRepository.devices.value.values.toList() }
    private val colorLabels by lazy { listOf(R.string.widget_follow_phone, R.string.widget_light, R.string.widget_dark).map(::getString) }
    private fun px(id: Int) = resources.getDimensionPixelSize(id)
    private fun color(id: Int) = getColor(id)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val info = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)
        if (info?.provider?.packageName != packageName || info.provider.className != NoiseControlWidget::class.java.name) {
            finish(); return
        }
        initialPreferences = WidgetPreferences.read(this, widgetId)
        initialAddress = WidgetDataSource.address(this, widgetId)
        draft = initialPreferences
        deviceAddress = initialAddress
        savedInstanceState?.let { state ->
            state.getString("device_address")?.let { deviceAddress = runCatching { MacAddress(it) }.getOrNull() }
            draft = draft.copy(background = state.getBoolean("background"), color = state.getInt("color"),
                opacity = state.getInt("opacity"), shape = state.getInt("shape"))
        }
        window.setDecorFitsSystemWindows(false)
        setContentView(content())
        bind()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (draft == initialPreferences && deviceAddress == initialAddress) finish() else confirmDiscard()
            }
        })
    }

    override fun onDestroy() {
        popup?.dismiss()
        super.onDestroy()
    }

    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        deviceAddress?.let { out.putString("device_address", it.value) }
        out.putBoolean("background", draft.background); out.putInt("color", draft.color)
        out.putInt("opacity", draft.opacity); out.putInt("shape", draft.shape)
    }

    private fun update(value: WidgetPreferences) {
        if (value == draft) return
        draft = value
        bind()
    }

    private fun bind() {
        // F(): the background switch hides the dependent rows instead of disabling them.
        fadeoutLayout.visibility = if (draft.background) View.VISIBLE else View.GONE
        shapeSection?.visibility = if (draft.background) View.VISIBLE else View.GONE
        colorSummary.text = colorLabels[draft.color]
        deviceSummary?.text = deviceName(known.firstOrNull { it.macAddress == deviceAddress } ?: known.first())
        shapeViews.forEach { it.isSelected = it.tag == draft.shape; it.invalidate() }
        refreshPreview()
    }

    private fun deviceName(device: me.kavishdevar.librepods.database.apple.AppleEntity) =
        device.metadata.name.ifBlank { device.metadata.modelNumber.ifBlank { "AirPods" } }

    private fun save() {
        deviceAddress?.let { address ->
            (application as LibrePodsApplication).widgetConfigRepository.setWidgetConfig(WidgetConfigEntity(widgetId, address))
        }
        WidgetPreferences.write(this, widgetId, draft)
        WidgetPublisher.update(this, forceIds = setOf(widgetId))
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        finish()
    }

    /** The AppCompat AlertDialog of `androidx.appcompat.app.b0` case 3; One UI renders the platform one alike. */
    private fun confirmDiscard() {
        AlertDialog.Builder(this)
            .setMessage(R.string.widget_unsaved_changes)
            .setNeutralButton(R.string.widget_dialog_cancel, null)
            .setNegativeButton(R.string.widget_discard_changes) { _, _ -> finish() }
            .setPositiveButton(R.string.widget_save) { _, _ -> save() }
            .show()
    }

    private fun text(value: CharSequence, size: Int, color: Int, weight: Int = 400) = TextView(this).apply {
        text = value
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(size))
        setTextColor(color)
        typeface = Typeface.create(Typeface.create("sec", Typeface.NORMAL), weight, false)
    }

    private fun ripple() = RippleDrawable(ColorStateList.valueOf(color(R.color.widget_settings_pressed)), null, ColorDrawable(0xFFFFFFFF.toInt()))

    private fun divider() = View(this).apply { setBackgroundColor(color(R.color.widget_settings_divider)) }.also {
        it.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, px(R.dimen.widget_settings_divider)).apply {
            marginStart = px(R.dimen.widget_settings_divider_padding); marginEnd = marginStart
        }
    }

    private fun content(): View {
        val page = color(R.color.widget_settings_page)
        val radius = resources.getDimension(R.dimen.widget_settings_corner_radius)
        val root = WidgetSettingsPage(this, page)
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(column, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

        // Collapsed SESL toolbar.
        val toolbar = FrameLayout(this)
        toolbar.addView(text(getString(R.string.widget_settings_title), R.dimen.widget_settings_title_size,
            color(R.color.widget_settings_text), 700).apply {
            isAccessibilityHeading = true
            maxLines = 1; ellipsize = TextUtils.TruncateAt.END
            setPaddingRelative(px(R.dimen.widget_settings_toolbar_inset), 0, px(R.dimen.widget_settings_toolbar_inset), 0)
            translationY = resources.getDimension(R.dimen.widget_settings_title_offset)
        }, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.CENTER_VERTICAL))
        column.addView(toolbar, LinearLayout.LayoutParams(MATCH_PARENT, px(R.dimen.widget_settings_toolbar_height)))

        // j30.g.x returns the responsive margin in dp, but UI85 assigns it as a pixel width.
        val side = SettingsStyle.responsiveMargin(resources.configuration.screenWidthDp, resources.configuration.screenHeightDp)
        previewHole = FrameLayout(this).apply {
            foreground = CornerOverlay(page, radius)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            addOnLayoutChangeListener { _, l, t, r, b, ol, ot, or, ob ->
                // Never add the preview child during layout: the resulting requestLayout is dropped.
                if (r - l != or - ol || b - t != ob - ot) post { refreshPreview() }
            }
        }
        root.hole = previewHole
        column.addView(previewHole, LinearLayout.LayoutParams(MATCH_PARENT, px(R.dimen.widget_settings_preview_height)).apply {
            marginStart = side; marginEnd = side; bottomMargin = px(R.dimen.widget_settings_preview_margin_bottom)
        })

        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = true
            scrollBarStyle = View.SCROLLBARS_OUTSIDE_OVERLAY
            isFillViewport = false
        }
        val bottom = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(bottom, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        column.addView(WidgetSettingsFadeFrame(this, scroll, page), LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f).apply {
            marginStart = side; marginEnd = side
        })

        bottom.addView(optionsCard(page, radius))
        if (WidgetPublisher.previewGeometry(this, widgetId).type == WidgetGeometry.MEDIUM) bottom.addView(shapes(page, radius))
        bottom.addView(View(this), LinearLayout.LayoutParams(MATCH_PARENT, px(R.dimen.widget_settings_bottom_spacing)))

        val buttons = floatingButtons()
        root.addView(buttons, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL))
        root.setOnApplyWindowInsetsListener { _, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            column.setPadding(bars.left, bars.top, bars.right, 0)
            (buttons.layoutParams as FrameLayout.LayoutParams).bottomMargin =
                bars.bottom + px(R.dimen.widget_settings_floating_margin_bottom)
            buttons.requestLayout()
            insets
        }
        return root
    }

    private fun row(vertical: Boolean = false) = LinearLayout(this).apply {
        orientation = if (vertical) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = px(R.dimen.widget_settings_list_min_height)
        val padding = px(R.dimen.widget_settings_row_padding)
        setPaddingRelative(padding, 0, padding, 0)
        background = ripple()
        isClickable = true
    }

    private fun summaryRow(title: Int, summary: TextView, onClick: (View) -> Unit) = row(vertical = true).apply {
        addView(text(getString(title), R.dimen.widget_settings_primary_text, color(R.color.widget_settings_text)))
        addView(summary)
        setOnClickListener(onClick)
    }

    private fun optionsCard(page: Int, radius: Float): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.widget_settings_card))
            foreground = CornerOverlay(page, radius)
        }
        val textColor = color(R.color.widget_settings_text)
        val accent = color(R.color.widget_settings_accent)
        if (known.size > 1) {
            // Per-instance device choice, presented like the Colors row.
            val summary = text("", R.dimen.widget_settings_secondary_text, accent)
            deviceSummary = summary
            card.addView(summaryRow(R.string.widget_devices, summary) { anchor ->
                showChoices(anchor, known.map(::deviceName), known.indexOfFirst { it.macAddress == deviceAddress }) {
                    deviceAddress = known[it].macAddress; bind()
                }
            })
            card.addView(divider())
        }

        val background = row().apply {
            addView(text(getString(R.string.widget_background), R.dimen.widget_settings_primary_text, textColor),
                LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            val label = getString(R.string.widget_background)
            addView(ComposeView(context).apply {
                setContent {
                    SettingsTheme(darkTheme = dark) {
                        SettingsSwitch(draft.background, { update(draft.copy(background = it)) }, label, compact = true)
                    }
                }
            }, LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                // The shared switch pads its track for feedback; the source track ends at the row inset.
                marginEnd = -(SettingsStyle.SwitchFeedbackInset.value * resources.displayMetrics.density).roundToInt()
            })
            setOnClickListener { update(draft.copy(background = !draft.background)) }
        }
        card.addView(background)

        val fadeout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        fadeoutLayout = fadeout
        fadeout.addView(divider())
        val opacity = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = px(R.dimen.widget_settings_row_padding)
            setPaddingRelative(padding, 0, padding, 0)
        }
        opacity.addView(text(getString(R.string.widget_opacity), R.dimen.widget_settings_primary_text, textColor).apply {
            gravity = Gravity.CENTER_VERTICAL
            minHeight = px(R.dimen.widget_settings_list_min_height)
        }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        val seek = RelativeLayout(this)
        val icon = ImageView(this).apply { id = View.generateViewId(); setImageDrawable(OpacityIconDrawable(context)) }
        seek.addView(icon, RelativeLayout.LayoutParams(px(R.dimen.widget_settings_opacity_icon_size), px(R.dimen.widget_settings_opacity_icon_size)).apply {
            addRule(RelativeLayout.ALIGN_PARENT_TOP); addRule(RelativeLayout.ALIGN_PARENT_START)
        })
        val opacityLabel = getString(R.string.widget_opacity)
        seek.addView(ComposeView(this).apply {
            translationY = resources.getDimension(R.dimen.widget_settings_seek_bar_offset)
            setContent {
                SettingsTheme(darkTheme = dark) {
                    // SeslSeekBar mode 8: no progress fill, markers centred on each of the three stops.
                    SettingsSeekBar(draft.opacity.toFloat(), { update(draft.copy(opacity = it.roundToInt().coerceIn(0, 2))) },
                        0f..2f, Modifier.fillMaxWidth().padding(horizontal = dimensionResource(R.dimen.widget_settings_seek_bar_inset))
                            .semantics { contentDescription = opacityLabel },
                        steps = 1, tickCount = 3, activeTrackColor = Color.Transparent,
                        tickInset = dimensionResource(R.dimen.widget_settings_seek_bar_tick_inset))
                }
            }
        }, RelativeLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            addRule(RelativeLayout.END_OF, icon.id); addRule(RelativeLayout.CENTER_VERTICAL)
        })
        opacity.addView(seek)
        opacity.addView(View(this), LinearLayout.LayoutParams(MATCH_PARENT, px(R.dimen.widget_settings_seek_bar_padding_bottom)))
        fadeout.addView(opacity)
        fadeout.addView(divider())
        colorSummary = text("", R.dimen.widget_settings_secondary_text, accent)
        fadeout.addView(summaryRow(R.string.widget_colors, colorSummary) { anchor ->
            showChoices(anchor, colorLabels, draft.color) { update(draft.copy(color = it)) }
        })
        card.addView(fadeout)
        return card
    }

    /** `sec_widget_colors_options_layout` in a focusable PopupWindow anchored as `in.q` case 1. */
    private fun showChoices(anchor: View, labels: List<String>, selected: Int, choose: (Int) -> Unit) {
        popup?.dismiss()
        val accent = color(R.color.widget_settings_accent)
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(color(R.color.widget_settings_card))
                cornerRadius = resources.getDimension(R.dimen.widget_settings_popup_radius)
            }
            val vertical = px(R.dimen.widget_settings_popup_padding_vertical)
            setPadding(0, vertical, 0, vertical)
        }
        val window = PopupWindow(list, px(R.dimen.widget_settings_popup_width), WRAP_CONTENT, true)
        labels.forEachIndexed { index, label ->
            val option = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                val padding = px(R.dimen.widget_settings_popup_padding_horizontal)
                setPaddingRelative(padding, 0, padding, 0)
                background = ripple()
                setOnClickListener { choose(index); window.dismiss() }
            }
            val chosen = index == selected
            option.addView(text(label, R.dimen.widget_settings_primary_text,
                if (chosen) accent else color(R.color.widget_settings_text)).apply {
                gravity = Gravity.CENTER_VERTICAL
                minHeight = px(R.dimen.widget_settings_popup_item_height)
                maxLines = 3; ellipsize = TextUtils.TruncateAt.END
                // lc.j.m()/l(): bold for the choice; the others drop to the default typeface.
                if (chosen) setTypeface(typeface, Typeface.BOLD) else setTypeface(null, Typeface.NORMAL)
            }, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            val check = px(R.dimen.widget_settings_popup_check_size)
            option.addView(ImageView(this).apply {
                setImageDrawable(CheckDrawable(accent, 24f, 24f))
                visibility = if (chosen) View.VISIBLE else View.GONE
            }, LinearLayout.LayoutParams(check, check))
            list.addView(option, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }
        // UI85 sets 8 as a raw pixel elevation.
        window.elevation = 8f
        list.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        window.showAsDropDown(anchor, 0, -list.measuredHeight)
        popup = window
    }

    private fun shapes(page: Int, radius: Float): View {
        val section = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        shapeSection = section
        section.addView(text(getString(R.string.widget_shape), R.dimen.widget_settings_subheader_text,
            color(R.color.widget_settings_subheader), 600).apply {
            setPaddingRelative(px(R.dimen.widget_settings_row_padding), px(R.dimen.widget_settings_subheader_padding_top),
                0, px(R.dimen.widget_settings_subheader_padding_bottom))
            isAccessibilityHeading = true
        })
        val card = FrameLayout(this).apply {
            setBackgroundColor(color(R.color.widget_settings_card))
            foreground = CornerOverlay(page, radius)
        }
        val scroller = HorizontalScrollView(this).apply { isFillViewport = true; isHorizontalScrollBarEnabled = false }
        val list = LinearLayout(this).apply { gravity = Gravity.CENTER }
        thumbnails.forEachIndexed { index, thumbnail ->
            val view = WidgetShapeView(this, thumbnail).apply {
                tag = thumbnail.shape
                contentDescription = getString(R.string.widget_shape_label, index + 1)
                isClickable = true; isFocusable = true
                setOnClickListener { update(draft.copy(shape = thumbnail.shape)) }
            }
            shapeViews.add(view)
            list.addView(view, LinearLayout.LayoutParams(px(R.dimen.widget_settings_shape_width), px(R.dimen.widget_settings_shape_height)).apply {
                marginStart = if (index == 0) px(R.dimen.widget_settings_shape_margin_edge) else 0
                marginEnd = if (index == thumbnails.lastIndex) px(R.dimen.widget_settings_shape_margin_edge)
                    else px(R.dimen.widget_settings_shape_item_margin)
            })
        }
        scroller.addView(list, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER_VERTICAL))
        card.addView(scroller, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        section.addView(card, LinearLayout.LayoutParams(MATCH_PARENT, px(R.dimen.widget_settings_shape_container_height)))
        return section
    }

    /** DividerButtonLayout with the toolbar menu's Cancel and Save. */
    private fun floatingButtons(): View {
        val bar = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            val padding = px(R.dimen.widget_settings_floating_padding)
            setPadding(padding, padding, padding, padding)
            background = GradientDrawable().apply {
                setColor(color(R.color.widget_settings_floating))
                cornerRadius = Float.MAX_VALUE
            }
            elevation = resources.getDimension(R.dimen.widget_settings_floating_elevation)
        }
        fun button(label: Int, action: () -> Unit) = FrameLayout(this).apply {
            minimumWidth = px(R.dimen.widget_settings_floating_button_min_width)
            val side = px(R.dimen.widget_settings_floating_button_side_padding)
            setPadding(side, 0, side, 0)
            background = RippleDrawable(ColorStateList.valueOf(color(R.color.widget_settings_pressed)), null,
                GradientDrawable().apply { setColor(0xFFFFFFFF.toInt()); cornerRadius = Float.MAX_VALUE })
            setOnClickListener { action() }
            addView(text(getString(label), R.dimen.widget_settings_floating_text, color(R.color.widget_settings_text), 600).apply {
                maxLines = 1; ellipsize = TextUtils.TruncateAt.END
                maxWidth = px(R.dimen.widget_settings_floating_button_max_width) - 2 * side
                setPadding(px(R.dimen.widget_settings_floating_highlight_horizontal), px(R.dimen.widget_settings_floating_highlight_top),
                    px(R.dimen.widget_settings_floating_highlight_horizontal), px(R.dimen.widget_settings_floating_highlight_bottom))
                gravity = Gravity.CENTER
            }, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER))
        }
        val height = px(R.dimen.widget_settings_floating_button_height)
        bar.addView(button(R.string.widget_cancel) { finish() }, LinearLayout.LayoutParams(WRAP_CONTENT, height))
        bar.addView(View(this).apply { setBackgroundColor(color(R.color.widget_settings_floating_divider)) },
            LinearLayout.LayoutParams(px(R.dimen.widget_settings_divider), px(R.dimen.widget_settings_floating_divider_height)))
        bar.addView(button(R.string.widget_save) { save() }, LinearLayout.LayoutParams(WRAP_CONTENT, height))
        return bar
    }

    private fun refreshPreview() {
        if (!::previewHole.isInitialized || previewHole.width == 0) return
        val density = resources.displayMetrics.density
        val source = WidgetPublisher.previewGeometry(this, widgetId)
        // The widget keeps its Home size, reduced only when it does not fit the preview.
        val fit = minOf(1f, previewHole.width / density / source.width, previewHole.height / density / source.height)
        val width = source.width * fit
        val height = source.height * fit
        val views = WidgetPublisher.preview(this, widgetId, draft, width, height, deviceAddress)
        previewHole.removeAllViews()
        val view = views.apply(this, previewHole)
        WidgetSurface.applyPreviewOutline(view, AppWidgetManager.getInstance(this).getAppWidgetOptions(widgetId),
            source, draft.shape, fit)
        // A preview must never send a real mode command or open the application.
        fun disableActions(node: View) {
            node.setOnClickListener(null); node.isClickable = false; node.isFocusable = false
            if (node is ViewGroup) for (i in 0 until node.childCount) disableActions(node.getChildAt(i))
        }
        disableActions(view)
        previewHole.addView(view, FrameLayout.LayoutParams((width * density).roundToInt(), (height * density).roundToInt(), Gravity.CENTER))
    }

    private companion object {
        // Row order and thumbnail geometry of sec_widget_settings_shape_list_layout. Shapes are
        // stored as the Manager's shape index; corners are top-left, top-right, bottom-right,
        // bottom-left in the 58 × 68 vector units. The default and left balloon use the smooth curve.
        val thumbnails = listOf(
            WidgetShapeThumbnail(0, listOf(10f, 10f, 10f, 10f), smooth = true),
            WidgetShapeThumbnail(2, listOf(14f, 5f, 14f, 5f), smooth = false),
            WidgetShapeThumbnail(1, listOf(5f, 14f, 5f, 14f), smooth = false),
            WidgetShapeThumbnail(4, listOf(14f, 5f, 14f, 14f), smooth = false),
            WidgetShapeThumbnail(3, listOf(5f, 14f, 14f, 14f), smooth = true),
        )
    }
}
