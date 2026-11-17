package me.kavishdevar.librepods.presentation.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.BuildConfig
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.components.common.AppInfoCard
import me.kavishdevar.librepods.presentation.components.common.DeviceInfoCard
import me.kavishdevar.librepods.presentation.components.primitives.StyledListItem

/**
 * The unsupported-device notice. No Samsung first-run screen corresponds to it, so it keeps the
 * upstream content and controls and only adopts the flat page container and 14sp body.
 */
@Composable
fun NotSupportedPage(
    onBack: () -> Unit,
    bypassCompatibilityCheck: () -> Unit
) {
    val scrollState = rememberScrollState()

    OnboardingPageScaffold(
        title = stringResource(R.string.not_supported),
        onBack = onBack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = OnboardingStyle.Inset)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OnboardingBodyText(stringResource(R.string.not_compatible_update))
            OnboardingBodyText(stringResource(R.string.not_compatible_description))
            Spacer(Modifier.height(4.dp))
            DeviceInfoCard()
            AppInfoCard()
            if (!BuildConfig.PLAY_BUILD) {
                StyledListItem(
                    contentText = stringResource(R.string.bypass_compatibility_check),
                    onClick = bypassCompatibilityCheck
                )
            }
        }
    }
}
