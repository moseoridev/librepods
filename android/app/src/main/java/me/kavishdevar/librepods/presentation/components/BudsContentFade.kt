package me.kavishdevar.librepods.presentation.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Foreground content effect; its layer spans the viewport, before scaffold padding. */
@Composable
internal fun Modifier.budsContentFade(): Modifier {
    val background = MaterialTheme.colorScheme.background
    val height = with(LocalDensity.current) { 56.dp.toPx() }
    val effect = remember(background, height) {
        // Locally authored linear fade. Keep the shader/effect across redraws; no frame clock.
        val shader = RuntimeShader("""
            uniform shader page;
            uniform float4 background;
            uniform float height;
            uniform float2 opacity;
            half4 main(float2 position) {
                half4 pixel = page.eval(position);
                if (position.y >= height) return pixel;
                float fraction = clamp(position.y / height, 0.0, 1.0);
                float alpha = mix(opacity.x, opacity.y, fraction);
                return mix(pixel, background, 1.0 - alpha);
            }
        """.trimIndent()).apply {
            setFloatUniform("background", background.red, background.green, background.blue, background.alpha)
            setFloatUniform("height", height)
            setFloatUniform("opacity", .12f, 1f)
        }
        RenderEffect.createRuntimeShaderEffect(shader, "page").asComposeRenderEffect()
    }
    return graphicsLayer {
        renderEffect = effect
        compositingStrategy = CompositingStrategy.Offscreen
    }
}
