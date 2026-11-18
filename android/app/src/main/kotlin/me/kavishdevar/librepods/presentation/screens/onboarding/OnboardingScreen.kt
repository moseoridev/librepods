package me.kavishdevar.librepods.presentation.screens.onboarding

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.ui.theme.SettingsTheme
import me.kavishdevar.librepods.utils.XposedState
import me.kavishdevar.librepods.utils.bypassDeviceCheck
import me.kavishdevar.librepods.utils.isSupported

private const val PAGE_WELCOME = 0
private const val PAGE_PRIVACY = 1
private const val PAGE_NOT_SUPPORTED = 2
private const val PAGE_PERMISSIONS = 3
private const val PAGE_SLIDE_DURATION_MS = 260

/** Button-driven setup with a full-width slide in the direction of travel. */
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
) {
    val sharedPreferences = LocalContext.current.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val isSupported = isSupported(sharedPreferences) || XposedState.bluetoothScopeEnabled

    var page by rememberSaveable { mutableIntStateOf(PAGE_WELCOME) }

    BackHandler(enabled = page > PAGE_WELCOME) {
        page = if (isSupported && page == PAGE_PERMISSIONS) PAGE_PRIVACY else page - 1
    }

    SettingsTheme {
        AnimatedContent(
            targetState = page,
            modifier = Modifier
                .fillMaxSize()
                .background(OnboardingStyle.PageBackground),
            transitionSpec = {
                val direction = if (targetState > initialState) SlideDirection.Start else SlideDirection.End
                slideIntoContainer(direction, tween(PAGE_SLIDE_DURATION_MS)) togetherWith
                    slideOutOfContainer(direction, tween(PAGE_SLIDE_DURATION_MS))
            },
            label = "onboarding-page"
        ) { index ->
            when (index) {
                PAGE_WELCOME -> WelcomePage(onForward = { page = PAGE_PRIVACY })
                PAGE_PRIVACY -> PrivacyPolicyPage(
                    onBack = { page = PAGE_WELCOME },
                    onForward = {
                        page = if (isSupported) PAGE_PERMISSIONS else PAGE_NOT_SUPPORTED
                    }
                )
                PAGE_NOT_SUPPORTED -> NotSupportedPage(
                    onBack = { page = PAGE_PRIVACY },
                    bypassCompatibilityCheck = { bypassDeviceCheck(sharedPreferences) }
                )
                else -> PermissionsPage(
                    onBackward = { page = if (isSupported) PAGE_PRIVACY else PAGE_NOT_SUPPORTED },
                    onForward = onOnboardingComplete
                )
            }
        }
    }
}

/** Wearable’s app-title, product-artwork and bottom-prompt order for a single welcome page. */
@Composable
private fun WelcomePage(onForward: () -> Unit) {
    OnboardingPageScaffold(
        title = null,
        bottom = {
            OnboardingCentredPrimaryButton(
                text = stringResource(R.string.onboarding_start),
                onClick = onForward,
                fill = OnboardingStyle.WearablePrimaryFill,
                label = OnboardingStyle.WearablePrimaryLabel,
            )
        }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = OnboardingStyle.Inset)
                .padding(top = 80.dp, bottom = 20.dp),
        ) {
            Text(
                text = stringResource(R.string.app_name),
                modifier = Modifier.fillMaxWidth(),
                style = OnboardingStyle.Title,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(72.dp))
            WelcomePodArtwork()
            Spacer(Modifier.height(28.dp))
            OnboardingBodyText(
                text = stringResource(R.string.app_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Welcome artwork: the app's own AirPods-left / case / -right silhouettes, centred and tinted from
 * the heading colour. Wearable's welcome page draws a large product image in this slot
 * (`wm_welcome_device_page_layout`), but that is vendor artwork; these three vectors already ship
 * with LibrePods for its notifications and widgets, so the slot is filled with the product's own
 * device art instead. Marked an unverified adaptation in the evidence README.
 */
@Composable
private fun WelcomePodArtwork() {
    Row(
        modifier = Modifier.fillMaxWidth().height(144.dp),
        horizontalArrangement = Arrangement.spacedBy(OnboardingStyle.WelcomeArtworkGap),
        verticalAlignment = Alignment.Bottom
    ) {
        Icon(
            painter = painterResource(R.drawable.airpods_pro_left_notification),
            contentDescription = null,
            modifier = Modifier
                .weight(1f)
                .height(144.dp),
            tint = OnboardingStyle.HeadingText
        )
        Icon(
            painter = painterResource(R.drawable.airpods_pro_case_notification),
            contentDescription = null,
            modifier = Modifier
                .weight(1f)
                .height(110.dp),
            tint = OnboardingStyle.HeadingText
        )
        Icon(
            painter = painterResource(R.drawable.airpods_pro_right_notification),
            contentDescription = null,
            modifier = Modifier
                .weight(1f)
                .height(144.dp),
            tint = OnboardingStyle.HeadingText
        )
    }
}

@Preview
@Composable
fun OnboardingScreenPreview() {
    OnboardingScreen {}
}
