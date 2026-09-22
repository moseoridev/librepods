package me.kavishdevar.librepods.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.AACPManager.Companion.ControlCommandIdentifiers
import me.kavishdevar.librepods.presentation.components.StyledList
import me.kavishdevar.librepods.presentation.components.BudsChoiceRow
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel

@Composable
fun CallControlScreen(viewModel: AirPodsViewModel, action: String, onCallControlValueChanged: (Boolean) -> Unit) {
    val state by viewModel.uiState.collectAsState()
    CallControlContent(action,
        state.controlStates[ControlCommandIdentifiers.CALL_MANAGEMENT_CONFIG]?.getOrNull(1) == 0x02.toByte(),
        onCallControlValueChanged)
}

@Composable
fun CallControlContent(action: String, flipped: Boolean, onChanged: (Boolean) -> Unit) {
    val isMute = action == stringResource(R.string.mute_unmute)
    val once = isMute != flipped
    SettingsDetailColumn {
        StyledList {
            BudsChoiceRow(name = stringResource(R.string.press_once), selected = once,
                onClick = { if (!once) onChanged(!isMute) })
            BudsChoiceRow(name = stringResource(R.string.press_twice), selected = !once,
                onClick = { if (once) onChanged(isMute) })
        }
    }
}
