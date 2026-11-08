package me.kavishdevar.librepods.core.devices

import kotlinx.serialization.Serializable
import me.kavishdevar.librepods.core.data.apple.DeviceColor

@Serializable
data class AppleMetadata(
    override val name: String = "",

    val model: AirPodsModel = AirPodsModel.UNKNOWN,
    val modelName: String = "",
    val modelNumber: String = "",
    val manufacturer: String = "",

    val serialNumber: String = "",
    val leftSerialNumber: String = "",
    val rightSerialNumber: String = "",

    val version1: String = "",
    val version2: String = "",
    val version3: String = "",

    val hardwareRevision: String = "",
    val updaterIdentifier: String = "",

    val color: DeviceColor = DeviceColor.Unknown
): DeviceMetadata {
    override val iconName: String
        get() = AirPodsSpecs.getSpec(model).genericIconName

    /**
     * Non-blank label for surfaces that require one. A persisted device name can be
     * blank before the information packet arrives, and Android rejects notification
     * channels with an empty name, so never hand [name] out on its own.
     */
    override val displayName: String
        get() = name.ifBlank { AirPodsSpecs.getSpec(model).displayName }

    /** Never lets a blank name replace the current one. */
    fun withName(newName: String): AppleMetadata =
        if (newName.isBlank()) this else copy(name = newName)
}
