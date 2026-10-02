package me.kavishdevar.librepods.ui.catalog

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.Color
import me.kavishdevar.librepods.ui.components.*
import me.kavishdevar.librepods.ui.theme.SettingsStyle
import me.kavishdevar.librepods.ui.theme.SettingsTheme

/** Isolated, debug-only usage example. Contains no app model, resources or vendor artwork. */
@Composable
fun SettingsComponentGallery() {
    var enabled by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf("Automatic") }
    var volume by remember { mutableFloatStateOf(75f) }
    var mode by remember { mutableStateOf("Adaptive") }
    var dialog by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf(false) }
    var gains by remember { mutableStateOf(listOf(50, 50, 50)) }
    val field = rememberTextFieldState("Device name")
    val scroll = rememberScrollState()
    SettingsScaffold(title = "Components", backLabel = "Back", expandableHeader = true) {
        SettingsScrollReporter(scroll.canScrollForward)
        Column(Modifier.verticalScroll(scroll).padding(horizontal = SettingsStyle.pageInset()).padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(SettingsStyle.GroupSpacing)) {
            SettingsToggle(label = "Master control", checked = enabled, onCheckedChange = { enabled = it }, header = true)
            SettingsList(title = "Rows", description = "The caller owns state and commands.") {
                SettingsListItem(name = "Read-only value", description = "Connected", descriptionIsState = true)
                SettingsListItem(name = "Open details", onClick = { dialog = true }, description = "Supporting text")
                SettingsToggle(label = "Available control", checked = enabled, onCheckedChange = { enabled = it })
                SettingsToggle(label = "Disabled control", checked = true, enabled = false, onCheckedChange = {})
            }
            SettingsList(title = "Choice") {
                listOf("Automatic", "Left", "Right").forEach { value ->
                    SettingsChoiceRow(value, selected == value, { selected = value })
                }
            }
            SettingsCard {
                SettingsModeStrip(listOf("Off", "Ambient", "Adaptive", "Cancellation"), mode, { mode = it },
                    label = { it }, artwork = { _, _ -> Text("•") })
            }
            SettingsCard {
                Column(Modifier.padding(18.dp)) {
                    Text("Volume")
                    SettingsSeekBar(volume, { volume = it }, 0f..100f)
                    SettingsInputField(field, placeholder = "Name")
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SettingsBatteryMeter(75, "Left", " 75%", "Left, 75%", Color(0xFF26E26D), 92.dp) {
                    Canvas(Modifier.fillMaxSize()) { drawCircle(Color.Gray, size.minDimension / 3) }
                }
                SettingsLoadingIndicator()
            }
            SettingsCard {
                SettingsEqualizer(listOf("Low", "Mid", "High").mapIndexed { index, label ->
                    SettingsEqualizerBand(label, gains[index], "$label gain")
                }, { index, value -> gains = gains.toMutableList().also { it[index] = value } },
                    valueRange = 0..100, valueLabel = { it.toString() })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsButton({ dialog = true }) { Text("Dialog") }
                SettingsButton({ sheet = true }, style = SettingsButtonStyle.Tonal) { Text("Sheet") }
                SettingsIconButton({ dialog = true }, "Open dialog") { Text("+") }
            }
        }
    }
    SettingsConfirmationDialog(dialog, "Confirm action", "The action is supplied by the caller.",
        "Confirm", "Cancel", onConfirm = { dialog = false }, onDismiss = { dialog = false })
    SettingsBottomSheet(sheet, { sheet = false }) { _ ->
        Text("Sheet content", Modifier.padding(14.dp))
        SettingsButton({ sheet = false }, style = SettingsButtonStyle.Text) { Text("Close") }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Large type", fontScale = 1.6f, showBackground = true)
@Composable
private fun SettingsGalleryPreview() {
    SettingsTheme { SettingsComponentGallery() }
}
