package me.kavishdevar.librepods.presentation.screens.onboarding

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.navigation.forwardPageTransition
import me.kavishdevar.librepods.presentation.navigation.popPageTransition
import me.kavishdevar.librepods.presentation.navigation.rememberPageParallaxPx
import me.kavishdevar.librepods.ui.theme.SettingsTheme
import me.kavishdevar.librepods.utils.XposedState
import me.kavishdevar.librepods.utils.bypassDeviceCheck
import me.kavishdevar.librepods.utils.isSupported

private const val PAGE_WELCOME = 0
private const val PAGE_PRIVACY = 1
private const val PAGE_NOT_SUPPORTED = 2
private const val PAGE_PERMISSIONS = 3

/**
 * Onboarding as a forced sequence of flat pages: welcome, privacy policy, the unsupported-device
 * notice when needed, and permissions. The page switch reuses the app's push/pop page
 * transitions instead of a carousel, so no page is masked, clipped or rounded.
 * Flow order, permission logic and results, consent persistence and the support decision are
 * unchanged.
 */
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
) {
    val sharedPreferences = LocalContext.current.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val isSupported = isSupported(sharedPreferences) || XposedState.bluetoothScopeEnabled

    var page by rememberSaveable { mutableIntStateOf(PAGE_WELCOME) }
    val parallaxPx = rememberPageParallaxPx()

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
                if (targetState > initialState) forwardPageTransition(parallaxPx)
                else popPageTransition(parallaxPx)
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

/**
 * Samsung welcome: no toolbar. The upstream greeting line and the 30sp app name form one centred
 * block in the body, as in the reference's body heading (`갤럭시 웨어러블` at y=360px = 120dp from the
 * screen top at 480dpi), with the introduction below it and the pill in the bottom bar.
 *
 * Order is upstream's: `welcome_to_appname` first, `app_name` as the single 30sp hero. The reference
 * has no greeting line, so the block is anchored at the reference's y and the hero necessarily begins
 * one greeting line lower; anchoring the block instead of the hero is what keeps a large font scale
 * from colliding the two lines.
 */
@Composable
private fun WelcomePage(onForward: () -> Unit) {
    OnboardingPageScaffold(
        title = null,
        bottom = {
            OnboardingCentredPrimaryButton(
                text = stringResource(R.string.proceed),
                onClick = onForward,
                fill = OnboardingStyle.WearablePrimaryFill,
                label = OnboardingStyle.WearablePrimaryLabel,
            )
        }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = OnboardingStyle.Inset)
                .padding(top = OnboardingStyle.WelcomeHeadingTop)
        ) {
            Text(
                text = stringResource(R.string.welcome_to_appname),
                modifier = Modifier.fillMaxWidth(),
                style = OnboardingStyle.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(OnboardingStyle.WelcomeGreetingGap))
            Text(
                text = stringResource(R.string.app_name),
                modifier = Modifier.fillMaxWidth(),
                style = OnboardingStyle.Title,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            OnboardingBodyText(
                text = stringResource(R.string.app_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview
@Composable
fun OnboardingScreenPreview() {
    OnboardingScreen {}
}
