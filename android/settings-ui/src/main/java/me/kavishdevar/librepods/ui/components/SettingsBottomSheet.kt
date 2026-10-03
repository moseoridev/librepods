package me.kavishdevar.librepods.ui.components

import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Current source card inside a Compose modal host. Window insets, gestures and
 * transition frames still need comparison with the original visible sheet.
 * Partial expansion is an explicit caller adaptation; the source opens Expanded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    allowPartialExpansion: Boolean = false,
    dismissOnClickOutside: Boolean = false,
    content: @Composable ColumnScope.(progress: Float) -> Unit,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = !allowPartialExpansion)
    val progress by animateFloatAsState(
        if (sheetState.targetValue == SheetValue.Expanded) 1f else 0f,
        label = "Settings sheet expansion",
    )
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        // The XML host spans the window; the inner card owns its width cap.
        sheetMaxWidth = Dp.Unspecified,
        shape = RectangleShape,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        // The native dialog dims the underlying Window. A drawn scrim changes
        // the pixels sampled by Samsung's card blur before the blur is applied.
        scrimColor = Color.Transparent,
        dragHandle = null,
        properties = ModalBottomSheetProperties(shouldDismissOnClickOutside = dismissOnClickOutside),
    ) {
        val view = LocalView.current
        val elevationPixels = with(LocalDensity.current) { 8.dp.toPx() }
        DisposableEffect(view, elevationPixels) {
            val window = (view.parent as? DialogWindowProvider)?.window
            val oldDimAmount = window?.attributes?.dimAmount
            val oldElevation = window?.decorView?.elevation
            val hadDimFlag = (window?.attributes?.flags ?: 0) and WindowManager.LayoutParams.FLAG_DIM_BEHIND != 0
            // DeviceDefault Dialog.show owns the dim amount, including Samsung's
            // light/reduced-transparency policy. Overriding it here changes blur.
            // Other platform dialog themes can leave a non-dimming window at 1.
            // Initialize that case from the caller's theme before enabling dim.
            val needsDimInitialization = oldDimAmount != null && oldDimAmount >= 1f
            if (needsDimInitialization) {
                val attributes = window!!.context.obtainStyledAttributes(intArrayOf(android.R.attr.backgroundDimAmount))
                val themedDim = try { attributes.getFloat(0, .65f) } finally { attributes.recycle() }
                window.setDimAmount(themedDim)
            }
            window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            // The native dialog's Window also has 8dp elevation. Its surface
            // insets affect Samsung's blur sampling, independently of the card.
            window?.setElevation(elevationPixels)
            onDispose {
                if (needsDimInitialization) window?.setDimAmount(oldDimAmount!!)
                if (!hadDimFlag) window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                if (oldElevation != null) {
                    window.setElevation(0f)
                    window.setElevation(oldElevation)
                }
            }
        }
        SettingsSheetSurface(Modifier.fillMaxWidth().padding(10.dp)) { content(progress) }
    }
}
