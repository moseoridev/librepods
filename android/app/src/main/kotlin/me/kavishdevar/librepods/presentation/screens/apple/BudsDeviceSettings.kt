package me.kavishdevar.librepods.presentation.screens.apple

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.compose.foundation.Image
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.core.bluetooth.aacp.types.ControlCommandIdentifier as Command
import me.kavishdevar.librepods.core.bluetooth.att.ATTHandle
import me.kavishdevar.librepods.core.data.StemAction
import me.kavishdevar.librepods.core.devices.*
import me.kavishdevar.librepods.presentation.icons.LocalIcons
import me.kavishdevar.librepods.presentation.navigation.pageTransitionBackground
import me.kavishdevar.librepods.presentation.navigation.rememberPageParallaxPx
import me.kavishdevar.librepods.presentation.navigation.forwardPageTransition
import me.kavishdevar.librepods.presentation.navigation.popPageTransition
import me.kavishdevar.librepods.presentation.navigation.predictivePopPageTransition
import me.kavishdevar.librepods.presentation.navigation.rememberPredictiveBackShiftPx
import me.kavishdevar.librepods.presentation.utils.LocalDebugMode
import me.kavishdevar.librepods.presentation.viewmodel.AppleUiState
import me.kavishdevar.librepods.presentation.viewmodel.AppleViewModel
import me.kavishdevar.librepods.ui.components.*
import me.kavishdevar.librepods.ui.theme.SettingsStyle
import me.kavishdevar.librepods.ui.theme.SettingsTheme
import kotlin.math.roundToInt

/**
 * Presentation routes only. Transport, persistence and capabilities remain owned by rewrite.
 * [Name] is not a route: the rename editor is the platform text-entry dialog over [Home].
 */
internal enum class DevicePage(val title: Int, val parent: DevicePage? = null) : NavKey {
    Home(0), Audio(R.string.audio_routing, Home), Controls(R.string.controls_gestures, Home),
    Accessibility(R.string.accessibility, Home), Battery(R.string.battery, Home),
    Microphone(R.string.microphone_mode, Audio), Equalizer(R.string.equalizer, Audio),
    Routing(R.string.automatic_connection, Audio), Left(R.string.left, Controls), Right(R.string.right, Controls),
    Mute(R.string.mute_unmute, Controls), EndCall(R.string.hang_up, Controls),
    Head(R.string.head_gestures, Controls), Version(R.string.version, Home), Name(R.string.name, Home),
    Extras(R.string.librepods_options, Home)
}

/** NavDisplay observes the stack through the snapshot system, so it must be a NavBackStack. */
private val DevicePageBackStackSaver = listSaver<NavBackStack<DevicePage>, Int>(
    save = { it.map(DevicePage::ordinal) },
    // A stack saved before the name editor became a dialog can still hold [DevicePage.Name].
    restore = { values ->
        NavBackStack(*values.map { DevicePage.entries[it] }
            .map { if (it == DevicePage.Name) DevicePage.Home else it }.toTypedArray())
    }
)

private fun DevicePageBackStack(initial: DevicePage): NavBackStack<DevicePage> =
    NavBackStack(
        *buildList {
            // The name editor is a dialog over Home rather than a page, so it starts at Home.
            val page = if (initial == DevicePage.Name) DevicePage.Home else initial
            // A page opened directly (tests, deep state restore) still has a Home to return to.
            var parent = page.parent
            while (parent != null) { add(0, parent); parent = parent.parent }
            add(page)
        }.toTypedArray()
    )

@Composable
fun BudsDeviceSettingsRoute(
    viewModel: AppleViewModel,
    navigateBack: (() -> Unit)?,
    navigateToHearingProtection: () -> Unit,
    navigateToHearingAid: () -> Unit,
    navigateToPurchase: () -> Unit,
    navigateToTransparencyCustomization: () -> Unit,
    navigateToRecording: () -> Unit,
    navigateToHeartRate: () -> Unit,
    navigateToDebug: () -> Unit,
    navigateToOverlays: () -> Unit,
    navigateToProximity: () -> Unit,
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val actions = remember(viewModel) { ViewModelSettingsActions(viewModel) }
    BudsDeviceSettings(ui, actions, navigateBack, navigateToHearingProtection,
        navigateToHearingAid, navigateToPurchase, navigateToTransparencyCustomization,
        navigateToRecording, navigateToHeartRate, navigateToDebug,
        navigateToOverlays, navigateToProximity, mediaVolume = { PhoneMediaVolume() })
}

