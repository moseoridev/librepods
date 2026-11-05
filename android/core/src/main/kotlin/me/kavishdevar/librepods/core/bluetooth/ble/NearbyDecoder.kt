/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

package me.kavishdevar.librepods.core.bluetooth.ble

import android.annotation.SuppressLint
import me.kavishdevar.librepods.core.bluetooth.verifyRPA
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/** Decodes only the selected AirPods. No scanner, service, handler, or repeating work. */
class NearbyDecoder(
    private val keys: () -> Pair<ByteArray?, ByteArray?>,
    private val now: () -> Long
) {
    data class AirPodsStatus(
        val address: String,
        val lastSeen: Long,
        val paired: Boolean = false,
        val model: String = "Unknown",
        val leftBattery: Int? = null,
        val rightBattery: Int? = null,
        val caseBattery: Int? = null,
        val isLeftInEar: Boolean = false,
        val isRightInEar: Boolean = false,
        val isLeftCharging: Boolean = false,
        val isRightCharging: Boolean = false,
        val isCaseCharging: Boolean = false,
        val lidOpen: Boolean = false,
        val color: String = "Unknown",
        val connectionState: String = "Unknown"
    )

    @Volatile private var status: AirPodsStatus? = null
    data class Observation(val address: String, val data: ByteArray, val seen: Long)
    private var observation: Observation? = null
    private var identityKey: ByteArray? = null
    private val verifiedAddresses = linkedSetOf<String>()
    private var lastValidCaseBattery: Int? = null
    private var caseStatus: CaseStatus? = null

    data class CaseStatus(val address: String, val lastSeen: Long,
        val leftBattery: Int?, val rightBattery: Int?, val caseBattery: Int?,
        val isLeftCharging: Boolean, val isRightCharging: Boolean, val isCaseCharging: Boolean)

    @Synchronized fun getCaseStatus(): CaseStatus? {
        val (irk, enc) = keys()
        refreshIdentity(irk, enc)
        return caseStatus?.takeIf { now() - it.lastSeen in 0..MAX_AGE_MS }
    }

    /** The rewrite's case TLV/AES format, without scan registration or device/Service ownership. */
    @Synchronized fun acceptCase(address: String, data: ByteArray, seen: Long): CaseStatus? {
        if (now() - seen !in 0..MAX_AGE_MS) return null
        val (irk, enc) = keys()
        refreshIdentity(irk, enc)
        if (enc?.size != 16 || seen <= (caseStatus?.lastSeen ?: -1)) return null
        var offset = 0
        while (offset + 2 <= data.size) {
            val kind = data[offset].toInt() and 0xff
            val length = data[offset + 1].toInt() and 0x1f
            val start = offset + 2
            val end = start + length
            if (end > data.size) return null
            if (kind == 7 && length >= 17 && data[start] == 6.toByte()) {
                val plain = decryptLastBytes(data.copyOfRange(start + 1, start + 17), enc) ?: return null
                if (plain[1] != 0x20.toByte() || plain[15] != 0.toByte() ||
                    !(3..5).all { i -> (plain[i].toInt() and 0xff).let { it == 0xff || (it and 0x7f) <= 100 } }) return null
                fun level(i: Int): Int? = (plain[i].toInt() and 0xff).let { if (it == 0xff) null else it and 0x7f }
                fun charging(i: Int) = plain[i].toInt() and 0x80 != 0
                return CaseStatus(address, seen, level(4), level(5), level(3), charging(4), charging(5), charging(3))
                    .also { caseStatus = it }
            }
            offset = end
        }
        return null
    }

    private fun refreshIdentity(irk: ByteArray?, enc: ByteArray?) {
        val identity = (irk ?: byteArrayOf()) + (enc ?: byteArrayOf())
        if (identityKey?.contentEquals(identity) != true) {
            clear()
            identityKey = identity.copyOf()
        }
    }

    @Synchronized fun getMostRecentStatus(): AirPodsStatus? {
        val (irk, enc) = keys()
        refreshIdentity(irk, enc)
        return status?.takeIf { now() - it.lastSeen in 0..MAX_AGE_MS }
    }

    @Synchronized fun getObservation(): Observation? =
        observation?.takeIf { getMostRecentStatus() != null }?.let { it.copy(data = it.data.copyOf()) }

    @Synchronized fun clear() {
        status = null
        observation = null
        identityKey = null
        verifiedAddresses.clear()
        lastValidCaseBattery = null
        caseStatus = null
    }

    /** Scan timestamps use elapsed realtime, so delayed batches cannot revive old proximity. */
    @Synchronized fun accept(address: String, data: ByteArray, seen: Long): AirPodsStatus? {
        if (now() - seen !in 0..MAX_AGE_MS || data.size < 27 ||
            data[0] != 7.toByte() || data[1] != 25.toByte()) return null
        val (irk, encryptionKey) = keys()
        if (irk?.size != 16) return null
        refreshIdentity(irk, encryptionKey)
        if (seen <= (status?.lastSeen ?: -1L)) return null
        return try {
            if (address !in verifiedAddresses) {
                if (!verifyRPA(address, irk)) return null
                if (verifiedAddresses.size >= 16) verifiedAddresses.remove(verifiedAddresses.first())
                verifiedAddresses.add(address)
            }
            val decrypted = encryptionKey?.takeIf { it.size == 16 }?.let { decryptLastBytes(data, it) }
            val parsed = if (decrypted != null) parseProximityMessageWithDecryption(address, data, decrypted, seen)
                else parseProximityMessage(address, data, seen)
            status = parsed
            observation = Observation(address, data.copyOf(), seen)
            parsed
        } catch (_: Exception) {
            null
        }
    }

    private val modelNames = mapOf(
        0x0E20 to "AirPods Pro",
        0x1420 to "AirPods Pro 2",
        0x2420 to "AirPods Pro 2 (USB-C)",
        0x0220 to "AirPods 1",
        0x0F20 to "AirPods 2",
        0x1320 to "AirPods 3",
        0x1920 to "AirPods 4",
        0x1B20 to "AirPods 4 (ANC)",
        0x0A20 to "AirPods Max",
        0x1F20 to "AirPods Max (USB-C)"
    )

    val colorNames = mapOf(
        0x00 to "White", 0x01 to "Black", 0x02 to "Red", 0x03 to "Blue",
        0x04 to "Pink", 0x05 to "Gray", 0x06 to "Silver", 0x07 to "Gold",
        0x08 to "Rose Gold", 0x09 to "Space Gray", 0x0A to "Dark Blue",
        0x0B to "Light Blue", 0x0C to "Yellow"
    )

    val connStates = mapOf(
        0x00 to "Disconnected", 0x04 to "Idle", 0x05 to "Music",
        0x06 to "Call", 0x07 to "Ringing", 0x09 to "Hanging Up", 0xFF to "Unknown"
    )

    @SuppressLint("GetInstance")
    private fun decryptLastBytes(data: ByteArray, key: ByteArray): ByteArray? {
        return try {
            if (data.size < 16) {
                return null
            }

            val block = data.copyOfRange(data.size - 16, data.size)
            val cipher = Cipher.getInstance("AES/ECB/NoPadding")
            val secretKey = SecretKeySpec(key, "AES")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            cipher.doFinal(block)
        } catch (e: Exception) {
            null
        }
    }

    private fun formatBattery(byteVal: Int): Pair<Boolean, Int> {
        val charging = (byteVal and 0x80) != 0
        val level = byteVal and 0x7F
        return Pair(charging, level)
    }

    private fun parseProximityMessageWithDecryption(address: String, data: ByteArray, decrypted: ByteArray, seen: Long): AirPodsStatus {
        val paired = data[2].toInt() == 1
        val modelId = ((data[3].toInt() and 0xFF) shl 8) or (data[4].toInt() and 0xFF)
        val model = modelNames[modelId] ?: "Unknown ($modelId)"

        val status = data[5].toInt() and 0xFF
//        val flagsCase = data[7].toInt() and 0xFF
        val lid = data[8].toInt() and 0xFF
        val color = colorNames[data[9].toInt()] ?: "Unknown"
        val conn = connStates[data[10].toInt()] ?: "Unknown (${data[10].toInt()})"

        val primaryLeft = ((status shr 5) and 0x01) == 1
        val thisInCase = ((status shr 6) and 0x01) == 1
        val xorFactor = primaryLeft xor thisInCase

        val isLeftInEar = if (xorFactor) (status and 0x08) != 0 else (status and 0x02) != 0
        val isRightInEar = if (xorFactor) (status and 0x02) != 0 else (status and 0x08) != 0

        val isFlipped = !primaryLeft

        val leftByteIndex = if (isFlipped) 2 else 1
        val rightByteIndex = if (isFlipped) 1 else 2

        val (isLeftCharging, leftBattery) = formatBattery(decrypted[leftByteIndex].toInt() and 0xFF)
        val (isRightCharging, rightBattery) = formatBattery(decrypted[rightByteIndex].toInt() and 0xFF)

        val rawCaseBatteryByte = decrypted[3].toInt() and 0xFF
        val (isCaseCharging, rawCaseBattery) = formatBattery(rawCaseBatteryByte)

        val caseBattery = if (rawCaseBatteryByte == 0xFF || (isCaseCharging && rawCaseBattery == 127)) {
            null
        } else {
            lastValidCaseBattery = rawCaseBattery
            rawCaseBattery
        }

        val lidOpen = ((lid shr 3) and 0x01) == 0

        return AirPodsStatus(
            address = address,
            lastSeen = seen,
            paired = paired,
            model = model,
            leftBattery = leftBattery.takeIf { it in 0..100 },
            rightBattery = rightBattery.takeIf { it in 0..100 },
            caseBattery = caseBattery,
            isLeftInEar = isLeftInEar,
            isRightInEar = isRightInEar,
            isLeftCharging = isLeftCharging,
            isRightCharging = isRightCharging,
            isCaseCharging = isCaseCharging,
            lidOpen = lidOpen,
            color = color,
            connectionState = conn
        )
    }

    private fun parseProximityMessage(address: String, data: ByteArray, seen: Long): AirPodsStatus {
        val paired = data[2].toInt() == 1
        val modelId = ((data[3].toInt() and 0xFF) shl 8) or (data[4].toInt() and 0xFF)
        val model = modelNames[modelId] ?: "Unknown ($modelId)"

        val status = data[5].toInt() and 0xFF
        val podsBattery = data[6].toInt() and 0xFF
        val flagsCase = data[7].toInt() and 0xFF
        val lid = data[8].toInt() and 0xFF
        val color = colorNames[data[9].toInt()] ?: "Unknown"
        val conn = connStates[data[10].toInt()] ?: "Unknown (${data[10].toInt()})"

        val primaryLeft = ((status shr 5) and 0x01) == 1
        val thisInCase = ((status shr 6) and 0x01) == 1
        val xorFactor = primaryLeft xor thisInCase

        val isLeftInEar = if (xorFactor) (status and 0x08) != 0 else (status and 0x02) != 0
        val isRightInEar = if (xorFactor) (status and 0x02) != 0 else (status and 0x08) != 0

        val isFlipped = !primaryLeft

        val leftBatteryNibble = if (isFlipped) (podsBattery shr 4) and 0x0F else podsBattery and 0x0F
        val rightBatteryNibble = if (isFlipped) podsBattery and 0x0F else (podsBattery shr 4) and 0x0F

        val caseBattery = flagsCase and 0x0F
        val flags = (flagsCase shr 4) and 0x0F

        val isLeftCharging = if (isFlipped) (flags and 0x02) != 0 else (flags and 0x01) != 0
        val isRightCharging = if (isFlipped) (flags and 0x01) != 0 else (flags and 0x02) != 0
        val isCaseCharging = (flags and 0x04) != 0

        val lidOpen = ((lid shr 3) and 0x01) == 0

        fun decodeBattery(n: Int): Int? = when (n) {
            in 0x0..0x9 -> n * 10
            in 0xA..0xE -> 100
            0xF -> null
            else -> null
        }

        return AirPodsStatus(
            address = address,
            lastSeen = seen,
            paired = paired,
            model = model,
            leftBattery = decodeBattery(leftBatteryNibble),
            rightBattery = decodeBattery(rightBatteryNibble),
            caseBattery = decodeBattery(caseBattery),
            isLeftInEar = isLeftInEar,
            isRightInEar = isRightInEar,
            isLeftCharging = isLeftCharging,
            isRightCharging = isRightCharging,
            isCaseCharging = isCaseCharging,
            lidOpen = lidOpen,
            color = color,
            connectionState = conn
        )
    }

    companion object {
        const val MAX_AGE_MS = 30_000L

        fun sameContent(first: AirPodsStatus?, second: AirPodsStatus?): Boolean =
            first?.copy(address = "", lastSeen = 0) == second?.copy(address = "", lastSeen = 0)
    }
}
