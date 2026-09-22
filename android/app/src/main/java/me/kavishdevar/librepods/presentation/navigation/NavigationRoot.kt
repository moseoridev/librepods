package me.kavishdevar.librepods.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.kyant.backdrop.backdrops.LayerBackdrop
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.components.StyledScaffold
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel

@Composable
fun NavigationRoot(
    showReleaseNotes: Boolean = false,
    updatesShown: () -> Unit = {},
    showOnboarding: Boolean = false,
    onboardingComplete: () -> Unit = {},
    airPodsViewModel: AirPodsViewModel
) {
    val backStack = remember {
        mutableStateListOf(
            when {
                showOnboarding -> Screen.Onboarding
                showReleaseNotes -> Screen.ReleaseNotes
                else -> Screen.AirPodsSettings
            }
        )
    }

    val currentScreen = backStack.last()

    val state by airPodsViewModel.uiState.collectAsState()

    val title = when (currentScreen) {
        Screen.Onboarding -> ""
        Screen.AirPodsSettings -> if (state.isLocallyConnected) state.deviceName else stringResource(R.string.app_name)
        Screen.AudioRouting -> stringResource(R.string.audio_routing)
        Screen.ControlsGestures -> stringResource(R.string.controls_gestures)
        Screen.Battery -> stringResource(R.string.battery)
        Screen.Accessibility -> stringResource(R.string.accessibility)
        Screen.AdaptiveStrength -> stringResource(R.string.customize_adaptive_audio)
        Screen.AppSettings -> stringResource(R.string.settings)
//        Screen.CameraControl -> stringResource(R.string.camera_control)
        Screen.Equalizer -> stringResource(R.string.equalizer)
        Screen.HeadTracking -> stringResource(R.string.head_gestures)
        Screen.HearingAid -> stringResource(R.string.hearing_aid)
        Screen.HearingAidAdjustments -> stringResource(R.string.adjustments)
        Screen.HearingProtection -> stringResource(R.string.hearing_protection)
        is Screen.LongPress -> currentScreen.bud
        Screen.OpenSourceLicenses -> stringResource(R.string.open_source_licenses)
        Screen.Purchase -> stringResource(R.string.unlock_advanced_features)
        Screen.Rename -> stringResource(R.string.name)
        Screen.TransparencyCustomization -> stringResource(R.string.customize_transparency_mode)
        Screen.Troubleshooting -> stringResource(R.string.troubleshooting)
        Screen.UpdateHearingTest -> stringResource(R.string.update_hearing_test)
        Screen.VersionInfo -> stringResource(R.string.version)
        is Screen.CallControl -> currentScreen.action
        Screen.MicrophoneSettings -> stringResource(R.string.microphone_mode)
        Screen.ReleaseNotes -> ""
    }

    val actionButtons = when (currentScreen) {
        Screen.AirPodsSettings -> listOf<@Composable (LayerBackdrop) -> Unit>({
            IconButton(onClick = { backStack.add(Screen.AppSettings) }) {
                Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings))
            }
        })
        else -> emptyList()
    }

    StyledScaffold(
        visible = currentScreen.showTopBar,
        title = title,
        showBackButton = backStack.size > 1,
        onNavigateBack = { backStack.removeAt(backStack.lastIndex) },
        expandableHeader = currentScreen in listOf(Screen.AudioRouting, Screen.ControlsGestures, Screen.Battery,
            Screen.HeadTracking, Screen.Accessibility, Screen.MicrophoneSettings) ||
            currentScreen is Screen.LongPress || currentScreen is Screen.CallControl,
        headerKey = currentScreen,
        actionButtons = actionButtons
    ) {
        AppNavGraph(
            showReleaseNotes = showReleaseNotes,
            updatesShown = updatesShown,
            showOnboarding = showOnboarding,
            onboardingComplete = onboardingComplete,
            backStack = backStack,
            airPodsViewModel = airPodsViewModel,
        )
    }
}
