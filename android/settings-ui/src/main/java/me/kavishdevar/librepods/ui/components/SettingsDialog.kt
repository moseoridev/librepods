package me.kavishdevar.librepods.ui.components

import android.content.res.Configuration
import android.view.Gravity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.view.View
import android.view.ViewOutlineProvider
import android.view.ViewGroup
import android.graphics.drawable.InsetDrawable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import me.kavishdevar.librepods.ui.theme.SettingsStyle

data class SettingsDialogAction(
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val tint: Color = Color.Unspecified,
)

/** The native Compose prompt and the platform text-entry prompt have distinct metrics. */
enum class SettingsDialogPresentation { Confirmation, TextEntry, Selection }

/** Caller-owned visibility and actions; no automatic confirmation or dismissal. */
@Composable
fun SettingsDialog(
    visible: Boolean,
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    actions: List<SettingsDialogAction> = emptyList(),
    presentation: SettingsDialogPresentation = SettingsDialogPresentation.Confirmation,
    message: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!visible) return
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val textEntry = presentation == SettingsDialogPresentation.TextEntry
    val selection = presentation == SettingsDialogPresentation.Selection
    val entryMetrics = settingsTextEntryMetrics(configuration)
    val outerVertical = if (textEntry) 8.dp else 16.dp
    // wm.k0 uses the host's available constraints, rather than rounded screen dp.
    BoxWithConstraints {
        val heightLimit = (maxHeight - 16.dp).coerceAtLeast(0.dp) *
            if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 1f else if (textEntry) .8f else (2f / 3f)
        Dialog(onDismissRequest = onDismissRequest, properties = properties) {
            val view = LocalView.current
            val dark = MaterialTheme.colorScheme.onSurface != Color(0xFF010102)
            var blurred by remember(view) { mutableStateOf(false) }
            val cornerPx = with(density) { if (textEntry) entryMetrics.radius.roundToPx().toFloat() else 26.dp.toPx() }
            val surfaceColor = if (dark) Color(0xFF252528) else Color(0xFFFCFCFF)
            val platformBackground = remember(textEntry, surfaceColor, cornerPx) {
                if (textEntry) settingsDialogBackground(surfaceColor.toArgb(), cornerPx) else null
            }
            // ContentFrameLayout bases the platform dialog's minimum width on screenWidthDp.
            val textEntryWidth = with(density) {
                (entryMetrics.windowWidth.toPx().toInt() - 2 * entryMetrics.horizontalInset.toPx().toInt())
                    .coerceAtLeast(0).toDp()
            }
            DisposableEffect(view, dark, cornerPx, outerVertical, entryMetrics) {
                val parent = view.parent as? View
                val oldPadding = parent?.let { intArrayOf(it.paddingLeft, it.paddingTop, it.paddingRight, it.paddingBottom) }
                // Keep the native view's blur bounds equal to its content, excluding outer insets.
                val horizontal = with(density) { if (textEntry) entryMetrics.horizontalInset.toPx().toInt() else 10.dp.roundToPx() }
                val vertical = with(density) { if (textEntry) outerVertical.toPx().toInt() else outerVertical.roundToPx() }
                // The platform window background supplies its own content insets.
                parent?.setPadding(if (textEntry) 0 else horizontal, if (textEntry) 0 else vertical,
                    if (textEntry) 0 else horizontal, if (textEntry) 0 else vertical)
                blurred = !textEntry && SettingsWindowBlur.apply(view, dark, cornerPx)
                val window = (parent as? DialogWindowProvider)?.window
                val decor = window?.decorView
                val oldBackground = decor?.background
                val oldElevation = decor?.elevation
                val oldOutline = decor?.outlineProvider
                val oldClip = decor?.clipToOutline
                val oldWidth = window?.attributes?.width
                val oldHeight = window?.attributes?.height
                if (textEntry && window != null && platformBackground != null) {
                    window.setBackgroundDrawable(InsetDrawable(platformBackground,
                        horizontal, vertical, horizontal, vertical))
                    // PhoneWindow.setElevation also updates the shadow's surface insets.
                    // Changing DecorView alone leaves the Compose host's initial insets.
                    window.setElevation(with(density) { 8.dp.toPx() })
                    decor?.apply {
                        outlineProvider = ViewOutlineProvider.BACKGROUND
                        clipToOutline = true
                    }
                }
                onDispose {
                    SettingsWindowBlur.clear(view)
                    if (textEntry && decor != null) {
                        decor.background = oldBackground
                        // Platform shadow insets grow monotonically for nonzero elevation.
                        // Reset before restoring so a later presentation can use its own host.
                        window?.setElevation(0f)
                        window?.setElevation(oldElevation ?: 0f)
                        decor.elevation = oldElevation ?: 0f
                        decor.outlineProvider = oldOutline
                        decor.clipToOutline = oldClip ?: false
                        if (oldWidth != null && oldHeight != null) window?.setLayout(oldWidth, oldHeight)
                    }
                    if (parent != null && oldPadding != null)
                        parent.setPadding(oldPadding[0], oldPadding[1], oldPadding[2], oldPadding[3])
                }
            }
            SideEffect { (view.parent as? DialogWindowProvider)?.window?.setGravity(Gravity.BOTTOM) }
            Surface(modifier.then(if (textEntry) Modifier.widthIn(max = textEntryWidth) else Modifier)
                .fillMaxWidth().heightIn(max = heightLimit)
                .onSizeChanged { size ->
                    if (textEntry) (view.parent as? DialogWindowProvider)?.window?.let { window ->
                        val width = size.width + with(density) { 2 * entryMetrics.horizontalInset.toPx().toInt() }
                        if (window.attributes.width != width) window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
                    }
                },
                shape = if (platformBackground != null) RectangleShape else SettingsStyle.GroupShape,
                color = if (blurred || platformBackground != null) Color.Transparent else surfaceColor,
                contentColor = MaterialTheme.colorScheme.onSurface) {
                Column(Modifier.padding(top = 24.dp, bottom = 20.dp)) {
                    // The native dialog merges its title into the theme's on-device family.
                    if (textEntry) SettingsDialogTitle(title, Modifier.fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, bottom = 16.dp))
                    else Text(title, style = SettingsStyle.RowTitle.copy(fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp, textMotion = TextMotion.Static,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)),
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp))
                    message?.let {
                        Text(it, Modifier.padding(horizontal = 24.dp),
                            style = SettingsStyle.RowTitle.copy(fontSize = 14.sp),
                            color = if (dark) Color(0xFFE9E9EC) else Color(0xFF252528))
                    }
                    Column(Modifier.weight(1f, fill = false).fillMaxWidth()
                        .then(if (selection) Modifier.padding(horizontal = 10.dp) else Modifier)
                        .verticalScroll(rememberScrollState())
                        // a1.f3 and o1.h0 round their 10dp and 6dp insets separately.
                        .padding(horizontal = if (selection) 6.dp else 24.dp)) {
                        ProvideTextStyle(SettingsStyle.RowTitle.copy(fontSize = 14.sp, lineHeight = 21.sp,
                            color = if (dark) Color(0xFFE9E9EC) else Color(0xFF252528))) { content() }
                    }
                    if (actions.isNotEmpty()) SettingsDialogActions(actions,
                        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp,
                            top = if (textEntry) 0.dp else 16.dp), textEntry)
                }
            }
        }
    }
}

