package me.kavishdevar.librepods.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.navigationevent.NavigationEvent

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

/**
 * Predictive back preview. The pages travel in the direction the finger is dragging instead of
 * scaling in place, so the preview reads as the back navigation it is committing to: the current
 * page leaves towards the swipe edge and the previous page follows behind it.
 *
 * Navigation3's default here is `fadeIn + scaleOut(0.7f)`, which shrinks the whole screen around
 * its centre and does not follow the gesture at all.
 */
internal fun <S> AnimatedContentTransitionScope<S>.predictivePopPageTransition(
    swipeEdge: Int,
): ContentTransform {
    // A right-edge drag pulls the pages leftwards, a left-edge drag pushes them rightwards.
    // Layout-direction aware: `Start`/`End` mirror under RTL by themselves.
    val towards =
        if (swipeEdge == NavigationEvent.EDGE_RIGHT) {
            AnimatedContentTransitionScope.SlideDirection.Start
        } else {
            AnimatedContentTransitionScope.SlideDirection.End
        }
    return slideIntoContainer(towards, tween(PAGE_TRANSITION_DURATION_MS)) togetherWith
        slideOutOfContainer(towards, tween(PAGE_TRANSITION_DURATION_MS))
}
