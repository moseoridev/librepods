package me.kavishdevar.librepods.presentation.screens.apple

import android.content.Context
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.compose.LottieAnimation
import me.kavishdevar.librepods.ui.components.SettingsBatteryMeter
import me.kavishdevar.librepods.ui.components.SettingsBatteryRow
import me.kavishdevar.librepods.ui.components.SettingsDeviceIllustration
import me.kavishdevar.librepods.ui.components.SettingsImageBackdrop
import me.kavishdevar.librepods.ui.components.SettingsList
import me.kavishdevar.librepods.ui.components.SettingsListScope
import me.kavishdevar.librepods.ui.components.SettingsListItem
import me.kavishdevar.librepods.ui.components.SettingsModeStrip
import me.kavishdevar.librepods.ui.components.SettingsRowDivider
import me.kavishdevar.librepods.ui.components.SettingsScaffold
import me.kavishdevar.librepods.ui.components.SettingsSectionLabel
import me.kavishdevar.librepods.ui.components.SettingsSwitch
import me.kavishdevar.librepods.ui.components.SettingsToggle
import me.kavishdevar.librepods.ui.theme.SettingsStyle
import me.kavishdevar.librepods.ui.theme.SettingsTheme

/**
 * The installed Manager 9.0.10.1724 Home, built from settings-ui and the vendor's own resources.
 *
 * Every string, drawable, colour and Lottie asset resolves from the installed
 * `com.samsung.accessory.budsunitemgr` package at runtime, so nothing vendor-owned is packaged
 * here and the only variable left against a native capture is our own component geometry. The
 * vendor package is read only: this file opens its resources and never invokes its code.
 *
 * Debug source set only; never packaged into a release variant.
 */
internal const val VENDOR_PACKAGE = "com.samsung.accessory.budsunitemgr"

@Composable
private fun NativeImage(vendor: Context, name: String, modifier: Modifier, tint: Color? = null,
    contentScale: ContentScale = ContentScale.Fit) {
    val id = vendor.resources.getIdentifier(name, "drawable", vendor.packageName)
    check(id != 0) { "Missing installed reference drawable: $name" }
    CompositionLocalProvider(LocalContext provides vendor) {
        Image(painterResource(id), null, modifier, contentScale = contentScale,
            colorFilter = tint?.let(ColorFilter::tint))
    }
}

@Composable
internal fun SettingsListScope.NativeHomeMenu(vendor: Context, label: String, icon: String,
    enabled: Boolean = false, switch: Boolean = false, checked: Boolean = false) {
    val ink = MaterialTheme.colorScheme.onSurface
    SettingsListItem(name = label, onClick = {}, enabled = enabled, homeMenu = true,
        leadingContent = { NativeImage(vendor, icon, Modifier.size(24.dp), ink) },
        trailingDivider = switch,
        trailingContent = if (switch) ({ SettingsSwitch(checked, {}, label,
            enabled = enabled, compact = true) }) else null)
}

