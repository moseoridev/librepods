package me.kavishdevar.librepods.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import me.kavishdevar.librepods.LibrePodsApplication
import me.kavishdevar.librepods.core.bluetooth.MacAddress
import me.kavishdevar.librepods.core.devices.AppleDevice
import me.kavishdevar.librepods.core.devices.ConnectionState
import me.kavishdevar.librepods.core.devices.Device

private const val TRANSITION_DURATION_MS = 300

/**
 * Standard AndroidX slide transition: the new page enters from the trailing
 * edge and the old one leaves towards the leading edge, reversing on back.
 * `slideIntoContainer`/`slideOutOfContainer` resolve the direction from the
 * navigation itself, so the hardware back button, an on-screen back button and
 * the predictive back gesture all share one transition.
 */
private fun <S> AnimatedContentTransitionScope<S>.slide(): ContentTransform =
    slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.Start,
        tween(TRANSITION_DURATION_MS)
    ) togetherWith slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.Start,
        tween(TRANSITION_DURATION_MS)
    )

@Composable
fun AppNavGraph(
    backStack: NavBackStack<Screen>,
    devicesState: State<Map<MacAddress, Device<*, *, *>>>,
    showReleaseNotes: Boolean = false,
    updatesShown: () -> Unit = {},
    onboardingComplete: () -> Unit = {},
) {
    val devices by devicesState
    val context = LocalContext.current

    val appDataRepository by lazy { (context.applicationContext as LibrePodsApplication).appDataRepository }
    val recordingRepository by lazy { (context.applicationContext as LibrePodsApplication).recordingRepository }
    val heartRateRepository by lazy { (context.applicationContext as LibrePodsApplication).heartRateRepository }

    val currentDevice: Device<*, *, *>? = devices[backStack.lastOrNull()?.let { (it as? DeviceScreen)?.macAddress }]

    LaunchedEffect(currentDevice) {
        val device = currentDevice as? AppleDevice ?: return@LaunchedEffect

        var wasConnected = false

        device.connectionState.collect { state ->
            when (state) {
                ConnectionState.CONNECTED -> {
                    wasConnected = true
                }

                ConnectionState.DISCONNECTED -> {
                    if (!wasConnected) return@collect

                    while (
                        backStack.size > 1 &&
                        backStack.lastOrNull() is DeviceScreen
                    ) {
                        backStack.removeAt(backStack.lastIndex)
                    }
                }

                else -> Unit
            }
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeAt(backStack.lastIndex)
            }
        },
        entryProvider = { screen ->
            NavEntry(screen) {
                RenderScreenContent(
                    screen = screen,
                    backStack = backStack,
                    devices = devices,
                    appDataRepository = appDataRepository,
                    recordingRepository = recordingRepository,
                    heartRateRepository = heartRateRepository,
                    showReleaseNotes = showReleaseNotes,
                    updatesShown = updatesShown,
                    onboardingComplete = onboardingComplete
                )
            }
        },
        transitionSpec = { slide() },
        popTransitionSpec = { slide() },
    )
}
