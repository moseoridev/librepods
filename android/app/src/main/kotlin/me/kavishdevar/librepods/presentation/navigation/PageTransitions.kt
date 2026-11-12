package me.kavishdevar.librepods.presentation.navigation

import android.content.res.Resources
import android.view.View
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEvent
import kotlin.math.roundToInt

internal const val PAGE_TRANSITION_DURATION_MS = 450
private const val POP_PAGE_TRANSITION_DURATION_MS = 400
private val PagePushEasing = CubicBezierEasing(.22f, .5f, 0f, 1f)
private val PagePopEasing = CubicBezierEasing(.22f, .25f, 0f, 1f)

/** TransitionAnimation.updateToCustomAnimIfNeeded: Samsung activity parallax is 120dp. */
@Composable
internal fun rememberPageParallaxPx(): Int {
    val density = LocalDensity.current
    return remember(density) { with(density) { 120.dp.roundToPx() } }
}

/**
 * Activity-level Shell dim surfaces are black, so page alpha composites over black.
 * NavDisplay keeps the foreground above the parallax page on push and pop. Their opaque
 * bounds overlap throughout the motion, covering the native 120dp edge extension.
 */
internal fun Modifier.pageTransitionBackground(): Modifier = background(Color.Black)

// Shell loads samsung_activity_* from its system Context. An app-only RTL locale
// mirrors content but does not select the system's ldrtl Window animation resources.
private fun pageDirection(forward: Boolean): AnimatedContentTransitionScope.SlideDirection {
    val systemRtl = Resources.getSystem().configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
    return if (systemRtl == forward) AnimatedContentTransitionScope.SlideDirection.Right
        else AnimatedContentTransitionScope.SlideDirection.Left
}

private const val PAGE_DIM_DURATION_MS = 150
private const val DIMMED_PAGE_ALPHA = .82f

/** Samsung samsung_activity_open_enter/exit: 450ms, full entry / 120dp background. */
internal fun <S> AnimatedContentTransitionScope<S>.forwardPageTransition(parallaxPx: Int): ContentTransform =
    slideIntoContainer(
        pageDirection(forward = true),
        tween(PAGE_TRANSITION_DURATION_MS, easing = PagePushEasing)
    ) togetherWith (slideOutOfContainer(
        pageDirection(forward = true),
        tween(PAGE_TRANSITION_DURATION_MS, easing = PagePushEasing),
        targetOffset = { full -> if (full < 0) -parallaxPx else parallaxPx }
    ) + fadeOut(tween(PAGE_DIM_DURATION_MS, easing = LinearEasing), targetAlpha = DIMMED_PAGE_ALPHA))

/** Samsung samsung_activity_close_enter/exit: 400ms, 120dp return / full exit. */
internal fun <S> AnimatedContentTransitionScope<S>.popPageTransition(parallaxPx: Int): ContentTransform =
    (slideIntoContainer(
        pageDirection(forward = false),
        tween(POP_PAGE_TRANSITION_DURATION_MS, easing = PagePopEasing),
        initialOffset = { full -> if (full < 0) -parallaxPx else parallaxPx }
    ) + fadeIn(tween(PAGE_DIM_DURATION_MS, easing = LinearEasing), initialAlpha = DIMMED_PAGE_ALPHA)) togetherWith slideOutOfContainer(
        pageDirection(forward = false),
        tween(POP_PAGE_TRANSITION_DURATION_MS, easing = PagePopEasing)
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
