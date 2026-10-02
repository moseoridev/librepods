package me.kavishdevar.librepods.ui.components

import android.os.Build
import android.view.View
import java.lang.reflect.Method

/** Optional OEM window effect. Missing or restricted APIs retain the solid surface. */
internal object SettingsWindowBlur {
    private class Api {
        val info = Class.forName("android.view.SemBlurInfo")
        val builder = Class.forName("android.view.SemBlurInfo\$Builder")
        val constructor = builder.getDeclaredConstructor(Int::class.javaPrimitiveType)
        fun method(name: String, vararg parameters: Class<*>): Method =
            builder.getDeclaredMethod(name, *parameters).apply { isAccessible = true }
        val preset = method("setColorCurvePreset", Int::class.javaPrimitiveType!!)
        val color = method("hidden_setBackgroundColor", Int::class.javaPrimitiveType!!)
        val corner = method("hidden_setBackgroundCornerRadius", Float::class.javaPrimitiveType!!)
        val build = method("hidden_build")
        val apply = View::class.java.getDeclaredMethod("hidden_semSetBlurInfo", info)
            .apply { isAccessible = true }
    }

    private val api by lazy {
        if (Build.VERSION.SDK_INT < 35 || !Build.MANUFACTURER.equals("samsung", ignoreCase = true)) null
        else optional { Api() }
    }

    // kw.t.a / wm.k1.r: native mode 0, dark/light curve 130/115 and 26dp corners.
    fun apply(view: View, dark: Boolean, cornerPx: Float, radiusPixels: Int? = null): Boolean {
        val effect = api ?: return false
        return optional {
            val builder = effect.constructor.newInstance(0)
            if (radiusPixels != null) effect.method("setRadius", Int::class.javaPrimitiveType!!)
                .invoke(builder, radiusPixels)
            effect.preset.invoke(builder, if (dark) 130 else 115)
            effect.color.invoke(builder, if (dark) 0x1AFFFFFF else 0)
            effect.corner.invoke(builder, cornerPx)
            effect.apply.invoke(view, effect.build.invoke(builder))
            true
        } ?: false
    }

    fun clear(view: View) {
        val effect = api ?: return
        optional { effect.apply.invoke(view, null) }
    }

    private inline fun <T> optional(block: () -> T): T? = try {
        block()
    } catch (_: ReflectiveOperationException) {
        null
    } catch (_: SecurityException) {
        null
    } catch (_: LinkageError) {
        null
    }
}
