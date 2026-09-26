@file:OptIn(dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi::class)

package me.kavishdevar.librepods.presentation.screens

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.res.stringResource
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.AACPManager
import me.kavishdevar.librepods.bluetooth.ATTHandles
import me.kavishdevar.librepods.data.AirPodsPro3
import me.kavishdevar.librepods.data.Capability
import me.kavishdevar.librepods.presentation.components.AudioSettings
import me.kavishdevar.librepods.presentation.components.CallControlSettings
import me.kavishdevar.librepods.presentation.components.ConnectionSettings
import me.kavishdevar.librepods.presentation.components.PressAndHoldSettings
import me.kavishdevar.librepods.presentation.components.StyledListItem
import me.kavishdevar.librepods.presentation.components.StyledToggle
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsUiState

// Page identity belongs to presentation; all pages share upstream state and actions.
enum class AirPodsSettingsPage { Home, AudioRouting, ControlsGestures, Battery }

internal fun LazyListScope.audioRoutingSettings(
    state: AirPodsUiState,
    setControlCommandBoolean: (AACPManager.Companion.ControlCommandIdentifiers, Boolean) -> Unit,
    setATTCharacteristicValue: (ATTHandles, ByteArray) -> Unit,
    onAutomaticEarDetectionChanged: (Boolean) -> Unit,
    onAutomaticConnectionChanged: (Boolean) -> Unit,
    setOffListeningMode: (Boolean) -> Unit,
    navigateToAdaptiveStrength: () -> Unit,
    navigateToEqualizer: () -> Unit,
    navigateToMicrophoneSettings: () -> Unit,
) {
    val capabilities = state.capabilities
    item(key = "audio") {
        val model = state.instance?.model ?: AirPodsPro3()
        val adaptiveVolumeCapability =
            model.capabilities.contains(Capability.ADAPTIVE_VOLUME)
        val conversationalAwarenessCapability =
            model.capabilities.contains(Capability.CONVERSATION_AWARENESS)
        val loudSoundReductionCapability =
            model.capabilities.contains(Capability.LOUD_SOUND_REDUCTION)
        val adaptiveAudioCapability =
            model.capabilities.contains(Capability.ADAPTIVE_VOLUME)

        val adaptiveVolumeChecked =
            state.controlStates[AACPManager.Companion.ControlCommandIdentifiers.ADAPTIVE_VOLUME_CONFIG]?.getOrNull(
                0
            ) == 0x01.toByte()
        val conversationalAwarenessChecked =
            state.controlStates[AACPManager.Companion.ControlCommandIdentifiers.CONVERSATION_DETECT_CONFIG]?.getOrNull(
                0
            ) == 0x01.toByte()

        AudioSettings(
            adaptiveVolumeCapability = adaptiveVolumeCapability,
            conversationalAwarenessCapability = conversationalAwarenessCapability,
            loudSoundReductionCapability = loudSoundReductionCapability,
            adaptiveAudioCapability = adaptiveAudioCapability,
            customEqCapability = true,
            adaptiveVolumeChecked = adaptiveVolumeChecked,
            onAdaptiveVolumeCheckedChange = { checked ->
                setControlCommandBoolean(
                    AACPManager.Companion.ControlCommandIdentifiers.ADAPTIVE_VOLUME_CONFIG,
                    checked
                )
            },
            conversationalAwarenessChecked = conversationalAwarenessChecked && state.isPremium,
            onConversationalAwarenessCheckedChange = { checked ->
                setControlCommandBoolean(
                    AACPManager.Companion.ControlCommandIdentifiers.CONVERSATION_DETECT_CONFIG,
                    checked
                )
            },
            loudSoundReductionChecked = state.loudSoundReductionEnabled,
            onLoudSoundReductionCheckedChange = { checked ->
                setATTCharacteristicValue(
                    ATTHandles.LOUD_SOUND_REDUCTION,
                    byteArrayOf(if (checked) 0x01.toByte() else 0x00.toByte())
                )
            },
            navigateToAdaptiveStrength = navigateToAdaptiveStrength,
            navigateToEqualizer = navigateToEqualizer,
            vendorIdHook = state.vendorIdHook,
            isPremium = state.isPremium
        )
    }

    item(key = "connection") {
        ConnectionSettings(
            automaticEarDetectionEnabled = state.automaticEarDetectionEnabled,
            onAutomaticEarDetectionChanged = onAutomaticEarDetectionChanged,
            automaticConnectionEnabled = state.automaticConnectionEnabled,
            onAutomaticConnectionChanged = onAutomaticConnectionChanged
        )
    }

    item(key = "microphone") {
        val id = AACPManager.Companion.ControlCommandIdentifiers.MIC_MODE

        val selectedModeText = when (state.controlStates[id]?.getOrNull(0) ?: 0x00.toByte()) {
            0x00.toByte() -> stringResource(R.string.microphone_automatic)
            0x01.toByte() -> stringResource(R.string.microphone_always_right)
            0x02.toByte() -> stringResource(R.string.microphone_always_left)
            else -> stringResource(R.string.microphone_automatic)
        }

        StyledListItem(
            name = stringResource(R.string.microphone_mode),
            description = selectedModeText,
            descriptionIsState = true,
            onClick = navigateToMicrophoneSettings
        )
    }

    if (capabilities.contains(Capability.SLEEP_DETECTION)) {
        item(key = "sleep_detection") {
            val id = AACPManager.Companion.ControlCommandIdentifiers.SLEEP_DETECTION_CONFIG
            StyledToggle(
                label = stringResource(R.string.sleep_detection),
                checked = state.controlStates[id]?.getOrNull(0) == 0x01.toByte(),
                onCheckedChange = { setControlCommandBoolean(id, it) },
                enabled = state.isPremium
            )
        }
    }

    if (capabilities.contains(Capability.LOUD_SOUND_REDUCTION)) {
        item(key = "off_listening") {
            val id = AACPManager.Companion.ControlCommandIdentifiers.ALLOW_OFF_OPTION
            StyledToggle(
                label = stringResource(R.string.off_listening_mode),
                description = stringResource(R.string.off_listening_mode_description),
                checked = state.controlStates[id]?.getOrNull(0) == 0x01.toByte(),
                onCheckedChange = setOffListeningMode
            )
        }
    }
}

