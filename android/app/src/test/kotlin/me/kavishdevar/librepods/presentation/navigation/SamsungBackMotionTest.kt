package me.kavishdevar.librepods.presentation.navigation

import androidx.compose.animation.core.LinearEasing
import org.junit.Assert.assertEquals
import org.junit.Test

class SamsungBackMotionTest {
    @Test fun closingPageKeepsItsProgressWhenNextGestureStarts() {
        var progress: Float? = .25f
        var outgoingPageIsCurrent = true
        val motion = SamsungBackMotion({ progress }, { outgoingPageIsCurrent })
        val easing = motion.easing(follow = { it * .5f }, complete = LinearEasing)
        assertEquals(.125f, easing.transform(.25f), .0001f)

        outgoingPageIsCurrent = false
        progress = null
        val closingPosition = easing.transform(.5f)

        progress = .1f
        assertEquals(closingPosition, easing.transform(.5f), .0001f)
        assertEquals(1f, easing.transform(1f), .0001f)
    }

    @Test fun cancelledPageCanFollowAnotherGesture() {
        var progress: Float? = .4f
        val motion = SamsungBackMotion({ progress }, { true })
        val easing = motion.easing(follow = { it * .5f }, complete = LinearEasing)
        assertEquals(.2f, easing.transform(.4f), .0001f)

        progress = null
        assertEquals(.1f, easing.transform(.2f), .0001f)

        progress = .6f
        assertEquals(.3f, easing.transform(.6f), .0001f)
    }
}
