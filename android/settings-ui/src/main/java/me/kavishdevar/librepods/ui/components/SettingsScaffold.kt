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

package me.kavishdevar.librepods.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Density
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
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
import me.kavishdevar.librepods.ui.theme.SettingsStyle
import me.kavishdevar.librepods.ui.theme.toolbarSubtitle
import me.kavishdevar.librepods.ui.theme.floatingToolbarBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScaffold(
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    title: String,
    backLabel: String,
    showBackButton: Boolean = false,
    onNavigateBack: () -> Unit = {},
    actionButtons: List<@Composable () -> Unit> = emptyList(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    expandableHeader: Boolean = false,
    headerKey: Any = title,
    subtitle: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    // Supplying a state makes the scaffold own the scroll viewport and content insets.
    // Content then supplies ordinary, non-scrolling composables (`wm.u1/w1`).
    scrollState: ScrollState? = null,
    // Source Home supplies a separate backdrop below the scrolling foreground (wm.b2).
    background: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    // The bottom fading edge is the scaffold's own, not the caller's: the original enables it from
    // the content's scroll state (`wm.b2` reads `ScrollState.c()`). The scaffold owns the holder and
    // the scrolling screen reports into it through [SettingsScrollReporter], so the edge tracks whether
    // content remains below rather than being guessed per screen.
    val scrollHolder = remember { SettingsScrollState() }
    val configuration = LocalConfiguration.current
    val compact = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && configuration.screenHeightDp < 580
    // The source token comes from a resource with default 0dp and h442dp 4dp.
    val toolbarTopPadding = if (configuration.screenHeightDp >= 442) 4.dp else 0.dp
    // kw.m1 measures the 24dp action and each 12dp content inset separately.
    // Its natural height can exceed the 48dp navigation slot by a pixel (450dpi).
    val headerRowHeight = with(LocalDensity.current) {
        maxOf(48.dp.roundToPx(), 24.dp.roundToPx() + 2 * 12.dp.roundToPx()).toDp()
    }
    val canExpand = visible && expandableHeader && configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
    val range = with(LocalDensity.current) { if (canExpand) 198.dp.toPx() else 0f }
    val scope = rememberCoroutineScope()
    val header = remember(headerKey, range) { SettingsHeaderState(range, scope) }
    DisposableEffect(header) { onDispose { header.stop() } }
    val scrolled = scrollState != null && scrollState.value > 0
    var floatingNavigationVisible by remember(scrollState) { mutableStateOf(scrolled) }
    LaunchedEffect(scrollState) {
        val state = scrollState ?: return@LaunchedEffect
        var previous = state.value
        var forwardDistance = 0
        snapshotFlow { state.value }.collectLatest { value ->
            forwardDistance = if (value >= previous) forwardDistance + value - previous else 0
            previous = value
            // wm.n2/b2: suppress the floating actions during a long forward scroll.
            floatingNavigationVisible = value > 0 && forwardDistance <= 1000
            // wm.m2 restores the actions 250ms after the last forward movement.
            // collectLatest cancels the pending reset when scrolling continues.
            delay(250)
            forwardDistance = 0
            floatingNavigationVisible = value > 0
        }
    }
    Scaffold(modifier = modifier.nestedScroll(header), containerColor = MaterialTheme.colorScheme.surfaceContainer,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (visible) {
                // kw.m2.a keeps the compact content 64dp tall (56dp in short landscape).
                // wm.i3 and kw.t.D each measure the top-padding token before the content.
                // Preserve the two measurements (sesl_action_bar_top_padding), whose
                // sum rounds to 22px at 450dpi rather than a combined 8dp's 23px.
                // wm.i3 supplies transparent compact-header containers. The content's
                // distinct indicator-background fade remains visible underneath.
                Column(Modifier.windowInsetsPadding(TopAppBarDefaults.windowInsets)
                    .padding(top = toolbarTopPadding)) {
                    Box(Modifier.fillMaxWidth().padding(top = toolbarTopPadding)
                        .height((if (compact) 56.dp else 64.dp) + 198.dp * header.fraction)
                        .clipToBounds()
                        .then(if (canExpand && !scrolled) Modifier.draggable(
                            state = rememberDraggableState { header.drag(it) }, orientation = Orientation.Vertical,
                            onDragStarted = { header.stop() }, onDragStopped = { header.settle() }
                        ) else Modifier)) {
                    // Retain the measured inset, but remove hidden input nodes so the
                    // content beneath the header can receive touches during scrolling.
                    if (!scrolled) {
                    if (canExpand) {
                        Box(Modifier.fillMaxWidth().wrapContentHeight(Alignment.Top, unbounded = true)
                            .height(262.dp)
                            .graphicsLayer {
                                translationY = (header.fraction - 1f) * range * .6f
                                alpha = (2f * header.fraction - 1f).coerceIn(0f, 1f)
                            }, contentAlignment = Alignment.Center) {
                            Text(title, Modifier.padding(horizontal = 24.dp)
                                .then(if (header.fraction > .5f) Modifier.semantics { heading() } else Modifier.clearAndSetSemantics {}),
                                style = SettingsStyle.ExpandedTitle, textAlign = TextAlign.Center,
                                maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Row(Modifier.align(if (canExpand) Alignment.BottomStart else Alignment.TopStart)
                        .semantics { isTraversalGroup = true }.fillMaxWidth().height(headerRowHeight),
                        verticalAlignment = Alignment.CenterVertically) {
                        if (showBackButton) {
                            // The current navigation slot measures 48dp around a 24dp icon.
                            // Its center and end must be measured as one slot: at 450dpi it
                            // is 135px wide, while 12+24+12dp rounds separately to 136px.
                            Spacer(Modifier.width(12.dp))
                            Box(Modifier.size(48.dp)
                                    .clickable(role = Role.Button, onClick = onNavigateBack)
                                    .semantics { contentDescription = backLabel }, contentAlignment = Alignment.Center) {
                                Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                                    if (navigationIcon != null) navigationIcon() else Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                                }
                            }
                        } else {
                            // ku.k3 reserves 28dp for the absent navigation slot. Captured
                            // title bounds place it another 8dp in; keeping those measurements
                            // separate yields 79+23px at density 2.8125, where a combined 36dp
                            // inset rounds to 101px.
                            Spacer(Modifier.width(28.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        Column(Modifier.weight(1f)
                            .graphicsLayer { alpha = (1f - 2f * header.fraction).coerceIn(0f, 1f) }
                            .then(if (header.fraction <= .5f) Modifier.semantics { heading() } else Modifier.clearAndSetSemantics {})) {
                            // SESL bounds title scaling; a title/subtitle pair uses unscaled type.
                            val density = LocalDensity.current
                            CompositionLocalProvider(LocalDensity provides Density(density.density,
                                if (subtitle != null) 1f else density.fontScale.coerceAtMost(1.3f))) {
                                Text(title, style = SettingsStyle.CompactTitle,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                subtitle?.let { Text(it, style = SettingsStyle.CompactSubtitle,
                                    color = MaterialTheme.colorScheme.toolbarSubtitle,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            }
                        }
                        if (actionButtons.isNotEmpty()) {
                            // Current ww.a0/ww.x use a 48dp action row with 6dp side
                            // insets and centered 36dp minimum slots. At fractional
                            // densities this differs from the legacy 4+24+8 insets.
                            Row(Modifier.height(48.dp).padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                actionButtons.forEach { action ->
                                    Box(Modifier.sizeIn(minWidth = 36.dp, minHeight = 36.dp),
                                        contentAlignment = Alignment.Center) { action() }
                                }
                            }
                        }
                        // The current action-free title ends 12dp before the edge.
                        // This affects which glyphs fit before an ellipsis in narrow windows.
                        Spacer(Modifier.width(12.dp))
                    }
                    }
                    }
                }
            }
        }) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        // The source scroll content remains beneath the navigation bar. Scaffold's bottom inset
        // would stop our content at its upper edge, leaving only the page background under the
        // fading edge. Keep the header and horizontal insets while drawing through the bottom.
        val contentPadding = PaddingValues(
            start = padding.calculateStartPadding(layoutDirection),
            top = padding.calculateTopPadding(),
            end = padding.calculateEndPadding(layoutDirection),
            bottom = 0.dp,
        )
        val sourceScroll = scrollState
        Box(Modifier.fillMaxSize()) {
            if (background != null) {
                val backdropOffset = with(LocalDensity.current) { -(sourceScroll?.value ?: 0).toDp() }
                // Native wm.q2 creates its header inside Scaffold content, so the backdrop
                // receives system padding only; the foreground separately reserves the header.
                val backdropPadding = PaddingValues(
                    start = padding.calculateStartPadding(layoutDirection),
                    top = TopAppBarDefaults.windowInsets.asPaddingValues().calculateTopPadding(),
                    end = padding.calculateEndPadding(layoutDirection),
                    bottom = padding.calculateBottomPadding(),
                )
                Box(Modifier.fillMaxSize().padding(backdropPadding).offset(y = backdropOffset)) { background() }
            }
            Box(Modifier.then(if (visible) Modifier.settingsContentFade(
                sourceScroll?.canScrollForward ?: scrollHolder.canScrollForward) else Modifier)
                .then(if (visible && sourceScroll == null) Modifier.padding(contentPadding) else Modifier)
                .fillMaxSize()) {
                CompositionLocalProvider(LocalSettingsScrollState provides scrollHolder) {
                    if (sourceScroll == null) content() else {
                        // Insets are inside the scroll container, so content can pass beneath
                        // the header. Keep the source's navigation inset plus 20dp end space.
                        val scrollPadding = PaddingValues(
                            start = padding.calculateStartPadding(layoutDirection),
                            top = if (visible) padding.calculateTopPadding() else 0.dp,
                            end = padding.calculateEndPadding(layoutDirection),
                            bottom = padding.calculateBottomPadding(),
                        )
                        Column(Modifier.fillMaxSize().verticalScroll(sourceScroll)
                            .padding(scrollPadding).padding(bottom = 20.dp)) { content() }
                    }
                }
            }
            if (visible && scrolled && floatingNavigationVisible && (showBackButton || actionButtons.isNotEmpty())) {
                Row(Modifier.fillMaxWidth().windowInsetsPadding(TopAppBarDefaults.windowInsets)
                    .padding(horizontal = 12.dp).padding(top = toolbarTopPadding + 4.dp)) {
                    // wm.l1.i/j -> wm.k1.s -> a1.b1: a shared smooth floating
                    // surface with 4dp shadow and .9 background alpha.
                    if (showBackButton) {
                        Box(Modifier.floatingHeaderSurface()) {
                            SettingsIconButton(onNavigateBack, backLabel) {
                                if (navigationIcon != null) navigationIcon()
                                else Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = null)
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (actionButtons.isNotEmpty()) {
                        // wm.l1.j → e20.w(case 9): one 12dp perimeter, with
                        // 12dp between actions. Padding every item doubles that gap.
                        Row(Modifier.floatingHeaderSurface().padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            actionButtons.forEach { action -> action() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Modifier.floatingHeaderSurface() = shadow(4.dp, SettingsStyle.PillShape)
    .background(MaterialTheme.colorScheme.floatingToolbarBackground.copy(alpha = .9f), SettingsStyle.PillShape)
    .clip(SettingsStyle.PillShape)
