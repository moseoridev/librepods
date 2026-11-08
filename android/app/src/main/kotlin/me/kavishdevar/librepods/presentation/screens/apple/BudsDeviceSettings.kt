package me.kavishdevar.librepods.presentation.screens.apple

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.LayoutDirection
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
import me.kavishdevar.librepods.presentation.utils.LocalDebugMode
import me.kavishdevar.librepods.presentation.viewmodel.AppleUiState
import me.kavishdevar.librepods.presentation.viewmodel.AppleViewModel
import me.kavishdevar.librepods.ui.components.*
import me.kavishdevar.librepods.ui.theme.SettingsStyle
import me.kavishdevar.librepods.ui.theme.SettingsTheme
import kotlin.math.roundToInt

/** Presentation routes only. Transport, persistence and capabilities remain owned by rewrite. */
internal enum class DevicePage(val title: Int, val parent: DevicePage? = null) {
    Home(0), Audio(R.string.audio_routing, Home), Controls(R.string.controls_gestures, Home),
    Accessibility(R.string.accessibility, Home), Battery(R.string.battery, Home),
    Microphone(R.string.microphone_mode, Audio), Equalizer(R.string.equalizer, Audio),
    Routing(R.string.automatic_connection, Audio), Left(R.string.left, Controls), Right(R.string.right, Controls),
    Mute(R.string.mute_unmute, Controls), EndCall(R.string.hang_up, Controls),
    Head(R.string.head_gestures, Controls), Version(R.string.version, Home), Name(R.string.name, Home),
    Extras(R.string.librepods_options, Home)
}

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
    var page by rememberSaveable(actions) { mutableStateOf(initialPage) }
    val keyboard = LocalSoftwareKeyboardController.current
    val back = {
        if (page == DevicePage.Name) keyboard?.hide()
        if (page != DevicePage.Home) page = page.parent ?: DevicePage.Home else navigateBack?.invoke()
        Unit
    }
    BackHandler(page != DevicePage.Home, onBack = back)
    SettingsTheme(darkTheme = darkTheme) {
        key(page) {
            val scroll = rememberScrollState()
            SettingsScaffold(
                title = if (page == DevicePage.Home || page == DevicePage.Name) ui.metadata.name else stringResource(page.title),
                backLabel = stringResource(R.string.back),
                showBackButton = page != DevicePage.Home || navigateBack != null,
                onNavigateBack = back,
                navigationIcon = { BudsBackIcon() },
                scrollState = scroll,
                background = if (page == DevicePage.Home || page == DevicePage.Name) ({ BudsHomeBackdrop(darkTheme) }) else null,
            ) {
                // Groups do not share one gap: a titled group and a section label bring
                // their own 16dp lead, and the battery row brings its own 14dp trail.
                SettingsGroupColumn(Modifier.fillMaxWidth().padding(horizontal = SettingsStyle.pageInset())
                    .testTag("device-page-${if (page == DevicePage.Name) DevicePage.Home.name else page.name}")) {
                    val go: (DevicePage) -> Unit = { page = it }
                    when (page) {
                        DevicePage.Home -> DeviceHome(ui, go, actions::disconnect, mediaVolume, { actions.setControlCommand(Command.LISTENING_MODE, it) }, navigateToPurchase)
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
                        DevicePage.Name -> {
                            DeviceHome(ui, go, actions::disconnect, mediaVolume, { actions.setControlCommand(Command.LISTENING_MODE, it) }, navigateToPurchase)
                            val draft = rememberTextFieldState(ui.metadata.name)
                            val focus = remember { FocusRequester() }
                            SettingsDialog(visible = true, title = stringResource(R.string.name), onDismissRequest = back,
                                modifier = Modifier.testTag("device-page-Name"),
                                presentation = SettingsDialogPresentation.TextEntry,
                                actions = listOf(SettingsDialogAction(stringResource(R.string.cancel), back),
                                    SettingsDialogAction(stringResource(R.string.widget_save), {
                                        keyboard?.hide()
                                        actions.renameDevice(draft.text.toString().trim())
                                        page = DevicePage.Home
                                    }, enabled = draft.text.isNotBlank()))) {
                                SettingsInputField(draft, focusRequester = focus)
                                val window = LocalWindowInfo.current
                                LaunchedEffect(window.isWindowFocused) { if (window.isWindowFocused) focus.requestFocus() }
                            }
                        }
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
    }
}

/** Locally drawn functional chevron, using the source's 72-unit/24dp proportions. */
@Composable
private fun BudsBackIcon() {
    val ink = MaterialTheme.colorScheme.onSurface
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val path = remember { Path().apply {
        moveTo(15.04f, 3.62f)
        lineTo(7.26f, 11.29f)
        quadraticTo(6.96f, 11.59f, 6.96f, 12f)
        quadraticTo(6.96f, 12.41f, 7.26f, 12.71f)
        lineTo(15.04f, 20.38f)
    } }
    Canvas(Modifier.size(24.dp)) {
        scale(if (rtl) -1f else 1f, 1f) {
            scale(size.width / 24f, size.height / 24f, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                drawPath(path, ink, style = Stroke(1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

private fun AppleUiState.value(command: Command): Int? = state.controlStates[command]?.firstOrNull()?.toInt()?.and(255)
private fun AppleUiState.has(capability: BaseCapability) = capability in AirPodsSpecs.getSpec(metadata.model).baseCapabilities

@Composable
private fun DeviceHome(ui: AppleUiState, go: (DevicePage) -> Unit, disconnect: () -> Unit, mediaVolume: @Composable () -> Unit,
    selectMode: (Int) -> Unit, navigateToPurchase: () -> Unit) {
    DeviceBatteries(ui)
    SettingsList { SettingsListItem(name = stringResource(R.string.name), description = ui.metadata.name, onClick = { go(DevicePage.Name) }) }
    if (ui.has(BaseCapability.LISTENING_MODE)) {
        SettingsList(title = stringResource(R.string.listening_mode)) {
            val modes = buildList {
                if (ui.value(Command.ALLOW_OFF_OPTION) == 1) add(1)
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
                if ((mode != 4 || ui.has(BaseCapability.ADAPTIVE_AUDIO)) && (mode != 1 || ui.value(Command.ALLOW_OFF_OPTION) == 1)) {
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
    if (ui.has(BaseCapability.LISTENING_MODE)) CommandToggle(ui, vm, Command.ALLOW_OFF_OPTION,
        R.string.off_listening_mode, R.string.off_listening_mode_description)
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
