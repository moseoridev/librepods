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

@file:OptIn(ExperimentalEncodingApi::class)

package me.kavishdevar.librepods.presentation.screens

// import me.kavishdevar.librepods.utils.RadareOffsetFinder
import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.graphics.shapes.Morph
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.highlight.Highlight
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import kotlinx.coroutines.delay
import me.kavishdevar.librepods.BuildConfig
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.AACPManager
import me.kavishdevar.librepods.bluetooth.ATTHandles
import me.kavishdevar.librepods.data.Capability
import me.kavishdevar.librepods.presentation.MaterialIcons
import me.kavishdevar.librepods.presentation.components.AboutCard
import me.kavishdevar.librepods.presentation.components.BatteryView
import me.kavishdevar.librepods.presentation.components.BudsScrollReporter
import me.kavishdevar.librepods.presentation.components.HearingHealthSettings
import me.kavishdevar.librepods.presentation.components.NoiseControlSettings
import me.kavishdevar.librepods.presentation.components.StyledButton
import me.kavishdevar.librepods.presentation.components.StyledList
import me.kavishdevar.librepods.presentation.components.StyledListItem
import me.kavishdevar.librepods.presentation.theme.BudsStyle
import me.kavishdevar.librepods.presentation.theme.LibrePodsTheme
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsUiState
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel
import me.kavishdevar.librepods.presentation.viewmodel.demoState
import java.util.concurrent.TimeUnit
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds

