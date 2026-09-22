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

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.AACPManager.Companion.ControlCommandIdentifiers
import me.kavishdevar.librepods.data.StemAction
import me.kavishdevar.librepods.presentation.components.*
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsUiState
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel

@Composable
fun LongPress(viewModel: AirPodsViewModel, name: String, navigateToPurchase: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    LongPressContent(state, name, { viewModel.setLongPressAction(name, it) },
        viewModel::toggleListeningMode, navigateToPurchase)
}

@Composable
fun LongPressContent(state: AirPodsUiState, side: String, onAction: (StemAction) -> Unit,
                     onToggleMode: (Int) -> Unit, navigateToPurchase: () -> Unit) {
    val action = if (side.equals("left", ignoreCase = true)) state.leftAction else state.rightAction
    val modes = state.controlStates[ControlCommandIdentifiers.LISTENING_MODE_CONFIGS]?.getOrNull(0)?.toInt() ?: 0
    SettingsDetailColumn {
        StyledList {
            BudsChoiceRow(name = stringResource(R.string.noise_control),
                selected = action == StemAction.CYCLE_NOISE_CONTROL_MODES,
                onClick = { onAction(StemAction.CYCLE_NOISE_CONTROL_MODES) })
            BudsChoiceRow(name = stringResource(R.string.digital_assistant),
                selected = action == StemAction.DIGITAL_ASSISTANT,
                onClick = { onAction(StemAction.DIGITAL_ASSISTANT) }, enabled = state.isPremium)
        }
        if (!state.isPremium) {
            StyledButton(onClick = navigateToPurchase, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.unlock_advanced_features))
            }
        }
        if (action == StemAction.CYCLE_NOISE_CONTROL_MODES) {
            StyledList(title = stringResource(R.string.noise_control),
                description = stringResource(R.string.press_and_hold_noise_control_description)) {
                val options = buildList {
                    if (state.offListeningMode) add(ModeOption(1, R.string.off, R.string.listening_mode_off_description, R.drawable.widget_noise_off))
                    add(ModeOption(4, R.string.transparency, R.string.listening_mode_transparency_description, R.drawable.transparency))
                    add(ModeOption(8, R.string.adaptive, R.string.listening_mode_adaptive_description, R.drawable.adaptive))
                    add(ModeOption(2, R.string.noise_cancellation, R.string.listening_mode_noise_cancellation_description, R.drawable.noise_cancellation))
                }
                options.forEach { option ->
                    StyledListItem(name = stringResource(option.title), description = stringResource(option.description),
                        selected = modes and option.bit != 0, onClick = { onToggleMode(option.bit) },
                        leadingContent = {
                            Icon(painterResource(option.icon), contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(28.dp))
                        })
                }
            }
        }
    }
}

private data class ModeOption(val bit: Int, val title: Int, val description: Int, val icon: Int)
