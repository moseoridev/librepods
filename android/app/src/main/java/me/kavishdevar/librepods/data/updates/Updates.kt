package me.kavishdevar.librepods.data.updates

import androidx.compose.runtime.Composable
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.screens.AirPodsSettingsScreenPreview
import me.kavishdevar.librepods.presentation.screens.EqualizerScreenPreview

val update1_0_0 = listOf(
        UpdateItem(
            titleRes = R.string.material3e,
            descriptionRes = R.string.update_m3e_description,
            demoComposeable = @Composable {
                AirPodsSettingsScreenPreview()
            }
        ),
        UpdateItem(
            titleRes = R.string.equalizer,
            descriptionRes = R.string.update_equalizer_description,
            demoComposeable = @Composable {
                EqualizerScreenPreview()
            }
        ),
    )

val updates = update1_0_0