@Composable
fun AirPodsSettingsRoute(
    viewModel: AirPodsViewModel,
    navigateToRename: () -> Unit,
    navigateToHearingProtection: () -> Unit,
    navigateToHearingAid: () -> Unit,
    navigateToLeftLongPress: () -> Unit,
    navigateToRightLongPress: () -> Unit,
    navigateToPurchase: () -> Unit,
    navigateToAdaptiveStrength: () -> Unit,
    navigateToEqualizer: () -> Unit,
    navigateToHeadTracking: () -> Unit,
    navigateToAccessibility: () -> Unit,
    navigateToVersion: () -> Unit,
    navigateToTroubleshooting: () -> Unit,
    navigateToCallControlScreen: (action: String) -> Unit,
    navigateToMicrophoneSettings: () -> Unit,
    navigateToAudioRouting: () -> Unit,
    navigateToControlsGestures: () -> Unit,
    navigateToBattery: () -> Unit,
    page: AirPodsSettingsPage = AirPodsSettingsPage.Home,
) {
    val state by viewModel.uiState.collectAsState()

    val topPadding = 16.dp
    val bottomPadding = 12.dp

    Box (
        modifier = Modifier
            .fillMaxSize()
    ) {
        AirPodsSettingsScreen(
            state = state,
            page = page,
            navigateToAudioRouting = navigateToAudioRouting,
            navigateToControlsGestures = navigateToControlsGestures,
            navigateToBattery = navigateToBattery,

            topPadding = topPadding,
            bottomPadding = bottomPadding,

            setControlCommandInt = viewModel::setControlCommandInt,
            setControlCommandBoolean = viewModel::setControlCommandBoolean,
//        setControlCommandValue = viewModel::setControlCommandValue,
            setControlCommandByte = viewModel::setControlCommandByte,

            setATTCharacteristicValue = viewModel::setATTCharacteristicValue,

            onAutomaticEarDetectionChanged = viewModel::setAutomaticEarDetectionEnabled,
            onAutomaticConnectionChanged = viewModel::setAutomaticConnectionEnabled,
            setDynamicEndOfCharge = viewModel::setDynamicEndOfCharge,
            setOffListeningMode = viewModel::setOffListeningMode,
            disconnect = viewModel::disconnect,

            navigateToRename = navigateToRename,
            navigateToHearingProtection = navigateToHearingProtection,
            navigateToHearingAid = navigateToHearingAid,
            navigateToLeftLongPress = navigateToLeftLongPress,
            navigateToRightLongPress = navigateToRightLongPress,
            navigateToPurchase = navigateToPurchase,
            navigateToAdaptiveStrength = navigateToAdaptiveStrength,
            navigateToEqualizer = navigateToEqualizer,
            navigateToHeadTracking = navigateToHeadTracking,
            navigateToAccessibility = navigateToAccessibility,
            navigateToVersion = navigateToVersion,
            navigateToTroubleshooting = navigateToTroubleshooting,
            navigateToCallControlScreen = navigateToCallControlScreen,
            navigateToMicrophoneSettings = navigateToMicrophoneSettings,

            activateDemoMode = viewModel::activateDemoMode,
            reconnectFromSavedMac = viewModel::reconnectFromSavedMac
        )
    }
}

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@SuppressLint("MissingPermission", "UnspecifiedRegisterReceiverFlag")
@Composable
fun AirPodsSettingsScreen(
        state: AirPodsUiState,

        topPadding: Dp = 16.dp,
        bottomPadding: Dp = 16.dp,

        setControlCommandInt: (AACPManager.Companion.ControlCommandIdentifiers, Int) -> Unit,
        setControlCommandBoolean: (AACPManager.Companion.ControlCommandIdentifiers, Boolean) -> Unit,
//        setControlCommandValue: (AACPManager.Companion.ControlCommandIdentifiers, ByteArray) -> Unit,
        setControlCommandByte: (AACPManager.Companion.ControlCommandIdentifiers, Byte) -> Unit,
        setATTCharacteristicValue: (ATTHandles, ByteArray) -> Unit,

        onAutomaticEarDetectionChanged: (Boolean) -> Unit,
        onAutomaticConnectionChanged: (Boolean) -> Unit,
        setDynamicEndOfCharge: (Boolean) -> Unit,
        setOffListeningMode: (Boolean) -> Unit,
        disconnect: () -> Unit,

        navigateToRename: () -> Unit,
        navigateToHearingProtection: () -> Unit,
        navigateToHearingAid: () -> Unit,
        navigateToLeftLongPress: () -> Unit,
        navigateToRightLongPress: () -> Unit,
        navigateToPurchase: () -> Unit,
        navigateToAdaptiveStrength: () -> Unit,
        navigateToEqualizer: () -> Unit,
        navigateToHeadTracking: () -> Unit,
        navigateToAccessibility: () -> Unit,
        navigateToVersion: () -> Unit,
        navigateToTroubleshooting: () -> Unit,
        navigateToCallControlScreen: (action: String) -> Unit,
        navigateToMicrophoneSettings: () -> Unit,

        activateDemoMode: () -> Unit,
        reconnectFromSavedMac: () -> Unit,
        page: AirPodsSettingsPage = AirPodsSettingsPage.Home,
        navigateToAudioRouting: () -> Unit = {},
        navigateToControlsGestures: () -> Unit = {},
        navigateToBattery: () -> Unit = {},
) {
    val sharedPreferences = LocalContext.current.getSharedPreferences("settings", MODE_PRIVATE)
    if (state.isLocallyConnected) {
        val capabilities = state.capabilities

        val listState = rememberLazyListState()
        BudsScrollReporter(listState.canScrollForward)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(horizontal = BudsStyle.pageInset()),
            contentPadding = PaddingValues(
                top = if (page == AirPodsSettingsPage.Home) topPadding else 0.dp,
                bottom = maxOf(bottomPadding, BudsStyle.GroupSpacing)),
            verticalArrangement = if (page == AirPodsSettingsPage.Home) Arrangement.Top
                else Arrangement.spacedBy(BudsStyle.GroupSpacing),
        ) {
            when (page) {
                AirPodsSettingsPage.AudioRouting -> audioRoutingSettings(
                    state, setControlCommandBoolean, setATTCharacteristicValue,
                    onAutomaticEarDetectionChanged, onAutomaticConnectionChanged,
                    setOffListeningMode, navigateToAdaptiveStrength, navigateToEqualizer,
                    navigateToMicrophoneSettings,
                )
                AirPodsSettingsPage.ControlsGestures -> controlsGesturesSettings(
                    state, navigateToLeftLongPress, navigateToRightLongPress,
                    navigateToCallControlScreen, navigateToHeadTracking,
                )
                AirPodsSettingsPage.Battery -> batterySettings(state, setDynamicEndOfCharge)
                AirPodsSettingsPage.Home -> {
                    item(key = "play_update_banner") {
                        if (state.timeUntilFOSSPremiumExpiry > 0L) {
                            val context = LocalContext.current
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF32829B), RoundedCornerShape(28.dp))
                                    .clip(RoundedCornerShape(28.dp))
                                    .clickable {
                                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = "mailto:".toUri()
                                            putExtra(Intent.EXTRA_EMAIL, arrayOf("billing@kavish.xyz"))
                                            putExtra(Intent.EXTRA_SUBJECT, "LibrePods Play billing error")
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "Please enter your GitHub username to restore your premium access:\n\nGitHub username: "
                                            )
                                        }
                                        context.startActivity(emailIntent)
                                    }) {
                                Text(
                                    text = stringResource(
                                        R.string.play_foss_premium_banner,
                                        maxOf(
                                            1,
                                            TimeUnit.MILLISECONDS.toDays(state.timeUntilFOSSPremiumExpiry)
                                                .toInt()
                                        )
                                    ), modifier = Modifier.padding(16.dp), style = TextStyle(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontFamily = FontFamily(Font(R.font.sf_pro))
                                    )
                                )
                            }
                        }
                    }

                    item(key = "battery") {
                        BatteryView(
                            batteryList = state.battery,
                            budsRes = state.instance?.model?.budsRes ?: R.drawable.airpods_pro_2_buds,
                            caseRes = state.instance?.model?.caseRes ?: R.drawable.airpods_pro_2_case
                        )
                    }
                    item(key = "spacer_battery") {
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    item(key = "name") {
                        StyledListItem(
                            name = stringResource(R.string.name),
                            description = state.deviceName,
                            onClick = navigateToRename,
                        )
                    }

                    val hasHearingAidCapability =
                        state.instance?.model?.capabilities?.contains(Capability.HEARING_AID) == true
                    val hasPPECapability =
                        state.instance?.model?.capabilities?.contains(Capability.PPE) == true

                    if (hasHearingAidCapability || hasPPECapability) {
                        if (hasPPECapability || state.vendorIdHook) {
                            item(key = "spacer_hearing_health") {
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                        item(key = "hearing_health") {
                            HearingHealthSettings(
                                hasPPECapability = hasPPECapability,
                                hasHearingAidCapability = hasHearingAidCapability,
                                vendorIdHook = state.vendorIdHook,
                                navigateToHearingProtection = navigateToHearingProtection,
                                navigateToHearingAid = navigateToHearingAid
                            )
                        }
                    }

                    if (capabilities.contains(Capability.LISTENING_MODE)) {
                        item(key = "spacer_noise") {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        item(key = "noise_control") {
                            NoiseControlSettings(
                                showOffListeningMode = state.offListeningMode,
                                noiseControlModeValue = state.controlStates[AACPManager.Companion.ControlCommandIdentifiers.LISTENING_MODE]?.getOrNull(
                                    0
                                )?.toInt() ?: 0,
                                onNoiseControlModeChanged = {
                                    setControlCommandInt(
                                        AACPManager.Companion.ControlCommandIdentifiers.LISTENING_MODE, it
                                    )
                                },
                            )
                        }
                    }

                    item(key = "settings_navigation") {
                        Spacer(Modifier.height(16.dp))
                        StyledList {
                            StyledListItem(name = stringResource(R.string.audio_routing), onClick = navigateToAudioRouting)
                            StyledListItem(name = stringResource(R.string.controls_gestures), onClick = navigateToControlsGestures)
                            StyledListItem(name = stringResource(R.string.accessibility), onClick = navigateToAccessibility)
                        }
                        Spacer(Modifier.height(16.dp))
                        StyledListItem(name = stringResource(R.string.battery), onClick = navigateToBattery)
                    }

                    item(key = "upgrade_button") {
                        if (!state.isPremium) {
                            Spacer(modifier = Modifier.height(28.dp))
                            StyledButton(
                                onClick = navigateToPurchase,
                                backdrop = rememberLayerBackdrop(),
                                modifier = Modifier.fillMaxWidth(),
                                maxScale = 0.05f,
                                surfaceColor = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    stringResource(R.string.unlock_advanced_features),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    item(key = "spacer_about") { Spacer(modifier = Modifier.height(32.dp)) }
                    item(key = "about") {
                        AboutCard(
                            modelName = state.modelName,
                            actualModel = state.actualModel,
                            serialNumbers = state.serialNumbers,
                            version = state.version3,
                            navigateToVersion = navigateToVersion
                        )
                    }

                    item(key = "spacer_disconnect") { Spacer(modifier = Modifier.height(28.dp)) }
                    item(key = "disconnect_button") {
                        StyledButton(
                            onClick = disconnect,
                            backdrop = rememberLayerBackdrop(),
                            isInteractive = false,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.disconnect),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

        //                item(key = "spacer_debug") { Spacer(modifier = Modifier.height(16.dp)) }
        //                item(key = "debug") { StyledListItem("debug", "Debug", navController) }

                }
            }

        }
    } else {
        val backdrop = rememberLayerBackdrop()
        Box(
            modifier = Modifier
                .drawBackdrop(
                    backdrop = rememberLayerBackdrop(),
                    exportedBackdrop = backdrop,
                    shape = { RoundedCornerShape(0.dp) },
                    highlight = {
                        Highlight.Ambient.copy(alpha = 0f)
                    },
                    effects = {}
                )
                .fillMaxSize()
                .padding(start = 8.dp, end = 8.dp, bottom = bottomPadding),
            contentAlignment = Alignment.Center
        ) {
            val tapCount = remember { mutableIntStateOf(0) }
            val lastTapTime = remember { mutableLongStateOf(0L) }

            var reconnecting by remember { mutableStateOf(false) }

            LaunchedEffect(reconnecting) {
                if (reconnecting) {
                    delay(5.seconds)
                    reconnecting = false
                }
            }

            val polygons = remember {
                listOf(
                    MaterialShapes.Cookie9Sided,
                    MaterialShapes.Clover4Leaf,
                    MaterialShapes.SoftBurst,
                    MaterialShapes.Sunny,
                    MaterialShapes.Pentagon,
                    MaterialShapes.Cookie4Sided,
                    MaterialShapes.Oval,
                )
            }

            val morphs = remember {
                buildList {
                    for (i in polygons.indices) {
                        add(
                            Morph(
                                polygons[i].normalized(),
                                polygons[(i + 1) % polygons.size].normalized()
                            )
                        )
                    }
                }
            }

            var currentMorphIndex by remember { mutableIntStateOf(0) }

            val morphProgress = remember { Animatable(0f) }

            LaunchedEffect(reconnecting) {
                if (!reconnecting) {
                    currentMorphIndex = 0
                    morphProgress.snapTo(0f)
                    return@LaunchedEffect
                }

                while (reconnecting) {
                    morphProgress.snapTo(0f)

                    morphProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 650,
                            easing = FastOutSlowInEasing
                        )
                    )

                    currentMorphIndex = (currentMorphIndex + 1) % morphs.size
                }
            }

            val path = remember { Path() }
            val scaleMatrix = remember { Matrix() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                activateDemoMode()
                            }
                        )
                    }
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 32.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    val now = System.currentTimeMillis()

                                    if (now - lastTapTime.longValue > 400) {
                                        tapCount.intValue = 0
                                    }

                                    tapCount.intValue++
                                    lastTapTime.longValue = now

                                    if (tapCount.intValue >= 5) {
                                        tapCount.intValue = 0
                                        activateDemoMode()
                                    }
                                }
                            )
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val primaryContainerColor = MaterialTheme.colorScheme.tertiaryContainer
                    val secondaryContainerColor = MaterialTheme.colorScheme.secondaryContainer

                    val animatedShapeColor by animateColorAsState(if (reconnecting) primaryContainerColor else secondaryContainerColor)

                    if (state.connectionSuccessful) {
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceBright,
                                    CircleShape
                                )
                                .clickable(
                                    interactionSource = null,
                                    indication = ripple(
                                        bounded = false,
                                        radius = 120.dp
                                    ),
                                    enabled = !reconnecting,
                                    onClick = {}
                                )
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            if (!reconnecting) {
                                                currentMorphIndex = 1
                                                reconnecting = true
                                                reconnectFromSavedMac()
                                            }
                                        },
                                        onPress = {
                                            if (!reconnecting) {
                                                morphProgress.animateTo(
                                                    targetValue = 1f,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessLow
                                                    )
                                                )
                                                tryAwaitRelease()
                                                morphProgress.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessLow
                                                    )
                                                )
                                            }
                                        }
                                    )
                                }
                                .drawWithContent {
                                    val activeMorph = morphs[currentMorphIndex]

                                    val shapePath = activeMorph.toPath(
                                        progress = morphProgress.value,
                                        path = path
                                    )

                                    val bounds = shapePath.getBounds()

                                    val scale = min(
                                        size.width / bounds.width,
                                        size.height / bounds.height
                                    ) * 0.8f

                                    scaleMatrix.reset()

                                    scaleMatrix.scale(x = scale, y = scale)

                                    shapePath.transform(scaleMatrix)

                                    shapePath.translate(size.center - shapePath.getBounds().center)

                                    drawPath(
                                        path = shapePath,
                                        color = animatedShapeColor
                                    )

                                    drawContent()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (reconnecting) MaterialIcons.bluetooth_searching else MaterialIcons.headset_off,
                                contentDescription = null,
                                modifier = Modifier.size(84.dp),
                                tint = if (reconnecting) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Spacer(Modifier.height(40.dp))

                        Text(
                            text = if (reconnecting) stringResource(R.string.reconnecting) else stringResource(R.string.tap_to_reconnect),
                            style = MaterialTheme.typography.labelSmallEmphasized,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.airpods_not_connected),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (sharedPreferences.getBoolean("bypass_device_check.v2", false)) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.compatibility_check_bypassed),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                if (!BuildConfig.PLAY_BUILD) {
                    OutlinedButton(
                        onClick = navigateToTroubleshooting,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(24.dp)
                    ) {
                        Text(
                            stringResource(
                                R.string.troubleshooting
                            ),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun AirPodsSettingsScreenPreview() {
    LibrePodsTheme() {
        Box (
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            AirPodsSettingsScreen(
                state = demoState,

                setControlCommandInt = { _, _ -> },
                setControlCommandBoolean = { _, _ -> },
                setControlCommandByte = { _, _ -> },
                setATTCharacteristicValue = { _, _ -> },

                onAutomaticEarDetectionChanged = {},
                onAutomaticConnectionChanged = {},
                setDynamicEndOfCharge = {},
                setOffListeningMode = {},
                disconnect = {},

                navigateToRename = {},
                navigateToHearingProtection = {},
                navigateToHearingAid = {},
                navigateToLeftLongPress = {},
                navigateToRightLongPress = {},
                navigateToPurchase = {},
                navigateToAdaptiveStrength = {},
                navigateToEqualizer = {},
                navigateToHeadTracking = {},
                navigateToAccessibility = {},
                navigateToVersion = {},
                navigateToTroubleshooting = {},
                navigateToCallControlScreen = {},
                navigateToMicrophoneSettings = {},

                activateDemoMode = {},
                reconnectFromSavedMac = {}
            )
        }
    }
}
