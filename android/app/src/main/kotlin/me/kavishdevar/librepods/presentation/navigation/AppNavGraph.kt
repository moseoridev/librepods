package me.kavishdevar.librepods.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import me.kavishdevar.librepods.LibrePodsApplication
import me.kavishdevar.librepods.core.bluetooth.MacAddress
import me.kavishdevar.librepods.core.devices.AppleDevice
import me.kavishdevar.librepods.core.devices.ConnectionState
import me.kavishdevar.librepods.core.devices.Device

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
    val pageParallaxPx = rememberPageParallaxPx()
    val predictiveShiftPx = rememberPredictiveBackShiftPx()

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
        modifier = Modifier.pageTransitionBackground(),
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
        transitionSpec = { forwardPageTransition(pageParallaxPx) },
        popTransitionSpec = { popPageTransition(pageParallaxPx) },
        predictivePopTransitionSpec = { swipeEdge ->
            predictivePopPageTransition(swipeEdge, predictiveShiftPx)
        },
    )
}
