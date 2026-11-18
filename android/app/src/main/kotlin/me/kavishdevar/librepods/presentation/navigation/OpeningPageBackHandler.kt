package me.kavishdevar.librepods.presentation.navigation

import androidx.compose.animation.EnterExitState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

/** A back gesture that starts while a page is opening must not seek the same transition. */
@Composable
internal fun OpeningPageBackHandler(isCurrent: () -> Boolean, onBack: () -> Unit) {
    val transition = LocalNavAnimatedContentScope.current.transition
    val opening = transition.currentState == EnterExitState.PreEnter &&
        transition.targetState == EnterExitState.Visible
    var hasOpened by remember { mutableStateOf(transition.currentState == EnterExitState.Visible) }
    if (!hasOpened && transition.currentState == EnterExitState.Visible) {
        SideEffect { hasOpened = true }
    }

    val navigation = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(
        state = navigation,
        isBackEnabled = isCurrent() && ((!hasOpened && opening) ||
            navigation.transitionState is NavigationEventTransitionState.InProgress),
        onBackCompleted = { if (isCurrent()) onBack() },
    )
}
