package me.kavishdevar.librepods.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEvent
import kotlin.math.roundToInt

internal const val PAGE_TRANSITION_DURATION_MS = 300

/** Forward navigation: the new page enters from the trailing edge, the old one leaves trailwards. */
internal fun <S> AnimatedContentTransitionScope<S>.forwardPageTransition(): ContentTransform =
    slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.Start,
        tween(PAGE_TRANSITION_DURATION_MS)
    ) togetherWith slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.Start,
        tween(PAGE_TRANSITION_DURATION_MS)
    )

/** Back navigation without a gesture: the exact reverse of [forwardPageTransition]. */
internal fun <S> AnimatedContentTransitionScope<S>.popPageTransition(): ContentTransform =
    slideIntoContainer(
        AnimatedContentTransitionScope.SlideDirection.End,
        tween(PAGE_TRANSITION_DURATION_MS)
    ) togetherWith slideOutOfContainer(
        AnimatedContentTransitionScope.SlideDirection.End,
        tween(PAGE_TRANSITION_DURATION_MS)
    )

// Predictive back preview, following the Android predictive back design guidance:
// the current page scales 100% -> 90% and shifts away from the edge the gesture started on, the
// destination scales 110% -> 100%, and the two cross over in a fade through at 35% progress.
// https://developer.android.com/design/ui/mobile/guides/patterns/predictive-back
private const val PREDICTIVE_DURATION_MS = 600
private const val EXIT_SCALE = 0.9f
private const val ENTER_SCALE = 1.1f
private const val FADE_THROUGH_FRACTION = 0.35f

/** Matches the interpolator SystemUI uses, so the screen moves quickly at the start of the drag. */
private val BackGestureEasing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)

/**
 * The guidance shifts the surface by `(width / 20) - 8dp`: centred, a 90% page leaves 5% on each
 * side, and this moves it away from the gesture edge until only an 8dp margin is left on the
 * opposite side, matching the AOSP cross-activity preview.
 */
@Composable
internal fun rememberPredictiveBackShiftPx(): Float {
    val density = LocalDensity.current
    val containerWidthPx = LocalWindowInfo.current.containerSize.width
    return remember(density, containerWidthPx) {
        val marginPx = with(density) { 8.dp.toPx() }
        (containerWidthPx / 20f - marginPx).coerceAtLeast(0f)
    }
}

/**
 * @param shiftPx how far the leaving page moves, from [rememberPredictiveBackShiftPx].
 */
internal fun <S> AnimatedContentTransitionScope<S>.predictivePopPageTransition(
    swipeEdge: Int,
    shiftPx: Float,
): ContentTransform {
    // Away from the edge the gesture started on, mirroring AOSP's cross-activity animation.
    // The distance is a nudge of roughly 11dp, not a full-width slide: a slide of the whole page
    // in the drag direction reads as forward navigation, whatever the finger is doing.
    val direction = if (swipeEdge == NavigationEvent.EDGE_RIGHT) -1f else 1f
    val offsetPx = (shiftPx * direction).roundToInt()

    return scaleIn(
        animationSpec = tween(PREDICTIVE_DURATION_MS, easing = BackGestureEasing),
        initialScale = ENTER_SCALE,
    ) + fadeIn(fadeThroughEnter()) togetherWith
        scaleOut(
            animationSpec = tween(PREDICTIVE_DURATION_MS, easing = BackGestureEasing),
            targetScale = EXIT_SCALE,
        ) + fadeOut(fadeThroughExit()) +
        slideOutHorizontally(
            animationSpec = tween(PREDICTIVE_DURATION_MS, easing = BackGestureEasing),
            targetOffsetX = { offsetPx },
        )
}

private const val FADE_THROUGH_KEYFRAME_MS = (PREDICTIVE_DURATION_MS * FADE_THROUGH_FRACTION).toInt()

/** The leaving page stays fully opaque until the cross over, then fades out. */
private fun fadeThroughExit(): FiniteAnimationSpec<Float> = keyframes {
    durationMillis = PREDICTIVE_DURATION_MS
    1f at 0
    0f at FADE_THROUGH_KEYFRAME_MS
    0f at PREDICTIVE_DURATION_MS
}

/** The destination is invisible until the cross over, then fades in. */
private fun fadeThroughEnter(): FiniteAnimationSpec<Float> = keyframes {
    durationMillis = PREDICTIVE_DURATION_MS
    0f at 0
    0f at FADE_THROUGH_KEYFRAME_MS
    1f at PREDICTIVE_DURATION_MS
}
