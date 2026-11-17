package me.kavishdevar.librepods.presentation.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.R

/**
 * The full privacy policy as a flat document page: 24sp left-aligned header title after the
 * 60dp back slot, 14sp body, 16sp subheadings and the consent pill in the bottom bar. The text is
 * unchanged from upstream.
 */
@Composable
fun PrivacyPolicyPage(
    onBack: () -> Unit,
    onForward: () -> Unit,
) {
    val scrollState = rememberScrollState()

    OnboardingPageScaffold(
        title = stringResource(R.string.privacy_policy),
        onBack = onBack,
        bottom = {
            // The consent is the Buds `동의` role, so it keeps the theme primary the reference
            // uses, not the monochrome Wearable pill the other pages use.
            OnboardingCentredPrimaryButton(
                text = stringResource(R.string.i_agree),
                onClick = onForward,
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = OnboardingStyle.Inset)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OnboardingHero(
                title = stringResource(R.string.privacy_policy),
                top = OnboardingStyle.HeroTopBuds,
                inset = 0.dp,
                titleStyle = OnboardingStyle.BudsHeroTitle,
                markGap = OnboardingStyle.BudsHeroGap
            )
            Spacer(Modifier.height(OnboardingStyle.HeroToDocument))
            Text(
                text = "Last updated: 20 June 2026",
                style = OnboardingStyle.Footnote,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OnboardingSectionHeading("Overview")

            OnboardingBodyText(
                "LibrePods does not collect, store, sell, or share personal information for advertising, analytics, tracking, or profiling purposes. The app does not include analytics, crash reporting, telemetry, advertising SDKs, or tracking services."
            )

            OnboardingBodyText(
                "All information remains on your device unless you explicitly choose to contact me, or create a GitHub issue from the app."
            )

            OnboardingSectionHeading("Third Party Services")

            OnboardingBodyText(
                "LibrePods provides several ways to contact me, including email, Discord, and GitHub Issues."
            )

            OnboardingSectionHeading("Email")

            OnboardingBodyText(
                "If you contact me by email, I receive your email address and any information you choose to include in your message. When using the contact form within LibrePods, your email client will open with a pre-filled email address, the subject line and body that you fill out. The body will also include LibrePods version information and device information to help with troubleshooting."
            )

            OnboardingBodyText("You can edit or remove any of this information before sending the email.")

            OnboardingSectionHeading("Discord")

            OnboardingBodyText(
                "The app provides a link to the LibrePods Discord server. If you choose to join the Discord server, you will be subject to Discord's privacy policy."
            )

            OnboardingBodyText(
                "I do not receive any information about you from Discord other than what is publicly visible in the Discord server, such as your username, joining date, common servers, and any messages or content you post in the server, unless you choose to share it with me in the Discord server."
            )

            OnboardingSectionHeading("GitHub Issues")

            OnboardingBodyText(
                "When creating a GitHub issue through LibrePods, the app will pre-fill the issue form with:"
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                OnboardingBodyText("• LibrePods version name and version code")
                OnboardingBodyText("• Device manufacturer and model")
                OnboardingBodyText("• Android build information")
                OnboardingBodyText("• Installation source (Google Play or GitHub)")
            }

            OnboardingBodyText(
                "This information helps diagnose bugs and provide support. No information is sent automatically. The information is only submitted if you choose to create the GitHub issue."
            )

            OnboardingSectionHeading("Contact")

            OnboardingBodyText(
                "If you have questions about this privacy policy, please contact me via email at privacy@kavish.xyz."
            )
        }
    }
}