/** kw.h/j: equal integer slots when natural labels fit; otherwise a vertical list. */
@Composable
private fun SettingsDialogActions(actions: List<SettingsDialogAction>, modifier: Modifier, textEntry: Boolean) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    Layout(modifier = modifier, content = {
        actions.forEach { action ->
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified,
                LocalDensity provides Density(density.density,
                    if (textEntry) density.fontScale else density.fontScale.coerceAtMost(1.3f))) {
                val tint = if (action.tint == Color.Unspecified) colors.onSurface else action.tint
                SettingsDialogActionSurface(textEntry = textEntry, label = action.label, enabled = action.enabled,
                    onClick = { if (action.enabled) action.onClick() },
                    contentColor = tint.copy(alpha = tint.alpha * if (textEntry || action.enabled) 1f else .4f)) {
                    Box(Modifier.defaultMinSize(minWidth = if (textEntry) 42.dp else Dp.Unspecified,
                        minHeight = if (textEntry) 36.dp else 40.dp)
                        .padding(horizontal = if (textEntry) 4.dp else 16.dp,
                            vertical = if (textEntry) 0.dp else 8.dp),
                        contentAlignment = if (textEntry) PlatformTextCenter else Alignment.Center) {
                        if (textEntry) SettingsDialogActionMeasure(action.label, Modifier.fillMaxWidth())
                        else Text(action.label, modifier = Modifier,
                            style = SettingsStyle.RowTitle.copy(fontSize = if (textEntry) 17.sp else 18.sp,
                            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                            textMotion = TextMotion.Animated,
                            platformStyle = PlatformTextStyle(includeFontPadding = textEntry)))
                    }
                }
            }
        }
        repeat(actions.size - 1) {
            // a1.o3 case 21 measures a filled box, not an antialiased divider stroke.
            Box(Modifier.size(width = 1.dp, height = 16.dp)
                .then(if (textEntry) Modifier.graphicsLayer { alpha = .23f } else Modifier).background(
                if (textEntry) (if (colors.onSurface == Color(0xFF010102)) Color(0xFF848487)
                    else Color(0xFFA3A3A7))
                else if (colors.onSurface == Color(0xFF010102)) Color(0xFFE9E9EC) else Color(0xFF4D4D52)))
        }
    }) { measurables, constraints ->
        val buttons = measurables.take(actions.size)
        val dividers = measurables.drop(actions.size).map { it.measure(Constraints()) }
        val width = constraints.maxWidth
        val natural = buttons.map { it.maxIntrinsicWidth(Constraints.Infinity) }
        val horizontal = natural.sumOf { it.toLong() } +
            dividers.sumOf { it.width } <= width
        val slot = if (horizontal) (width - dividers.sumOf { it.width }).coerceAtLeast(0) / actions.size else width
        // The platform button bar starts with natural widths and shares remaining space.
        val surplus = (width - dividers.sumOf { it.width } - natural.sum()).coerceAtLeast(0)
        val places = buttons.mapIndexed { index, button ->
            val buttonWidth = if (textEntry && horizontal) natural[index] +
                // LinearLayout distributes each share using the remaining weight and space.
                (surplus * (index + 1) / actions.size - surplus * index / actions.size) else slot
            button.measure(Constraints(minWidth = buttonWidth, maxWidth = buttonWidth,
                maxHeight = constraints.maxHeight))
        }
        val gap = 16.dp.roundToPx()
        val height = if (horizontal) places.maxOf { it.height }
            else places.sumOf { it.height } + gap * (places.size - 1)
        layout(width, constraints.constrainHeight(height)) {
            var cursor = 0
            places.forEachIndexed { index, place ->
                if (horizontal) {
                    place.placeRelative(cursor, (height - place.height) / 2)
                    cursor += place.width
                    dividers.getOrNull(index)?.let { divider ->
                        divider.placeRelative(cursor, (height - divider.height) / 2)
                        cursor += divider.width
                    }
                } else {
                    place.placeRelative(0, cursor)
                    cursor += place.height + gap
                }
            }
        }
    }
}

