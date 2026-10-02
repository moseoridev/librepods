package me.kavishdevar.librepods.ui.components

import android.content.res.Configuration
import android.os.Build
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The reference's API-qualified dialog dimensions, resolved from configuration dp. */
internal data class SettingsTextEntryMetrics(val windowWidth: Dp, val horizontalInset: Dp, val radius: Dp)

internal fun settingsTextEntryMetrics(configuration: Configuration): SettingsTextEntryMetrics {
    val width = configuration.screenWidthDp
    val height = configuration.screenHeightDp
    val portrait = configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
    val resolvedWidth = if (Build.VERSION.SDK_INT >= 35) {
        when {
            configuration.smallestScreenWidthDp >= 600 -> 420
            !portrait && width >= 1920 -> 380
            portrait && width >= 1920 && height >= 480 -> 380
            width in 420 until 1920 -> 420
            !portrait && height >= 480 -> 380
            else -> width
        }
    } else {
        if ((!portrait && (width >= 420 || height >= 480)) ||
            (portrait && width >= 420 && height >= 480)) 360 else width
    }
    val smallCorner = portrait && width == 352 && height == 307
    val narrowInset = smallCorner || (portrait && width == 361 && height == 399)
    return SettingsTextEntryMetrics(resolvedWidth.coerceIn(0, width.coerceAtLeast(0)).dp,
        if (narrowInset) 16.dp else 10.dp, if (smallCorner) 18.dp else 26.dp)
}
