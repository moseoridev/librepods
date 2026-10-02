package me.kavishdevar.librepods.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.ui.theme.SettingsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsSeekBarInteractionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun thumbHoverShowsFeedbackWithoutChangingValueAndRespectsDisabledState() {
        var enabled by mutableStateOf(true)
        var changes = 0
        compose.setContent {
            SettingsTheme(darkTheme = true) {
                Box(Modifier.size(240.dp, 80.dp).background(MaterialTheme.colorScheme.surface)
                    .testTag("capture"), contentAlignment = Alignment.Center) {
                    SettingsSeekBar(.5f, { changes++ }, 0f..1f,
                        Modifier.width(200.dp).testTag("slider"), enabled = enabled)
                }
            }
        }
        fun pixels(): IntArray {
            val image = compose.onNodeWithTag("capture").captureToImage().toPixelMap()
            return IntArray(image.width * image.height) { i ->
                image[i % image.width, i / image.width].toArgb()
            }
        }
        val slider = compose.onNodeWithTag("slider")
        val resting = pixels()
        slider.performMouseInput { enter(center) }
        assertFalse("An enabled thumb must react to pointer hover", resting.contentEquals(pixels()))
        slider.performMouseInput { exit() }
        assertTrue("Leaving the thumb must clear feedback", resting.contentEquals(pixels()))
        compose.runOnIdle { enabled = false }
        val disabled = pixels()
        slider.performMouseInput { enter(center) }
        assertTrue("A disabled thumb must not react to hover", disabled.contentEquals(pixels()))
        slider.performMouseInput { exit() }
        compose.runOnIdle { assertEquals(0, changes) }
    }
}
