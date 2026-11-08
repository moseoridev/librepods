package me.kavishdevar.librepods.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.ui.theme.SettingsStyle
import me.kavishdevar.librepods.ui.theme.SettingsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** The source's group spacing is not uniform; pin the boundaries that differ. */
class SettingsGroupColumnTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun tag(name: String) = Modifier.width(300.dp).height(40.dp).testTag(name)

    @Test fun sectionLabelAndOwnSpaceGroupsSkipTheOrdinaryGap() {
        compose.setContent {
            SettingsTheme {
                SettingsGroupColumn {
                    // A titled group opens with the label's own 16dp lead, so the container
                    // must contribute no gap there; otherwise it has 36dp where the source
                    // has 16dp.
                    SettingsList(modifier = Modifier.testTag("titled"), title = "Section") {
                        item { _, _ -> Box(tag("titled-row")) }
                    }
                    SettingsList(modifier = Modifier.testTag("plain")) {
                        item { _, _ -> Box(tag("plain-row")) }
                    }
                    Box(tag("own-lead").ownLeadingSpace())
                    Box(tag("own-tail").ownTrailingSpace())
                    Box(tag("last"))
                }
            }
        }
        // getUnclippedBoundsInRoot reports dp, so compare against the dp token directly.
        val gap = SettingsStyle.GroupSpacing.value
        fun bounds(name: String) = compose.onNodeWithTag(name).getUnclippedBoundsInRoot()
        val rootTop = bounds("titled").top.value
        val titledBottom = bounds("titled").bottom.value
        val plainTop = bounds("plain").top.value
        val plainBottom = bounds("plain").bottom.value
        val ownLeadTop = bounds("own-lead").top.value
        val ownLeadBottom = bounds("own-lead").bottom.value
        val ownTailTop = bounds("own-tail").top.value
        val lastTop = bounds("last").top.value

        // The first group starts at the container's top edge.
        assertEquals(0f, rootTop, 0.5f)
        // Happy path for the ordinary boundary: 20dp between two untitled groups.
        assertEquals(titledBottom + gap, plainTop, 0.5f)
        // A declared lead takes no gap of its own.
        assertEquals(plainBottom, ownLeadTop, 0.5f)
        // ...and neither does a declared trail from the group before it.
        assertEquals(ownLeadBottom + gap, ownTailTop, 0.5f)
        // The group after a declared trail also takes no gap: the trail owns that boundary.
        assertEquals(bounds("own-tail").bottom.value, lastTop, 0.5f)
    }
}