internal fun LazyListScope.controlsGesturesSettings(
    state: AirPodsUiState,
    navigateToLeftLongPress: () -> Unit,
    navigateToRightLongPress: () -> Unit,
    navigateToCallControlScreen: (String) -> Unit,
    navigateToHeadTracking: () -> Unit,
) {
    val capabilities = state.capabilities
    if (capabilities.contains(Capability.STEM_CONFIG)) {
        item(key = "press_hold") {
            PressAndHoldSettings(
                leftAction = state.leftAction,
                rightAction = state.rightAction,
                navigateToLeftLongPress = navigateToLeftLongPress,
                navigateToRightLongPress = navigateToRightLongPress
            )
        }
    }
    item(key = "call_control") {
        val bytes =
            state.controlStates[AACPManager.Companion.ControlCommandIdentifiers.CALL_MANAGEMENT_CONFIG]?.take(
                2
            )?.toByteArray() ?: byteArrayOf(0x00, 0x00)
        val flipped = try {
            bytes[1] == 0x02.toByte()
        } catch (_: Exception) {
            false
        }
        CallControlSettings(
            flipped = flipped,
            navigateToCallControlScreen = navigateToCallControlScreen
        )
    }

    if (capabilities.contains(Capability.HEAD_GESTURES)) {
        item(key = "head_tracking") {
            StyledListItem(
                name = stringResource(R.string.head_gestures),
                description = if (state.headGesturesEnabled) stringResource(R.string.on) else stringResource(R.string.off),
                descriptionIsState = true,
                onClick = navigateToHeadTracking
            )
        }
    }
}

internal fun LazyListScope.batterySettings(
    state: AirPodsUiState,
    setDynamicEndOfCharge: (Boolean) -> Unit,
) {
    item(key = "dynamic_end_of_charge") {
        StyledToggle(
            label = stringResource(R.string.optimized_charging),
            description = stringResource(R.string.optimized_charging_description),
            checked = state.dynamicEndOfCharge,
            onCheckedChange = setDynamicEndOfCharge
        )
    }
}
