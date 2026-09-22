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

package me.kavishdevar.librepods.presentation.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.AACPManager.Companion.ControlCommandIdentifiers
import me.kavishdevar.librepods.bluetooth.ATTHandles
import me.kavishdevar.librepods.data.Capability
import me.kavishdevar.librepods.presentation.components.*
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsUiState
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel

@Composable
fun AccessibilitySettingsScreen(viewModel: AirPodsViewModel, navigateToPurchase: () -> Unit,
                                navigateToTransparencyCustomization: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    AccessibilitySettingsContent(state, viewModel::setControlCommandByte,
        viewModel::setControlCommandBoolean,
        onToneVolumeChanged = { viewModel.setControlCommandValue(ControlCommandIdentifiers.CHIME_VOLUME,
            byteArrayOf(it.toByte(), 0x50)) },
        onLoudSoundReductionChanged = { viewModel.setATTCharacteristicValue(ATTHandles.LOUD_SOUND_REDUCTION,
            byteArrayOf(if (it) 1 else 0)) },
        navigateToPurchase, navigateToTransparencyCustomization)
}

/** Presentation-only content; composition and incoming snapshots never issue commands. */
@Composable
fun AccessibilitySettingsContent(state: AirPodsUiState,
    setControlCommandByte: (ControlCommandIdentifiers, Byte) -> Unit,
    setControlCommandBoolean: (ControlCommandIdentifiers, Boolean) -> Unit,
    onToneVolumeChanged: (Int) -> Unit,
    onLoudSoundReductionChanged: (Boolean) -> Unit,
    navigateToPurchase: () -> Unit,
    navigateToTransparencyCustomization: () -> Unit,
) {
    val hearingAid = state.controlStates[ControlCommandIdentifiers.HEARING_AID]
    val hearingAidEnabled = hearingAid?.getOrNull(0) == 1.toByte() && hearingAid.getOrNull(1) == 1.toByte()
    val connected = state.isLocallyConnected
    val premiumEnabled = connected && state.isPremium
    SettingsDetailColumn {
        if (!state.isPremium) {
            StyledButton(onClick = navigateToPurchase, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.unlock_advanced_features))
            }
        }
        AccessibilityChoices(
            title = stringResource(R.string.press_speed),
            description = stringResource(R.string.press_speed_description),
            options = listOf(0 to stringResource(R.string.default_option),
                1 to stringResource(R.string.slower), 2 to stringResource(R.string.slowest)),
            selected = state.controlStates[ControlCommandIdentifiers.DOUBLE_CLICK_INTERVAL]?.getOrNull(0),
            enabled = connected,
            onSelect = { setControlCommandByte(ControlCommandIdentifiers.DOUBLE_CLICK_INTERVAL, it) })
        AccessibilityChoices(
            title = stringResource(R.string.press_and_hold_duration),
            description = stringResource(R.string.press_and_hold_duration_description),
            options = listOf(0 to stringResource(R.string.default_option),
                1 to stringResource(R.string.shorter), 2 to stringResource(R.string.shortest)),
            selected = state.controlStates[ControlCommandIdentifiers.CLICK_HOLD_INTERVAL]?.getOrNull(0),
            enabled = connected,
            onSelect = { setControlCommandByte(ControlCommandIdentifiers.CLICK_HOLD_INTERVAL, it) })
        StyledToggle(
            title = stringResource(R.string.noise_control),
            label = stringResource(R.string.noise_cancellation_single_airpod),
            description = stringResource(R.string.noise_cancellation_single_airpod_description),
            checked = state.controlStates[ControlCommandIdentifiers.ONE_BUD_ANC_MODE]?.getOrNull(0) == 1.toByte(),
            onCheckedChange = { setControlCommandBoolean(ControlCommandIdentifiers.ONE_BUD_ANC_MODE, it) },
            enabled = premiumEnabled)
        if (Capability.LOUD_SOUND_REDUCTION in state.capabilities && state.vendorIdHook) {
            StyledToggle(label = stringResource(R.string.loud_sound_reduction),
                description = stringResource(R.string.loud_sound_reduction_description),
                checked = state.loudSoundReductionEnabled,
                onCheckedChange = onLoudSoundReductionChanged, enabled = premiumEnabled)
        }
        if (!hearingAidEnabled && state.vendorIdHook) {
            StyledListItem(name = stringResource(R.string.customize_transparency_mode),
                onClick = navigateToTransparencyCustomization, enabled = premiumEnabled)
        }
        // A pending gesture belongs to this device/connection and entitlement lifetime.
        key(state.instance?.serialNumber, connected, state.isPremium) {
            ToneVolumeControl(
                reported = state.controlStates[ControlCommandIdentifiers.CHIME_VOLUME]?.getOrNull(0)
                    ?.toInt()?.and(0xff)?.takeIf { it in 0..100 },
                enabled = premiumEnabled, onChanged = onToneVolumeChanged)
        }
        if (Capability.SWIPE_FOR_VOLUME in state.capabilities) {
            StyledToggle(label = stringResource(R.string.volume_control),
                description = stringResource(R.string.volume_control_description),
                checked = state.controlStates[ControlCommandIdentifiers.VOLUME_SWIPE_MODE]?.getOrNull(0) == 1.toByte(),
                onCheckedChange = { setControlCommandBoolean(ControlCommandIdentifiers.VOLUME_SWIPE_MODE, it) },
                enabled = premiumEnabled)
            AccessibilityChoices(
                title = stringResource(R.string.volume_swipe_speed),
                description = stringResource(R.string.volume_swipe_speed_description),
                options = listOf(1 to stringResource(R.string.default_option),
                    2 to stringResource(R.string.longer), 3 to stringResource(R.string.longest)),
                selected = state.controlStates[ControlCommandIdentifiers.VOLUME_SWIPE_INTERVAL]?.getOrNull(0),
                enabled = connected,
                onSelect = { setControlCommandByte(ControlCommandIdentifiers.VOLUME_SWIPE_INTERVAL, it) })
        }
    }
}

@Composable
private fun AccessibilityChoices(title: String, description: String, options: List<Pair<Int, String>>,
                                 selected: Byte?, enabled: Boolean, onSelect: (Byte) -> Unit) {
    StyledList(title = title, description = description, modifier = Modifier.selectableGroup()) {
        options.forEach { (value, label) ->
            BudsChoiceRow(name = label, selected = selected == value.toByte(), enabled = enabled,
                onClick = { if (selected != value.toByte()) onSelect(value.toByte()) })
        }
    }
}

@Composable
private fun ToneVolumeControl(reported: Int?, enabled: Boolean, onChanged: (Int) -> Unit) {
    var pending by remember { mutableStateOf<Float?>(null) }
    val send by rememberUpdatedState(onChanged)
    // Only user edits create pending work. Preserve the existing 100 ms quiet period,
    // with cancellation owned by composition instead of an independent IO scope.
    LaunchedEffect(pending) {
        val edit = pending ?: return@LaunchedEffect
        delay(100)
        send(edit.toInt())
        pending = null
    }
    val shown = pending ?: (reported ?: 75).toFloat()
    StyledSlider(label = stringResource(R.string.tone_volume),
        description = stringResource(R.string.tone_volume_description), value = shown,
        onValueChange = { if (enabled && it != shown) pending = it },
        valueRange = 0f..100f, snapPoints = listOf(75f),
        startIcon = "\uDBC0\uDEA1", endIcon = "\uDBC0\uDEA9", independent = true, enabled = enabled)
}