/** The product composition, independent of Bluetooth, repositories and system volume. */
@Composable
internal fun BudsDeviceSettings(
    ui: AppleUiState,
    actions: DeviceSettingsActions,
    navigateBack: (() -> Unit)? = null,
    navigateToHearingProtection: () -> Unit = {},
    navigateToHearingAid: () -> Unit = {},
    navigateToPurchase: () -> Unit = {},
    navigateToTransparencyCustomization: () -> Unit = {},
    navigateToRecording: () -> Unit = {},
    navigateToHeartRate: () -> Unit = {},
    navigateToDebug: () -> Unit = {},
    navigateToOverlays: () -> Unit = {},
    navigateToProximity: () -> Unit = {},
    initialPage: DevicePage = DevicePage.Home,
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    mediaVolume: @Composable () -> Unit,
) {
    // Nested pages are real navigation destinations so the platform's predictive back gesture
    // (Navigation3 -> navigationevent) drives them, instead of a local BackHandler animation.
    val pages = rememberSaveable(actions, initialPage, saver = DevicePageBackStackSaver) {
        DevicePageBackStack(initialPage)
    }
    // A single-entry stack leaves the screen, matching the platform's root behaviour.
    val popPage: () -> Unit = { if (pages.size > 1) pages.removeAt(pages.lastIndex) else navigateBack?.invoke() }
    // NavDisplay keeps an entry's content for as long as the entry stays on the stack, so the
    // content must read the latest state instead of capturing the value it was first given.
    val uiState = rememberUpdatedState(ui)
    val keyboard = LocalSoftwareKeyboardController.current
    val pageParallaxPx = rememberPageParallaxPx()
    val predictiveShiftPx = rememberPredictiveBackShiftPx()
    // The name editor is the platform text-entry dialog over Home, not a page of its own:
    // a separate destination slides a second copy of Home in behind the dialog, which reads as
    // the whole background refreshing.
    var renaming by rememberSaveable(actions) { mutableStateOf(initialPage == DevicePage.Name) }
    val back: () -> Unit = { popPage(); Unit }
    SettingsTheme(darkTheme = darkTheme) {
        NavDisplay(
            modifier = Modifier.pageTransitionBackground(),
            backStack = pages,
            onBack = back,
            entryProvider = { page ->
                NavEntry(page) {
                    DevicePageContent(uiState, actions, page, pages, back, mediaVolume, navigateBack,
                        navigateToHearingProtection, navigateToHearingAid, navigateToPurchase,
                        navigateToTransparencyCustomization, navigateToRecording, navigateToHeartRate,
                        navigateToDebug, navigateToOverlays, navigateToProximity, darkTheme,
                        onRename = { renaming = true })
                }
            },
            transitionSpec = { forwardPageTransition(pageParallaxPx) },
            popTransitionSpec = { popPageTransition(pageParallaxPx) },
            // Without this, NavDisplay falls back to its own fadeIn + scaleOut(0.7f) preview,
            // which shrinks the whole screen much further than the design guidance.
            predictivePopTransitionSpec = { swipeEdge ->
                predictivePopPageTransition(swipeEdge, predictiveShiftPx)
            },
        )
        if (renaming && pages.lastOrNull() == DevicePage.Home) {
            NameEditorDialog(
                name = ui.metadata.name,
                onDismiss = { keyboard?.hide(); renaming = false },
                onSave = { renamed -> keyboard?.hide(); actions.renameDevice(renamed); renaming = false },
            )
        }
    }
}

/** The native text-entry editor, layered over the unchanged Home page. */
@Composable
private fun NameEditorDialog(name: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val draft = rememberTextFieldState(name)
    val focus = remember { FocusRequester() }
    SettingsDialog(visible = true, title = stringResource(R.string.name), onDismissRequest = onDismiss,
        modifier = Modifier.testTag("device-page-Name"),
        presentation = SettingsDialogPresentation.TextEntry,
        actions = listOf(SettingsDialogAction(stringResource(R.string.cancel), onDismiss),
            SettingsDialogAction(stringResource(R.string.widget_save), { onSave(draft.text.toString().trim()) },
                enabled = draft.text.isNotBlank()))) {
        SettingsInputField(draft, focusRequester = focus)
        val window = LocalWindowInfo.current
        LaunchedEffect(window.isWindowFocused) { if (window.isWindowFocused) focus.requestFocus() }
    }
}

