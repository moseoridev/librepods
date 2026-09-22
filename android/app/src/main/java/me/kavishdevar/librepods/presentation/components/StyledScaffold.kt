/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package me.kavishdevar.librepods.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Density
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import android.content.res.Configuration
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import me.kavishdevar.librepods.presentation.theme.BudsStyle
import me.kavishdevar.librepods.presentation.theme.toolbarSubtitle
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyledScaffold(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    title: String,
    showBackButton: Boolean = false,
    onNavigateBack: () -> Unit = {},
    actionButtons: List<@Composable (backdrop: LayerBackdrop) -> Unit> = emptyList(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    expandableHeader: Boolean = false,
    headerKey: Any = title,
    subtitle: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val compact = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && configuration.screenHeightDp < 580
    val canExpand = visible && expandableHeader && configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
    val range = with(LocalDensity.current) { if (canExpand) 198.dp.toPx() else 0f }
    val scope = rememberCoroutineScope()
    val header = remember(headerKey, range) { BudsHeaderState(range, scope) }
    DisposableEffect(header) { onDispose { header.stop() } }
    Scaffold(modifier = Modifier.nestedScroll(header), containerColor = MaterialTheme.colorScheme.surfaceContainer,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (visible) {
                // ASC ku.i0.J/R, ku.w2, SESL top padding: 64+8dp (compact landscape 56+0).
                Column(Modifier.offset(y = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 0.dp else (-6).dp)
                    .windowInsetsPadding(TopAppBarDefaults.windowInsets)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(top = if (compact) 0.dp else 8.dp)) {
                    Box(Modifier.fillMaxWidth().height((if (compact) 56.dp else 64.dp) + 198.dp * header.fraction)
                        .clipToBounds()
                        .then(if (canExpand) Modifier.draggable(
                            state = rememberDraggableState { header.drag(it) }, orientation = Orientation.Vertical,
                            onDragStarted = { header.stop() }, onDragStopped = { header.settle() }
                        ) else Modifier)) {
                    if (canExpand) {
                        Box(Modifier.fillMaxWidth().wrapContentHeight(Alignment.Top, unbounded = true)
                            .height(262.dp)
                            .graphicsLayer {
                                translationY = (header.fraction - 1f) * range * .6f
                                alpha = (2f * header.fraction - 1f).coerceIn(0f, 1f)
                            }, contentAlignment = Alignment.Center) {
                            Text(title, Modifier.padding(horizontal = 24.dp)
                                .then(if (header.fraction > .5f) Modifier.semantics { heading() } else Modifier.clearAndSetSemantics {}),
                                style = BudsStyle.ExpandedTitle, textAlign = TextAlign.Center,
                                maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Row(Modifier.align(Alignment.BottomStart).semantics { isTraversalGroup = true }.fillMaxWidth().height(if (compact) 56.dp else 64.dp)
                        .padding(start = if (showBackButton) 24.dp else 28.dp, end = 18.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        if (showBackButton) {
                            val backLabel = androidx.compose.ui.res.stringResource(me.kavishdevar.librepods.R.string.navigate_back)
                            Box(Modifier.padding(end = 20.dp).size(24.dp)
                                .clickable(role = Role.Button, onClick = onNavigateBack)
                                .semantics { contentDescription = backLabel }, contentAlignment = Alignment.Center) {
                                if (navigationIcon != null) navigationIcon() else Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                            }
                        }
                        Column(Modifier.weight(1f)
                            .graphicsLayer { alpha = (1f - 2f * header.fraction).coerceIn(0f, 1f) }
                            .then(if (header.fraction <= .5f) Modifier.semantics { heading() } else Modifier.clearAndSetSemantics {})) {
                            // SESL bounds title scaling; a title/subtitle pair uses unscaled type.
                            val density = LocalDensity.current
                            CompositionLocalProvider(LocalDensity provides Density(density.density,
                                if (subtitle != null) 1f else density.fontScale.coerceAtMost(1.3f))) {
                                Text(title, style = BudsStyle.CompactTitle,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                subtitle?.let { Text(it, style = BudsStyle.CompactSubtitle,
                                    color = MaterialTheme.colorScheme.toolbarSubtitle,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            }
                        }
                        actionButtons.forEach { it(rememberLayerBackdrop()) }
                    }
                    }
                }
            }
        }) { padding ->
        Box(modifier.then(if (visible) Modifier.budsContentFade().padding(padding) else Modifier).fillMaxSize()) { content() }
    }
}
