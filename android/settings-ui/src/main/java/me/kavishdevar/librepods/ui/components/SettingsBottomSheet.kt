package me.kavishdevar.librepods.ui.components

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
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
        scrimColor = Color.Black.copy(alpha = .65f),
        dragHandle = null,
        properties = ModalBottomSheetProperties(shouldDismissOnClickOutside = dismissOnClickOutside),
    ) {
        SettingsSheetSurface(Modifier.fillMaxWidth().padding(10.dp)) { content(progress) }
    }
}
