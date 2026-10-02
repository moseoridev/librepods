package me.kavishdevar.librepods.ui.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextMotion
import me.kavishdevar.librepods.ui.theme.SettingsStyle

enum class SettingsButtonStyle { Text, Tonal, Filled }

/** wm.k1.g / kw.t.g: pill geometry, three color styles and capped label scaling. */
@Composable
fun SettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: SettingsButtonStyle = SettingsButtonStyle.Text,
    tint: Color = Color.Unspecified,
    containerColor: Color = Color.Unspecified,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val container = if (containerColor != Color.Unspecified) containerColor else when (style) {
        SettingsButtonStyle.Filled -> colors.primary
        SettingsButtonStyle.Tonal -> colors.secondaryContainer
        else -> Color.Transparent
    }
    val foreground = if (tint != Color.Unspecified) tint else when (style) {
        // wm.a0 explicitly supplies the current filled label token, overriding
        // kw.w0's generic content color.
        SettingsButtonStyle.Filled -> Color(0xFFFCFCFF)
        SettingsButtonStyle.Tonal -> colors.onSurface
        else -> LocalContentColor.current
    }
    val alpha = if (enabled) 1f else .4f
    // n1.l.d → rw.a: the shared smooth corner at 50%, rather than a circular arc.
    val shape = SettingsStyle.PillShape
    val density = LocalDensity.current
    // ku.i0.i caps button text scaling at 1.3. Do not alter the Activity configuration.
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale.coerceAtMost(1.3f)),
        // Let text and separately rounded padding determine the visual height.
        // Compose's clickable expands the hit region without enlarging the pill.
        LocalMinimumInteractiveComponentSize provides Dp.Unspecified,
    ) {
        Surface(onClick = onClick, enabled = enabled, modifier = modifier.semantics { role = Role.Button },
            shape = shape, color = container.copy(alpha = container.alpha * alpha),
            contentColor = foreground.copy(alpha = foreground.alpha * alpha)) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                // The current manager's common button defaults are 15sp and 16/10dp
                // content padding (wm.k1.g → kw.w0). Screen-specific 52dp heights
                // are supplied by the caller (nm.d), through modifier.
                ProvideTextStyle(SettingsStyle.RowTitle.copy(fontSize = 15.sp,
                    // wm.a0 keeps the filled label's explicit color even when
                    // kw.t.g dims its background and generic content color.
                    color = if (style == SettingsButtonStyle.Filled) foreground else Color.Unspecified,
                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                    textMotion = TextMotion.Animated)) { content() }
            }
        }
    }
}

/** The app supplies its own artwork and a localized action label. */
@Composable
fun SettingsIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Color.Unspecified,
    containerColor: Color = Color.Transparent,
    content: @Composable () -> Unit,
) {
    val ink = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurface else tint
    // kw.t.u uses a padded icon box rather than a clickable Material Surface.
    // The separate 24dp icon and 12dp insets retain its natural pixel rounding.
    Box(modifier.clip(CircleShape).background(containerColor)
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .semantics { this.contentDescription = contentDescription }
        .sizeIn(minWidth = 48.dp, minHeight = 48.dp).padding(12.dp),
        contentAlignment = Alignment.Center) {
        CompositionLocalProvider(LocalContentColor provides ink.copy(alpha = ink.alpha * if (enabled) 1f else .4f)) {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { content() }
        }
    }
}
