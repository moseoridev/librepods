package me.kavishdevar.librepods.presentation.screens.apple

/**
 * Presentation-only decoding of the observed 9A release build family.
 * 9442752 = 0x9015c0 = 9A348: https://github.com/librepods-org/librepods/issues/782
 * The 12-bit build field is inferred from this pair; other families and prerelease
 * suffixes have not been established. Leave those untouched, and retain the raw
 * value on the version detail page. Never use this display value for capabilities.
 */
internal fun firmwareDisplayVersion(raw: String): String {
    if (raw.isEmpty() || raw.any { it !in '0'..'9' }) return raw
    val packed = raw.toIntOrNull() ?: return raw
    if (packed ushr 16 != 0x90 || packed and 0xf != 0) return raw
    val build = (packed ushr 4) and 0xfff
    return if (build > 0) "9A$build" else raw
}