@Composable
private fun DevicePageContent(
    uiState: State<AppleUiState>,
    actions: DeviceSettingsActions,
    page: DevicePage,
    pages: NavBackStack<DevicePage>,
    back: () -> Unit,
    mediaVolume: @Composable () -> Unit,
    navigateBack: (() -> Unit)?,
    navigateToHearingProtection: () -> Unit,
    navigateToHearingAid: () -> Unit,
    navigateToPurchase: () -> Unit,
    navigateToTransparencyCustomization: () -> Unit,
    navigateToRecording: () -> Unit,
    navigateToHeartRate: () -> Unit,
    navigateToDebug: () -> Unit,
    navigateToOverlays: () -> Unit,
    navigateToProximity: () -> Unit,
    darkTheme: Boolean,
    onRename: () -> Unit,
) {
    val ui = uiState.value
    val scroll = rememberScrollState()
    SettingsScaffold(
        title = if (page == DevicePage.Home) ui.metadata.displayName else stringResource(page.title),
        backLabel = stringResource(R.string.back),
        showBackButton = pages.size > 1 || navigateBack != null,
        onNavigateBack = back,
        navigationIcon = { BudsBackIcon() },
        scrollState = scroll,
        background = if (page == DevicePage.Home) ({ BudsHomeBackdrop(darkTheme) }) else null,
    ) {
        // Groups do not share one gap: a titled group and a section label bring
        // their own 16dp lead, and the battery row brings its own 14dp trail.
        SettingsGroupColumn(Modifier.fillMaxWidth().padding(horizontal = SettingsStyle.pageInset())
            .testTag("device-page-${page.name}")) {
            // Repeated taps must not stack the same destination twice.
            val go: (DevicePage) -> Unit = { if (pages.lastOrNull() != it) pages.add(it) }
            when (page) {
                DevicePage.Home -> DeviceHome(ui, go, onRename,
                    actions::disconnect, mediaVolume, { actions.setControlCommand(Command.LISTENING_MODE, it) }, navigateToPurchase)
                DevicePage.Audio -> AudioPage(ui, actions, go)
                DevicePage.Controls -> ControlsPage(ui, go)
                DevicePage.Accessibility -> AccessibilityPage(ui, actions)
                DevicePage.Battery -> BatteryPage(ui, actions)
                DevicePage.Microphone -> CommandChoices(ui, actions, Command.MIC_MODE,
                    listOf(0 to R.string.microphone_automatic, 2 to R.string.microphone_always_left, 1 to R.string.microphone_always_right))
                DevicePage.Equalizer -> EqualizerPage(ui, actions)
                DevicePage.Routing -> CommandChoices(ui, actions, Command.SMART_ROUTING_MODE,
                    listOf(1 to R.string.on, 2 to R.string.off))
                DevicePage.Left, DevicePage.Right -> LongPressPage(ui, actions, page == DevicePage.Left)
                DevicePage.Mute, DevicePage.EndCall -> CallPage(ui, actions, page == DevicePage.Mute)
                DevicePage.Head -> HeadPage(ui, actions)
                DevicePage.Version -> SettingsList {
                    SettingsListItem(name = stringResource(R.string.version), description = firmwareDisplayVersion(ui.metadata.version3))
                    SettingsListItem(name = stringResource(R.string.version) + " 1", description = ui.metadata.version1)
                    SettingsListItem(name = stringResource(R.string.version) + " 2", description = ui.metadata.version2)
                    if (firmwareDisplayVersion(ui.metadata.version3) != ui.metadata.version3)
                        SettingsListItem(name = stringResource(R.string.version) + " 3", description = ui.metadata.version3)
                }
                DevicePage.Name -> Unit
                DevicePage.Extras -> {
                    val cap = AirPodsSpecs.getSpec(ui.metadata.model).baseCapabilities
                    SettingsList {
                        SettingsListItem(name = stringResource(R.string.overlay_settings), onClick = navigateToOverlays)
                        SettingsListItem(name = stringResource(R.string.automatic_connection), onClick = navigateToProximity)
                        if (BaseCapability.PPE in cap) SettingsListItem(name = stringResource(R.string.hearing_protection), onClick = navigateToHearingProtection)
                        if (BaseCapability.HEARING_AID in cap && ui.vendorIdHook) SettingsListItem(name = stringResource(R.string.hearing_aid), onClick = navigateToHearingAid)
                        if ((ui.metadata.version3.firstOrNull()?.digitToIntOrNull() ?: 0) >= 8)
                            SettingsListItem(name = stringResource(R.string.recorder), onClick = navigateToRecording)
                        if (BaseCapability.HRM in cap) SettingsListItem(name = stringResource(R.string.heart_rate), onClick = navigateToHeartRate)
                        // Upstream applied transparency tuning through the same ATT handle as
                        // hearing aid, so it was hidden while the buds acted as hearing aids.
                        val hearingAidEnabled = ui.state.controlStates[Command.HEARING_AID]?.let {
                            it.getOrNull(0)?.toInt() == 1 && it.getOrNull(1)?.toInt() == 1
                        } == true
                        if (!hearingAidEnabled && ui.vendorIdHook) SettingsListItem(
                            name = stringResource(R.string.customize_transparency_mode),
                            onClick = navigateToTransparencyCustomization, enabled = ui.isPremium)
                        if (LocalDebugMode.current) SettingsListItem(name = "Debug", onClick = navigateToDebug)
                    }
                    SettingsToggle(label = stringResource(R.string.disconnect_when_not_wearing), checked = ui.settings.disconnectWhenNotWearing,
                        onCheckedChange = { enabled -> actions.updateSettings { it.copy(disconnectWhenNotWearing = enabled) } })
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Filled navigation outline and white-cache/tint stages of ic_tw_ic_ab_back_mtrl. */
@Composable
private fun BudsBackIcon() {
    val ink = MaterialTheme.colorScheme.onSurface
    val image = remember {
        ImageVector.Builder("SettingsBack", 72.dp, 72.dp, 72f, 72f, autoMirror = true).apply {
            // An invisible stroke is omitted by the XML parser. Keeping its black RGB
            // brush here prevents VectorPainter's single-color Alpha8 cache path.
            path(fill = SolidColor(Color.White),
                strokeLineWidth = 1f, pathFillType = PathFillType.EvenOdd) {
                moveTo(46.6923f, 12.4566f)
                curveTo(47.5773f, 11.5836f, 47.5893f, 10.1586f, 46.7163f, 9.2736f)
                curveTo(45.8433f, 8.3916f, 44.4183f, 8.3826f, 43.5333f, 9.2556f)
                lineTo(20.1993f, 32.2656f)
                curveTo(19.1913f, 33.2586f, 18.6363f, 34.5876f, 18.6363f, 36.0036f)
                curveTo(18.6363f, 37.4166f, 19.1913f, 38.7486f, 20.1993f, 39.7416f)
                lineTo(43.5333f, 62.7486f)
                curveTo(43.9713f, 63.1776f, 44.5413f, 63.3936f, 45.1143f, 63.3936f)
                curveTo(45.6933f, 63.3936f, 46.2753f, 63.1686f, 46.7163f, 62.7246f)
                curveTo(47.5893f, 61.8396f, 47.5773f, 60.4146f, 46.6923f, 59.5416f)
                lineTo(23.3613f, 36.5346f)
                curveTo(23.1633f, 36.3456f, 23.1363f, 36.1206f, 23.1363f, 36.0036f)
                curveTo(23.1363f, 35.8866f, 23.1633f, 35.6586f, 23.3613f, 35.4696f)
                lineTo(46.6923f, 12.4566f)
                close()
            }
        }.build()
    }
    Image(rememberVectorPainter(image), null, Modifier.size(24.dp), colorFilter = ColorFilter.tint(ink))
}

private fun AppleUiState.value(command: Command): Int? = state.controlStates[command]?.firstOrNull()?.toInt()?.and(255)
private fun AppleUiState.has(capability: BaseCapability) = capability in AirPodsSpecs.getSpec(metadata.model).baseCapabilities

/**
 * Control command 0x34 has no query request and some bonds never report it, so fall back to
 * the value the user last stored. Without this both the Off mode and its toggle disappear.
 */
private fun AppleUiState.allowOffListeningMode(): Boolean =
    state.controlStates[Command.ALLOW_OFF_OPTION]?.firstOrNull()?.toInt()?.and(255)?.let { it == 1 }
        ?: settings.allowOffListeningMode

@Composable
private fun DeviceHome(ui: AppleUiState, go: (DevicePage) -> Unit, onRename: () -> Unit, disconnect: () -> Unit,
    mediaVolume: @Composable () -> Unit, selectMode: (Int) -> Unit, navigateToPurchase: () -> Unit) {
    DeviceBatteries(ui)
    SettingsList { SettingsListItem(name = stringResource(R.string.name), description = ui.metadata.displayName, onClick = onRename) }
    if (ui.has(BaseCapability.LISTENING_MODE)) {
        SettingsList(title = stringResource(R.string.listening_mode)) {
            val modes = buildList {
                if (ui.allowOffListeningMode()) add(1)
                add(3)
                if (ui.has(BaseCapability.ADAPTIVE_AUDIO)) add(4)
                add(2)
            }
            // A requested or persisted control value is not an acknowledgement from this session.
            item { _, _ -> SettingsModeStrip(modes, ui.state.confirmedControlStates[Command.LISTENING_MODE]?.firstOrNull()?.toInt(),
                onSelect = { mode -> if (ui.connectionState == ConnectionState.CONNECTED) selectMode(mode) },
                label = { modeLabel(it) }, artwork = { mode, selected -> BudsModeArtwork(mode, selected) }) }
        }
    }
    mediaVolume()
    SettingsList {
        SettingsListItem(name = stringResource(R.string.audio_routing), onClick = { go(DevicePage.Audio) }, homeMenu = true,
            leadingContent = { BudsMenuIcon(BudsMenuArtwork.Sound) })
        SettingsListItem(name = stringResource(R.string.controls_gestures), onClick = { go(DevicePage.Controls) }, homeMenu = true,
            leadingContent = { BudsMenuIcon(BudsMenuArtwork.Touch) })
        SettingsListItem(name = stringResource(R.string.accessibility), onClick = { go(DevicePage.Accessibility) }, homeMenu = true,
            leadingContent = { BudsMenuIcon(BudsMenuArtwork.Accessibility) })
    }
    SettingsList { SettingsListItem(name = stringResource(R.string.battery), onClick = { go(DevicePage.Battery) }) }
    SettingsList(title = stringResource(R.string.about)) {
        SettingsListItem(name = stringResource(R.string.model_name), description = ui.metadata.modelName)
        SettingsListItem(name = stringResource(R.string.model_number), description = ui.metadata.modelNumber)
        SettingsListItem(name = stringResource(R.string.serial_number), description = ui.metadata.serialNumber)
        SettingsListItem(name = stringResource(R.string.version), description = firmwareDisplayVersion(ui.metadata.version3), onClick = { go(DevicePage.Version) })
    }
    SettingsList {
        SettingsListItem(name = stringResource(R.string.librepods_options), onClick = { go(DevicePage.Extras) })
        SettingsListItem(name = stringResource(R.string.disconnect), onClick = disconnect)
    }
    if (!ui.isPremium) SettingsButton(onClick = navigateToPurchase, style = SettingsButtonStyle.Filled,
        modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.unlock_advanced_features))
    }
}

@Composable
private fun DeviceBatteries(ui: AppleUiState) {
    val spec = AirPodsSpecs.getSpec(ui.metadata.model)
    val batteries = ui.state.battery.associateBy { it.component }
    val primary = if (spec.components.any { it.type == DeviceComponent.HEADSET }) listOf(BatteryComponent.HEADSET)
        else listOf(BatteryComponent.LEFT, BatteryComponent.RIGHT)
    // Max specs inherit a default case image upstream, but have only a headset component.
    val caseImage = spec.caseImageRes.takeUnless { BatteryComponent.HEADSET in primary }
    val icons = LocalIcons.current
    val iconMap = icons.IconMap
    val meter: @Composable (BatteryComponent) -> Unit = { component ->
        val battery = batteries[component]
        val level = battery?.level?.takeIf { it in 0..100 }
        val label = stringResource(when (component) {
            BatteryComponent.LEFT -> R.string.left
            BatteryComponent.RIGHT -> R.string.right
            BatteryComponent.CASE -> R.string.case_alt
            BatteryComponent.HEADSET -> R.string.airpods_label
        })
        val percentage = level?.let { "$it%" } ?: "—"
        val charging = battery?.status == BatteryStatus.CHARGING || battery?.status == BatteryStatus.OPTIMIZED_CHARGING
        val deviceComponent = when (component) {
            BatteryComponent.LEFT -> DeviceComponent.LEFT
            BatteryComponent.RIGHT -> DeviceComponent.RIGHT
            BatteryComponent.CASE -> DeviceComponent.CASE
            BatteryComponent.HEADSET -> DeviceComponent.HEADSET
        }
        val iconName = spec.components.firstOrNull { it.type == deviceComponent }?.iconName
        val artwork = iconMap[iconName] ?: when (component) {
            BatteryComponent.LEFT -> icons.LeftCircleFill
            BatteryComponent.RIGHT -> icons.RightCircleFill
            BatteryComponent.CASE -> icons.AirPods4Case
            BatteryComponent.HEADSET -> iconMap[spec.genericIconName] ?: icons.Headphones
        }
        val description = if (charging) "$label $percentage ${stringResource(R.string.charging_label)}" else "$label $percentage"
        SettingsBatteryMeter(level, label, percentage, description, maxWidth = 92.dp,
            modifier = Modifier.testTag("battery-${component.name}"),
            nameContent = {
                if (charging) Icon(icons.Bolt, null, Modifier.size(12.dp))
                Text(label)
            }, artwork = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(artwork, null, Modifier.fillMaxSize())
                }
            })
    }
    // The source battery row carries 14dp of its own bottom padding and the first group
    // starts right after it, so this block supplies the whole gap below the meters.
    Column(Modifier.fillMaxWidth().ownTrailingSpace(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(6.dp))
        // AirPods art has different proportions from the Buds hero. Bound it to
        // the source's 92dp hero region, sharing the battery row's group weights.
        Row(Modifier.widthIn(max = 280.dp).fillMaxWidth().height(92.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(spec.primaryImageRes), null,
                Modifier.weight(primary.size.toFloat()).fillMaxHeight().padding(top = 12.dp).testTag("battery-primary-art"),
                contentScale = ContentScale.Fit)
            caseImage?.let { image ->
                Box(Modifier.weight(1f).fillMaxHeight().padding(start = 3.dp, top = 12.dp)) {
                    Image(painterResource(image), null, Modifier.fillMaxSize().testTag("battery-case-art"), contentScale = ContentScale.Fit)
                }
            }
        }
        SettingsBatteryRow(primaryMeters = primary.map { component -> @Composable { meter(component) } },
            modifier = Modifier.padding(vertical = 14.dp),
            secondaryMeter = if (caseImage != null) ({ meter(BatteryComponent.CASE) }) else null)
    }
}

@Composable
private fun modeLabel(mode: Int): String = stringResource(when (mode) {
    1 -> R.string.off; 2 -> R.string.noise_cancellation; 4 -> R.string.adaptive; else -> R.string.transparency
})

@Composable
private fun AudioPage(ui: AppleUiState, vm: DeviceSettingsActions, go: (DevicePage) -> Unit) {
    if (ui.has(BaseCapability.ADAPTIVE_VOLUME)) CommandToggle(ui, vm, Command.ADAPTIVE_VOLUME_CONFIG,
        R.string.personalized_volume, R.string.personalized_volume_description, premium = true)
    if (ui.has(BaseCapability.CONVERSATION_AWARENESS)) CommandToggle(ui, vm, Command.CONVERSATION_DETECT_CONFIG,
        R.string.conversation_awareness, R.string.conversation_awareness_description, premium = true)
    if ((ui.metadata.version3.firstOrNull()?.digitToIntOrNull() ?: 0) >= 9)
        SettingsList { SettingsListItem(name = stringResource(R.string.equalizer), onClick = { go(DevicePage.Equalizer) }) }
    SettingsList {
        SettingsToggle(label = stringResource(R.string.ear_detection), checked = ui.value(Command.EAR_DETECTION_CONFIG) == 1,
            enabled = ui.connectionState == ConnectionState.CONNECTED && ui.value(Command.EAR_DETECTION_CONFIG) != null,
            onCheckedChange = { enabled ->
                vm.setControlCommand(Command.EAR_DETECTION_CONFIG, enabled)
                vm.updateSettings { it.copy(earDetectionEnabled = enabled) }
            })
        SettingsListItem(name = stringResource(R.string.automatic_connection), onClick = { go(DevicePage.Routing) },
            descriptionIsState = true,
            description = ui.value(Command.SMART_ROUTING_MODE)?.let { stringResource(if (it == 1) R.string.on else R.string.off) })
    }
    SettingsList { SettingsListItem(name = stringResource(R.string.microphone_mode), onClick = { go(DevicePage.Microphone) },
        descriptionIsState = true,
        description = ui.value(Command.MIC_MODE)?.let { stringResource(when(it) { 2 -> R.string.microphone_always_left; 1 -> R.string.microphone_always_right; else -> R.string.microphone_automatic }) }) }
    if (ui.has(BaseCapability.ADAPTIVE_AUDIO)) CommandSlider(stringResource(R.string.customize_adaptive_audio),
        ui.value(Command.AUTO_ANC_STRENGTH), enabled = ui.connectionState == ConnectionState.CONNECTED && ui.isPremium) {
        vm.setControlCommand(Command.AUTO_ANC_STRENGTH, it)
    }
    if (ui.has(BaseCapability.SLEEP_DETECTION)) CommandToggle(ui, vm, Command.SLEEP_DETECTION_CONFIG, R.string.sleep_detection, premium = true)
}

@Composable
private fun ControlsPage(ui: AppleUiState, go: (DevicePage) -> Unit) {
    if (ui.has(BaseCapability.STEM_CONFIG)) SettingsList(title = stringResource(R.string.press_and_hold_airpods)) {
        SettingsListItem(name = stringResource(R.string.left), onClick = { go(DevicePage.Left) }, descriptionIsState = true, description = stringResource(
            if (ui.settings.leftLongPressAction == StemAction.DIGITAL_ASSISTANT) R.string.digital_assistant else R.string.listening_mode))
        SettingsListItem(name = stringResource(R.string.right), onClick = { go(DevicePage.Right) }, descriptionIsState = true, description = stringResource(
            if (ui.settings.rightLongPressAction == StemAction.DIGITAL_ASSISTANT) R.string.digital_assistant else R.string.listening_mode))
    }
    val flipped = ui.state.controlStates[Command.CALL_MANAGEMENT_CONFIG]?.getOrNull(1)?.toInt() == 2
    SettingsList(title = stringResource(R.string.call_controls)) {
        SettingsListItem(name = stringResource(R.string.answer_call), description = stringResource(R.string.press_once), descriptionIsState = true)
        SettingsListItem(name = stringResource(R.string.mute_unmute), onClick = { go(DevicePage.Mute) },
            descriptionIsState = true,
            description = stringResource(if (flipped) R.string.press_twice else R.string.press_once))
        SettingsListItem(name = stringResource(R.string.hang_up), onClick = { go(DevicePage.EndCall) },
            descriptionIsState = true,
            description = stringResource(if (flipped) R.string.press_once else R.string.press_twice))
    }
    if (ui.has(BaseCapability.HEAD_GESTURES)) SettingsList {
        SettingsListItem(name = stringResource(R.string.head_gestures), onClick = { go(DevicePage.Head) },
            descriptionIsState = true,
            description = stringResource(if (ui.settings.headGesturesEnabled) R.string.on else R.string.off))
    }
}

@Composable
private fun LongPressPage(ui: AppleUiState, vm: DeviceSettingsActions, left: Boolean) {
    val action = if (left) ui.settings.leftLongPressAction else ui.settings.rightLongPressAction
    SettingsList {
        SettingsChoiceRow(name = stringResource(R.string.listening_mode), selected = action == StemAction.CYCLE_listening_mode_MODES,
            onClick = { vm.setLongPressAction(if (left) "left" else "right", StemAction.CYCLE_listening_mode_MODES) })
        SettingsChoiceRow(name = stringResource(R.string.digital_assistant), selected = action == StemAction.DIGITAL_ASSISTANT,
            onClick = { vm.setLongPressAction(if (left) "left" else "right", StemAction.DIGITAL_ASSISTANT) })
    }
    if (action == StemAction.CYCLE_listening_mode_MODES) {
        val configured = ui.value(Command.LISTENING_MODE_CONFIGS)
        SettingsList(title = stringResource(R.string.listening_mode),
            description = stringResource(R.string.press_and_hold_listening_mode_description)) {
            listOf(1 to 1, 3 to 4, 4 to 8, 2 to 2).forEach { (mode, bit) ->
                if ((mode != 4 || ui.has(BaseCapability.ADAPTIVE_AUDIO)) && (mode != 1 || ui.allowOffListeningMode())) {
                    item { _, _ -> SettingsMultiChoiceRow(modeLabel(mode), configured?.and(bit) != 0 && configured != null,
                        onCheckedChange = { checked ->
                            // Preserve all other bits; independent screenshot choices have no Samsung modal rule.
                            configured?.let { vm.setControlCommand(Command.LISTENING_MODE_CONFIGS,
                                if (checked) it or bit else it and bit.inv()) }
                        }, enabled = configured != null && (ui.connectionState == ConnectionState.CONNECTED),
                        description = stringResource(when (mode) {
                            1 -> R.string.listening_mode_off_description
                            2 -> R.string.listening_mode_noise_cancellation_description
                            3 -> R.string.listening_mode_transparency_description
                            else -> R.string.listening_mode_adaptive_description
                        }), leadingContent = { BudsModeArtwork(mode, compact = true) }) }
                }
            }
        }
    }
}

@Composable
private fun CallPage(ui: AppleUiState, vm: DeviceSettingsActions, mute: Boolean) {
    val bytes = ui.state.controlStates[Command.CALL_MANAGEMENT_CONFIG]
    val flipped = bytes?.getOrNull(1)?.toInt() == 2
    SettingsList {
        listOf(false, true).forEach { double ->
            val desiredFlipped = if (mute) double else !double
            SettingsChoiceRow(name = stringResource(if (double) R.string.press_twice else R.string.press_once),
                selected = flipped == desiredFlipped, enabled = bytes != null && (ui.connectionState == ConnectionState.CONNECTED),
                onClick = { vm.setControlCommand(Command.CALL_MANAGEMENT_CONFIG,
                    byteArrayOf(0, if (desiredFlipped) 2 else 3)) })
        }
    }
}

@Composable
private fun HeadPage(ui: AppleUiState, vm: DeviceSettingsActions) {
    // Upstream kept the toggle reachable once enabled, so a lapsed purchase does not
    // silently disable a feature the wearer is already using.
    SettingsToggle(label = stringResource(if (ui.settings.headGesturesEnabled) R.string.settings_in_use else R.string.settings_not_in_use),
        checked = ui.settings.headGesturesEnabled, header = true,
        enabled = ui.isPremium || ui.settings.headGesturesEnabled,
        onCheckedChange = vm::setHeadGesturesEnabled)
    SettingsList(description = stringResource(R.string.head_gestures_details)) {
        SettingsListItem(name = stringResource(R.string.answer_call), description = stringResource(R.string.nod_head), descriptionIsState = true)
        SettingsListItem(name = stringResource(R.string.decline_call_label), description = stringResource(R.string.shake_head), descriptionIsState = true)
    }
}

@Composable
private fun AccessibilityPage(ui: AppleUiState, vm: DeviceSettingsActions) {
    CommandChoices(ui, vm, Command.DOUBLE_CLICK_INTERVAL,
        listOf(0 to R.string.default_option, 1 to R.string.slower, 2 to R.string.slowest), R.string.press_speed, R.string.press_speed_description)
    CommandChoices(ui, vm, Command.CLICK_HOLD_INTERVAL,
        listOf(0 to R.string.default_option, 1 to R.string.shorter_label, 2 to R.string.shortest_label), R.string.press_and_hold_duration, R.string.press_and_hold_duration_description)
    if (ui.has(BaseCapability.LISTENING_MODE)) Column(Modifier.ownLeadingSpace()) {
        SettingsSectionLabel(stringResource(R.string.listening_mode))
        CommandToggle(ui, vm, Command.ONE_BUD_ANC_MODE, R.string.noise_cancellation_single_airpod, R.string.noise_cancellation_single_airpod_description, premium = true)
    }
    CommandSlider(stringResource(R.string.tone_volume), ui.value(Command.CHIME_VOLUME),
        enabled = (ui.connectionState == ConnectionState.CONNECTED) && ui.isPremium, description = stringResource(R.string.tone_volume_description)) {
        vm.setControlCommand(Command.CHIME_VOLUME, it)
    }
    if (ui.has(BaseCapability.SWIPE_FOR_VOLUME)) {
        Column(Modifier.ownLeadingSpace()) {
            SettingsSectionLabel(stringResource(R.string.volume_control))
            CommandToggle(ui, vm, Command.VOLUME_SWIPE_MODE, R.string.volume_swipe, R.string.volume_control_description, premium = true)
        }
        // Keep the existing firmware-facing mapping until the conflicting 0/1-based documentation is resolved.
        CommandChoices(ui, vm, Command.VOLUME_SWIPE_INTERVAL,
            listOf(1 to R.string.default_option, 2 to R.string.longer, 3 to R.string.longest), R.string.volume_swipe_speed, R.string.volume_swipe_speed_description)
    }
    if (ui.has(BaseCapability.LISTENING_MODE)) SettingsToggle(label = stringResource(R.string.off_listening_mode),
        description = stringResource(R.string.off_listening_mode_description),
        checked = ui.allowOffListeningMode(),
        // The command is the source of truth while connected, but it is also the only way to
        // keep the choice: the firmware never reports 0x34 back, so persist what was requested.
        enabled = ui.connectionState == ConnectionState.CONNECTED,
        onCheckedChange = { checked ->
            vm.setControlCommand(Command.ALLOW_OFF_OPTION, checked)
            vm.updateSettings { it.copy(allowOffListeningMode = checked) }
        })
    if (ui.has(BaseCapability.LOUD_SOUND_REDUCTION) && ui.vendorIdHook)
        SettingsToggle(label = stringResource(R.string.loud_sound_reduction), checked = ui.state.loudSoundReductionEnabled,
            enabled = ui.isPremium && (ui.connectionState == ConnectionState.CONNECTED),
            onCheckedChange = { vm.writeATTCharacteristic(ATTHandle.LOUD_SOUND_REDUCTION, byteArrayOf(if (it) 1 else 0)) })
}

@Composable
private fun BatteryPage(ui: AppleUiState, vm: DeviceSettingsActions) {
    SettingsDescription(stringResource(R.string.airpods_battery_lifespan))
    CommandToggle(ui, vm, Command.DYNAMIC_END_OF_CHARGE, R.string.optimized_charging, R.string.optimized_charging_description)
}

@Composable
private fun CommandToggle(ui: AppleUiState, vm: DeviceSettingsActions, command: Command, label: Int,
    description: Int? = null, premium: Boolean = false, onChange: ((Boolean) -> Unit)? = null) {
    SettingsToggle(label = stringResource(label), description = description?.let { stringResource(it) },
        checked = ui.value(command) == 1,
        enabled = (ui.connectionState == ConnectionState.CONNECTED) && (!premium || ui.isPremium) && ui.value(command) != null,
        onCheckedChange = { checked ->
            if (onChange != null) { onChange(checked) } else { vm.setControlCommand(command, checked) }
        })
}

@Composable
private fun CommandChoices(ui: AppleUiState, vm: DeviceSettingsActions, command: Command, options: List<Pair<Int, Int>>, title: Int? = null, description: Int? = null) {
    SettingsList(title = title?.let { stringResource(it) }, description = description?.let { stringResource(it) }) {
        options.forEach { (value, label) ->
            SettingsChoiceRow(name = stringResource(label), selected = ui.value(command) == value,
                enabled = (ui.connectionState == ConnectionState.CONNECTED) && ui.value(command) != null,
                onClick = { vm.setControlCommand(command, value) })
        }
    }
}

@Composable
private fun CommandSlider(label: String, remote: Int?, enabled: Boolean, description: String? = null, send: (Int) -> Unit) {
    var value by remember { mutableIntStateOf(remote ?: 0) }
    var pending by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(remote) { if (pending == null) value = remote ?: 0 }
    val sendLatest by rememberUpdatedState(send)
    DisposableEffect(Unit) { onDispose { pending?.let(sendLatest) } }
    LaunchedEffect(pending) {
        val next = pending ?: return@LaunchedEffect
        delay(100)
        sendLatest(next)
        pending = null
    }
    Column {
        SettingsCard {
            SettingsSeekBarRow(label, value.toFloat(), { value = it.roundToInt(); pending = value }, 0f..100f,
                startLabel = "", endLabel = remote?.let { "$value%" } ?: "—",
                enabled = enabled && remote != null,
                activeTrackColor = androidx.compose.ui.graphics.Color.Unspecified)
        }
        description?.let { SettingsDescription(it, Modifier.padding(top = SettingsStyle.FooterTop)) }
    }
}

/** Source footer typography/insets, also used for introductory page explanations. */
@Composable
private fun SettingsDescription(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.padding(horizontal = SettingsStyle.RowInset), style = SettingsStyle.RowSummary,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun EqualizerPage(ui: AppleUiState, vm: DeviceSettingsActions) {
    val eq = ui.state.customEq
    var levels by remember(eq.low, eq.mid, eq.high) { mutableStateOf(listOf(eq.low, eq.mid, eq.high)) }
    var pending by remember { mutableStateOf<List<Int>?>(null) }
    DisposableEffect(vm) { onDispose { pending?.let { vm.setCustomEq(it[0], it[1], it[2]) } } }
    LaunchedEffect(pending) {
        val values = pending ?: return@LaunchedEffect
        delay(100)
        vm.setCustomEq(values[0], values[1], values[2])
        pending = null
    }
    val connected = ui.connectionState == ConnectionState.CONNECTED
    SettingsList(description = stringResource(R.string.airpods_equalizer_description)) {
        SettingsChoiceRow(name = stringResource(R.string.recommended), selected = !eq.isEnabled(),
            enabled = connected, onClick = { vm.setCustomEqEnabled(false) })
        SettingsChoiceRow(name = stringResource(R.string.custom), selected = eq.isEnabled(),
            enabled = connected, onClick = { vm.setCustomEqEnabled(true) })
    }
    SettingsCard {
        val labels = listOf(stringResource(R.string.eq_low), stringResource(R.string.eq_mid), stringResource(R.string.eq_high))
        SettingsEqualizer(labels.mapIndexed { i, label -> SettingsEqualizerBand(label, levels[i], label) },
            onValueChange = { i, value -> levels = levels.toMutableList().also { it[i] = value }; pending = levels },
            enabled = eq.isEnabled() && (ui.connectionState == ConnectionState.CONNECTED), valueRange = 0..100,
            valueLabel = { it.toString() })
    }
    SettingsButton(onClick = { levels = listOf(50, 50, 50); pending = levels }, enabled = eq.isEnabled() && connected, style = SettingsButtonStyle.Tonal) {
        Text(stringResource(R.string.reset))
    }
}

/** Only the visible, STARTED home observes system media volume. No polling or chime commands. */
@Composable
private fun PhoneMediaVolume() {
    val context = LocalContext.current
    val audio = remember(context) { context.getSystemService(AudioManager::class.java) }
    var volume by remember { mutableIntStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC)) }
    var maximum by remember { mutableIntStateOf(audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) }
    LifecycleStartEffect(audio) {
        fun refresh() { maximum = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC); volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC) }
        val receiver = object : BroadcastReceiver() { override fun onReceive(context: Context?, intent: Intent?) { refresh() } }
        refresh()
        ContextCompat.registerReceiver(context, receiver, IntentFilter("android.media.VOLUME_CHANGED_ACTION"), ContextCompat.RECEIVER_NOT_EXPORTED)
        onStopOrDispose { context.unregisterReceiver(receiver) }
    }
    MediaVolumeControl(volume, maximum, !audio.isVolumeFixed) {
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, it, 0)
        volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
    }
}

@Composable
internal fun MediaVolumeControl(volume: Int, maximum: Int, enabled: Boolean, onValueChange: (Int) -> Unit) {
    val label = stringResource(R.string.media_volume_label)
    SettingsCard {
        // Same title/inset geometry as wm.k1.D / SettingsSeekBarRow. The
        // media-specific icon slots adapt the iOS volume role without changing audio state.
        Column(Modifier.fillMaxWidth().padding(horizontal = SettingsStyle.RowInset).padding(bottom = 12.dp)) {
            Column(Modifier.heightIn(min = SettingsStyle.RowMinHeight).padding(vertical = SettingsStyle.RowVerticalPadding)) {
                Text(label, style = SettingsStyle.RowTitle)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BudsMenuIcon(BudsMenuArtwork.QuietSound)
                SettingsSeekBar(volume.toFloat(), { onValueChange(it.roundToInt()) },
                    0f..maximum.coerceAtLeast(1).toFloat(), Modifier.weight(1f).padding(horizontal = 12.dp)
                        .semantics { contentDescription = label }, enabled = enabled && maximum > 0,
                    steps = (maximum - 1).coerceAtLeast(0))
                BudsMenuIcon(BudsMenuArtwork.Sound)
            }
        }
    }
}
