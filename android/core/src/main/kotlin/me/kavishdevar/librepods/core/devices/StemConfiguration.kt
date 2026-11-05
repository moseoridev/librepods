package me.kavishdevar.librepods.core.devices

import me.kavishdevar.librepods.core.data.StemAction
import me.kavishdevar.librepods.core.data.StemPressBud
import me.kavishdevar.librepods.core.data.StemPressType

/** Firmware switches each press type to host handling for both buds together. */
fun AppleSettings.stemAction(bud: StemPressBud, type: StemPressType): StemAction = when (type) {
    StemPressType.SINGLE_PRESS -> if (bud == StemPressBud.LEFT) leftSinglePressAction else rightSinglePressAction
    StemPressType.DOUBLE_PRESS -> if (bud == StemPressBud.LEFT) leftDoublePressAction else rightDoublePressAction
    StemPressType.TRIPLE_PRESS -> if (bud == StemPressBud.LEFT) leftTriplePressAction else rightTriplePressAction
    StemPressType.LONG_PRESS -> if (bud == StemPressBud.LEFT) leftLongPressAction else rightLongPressAction
}

fun AppleSettings.handlesStemType(type: StemPressType): Boolean = StemPressBud.entries.any {
    stemAction(it, type) != StemAction.defaultActions.getValue(type)
}

fun AppleSettings.stemConfigMask(): Int = StemPressType.entries.fold(0) { mask, type ->
    mask or if (handlesStemType(type)) (1 shl type.ordinal) else 0
}

/** Listening-mode command values and their independent configuration bits differ. */
fun configuredListeningModes(mask: Int, adaptive: Boolean, off: Boolean): List<Int> =
    listOf(1 to 1, 3 to 4, 4 to 8, 2 to 2).filter { (mode, bit) ->
        mask and bit != 0 && (mode != 4 || adaptive) && (mode != 1 || off)
    }.map { it.first }