@Composable
private fun SettingsDialogActionSurface(textEntry: Boolean, label: String, enabled: Boolean, onClick: () -> Unit,
    contentColor: Color, content: @Composable () -> Unit) {
    if (textEntry) {
        SettingsTextEntryAction(label, enabled, onClick, contentColor, content)
    } else {
        val interactionSource = remember { MutableInteractionSource() }
        // kw.t.s explicitly chooses CornerFull (v1.i → t1.l1), then delegates to
        // the same kw.t.g feedback as ordinary buttons. Its corner is circular.
        val feedback = settingsButtonFeedback(enabled, interactionSource, CircleShape)
        Surface(modifier = Modifier.clickable(interactionSource = interactionSource,
            indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .then(feedback), shape = CircleShape, color = Color.Transparent,
            contentColor = contentColor, content = content)
    }
}

/** TextView's gravity truncates odd surplus pixels toward the leading/top edge. */
private object PlatformTextCenter : Alignment {
    override fun align(size: IntSize, space: IntSize, layoutDirection: LayoutDirection) =
        IntOffset((space.width - size.width) / 2, (space.height - size.height) / 2)
}

@Composable
fun SettingsConfirmationDialog(visible: Boolean, title: String, message: String,
    confirmText: String, dismissText: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    SettingsDialog(visible, title, onDismiss, actions = listOf(
        SettingsDialogAction(dismissText, onDismiss), SettingsDialogAction(confirmText, onConfirm),
    )) { Text(message, style = LocalTextStyle.current.copy(lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified)) }
}
