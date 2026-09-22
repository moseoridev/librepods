package me.kavishdevar.librepods.presentation.components

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Presentation-only scroll ownership; consumes only the distance the header actually travels. */
internal class BudsHeaderState(private val rangePx: Float, private val scope: CoroutineScope) : NestedScrollConnection {
    var fraction by mutableFloatStateOf(0f)
        private set
    private var settling: Job? = null

    fun stop() { settling?.cancel(); settling = null }

    fun drag(delta: Float): Float {
        stop()
        if (rangePx <= 0f) return 0f
        val before = fraction
        fraction = (before + delta / rangePx).coerceIn(0f, 1f)
        return (fraction - before) * rangePx
    }

    suspend fun settle() {
        stop()
        if (fraction <= 0f || fraction >= 1f) return
        // ASC gm.t2: settle to the nearer endpoint over 500ms.
        val task = scope.launch {
            animate(fraction, if (fraction < .5f) 0f else 1f, animationSpec = tween(500)) { value, _ ->
                fraction = value.coerceIn(0f, 1f)
            }
        }
        settling = task
        task.join()
        if (settling === task) settling = null
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source == NestedScrollSource.UserInput) stop()
        return if (available.y < 0) Offset(0f, drag(available.y)) else Offset.Zero
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
        if (available.y > 0) Offset(0f, drag(available.y)) else Offset.Zero

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        settle()
        return Velocity.Zero
    }
}
