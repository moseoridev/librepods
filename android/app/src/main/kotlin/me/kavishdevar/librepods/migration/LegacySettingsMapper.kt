package me.kavishdevar.librepods.migration

import me.kavishdevar.librepods.core.bluetooth.MacAddress
import me.kavishdevar.librepods.core.bluetooth.aacp.types.ControlCommandIdentifier
import me.kavishdevar.librepods.core.bluetooth.aacp.types.MagicKeyType
import me.kavishdevar.librepods.core.data.StemAction
import me.kavishdevar.librepods.core.data.apple.AppleCache
import me.kavishdevar.librepods.core.devices.*
import me.kavishdevar.librepods.database.app.AppSettingsEntity
import me.kavishdevar.librepods.database.app.AppStateEntity
import me.kavishdevar.librepods.database.apple.AppleEntity
import me.kavishdevar.librepods.database.widget.WidgetConfigEntity
import java.util.Base64

/** Pure conversion of the preserved branch's settings schema; never contacts Bluetooth. */
internal object LegacySettingsMapper {
    data class Plan(val appSettings: AppSettingsEntity, val appState: AppStateEntity,
        val device: AppleEntity?, val widgets: List<WidgetConfigEntity>)

    fun plan(values: Map<String, *>, widgetIds: Set<Int>, now: Long, playBuild: Boolean): Plan? {
        if (values.keys.none { it in setOf("mac_address", "onboarding_complete", "connection_successful", "foss_upgraded", "IRK", "ENC_KEY") }) return null
        fun bool(key: String, default: Boolean) = values[key] as? Boolean ?: default
        fun text(key: String) = values[key] as? String ?: ""
        fun number(key: String, default: Long = 0) = (values[key] as? Number)?.toLong() ?: default
        val address = text("mac_address").uppercase().takeIf { it.matches(Regex("([0-9A-F]{2}:){5}[0-9A-F]{2}")) }?.let(::MacAddress)
        val modelNumber = text("airpods_model_number")
        val model = AirPodsModel.entries.firstOrNull { modelNumber in AirPodsSpecs.getSpec(it).modelNumbers } ?: AirPodsModel.UNKNOWN
        fun action(key: String, default: StemAction): StemAction = when (val raw = text(key)) {
            "CYCLE_NOISE_CONTROL_MODES", "CYCLE_LISTENING_MODE_MODES" -> StemAction.CYCLE_listening_mode_MODES
            else -> StemAction.entries.find { it.name == raw } ?: default
        }
        val media = bool("takeover_when_media_start", false)
        val call = bool("takeover_when_ringing_call", false)
        val settings = AppleSettings(
            disconnectWhenNotWearing = bool("disconnect_when_not_wearing", false),
            earDetectionEnabled = bool("automatic_ear_detection", true),
            headGesturesEnabled = bool("head_gestures", true),
            leftLongPressAction = action("left_long_press_action", StemAction.CYCLE_listening_mode_MODES),
            rightLongPressAction = action("right_long_press_action", StemAction.CYCLE_listening_mode_MODES),
            showIslandPopup = bool("show_island_popup", true),
            showBottomSheetPopup = bool("show_bottom_sheet_popup", true),
            alwaysConnectWhenAccessoryDisconnected = false,
            connectOnPhoneWhenAccessoryDisconnected = call && bool("takeover_when_disconnected", false),
            connectOnPhoneWhenAccessoryIdle = call && bool("takeover_when_idle", false),
            connectOnPhoneWhenAccessoryMusic = call && bool("takeover_when_music", false),
            connectOnMediaWhenAccessoryDisconnected = media && bool("takeover_when_disconnected", false),
            connectOnMediaWhenAccessoryIdle = media && bool("takeover_when_idle", false),
            connectOnMediaWhenAccessoryMusic = media && bool("takeover_when_music", false),
            conversationAwarenessPauseMusicEnabled = bool("conversational_awareness_pause_music", false),
            relativeConversationAwarenessVolumeEnabled = bool("relative_conversational_awareness_volume", true),
            conversationAwarenessVolume = number("conversational_awareness_volume", 43).coerceIn(0, 100).toFloat())
        val owner = text("proximity_key_address").ifBlank { address?.value.orEmpty() }
        val keys = if (address != null && owner.equals(address.value, true)) buildMap {
            for ((key, kind) in listOf("IRK" to MagicKeyType.IRK, "ENC_KEY" to MagicKeyType.ENC_KEY)) {
                val decoded = runCatching { Base64.getDecoder().decode(text(key).filterNot { it.isWhitespace() }) }.getOrNull()
                if (decoded?.size == 16) put(kind, decoded)
            }
        } else emptyMap()
        val controls = buildMap<ControlCommandIdentifier, ByteArray> {
            for ((key, id) in listOf("off_listening_mode" to ControlCommandIdentifier.ALLOW_OFF_OPTION,
                "automatic_ear_detection" to ControlCommandIdentifier.EAR_DETECTION_CONFIG,
                "automatic_connection_ctrl_cmd" to ControlCommandIdentifier.SMART_ROUTING_MODE,
                "dynamic_end_of_charge" to ControlCommandIdentifier.DYNAMIC_END_OF_CHARGE)) {
                (values[key] as? Boolean)?.let { put(id, byteArrayOf(if (it) 1 else 0)) }
            }
            if (values["long_press_byte"] is Number) put(ControlCommandIdentifier.LISTENING_MODE_CONFIGS,
                byteArrayOf((number("long_press_byte").toInt() and 0x0f).toByte()))
        }
        val device = address?.let { AppleEntity(it, settings, AppleMetadata(
            name = text("airpods_name").ifBlank { text("name") }, model = model,
            modelNumber = modelNumber, manufacturer = text("airpods_manufacturer"),
            serialNumber = text("airpods_serial_number"), leftSerialNumber = text("airpods_left_serial_number"),
            rightSerialNumber = text("airpods_right_serial_number"), version1 = text("airpods_version1"),
            version2 = text("airpods_version2"), version3 = text("airpods_version3"),
            hardwareRevision = text("airpods_hardware_revision"), updaterIdentifier = text("airpods_updater_identifier")),
            AppleCache(magicKeys = keys, controlStates = controls)) }
        val state = AppStateEntity(isPremium = !playBuild && bool("foss_upgraded", false),
            hasCompletedOnboarding = bool("onboarding_complete", false),
            hasConnectedToAACP = bool("connection_successful", false),
            firstSuccessfulConnectionTime = number("first_connection_successful_time").takeIf { it > 0 },
            reviewPrompted = bool("review_prompted", false),
            timeUntilFOSSPremiumExpiry = (number("premium_expiry_time") - now).coerceAtLeast(0))
        return Plan(AppSettingsEntity(proximityCardSettings = me.kavishdevar.librepods.data.app.ProximityCardSettings(useVideo = true)), state, device,
            address?.let { mac -> widgetIds.filter { it >= 0 }.map { WidgetConfigEntity(it, mac) } }.orEmpty())
    }

    /** Existing rewrite settings/metadata win; missing compatible keys may be filled atomically. */
    fun mergeDevice(current: AppleEntity?, legacy: AppleEntity): AppleEntity {
        if (current == null) return legacy
        if (current.macAddress != legacy.macAddress) return current
        val conflicts = current.cache.magicKeys.any { (kind, key) ->
            legacy.cache.magicKeys[kind]?.let { !it.contentEquals(key) } == true
        }
        val overlap = current.cache.magicKeys.keys.intersect(legacy.cache.magicKeys.keys)
        if (conflicts || (current.cache.magicKeys.isNotEmpty() && overlap.isEmpty())) return current
        return current.copy(cache = current.cache.copy(magicKeys = legacy.cache.magicKeys + current.cache.magicKeys))
    }

    fun widgetIds(vararg stores: Map<String, *>): Set<Int> = stores.flatMap { store -> store.keys.mapNotNull { key ->
        Regex("^(\\d+)_(background|color|opacity|shape|automatic|items)$").matchEntire(key)?.groupValues?.get(1)?.toIntOrNull()
    } }.toSet()
}
