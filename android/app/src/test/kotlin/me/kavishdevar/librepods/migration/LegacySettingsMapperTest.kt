package me.kavishdevar.librepods.migration

import me.kavishdevar.librepods.core.bluetooth.MacAddress
import me.kavishdevar.librepods.core.bluetooth.aacp.types.MagicKeyType
import me.kavishdevar.librepods.core.data.StemAction
import me.kavishdevar.librepods.core.data.apple.AppleCache
import me.kavishdevar.librepods.core.devices.*
import me.kavishdevar.librepods.database.apple.AppleEntity
import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

class LegacySettingsMapperTest {
    private val mac = "AA:BB:CC:DD:EE:FF"
    private val irk = ByteArray(16) { it.toByte() }
    private val enc = ByteArray(16) { (it + 10).toByte() }
    private fun values() = mutableMapOf<String, Any>("mac_address" to mac.lowercase(),
        "IRK" to Base64.getEncoder().encodeToString(irk), "ENC_KEY" to Base64.getEncoder().encodeToString(enc),
        "airpods_model_number" to "A3440", "onboarding_complete" to true)

    @Test fun deviceIdentityKeysAndExplicitChoicesSurviveTheOldSchema() {
        val values = values().apply {
            put("proximity_key_address", mac)
            put("left_long_press_action", "CYCLE_NOISE_CONTROL_MODES")
            put("right_long_press_action", "DIGITAL_ASSISTANT")
            put("automatic_ear_detection", false)
            put("show_bottom_sheet_popup", false)
            put("takeover_when_idle", true)
            put("takeover_when_media_start", true)
        }
        val plan = checkNotNull(LegacySettingsMapper.plan(values, setOf(10, 11), 100, false))
        val device = checkNotNull(plan.device)
        assertEquals(MacAddress(mac), device.macAddress)
        assertEquals(AirPodsModel.AIRPODS_5_WCC, device.metadata.model)
        assertArrayEquals(irk, device.cache.magicKeys[MagicKeyType.IRK])
        assertArrayEquals(enc, device.cache.magicKeys[MagicKeyType.ENC_KEY])
        assertFalse(device.settings.earDetectionEnabled)
        assertFalse(device.settings.showBottomSheetPopup)
        assertEquals(StemAction.DIGITAL_ASSISTANT, device.settings.rightLongPressAction)
        assertTrue(device.settings.connectOnMediaWhenAccessoryIdle)
        assertFalse(device.settings.connectOnPhoneWhenAccessoryIdle)
        assertTrue(plan.appState.hasCompletedOnboarding)
        assertEquals(setOf(10, 11), plan.widgets.map { it.appWidgetId }.toSet())
    }

    @Test fun defaultsDoNotEnableTakeoverAndForeignOrMalformedKeysAreNotImported() {
        val foreign = values().apply { put("proximity_key_address", "11:22:33:44:55:66") }
        val device = checkNotNull(LegacySettingsMapper.plan(foreign, emptySet(), 0, false)?.device)
        assertTrue(device.cache.magicKeys.isEmpty())
        assertFalse(device.settings.connectOnMediaWhenAccessoryDisconnected)
        assertFalse(device.settings.connectOnPhoneWhenAccessoryIdle)
        val malformed = values().apply { put("IRK", "not a key"); put("ENC_KEY", true) }
        assertTrue(LegacySettingsMapper.plan(malformed, emptySet(), 0, false)!!.device!!.cache.magicKeys.isEmpty())
        assertNull(LegacySettingsMapper.plan(mapOf("some_new_setting" to true), emptySet(), 0, false))
        assertNull(LegacySettingsMapper.plan(values().apply { put("mac_address", "bad") }, emptySet(), 0, false)!!.device)
    }

    @Test fun existingRewriteChoicesWinAndKeyPairsCannotBeMixedAcrossIdentities() {
        val imported = checkNotNull(LegacySettingsMapper.plan(values(), emptySet(), 0, false)?.device)
        val current = imported.copy(settings = AppleSettings(showIslandPopup = false),
            metadata = AppleMetadata(name = "current"), cache = AppleCache(magicKeys = mapOf(MagicKeyType.IRK to irk)))
        val merged = LegacySettingsMapper.mergeDevice(current, imported)
        assertEquals(current.settings, merged.settings)
        assertEquals(current.metadata, merged.metadata)
        assertArrayEquals(enc, merged.cache.magicKeys[MagicKeyType.ENC_KEY])
        val conflict = current.copy(cache = AppleCache(magicKeys = mapOf(MagicKeyType.IRK to ByteArray(16) { 99 })))
        assertSame(conflict, LegacySettingsMapper.mergeDevice(conflict, imported))
        val incompleteLegacy = imported.copy(cache = AppleCache(magicKeys = mapOf(MagicKeyType.ENC_KEY to enc)))
        assertSame(current, LegacySettingsMapper.mergeDevice(current, incompleteLegacy))
    }

    @Test fun widgetSettingsKeepProviderIdsAndExpiredPremiumCannotBeRevived() {
        assertEquals(setOf(11, 12), LegacySettingsMapper.widgetIds(
            mapOf("11_items" to "3,1", "nonsense" to true), mapOf("12_opacity" to 2, "11_color" to 0)))
        val plan = LegacySettingsMapper.plan(values().apply {
            put("premium_expiry_time", 99L); put("foss_upgraded", true)
        }, emptySet(), 100, true)!!
        assertFalse(plan.appState.isPremium)
        assertEquals(0L, plan.appState.timeUntilFOSSPremiumExpiry)
    }
}