/** The 9.0.10.1724 Home content column, in the source's own order. */
@Composable
internal fun NativeHome(
    vendor: Context,
    dark: Boolean,
    scroll: ScrollState,
    onNavigateBack: () -> Unit,
    controlsEnabled: Boolean = true,
) {
    fun text(name: String): String {
        val id = vendor.resources.getIdentifier(name, "string", vendor.packageName)
        check(id != 0) { "Missing installed reference string: $name" }
        return vendor.getString(id)
    }
    fun color(name: String): Color {
        val id = vendor.resources.getIdentifier(name, "color", vendor.packageName)
        check(id != 0) { name }
        return Color(vendor.getColor(id))
    }
    val ink = MaterialTheme.colorScheme.onSurface
    val combined = 99
    val caseLevel = 29

    SettingsScaffold(
        title = "Galaxy Buds4",
        backLabel = "상위 메뉴로 이동",
        showBackButton = true,
        onNavigateBack = onNavigateBack,
        scrollState = scroll,
        navigationIcon = { NativeImage(vendor, "ic_tw_ic_ab_back_mtrl", Modifier.size(24.dp), ink) },
        actionButtons = listOf(
            { NativeImage(vendor, "ic_search_black_24", Modifier.size(24.dp), ink) },
            { StockOverflowIcon(ink) },
        ),
        background = {
            val id = vendor.resources.getIdentifier(
                if (dark) "bg_gradient_dark_oneui9" else "bg_gradient_light_oneui9",
                "drawable", vendor.packageName)
            CompositionLocalProvider(LocalContext provides vendor) {
                SettingsImageBackdrop(painterResource(id))
            }
        },
    ) {
        Column(Modifier.fillMaxWidth()) {
            // rl.r.l has an outer 6dp inset; zn.a adds 12dp above the 120x80dp hero.
            Spacer(Modifier.height(6.dp))
            // Hero and battery row overlap in rl.r.l's Box. Reserve the source
            // 92dp battery origin while the illustration paints beyond its box.
            Box(Modifier.fillMaxWidth().height(92.dp), contentAlignment = Alignment.TopCenter) {
                val id = vendor.resources.getIdentifier(
                    "home_image_handel_black_and_shadow", "drawable", vendor.packageName)
                CompositionLocalProvider(LocalContext provides vendor) {
                    SettingsDeviceIllustration(painterResource(id))
                }
            }
            @Composable fun meter(level: Int, name: String, artwork: String, width: Dp = 138.5.dp) {
                SettingsBatteryMeter(level, name, " $level%", "$name $level%", maxWidth = width,
                    nameContent = { Text(name) }) {
                    NativeImage(vendor, artwork, Modifier.size(30.dp, 18.dp), ink)
                }
            }
            SettingsBatteryRow(
                listOf {
                    SettingsBatteryMeter(
                        combined,
                        text("tips_device_left") + "•" + text("tips_device_right"),
                        " $combined%", "이어버드 $combined%", maxWidth = 138.5.dp,
                        nameContent = {
                            Text(text("tips_device_left") + "•")
                            Text(text("tips_device_right"))
                        },
                    ) {
                        NativeImage(vendor, "ic_battery_left_right_buds4", Modifier.size(30.dp, 18.dp), ink)
                    }
                },
                Modifier.padding(vertical = 14.dp).padding(horizontal = SettingsStyle.pageInset()),
                secondaryMeter = if (caseLevel in 0..100) ({
                    meter(caseLevel, text("case_version"), "ic_battery_case_buds4")
                }) else null,
            )
            Column(Modifier.fillMaxWidth().padding(horizontal = SettingsStyle.pageInset())) {
                SettingsList {
                    item { _, _ ->
                        SettingsModeStrip(listOf(0, 1, 2, 3), 0, {}, label = { mode ->
                            text(listOf("noise_control_off", "settings_ambient_sound", "adaptive",
                                "settings_noise_reduction_title")[mode])
                        }, artwork = { mode, selected ->
                            // rl.j1 returns the final frame when not playing; rl.i1 applies the
                            // source's selected/unselected tint to the layer.
                            val base = listOf("noise_control_off", "ambient_sound", "adaptive",
                                "active_noise_canceling")[mode]
                            val asset = if (selected) base else "${base}_deac"
                            val composition = remember(vendor, asset) {
                                val id = vendor.resources.getIdentifier(asset, "raw", vendor.packageName)
                                check(id != 0) { asset }
                                vendor.resources.openRawResource(id).use {
                                    LottieCompositionFactory.fromJsonInputStreamSync(it, null).value
                                }
                            }
                            val tint = color(if (selected) "buds_icon_on" else "buds_icon_off")
                            LottieAnimation(composition, progress = { 1f }, alignment = Alignment.TopStart,
                                modifier = Modifier.requiredSize(44.dp)
                                    .graphicsLayer { colorFilter = ColorFilter.tint(tint) })
                        })
                        SettingsRowDivider()
                    }
                    SettingsToggle(text("settings_call_quality_title"), checked = false,
                        onCheckedChange = {}, homeMenu = true)
                }
                Spacer(Modifier.height(20.dp))
                SettingsList {
                    SettingsToggle(text("media_controls"),
                        text("pinch") + " • " + text("swipe_up_and_down"),
                        checked = false, enabled = controlsEnabled, onCheckedChange = {},
                        trailingDivider = true, homeMenu = true)
                }
                SettingsSectionLabel(text("pinch_and_hold_controls"))
                SettingsList {
                    SettingsToggle(text("IDS_ST_HEADER_LEFT"), "꺼짐", descriptionIsState = true,
                        enabled = controlsEnabled, onCheckedChange = {}, trailingDivider = true, homeMenu = true)
                    SettingsToggle(text("IDS_ST_HEADER_RIGHT"), "꺼짐", descriptionIsState = true,
                        enabled = controlsEnabled, onCheckedChange = {}, trailingDivider = true, homeMenu = true)
                }
                SettingsSectionLabel(text("call_controls"))
                SettingsList {
                    SettingsToggle(text("answer_end_call"), text("pinch"), enabled = controlsEnabled,
                        onCheckedChange = {}, homeMenu = true)
                    SettingsToggle(text("tips_decline_call"), text("pinch_and_hold"), enabled = controlsEnabled,
                        onCheckedChange = {}, homeMenu = true)
                }
                Spacer(Modifier.height(20.dp))
                SettingsList {
                    NativeHomeMenu(vendor, text("voice_controls"), "ic_voice_control_ui8")
                    NativeHomeMenu(vendor, text("head_gesture"), "ic_head_gestures_ui8",
                        switch = true, checked = false)
                }
                Spacer(Modifier.height(20.dp))
                SettingsList { NativeHomeMenu(vendor, text("settings_sound_effect"), "ic_sounds_ui8") }
                Spacer(Modifier.height(20.dp))
                SettingsList {
                    NativeHomeMenu(vendor, text("manage_connections"), "ic_earbuds_connection_ui9",
                        enabled = true)
                    NativeHomeMenu(vendor, text("settings_advanced_features"), "ic_advanced_ui8")
                }
                Spacer(Modifier.height(20.dp))
                SettingsList { NativeHomeMenu(vendor, text("accessibility"), "ic_accessibility_ui8") }
                Spacer(Modifier.height(20.dp))
                SettingsList {
                    NativeHomeMenu(vendor, text("settings_find_my_buds"), "ic_fme_ui8")
                    NativeHomeMenu(vendor, text("diagnostics"), "ic_device_care_ui8")
                }
                Spacer(Modifier.height(20.dp))
                SettingsList {
                    NativeHomeMenu(vendor, text("card_menu_earbuds_software_update"), "ic_update_ui8")
                    NativeHomeMenu(vendor, text("card_menu_tips_and_user_manual"), "ic_tips_ui8",
                        enabled = true)
                    NativeHomeMenu(vendor, text("card_menu_about_earbuds"), "ic_about_earbuds_ui8",
                        enabled = true)
                }
            }
        }
    }
}

/** Hosts [NativeHome] under a configuration supplied from the intent, never from the device. */
internal fun ComponentActivity.showBudsManagerHome(dark: Boolean, configuration: Configuration) {
    val configured = createConfigurationContext(configuration)
    val vendor = createPackageContext(VENDOR_PACKAGE, 0).createConfigurationContext(configuration)
    setContent {
        CompositionLocalProvider(
            LocalConfiguration provides configuration,
            LocalContext provides configured,
            LocalDensity provides Density(resources.displayMetrics.density, configuration.fontScale),
        ) {
            SettingsTheme(darkTheme = dark) {
                NativeHome(vendor, dark, rememberScrollState(), onNavigateBack = {})
            }
        }
    }
}
