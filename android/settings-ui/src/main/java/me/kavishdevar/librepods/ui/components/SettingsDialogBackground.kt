package me.kavishdevar.librepods.ui.components

import android.graphics.drawable.GradientDrawable

/** The observed platform editor window has an ordinary circular GradientDrawable. */
internal fun settingsDialogBackground(color: Int, radiusPixels: Float): GradientDrawable =
    GradientDrawable().apply {
        setColor(color)
        cornerRadius = radiusPixels
    }
