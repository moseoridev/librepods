package me.kavishdevar.librepods.presentation.screens

import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.AACPManager.Companion.ControlCommandIdentifiers
import me.kavishdevar.librepods.presentation.components.BudsChoiceRow
import me.kavishdevar.librepods.presentation.components.StyledList
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel

@Composable
fun MicrophoneSettingsRoute(viewModel: AirPodsViewModel) {
    val state by viewModel.uiState.collectAsState()
    MicrophoneSettingsScreen(
        selectedMode = state.controlStates[ControlCommandIdentifiers.MIC_MODE]?.getOrNull(0)?.toInt(),
        enabled = state.isLocallyConnected,
        onMicrophoneSettingsChanged = { viewModel.setControlCommandInt(ControlCommandIdentifiers.MIC_MODE, it) })
}

@Composable
fun MicrophoneSettingsScreen(selectedMode: Int?, enabled: Boolean = true,
                             onMicrophoneSettingsChanged: (Int) -> Unit) {
    SettingsDetailColumn {
        StyledList(modifier = Modifier.selectableGroup()) {
            // iOS display order; upstream wire values remain Automatic=0, Left=2, Right=1.
            listOf(0 to R.string.microphone_automatic, 2 to R.string.microphone_always_left,
                1 to R.string.microphone_always_right).forEach { (value, label) ->
                BudsChoiceRow(name = stringResource(label), selected = selectedMode == value, enabled = enabled,
                    onClick = { if (selectedMode != value) onMicrophoneSettingsChanged(value) })
            }
        }
    }
}
