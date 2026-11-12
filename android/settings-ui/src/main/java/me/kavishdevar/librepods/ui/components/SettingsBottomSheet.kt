package me.kavishdevar.librepods.ui.components

import android.view.WindowManager
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.BottomSheet
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import me.kavishdevar.librepods.ui.R
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
 * Source Expanded sheet: a navigation-inset viewport, then a Window exit after hiding.
 * Partial expansion, outside dismissal and the pre-34 host retain the Compose adapter.
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
    val nativeHost = android.os.Build.VERSION.SDK_INT >= 34 &&
        !allowPartialExpansion && !dismissOnClickOutside
    val sheetState = if (nativeHost) rememberBottomSheetState(SheetValue.Expanded,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))
        else rememberModalBottomSheetState(skipPartiallyExpanded = !allowPartialExpansion)
    val callerMotion = MaterialTheme.motionScheme
    val hostMotion = remember(callerMotion) { object : MotionScheme by callerMotion {
        // nh.a / BottomSheetBehavior.b: medium2 (300ms) before Dialog dismissal.
        override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = tween(300)
    } }
    val scope = rememberCoroutineScope()
    val progress by animateFloatAsState(
        if (sheetState.targetValue == SheetValue.Expanded) 1f else 0f,
        label = "Settings sheet expansion",
    )
    val sheetContent: @Composable ColumnScope.() -> Unit = {
        val view = LocalView.current
        val elevationPixels = with(LocalDensity.current) { 8.dp.toPx() }
        DisposableEffect(view, elevationPixels, nativeHost) {
            val window = (view.parent as? DialogWindowProvider)?.window
            if (nativeHost) window?.setWindowAnimations(R.style.SettingsSheetWindowAnimation)
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
    if (nativeHost) {
        Dialog(onDismissRequest = { scope.launch { sheetState.hide(); onDismissRequest() } },
            properties = DialogProperties(usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false, dismissOnClickOutside = false)) {
            // Native CoordinatorLayout excludes the navigation bar from its hide range.
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))) {
                MaterialTheme(motionScheme = hostMotion) {
                    BottomSheet(modifier = modifier.align(androidx.compose.ui.Alignment.TopCenter), state = sheetState, onDismissRequest = onDismissRequest,
                        maxWidth = Dp.Unspecified, shape = RectangleShape,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface, tonalElevation = 0.dp,
                        dragHandle = null, contentWindowInsets = { WindowInsets(0) }) {
                        MaterialTheme(motionScheme = callerMotion) { sheetContent() }
                    }
                }
            }
        }
    } else {
        ModalBottomSheet(onDismissRequest = onDismissRequest, modifier = modifier,
            sheetState = sheetState, sheetMaxWidth = Dp.Unspecified, shape = RectangleShape,
            containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 0.dp, scrimColor = Color.Transparent, dragHandle = null,
            properties = ModalBottomSheetProperties(shouldDismissOnClickOutside = dismissOnClickOutside),
            content = sheetContent)
    }
}
