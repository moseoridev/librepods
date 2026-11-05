package me.kavishdevar.librepods.ui.components

import android.graphics.drawable.Animatable
import android.view.View
import android.widget.ProgressBar
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Current reference: ms.a.c / kw.n2 case 25, an indeterminate 24dp ProgressBar.
 * The public DeviceDefault widget style avoids a caller's Material theme replacing
 * the platform drawable. Its API 36 S25 animation data matches the reference.
 * Other devices use their platform implementation; no OEM resources are bundled.
 */
@Composable
fun SettingsLoadingIndicator(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.size(24.dp).semantics {
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        },
        factory = { context ->
            ProgressBar(context, null, 0, android.R.style.Widget_DeviceDefault_ProgressBar).apply {
                isIndeterminate = true
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
        },
        onRelease = { (it.indeterminateDrawable as? Animatable)?.stop() },
    )
}
